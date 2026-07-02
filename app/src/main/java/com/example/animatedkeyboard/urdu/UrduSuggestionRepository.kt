package com.example.animatedkeyboard.urdu

import android.content.Context
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings
import org.json.JSONObject

class UrduSuggestionRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)

    private val phrases: Map<String, String> by lazy { loadPhrases() }
    private val words: Map<String, List<String>> by lazy { loadWords() }

    /** Exact whole-code shortcuts, e.g. "AOA" -> "السلام علیکم". Checked case-sensitively first. */
    fun phraseMatch(typed: String): String? = phrases[typed]

    /**
     * Best single Urdu spelling for a fully-typed roman word, applying any
     * locally-learned preference (a word the user picked before for this key)
     * ahead of the bundled ranking.
     */
    fun bestWordMatch(typedLower: String): String? {
        val candidates = words[typedLower] ?: return null
        val learned = settings.urduWordPreference(typedLower)
        return if (learned != null && candidates.contains(learned)) learned else candidates.firstOrNull()
    }

    /**
     * Ranked Urdu candidates for a roman prefix as the user is still typing —
     * powers the live suggestion strip. Exact match ranks first, then
     * starts-with matches, each internally ordered by learned preference.
     */
    fun candidatesForPrefix(prefixLower: String, max: Int = 3): List<String> {
        if (prefixLower.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        words[prefixLower]?.let { exact ->
            orderByPreference(prefixLower, exact).forEach { out.add(it) }
        }
        if (out.size < max) {
            for ((key, candidates) in words) {
                if (out.size >= max) break
                if (key == prefixLower || !key.startsWith(prefixLower)) continue
                orderByPreference(key, candidates).forEach {
                    if (out.size < max) out.add(it)
                }
            }
        }
        return out.take(max)
    }

    private fun orderByPreference(key: String, candidates: List<String>): List<String> {
        val learned = settings.urduWordPreference(key) ?: return candidates
        if (!candidates.contains(learned)) return candidates
        return listOf(learned) + candidates.filter { it != learned }
    }

    /** Records that the user picked [chosenUrduWord] for [romanKeyLower] — bumps it to front next time. */
    fun recordUsage(romanKeyLower: String, chosenUrduWord: String) {
        settings.setUrduWordPreference(romanKeyLower, chosenUrduWord)
    }

    private fun loadPhrases(): Map<String, String> {
        return try {
            val json = appContext.assets.open("urdu_dictionary.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val obj = root.optJSONObject("phrases") ?: return emptyMap()
            val out = HashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optString(k)
            }
            out
        } catch (e: Exception) {
            Log.e("UrduSuggestionRepo", "Failed to load phrases: ${e.message}")
            emptyMap()
        }
    }

    private fun loadWords(): Map<String, List<String>> {
        return try {
            val json = appContext.assets.open("urdu_dictionary.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(json)
            val obj = root.optJSONObject("words") ?: return emptyMap()
            val out = HashMap<String, List<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val arr = obj.optJSONArray(k) ?: continue
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) list.add(arr.getString(i))
                out[k] = list
            }
            out
        } catch (e: Exception) {
            Log.e("UrduSuggestionRepo", "Failed to load words: ${e.message}")
            emptyMap()
        }
    }

    companion object {
        @Volatile private var instance: UrduSuggestionRepository? = null
        fun getInstance(context: Context): UrduSuggestionRepository {
            return instance ?: synchronized(this) {
                instance ?: UrduSuggestionRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
