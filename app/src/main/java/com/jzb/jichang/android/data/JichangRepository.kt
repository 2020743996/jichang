package com.jzb.jichang.android.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.SubscriptionSource
import com.jzb.jichang.android.service.SubscriptionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

class JichangRepository(private val dao: SnapshotDao) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val stateType = object : TypeToken<AppState>() {}.type
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutableState
    private val parser = SubscriptionParser()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    init {
        scope.launch {
            dao.observe().collectLatest { payload ->
                if (payload.isNullOrBlank()) {
                    dao.save(SnapshotEntity(payload = gson.toJson(AppState())))
                } else {
                    runCatching { gson.fromJson<AppState>(payload, stateType) }
                        .getOrNull()?.let { mutableState.value = it }
                }
            }
        }
    }

    fun close() { scope.cancel() }

    suspend fun addSource(name: String, url: String): String {
        val parsed = URI(url)
        require(parsed.scheme in setOf("http", "https") && !parsed.host.isNullOrBlank()) { "请输入有效的 HTTP 或 HTTPS 订阅地址" }
        val source = SubscriptionSource(UUID.randomUUID().toString(), name.trim().ifBlank { parsed.host }, url.trim())
        update { it.copy(sources = it.sources + source) }
        return source.id
    }

    suspend fun refreshSource(sourceId: String): Int = withContext(Dispatchers.IO) {
        val source = mutableState.value.sources.firstOrNull { it.id == sourceId } ?: error("订阅已不存在")
        try {
            val request = Request.Builder().url(source.url).header("User-Agent", "JichangAndroid/0.1.0").build()
            val response = http.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) error("服务器返回 HTTP ${it.code}")
                val body = it.body?.string().orEmpty()
                val result = parser.parse(body, source.id)
                if (result.nodes.isEmpty()) error("没有解析到可用节点（跳过 ${result.skippedCount} 条）")
                update { current ->
                    current.copy(
                        sources = current.sources.map { item -> if (item.id == source.id) item.copy(updatedAt = System.currentTimeMillis(), lastError = null) else item },
                        nodes = current.nodes.filterNot { node -> node.sourceId == source.id } + result.nodes,
                    )
                }
                result.skippedCount
            }
        } catch (error: Throwable) {
            update { current -> current.copy(sources = current.sources.map { item -> if (item.id == source.id) item.copy(lastError = error.message ?: "刷新失败") else item }) }
            throw error
        }
    }

    suspend fun removeSource(sourceId: String) = update { state ->
        state.copy(sources = state.sources.filterNot { it.id == sourceId }, nodes = state.nodes.filterNot { it.sourceId == sourceId })
    }

    suspend fun importNodes(raw: String): Pair<Int, Int> {
        val parsed = parser.parse(raw)
        require(parsed.nodes.isNotEmpty()) { "没有识别到受支持的节点。支持 Mihomo YAML、VMess/VLESS/Trojan/SS 等链接。" }
        update { it.copy(nodes = it.nodes + parsed.nodes) }
        return parsed.nodes.size to parsed.skippedCount
    }

    suspend fun toggleNode(nodeId: String) = update { state ->
        state.copy(nodes = state.nodes.map { if (it.id == nodeId) it.copy(enabled = !it.enabled) else it })
    }

    suspend fun removeNode(nodeId: String) = update { state -> state.copy(nodes = state.nodes.filterNot { it.id == nodeId }) }

    suspend fun addGroup(name: String, type: String, members: List<String>) = update { state ->
        require(name.isNotBlank()) { "请输入策略组名称" }
        require(state.ruleProfile.groups.none { it.name == name.trim() }) { "策略组名称已存在" }
        require(type in setOf("select", "url-test", "fallback", "load-balance")) { "策略组类型无效" }
        state.copy(ruleProfile = state.ruleProfile.copy(groups = state.ruleProfile.groups + PolicyGroup(name.trim(), type, members)))
    }

    suspend fun updateGroup(oldName: String, name: String, type: String, members: List<String>) = update { state ->
        require(name.isNotBlank()) { "请输入策略组名称" }
        require(state.ruleProfile.groups.none { it.name == name.trim() && it.name != oldName }) { "策略组名称已存在" }
        require(type in setOf("select", "url-test", "fallback", "load-balance")) { "策略组类型无效" }
        state.copy(ruleProfile = state.ruleProfile.copy(
            groups = state.ruleProfile.groups.map { if (it.name == oldName) it.copy(name = name.trim(), type = type, members = members) else it },
            rules = state.ruleProfile.rules.map { if (it.group == oldName) it.copy(group = name.trim()) else it },
        ))
    }

    suspend fun removeGroup(name: String) = update { state ->
        if (state.ruleProfile.groups.size <= 1) error("至少保留一个策略组")
        val groups = state.ruleProfile.groups.filterNot { it.name == name }
        val fallback = groups.first().name
        state.copy(ruleProfile = state.ruleProfile.copy(
            groups = groups,
            rules = state.ruleProfile.rules.map { if (it.group == name) it.copy(group = fallback) else it },
        ))
    }

    suspend fun addRule(type: String, value: String, group: String) = update { state ->
        require(type.isNotBlank() && value.isNotBlank()) { "规则类型和匹配内容不能为空" }
        require(group in state.ruleProfile.groups.map { it.name } || group in setOf("DIRECT", "REJECT")) { "请选择有效的策略组" }
        require(!value.contains(',') && !value.contains('\n') && !value.contains('\r')) { "匹配内容不能包含逗号或换行" }
        state.copy(ruleProfile = state.ruleProfile.copy(rules = state.ruleProfile.rules + RoutingRule(type.uppercase(), value.trim(), group)))
    }

    suspend fun removeRule(index: Int) = update { state -> state.copy(ruleProfile = state.ruleProfile.copy(rules = state.ruleProfile.rules.filterIndexed { i, _ -> i != index })) }

    private suspend fun update(transform: (AppState) -> AppState) = lock.withLock {
        val newState = transform(mutableState.value)
        mutableState.value = newState
        dao.save(SnapshotEntity(payload = gson.toJson(newState)))
    }
}
