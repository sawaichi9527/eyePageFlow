package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.core.EncodingDetector
import io.github.sawaichi9527.eyepageflow.core.PageMetrics
import io.github.sawaichi9527.eyepageflow.core.PaginationEngine
import io.github.sawaichi9527.eyepageflow.core.TextEncoding
import io.github.sawaichi9527.eyepageflow.core.WritingMode
import io.github.sawaichi9527.eyepageflow.render.Typeface

object Benchmark {
    private const val TARGET_BYTES = 10 * 1024 * 1024

    fun run() {
        System.setProperty("java.awt.headless", "true")

        val paragraph = """
            這是一段用來測試分頁速度的示範文字。書頁、封面、桌面與中縫應該要正確顯示。
            「引號、書名號《測試》、括號（甲乙）、省略號……與破折號——」都要能正確排列。
            PageByFlow 2026：Latin text 與數字 1234567890 在直排時應該旋轉或保持正立。
            中文排版需要處理禁則：句號、逗號、問號、驚嘆號不應出現在行首。
        """.trimIndent().replace("\n", "")

        val paragraphBytes = paragraph.toByteArray(TextEncoding.BIG5.charset)
        check(paragraphBytes.isNotEmpty())
        val repeats = ((TARGET_BYTES + paragraphBytes.size - 1) / paragraphBytes.size)
            .coerceAtLeast(1)
        val text = paragraph.repeat(repeats)
        val bytes = text.toByteArray(TextEncoding.BIG5.charset)

        val decodeStarted = System.nanoTime()
        val decoded = EncodingDetector.decode(bytes)
        val decodeMs = (System.nanoTime() - decodeStarted) / 1_000_000.0
        check(decoded.text.isNotEmpty())

        println(
            "input=${bytes.size / 1024 / 1024} MiB, chars=${decoded.text.length}, " +
                "encoding=${decoded.encoding.displayName}, replacements=${decoded.replacementCount}, " +
                "decode=%.1f ms".format(decodeMs),
        )

        val metrics = PageMetrics()
        val font = Typeface.pageFont(metrics)
        WritingMode.entries.forEach { mode ->
            val started = System.nanoTime()
            val engine = PaginationEngine(decoded.text, metrics, mode, font)
            val pagination = engine.paginate()
            val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
            val charsPerSecond = decoded.text.length / (elapsedMs / 1000.0)
            println(
                "mode=${mode.name.lowercase()}, pages=${pagination.pageCount}, " +
                    "paginate=%.1f ms, chars/s=%.0f".format(elapsedMs, charsPerSecond),
            )
            check(elapsedMs < 1_000.0) {
                "10 MiB pagination exceeded 1 second: %.1f ms".format(elapsedMs)
            }
        }
    }
}
