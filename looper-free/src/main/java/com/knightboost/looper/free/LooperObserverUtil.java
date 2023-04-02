package com.knightboost.looper.free;

import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Field;

public class LooperObserverUtil {

    public static boolean setObserver(final LooperMessageObserver looperMessageObserver) {
        try {
            Field sObserverField = Looper.class.getDeclaredField("sObserver");
            sObserverField.setAccessible(true);
            final Looper.Observer oldObserver = (Looper.Observer) sObserverField.get(Looper.class);
            Looper.setObserver(new Looper.Observer() {
                @Override
                public Object messageDispatchStarting() {
                    Object token = null;
                    if (oldObserver != null) {
                        token = oldObserver.messageDispatchStarting();
                    }
                    looperMessageObserver.messageDispatchStarting(token);
                    return token;
                }

                @Override
                public void messageDispatched(Object token, Message msg) {
                    if (oldObserver != null) {
                        oldObserver.messageDispatched(token, msg);

                    }
                    looperMessageObserver.messageDispatched(token, msg);
                }

                @Override
                public void dispatchingThrewException(Object token, Message msg, Exception exception) {
                    if (oldObserver != null) {
                        oldObserver.dispatchingThrewException(token, msg, exception);
                    }
                    looperMessageObserver.dispatchingThrewException(token, msg, exception);
                }
            });

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
