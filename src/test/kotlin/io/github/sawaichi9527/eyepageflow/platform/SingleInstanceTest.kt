package io.github.sawaichi9527.eyepageflow.platform

import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SingleInstanceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `second instance forwards paths to the primary`() {
        val lock = tempDir.resolve("instance.lock")
        val port = tempDir.resolve("instance.port")
        val first = SingleInstance(lock, port)
        assertTrue(first.acquire())
        try {
            val received = AtomicReference<String>()
            val latch = CountDownLatch(1)
            assertTrue(first.startServer { value ->
                received.set(value)
                latch.countDown()
            })

            val second = SingleInstance(lock, port)
            assertFalse(second.acquire())
            assertTrue(second.send(listOf(tempDir.resolve("novel.txt"))))
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertTrue(received.get()?.endsWith("novel.txt") == true)
        } finally {
            first.close()
        }
    }

    @Test
    fun `lock is reusable after close`() {
        val lock = tempDir.resolve("instance.lock")
        val port = tempDir.resolve("instance.port")
        val first = SingleInstance(lock, port)
        assertTrue(first.acquire())
        first.close()

        val second = SingleInstance(lock, port)
        assertTrue(second.acquire())
        second.close()
    }
}
