package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A replacement service cannot bind shared Shizuku tags before old cleanup finishes. */
internal class ServiceSessionGate {
    private val mutex = Mutex()
    suspend fun run(session: suspend () -> Unit) = mutex.withLock { session() }
}
