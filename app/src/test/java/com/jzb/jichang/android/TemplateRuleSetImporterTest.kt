package com.jzb.jichang.android

import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.TemplateProviderConflictAction
import com.jzb.jichang.android.service.TemplateRuleSetImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRuleSetImporterTest {
    private val template = ConfigTemplate("tpl-1", "家庭模板", templateYaml, "home.yaml")

    @Test fun importsSelectedProviderAndAssociatedRulesNamespacedByTemplate() {
        val parsed = MihomoTemplateParser().parse(template.rawYaml)
        val result = TemplateRuleSetImporter.apply(
            current = currentProfile(),
            template = template,
            selectedProviderIds = setOf(parsed.ruleProfile.providers.single().id),
        )

        assertEquals("家庭模板 · ads", result.providers.single().name)
        assertEquals(template.id, result.providers.single().sourceTemplateId)
        assertEquals(RoutingRule("RULE-SET", "家庭模板 · ads", "PROXY"), result.rules.first())
        assertEquals("MATCH", result.rules.last().type)
    }

    @Test fun providerNameCollisionSupportsKeepReplaceAndRename() {
        val parsed = MihomoTemplateParser().parse(template.rawYaml)
        val sourceProviderId = parsed.ruleProfile.providers.single().id
        val imported = TemplateRuleSetImporter.apply(currentProfile(), template, setOf(sourceProviderId))
        val existingId = imported.providers.single().id

        val keep = TemplateRuleSetImporter.apply(
            imported, template, setOf(sourceProviderId),
            mapOf(sourceProviderId to TemplateProviderConflictAction.KEEP_EXISTING),
        )
        assertEquals(1, keep.providers.size)
        assertEquals(existingId, keep.providers.single().id)

        val replaced = TemplateRuleSetImporter.apply(
            imported, template, setOf(sourceProviderId),
            mapOf(sourceProviderId to TemplateProviderConflictAction.REPLACE),
        )
        assertEquals(1, replaced.providers.size)
        assertNotEquals(existingId, replaced.providers.single().id)

        val renamed = TemplateRuleSetImporter.apply(
            imported, template, setOf(sourceProviderId),
            mapOf(sourceProviderId to TemplateProviderConflictAction.RENAME),
            mapOf(sourceProviderId to "家庭广告规则"),
        )
        assertEquals(2, renamed.providers.size)
        assertTrue(renamed.providers.any { it.name == "家庭广告规则" })
    }

    @Test fun collisionRequiresAnExplicitResolution() {
        val parsed = MihomoTemplateParser().parse(template.rawYaml)
        val providerId = parsed.ruleProfile.providers.single().id
        val imported = TemplateRuleSetImporter.apply(currentProfile(), template, setOf(providerId))

        val failure = runCatching { TemplateRuleSetImporter.apply(imported, template, setOf(providerId)) }
        assertTrue(failure.isFailure)
    }

    private fun currentProfile() = RuleProfile(
        groups = listOf(PolicyGroup("PROXY")),
        rules = listOf(RoutingRule("MATCH", "MATCH", "PROXY")),
    )

    private companion object {
        val templateYaml = """
            rule-providers:
              ads:
                type: http
                behavior: domain
                url: https://rules.example/ads.yaml
            proxy-groups:
              - name: TEMPLATE-ONLY
                type: select
                proxies: [DIRECT]
            rules:
              - RULE-SET,ads,TEMPLATE-ONLY
              - MATCH,TEMPLATE-ONLY
        """.trimIndent()
    }
}
