package ru.datamosh.radio

import android.app.Application

class RadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        LikesStore.init(this)
        PlayerWidgets.start(this)
    }
}
