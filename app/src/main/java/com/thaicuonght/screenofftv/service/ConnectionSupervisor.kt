package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

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
                onFailure(failure)
            } finally {
                withContext(NonCancellable) { disconnect() }
            }
            delay(retryDelayMs)
        }
    }
}
