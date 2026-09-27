package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import java.util.UUID

enum class TemplateProviderConflictAction { KEEP_EXISTING, REPLACE, RENAME }

/** Copies selected rule providers and their RULE-SET routing rules from a saved config template. */
object TemplateRuleSetImporter {
    fun apply(
        current: RuleProfile,
        template: ConfigTemplate,
        selectedProviderIds: Set<String>,
        conflictActions: Map<String, TemplateProviderConflictAction> = emptyMap(),
        renamedProviders: Map<String, String> = emptyMap(),
    ): RuleProfile {
        val parsed = MihomoTemplateParser().parse(template.rawYaml)
        val selected = parsed.ruleProfile.providers.filter { it.id in selectedProviderIds }
        require(selected.size == selectedProviderIds.size) { "所选规则集已不存在，请重新选择" }
        if (selected.isEmpty()) return current

        val currentNames = current.providers.map { it.name.lowercase() }.toSet()
        val decisions = selected.mapNotNull { provider ->
            val baseName = "${template.name} · ${provider.name}"
            val collision = baseName.lowercase() in currentNames
            val action = if (collision) conflictActions[provider.id]
                ?: error("规则集“$baseName”已存在，请选择保留、替换或改名")
            else null
            if (action == TemplateProviderConflictAction.KEEP_EXISTING) return@mapNotNull null
            val finalName = if (action == TemplateProviderConflictAction.RENAME) renamedProviders[provider.id]?.trim().orEmpty() else baseName
            require(finalName.isNotBlank()) { "请输入新的规则集名称" }
            require(finalName.none { it == ',' || it == '\n' || it == '\r' }) { "规则集名称不能包含逗号或换行" }
            Triple(provider, action, finalName)
        }
        val finalNames = decisions.map { it.third.lowercase() }
        require(finalNames.toSet().size == finalNames.size) { "导入名称重复，请为规则集指定不同名称" }
        decisions.forEach { (provider, action, name) ->
            if (action != TemplateProviderConflictAction.REPLACE) {
                require(current.providers.none { it.name.equals(name, true) }) { "规则集“$name”已存在，请选择替换或使用其他名称" }
            }
            require(decisions.none { other -> other.first.id != provider.id && other.third.equals(name, true) }) { "导入名称重复，请为规则集指定不同名称" }
        }

        val nextProviders = current.providers.toMutableList()
        val nextRules = current.rules.toMutableList()
        val validTargets = current.groups.map { it.name }.toSet() + setOf("DIRECT", "REJECT")
        val defaultTarget = current.groups.firstOrNull { it.name.equals("PROXY", true) }?.name
            ?: current.groups.firstOrNull()?.name ?: "DIRECT"
        decisions.forEach { (provider, action, finalName) ->
            val baseName = "${template.name} · ${provider.name}"
            if (action == TemplateProviderConflictAction.REPLACE) nextProviders.removeAll { it.name.equals(baseName, true) }
            nextProviders += provider.copy(
                id = UUID.randomUUID().toString(),
                name = finalName,
                sourceTemplateId = template.id,
                sourceTemplateName = template.name,
            )
            parsed.ruleProfile.rules.filter { it.type.equals("RULE-SET", true) && it.value == provider.name }.forEach { rule ->
                val mapped = rule.copy(value = finalName, group = rule.group.takeIf { it in validTargets } ?: defaultTarget)
                if (mapped !in nextRules) nextRules.add(mapped)
            }
        }
        val match = nextRules.lastOrNull { it.type.equals("MATCH", true) }
        nextRules.removeAll { it.type.equals("MATCH", true) }
        match?.let(nextRules::add)
        return current.copy(providers = nextProviders, rules = nextRules)
    }
}
