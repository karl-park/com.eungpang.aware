package com.eungpang.applocker.presentation

import android.app.Application
import com.eungpang.applocker.presentation.service.TimeCheckerNotification

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        TimeCheckerNotification.init(this)
    }
}