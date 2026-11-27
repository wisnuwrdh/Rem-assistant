package com.ecosystemku.assistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ChatSessionEntity::class,
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
        DailyOsTodoEntity::class,
        RemReminderEntity::class,
        KgNodeEntity::class,
        KgEdgeEntity::class,
        HesychItemEntity::class,
        HesychHistoryEntity::class,
        HesychMetaEntity::class
    ],
    version = 17,
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
    abstract fun remReminderDao(): RemReminderDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao
    abstract fun hesychDao(): HesychDao

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

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `choicesJson` TEXT")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `sessionId` INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    INSERT INTO `chat_sessions` (`id`, `title`, `createdAt`, `updatedAt`)
                    SELECT 1, 'Obrolan Sebelumnya', COALESCE(MIN(`createdAt`), 0), COALESCE(MAX(`createdAt`), 0)
                    FROM `chat_messages`
                    WHERE EXISTS (SELECT 1 FROM `chat_messages`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `rollingSummary` TEXT")
                db.execSQL("ALTER TABLE `chat_sessions` ADD COLUMN `summarizedCount` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rem_reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `triggerAtMs` INTEGER NOT NULL,
                        `fired` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `kg_nodes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nodeKey` TEXT NOT NULL,
                        `nodeType` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `salience` REAL NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `firstSeenAt` INTEGER NOT NULL,
                        `lastSeenAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_kg_nodes_nodeKey` ON `kg_nodes` (`nodeKey`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `kg_edges` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fromNodeId` INTEGER NOT NULL,
                        `toNodeId` INTEGER NOT NULL,
                        `relation` TEXT NOT NULL,
                        `evidence` TEXT NOT NULL,
                        `weight` REAL NOT NULL,
                        `confidence` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_kg_edges_fromNodeId` ON `kg_edges` (`fromNodeId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_kg_edges_toNodeId` ON `kg_edges` (`toNodeId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_kg_edges_fromNodeId_toNodeId_relation` ON `kg_edges` (`fromNodeId`, `toNodeId`, `relation`)"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `kg_nodes` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE `kg_nodes` ADD COLUMN `confidence` REAL NOT NULL DEFAULT 0.8")
                db.execSQL("ALTER TABLE `kg_nodes` ADD COLUMN `lastConfirmedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `kg_nodes` ADD COLUMN `sourceRef` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `kg_nodes` ADD COLUMN `validationState` TEXT NOT NULL DEFAULT 'unverified'")
                db.execSQL("UPDATE `kg_nodes` SET `lastConfirmedAt` = `lastSeenAt` WHERE `lastConfirmedAt` = 0")
                db.execSQL("UPDATE `kg_nodes` SET `validationState` = 'confirmed' WHERE `sourceType` = 'manual'")

                db.execSQL("ALTER TABLE `kg_edges` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE `kg_edges` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `kg_edges` ADD COLUMN `lastConfirmedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `kg_edges` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT 'chat'")
                db.execSQL("ALTER TABLE `kg_edges` ADD COLUMN `sourceRef` TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE `kg_edges` SET `createdAt` = `updatedAt` WHERE `createdAt` = 0")
                db.execSQL("UPDATE `kg_edges` SET `lastConfirmedAt` = `updatedAt` WHERE `lastConfirmedAt` = 0")
                db.execSQL("UPDATE `kg_edges` SET `status` = 'historical' WHERE `weight` < 0.4 OR `evidence` LIKE '[Historis%'")

                db.execSQL("ALTER TABLE `user_memories` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE `user_memories` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `user_memories` ADD COLUMN `deprecatedAt` INTEGER")
                db.execSQL("ALTER TABLE `user_memories` ADD COLUMN `supersededBy` TEXT")
                db.execSQL("UPDATE `user_memories` SET `createdAt` = `updatedAt` WHERE `createdAt` = 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hesych_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `favorite` INTEGER NOT NULL,
                        `totpSecret` TEXT NOT NULL,
                        `customFieldsJson` TEXT NOT NULL,
                        `category` TEXT,
                        `tags` TEXT,
                        `breachStatus` TEXT,
                        `breachCheckedAt` TEXT,
                        `metaVersion` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hesych_history` (
                        `hid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `encPassword` TEXT NOT NULL,
                        `changedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_hesych_history_itemId` ON `hesych_history` (`itemId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hesych_meta` (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        `value` TEXT NOT NULL
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
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
