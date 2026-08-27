package com.altomedia.sawargi.ads

import android.app.Activity
import android.content.Context
import com.altomedia.sawargi.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Manages full-screen AdMob ads (interstitial + rewarded).
 * Ads are loaded on request and shown when ready.
 */
object AdManager {

    fun loadInterstitial(
        activity: Activity?,
        onFail: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        val host = activity ?: return
        val adUnitId = host.getString(R.string.admob_interstitial)
        InterstitialAd.load(
            host,
            adUnitId,
            com.google.android.gms.ads.AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            onDismiss?.invoke()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            onFail?.invoke()
                        }
                    }
                    ad.show(host)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onFail?.invoke()
                }
            }
        )
    }

    fun loadRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onFail: (() -> Unit)? = null,
    ) {
        val host = activity ?: return
        val adUnitId = host.getString(R.string.admob_rewarded)
        RewardedAd.load(
            host,
            adUnitId,
            com.google.android.gms.ads.AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    ad.show(
                        host,
                        { rewardItem ->
                            // Grant the reward when the ad is finished watching
                            onReward()
                        }
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onFail?.invoke()
                }
            }
        )
    }
}