package com.thaicuonght.screenofftv.shizuku

import android.os.RemoteCallbackList
import android.util.Log
import com.thaicuonght.screenofftv.ipc.result.EventResult
import kotlin.system.exitProcess

/** Blocking read delivers a key immediately; no polling or grep pipe buffering. */
class EventsProviderService : IEventsProvider.Stub() {
    companion object { const val TAG = "EventsProviderService" }
    private val lock = Any()
    @Volatile private var running = false
    @Volatile private var process: Process? = null
    @Volatile private var thread: Thread? = null
    private val listeners = RemoteCallbackList<IEventsListener>()

    override fun isRunning() = running && process?.isAlive == true && thread?.isAlive == true
    override fun launch(filter: String?) = synchronized(lock) {
        if (running) return@synchronized
        val child = Runtime.getRuntime().exec(arrayOf("getevent"))
        process = child
        running = true
        thread = Thread({
            try {
                child.inputStream.bufferedReader().use { reader ->
                    while (running) {
                        val line = reader.readLine() ?: break
                        val parts = line.trim().split(Regex("\\s+"))
                        if (parts.size != 4 || parts[1] != "0001") continue
                        val event = EventResult(parts[0], parts[1], parts[2], parts[3])
                        val count = listeners.beginBroadcast()
                        try {
                            for (i in 0 until count) {
                                runCatching { listeners.getBroadcastItem(i).onEvent(event) }
                            }
                        } finally { listeners.finishBroadcast() }
                    }
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Input reader stopped", e)
            } finally {
                child.destroy()
                if (process === child) { process = null; running = false }
            }
        }, "TV-remote-input").also { it.start() }
    }
    override fun stop() = synchronized(lock) {
        running = false
        process?.destroy() // Unblocks readLine before joining.
        thread?.interrupt()
        thread?.join(500L)
        process = null
        thread = null
    }
    override fun registerListener(listener: IEventsListener?) { if (listener != null) listeners.register(listener) }
    override fun unregisterListener(listener: IEventsListener?) { if (listener != null) listeners.unregister(listener) }
    override fun destroy() { stop(); listeners.kill(); exitProcess(0) }
}
