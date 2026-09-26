package com.jzb.jichang.android

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.jzb.jichang.android.service.LanShareQrCode
import org.junit.Assert.assertEquals
import org.junit.Test

class LanShareQrCodeTest {
    @Test
    fun generatedQrDecodesToTheExactShareUrl() {
        val url = "http://192.168.1.24:38127/5x9uJ9-LAN-token/config.yaml"
        val matrix = LanShareQrCode.encodeMatrix(url, size = 640)
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            val x = index % matrix.width
            val y = index / matrix.width
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }

        val source = RGBLuminanceSource(matrix.width, matrix.height, pixels)
        val decoded = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text

        assertEquals(url, decoded)
    }
}
