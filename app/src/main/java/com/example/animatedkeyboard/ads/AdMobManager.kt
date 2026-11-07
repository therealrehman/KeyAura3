package com.example.animatedkeyboard.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
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
 * Start.io ads (StartApp SDK). Class name kept as AdMobManager so other files need no change.
 *
 *  • Animated themes, swipe tunes and the mini-game are locked on first install.
 *  • Tap a locked item:
 *      1. Internet off        -> "no internet" popup, nothing unlocked.
 *      2. Rewarded video OK   -> watch it to the end -> unlocked 12 hours.
 *      3. Rewarded not filled -> interstitial is shown; after it closes -> unlocked 1 hour.
 *      4. Nothing filled      -> unlocked 1 hour (only while online).
 *  • Interstitial also shows when the user changes theme/tune while already unlocked.
 *  • Banner is shown at the top of the main screen once it loads.
 */
class AdMobManager private constructor(private val appContext: Context) {

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("keyaura_admob_prefs", Context.MODE_PRIVATE)

    private var interstitialAd: StartAppAd? = null
    private var interstitialLoading = false
    private var rewardedAd: StartAppAd? = null
    private var rewardedLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var requestPending = false   // one unlock request at a time

    companion object {
        private const val TAG = "KeyAuraAds"
        private const val APP_ID = "207210854"

        const val UNLOCK_DURATION_MS = 12L * 60 * 60 * 1000   // full rewarded video
        const val FALLBACK_DURATION_MS = 1L * 60 * 60 * 1000  // no ad could be filled

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

    private fun durationOf(type: RewardType): Long =
        prefs.getLong(unlockKey(type) + "_dur", UNLOCK_DURATION_MS)

    fun isUnlocked(type: RewardType): Boolean {
        val t = prefs.getLong(unlockKey(type), 0L)
        return t > 0L && System.currentTimeMillis() - t < durationOf(type)
    }

    fun remainingMinutes(type: RewardType): Int {
        val t = prefs.getLong(unlockKey(type), 0L)
        if (t == 0L) return 0
        val remaining = durationOf(type) - (System.currentTimeMillis() - t)
        return if (remaining > 0) ((remaining + 59_999L) / 60_000L).toInt() else 0
    }

    private fun grantUnlock(type: RewardType, durationMs: Long) {
        prefs.edit()
            .putLong(unlockKey(type), System.currentTimeMillis())
            .putLong(unlockKey(type) + "_dur", durationMs)
            .apply()
        Log.d(TAG, "Unlock granted: $type for ${durationMs / 60_000} min")
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

    private fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
                Log.e(TAG, "Rewarded load failed (no fill?) - retry in 15s")
                rewardedAd = null
                rewardedLoading = false
                mainHandler.postDelayed({ loadRewarded() }, 15_000L)
            }
        })
    }

    fun unlockWithRewarded(activity: Activity, type: RewardType, onComplete: () -> Unit) {
        if (requestPending) return                       // already waiting: ignore extra taps silently
        if (!isOnline()) { showNoInternet(activity); return }
        requestPending = true
        val done: () -> Unit = { requestPending = false; onComplete() }
        val ad = rewardedAd
        if (ad != null) {
            showRewarded(activity, ad, type, done)
            return
        }
        loadRewarded()
        toast(activity, "Ad load ho raha hai, thoda wait karein...")
        waitForRewarded(activity, type, done, System.currentTimeMillis() + 8_000L)
    }

    private fun waitForRewarded(
        activity: Activity, type: RewardType, onComplete: () -> Unit, deadline: Long
    ) {
        val ad = rewardedAd
        if (ad != null) {
            showRewarded(activity, ad, type, onComplete)
            return
        }
        if (System.currentTimeMillis() > deadline) {
            fallbackUnlock(activity, type, onComplete)
            return
        }
        loadRewarded()
        mainHandler.postDelayed({ waitForRewarded(activity, type, onComplete, deadline) }, 400L)
    }

    private fun showRewarded(
        activity: Activity, ad: StartAppAd, type: RewardType, onComplete: () -> Unit
    ) {
        rewardedAd = null
        var completed = false
        ad.setVideoListener(object : VideoListener {
            override fun onVideoCompleted() { completed = true }
        })
        ad.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                loadRewarded()
                if (completed) {
                    grantUnlock(type, UNLOCK_DURATION_MS)
                    activity.runOnUiThread { onComplete() }
                } else {
                    requestPending = false
                    toast(activity, "Poori video dekhne par 12 ghante ka unlock milega.")
                }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                loadRewarded()
                fallbackUnlock(activity, type, onComplete)
            }
        })
    }

    /** No rewarded ad could be shown: try an interstitial, else unlock for 1 hour. Offline never unlocks. */
    private fun fallbackUnlock(activity: Activity, type: RewardType, onComplete: () -> Unit) {
        if (!isOnline()) { requestPending = false; showNoInternet(activity); return }
        val ia = interstitialAd
        if (ia == null) {
            loadInterstitial()
            grantUnlock(type, FALLBACK_DURATION_MS)
            toast(activity, "Ad abhi available nahi. 1 ghante ke liye unlock hua.")
            activity.runOnUiThread { onComplete() }
            return
        }
        interstitialAd = null
        ia.showAd(object : AdDisplayListener {
            override fun adHidden(a: com.startapp.sdk.adsbase.Ad?) {
                loadInterstitial()
                grantUnlock(type, FALLBACK_DURATION_MS)
                toast(activity, "1 ghante ke liye unlock hua.")
                activity.runOnUiThread { onComplete() }
            }
            override fun adDisplayed(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adClicked(a: com.startapp.sdk.adsbase.Ad?) {}
            override fun adNotDisplayed(a: com.startapp.sdk.adsbase.Ad?) {
                loadInterstitial()
                grantUnlock(type, FALLBACK_DURATION_MS)
                activity.runOnUiThread { onComplete() }
            }
        })
    }

    private fun showNoInternet(activity: Activity) {
        activity.runOnUiThread {
            android.app.AlertDialog.Builder(activity)
                .setTitle("Internet nahi hai")
                .setMessage("Unlock ke liye internet connect karke dobara try karein.")
                .setPositiveButton("OK", null)
                .show()
        }
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
