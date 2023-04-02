package com.knightboost.messageobserver

import android.annotation.SuppressLint
import android.os.*
import android.util.Log
import android.util.Printer
import com.knightboost.looper.free.LooperMessageObserver
import com.knightboost.looper.free.LooperObserverUtil

class MessageObserverManager public constructor(private val looper: Looper) {

    private val messageObserverHub = MessageObserverHub()


    @SuppressLint("SoonBlockedPrivateApi") fun printTransactionMessage(message: Message){
        if (message.what == 159){
            try {
                val class_ClientTransaction = Class.forName("android.app.servertransaction.ClientTransaction")
                val field_mLifecycleStateRequest = class_ClientTransaction.getDeclaredField("mLifecycleStateRequest")
                field_mLifecycleStateRequest.setAccessible(true)
                val class_ActivityLifecycleItem = Class.forName("android.app.servertransaction.ActivityLifecycleItem")
                val method_getTargetState = class_ActivityLifecycleItem.getDeclaredMethod("getTargetState")
                 method_getTargetState.setAccessible(true)
                var obj = message.obj
                if (obj!=null){
                    var request = field_mLifecycleStateRequest.get(obj)
                    if (request!=null){
                        var state = method_getTargetState.invoke(request)
                        var transactionName = "${state}";
                        if (state == 3){
                            transactionName ="resume Activity"
                        }else if (state ==4){
                            transactionName ="pause Activity"
                        }
                        else if (state ==5){
                            transactionName ="stop Activity"
                        } else if (state ==6){
                            transactionName ="destroy Activity"
                        }
                        Log.e("MainLooperBoost",
                            "msg dispatched [${transactionName}]:${message.target}  msg what = ${message.what},")

                    }

                }

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }

    }

    companion object {
        private var isReflectLoggingError = false
        private var mainMessageObserverManager = MessageObserverManager(Looper.getMainLooper())

        @JvmStatic
        fun getMain(): MessageObserverManager {
            return mainMessageObserverManager
        }

    }
    init {
        if (Build.VERSION.SDK_INT >= 29) {
            LooperObserverUtil.setObserver(object : LooperMessageObserver {
                override fun messageDispatchStarting(token: Any?): Any? {
                    if (Thread.currentThread() == looper.thread) {
                        messageObserverHub.messageDispatchStarting(
                            (">>>>> Dispatching to null null: 0")
                        )
                    }
                    return token
                }

                override fun messageDispatched(token: Any?, msg: Message?) {
                    if (Thread.currentThread() == looper.thread) {
                        if (msg!=null){
                            if (msg.what == 159){
                                printTransactionMessage(msg)

                            }else{
                                Log.d("MainLooperBoost",
                                    "msg dispatched:${msg.target} msg what = ${msg.what}," +
                                            " callback =${msg.callback}, " +
                                            "obj = ${msg.obj} "+
                                            "isAsynchronous = ${msg.isAsynchronous}")
                            }

                        }
                        messageObserverHub.messageDispatched("<<<<< Finished to null null" , msg)
                    }
                }

                override fun dispatchingThrewException(token: Any?, msg: Message?, exception: Exception?) {
                }
            })
        } else {
            setPrinter(object : Printer {
                override fun println(x: String?) {
                    val message = x;
                    if (message == null) {
                        return
                    }
                    if (message[0] == '>') {
                        messageObserverHub.messageDispatchStarting(x)
                    } else if (message[1] == '<') {
                        messageObserverHub.messageDispatched(x, null)
                    }

                }
            })
        }
    }



    private fun setPrinter(printer: Printer) {
        var originalPrinter: Printer? = null;
        try {
            if (!isReflectLoggingError) {
                val field_mLogging = Looper::class.java.getDeclaredField("mLogging")
                field_mLogging.isAccessible = true
                originalPrinter = field_mLogging.get(looper) as Printer?
            }
        } catch (e: java.lang.Exception) {
            isReflectLoggingError = true
        }
        looper.setMessageLogging(object : Printer {
            override fun println(x: String?) {
                if (originalPrinter != null) {
                    val oldPrinter = originalPrinter
                    oldPrinter.println(x)
                }
                printer.println(x)
            }
        })
    }

    public fun addMessageObserver(messageObserver: MessageObserver) {
        messageObserverHub.addMessageObserver(messageObserver)
    }

    public fun removeMessageObserver(messageObserver: MessageObserver) {
        messageObserverHub.removeMessageObserver(messageObserver)
    }
}