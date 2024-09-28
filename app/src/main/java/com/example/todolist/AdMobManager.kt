package com.example.todolist

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback


class AdMobManager(private val application: Application) {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false

    fun initialize() {
        Log.d("AdMobManager", "Initializing MobileAds")
        MobileAds.initialize(application) {
            Log.d("AdMobManager", "MobileAds initialized successfully")
            loadAd()
        }
    }

    private fun loadAd() {
        if (isLoadingAd || isAdAvailable()) {
            Log.d("AdMobManager", "Ad is loading or already available")
            return
        }

        isLoadingAd = true
        Log.d("AdMobManager", "Starting to load ad")
        val request = AdRequest.Builder().build()

        val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                Log.d("AdMobManager", "Ad loaded successfully")
                appOpenAd = ad
                isLoadingAd = false
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdMobManager", "Ad failed to load: ${loadAdError.message}")
                isLoadingAd = false
            }
        }

        AppOpenAd.load(
            application,
            "ca-app-pub-2107817689571311/9015709889",  // Test ad unit ID
            request,
            loadCallback
        )
    }

    fun showAdIfAvailable(activity: android.app.Activity) {
        if (isShowingAd) {
            Log.d("AdMobManager", "Ad is already showing")
            return
        }
        if (!isAdAvailable()) {
            Log.d("AdMobManager", "Ad not available, loading new ad")
            loadAd()
            return
        }
        Log.d("AdMobManager", "Showing ad")

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d("AdMobManager", "Ad dismissed")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e("AdMobManager", "Ad failed to show: ${adError.message}")
                appOpenAd = null
                isShowingAd = false
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d("AdMobManager", "Ad showed fullscreen content")
                isShowingAd = true
            }
        }

        appOpenAd?.show(activity)
    }

    private fun isAdAvailable(): Boolean = appOpenAd != null
}