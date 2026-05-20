package com.example.alphacinema.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HashUtils {
    fun hmacSha256(data: String, key: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(key.toByteArray(), algorithm)
        mac.init(secretKeySpec)
        val bytes = mac.doFinal(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
