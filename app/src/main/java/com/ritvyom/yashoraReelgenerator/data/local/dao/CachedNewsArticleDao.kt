package com.ritvyom.yashoraReelgenerator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ritvyom.yashoraReelgenerator.data.local.entities.CachedNewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedNewsArticleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<CachedNewsArticleEntity>)

    @Query("SELECT * FROM cached_news_articles WHERE feedSourceId = :sourceId ORDER BY timestampMillis DESC")
    fun getArticlesBySource(sourceId: String): Flow<List<CachedNewsArticleEntity>>

    @Query("SELECT * FROM cached_news_articles WHERE feedSourceId = :sourceId ORDER BY timestampMillis DESC")
    suspend fun getArticlesBySourceSync(sourceId: String): List<CachedNewsArticleEntity>

    @Query("SELECT * FROM cached_news_articles ORDER BY timestampMillis DESC")
    fun getAllArticles(): Flow<List<CachedNewsArticleEntity>>

    @Query("SELECT * FROM cached_news_articles ORDER BY timestampMillis DESC")
    suspend fun getAllArticlesSync(): List<CachedNewsArticleEntity>

    @Query("SELECT * FROM cached_news_articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR source LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    fun searchArticles(query: String): Flow<List<CachedNewsArticleEntity>>

    @Query("SELECT * FROM cached_news_articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR source LIKE '%' || :query || '%' ORDER BY timestampMillis DESC")
    suspend fun searchArticlesSync(query: String): List<CachedNewsArticleEntity>

    @Query("DELETE FROM cached_news_articles WHERE feedSourceId = :sourceId")
    suspend fun deleteArticlesForSource(sourceId: String)

    @Query("DELETE FROM cached_news_articles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_news_articles")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM cached_news_articles WHERE feedSourceId = :sourceId")
    suspend fun getCountForSource(sourceId: String): Int
}
