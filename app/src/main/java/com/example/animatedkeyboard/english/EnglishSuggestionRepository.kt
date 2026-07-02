package com.example.animatedkeyboard.english

import android.content.Context
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings

/**
 * Bundled English word list (dwyl/english-words, ~369k words) for the same
 * suggestion strip used by Urdu mode, shown when Urdu typing is off. Words
 * are pre-sorted at build time (assets/english_words.txt) so lookups use
 * binary search instead of a linear scan — fast even at this size.
 */
class EnglishSuggestionRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)
    private val sortedWords: List<String> by lazy { loadFromAssets() }

    fun candidatesForPrefix(prefixLower: String, max: Int = 3): List<String> {
        if (prefixLower.isEmpty() || sortedWords.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()

        val learned = settings.englishWordPreference(prefixLower)
        if (learned != null && learned.startsWith(prefixLower)) out.add(learned)

        var lo = 0
        var hi = sortedWords.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (sortedWords[mid] < prefixLower) lo = mid + 1 else hi = mid
        }
        var i = lo
        while (i < sortedWords.size && out.size < max) {
            val w = sortedWords[i]
            if (!w.startsWith(prefixLower)) break
            out.add(w)
            i++
        }
        return out.take(max)
    }

    /** Records that the user picked [chosenWord] while typing [prefixLower] — ranks it first next time. */
    fun recordUsage(prefixLower: String, chosenWord: String) {
        settings.setEnglishWordPreference(prefixLower, chosenWord)
    }

    private fun loadFromAssets(): List<String> {
        return try {
            appContext.assets.open("english_words.txt").bufferedReader(Charsets.UTF_8).useLines { seq ->
                seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: Exception) {
            Log.e("EnglishSuggestionRepo", "Failed to load english_words.txt: ${e.message}")
            emptyList()
        }
    }

    companion object {
        @Volatile private var instance: EnglishSuggestionRepository? = null
        fun getInstance(context: Context): EnglishSuggestionRepository {
            return instance ?: synchronized(this) {
                instance ?: EnglishSuggestionRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
