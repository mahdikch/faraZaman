package net.osmtracker

import android.app.Application
import android.content.Context
import net.osmtracker.util.LocaleUtil

class MyApplication:Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(LocaleUtil.setLocale(base!!,"fa"))
    }
}