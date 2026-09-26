package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.MihomoTemplateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class MihomoTemplateParserTest {
    private val yaml = """
        mixed-port: 7891
        mode: rule
        dns:
          enable: true
          nameserver: [1.1.1.1]
        tun:
          enable: true
          stack: system
        proxies:
          - {name: HK-01, type: ss, server: hk.example, port: 443, cipher: aes-128-gcm, password: secret}
        proxy-groups:
          - name: PROXY
            type: url-test
            proxies: [HK-01, DIRECT]
            url: https://example.com/check
            interval: 600
        rules:
          - DOMAIN-SUFFIX,example.com,PROXY
          - AND,((DOMAIN,internal.example),(NETWORK,udp)),DIRECT
          - MATCH,PROXY
        custom-section:
          keep: true
    """.trimIndent()

    @Test fun importsEditableSectionsAndPreservesOtherTemplateFieldsOnExport() {
        val parsed = MihomoTemplateParser().parse(yaml)
        val template = ConfigTemplate("template-1", "Base", yaml, "base.yaml")
        val profile = ConfigProfile(
            id = "profile-1", name = "From template", templateId = template.id,
            enabledNodeIds = parsed.nodes.map { it.id }.toSet(), ruleProfile = parsed.ruleProfile,
        )
        val output = MihomoConfigGenerator().generate(AppState(nodes = parsed.nodes, profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template)))
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>

        assertEquals(7891, (root["mixed-port"] as Number).toInt())
        assertEquals(true, (root["dns"] as Map<*, *>)["enable"])
        assertEquals("system", (root["tun"] as Map<*, *>)["stack"])
        assertEquals(true, (root["custom-section"] as Map<*, *>)["keep"])
        assertEquals("https://example.com/check", groups.single { it["name"] == "PROXY" }["url"])
        assertTrue(rules.contains("AND,((DOMAIN,internal.example),(NETWORK,udp)),DIRECT"))
        assertEquals("MATCH,PROXY", rules.last())
        assertEquals("secret", (root["proxies"] as List<Map<String, Any?>>).single()["password"])
    }

    @Test fun rejectsNonMihomoYamlAndUnsafeObjectTags() {
        assertTrue(runCatching { MihomoTemplateParser().parse("title: not mihomo") }.isFailure)
        assertTrue(runCatching { MihomoTemplateParser().parse("mode: rule\nvalue: !!java/object:java.lang.Runtime {}") }.isFailure)
    }
}
