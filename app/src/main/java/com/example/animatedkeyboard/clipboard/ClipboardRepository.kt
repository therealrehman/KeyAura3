package com.example.animatedkeyboard.clipboard

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

data class ClipboardEntry(
    val id: String,
    val type: String,
    val content: String,
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
            } catch (e: Exception) { null }
        }
    }
}

class ClipboardRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)
    private val maxUnpinned = 50
    private val maxStoredChars = 4000

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var cache: List<ClipboardEntry>? = null

    private val imagesDir: File by lazy {
        File(appContext.filesDir, "clipboard_images").apply { mkdirs() }
    }

    fun getAll(): List<ClipboardEntry> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val loaded = settings.getClipboardEntriesRaw()
                .mapNotNull { ClipboardEntry.fromJson(it) }
                .sortedByDescending { it.timestamp }
            cache = loaded
            return loaded
        }
    }

    fun addText(text: String, onAdded: ((ClipboardEntry) -> Unit)? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        executor.execute {
            val capped = if (trimmed.length > maxStoredChars) trimmed.substring(0, maxStoredChars) else trimmed
            val entries = getAll().toMutableList()
            if (entries.firstOrNull()?.let { it.type == "text" && it.content == capped } == true) return@execute
            entries.removeAll { it.type == "text" && it.content == capped }
            val entry = ClipboardEntry(
                id = "c${System.currentTimeMillis()}",
                type = "text",
                content = capped,
                pinned = false,
                timestamp = System.currentTimeMillis()
            )
            entries.add(0, entry)
            trimAndSave(entries)
            onAdded?.let { cb -> mainHandler.post { cb(entry) } }
        }
    }

    fun addImage(uri: Uri, resolver: ContentResolver) {
        executor.execute {
            try {
                val fileName = "clip_${System.currentTimeMillis()}.png"
                val outFile = File(imagesDir, fileName)
                resolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                } ?: return@execute
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
    }

    fun togglePin(id: String) {
        executor.execute {
            val entries = getAll().toMutableList()
            val idx = entries.indexOfFirst { it.id == id }
            if (idx == -1) return@execute
            entries[idx] = entries[idx].copy(pinned = !entries[idx].pinned)
            saveAll(entries)
        }
    }

    fun remove(id: String) {
        executor.execute {
            val entries = getAll().toMutableList()
            val removed = entries.find { it.id == id }
            entries.removeAll { it.id == id }
            saveAll(entries)
            if (removed?.type == "image") {
                try { File(removed.content).delete() } catch (_: Exception) {}
            }
        }
    }

    private fun saveAll(entries: List<ClipboardEntry>) {
        val sorted = entries.sortedByDescending { it.timestamp }
        settings.setClipboardEntriesRaw(sorted.map { it.toJson() })
        cache = sorted
    }

    private fun trimAndSave(entries: MutableList<ClipboardEntry>) {
        val pinned = entries.filter { it.pinned }
        val unpinned = entries.filter { !it.pinned }
        val keptUnpinned = unpinned.sortedByDescending { it.timestamp }.take(maxUnpinned)
        val evicted = unpinned.sortedByDescending { it.timestamp }.drop(maxUnpinned)
        for (e in evicted) {
            if (e.type == "image") {
                try { File(e.content).delete() } catch (_: Exception) {}
            }
        }
        saveAll(pinned + keptUnpinned)
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
