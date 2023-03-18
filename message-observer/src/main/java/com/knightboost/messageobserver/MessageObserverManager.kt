package com.knightboost.messageobserver

import android.os.*
import android.util.Printer
import com.knightboost.looper.free.LooperMessageObserver
import com.knightboost.looper.free.LooperObserverUtil

class MessageObserverManager public constructor(private val looper: Looper) {

    private val messageObserverHub = MessageObserverHub()

    companion object {
        private var isReflectLoggingError = false
        private var mainMessageObserverManager = MessageObserverManager(Looper.getMainLooper())

        @JvmStatic
        fun getMain(): MessageObserverManager {
            return mainMessageObserverManager
        }

    }

    private fun init() {
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
}