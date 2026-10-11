package com.ritvyom.yashoraReelgenerator.data.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Robust date and time parser & human-readable formatter for News RSS & Atom feeds.
 * Accurately parses ISO-8601 (e.g. Aaj Tak "2026-09-26T18:02:02+05:30"),
 * RFC-822 / 1123 / 2822 (e.g. Navbharat Times "Wed, 30 Sep 2026 09:00:00 +0530", BBC "Wed, 30 Sep 2026 02:59:36 GMT"),
 * preserves the authentic publication timezone (India Standard Time Asia/Kolkata for Indian news),
 * and prevents inaccurate fallback to fetch-time timestamps.
 */
object NewsDateUtils {

    val TIMEZONE_IST: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    val TIMEZONE_UTC: TimeZone = TimeZone.getTimeZone("UTC")

    private val isoRegex = Pattern.compile(
        "^(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})[T\\s](\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:\\.\\d+)?(?:([+-]\\d{2}):?(\\d{2})|Z)?$"
    )

    private val rfcRegex = Pattern.compile(
        "^(?:[A-Za-z]{3},\\s+)?(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{4})\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?(?:\\s+([+-]\\d{4}|[+-]\\d{2}:?\\d{2}|[A-Za-z]{3,4}))?$"
    )

    private val urlDateRegex = Pattern.compile("(?:20\\d{2})[/-](?:0[1-9]|1[0-2])[/-](?:0[1-9]|[12]\\d|3[01])")

    private val standardFormatsWithTz = listOf(
        // ISO 8601 with TZ
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mmXXX",
        "yyyy-MM-dd'T'HH:mmZ",

        // RFC 822 / 1123 / 2822 with TZ
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss XXX",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss z",
        "EEE, d MMM yyyy HH:mm:ss XXX",

        "dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm:ss z",
        "dd MMM yyyy HH:mm:ss XXX",
        "d MMM yyyy HH:mm:ss Z",
        "d MMM yyyy HH:mm:ss z",
        "d MMM yyyy HH:mm:ss XXX",

        "yyyy-MM-dd HH:mm:ss Z",
        "yyyy-MM-dd HH:mm:ss z",
        "yyyy-MM-dd HH:mm:ss XXX"
    )

    private val standardFormatsWithoutTz = listOf(
        // ISO & RFC without explicit TZ (will default to IST)
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE, d MMM yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss",
        "d MMM yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy/MM/dd HH:mm:ss",
        "dd-MM-yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "dd MMM yyyy, hh:mm a",
        "dd MMM yyyy, HH:mm",
        "d MMM yyyy, hh:mm a",
        "EEE, dd MMM yyyy",
        "dd MMM yyyy",
        "d MMM yyyy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd-MM-yyyy",
        "dd/MM/yyyy"
    )

    /**
     * Determines the appropriate display timezone for a news article.
     * Indian news feeds (Aaj Tak, Navbharat Times, BBC Hindi, Amar Ujala, OneIndia, ABP, etc.)
     * are anchored to India Standard Time (Asia/Kolkata, UTC+05:30) so dates/times match
     * real Indian publication times regardless of the host device's local timezone.
     */
    fun getTimeZoneForPublication(rawDate: String?, isHindi: Boolean = true): TimeZone {
        if (!rawDate.isNullOrBlank()) {
            val clean = rawDate.trim()
            if (clean.contains("+05:30") || clean.contains("+0530") || clean.contains("IST", ignoreCase = true)) {
                return TIMEZONE_IST
            }
            val offsetMatcher = Pattern.compile("([+-]\\d{2}):?(\\d{2})").matcher(clean)
            if (offsetMatcher.find()) {
                val signAndHours = offsetMatcher.group(1)
                val mins = offsetMatcher.group(2)
                return TimeZone.getTimeZone("GMT$signAndHours:$mins")
            }
            if (clean.endsWith("Z", ignoreCase = true) || clean.contains("GMT", ignoreCase = true) || clean.contains("UTC", ignoreCase = true)) {
                return TIMEZONE_IST // Convert UTC/GMT to IST for Indian audience
            }
        }
        return TIMEZONE_IST // Default to IST for reliable India time
    }

    /**
     * Parses any RSS/Atom date string into epoch milliseconds.
     * Returns 0L if completely unparseable.
     */
    fun parseToEpochMillis(rawDate: String?): Long {
        if (rawDate.isNullOrBlank()) return 0L

        // Strip CDATA and sanitize
        var clean = rawDate.trim()
            .removePrefix("<![CDATA[").removeSuffix("]]>")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (clean.isBlank()) return 0L

        // Normalize timezone names for reliable parsing
        if (clean.endsWith(" IST", ignoreCase = true)) {
            clean = clean.dropLast(4) + " +0530"
        } else if (clean.endsWith(" UTC", ignoreCase = true)) {
            clean = clean.dropLast(4) + " GMT"
        }

        // 1. Try standard date patterns with explicit timezone
        for (pattern in standardFormatsWithTz) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = true
                val date = sdf.parse(clean)
                if (date != null && date.time > 0L) {
                    return date.time
                }
            } catch (_: Exception) {}
        }

        // 2. Try standard date patterns without timezone (anchor to IST for Indian news)
        for (pattern in standardFormatsWithoutTz) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TIMEZONE_IST
                    isLenient = true
                }
                val date = sdf.parse(clean)
                if (date != null && date.time > 0L) {
                    return date.time
                }
            } catch (_: Exception) {}
        }

        // 3. Regex parser for ISO-8601 strings like "2026-09-26T18:02:02+05:30" or "2026-09-30 07:12:11"
        try {
            val matcher = isoRegex.matcher(clean)
            if (matcher.matches()) {
                val year = matcher.group(1)?.toIntOrNull() ?: return 0L
                val month = (matcher.group(2)?.toIntOrNull() ?: 1) - 1
                val day = matcher.group(3)?.toIntOrNull() ?: 1
                val hour = matcher.group(4)?.toIntOrNull() ?: 0
                val min = matcher.group(5)?.toIntOrNull() ?: 0
                val sec = matcher.group(6)?.toIntOrNull() ?: 0

                val cal = Calendar.getInstance()
                val tzOffsetHour = matcher.group(7)
                val tzOffsetMin = matcher.group(8)

                if (clean.endsWith("Z", ignoreCase = true)) {
                    cal.timeZone = TIMEZONE_UTC
                } else if (!tzOffsetHour.isNullOrBlank() && !tzOffsetMin.isNullOrBlank()) {
                    cal.timeZone = TimeZone.getTimeZone("GMT$tzOffsetHour:$tzOffsetMin")
                } else {
                    cal.timeZone = TIMEZONE_IST
                }

                cal.set(year, month, day, hour, min, sec)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        } catch (_: Exception) {}

        // 4. Regex parser for RFC 822 / 1123 strings (e.g. "Wed, 30 Sep 2026 09:00:00 +0530")
        try {
            val matcher = rfcRegex.matcher(clean)
            if (matcher.matches()) {
                val day = matcher.group(1)?.toIntOrNull() ?: 1
                val monthStr = matcher.group(2)?.lowercase(Locale.US) ?: "sep"
                val year = matcher.group(3)?.toIntOrNull() ?: 2026
                val hour = matcher.group(4)?.toIntOrNull() ?: 0
                val min = matcher.group(5)?.toIntOrNull() ?: 0
                val sec = matcher.group(6)?.toIntOrNull() ?: 0
                val tzStr = matcher.group(7)

                val monthIndex = when (monthStr.take(3)) {
                    "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5
                    "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                    else -> 8
                }

                val cal = Calendar.getInstance()
                if (!tzStr.isNullOrBlank()) {
                    if (tzStr.equals("GMT", ignoreCase = true) || tzStr.equals("UTC", ignoreCase = true) || tzStr.equals("Z", ignoreCase = true)) {
                        cal.timeZone = TIMEZONE_UTC
                    } else if (tzStr.matches(Regex("[+-]\\d{4}"))) {
                        cal.timeZone = TimeZone.getTimeZone("GMT${tzStr.substring(0, 3)}:${tzStr.substring(3)}")
                    } else if (tzStr.matches(Regex("[+-]\\d{2}:?\\d{2}"))) {
                        val signHours = tzStr.substring(0, 3)
                        val mins = tzStr.takeLast(2)
                        cal.timeZone = TimeZone.getTimeZone("GMT$signHours:$mins")
                    } else {
                        cal.timeZone = TIMEZONE_IST
                    }
                } else {
                    cal.timeZone = TIMEZONE_IST
                }

                cal.set(year, monthIndex, day, hour, min, sec)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        } catch (_: Exception) {}

        // 5. Try extracting YYYY-MM-DD from clean string
        try {
            val um = urlDateRegex.matcher(clean)
            if (um.find()) {
                val datePart = um.group(0)?.replace('/', '-') ?: ""
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TIMEZONE_IST
                }
                val d = sdf.parse(datePart)
                if (d != null && d.time > 0L) {
                    return d.time
                }
            }
        } catch (_: Exception) {}

        return 0L
    }

    /**
     * Formats publication timestamp into verified date & time:
     * e.g. "26 Sep 2026, 06:02 PM IST"
     * Honors India Standard Time (IST) so Indian news is 100% accurate.
     */
    fun formatDateTime(
        rawDate: String?,
        epochMillis: Long = 0L,
        isHindi: Boolean = true
    ): String {
        val targetMillis = if (epochMillis > 0L) epochMillis else parseToEpochMillis(rawDate)
        if (targetMillis <= 0L) {
            return cleanRawStringFallback(rawDate)
        }

        val tz = getTimeZoneForPublication(rawDate, isHindi)
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).apply {
            timeZone = tz
        }
        val formatted = sdf.format(Date(targetMillis))
        return "$formatted IST"
    }

    /**
     * Formats into date only in authentic IST publication timezone: e.g. "26 Sep 2026"
     */
    fun formatDateOnly(
        epochMillis: Long,
        rawDate: String? = null,
        isHindi: Boolean = true
    ): String {
        if (epochMillis <= 0L) return "Today"
        val tz = getTimeZoneForPublication(rawDate, isHindi)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
            timeZone = tz
        }
        return sdf.format(Date(epochMillis))
    }

    /**
     * Formats into 12-hour time only in authentic IST publication timezone: e.g. "06:02 PM"
     */
    fun formatTimeOnly(
        epochMillis: Long,
        rawDate: String? = null,
        isHindi: Boolean = true
    ): String {
        if (epochMillis <= 0L) return ""
        val tz = getTimeZoneForPublication(rawDate, isHindi)
        val sdf = SimpleDateFormat("hh:mm a", Locale.US).apply {
            timeZone = tz
        }
        return sdf.format(Date(epochMillis))
    }

    /**
     * Calculates genuine relative time since RSS publication ("Kitna samay pehle publish hua").
     * Supports Hindi and English with verified accuracy.
     */
    fun formatRelativeTime(
        rawDate: String?,
        epochMillis: Long = 0L,
        isHindi: Boolean = false
    ): String {
        val targetMillis = if (epochMillis > 0L) epochMillis else parseToEpochMillis(rawDate)
        if (targetMillis <= 0L) {
            return if (isHindi) "आज" else "Today"
        }

        val now = System.currentTimeMillis()
        val diffMs = now - targetMillis

        // If published within the last 3 minutes or minor clock drift
        if (diffMs in -TimeUnit.MINUTES.toMillis(5)..TimeUnit.MINUTES.toMillis(3)) {
            return if (isHindi) "अभी-अभी (Just now)" else "Just now"
        }

        // If future date due to major server clock drift (> 5 mins), return date directly
        if (diffMs < -TimeUnit.MINUTES.toMillis(5)) {
            return formatDateOnly(targetMillis, rawDate, isHindi)
        }

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        if (minutes < 60) {
            return if (isHindi) "${minutes} मिनट पहले" else "${minutes}m ago"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        if (hours < 24) {
            return if (isHindi) "${hours} घंटे पहले" else "${hours}h ago"
        }

        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        if (days == 1L) {
            return if (isHindi) "कल (Yesterday)" else "Yesterday"
        }
        if (days < 7) {
            return if (isHindi) "${days} दिन पहले" else "${days}d ago"
        }

        return formatDateOnly(targetMillis, rawDate, isHindi)
    }

    /**
     * Complete publication verification details object for cards and reader screen.
     */
    fun getPublicationDetails(
        source: String,
        category: String,
        rawDate: String?,
        epochMillis: Long = 0L,
        isHindi: Boolean = false
    ): PublicationDetails {
        val targetMillis = if (epochMillis > 0L) epochMillis else parseToEpochMillis(rawDate)
        val relative = formatRelativeTime(rawDate, targetMillis, isHindi)
        val dateStr = if (targetMillis > 0L) formatDateOnly(targetMillis, rawDate, isHindi) else "Today"
        val timeStr = if (targetMillis > 0L) formatTimeOnly(targetMillis, rawDate, isHindi) else ""
        val fullDateTime = if (targetMillis > 0L) formatDateTime(rawDate, targetMillis, isHindi) else cleanRawStringFallback(rawDate)

        val diffMs = if (targetMillis > 0L) System.currentTimeMillis() - targetMillis else Long.MAX_VALUE
        val isVeryFresh = diffMs in 0..TimeUnit.HOURS.toMillis(2)
        val isToday = diffMs in 0..TimeUnit.HOURS.toMillis(24)

        val freshnessBadge = when {
            isVeryFresh -> if (isHindi) "⚡ ताज़ा ख़बर (Breaking)" else "⚡ Breaking News"
            isToday -> if (isHindi) "📅 आज का समाचार" else "📅 Today's Story"
            else -> if (isHindi) "📁 हालिया समाचार" else "📁 Recent Story"
        }

        val freshnessColor = when {
            isVeryFresh -> 0xFF00FF87 // Vibrant Emerald Green
            isToday -> 0xFF00F0FF // Cyan
            else -> 0xFFD0BCFF // Lavender Purple
        }

        return PublicationDetails(
            sourceName = source,
            category = category,
            relativeAge = relative,
            formattedDate = dateStr,
            formattedTime = timeStr,
            fullDateTime = fullDateTime,
            freshnessBadge = freshnessBadge,
            freshnessColor = freshnessColor,
            isVeryFresh = isVeryFresh,
            epochMillis = targetMillis
        )
    }

    fun cleanRawStringFallback(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return "Today"
        return rawDate.removePrefix("<![CDATA[").removeSuffix("]]>")
            .replace("T", " ")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .replace("Z", "")
            .trim()
    }
}

data class PublicationDetails(
    val sourceName: String,
    val category: String,
    val relativeAge: String,
    val formattedDate: String,
    val formattedTime: String,
    val fullDateTime: String,
    val freshnessBadge: String,
    val freshnessColor: Long,
    val isVeryFresh: Boolean,
    val epochMillis: Long
)
