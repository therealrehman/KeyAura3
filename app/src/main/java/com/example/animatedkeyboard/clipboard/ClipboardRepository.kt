package com.example.animatedkeyboard.clipboard

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings
import org.json.JSONObject
import java.io.File

data class ClipboardEntry(
    val id: String,
    val type: String, // "text" or "image"
    val content: String, // the text itself, or an absolute local file path for images
    val pinned: Boolean,
    val timestamp: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("type", type); put("content", content)
        put("pinned", pinned); put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(o: JSONObject): ClipboardEntry? {
            return try {
                ClipboardEntry(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    content = o.getString("content"),
                    pinned = o.optBoolean("pinned", false),
                    timestamp = o.optLong("timestamp", 0L)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

class ClipboardRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)
    private val maxUnpinned = 50

    private val imagesDir: File by lazy {
        File(appContext.filesDir, "clipboard_images").apply { mkdirs() }
    }

    fun getAll(): List<ClipboardEntry> =
        settings.getClipboardEntriesRaw().mapNotNull { ClipboardEntry.fromJson(it) }
            .sortedByDescending { it.timestamp }

    /** Adds a copied text snippet, skipping an exact repeat of the most recent entry. */
    fun addText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val entries = getAll().toMutableList()
        if (entries.firstOrNull()?.let { it.type == "text" && it.content == trimmed } == true) return
        entries.add(0, ClipboardEntry(
            id = "c${System.currentTimeMillis()}",
            type = "text",
            content = trimmed,
            pinned = false,
            timestamp = System.currentTimeMillis()
        ))
        trimAndSave(entries)
    }

    /** Copies image bytes from [uri] into private app storage and records the local path. */
    fun addImage(uri: Uri, resolver: ContentResolver) {
        try {
            val fileName = "clip_${System.currentTimeMillis()}.png"
            val outFile = File(imagesDir, fileName)
            resolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return
            val entries = getAll().toMutableList()
            entries.add(0, ClipboardEntry(
                id = "c${System.currentTimeMillis()}",
                type = "image",
                content = outFile.absolutePath,
                pinned = false,
                timestamp = System.currentTimeMillis()
            ))
            trimAndSave(entries)
        } catch (e: Exception) {
            Log.w("ClipboardRepository", "Failed to save clipboard image: ${e.message}")
        }
    }

    fun togglePin(id: String) {
        val entries = getAll().toMutableList()
        val idx = entries.indexOfFirst { it.id == id }
        if (idx == -1) return
        entries[idx] = entries[idx].copy(pinned = !entries[idx].pinned)
        settings.setClipboardEntriesRaw(entries.map { it.toJson() })
    }

    fun remove(id: String) {
        val entries = getAll().toMutableList()
        val removed = entries.find { it.id == id }
        entries.removeAll { it.id == id }
        settings.setClipboardEntriesRaw(entries.map { it.toJson() })
        // Clean up the backing image file so we don't leak private storage.
        if (removed?.type == "image") {
            try { File(removed.content).delete() } catch (_: Exception) {}
        }
    }

    private fun trimAndSave(entries: MutableList<ClipboardEntry>) {
        val pinned = entries.filter { it.pinned }
        val unpinned = entries.filter { !it.pinned }
        val keptUnpinned = unpinned.sortedByDescending { it.timestamp }.take(maxUnpinned)
        val evicted = unpinned.sortedByDescending { it.timestamp }.drop(maxUnpinned)
        // Clean up image files for entries that fell off the 50-item cap.
        for (e in evicted) {
            if (e.type == "image") {
                try { File(e.content).delete() } catch (_: Exception) {}
            }
        }
        val merged = (pinned + keptUnpinned).sortedByDescending { it.timestamp }
        settings.setClipboardEntriesRaw(merged.map { it.toJson() })
    }

    companion object {
        @Volatile private var instance: ClipboardRepository? = null
        fun getInstance(context: Context): ClipboardRepository {
            return instance ?: synchronized(this) {
                instance ?: ClipboardRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
