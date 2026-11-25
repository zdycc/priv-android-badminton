package com.example.ding_badminton.data.submit

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoCodec {
    private val keyBytes = "9eb67u12c45cf59e".toByteArray(StandardCharsets.UTF_8)
    private val ivBytes = "e1c67e234f7f3c1e".toByteArray(StandardCharsets.UTF_8)

    fun encryptZeroPaddedToBase64(plainText: String): String {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        val iv = IvParameterSpec(ivBytes)
        val input = plainText.toByteArray(StandardCharsets.UTF_8)
        val padded = zeroPad(input, 16)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val encrypted = cipher.doFinal(padded)
        // 与网页端一致：先 AES -> Base64(cipherBytes)，再对这个字符串做一次 Base64。
        val inner = Base64.getEncoder().encodeToString(encrypted)
        return Base64.getEncoder().encodeToString(inner.toByteArray(StandardCharsets.UTF_8))
    }

    fun decryptBase64AesCbcZeroPadded(cipherTextBase64: String): String {
        // 优先按双层 Base64 解密；失败时回退单层，兼容历史链路。
        runCatching {
            val outerDecoded = Base64.getDecoder().decode(cipherTextBase64)
            val innerBase64 = String(outerDecoded, StandardCharsets.UTF_8)
            return decryptSingleLayer(innerBase64)
        }
        return decryptSingleLayer(cipherTextBase64)
    }

    private fun decryptSingleLayer(cipherTextBase64: String): String {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        val iv = IvParameterSpec(ivBytes)
        val encrypted = Base64.getDecoder().decode(cipherTextBase64)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plain = cipher.doFinal(encrypted)
        return stripZeroTail(plain).toString(StandardCharsets.UTF_8).trim()
    }

    private fun zeroPad(src: ByteArray, blockSize: Int): ByteArray {
        val r = src.size % blockSize
        if (r == 0) return src
        val target = src.size + (blockSize - r)
        return src.copyOf(target)
    }

    private fun stripZeroTail(bytes: ByteArray): ByteArray {
        var end = bytes.size
        while (end > 0 && bytes[end - 1].toInt() == 0) {
            end--
        }
        return bytes.copyOf(end)
    }
}
