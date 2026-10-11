package com.ritvyom.yashoraReelgenerator.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.ritvyom.yashoraReelgenerator.YashoraApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Real-time usage and quota metrics for a single user-configured AI Provider key.
 */
data class UserProviderUsageState(
    val providerId: ProviderId,
    val totalRequests: Int = 0,
    val todayRequests: Int = 0,
    val successfulRequests: Int = 0,
    val failedRequests: Int = 0,
    val estimatedTokensUsed: Long = 0L,
    val lastUsedTimestamp: Long = 0L,
    val dailyLimit: Int = 0,            // 0 means unmetered / pay-as-you-go
    val minuteLimit: Int = 0,
    val tierLabel: String = "User Custom API Key (BYOK)",
    val lastModelUsed: String = "",
    val lastError: String? = null
) {
    val remainingDailyQuota: Int? = if (dailyLimit > 0) (dailyLimit - todayRequests).coerceAtLeast(0) else null
    val dailyProgressFraction: Float = if (dailyLimit > 0) (todayRequests.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Tracks strictly the user's OWN API key usage per provider (Gemini, Groq, OpenAI, xAI Grok, DeepSeek).
 * Does NOT track or show global app/system/vertex keys to the user.
 * Exactly mirrors how ElevenLabs shows remaining quota and character usage.
 */
object UserAiUsageTracker {
    private const val PREFS_NAME = "user_byok_ai_usage_prefs"
    private const val KEY_PREFIX_TOTAL = "total_req_"
    private const val KEY_PREFIX_TODAY = "today_req_"
    private const val KEY_PREFIX_SUCCESS = "success_req_"
    private const val KEY_PREFIX_FAILED = "failed_req_"
    private const val KEY_PREFIX_TOKENS = "tokens_"
    private const val KEY_PREFIX_LAST_TIME = "last_time_"
    private const val KEY_PREFIX_LAST_DATE = "last_date_"
    private const val KEY_PREFIX_LAST_MODEL = "last_model_"
    private const val KEY_PREFIX_LAST_ERROR = "last_error_"

    private const val KEY_PREFIX_HASH_LAST_DATE = "hash_last_date_"
    private const val KEY_PREFIX_HASH_TODAY = "hash_today_req_"
    private const val KEY_PREFIX_HASH_TOTAL = "hash_total_req_"
    private const val KEY_PREFIX_HASH_SUCCESS = "hash_success_req_"
    private const val KEY_PREFIX_HASH_FAILED = "hash_failed_req_"
    private const val KEY_PREFIX_HASH_TOKENS = "hash_tokens_"
    private const val KEY_PREFIX_HASH_LAST_TIME = "hash_last_time_"
    private const val KEY_PREFIX_HASH_LAST_MODEL = "hash_last_model_"
    private const val KEY_PREFIX_HASH_LAST_ERROR = "hash_last_error_"
    private const val KEY_ACTIVE_HASH_PREFIX = "active_hash_"

    private val _usageMap = MutableStateFlow<Map<ProviderId, UserProviderUsageState>>(emptyMap())
    val usageMap: StateFlow<Map<ProviderId, UserProviderUsageState>> = _usageMap.asStateFlow()

    private var prefs: SharedPreferences? = null

    init {
        refreshAll()
    }

    private fun computeKeyHash(key: String): String {
        val clean = key.trim()
        if (clean.isBlank()) return ""
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(clean.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(16)
        } catch (_: Exception) {
            clean.hashCode().toString()
        }
    }

    private fun getPrefs(): SharedPreferences? {
        if (prefs == null) {
            val ctx = YashoraApplication.getInstance()
            if (ctx != null) {
                prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
        return prefs
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Standard free/developer tier limits per provider if user is on free tier.
     * Gemini: 1500 RPD, 15 RPM
     * Groq: 14400 RPD, 30 RPM
     * OpenAI / DeepSeek / xAI: Pay-as-you-go (0 indicates unmetered requests, tracked by usage & tokens)
     */
    fun getDefaultDailyLimit(providerId: ProviderId): Int {
        return when (providerId) {
            ProviderId.GEMINI -> 1500
            ProviderId.GROQ -> 14400
            ProviderId.OPENAI -> 0      // Pay-as-you-go balance / tokens
            ProviderId.XAI -> 0         // Prepaid balance
            ProviderId.DEEPSEEK -> 0    // Prepaid balance
            ProviderId.CLAUDE -> 0
        }
    }

    fun getDefaultMinuteLimit(providerId: ProviderId): Int {
        return when (providerId) {
            ProviderId.GEMINI -> 15
            ProviderId.GROQ -> 30
            ProviderId.OPENAI -> 60
            ProviderId.XAI -> 60
            ProviderId.DEEPSEEK -> 60
            ProviderId.CLAUDE -> 60
        }
    }

    /**
     * Called when a key is saved or connected to link historical metrics for this specific key.
     */
    @Synchronized
    fun onKeyConnected(providerId: ProviderId, apiKey: String) {
        val p = getPrefs() ?: return
        val hash = computeKeyHash(apiKey)
        val editor = p.edit()
        editor.putString(KEY_ACTIVE_HASH_PREFIX + providerId.name, hash)

        // If this key already has historical data saved under its hash, restore/sync into active provider slot
        if (hash.isNotBlank() && p.contains(KEY_PREFIX_HASH_TOTAL + hash)) {
            val today = getTodayDateString()
            val hashLastDate = p.getString(KEY_PREFIX_HASH_LAST_DATE + hash, "") ?: ""
            val hashToday = if (hashLastDate == today) p.getInt(KEY_PREFIX_HASH_TODAY + hash, 0) else 0
            val hashTotal = p.getInt(KEY_PREFIX_HASH_TOTAL + hash, 0)
            val hashSuccess = p.getInt(KEY_PREFIX_HASH_SUCCESS + hash, 0)
            val hashFailed = p.getInt(KEY_PREFIX_HASH_FAILED + hash, 0)
            val hashTokens = p.getLong(KEY_PREFIX_HASH_TOKENS + hash, 0L)
            val hashLastTime = p.getLong(KEY_PREFIX_HASH_LAST_TIME + hash, 0L)
            val hashLastModel = p.getString(KEY_PREFIX_HASH_LAST_MODEL + hash, "") ?: ""
            val hashLastError = p.getString(KEY_PREFIX_HASH_LAST_ERROR + hash, null)

            editor.putString(KEY_PREFIX_LAST_DATE + providerId.name, hashLastDate)
                .putInt(KEY_PREFIX_TODAY + providerId.name, hashToday)
                .putInt(KEY_PREFIX_TOTAL + providerId.name, hashTotal)
                .putInt(KEY_PREFIX_SUCCESS + providerId.name, hashSuccess)
                .putInt(KEY_PREFIX_FAILED + providerId.name, hashFailed)
                .putLong(KEY_PREFIX_TOKENS + providerId.name, hashTokens)
                .putLong(KEY_PREFIX_LAST_TIME + providerId.name, hashLastTime)
                .putString(KEY_PREFIX_LAST_MODEL + providerId.name, hashLastModel)
                .putString(KEY_PREFIX_LAST_ERROR + providerId.name, hashLastError)
        }
        editor.apply()
        refreshAll()
    }

    /**
     * Records a request made strictly with the USER'S custom API key.
     */
    @Synchronized
    fun recordUserRequest(
        providerId: ProviderId,
        isSuccess: Boolean,
        model: String = "",
        estimatedTokens: Int = 0,
        errorMessage: String? = null
    ) {
        val p = getPrefs() ?: return
        val today = getTodayDateString()
        val lastDate = p.getString(KEY_PREFIX_LAST_DATE + providerId.name, "")

        val todayReq = if (lastDate == today) p.getInt(KEY_PREFIX_TODAY + providerId.name, 0) + 1 else 1
        val totalReq = p.getInt(KEY_PREFIX_TOTAL + providerId.name, 0) + 1
        val successReq = p.getInt(KEY_PREFIX_SUCCESS + providerId.name, 0) + (if (isSuccess) 1 else 0)
        val failedReq = p.getInt(KEY_PREFIX_FAILED + providerId.name, 0) + (if (!isSuccess) 1 else 0)
        val tokens = p.getLong(KEY_PREFIX_TOKENS + providerId.name, 0L) + estimatedTokens.coerceAtLeast(0)
        val now = System.currentTimeMillis()

        val editor = p.edit()
            .putString(KEY_PREFIX_LAST_DATE + providerId.name, today)
            .putInt(KEY_PREFIX_TODAY + providerId.name, todayReq)
            .putInt(KEY_PREFIX_TOTAL + providerId.name, totalReq)
            .putInt(KEY_PREFIX_SUCCESS + providerId.name, successReq)
            .putInt(KEY_PREFIX_FAILED + providerId.name, failedReq)
            .putLong(KEY_PREFIX_TOKENS + providerId.name, tokens)
            .putLong(KEY_PREFIX_LAST_TIME + providerId.name, now)
            .putString(KEY_PREFIX_LAST_MODEL + providerId.name, model)
            .putString(KEY_PREFIX_LAST_ERROR + providerId.name, if (!isSuccess) (errorMessage ?: "Failed") else null)

        // Also persist under active key hash so disconnecting & reconnecting this key remembers its usage!
        val activeHash = p.getString(KEY_ACTIVE_HASH_PREFIX + providerId.name, "") ?: ""
        if (activeHash.isNotBlank()) {
            val hashLastDate = p.getString(KEY_PREFIX_HASH_LAST_DATE + activeHash, "")
            val hashTodayReq = if (hashLastDate == today) p.getInt(KEY_PREFIX_HASH_TODAY + activeHash, 0) + 1 else 1
            val hashTotalReq = p.getInt(KEY_PREFIX_HASH_TOTAL + activeHash, 0) + 1
            val hashSuccessReq = p.getInt(KEY_PREFIX_HASH_SUCCESS + activeHash, 0) + (if (isSuccess) 1 else 0)
            val hashFailedReq = p.getInt(KEY_PREFIX_HASH_FAILED + activeHash, 0) + (if (!isSuccess) 1 else 0)
            val hashTokens = p.getLong(KEY_PREFIX_HASH_TOKENS + activeHash, 0L) + estimatedTokens.coerceAtLeast(0)

            editor.putString(KEY_PREFIX_HASH_LAST_DATE + activeHash, today)
                .putInt(KEY_PREFIX_HASH_TODAY + activeHash, hashTodayReq)
                .putInt(KEY_PREFIX_HASH_TOTAL + activeHash, hashTotalReq)
                .putInt(KEY_PREFIX_HASH_SUCCESS + activeHash, hashSuccessReq)
                .putInt(KEY_PREFIX_HASH_FAILED + activeHash, hashFailedReq)
                .putLong(KEY_PREFIX_HASH_TOKENS + activeHash, hashTokens)
                .putLong(KEY_PREFIX_HASH_LAST_TIME + activeHash, now)
                .putString(KEY_PREFIX_HASH_LAST_MODEL + activeHash, model)
                .putString(KEY_PREFIX_HASH_LAST_ERROR + activeHash, if (!isSuccess) (errorMessage ?: "Failed") else null)
        }

        editor.apply()
        refreshAll()
    }

    /**
     * Resets counters for active provider display when disconnected, while retaining key-specific historical hash data.
     */
    @Synchronized
    fun resetUserUsage(providerId: ProviderId) {
        val p = getPrefs() ?: return
        p.edit()
            .remove(KEY_PREFIX_LAST_DATE + providerId.name)
            .remove(KEY_PREFIX_TODAY + providerId.name)
            .remove(KEY_PREFIX_TOTAL + providerId.name)
            .remove(KEY_PREFIX_SUCCESS + providerId.name)
            .remove(KEY_PREFIX_FAILED + providerId.name)
            .remove(KEY_PREFIX_TOKENS + providerId.name)
            .remove(KEY_PREFIX_LAST_TIME + providerId.name)
            .remove(KEY_PREFIX_LAST_MODEL + providerId.name)
            .remove(KEY_PREFIX_LAST_ERROR + providerId.name)
            .remove(KEY_ACTIVE_HASH_PREFIX + providerId.name)
            .apply()

        refreshAll()
    }

    @Synchronized
    fun refreshAll() {
        val p = getPrefs()
        val today = getTodayDateString()
        val newMap = mutableMapOf<ProviderId, UserProviderUsageState>()

        for (provider in ProviderId.values()) {
            val lastDate = p?.getString(KEY_PREFIX_LAST_DATE + provider.name, "") ?: ""
            val todayReq = if (lastDate == today) p?.getInt(KEY_PREFIX_TODAY + provider.name, 0) ?: 0 else 0
            val totalReq = p?.getInt(KEY_PREFIX_TOTAL + provider.name, 0) ?: 0
            val successReq = p?.getInt(KEY_PREFIX_SUCCESS + provider.name, 0) ?: 0
            val failedReq = p?.getInt(KEY_PREFIX_FAILED + provider.name, 0) ?: 0
            val tokens = p?.getLong(KEY_PREFIX_TOKENS + provider.name, 0L) ?: 0L
            val lastTime = p?.getLong(KEY_PREFIX_LAST_TIME + provider.name, 0L) ?: 0L
            val lastModel = p?.getString(KEY_PREFIX_LAST_MODEL + provider.name, "") ?: ""
            val lastError = p?.getString(KEY_PREFIX_LAST_ERROR + provider.name, null)

            val dailyLimit = getDefaultDailyLimit(provider)
            val minuteLimit = getDefaultMinuteLimit(provider)

            newMap[provider] = UserProviderUsageState(
                providerId = provider,
                totalRequests = totalReq,
                todayRequests = todayReq,
                successfulRequests = successReq,
                failedRequests = failedReq,
                estimatedTokensUsed = tokens,
                lastUsedTimestamp = lastTime,
                dailyLimit = dailyLimit,
                minuteLimit = minuteLimit,
                tierLabel = when (provider) {
                    ProviderId.GEMINI -> "Gemini API Free Tier (Personal Key)"
                    ProviderId.GROQ -> "Groq Cloud (Ultra-Fast Free Tier)"
                    ProviderId.OPENAI -> "OpenAI Platform (Personal BYOK)"
                    ProviderId.XAI -> "xAI Grok Console (Personal BYOK)"
                    ProviderId.DEEPSEEK -> "DeepSeek Platform (Personal BYOK)"
                    ProviderId.CLAUDE -> "Anthropic Claude (Personal BYOK)"
                },
                lastModelUsed = lastModel,
                lastError = lastError
            )
        }

        _usageMap.value = newMap
    }
}
