package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

/**
 * Start.io Rewarded Ads manager for KeyAura.
 * Drop-in replacement for AdMobManager — same API, same RewardType enum.
 */
class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_admob_prefs", Context.MODE_PRIVATE)

    private var rewardedAd: StartAppAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "StartIOManager"
        private const val APP_ID = "207210854"

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
        Log.d(TAG, "Unlock granted: $type for 6 hours")
    }

    fun grantMultipleUnlocks(vararg types: RewardType) {
        val editor = prefs.edit()
        val now = System.currentTimeMillis()
        for (type in types) editor.putLong(unlockKey(type), now)
        editor.apply()
        Log.d(TAG, "Multi-unlock granted: ${types.toList()}")
    }

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Start.io SDK — appId=$APP_ID")
        StartAppSDK.init(context, APP_ID, false)
        StartAppSDK.setUserConsent(context, "pas", System.currentTimeMillis(), true)
        loadAd()
    }

    // ── Load Ad ───────────────────────────────────────────────────────────────

    private fun loadAd() {
        if (isLoading) return
        isLoading = true
        Log.d(TAG, "Loading Start.io rewarded ad")
        val ad = StartAppAd(appContext)
        ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(a: com.startapp.sdk.adsbase.Ad) {
                Log.d(TAG, "Rewarded ad loaded")
                rewardedAd = ad
                isLoading = false
            }
            override fun onFailedToReceiveAd(a: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "Failed to load rewarded ad")
                rewardedAd = null
                isLoading = false
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
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "Ad not ready — loading")
            activity.runOnUiThread {
                Toast.makeText(activity, "Ad is loading… please try again.", Toast.LENGTH_SHORT).show()
            }
            loadAd()
            onFailed()
            return
        }

        ad.showAd(object : AdDisplayListener {
            override fun onSavedOffline() {}
            override fun onFailedToDisplayAd() {
                Log.e(TAG, "Ad failed to display")
                rewardedAd = null
                loadAd()
                activity.runOnUiThread {
                    Toast.makeText(activity, "Ad failed. Try again later.", Toast.LENGTH_SHORT).show()
                }
                onFailed()
            }
            override fun onHideAd() {
                Log.d(TAG, "Ad hidden — reward granted")
                grantUnlock(type)
                rewardedAd = null
                loadAd()
                activity.runOnUiThread { onRewarded() }
            }
            override fun onDisplayAd() {
                Log.d(TAG, "Ad displaying")
            }
        })
    }
}
