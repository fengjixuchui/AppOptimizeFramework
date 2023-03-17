package com.knightboost.messageobserver;

import android.os.Message;

import androidx.annotation.Nullable;

public interface MessageObserver {
    void onMessageDispatchStarting(String msg);

    /**
     *
     * @param msg  msg
     * @param message message
     */
    void onMessageDispatched(String msg,@Nullable Message message);
}
