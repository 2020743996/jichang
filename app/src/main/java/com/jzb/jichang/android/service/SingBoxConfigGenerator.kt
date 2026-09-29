package com.jzb.jichang.android.service

import com.google.gson.GsonBuilder
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RoutingRule
import java.io.File

data class SingBoxExportIssue(val location: String, val message: String, val kind: String = "config")
data class GeneratedSingBoxConfig(val json: String?, val exportedNodes: Int, val issues: List<SingBoxExportIssue>) {
    val ready: Boolean get() = json != null && issues.isEmpty()
}

/** Exports the app's editable model to the current stable sing-box schema. Never drops active rules or options silently. */
class SingBoxConfigGenerator(private val refresher: RuleProviderRefresher = RuleProviderRefresher()) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val nodeTypes = setOf("ss", "vmess", "vless", "trojan", "hysteria2", "tuic", "anytls", "snell", "http", "socks", "socks5")

    fun generate(state: AppState, profile: ConfigProfile, cacheRoot: File): GeneratedSingBoxConfig {
        val issues = mutableListOf<SingBoxExportIssue>()
        val ruleProfile = profile.ruleProfile
        if (!profile.advancedYaml.isNullOrBlank()) issues += SingBoxExportIssue("高级 YAML", "Mihomo 高级字段无法转换，请在独立配置中导出")
        if (profile.mihomoSettings.isNotEmpty()) issues += SingBoxExportIssue("基础配置", "Mihomo 专用设置无法等价转换")
        profile.templateId?.let { templateId ->
            val template = state.templates.firstOrNull { it.id == templateId }
            if (template == null) issues += SingBoxExportIssue("模板", "绑定的模板不存在")
            else {
                val parsed = runCatching { MihomoTemplateParser().parse(template.rawYaml) }.getOrNull()
                if (parsed == null) issues += SingBoxExportIssue("模板", "模板无法解析")
                else {
                    val extra = parsed.rawRoot.keys - setOf("proxies", "proxy-groups", "rules", "rule-providers", "sub-rules", "proxy-providers")
                    if (extra.isNotEmpty()) issues += SingBoxExportIssue("模板", "模板包含无法迁移的 Mihomo 字段：${extra.sorted().joinToString()}")
                    if (parsed.rawRoot["proxy-providers"] != null) issues += SingBoxExportIssue("模板订阅", "模板代理提供者无法完整转换；请改用资源中的已刷新节点")
                }
            }
        }
        val sourceIds = profile.selectedSourceIds
        val selected = state.nodes.filter { it.id in profile.enabledNodeIds && (it.sourceId == null || it.sourceId in sourceIds) }
        if (selected.isEmpty()) issues += SingBoxExportIssue("节点", "当前配置没有已启用节点", "node")
        val names = mutableSetOf("DIRECT", "REJECT")
        val tags = linkedMapOf<String, String>()
        val outbounds = mutableListOf<Map<String, Any?>>()
        selected.forEach { node ->
            val base = node.name.trim().ifBlank { "${node.type}-${node.port}" }
            var tag = base
            var suffix = 2
            while (!names.add(tag)) tag = "$base ($suffix)".also { suffix++ }
            tags[node.id] = tag
            convertNode(node, tag, issues)?.let(outbounds::add)
        }

        val autoGroups = buildList {
            for (region in NodeAutoGroups.regions.filter { it.key in profile.enabledRegions }) {
                val members = selected.filter { (profile.regionOverrides[it.id] ?: NodeAutoGroups.classify(it.name)) == region.key }.mapNotNull { tags[it.id] }
                add(PolicyGroup("🌏 ${region.title}", "select", members.ifEmpty { listOf("DIRECT") }, membersExplicit = true))
            }
            if (NodeAutoGroups.OTHER in profile.enabledRegions) {
                val members = selected.filter { (profile.regionOverrides[it.id] ?: NodeAutoGroups.classify(it.name)) == NodeAutoGroups.OTHER }.mapNotNull { tags[it.id] }
                add(PolicyGroup("🌏 其他", "select", members.ifEmpty { listOf("DIRECT") }, membersExplicit = true))
            }
        }
        val groups = (ruleProfile.groups.ifEmpty { listOf(PolicyGroup("PROXY")) } + autoGroups).distinctBy { it.name }
        val groupNames = groups.map { it.name }.toSet()
        if (groupNames.any { it in setOf("DIRECT", "REJECT") || it in tags.values }) issues += SingBoxExportIssue("策略组", "策略组与节点或保留名称重复", "group")
        groups.forEach { group ->
            val members = when {
                group.name == "PROXY" && group.members.isEmpty() && !group.membersExplicit && autoGroups.isNotEmpty() -> autoGroups.map { it.name }
                group.members.isEmpty() && !group.membersExplicit -> tags.values.toList()
                else -> group.members.mapNotNull { member ->
                    if (member.startsWith("node:")) tags[member.removePrefix("node:")].also {
                        if (it == null) issues += SingBoxExportIssue("策略组“${group.name}”", "引用的节点未启用或不存在", "group")
                    } else member
                }
            }
            val invalid = members.filter { it !in tags.values && it !in groupNames && it !in setOf("DIRECT", "REJECT") }
            if (invalid.isNotEmpty()) issues += SingBoxExportIssue("策略组“${group.name}”", "成员不存在：${invalid.joinToString()}", "group")
            if (members.isEmpty()) issues += SingBoxExportIssue("策略组“${group.name}”", "策略组没有成员", "group")
            val allowedExtra = if (group.type == "url-test") setOf("url", "interval", "tolerance") else emptySet()
            val unsupported = group.extra.keys - allowedExtra
            if (unsupported.isNotEmpty()) issues += SingBoxExportIssue("策略组“${group.name}”", "无法转换参数：${unsupported.sorted().joinToString()}", "group")
            val type = when (group.type) { "select" -> "selector"; "url-test" -> "urltest"; else -> null }
            if (type == null) issues += SingBoxExportIssue("策略组“${group.name}”", "不支持 ${group.type} 类型", "group")
            else {
                val result = linkedMapOf<String, Any?>("type" to type, "tag" to group.name, "outbounds" to members)
                if (type == "selector" && members.isNotEmpty()) result["default"] = members.first()
                if (type == "urltest") {
                    group.extra["url"]?.let { result["url"] = it.toString() }
                    group.extra["interval"]?.let { value ->
                        val seconds = value.toString().toIntOrNull()?.takeIf { it > 0 }
                        if (seconds == null) issues += SingBoxExportIssue("策略组“${group.name}”", "测试间隔须为正整数秒", "group")
                        else result["interval"] = "${seconds}s"
                    }
                    group.extra["tolerance"]?.let { value ->
                        val tolerance = value.toString().toIntOrNull()?.takeIf { it >= 0 }
                        if (tolerance == null) issues += SingBoxExportIssue("策略组“${group.name}”", "容差须为非负整数毫秒", "group")
                        else result["tolerance"] = tolerance
                    }
                }
                outbounds += result
            }
        }
        outbounds += mapOf("type" to "direct", "tag" to "DIRECT")
        outbounds += mapOf("type" to "block", "tag" to "REJECT")

        val ruleSetTags = mutableSetOf<String>()
        val ruleSets = ruleProfile.providers.mapNotNull { provider ->
            val location = "规则集“${provider.name}”"
            if (provider.name.isBlank() || !ruleSetTags.add(provider.name)) {
                issues += SingBoxExportIssue(location, "名称为空或重复", "provider"); return@mapNotNull null
            }
            if (provider.format.equals("mrs", true)) {
                issues += SingBoxExportIssue(location, "MRS 无法读取，请改用 YAML 或文本格式", "provider"); return@mapNotNull null
            }
            if (provider.format.lowercase() !in setOf("yaml", "text")) {
                issues += SingBoxExportIssue(location, "不支持 ${provider.format} 格式", "provider"); return@mapNotNull null
            }
            if (provider.extra.isNotEmpty()) issues += SingBoxExportIssue(location, "无法转换高级参数：${provider.extra.keys.joinToString()}", "provider")
            val status = state.ruleProviderStatuses.firstOrNull { it.profileId == profile.id && it.providerId == provider.id }
            val entries = runCatching { refresher.simulationEntries(provider, File(cacheRoot, profile.id), status?.cacheFileName) }
                .onFailure { issues += SingBoxExportIssue(location, it.message ?: "读取本地规则集失败", "provider") }.getOrNull()
            if (entries == null) { issues += SingBoxExportIssue(location, "没有可读取的本地缓存，请先刷新", "provider"); return@mapNotNull null }
            if (entries.isEmpty()) { issues += SingBoxExportIssue(location, "规则集为空", "provider"); return@mapNotNull null }
            val rules = entries.mapIndexedNotNull { index, entry ->
                val clean = entry.trim()
                val type = when (provider.behavior.lowercase()) {
                    "domain" -> if (clean.startsWith("+.") || clean.startsWith(".") || clean.startsWith("*.")) "DOMAIN-SUFFIX" else "DOMAIN"
                    "ipcidr" -> "IP-CIDR"
                    "classical" -> clean.substringBefore(',').trim().uppercase()
                    else -> ""
                }
                val value = if (provider.behavior.equals("classical", true)) clean.substringAfter(',', "").substringBefore(',').trim() else clean.removePrefix("+.").removePrefix("*.").let { if (clean.startsWith("*.")) ".$it" else it }
                val hasExtra = provider.behavior.equals("classical", true) && clean.count { it == ',' } > 1
                val converted = if (hasExtra) null else convertMatcher(type, value)
                if (converted == null) issues += SingBoxExportIssue("$location 第 ${index + 1} 项", "无法转换：$entry", "provider")
                converted
            }
            mapOf("type" to "inline", "tag" to provider.name, "rules" to rules)
        }

        if (ruleProfile.subRules.isNotEmpty()) issues += SingBoxExportIssue("子规则", "子规则无法等价转换，请先改为普通规则", "rule")
        val routeRules = mutableListOf<Map<String, Any?>>()
        val availableTags = groupNames + tags.values + setOf("DIRECT", "REJECT")
        ruleProfile.rules.forEachIndexed { index, rule ->
            val location = "第 ${index + 1} 条规则"
            if (rule.group !in availableTags) { issues += SingBoxExportIssue(location, "目标策略组“${rule.group}”不存在", "rule"); return@forEachIndexed }
            if (rule.extraParameters.isNotEmpty() || rule.rawLine != null || rule.noResolve || rule.source) {
                issues += SingBoxExportIssue(location, "包含无法等价转换的高级参数或原始规则", "rule"); return@forEachIndexed
            }
            if (rule.type.equals("MATCH", true)) {
                if (index != ruleProfile.rules.lastIndex) issues += SingBoxExportIssue(location, "MATCH 必须位于规则列表末尾", "rule")
                return@forEachIndexed
            }
            val matcher = if (rule.type.equals("RULE-SET", true)) {
                if (rule.value !in ruleSetTags) null else mapOf("rule_set" to listOf(rule.value))
            } else if (rule.type.uppercase() in setOf("AND", "OR", "NOT")) convertLogical(rule.type, rule.conditions)
            else convertMatcher(rule.type.uppercase(), rule.value)
            if (matcher == null) issues += SingBoxExportIssue(location, "${rule.type} 无法等价转换", "rule")
            else routeRules += matcher + mapOf("action" to "route", "outbound" to rule.group)
        }
        val final = ruleProfile.rules.lastOrNull { it.type.equals("MATCH", true) }?.group ?: (if ("PROXY" in groupNames) "PROXY" else groups.firstOrNull()?.name ?: "DIRECT")
        if (final !in availableTags) issues += SingBoxExportIssue("兜底规则", "目标策略组不存在", "rule")
        if (ruleProfile.rules.count { it.type.equals("MATCH", true) } > 1) issues += SingBoxExportIssue("兜底规则", "存在多条 MATCH 规则", "rule")
        if (issues.isNotEmpty()) return GeneratedSingBoxConfig(null, selected.size, issues.distinct())

        // Android SFA owns the VPN service. A TUN inbound asks it to route device traffic here.
        val root = linkedMapOf<String, Any?>(
            "log" to mapOf("level" to "info"),
            "dns" to mapOf("servers" to listOf(mapOf("type" to "local", "tag" to "local")), "final" to "local"),
            "inbounds" to listOf(mapOf("type" to "tun", "tag" to "tun-in", "address" to listOf("172.19.0.1/30"), "auto_route" to true)),
            "outbounds" to outbounds,
            "route" to mapOf("rules" to routeRules, "rule_set" to ruleSets, "final" to final),
        )
        return GeneratedSingBoxConfig(gson.toJson(root), selected.size, emptyList())
    }

    private fun convertMatcher(type: String, value: String): Map<String, Any?>? {
        if (value.isBlank()) return null
        val key = when (type) {
            "DOMAIN" -> "domain"; "DOMAIN-SUFFIX" -> "domain_suffix"; "DOMAIN-KEYWORD" -> "domain_keyword"
            "DOMAIN-REGEX" -> "domain_regex"; "IP-CIDR", "IP-CIDR6" -> "ip_cidr"
            "SRC-IP-CIDR" -> "source_ip_cidr"; "DST-PORT" -> "port"; "SRC-PORT" -> "source_port"; "NETWORK" -> "network"
            else -> return null
        }
        val parsed: Any = when (key) {
            "port", "source_port" -> value.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            "network" -> value.lowercase().takeIf { it in setOf("tcp", "udp") } ?: return null
            "domain_suffix" -> value.removePrefix("+.")
            else -> value
        }
        return mapOf(key to listOf(parsed))
    }

    private fun convertLogical(type: String, conditions: List<RuleCondition>): Map<String, Any?>? {
        val upper = type.uppercase()
        if (conditions.size < if (upper == "NOT") 1 else 2) return null
        val rules = conditions.map { condition ->
            if (condition.noResolve || condition.source || !condition.argument.isNullOrBlank()) return null
            if (condition.operator != null) {
                if (condition.type != null || condition.value.isNotBlank()) return null
                convertLogical(condition.operator, condition.children) ?: return null
            } else {
                if (condition.children.isNotEmpty()) return null
                convertMatcher(condition.type?.uppercase().orEmpty(), condition.value) ?: return null
            }
        }
        return if (upper == "NOT") {
            if (rules.size != 1) null else rules.single() + mapOf("invert" to true)
        } else if (upper in setOf("AND", "OR")) mapOf("type" to "logical", "mode" to upper.lowercase(), "rules" to rules)
        else null
    }

    private fun convertNode(node: ProxyNode, tag: String, issues: MutableList<SingBoxExportIssue>): Map<String, Any?>? {
        val type = node.type.lowercase()
        val location = "节点“${node.name}”"
        if (type !in nodeTypes) { issues += SingBoxExportIssue(location, "不支持 ${node.type} 协议", "node"); return null }
        val options = node.options.toMutableMap().apply { keys.removeAll(setOf("type", "name", "server", "port")) }
        fun take(key: String): Any? = options.remove(key)
        fun string(key: String): String? = take(key)?.toString()?.takeIf { it.isNotBlank() }
        fun required(key: String): String? = string(key).also { if (it == null) issues += SingBoxExportIssue(location, "缺少 $key", "node") }
        val result = linkedMapOf<String, Any?>("type" to when (type) { "ss" -> "shadowsocks"; "socks5" -> "socks"; else -> type }, "tag" to tag, "server" to node.server, "server_port" to node.port)
        when (type) {
            "ss" -> { result["method"] = required("cipher"); result["password"] = required("password") }
            "vmess" -> { result["uuid"] = required("uuid"); result["security"] = string("cipher") ?: "auto"; (take("alterId") ?: take("alter-id"))?.let { result["alter_id"] = (it as? Number)?.toInt() ?: it.toString().toIntOrNull() } }
            "vless" -> { result["uuid"] = required("uuid"); string("flow")?.let { result["flow"] = it } }
            "trojan", "hysteria2", "anytls" -> result["password"] = required("password")
            "tuic" -> { result["uuid"] = required("uuid"); result["password"] = required("password"); string("congestion-controller")?.let { result["congestion_control"] = it }; string("udp-relay-mode")?.let { result["udp_relay_mode"] = it } }
            "snell" -> { result["psk"] = required("psk"); take("version")?.let {
                result["version"] = it.toString().toIntOrNull() ?: run { issues += SingBoxExportIssue(location, "Snell 版本无效", "node"); null }
            } }
            "http", "socks", "socks5" -> { string("username")?.let { result["username"] = it }; string("password")?.let { result["password"] = it } }
        }
        val udp = take("udp")
        if (udp == false) issues += SingBoxExportIssue(location, "节点显式关闭 UDP，当前无法保证等价转换", "node")
        val tlsFlag = take("tls")
        val serverName = string("servername") ?: string("sni")
        val alpn = take("alpn")
        if (alpn != null && alpn !is List<*>) issues += SingBoxExportIssue(location, "ALPN 格式无法转换", "node")
        val insecure = take("skip-cert-verify")
        if (insecure != null && insecure !is Boolean) issues += SingBoxExportIssue(location, "证书校验开关无效", "node")
        val fingerprint = string("client-fingerprint")
        val realityValue = take("reality-opts")
        val reality = realityValue as? Map<*, *>
        if (realityValue != null && reality == null) issues += SingBoxExportIssue(location, "Reality 参数无法转换", "node")
        if (reality != null && reality.keys.any { it !in setOf("public-key", "short-id") }) issues += SingBoxExportIssue(location, "Reality 包含不支持的字段", "node")
        if (tlsFlag == false && (type in setOf("trojan", "hysteria2", "tuic", "anytls") || serverName != null || reality != null)) issues += SingBoxExportIssue(location, "TLS 已关闭，但协议或 TLS 参数要求启用", "node")
        if (tlsFlag == true || type in setOf("trojan", "hysteria2", "tuic", "anytls") || serverName != null || reality != null) {
            result["tls"] = linkedMapOf<String, Any?>("enabled" to true).apply {
                serverName?.let { put("server_name", it) }
                if (alpn is List<*>) put("alpn", alpn.mapNotNull { it?.toString() })
                if (insecure == true) put("insecure", true)
                fingerprint?.let { put("utls", mapOf("enabled" to true, "fingerprint" to it)) }
                reality?.let { put("reality", mapOf("enabled" to true, "public_key" to it["public-key"], "short_id" to it["short-id"])) }
            }
        } else if (tlsFlag != null && tlsFlag != false) issues += SingBoxExportIssue(location, "TLS 设置无法识别", "node")
        val network = string("network")
        if (network in setOf("ws", "grpc")) {
            val transportOptions = take("${network}-opts") as? Map<*, *>
            result["transport"] = when (network) {
                "ws" -> mapOf("type" to "ws", "path" to (transportOptions?.get("path") ?: "/"), "headers" to (transportOptions?.get("headers") ?: emptyMap<String, String>()))
                else -> mapOf("type" to "grpc", "service_name" to (transportOptions?.get("grpc-service-name") ?: ""))
            }
            val unsupported = transportOptions?.keys?.map { it.toString() }?.toSet().orEmpty() - if (network == "ws") setOf("path", "headers") else setOf("grpc-service-name")
            if (unsupported.isNotEmpty()) issues += SingBoxExportIssue(location, "传输参数无法转换：${unsupported.joinToString()}", "node")
        } else if (network != null && network !in setOf("tcp", "udp")) issues += SingBoxExportIssue(location, "传输类型 $network 无法转换", "node")
        else if (network != null) result["network"] = network
        if (type == "hysteria2") {
            val obfs = string("obfs")
            val obfsPassword = string("obfs-password")
            if (obfs != null) {
                if (obfs == "salamander" && obfsPassword != null) result["obfs"] = mapOf("type" to "salamander", "password" to obfsPassword)
                else issues += SingBoxExportIssue(location, "Hysteria2 混淆类型或密码不完整", "node")
            } else if (obfsPassword != null) issues += SingBoxExportIssue(location, "Hysteria2 混淆密码缺少类型", "node")
        }
        val remaining = options.filterValues { it != null }
        if (remaining.isNotEmpty()) issues += SingBoxExportIssue(location, "无法转换参数：${remaining.keys.sorted().joinToString()}", "node")
        return result
    }
}
