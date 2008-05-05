package com.zipeg;

import java.util.prefs.*;


public class Presets {

    private static boolean useUserSettings = testUserSettings();
    private static final Preferences user = useUserSettings ? null : Preferences.userNodeForPackage(Zipeg.class).node(System.getProperty("user.name"));

    public static void put(String key, String value) {
        if (useUserSettings) {
            UserSettings.put(key, value);
        } else {
            user.put(key, value);
        }
    }

    public static void putInt(String key, int value) {
        if (useUserSettings) {
            UserSettings.putInt(key, value);
        } else {
            user.putInt(key, value);
        }
    }

    public static void putLong(String key, long value) {
        if (useUserSettings) {
            UserSettings.putLong(key, value);
        } else {
            user.putLong(key, value);
        }
    }

    public static void putBoolean(String key, boolean value) {
        if (useUserSettings) {
            UserSettings.putBoolean(key, value);
        } else {
            user.putBoolean(key, value);
        }
    }

    public static String get(String key, String value) {
        return useUserSettings ? UserSettings.get(key, value) : user.get(key, value);
    }

    public static int getInt(String key, int value) {
        return useUserSettings ? UserSettings.getInt(key, value) : user.getInt(key, value);
    }

    public static long getLong(String key, long value) {
        return useUserSettings ? UserSettings.getLong(key, value) : user.getLong(key, value);
    }

    public static boolean getBoolean(String key, boolean value) {
        return useUserSettings ? UserSettings.getBoolean(key, value) : user.getBoolean(key, value);
    }

    public static void clear() {
        if (useUserSettings) {
            UserSettings.clear();
            return;
        }
        try {
            user.clear();
            sync();
        } catch (BackingStoreException e) {
            switchToUserSettings();
        }
    }

    public static void flushNow() {
        if (useUserSettings) {
            UserSettings.flushNow();
        } else {
            flush();
        }
    }

    public static void flush() {
        if (useUserSettings) {
            UserSettings.flush();
            return;
        }
        int retry = 4;
        for (;;) {
            try {
                retry--;
                user.flush();
                return;
            } catch (BackingStoreException e) {
                if (retry <= 0) {
                    switchToUserSettings();
                    return;
                } else {
                    Util.sleep((int)(Math.random() * 100) + 10);
                }
            }
        }
    }

    public static void sync() {
        if (useUserSettings) {
            UserSettings.sync();
            return;
        }
        int retry = 4;
        for (;;) {
            try {
                retry--;
                user.sync();
                return;
            } catch (BackingStoreException e) {
                if (retry <= 0) {
                    switchToUserSettings();
                    return;
                } else {
                    Util.sleep((int)(Math.random() * 100) + 10);
                }
            }
        }
    }

    private static boolean testUserSettings() {
        try {
            UserSettings.put("test", "test");
            UserSettings.flushNow();
            return UserSettings.getBoolean("useUserSettings", false);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void switchToUserSettings() {
        UserSettings.putBoolean("useUserSettings", true);
        UserSettings.flushNow();
        useUserSettings = true;
    }
}
