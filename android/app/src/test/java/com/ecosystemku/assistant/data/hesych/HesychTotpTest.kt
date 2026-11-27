package com.ecosystemku.assistant.data.hesych

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesychTotpTest {

    @Test
    fun base32DecodesRfc4648AndToleratesLowercaseWithoutPadding() {
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        assertEquals("12345678901234567890", String(HesychTotp.base32Decode(secret), StandardCharsets.US_ASCII))
        assertEquals(
            "12345678901234567890",
            String(HesychTotp.base32Decode(secret.lowercase()), StandardCharsets.US_ASCII)
        )

        assertTrue(HesychTotp.isValidBase32("JBSWY3DPEHPK3PXP"))
        assertFalse(HesychTotp.isValidBase32(""))
        assertFalse(HesychTotp.isValidBase32("ABC{"))
    }

    @Test
    fun generateTotpMatchesRfc6238Sha1Vectors() {
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        assertEquals("287082", HesychTotp.generateTotp(secret, 59L))
        assertEquals("081804", HesychTotp.generateTotp(secret, 1111111109L))
        assertEquals("050471", HesychTotp.generateTotp(secret, 1111111111L))
        assertEquals("279037", HesychTotp.generateTotp(secret, 2000000000L))
        assertEquals("353130", HesychTotp.generateTotp(secret, 20000000000L))
    }

    @Test
    fun secondsRemainingAlwaysWithin1To30() {
        val rem = HesychTotp.secondsRemaining(1_700_000_015_000L)
        assertTrue(rem in 1..30)
    }
}
