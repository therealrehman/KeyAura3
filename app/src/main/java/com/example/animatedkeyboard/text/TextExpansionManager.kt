package com.example.animatedkeyboard.text

import android.content.Context
import com.example.animatedkeyboard.settings.KeyboardSettings

/**
 * Manages user-defined text shortcuts (e.g., "omw" -> "On my way!").
 */
class TextExpansionManager(context: Context) {

    private val settings = KeyboardSettings.getInstance(context)
    private val separators = setOf(' ', '\n', '.', ',', '!', '?', ';', ':')

    fun getShortcuts(): Map<String, String> = settings.textShortcuts

    fun addShortcut(shortcut: String, expansion: String) {
        settings.addTextShortcut(shortcut, expansion)
    }

    fun removeShortcut(shortcut: String) {
        settings.removeTextShortcut(shortcut)
    }

    /**
     * Checks if the given word is a shortcut and returns the expansion, or null.
     */
    fun expandWord(word: String): String? {
        val trimmed = word.trim()
        return if (trimmed.isNotEmpty()) settings.textShortcuts[trimmed] else null
    }

    /**
     * Processes a text buffer: replaces shortcuts with expansions where applicable.
     * Returns the modified buffer.
     */
    fun processText(buffer: String): String {
        val words = buffer.split(Regex("\\s+"))
        val expanded = words.map { word ->
            val expansion = expandWord(word)
            expansion ?: word
        }
        return expanded.joinToString(" ")
    }
}
