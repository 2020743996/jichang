package com.jzb.jichang.android.service

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Creates a plain, high-contrast QR bitmap containing only the LAN share URL. */
object LanShareQrCode {
    fun createBitmap(content: String, size: Int = 640): Bitmap {
        val matrix = encodeMatrix(content, size)
        val pixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    fun encodeMatrix(content: String, size: Int = 640): BitMatrix {
        require(content.isNotBlank()) { "二维码内容不能为空" }
        require(size >= 256) { "二维码尺寸至少为 256 像素" }
        return MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }
}
