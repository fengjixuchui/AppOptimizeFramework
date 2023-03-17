package com.knightboost.optimize.looperopt;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.MessageQueue;
import android.view.Choreographer;

import java.lang.reflect.Field;

public class MainLooperBoost {

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
    private Handler mh = null;
    private Choreographer choreographer;
    private Handler choreographerHandler;
    private MessageQueue mhHandlerMessageQueue;
    private Field filed_mMessages;
    private Field field_next;

    private int curState;

    private void init(Context context) {
        //todo 前面加的操作
        try {
            choreographer = Choreographer.getInstance();
            class_Handler = Class.forName("android.os.Handler");
            class_MessageQueue = Class.forName("android.os.MessageQueue");
            filed_mMessages = class_MessageQueue.getDeclaredField("mMessages");
            filed_mMessages.setAccessible(true);

            class_Message = Class.forName("android.os.Message");
            field_next = class_Message.getDeclaredField("next");
            field_next.setAccessible(true);
            class_ActivityThread = Class.forName("android.app.ActivityThread");
            class_Choreographer = Class.forName("android.view.Choreographer");
            class_ViewRootImpl = Class.forName("android.view.ViewRootImpl");
            mh = getmH();
            choreographerHandler = getChoreographerHandler(choreographer);
            mhHandlerMessageQueue = getMessageQueue(mh);

            if (mh != null && mhHandlerMessageQueue != null && choreographerHandler != null) {
                this.curState = LAUNCH_STATE.INIT;

            }

        } catch (Exception e) {

        }
    }

    private Handler getmH() throws Exception {
        Object currentActivityThread = class_ActivityThread.getDeclaredMethod("currentActivityThread")
                .invoke(null);
        Field mhField = class_ActivityThread.getDeclaredField("mH");
        mhField.setAccessible(true);
        return (Handler) mhField.get(currentActivityThread);
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

    /**
     * 检查指定的MessageQueue 中是否包含 某个handler 发出的指定消息
     *
     * @param handler      发出消息的Handler
     * @param messageQueue messageQueue
     * @param what         消息对应的what值
     * @return true表示包含
     */
    private boolean containMessage(Handler handler, MessageQueue messageQueue, int what) {
        try {
            Message message = (Message) filed_mMessages.get(messageQueue);
            while (message != null) {
                if (message.what == what && message.getTarget() == handler) {
                    //copy it
                    Message obtain = Message.obtain(message);
                    //TODO 会不会存在移除太多的场景
                    handler.removeMessages(message.what);
                    handler.sendMessageAtFrontOfQueue(obtain);
                    return true;
                }
                message = nextMessage(message);
            }

            return false;
        } catch (Exception e) {
            return false;
        }

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

}
