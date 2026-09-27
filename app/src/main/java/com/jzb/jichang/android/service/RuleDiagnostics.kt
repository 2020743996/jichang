package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule

data class RuleIssue(val index: Int, val rule: RoutingRule, val message: String)

object RuleDiagnostics {
    fun inspect(profile: RuleProfile): List<RuleIssue> = buildList {
        val groupNames = profile.groups.map { it.name }.toSet() + setOf("DIRECT", "REJECT")
        val providerNames = profile.providers.map { it.name }.toSet()
        val subRuleNames = profile.subRules.map { it.name }.toSet()
        if (profile.groups.map { it.name.lowercase() }.toSet().size != profile.groups.size) add(RuleIssue(-1, RoutingRule("", "", ""), "策略组名称重复"))
        if (profile.providers.any { it.name.isBlank() } || profile.providers.map { it.name.lowercase() }.toSet().size != profile.providers.size) add(RuleIssue(-1, RoutingRule("", "", ""), "规则集提供者名称为空或重复"))
        if (profile.subRules.any { it.name.isBlank() } || profile.subRules.map { it.name.lowercase() }.toSet().size != profile.subRules.size) add(RuleIssue(-1, RoutingRule("", "", ""), "子规则名称为空或重复"))
        profile.providers.forEach { provider ->
            if (provider.type == "http" && (!provider.url.startsWith("http://") && !provider.url.startsWith("https://"))) add(RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), "规则集“${provider.name}”的 HTTP 地址无效"))
            if (provider.type == "inline" && provider.payload.isEmpty()) add(RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), "规则集“${provider.name}”没有内嵌规则"))
            if (provider.type !in setOf("http", "file", "inline")) add(RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), "规则集“${provider.name}”的来源类型无效"))
        }
        profile.rules.forEachIndexed { index, rule ->
            fun issue(message: String) { add(RuleIssue(index, rule, message)) }
            val type = rule.type.uppercase().trim()
            if (type !in MihomoConfigGenerator.supportedRuleTypes) issue("不支持的规则类型")
            if (rule.group !in groupNames) issue("目标策略组“${rule.group}”不存在")
            if (type != "MATCH" && type !in setOf("AND", "OR", "NOT") && rule.value.isBlank()) issue("匹配内容为空")
            if (rule.value.contains('\n') || rule.value.contains('\r')) issue("匹配内容不能换行")
            if (rule.value.contains(',') && type !in setOf("AND", "OR", "NOT")) issue("匹配内容包含逗号，无法安全生成规则")
            if (type == "RULE-SET" && rule.value !in providerNames) issue("引用的规则集提供者不存在")
            if (type == "SUB-RULE" && rule.value !in subRuleNames) issue("引用的子规则不存在")
            if (type in setOf("AND", "OR", "NOT")) {
                val minimum = if (type == "NOT") 1 else 2
                if (rule.conditions.size < minimum && rule.rawLine.isNullOrBlank()) issue("组合规则条件不足")
                rule.conditions.forEach { condition -> validateCondition(condition)?.let(::issue) }
                if (rule.rawLine?.let { it.contains('\n') || it.contains('\r') } == true) issue("原始规则不能包含换行")
            }
            if ((rule.noResolve || rule.source) && type !in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX")) issue("当前规则类型不支持 IP 参数")
            rule.extraParameters.forEach { parameter ->
                if (parameter.isBlank() || parameter.contains(',') || parameter.contains('\n') || parameter.contains('\r')) issue("存在无效的高级参数")
            }
        }
        profile.subRules.forEach { subRule ->
            subRule.rules.forEachIndexed { index, rule ->
                if (rule.group !in groupNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条规则的目标策略组不存在"))
                if (rule.type == "RULE-SET" && rule.value !in providerNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”引用的规则集提供者不存在"))
                if (rule.type == "SUB-RULE" && rule.value !in subRuleNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”引用的子规则不存在"))
            }
        }
        val matches = profile.rules.withIndex().filter { it.value.type.equals("MATCH", true) }
        if (matches.size > 1) matches.drop(1).forEach { add(RuleIssue(it.index, it.value, "MATCH 兜底规则只能有一条")) }
        if (matches.isNotEmpty() && matches.last().index != profile.rules.lastIndex) {
            val match = matches.last()
            add(RuleIssue(match.index, match.value, "MATCH 会在导出时移动到规则末尾"))
        }
    }

    private fun validateCondition(condition: RuleCondition): String? {
        val operator = condition.operator?.uppercase()
        if (operator != null) {
            val minimum = if (operator == "NOT") 1 else 2
            if (operator !in setOf("AND", "OR", "NOT") || condition.children.size < minimum) return "组合条件无效或子条件不足"
            return condition.children.firstNotNullOfOrNull(::validateCondition)
        }
        val type = condition.type?.uppercase()
        if (type !in MihomoConfigGenerator.supportedRuleTypes || type in setOf("RULE-SET", "SUB-RULE", "AND", "OR", "NOT", "MATCH")) return "子条件类型无效"
        if (condition.value.isBlank() || condition.value.contains(',') || condition.value.contains('\n') || condition.value.contains('\r')) return "子条件匹配内容为空或格式无效"
        return null
    }
}
