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
import android.view.Gravity
import com.huawei.hms.ads.AdListener
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.BannerAdSize
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.InterstitialAd
import com.huawei.hms.ads.NonPersonalizedAd
import com.huawei.hms.ads.banner.BannerView
import com.huawei.hms.ads.reward.Reward
import com.huawei.hms.ads.reward.RewardAd
import com.huawei.hms.ads.reward.RewardAdLoadListener
import com.huawei.hms.ads.reward.RewardAdStatusListener

/**
 * Petal Ads (Huawei Ads Kit, ads-prime build so it also serves on Google Play / non-HMS devices).
 * Class name kept as AdMobManager so other files need no change.
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

    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialOnClosed: (() -> Unit)? = null
    private var rewardedAd: RewardAd? = null
    private var rewardedLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var requestPending = false   // one unlock request at a time

    companion object {
        private const val TAG = "KeyAuraAds"

        // ── Petal Ads (Petal Publisher Center) ───────────────────────────────
        private const val PETAL_APP_ID = "119728959"
        private const val BANNER_AD_UNIT_ID       = "e4m505w0jj"
        private const val REWARDED_AD_UNIT_ID     = "l52xitj2ct"
        private const val INTERSTITIAL_AD_UNIT_ID = "m4owdn4mhy"

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

    /** Safe to call many times: the Petal Ads SDK is initialised once. */
    @Synchronized
    private fun ensureInit(context: Context) {
        if (initialized) return
        Log.d(TAG, "Initializing Petal Ads SDK")
        try {
            HwAds.init(context.applicationContext, PETAL_APP_ID)
            initialized = true
            // Until the user answers the ads-consent question, ads are NON-personalized.
            applyConsent(storedConsent() ?: false)
        } catch (e: Exception) {
            Log.e(TAG, "Petal Ads init failed", e)
        }
    }

    fun initialize(context: Context) {
        ensureInit(context)
        if (!initialized) return
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
        ensureInit(context)
        applyConsent(personalized)
    }

    private fun applyConsent(personalized: Boolean) {
        try {
            val options = HwAds.getRequestOptions().toBuilder()
                .setNonPersonalizedAd(
                    if (personalized) NonPersonalizedAd.ALLOW_ALL else NonPersonalizedAd.ALLOW_NON_PERSONALIZED
                )
                .build()
            HwAds.setRequestOptions(options)
        } catch (e: Exception) {
            Log.e(TAG, "Applying ad consent failed", e)
        }
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    /** Adds a Petal Ads banner into [container]. The container is hidden again if the banner fails. */
    fun attachBanner(activity: Activity, container: ViewGroup) {
        try {
            ensureInit(activity)
            container.removeAllViews()
            val banner = BannerView(activity)
            banner.setAdId(BANNER_AD_UNIT_ID)
            banner.setBannerAdSize(BannerAdSize.BANNER_SIZE_320_50)
            banner.setAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner loaded")
                    container.visibility = View.VISIBLE
                }
                override fun onAdFailed(errorCode: Int) {
                    Log.e(TAG, "Banner failed, error=$errorCode")
                    container.visibility = View.GONE
                }
            })
            container.addView(
                banner,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL
                )
            )
            container.visibility = View.VISIBLE
            banner.loadAd(AdParam.Builder().build())
        } catch (e: Exception) {
            Log.e(TAG, "Banner failed", e)
            container.visibility = View.GONE
        }
    }

    // ── Rewarded (unlock) ────────────────────────────────────────────────────

    private fun loadRewarded() {
        if (!initialized) return
        if (rewardedLoading || rewardedAd?.isLoaded == true) return
        rewardedLoading = true
        val ad = RewardAd(appContext, REWARDED_AD_UNIT_ID)
        ad.loadAd(AdParam.Builder().build(), object : RewardAdLoadListener() {
            override fun onRewardedLoaded() {
                Log.d(TAG, "Rewarded loaded")
                rewardedAd = ad
                rewardedLoading = false
            }
            override fun onRewardAdFailedToLoad(errorCode: Int) {
                Log.e(TAG, "Rewarded load failed, error=$errorCode (no fill?) - retry in 15s")
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
        activity: Activity, ad: RewardAd, type: RewardType, onComplete: () -> Unit
    ) {
        rewardedAd = null
        var completed = false
        ad.show(activity, object : RewardAdStatusListener() {
            override fun onRewarded(reward: Reward) { completed = true }
            override fun onRewardAdClosed() {
                loadRewarded()
                if (completed) {
                    grantUnlock(type, UNLOCK_DURATION_MS)
                    activity.runOnUiThread { onComplete() }
                } else {
                    requestPending = false
                    toast(activity, "Poori video dekhne par 12 ghante ka unlock milega.")
                }
            }
            override fun onRewardAdFailedToShow(errorCode: Int) {
                Log.e(TAG, "Rewarded failed to show, error=$errorCode")
                loadRewarded()
                fallbackUnlock(activity, type, onComplete)
            }
        })
    }

    /** No rewarded ad could be shown: try an interstitial, else unlock for 1 hour. Offline never unlocks. */
    private fun fallbackUnlock(activity: Activity, type: RewardType, onComplete: () -> Unit) {
        if (!isOnline()) { requestPending = false; showNoInternet(activity); return }
        val ia = interstitialAd
        if (ia == null || !ia.isLoaded) {
            loadInterstitial()
            grantUnlock(type, FALLBACK_DURATION_MS)
            toast(activity, "Ad abhi available nahi. 1 ghante ke liye unlock hua.")
            activity.runOnUiThread { onComplete() }
            return
        }
        interstitialOnClosed = {
            grantUnlock(type, FALLBACK_DURATION_MS)
            toast(activity, "1 ghante ke liye unlock hua.")
            activity.runOnUiThread { onComplete() }
        }
        ia.show(activity)
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

    private fun interstitial(): InterstitialAd {
        interstitialAd?.let { return it }
        val ad = InterstitialAd(appContext)
        ad.setAdId(INTERSTITIAL_AD_UNIT_ID)
        ad.setAdListener(object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Interstitial loaded")
                interstitialLoading = false
            }
            override fun onAdFailed(errorCode: Int) {
                Log.e(TAG, "Interstitial load failed, error=$errorCode")
                interstitialLoading = false
            }
            override fun onAdClosed() {
                val next = interstitialOnClosed
                interstitialOnClosed = null
                loadInterstitial()
                next?.invoke()
            }
        })
        interstitialAd = ad
        return ad
    }

    private fun loadInterstitial() {
        if (!initialized) return
        val ad = interstitial()
        if (interstitialLoading || ad.isLoaded) return
        interstitialLoading = true
        ad.loadAd(AdParam.Builder().build())
    }

    /** Shows an interstitial, then runs [onComplete]. If no ad is ready, runs [onComplete] straight away. */
    fun showInterstitialThen(activity: Activity, onComplete: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || !ad.isLoaded) {
            loadInterstitial()
            activity.runOnUiThread { onComplete() }
            return
        }
        interstitialOnClosed = { activity.runOnUiThread { onComplete() } }
        ad.show(activity)
    }

    private fun toast(activity: Activity, msg: String) {
        activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_LONG).show() }
    }
}
