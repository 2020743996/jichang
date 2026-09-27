package com.jzb.jichang.android

import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ConfigProfile
import com.jzb.jichang.android.model.PolicyGroup
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.RoutingRule
import com.jzb.jichang.android.model.RuleCondition
import com.jzb.jichang.android.model.RuleProvider
import com.jzb.jichang.android.model.RuleProfile
import com.jzb.jichang.android.model.SubscriptionSource
import com.jzb.jichang.android.model.ConfigTemplate
import com.jzb.jichang.android.service.ConfigExportOptions
import com.jzb.jichang.android.service.ConfigSourceMode
import com.jzb.jichang.android.service.MihomoConfigGenerator
import com.jzb.jichang.android.service.MihomoTemplateParser
import com.jzb.jichang.android.service.NodeAutoGroups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class MihomoConfigGeneratorTest {
    @Test fun clashMaxTemplateFiltersInlineNodesAndDropsPlaceholderWithoutSubscription() {
        val template = ConfigTemplate("clash-max", "Clash Max", clashMaxTemplate, "Clash_Max.yaml")
        val hk = ProxyNode("hk", null, "🇭🇰 香港 01", "ss", "hk.example", 443)
        val us = ProxyNode("us", null, "🇺🇸 洛杉矶 01", "ss", "us.example", 443)
        val jp = ProxyNode("jp", null, "🇯🇵 日本 01", "ss", "jp.example", 443)
        val parsed = MihomoTemplateParser().parse(template.rawYaml)
        val profile = ConfigProfile(
            id = "clash-max-profile", name = "Clash Max", templateId = template.id,
            enabledNodeIds = setOf(hk.id, us.id, jp.id), ruleProfile = parsed.ruleProfile,
        )
        val state = AppState(nodes = listOf(hk, us, jp), profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))

        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hkGroup = groups.single { it["name"] == "🇭🇰 香港节点" }
        val usGroup = groups.single { it["name"] == "🇺🇸 美国节点" }

        assertEquals(listOf("🇭🇰 香港 01"), hkGroup["proxies"])
        assertEquals(listOf("🇺🇸 洛杉矶 01"), usGroup["proxies"])
        assertTrue(root["proxy-providers"] == null)
        assertFalse(hkGroup.containsKey("include-all-providers"))
        assertFalse(usGroup.containsKey("include-all-providers"))
        assertEquals(emptyList<String>(), output.unresolvedTemplateProviders)
        assertFalse(output.yaml.contains("机场的订阅地址"))
    }

    @Test fun aSingleSelectedMihomoSubscriptionAutomaticallyBindsTemplateProvider() {
        val source = SubscriptionSource("only", "唯一机场", "https://sub.example/config", providerCompatible = true)
        val template = ConfigTemplate("clash-max", "Clash Max", clashMaxTemplate, "Clash_Max.yaml")
        val node = ProxyNode("hk", source.id, "香港 01", "ss", "hk.example", 443)
        val ruleProfile = MihomoTemplateParser().parse(template.rawYaml).ruleProfile
        val profile = ConfigProfile(
            id = "profile", name = "配置", templateId = template.id,
            selectedSourceIds = setOf(source.id), enabledNodeIds = setOf(node.id),
            sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS.name, ruleProfile = ruleProfile,
        )
        val state = AppState(listOf(source), listOf(node), listOf(profile), profile.id, listOf(template))
        val output = MihomoConfigGenerator().generate(state)
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml.byteInputStream())
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>

        assertEquals("https://sub.example/config", providers.getValue("我的节点")["url"])
        assertTrue(output.unresolvedTemplateProviders.isEmpty())
        assertFalse(output.yaml.contains("机场的订阅地址"))
    }

    @Test fun multipleSelectedSubscriptionsRequireExplicitTemplateBinding() {
        val first = SubscriptionSource("one", "机场一", "https://one.example/config", providerCompatible = true)
        val second = SubscriptionSource("two", "机场二", "https://two.example/config", providerCompatible = true)
        val template = ConfigTemplate("clash-max", "Clash Max", clashMaxTemplate, "Clash_Max.yaml")
        val profile = ConfigProfile(
            id = "profile", name = "配置", templateId = template.id,
            selectedSourceIds = setOf(first.id, second.id),
            sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS.name,
            ruleProfile = MihomoTemplateParser().parse(template.rawYaml).ruleProfile,
        )
        val state = AppState(sources = listOf(first, second), profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))
        val output = MihomoConfigGenerator().generate(state)

        assertEquals(listOf("我的节点"), output.unresolvedTemplateProviders)
        assertFalse(output.yaml.contains("机场的订阅地址"))
        assertFalse((output.yaml.substringAfter("proxy-providers:").substringBefore("proxy-groups:" )).contains("我的节点:"))
    }

    @Test fun bindsTemplateProviderAndSuppressesDuplicateRegionalGroups() {
        val source = SubscriptionSource("source", "机场订阅", "https://sub.example/user/token", providerCompatible = true)
        val node = ProxyNode("hk-node", source.id, "香港 IEPL", "ss", "hk.example", 443)
        val template = ConfigTemplate("template", "模板", regionalTemplate, "template.yaml")
        val profile = ConfigProfile(
            id = "profile", name = "分享配置", templateId = template.id,
            selectedSourceIds = setOf(source.id), enabledNodeIds = setOf(node.id),
            sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS.name,
            templateProviderBindings = mapOf("我的节点" to source.id),
            ruleProfile = MihomoTemplateParser().parse(regionalTemplate).ruleProfile,
        )
        val state = AppState(sources = listOf(source), nodes = listOf(node), profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))

        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>

        assertEquals("https://sub.example/user/token", providers.getValue("我的节点")["url"])
        assertEquals(listOf("我的节点"), groups.single { it["name"] == "香港自动选择" }["use"])
        assertEquals(0, groups.count { it["name"].toString().startsWith("🌏") })
        assertTrue(output.unresolvedTemplateProviders.isEmpty())
        assertFalse(output.yaml.contains("机场的订阅地址"))
    }

    @Test fun embeddedTemplateRemovesPlaceholderAndKeepsRegionalFilterOnLocalNodes() {
        val source = SubscriptionSource("source", "机场订阅", "https://sub.example/user/token", providerCompatible = true)
        val node = ProxyNode("hk-node", source.id, "香港 IEPL", "ss", "hk.example", 443)
        val template = ConfigTemplate("template", "模板", regionalTemplate, "template.yaml")
        val profile = ConfigProfile(
            id = "profile", name = "分享配置", templateId = template.id,
            selectedSourceIds = setOf(source.id), enabledNodeIds = setOf(node.id),
            sourceMode = ConfigSourceMode.EMBED_NODES.name,
            templateProviderBindings = mapOf("我的节点" to source.id),
            ruleProfile = MihomoTemplateParser().parse(regionalTemplate).ruleProfile,
        )
        val state = AppState(sources = listOf(source), nodes = listOf(node), profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))
        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hk = groups.single { it["name"] == "香港自动选择" }

        assertTrue(root["proxy-providers"] == null)
        assertFalse(output.yaml.contains("机场的订阅地址"))
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("香港 IEPL"), hk["proxies"])
        assertFalse(hk.containsKey("include-all"))
        assertEquals("香港", hk["filter"])
        assertEquals(0, groups.count { it["name"].toString().startsWith("🌏") })
    }

    @Test fun templatePlaceholderIsRemovedWhenNoSubscriptionIsSelected() {
        val template = ConfigTemplate("template", "模板", regionalTemplate, "template.yaml")
        val profile = ConfigProfile(id = "profile", name = "配置", templateId = template.id, sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS.name)
        val state = AppState(profiles = listOf(profile), activeProfileId = profile.id, templates = listOf(template))
        val output = MihomoConfigGenerator().generate(state)
        assertTrue(output.unresolvedTemplateProviders.isEmpty())
        assertFalse(output.yaml.contains("机场的订阅地址"))
        assertFalse(output.yaml.contains("我的节点:"))
    }

    @Test fun emitsParsableMihomoYamlWithGroupsRulesAndOnlyEnabledNodes() {
        val node = ProxyNode(
            id = "1", sourceId = null, name = "Node: one", type = "trojan", server = "node.example", port = 443,
            options = mapOf("type" to "trojan", "password" to "secret", "tls" to true),
        )
        val disabled = node.copy(id = "2", name = "disabled", enabled = false)
        val state = profileState(
            nodes = listOf(node, disabled),
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY", members = listOf("Node: one"))),
                rules = listOf(RoutingRule("DOMAIN-SUFFIX", "example.com", "PROXY")),
            ),
        )

        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        val proxies = root["proxies"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>

        assertEquals(1, output.exportedNodes)
        assertEquals(0, output.skippedNodes)
        assertEquals("Node: one", proxies.single()["name"])
        assertEquals(listOf("DOMAIN-SUFFIX,example.com,PROXY", "MATCH,PROXY"), rules)
    }

    @Test fun explicitlyEmptyStrategyGroupDoesNotExpandToAllEnabledNodes() {
        val node = ProxyNode("node-1", null, "Node one", "ss", "node.example", 443)
        val state = profileState(
            nodes = listOf(node),
            ruleProfile = RuleProfile(groups = listOf(PolicyGroup("EMPTY", membersExplicit = true))),
        )

        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>

        assertEquals(listOf("DIRECT"), groups.single { it["name"] == "EMPTY" }["proxies"])
    }

    @Test fun sanitizesUntrustedNamesAndCountsUnsupportedProtocols() {
        val unsafe = ProxyNode("1", null, "safe\nMATCH,example.org,DIRECT", "ss", "node.example", 443)
        val unsupported = ProxyNode("2", null, "unsupported", "ssr", "node.example", 443)
        val output = MihomoConfigGenerator().generate(profileState(nodes = listOf(unsafe, unsupported)))
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        val proxies = root["proxies"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>

        assertEquals(1, output.exportedNodes)
        assertEquals(1, output.skippedNodes)
        assertFalse(proxies.single()["name"].toString().contains('\n'))
        assertTrue(rules.none { it.startsWith("MATCH,example.org") })
    }

    @Test fun buildsRegionGroupsAndKeepsMihomoMatchAsLastRule() {
        val hongKong = ProxyNode("hk", null, "香港 01", "ss", "hk.example", 443)
        val japan = ProxyNode("jp", null, "Tokyo JP", "trojan", "jp.example", 443)
        val state = profileState(
            nodes = listOf(hongKong, japan),
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY")),
                rules = listOf(
                    RoutingRule("IP-CIDR", "10.0.0.0/8", "DIRECT", noResolve = true),
                    RoutingRule("MATCH", "", "DIRECT"),
                ),
            ),
        )
        val output = MihomoConfigGenerator().generate(state, ConfigExportOptions(enabledRegions = setOf("hk", "jp", NodeAutoGroups.OTHER)))
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hongKongGroup = groups.single { it["name"] == "🌏 香港" }
        val rules = root["rules"] as List<String>

        assertEquals(listOf("香港 01"), hongKongGroup["proxies"])
        assertEquals(listOf("🌏 香港", "🌏 日本", "🌏 其他"), groups.first { it["name"] == "PROXY" }["proxies"])
        assertEquals("IP-CIDR,10.0.0.0/8,DIRECT,no-resolve", rules[0])
        assertEquals("MATCH,DIRECT", rules.last())
        assertEquals("香港", NodeAutoGroups.title(NodeAutoGroups.classify("HK 香港线路")))
    }

    @Test fun referencesOnlyMihomoSubscriptionsAndKeepsManualNodesInline() {
        val source = SubscriptionSource("source-1", "机场", "https://sub.example/token", providerCompatible = true)
        val textSource = SubscriptionSource("source-2", "旧订阅", "https://sub.example/text", providerCompatible = false)
        val hk = ProxyNode("hk", source.id, "香港高速", "ss", "hk.example", 443)
        val local = ProxyNode("local", null, "自建节点", "trojan", "local.example", 443)
        val parsedText = ProxyNode("text", textSource.id, "日本线路", "vmess", "jp.example", 443)
        val state = profileState(sources = listOf(source, textSource), nodes = listOf(hk, local, parsedText))

        val output = MihomoConfigGenerator().generate(
            state,
            ConfigExportOptions(sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, excludedNodeIds = setOf(hk.id), selectedSourceIds = setOf(source.id, textSource.id), enabledNodeIds = setOf(hk.id, local.id, parsedText.id)),
        )
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val proxies = root["proxies"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val groups = root["proxy-groups"] as List<Map<String, Any?>>
        val hongKongGroup = groups.single { it["name"] == "🌏 香港" }

        assertEquals(1, output.referencedSubscriptions)
        assertEquals(setOf("自建节点", "日本线路"), proxies.map { it["name"] }.toSet())
        assertEquals("https://sub.example/token", providers.getValue("订阅-1")["url"])
        assertEquals(listOf("订阅-1"), hongKongGroup["use"])
        assertTrue(hongKongGroup["exclude-filter"].toString().contains("香港高速"))
    }

    @Test fun providerNumberingSkipsSubscriptionsThatCannotBeReferenced() {
        val textSource = SubscriptionSource("text", "普通订阅", "https://sub.example/text", providerCompatible = false)
        val mihomoSource = SubscriptionSource("mihomo", "Mihomo 订阅", "https://sub.example/mihomo", providerCompatible = true)
        val state = profileState(
            sources = listOf(textSource, mihomoSource),
            nodes = listOf(
                ProxyNode("text-node", textSource.id, "手动回退节点", "ss", "text.example", 443),
                ProxyNode("mihomo-node", mihomoSource.id, "Mihomo 节点", "ss", "mihomo.example", 443),
            ),
        )

        val output = MihomoConfigGenerator().generate(
            state,
            ConfigExportOptions(sourceMode = ConfigSourceMode.REFERENCE_SUBSCRIPTIONS, selectedSourceIds = setOf(textSource.id, mihomoSource.id), enabledNodeIds = setOf("text-node", "mihomo-node")),
        )
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        @Suppress("UNCHECKED_CAST")
        val providers = root["proxy-providers"] as Map<String, Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val proxies = root["proxies"] as List<Map<String, Any?>>

        assertEquals(setOf("订阅-1"), providers.keys)
        assertEquals("https://sub.example/mihomo", providers.getValue("订阅-1")["url"])
        assertEquals(setOf("手动回退节点"), proxies.map { it["name"] }.toSet())
    }

    @Test fun profileSelectionIsIndependentAndVisualRuleTypesSerialize() {
        val firstNode = ProxyNode("first", null, "东京线路", "ss", "tokyo.example", 443)
        val secondNode = firstNode.copy(id = "second", name = "香港线路", server = "hk.example")
        val profileA = ConfigProfile(
            id = "a", name = "A", enabledNodeIds = setOf(firstNode.id),
            ruleProfile = RuleProfile(
                groups = listOf(PolicyGroup("PROXY")),
                providers = listOf(RuleProvider("ads", "ads", type = "inline", behavior = "domain", payload = listOf("ads.example"))),
                rules = listOf(
                    RoutingRule("RULE-SET", "ads", "REJECT"),
                    RoutingRule("AND", "", "PROXY", conditions = listOf(
                        RuleCondition(type = "DOMAIN-SUFFIX", value = "example.com"),
                        RuleCondition(type = "NETWORK", value = "udp"),
                    )),
                ),
            ),
        )
        val profileB = ConfigProfile(id = "b", name = "B", enabledNodeIds = setOf(secondNode.id))
        val state = AppState(nodes = listOf(firstNode, secondNode), profiles = listOf(profileA, profileB), activeProfileId = profileA.id)
        val output = MihomoConfigGenerator().generate(state)
        @Suppress("UNCHECKED_CAST")
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(output.yaml)
        val proxies = root["proxies"] as List<Map<String, Any?>>
        val rules = root["rules"] as List<String>
        assertEquals(listOf("东京线路"), proxies.map { it["name"] })
        assertTrue(rules.contains("RULE-SET,ads,REJECT"))
        assertTrue(rules.contains("AND,((DOMAIN-SUFFIX,example.com),(NETWORK,udp)),PROXY"))
        assertEquals(listOf("香港线路"), MihomoConfigGenerator().generate(state, profileB).let {
            @Suppress("UNCHECKED_CAST")
            (Yaml(SafeConstructor(LoaderOptions())).load<Map<String, Any?>>(it.yaml)["proxies"] as List<Map<String, Any?>>).map { node -> node["name"] }
        })
    }

    private fun profileState(
        nodes: List<ProxyNode> = emptyList(),
        sources: List<SubscriptionSource> = emptyList(),
        ruleProfile: RuleProfile = RuleProfile(),
    ): AppState {
        val profile = ConfigProfile(
            id = "test", name = "Test", selectedSourceIds = sources.map { it.id }.toSet(),
            enabledNodeIds = nodes.filter { it.enabled }.map { it.id }.toSet(), ruleProfile = ruleProfile,
        )
        return AppState(sources, nodes, listOf(profile), profile.id)
    }

    private val regionalTemplate = """
        mixed-port: 7890
        proxy-providers:
          我的节点:
            type: http
            url: 机场的订阅地址
            path: ./providers/example.yaml
            interval: 3600
        proxy-groups:
          - name: 香港自动选择
            type: url-test
            use: [我的节点]
            filter: 香港
            url: https://www.gstatic.com/generate_204
            interval: 300
        rules:
          - MATCH,香港自动选择
    """.trimIndent()

    private val clashMaxTemplate = """
        mixed-port: 7892
        proxies:
          NodeParam: &NodeParam {type: http, interval: 86400, health-check: {enable: true, url: 'http://connectivitycheck.gstatic.com/generate_204', interval: 60}}
        proxy-providers:
          我的节点:
            url: '机场的订阅地址'
            <<: *NodeParam
            path: './proxy_provider/Providers.yaml'
        FilterHK: &FilterHK '^(?=.*((?i)🇭🇰|香港|(\b(HK|Hong)\b))).*$'
        FilterUS: &FilterUS '^(?=.*((?i)🇺🇸|美国|洛杉矶|(\b(US|United States)\b))).*$'
        FallBack: &FallBack {type: fallback, interval: 5, lazy: true, url: 'http://cp.cloudflare.com/generate_204', include-all-providers: true}
        proxy-groups:
          - {name: Proxy, type: select, proxies: [🇭🇰 香港节点, 🇺🇸 美国节点]}
          - {name: 🇭🇰 香港节点, <<: *FallBack, filter: *FilterHK}
          - {name: 🇺🇸 美国节点, <<: *FallBack, filter: *FilterUS}
        rules:
          - MATCH,Proxy
    """.trimIndent()
}
