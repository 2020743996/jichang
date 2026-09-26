package com.jzb.tower.android

import com.jzb.tower.android.model.AppState
import com.jzb.tower.android.model.PolicyGroup
import com.jzb.tower.android.model.ProxyNode
import com.jzb.tower.android.model.RoutingRule
import com.jzb.tower.android.model.RuleProfile
import com.jzb.tower.android.service.MihomoConfigGenerator
import org.junit.Test
import java.io.File

class MihomoFixtureWriterTest {
    @Test fun writesGeneratorOutputForOptionalNativeCoreValidation() {
        val path = System.getProperty("tower.fixturePath")?.takeIf(String::isNotBlank) ?: return
        val state = AppState(
            nodes = listOf(
                ProxyNode(
                    id = "native-check", sourceId = null, name = "validation node", type = "trojan",
                    server = "example.com", port = 443,
                    options = mapOf("type" to "trojan", "password" to "test-only", "tls" to true, "sni" to "example.com"),
                ),
            ),
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY", "select")),
                rules = listOf(RoutingRule("DOMAIN-SUFFIX", "example.org", "PROXY")),
            ),
        )
        File(path).apply { parentFile?.mkdirs(); writeText(MihomoConfigGenerator().generate(state).yaml) }
    }
}
