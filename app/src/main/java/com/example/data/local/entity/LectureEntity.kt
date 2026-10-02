package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class LectureBookmark(
    val timeSeconds: Long,
    val label: String
)

@Entity(tableName = "lecture_recordings")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val coachingSessionName: String = "",
    val filePath: String,
    val durationSeconds: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val bookmarksJson: String = "[]",
    val notes: String = ""
) {
    fun parseBookmarks(): List<LectureBookmark> {
        val result = mutableListOf<LectureBookmark>()
        try {
            val array = JSONArray(bookmarksJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    LectureBookmark(
                        timeSeconds = obj.optLong("timeSeconds", 0L),
                        label = obj.optString("label", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun withAddedBookmark(timeSeconds: Long, label: String): LectureEntity {
        val current = parseBookmarks().toMutableList()
        current.add(LectureBookmark(timeSeconds, label.trim().ifEmpty { "Important Concept" }))
        current.sortBy { it.timeSeconds }
        return copy(bookmarksJson = encodeBookmarks(current))
    }

    companion object {
        fun encodeBookmarks(bookmarks: List<LectureBookmark>): String {
            val array = JSONArray()
            for (bm in bookmarks) {
                val obj = JSONObject()
                obj.put("timeSeconds", bm.timeSeconds)
                obj.put("label", bm.label)
                array.put(obj)
            }
            return array.toString()
        }
    }
}
