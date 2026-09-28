package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import java.net.InetAddress

data class RuleSimulationInput(
    val hostOrIp: String = "",
    val destinationPort: Int? = null,
    val sourcePort: Int? = null,
    val network: String? = null,
    val processName: String? = null,
)

enum class RuleSimulationCertainty { DEFINITE, CONDITIONAL, UNKNOWN, NO_MATCH }

data class RuleSimulationResult(
    val certainty: RuleSimulationCertainty,
    val matchedIndex: Int? = null,
    val matchedRule: RoutingRule? = null,
    val target: String? = null,
    val uncertainIndexes: List<Int> = emptyList(),
    val explanation: String,
)

/** A conservative, offline evaluator. Unsupported or unavailable data stays unknown. */
object RuleSimulator {
    private enum class Verdict { MATCH, NO_MATCH, UNKNOWN }

    fun simulate(profile: RuleProfile, input: RuleSimulationInput, localRuleSets: Map<String, List<String>> = emptyMap()): RuleSimulationResult {
        val host = input.hostOrIp.trim()
        if (host.isBlank()) return RuleSimulationResult(RuleSimulationCertainty.NO_MATCH, explanation = "请输入域名或 IP 地址。")
        val uncertain = mutableListOf<Int>()
        profile.rules.forEachIndexed { index, rule ->
            when (evaluate(rule, input, host, profile, localRuleSets)) {
                Verdict.NO_MATCH -> Unit
                Verdict.UNKNOWN -> uncertain += index
                Verdict.MATCH -> {
                    return if (uncertain.isEmpty()) {
                        RuleSimulationResult(RuleSimulationCertainty.DEFINITE, index, rule, rule.group, explanation = "按当前规则顺序，确定命中第 ${index + 1} 条规则。")
                    } else {
                        RuleSimulationResult(RuleSimulationCertainty.CONDITIONAL, index, rule, rule.group, uncertain.toList(), "第 ${index + 1} 条是首个可确定匹配；前面的未知条件可能先命中并改变结果。")
                    }
                }
            }
        }
        return if (uncertain.isEmpty()) {
            RuleSimulationResult(RuleSimulationCertainty.NO_MATCH, explanation = "没有可确定匹配的规则，也没有 MATCH 兜底。")
        } else {
            RuleSimulationResult(RuleSimulationCertainty.UNKNOWN, uncertainIndexes = uncertain, explanation = "前面的规则依赖外部数据或缺少输入，无法确定最终命中结果。")
        }
    }

    private fun evaluate(rule: RoutingRule, input: RuleSimulationInput, host: String, profile: RuleProfile, localRuleSets: Map<String, List<String>>): Verdict = when (rule.type.uppercase()) {
        "MATCH" -> Verdict.MATCH
        "AND", "OR", "NOT" -> {
            val minimum = if (rule.type.equals("NOT", true)) 1 else 2
            if (rule.conditions.size < minimum) Verdict.UNKNOWN else combine(rule.type.uppercase(), rule.conditions.map { evaluate(it, input, host, profile, localRuleSets) })
        }
        "DOMAIN" -> if (isIp(host)) Verdict.NO_MATCH else bool(host.equals(rule.value, true))
        "DOMAIN-SUFFIX" -> if (isIp(host)) Verdict.NO_MATCH else bool(host.equals(rule.value.trimStart('.'), true) || host.endsWith(".${rule.value.trimStart('.')}", true))
        "DOMAIN-KEYWORD" -> if (isIp(host)) Verdict.NO_MATCH else bool(host.contains(rule.value, true))
        "DOMAIN-WILDCARD" -> if (isIp(host)) Verdict.NO_MATCH else wildcard(host, rule.value)
        "DST-PORT" -> comparePort(input.destinationPort, rule.value)
        "SRC-PORT" -> comparePort(input.sourcePort, rule.value)
        "NETWORK" -> input.network?.let { bool(it.equals(rule.value, true)) } ?: Verdict.UNKNOWN
        "PROCESS-NAME" -> input.processName?.let { bool(it.equals(rule.value, true)) } ?: Verdict.UNKNOWN
        "PROCESS-NAME-WILDCARD" -> input.processName?.let { wildcard(it, rule.value) } ?: Verdict.UNKNOWN
        "IP-CIDR", "IP-CIDR6" -> cidrMatch(host, rule.value)
        // The input is the destination host/IP; treating it as a source address would be a false positive.
        "SRC-IP-CIDR", "SRC-IP-SUFFIX", "SRC-IP-ASN", "SRC-GEOIP" -> Verdict.UNKNOWN
        "RULE-SET" -> profile.providers.firstOrNull { it.name == rule.value }?.let { provider ->
            val entries = if (provider.type.equals("inline", true)) provider.payload else localRuleSets[provider.id]
            entries?.let { evaluateRuleSet(provider.behavior, it, input, host, profile, localRuleSets) } ?: Verdict.UNKNOWN
        } ?: Verdict.UNKNOWN
        "DOMAIN-REGEX", "PROCESS-NAME-REGEX" -> {
            val candidate = if (rule.type.startsWith("PROCESS")) input.processName else host
            candidate?.let { runCatching { bool(Regex(rule.value).containsMatchIn(it)) }.getOrDefault(Verdict.UNKNOWN) } ?: Verdict.UNKNOWN
        }
        // A process name is not equivalent to the executable path.
        "PROCESS-PATH-REGEX" -> Verdict.UNKNOWN
        else -> Verdict.UNKNOWN
    }

    private fun evaluate(condition: RuleCondition, input: RuleSimulationInput, host: String, profile: RuleProfile, localRuleSets: Map<String, List<String>>): Verdict {
        condition.operator?.let { operator ->
            val minimum = if (operator.equals("NOT", true)) 1 else 2
            return if (condition.children.size < minimum) Verdict.UNKNOWN else combine(operator.uppercase(), condition.children.map { evaluate(it, input, host, profile, localRuleSets) })
        }
        val type = condition.type?.uppercase() ?: return Verdict.UNKNOWN
        val pseudoRule = RoutingRule(type, condition.value, "", condition.noResolve, condition.source)
        return evaluate(pseudoRule, input, host, profile, localRuleSets)
    }

    private fun evaluateRuleSet(behavior: String, entries: List<String>, input: RuleSimulationInput, host: String, profile: RuleProfile, localRuleSets: Map<String, List<String>>): Verdict {
        var unknown = false
        for (entry in entries) {
            val verdict = when (behavior.lowercase()) {
                "domain" -> domainEntryMatch(host, entry)
                "ipcidr" -> cidrMatch(host, entry.trim())
                "classical" -> {
                    val parts = entry.split(',', limit = 3).map(String::trim)
                    if (parts.size < 2 || parts[0].uppercase() !in CLASSICAL_LOCAL_TYPES) Verdict.UNKNOWN
                    else evaluate(RoutingRule(parts[0], parts[1], ""), input, host, profile, localRuleSets)
                }
                else -> Verdict.UNKNOWN
            }
            if (verdict == Verdict.MATCH) return Verdict.MATCH
            if (verdict == Verdict.UNKNOWN) unknown = true
        }
        return if (unknown) Verdict.UNKNOWN else Verdict.NO_MATCH
    }

    private fun domainEntryMatch(host: String, entry: String): Verdict {
        val value = entry.trim().trimEnd('.')
        val domain = when {
            value.startsWith("+.") || value.startsWith("*.") -> value.drop(2)
            value.startsWith(".") -> value.drop(1)
            else -> value
        }
        if (domain.isBlank() || domain.contains('*') || domain.contains('+')) return Verdict.UNKNOWN
        if (isIp(host)) return Verdict.NO_MATCH
        return when {
            value.startsWith("+.") -> bool(host.equals(domain, true) || host.endsWith(".$domain", true))
            value.startsWith("*.") || value.startsWith(".") -> bool(host.endsWith(".$domain", true))
            else -> bool(host.equals(domain, true))
        }
    }

    private val CLASSICAL_LOCAL_TYPES = setOf(
        "DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "DOMAIN-WILDCARD", "DOMAIN-REGEX",
        "IP-CIDR", "IP-CIDR6", "DST-PORT", "SRC-PORT", "NETWORK", "PROCESS-NAME",
        "PROCESS-NAME-WILDCARD", "PROCESS-NAME-REGEX",
    )

    private fun combine(operator: String, values: List<Verdict>): Verdict = when (operator) {
        "AND" -> when { values.any { it == Verdict.NO_MATCH } -> Verdict.NO_MATCH; values.all { it == Verdict.MATCH } -> Verdict.MATCH; else -> Verdict.UNKNOWN }
        "OR" -> when { values.any { it == Verdict.MATCH } -> Verdict.MATCH; values.all { it == Verdict.NO_MATCH } -> Verdict.NO_MATCH; else -> Verdict.UNKNOWN }
        "NOT" -> when (values.singleOrNull()) { Verdict.MATCH -> Verdict.NO_MATCH; Verdict.NO_MATCH -> Verdict.MATCH; else -> Verdict.UNKNOWN }
        else -> Verdict.UNKNOWN
    }

    private fun comparePort(port: Int?, expression: String): Verdict {
        if (port == null) return Verdict.UNKNOWN
        val range = expression.trim().split("-", limit = 2).map { it.toIntOrNull() ?: return Verdict.UNKNOWN }
        return bool(if (range.size == 1) port == range[0] else port in range[0]..range[1])
    }

    private fun cidrMatch(host: String, expression: String): Verdict {
        if (!isIp(host)) return Verdict.UNKNOWN
        val parts = expression.split("/", limit = 2)
        if (!isIp(parts[0])) return Verdict.UNKNOWN
        val network = runCatching { InetAddress.getByName(parts[0]).address }.getOrNull() ?: return Verdict.UNKNOWN
        val address = runCatching { InetAddress.getByName(host).address }.getOrNull() ?: return Verdict.UNKNOWN
        if (address.size != network.size) return Verdict.NO_MATCH
        val prefix = parts.getOrNull(1)?.toIntOrNull() ?: (network.size * 8)
        if (prefix !in 0..network.size * 8) return Verdict.UNKNOWN
        var remaining = prefix
        for (index in network.indices) {
            val bits = remaining.coerceIn(0, 8)
            val mask = if (bits == 0) 0 else (0xff shl (8 - bits)) and 0xff
            if ((address[index].toInt() and 0xff and mask) != (network[index].toInt() and 0xff and mask)) return Verdict.NO_MATCH
            remaining -= bits
        }
        return Verdict.MATCH
    }

    private fun wildcard(input: String, pattern: String): Verdict = runCatching {
        val regex = Regex("^" + pattern.split("*").joinToString(".*") { Regex.escape(it) } + "$", RegexOption.IGNORE_CASE)
        bool(regex.matches(input))
    }.getOrDefault(Verdict.UNKNOWN)

    private fun isIp(value: String): Boolean = value.contains(':') || value.matches(Regex("\\d{1,3}(?:\\.\\d{1,3}){3}"))
    private fun bool(value: Boolean) = if (value) Verdict.MATCH else Verdict.NO_MATCH
}
