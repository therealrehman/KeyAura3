package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
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
 * Handles:
 *  - SDK initialization (Game ID: 800110986)
 *  - Rewarded ad loading + showing with a completion callback
 *  - 12-hour unlock timers for THEMES, TUNES, and GAME
 *  - Banner ad loading into a container FrameLayout
 */
class UnityAdsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyaura_ads_prefs", Context.MODE_PRIVATE)

    companion object {
        const val GAME_ID = "800110986"
        const val PLACEMENT_REWARDED = "Rewarded_Android"
        const val PLACEMENT_BANNER  = "Banner_Android"
        const val UNLOCK_DURATION_MS = 12L * 60 * 60 * 1000 // 12 hours in ms

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

    /** Returns how many hours are left (ceiling), or 0 if expired/not unlocked. */
    fun remainingHours(type: RewardType): Int {
        val t = prefs.getLong(unlockKey(type), 0L)
        if (t == 0L) return 0
        val remaining = UNLOCK_DURATION_MS - (System.currentTimeMillis() - t)
        return if (remaining > 0) ((remaining + 3_599_999L) / 3_600_000L).toInt() else 0
    }

    private fun grantUnlock(type: RewardType) {
        prefs.edit().putLong(unlockKey(type), System.currentTimeMillis()).apply()
    }

    /**
     * Grant unlock for multiple types at once — used when one rewarded ad should
     * unlock both THEMES and TUNES simultaneously (e.g. from the keyboard chip CTA).
     */
    fun grantMultipleUnlocks(vararg types: RewardType) {
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        for (type in types) editor.putLong(unlockKey(type), now)
        editor.apply()
    }

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        if (UnityAds.isInitialized) return
        UnityAds.initialize(
            context,
            GAME_ID,
            false, // testMode = false for production
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    // SDK ready — no extra action needed; load happens on demand
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    // Silent fail — ads won't show, app functions normally
                }
            }
        )
    }

    // ── Rewarded Ad ───────────────────────────────────────────────────────────

    /**
     * Load and immediately show a rewarded ad.
     *
     * [onRewarded] — called (on UI thread) only when the user watches the ad
     *               to completion and the unlock is granted.
     * [onFailed]   — called (on UI thread) when the ad is unavailable or
     *               the user skipped before completion.
     */
    fun showRewardedAd(
        activity: Activity,
        type: RewardType,
        onRewarded: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (!UnityAds.isInitialized) {
            initialize(activity)
            Toast.makeText(activity, "Ads loading… please try again in a moment.", Toast.LENGTH_SHORT).show()
            onFailed()
            return
        }

        UnityAds.load(PLACEMENT_REWARDED, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                UnityAds.show(
                    activity,
                    placementId,
                    UnityAdsShowOptions(),
                    object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(
                            placementId: String,
                            error: UnityAds.UnityAdsShowError,
                            message: String
                        ) {
                            activity.runOnUiThread {
                                Toast.makeText(activity, "Ad failed to play. Try again later.", Toast.LENGTH_SHORT).show()
                                onFailed()
                            }
                        }

                        override fun onUnityAdsShowStart(placementId: String) {}
                        override fun onUnityAdsShowClick(placementId: String) {}

                        override fun onUnityAdsShowComplete(
                            placementId: String,
                            state: UnityAds.UnityAdsShowCompletionState
                        ) {
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                grantUnlock(type)
                                activity.runOnUiThread { onRewarded() }
                            } else {
                                // User skipped — no reward
                                activity.runOnUiThread {
                                    Toast.makeText(activity, "Watch the full ad to unlock.", Toast.LENGTH_SHORT).show()
                                    onFailed()
                                }
                            }
                        }
                    }
                )
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "No ad available right now. Try again later.", Toast.LENGTH_SHORT).show()
                    onFailed()
                }
            }
        })
    }

    // ── Banner Ad ─────────────────────────────────────────────────────────────

    /**
     * Load a banner ad and, when ready, add it to [container].
     * Call this after Unity Ads is initialized.
     */
    fun loadBannerInto(activity: Activity, container: FrameLayout) {
        if (!UnityAds.isInitialized) return
        val banner = BannerView(
            activity,
            PLACEMENT_BANNER,
            UnityBannerSize.getDynamicSize(activity)
        )
        banner.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerAdView: BannerView) {
                activity.runOnUiThread {
                    container.removeAllViews()
                    container.addView(bannerAdView)
                    container.visibility = android.view.View.VISIBLE
                }
            }
            override fun onBannerClick(bannerAdView: BannerView) {}
            override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                activity.runOnUiThread { container.visibility = android.view.View.GONE }
            }
            override fun onBannerShown(bannerAdView: BannerView) {}
            override fun onBannerLeftApplication(bannerView: BannerView) {}
        }
        banner.load()
    }
}
