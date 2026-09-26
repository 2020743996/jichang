package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.service.MihomoConfigGenerator
import org.junit.Test
import java.io.File

class MihomoFixtureWriterTest {
    @Test fun writesGeneratorOutputForOptionalNativeCoreValidation() {
        val path = System.getProperty("jichang.fixturePath")?.takeIf(String::isNotBlank) ?: return
        val nodes = listOf(
                ProxyNode(
                    id = "native-check", sourceId = null, name = "validation node", type = "trojan",
                    server = "example.com", port = 443,
                    options = mapOf("type" to "trojan", "password" to "test-only", "tls" to true, "sni" to "example.com"),
                ),
            )
        val profile = ConfigProfile("fixture", "Fixture", enabledNodeIds = nodes.map { it.id }.toSet(), ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY", "select")),
                rules = listOf(RoutingRule("DOMAIN-SUFFIX", "example.org", "PROXY")),
            ))
        val state = AppState(nodes = nodes, profiles = listOf(profile), activeProfileId = profile.id)
        File(path).apply { parentFile?.mkdirs(); writeText(MihomoConfigGenerator().generate(state).yaml) }

        val templateYaml = """
            mixed-port: 7891
            mode: rule
            dns:
              enable: true
              nameserver: [1.1.1.1]
            tun:
              enable: true
              stack: system
            custom-section:
              keep: true
        """.trimIndent()
        val template = ConfigTemplate("fixture-template", "Fixture template", templateYaml, "fixture.yaml")
        val templatedProfile = profile.copy(templateId = template.id)
        val templatedState = state.copy(profiles = listOf(templatedProfile), templates = listOf(template))
        File("$path.template.yaml").writeText(MihomoConfigGenerator().generate(templatedState).yaml)
    }
}
