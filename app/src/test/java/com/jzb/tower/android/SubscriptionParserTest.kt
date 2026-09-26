package com.jzb.tower.android

import com.jzb.tower.android.service.SubscriptionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SubscriptionParserTest {
    private val parser = SubscriptionParser()

    @Test fun parsesCommonShareUrisAndCountsUnsupportedLines() {
        val result = parser.parse(
            """
                trojan://secret@example.com:443?sni=example.org#Tokyo
                vless://00000000-0000-0000-0000-000000000001@edge.example:8443?encryption=none&security=tls&type=ws&host=cdn.example&path=%2Fedge#VLESS
                ss://YWVzLTI1Ni1nY206cGFzcw==@cipher.example:8388#SS
                unsupported://nope
            """.trimIndent()
        )

        assertEquals(3, result.nodes.size)
        assertEquals(1, result.skippedCount)
        assertEquals(setOf("trojan", "vless", "ss"), result.nodes.map { it.type }.toSet())
        assertEquals("Tokyo", result.nodes.first().name)
        assertEquals("cdn.example", result.nodes[1].options["ws-opts"].let { it as Map<*, *> }["headers"].let { it as Map<*, *> }["Host"])
    }

    @Test fun decodesBase64SubscriptionAndParsesClashYaml() {
        val uri = "trojan://secret@node.example:443#base64-node"
        val encoded = Base64.getEncoder().encodeToString(uri.toByteArray())
        assertEquals("base64-node", parser.parse(encoded).nodes.single().name)

        val result = parser.parse(
            """
                proxies:
                  - name: "Quoted: node"
                    type: ss
                    server: 127.0.0.1
                    port: 8388
                    cipher: aes-128-gcm
                    password: "with: punctuation"
                  - name: broken
                    type: mystery
            """.trimIndent()
        )
        assertEquals(1, result.nodes.size)
        assertEquals("Quoted: node", result.nodes.single().name)
        assertEquals(1, result.skippedCount)
    }

    @Test fun parsesVmessJsonShareLink() {
        val payload = """{"v":"2","ps":"vm","add":"vm.example","port":"443","id":"id-1","aid":"0","net":"ws","host":"cdn.example","path":"/ws","tls":"tls"}"""
        val link = "vmess://" + Base64.getEncoder().withoutPadding().encodeToString(payload.toByteArray())
        val node = parser.parse(link).nodes.single()
        assertEquals("vmess", node.type)
        assertEquals("vm", node.name)
        assertEquals(true, node.options["tls"])
        assertTrue(node.options.containsKey("ws-opts"))
    }

    @Test fun preservesHysteria2AndSurgeFieldsAndSkipsPluginLinks() {
        val result = parser.parse(
            """
                hysteria2://secret@hy.example:443?sni=edge.example&obfs=salamander#hy2
                SurgeSS = ss, ss.example, 8388, encrypt-method=aes-128-gcm, password=secret
                ss://YWVzLTI1Ni1nY206cGFzcw==@ss.example:443/?plugin=v2ray-plugin%3Btls%3Bhost%3Dcdn.example
            """.trimIndent()
        )
        assertEquals(2, result.nodes.size)
        assertEquals(1, result.skippedCount)
        assertEquals("hysteria2", result.nodes[0].type)
        assertEquals("salamander", result.nodes[0].options["obfs"])
        assertEquals("ss", result.nodes[1].type)
        assertEquals("aes-128-gcm", result.nodes[1].options["cipher"])
    }
}
