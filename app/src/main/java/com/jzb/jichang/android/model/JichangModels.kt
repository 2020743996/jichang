package com.jzb.jichang.android.model

data class SubscriptionSource(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
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
    val enabled: Boolean = true,
    val options: Map<String, Any?> = emptyMap(),
)

data class PolicyGroup(
    val name: String,
    val type: String = "select",
    val members: List<String> = emptyList(),
)

data class RoutingRule(
    val type: String,
    val value: String,
    val group: String,
)

data class RuleProfile(
    val groups: List<PolicyGroup> = listOf(PolicyGroup("PROXY", members = emptyList())),
    val rules: List<RoutingRule> = emptyList(),
)

data class AppState(
    val sources: List<SubscriptionSource> = emptyList(),
    val nodes: List<ProxyNode> = emptyList(),
    val ruleProfile: RuleProfile = RuleProfile(),
)

data class ParseResult(val nodes: List<ProxyNode>, val skippedCount: Int)
