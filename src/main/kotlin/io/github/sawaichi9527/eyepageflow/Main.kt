package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.platform.SingleInstance
import io.github.sawaichi9527.eyepageflow.platform.ToolkitSelection
import java.nio.file.Path

fun main(args: Array<String>) {
    applyToolkitSelection()
    when {
        args.contains("--help") || args.contains("-h") -> printHelp()
        args.contains("--benchmark") -> Benchmark.run()
        args.contains("--icon") -> {
            val index = args.indexOf("--icon")
            val target = args.getOrNull(index + 1)?.takeUnless { it.startsWith("--") }
                ?: "build/package/eyePageFlow.png"
            AppIcon.writeAs(Path.of(target))
        }
        args.contains("--render") -> {
            val index = args.indexOf("--render")
            val candidate = args.getOrNull(index + 1)
            val directory = if (candidate != null && !candidate.startsWith("--")) {
                candidate
            } else {
                "build/prototype"
            }
            PrototypeRender.run(Path.of(directory))
        }
        else -> {
            val paths = args.filter { !it.startsWith("-") }.map(Path::of)
            val instance = SingleInstance()
            if (!instance.acquire()) {
                instance.send(paths)
                return
            }
            instance.startServer { value -> ReaderWindow.openExternal(Path.of(value)) }
            ReaderWindow.launch(
                initialPath = paths.firstOrNull(),
                openSettings = args.contains("--settings"),
                demo = args.contains("--demo"),
                openEditor = args.contains("--editor"),
                openFind = args.contains("--find"),
                openChapters = args.contains("--chapters"),
                openChapterRule = args.contains("--chapter-rule"),
                openBookmarks = args.contains("--bookmarks"),
                openLibrary = args.contains("--library"),
                openFiles = args.contains("--files"),
                openGuide = args.contains("--guide"),
            )
        }
    }
}

/**
 * 設定 AWT toolkit（必須在 AWT 初始化前呼叫）。
 *
 * 細節見 [ToolkitSelection]：Linux + Wayland + JBR 才用 WLToolkit，
 * 其他環境沿用 JVM 預設，避免 launcher 寫死造成 X11 無法啟動。
 *
 * 覆寫請用 `-Deyepageflow.toolkit=XToolkit` 或 `EYEPAGEFLOW_TOOLKIT`。
 * 不能用 `awt.toolkit.name` 判斷「使用者是否指定」：JBR 啟動時就會把它塞進 JVM 參數。
 */
private fun applyToolkitSelection() {
    val hasWaylandToolkit = try {
        // initialize=false：只確認類別存在，不觸發 WLToolkit 的 native 靜態初始化
        // （直接 Class.forName 會丟 UnsatisfiedLinkError）。
        Class.forName("sun.awt.wl.WLToolkit", false, ClassLoader.getSystemClassLoader())
        true
    } catch (_: Throwable) {
        false
    }
    val override = System.getenv(ToolkitSelection.OVERRIDE_ENV)
        ?: System.getProperty(ToolkitSelection.OVERRIDE_PROPERTY)
    val toolkit = ToolkitSelection.decide(
        osName = System.getProperty("os.name") ?: "",
        sessionType = System.getenv("XDG_SESSION_TYPE"),
        hasWaylandToolkit = hasWaylandToolkit,
        override = override,
    ) ?: return
    System.setProperty("awt.toolkit.name", toolkit)
}

private fun printHelp() {    println(
        """
        eyePageFlow M0 prototype

        Usage:
          ./scripts/gradlew-local.sh run                 # Swing prototype
          ./scripts/gradlew-local.sh run --args="/path/to/novel.txt"
          ./scripts/gradlew-local.sh run --args="--settings"
          ./scripts/gradlew-local.sh run --args="--demo"
          ./scripts/gradlew-local.sh run --args="--editor"
          ./scripts/gradlew-local.sh run --args="--find"
          ./scripts/gradlew-local.sh run --args="--chapters"
          ./scripts/gradlew-local.sh run --args="--chapter-rule"
          ./scripts/gradlew-local.sh run --args="--bookmarks"
          ./scripts/gradlew-local.sh run --args="--library"
          ./scripts/gradlew-local.sh run --args="--files"
          ./scripts/gradlew-local.sh run --args="--guide"
          ./scripts/gradlew-local.sh run --args="--icon build/package/eyePageFlow.ico"
          ./scripts/gradlew-local.sh run --args="--render build/prototype"
          ./scripts/gradlew-local.sh run --args="--benchmark"

        Keys:
          Left / PgUp / Backspace / Up     previous spread
          Right / PgDn / Space / Down      next spread
          Home / End                       first / last page
          Ctrl+Left / Ctrl+Right           reading history back / forward
          Mouse click, wheel, scroll bar   move by page
          F1                               user guide
          F3                               find next
          V                         toggle vertical / horizontal
          D                         toggle single / double page
          T                         toggle theme
          B                         toggle low-key mode
        """.trimIndent(),
    )
}
