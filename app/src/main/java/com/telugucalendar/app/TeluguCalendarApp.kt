package com.telugucalendar.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeluguCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Google asks for the Mobile Ads SDK to be started off the main thread.
        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(this@TeluguCalendarApp) }
    }
}
