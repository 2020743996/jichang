package com.jzb.jichang.android.share

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.jzb.jichang.android.MainActivity
import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class LocalShareService : Service() {
    private val binder = LocalBinder()
    private var server: ConfigServer? = null
    @Volatile private var sharedConfig: Pair<String, String>? = null
    var currentUrl: String? = null
        private set

    inner class LocalBinder : Binder() { fun service(): LocalShareService = this@LocalShareService }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharing()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun startSharing(config: String, filename: String): String {
        stopSharing()
        val address = findLanAddress() ?: error("没有找到局域网地址，请连接 Wi-Fi 或有线网络")
        val tokenBytes = ByteArray(18).also(SecureRandom()::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
        sharedConfig = config to filename
        val active = ConfigServer(address, token) { sharedConfig ?: ("" to "鸡场.yaml") }
        active.start(5_000, false)
        server = active
        currentUrl = "http://$address:${active.listeningPort}/$token/config.yaml"
        return currentUrl!!
    }

    fun stopSharing() {
        server?.stop()
        server = null
        sharedConfig = null
        currentUrl = null
    }

    fun updateConfig(config: String, filename: String) { if (currentUrl != null) sharedConfig = config to filename }

    override fun onDestroy() {
        stopSharing()
        super.onDestroy()
    }

    private fun findLanAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { network ->
                when {
                    network.name.startsWith("wlan", true) -> 0
                    network.name.startsWith("ap", true) -> 1
                    network.name.startsWith("eth", true) || network.name.startsWith("en", true) -> 2
                    else -> 3
                }
            }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "局域网配置分享", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        val stopIntent = Intent(this, LocalShareService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0,
        )
        val openPendingIntent = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("正在分享 Mihomo 配置")
                .setContentText("仅持有随机链接的局域网设备可获取配置")
                .setContentIntent(openPendingIntent)
                .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "停止分享", stopPendingIntent).build())
                .setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("正在分享 Mihomo 配置")
                .setContentText("局域网配置分享服务运行中")
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止分享", stopPendingIntent)
                .setOngoing(true).build()
        }
    }

    private class ConfigServer(host: String, private val token: String, private val config: () -> Pair<String, String>) : NanoHTTPD(host, 0) {
        override fun serve(session: IHTTPSession): Response {
            if (session.method != Method.GET) return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed")
            val expected = "/$token/config.yaml"
            val actual = session.uri
            if (!MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
            val (yaml, filename) = config()
            return newFixedLengthResponse(Response.Status.OK, "text/yaml; charset=utf-8", yaml).apply {
                addHeader("Cache-Control", "no-store, no-cache, must-revalidate")
                addHeader("X-Content-Type-Options", "nosniff")
                addHeader("Content-Disposition", "attachment; filename*=UTF-8''${java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")}")
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.jzb.jichang.android.action.STOP_SHARE"
        private const val CHANNEL_ID = "jichang-local-share"
        private const val NOTIFICATION_ID = 5401
    }
}
