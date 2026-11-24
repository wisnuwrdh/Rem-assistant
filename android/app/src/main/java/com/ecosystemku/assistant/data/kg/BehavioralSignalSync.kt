package com.ecosystemku.assistant.data.kg

import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsHelpers
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mengekstrak sinyal perilaku objektif dari 5 modul produktivitas ke Knowledge Graph
 * secara deterministik lokal (0 token LLM). Tautan privat LinkKu selalu dikecualikan.
 */
object BehavioralSignalExtractor {

    private val LOW_ENERGY_MOODS = setOf("Lelah", "Berat", "Kabur")
    private val HIGH_ENERGY_MOODS = setOf("Fokus", "Semangat", "Terinspirasi")

    fun extractTriples(
        profileName: String,
        habits: List<DailyOsHabitEntity>,
        kronikEntries: List<KronikEntryEntity>,
        ideas: List<IdekuIdeaEntity>,
        transactions: List<CuciTransactionEntity>,
        cuciCategories: List<CuciCategoryEntity>,
        links: List<LinkkuLinkEntity>,
        linkkuCategories: List<LinkkuCategoryEntity>,
        todayStr: String = DailyOsHelpers.todayStr()
    ): List<ExtractedTripleDto> {
        val selfLabel = profileName.trim().ifBlank { "Tuan" }
        val result = mutableListOf<ExtractedTripleDto>()

        // 1. Daily OS: habit konsisten (streak >= 3)
        val activeHabits = habits.filter { !it.archived }
        activeHabits.forEach { habit ->
            val streak = DailyOsHelpers.calculateHabitStreak(habit, todayStr)
            if (streak >= 3) {
                result.add(
                    ExtractedTripleDto(
                        fromType = "SELF",
                        fromLabel = selfLabel,
                        relation = "KONSISTEN_PADA",
                        toType = "KEBIASAAN",
                        toLabel = habit.name,
                        evidence = "Streak aktif $streak hari berturut-turut di Daily OS",
                        confidence = 0.92f
                    )
                )
            }
        }

        // 2. Kronik + Daily OS: korelasi mood vs habit selesai/terlewat
        val parsedHistories = activeHabits.associate { it.id to DailyOsHelpers.parseHistory(it.historyJson) }
        val activeJournals = kronikEntries.filter { !it.isArchived && !it.moodStatus.isNullOrBlank() }

        val lowMoodMissedHabitCounts = mutableMapOf<Pair<String, String>, Int>()
        val highMoodDoneHabitCounts = mutableMapOf<Pair<String, String>, Int>()

        activeJournals.forEach { entry ->
            val mood = entry.moodStatus?.trim().orEmpty()
            val ds = entry.date.trim()
            if (ds.length < 10) return@forEach

            activeHabits.forEach { habit ->
                if (!DailyOsHelpers.isHabitScheduled(habit, ds)) return@forEach
                val done = parsedHistories[habit.id]?.get(ds).orEmpty().isNotEmpty()
                if (mood in LOW_ENERGY_MOODS && !done) {
                    val key = mood to habit.name
                    lowMoodMissedHabitCounts[key] = (lowMoodMissedHabitCounts[key] ?: 0) + 1
                } else if (mood in HIGH_ENERGY_MOODS && done) {
                    val key = mood to habit.name
                    highMoodDoneHabitCounts[key] = (highMoodDoneHabitCounts[key] ?: 0) + 1
                }
            }
        }

        lowMoodMissedHabitCounts.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(3)
            .forEach { (pair, count) ->
                result.add(
                    ExtractedTripleDto(
                        fromType = "EMOSI_PEMICU",
                        fromLabel = "Mood ${pair.first}",
                        relation = "MENURUNKAN",
                        toType = "KEBIASAAN",
                        toLabel = pair.second,
                        evidence = "Teramati terlewat $count kali di hari dengan mood ${pair.first} (Kronik x Daily OS)",
                        confidence = 0.8f
                    )
                )
            }

        highMoodDoneHabitCounts.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(3)
            .forEach { (pair, count) ->
                result.add(
                    ExtractedTripleDto(
                        fromType = "EMOSI_PEMICU",
                        fromLabel = "Mood ${pair.first}",
                        relation = "MENDUKUNG",
                        toType = "KEBIASAAN",
                        toLabel = pair.second,
                        evidence = "Selesai $count kali di hari dengan mood ${pair.first} (Kronik x Daily OS)",
                        confidence = 0.82f
                    )
                )
            }

        // 3. IdeKu: proyek/ide aktif sedang dikerjakan
        ideas.filter { it.type == "Idea" && it.status == "Proses" }
            .take(4)
            .forEach { idea ->
                result.add(
                    ExtractedTripleDto(
                        fromType = "SELF",
                        fromLabel = selfLabel,
                        relation = "SEDANG_MEMBANGUN",
                        toType = "TUJUAN",
                        toLabel = idea.title,
                        evidence = "Proyek aktif di IdeKu (${idea.category}, prioritas ${idea.priority})",
                        confidence = 0.9f
                    )
                )
            }

        // 4. CuciMoney+: pola alokasi pengeluaran teratas 30 hari terakhir (non-transfer)
        val cutoffDate = runCatching { LocalDate.parse(todayStr).minusDays(30).toString() }.getOrDefault("")
        val catNames = cuciCategories.associate { it.id to it.name }
        val spendByCategory = mutableMapOf<String, Long>()
        transactions.forEach { trx ->
            if (trx.direction == "out" && trx.transferPair == null && trx.date >= cutoffDate) {
                val catName = catNames[trx.categoryId]?.takeIf { it.isNotBlank() } ?: return@forEach
                spendByCategory[catName] = (spendByCategory[catName] ?: 0L) + trx.amount
            }
        }
        spendByCategory.entries
            .sortedByDescending { it.value }
            .take(2)
            .forEach { (catName, _) ->
                result.add(
                    ExtractedTripleDto(
                        fromType = "SELF",
                        fromLabel = selfLabel,
                        relation = "ALOKASI_UTAMA",
                        toType = "KONTEKS",
                        toLabel = "Pengeluaran $catName",
                        evidence = "Kategori pengeluaran paling dominan 30 hari terakhir di CuciMoney+",
                        confidence = 0.78f
                    )
                )
            }

        // 5. LinkKu Publik saja: minat topik belajar terbanyak (tautan privat isPrivate=true selalu dikecualikan)
        val pubCatMap = linkkuCategories.filter { !it.isPrivate }.associate { it.id to it.name }
        val publicLinks = links.filter { !it.isPrivate }
        val countByTopic = mutableMapOf<String, Int>()
        publicLinks.forEach { l ->
            val topic = pubCatMap[l.cat]?.takeIf { it.isNotBlank() } ?: l.cat.takeIf { it.isNotBlank() } ?: return@forEach
            countByTopic[topic] = (countByTopic[topic] ?: 0) + 1
        }
        countByTopic.entries
            .filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .take(3)
            .forEach { (topic, cnt) ->
                result.add(
                    ExtractedTripleDto(
                        fromType = "SELF",
                        fromLabel = selfLabel,
                        relation = "MENGUTAMAKAN",
                        toType = "MINAT",
                        toLabel = topic,
                        evidence = "$cnt bookmark publik tersimpan di kategori $topic (LinkKu)",
                        confidence = 0.75f
                    )
                )
            }

        return result
    }
}

class BehavioralSignalSync(
    private val db: AssistantDatabase,
    private val kgRepository: KnowledgeGraphRepository
) {
    suspend fun syncAll(profileName: String) = withContext(Dispatchers.IO) {
        val triples = BehavioralSignalExtractor.extractTriples(
            profileName = profileName,
            habits = db.dailyOsDao().getAllHabits(),
            kronikEntries = db.kronikDao().getAllEntries(),
            ideas = db.idekuDao().getAll(),
            transactions = db.cuciMoneyDao().getAllTransactions(),
            cuciCategories = db.cuciMoneyDao().getAllCategories(),
            links = db.linkkuDao().getAllLinks(),
            linkkuCategories = db.linkkuDao().getAllCategories()
        )
        // weightBoost = 0f agar sinkronisasi berulang tidak menggandakan bobot secara artifisial
        kgRepository.recordTriples(
            profileName = profileName,
            triples = triples,
            sourceType = "behavioral_sync",
            weightBoost = 0f
        )
    }
}
