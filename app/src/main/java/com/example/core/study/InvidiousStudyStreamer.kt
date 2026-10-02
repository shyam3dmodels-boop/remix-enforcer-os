package com.example.core.study

/**
 * Inspired by iv-org/invidious (Distraction-free YouTube frontend).
 * Parses YouTube/lecture links, resolves distraction-free audio/video streams,
 * and extracts video IDs for instant AI summarization and Groq Whisper transcription.
 */
data class StudyVideoInfo(
    val videoId: String,
    val cleanUrl: String,
    val invidiousEmbedUrl: String,
    val titleSuggestion: String
)

object InvidiousStudyStreamer {

    private val YT_REGEX = Regex(
        "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})",
        RegexOption.IGNORE_CASE
    )

    fun parseVideoLink(rawUrl: String): StudyVideoInfo? {
        val matcher = YT_REGEX.find(rawUrl) ?: return null
        val videoId = matcher.groupValues.getOrNull(1) ?: return null

        return StudyVideoInfo(
            videoId = videoId,
            cleanUrl = "https://www.youtube.com/watch?v=$videoId",
            invidiousEmbedUrl = "https://yewtu.be/embed/$videoId?autoplay=0&controls=1",
            titleSuggestion = "Study Lecture [$videoId]"
        )
    }

    fun buildDistractionFreePrompt(videoInfo: StudyVideoInfo): String {
        return """
            Please synthesize and explain the core educational content of YouTube lecture: ${videoInfo.cleanUrl}
            - Focus on definitions, derivations, formulas, and actionable concepts.
            - Strip all sponsor plugs, intros, and conversational filler.
            - Output as structured bullet points suitable for quick study revision.
        """.trimIndent()
    }
}
