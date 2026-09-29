package com.jzb.jichang.android.share

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.net.wifi.WifiManager
import com.jzb.jichang.android.MainActivity
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class LocalShareService : Service() {
    private val binder = LocalBinder()
    private var server: ConfigServer? = null
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
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
        startForeground(NOTIFICATION_ID, notification())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun startSharing(config: String, filename: String): String {
        stopSharing()
        val address = findLanAddress() ?: error("没有检测到可用的 Wi-Fi 或有线局域网，请连接网络后重试")
        val tokenBytes = ByteArray(18).also(SecureRandom()::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
        sharedConfig = config to filename
        val extension = if (filename.endsWith(".json", true)) "json" else "yaml"
        val active = ConfigServer(address, token, extension) { sharedConfig ?: ("" to "鸡场.$extension") }
        try {
            active.start(5_000, false)
            acquireShareLocks()
            server = active
            currentUrl = "http://$address:${active.listeningPort}/$token/config.$extension"
            return currentUrl!!
        } catch (error: Throwable) {
            active.stop()
            releaseShareLocks()
            sharedConfig = null
            throw error
        }
    }

    fun stopSharing() {
        server?.stop()
        server = null
        sharedConfig = null
        currentUrl = null
        releaseShareLocks()
    }

    fun updateConfig(config: String, filename: String) {
        val extension = if (filename.endsWith(".json", true)) "json" else "yaml"
        if (currentUrl?.endsWith("/config.$extension") == true) sharedConfig = config to filename
    }

    override fun onDestroy() {
        stopSharing()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun acquireShareLocks() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (cpuWakeLock?.isHeld != true) {
            cpuWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:lan-share").apply {
                setReferenceCounted(false)
                acquire()
            }
        }

        // Keep the radio available while the display is off. A failure to acquire this
        // optional optimization must not tear down the already-running HTTP server.
        runCatching {
            val wifiManager = getSystemService(WifiManager::class.java) ?: return@runCatching
            if (wifiLock == null) {
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:lan-share").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        }
    }

    private fun releaseShareLocks() {
        runCatching { if (wifiLock?.isHeld == true) wifiLock?.release() }
        runCatching { if (cpuWakeLock?.isHeld == true) cpuWakeLock?.release() }
        wifiLock = null
        cpuWakeLock = null
    }

    @Suppress("DEPRECATION")
    private fun findLanAddress(): String? = runCatching {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivity.activeNetwork
        val networks = (listOfNotNull(activeNetwork) + connectivity.allNetworks.toList()).distinct()
        networks.asSequence()
            .mapNotNull { network ->
                val capabilities = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
                val transportPriority = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 0
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 1
                    else -> return@mapNotNull null
                }
                val isActive = network == activeNetwork
                val addresses = connectivity.getLinkProperties(network)?.linkAddresses.orEmpty()
                    .mapNotNull { it.address as? Inet4Address }
                    .filter { it.isSiteLocalAddress && !it.isLoopbackAddress }
                addresses.firstOrNull()?.let { address -> Triple(transportPriority, isActive, address) }
            }
            .sortedWith(compareByDescending<Triple<Int, Boolean, Inet4Address>> { it.second }.thenBy { it.first })
            .firstOrNull()
            ?.third
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
                .setContentTitle("正在分享鸡场配置")
                .setContentText("后台分享运行中 · 点此返回管理")
                .setContentIntent(openPendingIntent)
                .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "停止分享", stopPendingIntent).build())
                .setOngoing(true).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("正在分享鸡场配置")
                .setContentText("后台分享运行中 · 点此返回管理")
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止分享", stopPendingIntent)
                .setOngoing(true).build()
        }
    }

    private class ConfigServer(host: String, private val token: String, private val extension: String, private val config: () -> Pair<String, String>) : NanoHTTPD(host, 0) {
        // Keep the response byte-for-byte for clients that reject invalid encodings.
        override fun useGzipWhenAccepted(response: Response): Boolean = false

        override fun serve(session: IHTTPSession): Response {
            if (session.method != Method.GET && session.method != Method.HEAD) {
                return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", "Method Not Allowed")
                    .apply { addHeader("Allow", "GET, HEAD") }
            }
            val expected = "/$token/config.$extension"
            val actual = session.uri
            if (!MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
            }
            val (content, filename) = config()
            val mime = if (extension == "json") "application/json; charset=utf-8" else "text/yaml; charset=utf-8"
            // NanoHTTPD 2.3.1 writes the body even for HEAD. Give it an empty stream while
            // retaining the length of the equivalent GET response in the metadata.
            val response = if (session.method == Method.HEAD) {
                newFixedLengthResponse(
                    Response.Status.OK,
                    mime,
                    ByteArrayInputStream(ByteArray(0)),
                    content.toByteArray(Charsets.UTF_8).size.toLong(),
                )
            } else newFixedLengthResponse(Response.Status.OK, mime, content)
            return response.apply {
                addHeader("Cache-Control", "no-store, no-cache, must-revalidate")
                addHeader("X-Content-Type-Options", "nosniff")
                // Keep the suggested profile filename without forcing import clients to
                // treat this response as a file attachment. The first LAN share version
                // returned content inline, which works with clients that import URLs directly.
                addHeader("Content-Disposition", "inline; filename*=UTF-8''${java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20")}")
            }
        }
    }

    companion object {
        const val ACTION_STOP = "com.jzb.jichang.android.action.STOP_SHARE"
        private const val CHANNEL_ID = "jichang-local-share"
        private const val NOTIFICATION_ID = 5401
    }
}
