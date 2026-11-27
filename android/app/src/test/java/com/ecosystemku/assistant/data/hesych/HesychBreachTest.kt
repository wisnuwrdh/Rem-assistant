package com.ecosystemku.assistant.data.hesych

import org.junit.Assert.assertEquals
import org.junit.Test

class HesychBreachTest {

    @Test
    fun sha1HexMatchesKnownHibpVectorForPassword() {
        // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        val hash = HesychBreachChecker.sha1Hex("password")
        assertEquals("5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8", hash)
    }

    @Test
    fun parseRangeResponseExtractsExactSuffixCount() {
        val body = """
            0018A45C4D1DEF81644B54AB7F969B88D65:21
            1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493
            FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF:0
        """.trimIndent()

        assertEquals(
            3861493,
            HesychBreachChecker.parseRangeResponse(body, "1e4c9b93f3f0682250b6cf8331b7ee68fd8")
        )
        assertEquals(
            0,
            HesychBreachChecker.parseRangeResponse(body, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        )
    }
}
