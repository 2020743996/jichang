package com.jzb.tower.android

import com.jzb.tower.android.model.AppState
import com.jzb.tower.android.model.PolicyGroup
import com.jzb.tower.android.model.ProxyNode
import com.jzb.tower.android.model.RoutingRule
import com.jzb.tower.android.service.MihomoConfigGenerator
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
            ruleProfile = com.jzb.tower.android.model.RuleProfile(
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
}
