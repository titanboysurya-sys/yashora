package com.ritvyom.yashoraReelgenerator.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ritvyom.yashoraReelgenerator.data.model.NewsItem

/**
 * Room Database entity for caching fetched news articles locally.
 * Enables zero-latency loading and complete offline reading and reel generation.
 */
@Entity(tableName = "cached_news_articles")
data class CachedNewsArticleEntity(
    @PrimaryKey
    val id: String,
    val feedSourceId: String,
    val title: String,
    val summary: String,
    val content: String,
    val link: String,
    val source: String,
    val category: String,
    val imageUrl: String?,
    val pubDate: String,
    val timestampMillis: Long,
    val language: String,
    val imagesJson: String,
    val videoUrlsJson: String,
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toNewsItem(): NewsItem {
        val imgList = if (imagesJson.isNotBlank()) {
            imagesJson.split(";;;").filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val vidList = if (videoUrlsJson.isNotBlank()) {
            videoUrlsJson.split(";;;").filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        return NewsItem(
            id = id,
            title = title,
            summary = summary,
            content = content,
            link = link,
            source = source,
            category = category,
            imageUrl = imageUrl,
            pubDate = pubDate,
            timestampMillis = timestampMillis,
            language = language,
            images = imgList,
            videoUrls = vidList
        )
    }

    companion object {
        fun fromNewsItem(item: NewsItem, feedSourceId: String): CachedNewsArticleEntity {
            return CachedNewsArticleEntity(
                id = item.id,
                feedSourceId = feedSourceId,
                title = item.title,
                summary = item.summary,
                content = item.content,
                link = item.link,
                source = item.source,
                category = item.category,
                imageUrl = item.imageUrl,
                pubDate = item.pubDate,
                timestampMillis = item.timestampMillis,
                language = item.language,
                imagesJson = item.images.joinToString(";;;"),
                videoUrlsJson = item.videoUrls.joinToString(";;;"),
                cachedAtMillis = System.currentTimeMillis()
            )
        }
    }
}
