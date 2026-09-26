package com.jzb.jichang.android.service

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class RemoteConfigFile(val bytes: ByteArray, val fileName: String)

data class RemoteDownloadProgress(val receivedBytes: Long, val totalBytes: Long?) {
    val fraction: Float? get() = totalBytes?.takeIf { it > 0 }?.let { (receivedBytes.toFloat() / it).coerceIn(0f, 1f) }
}

/** Downloads a remote Mihomo YAML file without importing it into the app's profile data. */
class RemoteConfigDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    fun download(
        url: String,
        requestedFileName: String = "",
        onProgress: (RemoteDownloadProgress) -> Unit = {},
    ): RemoteConfigFile {
        val parsedUrl = url.trim().toHttpUrlOrNull() ?: error("请输入有效的 HTTP(S) 配置链接")
        require(parsedUrl.scheme in setOf("http", "https")) { "仅支持 HTTP 或 HTTPS 链接" }
        require(parsedUrl.host.isNotBlank()) { "配置链接缺少主机名" }
        val request = Request.Builder().url(parsedUrl).header("User-Agent", "JichangAndroid/0.5.0").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("远程服务器返回 HTTP ${response.code}")
            val body = response.body ?: error("远程服务器没有返回文件内容")
            val declaredLength = body.contentLength().takeIf { it >= 0 }
            val output = ByteArrayOutputStream(if (declaredLength != null && declaredLength <= MAX_CONFIG_BYTES) declaredLength.toInt() else 16 * 1024)
            val buffer = Buffer()
            var received = 0L
            val source = body.source()
            while (true) {
                val read = source.read(buffer, 8_192)
                if (read == -1L) break
                received += read
                if (received > MAX_CONFIG_BYTES) error("远程配置超过 ${MAX_CONFIG_BYTES / (1024 * 1024)} MB 限制")
                output.write(buffer.readByteArray(read))
                onProgress(RemoteDownloadProgress(received, declaredLength))
            }
            val bytes = output.toByteArray()
            require(bytes.isNotEmpty()) { "远程文件为空" }
            val contentType = response.header("Content-Type").orEmpty().lowercase()
            val prefix = bytes.take(512).toByteArray().toString(StandardCharsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t').lowercase()
            require(!contentType.contains("text/html") && !prefix.startsWith("<!doctype html") && !prefix.startsWith("<html")) {
                "链接返回了网页而不是 Mihomo YAML 配置"
            }
            val name = resolveFileName(
                requestedFileName = requestedFileName,
                contentDisposition = response.header("Content-Disposition"),
                urlPath = response.request.url.encodedPath.substringAfterLast('/'),
            )
            onProgress(RemoteDownloadProgress(received, declaredLength ?: received))
            return RemoteConfigFile(bytes, name)
        }
    }

    companion object {
        const val MAX_CONFIG_BYTES = 25L * 1024 * 1024

        fun resolveFileName(requestedFileName: String, contentDisposition: String?, urlPath: String): String {
            val preferred = requestedFileName.trim().ifBlank {
                parseContentDispositionFileName(contentDisposition).ifBlank { decodeUrlPath(urlPath) }.ifBlank { "远程配置" }
            }
            val clean = preferred.substringAfterLast('/').substringAfterLast('\\')
                .replace("[\\\\/:*?\"<>|\\p{Cntrl}]".toRegex(), "_")
                .trim('.', ' ', '_')
                .removeSuffix(".yaml").removeSuffix(".yml")
                .ifBlank { "远程配置" }
            return "$clean.yaml"
        }

        private fun parseContentDispositionFileName(header: String?): String {
            if (header.isNullOrBlank()) return ""
            val extended = Regex("(?i)(?:^|;)\\s*filename\\*=\\s*(?:UTF-8''|)([^;]+)")
                .find(header)?.groupValues?.getOrNull(1)?.trim()?.trim('"', '\'')
            if (!extended.isNullOrBlank()) return runCatching { URLDecoder.decode(extended.replace("+", "%2B"), "UTF-8") }.getOrDefault(extended)
            return Regex("(?i)(?:^|;)\\s*filename\\s*=\\s*(?:\"([^\"]+)\"|([^;]+))")
                .find(header)?.let { it.groupValues[1].ifBlank { it.groupValues[2] }.trim() }.orEmpty()
        }

        private fun decodeUrlPath(path: String): String = runCatching { URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") }.getOrDefault(path)
    }
}
