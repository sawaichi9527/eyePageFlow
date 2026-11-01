package io.github.sawaichi9527.eyepageflow

import io.github.sawaichi9527.eyepageflow.platform.SingleInstance
import java.nio.file.Path

fun main(args: Array<String>) {
    when {
        args.contains("--help") || args.contains("-h") -> printHelp()
        args.contains("--benchmark") -> Benchmark.run()
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
            )
        }
    }
}

private fun printHelp() {
    println(
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
          ./scripts/gradlew-local.sh run --args="--render build/prototype"
          ./scripts/gradlew-local.sh run --args="--benchmark"

        Keys:
          Left / PgUp / Backspace   previous spread
          Right / PgDn / Space      next spread
          V                         toggle vertical / horizontal
          D                         toggle single / double page
          T                         toggle theme
          B                         toggle low-key mode
        """.trimIndent(),
    )
}
