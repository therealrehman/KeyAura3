package com.example.animatedkeyboard.emoji

import android.content.Context
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings
import org.json.JSONArray

data class EmojiEntry(
    val slug: String,
    val character: String,
    val unicodeName: String,
    val group: String,
    val subGroup: String
)

/**
 * Loads the bundled offline emoji dataset (app/src/main/assets/emojis.json).
 * The dataset was built locally from Unicode CLDR data using the same field
 * shape as emoji-api.com's response, so the app never needs to call the
 * network or embed an API key inside the public repo.
 */
class EmojiRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)

    private val allEmojis: List<EmojiEntry> by lazy { loadFromAssets() }

    val groups: List<String> by lazy {
        // Fixed, sensible tab order rather than JSON encounter-order.
        val preferredOrder = listOf(
            "Smileys & Emotion", "People & Body", "Animals & Nature",
            "Food & Drink", "Travel & Places", "Activities",
            "Objects", "Symbols", "Flags"
        )
        val present = allEmojis.map { it.group }.toSet()
        preferredOrder.filter { it in present }
    }

    fun emojisForGroup(group: String): List<EmojiEntry> =
        allEmojis.filter { it.group == group }

    fun search(query: String): List<EmojiEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return allEmojis.filter {
            it.unicodeName.lowercase().contains(q) || it.slug.contains(q)
        }
    }

    fun recents(): List<EmojiEntry> {
        val chars = settings.recentEmojis()
        if (chars.isEmpty()) return emptyList()
        val byChar = allEmojis.associateBy { it.character }
        // Preserve most-recent-first order from settings; skip any that
        // somehow aren't in the dataset rather than crashing.
        return chars.mapNotNull { byChar[it] }
    }

    fun recordUsage(entry: EmojiEntry) {
        settings.addRecentEmoji(entry.character)
    }

    private fun loadFromAssets(): List<EmojiEntry> {
        return try {
            val json = appContext.assets.open("emojis.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONArray(json)
            val out = ArrayList<EmojiEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    EmojiEntry(
                        slug = o.optString("slug"),
                        character = o.optString("character"),
                        unicodeName = o.optString("unicodeName"),
                        group = o.optString("group"),
                        subGroup = o.optString("subGroup")
                    )
                )
            }
            out
        } catch (e: Exception) {
            Log.e("EmojiRepository", "Failed to load bundled emojis.json: ${e.message}")
            emptyList()
        }
    }

    companion object {
        @Volatile
        private var instance: EmojiRepository? = null

        fun getInstance(context: Context): EmojiRepository {
            return instance ?: synchronized(this) {
                instance ?: EmojiRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
