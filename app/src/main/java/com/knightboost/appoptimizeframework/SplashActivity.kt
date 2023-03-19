package com.knightboost.appoptimizeframework

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.knightboost.optimize.looperopt.ColdLaunchBoost
import com.knightboost.optimize.looperopt.ColdLaunchBoost.WatchingState
import java.time.Duration

class SplashActivity : AppCompatActivity() {
    val handler = Handler(Looper.getMainLooper())

    var once = true;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        Log.d("MainLooperBoost", "SplashActivity onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainLooperBoost", "SplashActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainLooperBoost", "SplashActivity onResume")
        Handler().postDelayed({
            //发送3秒的耗时消息到队列中
            //这里为了方便模拟，直接在主线程发送耗时任务,模拟耗时消息在 启动Activity消息之前的场景
            handler.post({
                Thread.sleep(3000)
                Log.e("MainLooperBoost", "任务处理3000ms")
            })
            val intent = Intent(this, MainActivity::class.java)
            Log.e("MainLooperBoost", "begin start to MainActivity")
            if (once){
                startActivity(intent)
                once =false
            }
        },1000)

    }

//    override fun onResume() {
//        super.onResume()
//        Log.d("MainLooperBoost", "SplashActivity onResume")
//
//        Handler().postDelayed({
//            //发送3秒的耗时消息到队列中
//            //这里为了方便模拟，直接在主线程发送耗时任务,模拟耗时消息在 启动Activity消息之前的场景
//            handler.post({
//                Thread.sleep(3000)
//                Log.e("MainLooperBoost", "任务处理3000ms")
//            })
//            val intent = Intent(this, MainActivity::class.java)
//            Log.e("MainLooperBoost", "begin start to MainActivity")
//            if (once){
//                startActivity(intent)
//                once =false
//            }
//            //标记需要检查 启动Activity的相关消息
//            ColdLaunchBoost.getInstance().setCurWatchingState(WatchingState.STATE_WATCHING_START_MAIN_ACTIVITY)
//        },1000)
//
//
//    }



    override fun onPause() {
        super.onPause()
        Log.d("MainLooperBoost", "SplashActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainLooperBoost", "SplashActivity onStop")
    }

    fun gotoMainPage() {
        //watch_activity
    }

}