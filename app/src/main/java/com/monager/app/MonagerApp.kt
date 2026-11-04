package com.monager.app

import android.app.Application
import com.monager.app.di.AppContainer

class MonagerApp : Application() {
    // Lazy: nothing heavy runs in onCreate; the DB opens on first use.
    val container: AppContainer by lazy { AppContainer(this) }
}
