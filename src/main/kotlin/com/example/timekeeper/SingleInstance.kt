package com.example.timekeeper

import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import javax.swing.SwingUtilities

/**
 * Only one copy of TimeKeeper may run at a time — two would both count the same
 * foreground window and double your totals. The first copy holds a lock file;
 * a later launch finds the lock taken, pokes the running copy to surface its
 * window, and bows out.
 */
object SingleInstance {

    private var lockChannel: RandomAccessFile? = null
    private var lock: FileLock? = null

    /**
     * Claim the single-instance lock. Returns true if this process is now the
     * one running copy, false if another already holds it. [onShow] is invoked
     * (on the EDT) whenever a later launch asks us to bring our window forward.
     */
    fun claim(onShow: () -> Unit): Boolean {
        AppPaths.ensureDataDir()
        val channel = RandomAccessFile(AppPaths.dataDir.resolve("instance.lock").toFile(), "rw")
        val acquired = try {
            channel.channel.tryLock()
        } catch (e: Exception) {
            null
        }
        if (acquired == null) {
            channel.close()
            return false
        }
        lockChannel = channel
        lock = acquired
        startShowServer(onShow)
        return true
    }

    /** Ask the already-running copy to show itself. Best-effort. */
    fun notifyExisting(): Boolean = try {
        Socket(InetAddress.getLoopbackAddress(), PORT).use { it.getOutputStream().write(1) }
        true
    } catch (e: IOException) {
        false
    }

    fun release() {
        runCatching { lock?.release() }
        runCatching { lockChannel?.close() }
    }

    private fun startShowServer(onShow: () -> Unit) {
        try {
            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), PORT))
            Thread {
                while (!server.isClosed) {
                    try {
                        server.accept().use { it.getInputStream().read() }
                        SwingUtilities.invokeLater(onShow)
                    } catch (e: IOException) {
                        // A dropped connection is fine; keep listening.
                    }
                }
            }.apply {
                isDaemon = true
                name = "timekeeper-show"
            }.start()
        } catch (e: IOException) {
            // Couldn't listen for show-requests; the app still works, second
            // launches just won't raise the window.
        }
    }

    private const val PORT = 47653
}
