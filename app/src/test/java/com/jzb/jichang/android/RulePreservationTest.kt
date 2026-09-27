package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.service.MihomoConfigGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class RulePreservationTest {
    @Test fun generatorPreservesAdvancedParametersAndImportedRawCombinations() {
        val profile = ConfigProfile(
            id = "test",
            name = "Test",
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY")),
                rules = listOf(
                    RoutingRule("DOMAIN-SUFFIX", "example.com", "PROXY", extraParameters = listOf("custom-param")),
                    RoutingRule("AND", "", "PROXY", rawLine = "AND,((DOMAIN-SUFFIX,example.com),(NETWORK,udp)),PROXY"),
                ),
            ),
        )
        val yaml = MihomoConfigGenerator().generate(AppState(profiles = listOf(profile), activeProfileId = profile.id)).yaml

        assertTrue(yaml.contains("DOMAIN-SUFFIX,example.com,PROXY,custom-param"))
        assertTrue(yaml.contains("AND,((DOMAIN-SUFFIX,example.com),(NETWORK,udp)),PROXY"))
    }
}
