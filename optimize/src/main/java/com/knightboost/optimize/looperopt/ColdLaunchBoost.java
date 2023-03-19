package com.knightboost.optimize.looperopt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.Choreographer;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.knightboost.messageobserver.MessageObserver;
import com.knightboost.messageobserver.MessageObserverManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ColdLaunchBoost {

    private MessageOptHandler messageOptHandler;
    private Field field_mHandlerOfViewRootImpl;
    private Handler handlerOfViewRoot;
    private Field field_nextMessage;

    interface LAUNCH_STATE {
        int INIT = 0;
        int ACTIVITY = 1;
        int ON_MEASURE = 2;
        int WINDOW_FOCUS_CHANGE = 3;
        int ON_DRAW = 4;
    }

    private Class<?> class_ActivityThread;
    private Class<?> class_Choreographer;
    private Class<?> class_ViewRootImpl;
    private Class<?> class_Handler;
    private Class<?> class_MessageQueue;
    private Class<?> class_Message;
    private Handler mh;
    private Choreographer choreographer;
    private Handler choreographerHandler;
    private MessageQueue mhHandlerMessageQueue;
    private Field filed_mMessages;
    private Field field_next;

    private boolean isBootFinish = false;
    private boolean enable = false;

    private int curWatchingState = WatchingState.STATE_INIT;

    public static interface WatchingState {
        int STATE_INIT = 0;
        int STATE_WATCHING_START_MAIN_ACTIVITY = 1;
        int STATE_WATCHING_DO_FRAME = 2;
        int STATE_WINDOW_FOCUS_CHANGE = 3;
    }

    private static ColdLaunchBoost instance = null;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ColdLaunchBoost(context);
        }
    }

    @NonNull
    public static ColdLaunchBoost getInstance() {
        return instance;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
        if (!enable) {
            MessageObserverManager.getMain().removeMessageObserver(messageOptHandler);
        }else {
            MessageObserverManager.getMain().addMessageObserver(messageOptHandler);
        }
    }

    private ColdLaunchBoost(Context context) {
        //todo 前面加的操作
        try {
            try {
                choreographer = Choreographer.getInstance();
            } catch (Exception e) {
                throw e;
            }
            class_Handler = Class.forName("android.os.Handler");
            class_MessageQueue = Class.forName("android.os.MessageQueue");
            filed_mMessages = class_MessageQueue.getDeclaredField("mMessages");
            filed_mMessages.setAccessible(true);
            field_nextMessage = Message.class.getDeclaredField("next");
            field_nextMessage.setAccessible(true);

            class_Message = Class.forName("android.os.Message");
            field_next = class_Message.getDeclaredField("next");
            field_next.setAccessible(true);
            class_ActivityThread = Class.forName("android.app.ActivityThread");
            class_Choreographer = Class.forName("android.view.Choreographer");
            class_ViewRootImpl = Class.forName("android.view.ViewRootImpl");

            field_mHandlerOfViewRootImpl = class_ViewRootImpl.getDeclaredField("mHandler");
            field_mHandlerOfViewRootImpl.setAccessible(true);

            mh = reflectGetmH();
            choreographerHandler = getChoreographerHandler(choreographer);
            mhHandlerMessageQueue = getMessageQueue(mh);
            messageOptHandler = new MessageOptHandler(this);
            if (mh != null && mhHandlerMessageQueue != null && choreographerHandler != null) {
                this.curWatchingState = LAUNCH_STATE.INIT;
                MessageObserverManager.getMain().addMessageObserver(messageOptHandler);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurWatchingState() {
        return curWatchingState;
    }

    public void setCurWatchingState(int watchState) {
        this.curWatchingState = watchState;
    }

    public Handler getChoreographerHandler() {
        return choreographerHandler;
    }

    public MessageQueue getMessageQueue() {
        return mhHandlerMessageQueue;
    }

    public boolean isBootFinish() {
        return isBootFinish;
    }

    public void setBottFinish(boolean bootFinish) {
        this.isBootFinish = bootFinish;
        if (bootFinish) {
            MessageObserverManager.getMain().removeMessageObserver(messageOptHandler);
        }
    }

    private Handler reflectGetmH() throws Exception {
        Object currentActivityThread = class_ActivityThread.getDeclaredMethod("currentActivityThread")
                .invoke(null);
        Field mhField = class_ActivityThread.getDeclaredField("mH");
        mhField.setAccessible(true);
        return (Handler) mhField.get(currentActivityThread);
    }

    public Handler getmH() {
        return mh;
    }

    private Handler getChoreographerHandler(Choreographer choreographer) throws Exception {
        Field mHandlerField = class_Choreographer.getDeclaredField("mHandler");
        mHandlerField.setAccessible(true);

        return (Handler) mHandlerField.get(choreographer);
    }

    private MessageQueue getMessageQueue(Handler handler) throws Exception {
        Field mQueueField = this.class_Handler.getDeclaredField("mQueue");
        mQueueField.setAccessible(true);
        return (MessageQueue) mQueueField.get(handler);
    }

    private Message nextMessage(Message message) {
        try {
            Message next = (Message) field_next.get(message);
            return next;
        } catch (IllegalAccessException e) {
            //report it
            return null;
        }

    }

    @SuppressLint("DiscouragedPrivateApi")
    public boolean setMessageNext(Message target, Message nextMessage) {
        try {
            field_nextMessage.set(target, nextMessage);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private Message hasMessage(Handler handler, Message head, int what) {
        Message cur = head;
        while (cur != null) {
            if (cur.what == what && cur.getTarget() == handler) {
                return cur;
            }
            cur = nextMessage(cur);
        }
        return null;

    }

    public boolean upgradeMessagePriority(Handler handler, MessageQueue messageQueue,
                                          TargetMessageChecker targetMessageChecker) {
        synchronized (messageQueue) {
            try {
                Message message = (Message) filed_mMessages.get(messageQueue);
                Message preMessage = null;
                while (message != null) {
                    if (targetMessageChecker.isTargetMessage(message)) {
                        // 拷贝消息
                        Message copy = Message.obtain(message);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            if (message.isAsynchronous()) {
                                copy.setAsynchronous(true);
                            }
                        }
                        if (preMessage != null) { //如果已经在队列首部了，则不需要优化
                            //当前消息的下一个消息
                            Message next = nextMessage(message);
                            setMessageNext(preMessage, next);
                            handler.sendMessageAtFrontOfQueue(copy);
                            return true;
                        }
                        return false;
                    }
                    preMessage = message;
                    message = nextMessage(message);
                }
            } catch (Exception e) {
                //todo report
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean removeSyncBarrier(MessageQueue messageQueue, int token) {
        try {
            Method removeSyncBarrier = class_MessageQueue.getDeclaredMethod("removeSyncBarrier", int.class);
            removeSyncBarrier.setAccessible(true);
            removeSyncBarrier.invoke(messageQueue, token);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 移动消息屏障至队首
     *
     * @param messageQueue
     * @param handler
     * @return
     */
    public boolean upgradeBarrierMessagePriority(MessageQueue messageQueue, Handler handler) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false;
        }
        synchronized (messageQueue) {
            try {
                Message message = (Message) filed_mMessages.get(messageQueue);
                if (message != null && message.getTarget() == null) {
                    return false;
                }
                while (message != null) {
                    if (message.getTarget() == null) { // target 为null 说明为 屏障消息
                        Message cloneBarrier = Message.obtain(message);
                        removeSyncBarrier(messageQueue, message.arg1); //message.arg1 是屏障消息的 token, 后续的async消息会根据这个值进行屏障消息的移除
                        handler.sendMessageAtFrontOfQueue(cloneBarrier);
                        cloneBarrier.setTarget(null);
                        return true;
                    }
                    message = nextMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean upgradeAsyncMessagePriority(Handler handler, MessageQueue messageQueue, int what) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false;
        }
        //todo 版本判断
        synchronized (messageQueue) {
            try {
                Message message = (Message) filed_mMessages.get(messageQueue);
                Message barrierMessage = null;
                while (message != null) {
                    if (barrierMessage == null && message.getTarget() == null) {
                        barrierMessage = message;
                    }
                    if (message.what == what && message.getTarget() == handler && message.isAsynchronous()) {
                        if (barrierMessage == null) {
                            return false;
                        }
                        Message cloneBarrier = Message.obtain(barrierMessage);
                        removeSyncBarrier(messageQueue, barrierMessage.arg1);
                        handler.sendMessageAtFrontOfQueue(cloneBarrier);
                        cloneBarrier.setTarget(null);
                        return true;
                    }
                    message = nextMessage(message);
                }
            } catch (Exception e) {
                //todo report
                e.printStackTrace();
            }
        }

        return false;
    }

    public boolean upgradeMessagePriority(Handler handler, MessageQueue messageQueue, int what) {
        synchronized (messageQueue) {
            try {
                Message message = (Message) filed_mMessages.get(messageQueue);
                Message preMessage = null;
                while (message != null) {
                    if (message.what == what && message.getTarget() == handler) {
                        Message copy = Message.obtain(message);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            if (message.isAsynchronous()) {
                                copy.setAsynchronous(true);
                            }
                        }
                        // handler.removeMessages(what);
                        if (preMessage != null) {
                            Message next = nextMessage(message);
                            setMessageNext(preMessage, next);
                        }
                        handler.sendMessageAtFrontOfQueue(copy);
                        return true;
                    }
                    preMessage = message;
                    message = nextMessage(message);
                }
            } catch (Exception e) {
                //todo report
                e.printStackTrace();
            }
        }

        return false;
    }

    public Handler getHandlerOfViewRootImpl(ViewParent parent) {
        try {
            handlerOfViewRoot = (Handler) field_mHandlerOfViewRootImpl.get(parent);
            return handlerOfViewRoot;
        } catch (Exception e) {
            //should never happen
            return null;
        }
    }

    public Handler getCurHandlerOfViewRootImpl() {
        return handlerOfViewRoot;
    }

    public void printMessages() {
        printMessages(mhHandlerMessageQueue);
    }

    public void printMessages(MessageQueue messageQueue) {
        try {
            Message msg = (Message) filed_mMessages.get(messageQueue);
            StringBuilder sb = new StringBuilder();
            sb.append("messages: ");
            while (msg != null) {
                sb.append("{");
                sb.append("handler= ").append(msg.getTarget())
                        .append(", what=").append(msg.what)
                        .append(" }");
                msg = nextMessage(msg);
            }
            log(sb.toString());

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void log(String msg) {
        Log.e("MainLooperBoost", msg);
    }



}
