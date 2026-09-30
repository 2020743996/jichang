package com.jzb.jichang.android

import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal val LocalMotionEnabled = compositionLocalOf { true }
internal data class EditorSaveState(val saving: Boolean = false, val error: String? = null)
internal val LocalEditorSaveState = compositionLocalOf { EditorSaveState() }
internal val QuickOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
internal fun <T> quickMotion(duration: Int = 180) = tween<T>(duration, easing = QuickOut)

@Composable
internal fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }.getOrDefault(true)
    var enabled by remember { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { enabled = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}

@Composable
internal fun debouncedQuery(query: String): String {
    var value by remember { mutableStateOf(query) }
    LaunchedEffect(query) { delay(150); value = query }
    return value
}

/** A key change clears stale content immediately. CPU work runs outside composition. */
@Composable
internal fun <T> backgroundValue(key: Any?, initial: T, compute: suspend () -> T): T {
    val result = remember(key) { mutableStateOf(initial) }
    LaunchedEffect(key) {
        result.value = withContext(Dispatchers.Default) { compute() }
    }
    return result.value
}

internal inline fun <reified T> jsonSaver() = androidx.compose.runtime.saveable.Saver<T, String>(
    save = { com.google.gson.Gson().toJson(it) },
    restore = { com.google.gson.Gson().fromJson<T>(it, object : com.google.gson.reflect.TypeToken<T>() {}.type) },
)
