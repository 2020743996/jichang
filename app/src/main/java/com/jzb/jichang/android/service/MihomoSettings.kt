package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.ConfigProfile
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.util.LinkedHashMap

/** Field ownership shared by the visual editor, advanced YAML editor and config generator. */
object MihomoSettings {
    val applicationManagedKeys = setOf(
        "proxies", "proxy-providers", "proxy-groups", "rules", "rule-providers", "sub-rules",
    )

    val visualManagedKeys = setOf(
        "port", "socks-port", "redir-port", "tproxy-port", "mixed-port", "allow-lan", "bind-address",
        "mode", "log-level", "process-mode", "ipv6", "unified-delay", "tcp-concurrent", "keep-alive-idle", "keep-alive-interval",
        "external-controller", "external-ui", "external-ui-url", "secret", "authentication", "interface-name", "routing-mark",
        "global-client-fingerprint", "dns", "tun", "sniffer", "ntp",
    )

    val allManagedKeys = applicationManagedKeys + visualManagedKeys

    private val partialBlocks = mapOf(
        "dns" to setOf("enable", "listen", "enhanced-mode", "fake-ip-range", "ipv6", "use-hosts", "use-system-hosts", "respect-rules", "default-nameserver", "nameserver", "fallback", "proxy-server-nameserver"),
        "tun" to setOf("enable", "stack", "auto-route", "auto-detect-interface", "strict-route", "dns-hijack", "mtu"),
        "sniffer" to setOf("enable", "force-dns-mapping", "parse-pure-ip", "override-destination"),
        "ntp" to setOf("enable", "server", "port", "interval", "write-to-system"),
    )

    private val defaults: Map<String, Any?> = linkedMapOf(
        "mixed-port" to 7890,
        "allow-lan" to false,
        "mode" to "rule",
        "log-level" to "info",
        "ipv6" to true,
    )

    fun visualFieldsFromRoot(root: Map<String, Any?>): LinkedHashMap<String, Any?> = LinkedHashMap<String, Any?>().apply {
        root.filterKeys { it in visualManagedKeys && it !in partialBlocks.keys }.forEach { (key, value) -> put(key, value) }
        partialBlocks.forEach { (block, fields) ->
            val source = root[block].asStringMap().orEmpty()
            val managed = source.filterKeys { it in fields }
            if (managed.isNotEmpty()) put(block, managed)
        }
    }

    fun effectiveVisualSettings(templateRoot: Map<String, Any?>, profile: ConfigProfile): LinkedHashMap<String, Any?> =
        LinkedHashMap<String, Any?>().apply {
            putAll(defaults)
            putAll(visualFieldsFromRoot(templateRoot))
            profile.mihomoSettings.forEach { (key, value) ->
                require(key in visualManagedKeys) { "视觉配置包含不受管理的字段：$key" }
                if (key in partialBlocks) {
                    put(key, LinkedHashMap((get(key).asStringMap().orEmpty()) + value.asStringMap().orEmpty()))
                } else put(key, value)
            }
        }

    fun mergeVisualSettings(root: LinkedHashMap<String, Any?>, settings: Map<String, Any?>) {
        normalizeVisualSettings(settings).forEach { (key, value) ->
            if (key in partialBlocks) {
                val merged = LinkedHashMap(root[key].asStringMap().orEmpty()).apply { putAll(value.asStringMap().orEmpty()) }
                root[key] = merged
            } else root[key] = value
        }
    }

    fun normalizeVisualSettings(raw: Map<String, Any?>): LinkedHashMap<String, Any?> {
        val unknown = raw.keys - visualManagedKeys
        require(unknown.isEmpty()) { "视觉配置包含不受管理的字段：${unknown.joinToString("、")}" }
        val normalized = LinkedHashMap(raw)
        listOf("port", "socks-port", "redir-port", "tproxy-port", "mixed-port", "keep-alive-idle", "keep-alive-interval", "routing-mark").forEach { key ->
            if (key in normalized) {
                val value = normalized[key]
                if (value == null || value.toString().isBlank()) normalized.remove(key)
                else {
                    val number = (value as? Number)?.toInt() ?: value.toString().toIntOrNull()
                        ?: error("$key 必须是整数")
                    val range = when (key) {
                        "keep-alive-idle" -> 0..86400
                        "keep-alive-interval" -> 1..86400
                        "routing-mark" -> 0..Int.MAX_VALUE
                        else -> 1..65535
                    }
                    require(number in range) { "$key 超出有效范围" }
                    normalized[key] = number
                }
            }
        }
        listOf("allow-lan", "ipv6", "unified-delay", "tcp-concurrent").forEach { key ->
            normalized[key]?.let { value ->
                normalized[key] = when (value) {
                    is Boolean -> value
                    else -> value.toString().toBooleanStrictOrNull() ?: error("$key 必须为开关值")
                }
            }
        }
        normalized["mode"]?.let { require(it.toString() in setOf("rule", "global", "direct")) { "mode 只能是 rule、global 或 direct" } }
        normalized["log-level"]?.let { require(it.toString() in setOf("silent", "error", "warning", "info", "debug")) { "日志等级无效" } }
        normalized["process-mode"]?.let { require(it.toString() in setOf("always", "strict", "off")) { "process-mode 无效" } }
        normalized["bind-address"]?.let { require(it.toString().isNotBlank()) { "bind-address 不能为空" } }
        normalized["authentication"]?.let {
            val values = toLines(it)
            if (values.isEmpty()) normalized.remove("authentication") else normalized["authentication"] = values
        }
        listOf("dns", "tun", "sniffer", "ntp").forEach { key ->
            if (key in normalized) {
                val block = normalizeBlock(normalized[key], key)
                if (block == null) normalized.remove(key) else {
                    val blockMap = block.asStringMap() ?: error("$key 必须是 YAML 对象")
                    val unknownFields = blockMap.keys - partialBlocks.getValue(key)
                    require(unknownFields.isEmpty()) { "$key 中包含由高级 YAML 管理的字段：${unknownFields.sorted().joinToString("、")}" }
                    normalized[key] = blockMap
                }
            }
        }
        (normalized["dns"] as? Map<*, *>)?.get("enhanced-mode")?.let {
            require(it.toString() in setOf("fake-ip", "redir-host")) { "DNS 增强模式无效" }
        }
        (normalized["tun"] as? Map<*, *>)?.get("stack")?.let {
            require(it.toString() in setOf("system", "gvisor", "mixed")) { "TUN 协议栈无效" }
        }
        listOf("external-controller", "external-ui", "external-ui-url", "secret", "interface-name", "global-client-fingerprint").forEach { key ->
            normalized[key]?.let { if (it.toString().isBlank()) normalized.remove(key) else normalized[key] = it.toString() }
        }
        return normalized
    }

    fun validateAdvancedYaml(text: String): LinkedHashMap<String, Any?> {
        if (text.isBlank()) return linkedMapOf()
        val options = LoaderOptions().apply { maxAliasesForCollections = 30; codePointLimit = 2 * 1024 * 1024 }
        val loaded = runCatching { Yaml(SafeConstructor(options)).load<Any?>(text.trimStart('\uFEFF')) }
            .getOrElse { throw IllegalArgumentException("高级 YAML 格式错误：${it.message ?: "无法解析"}") }
        val root = loaded.asStringMap() ?: error("高级 YAML 顶层必须是键值对象")
        val conflicts = root.keys intersect (applicationManagedKeys + (visualManagedKeys - partialBlocks.keys))
        require(conflicts.isEmpty()) { "这些字段由表单管理，不能在高级 YAML 中编辑：${conflicts.sorted().joinToString("、")}" }
        partialBlocks.forEach { (block, managedFields) ->
            root[block]?.let { rawBlock ->
                val blockMap = rawBlock.asStringMap() ?: error("高级 YAML 中的 $block 必须是对象")
                val blockConflicts = blockMap.keys intersect managedFields
                require(blockConflicts.isEmpty()) { "$block 中这些字段由表单管理：${blockConflicts.sorted().joinToString("、")}" }
            }
        }
        return LinkedHashMap(root)
    }

    fun dumpAdvancedFields(root: Map<String, Any?>): String {
        val advanced = LinkedHashMap(root.filterKeys { it !in allManagedKeys })
        partialBlocks.forEach { (block, managedFields) ->
            val source = root[block].asStringMap().orEmpty()
            val unmanaged = source.filterKeys { it !in managedFields }
            if (unmanaged.isNotEmpty()) advanced[block] = unmanaged
        }
        if (advanced.isEmpty()) return ""
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            indicatorIndent = 0
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            width = 120
        }
        return Yaml(options).dump(advanced)
    }

    /**
     * Template advanced fields are an editable replacement set. Visual/app-owned fields keep
     * their original place and are overlaid later by the generator.
     */
    fun applyAdvancedFields(root: LinkedHashMap<String, Any?>, yaml: String?) {
        if (yaml == null) return
        root.keys.filter { it !in allManagedKeys }.toList().forEach(root::remove)
        val advanced = validateAdvancedYaml(yaml)
        advanced.forEach { (key, value) ->
            if (key in partialBlocks) {
                val managedFields = partialBlocks.getValue(key)
                val merged = LinkedHashMap(root[key].asStringMap().orEmpty()).apply {
                    keys.filter { it !in managedFields }.toList().forEach { remove(it) }
                    putAll(value.asStringMap().orEmpty())
                }
                root[key] = merged
            } else root[key] = value
        }
    }

    private fun normalizeBlock(value: Any?, label: String): Any? {
        if (value == null) return null
        val map = value.asStringMap() ?: error("$label 必须是 YAML 对象")
        val result = LinkedHashMap(map)
        val listKeys = when (label) {
            "dns" -> setOf("default-nameserver", "nameserver", "fallback", "proxy-server-nameserver", "direct-nameserver", "fake-ip-filter")
            "tun" -> setOf("dns-hijack")
            else -> emptySet()
        }
        listKeys.forEach { key -> if (key in result) {
            val lines = toLines(result[key])
            if (lines.isEmpty()) result.remove(key) else result[key] = lines
        } }
        val booleanKeys = when (label) {
            "dns" -> setOf("enable", "ipv6", "use-hosts", "use-system-hosts", "respect-rules")
            "tun" -> setOf("enable", "auto-route", "auto-detect-interface", "strict-route", "endpoint-independent-nat")
            "sniffer" -> setOf("enable", "force-dns-mapping", "parse-pure-ip", "override-destination")
            "ntp" -> setOf("enable", "write-to-system")
            else -> emptySet()
        }
        booleanKeys.forEach { key -> result[key]?.let { value ->
            result[key] = when (value) {
                is Boolean -> value
                else -> value.toString().toBooleanStrictOrNull() ?: error("$label.$key 必须为开关值")
            }
        } }
        val integerRanges = when (label) {
            "tun" -> mapOf("mtu" to (576..9000))
            "ntp" -> mapOf("port" to (1..65535), "interval" to (1..Int.MAX_VALUE))
            else -> emptyMap()
        }
        integerRanges.forEach { (key, range) -> if (key in result) {
            val value = result[key]
            if (value == null || value.toString().isBlank()) result.remove(key)
            else {
                val number = (value as? Number)?.toInt() ?: value.toString().toIntOrNull() ?: error("$label.$key 必须是整数")
                require(number in range) { "$label.$key 超出有效范围" }
                result[key] = number
            }
        } }
        return result
    }

    private fun Any?.asStringMap(): Map<String, Any?>? {
        val map = this as? Map<*, *> ?: return null
        return map.entries.associate { (key, value) -> key.toString() to value }
    }

    private fun toLines(value: Any?): List<String> = when (value) {
        is List<*> -> value.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
        else -> value?.toString().orEmpty().lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    }
}

/** Keeps nested protocol options (booleans, lists and TLS/transport maps) typed during node edits. */
object MihomoNodeOptionsYaml {
    private val reserved = setOf("name", "type", "server", "port")

    fun dump(options: Map<String, Any?>): String {
        val fields = options.filterKeys { it !in reserved }
        if (fields.isEmpty()) return ""
        val dumpOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
            defaultScalarStyle = DumperOptions.ScalarStyle.PLAIN
            width = 120
        }
        return Yaml(dumpOptions).dump(fields)
    }

    fun parse(text: String): LinkedHashMap<String, Any?> {
        if (text.isBlank()) return linkedMapOf()
        val options = LoaderOptions().apply { maxAliasesForCollections = 20; codePointLimit = 512 * 1024 }
        val loaded = runCatching { Yaml(SafeConstructor(options)).load<Any?>(text.trimStart('\uFEFF')) }
            .getOrElse { throw IllegalArgumentException("节点参数 YAML 格式错误：${it.message ?: "无法解析"}") }
        val values = loaded.asStringMap() ?: error("节点参数必须是 YAML 键值对象")
        val conflicts = values.keys intersect reserved
        require(conflicts.isEmpty()) { "节点名称、类型、服务器和端口请在上方字段编辑" }
        return LinkedHashMap(values)
    }

    private fun Any?.asStringMap(): Map<String, Any?>? {
        val map = this as? Map<*, *> ?: return null
        return map.entries.associate { (key, value) -> key.toString() to value }
    }
}
