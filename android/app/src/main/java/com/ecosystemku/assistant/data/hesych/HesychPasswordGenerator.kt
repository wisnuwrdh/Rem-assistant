package com.ecosystemku.assistant.data.hesych

import java.security.SecureRandom
import java.util.Locale

enum class HesychGenMode { PASSWORD, PASSPHRASE }

data class HesychGenOptions(
    val mode: HesychGenMode = HesychGenMode.PASSWORD,
    val length: Int = 16,
    val words: Int = 4,
    val separator: String = "-",
    val upper: Boolean = true,
    val lower: Boolean = true,
    val numbers: Boolean = true,
    val symbols: Boolean = true,
    val excludeAmbiguous: Boolean = true,
    val capitalize: Boolean = true,
    val includeNumber: Boolean = true
)

/**
 * Cryptographically secure password and passphrase generator + strength scorer,
 * matching Hesych Web (`lib/password.ts` and `lib/passphrase.ts`).
 */
object HesychPasswordGenerator {
    private const val AMBIGUOUS = "0O1lI"
    private val random = SecureRandom()

    val WORDLIST: List<String> = listOf(
        "apple", "beach", "cloud", "dance", "eagle", "flame", "grape", "honey", "igloo", "jungle",
        "kite", "lemon", "maple", "night", "ocean", "pizza", "queen", "river", "storm", "tiger",
        "ultra", "vivid", "water", "xenon", "yacht", "zebra", "amber", "brave", "crisp", "delta",
        "ember", "frost", "gleam", "hyper", "ivory", "joker", "karma", "lunar", "magic", "noble",
        "orbit", "pearl", "quill", "radar", "solar", "tidal", "umbra", "vapor", "woven", "xylem",
        "yield", "zonal", "atlas", "blaze", "cedar", "drift", "elbow", "fable", "glide", "haven",
        "inert", "jewel", "kneel", "lance", "metro", "nerve", "onset", "prism", "quest", "relay",
        "spine", "trove", "unity", "verse", "whirl", "expel", "zesty", "acorn", "bloom", "comet",
        "dunes", "epoch", "flint", "glyph", "haste", "inlet", "jarls", "knack", "lumen", "manor",
        "nexus", "optic", "pixel", "quota", "relic", "sweep", "thorn", "untie", "visor", "waltz",
        "xerox", "yearn", "zephyr", "abode", "brine", "crest", "dwelt", "exile", "forge", "grove",
        "heron", "irony", "joust", "knave", "lyric", "marsh", "nymph", "outdo", "plume", "quirk",
        "resin", "swift", "tryst", "usher", "vigor", "wrath", "expat", "zippy", "acrid", "blunt",
        "cabin", "depot", "epoch", "fleck", "guild", "hyena", "irked", "joist", "knelt", "latch",
        "medal", "nudge", "oaken", "plank", "qualm", "rusty", "scone", "tabby", "ulcer", "venom",
        "wader", "extol", "zonal", "angel", "bride", "crimp", "donut", "ether", "funky", "gavel",
        "hinds", "index", "joust", "knobs", "lilac", "mirth", "noble", "oxide", "pilot", "query",
        "renew", "scald", "taboo", "upset", "vault", "weary", "expel", "zippo", "alibi", "boxer",
        "chasm", "disco", "ember", "fjord", "guava", "hippo", "ingot", "jelly", "kudos", "lingo"
    )

    fun scorePassword(pw: String): Int {
        if (pw.isEmpty()) return 0
        var score = 0
        if (pw.length >= 8) score++
        if (pw.length >= 12) score++
        if (pw.any { it in 'A'..'Z' } && pw.any { it in 'a'..'z' }) score++
        if (pw.any { it in '0'..'9' }) score++
        if (pw.any { !it.isLetterOrDigit() }) score++
        return when {
            score <= 1 -> 1
            score <= 2 -> 2
            score <= 3 -> 3
            else -> 4
        }
    }

    fun strengthLabel(score: Int): String = when (score) {
        1 -> "Lemah"
        2 -> "Cukup"
        3 -> "Kuat"
        4 -> "Sangat Kuat"
        else -> ""
    }

    fun quickGenerate(): String {
        val chars = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*"
        val sb = StringBuilder(18)
        repeat(18) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    fun generateRandomPassword(opts: HesychGenOptions): String {
        val targetLen = opts.length.coerceIn(4, 128)
        val pools = mutableListOf<String>()
        if (opts.upper) pools += if (opts.excludeAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (opts.lower) pools += if (opts.excludeAmbiguous) "abcdefghjkmnpqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
        if (opts.numbers) pools += if (opts.excludeAmbiguous) "23456789" else "0123456789"
        if (opts.symbols) pools += "!@#$%^&*-_+=?"

        val activePools = pools.ifEmpty { listOf("abcdefghijklmnopqrstuvwxyz") }
        val all = activePools.joinToString("")

        fun pickFrom(pool: String): Char {
            while (true) {
                val c = pool[random.nextInt(pool.length)]
                if (!opts.excludeAmbiguous || c !in AMBIGUOUS) return c
            }
        }

        val out = ArrayList<Char>(targetLen)
        for (pool in activePools) {
            if (out.size >= targetLen) break
            out.add(pickFrom(pool))
        }
        while (out.size < targetLen) {
            out.add(pickFrom(all))
        }
        for (i in out.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = out[i]
            out[i] = out[j]
            out[j] = tmp
        }
        return String(out.toCharArray())
    }

    fun generatePassphrase(opts: HesychGenOptions): String {
        val count = opts.words.coerceIn(2, 12)
        val words = (0 until count).map {
            val raw = WORDLIST[random.nextInt(WORDLIST.size)]
            if (opts.capitalize) {
                raw.replaceFirstChar { ch -> ch.titlecase(Locale.US) }
            } else {
                raw
            }
        }
        val base = words.joinToString(opts.separator)
        return if (opts.includeNumber) {
            val num = random.nextInt(90) + 10
            "$base${opts.separator}$num"
        } else {
            base
        }
    }

    fun generateOne(opts: HesychGenOptions): String {
        return if (opts.mode == HesychGenMode.PASSPHRASE) {
            generatePassphrase(opts)
        } else {
            generateRandomPassword(opts)
        }
    }
}
