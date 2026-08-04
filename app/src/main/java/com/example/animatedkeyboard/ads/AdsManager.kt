package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object AdsManager {

    private const val TAG = "AdsManager"
    private const val GAME_ID = "800110986"
    private const val TEST_MODE = false

    const val PLACEMENT_REWARDED = "Rewarded_Android"
    const val PLACEMENT_BANNER   = "Banner_Android"

    private const val UNLOCK_DURATION_MS = 12 * 60 * 60 * 1000L

    private const val PREF_NAME        = "ads_unlocks"
    private const val KEY_THEMES_UNTIL = "themes_unlocked_until"
    private const val KEY_TUNES_UNTIL  = "tunes_unlocked_until"
    private const val KEY_GAME_UNTIL   = "game_unlocked_until"

    private var initialized = false
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (initialized) return
        UnityAds.initialize(context.applicationContext, GAME_ID, TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    initialized = true
                    Log.d(TAG, "Unity Ads initialized")
                    UnityAds.load(PLACEMENT_REWARDED, loadListener)
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError, message: String
                ) { Log.e(TAG, "Init failed: $message") }
            })
    }

    private val loadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(p: String) { Log.d(TAG, "Loaded: $p") }
        override fun onUnityAdsFailedToLoad(p: String, e: UnityAds.UnityAdsLoadError, m: String) {
            Log.w(TAG, "Load failed $p: $m")
        }
    }

    fun showThemesAd(activity: Activity, onUnlocked: () -> Unit) =
        showRewarded(activity, KEY_THEMES_UNTIL, onUnlocked)

    fun showTunesAd(activity: Activity, onUnlocked: () -> Unit) =
        showRewarded(activity, KEY_TUNES_UNTIL, onUnlocked)

    fun showGameAd(activity: Activity, onUnlocked: () -> Unit) =
        showRewarded(activity, KEY_GAME_UNTIL, onUnlocked)

    private fun showRewarded(activity: Activity, prefKey: String, onUnlocked: () -> Unit) {
        if (!initialized) { init(activity); return }
        UnityAds.show(activity, PLACEMENT_REWARDED, UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(p: String) {}
                override fun onUnityAdsShowClick(p: String) {}
                override fun onUnityAdsShowComplete(p: String, state: UnityAds.UnityAdsShowCompletionState) {
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        prefs.edit().putLong(prefKey, System.currentTimeMillis() + UNLOCK_DURATION_MS).apply()
                        activity.runOnUiThread { onUnlocked() }
                    }
                    UnityAds.load(PLACEMENT_REWARDED, loadListener)
                }
                override fun onUnityAdsShowFailure(p: String, e: UnityAds.UnityAdsShowError, m: String) {
                    Log.e(TAG, "Show failed: $m")
                    UnityAds.load(PLACEMENT_REWARDED, loadListener)
                }
            })
    }

    fun isThemesUnlocked(): Boolean = isUnlocked(KEY_THEMES_UNTIL)
    fun isTunesUnlocked(): Boolean  = isUnlocked(KEY_TUNES_UNTIL)
    fun isGameUnlocked(): Boolean   = isUnlocked(KEY_GAME_UNTIL)

    private fun isUnlocked(key: String): Boolean {
        if (!::prefs.isInitialized) return false
        return System.currentTimeMillis() < prefs.getLong(key, 0L)
    }

    fun remainingMs(key: String): Long {
        if (!::prefs.isInitialized) return 0L
        return maxOf(0L, prefs.getLong(key, 0L) - System.currentTimeMillis())
    }

    fun themesRemainingMs()  = remainingMs(KEY_THEMES_UNTIL)
    fun tunesRemainingMs()   = remainingMs(KEY_TUNES_UNTIL)
    fun gameRemainingMs()    = remainingMs(KEY_GAME_UNTIL)

    fun formatRemaining(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        return "${h}h ${m}m"
    }
}
