package com.ballooner

import android.app.Application
import android.content.Context
import com.ballooner.data.settings.LocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BalloonerApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
