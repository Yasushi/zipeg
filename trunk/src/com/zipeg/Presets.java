package com.zipeg;

import java.util.prefs.*;


public class Presets {

    private static final Preferences user = Preferences.userNodeForPackage(Zipeg.class).node(System.getProperty("user.name"));

    public static void put(String key, String value) {
        user.put(key, value);
    }

    public static void putInt(String key, int value) {
        user.putInt(key, value);
    }

    public static void putLong(String key, long value) {
        user.putLong(key, value);
    }

    public static void putBoolean(String key, boolean value) {
        user.putBoolean(key, value);
    }

    public static String get(String key, String value) {
        return user.get(key, value);
    }

    public static int getInt(String key, int value) {
        return user.getInt(key, value);
    }

    public static long getLong(String key, long value) {
        return user.getLong(key, value);
    }

    public static boolean getBoolean(String key, boolean value) {
        return user.getBoolean(key, value);
    }

    public static void clear() {
        try {
            user.clear();
            sync();
        } catch (BackingStoreException e) {
            throw new Error(e);
        }
    }

    public static void flush() {
        int retry = 4;
        for (;;) {
            try {
                retry--;
                user.flush();
                return;
            } catch (BackingStoreException e) {
                if (retry == 0) {
                    throw new Error(e);
                } else {
                    Util.sleep((int)(Math.random() * 100) + 10);
                }
            }
        }
    }

    public static void sync() {
        int retry = 4;
        for (;;) {
            try {
                retry--;
                user.sync();
                return;
            } catch (BackingStoreException e) {
                if (retry == 0) {
                    throw new Error(e);
                } else {
                    Util.sleep((int)(Math.random() * 100) + 10);
                }
            }
        }
    }

}
