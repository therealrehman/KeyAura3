package com.example.animatedkeyboard.theme

import android.content.Context

class ThemeRepository private constructor(private val context: Context) {

    fun getAllThemes(): List<AnimationTheme> = AnimationTheme.valuesList

    fun getTheme(index: Int): AnimationTheme = AnimationTheme.fromIndex(index)

    fun getThemeByName(name: String): AnimationTheme? {
        return AnimationTheme.valuesList.find { it.displayName == name }
    }

    companion object {
        @Volatile
        private var instance: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return instance ?: synchronized(this) {
                instance ?: ThemeRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
