package com.thaicuonght.screenofftv.shizuku

import java.util.concurrent.TimeUnit

/** Checks completion, not just that a command was written to a shell's stdin. */
internal class ShellCommandRunner(private val timeoutMs: Long = 5_000) {
    fun run(vararg command: String): String {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        try {
            check(process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                "Settings command timed out"
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            check(process.exitValue() == 0) { "Settings command failed (${process.exitValue()}): $output" }
            return output
        } finally {
            process.destroy()
        }
    }

    fun putSystemInt(name: String, value: Int) {
        require(name == "screen_brightness" || name == "screen_brightness_mode")
        run("settings", "put", "system", name, value.toString())
        val actual = run("settings", "get", "system", name)
        check(actual.toIntOrNull() == value) { "Could not verify $name: expected $value, got $actual" }
    }
}
