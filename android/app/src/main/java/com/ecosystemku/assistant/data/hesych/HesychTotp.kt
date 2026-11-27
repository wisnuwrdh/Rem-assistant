package com.ecosystemku.assistant.data.hesych

import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 6238 / Google Authenticator 6-digit TOTP (HMAC-SHA1, 30-second window)
 * with RFC 4648 Base32 decoding.
 */
object HesychTotp {
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun isValidBase32(raw: String): Boolean {
        val clean = raw.trim().replace(" ", "").uppercase(Locale.US)
        if (clean.isEmpty()) return false
        return clean.matches(Regex("^[A-Z2-7]+=*$")) && clean.replace("=", "").isNotEmpty()
    }

    fun base32Decode(raw: String): ByteArray {
        val clean = raw.trim()
            .replace(" ", "")
            .replace(Regex("=+$"), "")
            .uppercase(Locale.US)
        if (clean.isEmpty()) throw IllegalArgumentException("Empty base32 secret")

        var bits = 0
        var value = 0
        val out = ArrayList<Byte>((clean.length * 5) / 8)
        for (ch in clean) {
            val idx = BASE32.indexOf(ch)
            if (idx == -1) throw IllegalArgumentException("Invalid base32 character: $ch")
            value = (value shl 5) or idx
            bits += 5
            if (bits >= 8) {
                out.add(((value ushr (bits - 8)) and 0xff).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }

    fun generateTotp(
        secret: String,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): String {
        val keyBytes = base32Decode(secret)
        var counter = Math.floorDiv(timeSeconds, 30L)
        val msg = ByteArray(8)
        for (i in 7 downTo 0) {
            msg[i] = (counter and 0xffL).toByte()
            counter = counter ushr 8
        }

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hmac = mac.doFinal(msg)

        val offset = hmac[19].toInt() and 0x0f
        val code = ((hmac[offset].toInt() and 0x7f) shl 24) or
            ((hmac[offset + 1].toInt() and 0xff) shl 16) or
            ((hmac[offset + 2].toInt() and 0xff) shl 8) or
            (hmac[offset + 3].toInt() and 0xff)

        return (code % 1_000_000).toString().padStart(6, '0')
    }

    fun secondsRemaining(nowMs: Long = System.currentTimeMillis()): Int {
        val sec = Math.floorDiv(nowMs, 1000L)
        return (30L - (sec % 30L)).toInt()
    }
}
