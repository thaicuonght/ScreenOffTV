package com.thaicuonght.screenofftv.shizuku

import org.junit.Assert.*
import org.junit.Test

class ShellCommandRunnerTest {
    @Test fun successfulCommandReturnsOutput() {
        assertEquals("42", ShellCommandRunner().run("sh", "-c", "printf 42"))
    }

    @Test fun nonzeroExitDoesNotReportSuccess() {
        val failure = runCatching { ShellCommandRunner().run("sh", "-c", "echo denied; exit 7") }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(failure!!.message!!.contains("7"))
        assertTrue(failure.message!!.contains("denied"))
    }

    @Test fun stuckCommandIsBounded() {
        val failure = runCatching { ShellCommandRunner(timeoutMs = 20).run("sh", "-c", "sleep 1") }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(failure!!.message!!.contains("timed out"))
    }
}
