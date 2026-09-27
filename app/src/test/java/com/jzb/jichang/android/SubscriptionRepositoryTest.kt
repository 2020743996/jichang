package com.jzb.jichang.android

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.jzb.jichang.android.data.SnapshotDao
import com.jzb.jichang.android.data.SnapshotEntity
import com.jzb.jichang.android.data.JichangRepository
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.RuleProviderStatus
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.SubRuleProfile
import com.jzb.jichang.android.model.SubscriptionSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionRepositoryTest {
    @Test fun failedRefreshKeepsExistingNodesAndRecordsError() = runBlocking {
        val source = SubscriptionSource("source-1", "test", "http://127.0.0.1:1/subscription")
        val oldNode = ProxyNode("node-1", source.id, "old node", "trojan", "old.example", 443)
        val dao = InMemorySnapshotDao(Gson().toJson(AppState(sources = listOf(source), nodes = listOf(oldNode))))
        val repository = JichangRepository(dao)
        delay(100)

        try { runCatching { repository.refreshSource(source.id) } }
        finally { repository.close() }

        assertEquals(listOf(oldNode), repository.state.value.nodes)
        assertNotNull(repository.state.value.sources.single().lastError)
    }

    @Test fun sourceUrlsMustBeHttpOrHttps() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        val result = try { runCatching { repository.addSource("bad", "file:///etc/passwd") } }
        finally { repository.close() }
        assertEquals(true, result.isFailure)
    }

    @Test fun templatesCanCreateProfilesAndCannotBeDeletedWhileInUse() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        delay(100)
        val raw = """
            mixed-port: 7891
            proxies:
            - {name: Tokyo, type: ss, server: tokyo.example, port: 443, cipher: aes-128-gcm, password: secret}
            rule-providers:
              ads:
                type: http
                behavior: domain
                url: https://rules.example/ads.yaml
            proxy-groups:
              - {name: PROXY, type: select, proxies: [Tokyo, DIRECT]}
            rules:
              - RULE-SET,ads,PROXY
              - MATCH,PROXY
        """.trimIndent()
        try {
            repository.saveTemplate("Tokyo base", raw, "tokyo.yaml")
            val templateId = repository.state.value.templates.single().id
            repository.createProfileFromTemplate("Tokyo copy", "tokyo-copy.yaml", templateId)
            val profile = repository.state.value.activeProfile

            assertEquals(templateId, profile.templateId)
            assertEquals(1, repository.state.value.nodes.size)
            assertEquals(setOf(repository.state.value.nodes.single().id), profile.enabledNodeIds)
            assertEquals(templateId, profile.ruleProfile.providers.single().sourceTemplateId)
            assertEquals("Tokyo base", profile.ruleProfile.providers.single().sourceTemplateName)
            assertTrue(runCatching { repository.deleteTemplate(templateId) }.isFailure)

            repository.deleteProfile(profile.id)
            repository.renameTemplate(templateId, "Tokyo template")
            repository.deleteTemplate(templateId)
            assertEquals(emptyList<Any>(), repository.state.value.templates)
        } finally {
            repository.close()
        }
    }

    @Test fun oldRecipeDataIsIgnoredAndRemovedOnNextSnapshotSave() = runBlocking {
        val legacyJson = JsonParser.parseString(Gson().toJson(AppState())).asJsonObject.apply {
            add("ruleRecipes", JsonParser.parseString("[]"))
            remove("ruleProviderStatuses")
        }.toString()
        val dao = InMemorySnapshotDao(legacyJson)
        val repository = JichangRepository(dao)
        delay(100)
        try {
            assertTrue(repository.state.value.ruleProviderStatuses.isEmpty())
            val profile = repository.state.value.activeProfile
            repository.updateExportSettings(profile.sourceMode, profile.enabledRegions, profile.regionOverrides)
            assertTrue(!JsonParser.parseString(dao.readStored()).asJsonObject.has("ruleRecipes"))
        } finally {
            repository.close()
        }
    }

    @Test fun ruleProviderStatusIsStoredLocallyAndCanKeepLastSuccessOnFailure() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        delay(100)
        try {
            val status = RuleProviderStatus("default", "provider-1", 1234L, itemCount = 18, cacheFileName = "provider-1.cache")
            repository.updateRuleProviderStatus(status)
            repository.updateRuleProviderStatus(status.copy(error = "HTTP 503"))
            assertEquals(status.copy(error = "HTTP 503"), repository.state.value.ruleProviderStatuses.single())
        } finally {
            repository.close()
        }
    }

    @Test fun renamingAndDeletingStrategyGroupsUpdatesNestedRules() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        delay(100)
        try {
            repository.addGroup("BACKUP", "select", emptyList())
            repository.saveRuleProfile(RuleProfile(
                groups = listOf(
                    com.jzb.jichang.android.model.PolicyGroup("PROXY"),
                    com.jzb.jichang.android.model.PolicyGroup("BACKUP"),
                    com.jzb.jichang.android.model.PolicyGroup("WRAPPER", members = listOf("PROXY")),
                ),
                rules = listOf(RoutingRule("DOMAIN", "example.com", "PROXY")),
                subRules = listOf(SubRuleProfile("local", listOf(RoutingRule("DOMAIN", "local.test", "PROXY")))),
            ))
            repository.updateGroup("PROXY", "FAST", "select", emptyList())
            assertEquals("FAST", repository.state.value.ruleProfile.rules.single().group)
            assertEquals("FAST", repository.state.value.ruleProfile.subRules.single().rules.single().group)
            assertEquals(listOf("FAST"), repository.state.value.ruleProfile.groups.single { it.name == "WRAPPER" }.members)
            repository.removeGroup("FAST")
            assertEquals("BACKUP", repository.state.value.ruleProfile.rules.single().group)
            assertEquals("BACKUP", repository.state.value.ruleProfile.subRules.single().rules.single().group)
            assertTrue(repository.state.value.ruleProfile.groups.single { it.name == "WRAPPER" }.members.isEmpty())
        } finally {
            repository.close()
        }
    }

    @Test fun creatingStrategyGroupUpdatesSelectedRulesAndAddsRuleSetRoutesBeforeMatch() = runBlocking {
        val node = ProxyNode("node-1", null, "one", "ss", "node.example", 443)
        val provider = RuleProvider("provider-1", "ads", type = "http", url = "https://rules.example/ads.yaml")
        val profile = com.jzb.jichang.android.model.ConfigProfile(
            "default", "默认配置", enabledNodeIds = setOf(node.id),
            ruleProfile = RuleProfile(
                groups = listOf(com.jzb.jichang.android.model.PolicyGroup("PROXY")),
                rules = listOf(RoutingRule("DOMAIN", "example.com", "PROXY"), RoutingRule("MATCH", "MATCH", "PROXY")),
                providers = listOf(provider),
            ),
        )
        val repository = JichangRepository(InMemorySnapshotDao(Gson().toJson(AppState(nodes = listOf(node), profiles = listOf(profile)))))
        delay(100)
        try {
            repository.addGroup("FAST", "select", listOf("node:${node.id}", "PROXY"), setOf(0), setOf(provider.id))
            val updated = repository.state.value.ruleProfile
            assertTrue(updated.groups.last().membersExplicit)
            assertEquals(listOf("node:${node.id}", "PROXY"), updated.groups.last().members)
            assertEquals("FAST", updated.rules[0].group)
            assertEquals(RoutingRule("RULE-SET", "ads", "FAST"), updated.rules[1])
            assertEquals("MATCH", updated.rules.last().type)
        } finally {
            repository.close()
        }
    }

    private class InMemorySnapshotDao(initial: String? = null) : SnapshotDao {
        private val payload = MutableStateFlow(initial)
        override fun observe(): Flow<String?> = payload
        override suspend fun save(snapshot: SnapshotEntity) { payload.value = snapshot.payload }
        fun readStored(): String = payload.value.orEmpty()
    }
}
