package io.github.sawaichi9527.eyepageflow.core

import java.nio.file.Path

data class TextDocument(
    val path: Path?,
    val text: String,
    val encoding: TextEncoding,
    val hadBom: Boolean,
    val lineEnding: LineEnding,
    val modified: Boolean = false,
) {
    val displayName: String
        get() = path?.fileName?.toString() ?: "未命名"

    fun withText(newText: String): TextDocument = copy(text = newText, modified = true)

    fun markSaved(
        targetPath: Path,
        savedEncoding: TextEncoding,
        savedWithBom: Boolean,
    ): TextDocument = copy(
        path = targetPath,
        encoding = savedEncoding,
        hadBom = savedWithBom,
        modified = false,
    )
}
