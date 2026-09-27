package com.jzb.jichang.android.share

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

internal data class EncodedShareBody(val bytes: ByteArray, val compressed: Boolean)

/** Immutable YAML bytes with a lazily cached compressed representation for concurrent LAN requests. */
internal class SharePayload(val yaml: String, val filename: String) {
    val raw: ByteArray by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { yaml.toByteArray(Charsets.UTF_8) }
    @Volatile private var gzipCache: ByteArray? = null

    fun encode(acceptsGzip: Boolean): EncodedShareBody {
        if (!acceptsGzip || raw.size < 1024) return EncodedShareBody(raw, false)
        val compressed = gzip()
        return if (compressed.size < raw.size) EncodedShareBody(compressed, true) else EncodedShareBody(raw, false)
    }

    private fun gzip(): ByteArray {
        gzipCache?.let { return it }
        val compressed = ByteArrayOutputStream().use { buffer ->
            GZIPOutputStream(buffer).use { it.write(raw) }
            buffer.toByteArray()
        }
        return synchronized(this) { gzipCache ?: compressed.also { gzipCache = it } }
    }
}
