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
            proxy-groups:
              - {name: PROXY, type: select, proxies: [Tokyo, DIRECT]}
            rules:
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
            assertTrue(runCatching { repository.deleteTemplate(templateId) }.isFailure)

            repository.deleteProfile(profile.id)
            repository.renameTemplate(templateId, "Tokyo template")
            repository.deleteTemplate(templateId)
            assertEquals(emptyList<Any>(), repository.state.value.templates)
        } finally {
            repository.close()
        }
    }

    @Test fun ruleRecipesAreSharedAndApplyingSkipsDuplicatesAndKeepsMatchLast() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        delay(100)
        val rules = listOf(
            RoutingRule("GEOSITE", "category-ads-all", "REJECT"),
            RoutingRule("GEOSITE", "cn", "DIRECT"),
            RoutingRule("GEOSITE", "geolocation-!cn", "PROXY"),
            RoutingRule("MATCH", "MATCH", "PROXY"),
        )
        try {
            val id = repository.saveRuleRecipe("基础分流", rules)
            repository.createProfile("第二份", "second", false)
            assertEquals(4 to 0, repository.applyRuleRecipe(repository.state.value.ruleRecipes.single().rules))
            assertEquals("MATCH", repository.state.value.ruleProfile.rules.last().type)
            assertEquals(0 to 4, repository.applyRuleRecipe(repository.state.value.ruleRecipes.single().rules))
            repository.renameRuleRecipe(id, "家庭分流")
            assertEquals("家庭分流", repository.state.value.ruleRecipes.single().name)
            repository.deleteRuleRecipe(id)
            assertTrue(repository.state.value.ruleRecipes.isEmpty())
        } finally {
            repository.close()
        }
    }

    @Test fun recipeWithMissingTargetIsRejectedWithoutChangingRules() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        delay(100)
        try {
            val before = repository.state.value.ruleProfile.rules
            val result = runCatching { repository.applyRuleRecipe(listOf(RoutingRule("DOMAIN", "example.com", "MISSING"))) }
            assertTrue(result.isFailure)
            assertEquals(before, repository.state.value.ruleProfile.rules)
        } finally {
            repository.close()
        }
    }

    @Test fun olderSnapshotsWithoutRecipeLibraryLoadWithAnEmptyLibrary() = runBlocking {
        val legacy = JsonParser.parseString(Gson().toJson(AppState())).asJsonObject.apply { remove("ruleRecipes") }.toString()
        val repository = JichangRepository(InMemorySnapshotDao(legacy))
        delay(100)
        try {
            assertTrue(repository.state.value.ruleRecipes.isEmpty())
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
                groups = listOf(com.jzb.jichang.android.model.PolicyGroup("PROXY"), com.jzb.jichang.android.model.PolicyGroup("BACKUP")),
                rules = listOf(RoutingRule("DOMAIN", "example.com", "PROXY")),
                subRules = listOf(SubRuleProfile("local", listOf(RoutingRule("DOMAIN", "local.test", "PROXY")))),
            ))
            repository.updateGroup("PROXY", "FAST", "select", emptyList())
            assertEquals("FAST", repository.state.value.ruleProfile.rules.single().group)
            assertEquals("FAST", repository.state.value.ruleProfile.subRules.single().rules.single().group)
            repository.removeGroup("FAST")
            assertEquals("BACKUP", repository.state.value.ruleProfile.rules.single().group)
            assertEquals("BACKUP", repository.state.value.ruleProfile.subRules.single().rules.single().group)
        } finally {
            repository.close()
        }
    }

    private class InMemorySnapshotDao(initial: String? = null) : SnapshotDao {
        private val payload = MutableStateFlow(initial)
        override fun observe(): Flow<String?> = payload
        override suspend fun save(snapshot: SnapshotEntity) { payload.value = snapshot.payload }
    }
}
