package com.ritvyom.yashoraReelgenerator.presentation.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ritvyom.yashoraReelgenerator.data.model.NewsFeedSource
import com.ritvyom.yashoraReelgenerator.data.model.NewsItem
import com.ritvyom.yashoraReelgenerator.data.model.PopularNewsFeeds
import com.ritvyom.yashoraReelgenerator.data.repository.NewsFeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsFeedRepository(application)

    private val _availableSources = MutableStateFlow<List<NewsFeedSource>>(repository.getAllSources())
    val availableSources: StateFlow<List<NewsFeedSource>> = _availableSources.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow<String>("All") // "All", "Hindi", "English"
    val selectedLanguageFilter: StateFlow<String> = _selectedLanguageFilter.asStateFlow()

    private val _filteredSources = MutableStateFlow<List<NewsFeedSource>>(repository.getAllSources())
    val filteredSources: StateFlow<List<NewsFeedSource>> = _filteredSources.asStateFlow()

    private val _currentSource = MutableStateFlow<NewsFeedSource>(
        _availableSources.value.firstOrNull() ?: PopularNewsFeeds.ALL_FEEDS[0]
    )
    val currentSource: StateFlow<NewsFeedSource> = _currentSource.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    companion object {
        const val PAGE_SIZE = 20
    }

    private var currentDisplayLimit = PAGE_SIZE
    private val fetchedSupplementaryUrls = mutableSetOf<String>()

    private val _allFetchedNews = MutableStateFlow<List<NewsItem>>(emptyList())

    private val _filteredNews = MutableStateFlow<List<NewsItem>>(emptyList())
    // Expose displayedNews as the paginated 20-item batch for the UI list
    private val _displayedNews = MutableStateFlow<List<NewsItem>>(emptyList())
    val displayedNews: StateFlow<List<NewsItem>> = _displayedNews.asStateFlow()
    val filteredNews: StateFlow<List<NewsItem>> = _displayedNews.asStateFlow()

    private val _totalAvailableCount = MutableStateFlow<Int>(0)
    val totalAvailableCount: StateFlow<Int> = _totalAvailableCount.asStateFlow()

    private val _hasMoreNews = MutableStateFlow<Boolean>(false)
    val hasMoreNews: StateFlow<Boolean> = _hasMoreNews.asStateFlow()

    private val _isLoadingMore = MutableStateFlow<Boolean>(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGeneratingScript = MutableStateFlow<Boolean>(false)
    val isGeneratingScript: StateFlow<Boolean> = _isGeneratingScript.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isFromCache = MutableStateFlow<Boolean>(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    private val _cacheTimestampText = MutableStateFlow<String?>("Live")
    val cacheTimestampText: StateFlow<String?> = _cacheTimestampText.asStateFlow()

    init {
        updateFilteredSources()
        // Cache-first initial load: if already cached on disk or memory, load instantly without network hit
        val initialSource = _currentSource.value
        val cached = repository.getCachedFeed(initialSource.id)
        if (!cached.isNullOrEmpty()) {
            _allFetchedNews.value = cached
            _isFromCache.value = true
            _cacheTimestampText.value = repository.getFormattedCacheTime(initialSource.id)
            currentDisplayLimit = PAGE_SIZE
            fetchedSupplementaryUrls.clear()
            applyFilterAndSearch()
        } else {
            loadFeed(initialSource, forceRefresh = false)
        }
    }

    fun selectLanguageFilter(lang: String) {
        _selectedLanguageFilter.value = lang
        currentDisplayLimit = PAGE_SIZE
        updateFilteredSources()

        val matchingSources = _filteredSources.value
        // If current source does not match language, switch to first matching source
        if (lang != "All" && !_currentSource.value.language.equals(lang, ignoreCase = true)) {
            val nextSource = matchingSources.firstOrNull() ?: _currentSource.value
            selectSource(nextSource)
        } else {
            applyFilterAndSearch()
        }
    }

    private fun updateFilteredSources() {
        val lang = _selectedLanguageFilter.value
        val all = _availableSources.value
        _filteredSources.value = if (lang == "All") {
            all
        } else {
            all.filter { it.language.equals(lang, ignoreCase = true) }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        currentDisplayLimit = PAGE_SIZE
        // Find matching source within currently filtered sources or all sources
        val matchingSource = _filteredSources.value.firstOrNull { it.category.equals(category, ignoreCase = true) }
            ?: _availableSources.value.firstOrNull { it.category.equals(category, ignoreCase = true) }

        if (matchingSource != null && category != "All" && matchingSource.id != _currentSource.value.id) {
            selectSource(matchingSource)
        } else {
            applyFilterAndSearch()
        }
    }

    /**
     * Cache-first source selector:
     * When switching between sources (e.g. Aaj Tak -> Amar Ujala -> Aaj Tak),
     * previously loaded data is served INSTANTLY from cache with 0ms latency
     * and no loading spinner, exactly as requested by user.
     */
    fun selectSource(source: NewsFeedSource) {
        val isSameSource = _currentSource.value.id == source.id
        _currentSource.value = source
        currentDisplayLimit = PAGE_SIZE
        fetchedSupplementaryUrls.clear()
        // When selecting a source (especially custom user feeds), reset category filter to "All"
        // so that all stories from that source channel are immediately displayed!
        _selectedCategory.value = "All"

        if (isSameSource && _allFetchedNews.value.isNotEmpty()) {
            applyFilterAndSearch()
            return
        }

        // 1. Check multi-level cache first
        val cached = repository.getCachedFeed(source.id)
        if (!cached.isNullOrEmpty()) {
            _allFetchedNews.value = cached
            _isFromCache.value = true
            _cacheTimestampText.value = repository.getFormattedCacheTime(source.id)
            applyFilterAndSearch()
            _statusMessage.value = "Loaded from cache • ${source.name}"
            return
        }

        // 2. Not yet cached: fetch over network and cache it
        loadFeed(source, forceRefresh = false)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        currentDisplayLimit = PAGE_SIZE
        applyFilterAndSearch()
    }

    /**
     * Explicit user-triggered refresh: fetches fresh news from live RSS,
     * updates memory and disk cache, and updates timestamps.
     */
    fun refreshCurrentFeed() {
        currentDisplayLimit = PAGE_SIZE
        fetchedSupplementaryUrls.clear()
        loadFeed(_currentSource.value, forceRefresh = true)
    }

    private fun loadFeed(source: NewsFeedSource, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Only show full loading spinner if we don't already have items showing
            if (_allFetchedNews.value.isEmpty() || forceRefresh) {
                _isLoading.value = true
            }
            _statusMessage.value = null
            try {
                val items = repository.fetchFeed(source, forceRefresh = forceRefresh)
                _allFetchedNews.value = items
                _isFromCache.value = !forceRefresh && repository.isServedFromCache(source.id)
                _cacheTimestampText.value = repository.getFormattedCacheTime(source.id)
                applyFilterAndSearch()

                val count = items.size
                if (count == 0 && source.isCustom) {
                    _statusMessage.value = "No stories found in ${source.name}"
                } else if (forceRefresh) {
                    _statusMessage.value = "✓ Refreshed! $count fresh stories"
                } else {
                    _statusMessage.value = "$count stories ready (${source.name})"
                }
            } catch (e: Exception) {
                // If live fetch fails, fallback to cache
                val cached = repository.getCachedFeed(source.id)
                if (!cached.isNullOrEmpty()) {
                    _allFetchedNews.value = cached
                    _isFromCache.value = true
                    _cacheTimestampText.value = repository.getFormattedCacheTime(source.id)
                    applyFilterAndSearch()
                    _statusMessage.value = "Offline mode • Showing saved news"
                } else if (source.isCustom) {
                    _allFetchedNews.value = emptyList()
                    _isFromCache.value = false
                    _cacheTimestampText.value = null
                    applyFilterAndSearch()
                    _statusMessage.value = "Could not reach feed. Check URL or internet connection."
                } else {
                    val fallbacks = repository.getCuratedFallbackNews(source.category, source.language)
                    _allFetchedNews.value = fallbacks
                    _isFromCache.value = false
                    _cacheTimestampText.value = "Curated"
                    applyFilterAndSearch()
                    _statusMessage.value = "Loaded trending stories"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun applyFilterAndSearch() {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value
        val lang = _selectedLanguageFilter.value

        var list = _allFetchedNews.value

        // If user specifically picked a custom source, do not drop its items due to language filter
        if (lang != "All" && !_currentSource.value.isCustom) {
            list = list.filter { it.language.equals(lang, ignoreCase = true) }
        }

        // For custom sources, show all their feed articles without filtering by standard categories
        if (category != "All" && category.isNotBlank() && !_currentSource.value.isCustom) {
            list = list.filter {
                it.category.equals(category, ignoreCase = true) ||
                (it.category.isBlank() && _currentSource.value.category.equals(category, ignoreCase = true))
            }
        }

        if (query.isNotBlank()) {
            val terms = query.split(" ").filter { it.isNotBlank() }
            list = list.filter { item ->
                val titleLower = item.title.lowercase()
                val summaryLower = item.summary.lowercase()
                val contentLower = item.content.lowercase()
                val sourceLower = item.source.lowercase()
                val categoryLower = item.category.lowercase()
                // All search terms match any of the fields (title, summary, content, source, category)
                terms.all { term ->
                    titleLower.contains(term) ||
                    summaryLower.contains(term) ||
                    contentLower.contains(term) ||
                    sourceLower.contains(term) ||
                    categoryLower.contains(term)
                }
            }
        }

        _filteredNews.value = list
        _totalAvailableCount.value = list.size
        _displayedNews.value = list.take(currentDisplayLimit)
        _hasMoreNews.value = (list.size > currentDisplayLimit) || hasMoreSupplementaryFeedsAvailable()
    }

    private fun hasMoreSupplementaryFeedsAvailable(): Boolean {
        val src = _currentSource.value
        if (src.id == "all_sources_combined") {
            return true
        }
        return src.supplementaryUrls.any { it !in fetchedSupplementaryUrls }
    }

    /**
     * Loads the next 20 news items.
     * When user scrolls down past 20 items, this appends the next 20 items.
     * If available in memory/disk cache, loads instantly without network call.
     * If in-memory items are exhausted, fetches the next batch from supplementary RSS section feeds.
     */
    fun loadMoreNews() {
        if (_isLoadingMore.value || _isLoading.value) return
        val fullList = _filteredNews.value

        if (currentDisplayLimit < fullList.size) {
            // Memory/cache pool has more items -> display next 20 immediately
            currentDisplayLimit += PAGE_SIZE
            _displayedNews.value = fullList.take(currentDisplayLimit)
            _hasMoreNews.value = (fullList.size > currentDisplayLimit) || hasMoreSupplementaryFeedsAvailable()
            _statusMessage.value = "Showing ${minOf(currentDisplayLimit, fullList.size)} of ${fullList.size} stories"
            return
        }

        // Check if secondary/supplementary network section feeds exist
        if (!hasMoreSupplementaryFeedsAvailable()) {
            _hasMoreNews.value = false
            return
        }

        val source = _currentSource.value
        viewModelScope.launch {
            _isLoadingMore.value = true
            _statusMessage.value = "Fetching next 20 stories for ${source.name}..."
            try {
                val (newItems, fetchedUrl) = repository.fetchMoreForSource(source, fetchedSupplementaryUrls)
                if (fetchedUrl != null) {
                    fetchedSupplementaryUrls.add(fetchedUrl)
                }
                if (newItems.isNotEmpty()) {
                    val combined = (_allFetchedNews.value + newItems)
                        .distinctBy { it.title.trim().lowercase() }
                        .sortedByDescending { it.timestampMillis }
                    _allFetchedNews.value = combined
                    repository.saveCachedFeed(source.id, combined)
                    currentDisplayLimit += PAGE_SIZE
                    applyFilterAndSearch()
                    _statusMessage.value = "+${newItems.size} more stories fetched (${_displayedNews.value.size} ready)"
                } else {
                    _hasMoreNews.value = hasMoreSupplementaryFeedsAvailable()
                }
            } catch (e: Exception) {
                Log.w("NewsViewModel", "loadMoreNews network error: ${e.message}")
                _hasMoreNews.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Resolves all exact article photographs for video generation and slideshows.
     */
    fun getExactArticleImages(news: NewsItem, onComplete: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val photos = repository.getExactArticleImages(news)
                onComplete(photos)
            } catch (e: Exception) {
                val fallback = news.imageUrl?.let { listOf(it) } ?: listOf(repository.getCategoryFallbackImage(news.category))
                onComplete(fallback)
            }
        }
    }

    fun addUserDefinedFeed(
        customName: String,
        feedUrl: String,
        category: String,
        language: String = "English",
        onComplete: (Boolean, String) -> Unit
    ) {
        val rawUrl = feedUrl.trim()
        if (rawUrl.isBlank()) {
            onComplete(false, "Please enter an RSS feed URL")
            return
        }

        val cleanUrl = if (rawUrl.startsWith("http://", ignoreCase = true) || rawUrl.startsWith("https://", ignoreCase = true)) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val cleanCat = category.trim().ifEmpty { "General" }

        viewModelScope.launch {
            _isLoading.value = true
            // Pre-fetch feed XML or webpage HTML to validate and discover channel title / RSS feed URL
            val xmlBody = repository.fetchXmlWithRedirectAndCookies(cleanUrl)
            val discoveredUrl = if (!xmlBody.isNullOrBlank()) repository.discoverFeedUrl(xmlBody, cleanUrl) else null
            val finalFeedUrl = discoveredUrl ?: cleanUrl

            // If we discovered a new feed URL, try to extract channel title from it
            val targetBody = if (discoveredUrl != null && discoveredUrl != cleanUrl) {
                repository.fetchXmlWithRedirectAndCookies(discoveredUrl) ?: xmlBody
            } else {
                xmlBody
            }

            val detectedTitle = targetBody?.let { repository.extractChannelTitle(it) }
                ?: xmlBody?.let { repository.extractChannelTitle(it) }

            val cleanName = if (customName.isNotBlank()) {
                customName.trim()
            } else if (!detectedTitle.isNullOrBlank()) {
                detectedTitle
            } else {
                "User RSS Feed"
            }

            val newSource = NewsFeedSource(
                id = "user_rss_${System.currentTimeMillis()}",
                name = cleanName,
                category = cleanCat,
                feedUrl = finalFeedUrl,
                iconEmoji = "🌐",
                language = language,
                isCustom = true
            )

            repository.saveUserDefinedSource(newSource)
            _availableSources.value = repository.getAllSources()
            _selectedLanguageFilter.value = "All"
            _selectedCategory.value = "All"
            updateFilteredSources()
            selectSource(newSource)
            onComplete(true, "Added source '$cleanName'!")
        }
    }

    fun deleteUserDefinedFeed(sourceId: String) {
        repository.deleteUserDefinedSource(sourceId)
        _availableSources.value = repository.getAllSources()
        updateFilteredSources()
        if (_currentSource.value.id == sourceId) {
            val fallbackSource = _filteredSources.value.firstOrNull() ?: _availableSources.value.firstOrNull() ?: PopularNewsFeeds.ALL_FEEDS[0]
            selectSource(fallbackSource)
        }
        _statusMessage.value = "Source removed"
    }

    fun getExactArticleMedia(news: NewsItem, onComplete: (images: List<String>, videos: List<String>) -> Unit) {
        viewModelScope.launch {
            val (images, videos) = repository.getExactArticleMedia(news)
            onComplete(images, videos)
        }
    }

    fun getExactArticleVideos(news: NewsItem, onComplete: (List<String>) -> Unit) {
        viewModelScope.launch {
            val videos = repository.getExactArticleVideos(news)
            onComplete(videos)
        }
    }

    fun generateReelScriptWithGemini(
        news: NewsItem,
        tone: String = "Breaking News",
        language: String = "Hindi & English",
        duration: String = "Short",
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingScript.value = true
            try {
                val script = repository.generateScriptUsingGemini(news, tone, language, duration)
                onResult(script)
            } catch (e: Exception) {
                val fallbackScript = repository.generateViralReelScript(news, tone, language)
                onResult(fallbackScript)
            } finally {
                _isGeneratingScript.value = false
            }
        }
    }

    fun generateReelScriptLocal(news: NewsItem, tone: String = "Breaking News", language: String = "Hindi & English"): String {
        return repository.generateViralReelScript(news, tone, language)
    }

    fun generateSocialCaption(news: NewsItem): String {
        return repository.generateSocialMediaCaption(news)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
