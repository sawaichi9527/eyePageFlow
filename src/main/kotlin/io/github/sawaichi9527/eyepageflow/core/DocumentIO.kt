package io.github.sawaichi9527.eyepageflow.core

import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Path

class DocumentDecodeException(message: String) : Exception(message)

sealed interface SaveResult {
    data class Success(val document: TextDocument) : SaveResult

    data class Unmappable(
        val encoding: TextEncoding,
        val characters: List<Char>,
    ) : SaveResult

    data class Failure(val message: String, val cause: Throwable? = null) : SaveResult
}

object DocumentIO {
    fun read(path: Path): TextDocument {
        val bytes = Files.readAllBytes(path)
        val decoded = EncodingDetector.decode(bytes)
        if (decoded.replacementCount > 0) {
            throw DocumentDecodeException(
                "檔案包含 ${decoded.replacementCount} 個無法以 ${decoded.encoding.displayName} 解碼的位元組。",
            )
        }
        return TextDocument(
            path = path,
            text = decoded.text,
            encoding = decoded.encoding,
            hadBom = decoded.hadBom,
            lineEnding = decoded.lineEnding,
        )
    }

    fun newDocument(text: String, path: Path? = null): TextDocument = TextDocument(
        path = path,
        text = text,
        encoding = TextEncoding.UTF8,
        hadBom = false,
        lineEnding = LineEnding.LF,
    )

    fun save(
        document: TextDocument,
        targetPath: Path,
        encodingOverride: TextEncoding? = null,
        bomOverride: Boolean? = null,
    ): SaveResult {
        val encoding = encodingOverride ?: document.encoding
        val withBom = bomOverride ?: if (encodingOverride == null) {
            document.hadBom
        } else {
            encoding == TextEncoding.UTF16LE || encoding == TextEncoding.UTF16BE
        }

        val bytes = try {
            encode(document.text, encoding, withBom)
        } catch (_: CharacterCodingException) {
            return SaveResult.Unmappable(
                encoding = encoding,
                characters = findUnmappable(document.text, encoding),
            )
        } catch (exception: Exception) {
            return SaveResult.Failure("編碼失敗：${exception.message}", exception)
        }

        return try {
            Files.write(targetPath, bytes)
            SaveResult.Success(document.markSaved(targetPath, encoding, withBom))
        } catch (exception: Exception) {
            SaveResult.Failure("寫入失敗：${exception.message}", exception)
        }
    }

    fun encode(
        text: String,
        encoding: TextEncoding,
        withBom: Boolean = false,
    ): ByteArray {
        val encoder = encoding.charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val buffer = encoder.encode(CharBuffer.wrap(text))
        val payload = ByteArray(buffer.remaining())
        buffer.get(payload)
        if (!withBom) return payload
        val bom = encoding.bom ?: return payload
        return bom + payload
    }

    fun findUnmappable(text: String, encoding: TextEncoding): List<Char> {
        val encoder = encoding.charset.newEncoder()
        val seen = LinkedHashSet<Char>()
        text.forEach { char ->
            if (!encoder.canEncode(char)) {
                seen += char
                if (seen.size >= 20) return seen.toList()
            }
        }
        return seen.toList()
    }
}
