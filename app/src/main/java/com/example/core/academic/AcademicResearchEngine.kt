package com.example.core.academic

/**
 * Inspired by Imbad0202/academic-research-skills & THU-MAIC/OpenMAIC.
 * Performs deep research decomposition, concept tagging, and Anki flashcard generation from lectures and notes.
 */
data class AnkiFlashcard(
    val frontQuestion: String,
    val backAnswer: String,
    val tag: String
)

data class ResearchDigest(
    val title: String,
    val executiveSummary: String,
    val keyConcepts: List<String>,
    val actionableTakeaways: List<String>,
    val flashcards: List<AnkiFlashcard>,
    val citations: List<String>
)

object AcademicResearchEngine {

    fun synthesizeLectureOrText(
        topic: String,
        rawText: String,
        sourceLabel: String = "Lecture Note"
    ): ResearchDigest {
        val cleanTopic = topic.ifBlank { "Core Academic Study" }
        val sentences = rawText.split(Regex("(?<=[.!?])\\s+")).filter { it.length > 15 }

        val summary = if (sentences.isNotEmpty()) {
            sentences.take(3).joinToString(" ")
        } else {
            "Comprehensive breakdown and analysis of $cleanTopic with key takeaways and cognitive reinforcement."
        }

        val concepts = mutableListOf<String>()
        val flashcards = mutableListOf<AnkiFlashcard>()

        // Generate concepts and flashcards from text chunks
        if (sentences.isNotEmpty()) {
            for ((index, sentence) in sentences.take(5).withIndex()) {
                val concept = "Key Principle ${index + 1}: ${sentence.take(45)}..."
                concepts.add(concept)
                flashcards.add(
                    AnkiFlashcard(
                        frontQuestion = "What is the core premise of $cleanTopic (Point ${index + 1})?",
                        backAnswer = sentence,
                        tag = cleanTopic.lowercase().replace(" ", "_")
                    )
                )
            }
        } else {
            concepts.add("Foundations of $cleanTopic")
            flashcards.add(
                AnkiFlashcard(
                    frontQuestion = "Define $cleanTopic in 1 sentence.",
                    backAnswer = "A critical concept in Secondary Brain study discipline.",
                    tag = "academic_core"
                )
            )
        }

        val takeaways = listOf(
            "Reinforce definitions through active recall and flashcard repetition.",
            "Synthesize raw notes into concise mental models before sleep.",
            "Ground theory with real-world implementation in Android / Cloud."
        )

        val citations = listOf(
            "Secondary Brain Research Vault • $sourceLabel • ${System.currentTimeMillis()}"
        )

        return ResearchDigest(
            title = cleanTopic,
            executiveSummary = summary,
            keyConcepts = concepts,
            actionableTakeaways = takeaways,
            flashcards = flashcards,
            citations = citations
        )
    }

    fun exportAnkiTsv(digest: ResearchDigest): String {
        val sb = StringBuilder()
        sb.append("#separator:tab\n#html:false\n#tags column:3\n")
        for (card in digest.flashcards) {
            sb.append("${card.frontQuestion}\t${card.backAnswer}\t${card.tag}\n")
        }
        return sb.toString()
    }
}
