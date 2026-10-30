package com.ecoalertum.android

import android.app.Application
import com.ecoalertum.android.core.data.AppContainer

/** Creates the [AppContainer] once for the whole app. Access it with `(application as EcoAlertApplication).container`. */
class EcoAlertApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
