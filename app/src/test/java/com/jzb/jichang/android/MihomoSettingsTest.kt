package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.MihomoSettings
import com.jzb.jichang.android.service.MihomoNodeOptionsYaml
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.SubscriptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.util.Base64

class MihomoSettingsTest {
    @Test fun profileVisualSettingsAreEmittedAndValidatedAsMihomoValues() {
        val profile = ConfigProfile(
            id = "one", name = "配置",
            mihomoSettings = mapOf(
                "mixed-port" to "1080",
                "allow-lan" to true,
                "mode" to "global",
                "dns" to mapOf(
                    "enable" to true,
                    "listen" to "127.0.0.1:1053",
                    "nameserver" to "1.1.1.1\n8.8.8.8",
                ),
                "tun" to mapOf("enable" to true, "mtu" to "9000", "dns-hijack" to "any:53\ntcp://any:53"),
            ),
        )
        val output = MihomoConfigGenerator().generate(AppState(profiles = listOf(profile), activeProfileId = profile.id))
        val root = parse(output.yaml)

        assertEquals(1080, root["mixed-port"])
        assertEquals(true, root["allow-lan"])
        assertEquals("global", root["mode"])
        @Suppress("UNCHECKED_CAST")
        val dns = root["dns"] as Map<String, Any?>
        assertEquals(listOf("1.1.1.1", "8.8.8.8"), dns["nameserver"])
        @Suppress("UNCHECKED_CAST")
        val tun = root["tun"] as Map<String, Any?>
        assertEquals(9000, tun["mtu"])
        assertEquals(listOf("any:53", "tcp://any:53"), tun["dns-hijack"])
        assertThrows(IllegalArgumentException::class.java) {
            MihomoSettings.normalizeVisualSettings(mapOf("mixed-port" to "70000"))
        }
    }

    @Test fun advancedYamlCanReplaceUnmanagedTemplateFieldsButCannotOverrideEditorFields() {
        val templateYaml = """
            mixed-port: 7891
            profile:
              store-selected: true
            geodata-mode: true
            dns:
              enable: false
              nameserver-policy:
                "geosite:cn": 114.114.114.114
            proxies: []
            proxy-groups: []
            rules: [MATCH, DIRECT]
        """.trimIndent()
        val template = ConfigTemplate("template", "模板", templateYaml, "template.yaml")
        val profile = ConfigProfile(
            id = "profile", name = "配置", templateId = template.id,
            mihomoSettings = mapOf("mixed-port" to "1080", "dns" to mapOf("enable" to true, "nameserver" to listOf("1.1.1.1"))),
            advancedYaml = "profile:\n  store-selected: false\ngeodata-mode: true\ndns:\n  nameserver-policy:\n    \"geosite:cn\": 8.8.8.8\n",
            ruleProfile = MihomoTemplateParser().parse(templateYaml).ruleProfile,
        )
        val state = AppState(profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))
        val root = parse(MihomoConfigGenerator().generate(state).yaml)

        assertEquals(1080, root["mixed-port"])
        @Suppress("UNCHECKED_CAST")
        val profileFields = root["profile"] as Map<String, Any?>
        assertEquals(false, profileFields["store-selected"])
        assertEquals(true, root["geodata-mode"])
        @Suppress("UNCHECKED_CAST")
        val dns = root["dns"] as Map<String, Any?>
        assertEquals(true, dns["enable"])
        assertEquals(listOf("1.1.1.1"), dns["nameserver"])
        @Suppress("UNCHECKED_CAST")
        val policy = dns["nameserver-policy"] as Map<String, Any?>
        assertEquals("8.8.8.8", policy["geosite:cn"])
        assertThrows(IllegalArgumentException::class.java) {
            MihomoSettings.validateAdvancedYaml("dns:\n  enable: true\n")
        }
    }

    @Test fun deletingVisualRuleProvidersDoesNotLeaveStaleTemplateBlock() {
        val templateYaml = """
            mixed-port: 7890
            rule-providers:
              old:
                type: http
                behavior: domain
                url: https://example.com/rules.yaml
                path: ./rules.yaml
            proxies: []
            proxy-groups:
              - name: PROXY
                type: select
                proxies: [DIRECT]
            rules: [MATCH, PROXY]
        """.trimIndent()
        val template = ConfigTemplate("template", "模板", templateYaml, "template.yaml")
        val parsed = MihomoTemplateParser().parse(templateYaml)
        val profile = ConfigProfile(
            id = "profile", name = "配置", templateId = template.id,
            ruleProfile = parsed.ruleProfile.copy(providers = emptyList()),
            advancedYaml = MihomoSettings.dumpAdvancedFields(parsed.rawRoot),
        )
        val root = parse(MihomoConfigGenerator().generate(AppState(profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))).yaml)
        assertFalse(root.containsKey("rule-providers"))
        assertTrue(root.containsKey("rules"))
    }

    @Test fun parsesAndExportsShadowsocksRFromSubscriptionUri() {
        fun b64(value: String) = Base64.getEncoder().withoutPadding().encodeToString(value.toByteArray())
        val body = "ssr.example:8388:auth_aes128_md5:aes-128-ctr:tls1.2_ticket_auth:${b64("secret")}/?remarks=${b64("SSR HK")}&obfsparam=${b64("cdn.example")}"
        val uri = "ssr://${b64(body)}"
        val parsed = SubscriptionParser().parse(uri)
        assertEquals(0, parsed.skippedCount)
        assertEquals("ssr", parsed.nodes.single().type)
        assertEquals("secret", parsed.nodes.single().options["password"])
        assertEquals("cdn.example", parsed.nodes.single().options["obfs-param"])

        val profile = ConfigProfile(id = "ssr", name = "SSR", enabledNodeIds = setOf(parsed.nodes.single().id))
        val output = MihomoConfigGenerator().generate(AppState(nodes = parsed.nodes, profiles = listOf(profile), activeProfileId = profile.id))
        val root = parse(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val proxy = (root["proxies"] as List<Map<String, Any?>>).single()
        assertEquals("ssr", proxy["type"])
        assertEquals("SSR HK", proxy["name"])
        assertTrue(output.yaml.contains("protocol: auth_aes128_md5"))
    }

    @Test fun nodeOptionsEditorRoundTripsNestedAndTypedProtocolFields() {
        val options = mapOf(
            "uuid" to "user-id",
            "tls" to true,
            "alpn" to listOf("h2", "http/1.1"),
            "reality-opts" to mapOf("public-key" to "key", "short-id" to "abcd"),
        )
        val edited = MihomoNodeOptionsYaml.parse(MihomoNodeOptionsYaml.dump(options))
        assertEquals(true, edited["tls"])
        assertEquals(listOf("h2", "http/1.1"), edited["alpn"])
        @Suppress("UNCHECKED_CAST")
        val reality = edited["reality-opts"] as Map<String, Any?>
        assertEquals("key", reality["public-key"])
        assertThrows(IllegalArgumentException::class.java) { MihomoNodeOptionsYaml.parse("type: ss\n") }
    }

    private fun parse(yaml: String): Map<String, Any?> =
        Yaml(SafeConstructor(LoaderOptions())).load(yaml.byteInputStream())
}
