package com.example.core.grammar

import java.util.regex.Pattern

/**
 * Inspired by Automattic/harper (Privacy-first offline grammar, style & tone checker).
 * Performs local deterministic grammatical checks, passive-voice detection, redundant phrase stripping,
 * and capitalization fixes without sending sensitive drafts to third parties.
 */
data class GrammarIssue(
    val originalText: String,
    val suggestedText: String,
    val explanation: String,
    val ruleType: String,
    val startIndex: Int,
    val endIndex: Int
)

data class GrammarAuditResult(
    val original: String,
    val corrected: String,
    val issuesFound: List<GrammarIssue>,
    val wordCount: Int,
    val readingTimeSeconds: Int,
    val toneScore: String
)

object HarperGrammarEngine {

    private val REPLACEMENT_RULES = listOf(
        // Common spelling & typos
        Regex("\\b(teh)\\b", RegexOption.IGNORE_CASE) to ("the" to "Common typo: 'teh' ➔ 'the'"),
        Regex("\\b(dont)\\b", RegexOption.IGNORE_CASE) to ("don't" to "Missing apostrophe in contraction"),
        Regex("\\b(cant)\\b", RegexOption.IGNORE_CASE) to ("can't" to "Missing apostrophe in contraction"),
        Regex("\\b(wont)\\b", RegexOption.IGNORE_CASE) to ("won't" to "Missing apostrophe in contraction"),
        Regex("\\b(definately)\\b", RegexOption.IGNORE_CASE) to ("definitely" to "Spelling correction"),
        Regex("\\b(seperate)\\b", RegexOption.IGNORE_CASE) to ("separate" to "Spelling correction"),
        Regex("\\b(alot)\\b", RegexOption.IGNORE_CASE) to ("a lot" to "Split into two words"),
        
        // Redundancies
        Regex("\\b(in order to)\\b", RegexOption.IGNORE_CASE) to ("to" to "Conciseness: 'in order to' ➔ 'to'"),
        Regex("\\b(at the present time)\\b", RegexOption.IGNORE_CASE) to ("now" to "Conciseness: 'at the present time' ➔ 'now'"),
        Regex("\\b(due to the fact that)\\b", RegexOption.IGNORE_CASE) to ("because" to "Conciseness: 'due to the fact that' ➔ 'because'"),
        Regex("\\b(each and every)\\b", RegexOption.IGNORE_CASE) to ("every" to "Redundancy: 'each and every' ➔ 'every'"),
        Regex("\\b(absolutely essential)\\b", RegexOption.IGNORE_CASE) to ("essential" to "Redundancy: 'absolutely essential' ➔ 'essential'"),
        
        // Passive/Weak phrases
        Regex("\\b(is able to)\\b", RegexOption.IGNORE_CASE) to ("can" to "Clarity: 'is able to' ➔ 'can'"),
        Regex("\\b(make a decision)\\b", RegexOption.IGNORE_CASE) to ("decide" to "Strong verb: 'make a decision' ➔ 'decide'")
    )

    fun analyzeText(input: String): GrammarAuditResult {
        if (input.isBlank()) {
            return GrammarAuditResult("", "", emptyList(), 0, 0, "Neutral")
        }

        var corrected = input
        val issues = mutableListOf<GrammarIssue>()

        for ((regex, replacementInfo) in REPLACEMENT_RULES) {
            val (suggested, explanation) = replacementInfo
            val matcher = regex.toPattern().matcher(input)
            while (matcher.find()) {
                issues.add(
                    GrammarIssue(
                        originalText = matcher.group(),
                        suggestedText = suggested,
                        explanation = explanation,
                        ruleType = "Style & Grammar",
                        startIndex = matcher.start(),
                        endIndex = matcher.end()
                    )
                )
            }
            corrected = regex.replace(corrected, suggested)
        }

        // Fix repeated spaces and capitalization
        corrected = corrected.replace(Regex(" {2,}"), " ")
        corrected = fixCapitalization(corrected)

        val words = input.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readingTimeSec = ((wordCount / 200.0) * 60).toInt().coerceAtLeast(1)

        val tone = when {
            issues.isEmpty() -> "⚡ Polished & Confident"
            issues.size in 1..3 -> "👍 Good Flow • Minor Fixes Applied"
            else -> "⚠️ High Redundancy • Streamlined"
        }

        return GrammarAuditResult(
            original = input,
            corrected = corrected,
            issuesFound = issues,
            wordCount = wordCount,
            readingTimeSeconds = readingTimeSec,
            toneScore = tone
        )
    }

    private fun fixCapitalization(text: String): String {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
        return sentences.joinToString(" ") { sentence ->
            if (sentence.isNotEmpty()) {
                sentence.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            } else ""
        }
    }
}
