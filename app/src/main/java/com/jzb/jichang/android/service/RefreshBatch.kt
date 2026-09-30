package com.jzb.jichang.android.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** One refresh session owns its IDs until every child has finished or cancelled. */
data class RefreshProgress(
    val total: Int = 0,
    val succeeded: Int = 0,
    val failedIds: Set<String> = emptySet(),
    val ignored: Int = 0,
    val requestedIds: Set<String> = emptySet(),
    val runningIds: Set<String> = emptySet(),
    val active: Boolean = false,
    val cancelled: Boolean = false,
) {
    val failed: Int get() = failedIds.size
    val completed: Int get() = succeeded + failed + ignored
}

class RefreshBatch(private val scope: CoroutineScope, private val permits: Semaphore = Semaphore(3)) {
    private val mutableProgress = MutableStateFlow(RefreshProgress())
    val progress: StateFlow<RefreshProgress> = mutableProgress
    private var job: Job? = null

    @Synchronized
    fun start(ids: List<String>, task: suspend (String) -> Unit, onFinish: (RefreshProgress) -> Unit = {}): Boolean {
        if (job?.isActive == true || mutableProgress.value.active) return false
        val keys = ids.distinct()
        if (keys.isEmpty()) return false
        mutableProgress.value = RefreshProgress(total = keys.size, requestedIds = keys.toSet(), active = true)
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                coroutineScope {
                    keys.map { id -> async {
                        permits.withPermit {
                            mutableProgress.update { it.copy(runningIds = it.runningIds + id) }
                            try {
                                task(id)
                                mutableProgress.update { it.copy(succeeded = it.succeeded + 1) }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: StaleRefreshException) {
                                mutableProgress.update { it.copy(ignored = it.ignored + 1) }
                            } catch (_: Exception) {
                                mutableProgress.update { it.copy(failedIds = it.failedIds + id) }
                            } finally {
                                mutableProgress.update { it.copy(runningIds = it.runningIds - id) }
                            }
                        }
                    } }.awaitAll()
                }
                onFinish(mutableProgress.value)
            } catch (error: CancellationException) {
                mutableProgress.update { it.copy(cancelled = true) }
                throw error
            } finally {
                mutableProgress.update { it.copy(active = false, runningIds = emptySet()) }
            }
        }
        job!!.start()
        return true
    }

    suspend fun awaitIdle() { job?.join() }
    fun cancel() { job?.cancel() }
}
