package com.ecosystemku.assistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "cuci_books")
data class CuciBookEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val accountIdsJson: String = "[]"
)

@Entity(tableName = "cuci_accounts")
data class CuciAccountEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String = "cash",
    val icon: String? = null,
    val balance: Long = 0L,
    val notes: String = "",
    val useInBook: Boolean = true,
    val includeAsset: Boolean = true,
    val isSavings: Boolean = false
)

@Entity(tableName = "cuci_categories")
data class CuciCategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val type: String = "expense", // expense | income | both
    val emoji: String = "📌",
    val sortOrder: Int = 0
)

@Entity(tableName = "cuci_members")
data class CuciMemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "😄",
    val sortOrder: Int = 0
)

@Entity(tableName = "cuci_transactions")
data class CuciTransactionEntity(
    @PrimaryKey val id: Long,
    val bookId: Long,
    val title: String,
    val amount: Long,
    val direction: String, // "in" | "out"
    val categoryId: Long,
    val accountId: Long,
    val date: String, // "YYYY-MM-DD"
    val memberId: String = "m1",
    val transferPair: Long? = null,
    val savingsPot: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cuci_master_budgets")
data class CuciMasterBudgetEntity(
    @PrimaryKey val id: Long,
    val bookId: Long,
    val month: Int, // 0..11
    val year: Int,
    val limitAmount: Long,
    val startDate: String = "",
    val endDate: String = "",
    val note: String = ""
)

@Entity(tableName = "cuci_wishlists")
data class CuciWishlistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: Long = 0L,
    val priority: String = "Nanti", // Penting | Nanti | Kapan-kapan
    val image: String = "bag",
    val status: String = "Belum", // Belum | Selesai
    val saved: Long = 0L,
    val realizedAt: Long? = null,
    val realizedAmount: Long = 0L,
    val historyJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CuciSavingsHistoryDto(
    val id: Long = 0L,
    val type: String = "in", // "in" | "out"
    val kind: String? = null, // "realize" | "cancel"
    val amount: Long = 0L,
    val date: String = "",
    val accountId: Long = 0L,
    val accountName: String = "",
    val ts: Long = 0L,
    val trxIds: List<Long> = emptyList()
)

@Serializable
data class CuciBackupBookDto(
    val id: Long = 1L,
    val name: String = "",
    val accountIds: List<Long> = emptyList()
)

@Serializable
data class CuciBackupAccountDto(
    val id: Long = 1L,
    val name: String = "",
    val type: String = "cash",
    val icon: String? = null,
    val balance: Long = 0L,
    val notes: String = "",
    val useInBook: Boolean = true,
    val includeAsset: Boolean = true,
    val isSavings: Boolean = false
)

@Serializable
data class CuciBackupCategoryDto(
    val id: Long = 1L,
    val name: String = "",
    val type: String = "expense",
    val emoji: String = "📌"
)

@Serializable
data class CuciBackupMemberDto(
    val id: String = "m1",
    val name: String = "diri sendiri",
    val emoji: String = "😄"
)

@Serializable
data class CuciBackupTransactionDto(
    val id: Long = 0L,
    val bookId: Long = 1L,
    val title: String = "",
    val amount: Long = 0L,
    val direction: String = "out",
    @SerialName("category_id") val categoryId: Long = 12L,
    @SerialName("account_id") val accountId: Long = 1L,
    val date: String = "",
    @SerialName("member_id") val memberId: String = "m1",
    val transferPair: Long? = null,
    val savingsPot: String? = null,
    val createdAt: Long = 0L
)

@Serializable
data class CuciBackupMasterBudgetDto(
    val id: Long = 0L,
    val bookId: Long = 1L,
    val month: Int = 0,
    val year: Int = 2026,
    val limit: Long = 0L,
    val startDate: String = "",
    val endDate: String = "",
    val note: String = ""
)

@Serializable
data class CuciBackupPotSnapDto(
    val title: String = "",
    val price: Long = 0L,
    val image: String = "bag"
)

@Serializable
data class CuciBackupPotDto(
    val wishId: String = "",
    val saved: Long = 0L,
    val history: List<CuciSavingsHistoryDto> = emptyList(),
    val snap: CuciBackupPotSnapDto? = null,
    val realizedAt: Long? = null,
    val realizedAmount: Long = 0L
)

@Serializable
data class CuciBackupSavingsDto(
    val pots: Map<String, CuciBackupPotDto> = emptyMap()
)

@Serializable
data class CuciBackupDbDto(
    val books: List<CuciBackupBookDto> = emptyList(),
    val accounts: List<CuciBackupAccountDto> = emptyList(),
    val categories: List<CuciBackupCategoryDto> = emptyList(),
    val transactions: List<CuciBackupTransactionDto> = emptyList(),
    val members: List<CuciBackupMemberDto> = emptyList()
)

@Serializable
data class CuciBackupWrapperDto(
    @SerialName("_version") val version: Int = 1,
    @SerialName("_app") val app: String = "cucimoney",
    @SerialName("_exportedAt") val exportedAt: String = "",
    val db: CuciBackupDbDto = CuciBackupDbDto(),
    val masterBudgets: List<CuciBackupMasterBudgetDto> = emptyList(),
    val savings: CuciBackupSavingsDto = CuciBackupSavingsDto()
)
