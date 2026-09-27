package com.jzb.jichang.android.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalShareController(context: Context) {
    private val appContext = context.applicationContext
    private val mutableUrl = MutableStateFlow<String?>(null)
    private val mutableError = MutableStateFlow<String?>(null)
    private val mutableStats = MutableStateFlow<ShareTransferStats?>(null)
    val url: StateFlow<String?> = mutableUrl
    val error: StateFlow<String?> = mutableError
    val transferStats: StateFlow<ShareTransferStats?> = mutableStats.asStateFlow()
    private var pendingConfig: Pair<String, String>? = null
    private var bound = false
    private var service: LocalShareService? = null
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as? LocalShareService.LocalBinder)?.service()
            val activeService = service ?: return
            activeService.onTransferStats = { mutableStats.value = it }
            mutableStats.value = activeService.transferStats.value
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
        pendingConfig = config to filename
        mutableError.value = null
        val intent = Intent(appContext, LocalShareService::class.java)
        ContextCompat.startForegroundService(appContext, intent)
        if (!bound) {
            bound = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) mutableError.value = "无法连接局域网分享服务"
        }
    }

    fun stop() {
        service?.stopSharing()
        service?.onTransferStats = null
        appContext.stopService(Intent(appContext, LocalShareService::class.java))
        if (bound) appContext.unbindService(connection)
        bound = false
        service = null
        pendingConfig = null
        mutableUrl.value = null
        mutableError.value = null
        mutableStats.value = null
    }

    fun updateConfig(config: String, filename: String) { service?.updateConfig(config, filename) }

    fun onResume() {
        if (bound && service != null) mutableUrl.value = service?.currentUrl
    }

    fun unbind() {
        service?.onTransferStats = null
        if (bound) appContext.unbindService(connection)
        bound = false
        service = null
    }
}
