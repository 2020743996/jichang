package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProvider
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
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
    val unresolvedTemplateProviders: List<String> = emptyList(),
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
        val parsedTemplate = profile.templateId?.let { id -> state.templates.firstOrNull { it.id == id } }
            ?.let { runCatching { MihomoTemplateParser().parse(it.rawYaml) }.getOrNull() }
        val templateRoot = parsedTemplate?.rawRoot.orEmpty()
        val placeholderNames = parsedTemplate?.subscriptionParameters?.map { it.providerName }.orEmpty()
        val templateHasRegionalGroups = parsedTemplate?.hasRegionalProxyGroups == true
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
        val boundTemplateSources = placeholderNames.mapNotNull { name ->
            val explicit = profile.templateProviderBindings[name]?.let { id -> providerSources.firstOrNull { it.id == id } }
            val source = explicit ?: providerSources.singleOrNull()
            source?.let { name to it }
        }
        val unresolvedTemplateProviders = if (exportOptions.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS && providerSources.size > 1) {
            placeholderNames.filter { name -> boundTemplateSources.none { it.first == name } }
        } else emptyList()
        val templateProviderNames = boundTemplateSources.associate { (name, source) -> source.id to name }
        val genericProviderSources = providerSources.filterNot { it.id in templateProviderNames.keys }
        val providerNames = buildList {
            addAll(boundTemplateSources.map { it.first })
            addAll(genericProviderSources.indices.map { "订阅-${it + 1}" })
        }
        val providerSourceIds = (boundTemplateSources.map { it.second.id } + genericProviderSources.map { it.id }).toSet()
        val embeddedNodes = if (exportOptions.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) {
            supportedSelected.filter { it.sourceId == null || it.sourceId !in providerSourceIds }
        } else supportedSelected
        val names = uniqueNames(embeddedNodes)
        val proxies = embeddedNodes.mapIndexed { index, node -> proxyMap(node, names[index]) }
        val proxyNamesById = embeddedNodes.mapIndexed { index, node -> node.id to names[index] }.toMap()
        val providerSnapshotNodes = state.nodes.filter { it.sourceId in providerSourceIds }
        val selectedNodeIds = exportOptions.enabledNodeIds ?: state.nodes.filter { it.enabled }.map { it.id }.toSet()
        val includedRemoteNodes = providerSnapshotNodes.filter { it.id in selectedNodeIds && it.id !in exportOptions.excludedNodeIds }
        val regions = includedRemoteNodes.associate { node -> node.id to regionFor(node, exportOptions) }
        val generated = generateRegionGroups(
            options = if (templateHasRegionalGroups) exportOptions.copy(enabledRegions = emptySet()) else exportOptions,
            embeddedNodes = embeddedNodes,
            embeddedNames = names,
            providerNames = providerNames,
            providerSnapshotNodes = providerSnapshotNodes,
            includedRemoteNodes = includedRemoteNodes,
            regions = regions,
        )

        val placeholderProviderNames = placeholderNames.toSet()
        val rawProviders = templateRoot["proxy-providers"].asStringMap().orEmpty()
        val outputTemplateProviders = LinkedHashMap<String, Any?>().apply {
            rawProviders.forEach { (name, value) -> if (name !in placeholderProviderNames) put(name, value) }
            if (exportOptions.sourceMode == ConfigSourceMode.REFERENCE_SUBSCRIPTIONS) {
                boundTemplateSources.forEach { (name, source) ->
                    val original = rawProviders[name].asStringMap().orEmpty()
                    put(name, LinkedHashMap<String, Any?>().apply {
                        putAll(original)
                        put("url", source.url)
                    })
                }
                genericProviderSources.forEachIndexed { index, source -> put("订阅-${index + 1}", subscriptionProvider(source, index + 1)) }
            }
        }
        val availableTemplateProviderNames = outputTemplateProviders.keys
        val removedPlaceholderNames = placeholderProviderNames - availableTemplateProviderNames
        val groupsUsingTemplateProviders = profile.ruleProfile.groups.filter { group ->
            (group.extra["use"] as? List<*>)?.any { it?.toString() in placeholderProviderNames } == true
        }.map { it.name }.toSet()

        val ruleProfile = profile.ruleProfile
        val configuredGroups = ruleProfile.groups.filter { it.name.isNotBlank() }.ifEmpty { listOf(PolicyGroup("PROXY")) }.map { group ->
            if (placeholderNames.isEmpty()) group else {
                val use = (group.extra["use"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
                if (use.none { it in placeholderNames }) group else {
                    val remainingUse = use.filterNot { it in removedPlaceholderNames }
                    val extra = LinkedHashMap(group.extra).apply {
                        if (remainingUse.isEmpty()) remove("use") else put("use", remainingUse)
                    }
                    group.copy(extra = extra)
                }
            }
        }
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
            val eligibleNodeNames = configuredMembers.filter { it in names }
                .filter { nodeName ->
                    val filter = group.extra["filter"]?.toString()
                    val excludeFilter = group.extra["exclude-filter"]?.toString()
                    (filter == null || matchesGroupFilter(nodeName, filter, group.name)) &&
                        (excludeFilter == null || !matchesGroupFilter(nodeName, excludeFilter, group.name))
                }
            val candidates = (eligibleNodeNames + configuredMembers.filter { it in groupNames || it == "DIRECT" || it == "REJECT" }).distinct()
            linkedMapOf<String, Any?>(
                "name" to group.name.safeName(),
                "type" to (group.type.takeIf { it in setOf("select", "url-test", "fallback", "load-balance") } ?: "select"),
                "proxies" to candidates.ifEmpty { listOf("DIRECT") },
            ).apply {
                group.extra.forEach { (key, value) ->
                    if (key !in setOf("name", "type", "proxies")) put(key, value)
                }
                if (outputTemplateProviders.isEmpty() ||
                    (group.name in groupsUsingTemplateProviders && group.name in groupNames &&
                        group.extra["use"] == null && removedPlaceholderNames.isNotEmpty())
                ) {
                    remove("include-all-providers")
                    remove("include-all")
                }
                if (group.type in setOf("url-test", "fallback", "load-balance")) {
                    putIfAbsent("url", "https://www.gstatic.com/generate_204")
                    putIfAbsent("interval", 300)
                }
                if (providerNames.isNotEmpty()) {
                    if (isGenerated) {
                        put("use", providerNames)
                        generated.filters[group.name]?.let { filters ->
                            filters.first?.let { put("filter", it) }
                            filters.second?.let { put("exclude-filter", it) }
                        }
                    } else if (!isDefaultMaster && group.extra["use"] == null && group.name !in groupsUsingTemplateProviders) {
                        val remoteMembers = if (group.members.isEmpty()) includedRemoteNodes else group.members.mapNotNull { member ->
                            if (member.startsWith("node:")) includedRemoteNodes.firstOrNull { it.id == member.removePrefix("node:") } else null
                        }
                        if (remoteMembers.isNotEmpty() || group.members.isEmpty()) {
                            put("use", providerNames)
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

        val root = LinkedHashMap<String, Any?>().apply {
            putAll(templateRoot)
            putIfAbsent("mixed-port", 7890)
            putIfAbsent("allow-lan", false)
            putIfAbsent("mode", "rule")
            putIfAbsent("log-level", "info")
            putIfAbsent("ipv6", true)
        }
        root["proxies"] = proxies
        if (outputTemplateProviders.isNotEmpty()) root["proxy-providers"] = outputTemplateProviders else root.remove("proxy-providers")
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
        return GeneratedConfig(Yaml(dumpOptions).dump(root), embeddedNodes.size, skipped, outputTemplateProviders.size, unresolvedTemplateProviders)
    }

    private fun serializeRule(rule: com.jzb.jichang.android.model.RoutingRule, target: String, subRuleNames: Set<String> = emptySet(), ruleProviderNames: Set<String> = emptySet()): String? {
        val type = rule.type.uppercase()
        val value = rule.value.trim()
        if (type in setOf("AND", "OR", "NOT")) {
            val minimum = if (type == "NOT") 1 else 2
            if (rule.conditions.size < minimum) {
                val original = rule.rawLine?.takeIf { it.isNotBlank() && !it.contains('\n') && !it.contains('\r') } ?: return null
                val targetSeparator = original.lastIndexOf(',')
                return if (targetSeparator > 0) original.substring(0, targetSeparator + 1) + target else null
            }
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
            addAll(rule.extraParameters.filter { it.isNotBlank() && !it.contains(',') && !it.contains('\n') && !it.contains('\r') })
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
        provider.extra.forEach { (key, value) ->
            if (key !in setOf("type", "behavior", "format", "url", "path", "interval", "payload", "header")) put(key, value)
        }
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

    private fun Any?.asStringMap(): Map<String, Any?>? {
        val raw = this as? Map<*, *> ?: return null
        return raw.entries.associate { (key, value) -> key.toString() to value }
    }

    private fun subscriptionProvider(source: com.jzb.jichang.android.model.SubscriptionSource, index: Int) = linkedMapOf<String, Any?>(
        "type" to "http", "url" to source.url, "path" to "./providers/jichang-$index.yaml", "interval" to 3600,
        "health-check" to linkedMapOf("enable" to true, "url" to "https://www.gstatic.com/generate_204", "interval" to 600),
    )

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

    private fun matchesGroupFilter(nodeName: String, expression: String, groupName: String): Boolean {
        val regexMatch = runCatching { Regex(expression).containsMatchIn(nodeName) }.getOrNull()
        if (regexMatch != null) return regexMatch
        val expectedRegion = NodeAutoGroups.regionForGroupName(groupName) ?: return false
        return NodeAutoGroups.classify(nodeName) == expectedRegion
    }

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
