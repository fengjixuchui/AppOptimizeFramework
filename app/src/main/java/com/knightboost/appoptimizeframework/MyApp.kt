package com.knightboost.appoptimizeframework

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.knightboost.optimize.looperopt.ColdLaunchBoost
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MyApp : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("")
        };
        Log.e("MainLooperBoost","Application attachBaseContext")

        ColdLaunchBoost.init(this)
        ColdLaunchBoost.getInstance().setEnable(true)
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("MainLooperBoost","Application onCreate")

    }
}