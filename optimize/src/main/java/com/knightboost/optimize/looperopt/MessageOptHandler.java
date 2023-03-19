package com.knightboost.optimize.looperopt;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.knightboost.messageobserver.MessageObserver;
import com.knightboost.messageobserver.MessageObserverManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.knightboost.optimize.looperopt.MsgCode.VIEW_ROOT_HANDLER_MSG_WINDOW_FOCUS_CHANGED;

public class MessageOptHandler implements MessageObserver {

    private final ColdLaunchBoost mainLooperBoost;
    private  Method method_getTargetState;
    private  Field field_mLifecycleStateRequest;

    private boolean pauseActivityUpgardeSuccess;
    private boolean startActivityUpgradeSuccess;

    private static final int TARGET_STATE_ON_PAUSE = 4;
    private static final int TARGET_STATE_ON_RESUME = 3;


    @SuppressLint("SoonBlockedPrivateApi")
    public MessageOptHandler(ColdLaunchBoost mainLooperBoost) {
        this.mainLooperBoost = mainLooperBoost;
        try {
            Class<?> class_ClientTransaction = Class.forName("android.app.servertransaction.ClientTransaction");
            field_mLifecycleStateRequest = class_ClientTransaction.getDeclaredField("mLifecycleStateRequest");
            field_mLifecycleStateRequest.setAccessible(true);
            Class<?> class_ActivityLifecycleItem = Class.forName("android.app.servertransaction.ActivityLifecycleItem");
            method_getTargetState = class_ActivityLifecycleItem.getDeclaredMethod("getTargetState");
            method_getTargetState.setAccessible(true);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public void onMessageDispatchStarting(String msg) {

    }

    /**
     * 当前消息处理结束回调
     * @param msg  msg
     * @param message message
     */
    @Override
    public void onMessageDispatched(String msg, @Nullable Message message) {
        //进行消息队列检查
        int state = mainLooperBoost.getCurWatchingState();
        switch (state) {
            //1. 检查 startActivity相应消息
            case ColdLaunchBoost.WatchingState.STATE_WATCHING_START_MAIN_ACTIVITY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (!pauseActivityUpgardeSuccess){
                        pauseActivityUpgardeSuccess = mainLooperBoost.upgradeMessagePriority(mainLooperBoost.getmH(),
                                mainLooperBoost.getMessageQueue(), isPauseMessage);
                        //todo remove debug
                        if (pauseActivityUpgardeSuccess){
                            mainLooperBoost.log("!!! pause Activity upgrade success");
                        }
                        pauseActivityUpgardeSuccess =false;
                    }

                    if (!startActivityUpgradeSuccess){
                        startActivityUpgradeSuccess = mainLooperBoost.upgradeMessagePriority(mainLooperBoost.getmH(),
                                mainLooperBoost.getMessageQueue(), isResumeActivityMessage);
                        if (startActivityUpgradeSuccess){
                            mainLooperBoost.log("!!! startActivity upgrade Success");
                        }
                    }
                }else {

                    if (!pauseActivityUpgardeSuccess) {
                        pauseActivityUpgardeSuccess = mainLooperBoost.upgradeMessagePriority(mainLooperBoost.getmH(),
                                mainLooperBoost.getMessageQueue(), MsgCode.MH_MSG_PAUSE_ACTIVITY);
                        if (pauseActivityUpgardeSuccess){
                            mainLooperBoost.log("!!! pause Activity upgrade success");
                            //log pause_activity opt success
                        }
                    }
                    boolean startActivityOptSuccess = mainLooperBoost.upgradeMessagePriority(mainLooperBoost.getmH(),
                            mainLooperBoost.getMessageQueue(), MsgCode.MH_MSG_LAUNCH_ACTIVITY);
                    if (!startActivityOptSuccess) return;
                    mainLooperBoost.log("!!! startActivity upgrade Success");

                }
                break;
            //2. 帧绘制消息优化
            case ColdLaunchBoost.WatchingState.STATE_WATCHING_DO_FRAME:
                boolean success = mainLooperBoost.upgradeBarrierMessagePriority(
                        mainLooperBoost.getMessageQueue(),
                        mainLooperBoost.getChoreographerHandler()
                );
                if (success){
                    mainLooperBoost.log("!! MSG DO FRAME opt success");
                    new Handler(Looper.getMainLooper(), new Handler.Callback() {
                        @Override
                        public boolean handleMessage(@NonNull Message msg) {
                            mainLooperBoost.log("fake Message");
                            mainLooperBoost.setCurWatchingState(ColdLaunchBoost.WatchingState.STATE_WINDOW_FOCUS_CHANGE);
                            return true;
                        }
                    }).sendMessageAtFrontOfQueue(Message.obtain());

                    mainLooperBoost.setBottFinish(true);
                    // mainLooperBoost.setCurWatchingState(ColdLaunchBoost.WatchingState.STATE_WINDOW_FOCUS_CHANGE);
                }

                //mark
                break;
            case  ColdLaunchBoost.WatchingState.STATE_WINDOW_FOCUS_CHANGE:
                Handler handler = mainLooperBoost.getCurHandlerOfViewRootImpl();
                if (handler == null){
                    return;
                }
                boolean upgradeSuccess = mainLooperBoost.upgradeMessagePriority(handler,
                        mainLooperBoost.getMessageQueue(), VIEW_ROOT_HANDLER_MSG_WINDOW_FOCUS_CHANGED);
                if (!upgradeSuccess){
                    return;
                }
                mainLooperBoost.log("!! view root window focus change opt success");
                // log msg_window_focus_changed opt success
                //put record
                break;
            default:
                break;

        }
    }

    // @Override
    // public void onMessageDispatched(String msg, @Nullable Message message) {
    //     if (!this.mainLooperBoost.isBootFinish()){
    //         checkWatchingState();
    //     }else {
    //         //remove
    //         MessageObserverManager.getMain().removeMessageObserver(this);
    //         mainLooperBoost.stop();
    //
    //     }
    // }


    private final TargetMessageChecker isPauseMessage =new TargetMessageChecker() {
        @Override
        public boolean isTargetMessage(Message message) {
            if (message.what!=159){
                return false;
            }
            Object obj = message.obj;
            if (obj == null){
                return false;
            }
            try {
                Object lifecycleStateRequest = field_mLifecycleStateRequest.get(obj);
                if (lifecycleStateRequest == null){
                    return false;
                }
                Integer targetState = (Integer) method_getTargetState.invoke(lifecycleStateRequest);
                if (targetState!=null && targetState ==TARGET_STATE_ON_PAUSE){
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
            return false;
        }
    };

    private final TargetMessageChecker isResumeActivityMessage =new TargetMessageChecker() {
        @Override
        public boolean isTargetMessage(Message message) {
            if (message.what!=159){
                return false;
            }
            Object obj = message.obj;
            if (obj == null){
                return false;
            }
            try {
                Object lifecycleStateRequest = field_mLifecycleStateRequest.get(obj);
                if (lifecycleStateRequest == null){
                    return false;
                }
                Integer targetState = (Integer) method_getTargetState.invoke(lifecycleStateRequest);
                if (targetState!=null && targetState ==TARGET_STATE_ON_RESUME){
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }
    };


}
