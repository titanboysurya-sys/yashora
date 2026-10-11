package com.ritvyom.yashoraReelgenerator.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritvyom.yashoraReelgenerator.data.ai.ProviderId
import com.ritvyom.yashoraReelgenerator.data.ai.UserAiUsageTracker
import com.ritvyom.yashoraReelgenerator.data.remote.GeminiQuotaTracker
import com.ritvyom.yashoraReelgenerator.presentation.components.AiProvidersStudioDialog
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize
import com.ritvyom.yashoraReelgenerator.presentation.viewmodels.MainViewModel
import java.text.NumberFormat

/**
 * Dedicated real-time API Quota, Token, and Consumption Monitoring Dashboard.
 * Displays live usage stats, progress bars, rate limits, and remaining balance
 * for both Gemini API and ElevenLabs Voice AI, plus additional configured BYOK providers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiUsageDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToVoiceStudio: () -> Unit = {}
) {
    val appLanguageState by viewModel.appLanguage.collectAsState()
    val geminiQuota by GeminiQuotaTracker.quotaState.collectAsState()
    val userUsageMap by UserAiUsageTracker.usageMap.collectAsState()
    val voxElevenSubscription by viewModel.voxElevenSubscription.collectAsState()
    val isFetchingVoxSubscription by viewModel.isFetchingSubscription.collectAsState()
    val elevenLabsApiKey by viewModel.elevenLabsApiKey.collectAsState()
    val activeVoiceName by viewModel.voxElevenVoiceName.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()

    var showProviderKeyDialog by remember { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getInstance() }

    LaunchedEffect(Unit) {
        if (elevenLabsApiKey.isNotBlank()) {
            viewModel.refreshVoxElevenSubscription()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "API Usage & Quota Dashboard".localize(appLanguageState),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Real-Time Gemini & ElevenLabs Consumption".localize(appLanguageState),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("api_dashboard_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (elevenLabsApiKey.isNotBlank()) {
                                viewModel.refreshVoxElevenSubscription()
                            }
                            UserAiUsageTracker.refreshAll()
                        },
                        modifier = Modifier.testTag("api_dashboard_refresh_btn")
                    ) {
                        if (isFetchingVoxSubscription) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Usage Stats"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("api_usage_dashboard_scroll"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Overview Health Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Consumption Monitor Active".localize(appLanguageState),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Direct metrics from Gemini & ElevenLabs APIs".localize(appLanguageState),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showProviderKeyDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("api_dashboard_manage_keys_btn")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Keys".localize(appLanguageState), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 2. Google Gemini API Real-Time Quota Card
            item {
                val geminiUserUsage = userUsageMap[ProviderId.GEMINI]
                val isCustomKey = geminiApiKey.isNotBlank()
                val dailyLimit = if (isCustomKey && (geminiUserUsage?.dailyLimit ?: 0) > 0) {
                    geminiUserUsage!!.dailyLimit
                } else {
                    GeminiQuotaTracker.DEFAULT_FREE_RPD
                }
                val todayReq = if (isCustomKey && geminiUserUsage != null) {
                    geminiUserUsage.todayRequests
                } else {
                    geminiQuota.todayRequests
                }
                val remainingDaily = (dailyLimit - todayReq).coerceAtLeast(0)
                val dailyProgress = if (dailyLimit > 0) (todayReq.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f
                val totalTokens = geminiUserUsage?.estimatedTokensUsed ?: (todayReq * 420L)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_quota_dashboard_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A73E8).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Google Gemini AI Quota".localize(appLanguageState),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isCustomKey) "Personal API Key (BYOK)" else "Developer Free Tier (Google AI)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (geminiQuota.isRateLimited) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = if (geminiQuota.isRateLimited) "Rate-Limited (429)" else "Active • 15 RPM",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (geminiQuota.isRateLimited) Color(0xFFC62828) else Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Daily Requests Progress Bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daily Requests (Free Tier 1,500 RPD)".localize(appLanguageState),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${numberFormat.format(todayReq)} / ${numberFormat.format(dailyLimit)} Req",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { dailyProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (dailyProgress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Remaining Today".localize(appLanguageState), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${numberFormat.format(remainingDaily)} Req",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remainingDaily < 100) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Estimated Tokens".localize(appLanguageState), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = numberFormat.format(totalTokens),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Reset (UTC)".localize(appLanguageState), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = geminiQuota.dailyResetCountdown.ifBlank { "00:00 UTC" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ElevenLabs Neural AI Character Usage Card
            item {
                val hasElevenKey = elevenLabsApiKey.isNotBlank()
                val isVerified = hasElevenKey && voxElevenSubscription != null && voxElevenSubscription?.tier != "circuit_breaker_active"
                val usedChars = voxElevenSubscription?.characterCount ?: 0
                val limitChars = voxElevenSubscription?.characterLimit ?: 10000
                val remainingChars = (limitChars - usedChars).coerceAtLeast(0)
                val charProgress = if (limitChars > 0) (usedChars.toFloat() / limitChars.toFloat()).coerceIn(0f, 1f) else 0f
                val tierName = (voxElevenSubscription?.tier ?: if (hasElevenKey) "Verifying" else "Free").uppercase()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("elevenlabs_quota_dashboard_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ElevenLabs Neural Voice Quota".localize(appLanguageState),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = if (isVerified) "Account Tier: $tierName" else if (hasElevenKey) "Key Verification Required" else "No ElevenLabs Key (Using Device TTS)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isVerified) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (isVerified) "Verified" else if (hasElevenKey) "Unverified" else "Not Configured",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVerified) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isVerified) {
                            // Character Consumption Progress Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Monthly Character Quota".localize(appLanguageState),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${numberFormat.format(usedChars)} / ${numberFormat.format(limitChars)} Chars",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { charProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (charProgress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            // Stats Summary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Remaining Characters".localize(appLanguageState), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = numberFormat.format(remainingChars),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (remainingChars < 1000) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Active Voice".localize(appLanguageState), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = activeVoiceName.ifBlank { "Rachel" }.take(14),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Connect your xi-api-key in Voice Studio to view live character metrics and unlock ultra-realistic neural speech.".localize(appLanguageState),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }

                        // Navigation Button to Voice Studio
                        OutlinedButton(
                            onClick = onNavigateToVoiceStudio,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Voice Studio & Configure ElevenLabs".localize(appLanguageState), fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // 4. Other Multi-AI Providers Breakdown (Groq, OpenAI, xAI Grok, DeepSeek)
            item {
                val otherProviders = listOf(ProviderId.GROQ, ProviderId.OPENAI, ProviderId.XAI, ProviderId.DEEPSEEK)
                val configuredOthers = otherProviders.filter { userUsageMap[it]?.totalRequests ?: 0 > 0 }

                if (configuredOthers.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Additional Configured BYOK Providers".localize(appLanguageState),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        configuredOthers.forEach { provider ->
                            val usage = userUsageMap[provider] ?: return@forEach
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(provider.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            text = "Today: ${usage.todayRequests} Req • Tokens: ${numberFormat.format(usage.estimatedTokensUsed)}",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Active",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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

    if (showProviderKeyDialog) {
        AiProvidersStudioDialog(
            viewModel = viewModel,
            languageState = appLanguageState,
            onDismiss = { showProviderKeyDialog = false }
        )
    }
}
