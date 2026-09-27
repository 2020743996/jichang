package com.jzb.jichang.android

import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.service.RuleSimulationCertainty
import com.jzb.jichang.android.service.RuleSimulationInput
import com.jzb.jichang.android.service.RuleSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSimulatorTest {
    @Test fun `first matching rule follows configured order`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(
                RoutingRule("DOMAIN-SUFFIX", "example.com", "DIRECT"),
                RoutingRule("DOMAIN", "api.example.com", "PROXY"),
                RoutingRule("MATCH", "", "REJECT"),
            )),
            RuleSimulationInput(hostOrIp = "api.example.com"),
        )
        assertEquals(RuleSimulationCertainty.DEFINITE, result.certainty)
        assertEquals(0, result.matchedIndex)
        assertEquals("DIRECT", result.target)
    }

    @Test fun `unknown data before a certain match makes result conditional`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(
                RoutingRule("GEOSITE", "cn", "DIRECT"),
                RoutingRule("DOMAIN-SUFFIX", "example.com", "PROXY"),
                RoutingRule("MATCH", "", "REJECT"),
            )),
            RuleSimulationInput(hostOrIp = "api.example.com"),
        )
        assertEquals(RuleSimulationCertainty.CONDITIONAL, result.certainty)
        assertEquals(listOf(0), result.uncertainIndexes)
        assertEquals("PROXY", result.target)
    }

    @Test fun `cidr and logical conditions are evaluated locally`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(
                RoutingRule("AND", "", "DIRECT", conditions = listOf(
                    RuleCondition(type = "IP-CIDR", value = "192.168.0.0/16"),
                    RuleCondition(type = "DST-PORT", value = "443"),
                )),
                RoutingRule("MATCH", "", "PROXY"),
            )),
            RuleSimulationInput(hostOrIp = "192.168.1.20", destinationPort = 443),
        )
        assertEquals(RuleSimulationCertainty.DEFINITE, result.certainty)
        assertEquals(0, result.matchedIndex)
    }

    @Test fun `unprovided matching fields remain unknown`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(RoutingRule("DST-PORT", "443", "PROXY"))),
            RuleSimulationInput(hostOrIp = "example.com"),
        )
        assertEquals(RuleSimulationCertainty.UNKNOWN, result.certainty)
        assertTrue(result.uncertainIndexes.contains(0))
    }

    @Test fun `source address rules are not evaluated against the destination address`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(RoutingRule("SRC-IP-CIDR", "192.168.0.0/16", "DIRECT"))),
            RuleSimulationInput(hostOrIp = "192.168.1.20"),
        )
        assertEquals(RuleSimulationCertainty.UNKNOWN, result.certainty)
        assertEquals(listOf(0), result.uncertainIndexes)
    }

    @Test fun `process path regex does not use the process name as a substitute`() {
        val result = RuleSimulator.simulate(
            RuleProfile(rules = listOf(RoutingRule("PROCESS-PATH-REGEX", "chrome", "PROXY"))),
            RuleSimulationInput(hostOrIp = "example.com", processName = "chrome"),
        )
        assertEquals(RuleSimulationCertainty.UNKNOWN, result.certainty)
    }

    @Test fun `inline domain rule set can be simulated from local payload`() {
        val result = RuleSimulator.simulate(
            RuleProfile(
                providers = listOf(RuleProvider("ads", "ads", type = "inline", behavior = "domain", payload = listOf("+.ads.example", "tracker.test"))),
                rules = listOf(RoutingRule("RULE-SET", "ads", "REJECT"), RoutingRule("MATCH", "", "DIRECT")),
            ),
            RuleSimulationInput(hostOrIp = "img.ads.example"),
        )
        assertEquals(RuleSimulationCertainty.DEFINITE, result.certainty)
        assertEquals("REJECT", result.target)
    }
}
