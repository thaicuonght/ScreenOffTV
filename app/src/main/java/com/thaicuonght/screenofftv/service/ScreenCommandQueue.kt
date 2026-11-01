package com.thaicuonght.screenofftv.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class ScreenCommand { On, Off, Toggle }

/** Read readiness and picture state only after earlier commands have finished. */
internal class ScreenCommandQueue {
    private val mutex = Mutex()

    suspend fun <T> withLock(action: suspend () -> T): T = mutex.withLock { action() }

    suspend fun execute(
        command: ScreenCommand,
        ready: () -> Boolean,
        isOn: () -> Boolean,
        turnOn: suspend () -> Unit,
        turnOff: suspend () -> Unit,
    ) = withLock {
        if (!ready()) return@withLock
        when (command) {
            ScreenCommand.On -> if (!isOn()) turnOn()
            ScreenCommand.Off -> if (isOn()) turnOff()
            ScreenCommand.Toggle -> if (isOn()) turnOff() else turnOn()
        }
    }
}
