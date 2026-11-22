package com.ecosystemku.assistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkkuDao {
    @Query("SELECT * FROM linkku_links ORDER BY ts DESC")
    fun observeAllLinks(): Flow<List<LinkkuLinkEntity>>

    @Query("SELECT * FROM linkku_categories ORDER BY isPrivate ASC, sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<LinkkuCategoryEntity>>

    @Query("SELECT * FROM linkku_links ORDER BY ts DESC")
    suspend fun getAllLinks(): List<LinkkuLinkEntity>

    @Query("SELECT * FROM linkku_links WHERE isPrivate = 0 ORDER BY ts DESC LIMIT :limit")
    suspend fun getPublicLinks(limit: Int = 20): List<LinkkuLinkEntity>

    @Query("SELECT * FROM linkku_categories ORDER BY isPrivate ASC, sortOrder ASC, name ASC")
    suspend fun getAllCategories(): List<LinkkuCategoryEntity>

    @Query("SELECT * FROM linkku_links WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LinkkuLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLink(link: LinkkuLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinks(links: List<LinkkuLinkEntity>)

    @Query("DELETE FROM linkku_links WHERE id = :id")
    suspend fun deleteLinkById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: LinkkuCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<LinkkuCategoryEntity>)

    @Query("DELETE FROM linkku_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("UPDATE linkku_links SET cat = '' WHERE cat = :catId")
    suspend fun clearCategoryFromPublicLinks(catId: String)

    @Query("UPDATE linkku_links SET pcat = '' WHERE pcat = :catId")
    suspend fun clearCategoryFromPrivateLinks(catId: String)

    @Query("DELETE FROM linkku_links")
    suspend fun clearAllLinks()

    @Query("DELETE FROM linkku_categories")
    suspend fun clearAllCategories()
}
