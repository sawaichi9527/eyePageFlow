package io.github.sawaichi9527.eyepageflow.platform

import io.github.sawaichi9527.eyepageflow.core.AppPaths
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock as NioFileLock
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class SingleInstance(
    private val lockFile: Path = AppPaths.instanceLockFile(),
    private val portFile: Path = AppPaths.instancePortFile(),
) {
    private var channel: FileChannel? = null
    private var fileLock: NioFileLock? = null
    private var server: ServerSocket? = null

    fun acquire(): Boolean {
        return try {
            lockFile.parent?.let { Files.createDirectories(it) }
            val opened = FileChannel.open(
                lockFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val lock = opened.tryLock()
            if (lock == null) {
                opened.close()
                false
            } else {
                channel = opened
                fileLock = lock
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun startServer(onOpen: (String) -> Unit): Boolean {
        return try {
            val socket = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
            server = socket
            portFile.parent?.let { Files.createDirectories(it) }
            Files.writeString(portFile, socket.localPort.toString())
            val keeper = this
            val thread = Thread {
                while (!socket.isClosed && keeper.server === socket) {
                    try {
                        socket.accept().use { client ->
                            BufferedReader(
                                InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8),
                            ).readLine()?.takeIf { it.isNotBlank() }?.let(onOpen)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            thread.isDaemon = true
            thread.name = "eyePageFlow-single-instance"
            thread.start()
            true
        } catch (exception: Exception) {
            println("single instance: could not start server (${exception.message})")
            false
        }
    }

    fun send(paths: List<Path>): Boolean {
        if (paths.isEmpty()) return false
        return try {
            val port = Files.readString(portFile).trim().toInt()
            Socket(InetAddress.getLoopbackAddress(), port).use { socket ->
                PrintWriter(
                    OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true,
                ).use { writer ->
                    paths.forEach { writer.println(it.toAbsolutePath().normalize()) }
                }
            }
            true
        } catch (exception: Exception) {
            println("single instance: could not contact primary (${exception.message})")
            false
        }
    }

    fun close() {
        try {
            server?.close()
        } catch (_: Exception) {
            // Ignore.
        }
        server = null
        try {
            fileLock?.release()
        } catch (_: Exception) {
            // Ignore.
        }
        fileLock = null
        try {
            channel?.close()
        } catch (_: Exception) {
            // Ignore.
        }
        channel = null
    }
}
