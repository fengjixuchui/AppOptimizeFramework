package com.knightboost.appoptimizeframework

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.knightboost.optimize.looperopt.ColdLaunchBoost

/**
 * i 103-> watch_on_resume  2
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
//        ColdLaunchBoost.getInstance().log("before super onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainLooperBoost","MainActivity onCreate");

    }

    override fun onStart() {
        val decorView = window.decorView
        super.onStart()
        Log.d("MainLooperBoost","MainActivity onStart");
        var contentView = findViewById<View>(android.R.id.content)

    }

    override fun onResume() {
        //标记 接下来需要优化 frame消息
        Log.d("MainLooperBoost","MainActivity before super onResume");

        ColdLaunchBoost.getInstance().setCurWatchingState(ColdLaunchBoost.WatchingState.STATE_WATCHING_DO_FRAME)
        super.onResume()
        Log.d("MainLooperBoost","MainActivity onResume");
        // todo code is 3
        window.decorView.post {
            Log.e("Launch","decorView post finish")
            ColdLaunchBoost.getInstance().setBottFinish(true)
        }

    }



    private var windowFocusFirstChangeConsume = true;
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (windowFocusFirstChangeConsume){
            Log.e("MainLooperBoost","MainActivity onWindowFocusChanged");
            windowFocusFirstChangeConsume = false;
        }

    }


}