package com.example.animatedkeyboard.gesture

import android.content.Context
import android.util.Log

/**
 * A trie (prefix tree) for efficient word prediction.
 * Loads the bundled English words list into memory.
 */
class TrieDictionary(context: Context) {

    private val root = TrieNode()

    init {
        loadWords(context)
    }

    private fun loadWords(context: Context) {
        try {
            context.assets.open("english_words.txt").bufferedReader().useLines { lines ->
                lines.forEach { word ->
                    val trimmed = word.trim().lowercase()
                    if (trimmed.isNotEmpty()) insert(trimmed)
                }
            }
            Log.d("TrieDictionary", "Loaded dictionary with ${countWords(root)} words")
        } catch (e: Exception) {
            Log.e("TrieDictionary", "Failed to load words: ${e.message}")
        }
    }

    fun insert(word: String) {
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { TrieNode() }
        }
        node.isEndOfWord = true
    }

    fun search(prefix: String): List<String> {
        val result = mutableListOf<String>()
        var node = root
        for (ch in prefix) {
            node = node.children[ch] ?: return result
        }
        collectWords(node, prefix, result)
        return result
    }

    private fun collectWords(node: TrieNode, prefix: String, result: MutableList<String>) {
        if (result.size >= 5) return
        if (node.isEndOfWord) result.add(prefix)
        for ((char, child) in node.children) {
            collectWords(child, prefix + char, result)
        }
    }

    fun containsWord(word: String): Boolean {
        var node = root
        for (ch in word) {
            node = node.children[ch] ?: return false
        }
        return node.isEndOfWord
    }

    private fun countWords(node: TrieNode): Int {
        var count = if (node.isEndOfWord) 1 else 0
        for (child in node.children.values) {
            count += countWords(child)
        }
        return count
    }

    private data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        var isEndOfWord: Boolean = false
    )
}
