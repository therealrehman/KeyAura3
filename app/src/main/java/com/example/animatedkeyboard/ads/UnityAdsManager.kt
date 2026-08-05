package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Centralized Unity Ads manager for KeyAura.
 *
 * Fix summary:
 *  - Full logging on every lifecycle event
 *  - Pre-loads rewarded ad after init completes (eliminates first-request delay)
 *  - Guards against show() before load() completes
 *  - Correct context usage (applicationContext only)
 *  - Race condition eliminated via isReady flag + callback queue
 */
class UnityAdsManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_ads_prefs", Context.MODE_PRIVATE)

    // Whether a rewarded ad is loaded and ready to show
    private var rewardedLoaded = false

    // If user taps "Watch Ad" before load finishes, we queue the request
    private var pendingShowRequest: (() -> Unit)? = null

    companion object {
        private const val TAG = "UnityAdsManager"

        const val GAME_ID            = "800110986"
        const val PLACEMENT_REWARDED = "Rewarded_Android"
        const val PLACEMENT_BANNER   = "Banner_Android"
        const val UNLOCK_DURATION_MS = 12L * 60 * 60 * 1000

        private const val KEY_THEMES_UNLOCK = "themes_unlock_time"
        private const val KEY_TUNES_UNLOCK  = "tunes_unlock_time"
        private const val KEY_GAME_UNLOCK   = "game_unlock_time"

        @Volatile private var instance: UnityAdsManager? = null

        fun getInstance(context: Context): UnityAdsManager =
            instance ?: synchronized(this) {
                instance ?: UnityAdsManager(context.applicationContext).also { instance = it }
            }
    }

    enum class RewardType { THEMES, TUNES, GAME }

    private fun unlockKey(type: RewardType) = when (type) {
        RewardType.THEMES -> KEY_THEMES_UNLOCK
        RewardType.TUNES  -> KEY_TUNES_UNLOCK
        RewardType.GAME   -> KEY_GAME_UNLOCK
    }

    // ── Unlock State ──────────────────────────────────────────────────────────

    fun isUnlocked(type: RewardType): Boolean {
        val t = prefs.getLong(unlockKey(type), 0L)
        return t > 0L && System.currentTimeMillis() - t < UNLOCK_DURATION_MS
    }

    fun remainingHours(type: RewardType): Int {
        val t = prefs.getLong(unlockKey(type), 0L)
        if (t == 0L) return 0
        val remaining = UNLOCK_DURATION_MS - (System.currentTimeMillis() - t)
        return if (remaining > 0) ((remaining + 3_599_999L) / 3_600_000L).toInt() else 0
    }

    private fun grantUnlock(type: RewardType) {
        prefs.edit().putLong(unlockKey(type), System.currentTimeMillis()).apply()
        Log.d(TAG, "Unlock granted: $type for 12 hours")
    }

    fun grantMultipleUnlocks(vararg types: RewardType) {
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        for (type in types) {
            editor.putLong(unlockKey(type), now)
            Log.d(TAG, "Multi-unlock granted: $type")
        }
        editor.apply()
    }

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        if (UnityAds.isInitialized) {
            Log.d(TAG, "SDK already initialized — skipping")
            // Pre-load in case it wasn't loaded yet
            if (!rewardedLoaded) preloadRewarded()
            return
        }

        Log.d(TAG, "Initializing Unity Ads — gameId=$GAME_ID testMode=true")

        UnityAds.initialize(
            context.applicationContext,
            GAME_ID,
            true, // testMode — set false for production
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d(TAG, "✅ Unity Ads initialized successfully")
                    preloadRewarded()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    Log.e(TAG, "❌ Unity Ads init FAILED — error=$error message=$message")
                }
            }
        )
    }

    // ── Pre-load Rewarded Ad ──────────────────────────────────────────────────

    private fun preloadRewarded() {
        Log.d(TAG, "Pre-loading rewarded ad — placement=$PLACEMENT_REWARDED")
        rewardedLoaded = false

        UnityAds.load(PLACEMENT_REWARDED, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d(TAG, "✅ Rewarded ad LOADED — placement=$placementId")
                rewardedLoaded = true
                // If someone was waiting, fire their request now
                pendingShowRequest?.invoke()
                pendingShowRequest = null
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e(TAG, "❌ Rewarded ad FAILED to load — placement=$placementId error=$error message=$message")
                rewardedLoaded = false
                pendingShowRequest = null
            }
        })
    }

    // ── Rewarded Ad ───────────────────────────────────────────────────────────

    fun showRewardedAd(
        activity: Activity,
        type: RewardType,
        onRewarded: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        Log.d(TAG, "showRewardedAd called — type=$type initialized=${UnityAds.isInitialized} loaded=$rewardedLoaded")

        if (!UnityAds.isInitialized) {
            Log.w(TAG, "SDK not initialized — calling initialize() and queuing request")
            initialize(activity)
            pendingShowRequest = { doShowRewarded(activity, type, onRewarded, onFailed) }
            activity.runOnUiThread {
                Toast.makeText(activity, "Ads loading… please try again in a moment.", Toast.LENGTH_SHORT).show()
            }
            onFailed()
            return
        }

        if (!rewardedLoaded) {
            Log.w(TAG, "Ad not loaded yet — loading now and queuing show")
            pendingShowRequest = { doShowRewarded(activity, type, onRewarded, onFailed) }
            preloadRewarded()
            activity.runOnUiThread {
                Toast.makeText(activity, "Ad is loading… please try again in a moment.", Toast.LENGTH_SHORT).show()
            }
            onFailed()
            return
        }

        doShowRewarded(activity, type, onRewarded, onFailed)
    }

    private fun doShowRewarded(
        activity: Activity,
        type: RewardType,
        onRewarded: () -> Unit,
        onFailed: () -> Unit
    ) {
        Log.d(TAG, "Showing rewarded ad — placement=$PLACEMENT_REWARDED type=$type")
        rewardedLoaded = false // mark as consumed

        UnityAds.show(
            activity,
            PLACEMENT_REWARDED,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d(TAG, "▶ Rewarded ad STARTED — placement=$placementId")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d(TAG, "👆 Rewarded ad CLICKED — placement=$placementId")
                }

                override fun onUnityAdsShowComplete(
                    placementId: String,
                    state: UnityAds.UnityAdsShowCompletionState
                ) {
                    Log.d(TAG, "⏹ Rewarded ad COMPLETE — placement=$placementId state=$state")
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        grantUnlock(type)
                        activity.runOnUiThread { onRewarded() }
                    } else {
                        Log.w(TAG, "Ad skipped — no reward granted")
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Watch the full ad to unlock.", Toast.LENGTH_SHORT).show()
                            onFailed()
                        }
                    }
                    // Pre-load next ad
                    preloadRewarded()
                }

                override fun onUnityAdsShowFailure(
                    placementId: String,
                    error: UnityAds.UnityAdsShowError,
                    message: String
                ) {
                    Log.e(TAG, "❌ Rewarded ad SHOW FAILED — placement=$placementId error=$error message=$message")
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Ad failed to play. Try again later.", Toast.LENGTH_SHORT).show()
                        onFailed()
                    }
                    preloadRewarded()
                }
            }
        )
    }

    // ── Banner Ad ─────────────────────────────────────────────────────────────

    fun loadBannerInto(activity: Activity, container: FrameLayout) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "loadBannerInto called before SDK init — skipping")
            return
        }

        Log.d(TAG, "Loading banner ad — placement=$PLACEMENT_BANNER")

        val banner = BannerView(
            activity,
            PLACEMENT_BANNER,
            UnityBannerSize.getDynamicSize(activity)
        )

        banner.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerAdView: BannerView) {
                Log.d(TAG, "✅ Banner LOADED")
                activity.runOnUiThread {
                    container.removeAllViews()
                    container.addView(bannerAdView)
                    container.visibility = View.VISIBLE
                }
            }

            override fun onBannerClick(bannerAdView: BannerView) {
                Log.d(TAG, "👆 Banner CLICKED")
            }

            override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                Log.e(TAG, "❌ Banner FAILED — code=${errorInfo.errorCode} msg=${errorInfo.errorMessage}")
                activity.runOnUiThread { container.visibility = View.GONE }
            }

            override fun onBannerShown(bannerAdView: BannerView) {
                Log.d(TAG, "✅ Banner SHOWN")
            }

            override fun onBannerLeftApplication(bannerView: BannerView) {}
        }

        banner.load()
    }
}
