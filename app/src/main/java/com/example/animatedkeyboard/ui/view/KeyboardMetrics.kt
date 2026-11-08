package com.example.animatedkeyboard.ui.view

import android.content.Context

/**
 * Single source of truth for how big the keyboard is, so the key view, the
 * emoji / clipboard / game panels and the IME window all agree.
 *
 *  - Portrait: full width, 35% of the screen height (unchanged from before).
 *  - Landscape: the keyboard keeps its PORTRAIT size (portrait width x portrait
 *    height), is centred horizontally and sits on the bottom edge. The height is
 *    capped so it can never swallow more than [LANDSCAPE_MAX_FRACTION] of the
 *    short screen side.
 */
object KeyboardMetrics {
    private const val PREFS = "keyaura_ime"
    private const val KEY_PORTRAIT_H = "kb_h_portrait_v2"

    const val PORTRAIT_FRACTION = 0.35f
    private const val LANDSCAPE_MAX_FRACTION = 0.70f

    @Volatile private var cachedPortraitH = 0

    fun isLandscape(context: Context): Boolean {
        val dm = context.resources.displayMetrics
        return dm.widthPixels > dm.heightPixels
    }

    /** Height of the keyboard (and of every panel that replaces it) in pixels. */
    fun heightPx(context: Context): Int {
        val dm = context.resources.displayMetrics
        val shortSide = minOf(dm.widthPixels, dm.heightPixels)
        val longSide = maxOf(dm.widthPixels, dm.heightPixels)

        if (!isLandscape(context)) {
            val h = (dm.heightPixels * PORTRAIT_FRACTION).toInt()
            rememberPortraitHeight(context, h)
            return h
        }

        // Landscape: reuse the real portrait height if we have seen it, otherwise estimate it.
        val portraitH = portraitHeight(context).takeIf { it > 0 } ?: (longSide * PORTRAIT_FRACTION).toInt()
        return minOf(portraitH, (shortSide * LANDSCAPE_MAX_FRACTION).toInt())
    }

    /** Width of the keyboard in landscape = the portrait width (the short side of the screen). */
    fun landscapeWidthPx(context: Context): Int {
        val dm = context.resources.displayMetrics
        return minOf(dm.widthPixels, dm.heightPixels)
    }

    private fun portraitHeight(context: Context): Int {
        if (cachedPortraitH > 0) return cachedPortraitH
        val saved = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_PORTRAIT_H, 0)
        cachedPortraitH = saved
        return saved
    }

    private fun rememberPortraitHeight(context: Context, h: Int) {
        if (h <= 0 || h == cachedPortraitH) return
        cachedPortraitH = h
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_PORTRAIT_H, h).apply()
    }
}
