package com.ecosystemku.assistant.data.hesych

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HesychPasswordGeneratorTest {

    @Test
    fun randomPasswordRespectsLengthClassesAndAmbiguousExclusion() {
        val opts = HesychGenOptions(
            mode = HesychGenMode.PASSWORD,
            length = 20,
            upper = true,
            lower = true,
            numbers = true,
            symbols = true,
            excludeAmbiguous = true
        )
        val pw = HesychPasswordGenerator.generateOne(opts)
        assertEquals(20, pw.length)
        assertTrue(pw.any { it in 'A'..'Z' })
        assertTrue(pw.any { it in 'a'..'z' })
        assertTrue(pw.any { it in '0'..'9' })
        assertTrue(pw.any { !it.isLetterOrDigit() })
        assertFalse(pw.any { it in "0O1lI" })
    }

    @Test
    fun passphraseRespectsWordCountSeparatorAndNumberSuffix() {
        val opts = HesychGenOptions(
            mode = HesychGenMode.PASSPHRASE,
            words = 4,
            separator = "-",
            capitalize = true,
            includeNumber = true
        )
        val phrase = HesychPasswordGenerator.generateOne(opts)
        val parts = phrase.split("-")
        assertEquals(5, parts.size)
        assertTrue(parts.last().toInt() in 10..99)
        assertTrue(parts.take(4).all { it.first().isUpperCase() })
    }

    @Test
    fun strengthScoringMatchesExpectedBands() {
        assertEquals(0, HesychPasswordGenerator.scorePassword(""))
        assertEquals(1, HesychPasswordGenerator.scorePassword("abc"))
        assertEquals(2, HesychPasswordGenerator.scorePassword("abcdefgh"))
        assertEquals(3, HesychPasswordGenerator.scorePassword("Abcdefgh1234"))
        assertEquals(4, HesychPasswordGenerator.scorePassword("Abcdefgh1234!@#"))
    }
}
