package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.adlisteners.VideoListener

/**
 * Start.io ads (StartApp SDK). Name kept as AdMobManager so other files need no change.
 *
 *  • Animated themes, swipe tunes and the mini-game are locked on first install.
 *  • REWARDED video: tap a locked item -> watch the full video -> unlocked for 12 hours.
 *    If the video is not ready, or the user closes it early, nothing is unlocked (a toast says why).
 *  • INTERSTITIAL: shown when the user changes theme or tune while already unlocked (not a reward).
 *  • BANNER: shown at the top of the main screen once it loads.
 */
class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_admob_prefs", Context.MODE_PRIVATE)

    private var interstitialAd: StartAppAd? = null
    private var interstitialLoading = false
    private var rewardedAd: StartAppAd? = null
    private var rewardedLoading = false

    companion object {
        private const val TAG = "KeyAuraAds"
        private const val APP_ID = "207210854"

        // 12-hour unlock window
        const val UNLOCK_DURATION_MS = 12L * 60 * 60 * 1000

        private const val KEY_THEMES_UNLOCK = "themes_unlock_time"
        private const val KEY_TUNES_UNLOCK  = "tunes_unlock_time"
        private const val KEY_GAME_UNLOCK   = "game_unlock_time"
        private const val KEY_CONSENT       = "ads_personalized"

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

    // ── Unlock state ─────────────────────────────────────────────────────────

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
        Log.d(TAG, "Unlock granted: $type for 12 hours")
    }

    // ── Initialization ───────────────────────────────────────────────────────

    fun initialize(context: Context) {
        Log.d(TAG, "Initializing Start.io SDK")
        StartAppSDK.init(context, APP_ID, false)
        // Until the user answers the ads-consent question, ads are NON-personalized.
        applyConsent(context, storedConsent() ?: false)
        loadInterstitial()
        loadRewarded()
    }

    // ── Consent ──────────────────────────────────────────────────────────────

    fun needsConsentAnswer(): Boolean = !prefs.contains(KEY_CONSENT)

    private fun storedConsent(): Boolean? =
        if (prefs.contains(KEY_CONSENT)) prefs.getBoolean(KEY_CONSENT, false) else null

    fun saveConsent(context: Context, personalized: Boolean) {
        prefs.edit().putBoolean(KEY_CONSENT, personalized).apply()
        applyConsent(context, personalized)
    }

    private fun applyConsent(context: Context, personalized: Boolean) {
        StartAppSDK.setUserConsent(context, "pas", System.currentTimeMillis(), personalized)
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    /** Adds a Start.io banner into [container]. The container stays hidden if the banner fails. */
    fun attachBanner(activity: Activity, container: ViewGroup) {
        try {
            container.removeAllViews()
            val banner = Banner(activity)
            container.addView(
                banner,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            container.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Banner failed", e)
            container.visibility = View.GONE
        }
    }

    // ── Rewarded (unlock) ────────────────────────────────────────────────────

    private fun loadRewarded() {
        if (rewardedLoading || rewardedAd != null) return
        rewardedLoading = true
        val ad = StartAppAd(appContext)
        ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(a: com.startapp.sdk.adsbase.Ad) {
                Log.d(TAG, "Rewarded loaded")
                rewardedAd = ad
                rewardedLoading = false
            }
            override fun onFailedToReceiveAd(a: com.startapp.sdk.adsbase.Ad?) {
                Log.e(TAG, "Rewarded load failed")
                rewardedAd = null
                rewardedLoading = false
            }
        })
    }

    /**
     * Unlocks [type] for 12 hours ONLY after the user watches the rewarded video to the end.
     * If the video is not ready or is closed early, nothing is unlocked and a message is shown.
     */
    fun unlockWithRewarded(activity: Activity, type: RewardType, onComplete: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            toast(activity, "Ad abhi ready nahi hai. Thodi der baad dobara try karein.")
            return
        }
        var completed = false
        ad.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() { completed = true }
        })
        ad.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                rewardedAd = null
                loadRewarded()
                if (completed) {
                    grantUnlock(type)
                    activity.runOnUiThread { onComplete() }
                } else {
                    toast(activity, "Poori video dekhne par hi unlock hoga.")
                }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                rewardedAd = null
                loadRewarded()
                toast(activity, "Ad abhi dikhaya nahi ja saka. Dobara try karein.")
            }
        })
    }

    // ── Interstitial (theme / tune change while unlocked; not a reward) ──────

    private fun loadInterstitial() {
        if (interstitialLoading) return
        interstitialLoading = true
        val ad = StartAppAd(appContext)
        ad.loadAd(StartAppAd.AdMode.AUTOMATIC, object : AdEventListener {
            override fun onReceiveAd(a: com.startapp.sdk.adsbase.Ad) {
                interstitialAd = ad
                interstitialLoading = false
            }
            override fun onFailedToReceiveAd(a: com.startapp.sdk.adsbase.Ad?) {
                interstitialAd = null
                interstitialLoading = false
            }
        })
    }

    /** Shows an interstitial, then runs [onComplete]. If no ad is ready, runs [onComplete] straight away. */
    fun showInterstitialThen(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial()
            activity.runOnUiThread { onComplete() }
            return
        }
        ad.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                interstitialAd = null
                loadInterstitial()
                activity.runOnUiThread { onComplete() }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                interstitialAd = null
                loadInterstitial()
                activity.runOnUiThread { onComplete() }
            }
        })
    }

    private fun toast(activity: Activity, msg: String) {
        activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_LONG).show() }
    }
}
