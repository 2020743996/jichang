package com.jzb.jichang.android.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.SubscriptionSource
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.RuleProviderStatus
import com.jzb.jichang.android.model.SubRuleProfile
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.SubscriptionParser
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.MihomoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

class JichangRepository(private val dao: SnapshotDao) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val lock = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableState = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutableState
    private val parser = SubscriptionParser()
    private val templateParser = MihomoTemplateParser()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    init {
        scope.launch {
            val payload = dao.observe().first()
            if (payload.isNullOrBlank()) {
                dao.save(SnapshotEntity(payload = gson.toJson(AppState())))
            } else {
                runCatching { decodeState(payload) }.getOrNull()?.let { migrated ->
                    mutableState.value = migrated
                    if (!runCatching { JsonParser.parseString(payload).asJsonObject.has("profiles") }.getOrDefault(false)) {
                        dao.save(SnapshotEntity(payload = gson.toJson(migrated)))
                    }
                }
            }
        }
    }

    fun close() { scope.cancel() }

    private fun decodeState(payload: String): AppState {
        val json = JsonParser.parseString(payload).asJsonObject
        val sources = json.getAsJsonArray("sources")?.let { gson.fromJson<List<SubscriptionSource>>(it, object : TypeToken<List<SubscriptionSource>>() {}.type) }.orEmpty()
        val nodes = json.getAsJsonArray("nodes")?.let { gson.fromJson<List<ProxyNode>>(it, object : TypeToken<List<ProxyNode>>() {}.type) }.orEmpty()
        val profilesJson = json.getAsJsonArray("profiles")
        val templates = json.getAsJsonArray("templates")?.let {
            gson.fromJson<List<ConfigTemplate>>(it, object : TypeToken<List<ConfigTemplate>>() {}.type)
        }.orEmpty()
        if (profilesJson != null && profilesJson.size() > 0) {
            val profiles = gson.fromJson<List<ConfigProfile>>(profilesJson, object : TypeToken<List<ConfigProfile>>() {}.type)
                .map { it.copy(templateProviderBindings = it.templateProviderBindings.orEmpty()) }
            val activeId = json.get("activeProfileId")?.asString?.takeIf { id -> profiles.any { it.id == id } } ?: profiles.first().id
            val ruleProviderStatuses = json.getAsJsonArray("ruleProviderStatuses")?.let {
                gson.fromJson<List<RuleProviderStatus>>(it, object : TypeToken<List<RuleProviderStatus>>() {}.type)
            }.orEmpty()
            return AppState(sources, nodes, profiles, activeId, templates, ruleProviderStatuses)
        }

        // Version-1 stored one global rule profile and global enabled flags on resources.
        val rules = json.get("ruleProfile")?.let { gson.fromJson(it, RuleProfile::class.java) } ?: RuleProfile()
        val migrated = ConfigProfile(
            id = "default",
            name = "默认配置",
            selectedSourceIds = sources.filter { it.enabled }.map { it.id }.toSet(),
            enabledNodeIds = nodes.filter { it.enabled }.map { it.id }.toSet(),
            ruleProfile = rules,
        )
        return AppState(sources, nodes, listOf(migrated), migrated.id, templates)
    }

    suspend fun createProfile(name: String, fileName: String, copyActive: Boolean): String = update { state ->
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "请输入配置名称" }
        require(state.profiles.none { it.name.equals(cleanName, true) }) { "配置名称已存在" }
        val active = state.activeProfile
        val profile = if (copyActive) active.copy(
            id = UUID.randomUUID().toString(), name = cleanName, fileName = fileName.trim().ifBlank { cleanName },
            selectedSourceIds = active.selectedSourceIds.toSet(), enabledNodeIds = active.enabledNodeIds.toSet(),
            regionOverrides = active.regionOverrides.toMap(), ruleProfile = active.ruleProfile.copy(
                groups = active.ruleProfile.groups.toList(), rules = active.ruleProfile.rules.toList(),
                providers = active.ruleProfile.providers.toList(), subRules = active.ruleProfile.subRules.toList(),
            ), templateProviderBindings = active.templateProviderBindings.toMap(),
        ) else ConfigProfile(
            id = UUID.randomUUID().toString(), name = cleanName, fileName = fileName.trim().ifBlank { cleanName },
        )
        state.copy(profiles = state.profiles + profile, activeProfileId = profile.id)
    }.activeProfileId

    suspend fun saveTemplate(name: String, rawYaml: String, fileName: String): String = update { state ->
        templateParser.parse(rawYaml)
        val cleanName = name.trim().ifBlank { fileName.substringBeforeLast('.') }
        require(cleanName.isNotBlank()) { "请输入模板名称" }
        require(state.templates.none { it.name.equals(cleanName, true) }) { "模板名称已存在" }
        val template = ConfigTemplate(UUID.randomUUID().toString(), cleanName, rawYaml, fileName)
        state.copy(templates = state.templates + template)
    }.templates.last().id

    suspend fun renameTemplate(templateId: String, name: String) = update { state ->
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "请输入模板名称" }
        require(state.templates.any { it.id == templateId }) { "模板不存在" }
        require(state.templates.none { it.id != templateId && it.name.equals(cleanName, true) }) { "模板名称已存在" }
        state.copy(templates = state.templates.map { if (it.id == templateId) it.copy(name = cleanName) else it })
    }

    suspend fun deleteTemplate(templateId: String) = update { state ->
        require(state.profiles.none { it.templateId == templateId }) { "有配置正在使用此模板，请先删除相关配置" }
        state.copy(templates = state.templates.filterNot { it.id == templateId })
    }

    suspend fun updateRuleProviderStatus(status: RuleProviderStatus) = update { state ->
        val key = status.profileId to status.providerId
        state.copy(ruleProviderStatuses = state.ruleProviderStatuses.filterNot { (it.profileId to it.providerId) == key } + status)
    }

    suspend fun createProfileFromTemplate(name: String, fileName: String, templateId: String): String = update { state ->
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "请输入配置名称" }
        require(state.profiles.none { it.name.equals(cleanName, true) }) { "配置名称已存在" }
        val template = state.templates.firstOrNull { it.id == templateId } ?: error("模板不存在")
        val parsed = templateParser.parse(template.rawYaml)
        val importedNodes = parsed.nodes
        val profile = ConfigProfile(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            fileName = fileName.trim().ifBlank { cleanName },
            enabledNodeIds = importedNodes.map { it.id }.toSet(),
            ruleProfile = parsed.ruleProfile.copy(
                providers = parsed.ruleProfile.providers.map { provider ->
                    provider.copy(sourceTemplateId = template.id, sourceTemplateName = template.name)
                },
            ),
            templateId = template.id,
            mihomoSettings = MihomoSettings.visualFieldsFromRoot(parsed.rawRoot),
            advancedYaml = MihomoSettings.dumpAdvancedFields(parsed.rawRoot),
        )
        state.copy(nodes = state.nodes + importedNodes, profiles = state.profiles + profile, activeProfileId = profile.id)
    }.activeProfileId

    suspend fun switchProfile(profileId: String) = update { state ->
        require(state.profiles.any { it.id == profileId }) { "配置文件不存在" }
        state.copy(activeProfileId = profileId)
    }

    suspend fun updateProfile(profileId: String, name: String, fileName: String) = update { state ->
        val cleanName = name.trim()
        val cleanFileName = fileName.trim()
        require(cleanName.isNotBlank()) { "请输入配置名称" }
        require(cleanFileName.isNotBlank()) { "请输入导出文件名" }
        require(state.profiles.none { it.id != profileId && it.name.equals(cleanName, true) }) { "配置名称已存在" }
        state.copy(profiles = state.profiles.map { if (it.id == profileId) it.copy(name = cleanName, fileName = cleanFileName) else it })
    }

    suspend fun deleteProfile(profileId: String) = update { state ->
        require(state.profiles.size > 1) { "至少保留一个配置文件" }
        val profiles = state.profiles.filterNot { it.id == profileId }
        val activeId = if (state.activeProfileId == profileId) profiles.first().id else state.activeProfileId
        state.copy(profiles = profiles, activeProfileId = activeId)
    }

    suspend fun addSource(name: String, url: String): String {
        val parsed = URI(url.trim())
        require(parsed.scheme in setOf("http", "https") && !parsed.host.isNullOrBlank()) { "请输入有效的 HTTP 或 HTTPS 订阅地址" }
        val source = SubscriptionSource(UUID.randomUUID().toString(), name.trim().ifBlank { parsed.host }, url.trim())
        update { state -> state.copy(
            sources = state.sources + source,
            profiles = state.profiles.map { profile -> if (profile.id == state.activeProfileId) profile.copy(selectedSourceIds = profile.selectedSourceIds + source.id) else profile },
        ) }
        return source.id
    }

    suspend fun refreshSource(sourceId: String): Int = withContext(Dispatchers.IO) {
        val source = mutableState.value.sources.firstOrNull { it.id == sourceId } ?: error("订阅已不存在")
        try {
            val request = Request.Builder().url(source.url).header("User-Agent", "JichangAndroid/0.5.0").build()
            val response = http.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) error("服务器返回 HTTP ${it.code}")
                val body = it.body?.string().orEmpty()
                val result = parser.parse(body, source.id)
                if (result.nodes.isEmpty()) error("没有解析到可用节点（跳过 ${result.skippedCount} 条）")
                val providerCompatible = body.trimStart('\uFEFF', ' ', '\n', '\r', '\t').startsWith("proxies:")
                update { current ->
                    val oldNodes = current.nodes.filter { it.sourceId == source.id }
                    val stableNodes = stabilizeSubscriptionNodes(source.id, oldNodes, result.nodes)
                    val oldIds = oldNodes.map { it.id }.toSet()
                    val freshIds = stableNodes.map { it.id }.toSet()
                    current.copy(
                        sources = current.sources.map { item -> if (item.id == source.id) item.copy(updatedAt = System.currentTimeMillis(), lastError = null, providerCompatible = providerCompatible) else item },
                        nodes = current.nodes.filterNot { node -> node.sourceId == source.id } + stableNodes,
                        profiles = current.profiles.map { profile ->
                            val wasSelected = profile.enabledNodeIds.intersect(oldIds)
                            val mappedSelected = stableNodes.mapIndexedNotNull { index, node ->
                                val oldId = stableNodes[index].id.takeIf { it in oldIds }
                                if (oldId != null && oldId in wasSelected) node.id else null
                            }.toSet()
                            val retained = profile.enabledNodeIds - oldIds
                            val includeNew = source.id in profile.selectedSourceIds
                            val newSelected = stableNodes.filter { it.id !in oldIds && includeNew }.map { it.id }.toSet()
                            val keptSelection = mappedSelected + retained + newSelected
                            val valid = (profile.enabledNodeIds - oldIds) + freshIds
                            profile.copy(enabledNodeIds = keptSelection.intersect(valid))
                        },
                    )
                }
                result.skippedCount
            }
        } catch (error: Throwable) {
            update { current -> current.copy(sources = current.sources.map { item -> if (item.id == source.id) item.copy(lastError = error.message ?: "刷新失败") else item }) }
            throw error
        }
    }

    private fun stabilizeSubscriptionNodes(sourceId: String, previous: List<ProxyNode>, fresh: List<ProxyNode>): List<ProxyNode> {
        val oldByKey = previous.groupBy(::nodeIdentity).mapValues { (_, values) -> values.map { it.id }.toMutableList() }
        val seen = mutableMapOf<String, Int>()
        return fresh.map { node ->
            val key = nodeIdentity(node)
            val oldId = oldByKey[key]?.removeFirstOrNull()
            val occurrence = seen.getOrDefault(key, 0).also { seen[key] = it + 1 }
            node.copy(id = oldId ?: stableNodeId(sourceId, key, occurrence))
        }
    }

    private fun nodeIdentity(node: ProxyNode): String {
        val values = linkedMapOf("type" to node.type.lowercase(), "server" to node.server.lowercase(), "port" to node.port, "name" to node.name.trim(), "options" to node.options.filterKeys { it !in setOf("name", "server", "port") })
        return gson.toJson(values)
    }

    private fun stableNodeId(sourceId: String, identity: String, occurrence: Int): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$sourceId|$identity|$occurrence".toByteArray(StandardCharsets.UTF_8))
        return "sub-" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    suspend fun refreshAllSources(onProgress: (String) -> Unit = {}): Pair<Int, Int> {
        val ids = mutableState.value.sources.map { it.id }
        var succeeded = 0
        ids.forEach { id ->
            onProgress(id)
            try { refreshSource(id); succeeded++ } catch (_: Throwable) { }
        }
        onProgress("")
        return succeeded to (ids.size - succeeded)
    }

    suspend fun removeSource(sourceId: String) = update { state ->
        val removedNodeIds = state.nodes.filter { it.sourceId == sourceId }.map { "node:${it.id}" }.toSet()
        state.copy(
            sources = state.sources.filterNot { it.id == sourceId },
            nodes = state.nodes.filterNot { it.sourceId == sourceId },
            profiles = state.profiles.map { profile -> profile.copy(
                selectedSourceIds = profile.selectedSourceIds - sourceId,
                enabledNodeIds = profile.enabledNodeIds - state.nodes.filter { it.sourceId == sourceId }.map { it.id }.toSet(),
                ruleProfile = profile.ruleProfile.copy(groups = profile.ruleProfile.groups.map { group ->
                    val remaining = group.members - removedNodeIds
                    if (remaining.size != group.members.size) group.copy(members = remaining, membersExplicit = true) else group
                }),
            ) },
        )
    }

    suspend fun toggleSource(sourceId: String) = updateProfile { profile ->
        profile.copy(selectedSourceIds = if (sourceId in profile.selectedSourceIds) profile.selectedSourceIds - sourceId else profile.selectedSourceIds + sourceId)
    }

    suspend fun setSourcesEnabled(sourceIds: Set<String>, enabled: Boolean) = updateProfile { profile ->
        profile.copy(selectedSourceIds = if (enabled) profile.selectedSourceIds + sourceIds else profile.selectedSourceIds - sourceIds)
    }

    suspend fun importNodes(raw: String): Pair<Int, Int> {
        val parsed = parser.parse(raw)
        require(parsed.nodes.isNotEmpty()) { "没有识别到受支持的节点。支持 Mihomo YAML、VMess/VLESS/Trojan/SS 等链接。" }
        update { state -> state.copy(
            nodes = state.nodes + parsed.nodes,
            profiles = state.profiles.map { profile -> if (profile.id == state.activeProfileId) profile.copy(enabledNodeIds = profile.enabledNodeIds + parsed.nodes.map { it.id }) else profile },
        ) }
        return parsed.nodes.size to parsed.skippedCount
    }

    suspend fun toggleNode(nodeId: String) = toggleNode(nodeId, nodeId !in mutableState.value.activeProfile.enabledNodeIds)

    suspend fun toggleNode(nodeId: String, enabled: Boolean) = updateProfile { profile ->
        profile.copy(enabledNodeIds = if (enabled) profile.enabledNodeIds + nodeId else profile.enabledNodeIds - nodeId)
    }

    suspend fun setNodesEnabled(nodeIds: Set<String>, enabled: Boolean) = updateProfile { profile ->
        profile.copy(enabledNodeIds = if (enabled) profile.enabledNodeIds + nodeIds else profile.enabledNodeIds - nodeIds)
    }

    suspend fun updateNode(nodeId: String, name: String, type: String, server: String, port: Int, options: Map<String, Any?>) = update { state ->
        require(name.isNotBlank()) { "请输入节点名称" }
        require(server.isNotBlank()) { "请输入服务器地址" }
        require(port in 1..65535) { "端口须为 1-65535" }
        state.copy(nodes = state.nodes.map { node -> if (node.id == nodeId) node.copy(name = name.trim(), type = type, server = server.trim(), port = port, options = options + mapOf("type" to type, "name" to name.trim(), "server" to server.trim(), "port" to port)) else node })
    }

    suspend fun removeNode(nodeId: String) = update { state -> state.copy(
        nodes = state.nodes.filterNot { it.id == nodeId },
        profiles = state.profiles.map { profile -> profile.copy(
            enabledNodeIds = profile.enabledNodeIds - nodeId,
            regionOverrides = profile.regionOverrides - nodeId,
            ruleProfile = profile.ruleProfile.copy(groups = profile.ruleProfile.groups.map { group ->
                if ("node:$nodeId" in group.members) group.copy(members = group.members - "node:$nodeId", membersExplicit = true) else group
            }),
        ) },
    ) }

    suspend fun addGroup(
        name: String,
        type: String,
        members: List<String>,
        ruleIndices: Set<Int> = emptySet(),
        providerIds: Set<String> = emptySet(),
    ) = updateProfile { profile ->
        val rules = profile.ruleProfile
        require(name.isNotBlank()) { "请输入策略组名称" }
        require(rules.groups.none { group -> group.name == name.trim() }) { "策略组名称已存在" }
        require(type in setOf("select", "url-test", "fallback", "load-balance", "ssid", "smart")) { "策略组类型无效" }
        val cleanName = name.trim()
        val effectiveNodes = mutableState.value.nodes.filter { node ->
            (node.sourceId == null || node.sourceId in profile.selectedSourceIds) && node.id in profile.enabledNodeIds
        }.map { "node:${it.id}" }.toSet()
        val validMembers = effectiveNodes + rules.groups.map { it.name } + setOf("DIRECT", "REJECT")
        require(members.all { it in validMembers && it != cleanName }) { "策略组成员包含无效节点或策略组" }
        require(ruleIndices.all { it in rules.rules.indices && !rules.rules[it].type.equals("MATCH", true) }) { "所选规则已发生变化，请重新选择" }
        val selectedProviders = rules.providers.filter { it.id in providerIds }
        require(selectedProviders.size == providerIds.size) { "所选规则集已不存在，请重新选择" }
        val targetRules = rules.rules.mapIndexed { index, rule ->
            if (index in ruleIndices) rule.copy(group = cleanName) else rule
        }.toMutableList()
        selectedProviders.forEach { provider ->
            val matching = targetRules.indices.filter { index ->
                targetRules[index].type.equals("RULE-SET", true) && targetRules[index].value == provider.name
            }
            if (matching.isEmpty()) targetRules += RoutingRule("RULE-SET", provider.name, cleanName)
            else matching.forEach { index -> targetRules[index] = targetRules[index].copy(group = cleanName) }
        }
        val fallback = targetRules.lastOrNull { it.type.equals("MATCH", true) }
        targetRules.removeAll { it.type.equals("MATCH", true) }
        fallback?.let(targetRules::add)
        val newGroups = rules.groups + PolicyGroup(cleanName, type, members.distinct(), membersExplicit = true)
        requireAcyclicGroups(newGroups)
        profile.copy(ruleProfile = rules.copy(
            groups = newGroups,
            rules = targetRules,
        ))
    }

    suspend fun updateGroup(oldName: String, name: String, type: String, members: List<String>) = updateProfile { profile ->
        val rules = profile.ruleProfile
        require(name.isNotBlank()) { "请输入策略组名称" }
        require(rules.groups.none { it.name == name.trim() && it.name != oldName }) { "策略组名称已存在" }
        require(type in setOf("select", "url-test", "fallback", "load-balance", "ssid", "smart")) { "策略组类型无效" }
        val effectiveNodes = mutableState.value.nodes.filter { node ->
            (node.sourceId == null || node.sourceId in profile.selectedSourceIds) && node.id in profile.enabledNodeIds
        }.map { "node:${it.id}" }.toSet()
        val validMembers = effectiveNodes + rules.groups.filterNot { it.name == oldName }.map { it.name } + setOf("DIRECT", "REJECT")
        require(members.all { it in validMembers && it != name.trim() }) { "策略组成员包含无效节点或策略组" }
        val newGroups = rules.groups.map { group ->
            when {
                group.name == oldName -> group.copy(name = name.trim(), type = type, members = members.distinct(), membersExplicit = true)
                oldName in group.members -> group.copy(members = group.members.map { if (it == oldName) name.trim() else it })
                else -> group
            }
        }
        requireAcyclicGroups(newGroups)
        val renamedRules = rules.rules.map { if (it.group == oldName) it.copy(group = name.trim()) else it }
        val renamedSubRules = rules.subRules.map { sub -> sub.copy(rules = sub.rules.map { if (it.group == oldName) it.copy(group = name.trim()) else it }) }
        profile.copy(ruleProfile = rules.copy(groups = newGroups, rules = renamedRules, subRules = renamedSubRules))
    }

    suspend fun removeGroup(name: String) = updateProfile { profile ->
        val rules = profile.ruleProfile
        if (rules.groups.size <= 1) error("至少保留一个策略组")
        val groups = rules.groups.filterNot { it.name == name }.map { group ->
            if (name in group.members) group.copy(members = group.members - name, membersExplicit = true) else group
        }
        val fallback = groups.first().name
        val reassignedRules = rules.rules.map { if (it.group == name) it.copy(group = fallback) else it }
        val reassignedSubRules = rules.subRules.map { sub -> sub.copy(rules = sub.rules.map { if (it.group == name) it.copy(group = fallback) else it }) }
        profile.copy(ruleProfile = rules.copy(groups = groups, rules = reassignedRules, subRules = reassignedSubRules))
    }

    suspend fun addRule(rule: RoutingRule) = updateProfile { profile ->
        validateRule(rule, profile.ruleProfile)
        val fallback = profile.ruleProfile.rules.lastOrNull { it.type.equals("MATCH", true) }
        val rules = if (rule.type.equals("MATCH", true)) profile.ruleProfile.rules.filterNot { it.type.equals("MATCH", true) } + rule
        else profile.ruleProfile.rules.filterNot { it.type.equals("MATCH", true) } + rule + listOfNotNull(fallback)
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = rules))
    }

    suspend fun updateRule(index: Int, rule: RoutingRule) = updateProfile { profile ->
        require(index in profile.ruleProfile.rules.indices) { "规则已不存在" }
        validateRule(rule, profile.ruleProfile)
        var rules = profile.ruleProfile.rules.toMutableList().also { it[index] = rule }
        if (rule.type.equals("MATCH", true)) rules = (rules.filterNot { it.type.equals("MATCH", true) } + rule).toMutableList()
        else {
            val fallback = rules.lastOrNull { it.type.equals("MATCH", true) }
            rules = rules.filterNot { it.type.equals("MATCH", true) }.toMutableList().apply { fallback?.let(::add) }
        }
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = rules))
    }

    private fun validateRule(rule: RoutingRule, profile: RuleProfile) {
        require(rule.type.uppercase() in MihomoConfigGenerator.supportedRuleTypes) { "规则类型无效" }
        val composite = rule.type.uppercase() in setOf("AND", "OR", "NOT")
        require(rule.type.equals("MATCH", true) || rule.value.isNotBlank() || rule.conditions.isNotEmpty() || (composite && !rule.rawLine.isNullOrBlank())) { "请输入匹配内容" }
        require(rule.group in profile.groups.map { it.name } || rule.group in setOf("DIRECT", "REJECT")) { "请选择有效的策略组" }
        require(rule.value.none { it == '\n' || it == '\r' }) { "匹配内容不能包含换行" }
        require(rule.type.uppercase() in setOf("MATCH", "AND", "OR", "NOT") || ',' !in rule.value) { "匹配内容不能包含逗号" }
        require(rule.type.uppercase() != "RULE-SET" || profile.providers.any { it.name == rule.value }) { "请先创建对应的规则集" }
        require(rule.type.uppercase() != "SUB-RULE" || profile.subRules.any { it.name == rule.value }) { "请先创建对应的子规则" }
        require(rule.extraParameters.none { it.isBlank() || it.contains(',') || it.contains('\n') || it.contains('\r') }) { "高级参数格式无效" }
        require(rule.rawLine?.none { it == '\n' || it == '\r' } != false) { "原始规则不能包含换行" }
        if (rule.type.uppercase() in setOf("AND", "OR", "NOT") && rule.conditions.size < if (rule.type.equals("NOT", true)) 1 else 2) {
            require(!rule.rawLine.isNullOrBlank() && !rule.rawLine.contains('\n') && !rule.rawLine.contains('\r')) { "组合规则条件不足，且没有可保留的原始规则" }
        }
    }

    suspend fun moveRule(index: Int, offset: Int) = updateProfile { profile ->
        val rules = profile.ruleProfile.rules
        if (index !in rules.indices || rules[index].type.equals("MATCH", true)) return@updateProfile profile
        val lastMovable = rules.lastIndex - if (rules.any { it.type.equals("MATCH", true) }) 1 else 0
        val destination = (index + offset).coerceIn(0, lastMovable.coerceAtLeast(0))
        if (destination == index) return@updateProfile profile
        val changed = rules.toMutableList().also { items -> items.add(destination, items.removeAt(index)) }
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = changed))
    }

    suspend fun removeRule(index: Int) = updateProfile { profile ->
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = profile.ruleProfile.rules.filterIndexed { i, _ -> i != index }))
    }

    suspend fun removeRules(indices: Set<Int>) = updateProfile { profile ->
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = profile.ruleProfile.rules.filterIndexed { index, _ -> index !in indices }))
    }

    suspend fun changeRuleTargets(indices: Set<Int>, target: String) = updateProfile { profile ->
        require(target in profile.ruleProfile.groups.map { it.name } + setOf("DIRECT", "REJECT")) { "请选择有效的策略目标" }
        val rules = profile.ruleProfile.rules.mapIndexed { index, rule -> if (index in indices && !rule.type.equals("MATCH", true)) rule.copy(group = target) else rule }
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = rules))
    }

    suspend fun duplicateRule(index: Int) = updateProfile { profile ->
        require(index in profile.ruleProfile.rules.indices) { "规则已不存在" }
        val source = profile.ruleProfile.rules[index]
        require(!source.type.equals("MATCH", true)) { "MATCH 兜底规则不能复制" }
        val rules = profile.ruleProfile.rules.toMutableList()
        rules.add(index + 1, source.copy())
        profile.copy(ruleProfile = profile.ruleProfile.copy(rules = rules))
    }

    suspend fun saveRuleProfile(profile: RuleProfile) = updateProfile { it.copy(ruleProfile = profile) }

    suspend fun saveMihomoSettings(settings: Map<String, Any?>) = updateProfile {
        it.copy(mihomoSettings = MihomoSettings.normalizeVisualSettings(settings))
    }

    suspend fun saveAdvancedYaml(yaml: String) = updateProfile {
        MihomoSettings.validateAdvancedYaml(yaml)
        it.copy(advancedYaml = yaml)
    }

    suspend fun updateExportSettings(sourceMode: String, enabledRegions: Set<String>, regionOverrides: Map<String, String>) = updateProfile { profile ->
        profile.copy(sourceMode = sourceMode, enabledRegions = enabledRegions, regionOverrides = regionOverrides)
    }

    suspend fun setTemplateProviderBinding(providerName: String, sourceId: String?) = updateProfile { profile ->
        profile.copy(templateProviderBindings = if (sourceId.isNullOrBlank()) profile.templateProviderBindings - providerName else profile.templateProviderBindings + (providerName to sourceId))
    }

    private fun ruleProfile(profile: ConfigProfile) = profile.ruleProfile

    private fun requireAcyclicGroups(groups: List<PolicyGroup>) {
        val names = groups.map { it.name }.toSet()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(name: String) {
            if (name in visited) return
            require(name !in visiting) { "策略组成员不能形成循环引用" }
            visiting += name
            groups.firstOrNull { it.name == name }?.members?.filter { it in names }?.forEach(::visit)
            visiting -= name
            visited += name
        }
        names.forEach(::visit)
    }

    private suspend fun updateProfile(transform: (ConfigProfile) -> ConfigProfile) = update { state ->
        state.copy(profiles = state.profiles.map { if (it.id == state.activeProfileId) transform(it) else it })
    }

    private suspend fun update(transform: (AppState) -> AppState): AppState = lock.withLock {
        val next = transform(mutableState.value)
        mutableState.value = next
        dao.save(SnapshotEntity(payload = gson.toJson(next)))
        next
    }
}
