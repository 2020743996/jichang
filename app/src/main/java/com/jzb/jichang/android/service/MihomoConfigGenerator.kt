package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProvider
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.util.LinkedHashMap

enum class ConfigSourceMode { EMBED_NODES, REFERENCE_SUBSCRIPTIONS }

data class ConfigExportOptions(
    val sourceMode: ConfigSourceMode = ConfigSourceMode.EMBED_NODES,
    val excludedNodeIds: Set<String> = emptySet(),
    val regionOverrides: Map<String, String> = emptyMap(),
    val enabledRegions: Set<String> = NodeAutoGroups.allKeys,
    val selectedSourceIds: Set<String>? = null,
    val enabledNodeIds: Set<String>? = null,
)

data class GeneratedConfig(
    val yaml: String,
    val exportedNodes: Int,
    val skippedNodes: Int,
    val referencedSubscriptions: Int = 0,
)

/** Builds one Mihomo profile; it intentionally has no per-client dialect matrix. */
class MihomoConfigGenerator {
    private val supportedTypes = setOf(
        "ss", "vmess", "vless", "trojan", "hysteria", "hysteria2", "tuic", "wireguard",
        "anytls", "snell", "socks5", "http", "ssh", "socks"
    )

    fun generate(state: AppState, exportOptions: ConfigExportOptions = optionsFor(state.activeProfile)): GeneratedConfig =
        generate(state, state.activeProfile, exportOptions)

    fun generate(state: AppState, profile: ConfigProfile, exportOptions: ConfigExportOptions = optionsFor(profile)): GeneratedConfig {
        val enabledSources = state.sources.filter { it.id in (exportOptions.selectedSourceIds ?: state.sources.filter { source -> source.enabled }.map { it.id }.toSet()) }
        val enabledSourceIds = enabledSources.map { it.id }.toSet()
        val selectedNodes = state.nodes.filter { node ->
            node.id in (exportOptions.enabledNodeIds ?: state.nodes.filter { it.enabled }.map { it.id }.toSet()) &&
                node.id !in exportOptions.excludedNodeIds && (node.sourceId == null || node.sourceId in enabledSourceIds)
        }
        val supportedSelected = selectedNodes.filter { it.type.lowercase() in supportedTypes }
        val skipped = selectedNodes.size - supportedSelected.size
        val providerSources = if (exportOptions.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) {
            enabledSources.filter { it.providerCompatible == true }
        } else emptyList()
        val providerNames = providerSources.mapIndexed { index, source -> source.id to "订阅-${index + 1}" }.toMap()
        val embeddedNodes = if (exportOptions.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) {
            supportedSelected.filter { it.sourceId == null || it.sourceId !in providerNames.keys }
        } else supportedSelected
        val names = uniqueNames(embeddedNodes)
        val proxies = embeddedNodes.mapIndexed { index, node -> proxyMap(node, names[index]) }
        val proxyNamesById = embeddedNodes.mapIndexed { index, node -> node.id to names[index] }.toMap()
        val providerSnapshotNodes = state.nodes.filter { it.sourceId in providerNames.keys }
        val selectedNodeIds = exportOptions.enabledNodeIds ?: state.nodes.filter { it.enabled }.map { it.id }.toSet()
        val includedRemoteNodes = providerSnapshotNodes.filter { it.id in selectedNodeIds && it.id !in exportOptions.excludedNodeIds }
        val regions = includedRemoteNodes.associate { node -> node.id to regionFor(node, exportOptions) }
        val generated = generateRegionGroups(
            options = exportOptions,
            embeddedNodes = embeddedNodes,
            embeddedNames = names,
            providerNames = providerNames.values.toList(),
            providerSnapshotNodes = providerSnapshotNodes,
            includedRemoteNodes = includedRemoteNodes,
            regions = regions,
        )

        val ruleProfile = profile.ruleProfile
        val configuredGroups = ruleProfile.groups.filter { it.name.isNotBlank() }.ifEmpty { listOf(PolicyGroup("PROXY")) }
        val groups = (configuredGroups + generated.groups).distinctBy { it.name }
        val groupNames = groups.map { it.name }.toSet()
        val allNodeNames = state.nodes.associate { it.id to it.name.safeName() }
        val groupYaml = groups.map { group ->
            val isGenerated = generated.groups.any { it.name == group.name }
            val isDefaultMaster = !isGenerated && group.name == "PROXY" && group.members.isEmpty() && generated.groups.isNotEmpty()
            val configuredMembers = when {
                isGenerated -> group.members
                isDefaultMaster -> generated.groups.map { it.name }
                group.members.isEmpty() -> names
                else -> group.members.mapNotNull { member ->
                    val id = member.removePrefix("node:")
                    proxyNamesById[id] ?: member.takeUnless { member.startsWith("node:") }
                }
            }
            val candidates = configuredMembers.filter { it in names || it in groupNames || it == "DIRECT" || it == "REJECT" }
            linkedMapOf<String, Any?>(
                "name" to group.name.safeName(),
                "type" to (group.type.takeIf { it in setOf("select", "url-test", "fallback", "load-balance") } ?: "select"),
                "proxies" to candidates.ifEmpty { listOf("DIRECT") },
            ).apply {
                if (group.type in setOf("url-test", "fallback", "load-balance")) {
                    put("url", "https://www.gstatic.com/generate_204")
                    put("interval", 300)
                }
                if (providerNames.isNotEmpty()) {
                    if (isGenerated) {
                        put("use", providerNames.values.toList())
                        generated.filters[group.name]?.let { filters ->
                            filters.first?.let { put("filter", it) }
                            filters.second?.let { put("exclude-filter", it) }
                        }
                    } else if (!isDefaultMaster) {
                        val remoteMembers = if (group.members.isEmpty()) includedRemoteNodes else group.members.mapNotNull { member ->
                            if (member.startsWith("node:")) includedRemoteNodes.firstOrNull { it.id == member.removePrefix("node:") } else null
                        }
                        if (remoteMembers.isNotEmpty() || group.members.isEmpty()) {
                            put("use", providerNames.values.toList())
                            if (group.members.isNotEmpty()) put("filter", combinePatterns(remoteMembers.map { exactNamePattern(allNodeNames[it.id].orEmpty()) }) ?: "$^")
                        }
                    }
                }
            }
        }

        val fallbackDefault = if ("PROXY" in groupNames) "PROXY" else groups.firstOrNull()?.name ?: "DIRECT"
        val explicitFallback = ruleProfile.rules.lastOrNull { it.type.equals("MATCH", true) }
        val regularRules = ruleProfile.rules.filterNot { it.type.equals("MATCH", true) }.mapNotNull { rule ->
            val type = rule.type.uppercase().trim()
            val value = rule.value.trim()
            val target = rule.group.trim().takeIf { it in groupNames || it == "DIRECT" || it == "REJECT" } ?: return@mapNotNull null
            if (type !in supportedRuleTypes) return@mapNotNull null
            serializeRule(rule, target, ruleProfile.subRules.map { it.name }.toSet(), ruleProfile.providers.map { it.name }.toSet())
        }.toMutableList()
        val fallbackTarget = explicitFallback?.group?.takeIf { it in groupNames || it == "DIRECT" || it == "REJECT" } ?: fallbackDefault
        regularRules += "MATCH,$fallbackTarget"

        val root = linkedMapOf<String, Any?>(
            "mixed-port" to 7890,
            "allow-lan" to false,
            "mode" to "rule",
            "log-level" to "info",
            "ipv6" to true,
            "proxies" to proxies,
        )
        if (providerNames.isNotEmpty()) root["proxy-providers"] = providerSources.mapIndexed { index, source ->
            "订阅-${index + 1}" to linkedMapOf<String, Any?>(
                "type" to "http",
                "url" to source.url,
                "path" to "./providers/jichang-${index + 1}.yaml",
                "interval" to 3600,
                "health-check" to linkedMapOf("enable" to true, "url" to "https://www.gstatic.com/generate_204", "interval" to 600),
            )
        }.toMap(LinkedHashMap())
        if (ruleProfile.providers.isNotEmpty()) root["rule-providers"] = ruleProfile.providers
            .filter { it.name.isNotBlank() }
            .associateTo(LinkedHashMap()) { provider -> provider.name.safeKey() to ruleProviderMap(provider) }
        if (ruleProfile.subRules.isNotEmpty()) root["sub-rules"] = ruleProfile.subRules
            .filter { it.name.isNotBlank() }
            .associateTo(LinkedHashMap()) { subRule -> subRule.name.safeKey() to subRule.rules.mapNotNull { sub -> serializeRule(sub, sub.group.ifBlank { "DIRECT" }, ruleProfile.subRules.map { it.name }.toSet(), ruleProfile.providers.map { it.name }.toSet()) } }
        root["proxy-groups"] = groupYaml
        root["rules"] = regularRules

        val dumpOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            width = 120
        }
        return GeneratedConfig(Yaml(dumpOptions).dump(root), embeddedNodes.size, skipped, providerNames.size)
    }

    private fun serializeRule(rule: com.jzb.jichang.android.model.RoutingRule, target: String, subRuleNames: Set<String> = emptySet(), ruleProviderNames: Set<String> = emptySet()): String? {
        val type = rule.type.uppercase()
        val value = rule.value.trim()
        if (type in setOf("AND", "OR", "NOT")) {
            val minimum = if (type == "NOT") 1 else 2
            if (rule.conditions.size < minimum) return null
            val conditions = rule.conditions.mapNotNull(::serializeConditionRule)
            if (conditions.size < minimum) return null
            return "$type,(${conditions.joinToString(",") { "($it)" }}),$target"
        }
        if (type == "SUB-RULE") {
            if (rule.value.isBlank() || rule.value !in subRuleNames) return null
            return "SUB-RULE,${rule.value.safeField()},$target"
        }
        if (type == "RULE-SET" && value !in ruleProviderNames) return null
        if (value.isBlank() || value.contains('\n') || value.contains('\r')) return null
        val params = buildList {
            if (rule.noResolve && type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "SRC-IP-CIDR", "SRC-IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN")) add("no-resolve")
            if (rule.source && type in setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")) add("src")
        }
        return listOf(type, value, target).plus(params).joinToString(",")
    }

    private fun serializeConditionRule(condition: RuleCondition): String? {
        val operator = condition.operator?.uppercase()
        if (operator != null) {
            if (operator !in setOf("AND", "OR", "NOT") || condition.children.size < if (operator == "NOT") 1 else 2) return null
            val children = condition.children.mapNotNull(::serializeConditionRule)
            if (children.size < if (operator == "NOT") 1 else 2) return null
            return "$operator,(${children.joinToString(",") { "($it)" }})"
        }
        val type = condition.type?.uppercase()?.takeIf { it in conditionTypes } ?: return null
        val value = condition.value.trim().takeIf(String::isNotBlank) ?: return null
        if (value.contains(',') || value.contains('\n') || value.contains('\r')) return null
        val params = buildList {
            if (condition.noResolve && type in ipRuleTypes) add("no-resolve")
            if (condition.source && type in targetIpRuleTypes) add("src")
            condition.argument?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        }
        return listOf(type, value).plus(params).joinToString(",")
    }

    private fun ruleProviderMap(provider: RuleProvider): LinkedHashMap<String, Any?> = linkedMapOf<String, Any?>(
        "type" to provider.type,
        "behavior" to provider.behavior,
        "format" to provider.format,
    ).apply {
        if (provider.type == "http") {
            put("url", provider.url)
            put("path", provider.path.ifBlank { "./rule-providers/${provider.name.safeKey()}.yaml" })
            put("interval", provider.interval.coerceAtLeast(60))
            if (provider.headers.isNotEmpty()) put("header", provider.headers)
        } else if (provider.type == "file") put("path", provider.path.ifBlank { "./rule-providers/${provider.name.safeKey()}.yaml" })
        if (provider.type == "inline") put("payload", provider.payload)
    }

    private fun optionsFor(profile: ConfigProfile) = ConfigExportOptions(
        sourceMode = runCatching { ConfigSourceMode.valueOf(profile.sourceMode) }.getOrDefault(ConfigSourceMode.EMBED_NODES),
        regionOverrides = profile.regionOverrides,
        enabledRegions = profile.enabledRegions,
        selectedSourceIds = profile.selectedSourceIds,
        enabledNodeIds = profile.enabledNodeIds,
    )

    private fun String.safeField(): String = if (contains(',') || contains('\n') || contains('\r')) "" else this
    private fun String.safeKey(): String = replace("[\\r\\n]+".toRegex(), "_").trim().ifBlank { "rules" }

    private data class GeneratedGroups(val groups: List<PolicyGroup>, val filters: Map<String, Pair<String?, String?>>)

    private fun generateRegionGroups(
        options: ConfigExportOptions,
        embeddedNodes: List<ProxyNode>,
        embeddedNames: List<String>,
        providerNames: List<String>,
        providerSnapshotNodes: List<ProxyNode>,
        includedRemoteNodes: List<ProxyNode>,
        regions: Map<String, String>,
    ): GeneratedGroups {
        if (options.enabledRegions.isEmpty()) return GeneratedGroups(emptyList(), emptyMap())
        val groups = mutableListOf<PolicyGroup>()
        val filters = linkedMapOf<String, Pair<String?, String?>>()
        val localRegionById = embeddedNodes.associate { node -> node.id to regionFor(node, options) }
        val remoteById = includedRemoteNodes.associateBy { it.id }
        val providerExcluded = providerSnapshotNodes.filter { it.id !in remoteById.keys }.map { it.name }

        for (region in NodeAutoGroups.regions.filter { it.key in options.enabledRegions }) {
            val name = "🌏 ${region.title}"
            val members = embeddedNodes.indices.filter { localRegionById[embeddedNodes[it].id] == region.key }.map { embeddedNames[it] }
            groups += PolicyGroup(name, "select", members.ifEmpty { listOf("DIRECT") })
            if (providerNames.isNotEmpty()) {
                val extraIncludes = includedRemoteNodes.filter { regions[it.id] == region.key && NodeAutoGroups.classify(it.name) != region.key }.map { exactNamePattern(it.name) }
                val movedOut = includedRemoteNodes.filter { regions[it.id] != region.key && NodeAutoGroups.classify(it.name) == region.key }.map { exactNamePattern(it.name) }
                filters[name] = combinePatterns(listOf(region.filter) + extraIncludes) to combinePatterns(providerExcluded.map(::exactNamePattern) + movedOut)
            }
        }

        if (NodeAutoGroups.OTHER in options.enabledRegions) {
            val name = "🌏 其他"
            val members = embeddedNodes.indices.filter { localRegionById[embeddedNodes[it].id] == NodeAutoGroups.OTHER }.map { embeddedNames[it] }
            groups += PolicyGroup(name, "select", members.ifEmpty { listOf("DIRECT") })
            if (providerNames.isNotEmpty()) {
                val names = includedRemoteNodes.filter { regions[it.id] == NodeAutoGroups.OTHER }.map { exactNamePattern(it.name) }
                filters[name] = (combinePatterns(names) ?: "$^") to combinePatterns(providerExcluded.map(::exactNamePattern))
            }
        }
        return GeneratedGroups(groups, filters)
    }

    private fun regionFor(node: ProxyNode, options: ConfigExportOptions): String =
        options.regionOverrides[node.id]?.takeIf { it in NodeAutoGroups.allKeys } ?: NodeAutoGroups.classify(node.name)

    private fun combinePatterns(patterns: List<String>): String? = patterns.filter(String::isNotBlank).distinct().takeIf { it.isNotEmpty() }?.joinToString("|", "(", ")")

    private fun exactNamePattern(name: String): String = "(?i)^${Regex.escape(name)}$"

    private fun proxyMap(node: ProxyNode, name: String): LinkedHashMap<String, Any?> = LinkedHashMap<String, Any?>().apply {
        node.options.forEach { (key, value) -> if (value != null && key !in setOf("name", "server", "port")) put(key, value) }
        put("type", node.type.lowercase())
        put("name", name)
        put("server", node.server)
        put("port", node.port)
    }

    private fun uniqueNames(nodes: List<ProxyNode>): List<String> {
        val seen = mutableSetOf<String>()
        return nodes.mapIndexed { index, node ->
            val base = node.name.safeName().ifBlank { "${node.type}-${index + 1}" }
            var name = base
            var suffix = 2
            while (!seen.add(name)) name = "$base ($suffix)".also { suffix++ }
            name
        }
    }

    private fun String.safeName(): String = replace("[\\r\\n]+".toRegex(), " ").trim().ifBlank { "Node" }

    companion object {
        val supportedRuleTypes = setOf(
            "DOMAIN", "DOMAIN-SUFFIX", "DOMAIN-KEYWORD", "DOMAIN-WILDCARD", "DOMAIN-REGEX", "GEOSITE",
            "IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX",
            "DST-PORT", "SRC-PORT", "IN-PORT", "IN-TYPE", "IN-USER", "IN-NAME", "REMATCH-NAME",
            "PROCESS-PATH", "PROCESS-PATH-WILDCARD", "PROCESS-PATH-REGEX", "PROCESS-NAME", "PROCESS-NAME-WILDCARD", "PROCESS-NAME-REGEX",
            "UID", "NETWORK", "DSCP", "RULE-SET", "AND", "OR", "NOT", "SUB-RULE", "MATCH",
        )
        private val conditionTypes = supportedRuleTypes - setOf("RULE-SET", "AND", "OR", "NOT", "SUB-RULE", "MATCH")
        private val ipRuleTypes = setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP", "SRC-GEOIP", "SRC-IP-ASN", "SRC-IP-CIDR", "SRC-IP-SUFFIX")
        private val targetIpRuleTypes = setOf("IP-CIDR", "IP-CIDR6", "IP-SUFFIX", "IP-ASN", "GEOIP")
    }
}
