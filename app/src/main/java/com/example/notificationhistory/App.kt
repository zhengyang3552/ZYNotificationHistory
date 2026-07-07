package com.example.notificationhistory

import android.app.Application
import com.example.notificationhistory.data.AppDatabase

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
    }
}