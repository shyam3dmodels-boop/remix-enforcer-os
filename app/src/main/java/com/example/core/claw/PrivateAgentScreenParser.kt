package com.example.core.claw

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Screen Layout Perception & Hierarchy Parser for PrivateAgent.
 *
 * Implements:
 * 1. Deep UI hierarchy tree parsing.
 * 2. Self-overlay exclusion filter (avoids hallucinations from own floating bubble/HUD).
 * 3. Exact screen bounding box calculation & center coordinate resolution for LLM grounding.
 */
object PrivateAgentScreenParser {

    private const val TAG = "PrivateAgentParser"
    private const val SELF_PACKAGE = "com.aistudio.enforcer.vhudxn"

    data class ParsedNode(
        val index: Int,
        val text: String,
        val contentDescription: String,
        val viewId: String,
        val className: String,
        val isClickable: Boolean,
        val isEditable: Boolean,
        val isCheckable: Boolean,
        val bounds: Rect,
        val centerX: Int,
        val centerY: Int
    )

    /**
     * Dumps active screen hierarchy as clean formatted JSON for LLM prompts,
     * filtering out nodes belonging to the agent's own floating HUD.
     */
    fun dumpScreenHierarchy(
        root: AccessibilityNodeInfo?,
        filterSelfOverlay: Boolean = true
    ): Pair<List<ParsedNode>, String> {
        if (root == null) return emptyList<ParsedNode>() to "[]"

        val nodes = mutableListOf<ParsedNode>()
        val jsonArray = JSONArray()

        var nodeIndex = 0

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return

            // Exclude self overlay if requested
            val pkg = node.packageName?.toString() ?: ""
            val viewId = node.viewIdResourceName ?: ""
            if (filterSelfOverlay && (pkg == SELF_PACKAGE || viewId.contains("pokeclaw") || viewId.contains("private_agent"))) {
                // Skip self overlay elements to prevent the LLM from trying to click its own HUD
                return
            }

            val text = node.text?.toString()?.trim() ?: ""
            val desc = node.contentDescription?.toString()?.trim() ?: ""
            val className = node.className?.toString()?.substringAfterLast(".") ?: "View"
            val clickable = node.isClickable
            val editable = node.isEditable
            val checkable = node.isCheckable

            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Only capture nodes with meaningful size and text/desc or interactivity
            val isMeaningful = (text.isNotBlank() || desc.isNotBlank() || clickable || editable || checkable) &&
                    bounds.width() > 10 && bounds.height() > 10

            if (isMeaningful) {
                val cX = bounds.centerX()
                val cY = bounds.centerY()

                val parsed = ParsedNode(
                    index = nodeIndex,
                    text = text,
                    contentDescription = desc,
                    viewId = viewId,
                    className = className,
                    isClickable = clickable,
                    isEditable = editable,
                    isCheckable = checkable,
                    bounds = bounds,
                    centerX = cX,
                    centerY = cY
                )
                nodes.add(parsed)

                val nodeObj = JSONObject().apply {
                    put("id", nodeIndex)
                    if (text.isNotBlank()) put("text", text)
                    if (desc.isNotBlank()) put("desc", desc)
                    if (viewId.isNotBlank()) put("res_id", viewId.substringAfterLast("/"))
                    put("type", className)
                    put("clickable", clickable)
                    put("editable", editable)
                    put("center", JSONArray(listOf(cX, cY)))
                    put("bounds", JSONArray(listOf(bounds.left, bounds.top, bounds.right, bounds.bottom)))
                }
                jsonArray.put(nodeObj)
                nodeIndex++
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))
            }
        }

        try {
            traverse(root)
        } catch (e: Exception) {
            Log.e(TAG, "Error dumping accessibility hierarchy", e)
        }

        return nodes to jsonArray.toString(2)
    }
}
