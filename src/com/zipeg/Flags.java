package com.zipeg;

public class Flags {

    public static final int
        LOCATION_LAST           = 0x00000001,
        LOCATION_ARCHIVE        = 0x00000002,
        LOCATION_DOCUMENTS      = 0x00000004,
        LOCATION_DESKTOP        = 0x00000008,

        EXTRACT_SELECTED        = 0x00000010,
        EXTRACT_WHOLE           = 0x00000020,
        EXTRACT_ASK             = 0x00000040,

        APPEND_ARCHIVE_NAME     = 0x00001000,
        PLAY_SOUNDS             = 0x00002000,
        METAL                   = 0x00004000,
        DIRECTORIES_FIRST       = 0x00008000,
        NO_DS_STORE             = 0x00010000,
        NO_RES_FORKS            = 0x00020000,

        PROMPT_CREATE_FOLDERS   = 0x01000000,
        PROMPT_EXTRACT_SELECTED = 0x02000000,
        FORCE_ENCODING          = 0x04000000,
        CLOSE_AFTER_EXTRACT     = 0x08000000,
        NO_APPEND_OK            = 0x10000000,
        DONT_OPEN_NESTED        = 0x20000000,

        CASE_SENSITIVE          = 0x40000000,

        DEFAULT = LOCATION_LAST|
                  EXTRACT_SELECTED|
                  PROMPT_CREATE_FOLDERS|PROMPT_EXTRACT_SELECTED|
                  APPEND_ARCHIVE_NAME|PLAY_SOUNDS|DIRECTORIES_FIRST
    ;

    private static long flags = loadFlags();

    private Flags() {}

    public static long getFlags() {
        return flags;
    }

    public static boolean getFlag(long f) {
        return (flags & f) != 0;
    }

    public static void addFlag(long f) {
        flags |= f;
        saveFlags();
    }

    public static void removeFlag(long f) {
        flags = flags & ~f;
        saveFlags();
    }

    private static long loadFlags() {
//      Debug.traceln("loadFlags " + prefs.getLong("flags", DEFAULT));
        return Presets.getLong("flags", Util.isMac() ? DEFAULT|METAL : DEFAULT);
    }

    private static void saveFlags() {
//      Debug.traceln("saveFlags " + Presets.getLong("flags", DEFAULT));
        Presets.putLong("flags", flags);
        Presets.sync();
    }

}
