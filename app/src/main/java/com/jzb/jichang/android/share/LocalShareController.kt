package com.jzb.jichang.android.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalShareController(context: Context) {
    private val appContext = context.applicationContext
    private val mutableUrl = MutableStateFlow<String?>(null)
    private val mutableError = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = mutableUrl
    val error: StateFlow<String?> = mutableError
    private var pendingConfig: Pair<String, String>? = null
    private var bound = false
    private var service: LocalShareService? = null
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? LocalShareService.LocalBinder)?.service()
            val activeService = service ?: return
            runCatching { pendingConfig?.let { activeService.startSharing(it.first, it.second) } ?: activeService.currentUrl }
                .onSuccess { mutableUrl.value = it; mutableError.value = null }
                .onFailure { error ->
                    mutableUrl.value = null
                    mutableError.value = error.message ?: "无法启动局域网分享"
                    if (bound) appContext.unbindService(connection)
                    bound = false
                    service = null
                    appContext.stopService(Intent(appContext, LocalShareService::class.java))
                }
            pendingConfig = null
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null; mutableUrl.value = null }
    }

    fun start(config: String, filename: String) {
        mutableError.value = null
        service?.let { activeService ->
            runCatching {
                ContextCompat.startForegroundService(appContext, Intent(appContext, LocalShareService::class.java))
                activeService.startSharing(config, filename)
            }
                .onSuccess { mutableUrl.value = it }
                .onFailure { error ->
                    mutableUrl.value = null
                    mutableError.value = error.message ?: "无法启动局域网分享"
                }
            return
        }
        pendingConfig = config to filename
        mutableUrl.value = null
        val intent = Intent(appContext, LocalShareService::class.java)
        runCatching {
            ContextCompat.startForegroundService(appContext, intent)
            if (!bound) bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            check(bound) { "无法连接局域网分享服务" }
        }.onFailure { error ->
            pendingConfig = null
            mutableUrl.value = null
            mutableError.value = error.message ?: "无法启动局域网分享"
            appContext.stopService(intent)
        }
    }

    fun stop() {
        service?.stopSharing()
        appContext.stopService(Intent(appContext, LocalShareService::class.java))
        if (bound) appContext.unbindService(connection)
        bound = false
        service = null
        pendingConfig = null
        mutableUrl.value = null
        mutableError.value = null
    }

    fun updateConfig(config: String, filename: String) { service?.updateConfig(config, filename) }

    fun onResume() {
        if (bound && service != null) {
            mutableUrl.value = service?.currentUrl
        }
    }

    fun unbind() {
        if (bound) appContext.unbindService(connection)
        bound = false
        service = null
    }
}
