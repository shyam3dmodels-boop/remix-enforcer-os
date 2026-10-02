package com.example.core.recorder

import com.example.data.local.entity.VoiceTaskEntity

object VoiceTaskParser {

    private val HOMEWORK_KEYWORDS = listOf(
        "note this for homework",
        "homework",
        "for homework",
        "assignment",
        "complete exercise",
        "problem set",
        "practice questions",
        "submit tomorrow",
        "due tomorrow",
        "hw"
    )

    private val IMPORTANT_TEST_KEYWORDS = listOf(
        "critical action item",
        "urgent priority",
        "high priority task",
        "important priority",
        "key action item",
        "top priority",
        "urgent follow-up",
        "mark this urgent",
        "starred item"
    )

    private val DUE_DATE_KEYWORDS = listOf(
        "due date",
        "submit by",
        "deadline",
        "by monday",
        "by friday",
        "before next class"
    )

    /**
     * Analyzes raw transcription or speech input string and converts it into a structured
     * VoiceTaskEntity with category ("HOMEWORK", "IMPORTANT_TEST", "DUE_DATE", "GENERAL")
     * and cleaned task title.
     */
    fun parseSpokenPhrase(rawSpeech: String, lectureTitle: String = "Classroom Notes"): VoiceTaskEntity {
        val trimmed = rawSpeech.trim()
        val lower = trimmed.lowercase()

        val category: String
        val urgency: String
        var cleanedTitle = trimmed

        when {
            IMPORTANT_TEST_KEYWORDS.any { lower.contains(it) } -> {
                category = "IMPORTANT_TEST"
                urgency = "CRITICAL"
                cleanedTitle = stripKeywords(trimmed, IMPORTANT_TEST_KEYWORDS)
            }
            HOMEWORK_KEYWORDS.any { lower.contains(it) } -> {
                category = "HOMEWORK"
                urgency = "HIGH"
                cleanedTitle = stripKeywords(trimmed, HOMEWORK_KEYWORDS)
            }
            DUE_DATE_KEYWORDS.any { lower.contains(it) } -> {
                category = "DUE_DATE"
                urgency = "HIGH"
                cleanedTitle = stripKeywords(trimmed, DUE_DATE_KEYWORDS)
            }
            else -> {
                category = "GENERAL"
                urgency = "NORMAL"
            }
        }

        if (cleanedTitle.isBlank()) {
            cleanedTitle = if (category == "HOMEWORK") {
                "Homework assignment from $lectureTitle"
            } else if (category == "IMPORTANT_TEST") {
                "Key exam concept to revise from $lectureTitle"
            } else {
                "Study note from $lectureTitle"
            }
        } else {
            // Capitalize first letter
            cleanedTitle = cleanedTitle.replaceFirstChar { it.uppercase() }
        }

        return VoiceTaskEntity(
            title = cleanedTitle,
            category = category,
            lectureTitle = lectureTitle,
            timestamp = System.currentTimeMillis(),
            isCompleted = false,
            urgencyLevel = urgency,
            rawVoiceText = rawSpeech
        )
    }

    private fun stripKeywords(text: String, keywords: List<String>): String {
        var result = text
        for (kw in keywords) {
            val regex = Regex("(?i)\\b" + Regex.escape(kw) + "\\b:?")
            result = regex.replace(result, "")
        }
        return result.trim().trim(':', '-', ',', ' ')
    }
}
