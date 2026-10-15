package com.ritvyom.yashoraReelgenerator.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.VideoView
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.provider.MediaStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

import com.ritvyom.yashoraReelgenerator.data.local.entities.ExportHistoryEntity
import com.ritvyom.yashoraReelgenerator.data.local.entities.ProjectEntity
import com.ritvyom.yashoraReelgenerator.presentation.components.*
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.MainViewModel
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import com.ritvyom.yashoraReelgenerator.presentation.utils.EditorConstants
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.ScriptViewModel
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.ScriptViewModelFactory
import coil.compose.AsyncImage
import coil.request.videoFrameMillis
import androidx.compose.ui.layout.ContentScale

// Color Constants for the luxury aesthetic
private val CosmicBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

private val CardBg: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

private val BorderColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

private val GlowingPurple: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

private val OnGlowingPurple: Color
    @Composable
    get() = MaterialTheme.colorScheme.onPrimary

private val HotPink: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

private val DarkAura: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

private val SecondaryMuted: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onCreateNewProject: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onDirectEditProjectCreated: () -> Unit,
    onOpenScriptGenerator: () -> Unit,
    onOpenVoiceProvider: (() -> Unit)? = null,
    onOpenManualStudio: (() -> Unit)? = null,
    onOpenNewsToReel: (() -> Unit)? = null,
    onOpenApiDashboard: (() -> Unit)? = null
) {
    val recentProjects by viewModel.recentProjects.collectAsState()
    val exportHistory by viewModel.exportHistory.collectAsState()
    val appLanguageState by viewModel.appLanguage.collectAsState()
    val globalFirebaseUser by com.ritvyom.yashoraReelgenerator.data.remote.FirebaseCloudManager.currentUserState.collectAsState()

    val scriptViewModel: ScriptViewModel = viewModel(
        factory = ScriptViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
    val cachedScripts by scriptViewModel.historyState.collectAsState()
    val savedVoiceClips by viewModel.savedVoiceClips.collectAsState()
    val currentlyPlayingClipId by viewModel.currentlyPlayingClipId.collectAsState()
    val communityReels by viewModel.communityReels.collectAsState()
    val isLoadingCommunityReels by viewModel.isLoadingCommunityReels.collectAsState()
    var selectedCommunityReelForPreview by remember { mutableStateOf<com.ritvyom.yashoraReelgenerator.data.remote.SupabaseCloudManager.CachedStoryboard?>(null) }

    var activeTabState by remember { mutableIntStateOf(0) } // 0 = Projects, 1 = Scripts, 2 = Voice & Audio, 3 = Exports
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var historyToDelete by remember { mutableStateOf<ExportHistoryEntity?>(null) }
    var historyToPlay by remember { mutableStateOf<ExportHistoryEntity?>(null) }
    var historyToShare by remember { mutableStateOf<ExportHistoryEntity?>(null) }
    var selectedLibraryScript by remember { mutableStateOf<com.ritvyom.yashoraReelgenerator.data.model.VideoScript?>(null) }
    var scriptToDelete by remember { mutableStateOf<com.ritvyom.yashoraReelgenerator.data.model.VideoScript?>(null) }
    var clipToDelete by remember { mutableStateOf<com.ritvyom.yashoraReelgenerator.data.voxeleven.SavedVoiceClip?>(null) }

    val context = LocalContext.current
    var isCreatingDirectProject by remember { mutableStateOf(false) }

    // Bottom Navigation tab selector (Home, AI Tools, Projects, Templates, Premium)
    val bottomTabSelected by viewModel.bottomTabSelected.collectAsState()
    val providerConfigs by viewModel.unifiedAiRouter.providerConfigs.collectAsState()
    val hasConfiguredAiKey = remember(providerConfigs) {
        providerConfigs.values.any { it.hasValidKey && it.isUserProvided }
    }
    val bottomTabHistory by viewModel.bottomTabHistory.collectAsState()

    // If current tab is AI Chat but user has no custom AI key configured, gracefully switch back to Home
    LaunchedEffect(hasConfiguredAiKey, bottomTabSelected) {
        if (bottomTabSelected == "AI Chat" && !hasConfiguredAiKey) {
            viewModel.changeTab("Home")
        }
    }

    fun changeTab(newTab: String) {
        viewModel.changeTab(newTab)
    }



    // Quick AI tools Dialog State
    var showQuickScriptWriterDialog by remember { mutableStateOf(false) }
    var showLearnHowItsWorksDialog by remember { mutableStateOf(false) }
    var showLogoAccountDialog by remember { mutableStateOf(false) }
    var showDirectEditChooserSheet by remember { mutableStateOf(false) }

    var pendingImportUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var directProjectLoadingPhaseText by remember { mutableStateOf("Loading Editor...") }

    val visualMediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val uriStrings = uris.map { it.toString() }
            isCreatingDirectProject = true
            directProjectLoadingPhaseText = "Loading Editor..."
            viewModel.createDirectEditProject(
                title = "Video Project #${System.currentTimeMillis() % 1000}",
                uris = uriStrings,
                context = context,
                autoTranscribeCaptions = false,
                onComplete = {
                    isCreatingDirectProject = false
                    onDirectEditProjectCreated()
                }
            )
        }
    }

    val directPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val uriStrings = uris.map { it.toString() }
            isCreatingDirectProject = true
            directProjectLoadingPhaseText = "Loading Editor..."
            viewModel.createDirectEditProject(
                title = "Direct Cut #${System.currentTimeMillis() % 1000}",
                uris = uriStrings,
                context = context,
                autoTranscribeCaptions = false,
                onComplete = {
                    isCreatingDirectProject = false
                    onDirectEditProjectCreated()
                }
            )
        } else {
            isCreatingDirectProject = true
            directProjectLoadingPhaseText = "Loading Editor..."
            viewModel.createDirectEditProject(
                title = "Direct Cut #${System.currentTimeMillis() % 1000}",
                uris = emptyList(),
                context = context,
                autoTranscribeCaptions = false,
                onComplete = {
                    isCreatingDirectProject = false
                    onDirectEditProjectCreated()
                }
            )
        }
    }

    val hasActiveHomeOverlay = historyToPlay != null || historyToShare != null ||
            projectToDelete != null || historyToDelete != null || scriptToDelete != null ||
            selectedLibraryScript != null || clipToDelete != null || showQuickScriptWriterDialog ||
            showLearnHowItsWorksDialog || showLogoAccountDialog || showDirectEditChooserSheet

    BackHandler(enabled = hasActiveHomeOverlay) {
        when {
            historyToPlay != null -> historyToPlay = null
            historyToShare != null -> historyToShare = null
            projectToDelete != null -> projectToDelete = null
            historyToDelete != null -> historyToDelete = null
            scriptToDelete != null -> scriptToDelete = null
            selectedLibraryScript != null -> selectedLibraryScript = null
            clipToDelete != null -> clipToDelete = null
            showQuickScriptWriterDialog -> showQuickScriptWriterDialog = false
            showLearnHowItsWorksDialog -> showLearnHowItsWorksDialog = false
            showLogoAccountDialog -> showLogoAccountDialog = false
            showDirectEditChooserSheet -> showDirectEditChooserSheet = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (bottomTabSelected != "Home") {
                        IconButton(onClick = { viewModel.changeTab("Home") }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back".localize(appLanguageState),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Violet Y brand logo icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(GlowingPurple, HotPink)
                                    )
                                )
                                .clickable { showLogoAccountDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Y",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Yashora".localize(appLanguageState),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "AI REEL GENERATOR".localize(appLanguageState),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GlowingPurple,
                                letterSpacing = 2.2.sp,
                                modifier = Modifier.padding(top = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Golden crown premium button
                    IconButton(
                        onClick = { changeTab("Premium") },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Passes",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Settings action button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings".localize(appLanguageState),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // User profile picture if authenticated
                    globalFirebaseUser?.let { user ->
                        Spacer(modifier = Modifier.width(6.dp))
                        UserProfileIcon(
                            user = user,
                            appLanguage = appLanguageState,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBg
                )
            )
        },
        floatingActionButton = {
            if (bottomTabSelected == "Home" || bottomTabSelected == "Projects") {
                FloatingActionButton(
                    onClick = { onCreateNewProject() },
                    containerColor = GlowingPurple,
                    contentColor = OnGlowingPurple,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("floating_create_reel_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Create Reel".localize(appLanguageState),
                            tint = OnGlowingPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Reel".localize(appLanguageState),
                            color = OnGlowingPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        thickness = 0.8.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomNavButton(
                            label = "Home",
                            iconNormal = Icons.Outlined.Home,
                            iconSelected = Icons.Default.Home,
                            isSelected = bottomTabSelected == "Home",
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { changeTab("Home") }
                        )
                        CustomNavButton(
                            label = "Edit",
                            iconNormal = Icons.Outlined.ContentCut,
                            iconSelected = Icons.Default.ContentCut,
                            isSelected = showDirectEditChooserSheet,
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { showDirectEditChooserSheet = true }
                        )
                        CustomNavButton(
                            label = "AI Tools",
                            iconNormal = Icons.Outlined.AutoAwesome,
                            iconSelected = Icons.Default.AutoAwesome,
                            isSelected = bottomTabSelected == "AI Tools",
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { changeTab("AI Tools") }
                        )
                        if (hasConfiguredAiKey) {
                            CustomNavButton(
                                label = "AI Chat",
                                iconNormal = Icons.Outlined.SmartToy,
                                iconSelected = Icons.Default.SmartToy,
                                isSelected = bottomTabSelected == "AI Chat",
                                appLanguage = appLanguageState,
                                modifier = Modifier.weight(1f),
                                onClick = { changeTab("AI Chat") }
                            )
                        }
                        CustomNavButton(
                            label = "My Library",
                            iconNormal = Icons.Outlined.VideoLibrary,
                            iconSelected = Icons.Default.VideoLibrary,
                            isSelected = bottomTabSelected == "Projects",
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { changeTab("Projects") }
                        )
                        CustomNavButton(
                            label = "Templates",
                            iconNormal = Icons.Outlined.GridView,
                            iconSelected = Icons.Default.GridView,
                            isSelected = bottomTabSelected == "Templates",
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { changeTab("Templates") }
                        )
                        CustomNavButton(
                            label = "Premium",
                            iconNormal = Icons.Outlined.WorkspacePremium,
                            iconSelected = Icons.Default.WorkspacePremium,
                            isSelected = bottomTabSelected == "Premium",
                            appLanguage = appLanguageState,
                            modifier = Modifier.weight(1f),
                            onClick = { changeTab("Premium") }
                        )
                    }
                }
            }
        },
        containerColor = CosmicBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (bottomTabSelected) {
                "Home" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        item {
                            // Section 1: Dashboard Hero Banner
                            DashboardHeroBanner(
                                appLanguage = appLanguageState,
                                onCreateClick = { onCreateNewProject() }
                            )
                        }

                        item {
                            // Section 2: Quick Tools Category Row
                            QuickCategoryRow(
                                appLanguage = appLanguageState,
                                onReelArchitectClick = { onCreateNewProject() },
                                onNewsToReelClick = onOpenNewsToReel,
                                onScriptClick = onOpenScriptGenerator,
                                onTemplatesClick = { changeTab("Templates") },
                                onVoiceProviderClick = onOpenVoiceProvider,
                                onManualStudioClick = { showDirectEditChooserSheet = true }
                            )
                        }

                        item {
                            // Section 2.2: Live RSS News to Reel Spotlight Card
                            NewsToReelSpotlightCard(
                                appLanguage = appLanguageState,
                                onClick = { onOpenNewsToReel?.invoke() }
                            )
                        }

                        item {
                            // Section 2.3: Trending Community Reels & Templates (Supabase Cloud Cache)
                            com.ritvyom.yashoraReelgenerator.presentation.components.TrendingCommunityReelsSection(
                                communityReels = communityReels,
                                isLoading = isLoadingCommunityReels,
                                appLanguage = appLanguageState,
                                onCardClick = { reel -> selectedCommunityReelForPreview = reel },
                                onRemixClick = { reel ->
                                    viewModel.createProjectFromCommunityReel(reel) { project ->
                                        onOpenProject(project)
                                    }
                                },
                                onSeeAllClick = { changeTab("Templates") }
                            )
                        }

                        // Section 2.5: User's Personal API Key Quota Tracker (Shows ONLY user's own BYOK keys, exactly like ElevenLabs)
                        if (hasConfiguredAiKey) {
                            item {
                                val userUsageMap by com.ritvyom.yashoraReelgenerator.data.ai.UserAiUsageTracker.usageMap.collectAsState()
                                val userConnectedProviders = remember(providerConfigs) {
                                    providerConfigs.values.filter { it.hasValidKey && it.isUserProvided }.map { it.providerId }
                                }
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        userConnectedProviders.forEach { pId ->
                                            com.ritvyom.yashoraReelgenerator.presentation.components.UserPersonalProviderUsageCard(
                                                providerId = pId,
                                                usage = userUsageMap[pId],
                                                hasValidKey = true,
                                                appLanguage = appLanguageState
                                            )
                                        }

                                        if (onOpenApiDashboard != null) {
                                            FilledTonalButton(
                                                onClick = onOpenApiDashboard,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("View Full API Quota & Token Dashboard ↗".localize(appLanguageState), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Custom Tab Header selector
                            CustomTabRowHeader(
                                recentProjectsCount = recentProjects.size,
                                exportHistoryCount = exportHistory.size,
                                cachedScriptsCount = cachedScripts.size,
                                savedVoiceClipsCount = savedVoiceClips.size,
                                activeTab = activeTabState,
                                appLanguage = appLanguageState,
                                onTabSelected = { activeTabState = it }
                            )
                        }

                        if (activeTabState == 0) {
                            // Projects List
                            if (recentProjects.isEmpty()) {
                                item {
                                    CustomEmptyStateView(
                                        appLanguageState = appLanguageState,
                                        onLearnWorksClick = { showLearnHowItsWorksDialog = true },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            } else {
                                items(recentProjects, key = { it.id }) { project ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        ProjectCard(
                                            project = project,
                                            onClick = { onOpenProject(project) },
                                            onDelete = { projectToDelete = project },
                                            onShare = {
                                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, project.title)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "${project.title}\n\n${project.scriptText}")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Reel Script"))
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (activeTabState == 1) {
                            // Saved Scripts List
                            if (cachedScripts.isEmpty()) {
                                item {
                                    CustomEmptyStateView(
                                        appLanguageState = appLanguageState,
                                        onLearnWorksClick = { showLearnHowItsWorksDialog = true },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            } else {
                                items(cachedScripts, key = { "script_${it.id}" }) { script ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        ScriptLibraryCard(
                                            script = script,
                                            appLanguageState = appLanguageState,
                                            onClick = { selectedLibraryScript = script },
                                            onDelete = { scriptToDelete = script },
                                            onFavorite = { scriptViewModel.toggleFavorite(script) }
                                        )
                                    }
                                }
                            }
                        } else if (activeTabState == 2) {
                            // Voice & Audio Clips List
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, GlowingPurple.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(GlowingPurple.copy(alpha = 0.18f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RecordVoiceOver,
                                                    contentDescription = null,
                                                    tint = GlowingPurple,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "AI Voice Studio".localize(appLanguageState),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "ElevenLabs Neural AI & Device TTS".localize(appLanguageState),
                                                    fontSize = 11.sp,
                                                    color = SecondaryMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onOpenVoiceProvider?.invoke() },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "New Voice".localize(appLanguageState),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            if (savedVoiceClips.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(24.dp)
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = GlowingPurple.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No saved voice clips yet".localize(appLanguageState),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Generate and save narration audio using ElevenLabs or Device TTS in AI Voice Studio.".localize(appLanguageState),
                                                fontSize = 11.sp,
                                                color = SecondaryMuted,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { onOpenVoiceProvider?.invoke() },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Open AI Voice Studio".localize(appLanguageState), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(savedVoiceClips, key = { "voice_clip_${it.id}" }) { clip ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
                                        VoiceClipCard(
                                            clip = clip,
                                            isPlaying = currentlyPlayingClipId == clip.id,
                                            appLanguageState = appLanguageState,
                                            onPlayToggle = {
                                                if (currentlyPlayingClipId == clip.id) {
                                                    viewModel.stopVoiceClipPlayback()
                                                } else {
                                                    viewModel.playVoiceClip(clip)
                                                }
                                            },
                                            onApplyToVideo = {
                                                viewModel.setCustomMasterVoice(clip.filePath, clip.title)
                                                Toast.makeText(context, "Applied as Master Voice for all scenes in Video!".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                                            },
                                            onDownload = {
                                                viewModel.downloadVoiceClipToStorage(context, clip)
                                            },
                                            onShare = {
                                                try {
                                                    val file = java.io.File(clip.filePath)
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "audio/*"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Voice Clip"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDelete = {
                                                clipToDelete = clip
                                            }
                                        )
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(110.dp))
                                }
                            }
                        } else {
                            // Exports List
                            if (exportHistory.isEmpty()) {
                                item {
                                    CustomEmptyStateView(
                                        appLanguageState = appLanguageState,
                                        onLearnWorksClick = { showLearnHowItsWorksDialog = true },
                                        modifier = Modifier.padding(top = 10.dp)
                                    )
                                }
                            } else {
                                items(exportHistory, key = { it.id }) { history ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        HistoryCard(
                                            history = history,
                                            appLanguageState = appLanguageState,
                                            onPlay = { historyToPlay = history },
                                            onShare = { historyToShare = history },
                                            onDelete = { historyToDelete = history }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "AI Tools" -> {
                    AIToolsTab(
                        appLanguageState = appLanguageState,
                        viewModel = viewModel,
                        onScriptGeneratorClick = onOpenScriptGenerator,
                        onVoiceProviderClick = onOpenVoiceProvider,
                        onManualStudioClick = { showDirectEditChooserSheet = true },
                        onNewsToReelClick = onOpenNewsToReel
                    )
                }

                "AI Chat" -> {
                    AiStudioChatTab(
                        viewModel = viewModel,
                        appLanguageState = appLanguageState,
                        onNavigateToScriptConfig = { generatedScript ->
                            viewModel.setQuickScript(generatedScript)
                            onCreateNewProject()
                        }
                    )
                }

                "Projects" -> {
                    // Full page list tab for projects & saved scripts library
                    Column(modifier = Modifier.fillMaxSize()) {
                        CustomTabRowHeader(
                            recentProjectsCount = recentProjects.size,
                            exportHistoryCount = exportHistory.size,
                            cachedScriptsCount = cachedScripts.size,
                            savedVoiceClipsCount = savedVoiceClips.size,
                            activeTab = activeTabState,
                            appLanguage = appLanguageState,
                            onTabSelected = { activeTabState = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (activeTabState == 0) {
                            if (recentProjects.isEmpty()) {
                                CustomEmptyStateView(
                                    appLanguageState = appLanguageState,
                                    onLearnWorksClick = { showLearnHowItsWorksDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, start = 16.dp, end = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(recentProjects, key = { it.id }) { project ->
                                        ProjectCard(
                                            project = project,
                                            onClick = { onOpenProject(project) },
                                            onDelete = { projectToDelete = project },
                                            onShare = {
                                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, project.title)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, "${project.title}\n\n${project.scriptText}")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Reel Script"))
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (activeTabState == 1) {
                            if (cachedScripts.isEmpty()) {
                                CustomEmptyStateView(
                                    appLanguageState = appLanguageState,
                                    onLearnWorksClick = { showLearnHowItsWorksDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, start = 16.dp, end = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(cachedScripts, key = { "script_${it.id}" }) { script ->
                                        ScriptLibraryCard(
                                            script = script,
                                            appLanguageState = appLanguageState,
                                            onClick = { selectedLibraryScript = script },
                                            onDelete = { scriptToDelete = script },
                                            onFavorite = { scriptViewModel.toggleFavorite(script) }
                                        )
                                    }
                                }
                            }
                        } else if (activeTabState == 2) {
                            if (savedVoiceClips.isEmpty()) {
                                CustomEmptyStateView(
                                    appLanguageState = appLanguageState,
                                    onLearnWorksClick = { showLearnHowItsWorksDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, start = 16.dp, end = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            border = BorderStroke(1.dp, GlowingPurple.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(
                                                        imageVector = Icons.Default.RecordVoiceOver,
                                                        contentDescription = null,
                                                        tint = GlowingPurple,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = "AI Voice Studio".localize(appLanguageState),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "ElevenLabs Neural AI & Device TTS".localize(appLanguageState),
                                                            fontSize = 10.sp,
                                                            color = SecondaryMuted
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { onOpenVoiceProvider?.invoke() },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("New Voice".localize(appLanguageState), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    items(savedVoiceClips, key = { "proj_voice_clip_${it.id}" }) { clip ->
                                        VoiceClipCard(
                                            clip = clip,
                                            isPlaying = currentlyPlayingClipId == clip.id,
                                            appLanguageState = appLanguageState,
                                            onPlayToggle = {
                                                if (currentlyPlayingClipId == clip.id) {
                                                    viewModel.stopVoiceClipPlayback()
                                                } else {
                                                    viewModel.playVoiceClip(clip)
                                                }
                                            },
                                            onApplyToVideo = {
                                                viewModel.setCustomMasterVoice(clip.filePath, clip.title)
                                                Toast.makeText(context, "Applied as Master Voice for all scenes in Video!".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                                            },
                                            onDownload = {
                                                viewModel.downloadVoiceClipToStorage(context, clip)
                                            },
                                            onShare = {
                                                try {
                                                    val file = java.io.File(clip.filePath)
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "audio/*"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Voice Clip"))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDelete = {
                                                clipToDelete = clip
                                            }
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(110.dp))
                                    }
                                }
                            }
                        } else {
                            if (exportHistory.isEmpty()) {
                                CustomEmptyStateView(
                                    appLanguageState = appLanguageState,
                                    onLearnWorksClick = { showLearnHowItsWorksDialog = true }
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 90.dp, start = 16.dp, end = 16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(exportHistory, key = { it.id }) { history ->
                                        HistoryCard(
                                            history = history,
                                            appLanguageState = appLanguageState,
                                            onPlay = { historyToPlay = history },
                                            onShare = { historyToShare = history },
                                            onDelete = { historyToDelete = history }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Templates" -> {
                    TemplatesTab(
                        appLanguageState = appLanguageState,
                        viewModel = viewModel,
                        onOpenProject = onOpenProject,
                        onTemplateSelect = { style, aspect, lang, voiceCat, voice, script, topic, pubStyle, musicCat, visMedium ->
                            viewModel.selectedStyleName.value = style
                            viewModel.selectedAspectRatio.value = aspect
                            viewModel.selectedLanguage.value = lang
                            viewModel.selectedVoiceCategory.value = voiceCat
                            viewModel.selectedVoiceName.value = voice
                            viewModel.scriptText.value = script
                            viewModel.topicContext.value = topic
                            viewModel.selectedPublishingStyle.value = pubStyle
                            viewModel.bgMusicCategory.value = musicCat
                            viewModel.selectedVisualMedium.value = visMedium
                            onCreateNewProject()
                        }
                    )
                }

                "Premium" -> {
                    PremiumPaywallTab(appLanguageState = appLanguageState)
                }
            }
        }
    }

    // Play Dialog overlay
    historyToPlay?.let { history ->
        ReelPlayerDialog(
            history = history,
            viewModel = viewModel,
            appLanguage = appLanguageState,
            onDismiss = { historyToPlay = null }
        )
    }

    // Video FileProvider Share Dialog overlay
    historyToShare?.let { history ->
        VideoShareDialog(
            filePath = history.filePath,
            projectTitle = history.projectTitle,
            aspectRatio = history.resolution,
            appLanguageState = appLanguageState,
            onDismiss = { historyToShare = null },
            onPlayVideo = {
                historyToPlay = history
                historyToShare = null
            }
        )
    }

    // Community Reel Preview Dialog Overlay (Supabase Caching)
    selectedCommunityReelForPreview?.let { reel ->
        com.ritvyom.yashoraReelgenerator.presentation.components.CommunityReelPreviewDialog(
            storyboard = reel,
            onDismiss = { selectedCommunityReelForPreview = null },
            onRemixClick = { targetReel ->
                selectedCommunityReelForPreview = null
                viewModel.createProjectFromCommunityReel(targetReel) { project ->
                    onOpenProject(project)
                }
            }
        )
    }

    // Direct Project Builder Loading Screen Overlay
    if (isCreatingDirectProject) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isCreatingDirectProject = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, GlowingPurple),
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 290.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PremiumCircularLoader(sizeDp = 48)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = directProjectLoadingPhaseText.localize(appLanguageState),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { isCreatingDirectProject = false }
                    ) {
                        Text(
                            text = "Cancel".localize(appLanguageState),
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showLogoAccountDialog) {
        UserProfileDetailsDialog(
            user = globalFirebaseUser,
            appLanguage = appLanguageState,
            onDismiss = { showLogoAccountDialog = false }
        )
    }

    if (showDirectEditChooserSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDirectEditChooserSheet = false },
            containerColor = Color(0xFF14141E),
            tonalElevation = 8.dp
        ) {
            val directEditScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(directEditScrollState)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlowingPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = GlowingPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Video Editor Studio".localize(appLanguageState),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Edit your own videos & photos directly".localize(appLanguageState),
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = { showDirectEditChooserSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Pick from Device (Gallery) - Highlighted Hero Card
                Surface(
                    onClick = {
                        showDirectEditChooserSheet = false
                        try {
                            visualMediaPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        } catch (e: Exception) {
                            directPickerLauncher.launch("*/*")
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1F1C2E),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GlowingPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlowingPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pick Videos & Photos from Phone".localize(appLanguageState),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select one or multiple clips from gallery to edit".localize(appLanguageState),
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = GlowingPurple, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Blank Canvas / Start Fresh
                Surface(
                    onClick = {
                        showDirectEditChooserSheet = false
                        isCreatingDirectProject = true
                        viewModel.createDirectEditProject(
                            title = "Project #${System.currentTimeMillis() % 1000}",
                            uris = emptyList(),
                            context = context,
                            onComplete = {
                                isCreatingDirectProject = false
                                onDirectEditProjectCreated()
                            }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1B1B26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF29293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddBox, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Blank Project".localize(appLanguageState),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Empty timeline to create from scratch".localize(appLanguageState),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Quick Script Writer dialog using Gemini
    if (showQuickScriptWriterDialog) {
        var topicInput by remember { mutableStateOf("") }
        var selectedStyle by remember { mutableStateOf("Cinematic") }
        var selectedLang by remember { mutableStateOf("English") }
        val isGenerating by viewModel.isGeneratingScript.collectAsState()
        val generationError by viewModel.scriptGenerationError.collectAsState()
        var showHomeScriptAd by remember { mutableStateOf(false) }

        MockInterstitialAdDialog(
            show = showHomeScriptAd,
            onDismiss = {
                showHomeScriptAd = false
                if (topicInput.trim().isNotEmpty()) {
                    viewModel.generateScript(
                        topic = topicInput,
                        style = selectedStyle,
                        language = selectedLang,
                        durationOption = "30 seconds"
                    ) { generated ->
                        viewModel.scriptText.value = generated
                        showQuickScriptWriterDialog = false
                        onCreateNewProject()
                    }
                }
            }
        )

        AlertDialog(
            onDismissRequest = { if (!isGenerating) showQuickScriptWriterDialog = false },
            title = {
                Text(
                    text = "AI Script Writer Assistant".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Write a short topic prompt, and our server generative model will create a viral storyboard script instantly.".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = { Text("e.g., 5 Morning Habits for High Productivity", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = BorderColor,
                            focusedBorderColor = GlowingPurple,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGenerating
                    )

                    if (isGenerating) {
                        ScriptGenerationLoadingSkeleton(
                            title = "Generating AI Script with Yashora AI Engine...",
                            appLanguageState = appLanguageState,
                            showSteps = true,
                            showCancelButton = false,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    generationError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topicInput.trim().isNotEmpty()) {
                            showHomeScriptAd = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowingPurple,
                        contentColor = OnGlowingPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = topicInput.trim().isNotEmpty() && !isGenerating
                ) {
                    Text("Generate & Compile".localize(appLanguageState), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickScriptWriterDialog = false }, enabled = !isGenerating) {
                    Text("Cancel".localize(appLanguageState), color = SecondaryMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Learn How It Works dialog
    if (showLearnHowItsWorksDialog) {
        AlertDialog(
            onDismissRequest = { showLearnHowItsWorksDialog = false },
            title = {
                Text(
                    text = "Understanding Yashora".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yashora is a high-fidelity cinematic contents system powered by Yashora Neural Core. By breaking down prompts into storyboard cells, it automates script writing, visual styles generation, and text-to-speech voice syncing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 17.sp
                    )
                    Text(
                        text = "1. Enter a script manually or generate one via AI write co-pilots.",
                        fontSize = 12.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "2. Customize parameters including TTS accent category, visual imagery style, aspect ratios and audio filters.",
                        fontSize = 12.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "3. Render inside our smart editor. Clip, adjust velocity, add transitions, overlays then export MP4 flawlessly.",
                        fontSize = 12.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLearnHowItsWorksDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowingPurple,
                        contentColor = OnGlowingPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It!".localize(appLanguageState), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete Confirmation Dialogs
    // Script Library Delete Confirmation
    scriptToDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            title = { Text("Delete Saved Script?".localize(appLanguageState), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete".localize(appLanguageState) + " '${script.title.ifEmpty { script.topic }}' " + "permanently? This action cannot be undone.".localize(appLanguageState), color = SecondaryMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        scriptViewModel.deleteScript(script)
                        scriptToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete".localize(appLanguageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { scriptToDelete = null }) {
                    Text("Cancel".localize(appLanguageState), color = SecondaryMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Voice Clip Delete Confirmation
    clipToDelete?.let { clip ->
        AlertDialog(
            onDismissRequest = { clipToDelete = null },
            title = { Text("Delete Voice Clip?".localize(appLanguageState), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete".localize(appLanguageState) + " '${clip.title}' " + "permanently? This action cannot be undone.".localize(appLanguageState), color = SecondaryMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoiceClip(clip)
                        clipToDelete = null
                        Toast.makeText(context, "Voice clip deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete".localize(appLanguageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { clipToDelete = null }) {
                    Text("Cancel".localize(appLanguageState), color = SecondaryMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Script Library Detail Dialog
    selectedLibraryScript?.let { script ->
        var editableTitle by remember(script.id) { mutableStateOf(script.title) }
        var editableContent by remember(script.id) { mutableStateOf(script.fullScript) }
        val clipboardManager = LocalClipboardManager.current

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedLibraryScript = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                color = CosmicBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedLibraryScript = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Saved Script Details".localize(appLanguageState),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = {
                                scriptViewModel.toggleFavorite(script)
                            }
                        ) {
                            Icon(
                                imageVector = if (script.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (script.isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { 
                                editableTitle = it 
                                scriptViewModel.updateScriptContent(script.id, it, editableContent)
                            },
                            label = { Text("Script Title".localize(appLanguageState)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = editableContent,
                            onValueChange = { 
                                editableContent = it 
                                scriptViewModel.updateScriptContent(script.id, editableTitle, it)
                            },
                            label = { Text("Script Body".localize(appLanguageState)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 250.dp, max = 500.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Script properties tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Properties:".localize(appLanguageState),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryMuted
                            )
                            Box(
                                modifier = Modifier
                                    .background(GlowingPurple.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = script.tone.uppercase(Locale.getDefault()),
                                    fontSize = 10.sp,
                                    color = GlowingPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(HotPink.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = script.duration.uppercase(Locale.getDefault()),
                                    fontSize = 10.sp,
                                    color = HotPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = script.language,
                                fontSize = 12.sp,
                                color = SecondaryMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(editableContent))
                                Toast.makeText(context, "Script copied to clipboard!".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkAura)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy".localize(appLanguageState), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val updated = script.copy(title = editableTitle, fullScript = editableContent)
                                scriptViewModel.updateScriptContent(script.id, editableTitle, editableContent)
                                scriptViewModel.exportScriptToSupabase(updated) { success ->
                                    val msg = if (success) {
                                        if (appLanguageState.contains("hi", true)) "स्क्रिप्ट Supabase क्लाउड में सिंक हो गई!"
                                        else "Script synced to Supabase Cloud!"
                                    } else {
                                        "Script updates saved locally!".localize(appLanguageState)
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appLanguageState.contains("hi", true)) "Supabase सिंक" else "Sync Cloud", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.scriptText.value = editableContent
                                viewModel.topicContext.value = script.topic
                                selectedLibraryScript = null
                                onCreateNewProject()
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Video".localize(appLanguageState), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project draft?".localize(appLanguageState), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete".localize(appLanguageState) + " '${project.title}' " + "permanently? This action cannot be undone.".localize(appLanguageState), color = SecondaryMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete".localize(appLanguageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel".localize(appLanguageState), color = SecondaryMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    historyToDelete?.let { history ->
        AlertDialog(
            onDismissRequest = { historyToDelete = null },
            title = { Text("Remove Export Entry?".localize(appLanguageState), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will delete the history log for".localize(appLanguageState) + " '${history.projectTitle}'. " + "It does not delete physical MP4 off device storage.".localize(appLanguageState), color = SecondaryMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistory(history)
                        historyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                     Text("Remove".localize(appLanguageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { historyToDelete = null }) {
                    Text("Cancel".localize(appLanguageState), color = SecondaryMuted)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ----------------------------------------------------
// SUB-COMPONENTS
// ----------------------------------------------------

@Composable
fun CustomNavButton(
    label: String,
    iconNormal: androidx.compose.ui.graphics.vector.ImageVector,
    iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    appLanguage: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val localizedLabel = remember(label, appLanguage) {
        label.localize(appLanguage)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon container with active indicator pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) GlowingPurple.copy(alpha = 0.18f)
                        else Color.Transparent
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) iconSelected else iconNormal,
                    contentDescription = localizedLabel,
                    tint = if (isSelected) GlowingPurple else SecondaryMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = localizedLabel,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) GlowingPurple else SecondaryMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp)
            )
        }
    }
}

@Composable
fun DashboardHeroBanner(
    appLanguage: String,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF23144C)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF13093B), Color(0xFF08041B))
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    // Accent AI capsule badge
                    Box(
                        modifier = Modifier
                            .background(GlowingPurple.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .border(1.dp, GlowingPurple.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = GlowingPurple,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI POWERED".localize(appLanguage),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AI Reel Architect".localize(appLanguage),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Convert scripts into rich visual storyboards with custom neural models instantly.".localize(appLanguage),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .wrapContentWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(HotPink, GlowingPurple)
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Create New Reel".localize(appLanguage),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // 3D Isometric floating design on right side
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(110.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    OrbitingPlayDemoVisual()
                }
            }
        }
    }
}

@Composable
fun OrbitingPlayDemoVisual() {
    val GlowingPurple = GlowingPurple
    val HotPink = HotPink
    val infiniteTransition = rememberInfiniteTransition(label = "OrbitMotion")
    val tiltZ by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = -11f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TiltMotion"
    )

    Box(
        modifier = Modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background sphere
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    Brush.radialGradient(listOf(GlowingPurple.copy(alpha = 0.25f), Color.Transparent)),
                    shape = CircleShape
                )
        )

        // Floating tilted Orbit Card shape
        Card(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    rotationZ = tiltZ
                    rotationY = -28f
                    cameraDistance = 8f
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, GlowingPurple)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Drawings for orbiting space lines and stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw a thin orbiting oval ellipse around center
            drawContext.canvas.save()
            drawContext.canvas.rotate(-30f)
            drawArc(
                color = GlowingPurple.copy(alpha = 0.6f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.4f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawContext.canvas.restore()

            // Draw micro stars sparkles
            drawCircle(color = Color.White, radius = 2f, center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f))
            drawCircle(color = HotPink, radius = 3f, center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f))
            drawCircle(color = GlowingPurple, radius = 2.5f, center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.8f))
        }
    }
}

@Composable
fun QuickCategoryRow(
    appLanguage: String,
    onReelArchitectClick: () -> Unit,
    onNewsToReelClick: (() -> Unit)? = null,
    onScriptClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onVoiceProviderClick: (() -> Unit)? = null,
    onManualStudioClick: (() -> Unit)? = null
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            QuickToolTile(
                title = "AI Reel Architect",
                icon = "🎬",
                bgTint = Color(0xFF140D36),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = onReelArchitectClick
            )
        }
        item {
            QuickToolTile(
                title = "News to Reel",
                icon = "📰",
                bgTint = Color(0xFF1B0C38),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = { onNewsToReelClick?.invoke() }
            )
        }
        item {
            QuickToolTile(
                title = "AI Script Writer",
                icon = "📝",
                bgTint = Color(0xFF230D2E),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = onScriptClick
            )
        }
        item {
            QuickToolTile(
                title = "Voice Studio",
                icon = "🎙️",
                bgTint = Color(0xFF0D253A),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = { onVoiceProviderClick?.invoke() }
            )
        }
        item {
            QuickToolTile(
                title = "Edit",
                icon = "✂️",
                bgTint = Color(0xFF1A2A1A),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = { onManualStudioClick?.invoke() }
            )
        }
        item {
            QuickToolTile(
                title = "Templates",
                icon = "🪄",
                bgTint = Color(0xFF28180E),
                modifier = Modifier.width(76.dp),
                appLanguage = appLanguage,
                onClick = onTemplatesClick
            )
        }
    }
}

@Composable
fun NewsToReelSpotlightCard(
    appLanguage: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF130926)),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFFFF007F), Color(0xFF00FF87)))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF007F), Color(0xFF8A2BE2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📰", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE NEWS TO REEL".localize(appLanguage),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF00FF87).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FF87))
                    ) {
                        Text(
                            text = "NEW",
                            color = Color(0xFF00FF87),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Read live RSS stories, auto-craft AI reels & share to social media".localize(appLanguage),
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFFF007F),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun QuickToolTile(
    title: String,
    icon: String,
    bgTint: Color,
    modifier: Modifier = Modifier,
    appLanguage: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title.localize(appLanguage),
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CustomTabRowHeader(
    recentProjectsCount: Int,
    exportHistoryCount: Int,
    cachedScriptsCount: Int,
    savedVoiceClipsCount: Int,
    activeTab: Int,
    appLanguage: String,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab,
        containerColor = Color.Transparent,
        contentColor = GlowingPurple,
        divider = {},
        edgePadding = 12.dp,
        indicator = { tabPositions ->
            if (activeTab < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    height = 2.5.dp,
                    color = GlowingPurple
                )
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Tab(
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            text = {
                Text(
                    text = "Reels".localize(appLanguage) + " ($recentProjectsCount)",
                    fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (activeTab == 0) MaterialTheme.colorScheme.onBackground else SecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = if (activeTab == 0) GlowingPurple else SecondaryMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        Tab(
            selected = activeTab == 1,
            onClick = { onTabSelected(1) },
            text = {
                Text(
                    text = "Scripts".localize(appLanguage) + " ($cachedScriptsCount)",
                    fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (activeTab == 1) MaterialTheme.colorScheme.onBackground else SecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = if (activeTab == 1) GlowingPurple else SecondaryMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        Tab(
            selected = activeTab == 2,
            onClick = { onTabSelected(2) },
            text = {
                Text(
                    text = "Voice & Audio".localize(appLanguage) + " ($savedVoiceClipsCount)",
                    fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (activeTab == 2) MaterialTheme.colorScheme.onBackground else SecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (activeTab == 2) GlowingPurple else SecondaryMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        Tab(
            selected = activeTab == 3,
            onClick = { onTabSelected(3) },
            text = {
                Text(
                    text = "Exports".localize(appLanguage) + " ($exportHistoryCount)",
                    fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (activeTab == 3) MaterialTheme.colorScheme.onBackground else SecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = if (activeTab == 3) GlowingPurple else SecondaryMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

@Composable
fun CustomEmptyStateView(
    modifier: Modifier = Modifier,
    appLanguageState: String,
    onLearnWorksClick: () -> Unit
) {
    val GlowingPurple = GlowingPurple
    val HotPink = HotPink
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Background Core
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(GlowingPurple.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // Scaled Canvas Graphic elements for clouds & open box
            Canvas(modifier = Modifier.size(120.dp)) {
                val cx = size.width / 2f
                val cy = size.height * 0.65f
                val w = size.width * 0.22f
                val h = size.height * 0.16f
                
                // Cloud geometry above
                val cloudY = cy - h * 1.8f
                val cloudW = w * 1.4f
                val cloudH = h * 0.9f
                
                val cloudPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - cloudW * 0.8f, cloudY)
                    cubicTo(cx - cloudW * 0.8f, cloudY - cloudH * 0.6f, cx - cloudW * 0.4f, cloudY - cloudH * 0.8f, cx - cloudW * 0.1f, cloudY - cloudH * 0.5f)
                    cubicTo(cx + cloudW * 0.1f, cloudY - cloudH * 1.1f, cx + cloudW * 0.6f, cloudY - cloudH * 0.9f, cx + cloudW * 0.8f, cloudY - cloudH * 0.4f)
                    cubicTo(cx + cloudW * 1.1f, cloudY - cloudH * 0.2f, cx + cloudW * 1.1f, cloudY + cloudH * 0.2f, cx + cloudW * 0.8f, cloudY + cloudH * 0.4f)
                    cubicTo(cx + cloudW * 0.8f, cloudY + cloudH * 0.6f, cx + cloudW * 0.3f, cloudY + cloudH * 0.6f, cx, cloudY + cloudH * 0.4f)
                    cubicTo(cx - cloudW * 0.3f, cloudY + cloudH * 0.6f, cx - cloudW * 0.8f, cloudY + cloudH * 0.5f, cx - cloudW * 0.8f, cloudY)
                    close()
                }
                drawPath(
                    path = cloudPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(GlowingPurple.copy(alpha = 0.85f), HotPink.copy(alpha = 0.85f))
                    )
                )

                // 3D Isometric box structure
                // Front Left Wall
                val frontLeftPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - w, cy)
                    lineTo(cx, cy + h * 0.5f)
                    lineTo(cx, cy + h * 1.4f)
                    lineTo(cx - w, cy + h * 0.9f)
                    close()
                }
                drawPath(
                    path = frontLeftPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E0E3D), Color(0xFF0F071F))
                    )
                )

                // Front Right Wall
                val frontRightPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy + h * 0.5f)
                    lineTo(cx + w, cy)
                    lineTo(cx + w, cy + h * 0.9f)
                    lineTo(cx, cy + h * 1.4f)
                    close()
                }
                drawPath(
                    path = frontRightPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C155E), Color(0xFF160B30))
                    )
                )

                // Bottom interior floor
                val topFloorPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + w, cy - h * 0.5f)
                    lineTo(cx, cy - h)
                    lineTo(cx - w, cy - h * 0.5f)
                    close()
                }
                drawPath(
                    path = topFloorPath,
                    color = Color(0xFF08020E)
                )

                // Left opening flap
                val leftFlapPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - w, cy)
                    lineTo(cx - w * 1.4f, cy + h * 0.15f)
                    lineTo(cx - w * 0.8f, cy + h * 0.55f)
                    lineTo(cx, cy + h * 0.5f)
                    close()
                }
                drawPath(
                    path = leftFlapPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4C1E8F), Color(0xFF2B0E57))
                    )
                )

                // Right opening flap
                val rightFlapPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy + h * 0.5f)
                    lineTo(cx + w * 0.8f, cy + h * 0.55f)
                    lineTo(cx + w * 1.4f, cy + h * 0.15f)
                    lineTo(cx + w, cy)
                    close()
                }
                drawPath(
                    path = rightFlapPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF6732D6), Color(0xFF3C1488))
                    )
                )

                // Twinkling coordinates sparks
                drawCircle(color = Color(0xFFE28CFE), radius = 4f, center = androidx.compose.ui.geometry.Offset(cx - w * 0.4f, cy - h * 1.1f))
                drawCircle(color = Color(0xFFA5BBFF), radius = 3f, center = androidx.compose.ui.geometry.Offset(cx + w * 0.6f, cy - h * 1.5f))
                drawCircle(color = Color.White, radius = 2.5f, center = androidx.compose.ui.geometry.Offset(cx, cy - h * 2.2f))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "No Project Drafts Found".localize(appLanguageState),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Click 'Create Reel' at the bottom right corner to start breaking down scripts".localize(appLanguageState),
            fontSize = 11.sp,
            color = SecondaryMuted,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Play Tutorial Button
        Button(
            onClick = onLearnWorksClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(100.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = GlowingPurple,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Learn How It Works".localize(appLanguageState),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val dateStr = formatter.format(Date(project.updatedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlowingPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (project.status == "Completed") Icons.Default.MovieFilter else Icons.Default.EditNote,
                    contentDescription = null,
                    tint = GlowingPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(GlowingPurple.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.videoStyle,
                            fontSize = 9.sp,
                            color = GlowingPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.language,
                        fontSize = 10.sp,
                        color = SecondaryMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = project.aspectRatio,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = SecondaryMuted
                    )
                    if (project.status.equals("Draft", ignoreCase = true)) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFA000).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Draft • Resume",
                                fontSize = 9.sp,
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    color = SecondaryMuted.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Share to Cloud Pool",
                        tint = GlowingPurple.copy(alpha = 0.85f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = SecondaryMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ScriptLibraryCard(
    script: com.ritvyom.yashoraReelgenerator.data.model.VideoScript,
    appLanguageState: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    val dateStr = formatter.format(Date(script.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlowingPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = GlowingPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.title.ifEmpty { "Script: ${script.topic}" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(GlowingPurple.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = script.tone.uppercase(Locale.getDefault()),
                            fontSize = 9.sp,
                            color = GlowingPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(HotPink.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = script.duration.uppercase(Locale.getDefault()),
                            fontSize = 9.sp,
                            color = HotPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = script.language,
                        fontSize = 10.sp,
                        color = SecondaryMuted
                    )
                }
                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    color = SecondaryMuted.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            
            // Favorite Button
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (script.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (script.isFavorite) Color.Red else SecondaryMuted.copy(alpha = 0.6f)
                )
            }
            
            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = SecondaryMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun VoiceClipCard(
    clip: com.ritvyom.yashoraReelgenerator.data.voxeleven.SavedVoiceClip,
    isPlaying: Boolean,
    appLanguageState: String,
    onPlayToggle: () -> Unit,
    onApplyToVideo: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_clip_card_${clip.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, if (isPlaying) GlowingPurple else BorderColor.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) GlowingPurple.copy(alpha = 0.09f) else CardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Play Button + Title & Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular 48dp Touch Target Play Button
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isPlaying) GlowingPurple else GlowingPurple.copy(alpha = 0.16f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop Voice Clip" else "Play Voice Clip",
                        tint = if (isPlaying) Color.White else GlowingPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clip.title.ifEmpty { "Voice Clip" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GlowingPurple.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = clip.engineName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlowingPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (clip.voiceName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HotPink.copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = clip.voiceName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotPink,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val kbSize = clip.fileSizeBytes / 1024
                        if (kbSize > 0) {
                            Text(
                                text = "${kbSize} KB",
                                fontSize = 10.sp,
                                color = SecondaryMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Script Text Preview
            if (clip.scriptText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = clip.scriptText,
                    fontSize = 11.5.sp,
                    color = SecondaryMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Bar: Use in Reel Primary + Download / Share / Delete Icon Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onApplyToVideo,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use in Reel".localize(appLanguageState),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Voice Clip",
                            tint = GlowingPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Voice Clip",
                            tint = SecondaryMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Voice Clip",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    history: ExportHistoryEntity,
    appLanguageState: String,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()) }
    val dateStr = formatter.format(Date(history.createdAt))
    val mbSize = "%.1f MB".format(history.fileSize / (1024f * 1024f))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = CardBg
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Reel".localize(appLanguageState),
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.projectTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2E7D32).copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = history.resolution,
                            fontSize = 9.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mbSize,
                        fontSize = 11.sp,
                        color = SecondaryMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  $dateStr",
                        fontSize = 10.sp,
                        color = SecondaryMuted.copy(alpha = 0.7f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = GlowingPurple,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Click to Play Reel | movies/Yashora".localize(appLanguageState),
                        fontSize = 10.sp,
                        color = GlowingPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share".localize(appLanguageState),
                    tint = GlowingPurple
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = SecondaryMuted.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AIToolsTab(
    appLanguageState: String,
    viewModel: MainViewModel,
    onScriptGeneratorClick: () -> Unit,
    onVoiceProviderClick: (() -> Unit)? = null,
    onManualStudioClick: (() -> Unit)? = null,
    onNewsToReelClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog Visibility States
    var showCaptionsDialog by remember { mutableStateOf(false) }
    var showChromaKeyDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }

    val items = listOf(
        AIToolDefinition(
            title = "Live News to Reel & Social Share",
            description = "Read live RSS news feeds (Google News, TechCrunch, NDTV, BBC, Mint) and convert any story into a viral AI Reel with 1-click social media upload.",
            emoji = "📰",
            action = { onNewsToReelClick?.invoke() }
        ),
        AIToolDefinition(
            title = "Edit",
            description = "Direct device video & photo editing: Trim, audio extraction, voice pitch, speed curve, captions and multi-track mixing.",
            emoji = "✂️",
            action = { onManualStudioClick?.invoke() }
        ),
        AIToolDefinition(
            title = "AI Voice Studio",
            description = "High-definition human speech synthesis, multi-accent narration, and ElevenLabs voice engine studio.",
            emoji = "🎙️",
            action = { onVoiceProviderClick?.invoke() }
        ),
        AIToolDefinition(
            title = "AI Script Generator",
            description = "Input a prompt topic and let our servers write highly localized viral hooks structure.",
            emoji = "📝",
            action = onScriptGeneratorClick
        ),
        AIToolDefinition(
            title = "Captions & Color Codes",
            description = "Manage text overlay boundaries, styling fonts and word-by-word active coloring keys.",
            emoji = "🎨",
            action = { showCaptionsDialog = true }
        ),
        AIToolDefinition(
            title = "Chroma Key Backgrounds",
            description = "Easily load stock frames, or erase custom colors using modern greenscreen editors.",
            emoji = "🎥",
            action = { showChromaKeyDialog = true }
        ),
        AIToolDefinition(
            title = "Sound Design & Music",
            description = "Attach localized atmospheric tracks, sound effects, and high energy background scores.",
            emoji = "🎵",
            action = { showSoundDialog = true }
        ),
        AIToolDefinition(
            title = "Cinematic Aspect Ratios",
            description = "Instantly adapt generated frame storyboards between 9:16 Shorts or 16:9 widescreen.",
            emoji = "📐",
            action = { showAspectRatioDialog = true }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI Specialized Co-Pilots".localize(appLanguageState),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Creative, instant workflow accelerators customized for high CTR engagement Reels.".localize(appLanguageState),
            fontSize = 11.sp,
            color = SecondaryMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        items.forEach { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(onClick = tool.action),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlowingPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tool.emoji, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title.localize(appLanguageState),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tool.description.localize(appLanguageState),
                            fontSize = 11.sp,
                            color = SecondaryMuted,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SecondaryMuted
                    )
                }
            }
        }
    }

    // 1. CAPTIONS & COLOR CODES DIALOG
    if (showCaptionsDialog) {
        var selectedFont by remember { mutableStateOf("TikTok Style") }
        var selectedColorCode by remember { mutableStateOf("#FFFF00") } // default yellow neon
        var selectedDesign by remember { mutableStateOf("Classic Box") }

        AlertDialog(
            onDismissRequest = { showCaptionsDialog = false },
            title = {
                Text(
                    text = "Captions & Color Codes Studio".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Customize global subtitle presets and highlight styling color keys for maximum viewer retention.".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    // Font Selection
                    Column {
                        Text(
                            text = "Caption Font Style".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            listOf("TikTok Style", "Bold Impact", "Classic Sans", "Neon Glow", "Vintage Serif").forEach { font ->
                                val isSel = selectedFont == font
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFont = font },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) GlowingPurple.copy(alpha = 0.15f) else CardBg
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) GlowingPurple else BorderColor)
                                ) {
                                    Text(
                                        text = font.localize(appLanguageState),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSel) GlowingPurple else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Active Accent Color Selection
                    Column {
                        Text(
                            text = "Viral Key Color Highlight".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            val colors = listOf(
                                Pair("#FFFF00", Color(0xFFFFFF00)), // Yellow
                                Pair("#00E5FF", Color(0xFF00E5FF)), // Cyan
                                Pair("#FF007F", Color(0xFFFF007F)), // Pink
                                Pair("#39FF14", Color(0xFF39FF14)), // Lime
                                Pair("#FF6C00", Color(0xFFFF6C00)), // Orange
                                Pair("#FFFFFF", Color(0xFFFFFFFF))  // White
                            )
                            colors.forEach { (hex, clr) ->
                                val isSel = selectedColorCode.lowercase() == hex.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(clr)
                                        .border(
                                            width = if (isSel) 3.dp else 1.dp,
                                            color = if (isSel) GlowingPurple else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorCode = hex }
                                )
                            }
                        }
                    }

                    // Style Design
                    Column {
                        Text(
                            text = "Subtitle Design Template".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            listOf("Classic Box", "Double Outline", "Clean Minimal").forEach { design ->
                                val isSel = selectedDesign == design
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDesign = design },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) GlowingPurple.copy(alpha = 0.15f) else CardBg
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) GlowingPurple else BorderColor)
                                ) {
                                    Text(
                                        text = design.localize(appLanguageState),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSel) GlowingPurple else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Live Preview
                    Column {
                        Text(
                            text = "Acoustic Live Preview Frame".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryMuted
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0F0C1B), Color(0xFF2C1B4D))
                                    )
                                )
                                .border(1.dp, GlowingPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtitle Render engine preview
                            val parsedColor = remember(selectedColorCode) {
                                try { Color(android.graphics.Color.parseColor(selectedColorCode)) } catch (e: Exception) { Color.Yellow }
                            }
                            val textFont = when (selectedFont) {
                                "TikTok Style" -> FontFamily.SansSerif
                                "Bold Impact" -> FontFamily.Default
                                "Classic Sans" -> FontFamily.SansSerif
                                "Neon Glow" -> FontFamily.Monospace
                                "Vintage Serif" -> FontFamily.Serif
                                else -> FontFamily.Default
                            }

                            when (selectedDesign) {
                                "Classic Box" -> {
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Yashora: AI Reel Co-Pilot! 🔮",
                                            color = parsedColor,
                                            fontFamily = textFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                "Double Outline" -> {
                                    Text(
                                        text = "Yashora: AI Reel Co-Pilot! 🔮",
                                        color = parsedColor,
                                        fontFamily = textFont,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.graphicsLayer {
                                            shadowElevation = 8f
                                            shape = RoundedCornerShape(4.dp)
                                            clip = false
                                        },
                                        style = LocalTextStyle.current.copy(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black,
                                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                                blurRadius = 4f
                                            )
                                        )
                                    )
                                }
                                else -> {
                                    // Clean Minimal
                                    Text(
                                        text = "Yashora: AI Reel Co-Pilot! 🔮",
                                        color = parsedColor,
                                        fontFamily = textFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx("Pop")
                        }
                        Toast.makeText(context, "Global caption style preset saved! Future projects will use this aesthetic.".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                        showCaptionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                ) {
                    Text("Save Default Preset".localize(appLanguageState), color = OnGlowingPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCaptionsDialog = false }) {
                    Text("Cancel".localize(appLanguageState), color = GlowingPurple)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 2. CHROMA KEY BACKGROUNDS DIALOG
    if (showChromaKeyDialog) {
        var isChromaEnabled by remember { mutableStateOf(true) }
        var chromaTargetColor by remember { mutableStateOf("Green") }
        var chromaSensitivity by remember { mutableStateOf(0.5f) }

        AlertDialog(
            onDismissRequest = { showChromaKeyDialog = false },
            title = {
                Text(
                    text = "Chroma Key Greenscreen Engine".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Erase custom colored backdrops dynamically. Perfect for adding cinematic overlay scenes or overlay templates.".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    // Enable Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activate Auto Chroma Key".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isChromaEnabled,
                            onCheckedChange = { isChromaEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GlowingPurple
                            )
                        )
                    }

                    // Target Color Selector
                    Column {
                        Text(
                            text = "Chroma Screen Base Color".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            listOf("Green", "Blue", "Magenta").forEach { color ->
                                val isSel = chromaTargetColor == color
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { chromaTargetColor = color },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) GlowingPurple.copy(alpha = 0.15f) else CardBg
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) GlowingPurple else BorderColor)
                                ) {
                                    Text(
                                        text = color.localize(appLanguageState),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSel) GlowingPurple else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(vertical = 8.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Key Sensitivity / Threshold".localize(appLanguageState),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(chromaSensitivity * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlowingPurple
                            )
                        }
                        Slider(
                            value = chromaSensitivity,
                            onValueChange = { chromaSensitivity = it },
                            valueRange = 0.1f..0.9f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlowingPurple,
                                activeTrackColor = GlowingPurple,
                                inactiveTrackColor = BorderColor
                            ),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Interactive simulated keying output preview
                    Column {
                        Text(
                            text = "AI Keyer Real-Time Simulator".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryMuted
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                        ) {
                            // Left screen - Original Green Screen
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Raw Input Feed".localize(appLanguageState),
                                    fontSize = 9.sp,
                                    color = SecondaryMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (chromaTargetColor) {
                                                "Green" -> Color(0xFF14C832)
                                                "Blue" -> Color(0xFF1446C8)
                                                else -> Color(0xFFC814A0)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Right screen - Keyed Output
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Keyed Output".localize(appLanguageState),
                                    fontSize = 9.sp,
                                    color = SecondaryMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isChromaEnabled) {
                                                Modifier.background(
                                                    Brush.radialGradient(
                                                        listOf(Color(0xFF8A2BE2), Color(0xFF0F0922)),
                                                        radius = 120f
                                                    )
                                                )
                                            } else {
                                                Modifier.background(
                                                    when (chromaTargetColor) {
                                                        "Green" -> Color(0xFF14C832)
                                                        "Blue" -> Color(0xFF1446C8)
                                                        else -> Color(0xFFC814A0)
                                                    }
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (isChromaEnabled) Color.Cyan.copy(alpha = chromaSensitivity.coerceAtLeast(0.3f)) else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx("Sci-Fi Laser")
                        }
                        Toast.makeText(context, "Chroma key profile calibrated successfully! Setup active for future scene templates.".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                        showChromaKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                ) {
                    Text("Apply Chroma Defaults".localize(appLanguageState), color = OnGlowingPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChromaKeyDialog = false }) {
                    Text("Cancel".localize(appLanguageState), color = GlowingPurple)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 3. SOUND DESIGN & MUSIC DIALOG (Dynamic SFX synthesizers!)
    if (showSoundDialog) {
        var selectedMusicCat by remember { mutableStateOf(viewModel.bgMusicCategory.value) }
        var bgVolumeState by remember { mutableStateOf(viewModel.bgMusicVolume.value) }
        var voiceVolumeState by remember { mutableStateOf(viewModel.voiceVolume.value) }
        var textToAudition by remember { mutableStateOf("Yashora AI Reel Generator makes sound design effortless!") }

        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = {
                Text(
                    text = "Acoustic Sound Board Studio".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Synthesize local SFX on-the-fly, test narrative text voice profiles and balance mixer gains.".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    // Music Categories
                    Column {
                        Text(
                            text = "Background Score Track Theme".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .fillMaxWidth()
                        ) {
                            listOf("Cinematic", "Technology", "Inspirational", "Documentary", "Retro").forEach { mCat ->
                                val isSel = selectedMusicCat == mCat
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedMusicCat = mCat },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) GlowingPurple.copy(alpha = 0.15f) else CardBg
                                    ),
                                    border = BorderStroke(1.dp, if (isSel) GlowingPurple else BorderColor)
                                ) {
                                    Text(
                                        text = mCat.localize(appLanguageState),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSel) GlowingPurple else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(vertical = 6.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Audio Volume Sliders
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Music Gain Level".localize(appLanguageState), fontSize = 10.sp, color = SecondaryMuted)
                            Text("${(bgVolumeState * 100).toInt()}%", fontSize = 10.sp, color = GlowingPurple, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = bgVolumeState,
                            onValueChange = { bgVolumeState = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = GlowingPurple, activeTrackColor = GlowingPurple)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TTS Narration Gain".localize(appLanguageState), fontSize = 10.sp, color = SecondaryMuted)
                            Text("${(voiceVolumeState * 100).toInt()}%", fontSize = 10.sp, color = GlowingPurple, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = voiceVolumeState,
                            onValueChange = { voiceVolumeState = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = GlowingPurple, activeTrackColor = GlowingPurple)
                        )
                    }

                    // SFX Synth Pad Grid (Plays dynamically using SoundSynth!)
                    Column {
                        Text(
                            text = "Interactive SFX Synth Pad (Tap to Play)".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp).fillMaxWidth()
                        ) {
                            listOf("Whoosh", "Boom", "Pop").forEach { sfx ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            coroutineScope.launch { com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx(sfx) }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                                    border = BorderStroke(1.dp, Color(0xFF39FF14).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = Color(0xFF39FF14),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sfx,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp).fillMaxWidth()
                        ) {
                            listOf("Sci-Fi Laser", "Digital Hit", "Cyber Pulse").forEach { sfx ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            coroutineScope.launch { com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx(sfx) }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sfx,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TTS Voice Audition Field
                    Column {
                        Text(
                            text = "Audition Voice Engine Output".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = textToAudition,
                                onValueChange = { textToAudition = it },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GlowingPurple,
                                    unfocusedBorderColor = BorderColor
                                )
                            )
                            IconButton(
                                onClick = {
                                    viewModel.speakText(textToAudition)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlowingPurple)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Speak Audition",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bgMusicCategory.value = selectedMusicCat
                        viewModel.bgMusicVolume.value = bgVolumeState
                        viewModel.voiceVolume.value = voiceVolumeState
                        coroutineScope.launch {
                            com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx("Notification Swipe")
                        }
                        Toast.makeText(context, "Sound mixer profile locked! Global scores updated.".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                        showSoundDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                ) {
                    Text("Lock Sound Profile".localize(appLanguageState), color = OnGlowingPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("Cancel".localize(appLanguageState), color = GlowingPurple)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 4. CINEMATIC ASPECT RATIOS DIALOG
    if (showAspectRatioDialog) {
        var activeRatio by remember { mutableStateOf(viewModel.selectedAspectRatio.value) }

        AlertDialog(
            onDismissRequest = { showAspectRatioDialog = false },
            title = {
                Text(
                    text = "Cinematic Aspect Ratios Optimizer".localize(appLanguageState),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GlowingPurple
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Instantly crop, shape and scale your video frames. Select an aspect ratio to optimize frame composition.".localize(appLanguageState),
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        lineHeight = 15.sp
                    )

                    // Aspect Ratio Choices
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("9:16", "16:9", "1:1", "21:9").forEach { ratio ->
                            val isSel = activeRatio == ratio
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeRatio = ratio },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) GlowingPurple.copy(alpha = 0.15f) else CardBg
                                ),
                                border = BorderStroke(1.dp, if (isSel) GlowingPurple else BorderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = ratio,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSel) GlowingPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when(ratio) {
                                            "9:16" -> "Reels/Shorts"
                                            "16:9" -> "Widescreen"
                                            "1:1" -> "Square Grid"
                                            else -> "Anamorphic"
                                        }.localize(appLanguageState),
                                        fontSize = 8.sp,
                                        color = SecondaryMuted,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Framing Composition Canvas
                    Column {
                        Text(
                            text = "AI Framing Composition Simulator".localize(appLanguageState),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .border(1.dp, BorderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render mockup representation of scenic background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFF4500), Color(0xFFE0115F), Color(0xFF191970))
                                        )
                                    )
                            ) {
                                // Simple landscape grid mockup
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                                        radius = 40f,
                                        center = center.copy(y = center.y - 10f)
                                    )
                                }
                            }

                            // Superimpose the crop bounding box representing aspect ratio
                            val ratioModifier = when (activeRatio) {
                                "9:16" -> Modifier.aspectRatio(9f/16f).fillMaxHeight()
                                "16:9" -> Modifier.aspectRatio(16f/9f).fillMaxWidth(0.9f)
                                "1:1" -> Modifier.aspectRatio(1f).fillMaxHeight(0.9f)
                                else -> Modifier.aspectRatio(21f/9f).fillMaxWidth(0.95f) // 21:9
                            }

                            Box(
                                modifier = ratioModifier
                                    .border(2.5.dp, Color(0xFF39FF14), RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = activeRatio,
                                    color = Color(0xFF39FF14),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                )
                                Text(
                                    text = "Framed Region".localize(appLanguageState),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.selectedAspectRatio.value = activeRatio
                        coroutineScope.launch {
                            com.ritvyom.yashoraReelgenerator.presentation.utils.SoundSynth.playSfx("Boom")
                        }
                        Toast.makeText(context, "Default aspect ratio updated to $activeRatio!".localize(appLanguageState), Toast.LENGTH_SHORT).show()
                        showAspectRatioDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                ) {
                    Text("Update Default Ratio".localize(appLanguageState), color = OnGlowingPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAspectRatioDialog = false }) {
                    Text("Cancel".localize(appLanguageState), color = GlowingPurple)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

data class AIToolDefinition(
    val title: String,
    val description: String,
    val emoji: String,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesTab(
    appLanguageState: String,
    viewModel: MainViewModel,
    onOpenProject: ((ProjectEntity) -> Unit)? = null,
    onTemplateSelect: (
        style: String,
        aspect: String,
        language: String,
        ttsCategory: String,
        voice: String,
        script: String,
        topic: String,
        pubStyle: String,
        musicCat: String,
        visMedium: String
    ) -> Unit
) {
    val templates = listOf(
        TemplateItem(
            title = "Cinematic Daily Vlog",
            styleName = "Cinematic",
            aspectRatio = "9:16",
            language = "English",
            voiceCategory = "Male",
            voiceName = "Professional Man",
            icon = "🎥",
            description = "Create gorgeous modern vlogs with elegant pacing and warm realistic narration.",
            scriptText = "Rise and shine. There is something magical about the quiet hours of the morning. The sound of coffee dripping, the warm touch of early sunlight on the floor. Today is a clean slate. Let's make every single second count, finding beauty in the little things.",
            topicContext = "morning aesthetic, coffee drip, minimalist room, sunlight through window, soft walking, cozy lifestyle",
            publishingStyle = "TikTok / Instagram Reels",
            bgMusicCategory = "Cinematic",
            visualMedium = "Video"
        ),
        TemplateItem(
            title = "Cyberpunk Neon Promo",
            styleName = "Cyberpunk",
            aspectRatio = "9:16",
            language = "Hindi",
            voiceCategory = "Female",
            voiceName = "Energetic Girl",
            icon = "⚡",
            description = "Action packed, high contrast imagery and high energy neural voiceovers.",
            scriptText = "भविष्य यहाँ है। क्या आप तैयार हैं? रात की नियॉन रोशनियों में खो जाइए, जहाँ टेक्नोलॉजी और इंसानियत एक हो जाते हैं। भविष्य की गति को पहचानें, क्योंकि समय किसी का इंतज़ार नहीं करता। आज ही हमारे साथ इस सफर की शुरुआत करें!",
            topicContext = "neon city rain, glowing futuristic billboards, cybernetic street look, synthwave aesthetic, high energy metropolitan night",
            publishingStyle = "TikTok / Instagram Reels",
            bgMusicCategory = "Technology",
            visualMedium = "Video"
        ),
        TemplateItem(
            title = "Viral Motivational Quote",
            styleName = "Fantasy",
            aspectRatio = "9:16",
            language = "English",
            voiceCategory = "Female",
            voiceName = "Storyteller",
            icon = "✨",
            description = "Calm atmospheric backgrounds paired with inspiring, soulful female narration.",
            scriptText = "The universe isn't holding you back, and neither is anyone else. Your only limit is the size of your belief. Every giant oak tree started as a tiny seed that refused to give up. Keep pushing, keep dreaming. Your time is coming.",
            topicContext = "majestic mountain top, starry cosmic night, glowing ethereal clouds, abstract light trails, universe inspiration",
            publishingStyle = "TikTok / Instagram Reels",
            bgMusicCategory = "Inspirational",
            visualMedium = "Image"
        ),
        TemplateItem(
            title = "Retro Editorial Shorts",
            styleName = "Vintage",
            aspectRatio = "9:16",
            language = "English",
            voiceCategory = "Male",
            voiceName = "Deep Narrator",
            icon = "🎞️",
            description = "Timeless grain presets with historical storytelling baritone voice over.",
            scriptText = "In a world obsessed with speed, there is elegance in slowing down. The click of a typewriter. The scratch of ink on paper. Some stories aren't meant to be swiped away in seconds. They are written to endure, captured in timeless monochrome.",
            topicContext = "vintage typewriter, 1950s streets, classic jazz club, film grain, old fountain pen writing, nostalgic memories",
            publishingStyle = "Cinematic Vlog / Trailer",
            bgMusicCategory = "Documentary",
            visualMedium = "Video"
        ),
        TemplateItem(
            title = "Epic Travel Showcase",
            styleName = "Anime",
            aspectRatio = "16:9",
            language = "English",
            voiceCategory = "Male",
            voiceName = "Professional Man",
            icon = "✈️",
            description = "Widescreen colorful watercolor sketch frames perfect for scenic world travel reviews.",
            scriptText = "Pack your bags. The world is too beautiful to spend your life in one place. From the crystal clear turquoise coastlines to the mist-covered mountain peaks, adventure is calling. Let go of fear, step into the unknown, and write your own epic story.",
            topicContext = "gorgeous turquoise beaches, majestic cliff sunrise, winding mountain roads, epic world exploration, vibrant scenic drone shots",
            publishingStyle = "Cinematic Vlog / Trailer",
            bgMusicCategory = "Cinematic",
            visualMedium = "Video"
        )
    )

    val communityReels by viewModel.communityReels.collectAsState()
    var selectedTemplateSubTab by remember { mutableIntStateOf(0) }
    var previewingCommunityReel by remember { mutableStateOf<com.ritvyom.yashoraReelgenerator.data.remote.SupabaseCloudManager.CachedStoryboard?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab Switcher Header (Community Video Reels vs Style Presets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTemplateSubTab == 0) GlowingPurple else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (selectedTemplateSubTab == 0) HotPink else Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTemplateSubTab = 0 }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Community Reels (${communityReels.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selectedTemplateSubTab == 1) GlowingPurple else Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, if (selectedTemplateSubTab == 1) HotPink else Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTemplateSubTab = 1 }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Style Presets (${templates.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (selectedTemplateSubTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                com.ritvyom.yashoraReelgenerator.presentation.components.CommunityReelsGrid(
                    communityReels = communityReels,
                    onCardClick = { reel -> previewingCommunityReel = reel },
                    onRemixClick = { reel ->
                        viewModel.createProjectFromCommunityReel(reel) { project ->
                            onOpenProject?.invoke(project)
                        }
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Trending Presets".localize(appLanguageState),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Instantly configure high CTR Reels metadata using viral industry presets.".localize(appLanguageState),
                    fontSize = 11.sp,
                    color = SecondaryMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                templates.forEach { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(template.icon, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = template.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .background(GlowingPurple.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(template.styleName, fontSize = 8.sp, color = GlowingPurple, fontWeight = FontWeight.Bold)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(HotPink.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(template.aspectRatio, fontSize = 8.sp, color = HotPink, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        onTemplateSelect(
                                            template.styleName,
                                            template.aspectRatio,
                                            template.language,
                                            template.voiceCategory,
                                            template.voiceName,
                                            template.scriptText,
                                            template.topicContext,
                                            template.publishingStyle,
                                            template.bgMusicCategory,
                                            template.visualMedium
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GlowingPurple,
                                        contentColor = OnGlowingPurple
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Use Preset".localize(appLanguageState), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnGlowingPurple)
                                }
                            }

                            Text(
                                text = template.description,
                                fontSize = 12.sp,
                                color = SecondaryMuted,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    previewingCommunityReel?.let { reel ->
        com.ritvyom.yashoraReelgenerator.presentation.components.CommunityReelPreviewDialog(
            storyboard = reel,
            onDismiss = { previewingCommunityReel = null },
            onRemixClick = { targetReel ->
                previewingCommunityReel = null
                viewModel.createProjectFromCommunityReel(targetReel) { project ->
                    onOpenProject?.invoke(project)
                }
            }
        )
    }
}

data class TemplateItem(
    val title: String,
    val styleName: String,
    val aspectRatio: String,
    val language: String,
    val voiceCategory: String, // "Male", "Female"
    val voiceName: String,
    val icon: String,
    val description: String,
    val scriptText: String,
    val topicContext: String,
    val publishingStyle: String,
    val bgMusicCategory: String,
    val visualMedium: String
)

@Composable
fun PremiumPaywallTab(appLanguageState: String) {
    val utilities = listOf(
        PremiumUtility("Unlimited AI Scripting", "Uncapped generation. Bypass standard throttling limits completely.", "⚡"),
        PremiumUtility("Ultra HD stock visuals library", "Full access to high-definition royalty-free digital cinematic assets.", "💎"),
        PremiumUtility("Priority Express Render Cores", "Maximum processing priority on local and cloud rendering streams.", "🚀"),
        PremiumUtility("Aura Voice Narration Hub", "Access all premium vocal archetypes, dynamic speed, and voice synthesizers.", "🎙️"),
        PremiumUtility("Premium Cinematic Soundtracks", "Full integration of rhythmic backing beats and atmospheric loops.", "🎵")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C190F)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Yashora Pro - 100% Free".localize(appLanguageState),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "All premium cinematography features are unlocked and complimentary for our global creators.".localize(appLanguageState),
            fontSize = 12.sp,
            color = SecondaryMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Premium Status Card (Professional Level)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.5.dp, GlowingPurple.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "License Status".localize(appLanguageState),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active (Lifetime)".localize(appLanguageState),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Access Cost".localize(appLanguageState),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$0.00 / Month (Free)".localize(appLanguageState),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Plan Type".localize(appLanguageState),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Global Creator License".localize(appLanguageState),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        utilities.forEach { utility ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = utility.emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = utility.title.localize(appLanguageState),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = utility.subtitle.localize(appLanguageState),
                            fontSize = 11.sp,
                            color = SecondaryMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subscription CTA Buy Button - Free Active Status Style
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(GlowingPurple, HotPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lifetime Pro Status Active | Free".localize(appLanguageState),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Your professional creator license is fully active and synchronised.".localize(appLanguageState),
            color = SecondaryMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

data class PremiumUtility(
    val title: String,
    val subtitle: String,
    val emoji: String
)

// ----------------------------------------------------
// UTILS & SHARED LOGIC
// ----------------------------------------------------

fun getPlayableUri(context: Context, filePath: String, projectId: Int? = null): Uri? {
    if (filePath.isEmpty() && projectId == null) return null
    if (filePath.startsWith("content://") || filePath.startsWith("http://") || filePath.startsWith("https://")) {
        return Uri.parse(filePath)
    }
    val directFile = if (filePath.isNotEmpty()) java.io.File(filePath) else null
    if (directFile != null && directFile.exists() && directFile.length() > 0) {
        return Uri.fromFile(directFile)
    }
    return com.ritvyom.yashoraReelgenerator.presentation.utils.VideoFileManager.getPlayableUri(context, filePath, projectId)
        ?: try { if (filePath.isNotEmpty()) Uri.parse(filePath) else null } catch (e: Exception) { null }
}

fun shareVideo(context: Context, filePath: String, appLanguageState: String) {
    com.ritvyom.yashoraReelgenerator.presentation.utils.VideoShareManager.shareVideo(
        context = context,
        filePath = filePath,
        appLanguageState = appLanguageState
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUri: Uri,
    appLanguage: String,
    modifier: Modifier = Modifier,
    filePath: String? = null,
    autoPlay: Boolean = false
) {
    val context = LocalContext.current
    var isVideoError by remember { mutableStateOf(false) }
    var isPlayingState by remember { mutableStateOf(autoPlay) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val targetUri = remember(videoUri, filePath) {
        if (!filePath.isNullOrEmpty()) {
            val f = java.io.File(filePath)
            if (f.exists() && f.length() > 0) Uri.fromFile(f) else videoUri
        } else {
            videoUri
        }
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = autoPlay
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                isPlayingState = false
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("HomeScreen", "ExoPlayer playback error: ${error.message}", error)
                isVideoError = true
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isPlayingState = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(targetUri) {
        try {
            isVideoError = false
            val item = MediaItem.fromUri(targetUri)
            exoPlayer.setMediaItem(item)
            exoPlayer.prepare()
            if (isPlayingState) exoPlayer.play()
        } catch (e: Exception) {
            Log.e("HomeScreen", "Failed setting ExoPlayer media item", e)
            isVideoError = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0914)),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("🎬", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High Fidelity Streaming active... Tap to retry playback".localize(appLanguage),
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        isVideoError = false
                        try {
                            exoPlayer.setMediaItem(MediaItem.fromUri(targetUri))
                            exoPlayer.prepare()
                            exoPlayer.play()
                            isPlayingState = true
                        } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingPurple)
                ) {
                    Text("Retry Playback".localize(appLanguage), fontSize = 12.sp, color = Color.White)
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { pv ->
                    pv.player = exoPlayer
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        isPlayingState = !isPlayingState
                        if (isPlayingState) {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.play()
                        } else {
                            exoPlayer.pause()
                        }
                    }
            )
        }
    }
}

@Composable
fun ReelPlayerDialog(
    history: ExportHistoryEntity,
    viewModel: MainViewModel,
    appLanguage: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val videoUri = remember(history.filePath, history.projectId) {
        getPlayableUri(context, history.filePath, history.projectId)
    }
    var useVideoPlayer by remember { mutableStateOf(videoUri != null) }
    
    var scenes by remember { mutableStateOf<List<com.ritvyom.yashoraReelgenerator.domain.models.Scene>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentSceneIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var progressSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(history.projectId) {
        isLoading = true
        scenes = viewModel.getProjectScenes(history.projectId)
        isLoading = false
        if (scenes.isNotEmpty() && !useVideoPlayer) {
            isPlaying = true
        }
    }

    LaunchedEffect(isPlaying, currentSceneIndex, scenes, useVideoPlayer) {
        if (useVideoPlayer) {
            viewModel.stopSpeak()
            return@LaunchedEffect
        }
        if (isPlaying && scenes.isNotEmpty() && currentSceneIndex in scenes.indices) {
            val scene = scenes[currentSceneIndex]
            val sceneDuration = scene.durationSeconds.toFloat()
            progressSeconds = 0f
            
            val isVideo = scene.mediaType.equals("VIDEO", ignoreCase = true) ||
                    scene.visualPrompt.contains("Imported", ignoreCase = true) ||
                    com.ritvyom.yashoraReelgenerator.presentation.utils.ReelVideoCompiler.isVideoPath(context, scene.mediaPath ?: "")
            if (!isVideo && scene.narrationText.isNotBlank() && !viewModel.isPlaceholderNarration(scene.narrationText)) {
                viewModel.speakText(scene.narrationText)
            }
            
            while (isPlaying && progressSeconds < sceneDuration && !useVideoPlayer) {
                delay(100)
                progressSeconds += 0.1f
            }
            
            if (isPlaying && !useVideoPlayer) {
                if (currentSceneIndex < scenes.lastIndex) {
                    currentSceneIndex++
                } else {
                    isPlaying = false
                    progressSeconds = sceneDuration
                    viewModel.stopSpeak()
                }
            }
        } else {
            viewModel.stopSpeak()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeak()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        confirmButton = {},
        dismissButton = {},
        title = null,
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0914)),
                border = BorderStroke(1.5.dp, GlowingPurple.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = (if (useVideoPlayer) "High Fidelity Player" else "Reel Player Simulator").localize(appLanguage),
                                fontWeight = FontWeight.Bold,
                                color = GlowingPurple,
                                fontSize = 16.sp
                            )
                            Text(
                                text = history.projectTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { shareVideo(context, history.filePath, appLanguage) }) {
                                Icon(Icons.Default.Share, contentDescription = "Share".localize(appLanguage), tint = GlowingPurple)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close".localize(appLanguage), tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (videoUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { useVideoPlayer = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (useVideoPlayer) GlowingPurple else Color.Transparent,
                                    contentColor = if (useVideoPlayer) Color.White else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Real Video Player".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { useVideoPlayer = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!useVideoPlayer) GlowingPurple else Color.Transparent,
                                    contentColor = if (!useVideoPlayer) Color.White else Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scene Simulator".localize(appLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PremiumCircularLoader(sizeDp = 48)
                        }
                    } else if (scenes.isEmpty() && !useVideoPlayer) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Oops! Project assets cannot be loaded.".localize(appLanguage), color = Color.Gray, fontSize = 12.sp)
                        }
                    } else if (useVideoPlayer && videoUri != null) {
                        VideoPlayer(videoUri = videoUri, appLanguage = appLanguage, modifier = Modifier.fillMaxWidth(), filePath = history.filePath)
                    } else {
                        val activeScene = scenes[currentSceneIndex]
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1E1730), Color(0xFF0C0914))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!activeScene.mediaPath.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(activeScene.mediaPath)
                                            .crossfade(true)
                                            .videoFrameMillis(1000)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    val filterIndex = activeScene.selectedFilterIndex
                                    if (filterIndex in EditorConstants.FILTERS.indices) {
                                        val filterColor = EditorConstants.FILTERS[filterIndex].tintColor
                                        if (filterColor.alpha > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(filterColor)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                )
                                            )
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🎬", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Scene ${activeScene.sceneNumber}".localize(appLanguage), color = Color.Gray)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        activeScene.textOverlay?.let { over ->
                                            if (over.isNotEmpty()) {
                                                Text(
                                                    text = over.uppercase(),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = parseHexColor(activeScene.overlayColor),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                            }
                                        }

                                        Text(
                                            text = activeScene.subtitle,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = progressSeconds < 0.4f,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scene ${activeScene.sceneNumber} / ${scenes.size}".localize(appLanguage),
                                    color = GlowingPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Duration: ${activeScene.durationSeconds}s".localize(appLanguage),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            val currentSceneDuration = activeScene.durationSeconds.toFloat()
                            LinearProgressIndicator(
                                progress = (progressSeconds / currentSceneDuration).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = GlowingPurple,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentSceneIndex > 0) {
                                            currentSceneIndex--
                                            progressSeconds = 0f
                                        } else {
                                            currentSceneIndex = 0
                                            progressSeconds = 0f
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                FloatingActionButton(
                                    onClick = { isPlaying = !isPlaying },
                                    containerColor = GlowingPurple,
                                    contentColor = OnGlowingPurple,
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        modifier = Modifier.size(28.dp),
                                        tint = OnGlowingPurple
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        if (currentSceneIndex < scenes.lastIndex) {
                                            currentSceneIndex++
                                            progressSeconds = 0f
                                        } else {
                                            currentSceneIndex = 0
                                            progressSeconds = 0f
                                            isPlaying = false
                                            viewModel.stopSpeak()
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Real simulated vocal synthesis is active!".localize(appLanguage),
                                fontSize = 9.sp,
                                color = Color.Gray.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
    )
}

