package com.ritvyom.yashoraReelgenerator.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ritvyom.yashoraReelgenerator.presentation.utils.localize

@Composable
fun SocialUploadSheet(
    title: String,
    caption: String,
    sourceUrl: String? = null,
    videoFilePath: String? = null,
    appLanguage: String = "en",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedToClipboard by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF130A24)),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFFFF007F)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8A2BE2), Color(0xFFFF007F))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Upload & Share to Media".localize(appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "1-Tap Direct Social Publisher".localize(appLanguage),
                                fontSize = 11.sp,
                                color = Color(0xFFD0BCFF)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Caption Preview Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1F1238),
                    border = BorderStroke(1.dp, Color(0xFF3B2366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIRAL CAPTION & TAGS".localize(appLanguage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00FF87),
                                letterSpacing = 1.sp
                            )
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(caption))
                                    copiedToClipboard = true
                                    Toast.makeText(context, "Caption & Hashtags copied!".localize(appLanguage), Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = if (copiedToClipboard) Color(0xFF00FF87) else Color(0xFFD0BCFF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (copiedToClipboard) "Copied".localize(appLanguage) else "Copy".localize(appLanguage),
                                    fontSize = 11.sp,
                                    color = if (copiedToClipboard) Color(0xFF00FF87) else Color(0xFFD0BCFF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = caption,
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Select Platform to Publish:".localize(appLanguage),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Social Grid Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SocialAppTargetTile(
                        appName = "Instagram",
                        iconEmoji = "📸",
                        accentColor = Color(0xFFE1306C),
                        onClick = {
                            shareToSpecificApp(context, "com.instagram.android", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "Shorts",
                        iconEmoji = "▶️",
                        accentColor = Color(0xFFFF0000),
                        onClick = {
                            shareToSpecificApp(context, "com.google.android.youtube", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "WhatsApp",
                        iconEmoji = "💬",
                        accentColor = Color(0xFF25D366),
                        onClick = {
                            shareToSpecificApp(context, "com.whatsapp", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "Facebook",
                        iconEmoji = "📘",
                        accentColor = Color(0xFF1877F2),
                        onClick = {
                            shareToSpecificApp(context, "com.facebook.katana", caption, videoFilePath)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SocialAppTargetTile(
                        appName = "X (Twitter)",
                        iconEmoji = "🐦",
                        accentColor = Color(0xFF1DA1F2),
                        onClick = {
                            shareToSpecificApp(context, "com.twitter.android", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "Telegram",
                        iconEmoji = "✈️",
                        accentColor = Color(0xFF0088CC),
                        onClick = {
                            shareToSpecificApp(context, "org.telegram.messenger", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "LinkedIn",
                        iconEmoji = "💼",
                        accentColor = Color(0xFF0A66C2),
                        onClick = {
                            shareToSpecificApp(context, "com.linkedin.android", caption, videoFilePath)
                        }
                    )
                    SocialAppTargetTile(
                        appName = "More Apps",
                        iconEmoji = "🌐",
                        accentColor = Color(0xFFB388FF),
                        onClick = {
                            shareViaSystemChooser(context, caption, videoFilePath)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Button: Direct System Share
                Button(
                    onClick = {
                        shareViaSystemChooser(context, caption, videoFilePath)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8A2BE2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share to All Media".localize(appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialAppTargetTile(
    appName: String,
    iconEmoji: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.18f))
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = iconEmoji, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = appName,
            color = Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

private fun shareToSpecificApp(
    context: Context,
    packageName: String,
    caption: String,
    videoFilePath: String?
) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, caption)
            setPackage(packageName)
            if (!videoFilePath.isNullOrBlank()) {
                val file = java.io.File(videoFilePath)
                if (file.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "video/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
            } else {
                type = "text/plain"
            }
        }
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        // App not installed on device, fallback to generic chooser
        Toast.makeText(context, "Opening standard share...", Toast.LENGTH_SHORT).show()
        shareViaSystemChooser(context, caption, videoFilePath)
    }
}

private fun shareViaSystemChooser(
    context: Context,
    caption: String,
    videoFilePath: String?
) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, caption)
            if (!videoFilePath.isNullOrBlank()) {
                val file = java.io.File(videoFilePath)
                if (file.exists()) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "video/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
            } else {
                type = "text/plain"
            }
        }
        val chooser = Intent.createChooser(shareIntent, "Share News Reel")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch share: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
