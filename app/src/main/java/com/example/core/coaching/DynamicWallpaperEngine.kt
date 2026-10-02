package com.example.core.coaching

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DynamicWallpaperEngine(private val context: Context) {

    fun generateChecklistBitmap(
        title: String,
        departureTime: String,
        checklistItems: List<String>,
        width: Int = 1080,
        height: Int = 2400
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background: Deep Cyber Midnight
        canvas.drawColor(Color.parseColor("#080C14"))

        // Paint setup
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val monoTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val monoNormal = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        // Subtle background grid lines
        paint.color = Color.parseColor("#152238")
        paint.strokeWidth = 2f
        for (x in 0..width step 120) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
        }
        for (y in 0..height step 120) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }

        // Top Accent Bar (Cyber Cyan)
        paint.color = Color.parseColor("#00E5FF")
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width.toFloat(), 18f, paint)

        // Date String
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val dateText = dateFormat.format(Date()).uppercase()
        paint.color = Color.parseColor("#64748B")
        paint.typeface = monoNormal
        paint.textSize = 34f
        canvas.drawText(dateText, 80f, 260f, paint)

        // Header Title
        paint.color = Color.parseColor("#00E5FF")
        paint.typeface = monoTypeface
        paint.textSize = 56f
        canvas.drawText("ENFORCER OS // PROTOCOL", 80f, 340f, paint)

        // Coaching Subtitle Card
        val cardRect = RectF(80f, 400f, (width - 80).toFloat(), 640f)
        paint.color = Color.parseColor("#0F1B2F")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)

        // Card Content
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FFFFFF")
        paint.typeface = monoTypeface
        paint.textSize = 42f
        canvas.drawText(title.uppercase(), 120f, 480f, paint)

        paint.color = Color.parseColor("#00E676")
        paint.typeface = monoNormal
        paint.textSize = 36f
        canvas.drawText("DEPARTURE TIME: $departureTime", 120f, 550f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 28f
        canvas.drawText("STATUS: PRE-DEPARTURE CHECKLIST ACTIVE", 120f, 600f, paint)

        // Checklist Section Title
        paint.color = Color.parseColor("#00E5FF")
        paint.typeface = monoTypeface
        paint.textSize = 40f
        canvas.drawText("// MANDATORY BAG ITEMS", 80f, 740f, paint)

        // Draw Checklist Items
        var currentY = 830f
        for ((index, item) in checklistItems.withIndex()) {
            val itemBox = RectF(80f, currentY - 50f, (width - 80).toFloat(), currentY + 45f)
            paint.color = Color.parseColor("#0D1626")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(itemBox, 16f, 16f, paint)

            // Border
            paint.color = Color.parseColor("#1F3354")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(itemBox, 16f, 16f, paint)

            // Checkbox icon
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.parseColor("#00E676")
            val cbRect = RectF(120f, currentY - 30f, 165f, currentY + 15f)
            canvas.drawRoundRect(cbRect, 8f, 8f, paint)

            // Item Number & Text
            paint.style = Paint.Style.FILL
            paint.typeface = monoTypeface
            paint.color = Color.parseColor("#F1F5F9")
            paint.textSize = 36f
            canvas.drawText("[0${index + 1}]  ${item.uppercase()}", 200f, currentY, paint)

            currentY += 120f
        }

        // Bottom Motivational Callout
        val bottomY = height - 280f
        val footerBox = RectF(80f, bottomY - 60f, (width - 80).toFloat(), bottomY + 80f)
        paint.color = Color.parseColor("#142010")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(footerBox, 20f, 20f, paint)

        paint.color = Color.parseColor("#00E676")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(footerBox, 20f, 20f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#00E676")
        paint.typeface = monoTypeface
        paint.textSize = 34f
        canvas.drawText("DISCIPLINE OVER EXCUSES", 120f, bottomY - 10f, paint)

        paint.color = Color.parseColor("#A7F3D0")
        paint.typeface = monoNormal
        paint.textSize = 28f
        canvas.drawText("Tap 'I Remember' in Enforcer OS to restore wallpaper.", 120f, bottomY + 40f, paint)

        return bitmap
    }

    fun applyLockscreenWallpaper(bitmap: Bitmap): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            Log.d("DynamicWallpaperEngine", "Lockscreen wallpaper applied successfully")
            true
        } catch (e: Exception) {
            Log.e("DynamicWallpaperEngine", "Failed to set lockscreen wallpaper", e)
            false
        }
    }

    fun clearLockscreenWallpaper(): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.clear()
            }
            Log.d("DynamicWallpaperEngine", "Lockscreen wallpaper cleared")
            true
        } catch (e: Exception) {
            Log.e("DynamicWallpaperEngine", "Failed to clear lockscreen wallpaper", e)
            false
        }
    }
}
