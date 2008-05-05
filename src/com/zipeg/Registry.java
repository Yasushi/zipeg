package com.zipeg;

import java.io.*;

public class Registry implements FileAssociationHandler {

    private static final String ZIPEG = "Zipeg";
    private static final int iconIndex = Util.isWindows() ?
            (Util.getOsVersion() > 5.1 ? -112 : -113) : -1;
    private static boolean loaded;
    private static Registry instance;

    private Registry() {
        loadLibrary();
    }

    public static Registry getInstance() {
        if (instance == null) {
            instance = new Registry();
        }
        return instance;
    }

    public boolean isAvailable() {
        return true;
    }

    public long initializeOLE() {
        return initializeOle();
    }

    /** Terminates all processes based on module name but not current process.
     * @param name process module name e.g. "notepad.exe"
     * @return number of terminated processes.
     */
    public long killProcessByName(String name) {
        return killProcess(name);
    }

    public long shellExecute(long mask, String verb, String file, String params, String dir) {
        return shellExec(mask, verb, file, params, dir);
    }

    public static void moveFileToRecycleBin(String abspathname) throws IOException {
        loadLibrary();
        moveToRecycleBin(abspathname);
    }

    public void setHandled(long selected) {
        long was = getHandled();
        boolean changed = false;
        for (int i = 0; i < ext2uti.length; i++) {
            if ("chm".equals(ext2uti[i]) || "iso".equals(ext2uti[i])) {
                continue; // leave chm and iso alone on Window
            }
            long flag = 1L << i;
            if ((flag & selected) != (flag & was)) {
                String ext = "." + ext2uti[i][0];
                if (ext.equalsIgnoreCase(".chm")) {
                    continue; // do not associate with chm!
                }
                String set = null;
                String current = getHandler(ext);
                if ((flag & selected) != 0) {
                    // save existing:
                    if (current != null && !"".equals(current) && !ZIPEG.equals(current)) {
                        Presets.put("Registry.Handler:" + ext, current);
                        Presets.sync();
                    }
                    if (!ZIPEG.equals(current)) {
                        set = ZIPEG;
                    }
                } else {
                    if (ZIPEG.equals(current)) {
                        set = Presets.get("Registry.Handler:" + ext, null);
                        if (set == null) {
                            set = "CompressedFolder";
                            Debug.traceln("Warning: no stored association for " + ext + " reverting to \"" + set + "\")");
                        }
                    }
                }
                if (set != null) {
                    Debug.traceln("Registry.setHandler(" + ext + ", " + set + ")");
                    setHandler(ext, set);
                    changed = true;
                } else {
                    Debug.traceln("not changed for " + ext);
                }
            }
        }
        if (changed) {
            notifyShellAssociationsChanged();
            notifyShellAllChanged();
        }
    }

    public long getHandled() {
        long result = 0;
        for (int i = 0; i < ext2uti.length; i++) {
            if (ZIPEG.equals(getHandler("." + ext2uti[i][0]))) {
                result |= (1L << i);
            } else {
//              Debug.traceln("foreign: " + ct + " = " + ch);
            }
        }
        return result;
    }

    public static class Key {

        private long key;
        private boolean isroot;

        public final static Key CLASSES_ROOT = new Key(0x80000000L);
        public final static Key CURRENT_USER =  new Key(0x80000001L);
        public final static Key LOCAL_MACHINE =  new Key(0x80000002L);
        public final static Key USERS =  new Key(0x80000003L);
        public final static Key PERFORMANCE_DATA =  new Key(0x80000004L);
        public final static Key PERFORMANCE_TEXT =  new Key(0x80000050L);
        public final static Key PERFORMANCE_NLSTEXT =  new Key(0x80000060L);
        public final static Key CURRENT_CONFIG =  new Key(0x80000005L);
        public final static Key DYN_DATA =  new Key(0x80000006L);

        private Key(long k) {
            key = k;
            isroot = true;
        }

        private Key() {
            loadLibrary();
        }

        public static boolean exists(Key parent, String path) {
            try {
                loadLibrary();
                long k = openKey(parent.key, path, KEY_ALL_ACCESS);
                closeKey(k);
                return true;
            } catch (IOException iox){
                return false;
            }
        }

        public boolean exists(String path) {
            return exists(this, path);
        }

        public Key open(String path) {
            try {
                loadLibrary();
                long k = openKey(key, path, KEY_ALL_ACCESS);
                Key key = new Key();
                key.key = k;
                return key;
            } catch (IOException iox){
 //             iox.printStackTrace();
                return null;
            }
        }

        public Key create(String subkey) {
            try {
                loadLibrary();
                int[] disposition = new int[1];
                long k = createKey(key, subkey, null, REG_OPTION_NON_VOLATILE,
                                   KEY_ALL_ACCESS, disposition);
                Key key = new Key();
                key.key = k;
                return key;
            } catch (IOException iox){
//              iox.printStackTrace();
                return null;
            }
        }

        public boolean deleteKey(String subkey) {
            try {
                loadLibrary();
                Registry.deleteKey(key, subkey);
                return true;
            } catch (IOException iox){
                iox.printStackTrace();
                return false;
            }
        }

        public boolean deleteValue(String name) {
            try {
                loadLibrary();
                Registry.deleteValue(key, name);
                return true;
            } catch (IOException iox){
                iox.printStackTrace();
                return false;
            }
        }

        public void close() {
            try {
                if (key != 0 && !isroot) {
                    Registry.closeKey(key);
                    key = 0;
                }
            } catch (IOException iox){
                iox.printStackTrace();
            }
        }

        public void flush() {
            try {
                loadLibrary();
                Registry.flushKey(key);
            } catch (IOException iox){
                iox.printStackTrace();
            }
        }

        public void put(String name, long value) {
            try {
                Registry.setValue(key, name, value);
            } catch (IOException iox){
                iox.printStackTrace();
            }
        }

        public void put(String name, String value) {
            try {
                Registry.setValue(key, name, value);
            } catch (IOException iox){
                iox.printStackTrace();
            }
        }

        public void put(String name, byte[] data) {
            try {
                Registry.setValue(key, name, data);
            } catch (IOException iox){
                iox.printStackTrace();
            }
        }

        public boolean isString(String name) {
            try {
                return Registry.getValue(key, name) instanceof String;
            } catch (IOException iox){
                return false;
            }
        }

        public boolean isLong(String name) {
            try {
                return Registry.getValue(key, name) instanceof Long;
            } catch (IOException iox){
                return false;
            }
        }

        public boolean isBinary(String name) {
            try {
                return Registry.getValue(key, name) instanceof byte[];
            } catch (IOException iox){
                return false;
            }
        }

        public Object get(String name) {
            try {
                return Registry.getValue(key, name);
            } catch (IOException iox){
                iox.printStackTrace();
                return null;
            }
        }

        public String get(String name, String defvalue) {
            try {
                return Registry.getValueString(key, name);
            } catch (IOException iox){
                return defvalue;
            }
        }

        public long get(String name, long defvalue) {
            try {
                Long v = Registry.getValueLong(key, name);
                return v != null ? v.longValue() : defvalue;
            } catch (IOException iox){
                return defvalue;
            }
        }

        public byte[] get(String name, byte[] defvalue) {
            try {
                return (byte[])Registry.getValue(key, name);
            } catch (IOException iox){
                return defvalue;
            }
        }

        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }

    }

    private static boolean createApplicationKeys(Key key, String installdir) {
        Key software = key.open("SOFTWARE");
        if (software == null) {
            return false;
        }
        Key zipeg = software.create("Zipeg");
        if (zipeg == null) {
            return false;
        }
        zipeg.put("", "Zipeg: Archive Browser and Explorer");
        zipeg.put("DefaultIcon", installdir + "\\Zipeg.exe," + iconIndex);
        zipeg.put("Path", installdir);
        zipeg.flush();
        zipeg.close();
        software.close();
        return true;
    }

    private static void createShellCommands(Key key, String exe) {
        Key shell = key.create("shell");

        Key action = shell.create("open");
        action.put("", "Open with Zipeg");
        Key command = action.create("command");
        command.put("", "\"" + exe + "\" \"%1\"");
        command.close();
        action.close();
/*      TODO: implement me
        action = shell.create("open.zipeg.extract.to");
        action.put("", "Zipeg: Extract to ...");
        command = action.create("command");
        command.put("", "\"" + exe + "\" --extract-to \"%1\"");
        command.close();
        action.close();

        action = shell.create("open.zipeg.extract.here");
        action.put("", "Zipeg: Extract here");
        command = action.create("command");
        command.put("", "\"" + exe + "\" --extract-here \"%1\"");
        command.close();
        action.close();
*/
        shell.close();
    }

    private static void createDefaultIcon(Key key, String exe) {
        Key defaultIcon = key.create("DefaultIcon");
        defaultIcon.put("", "\"" + exe + "\"," + iconIndex);
        defaultIcon.close();
    }

    private void setHandler(String ext, String handler) {
        Key key = Key.CLASSES_ROOT.open(ext);
        if (key == null) {
            key = Key.CLASSES_ROOT.create(ext);
        }
        if (key != null) {
            key.put("", handler);
            key.put("PerceivedType", "compressed");
            Key openWithList = key.create("OpenWithList");
            if (openWithList == null) {
                openWithList = key.open("OpenWithList");
            }
            if (openWithList != null) {
                openWithList.create("Zipeg.exe");
                openWithList.close();
            }
            key.close();
        }
    }

    private String getHandler(String ext) {
        Key zip = Key.CLASSES_ROOT.open(ext);
        if (zip != null) {
            String h = zip.get("", "");
            zip.close();
            return h;
        }
        return "";
    }

    public static boolean registerZipeg(String installdir) {
        String exe = installdir + "\\Zipeg.exe";
        if (!createApplicationKeys(Key.LOCAL_MACHINE, installdir)) {
            return false;
        }
        if (!createApplicationKeys(Key.CURRENT_USER, installdir)) {
            return false;
        }
        Key appPaths = Key.LOCAL_MACHINE.open("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths");
        Key zipeg = appPaths.create("Zipeg.exe");
        zipeg.put("", exe);
        zipeg.put("Path", installdir);
        zipeg.close();
        appPaths.close();

        Key associations = Key.CLASSES_ROOT.open("SystemFileAssociations");
        if (associations != null) {
            Key compressed = associations.create("compressed");
            if (compressed != null) {
                createShellCommands(compressed, exe);
                compressed.close();
            }
            associations.close();
        }
        // CLASSES_ROOT/Applications
        Key applications = Key.CLASSES_ROOT.open("Applications");
        if (applications != null) {
            zipeg = applications.create("Zipeg.exe");
            if (zipeg != null) {
                createDefaultIcon(zipeg, exe);
                createShellCommands(zipeg, exe);
                Key supportedTypes = zipeg.create("SupportedTypes");
                supportedTypes.put(".zip", "");
                supportedTypes.close();
                zipeg.close();
            }
            applications.close();
        }
        // IE7 way of doing the same:
        Key classes = Key.LOCAL_MACHINE.open("SOFTWARE\\Classes");
        if (classes != null) {
            Key assoc = classes.create("Zipeg.AssocFile.ZIP");
            if (assoc != null) {
                assoc.put("", "Compressed Zip Archive");
                createDefaultIcon(assoc, exe);
                createShellCommands(assoc, exe);
                assoc.close();
            }
            classes.close();
        }
        Key software = Key.LOCAL_MACHINE.open("SOFTWARE");
        zipeg = software.create("Zipeg");
        Key capabilities = zipeg.create("Capabilities");
        capabilities.put("ApplicationDescription",
                         "Zipeg Archive Explorer - the safer way to browse your archives");
        Key assoc = capabilities.create("FileAssociations");
        assoc.put(".zip", "Zipeg.AssocFile.ZIP");
        assoc.close();
        Key mime = capabilities.create("MimeAssociations");
        mime.put("application/x-zip-compressed", "Zipeg.AssocFile.ZIP");
        mime.close();
        capabilities.close();
        zipeg.close();

        Key registeredApplications = software.create("RegisteredApplications");
        if (registeredApplications != null) {
            // on one instance of XP there is crash report of registeredApplications == null
            registeredApplications.put("Zipeg", "Software\\Zipeg\\Capabilities");
            registeredApplications.close();
        }
        software.close();

        // Legacy way of doing it:
        zipeg = Key.CLASSES_ROOT.create("Zipeg");
        if (zipeg != null) {
            createDefaultIcon(zipeg, exe);
            createShellCommands(zipeg, exe);
            zipeg.close();
        }

        classes = Key.LOCAL_MACHINE.open("SOFTWARE\\Classes");
        zipeg = classes.create("Zipeg");
        createDefaultIcon(zipeg, exe);
        createShellCommands(zipeg, exe);
        zipeg.close();
        classes.close();

        try {
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5097859
            Key js = Key.LOCAL_MACHINE.open("SOFTWARE\\JavaSoft");
            js.create("Prefs");
        } catch (Throwable x) {
            /* ignore */
        }
        return true;
    }

    private static void setValue(long key, String subkey, byte[] data) throws IOException {
        setValue(key, subkey, REG_BINARY, data);
    }

    private static void setValue(long key, String subkey, String s) throws IOException {
        byte[] data = new byte[(s.length() + 1) * 2];
        int k = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = ((int)s.charAt(i)) & 0xFFFF;
            data[k++] = (byte)c;
            c >>>= 8;
            data[k++] = (byte)c;
        }
        setValue(key, subkey, REG_SZ, data);
    }

    private static void setValue(long key, String subkey, long v) throws IOException {
        if (v < 0 || v > 0xFFFFFFFFFL) {
            throw new IllegalArgumentException("value out of range: 0x" + Long.toHexString(v));
        }
        byte[] data = new byte[4];
        for (int i = 0; i < 4; i++) {
            data[i] = (byte)v;
            v >>>= 8;
        }
        setValue(key, subkey, REG_DWORD, data);
    }

    private static Object getValue(long key, String subkey) throws IOException {
        int[] type = new int[1];
        byte[] data = getValue(key, subkey, type);
        if (type[0] == REG_DWORD) {
            assert data.length == 4 : subkey;
            long v = 0;
            for (int i = 3; i >= 0; i--) {
                v = (v << 8) | (((int)data[i]) & 0xFF);
            }
            return new Long(v);
        } else if (type[0] == REG_SZ || type[0] == REG_EXPAND_SZ) {
            if (data.length == 1 && (data[0] == 0 || data[0] == '@' || data[0] == 0x20)) {
                return "";
            }
            assert data.length % 2 == 0 : "\"" + subkey + "\" len=" + data.length + " type=" + type[0] +
                    " data[0]=0x" + Integer.toHexString(data[0]);
            int len = data.length;
            while (len >= 2 && data.length > 2 && data[len - 1] == 0 && data[len - 2] == 0) {
                len -= 2;
            }
            if (len == 0) {
                return "";
            } else {
                len = len / 2;
                char[] text = new char[len];
                int k = 0;
                for (int i = 0; i < len; i++) {
                    int lo = ((int)data[k++]) & 0xFF;
                    int hi = ((int)data[k++]) & 0xFF;
                    text[i] = (char)((hi << 8) | lo);
                }
                return new String(text);
            }
        } else if (type[0] == REG_BINARY) {
            return data;
        } else {
            throw new IllegalArgumentException("unsupported registry type: " + type[0]);
        }
    }

    private static String getValueString(long key, String subkey) throws IOException {
        return (String)getValue(key, subkey);
    }

    private static Long getValueLong(long key, String subkey) throws IOException {
        return (Long)getValue(key, subkey);
    }

    static void loadLibrary() {
        if (loaded) {
            return;
        }
        String path = new File(".", getLibraryName()).getAbsolutePath();
        try {
            System.loadLibrary(getLibraryName());
            loaded = true;
        } catch (Throwable ex) {
            throw new Error("failed to load " + path);
        }
    }

    public static String getLibraryName() {
        assert Util.isWindows();
        return "win32reg";
    }

    // native JNI registry bridge:

    private static final int // access:
            KEY_QUERY_VALUE         = 0x0001,
            KEY_SET_VALUE           = 0x0002,
            KEY_CREATE_SUB_KEY      = 0x0004,
            KEY_ENUMERATE_SUB_KEYS  = 0x0008,
            KEY_NOTIFY              = 0x0010,
            KEY_CREATE_LINK         = 0x0020,
/*
            KEY_WOW64_32KEY         = 0x0200,
            KEY_WOW64_64KEY         = 0x0100,
            KEY_WOW64_RES           = 0x0300,
*/
            SYNCHRONIZE             = 0x00100000,
            STANDARD_RIGHTS_ALL     = 0x001F0000,
/*
            STANDARD_RIGHTS_WRITE   = 0x00020000,
            STANDARD_RIGHTS_READ    = 0x00020000,
            KEY_READ = (STANDARD_RIGHTS_READ |
                        KEY_QUERY_VALUE |
                        KEY_ENUMERATE_SUB_KEYS |
                        KEY_NOTIFY)
                        &
                        ~SYNCHRONIZE,
            KEY_WRITE = (STANDARD_RIGHTS_WRITE |
                        KEY_SET_VALUE |
                        KEY_CREATE_SUB_KEY)
                        &
                        ~SYNCHRONIZE,
            KEY_EXECUTE = KEY_READ & ~SYNCHRONIZE,
*/
            KEY_ALL_ACCESS = (STANDARD_RIGHTS_ALL |
                              KEY_QUERY_VALUE |
                              KEY_SET_VALUE |
                              KEY_CREATE_SUB_KEY |
                              KEY_ENUMERATE_SUB_KEYS |
                              KEY_NOTIFY |
                              KEY_CREATE_LINK)
                              &
                              ~SYNCHRONIZE,
            // options
            REG_OPTION_NON_VOLATILE = 0x00000000,   // Key is preserved when system is rebooted
/*
            REG_OPTION_VOLATILE = 0x00000001,   // Key is not preserved when system is rebooted
            REG_OPTION_CREATE_LINK = 0x00000002,   // Created key is a symbolic link
            REG_OPTION_BACKUP_RESTORE = 0x00000004,   // open for backup or restore special access rules
            REG_OPTION_OPEN_LINK = 0x00000008,   // Open symbolic link
            // disposition
            REG_CREATED_NEW_KEY = 0x00000001,   // created New Registry Key
            REG_OPENED_EXISTING_KEY = 0x00000002,   // opened Existing Key
            // data type:
            REG_NONE                    = 0,   // No value type
*/
            REG_SZ                      = 1,   // Unicode nul terminated string
            REG_EXPAND_SZ               = 2,   // Unicode nul terminated string
                                               // (with environment variable references)
            REG_BINARY                  = 3,   // Free form binary
            REG_DWORD                   = 4;   // 32-bit number
/*
            REG_DWORD_LITTLE_ENDIAN     = 4,   // 32-bit number =same as REG_DWORD)
            REG_DWORD_BIG_ENDIAN        = 5,   // 32-bit number
            REG_LINK                    = 6,   // Symbolic Link =unicode)
            REG_MULTI_SZ                = 7,   // Multiple Unicode strings
            REG_RESOURCE_LIST           = 8,   // Resource list in the resource map
            REG_FULL_RESOURCE_DESCRIPTOR = 9,  // Resource list in the hardware description
            REG_RESOURCE_REQUIREMENTS_LIST = 10,
            REG_QWORD                   = 11,  // 64-bit number
            REG_QWORD_LITTLE_ENDIAN     = 11;  // 64-bit number =same as REG_QWORD)
*/

    //  private native static long connectRegistry(long key, String host) throws IOException;
    private native static long openKey(long key, String subkey, int access) throws IOException;
    private native static long createKey(long key, String subkey, String regclass, int options, int access,
                                         int[] disposition) throws IOException;
    private native static void closeKey(long key) throws IOException;
    private native static void flushKey(long key) throws IOException;
    private native static void deleteKey(long key, String subkey) throws IOException;
    private native static void deleteValue(long key, String subkey) throws IOException;
    private native static void setValue(long key, String subkey, int type, byte[] data) throws IOException;
    private native static byte[] getValue(long key, String subkey, int[] type) throws IOException;
    private native static void moveToRecycleBin(String abspathname) throws IOException;
    private native static void notifyShellAssociationsChanged();
    private native static void notifyShellAllChanged();
    private native static long initializeOle();
    private native static long killProcess(String name);
    public static final int
        SEE_MASK_CLASSNAME         = 0x00000001,
        SEE_MASK_CLASSKEY          = 0x00000003,
        // Note INVOKEIDLIST overrides IDLIST
        SEE_MASK_IDLIST            = 0x00000004,
        SEE_MASK_INVOKEIDLIST      = 0x0000000c,
        SEE_MASK_ICON              = 0x00000010,
        SEE_MASK_HOTKEY            = 0x00000020,
        SEE_MASK_NOCLOSEPROCESS    = 0x00000040,
        SEE_MASK_CONNECTNETDRV     = 0x00000080,
        SEE_MASK_FLAG_DDEWAIT      = 0x00000100,
        SEE_MASK_DOENVSUBST        = 0x00000200,
        SEE_MASK_FLAG_NO_UI        = 0x00000400,
        SEE_MASK_UNICODE           = 0x00004000,
        SEE_MASK_NO_CONSOLE        = 0x00008000,
        SEE_MASK_ASYNCOK           = 0x00100000,
        SEE_MASK_HMONITOR          = 0x00200000,
        SEE_MASK_NOZONECHECKS      = 0x00800000,
        SEE_MASK_NOQUERYCLASSSTORE = 0x01000000,
        SEE_MASK_WAITFORINPUTIDLE  = 0x02000000,
        SEE_MASK_FLAG_LOG_USAGE    = 0x04000000,
        // return codes: > 32 OK
        SE_ERR_FNF              = 2,       // file not found
        SE_ERR_PNF              = 3,       // path not found
        SE_ERR_ACCESSDENIED     = 5,       // access denied
        SE_ERR_OOM              = 8,       // out of memory
        SE_ERR_DLLNOTFOUND      = 32,
        // error values for ShellExecute() beyond the regular WinExec() codes
        SE_ERR_SHARE            = 26,
        SE_ERR_ASSOCINCOMPLETE  = 27,
        SE_ERR_DDETIMEOUT       = 28,
        SE_ERR_DDEFAIL          = 29,
        SE_ERR_DDEBUSY          = 30,
        SE_ERR_NOASSOC          = 31;

    private native static long shellExec(long mask, String verb, String file, String params, String dir);
    /*
        The SEE_MASK_FLAG_DDEWAIT flag must be specified if the thread
        calling ShellExecuteEx does not have a message loop or if the thread or process
        will terminate soon after ShellExecuteEx returns.
        Under such conditions, the calling thread will not be available
        to complete the DDE conversation, so it is important that ShellExecuteEx
        complete the conversation before returning control to the caller.
        Failure to complete the conversation can result in an unsuccessful
        launch of the document.

        To include double quotation marks in lpParameters,
        enclose each mark in a pair of quotation marks, as in the following example.

        sei.lpParameters = "An example: \"\"\"quoted text\"\"\"";
        In this case, the application receives
        three parameters: An, example:, and "quoted text".
     */
}
