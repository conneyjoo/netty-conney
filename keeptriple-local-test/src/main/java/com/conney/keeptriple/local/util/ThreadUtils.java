package com.conney.keeptriple.local.util;

public class ThreadUtils {

    public static void sleepSilent(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
