package com.example.animatedkeyboard.english

import android.content.Context
import android.util.Log
import com.example.animatedkeyboard.settings.KeyboardSettings

/**
 * Bundled English word list — Google's 10,000 most common English words
 * (n-gram frequency analysis of Google's Trillion Word Corpus), NOT an
 * exhaustive dictionary. Earlier this used a full ~369k-word alphabetical
 * dictionary; scanning it alphabetically surfaced obscure/rare words (e.g.
 * typing "an" suggested "anabaena" before "and") that felt meaningless and,
 * being unusually long, often overflowed their suggestion chip and visually
 * overlapped the next one. This list is small enough for a simple linear
 * scan (no binary search needed) and — critically — kept in its ORIGINAL
 * frequency order, so the first matches found really are the most common
 * words with that prefix, not just the alphabetically-first ones.
 */
class EnglishSuggestionRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings.getInstance(appContext)
    private val wordsByFrequency: List<String> by lazy { loadFromAssets() }

    fun candidatesForPrefix(prefixLower: String, max: Int = 3): List<String> {
        if (prefixLower.isEmpty() || wordsByFrequency.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()

        val learned = settings.englishWordPreference(prefixLower)
        if (learned != null && learned.startsWith(prefixLower)) out.add(learned)

        for (w in wordsByFrequency) {
            if (out.size >= max) break
            if (w.startsWith(prefixLower)) out.add(w)
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
