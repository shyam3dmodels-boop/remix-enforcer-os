package com.example.core.travel

import android.content.Context
import com.example.core.user.UserProfileManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Inspired by carolinaaafy/travel-memory-sticker-card.
 * Transforms user location pins, daily photos, and ambient telemetry into collectible travel sticker cards.
 */
data class TravelStickerCard(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val stickers: List<String>,
    val moodSummary: String,
    val weatherBadge: String,
    val formattedDate: String,
    val notes: String = ""
)

object TravelMemoryStickerCardHelper {

    private val STICKER_POOL = listOf(
        "📍 EXPLORER", "✨ MEMORY", "🚀 DISCOVERY", "🛡️ ENFORCED",
        "🎯 MISSION COMPLETED", "⚡ POWERED", "🌅 GOLDEN HOUR", "☕ FOCUS POINT"
    )

    fun createCardFromCurrentProfile(
        context: Context,
        title: String,
        notes: String = "",
        customStickers: List<String> = emptyList()
    ): TravelStickerCard {
        val profileManager = UserProfileManager.getInstance(context)
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        
        val selectedStickers = if (customStickers.isNotEmpty()) {
            customStickers
        } else {
            listOf(STICKER_POOL.random(), STICKER_POOL.random())
        }

        return TravelStickerCard(
            title = title.ifBlank { "Checkpoint @ ${profileManager.locationLabel.value}" },
            locationName = profileManager.locationLabel.value,
            latitude = profileManager.latitude.value,
            longitude = profileManager.longitude.value,
            timestamp = now,
            stickers = selectedStickers,
            moodSummary = profileManager.currentStatus.value,
            weatherBadge = "🌤️ Clear • 24°C",
            formattedDate = dateFormat.format(Date(now)),
            notes = notes
        )
    }

    fun toMarkdown(card: TravelStickerCard): String {
        val stickersStr = card.stickers.joinToString("  ") { "🏷️ `$it`" }
        return """
            ╔══════════════════════════════════════════════════╗
            ║ 🎴 TRAVEL MEMORY STICKER CARD                    ║
            ╠══════════════════════════════════════════════════╣
            ║ **${card.title}**
            ║ 📍 **Location:** ${card.locationName}
            ║ 🌐 **Coordinates:** ${"%.4f".format(card.latitude)}° N, ${"%.4f".format(card.longitude)}° E
            ║ 📅 **Date:** ${card.formattedDate}
            ║ 🏷️ **Badges:** $stickersStr
            ║ 💭 **Status:** ${card.moodSummary}
            ║ 🌤️ **Weather:** ${card.weatherBadge}
            ${if (card.notes.isNotBlank()) "║ 📝 **Notes:** ${card.notes}\n" else ""}╚══════════════════════════════════════════════════╝
        """.trimIndent()
    }
}
