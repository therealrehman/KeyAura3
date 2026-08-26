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
 * Start.io Interstitial Ads — 3-hour unlock system.
 *
 * Strategy:
 *  • Animated themes + swipe tunes are locked on first install.
 *  • First tap → interstitial → 3hr unlock granted.
 *  • While unlocked, every theme/tune change → interstitial.
 *  • After 3hrs → keyboard falls back to static theme, shows unlock chip.
 *  • Chip tap → MainActivity opens → interstitial → previous animated theme re-applies.
 */
class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_admob_prefs", Context.MODE_PRIVATE)

    private var interstitialAd: StartAppAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "StartIOManager"
        private const val APP_ID = "207210854"

        // 3-hour unlock window — maximizes daily ad cycles (8 per day)
        const val UNLOCK_DURATION_MS = 3L * 60 * 60 * 1000

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

    fun remainingMinutes(type: RewardType): Int {
        val t = prefs.getLong(unlockKey(type), 0L)
        if (t == 0L) return 0
        val remaining = UNLOCK_DURATION_MS - (System.currentTimeMillis() - t)
        return if (remaining > 0) ((remaining + 59_999L) / 60_000L).toInt() else 0
    }

    private fun grantUnlock(type: RewardType) {
        prefs.edit().putLong(unlockKey(type), System.currentTimeMillis()).apply()
        Log.d(TAG, "Unlock granted: $type for 3 hours")
    }

    // ── Initialization ────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Start.io SDK 5.3.2")
        StartAppSDK.init(context, APP_ID, false)
        StartAppSDK.setUserConsent(context, "pas", System.currentTimeMillis(), true)
        loadAd()
    }

    // ── Load Interstitial ─────────────────────────────────────────────────────

    private fun loadAd() {
        if (isLoading) return
        isLoading = true
        val ad = StartAppAd(appContext)
        ad.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
            override fun onReceiveAd(a: com.startapp.sdk.adsbase.Ad) {
                Log.d(TAG, "Interstitial loaded ✅")
                interstitialAd = ad
                isLoading = false
            }
            override fun onFailedToReceiveAd(a: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "Interstitial load failed")
                interstitialAd = null
                isLoading = false
            }
        })
    }

    // ── FLOW 1: LOCKED → show ad → grant 3hr unlock → run action ─────────────

    fun unlockWithInterstitial(
        activity: Activity,
        type: RewardType,
        onComplete: () -> Unit
    ) {
        val ad = interstitialAd
        if (ad == null) {
            // Ad not ready — grant unlock anyway (don't block user)
            Log.w(TAG, "Ad not ready for unlock — granting unlock directly")
            loadAd()
            grantUnlock(type)
            activity.runOnUiThread { onComplete() }
            return
        }

        ad.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                Log.d(TAG, "Unlock ad closed — granting $type unlock")
                interstitialAd = null
                loadAd()
                grantUnlock(type)
                activity.runOnUiThread { onComplete() }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "Unlock ad not displayed — granting unlock directly")
                interstitialAd = null
                loadAd()
                grantUnlock(type)
                activity.runOnUiThread { onComplete() }
            }
        })
    }

    // ── FLOW 2: ALREADY UNLOCKED → show ad → run action (theme change) ────────

    fun showInterstitialThen(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "Ad not ready for mid-session — applying directly")
            loadAd()
            activity.runOnUiThread { onComplete() }
            return
        }

        ad.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                interstitialAd = null
                loadAd()
                activity.runOnUiThread { onComplete() }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                interstitialAd = null
                loadAd()
                activity.runOnUiThread { onComplete() }
            }
        })
    }
}
