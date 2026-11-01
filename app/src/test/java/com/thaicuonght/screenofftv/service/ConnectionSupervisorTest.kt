package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ConnectionSupervisorTest {
    @Test fun failedConnectionRetriesAndCancellationReleasesResources() = runBlocking {
        var attempts = 0
        var releases = 0
        val connected = CompletableDeferred<Unit>()
        val supervisor = ConnectionSupervisor(
            connect = {
                attempts++
                if (attempts == 1) error("Shizuku not ready")
                connected.complete(Unit)
            },
            isHealthy = { true }, disconnect = { releases++ }, onFailure = {},
            retryDelayMs = 1, healthCheckMs = 1,
        )
        val job = launch { supervisor.run() }
        withTimeout(2_000) { connected.await() }
        job.cancelAndJoin()
        assertEquals(2, attempts)
        assertEquals(2, releases)
    }

    @Test fun lostInputReconnectsWithoutManualServiceStart() = runBlocking {
        var attempts = 0
        val reconnected = CompletableDeferred<Unit>()
        val supervisor = ConnectionSupervisor(
            connect = { if (++attempts == 2) reconnected.complete(Unit) },
            isHealthy = { attempts > 1 }, disconnect = {}, onFailure = {},
            retryDelayMs = 1, healthCheckMs = 1,
        )
        val job = launch { supervisor.run() }
        withTimeout(2_000) { reconnected.await() }
        job.cancelAndJoin()
        assertEquals(2, attempts)
    }

    @Test fun bindingTimeoutRetriesInsteadOfCancellingSupervisor() = runBlocking {
        var attempts = 0
        val connected = CompletableDeferred<Unit>()
        val supervisor = ConnectionSupervisor(
            connect = {
                if (++attempts == 1) withTimeout(1) { awaitCancellation() }
                connected.complete(Unit)
            },
            isHealthy = { true }, disconnect = {}, onFailure = {},
            retryDelayMs = 1, healthCheckMs = 1,
        )
        val job = launch { supervisor.run() }
        withTimeout(2_000) { connected.await() }
        job.cancelAndJoin()
        assertEquals(2, attempts)
    }

    @Test fun userStopCancelsWaitingAndDoesNotRetry() = runBlocking {
        var attempts = 0
        var failures = 0
        var releases = 0
        val waiting = CompletableDeferred<Unit>()
        val supervisor = ConnectionSupervisor(
            connect = { attempts++; waiting.complete(Unit); awaitCancellation() },
            isHealthy = { true }, disconnect = { releases++ }, onFailure = { failures++ },
            retryDelayMs = 1, healthCheckMs = 1,
        )
        val job = launch { supervisor.run() }
        withTimeout(2_000) { waiting.await() }
        job.cancelAndJoin()
        assertEquals(1, attempts)
        assertEquals(0, failures)
        assertEquals(1, releases)
    }
}
