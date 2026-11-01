package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ScreenCommandQueueTest {
    @Test fun twoQueuedTogglesRestoreThePicture() = runBlocking {
        val queue = ScreenCommandQueue()
        val firstWrite = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        var on = true
        val actions = mutableListOf<String>()
        val first = launch {
            queue.execute(ScreenCommand.Toggle, { true }, { on }, { on = true }, {
                firstWrite.complete(Unit)
                releaseWrite.await()
                on = false
                actions += "off"
            })
        }
        withTimeout(2_000) { firstWrite.await() }
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.execute(ScreenCommand.Toggle, { true }, { on }, { on = true; actions += "on" }, { on = false })
        }
        releaseWrite.complete(Unit)
        joinAll(first, second)
        assertTrue(on)
        assertEquals(listOf("off", "on"), actions)
    }

    @Test fun queuedCommandRechecksConnectionAfterWaiting() = runBlocking {
        val queue = ScreenCommandQueue()
        val held = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var connected = true
        val holder = launch { queue.withLock { held.complete(Unit); release.await() } }
        held.await()
        val command = launch(start = CoroutineStart.UNDISPATCHED) {
            queue.execute(ScreenCommand.Toggle, { connected }, { true }, { fail("Disconnected") }, { fail("Disconnected") })
        }
        connected = false
        release.complete(Unit)
        joinAll(holder, command)
    }

    @Test fun repeatedExplicitOffDoesNotCaptureBrightnessTwice() = runBlocking {
        val queue = ScreenCommandQueue()
        var on = true
        var captures = 0
        repeat(2) {
            queue.execute(ScreenCommand.Off, { true }, { on }, {}, { captures++; on = false })
        }
        assertEquals(1, captures)
    }

    @Test fun screenWakeStateIsReadByTheNextToggle() = runBlocking {
        val queue = ScreenCommandQueue()
        var on = false
        queue.withLock { on = true }
        queue.execute(ScreenCommand.Toggle, { true }, { on }, { fail("Already awake") }, { on = false })
        assertFalse(on)
    }
}
