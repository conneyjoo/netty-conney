package com.conney.keeptriple.local.util;

import java.util.Random;
import java.util.UUID;

public class Toolkit {

    public static int randomInt() {
        return Math.abs(new Random().nextInt());
    }

    public static int randomInt(int scope) {
        return Math.abs(new Random().nextInt()) % scope;
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
