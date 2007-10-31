package com.zipeg;

import java.util.prefs.*;


public class Presets {

    private static final Preferences pref = Preferences.userNodeForPackage(Zipeg.class); // legacy
    private static final Preferences user = pref.node(System.getProperty("user.name"));

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
        return user.get(key, pref.get(key, value));
    }

    public static int getInt(String key, int value) {
        return user.getInt(key, pref.getInt(key, value));
    }

    public static long getLong(String key, long value) {
        return user.getLong(key, pref.getLong(key, value));
    }

    public static boolean getBoolean(String key, boolean value) {
        return user.getBoolean(key, pref.getBoolean(key, value));
    }

    public static void clear() throws BackingStoreException {
        user.clear();
        pref.clear();
    }

    public static void flush() throws BackingStoreException {
        user.flush();
    }
}
