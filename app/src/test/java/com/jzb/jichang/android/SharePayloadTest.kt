package com.jzb.jichang.android

import com.jzb.jichang.android.share.SharePayload
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class SharePayloadTest {
    @Test fun gzipPayloadDecodesToExactYamlAndIsCached() {
        val yaml = ("proxies:\n" + "  - name: node\n    type: ss\n    server: example.com\n    password: secret\n".repeat(300))
        val payload = SharePayload(yaml, "profile.yaml")
        val first = payload.encode(acceptsGzip = true)
        val second = payload.encode(acceptsGzip = true)

        assertTrue(first.compressed)
        assertArrayEquals(yaml.toByteArray(), GZIPInputStream(ByteArrayInputStream(first.bytes)).use { it.readBytes() })
        assertArrayEquals(first.bytes, second.bytes)
    }

    @Test fun plainClientAndSmallYamlReceiveUncompressedBytes() {
        val plainYaml = "mixed-port: 7890\nrules:\n  - MATCH,DIRECT\n"
        assertFalse(SharePayload(plainYaml, "a.yaml").encode(acceptsGzip = true).compressed)
        val yaml = "x".repeat(4096)
        val response = SharePayload(yaml, "a.yaml").encode(acceptsGzip = false)
        assertFalse(response.compressed)
        assertArrayEquals(yaml.toByteArray(), response.bytes)
    }
}
