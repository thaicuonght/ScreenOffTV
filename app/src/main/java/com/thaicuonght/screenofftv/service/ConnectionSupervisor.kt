package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class PermanentConnectionException(message: String) : Exception(message)

/** Keeps a foreground service connected until its owner explicitly stops it. */
internal class ConnectionSupervisor(
    private val connect: suspend () -> Unit,
    private val isHealthy: suspend () -> Boolean,
    private val disconnect: suspend () -> Unit,
    private val onFailure: (Exception) -> Unit,
    private val retryDelayMs: Long = 1_000,
    private val healthCheckMs: Long = 1_000,
) {
    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            var retry = true
            try {
                connect()
                while (currentCoroutineContext().isActive) {
                    delay(healthCheckMs)
                    check(isHealthy()) { "Shizuku display or remote input disconnected" }
                }
            } catch (timeout: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                onFailure(timeout)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                retry = failure !is PermanentConnectionException
                onFailure(failure)
            } finally {
                val reportFailure = currentCoroutineContext().isActive
                withContext(NonCancellable) {
                    try {
                        disconnect()
                    } catch (failure: Exception) {
                        if (reportFailure) onFailure(failure)
                    }
                }
            }
            if (!retry) return
            delay(retryDelayMs)
        }
    }
}
