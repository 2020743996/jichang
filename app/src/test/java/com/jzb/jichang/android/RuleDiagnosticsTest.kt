package com.jzb.jichang.android

import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.service.RuleDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleDiagnosticsTest {
    @Test fun reportsMissingTargetsAndReferencesAndMalformedCombinations() {
        val profile = RuleProfile(
            groups = listOf(PolicyGroup("PROXY")),
            rules = listOf(
                RoutingRule("DOMAIN", "example.com", "MISSING"),
                RoutingRule("RULE-SET", "missing-provider", "PROXY"),
                RoutingRule("SUB-RULE", "missing-subrule", "PROXY"),
                RoutingRule("AND", "", "PROXY", conditions = listOf(RuleCondition(type = "DOMAIN", value = ""))),
            ),
        )
        val issues = RuleDiagnostics.inspect(profile)

        assertTrue(issues.any { it.message.contains("目标策略组") })
        assertTrue(issues.any { it.message.contains("规则集提供者") })
        assertTrue(issues.any { it.message.contains("子规则") })
        assertTrue(issues.any { it.message.contains("组合规则") })
    }

    @Test fun allowsImportedRawCompositeExpressionToBePreserved() {
        val profile = RuleProfile(
            groups = listOf(PolicyGroup("PROXY")),
            rules = listOf(RoutingRule("AND", "", "PROXY", rawLine = "AND,((DOMAIN-SUFFIX,example.com),(NETWORK,udp)),PROXY")),
        )
        assertTrue(RuleDiagnostics.inspect(profile).isEmpty())
    }

    @Test fun duplicateMatchersWithDifferentTargetsAreWarningsNotErrors() {
        val profile = RuleProfile(
            groups = listOf(PolicyGroup("PROXY"), PolicyGroup("BACKUP")),
            rules = listOf(
                RoutingRule("DOMAIN-SUFFIX", "example.com", "PROXY"),
                RoutingRule("DOMAIN-SUFFIX", "example.com", "BACKUP"),
            ),
        )

        val conflict = RuleDiagnostics.inspect(profile).single()
        assertTrue(conflict.warning)
        assertTrue(conflict.message.contains("第 1 条"))
        assertEquals(0, conflict.relatedIndex)
    }

    @Test fun reportsCyclicProxyGroupMembership() {
        val profile = RuleProfile(groups = listOf(
            PolicyGroup("A", members = listOf("B")),
            PolicyGroup("B", members = listOf("A")),
        ))

        assertTrue(RuleDiagnostics.inspect(profile).any { it.message.contains("循环成员引用") && it.groupName != null })
    }

    @Test fun reportsStrategyGroupsReferencingDeletedNodesWhenInventoryIsAvailable() {
        val profile = RuleProfile(groups = listOf(PolicyGroup("PROXY", members = listOf("node:gone"))))

        assertTrue(RuleDiagnostics.inspect(profile, emptySet()).any { it.message.contains("节点已不存在") && it.groupName == "PROXY" })
    }
}
