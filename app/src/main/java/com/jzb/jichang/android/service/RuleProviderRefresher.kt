package com.jzb.jichang.android.service

import com.jzb.jichang.android.model.RuleProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.util.concurrent.TimeUnit

data class RefreshedRuleProvider(val bytes: ByteArray, val cacheFileName: String, val itemCount: Int?)

class RuleProviderRefresher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    fun preview(provider: RuleProvider, cacheDirectory: File, cachedFileName: String?): String {
        val bytes = when (provider.type.lowercase()) {
            "inline" -> provider.payload.joinToString("\n").toByteArray(Charsets.UTF_8)
            "http" -> cachedFileName?.let { readCached(cacheDirectory, it) } ?: error("尚无本地缓存，请先刷新规则集")
            "file" -> readLocal(provider, cacheDirectory)
            else -> error("未知规则集来源类型：${provider.type}")
        }
        if (provider.format.equals("mrs", true)) return "MRS 是二进制格式，已缓存 ${bytes.size} 字节；此格式无法在规则页逐行预览。"
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        return text.lineSequence().take(MAX_PREVIEW_LINES).joinToString("\n").ifBlank { "规则集内容为空。" }
    }

    /** Reads only local content. A missing cache or an MRS binary remains unknown to the simulator. */
    fun simulationEntries(provider: RuleProvider, cacheDirectory: File, cachedFileName: String?): List<String>? {
        if (provider.format.equals("mrs", true)) return null
        val bytes = when (provider.type.lowercase()) {
            "inline" -> return provider.payload
            "http" -> cachedFileName?.let { readCached(cacheDirectory, it) } ?: return null
            "file" -> readLocal(provider, cacheDirectory)
            else -> return null
        }
        require(bytes.size <= MAX_BYTES) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
        val content = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val entries = when (provider.format.lowercase()) {
            "text" -> content.lineSequence().map(String::trim).filter { it.isNotBlank() && !it.startsWith("#") }.toList()
            "yaml" -> {
                val options = LoaderOptions().apply { maxAliasesForCollections = 30; codePointLimit = MAX_BYTES }
                val loaded = Yaml(SafeConstructor(options)).load<Any?>(content)
                val payload = when (loaded) {
                    is Map<*, *> -> loaded["payload"]
                    is List<*> -> loaded
                    else -> null
                }
                (payload as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
                    ?: error("YAML 规则集缺少 payload 列表")
            }
            else -> return null
        }
        return entries
    }

    suspend fun refresh(provider: RuleProvider, cacheDirectory: File): RefreshedRuleProvider {
        var temporaryCache: File? = null
        var createdCache: File? = null
        try {
            return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val bytes = when (provider.type.lowercase()) {
                    "http" -> fetch(provider)
                    "inline" -> provider.payload.joinToString("\n").toByteArray(Charsets.UTF_8)
                    "file" -> readLocal(provider, cacheDirectory)
                    else -> error("未知规则集来源类型：${provider.type}")
                }
                require(bytes.isNotEmpty()) { "规则集内容为空" }
                require(bytes.size <= MAX_BYTES) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
                val safeName = provider.id.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "provider" } + "-${java.util.UUID.randomUUID()}.cache"
                if (provider.type.equals("http", true)) {
                    cacheDirectory.mkdirs()
                    val temporary = File(cacheDirectory, "$safeName.tmp").also { temporaryCache = it }
                    val destination = File(cacheDirectory, safeName).also { createdCache = it }
                    temporary.writeBytes(bytes)
                    check(temporary.renameTo(destination) || temporary.copyTo(destination, overwrite = true).delete()) { "无法保存规则集缓存" }
                }
                RefreshedRuleProvider(bytes, safeName, countItems(bytes, provider.format))
            }
        } catch (error: Exception) {
            // This also handles cancellation at the dispatcher return boundary.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                temporaryCache?.delete()
                createdCache?.delete()
            }
            throw error
        }
    }

    fun readCached(cacheDirectory: File, cacheFileName: String): ByteArray? =
        runCatching { File(cacheDirectory, cacheFileName).takeIf { it.isFile && it.canonicalFile.parentFile == cacheDirectory.canonicalFile }?.readBytes() }.getOrNull()

    private suspend fun fetch(provider: RuleProvider): ByteArray {
        val url = provider.url.trim().toHttpUrlOrNull() ?: error("规则集 URL 无效")
        require(url.scheme in setOf("http", "https")) { "规则集只支持 HTTP(S) URL" }
        val request = Request.Builder().url(url).header("User-Agent", "JichangAndroid/0.11.0").apply {
            provider.headers.forEach { (name, values) -> values.forEach { value -> addHeader(name, value) } }
        }.get().build()
        return client.newCall(request).readCancellable { response ->
            if (!response.isSuccessful) error("远程服务器返回 HTTP ${response.code}")
            val body = response.body ?: error("远程服务器没有返回规则集内容")
            require(body.contentLength() <= MAX_BYTES || body.contentLength() < 0) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
            val output = java.io.ByteArrayOutputStream()
            body.byteStream().use { input ->
                val chunk = ByteArray(8192)
                while (true) {
                    val count = input.read(chunk)
                    if (count < 0) break
                    require(output.size() + count <= MAX_BYTES) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
                    output.write(chunk, 0, count)
                }
            }
            val bytes = output.toByteArray()
            require(bytes.size <= MAX_BYTES) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
            bytes
        }
    }

    private fun readLocal(provider: RuleProvider, cacheDirectory: File): ByteArray {
        val root = cacheDirectory.canonicalFile
        val relative = provider.path.removePrefix("./").removePrefix("rules/")
        val file = File(root, relative).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) { "本地规则集路径必须位于应用规则集目录内" }
        require(file.isFile) { "找不到本地规则集文件：${provider.path}" }
        require(file.length() <= MAX_BYTES) { "规则集超过 ${MAX_BYTES / (1024 * 1024)} MB 限制" }
        return file.readBytes()
    }

    private fun countItems(bytes: ByteArray, format: String): Int? {
        if (format.equals("mrs", true)) return null
        val text = runCatching { bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF") }.getOrNull() ?: return null
        return text.lineSequence().map(String::trim)
            .filter { line -> line.isNotBlank() && !line.startsWith("#") && line != "payload:" && line != "rules:" }
            .count()
    }

    companion object { const val MAX_BYTES = 20 * 1024 * 1024; const val MAX_PREVIEW_LINES = 300 }
}
