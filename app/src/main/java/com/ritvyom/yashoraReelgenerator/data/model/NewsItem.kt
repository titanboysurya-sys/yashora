package com.ritvyom.yashoraReelgenerator.data.model

/**
 * Represents a news article fetched from an RSS / backend news feed.
 */
data class NewsItem(
    val id: String,
    val title: String,
    val summary: String,
    val content: String = "",
    val link: String,
    val source: String,
    val category: String,
    val imageUrl: String? = null,
    val pubDate: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val language: String = "English",
    val images: List<String> = emptyList(), // Exact photos extracted directly from the news article metadata & content
    val videoUrls: List<String> = emptyList() // Exact video clips extracted directly from the RSS article metadata (<enclosure>, <media:content>, etc.)
) {
    /**
     * Returns all extracted source-specific media URLs with videos and RSS metadata media prioritized.
     */
    fun allSourceMediaPrioritized(): List<String> {
        val result = mutableListOf<String>()
        // Prioritize source-specific video URLs
        result.addAll(videoUrls.filter { it.isNotBlank() })
        // Then primary image URL
        if (!imageUrl.isNullOrBlank() && !result.contains(imageUrl)) {
            result.add(imageUrl)
        }
        // Then remaining images
        images.filter { it.isNotBlank() && !result.contains(it) }.forEach {
            result.add(it)
        }
        return result
    }

    /**
     * Clean, human-readable date & time, e.g. "30 Sep 2026, 09:00 AM"
     */
    val readableDateTime: String
        get() = com.ritvyom.yashoraReelgenerator.data.utils.NewsDateUtils.formatDateTime(
            rawDate = pubDate,
            epochMillis = timestampMillis,
            isHindi = language.equals("Hindi", ignoreCase = true)
        )

    /**
     * Relative age since publication, e.g. "45m ago" or "45 मिनट पहले"
     */
    fun relativeTime(isHindi: Boolean = false): String =
        com.ritvyom.yashoraReelgenerator.data.utils.NewsDateUtils.formatRelativeTime(
            rawDate = pubDate,
            epochMillis = timestampMillis,
            isHindi = isHindi || language.equals("Hindi", ignoreCase = true)
        )

    /**
     * Full publication details object with verified publisher and timestamp information
     */
    fun getPublicationDetails(isHindi: Boolean = false) =
        com.ritvyom.yashoraReelgenerator.data.utils.NewsDateUtils.getPublicationDetails(
            source = source,
            category = category,
            rawDate = pubDate,
            epochMillis = timestampMillis,
            isHindi = isHindi || language.equals("Hindi", ignoreCase = true)
        )
}

/**
 * Represents an RSS feed source channel.
 */
data class NewsFeedSource(
    val id: String,
    val name: String,
    val category: String,
    val feedUrl: String,
    val iconEmoji: String,
    val language: String = "English", // "Hindi", "English", or "All"
    val isCustom: Boolean = false,
    val supplementaryUrls: List<String> = emptyList()
)

/**
 * Predefined popular and reliable RSS feed channels for Hindi & English.
 */
object PopularNewsFeeds {
    val ALL_FEEDS = listOf(
        // --- ALL SOURCES AGGREGATOR ---
        NewsFeedSource(
            id = "all_sources_combined",
            name = "All Sources (सभी स्रोत)",
            category = "All",
            feedUrl = "ALL_AGGREGATE",
            iconEmoji = "🌐",
            language = "All"
        ),

        // --- HINDI SOURCES (हिंदी समाचार स्रोत) ---
        NewsFeedSource(
            id = "aajtak_top",
            name = "Aaj Tak (आज तक)",
            category = "India",
            feedUrl = "https://www.aajtak.in/rssfeeds?id=home",
            iconEmoji = "⚡",
            language = "Hindi",
            supplementaryUrls = listOf(
                "https://www.aajtak.in/rssfeeds?id=national",
                "https://www.aajtak.in/rssfeeds?id=entertainment",
                "https://www.aajtak.in/rssfeeds?id=sports",
                "https://www.aajtak.in/rssfeeds?id=technology",
                "https://www.aajtak.in/rssfeeds?id=business",
                "https://www.aajtak.in/rssfeeds?id=world"
            )
        ),
        NewsFeedSource(
            id = "nbt_hindi",
            name = "Navbharat Times (NBT)",
            category = "India",
            feedUrl = "https://navbharattimes.indiatimes.com/sitemap/rssfeed.xml",
            iconEmoji = "📰",
            language = "Hindi",
            supplementaryUrls = listOf(
                "https://navbharattimes.indiatimes.com/rssfeedstopstories.cms",
                "https://navbharattimes.indiatimes.com/india/rssfeedsection/1563456.cms",
                "https://navbharattimes.indiatimes.com/movie-masti/rssfeedsection/10814785.cms",
                "https://navbharattimes.indiatimes.com/business/rssfeedsection/1898018.cms",
                "https://navbharattimes.indiatimes.com/sports/rssfeedsection/2279793.cms"
            )
        ),
        NewsFeedSource(
            id = "bbc_hindi",
            name = "BBC News हिंदी",
            category = "World",
            feedUrl = "https://feeds.bbci.co.uk/hindi/rss.xml",
            iconEmoji = "🌐",
            language = "Hindi",
            supplementaryUrls = listOf(
                "https://feeds.bbci.co.uk/hindi/india/rss.xml",
                "https://feeds.bbci.co.uk/hindi/international/rss.xml",
                "https://feeds.bbci.co.uk/hindi/science/rss.xml"
            )
        ),
        NewsFeedSource(
            id = "abp_hindi",
            name = "ABP Live (एबीपी न्यूज़)",
            category = "India",
            feedUrl = "https://news.abplive.com/home/feed",
            iconEmoji = "🔴",
            language = "Hindi",
            supplementaryUrls = listOf(
                "https://news.abplive.com/india-news/feed",
                "https://news.abplive.com/entertainment/feed",
                "https://news.abplive.com/sports/feed",
                "https://news.abplive.com/technology/feed",
                "https://news.abplive.com/business/feed"
            )
        ),
        NewsFeedSource(
            id = "oneindia_hindi",
            name = "OneIndia Hindi (वनइंडिया)",
            category = "India",
            feedUrl = "https://hindi.oneindia.com/rss/hindi-news-fb.xml",
            iconEmoji = "🇮🇳",
            language = "Hindi"
        ),
        NewsFeedSource(
            id = "amar_ujala_top",
            name = "Amar Ujala (अमर उजाला)",
            category = "India",
            feedUrl = "https://www.amarujala.com/rss/breaking-news.xml",
            iconEmoji = "📰",
            language = "Hindi",
            supplementaryUrls = listOf(
                "https://www.amarujala.com/rss/india-news.xml",
                "https://www.amarujala.com/rss/technology-news.xml",
                "https://www.amarujala.com/rss/business-news.xml",
                "https://www.amarujala.com/rss/entertainment-news.xml",
                "https://www.amarujala.com/rss/sports-news.xml"
            )
        ),
        NewsFeedSource(
            id = "amar_ujala_tech",
            name = "Amar Ujala टेक",
            category = "Tech & AI",
            feedUrl = "https://www.amarujala.com/rss/technology-news.xml",
            iconEmoji = "💻",
            language = "Hindi"
        ),
        NewsFeedSource(
            id = "amar_ujala_biz",
            name = "Amar Ujala व्यापार",
            category = "Business",
            feedUrl = "https://www.amarujala.com/rss/business-news.xml",
            iconEmoji = "📈",
            language = "Hindi"
        ),
        NewsFeedSource(
            id = "amar_ujala_ent",
            name = "Amar Ujala मनोरंजन",
            category = "Entertainment",
            feedUrl = "https://www.amarujala.com/rss/entertainment-news.xml",
            iconEmoji = "🎬",
            language = "Hindi"
        ),
        NewsFeedSource(
            id = "amar_ujala_sports",
            name = "Amar Ujala खेल",
            category = "Sports",
            feedUrl = "https://www.amarujala.com/rss/sports-news.xml",
            iconEmoji = "🏏",
            language = "Hindi"
        ),

        // --- ENGLISH & INTERNATIONAL SOURCES (अंतर्राष्ट्रीय समाचार स्रोत) ---
        NewsFeedSource(
            id = "bbc_world",
            name = "BBC World News",
            category = "World",
            feedUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
            iconEmoji = "🌐",
            language = "English",
            supplementaryUrls = listOf(
                "https://feeds.bbci.co.uk/news/technology/rss.xml",
                "https://feeds.bbci.co.uk/news/business/rss.xml",
                "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"
            )
        ),
        NewsFeedSource(
            id = "cnn_world",
            name = "CNN International",
            category = "World",
            feedUrl = "http://rss.cnn.com/rss/edition_world.rss",
            iconEmoji = "🌍",
            language = "English"
        ),
        NewsFeedSource(
            id = "toi_top",
            name = "Times of India",
            category = "India",
            feedUrl = "https://timesofindia.indiatimes.com/rssfeedstopstories.cms",
            iconEmoji = "🇮🇳",
            language = "English",
            supplementaryUrls = listOf(
                "https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms",
                "https://timesofindia.indiatimes.com/rssfeeds/4719148.cms",
                "https://timesofindia.indiatimes.com/rssfeeds/66949542.cms",
                "https://timesofindia.indiatimes.com/rssfeeds/1898055.cms"
            )
        ),
        NewsFeedSource(
            id = "the_hindu",
            name = "The Hindu",
            category = "India",
            feedUrl = "https://www.thehindu.com/news/national/feeder/default.rss",
            iconEmoji = "📰",
            language = "English",
            supplementaryUrls = listOf(
                "https://www.thehindu.com/news/international/feeder/default.rss",
                "https://www.thehindu.com/business/feeder/default.rss",
                "https://www.thehindu.com/sport/feeder/default.rss",
                "https://www.thehindu.com/sci-tech/technology/feeder/default.rss"
            )
        ),
        NewsFeedSource(
            id = "techcrunch",
            name = "TechCrunch",
            category = "Tech & AI",
            feedUrl = "https://techcrunch.com/feed/",
            iconEmoji = "💻",
            language = "English",
            supplementaryUrls = listOf(
                "https://techcrunch.com/category/artificial-intelligence/feed/",
                "https://techcrunch.com/category/startups/feed/"
            )
        ),
        NewsFeedSource(
            id = "the_verge",
            name = "The Verge",
            category = "Tech & AI",
            feedUrl = "https://www.theverge.com/rss/index.xml",
            iconEmoji = "⚡",
            language = "English"
        ),
        NewsFeedSource(
            id = "wired_tech",
            name = "Wired Magazine",
            category = "Tech & AI",
            feedUrl = "https://www.wired.com/feed/rss",
            iconEmoji = "🔮",
            language = "English"
        ),
        NewsFeedSource(
            id = "mint_biz",
            name = "Livemint Business",
            category = "Business",
            feedUrl = "https://www.livemint.com/rss/news",
            iconEmoji = "📈",
            language = "English"
        ),
        NewsFeedSource(
            id = "espn_sports",
            name = "ESPN Sports",
            category = "Sports",
            feedUrl = "https://www.espn.com/espn/rss/news",
            iconEmoji = "🏏",
            language = "English"
        )
    )

    val CATEGORIES = listOf(
        "All",
        "India",
        "World",
        "Tech & AI",
        "Business",
        "Entertainment",
        "Sports"
    )

    val LANGUAGES = listOf(
        "All",
        "Hindi",
        "English"
    )
}
