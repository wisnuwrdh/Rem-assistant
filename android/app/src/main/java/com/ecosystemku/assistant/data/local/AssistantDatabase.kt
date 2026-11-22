package com.ecosystemku.assistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        UserMemoryEntity::class,
        ConversationSummaryEntity::class,
        KronikEntryEntity::class,
        LinkkuLinkEntity::class,
        LinkkuCategoryEntity::class,
        CuciBookEntity::class,
        CuciAccountEntity::class,
        CuciCategoryEntity::class,
        CuciMemberEntity::class,
        CuciTransactionEntity::class,
        CuciMasterBudgetEntity::class,
        CuciWishlistEntity::class,
        IdekuIdeaEntity::class,
        DailyOsHabitEntity::class,
        DailyOsTodoEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AssistantDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun kronikDao(): KronikDao
    abstract fun linkkuDao(): LinkkuDao
    abstract fun cuciMoneyDao(): CuciMoneyDao
    abstract fun idekuDao(): IdekuDao
    abstract fun dailyOsDao(): DailyOsDao

    companion object {
        @Volatile
        private var instance: AssistantDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_memories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `factKey` TEXT NOT NULL,
                        `factValue` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `sourceMessageId` INTEGER,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_memories_factKey` ON `user_memories` (`factKey`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `conversation_summaries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `summaryText` TEXT NOT NULL,
                        `turnCount` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Transisi aman tanpa tabel sub-app
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Transisi aman tanpa tabel sub-app
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `cuci_wallet`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_transactions`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_books`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_accounts`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_categories`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_members`")
                db.execSQL("DROP TABLE IF EXISTS `cuci_budgets`")
                db.execSQL("DROP TABLE IF EXISTS `app_cross_links`")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `kronik_entries` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `date` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `moodStatus` TEXT,
                        `moodSvgKey` TEXT,
                        `habitsLogJson` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `generatedAt` TEXT,
                        `createdAt` TEXT NOT NULL,
                        `updatedAt` TEXT NOT NULL,
                        `completedAt` TEXT,
                        `archivedAt` TEXT,
                        `isArchived` INTEGER NOT NULL,
                        `pinned` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `linkku_links` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `url` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `cat` TEXT NOT NULL,
                        `pcat` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `isPrivate` INTEGER NOT NULL,
                        `thumb` TEXT NOT NULL,
                        `ts` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `linkku_categories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `isPrivate` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_books` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `accountIdsJson` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_accounts` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `icon` TEXT,
                        `balance` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `useInBook` INTEGER NOT NULL,
                        `includeAsset` INTEGER NOT NULL,
                        `isSavings` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_categories` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_members` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_transactions` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `bookId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `direction` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `memberId` TEXT NOT NULL,
                        `transferPair` INTEGER,
                        `savingsPot` TEXT,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_master_budgets` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `bookId` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `limitAmount` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cuci_wishlists` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `priority` TEXT NOT NULL,
                        `image` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `saved` INTEGER NOT NULL,
                        `realizedAt` INTEGER,
                        `realizedAmount` INTEGER NOT NULL,
                        `historyJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ideku_ideas` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `jalur` TEXT NOT NULL,
                        `nextStep` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `price` INTEGER NOT NULL,
                        `image` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dailyos_habits` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `activeDaysJson` TEXT NOT NULL,
                        `subTasksJson` TEXT NOT NULL,
                        `historyJson` TEXT NOT NULL,
                        `collapsed` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dailyos_todos` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `deadline` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `completedAt` TEXT,
                        `subtasksJson` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AssistantDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AssistantDatabase::class.java,
                    "rem_assistant.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
