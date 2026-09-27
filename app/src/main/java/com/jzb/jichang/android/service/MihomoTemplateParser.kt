package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.SubRuleProfile
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.util.UUID
import java.net.URI

data class ParsedMihomoTemplate(
    val rawRoot: Map<String, Any?>,
    val nodes: List<ProxyNode>,
    val ruleProfile: RuleProfile,
    val subscriptionParameters: List<TemplateSubscriptionParameter>,
    val hasRegionalProxyGroups: Boolean,
)

data class TemplateSubscriptionParameter(val providerName: String)

/** Reads templates with SnakeYAML's safe constructor and keeps the original document for export. */
class MihomoTemplateParser {
    fun parse(rawYaml: String): ParsedMihomoTemplate {
        require(rawYaml.isNotBlank()) { "模板内容为空" }
        val loaderOptions = LoaderOptions().apply {
            maxAliasesForCollections = 30
            codePointLimit = 8 * 1024 * 1024
        }
        val loaded = runCatching { Yaml(SafeConstructor(loaderOptions)).load<Any?>(rawYaml.trimStart('\uFEFF')) }
            .getOrElse { throw IllegalArgumentException("YAML 模板无法解析：${it.message ?: "格式错误"}") }
        val root = loaded.asStringMap() ?: error("模板根节点必须是 YAML 配置对象")
        require(root.keys.any { it in MIHOMO_KEYS }) { "没有识别到 Mihomo 配置字段" }

        val nodes = (root["proxies"] as? List<*>)?.mapNotNull { raw ->
            val map = raw.asStringMap() ?: return@mapNotNull null
            val type = map["type"]?.toString()?.lowercase()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val server = map["server"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val port = map["port"].asInt()?.takeIf { it in 1..65535 } ?: return@mapNotNull null
            val name = map["name"]?.toString()?.takeIf(String::isNotBlank) ?: "$type $server:$port"
            ProxyNode(UUID.randomUUID().toString(), null, name, type, server, port, options = map)
        }.orEmpty()
        val nodeIdsByName = nodes.associate { it.name to it.id }
        val groups = (root["proxy-groups"] as? List<*>)?.mapNotNull { raw ->
            val map = raw.asStringMap() ?: return@mapNotNull null
            val name = map["name"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val type = map["type"]?.toString()?.takeIf(String::isNotBlank) ?: "select"
            val members = (map["proxies"] as? List<*>)?.mapNotNull { member ->
                val value = member?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                nodeIdsByName[value]?.let { "node:$it" } ?: value
            }.orEmpty()
            PolicyGroup(name, type, members, map - setOf("name", "type", "proxies"))
        }.orEmpty()
        val ruleProviders = root["rule-providers"].asStringMap()?.mapNotNull { (name, raw) ->
            val map = raw.asStringMap() ?: return@mapNotNull null
            RuleProvider(
                id = UUID.randomUUID().toString(), name = name,
                type = map["type"]?.toString() ?: "http",
                url = map["url"]?.toString().orEmpty(),
                path = map["path"]?.toString().orEmpty(),
                interval = map["interval"].asInt() ?: 86400,
                behavior = map["behavior"]?.toString() ?: "domain",
                format = map["format"]?.toString() ?: "yaml",
                payload = (map["payload"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
                headers = map["header"].asStringMap()?.mapValues { (_, v) -> (v as? List<*>)?.mapNotNull { it?.toString() }.orEmpty() }.orEmpty(),
                extra = map - setOf("type", "behavior", "format", "url", "path", "interval", "payload", "header"),
            )
        }.orEmpty()
        val rules = (root["rules"] as? List<*>)?.mapNotNull { raw -> parseRule(raw?.toString().orEmpty()) }.orEmpty()
        val subRules = root["sub-rules"].asStringMap()?.map { (name, raw) ->
            SubRuleProfile(name, (raw as? List<*>)?.mapNotNull { parseRule(it?.toString().orEmpty()) }.orEmpty())
        }.orEmpty()
        val ruleProfile = RuleProfile(
            groups = groups.ifEmpty { listOf(PolicyGroup("PROXY")) },
            rules = rules,
            providers = ruleProviders,
            subRules = subRules,
        )
        val providers = root["proxy-providers"].asStringMap().orEmpty()
        val subscriptionParameters = providers.mapNotNull { (name, raw) ->
            val provider = raw.asStringMap() ?: return@mapNotNull null
            if (!provider["type"].toString().equals("http", ignoreCase = true)) return@mapNotNull null
            val url = provider["url"]?.toString().orEmpty().trim()
            if (isSubscriptionPlaceholder(url)) TemplateSubscriptionParameter(name) else null
        }
        val hasRegionalProxyGroups = groups.any { group ->
            val hasProviderFilter = group.extra.keys.any { it == "filter" || it == "exclude-filter" }
            hasProviderFilter && NodeAutoGroups.isRegionalGroup(group.name)
        }
        return ParsedMihomoTemplate(root, nodes, ruleProfile, subscriptionParameters, hasRegionalProxyGroups)
    }

    private fun isSubscriptionPlaceholder(value: String): Boolean {
        if (value.isBlank()) return true
        val marker = Regex("(?i)(\\{\\{|\\$\\{?|订阅.{0,6}(地址|链接|url)|机场.{0,6}(地址|链接|url)|your[_ -]?(subscription|url)|placeholder|replace[_ -]?me|example\\.(com|org|net))")
        if (marker.containsMatchIn(value)) return true
        val uri = runCatching { URI(value) }.getOrNull() ?: return true
        return uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()
    }

    private fun parseRule(line: String): RoutingRule? {
        val parts = line.split(',')
        if (parts.size < 2) return null
        val type = parts[0].trim().uppercase()
        val target = when {
            type == "MATCH" -> parts.getOrNull(1)
            type in setOf("AND", "OR", "NOT") -> parts.lastOrNull()
            else -> parts.getOrNull(2)
        }?.trim()?.takeIf(String::isNotBlank) ?: "DIRECT"
        val value = if (type == "MATCH") "MATCH" else parts[1].trim()
        if (value.isBlank()) return null
        val extras = parts.drop(3).map(String::trim).filter(String::isNotBlank)
        return RoutingRule(
            type = type,
            value = value,
            group = target,
            noResolve = "no-resolve" in extras,
            source = "src" in extras,
            extraParameters = extras.filterNot { it == "no-resolve" || it == "src" },
            rawLine = line.trim().takeIf { type in setOf("AND", "OR", "NOT") },
        )
    }

    private fun Any?.asStringMap(): Map<String, Any?>? {
        val raw = this as? Map<*, *> ?: return null
        return raw.entries.associate { (key, value) -> key.toString() to value }
    }

    private fun Any?.asInt(): Int? = when (this) {
        is Number -> toInt()
        else -> toString().toIntOrNull()
    }

    companion object {
        private val MIHOMO_KEYS = setOf("proxies", "proxy-providers", "proxy-groups", "rule-providers", "rules", "dns", "tun", "mode", "port", "mixed-port", "redir-port", "tproxy-port", "listeners", "hosts", "profile", "sniffer", "geodata-mode")
    }
}
