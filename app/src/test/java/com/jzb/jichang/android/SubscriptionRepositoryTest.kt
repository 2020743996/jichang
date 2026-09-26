package com.jzb.jichang.android

import com.google.gson.Gson
import com.jzb.jichang.android.data.SnapshotDao
import com.jzb.jichang.android.data.SnapshotEntity
import com.jzb.jichang.android.data.JichangRepository
import com.jzb.jichang.android.model.AppState
import com.jzb.jichang.android.model.ProxyNode
import com.jzb.jichang.android.model.SubscriptionSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SubscriptionRepositoryTest {
    @Test fun failedRefreshKeepsExistingNodesAndRecordsError() = runBlocking {
        val source = SubscriptionSource("source-1", "test", "http://127.0.0.1:1/subscription")
        val oldNode = ProxyNode("node-1", source.id, "old node", "trojan", "old.example", 443)
        val dao = InMemorySnapshotDao(Gson().toJson(AppState(sources = listOf(source), nodes = listOf(oldNode))))
        val repository = JichangRepository(dao)
        delay(100)

        try { runCatching { repository.refreshSource(source.id) } }
        finally { repository.close() }

        assertEquals(listOf(oldNode), repository.state.value.nodes)
        assertNotNull(repository.state.value.sources.single().lastError)
    }

    @Test fun sourceUrlsMustBeHttpOrHttps() = runBlocking {
        val repository = JichangRepository(InMemorySnapshotDao())
        val result = try { runCatching { repository.addSource("bad", "file:///etc/passwd") } }
        finally { repository.close() }
        assertEquals(true, result.isFailure)
    }

    private class InMemorySnapshotDao(initial: String? = null) : SnapshotDao {
        private val payload = MutableStateFlow(initial)
        override fun observe(): Flow<String?> = payload
        override suspend fun save(snapshot: SnapshotEntity) { payload.value = snapshot.payload }
    }
}
