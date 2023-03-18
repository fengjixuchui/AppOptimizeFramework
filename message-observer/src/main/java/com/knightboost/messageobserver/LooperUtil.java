package com.knightboost.messageobserver;

import android.os.Looper;
import android.util.Printer;

import java.lang.reflect.Field;

public class LooperUtil {
    public static void setMessageLogging(Looper looper,Printer printer) {
        Field field_mLogging = null;
        try {
            field_mLogging = Looper.class.getDeclaredField("mLogging");
        } catch (NoSuchFieldException e) {
            return;
        }
        field_mLogging.setAccessible(true);
        Printer oldPrinter = null;
        try {
            oldPrinter = (Printer) field_mLogging.get(looper);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        looper.setMessageLogging(new Printer() {
            @Override
            public void println(String x) {

            }
        });
    }
}
