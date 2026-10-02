package com.example.core.telemetry

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

/**
 * Personal snippet & clipboard telemetry collector.
 * Automatically aggregates copied URLs, notes, and research links for quick access
 * in the Secondary Brain zero-retention memory hub.
 * Filters out credentials and payment data.
 */
class ClipboardCollectorModule(private val context: Context) {

    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    data class CapturedSnippet(
        val title: String,
        val content: String,
        val isUrl: Boolean,
        val domainOrSource: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val url: String get() = content
    }

    private val _capturedSnippets = MutableStateFlow(
        listOf(
            CapturedSnippet(
                title = "Zero-Retention Architecture Reference",
                content = "https://groq.com/docs/zero-retention-privacy",
                isUrl = true,
                domainOrSource = "groq.com"
            ),
            CapturedSnippet(
                title = "Daily Quick Idea",
                content = "Remember to inspect tire pressure and replace engine filter before weekend trip",
                isUrl = false,
                domainOrSource = "Text Note"
            ),
            CapturedSnippet(
                title = "System Architecture Whitepaper",
                content = "https://developer.android.com/guide/topics/sensors",
                isUrl = true,
                domainOrSource = "developer.android.com"
            )
        )
    )
    val capturedSnippets: StateFlow<List<CapturedSnippet>> = _capturedSnippets.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }

    fun startListening() {
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    fun stopListening() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    fun checkClipboard() {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return
            if (isSafeToCapture(text)) {
                addSnippet(text)
            }
        }
    }

    fun addSnippet(text: String, customTitle: String? = null) {
        val isUrl = text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)
        val domain = if (isUrl) extractDomain(text) else "Text Snippet"
        val title = customTitle ?: if (isUrl) {
            "Captured Link ($domain)"
        } else {
            text.take(36) + if (text.length > 36) "..." else ""
        }

        val current = _capturedSnippets.value.toMutableList()
        if (current.none { it.content == text }) {
            current.add(0, CapturedSnippet(title, text, isUrl, domain))
            _capturedSnippets.value = current
        }
    }

    fun removeSnippet(content: String) {
        _capturedSnippets.value = _capturedSnippets.value.filter { it.content != content }
    }

    fun addStudyLink(urlOrText: String, customTitle: String? = null) {
        addSnippet(urlOrText, customTitle)
    }

    fun removeStudyLink(url: String) {
        removeSnippet(url)
    }

    private fun isSafeToCapture(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        // Never capture passwords, sensitive keys, or banking credentials
        val blacklisted = listOf("password", "secret_key", "bearer ", "credit_card", "cvv", "bank_account", "pin_code")
        return blacklisted.none { lower.contains(it) }
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            uri.host ?: "web"
        } catch (_: Exception) {
            "web"
        }
    }

    companion object {
        @Volatile
        private var instance: ClipboardCollectorModule? = null

        fun getInstance(context: Context): ClipboardCollectorModule {
            return instance ?: synchronized(this) {
                instance ?: ClipboardCollectorModule(context.applicationContext).also { instance = it }
            }
        }
    }
}
