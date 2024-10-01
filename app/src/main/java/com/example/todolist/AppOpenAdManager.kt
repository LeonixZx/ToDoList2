package com.example.todolist

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdManager(private val application: Application) : DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    private var lastAdLoadAttempt = 0L
    private val AD_LOAD_COOLDOWN = 15000L // 1 minute cooldown (60000L)
    private val NO_FILL_RETRY_DELAY = 150000L // 5 minutes (300000L)
    private var retryAttempt = 0
    private val MAX_RETRY_ATTEMPTS = 3

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun loadAd() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdLoadAttempt < AD_LOAD_COOLDOWN) {
            Log.d("AppOpenAdManager", "Skipping ad load due to cooldown")
            return
        }
        if (isLoadingAd || isAdAvailable()) {
            return
        }

        isLoadingAd = true
        lastAdLoadAttempt = currentTime
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            application,
            "ca-app-pub-2107817689571311/7310943642", // Ensure this is correct for App Open ads,   ca-app-pub-3940256099942544/3419835294 (Test Ads)

            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    retryAttempt = 0
                    Log.d("AppOpenAdManager", "Ad was loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    Log.e("AppOpenAdManager", "Ad failed to load. Error: ${loadAdError.message}")

                    when (loadAdError.code) {
                        AdRequest.ERROR_CODE_NO_FILL -> {
                            Log.d("AppOpenAdManager", "No fill error. Retrying in 5 minutes.")
                            retryLoadAdWithDelay(NO_FILL_RETRY_DELAY)
                        }
                        else -> {
                            Log.e("AppOpenAdManager", "Other error. Code: ${loadAdError.code}")
                            retryLoadAdWithDelay(AD_LOAD_COOLDOWN)
                        }
                    }
                }
            }
        )
    }

    private fun retryLoadAdWithDelay(delay: Long) {
        if (retryAttempt < MAX_RETRY_ATTEMPTS) {
            retryAttempt++
            Handler(Looper.getMainLooper()).postDelayed({
                loadAd()
            }, delay)
        } else {
            Log.d("AppOpenAdManager", "Max retry attempts reached. Stopping ad load attempts.")
            retryAttempt = 0
        }
    }

    fun showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable()) {
            Log.d("AppOpenAdManager", "Will show ad.")

            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AppOpenAdManager", "Failed to show ad: ${adError.message}")
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }

            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            currentActivity?.let { appOpenAd?.show(it) }
        } else {
            Log.d("AppOpenAdManager", "Can't show ad.")
            loadAd()
        }
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        showAdIfAvailable()
        Log.d("AppOpenAdManager", "onStart")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }
}