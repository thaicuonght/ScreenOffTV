package com.thaicuonght.screenofftv.ipc.result

import org.junit.Assert.*
import org.junit.Test

class ResultTest {
    @Test fun successAndErrorUseDifferentCodes() {
        val ok = UnitResult.Ok()
        val error = UnitResult.Err(IllegalStateException("unsupported display"))
        assertTrue(ok.isOk); assertFalse(ok.isErr)
        assertTrue(error.isErr); assertFalse(error.isOk)
        var called = false
        error.withResult(ok = { fail("error must not call success") }, err = { name, _ ->
            called = true
            assertTrue(name.orEmpty().contains("unsupported display"))
        })
        assertTrue(called)
    }
}
