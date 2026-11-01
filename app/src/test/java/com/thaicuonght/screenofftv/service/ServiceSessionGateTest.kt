package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ServiceSessionGateTest {
    @Test fun restartWaitsForCancelledSessionCleanup() = runBlocking {
        val gate = ServiceSessionGate()
        val connected = CompletableDeferred<Unit>()
        val cleanupStarted = CompletableDeferred<Unit>()
        val finishCleanup = CompletableDeferred<Unit>()
        var replacementStarted = false
        val old = launch {
            gate.run {
                try { connected.complete(Unit); awaitCancellation() }
                finally { withContext(NonCancellable) { cleanupStarted.complete(Unit); finishCleanup.await() } }
            }
        }
        connected.await()
        old.cancel()
        cleanupStarted.await()
        val replacement = launch(start = CoroutineStart.UNDISPATCHED) { gate.run { replacementStarted = true } }
        assertFalse(replacementStarted)
        finishCleanup.complete(Unit)
        joinAll(old, replacement)
        assertTrue(replacementStarted)
    }

    @Test fun cancelledWaitingSessionDoesNotBlockNextStart() = runBlocking {
        val gate = ServiceSessionGate()
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val old = launch { gate.run { entered.complete(Unit); finish.await() } }
        entered.await()
        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) { gate.run { fail("Cancelled waiter must not start") } }
        cancelled.cancelAndJoin()
        finish.complete(Unit)
        old.join()
        var started = false
        gate.run { started = true }
        assertTrue(started)
    }
}
