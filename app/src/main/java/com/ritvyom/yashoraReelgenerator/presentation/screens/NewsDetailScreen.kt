package com.ritvyom.yashoraReelgenerator.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ritvyom.yashoraReelgenerator.data.model.NewsItem
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize

/**
 * Detailed View Screen for a selected news article.
 * Opens when a user taps a news card. Shows the full article text, metadata,
 * media gallery, and a prominent 'Generate Reel' button to start the video creation process.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    article: NewsItem,
    appLanguage: String,
    onBack: () -> Unit,
    onGenerateReel: (NewsItem) -> Unit,
    onShare: (NewsItem) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    var readerFontSize by remember { mutableStateOf(15.sp) }

    // Intercept back button to return to feed
    BackHandler {
        onBack()
    }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2200)
            isCopied = false
        }
    }

    val isHindiNews = appLanguage.contains("hi", ignoreCase = true) ||
            article.language.equals("Hindi", ignoreCase = true)
    val pubDetails = article.getPublicationDetails(isHindi = isHindiNews)

    val copyFullArticle: () -> Unit = {
        val textToCopy = buildString {
            appendLine(article.title)
            appendLine()
            val body = article.content.ifBlank { article.summary }
            if (body.isNotBlank()) {
                appendLine(body)
                appendLine()
            }
            if (article.source.isNotBlank()) {
                appendLine("📰 Source / स्रोत: ${article.source}")
            }
            appendLine("🕒 Published: ${pubDetails.fullDateTime} (${pubDetails.relativeAge})")
            if (article.link.isNotBlank()) {
                appendLine("🔗 Link: ${article.link}")
            }
        }
        clipboardManager.setText(AnnotatedString(textToCopy))
        isCopied = true
        Toast.makeText(context, "✓ Article text copied / पूरा टेक्स्ट कॉपी हो गया!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF8A2BE2).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = article.source.ifBlank { "News" },
                                color = Color(0xFF00FF87),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Article Details".localize(appLanguage),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("news_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick Copy
                    IconButton(
                        onClick = copyFullArticle,
                        modifier = Modifier.testTag("news_detail_copy_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Full Text",
                            tint = if (isCopied) Color(0xFF00FF87) else Color(0xFF00F0FF)
                        )
                    }
                    // Share
                    IconButton(onClick = { onShare(article) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFD0BCFF)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D051D)
                )
            )
        },
        bottomBar = {
            // Sticky Bottom CTA: Generate Reel from Article
            Surface(
                color = Color(0xFF14082B),
                border = BorderStroke(1.dp, Color(0xFF381D60)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onGenerateReel(article) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_reel_from_detail_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF007F)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "⚡ Choose 3 Draft Reel Styles".localize(appLanguage),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Breaking, Explainer & Storyteller drafts ready for editor".localize(appLanguage),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF090314)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Copy notification banner
            AnimatedVisibility(
                visible = isCopied,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = Color(0xFF00FF87),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0D061A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓ Full article text copied to clipboard!".localize(appLanguage),
                            color = Color(0xFF0D061A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hero Article Image
            if (!article.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF381F60), RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xAA090314))
                                )
                            )
                    )
                    // Source overlay chip
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "📰 ${article.source}",
                            color = Color(0xFF00FF87),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Categories & Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pill
                Surface(
                    color = Color(0xFF8A2BE2).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF8A2BE2))
                ) {
                    Text(
                        text = "🏷️ ${article.category.ifBlank { "Top News" }}",
                        color = Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Language Pill
                Surface(
                    color = if (article.language.equals("Hindi", ignoreCase = true)) Color(0xFFFF9933).copy(alpha = 0.2f) else Color(0xFF00F0FF).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (article.language.equals("Hindi", ignoreCase = true)) Color(0xFFFF9933) else Color(0xFF00F0FF))
                ) {
                    Text(
                        text = if (article.language.equals("Hindi", ignoreCase = true)) "🇮🇳 हिंदी" else "🇬🇧 English",
                        color = if (article.language.equals("Hindi", ignoreCase = true)) Color(0xFFFFB86C) else Color(0xFF00F0FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Freshness Tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(pubDetails.freshnessColor).copy(alpha = 0.18f),
                    border = BorderStroke(0.8.dp, Color(pubDetails.freshnessColor))
                ) {
                    Text(
                        text = pubDetails.freshnessBadge,
                        color = Color(pubDetails.freshnessColor),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Headline / Title
            SelectionContainer {
                Text(
                    text = article.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 27.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF14082B),
                border = BorderStroke(1.dp, Color(0xFF381D60)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF87))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = article.source,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = pubDetails.relativeAge,
                            color = Color(pubDetails.freshnessColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF2B164D), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Published / प्रकाशित: ${pubDetails.fullDateTime}",
                            color = Color(0xFFB39DDB),
                            fontSize = 11.sp
                        )

                        if (article.link.isNotBlank()) {
                            Row(
                                modifier = Modifier.clickable {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Original Link",
                                    color = Color(0xFF00F0FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFF00F0FF),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reading controls bar (Copy Full, Font Size Down / Up)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF100624),
                border = BorderStroke(1.dp, Color(0xFF2D164D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1-Click Copy
                    OutlinedButton(
                        onClick = copyFullArticle,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isCopied) Color(0xFF00FF87) else Color(0xFF00F0FF).copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = if (isCopied) Color(0xFF00FF87) else Color(0xFF00F0FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCopied) "Copied!" else "Copy Full Text",
                            color = if (isCopied) Color(0xFF00FF87) else Color(0xFF00F0FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Font Size adjustments
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Text Size:",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (readerFontSize.value > 12) {
                                    readerFontSize = (readerFontSize.value - 2).sp
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A-", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                if (readerFontSize.value < 24) {
                                    readerFontSize = (readerFontSize.value + 2).sp
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A+", color = Color(0xFF00FF87), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full Article Text Body
            SelectionContainer {
                Column {
                    val fullBody = article.content.ifBlank { article.summary }
                    val paragraphs = fullBody.split("\n\n", "\n").filter { it.isNotBlank() }

                    if (paragraphs.isNotEmpty()) {
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph.trim(),
                                color = Color(0xFFE2D6FF),
                                fontSize = readerFontSize,
                                lineHeight = (readerFontSize.value * 1.55).sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        Text(
                            text = article.title,
                            color = Color(0xFFE2D6FF),
                            fontSize = readerFontSize,
                            lineHeight = (readerFontSize.value * 1.55).sp
                        )
                    }
                }
            }

            // Attached Media Clips from RSS (if available)
            if (article.videoUrls.isNotEmpty() || article.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF14082B),
                    border = BorderStroke(1.dp, Color(0xFF381D60)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📸 Article Media Assets for Reel".localize(appLanguage),
                            color = Color(0xFF00FF87),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "These authentic media assets will be automatically incorporated into your generated video reel scenes.".localize(appLanguage),
                            color = Color(0xFFB39DDB),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(article.videoUrls) { vidUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2E174D))
                                        .border(1.dp, Color(0xFF00F0FF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Video Clip",
                                            tint = Color(0xFF00F0FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text("HD Clip", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            items(article.images) { imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF00FF87).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Image asset",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
