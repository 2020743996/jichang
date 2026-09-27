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
import com.jzb.jichang.android.service.GeneratedConfig
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.RuleProviderRefresher
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JichangRepository(JichangDatabase.get(application).snapshots())
    private val generator = MihomoConfigGenerator()
    val state: StateFlow<AppState> = repository.state

    var message: String? by mutableStateOf(null)
        private set
    var messageIsError: Boolean by mutableStateOf(false)
        private set
    var generated: GeneratedConfig = generator.generate(AppState())
        private set
    var refreshingSourceIds: Set<String> by mutableStateOf(emptySet())
        private set
    var refreshingRuleProviderIds: Set<String> by mutableStateOf(emptySet())
        private set
    var ruleProviderPreview: Pair<String, String>? by mutableStateOf(null)
        private set

    fun generate(profile: ConfigProfile = state.value.activeProfile): GeneratedConfig {
        generated = generator.generate(state.value, profile)
        return generated
    }

    fun createProfile(name: String, fileName: String, copyActive: Boolean) = run { repository.createProfile(name, fileName, copyActive); "配置已创建并切换" }
    fun createProfileFromTemplate(name: String, fileName: String, templateId: String) = run {
        repository.createProfileFromTemplate(name, fileName, templateId); "已基于模板创建并切换配置"
    }
    fun saveTemplate(name: String, yaml: String, fileName: String) = run {
        repository.saveTemplate(name, yaml, fileName); "模板已保存到本机"
    }
    suspend fun saveTemplateNow(name: String, yaml: String, fileName: String) {
        repository.saveTemplate(name, yaml, fileName)
    }
    fun renameTemplate(id: String, name: String) = run { repository.renameTemplate(id, name); "模板名称已更新" }
    fun deleteTemplate(id: String) = run { repository.deleteTemplate(id); "模板已删除" }
    fun updateRuleProviderStatus(status: RuleProviderStatus) = run { repository.updateRuleProviderStatus(status); null }
    fun refreshRuleProvider(profile: ConfigProfile, provider: com.jzb.jichang.android.model.RuleProvider) = run {
        refreshingRuleProviderIds = refreshingRuleProviderIds + provider.id
        try {
            val refreshed = withContext(Dispatchers.IO) {
                RuleProviderRefresher().refresh(provider, File(getApplication<Application>().filesDir, "rule-providers/${profile.id}"))
            }
            repository.updateRuleProviderStatus(RuleProviderStatus(profile.id, provider.id, System.currentTimeMillis(), null, refreshed.itemCount, refreshed.cacheFileName))
            "规则集已刷新${refreshed.itemCount?.let { "，$it 项" }.orEmpty()}"
        } catch (error: Throwable) {
            val previous = state.value.ruleProviderStatuses.firstOrNull { it.profileId == profile.id && it.providerId == provider.id }
            repository.updateRuleProviderStatus(RuleProviderStatus(profile.id, provider.id, previous?.refreshedAt, error.message ?: "刷新失败", previous?.itemCount, previous?.cacheFileName))
            throw error
        } finally {
            refreshingRuleProviderIds = refreshingRuleProviderIds - provider.id
        }
    }
    fun previewRuleProvider(profile: ConfigProfile, provider: com.jzb.jichang.android.model.RuleProvider, status: RuleProviderStatus?) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    RuleProviderRefresher().preview(provider, File(getApplication<Application>().filesDir, "rule-providers/${profile.id}"), status?.cacheFileName)
                }
            }.onSuccess { ruleProviderPreview = provider.name to it }
                .onFailure { message = it.message ?: "无法预览规则集"; messageIsError = true }
        }
    }
    fun dismissRuleProviderPreview() { ruleProviderPreview = null }
    fun switchProfile(id: String) = run { repository.switchProfile(id); null }
    fun updateProfile(id: String, name: String, fileName: String) = run { repository.updateProfile(id, name, fileName); "配置已保存" }
    fun deleteProfile(id: String) = run { repository.deleteProfile(id); "配置已删除" }

    fun run(action: suspend () -> String?) {
        viewModelScope.launch {
            message = null
            messageIsError = false
            try {
                message = action()
                messageIsError = message?.let { it.contains("失败") || it.contains("错误") } == true
            } catch (error: Throwable) { message = error.message ?: "操作失败"; messageIsError = true }
        }
    }

    fun addSource(name: String, url: String) = run {
        val id = repository.addSource(name, url)
        try {
            val skipped = repository.refreshSource(id)
            "订阅已添加；跳过 $skipped 条无法识别的记录"
        } catch (error: Throwable) {
            "订阅已保存，但刷新失败：${error.message}"
        }
    }

    fun refreshSource(id: String) = run {
        refreshingSourceIds = refreshingSourceIds + id
        try { val skipped = repository.refreshSource(id); "订阅已更新；跳过 $skipped 条无法识别的记录" }
        finally { refreshingSourceIds = refreshingSourceIds - id }
    }
    fun refreshAllSources() = run {
        val total = state.value.sources.size
        if (total == 0) return@run "没有可刷新的订阅"
        val (ok, failed) = repository.refreshAllSources { id -> refreshingSourceIds = if (id.isBlank()) emptySet() else setOf(id) }
        "批量刷新完成：成功 $ok 个，失败 $failed 个"
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
    fun addGroup(name: String, type: String, members: List<String>, ruleIndices: Set<Int> = emptySet(), providerIds: Set<String> = emptySet()) = run {
        repository.addGroup(name, type, members, ruleIndices, providerIds); "策略组已添加，关联规则目标已更新"
    }
    fun updateGroup(oldName: String, name: String, type: String, members: List<String>) = run { repository.updateGroup(oldName, name, type, members); "策略组已更新" }
    fun removeGroup(name: String) = run { repository.removeGroup(name); "策略组已删除" }
    fun addRule(rule: RoutingRule) = run { repository.addRule(rule); "规则已添加" }
    fun updateRule(index: Int, rule: RoutingRule) = run { repository.updateRule(index, rule); "规则已更新" }
    fun moveRule(index: Int, offset: Int) = run { repository.moveRule(index, offset); "规则顺序已更新" }
    fun removeRule(index: Int) = run { repository.removeRule(index); "规则已删除" }
    fun removeRules(indices: Set<Int>) = run { repository.removeRules(indices); "已删除 ${indices.size} 条规则" }
    fun changeRuleTargets(indices: Set<Int>, target: String) = run { repository.changeRuleTargets(indices, target); "已将 ${indices.size} 条规则的目标改为 $target" }
    fun duplicateRule(index: Int) = run { repository.duplicateRule(index); "规则已复制" }
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
