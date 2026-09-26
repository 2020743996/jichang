package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
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
