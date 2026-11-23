package com.ecosystemku.assistant.ui.apps.ideku

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.IdekuBackupWrapperDto
import com.ecosystemku.assistant.data.local.IdekuIdeaDto
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.network.IdekuAiResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

enum class IdekuViewMode { GROUP, FLAT, GRID }

data class IdekuUiState(
    val ideas: List<IdekuIdeaEntity> = emptyList(),
    val categories: List<String> = IdekuHelpers.DEFAULT_CATEGORIES,
    val wishlists: List<CuciWishlistEntity> = emptyList(),
    val viewMode: IdekuViewMode = IdekuViewMode.GROUP
)

data class IdekuActions(
    val onSaveIdea: suspend (
        editId: Long?,
        type: String,
        title: String,
        notes: String,
        category: String,
        priority: String,
        jalur: String,
        nextStep: String,
        price: Long,
        image: String
    ) -> Unit,
    val onDeleteIdea: suspend (Long) -> Unit,
    val onUpdateStatus: suspend (Long, String) -> Unit,
    val onSwapFocus: suspend (newId: Long, parkId: Long) -> Unit,
    val onUpdatePriority: suspend (Long, String) -> Unit,
    val onUpdateMeta: suspend (id: Long, jalur: String, nextStep: String) -> Unit,
    val onAddCategory: (String) -> Boolean,
    val onDeleteCategory: (String) -> Boolean,
    val onSetViewMode: (IdekuViewMode) -> Unit,
    val onPerkayaIdea: suspend (IdekuIdeaEntity) -> IdekuAiResult,
    val onBreakdownIdea: suspend (IdekuIdeaEntity) -> IdekuAiResult,
    val onExportJson: suspend () -> String,
    val onImportJson: suspend (String) -> Result<Int>,
    val onDeleteAll: suspend () -> Unit
)

data class IdekuAiDecision(
    val label: String,          // "Layak" | "Pertimbangkan" | "Tidak Layak"
    val recommendation: String, // "Beli" | "Tunda" | "Jangan"
    val reason: String,
    val color: Color
)

data class IdekuIconDef(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object IdekuHelpers {
    const val FOCUS_LIMIT = 3
    val STATUSES = listOf("Belum", "Proses", "Selesai", "Parkir")
    val PRIORITIES = listOf("Penting", "Nanti", "Kapan-kapan")
    val DEFAULT_CATEGORIES = listOf("Dev", "Bisnis", "Konten", "Belajar", "Kreatif", "Personal", "Belanja")

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val CAT_PALETTE = listOf(
        Color(0xFF6366F1),
        Color(0xFFEC4899),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFF06B6D4),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
        Color(0xFF14B8A6),
        Color(0xFFF97316),
        Color(0xFF84CC16),
        Color(0xFF3B82F6),
        Color(0xFFD946EF)
    )

    val WISHLIST_ICONS = listOf(
        IdekuIconDef("bag", "Tas/Belanja", Icons.Outlined.ShoppingBag),
        IdekuIconDef("phone", "HP", Icons.Outlined.PhoneAndroid),
        IdekuIconDef("laptop", "Laptop", Icons.Outlined.LaptopMac),
        IdekuIconDef("headphone", "Headphone", Icons.Outlined.Headphones),
        IdekuIconDef("watch", "Jam", Icons.Outlined.Watch),
        IdekuIconDef("camera", "Kamera", Icons.Outlined.CameraAlt),
        IdekuIconDef("shirt", "Pakaian", Icons.Outlined.Checkroom),
        IdekuIconDef("shoe", "Sepatu", Icons.Outlined.DirectionsRun),
        IdekuIconDef("book", "Buku", Icons.Outlined.MenuBook),
        IdekuIconDef("gamepad", "Gaming", Icons.Outlined.SportsEsports),
        IdekuIconDef("furniture", "Furnitur", Icons.Outlined.Home),
        IdekuIconDef("food", "Makanan", Icons.Outlined.Coffee),
        IdekuIconDef("bike", "Kendaraan", Icons.Outlined.DirectionsBike),
        IdekuIconDef("tool", "Alat", Icons.Outlined.Construction),
        IdekuIconDef("gift", "Hadiah", Icons.Outlined.CardGiftcard),
        IdekuIconDef("beauty", "Kecantikan", Icons.Outlined.FavoriteBorder),
        IdekuIconDef("sport", "Olahraga", Icons.Outlined.FitnessCenter),
        IdekuIconDef("car", "Mobil", Icons.Outlined.DirectionsCar),
        IdekuIconDef("music", "Musik", Icons.Outlined.MusicNote),
        IdekuIconDef("health", "Kesehatan", Icons.Outlined.AutoAwesome),
        IdekuIconDef("star", "Lainnya", Icons.Outlined.StarBorder),
        IdekuIconDef("other", "Umum", Icons.Outlined.AttachMoney),
        IdekuIconDef("tv", "TV/Monitor", Icons.Outlined.Tv),
        IdekuIconDef("glasses", "Kacamata", Icons.Outlined.Visibility),
        IdekuIconDef("plant", "Tanaman", Icons.Outlined.Eco),
        IdekuIconDef("palette", "Art Supply", Icons.Outlined.Brush),
        IdekuIconDef("pen", "Stationery", Icons.Outlined.Create)
    )

    fun wishlistIcon(key: String?): ImageVector {
        return WISHLIST_ICONS.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.ShoppingBag
    }

    fun categoryIcon(cat: String): ImageVector {
        return when (cat.trim().lowercase()) {
            "dev" -> Icons.Outlined.Code
            "bisnis" -> Icons.Outlined.BusinessCenter
            "konten" -> Icons.Outlined.Campaign
            "belajar" -> Icons.Outlined.School
            "kreatif" -> Icons.Outlined.Brush
            "personal" -> Icons.Outlined.Person
            "belanja" -> Icons.Outlined.ShoppingBag
            else -> Icons.Outlined.Lightbulb
        }
    }

    fun categoryColor(cat: String): Color {
        val name = cat.ifBlank { "Lainnya" }
        var hash = 0
        for (ch in name) {
            hash = ch.code + ((hash shl 5) - hash)
        }
        return CAT_PALETTE[abs(hash) % CAT_PALETTE.size]
    }

    fun statusDotColor(status: String): Color = when (status) {
        "Proses" -> Color(0xFFF59E0B)
        "Selesai" -> Color(0xFF10B981)
        "Parkir" -> Color(0xFF94A3B8)
        else -> Color(0xFF9CA3AF)
    }

    fun priorityColor(priority: String): Color = when (priority) {
        "Penting" -> Color(0xFFEF4444)
        "Nanti" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    private val idLocale = Locale("in", "ID")
    private val fmtDate = DateTimeFormatter.ofPattern("dd MMM yyyy", idLocale)
    private val nfRp = ThreadLocal.withInitial { NumberFormat.getNumberInstance(idLocale) }

    fun formatRp(amount: Long): String =
        "Rp ${nfRp.get()!!.format(abs(amount))}"

    fun formatDate(ts: Long): String {
        return runCatching {
            Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(fmtDate)
        }.getOrDefault("")
    }

    fun analyzeWishlist(item: IdekuIdeaEntity): IdekuAiDecision {
        var score = 50
        score += when (item.priority) {
            "Penting" -> 20
            "Nanti" -> 5
            "Kapan-kapan" -> -15
            else -> 0
        }
        val price = item.price
        score += when {
            price <= 0L -> 0
            price <= 100_000L -> 10
            price <= 500_000L -> 0
            price <= 2_000_000L -> -12
            else -> -25
        }
        val notesTrim = item.notes.trim()
        score += when {
            notesTrim.isEmpty() -> -15
            notesTrim.length < 20 -> -5
            notesTrim.length < 50 -> 5
            notesTrim.split(Regex("\\s+")).size >= 10 -> 15
            else -> 10
        }
        val investmentCats = setOf("Kreatif", "Dev", "Bisnis", "Konten", "Belajar")
        score += when {
            item.category in investmentCats -> if (price >= 500_000L) 8 else 5
            item.category == "Personal" -> 2
            item.category == "Belanja" -> 3
            else -> 0
        }
        score = score.coerceIn(0, 100)

        val hasNotes = notesTrim.isNotEmpty()
        return when {
            score >= 70 -> {
                val reason = listOfNotNull(
                    if (item.priority == "Penting") "Prioritas tinggi dan ${if (hasNotes) "ada rencana jelas" else "perlu segera ditindaklanjuti"}." else null,
                    if (price in 1L..100_000L) "Harga terjangkau, risiko minimal." else null,
                    if (hasNotes) "Ada catatan detail yang menunjukkan keputusan matang." else null,
                    if (item.category in investmentCats) "Investasi untuk kebutuhan jangka panjang." else null
                ).firstOrNull() ?: "Semua faktor mendukung untuk segera membeli."
                IdekuAiDecision("Layak", "Beli", reason, Color(0xFF10B981))
            }
            score >= 50 -> {
                val reason = listOfNotNull(
                    if (price > 500_000L && !hasNotes) "Harga cukup tinggi, tapi catatan masih kurang detail." else null,
                    if (item.priority == "Nanti" && price > 100_000L) "Prioritas sedang + harga cukup tinggi, percepat riset atau tunda." else null,
                    if (!hasNotes) "Tanpa catatan detail, lebih baik tunda hingga yakin 100%." else null
                ).firstOrNull() ?: "Ada beberapa faktor yang perlu dipertimbangkan lebih lanjut."
                IdekuAiDecision("Pertimbangkan", "Tunda", reason, Color(0xFFF59E0B))
            }
            else -> {
                val reason = listOfNotNull(
                    if (item.priority == "Kapan-kapan" && price > 500_000L) "Prioritas rendah tapi harga tinggi—bukan waktu yang tepat." else null,
                    if (price > 2_000_000L && item.priority != "Penting") "Harga sangat tinggi untuk prioritas tidak urgent." else null,
                    if (!hasNotes && price > 500_000L) "Tanpa rencana jelas + harga tinggi = impulsif." else null
                ).firstOrNull() ?: "Belum waktu yang tepat untuk membeli item ini."
                IdekuAiDecision("Tidak Layak", "Jangan", reason, Color(0xFFEF4444))
            }
        }
    }

    fun matchingWishlistPot(idea: IdekuIdeaEntity, pots: List<CuciWishlistEntity>): CuciWishlistEntity? {
        if (idea.type != "Wishlist") return null
        return pots.firstOrNull { w ->
            w.id == idea.id.toString() ||
                w.id == "wish_${idea.id}" ||
                w.title.equals(idea.title, ignoreCase = true)
        }?.takeIf { it.saved > 0L }
    }

    fun buildExportJson(ideas: List<IdekuIdeaEntity>): String {
        val dtos = ideas.map {
            IdekuIdeaDto(
                id = it.id,
                title = it.title,
                notes = it.notes,
                category = it.category,
                jalur = it.jalur,
                next = it.nextStep,
                priority = it.priority,
                status = it.status,
                type = it.type,
                price = it.price,
                image = it.image
            )
        }
        return json.encodeToString(dtos)
    }

    fun parseImportJson(raw: String): List<IdekuIdeaEntity> {
        val trimmed = raw.trim()
        val dtos = if (trimmed.startsWith("[")) {
            json.decodeFromString<List<IdekuIdeaDto>>(trimmed)
        } else {
            json.decodeFromString<IdekuBackupWrapperDto>(trimmed).ideas
        }
        return dtos.filter { it.id > 0L && it.title.isNotBlank() }.map {
            IdekuIdeaEntity(
                id = it.id,
                title = it.title.trim(),
                notes = it.notes,
                category = it.category.ifBlank { "Personal" },
                jalur = it.jalur,
                nextStep = it.next,
                priority = if (it.priority in PRIORITIES) it.priority else "Nanti",
                status = if (it.status in STATUSES) it.status else "Belum",
                type = if (it.type == "Wishlist") "Wishlist" else "Idea",
                price = it.price.coerceAtLeast(0L),
                image = it.image.ifBlank { "bag" },
                createdAt = it.id
            )
        }
    }
}
