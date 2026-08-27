package com.altomedia.sawargi

import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds

/**
 * SAWARGI - ALTOMEDIA
 * Base application class.
 */
class SawargiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Google Mobile Ads SDK with the App ID from strings.xml
        MobileAds.initialize(this) { }
    }

    companion object {
        lateinit var instance: SawargiApplication
            private set

        fun appContext(): Context = instance.applicationContext
    }
}