package com.example.core.shellmind

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * ShellMind Workspace & File Manager for on-device coding.
 * Provides safe file system operations, workspace directory trees, diff generation, and file editing.
 */
class ShellMindWorkspaceManager private constructor(private val context: Context) {

    private var currentWorkspaceRoot: File = File(context.filesDir, "workspace").apply { mkdirs() }

    companion object {
        private const val TAG = "ShellMindWorkspace"

        @Volatile
        private var instance: ShellMindWorkspaceManager? = null

        fun getInstance(context: Context): ShellMindWorkspaceManager {
            return instance ?: synchronized(this) {
                instance ?: ShellMindWorkspaceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getWorkspacePath(): String = currentWorkspaceRoot.absolutePath

    fun setWorkspaceRoot(directoryPath: String): Boolean {
        val dir = File(directoryPath)
        return if (dir.exists() && dir.isDirectory) {
            currentWorkspaceRoot = dir
            true
        } else {
            false
        }
    }

    fun listFiles(relativePath: String = ""): List<WorkspaceFileItem> {
        val targetDir = if (relativePath.isBlank()) currentWorkspaceRoot else File(currentWorkspaceRoot, relativePath)
        if (!targetDir.exists() || !targetDir.isDirectory) return emptyList()

        val files = targetDir.listFiles() ?: return emptyList()
        return files.map { file ->
            WorkspaceFileItem(
                name = file.name,
                relativePath = file.relativeTo(currentWorkspaceRoot).path.replace('\\', '/'),
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun readFile(relativePath: String): String? {
        val file = File(currentWorkspaceRoot, relativePath)
        return try {
            if (file.exists() && file.isFile) {
                file.readText(Charsets.UTF_8)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file $relativePath", e)
            null
        }
    }

    fun writeFile(relativePath: String, content: String): Boolean {
        val file = File(currentWorkspaceRoot, relativePath)
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file $relativePath", e)
            false
        }
    }

    fun deleteFile(relativePath: String): Boolean {
        val file = File(currentWorkspaceRoot, relativePath)
        return try {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file $relativePath", e)
            false
        }
    }

    fun createDirectory(relativePath: String): Boolean {
        val dir = File(currentWorkspaceRoot, relativePath)
        return dir.mkdirs()
    }

    fun searchFiles(query: String): List<WorkspaceSearchResult> {
        val results = mutableListOf<WorkspaceSearchResult>()
        val q = query.lowercase()

        currentWorkspaceRoot.walkTopDown().maxDepth(6).forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                val rel = file.relativeTo(currentWorkspaceRoot).path.replace('\\', '/')
                if (file.name.lowercase().contains(q)) {
                    results.add(WorkspaceSearchResult(rel, 0, "[Filename match: ${file.name}]"))
                }
                try {
                    val lines = file.readLines(Charsets.UTF_8)
                    lines.forEachIndexed { idx, line ->
                        if (line.lowercase().contains(q) && results.size < 50) {
                            results.add(WorkspaceSearchResult(rel, idx + 1, line.trim()))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return results
    }
}

data class WorkspaceFileItem(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

data class WorkspaceSearchResult(
    val filePath: String,
    val lineNumber: Int,
    val snippet: String
)
