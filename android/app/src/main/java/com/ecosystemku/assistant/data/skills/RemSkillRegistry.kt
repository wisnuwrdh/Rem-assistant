package com.ecosystemku.assistant.data.skills

import android.content.Context
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class ActiveRemSkill(
    val id: String,
    val title: String,
    val body: String
)

object RemSkillSelector {
    const val SKILL_DIGITAL_TWIN = "digital-twin-interviewer"
    const val SKILL_WEEKLY_REVIEW = "weekly-life-review"
    const val SKILL_FINANCIAL_ADVISOR = "cucimoney-financial-advisor"
    const val SKILL_KRONIK_THERAPIST = "kronik-reflection-therapist"
    const val SKILL_IDEKU_STRATEGIST = "ideku-execution-strategist"

    private val SKILL_TITLES = mapOf(
        SKILL_DIGITAL_TWIN to "Digital Twin Interviewer",
        SKILL_WEEKLY_REVIEW to "Weekly Life Review",
        SKILL_FINANCIAL_ADVISOR to "CuciMoney Financial Advisor",
        SKILL_KRONIK_THERAPIST to "Kronik Reflection Therapist",
        SKILL_IDEKU_STRATEGIST to "IdeKu Execution Strategist"
    )

    private val WEEKLY_REVIEW_PHRASES = listOf(
        "review mingguan",
        "review minggu ini",
        "evaluasi mingguan",
        "evaluasi minggu ini",
        "rekap mingguan",
        "rekap minggu ini",
        "rangkum minggu ini",
        "weekly review",
        "life review",
        "lintas modul",
        "semua modul"
    )

    private val FINANCIAL_PHRASES = listOf(
        "cashflow",
        "arus kas",
        "audit keuangan",
        "cek keuangan",
        "kondisi keuangan",
        "master budget",
        "over budget",
        "kebocoran",
        "duit cepet habis",
        "duit cepat habis",
        "uang cepat habis",
        "uang cepet habis",
        "strategi anggaran",
        "pembagian anggaran",
        "mampu beli",
        "worth it"
    )

    private val REFLECTION_PHRASES = listOf(
        "overthinking",
        "nyalahin diri",
        "salahin diri",
        "menyalahkan diri",
        "merasa gagal",
        "gagal total",
        "gak kompeten",
        "tidak kompeten",
        "burnout",
        "lelah mental",
        "capek mental",
        "pemicu emosi",
        "refleksi diri",
        "refleksi emosi",
        "curhat",
        "kepikiran"
    )

    private val IDEKU_PHRASES = listOf(
        "ideku menumpuk",
        "ideku numpuk",
        "ide menumpuk",
        "ide numpuk",
        "kebanyakan ide",
        "terlalu banyak ide",
        "fokus ke mana",
        "fokus kemana",
        "proyek mana dulu",
        "bingung mulai dari mana",
        "punya ide baru",
        "kepikiran ide",
        "next step",
        "langkah pertama",
        "proses vs parkir",
        "parkir ide"
    )

    fun titleFor(skillId: String): String = SKILL_TITLES[skillId] ?: skillId

    fun stripFrontmatter(rawMarkdown: String): String {
        val normalized = rawMarkdown.replace("\r\n", "\n").trimStart()
        if (!normalized.startsWith("---\n")) {
            return rawMarkdown.trim()
        }
        val closeIdx = normalized.indexOf("\n---", startIndex = 4)
        if (closeIdx < 0) {
            return rawMarkdown.trim()
        }
        val afterClose = closeIdx + 4
        return normalized.substring(afterClose).trim()
    }

    fun selectSkillId(
        userMessage: String,
        sessionTitle: String?,
        isKenaliAku: Boolean
    ): String? {
        if (isKenaliAku || sessionTitle?.contains("Kenali Aku", ignoreCase = true) == true) {
            return SKILL_DIGITAL_TWIN
        }
        val text = userMessage.trim().lowercase(Locale.ROOT)
        if (text.isEmpty()) return null

        if (WEEKLY_REVIEW_PHRASES.any { text.contains(it) }) {
            return SKILL_WEEKLY_REVIEW
        }
        if (matchesFinancialAdvice(text)) {
            return SKILL_FINANCIAL_ADVISOR
        }
        if (matchesKronikReflection(text)) {
            return SKILL_KRONIK_THERAPIST
        }
        if (matchesIdekuStrategy(text)) {
            return SKILL_IDEKU_STRATEGIST
        }
        return null
    }

    private fun matchesFinancialAdvice(text: String): Boolean {
        if (FINANCIAL_PHRASES.any { text.contains(it) }) return true

        val asksWishlistDecision = text.contains("wishlist") && hasAny(
            text,
            "beli", "boleh", "layak", "cukup", "tahan", "tunda", "cicil", "menurut", "gimana"
        )
        if (asksWishlistDecision) return true

        val asksBudgetAnalysis = (text.contains("keuangan") || text.contains("pengeluaran") || text.contains("budget") || text.contains("anggaran")) &&
            hasAny(text, "analisis", "evaluasi", "bocor", "boros", "hemat", "saran", "menurut", "kenapa", "cek", "atur", "bagi")
        return asksBudgetAnalysis
    }

    private fun matchesKronikReflection(text: String): Boolean {
        if (REFLECTION_PHRASES.any { text.contains(it) }) return true

        val mentionsJournal = text.contains("kronik") || text.contains("jurnal")
        if (mentionsJournal && hasAny(text, "refleksi", "mood", "emosi", "pola", "baca ulang", "evaluasi", "sedih", "stres", "stress", "berat", "tertekan")) {
            return true
        }

        return hasAny(text, "stres", "stress", "tertekan", "cemas", "frustrasi", "kecewa berat", "down banget", "sedih banget")
    }

    private fun matchesIdekuStrategy(text: String): Boolean {
        if (IDEKU_PHRASES.any { text.contains(it) }) return true

        val mentionsIde = text.contains("ideku") || Regex("""\bide\b""").containsMatchIn(text) || text.contains("proyek")
        return mentionsIde && hasAny(
            text,
            "rapikan", "rapihin", "pilah", "fokus", "prioritas", "parkir", "validasi", "layak", "eksekusi", "pecah", "gabung"
        )
    }

    private fun hasAny(text: String, vararg terms: String): Boolean = terms.any { text.contains(it) }
}

class RemSkillRegistry(private val context: Context) {
    private val cache = ConcurrentHashMap<String, String>()

    fun resolveSkill(
        userMessage: String,
        sessionTitle: String?,
        isKenaliAku: Boolean
    ): ActiveRemSkill? {
        val skillId = RemSkillSelector.selectSkillId(
            userMessage = userMessage,
            sessionTitle = sessionTitle,
            isKenaliAku = isKenaliAku
        ) ?: return null

        val body = loadSkillBody(skillId) ?: return null
        return ActiveRemSkill(
            id = skillId,
            title = RemSkillSelector.titleFor(skillId),
            body = body
        )
    }

    private fun loadSkillBody(skillId: String): String? {
        cache[skillId]?.let { return it }
        val loaded = runCatching {
            context.assets.open("skills/$skillId.md").bufferedReader(Charsets.UTF_8).use { reader ->
                RemSkillSelector.stripFrontmatter(reader.readText())
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        cache[skillId] = loaded
        return loaded
    }
}
