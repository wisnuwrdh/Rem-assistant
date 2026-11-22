package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CuciMoneyDao {

    // ── Observe Flows ───────────────────────────────────────────────────────
    @Query("SELECT * FROM cuci_books ORDER BY id ASC")
    fun observeBooks(): Flow<List<CuciBookEntity>>

    @Query("SELECT * FROM cuci_accounts ORDER BY isSavings ASC, id ASC")
    fun observeAccounts(): Flow<List<CuciAccountEntity>>

    @Query("SELECT * FROM cuci_categories ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(): Flow<List<CuciCategoryEntity>>

    @Query("SELECT * FROM cuci_members ORDER BY sortOrder ASC, id ASC")
    fun observeMembers(): Flow<List<CuciMemberEntity>>

    @Query("SELECT * FROM cuci_transactions ORDER BY date DESC, createdAt DESC")
    fun observeTransactions(): Flow<List<CuciTransactionEntity>>

    @Query("SELECT * FROM cuci_master_budgets ORDER BY year DESC, month DESC")
    fun observeMasterBudgets(): Flow<List<CuciMasterBudgetEntity>>

    @Query("SELECT * FROM cuci_wishlists ORDER BY createdAt DESC")
    fun observeWishlists(): Flow<List<CuciWishlistEntity>>

    // ── Suspend Getters ─────────────────────────────────────────────────────
    @Query("SELECT * FROM cuci_books ORDER BY id ASC")
    suspend fun getAllBooks(): List<CuciBookEntity>

    @Query("SELECT * FROM cuci_books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): CuciBookEntity?

    @Query("SELECT * FROM cuci_accounts ORDER BY isSavings ASC, id ASC")
    suspend fun getAllAccounts(): List<CuciAccountEntity>

    @Query("SELECT * FROM cuci_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): CuciAccountEntity?

    @Query("SELECT * FROM cuci_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategories(): List<CuciCategoryEntity>

    @Query("SELECT * FROM cuci_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CuciCategoryEntity?

    @Query("SELECT * FROM cuci_members ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllMembers(): List<CuciMemberEntity>

    @Query("SELECT * FROM cuci_members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: String): CuciMemberEntity?

    @Query("SELECT * FROM cuci_transactions ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactions(): List<CuciTransactionEntity>

    @Query("SELECT * FROM cuci_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): CuciTransactionEntity?

    @Query("SELECT * FROM cuci_transactions WHERE transferPair = :pairId")
    suspend fun getTransactionsByPair(pairId: Long): List<CuciTransactionEntity>

    @Query("SELECT * FROM cuci_transactions WHERE bookId = :bookId")
    suspend fun getTransactionsByBook(bookId: Long): List<CuciTransactionEntity>

    @Query("SELECT * FROM cuci_master_budgets ORDER BY year DESC, month DESC")
    suspend fun getAllMasterBudgets(): List<CuciMasterBudgetEntity>

    @Query("SELECT * FROM cuci_wishlists ORDER BY createdAt DESC")
    suspend fun getAllWishlists(): List<CuciWishlistEntity>

    @Query("SELECT * FROM cuci_wishlists WHERE id = :id LIMIT 1")
    suspend fun getWishlistById(id: String): CuciWishlistEntity?

    // ── Upserts ─────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: CuciBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooks(books: List<CuciBookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: CuciAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<CuciAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CuciCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CuciCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: CuciMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<CuciMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: CuciTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<CuciTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMasterBudget(budget: CuciMasterBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMasterBudgets(budgets: List<CuciMasterBudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWishlist(wishlist: CuciWishlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWishlists(wishlists: List<CuciWishlistEntity>)

    // ── Balance Adjustment ──────────────────────────────────────────────────
    @Query("UPDATE cuci_accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun adjustAccountBalance(accountId: Long, delta: Long)

    // ── Deletes ─────────────────────────────────────────────────────────────
    @Query("DELETE FROM cuci_books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("DELETE FROM cuci_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("DELETE FROM cuci_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM cuci_members WHERE id = :id")
    suspend fun deleteMemberById(id: String)

    @Query("DELETE FROM cuci_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM cuci_transactions WHERE transferPair = :pairId")
    suspend fun deleteTransactionsByPair(pairId: Long)

    @Query("DELETE FROM cuci_transactions WHERE bookId = :bookId")
    suspend fun deleteTransactionsByBook(bookId: Long)

    @Query("DELETE FROM cuci_master_budgets WHERE id = :id")
    suspend fun deleteMasterBudgetById(id: Long)

    @Query("DELETE FROM cuci_master_budgets WHERE bookId = :bookId")
    suspend fun deleteMasterBudgetsByBook(bookId: Long)

    @Query("DELETE FROM cuci_wishlists WHERE id = :id")
    suspend fun deleteWishlistById(id: String)

    @Query("DELETE FROM cuci_books")
    suspend fun clearAllBooks()

    @Query("DELETE FROM cuci_accounts")
    suspend fun clearAllAccounts()

    @Query("DELETE FROM cuci_categories")
    suspend fun clearAllCategories()

    @Query("DELETE FROM cuci_members")
    suspend fun clearAllMembers()

    @Query("DELETE FROM cuci_transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM cuci_master_budgets")
    suspend fun clearAllMasterBudgets()

    @Query("DELETE FROM cuci_wishlists")
    suspend fun clearAllWishlists()

    @Transaction
    suspend fun replaceAllData(
        books: List<CuciBookEntity>,
        accounts: List<CuciAccountEntity>,
        categories: List<CuciCategoryEntity>,
        members: List<CuciMemberEntity>,
        transactions: List<CuciTransactionEntity>,
        budgets: List<CuciMasterBudgetEntity>,
        wishlists: List<CuciWishlistEntity>
    ) {
        clearAllTransactions()
        clearAllMasterBudgets()
        clearAllWishlists()
        clearAllMembers()
        clearAllCategories()
        clearAllAccounts()
        clearAllBooks()

        if (books.isNotEmpty()) upsertBooks(books)
        if (accounts.isNotEmpty()) upsertAccounts(accounts)
        if (categories.isNotEmpty()) upsertCategories(categories)
        if (members.isNotEmpty()) upsertMembers(members)
        if (transactions.isNotEmpty()) upsertTransactions(transactions)
        if (budgets.isNotEmpty()) upsertMasterBudgets(budgets)
        if (wishlists.isNotEmpty()) upsertWishlists(wishlists)
    }
}
