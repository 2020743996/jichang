package com.jzb.jichang.android.service

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Portable, unencrypted backup shared with the native macOS app. */
object PortableBackup {
    private const val FORMAT = "jichang-backup"
    private const val VERSION = 1
    private const val MAX_UNCOMPRESSED_BYTES = 256L * 1024 * 1024
    private const val STATE_PATH = "state.json"
    private const val MANIFEST_PATH = "manifest.json"

    data class Contents(val stateJson: String, val cacheFiles: Map<String, ByteArray>)

    fun create(stateJson: String, cacheRoot: File): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_PATH))
            zip.write("{\"format\":\"$FORMAT\",\"version\":$VERSION}".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(STATE_PATH))
            zip.write(stateJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            if (cacheRoot.isDirectory) {
                cacheRoot.walkTopDown().filter(File::isFile).forEach { file ->
                    val relative = file.relativeTo(cacheRoot).invariantSeparatorsPath
                    if (isSafeRelativePath(relative)) {
                        zip.putNextEntry(ZipEntry("rule-providers/$relative"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
        return buffer.toByteArray()
    }

    fun read(bytes: ByteArray): Contents {
        var manifest: String? = null
        var state: String? = null
        var totalBytes = 0L
        val caches = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(isSafeRelativePath(name)) { "备份包含无效路径" }
                require(!entry.isDirectory) { "备份格式无效" }
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = zip.read(chunk)
                    if (count < 0) break
                    totalBytes += count
                    require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "备份文件过大" }
                    buffer.write(chunk, 0, count)
                }
                val content = buffer.toByteArray()
                when (name) {
                    MANIFEST_PATH -> manifest = content.toString(Charsets.UTF_8)
                    STATE_PATH -> state = content.toString(Charsets.UTF_8)
                    else -> if (name.startsWith("rule-providers/")) {
                        val relative = name.removePrefix("rule-providers/")
                        require(isSafeRelativePath(relative)) { "备份包含无效缓存路径" }
                        caches[relative] = content
                    }
                }
                zip.closeEntry()
            }
        }
        val metadata = requireNotNull(manifest) { "不是有效的鸡场备份文件" }
        require(metadata.contains("\"format\":\"$FORMAT\"")) { "不是有效的鸡场备份文件" }
        val version = Regex("\\\"version\\\"\\s*:\\s*(\\d+)").find(metadata)?.groupValues?.get(1)?.toIntOrNull()
        require(version == VERSION) { "不支持的备份版本：${version ?: "未知"}" }
        return Contents(requireNotNull(state) { "备份缺少状态数据" }, caches)
    }

    /** Keeps the previous directory until the database snapshot has committed. */
    class CacheReplacement internal constructor(private val root: File, private val staged: File, private val previous: File) {
        private var installed = false
        fun install() {
            if (root.exists()) check(root.renameTo(previous)) { "无法替换规则集缓存" }
            if (!staged.renameTo(root)) {
                if (previous.exists()) previous.renameTo(root)
                error("无法恢复规则集缓存")
            }
            installed = true
        }
        fun commit() { installed = false; previous.deleteRecursively() }
        fun close() {
            if (installed) {
                root.deleteRecursively()
                if (previous.exists()) check(previous.renameTo(root)) { "无法还原原规则集缓存" }
            }
            staged.deleteRecursively()
        }
    }

    fun stageCacheFiles(cacheRoot: File, files: Map<String, ByteArray>): CacheReplacement {
        val parent = cacheRoot.parentFile ?: error("缓存目录无效")
        val staged = File(parent, "rule-providers-import-${System.nanoTime()}")
        require(staged.mkdirs()) { "无法准备缓存导入目录" }
        try {
            files.forEach { (relative, bytes) ->
                require(isSafeRelativePath(relative)) { "备份包含无效缓存路径" }
                val output = File(staged, relative)
                require(output.canonicalPath.startsWith(staged.canonicalPath + File.separator)) { "备份包含无效缓存路径" }
                output.parentFile?.mkdirs()
                output.writeBytes(bytes)
            }
            return CacheReplacement(cacheRoot, staged, File(parent, "rule-providers-previous-${System.nanoTime()}"))
        } catch (error: Exception) { staged.deleteRecursively(); throw error }
    }

    fun replaceCacheFiles(cacheRoot: File, files: Map<String, ByteArray>) {
        val replacement = stageCacheFiles(cacheRoot, files)
        try { replacement.install(); replacement.commit() } finally { replacement.close() }
    }

    private fun isSafeRelativePath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && '\\' !in path &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." }
}
