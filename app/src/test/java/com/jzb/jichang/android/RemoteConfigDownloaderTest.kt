package com.jzb.jichang.android

import com.jzb.jichang.android.service.RemoteConfigDownloader
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class RemoteConfigDownloaderTest {
    private lateinit var server: StubHttpServer

    @Before fun startServer() {
        server = StubHttpServer().apply { start() }
    }

    @After fun stopServer() {
        server.close()
    }

    @Test fun followsRedirectPreservesYamlBytesAndUsesResponseFilename() {
        val body = "\uFEFF# Mihomo\nmode: rule\n".toByteArray(StandardCharsets.UTF_8)
        server.respond("/redirect", 302, headers = mapOf("Location" to "/config.yaml"))
        server.respond("/config.yaml", 200, body, mapOf(
            "Content-Type" to "application/yaml",
            "Content-Disposition" to "attachment; filename*=UTF-8''my%20mihomo.yaml",
        ))
        var progressSeen = false

        val result = RemoteConfigDownloader().download(server.url("/redirect")) { progressSeen = true }

        assertArrayEquals(body, result.bytes)
        assertEquals("my mihomo.yaml", result.fileName)
        assertTrue(progressSeen)
    }

    @Test fun customNameOverridesServerFilenameAndIsSanitized() {
        val body = "mode: rule\n".toByteArray()
        server.respond("/file", 200, body, mapOf("Content-Disposition" to "attachment; filename=ignored.yaml"))

        val result = RemoteConfigDownloader().download(server.url("/file"), "../个人:配置.yml")

        assertEquals("个人_配置.yaml", result.fileName)
        assertArrayEquals(body, result.bytes)
    }

    @Test fun fallsBackToUrlNameThenGenericName() {
        assertEquals("remote.yaml", RemoteConfigDownloader.resolveFileName("", null, "remote.yml"))
        assertEquals("远程配置.yaml", RemoteConfigDownloader.resolveFileName("", null, ""))
        assertEquals("named.yaml", RemoteConfigDownloader.resolveFileName("named.yaml", "attachment; filename=server.yaml", "other.yaml"))
    }

    @Test fun rejectsInvalidLinksHttpErrorsEmptyFilesAndHtmlPages() {
        server.respond("/missing", 404)
        server.respond("/empty", 200)
        server.respond("/login", 200, "<html><body>login</body></html>".toByteArray(), mapOf("Content-Type" to "text/html"))
        val downloader = RemoteConfigDownloader()

        assertTrue(runCatching { downloader.download("file:///tmp/config.yaml") }.exceptionOrNull()?.message.orEmpty().contains("HTTP(S)"))
        assertTrue(runCatching { downloader.download(server.url("/missing")) }.exceptionOrNull()?.message.orEmpty().contains("404"))
        assertTrue(runCatching { downloader.download(server.url("/empty")) }.exceptionOrNull()?.message.orEmpty().contains("为空"))
        assertTrue(runCatching { downloader.download(server.url("/login")) }.exceptionOrNull()?.message.orEmpty().contains("网页"))
    }

    private class StubHttpServer : AutoCloseable {
        private val socket = ServerSocket(0)
        private val responses = ConcurrentHashMap<String, StubResponse>()
        @Volatile private var running = false
        private val worker = Thread(::serve).apply { isDaemon = true }

        fun start() {
            running = true
            worker.start()
        }

        fun respond(path: String, code: Int, body: ByteArray = byteArrayOf(), headers: Map<String, String> = emptyMap()) {
            responses[path] = StubResponse(code, body, headers)
        }

        fun url(path: String) = "http://127.0.0.1:${socket.localPort}$path"

        private fun serve() {
            while (running) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                Thread({ handle(client) }, "jichang-test-http").apply { isDaemon = true; start() }
            }
        }

        private fun handle(client: Socket) {
            client.use { connection ->
                val input = connection.getInputStream().bufferedReader(StandardCharsets.ISO_8859_1)
                val path = input.readLine()?.split(' ')?.getOrNull(1)?.substringBefore('?') ?: return
                while (input.readLine()?.isNotEmpty() == true) Unit
                val response = responses[path] ?: StubResponse(404, byteArrayOf(), emptyMap())
                val output = connection.getOutputStream()
                val reason = when (response.code) { 200 -> "OK"; 302 -> "Found"; 404 -> "Not Found"; else -> "Error" }
                val headers = response.headers + mapOf("Content-Length" to response.body.size.toString(), "Connection" to "close")
                output.write("HTTP/1.1 ${response.code} $reason\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                headers.forEach { (name, value) -> output.write("$name: $value\r\n".toByteArray(StandardCharsets.ISO_8859_1)) }
                output.write("\r\n".toByteArray(StandardCharsets.ISO_8859_1))
                output.write(response.body)
                output.flush()
            }
        }

        override fun close() {
            running = false
            socket.close()
            worker.join(1_000)
        }
    }

    private data class StubResponse(val code: Int, val body: ByteArray, val headers: Map<String, String>)
}
