package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ProxyNode
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.util.LinkedHashMap

data class GeneratedConfig(val yaml: String, val exportedNodes: Int, val skippedNodes: Int)

/** Builds one Mihomo profile; it intentionally has no per-client dialect matrix. */
class MihomoConfigGenerator {
    private val supportedTypes = setOf(
        "ss", "vmess", "vless", "trojan", "hysteria", "hysteria2", "tuic", "wireguard",
        "anytls", "snell", "socks5", "http", "ssh", "socks"
    )

    fun generate(state: AppState): GeneratedConfig {
        val selected = state.nodes.filter { it.enabled }
        val exportable = selected.filter { it.type.lowercase() in supportedTypes }
        val skipped = selected.size - exportable.size
        val names = uniqueNames(exportable)
        val proxies = exportable.mapIndexed { index, node ->
            val result = LinkedHashMap<String, Any?>()
            node.options.forEach { (key, value) -> if (value != null) result[key] = value }
            result["type"] = node.type.lowercase()
            result["name"] = names[index]
            result["server"] = node.server
            result["port"] = node.port
            result
        }

        val nodeNames = names.toSet()
        val nodeNamesByID = exportable.mapIndexed { index, node -> "node:${node.id}" to names[index] }.toMap()
        val configuredGroups = state.ruleProfile.groups.filter { it.name.isNotBlank() }
        val groups = if (configuredGroups.isEmpty()) listOf(com.jzb.jichang.android.model.PolicyGroup("PROXY")) else configuredGroups
        val groupNames = groups.map { it.name }.toSet()
        val groupYaml = groups.map { group ->
            val configuredMembers = if (group.members.isEmpty()) names else group.members.map { member ->
                nodeNamesByID[member] ?: member
            }
            val candidates = configuredMembers.filter { it in nodeNames || it in groupNames || it == "DIRECT" || it == "REJECT" }
            linkedMapOf<String, Any?>(
                "name" to group.name.safeName(),
                "type" to (group.type.takeIf { it in setOf("select", "url-test", "fallback", "load-balance") } ?: "select"),
                "proxies" to if (candidates.isEmpty()) listOf("DIRECT") else candidates,
            ).apply {
                if (group.type in setOf("url-test", "fallback", "load-balance")) {
                    put("url", "https://www.gstatic.com/generate_204")
                    put("interval", 300)
                }
            }
        }.toMutableList()
        if (groupNames.isEmpty()) {
            groupYaml += linkedMapOf("name" to "PROXY", "type" to "select", "proxies" to if (names.isEmpty()) listOf("DIRECT") else names)
        }

        val availableGroup = (groups.firstOrNull()?.name ?: "PROXY").takeIf { it in groupNames } ?: "PROXY"
        val ruleLines = state.ruleProfile.rules.mapNotNull { rule ->
            val type = rule.type.uppercase().trim()
            val value = rule.value.trim()
            val target = rule.group.trim().takeIf { it in groupNames || it == "DIRECT" || it == "REJECT" } ?: return@mapNotNull null
            if (type.isBlank() || type.contains(',') || value.contains('\n') || value.contains('\r') || value.contains(',')) return@mapNotNull null
            "$type,$value,$target"
        }.toMutableList()
        if (ruleLines.none { it.startsWith("MATCH,") || it.startsWith("FINAL,") }) ruleLines += "MATCH,$availableGroup"

        val root = linkedMapOf<String, Any?>(
            "mixed-port" to 7890,
            "allow-lan" to false,
            "mode" to "rule",
            "log-level" to "info",
            "ipv6" to true,
            "proxies" to proxies,
            "proxy-groups" to groupYaml,
            "rules" to ruleLines,
        )
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            width = 120
        }
        return GeneratedConfig(Yaml(options).dump(root), exportable.size, skipped)
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
}
