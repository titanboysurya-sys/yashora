package com.ritvyom.yashoraReelgenerator.presentation.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.data.model.NewsFeedSource
import com.ritvyom.yashoraReelgenerator.data.model.NewsItem
import com.ritvyom.yashoraReelgenerator.data.model.PopularNewsFeeds
import com.ritvyom.yashoraReelgenerator.presentation.components.SocialUploadSheet
import com.ritvyom.yashoraReelgenerator.presentation.components.news.NewsDraftVariationsSheet
import com.ritvyom.yashoraReelgenerator.domain.models.NewsReelDraftVariation
import com.ritvyom.yashoraReelgenerator.presentation.components.rememberShimmerBrush
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.MainViewModel
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.NewsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsToReelScreen(
    mainViewModel: MainViewModel,
    appLanguageState: String,
    onBack: () -> Unit,
    onNavigateToScriptConfig: () -> Unit,
    onNavigateToGeneration: () -> Unit,
    onNavigateToEditor: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val newsViewModel: NewsViewModel = viewModel(factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))

    val availableSources by newsViewModel.filteredSources.collectAsState()
    val selectedLanguageFilter by newsViewModel.selectedLanguageFilter.collectAsState()
    val currentSource by newsViewModel.currentSource.collectAsState()
    val selectedCategory by newsViewModel.selectedCategory.collectAsState()
    val searchQuery by newsViewModel.searchQuery.collectAsState()
    val filteredNews by newsViewModel.filteredNews.collectAsState()
    val displayedNews by newsViewModel.displayedNews.collectAsState()
    val totalAvailableCount by newsViewModel.totalAvailableCount.collectAsState()
    val hasMoreNews by newsViewModel.hasMoreNews.collectAsState()
    val isLoadingMore by newsViewModel.isLoadingMore.collectAsState()
    val isLoading by newsViewModel.isLoading.collectAsState()
    val isGeneratingScript by newsViewModel.isGeneratingScript.collectAsState()
    val statusMessage by newsViewModel.statusMessage.collectAsState()
    val isFromCache by newsViewModel.isFromCache.collectAsState()
    val cacheTimestampText by newsViewModel.cacheTimestampText.collectAsState()

    val listState = rememberLazyListState()

    // Pagination callback: triggered when the last item in the LazyColumn is reached
    val onLastItemReached = remember(newsViewModel, hasMoreNews, isLoadingMore, isLoading) {
        {
            if (hasMoreNews && !isLoadingMore && !isLoading) {
                newsViewModel.loadMoreNews()
            }
        }
    }

    // Trigger pagination callback when the last item in the list is reached
    val isLastItemReached by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 1
        }
    }

    LaunchedEffect(isLastItemReached) {
        if (isLastItemReached) {
            onLastItemReached()
        }
    }

    // Scroll-aware auto-hiding for RSS settings and filter controls
    var isSettingsHeaderVisible by remember { mutableStateOf(true) }

    // Scroll to top when switching source channel or category or language
    LaunchedEffect(currentSource.id, selectedCategory, selectedLanguageFilter) {
        if (listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
        isSettingsHeaderVisible = true
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f) {
                    // Scrolling UP into the news feed (reading downward) -> hide settings header
                    if (isSettingsHeaderVisible) {
                        isSettingsHeaderVisible = false
                    }
                } else if (delta > 12f) {
                    // Scrolling DOWN (pulling down towards top) -> reveal settings header
                    if (!isSettingsHeaderVisible) {
                        isSettingsHeaderVisible = true
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Automatically reveal RSS settings header when returning near the very top
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == 0 && offset < 15) {
                    isSettingsHeaderVisible = true
                }
            }
    }

    // Dialog & Detail Screen States
    var selectedNewsForRead by remember { mutableStateOf<NewsItem?>(null) }
    var selectedNewsForReel by remember { mutableStateOf<NewsItem?>(null) }
    var selectedNewsForShare by remember { mutableStateOf<NewsItem?>(null) }
    var showFeedsManagerDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Full Detailed View Screen when tapping a news card
    selectedNewsForRead?.let { detailedNews ->
        NewsDetailScreen(
            article = detailedNews,
            appLanguage = appLanguageState,
            onBack = { selectedNewsForRead = null },
            onGenerateReel = { news ->
                selectedNewsForRead = null
                selectedNewsForReel = news
            },
            onShare = { news ->
                selectedNewsForShare = news
            }
        )
        return
    }

    // Intercept back
    BackHandler {
        onBack()
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it.localize(appLanguageState), Toast.LENGTH_SHORT).show()
            newsViewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF1F1235),
                        contentColor = Color(0xFF00FF87),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Global News Wire".localize(appLanguageState),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            // Status Badge: Live or Cached
                            Surface(
                                color = if (isFromCache) Color(0xFF00F0FF).copy(alpha = 0.15f) else Color(0xFF00FF87).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isFromCache) Color(0xFF00F0FF).copy(alpha = 0.5f) else Color(0xFF00FF87).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isFromCache) Color(0xFF00F0FF) else Color(0xFF00FF87))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFromCache) "⚡ CACHED" else "● LIVE RSS",
                                        color = if (isFromCache) Color(0xFF00F0FF) else Color(0xFF00FF87),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isFromCache) "Instant cache • 0ms load • Tap ↻ to refresh".localize(appLanguageState)
                            else "Select news article & convert into viral AI Reel".localize(appLanguageState),
                            fontSize = 11.sp,
                            color = Color(0xFFB388FF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Manage / Add user-defined RSS feeds
                    IconButton(onClick = { showFeedsManagerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = "Manage Feeds",
                            tint = Color(0xFF00FF87)
                        )
                    }
                    // Refresh Feed (User-controlled fresh fetch)
                    IconButton(
                        onClick = { newsViewModel.refreshCurrentFeed() },
                        modifier = Modifier.testTag("refresh_news_feed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF00F0FF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0418)
                )
            )
        },
        containerColor = Color(0xFF0B0418)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Collapsible RSS Settings, Channels & Filters Header (hides when scrolling up, reveals on pull down)
            AnimatedVisibility(
                visible = isSettingsHeaderVisible,
                enter = expandVertically(animationSpec = androidx.compose.animation.core.tween(260)) + fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
                exit = shrinkVertically(animationSpec = androidx.compose.animation.core.tween(260)) + fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search Bar
                    OutlinedTextField(
                value = searchQuery,
                onValueChange = { newsViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                placeholder = {
                    Text(
                        text = "Search news topics (e.g. AI, Space, Tech, Markets)...".localize(appLanguageState),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFB388FF),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { newsViewModel.updateSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF8A2BE2),
                    unfocusedBorderColor = Color(0xFF2B1A4A),
                    focusedContainerColor = Color(0xFF160B29),
                    unfocusedContainerColor = Color(0xFF160B29)
                )
            )

            // Language Filter Selector Bar (Hindi, English, All)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("All", "🌐 All (सभी)", Color(0xFF8A2BE2)),
                    Triple("Hindi", "🇮🇳 हिंदी (Hindi)", Color(0xFFFF9933)),
                    Triple("English", "🇬🇧 English", Color(0xFF00F0FF))
                ).forEach { (langCode, label, activeColor) ->
                    val isSelected = selectedLanguageFilter == langCode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) activeColor.copy(alpha = 0.22f) else Color(0xFF150A26),
                        border = BorderStroke(1.dp, if (isSelected) activeColor else Color(0xFF2C194D)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { newsViewModel.selectLanguageFilter(langCode) }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF9E8DB8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PopularNewsFeeds.CATEGORIES) { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF8A2BE2) else Color(0xFF1E1038),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFF007F) else Color(0xFF3B2366)
                        ),
                        modifier = Modifier.clickable { newsViewModel.selectCategory(category) }
                    ) {
                        Text(
                            text = category.localize(appLanguageState),
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // RSS Source Channel Selector Row (including user-defined feeds)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Add Custom Feed" shortcut button
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1A1230),
                        border = BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { showFeedsManagerDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Feed",
                                tint = Color(0xFF00FF87),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Feed".localize(appLanguageState),
                                color = Color(0xFF00FF87),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                items(availableSources) { source ->
                    val isSelected = currentSource.id == source.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF2D1854) else Color(0xFF140B24),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00FF87) else Color(0xFF2E1C4B)
                        ),
                        modifier = Modifier.clickable { newsViewModel.selectSource(source) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = source.iconEmoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = source.name,
                                color = if (isSelected) Color.White else Color(0xFFB5A7CE),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (source.isCustom) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    color = Color(0xFF00FF87).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "User",
                                        color = Color(0xFF00FF87),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active Feed Indicator Bar (International Newsroom Ticker)
            Surface(
                color = Color(0xFF140B26),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isFromCache) Color(0xFF00F0FF).copy(alpha = 0.35f) else Color(0xFF00FF87).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = currentSource.iconEmoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSource.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF281845),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (totalAvailableCount > displayedNews.size) "${displayedNews.size} of $totalAvailableCount stories" else "${displayedNews.size} stories",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            // Cache or Live Subtext
                            Text(
                                text = if (isFromCache) "⚡ ${cacheTimestampText ?: "Cached offline"} • Instant" else "● Live RSS Feed",
                                color = if (isFromCache) Color(0xFF00F0FF) else Color(0xFF00FF87),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Quick Refresh Button (User control to fetch fresh feed)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF22113D),
                        border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { newsViewModel.refreshCurrentFeed() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Refresh".localize(appLanguageState),
                                color = Color(0xFF00F0FF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Compact Slim Status Bar shown when settings are collapsed
    AnimatedVisibility(
        visible = !isSettingsHeaderVisible,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
    ) {
        Surface(
            color = Color(0xFF140B26).copy(alpha = 0.96f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 3.dp)
                .clickable { isSettingsHeaderVisible = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(text = currentSource.iconEmoji, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${currentSource.name} • $selectedCategory",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Show Settings",
                        tint = Color(0xFF00FF87),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "⚙️ Settings (Pull / Tap)".localize(appLanguageState),
                        color = Color(0xFF00FF87),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Feed Content List or Loading State
            if (isLoading) {
                val shimmerBrush = rememberShimmerBrush(
                    shimmerColors = listOf(
                        Color(0xFF160B29),
                        Color(0xFF2C164D),
                        Color(0xFF160B29)
                    )
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "shimmer_loading_header") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color(0xFF160B29),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00FF87),
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aggregating latest news feeds...".localize(appLanguageState),
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    items(3, key = { "initial_shimmer_$it" }) {
                        NewsCardShimmerItem(brush = shimmerBrush)
                    }
                }
            } else if (filteredNews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No articles found in this feed".localize(appLanguageState),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try switching category, search keyword or refresh".localize(appLanguageState),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { newsViewModel.refreshCurrentFeed() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refresh Feed", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayedNews, key = { it.id }) { newsItem ->
                        NewsArticleCard(
                            news = newsItem,
                            appLanguage = appLanguageState,
                            onReadClick = { selectedNewsForRead = newsItem },
                            onCreateReelClick = { selectedNewsForReel = newsItem },
                            onShareClick = { selectedNewsForShare = newsItem }
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more_progress_indicator") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                color = Color(0xFF1B0E33),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00F0FF),
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Loading next 20 stories...".localize(appLanguageState),
                                        color = Color(0xFF00F0FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        items(2, key = { "shimmer_more_$it" }) {
                            NewsCardShimmerItem(
                                brush = rememberShimmerBrush(
                                    shimmerColors = listOf(
                                        Color(0xFF160B29),
                                        Color(0xFF2C164D),
                                        Color(0xFF160B29)
                                    )
                                )
                            )
                        }
                    } else if (hasMoreNews) {
                        item(key = "load_more_manual_button") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { newsViewModel.loadMoreNews() },
                                color = Color(0xFF160B29),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF87),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scroll down or Tap to load next 20 stories".localize(appLanguageState),
                                        color = Color(0xFF00FF87),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    } else if (displayedNews.isNotEmpty()) {
                        item(key = "all_caught_up_footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓ You're all caught up! (${displayedNews.size} stories)".localize(appLanguageState),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detailed article reading and 'Generate Reel' action is now handled by the dedicated NewsDetailScreen!

    // -------------------------------------------------------------------------
    // GEMINI AI NEWS REEL SCRIPT GENERATOR DIALOG
    // -------------------------------------------------------------------------
    selectedNewsForReel?.let { news ->
        var selectedTone by remember { mutableStateOf("Breaking News") }
        var selectedLanguage by remember {
            mutableStateOf(if (news.language.contains("Hindi", ignoreCase = true) || news.language.isBlank()) "Hindi" else "English")
        }
        var selectedDuration by remember { mutableStateOf("Short") } // Short (15-30s), Medium (60s)
        var generatedScriptText by remember {
            mutableStateOf(newsViewModel.generateReelScriptLocal(news, selectedTone, selectedLanguage))
        }

        // Exact media (photos & video clips) extracted directly from the selected news article metadata & content
        var articleImages by remember {
            mutableStateOf(
                if (news.images.isNotEmpty()) news.images
                else if (!news.imageUrl.isNullOrBlank()) listOf(news.imageUrl)
                else emptyList()
            )
        }
        var articleVideos by remember {
            mutableStateOf(news.videoUrls)
        }
        var isFetchingArticleMedia by remember { mutableStateOf(false) }

        LaunchedEffect(news) {
            if (articleImages.isEmpty() && articleVideos.isEmpty()) {
                isFetchingArticleMedia = true
                newsViewModel.getExactArticleMedia(news) { resolvedImages, resolvedVideos ->
                    articleImages = resolvedImages
                    articleVideos = resolvedVideos
                    isFetchingArticleMedia = false
                }
            }
        }

        var showManualGeminiDialog by remember { mutableStateOf(false) }

        if (!showManualGeminiDialog) {
            NewsDraftVariationsSheet(
                news = news,
                articleImages = articleImages,
                articleVideos = articleVideos,
                appLanguage = appLanguageState,
                onSelectAndOpenInEditor = { variation ->
                    val chosenLang = if (news.language.contains("Hindi", ignoreCase = true) || selectedLanguage.contains("Hindi", ignoreCase = true) || news.title.any { it in '\u0900'..'\u097F' }) "Hindi" else "English"
                    mainViewModel.selectedLanguage.value = chosenLang
                    mainViewModel.createProjectFromDraftVariation(
                        title = news.title,
                        scriptText = variation.fullScript,
                        scenes = variation.scenes
                    ) {
                        selectedNewsForReel = null
                        onNavigateToEditor()
                    }
                },
                onSelectAndSendToPipeline = { variation ->
                    mainViewModel.setNewsArticleMedia(articleImages, news.title, articleVideos)
                    mainViewModel.scriptText.value = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer.cleanScriptText(variation.fullScript)
                    mainViewModel.topicContext.value = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer.cleanNewsTopic(news.title)
                    mainViewModel.selectedLanguage.value = if (news.language == "Hindi" || selectedLanguage.contains("Hindi") || news.title.any { it in '\u0900'..'\u097F' }) "Hindi" else "English"
                    selectedNewsForReel = null
                    onNavigateToScriptConfig()
                },
                onOpenCustomScriptDialog = {
                    showManualGeminiDialog = true
                },
                onDismiss = { selectedNewsForReel = null }
            )
        } else {
            Dialog(onDismissRequest = { selectedNewsForReel = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140B28)),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFF8A2BE2)))),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title and Gemini Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF007F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Yashora News Studio".localize(appLanguageState),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "✨ Professional News & Reel Architect".localize(appLanguageState),
                                    fontSize = 10.sp,
                                    color = Color(0xFF00FF87),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00FF87).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF00FF87)),
                                modifier = Modifier
                                    .clickable { showManualGeminiDialog = false }
                                    .padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "⚡ 3 Draft Styles".localize(appLanguageState),
                                    color = Color(0xFF00FF87),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = { selectedNewsForReel = null }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Article Title Summary Box
                    Surface(
                        color = Color(0xFF1D1037),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF38235E))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = news.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (news.language == "Hindi") Color(0xFFFF9933).copy(alpha = 0.2f) else Color(0xFF00F0FF).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, if (news.language == "Hindi") Color(0xFFFF9933) else Color(0xFF00F0FF))
                                ) {
                                    Text(
                                        text = if (news.language == "Hindi") "🇮🇳 हिंदी" else "🇬🇧 English",
                                        color = if (news.language == "Hindi") Color(0xFFFFCC80) else Color(0xFF80F7FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Source: ${news.source} • ${news.category}",
                                    color = Color(0xFFB5A7CE),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real News Article Media (Videos & Photos pitched directly to Reel scenes from RSS metadata)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF180D2E),
                        border = BorderStroke(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val totalMedia = articleVideos.size + articleImages.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF87),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Source Media ($totalMedia: ${articleVideos.size} clips, ${articleImages.size} photos)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isFetchingArticleMedia) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(10.dp),
                                            color = Color(0xFF00FF87),
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Extracting RSS...", color = Color.Gray, fontSize = 9.sp)
                                    }
                                } else {
                                    Text(
                                        text = "Pitched to Reel ✓",
                                        color = Color(0xFF00FF87),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Extracted directly from RSS article metadata & source media. Prioritized strictly over external stock libraries.",
                                color = Color(0xFFB39DDB),
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )

                            if (articleVideos.isNotEmpty() || articleImages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Video clips first (highest priority)
                                    items(articleVideos) { vidUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 96.dp, height = 54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2E174D))
                                                .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircle,
                                                    contentDescription = "Video Clip",
                                                    tint = Color(0xFF00F0FF),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "HD Video Clip",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color(0xFF00F0FF), RoundedCornerShape(topStart = 4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("RSS", color = Color.Black, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Image photos
                                    items(articleImages) { imgUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(width = 86.dp, height = 54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = imgUrl,
                                                contentDescription = "News Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color(0xFF00FF87), RoundedCornerShape(topStart = 4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("✓", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tone / Style Selector
                    Text(
                        text = "Script Style / Tone:".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Breaking News", "Tech Viral", "Explainer Story", "Dramatic Mystery")) { tone ->
                            val isSel = selectedTone == tone
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF8A2BE2) else Color(0xFF20133A),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFFF007F) else Color(0xFF38235E)),
                                modifier = Modifier.clickable { selectedTone = tone }
                            ) {
                                Text(
                                    text = tone,
                                    color = if (isSel) Color.White else Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Duration Selector
                    Text(
                        text = "Reel Target Length:".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Short" to "15-30s Hook", "Medium" to "45-60s Deep").forEach { (durationKey, label) ->
                            val isSel = selectedDuration == durationKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF00FF87).copy(alpha = 0.2f) else Color(0xFF20133A),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF00FF87) else Color(0xFF38235E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDuration = durationKey }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color(0xFF00FF87) else Color.LightGray,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Selector
                    Text(
                        text = "Language:".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hindi", "English").forEach { lang ->
                            val isSel = selectedLanguage.equals(lang, ignoreCase = true)
                            val displayLabel = if (lang == "Hindi") "🇮🇳 शुद्ध हिंदी (Hindi)" else "🌐 English"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF8A2BE2) else Color(0xFF22143D),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFFFF007F) else Color(0xFF3D236E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLanguage = lang }
                            ) {
                                Text(
                                    text = displayLabel,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent "Generate with Gemini AI" Action Button
                    Button(
                        onClick = {
                            newsViewModel.generateReelScriptWithGemini(
                                news = news,
                                tone = selectedTone,
                                language = selectedLanguage,
                                duration = selectedDuration
                            ) { generated ->
                                generatedScriptText = generated
                            }
                        },
                        enabled = !isGeneratingScript,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8A2BE2)
                        )
                    ) {
                        if (isGeneratingScript) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Writing script from news data...".localize(appLanguageState),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF00FF87),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✨ AI Script Refine & Format".localize(appLanguageState),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Script Preview / Editor Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Script Narration (Editable):".localize(appLanguageState),
                            fontSize = 11.sp,
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Medium
                        )
                        val wordCount = generatedScriptText.trim().split("\\s+".toRegex()).size
                        Text(
                            text = "$wordCount words (~${(wordCount / 2.5).toInt()}s spoken)",
                            fontSize = 10.sp,
                            color = Color(0xFF00FF87)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = generatedScriptText,
                        onValueChange = { generatedScriptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFFF007F),
                            unfocusedBorderColor = Color(0xFF3E236E),
                            focusedContainerColor = Color(0xFF1B0E33),
                            unfocusedContainerColor = Color(0xFF1B0E33)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, lineHeight = 16.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions: Share Idea or Proceed to Video & Voiceover Pipeline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val item = news
                                selectedNewsForShare = item
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF8A2BE2))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share".localize(appLanguageState),
                                color = Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                mainViewModel.setNewsArticleMedia(articleImages, news.title, articleVideos)
                                mainViewModel.scriptText.value = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer.cleanScriptText(generatedScriptText)
                                mainViewModel.topicContext.value = com.ritvyom.yashoraReelgenerator.presentation.viewmodels.UnifiedPromptRefinementLayer.cleanNewsTopic(news.title)
                                mainViewModel.selectedLanguage.value = if (news.language == "Hindi" || selectedLanguage.contains("Hindi")) "Hindi" else "English"
                                selectedNewsForReel = null
                                onNavigateToScriptConfig()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF007F)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reel Studio 🎬".localize(appLanguageState),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // -------------------------------------------------------------------------
    // SOCIAL SHARE / UPLOAD SHEET
    // -------------------------------------------------------------------------
    selectedNewsForShare?.let { news ->
        val caption = remember(news) { newsViewModel.generateSocialCaption(news) }
        SocialUploadSheet(
            title = news.title,
            caption = caption,
            sourceUrl = news.link,
            appLanguage = appLanguageState,
            onDismiss = { selectedNewsForShare = null }
        )
    }

    // -------------------------------------------------------------------------
    // USER-DEFINED RSS FEEDS MANAGER DIALOG
    // -------------------------------------------------------------------------
    if (showFeedsManagerDialog) {
        var rssUrlInput by remember { mutableStateOf("") }
        var rssNameInput by remember { mutableStateOf("") }
        var rssCategoryInput by remember { mutableStateOf("General") }
        var rssLanguageInput by remember { mutableStateOf("All") }
        var isAddingFeed by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showFeedsManagerDialog = false }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140B28)),
                border = BorderStroke(1.dp, Color(0xFF00FF87)),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RssFeed,
                                contentDescription = null,
                                tint = Color(0xFF00FF87),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RSS Feed Channels".localize(appLanguageState),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showFeedsManagerDialog = false }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable content (form + list of sources) to ensure responsive fit on all screens
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            // Form to add custom RSS feed
                            Surface(
                                color = Color(0xFF1C1036),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF38235E))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "+ Add User-Defined RSS Source".localize(appLanguageState),
                                        color = Color(0xFF00FF87),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = rssNameInput,
                                        onValueChange = { rssNameInput = it },
                                        placeholder = { Text("Source Name (Optional: Auto-detects e.g. KOREA.NET)", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray,
                                            focusedBorderColor = Color(0xFF00FF87),
                                            unfocusedBorderColor = Color(0xFF38235E)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = rssUrlInput,
                                        onValueChange = { rssUrlInput = it },
                                        placeholder = { Text("RSS URL (e.g. http://world.kbs.co.kr/...)", fontSize = 11.sp, color = Color.Gray) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray,
                                            focusedBorderColor = Color(0xFF00FF87),
                                            unfocusedBorderColor = Color(0xFF38235E)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text("Feed Language:", color = Color(0xFFD0BCFF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("All" to "🌐 All", "English" to "🇬🇧 English", "Hindi" to "🇮🇳 Hindi").forEach { (langKey, label) ->
                                            val isSel = rssLanguageInput == langKey
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) Color(0xFF00FF87).copy(alpha = 0.2f) else Color(0xFF140B28),
                                                border = BorderStroke(1.dp, if (isSel) Color(0xFF00FF87) else Color(0xFF38235E)),
                                                modifier = Modifier.clickable { rssLanguageInput = langKey }
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color(0xFF00FF87) else Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            if (rssUrlInput.isNotBlank()) {
                                                isAddingFeed = true
                                                newsViewModel.addUserDefinedFeed(
                                                    customName = rssNameInput,
                                                    feedUrl = rssUrlInput,
                                                    category = rssCategoryInput,
                                                    language = rssLanguageInput
                                                ) { success, msg ->
                                                    isAddingFeed = false
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    if (success) {
                                                        rssNameInput = ""
                                                        rssUrlInput = ""
                                                        showFeedsManagerDialog = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isAddingFeed && rssUrlInput.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                                    ) {
                                        if (isAddingFeed) {
                                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(14.dp))
                                        } else {
                                            Text("Verify & Save RSS Source", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Active & Configured Feeds:".localize(appLanguageState),
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(availableSources) { source ->
                            val isCurrent = source.id == currentSource.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) Color(0xFF2C1650) else Color(0xFF180D2F),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) Color(0xFF00FF87) else Color(0xFF2C194D)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        newsViewModel.selectSource(source)
                                        showFeedsManagerDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = source.iconEmoji, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = source.name,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (source.isCustom) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFF00FF87).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "User Source",
                                                            color = Color(0xFF00FF87),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = source.category,
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCurrent) {
                                            Text(
                                                text = "Active",
                                                color = Color(0xFF00FF87),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (source.isCustom) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { newsViewModel.deleteUserDefinedFeed(source.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// NEWS ARTICLE CARD COMPONENT
// -----------------------------------------------------------------------------
@Composable
private fun NewsArticleCard(
    news: NewsItem,
    appLanguage: String,
    onReadClick: () -> Unit,
    onCreateReelClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF160C2C)),
        border = BorderStroke(1.dp, Color(0xFF2C194D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("news_card_${news.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val isHindiApp = appLanguage.contains("hi", ignoreCase = true) || news.language.equals("Hindi", ignoreCase = true)
            val pubDetails = news.getPublicationDetails(isHindi = isHindiApp)
            val clipboard = LocalClipboardManager.current
            val ctx = LocalContext.current
            var isCardCopied by remember { mutableStateOf(false) }
            var isContentExpanded by remember { mutableStateOf(false) }

            val fullContentText = remember(news) { news.content.ifBlank { news.summary } }
            val wordCount = remember(fullContentText) {
                fullContentText.split(Regex("\\s+")).count { it.isNotBlank() }
            }
            val readMinutes = remember(wordCount) { (wordCount / 160).coerceAtLeast(1) }

            LaunchedEffect(isCardCopied) {
                if (isCardCopied) {
                    kotlinx.coroutines.delay(2000)
                    isCardCopied = false
                }
            }

            // Top Header: Source Branding, Language Tag, Category & Quick Copy Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFF8A2BE2).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = news.source,
                            color = Color(0xFFE0D0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (news.language == "Hindi") Color(0xFFFF9933).copy(alpha = 0.2f) else Color(0xFF00F0FF).copy(alpha = 0.16f),
                        border = BorderStroke(0.8.dp, if (news.language == "Hindi") Color(0xFFFF9933) else Color(0xFF00F0FF))
                    ) {
                        Text(
                            text = if (news.language == "Hindi") "🇮🇳 हिंदी" else "🇬🇧 Eng",
                            color = if (news.language == "Hindi") Color(0xFFFFCC80) else Color(0xFF80F7FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${news.category}",
                        color = Color(0xFFA597BF),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        val textToCopy = buildString {
                            appendLine(news.title)
                            appendLine()
                            appendLine(fullContentText)
                            appendLine()
                            appendLine("📰 Source / स्रोत: ${news.source}")
                            appendLine("🕒 Published / प्रकाशित: ${pubDetails.fullDateTime} (${pubDetails.relativeAge})")
                            if (news.link.isNotBlank()) appendLine("🔗 Official Link: ${news.link}")
                        }
                        clipboard.setText(AnnotatedString(textToCopy))
                        isCardCopied = true
                        Toast.makeText(ctx, "✓ Text copied / टेक्स्ट कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCardCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy news text",
                        tint = if (isCardCopied) Color(0xFF00FF87) else Color(0xFF00F0FF),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Dedicated Publication & Verification Info Strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF100722),
                border = BorderStroke(0.8.dp, Color(0xFF2E1A52)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Relative time with freshness indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (pubDetails.isVeryFresh) Icons.Default.Bolt else Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(pubDetails.freshnessColor),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pubDetails.relativeAge,
                            color = Color(pubDetails.freshnessColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF21133D)
                        ) {
                            Text(
                                text = "⏱️ $readMinutes min read",
                                color = Color(0xFFD0BCFF),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Right: Exact Formatted RSS Publication Date & Time in IST
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pubDetails.formattedTime.isNotBlank()) {
                                "${pubDetails.formattedDate} • ${pubDetails.formattedTime} IST"
                            } else {
                                "${pubDetails.formattedDate} IST"
                            },
                            color = Color(0xFFE2D9F3),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Featured Visual Media Banner (Clickable to open Reader)
            if (!news.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF381F63), RoundedCornerShape(14.dp))
                        .clickable(onClick = onReadClick)
                ) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = news.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Dark gradient scrim at the bottom for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000), Color(0xDD000000)),
                                    startY = 100f
                                )
                            )
                    )

                    // Media asset badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xCC000000),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.6.dp, Color(0xFF00FF87).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (news.images.size > 1) "📷 ${news.images.size} Photos" else "📷 Photo",
                                color = Color(0xFF00FF87),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (news.videoUrls.isNotEmpty()) {
                            Surface(
                                color = Color(0xCC000000),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.6.dp, Color(0xFF00F0FF).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "🎬 Video Ready",
                                    color = Color(0xFF00F0FF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Headline / News Title
            Text(
                text = news.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 21.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReadClick)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Full Story Content Reader (Directly in card without clicking, 0 API cost!)
            SelectionContainer {
                Text(
                    text = fullContentText,
                    color = Color(0xFFE2D6F8),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = if (isContentExpanded || fullContentText.length <= 320) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // In-place expand/collapse toggle for long stories
            if (fullContentText.length > 320) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF22123F),
                    border = BorderStroke(1.dp, Color(0xFF48297A)),
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .clickable { isContentExpanded = !isContentExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isContentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isContentExpanded) {
                                "🔼 Show Less / कम दिखाएं".localize(appLanguage)
                            } else {
                                "📖 Read Full Story / पूरा समाचार पढ़ें".localize(appLanguage)
                            },
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================================
            // RESPONSIVE ACTION BUTTONS (TIER 1: FULL-WIDTH CTA, TIER 2: UTILITIES)
            // Guaranteed to fit on ANY device screen size with zero overflow
            // =========================================================================

            // Tier 1: Prominent Primary Creation Action (Yashora News Reel)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFF8A2BE2)))
                    )
                    .clickable(onClick = onCreateReelClick)
                    .testTag("create_reel_button_${news.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yashora News Reel (AI Video)".localize(appLanguage),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 2: Secondary Responsive Utility Actions (Detailed, Share, Source)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Detailed Reader Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF22113D),
                    border = BorderStroke(1.dp, Color(0xFF3E236E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable(onClick = onReadClick)
                        .testTag("read_news_button_${news.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reader".localize(appLanguage),
                            color = Color(0xFFD0BCFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Share & Upload Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF22113D),
                    border = BorderStroke(1.dp, Color(0xFF3E236E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable(onClick = onShareClick)
                        .testTag("share_news_button_${news.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF00FF87),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Share".localize(appLanguage),
                            color = Color(0xFF00FF87),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Official Source Link (Opens Publisher in Browser)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF22113D),
                    border = BorderStroke(1.dp, Color(0xFF3E236E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable {
                            if (news.link.isNotBlank()) {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(news.link))
                                    ctx.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val textToCopy = "${news.title}\n\n$fullContentText"
                                clipboard.setText(AnnotatedString(textToCopy))
                                Toast.makeText(ctx, "✓ Copied / कॉपी हो गया", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .testTag("source_link_button_${news.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (news.link.isNotBlank()) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (news.link.isNotBlank()) "Source".localize(appLanguage) else "Copy".localize(appLanguage),
                            color = Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Shimmer Skeleton placeholder for news articles during initial loading
 * and progressive infinite scroll pagination.
 */
@Composable
fun NewsCardShimmerItem(
    brush: androidx.compose.ui.graphics.Brush,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140B28)),
        border = BorderStroke(1.dp, Color(0xFF2C164D)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Source badge and relative time row placeholders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Featured Thumbnail Skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Headline lines skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Summary line skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons row skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

