package com.jzb.jichang.android.model

data class SubscriptionSource(
    val id: String,
    val name: String,
    val url: String,
    /** Kept for reading version-1 snapshots. Selection is now stored on ConfigProfile. */
    val enabled: Boolean = true,
    val providerCompatible: Boolean? = null,
    val updatedAt: Long? = null,
    val lastError: String? = null,
)

data class ProxyNode(
    val id: String,
    val sourceId: String?,
    val name: String,
    val type: String,
    val server: String,
    val port: Int,
    /** Kept for migration from version-1 snapshots; use ConfigProfile.enabledNodeIds. */
    val enabled: Boolean = true,
    val options: Map<String, Any?> = emptyMap(),
)

data class PolicyGroup(
    val name: String,
    val type: String = "select",
    val members: List<String> = emptyList(),
    /** Keeps Mihomo group options that the visual editor does not expose. */
    val extra: Map<String, Any?> = emptyMap(),
    /** Distinguishes an intentionally empty selection from legacy auto-include-all groups. */
    val membersExplicit: Boolean = false,
)

/** A serializable rule condition. Groups use operator=AND/OR/NOT; leaves use type/value. */
data class RuleCondition(
    val operator: String? = null,
    val type: String? = null,
    val value: String = "",
    val argument: String? = null,
    val noResolve: Boolean = false,
    val source: Boolean = false,
    val children: List<RuleCondition> = emptyList(),
)

data class RoutingRule(
    val type: String,
    val value: String,
    val group: String,
    val noResolve: Boolean = false,
    val source: Boolean = false,
    val conditions: List<RuleCondition> = emptyList(),
    val extraParameters: List<String> = emptyList(),
    /** Original Mihomo line for imported expressions not yet editable in the visual rule form. */
    val rawLine: String? = null,
)

data class RuleProvider(
    val id: String,
    val name: String,
    val type: String = "http",
    val url: String = "",
    val path: String = "",
    val interval: Int = 86400,
    val behavior: String = "domain",
    val format: String = "yaml",
    val payload: List<String> = emptyList(),
    val headers: Map<String, List<String>> = emptyMap(),
    val extra: Map<String, Any?> = emptyMap(),
    /** The downloaded config template this provider was copied from, if any. */
    val sourceTemplateId: String? = null,
    val sourceTemplateName: String? = null,
)

data class SubRuleProfile(
    val name: String,
    val rules: List<RoutingRule> = emptyList(),
)

data class RuleProfile(
    val groups: List<PolicyGroup> = listOf(PolicyGroup("PROXY")),
    val rules: List<RoutingRule> = emptyList(),
    val providers: List<RuleProvider> = emptyList(),
    val subRules: List<SubRuleProfile> = emptyList(),
)

data class ConfigProfile(
    val id: String,
    val name: String,
    val fileName: String = name,
    val selectedSourceIds: Set<String> = emptySet(),
    val enabledNodeIds: Set<String> = emptySet(),
    val ruleProfile: RuleProfile = RuleProfile(),
    val sourceMode: String = "EMBED_NODES",
    val enabledRegions: Set<String> = setOf("hk", "tw", "jp", "sg", "us", "kr", "other"),
    val regionOverrides: Map<String, String> = emptyMap(),
    val templateId: String? = null,
    /** Template proxy-provider name to an existing shared subscription source ID. */
    val templateProviderBindings: Map<String, String> = emptyMap(),
    /** Mihomo fields managed by the visual settings form, isolated per profile. */
    val mihomoSettings: Map<String, Any?> = emptyMap(),
    /** null inherits template-only fields; a value replaces the advanced field set. */
    val advancedYaml: String? = null,
)

data class ConfigTemplate(
    val id: String,
    val name: String,
    val rawYaml: String,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class RuleProviderStatus(
    val profileId: String,
    val providerId: String,
    val refreshedAt: Long? = null,
    val error: String? = null,
    val itemCount: Int? = null,
    val cacheFileName: String? = null,
)

data class AppState(
    val sources: List<SubscriptionSource> = emptyList(),
    val nodes: List<ProxyNode> = emptyList(),
    val profiles: List<ConfigProfile> = listOf(ConfigProfile("default", "默认配置")),
    val activeProfileId: String = "default",
    val templates: List<ConfigTemplate> = emptyList(),
    val ruleProviderStatuses: List<RuleProviderStatus> = emptyList(),
) {
    val activeProfile: ConfigProfile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull() ?: ConfigProfile("default", "默认配置")
    val ruleProfile: RuleProfile get() = activeProfile.ruleProfile
}

data class ParseResult(val nodes: List<ProxyNode>, val skippedCount: Int)
