package io.github.sawaichi9527.eyepageflow.platform

/**
 * 依執行環境挑選 AWT toolkit。
 *
 * 先前 `jpackage` 產生的 launcher 把 `-Dawt.toolkit.name=WLToolkit` 寫死，
 * 在 X11 session 或非 JBR runtime 上會直接起不來。改成執行時判斷：
 * 只有「Linux + Wayland session + JVM 真的有 JBR 的 Wayland toolkit」才選 WLToolkit，
 * 其餘一律沿用 JVM 預設（XToolkit／Windows 的 WToolkit）。
 *
 * 覆寫要用本專案自己的鍵 `-Deyepageflow.toolkit=`（或環境變數 `EYEPAGEFLOW_TOOLKIT`）。
 * 不能靠 `awt.toolkit.name`：JBR 啟動時就會把 `-Dawt.toolkit.name=XToolkit` 塞進
 * JVM 參數，跟使用者自己指定的長得一模一樣。
 */
object ToolkitSelection {
    const val WAYLAND_TOOLKIT = "WLToolkit"
    const val OVERRIDE_PROPERTY = "eyepageflow.toolkit"
    const val OVERRIDE_ENV = "EYEPAGEFLOW_TOOLKIT"

    /** 回傳應該設定的 toolkit 名稱；回傳 null 表示沿用 JVM 預設。 */
    fun decide(
        osName: String,
        sessionType: String?,
        hasWaylandToolkit: Boolean,
        override: String?,
    ): String? {
        if (override != null) return override
        if (!osName.lowercase().contains("linux")) return null
        if (!sessionType.equals("wayland", ignoreCase = true)) return null
        return if (hasWaylandToolkit) WAYLAND_TOOLKIT else null
    }
}
