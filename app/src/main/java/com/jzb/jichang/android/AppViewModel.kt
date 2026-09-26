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
import com.jzb.jichang.android.service.GeneratedConfig
import com.jzb.jichang.android.service.MihomoConfigGenerator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JichangRepository(JichangDatabase.get(application).snapshots())
    private val generator = MihomoConfigGenerator()
    val state: StateFlow<AppState> = repository.state

    var message: String? by mutableStateOf(null)
        private set
    var generated: GeneratedConfig = generator.generate(AppState())
        private set

    fun generate(): GeneratedConfig {
        generated = generator.generate(state.value)
        return generated
    }

    fun run(action: suspend () -> String?) {
        viewModelScope.launch {
            message = null
            try { message = action() } catch (error: Throwable) { message = error.message ?: "操作失败" }
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
        val skipped = repository.refreshSource(id)
        "订阅已更新；跳过 $skipped 条无法识别的记录"
    }
    fun removeSource(id: String) = run { repository.removeSource(id); "订阅已删除" }
    fun importNodes(raw: String) = run {
        val (count, skipped) = repository.importNodes(raw)
        "已添加 $count 个节点；跳过 $skipped 条"
    }
    fun toggleNode(id: String) = run { repository.toggleNode(id); null }
    fun removeNode(id: String) = run { repository.removeNode(id); "节点已删除" }
    fun addGroup(name: String, type: String, members: List<String>) = run { repository.addGroup(name, type, members); "策略组已添加" }
    fun updateGroup(oldName: String, name: String, type: String, members: List<String>) = run { repository.updateGroup(oldName, name, type, members); "策略组已更新" }
    fun removeGroup(name: String) = run { repository.removeGroup(name); "策略组已删除" }
    fun addRule(type: String, value: String, group: String) = run { repository.addRule(type, value, group); "规则已添加" }
    fun removeRule(index: Int) = run { repository.removeRule(index); "规则已删除" }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
