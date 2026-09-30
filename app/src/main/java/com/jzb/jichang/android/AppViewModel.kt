package com.jzb.jichang.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jzb.jichang.android.data.JichangDatabase
import com.jzb.jichang.android.data.JichangRepository
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RuleProviderStatus
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.service.RuleProviderRefresher
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import com.jzb.jichang.android.data.ProfileTarget
import com.jzb.jichang.android.service.RefreshBatch
import com.jzb.jichang.android.service.StaleRefreshException

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JichangRepository(JichangDatabase.get(application).snapshots())
    val state: StateFlow<AppState> = repository.state
    val ready = repository.ready
    val loadError = repository.loadError
    fun retryLoad() = repository.retryLoad()
    var messageId by mutableStateOf(0L)
        private set
    var saving by mutableStateOf(false)
        private set
    var saveError: String? by mutableStateOf(null)
        private set
    private val refreshPermits = kotlinx.coroutines.sync.Semaphore(3)
    private val sourcesBatch = RefreshBatch(viewModelScope, refreshPermits)
    private val providersBatch = RefreshBatch(viewModelScope, refreshPermits)
    val sourceRefreshProgress = sourcesBatch.progress
    val providerRefreshProgress = providersBatch.progress
    val refreshingSourceIds: Set<String> get() = sourcesBatch.progress.value.takeIf { it.active }?.requestedIds.orEmpty()
    val refreshingRuleProviderIds: Set<String> get() = providersBatch.progress.value.takeIf { it.active }?.requestedIds.orEmpty()
    val refreshingAllRuleProviders: Boolean get() = providersBatch.progress.value.active
    fun cancelSourceRefresh() = sourcesBatch.cancel()
    fun cancelProviderRefresh() = providersBatch.cancel()

    fun notify(text: String?, error: Boolean = false) {
        message = text
        messageIsError = error
        messageId++
    }

    /** Editors await this result before closing; failure leaves their draft intact. */
    fun save(action: () -> Deferred<Boolean>, onSuccess: () -> Unit) {
        if (saving) return
        saving = true
        saveError = null
        viewModelScope.launch {
            try {
                if (action().await()) onSuccess()
                else saveError = message ?: "保存失败，请重试"
            } catch (error: CancellationException) {
                throw error
            } finally { saving = false }
        }
    }
    fun clearSaveError() { saveError = null }

    var message: String? by mutableStateOf(null)
        private set
    var messageIsError: Boolean by mutableStateOf(false)
        private set
    var ruleProviderPreview: Pair<String, String>? by mutableStateOf(null)
        private set

    fun createProfile(name: String, fileName: String, copyActive: Boolean) = run { repository.createProfile(name, fileName, copyActive); "配置已创建并切换" }
    fun createProfileFromTemplate(name: String, fileName: String, templateId: String) = run {
        repository.createProfileFromTemplate(name, fileName, templateId); "已基于模板创建并切换配置"
    }
    fun saveTemplate(name: String, yaml: String, fileName: String) = run {
        repository.saveTemplate(name, yaml, fileName); "模板已保存到本机"
    }
    suspend fun saveTemplateNow(name: String, yaml: String, fileName: String, remoteURL: String? = null, refreshedAt: Long? = null) {
        repository.saveTemplate(name, yaml, fileName, remoteURL, refreshedAt)
    }
    fun renameTemplate(id: String, name: String) = run { repository.renameTemplate(id, name); "模板名称已更新" }
    fun deleteTemplate(id: String) = run { repository.deleteTemplate(id); "模板已删除" }
    fun updateRuleProviderStatus(status: RuleProviderStatus) = run { repository.updateRuleProviderStatus(status); null }
    fun refreshRuleProvider(profile: ConfigProfile, provider: RuleProvider) = refreshProviders(profile, listOf(provider))
    fun refreshAllRuleProviders(profile: ConfigProfile) = refreshProviders(profile, profile.ruleProfile.providers.filter { it.type.equals("http", true) })
    fun retryFailedRuleProviders(profile: ConfigProfile) = refreshProviders(profile, profile.ruleProfile.providers.filter { it.id in providersBatch.progress.value.failedIds })
    private fun refreshProviders(profile: ConfigProfile, providers: List<RuleProvider>) {
        val byId = providers.associateBy { it.id }
        providersBatch.start(providers.map { it.id }, { id -> refreshRuleProviderNow(profile, byId.getValue(id)) }) {
            notify("规则集刷新完成：成功 ${it.succeeded} 个，失败 ${it.failed} 个" + if (it.ignored > 0) "，忽略 ${it.ignored} 个过期结果" else "", it.failed > 0)
        }
    }
    private suspend fun refreshRuleProviderNow(profile: ConfigProfile, provider: RuleProvider) {
        val directory = File(getApplication<Application>().filesDir, "rule-providers/${profile.id}")
        var newCache: File? = null
        val previousCache = state.value.ruleProviderStatuses.firstOrNull { it.profileId == profile.id && it.providerId == provider.id }?.cacheFileName
        try {
            val refreshed = RuleProviderRefresher().refresh(provider, directory)
            newCache = File(directory, refreshed.cacheFileName)
            repository.commitRuleProviderStatus(provider, RuleProviderStatus(profile.id, provider.id, System.currentTimeMillis(), null, refreshed.itemCount, refreshed.cacheFileName))
            newCache = null
            // Cache housekeeping must not turn a durably saved refresh into a failure.
            runCatching { repository.deleteSupersededCache(profile.id, directory, previousCache) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: StaleRefreshException) {
            throw error
        } catch (error: Exception) {
            val previous = state.value.ruleProviderStatuses.firstOrNull { it.profileId == profile.id && it.providerId == provider.id }
            repository.commitRuleProviderStatus(provider, RuleProviderStatus(profile.id, provider.id, previous?.refreshedAt, error.message ?: "刷新失败", previous?.itemCount, previous?.cacheFileName))
            throw error
        } finally {
            withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                repository.deleteSupersededCache(profile.id, directory, newCache?.name)
            }
        }
    }
    fun previewRuleProvider(profile: ConfigProfile, provider: com.jzb.jichang.android.model.RuleProvider, status: RuleProviderStatus?) {
        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    RuleProviderRefresher().preview(provider, File(getApplication<Application>().filesDir, "rule-providers/${profile.id}"), status?.cacheFileName)
                }
                ruleProviderPreview = provider.name to preview
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                notify(error.message ?: "无法预览规则集", true)
            }
        }
    }
    fun dismissRuleProviderPreview() { ruleProviderPreview = null }
    fun switchProfile(id: String) = run { repository.switchProfile(id); null }
    fun updateProfile(id: String, name: String, fileName: String) = run { repository.updateProfile(id, name, fileName); "配置已保存" }
    fun deleteProfile(id: String) = run { repository.deleteProfile(id); "配置已删除" }
    suspend fun exportBackup(): ByteArray = repository.exportBackup(File(getApplication<Application>().filesDir, "rule-providers"))
    fun importBackup(bytes: ByteArray) = run {
        repository.importBackup(bytes, File(getApplication<Application>().filesDir, "rule-providers"))
        "备份已恢复"
    }

    fun run(action: suspend () -> String?): Deferred<Boolean> {
        val profileId = state.value.activeProfileId
        return viewModelScope.async(ProfileTarget(profileId)) {
            try {
                repository.awaitReady()
                notify(action())
                true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                notify(error.message ?: "操作失败", true)
                false
            }
        }
    }

    private fun runRules(action: suspend () -> String?): Deferred<Boolean> {
        val expected = state.value.ruleProfile.rules
        return run { withContext(com.jzb.jichang.android.data.RuleSnapshot(expected)) { action() } }
    }

    fun addSource(name: String, url: String) = run {
        val id = repository.addSource(name, url)
        refreshSavedSource(id, url.trim())
        "订阅已保存，正在刷新"
    }
    fun updateSource(id: String, name: String, url: String) = run {
        val changedUrl = state.value.sources.firstOrNull { it.id == id }?.url != url.trim()
        repository.updateSource(id, name, url)
        if (changedUrl) refreshSavedSource(id, url.trim())
        if (changedUrl) "订阅已保存，正在刷新；原节点保留至刷新成功" else "订阅已保存"
    }
    private fun refreshSavedSource(id: String, url: String) {
        viewModelScope.launch {
            do {
                sourcesBatch.awaitIdle()
                if (state.value.sources.none { it.id == id && it.url == url }) return@launch
            } while (!startSourceRefresh(listOf(id)))
        }
    }
    fun refreshSource(id: String) { startSourceRefresh(listOf(id)) }
    fun refreshAllSources() { startSourceRefresh(state.value.sources.map { it.id }) }
    fun retryFailedSources() { startSourceRefresh(sourcesBatch.progress.value.failedIds.filter { id -> state.value.sources.any { it.id == id } }) }
    private fun startSourceRefresh(ids: List<String>): Boolean =
        sourcesBatch.start(ids, { id -> repository.refreshSource(id); Unit }) {
            notify("订阅刷新完成：成功 ${it.succeeded} 个，失败 ${it.failed} 个" + if (it.ignored > 0) "，忽略 ${it.ignored} 个过期结果" else "", it.failed > 0)
        }
    fun reorderRules(profileId: String, expected: List<RoutingRule>, order: List<Int>) = run {
        repository.reorderRules(profileId, expected, order)
        "规则顺序已保存"
    }
    fun toggleSource(id: String) = run { repository.toggleSource(id); null }
    fun removeSource(id: String) = run { repository.removeSource(id); "订阅已删除" }
    fun importNodes(raw: String) = run {
        val (count, skipped) = repository.importNodes(raw)
        "已添加 $count 个节点；跳过 $skipped 条"
    }
    fun toggleNode(id: String) = run { repository.toggleNode(id); null }
    fun setNodeEnabled(id: String, enabled: Boolean) = run { repository.toggleNode(id, enabled); null }
    fun setNodesEnabled(ids: Set<String>, enabled: Boolean) = run { repository.setNodesEnabled(ids, enabled); "已${if (enabled) "启用" else "停用"} ${ids.size} 个节点" }
    fun updateNode(id: String, name: String, type: String, server: String, port: Int, options: Map<String, Any?>) = run { repository.updateNode(id, name, type, server, port, options); "节点已保存" }
    fun removeNode(id: String) = run { repository.removeNode(id); "节点已删除" }
    fun addGroup(name: String, type: String, members: List<String>, ruleIndices: Set<Int> = emptySet(), providerIds: Set<String> = emptySet()) = runRules {
        repository.addGroup(name, type, members, ruleIndices, providerIds); "策略组已添加，关联规则目标已更新"
    }
    fun updateGroup(oldName: String, name: String, type: String, members: List<String>) = runRules { repository.updateGroup(oldName, name, type, members); "策略组已更新" }
    fun removeGroup(name: String) = runRules { repository.removeGroup(name); "策略组已删除" }
    fun addRule(rule: RoutingRule) = runRules { repository.addRule(rule); "规则已添加" }
    fun updateRule(index: Int, rule: RoutingRule) = runRules { repository.updateRule(index, rule); "规则已更新" }
    fun moveRule(index: Int, offset: Int) = runRules { repository.moveRule(index, offset); "规则顺序已更新" }
    fun removeRule(index: Int) = runRules { repository.removeRule(index); "规则已删除" }
    fun removeRules(indices: Set<Int>) = runRules { repository.removeRules(indices); "已删除 ${indices.size} 条规则" }
    fun changeRuleTargets(indices: Set<Int>, target: String) = runRules { repository.changeRuleTargets(indices, target); "已将 ${indices.size} 条规则的目标改为 $target" }
    fun duplicateRule(index: Int) = runRules { repository.duplicateRule(index); "规则已复制" }
    fun saveRuleProfile(profile: RuleProfile) = run { repository.saveRuleProfile(profile); "规则集配置已保存" }
    fun saveMihomoSettings(settings: Map<String, Any?>) = run { repository.saveMihomoSettings(settings); "Mihomo 设置已保存" }
    fun saveAdvancedYaml(yaml: String) = run { repository.saveAdvancedYaml(yaml); "高级字段已保存" }
    fun updateExportSettings(sourceMode: String, enabledRegions: Set<String>, regionOverrides: Map<String, String>) = run { repository.updateExportSettings(sourceMode, enabledRegions, regionOverrides); null }
    fun bindTemplateProvider(name: String, sourceId: String?) = run { repository.setTemplateProviderBinding(name, sourceId); "模板订阅绑定已更新" }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
