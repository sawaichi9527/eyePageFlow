package io.github.sawaichi9527.eyepageflow.core

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * 最小可用的 i18n 入口：主視窗、狀態列與說明視窗的文字集中在這裡。
 *
 * 語言由 `-Deyepageflow.lang=` 或 `EYEPAGEFLOW_LANG` 決定，預設繁體中文。
 * 找不到的鍵會先退回英文、再退回繁中，所以新增語言時不必一次翻完。
 * 對話框（顯示設定、尋找／取代等）目前仍直接寫死中文。
 */
object Messages {
    private const val BASE_RESOURCE = "i18n/messages.properties"

    private var language: String = "zh-TW"
    private var table: Properties? = null

    val currentLanguage: String
        get() {
            load()
            return language
        }

    operator fun get(key: String): String {
        load()
        return table?.getProperty(key) ?: key
    }

    fun format(key: String, vararg args: Any): String = String.format(get(key), *args)

    /** 測試與啟動參數用；傳 null 會重新依系統屬性／環境變數決定。 */
    fun init(requested: String? = null) {
        synchronized(this) {
            table = null
        }
        load(requested)
    }

    private fun load(requested: String? = null) {
        if (table != null) return
        val resolved = normalize(
            requested
                ?: System.getProperty("eyepageflow.lang")
                ?: System.getenv("EYEPAGEFLOW_LANG")
                ?: "zh-TW",
        )
        val merged = Properties().apply { putAll(read(BASE_RESOURCE)) }
        if (resolved != "en" && resolved != "zh-TW") {
            merged.putAll(read(resourceFor("en")))
        }
        if (resolved != "zh-TW") {
            merged.putAll(read(resourceFor(resolved)))
        }
        synchronized(this) {
            table = merged
            language = resolved
        }
    }

    private fun normalize(value: String): String = when {
        value.startsWith("ja", ignoreCase = true) -> "ja"
        value.startsWith("en", ignoreCase = true) -> "en"
        else -> "zh-TW"
    }

    private fun resourceFor(lang: String): String = when (lang) {
        "en" -> "i18n/messages_en.properties"
        "ja" -> "i18n/messages_ja.properties"
        else -> BASE_RESOURCE
    }

    private fun read(resource: String): Properties {
        val stream = Messages::class.java.getResourceAsStream("/$resource")
            ?: return Properties()
        return Properties().apply {
            InputStreamReader(stream, StandardCharsets.UTF_8).use { load(it) }
        }
    }
}
