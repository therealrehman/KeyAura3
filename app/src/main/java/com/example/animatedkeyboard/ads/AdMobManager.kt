package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Centralized AdMob Rewarded Ads manager for KeyAura.
 *
 * - Test mode: uses Google test ad unit ID
 * - 12-hour unlock timers for THEMES, TUNES, GAME
 * - Pre-loads ad after init
 * - Race condition safe: queues show request if ad not ready
 */
class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_admob_prefs", Context.MODE_PRIVATE)

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var pendingShow: (() -> Unit)? = null

    companion object {
        private const val TAG = "AdMobManager"

        // Test ad unit ID — replace with real one when going production
        private const val AD_UNIT_ID = "ca-app-pub-6764009090264687/6880543585"

        const val UNLOCK_DURATION_MS = 6L * 60 * 60 * 1000

        private const val KEY_THEMES_UNLOCK = "themes_unlock_time"
        private const val KEY_TUNES_UNLOCK  = "tunes_unlock_time"
        private const val KEY_GAME_UNLOCK   = "game_unlock_time"

        @Volatile private var instance: AdMobManager? = null

        fun getInstance(context: Context): AdMobManager =
            instance ?: synchronized(this) {
                instance ?: AdMobManager(context.applicationContext).also { instance = it }
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
        Log.d(TAG, "✅ Unlock granted: $type for 12 hours")
    }

    fun grantMultipleUnlocks(vararg types: RewardType) {
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        for (type in types) editor.putLong(unlockKey(type), now)
        editor.apply()
        Log.d(TAG, "✅ Multi-unlock granted: ${types.toList()}")
    }

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        Log.d(TAG, "Initializing AdMob SDK")
        MobileAds.initialize(context.applicationContext) {
            Log.d(TAG, "✅ AdMob initialized")
            loadAd()
        }
    }

    // ── Load Ad ───────────────────────────────────────────────────────────────

    private fun loadAd() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        Log.d(TAG, "Loading rewarded ad — unit=$AD_UNIT_ID")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(appContext, AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✅ Rewarded ad LOADED")
                    rewardedAd = ad
                    isLoading = false
                    pendingShow?.invoke()
                    pendingShow = null
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "❌ Rewarded ad FAILED to load — ${error.message}")
                    rewardedAd = null
                    isLoading = false
                    pendingShow = null
                }
            })
    }

    // ── Show Rewarded Ad ──────────────────────────────────────────────────────

    fun showRewardedAd(
        activity: Activity,
        type: RewardType,
        onRewarded: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        Log.d(TAG, "showRewardedAd called — type=$type adReady=${rewardedAd != null}")

        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "Ad not ready — queuing request and loading")
            pendingShow = { showRewardedAd(activity, type, onRewarded, onFailed) }
            loadAd()
            activity.runOnUiThread {
                Toast.makeText(activity, "Ad is loading… please try again in a moment.", Toast.LENGTH_SHORT).show()
            }
            onFailed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                loadAd() // pre-load next
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ Ad SHOW FAILED — ${error.message}")
                rewardedAd = null
                loadAd()
                activity.runOnUiThread {
                    Toast.makeText(activity, "Ad failed. Try again later.", Toast.LENGTH_SHORT).show()
                    onFailed()
                }
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ Ad showing")
            }
        }

        ad.show(activity) { _ ->
            Log.d(TAG, "✅ Reward earned — type=$type")
            grantUnlock(type)
            activity.runOnUiThread { onRewarded() }
        }
    }
}
