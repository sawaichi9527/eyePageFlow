package io.github.sawaichi9527.eyepageflow.core

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

enum class TextEncoding(
    val charsetName: String,
    val displayName: String,
    val bom: ByteArray?,
) {
    UTF8(
        "UTF-8",
        "UTF-8",
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
    ),
    UTF16LE(
        "UTF-16LE",
        "UTF-16 LE",
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()),
    ),
    UTF16BE(
        "UTF-16BE",
        "UTF-16 BE",
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
    ),
    BIG5("Big5", "Big5", null),
    GB18030("GB18030", "GB18030", null),
    ;

    val charset: Charset
        get() = Charset.forName(charsetName)
}

enum class LineEnding {
    LF,
    CRLF,
    CR,
    MIXED,
}

data class DecodedText(
    val text: String,
    val encoding: TextEncoding,
    val hadBom: Boolean,
    val lineEnding: LineEnding,
    val replacementCount: Int,
)

object EncodingDetector {
    fun decode(bytes: ByteArray): DecodedText {
        if (bytes.isEmpty()) {
            return DecodedText("", TextEncoding.UTF8, false, LineEnding.LF, 0)
        }

        TextEncoding.UTF8.bom?.let { bom ->
            if (bytes.startsWith(bom)) {
                return buildResult(bytes, bom.size, TextEncoding.UTF8, true)
            }
        }
        TextEncoding.UTF16LE.bom?.let { bom ->
            if (bytes.startsWith(bom)) {
                return buildResult(bytes, bom.size, TextEncoding.UTF16LE, true)
            }
        }
        TextEncoding.UTF16BE.bom?.let { bom ->
            if (bytes.startsWith(bom)) {
                return buildResult(bytes, bom.size, TextEncoding.UTF16BE, true)
            }
        }

        strictDecode(bytes, TextEncoding.UTF8.charset)?.let { text ->
            return DecodedText(
                text = text,
                encoding = TextEncoding.UTF8,
                hadBom = false,
                lineEnding = detectLineEnding(text),
                replacementCount = 0,
            )
        }

        // Big5 is the primary legacy encoding for this project. GB18030 is the
        // fallback for simplified-Chinese files that are not valid Big5.
        strictDecode(bytes, TextEncoding.BIG5.charset)?.let { text ->
            return DecodedText(
                text = text,
                encoding = TextEncoding.BIG5,
                hadBom = false,
                lineEnding = detectLineEnding(text),
                replacementCount = 0,
            )
        }

        strictDecode(bytes, TextEncoding.GB18030.charset)?.let { text ->
            return DecodedText(
                text = text,
                encoding = TextEncoding.GB18030,
                hadBom = false,
                lineEnding = detectLineEnding(text),
                replacementCount = 0,
            )
        }

        return buildResult(bytes, 0, TextEncoding.BIG5, false)
    }

    private fun buildResult(
        bytes: ByteArray,
        offset: Int,
        encoding: TextEncoding,
        hadBom: Boolean,
    ): DecodedText {
        val payload = if (offset == 0) bytes else bytes.copyOfRange(offset, bytes.size)
        val strict = strictDecode(payload, encoding.charset)
        val text = strict ?: decodeWithReplacement(payload, encoding.charset)
        val replacementCount = text.count { it == '\uFFFD' }
        val withoutLeadingBom = text.removePrefix("\uFEFF")
        return DecodedText(
            text = withoutLeadingBom,
            encoding = encoding,
            hadBom = hadBom,
            lineEnding = detectLineEnding(withoutLeadingBom),
            replacementCount = replacementCount,
        )
    }

    private fun strictDecode(bytes: ByteArray, charset: Charset): String? = try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun decodeWithReplacement(bytes: ByteArray, charset: Charset): String =
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
            .decode(ByteBuffer.wrap(bytes))
            .toString()

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (index in prefix.indices) {
            if (this[index] != prefix[index]) return false
        }
        return true
    }

    private fun detectLineEnding(text: String): LineEnding {
        var crlf = 0
        var lf = 0
        var cr = 0
        var index = 0
        while (index < text.length) {
            when (text[index]) {
                '\r' -> {
                    if (index + 1 < text.length && text[index + 1] == '\n') {
                        crlf += 1
                        index += 1
                    } else {
                        cr += 1
                    }
                }
                '\n' -> lf += 1
            }
            index += 1
        }
        val kinds = listOf(crlf, lf, cr).count { it > 0 }
        return when {
            kinds == 0 -> LineEnding.LF
            kinds > 1 -> LineEnding.MIXED
            crlf > 0 -> LineEnding.CRLF
            cr > 0 -> LineEnding.CR
            else -> LineEnding.LF
        }
    }
}
