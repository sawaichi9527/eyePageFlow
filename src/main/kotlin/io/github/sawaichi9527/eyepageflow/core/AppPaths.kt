package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Path

object AppPaths {
    fun configDir(): Path {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("win")) {
            val appData = System.getenv("APPDATA")
            val base = if (!appData.isNullOrBlank()) {
                Path.of(appData)
            } else {
                Path.of(System.getProperty("user.home"))
            }
            return base.resolve("eyePageFlow")
        }
        val xdg = System.getenv("XDG_CONFIG_HOME")
        val base = if (!xdg.isNullOrBlank()) {
            Path.of(xdg)
        } else {
            Path.of(System.getProperty("user.home"), ".config")
        }
        return base.resolve("eyePageFlow")
    }

    fun settingsFile(): Path = configDir().resolve("settings.properties")

    fun bookmarksFile(): Path = configDir().resolve("bookmarks.properties")

    fun recentFilesFile(): Path = configDir().resolve("recent-files.properties")

    fun readingPositionsFile(): Path = configDir().resolve("reading-positions.properties")

    fun libraryFile(): Path = configDir().resolve("library.properties")

    fun instanceLockFile(): Path = configDir().resolve("instance.lock")

    fun instancePortFile(): Path = configDir().resolve("instance.port")
}
