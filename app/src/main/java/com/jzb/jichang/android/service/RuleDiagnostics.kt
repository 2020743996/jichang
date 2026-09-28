package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule

data class RuleIssue(
    val index: Int,
    val rule: RoutingRule,
    val message: String,
    val providerId: String? = null,
    val subRuleName: String? = null,
    val groupName: String? = null,
    val relatedIndex: Int? = null,
    val warning: Boolean = false,
)

object RuleDiagnostics {
    private data class MatcherKey(
        val type: String,
        val value: String,
        val conditions: List<RuleCondition>,
        val noResolve: Boolean,
        val source: Boolean,
        val extraParameters: List<String>,
        val rawLine: String?,
    )

    fun inspect(profile: RuleProfile, availableNodeIds: Set<String>? = null): List<RuleIssue> = buildList {
        val groupsByName = LinkedHashMap<String, PolicyGroup>()
        profile.groups.forEach { groupsByName.putIfAbsent(it.name, it) }
        val groupNames = groupsByName.keys + setOf("DIRECT", "REJECT")
        val providerNames = profile.providers.map { it.name }.toSet()
        val subRuleNames = profile.subRules.map { it.name }.toSet()
        if (profile.groups.map { it.name.lowercase() }.toSet().size != profile.groups.size) add(RuleIssue(-1, RoutingRule("", "", ""), "策略组名称重复"))
        profile.groups.forEach { group ->
            group.members.filter { !it.startsWith("node:") && it !in groupNames }.forEach { member ->
                add(RuleIssue(-1, RoutingRule("", "", ""), "策略组“${group.name}”包含无效成员“$member”", groupName = group.name))
            }
            availableNodeIds?.let { nodeIds -> group.members.filter { it.startsWith("node:") && it.removePrefix("node:") !in nodeIds }.forEach { member ->
                add(RuleIssue(-1, RoutingRule("", "", ""), "策略组“${group.name}”引用的节点已不存在", groupName = group.name))
            } }
        }
        val visitingGroups = mutableSetOf<String>()
        val visitedGroups = mutableSetOf<String>()
        fun visitGroup(name: String) {
            if (name in visitedGroups) return
            if (!visitingGroups.add(name)) {
                add(RuleIssue(-1, RoutingRule("", "", ""), "策略组“$name”存在循环成员引用", groupName = name))
                return
            }
            groupsByName[name]?.members?.filter { it in groupsByName }?.forEach(::visitGroup)
            visitingGroups -= name
            visitedGroups += name
        }
        profile.groups.forEach { visitGroup(it.name) }
        if (profile.providers.any { it.name.isBlank() } || profile.providers.map { it.name.lowercase() }.toSet().size != profile.providers.size) add(RuleIssue(-1, RoutingRule("", "", ""), "规则集提供者名称为空或重复"))
        if (profile.subRules.any { it.name.isBlank() } || profile.subRules.map { it.name.lowercase() }.toSet().size != profile.subRules.size) add(RuleIssue(-1, RoutingRule("", "", ""), "子规则名称为空或重复"))
        profile.providers.forEach { provider ->
            fun providerIssue(message: String) { add(RuleIssue(-1, RoutingRule("RULE-SET", provider.name, ""), message, providerId = provider.id)) }
            if (provider.type == "http" && (!provider.url.startsWith("http://") && !provider.url.startsWith("https://"))) providerIssue("规则集“${provider.name}”的 HTTP 地址无效")
            if (provider.type == "http" && provider.interval <= 0) providerIssue("规则集“${provider.name}”的更新间隔必须大于 0")
            if (provider.type == "inline" && provider.payload.isEmpty()) providerIssue("规则集“${provider.name}”没有内嵌规则")
            if (provider.type == "file" && provider.path.isBlank()) providerIssue("规则集“${provider.name}”没有本地文件路径")
            if (provider.type !in setOf("http", "file", "inline")) providerIssue("规则集“${provider.name}”的来源类型无效")
            if (provider.behavior !in setOf("domain", "ipcidr", "classical")) providerIssue("规则集“${provider.name}”的匹配行为无效")
            if (provider.format !in setOf("yaml", "text", "mrs")) providerIssue("规则集“${provider.name}”的格式无效")
            if (provider.headers.any { (name, values) -> name.isBlank() || values.isEmpty() || values.any(String::isBlank) }) providerIssue("规则集“${provider.name}”包含空请求头")
        }
        val earlierRulesByMatcher = mutableMapOf<MatcherKey, MutableList<Pair<Int, RoutingRule>>>()
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
            val matcher = MatcherKey(rule.type.uppercase(), rule.value, rule.conditions, rule.noResolve, rule.source, rule.extraParameters, rule.rawLine)
            earlierRulesByMatcher[matcher]?.forEach { (earlierIndex, earlier) ->
                if (rule.group != earlier.group) {
                    add(RuleIssue(index, rule, "与第 ${earlierIndex + 1} 条规则的匹配条件相同，但目标策略组不同", relatedIndex = earlierIndex, warning = true))
                }
            }
            earlierRulesByMatcher.getOrPut(matcher) { mutableListOf() }.add(index to rule)
        }
        profile.subRules.forEach { subRule ->
            subRule.rules.forEachIndexed { index, rule ->
                val type = rule.type.uppercase().trim()
                if (type !in MihomoConfigGenerator.supportedRuleTypes) add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条规则类型不支持"))
                if (rule.group !in groupNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条规则的目标策略组不存在", subRuleName = subRule.name, groupName = rule.group))
                if (type != "MATCH" && type !in setOf("AND", "OR", "NOT") && rule.value.isBlank()) add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条规则匹配内容为空"))
                if (type == "RULE-SET" && rule.value !in providerNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”引用的规则集提供者不存在", subRuleName = subRule.name))
                if (type == "SUB-RULE" && rule.value !in subRuleNames) add(RuleIssue(-1, rule, "子规则“${subRule.name}”引用的子规则不存在", subRuleName = subRule.name))
                if (type in setOf("AND", "OR", "NOT")) {
                    val minimum = if (type == "NOT") 1 else 2
                    if (rule.conditions.size < minimum && rule.rawLine.isNullOrBlank()) add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条组合规则条件不足"))
                    rule.conditions.forEach { condition -> validateCondition(condition)?.let { add(RuleIssue(-1, rule, "子规则“${subRule.name}”第 ${index + 1} 条规则：$it")) } }
                }
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
