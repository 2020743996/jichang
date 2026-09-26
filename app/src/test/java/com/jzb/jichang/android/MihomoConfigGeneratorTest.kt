package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.SubscriptionSource
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.ConfigSourceMode
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.NodeAutoGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class MihomoConfigGeneratorTest {
    @Test fun emitsParsableMihomoYamlWithGroupsRulesAndOnlyEnabledNodes() {
        val node = ProxyNode(
            id = "1", sourceId = null, name = "Node: one", type = "trojan", server = "node.example", port = 443,
            options = mapOf("type" to "trojan", "password" to "secret", "tls" to true),
        )
        val disabled = node.copy(id = "2", name = "disabled", enabled = false)
        val state = AppState(
            nodes = listOf(node, disabled),
            ruleProfile = com.jzb.jichang.android.model.RuleProfile(
                groups = listOf(PolicyGroup("PROXY", members = listOf("Node: one"))),
                rules = listOf(RoutingRule("DOMAIN-SUFFIX", "example.com", "PROXY")),
            ),
        )

        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        val proxies = root["proxies"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>

        assertEquals(1, output.exportedNodes)
        assertEquals(0, output.skippedNodes)
        assertEquals("Node: one", proxies.single()["name"])
        assertEquals(listOf("DOMAIN-SUFFIX,example.com,PROXY", "MATCH,PROXY"), rules)
    }

    @Test fun sanitizesUntrustedNamesAndCountsUnsupportedProtocols() {
        val unsafe = ProxyNode("1", null, "safe\nMATCH,example.org,DIRECT", "ss", "node.example", 443)
        val unsupported = ProxyNode("2", null, "unsupported", "ssr", "node.example", 443)
        val output = MihomoConfigGenerator().generate(AppState(nodes = listOf(unsafe, unsupported)))
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        val proxies = root["proxies"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>

        assertEquals(1, output.exportedNodes)
        assertEquals(1, output.skippedNodes)
        assertFalse(proxies.single()["name"].toString().contains('\n'))
        assertTrue(rules.none { it.startsWith("MATCH,example.org") })
    }

    @Test fun buildsRegionGroupsAndKeepsMihomoMatchAsLastRule() {
        val hongKong = ProxyNode("hk", null, "香港 01", "ss", "hk.example", 443)
        val japan = ProxyNode("jp", null, "Tokyo JP", "trojan", "jp.example", 443)
        val state = AppState(
            nodes = listOf(hongKong, japan),
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY")),
                rules = listOf(
                    RoutingRule("IP-CIDR", "10.0.0.0/8", "DIRECT", noResolve = true),
                    RoutingRule("MATCH", "", "DIRECT"),
                ),
            ),
        )
        val output = MihomoConfigGenerator().generate(state, ConfigExportOptions(enabledRegions = setOf("hk", "jp", NodeAutoGroups.OTHER)))
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hongKongGroup = groups.single { it["name"] == "🌏 香港" }
        val rules = root["rules"] as List<String>

        assertEquals(listOf("香港 01"), hongKongGroup["proxies"])
        assertEquals(listOf("🌏 香港", "🌏 日本", "🌏 其他"), groups.first { it["name"] == "PROXY" }["proxies"])
        assertEquals("IP-CIDR,10.0.0.0/8,DIRECT,no-resolve", rules[0])
        assertEquals("MATCH,DIRECT", rules.last())
        assertEquals("香港", NodeAutoGroups.title(NodeAutoGroups.classify("HK 香港线路")))
    }

    @Test fun referencesOnlyMihomoSubscriptionsAndKeepsManualNodesInline() {
        val source = SubscriptionSource("source-1", "机场", "https://sub.example/token", providerCompatible = true)
        val textSource = SubscriptionSource("source-2", "旧订阅", "https://sub.example/text", providerCompatible = false)
        val hk = ProxyNode("hk", source.id, "香港高速", "ss", "hk.example", 443)
        val local = ProxyNode("local", null, "自建节点", "trojan", "local.example", 443)
        val parsedText = ProxyNode("text", textSource.id, "日本线路", "vmess", "jp.example", 443)
        val state = AppState(sources = listOf(source, textSource), nodes = listOf(hk, local, parsedText))

        val output = MihomoConfigGenerator().generate(
            state,
            ConfigExportOptions(sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, excludedNodeIds = setOf(hk.id)),
        )
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val proxies = root["proxies"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hongKongGroup = groups.single { it["name"] == "🌏 香港" }

        assertEquals(1, output.referencedSubscriptions)
        assertEquals(setOf("自建节点", "日本线路"), proxies.map { it["name"] }.toSet())
        assertEquals("https://sub.example/token", providers.getValue("订阅-1")["url"])
        assertEquals(listOf("订阅-1"), hongKongGroup["use"])
        assertTrue(hongKongGroup["exclude-filter"].toString().contains("香港高速"))
    }

    @Test fun providerNumberingSkipsSubscriptionsThatCannotBeReferenced() {
        val textSource = SubscriptionSource("text", "普通订阅", "https://sub.example/text", providerCompatible = false)
        val mihomoSource = SubscriptionSource("mihomo", "Mihomo 订阅", "https://sub.example/mihomo", providerCompatible = true)
        val state = AppState(
            sources = listOf(textSource, mihomoSource),
            nodes = listOf(
                ProxyNode("text-node", textSource.id, "手动回退节点", "ss", "text.example", 443),
                ProxyNode("mihomo-node", mihomoSource.id, "Mihomo 节点", "ss", "mihomo.example", 443),
            ),
        )

        val output = MihomoConfigGenerator().generate(
            state,
            ConfigExportOptions(sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS),
        )
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val proxies = root["proxies"] as List<Map<String, Any?>>

        assertEquals(setOf("订阅-1"), providers.keys)
        assertEquals("https://sub.example/mihomo", providers.getValue("订阅-1")["url"])
        assertEquals(setOf("手动回退节点"), proxies.map { it["name"] }.toSet())
    }
}
