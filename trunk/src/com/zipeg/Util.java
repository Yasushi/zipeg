package com.zipeg;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import java.awt.event.*;
import java.awt.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.NumberFormat;
import java.security.SecureRandom;
import java.nio.channels.FileChannel;
import javax.swing.Timer;

public final class Util {

    private static final boolean isMac = System.getProperty("os.name").toLowerCase().indexOf("mac os x") >= 0;
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
    private static final boolean isLinux = System.getProperty("os.name").toLowerCase().indexOf("linux") >= 0;

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;
    public static final int GB = 1024 * MB;
    public static final Class[] VOID = new Class[]{};
    public static final Class[] OBJECT = new Class[]{Object.class};
    public static final Class[] STRING = new Class[]{String.class};
    public static final Class[] MAP = new Class[]{Map.class};
    public static final Object[] NONE = new Object[]{};
    private static String processor = System.getProperty("os.arch");
    private static float osVersion = parseOsVersion();
    private static final float javaVersion = parseJavaVersion();
    private static final String version = readVersionAndRevision();
    private static int revision;
    private static File tmp; // temp directory
    private static File desktop;
    private static File cache; // cache directory
    private static Method getDesktop =
            isWindows() ?
            getDeclaredMethod("sun.awt.shell.Win32ShellFolderManager2.getDesktop", VOID) :
            isMac() ?
            getDeclaredMethod("com.zipeg.mac.MacSpecific.getDeskFolder", VOID) :
            null;

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static void sleep(int milliseconds) {
        try { Thread.sleep(milliseconds); } catch (InterruptedException e) { /* ignore */ }
    }

    public static void invokeLater(int milliseconds, final Runnable r) {
        Timer timer = new Timer(milliseconds, new ActionListener(){
            public void actionPerformed(ActionEvent actionEvent) {
                r.run();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static boolean isMac() {
        return isMac;
    }

    public static boolean isLinux() {
        return isLinux;
    }

    public static boolean isWindows() {
        return isWindows;
    }

    /**
     * @return known processor types: i386, ppc
     */
    public static String getProcessor() {
        return processor;
    }

    /**
     * @return 1.5006 for 1.5.0_6
     */
    public static float getJavaVersion() {
        return javaVersion;
    }

    /**
     * @return 10.48 for 10.4.8 or 10.39 for 10.3.9
     */
    public static float getOsVersion() {
        return osVersion;
    }

    public static String getVersion() {
        return version;
    }

    public static int getRevision() {
        return revision;
    }

    public static String getHome() {
        return System.getProperty("user.home");
    }

    public static File getDocuments() {
        return new File(getHome(), isMac() ? "Documents" : "My Documents");
    }

    public static File getDesktop() {
        if (desktop == null) {
            desktop = (File)call(getDesktop, null, NONE);
            if (desktop == null) {
                desktop = new File(getHome(), "Desktop");
            }
        }
        assert desktop.isDirectory() : desktop;
        return desktop;
    }

    public static File getUserPreferences() {
        File p;
        if (isMac()) {
            p = new File(getHome(), "Library/Preferences");
/*
            try {
                String up = MacOSX.findFolder(MacOSX.kUserDomain, MacOSX.kPreferencesFolderType, true);
                p = new File(up);
            } catch (IOException e) {
                Debug.printStackTrace("", e);
                p = new File(getHome(), "Library/Preferences");
            }
*/
        } else if (isWindows()) {
            // TODO: ShGetSpecialFolder is a better way:
            p = new File(getHome(), "Local Settings\\Application Data");
        } else {
            // TODO: for Un*x there should be some kind of standard:
            p = new File(getHome(), ".java-apps-user-prefs");
        }
        try {
            p = p.getCanonicalFile();
            p.mkdirs();
            return p;
        } catch (IOException e) {
            throw new Error(e);
        }
    }
    
    public static File getTmp() {
        if (tmp == null) {
            try {
                if (isMac()) {
                    tmp = (File)callStatic("com.zipeg.mac.MacSpecific.getTempFolder", Util.NONE);
                } else {
                    File temp = File.createTempFile("zipeg.com", "test", null);
                    tmp = temp.getParentFile();
                    if (!temp.delete()) {
                        temp.deleteOnExit();
                    }
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        assert tmp.isDirectory() : tmp;
        return tmp;
    }

    /**
     * @return full pathname of java executable
     */
    public static String getJava() {
        File bin = new File(System.getProperty("java.home"), "bin");
        return getCanonicalPath(new File(bin, isMac() ? "java" : "javaw.exe"));
    }

    private static File getApplicationData() {
        File data = new File(!isWindows() ?
                             getTmp() :
                             new File(System.getProperty("user.home"), "/Application Data"), "com.zipeg");
        if (!data.isDirectory()) {
            data.mkdirs();
        }
        return data;
    }

    public static Map getenv() {
        return getJavaVersion() < 1.5 ? null :
                (Map)callStatic("java.lang.System.getenv", Util.NONE);
    }

    public static String[] getEnvFilterOutMacCocoaCFProcessPath() {
        // http://lists.apple.com/archives/printing/2003/Apr/msg00074.html
        // it definitely breaks Runtime.exec("/usr/bin/open", ...) on Leopard
        String[] env = null;
        Map v = Util.getenv();
        if (v != null) {
            ArrayList a = new ArrayList();
            for (Iterator i = v.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry)i.next();
                String key = (String)e.getKey();
//              Debug.traceln(key + "=" + e.getValue());
                if (!"CFProcessPath".equalsIgnoreCase(key)) {
                    a.add(key + "=" + e.getValue());
                }
            }
            env = (String[])a.toArray(new String[a.size()]);
        }
        return env;
    }

    public static File getCacheDirectory() {
        // System.getProperty("user.home")/Library/Caches/com.zipeg/ is unhappy place for Finder!
        // see study/DnD project
        if (cache == null) {
            cache = new File(getApplicationData(), luid());
            File first = cache;
            int retry = 10;
            while (retry > 0 && !cache.mkdirs()) {
                sleep((int)(Math.random() * 100) + 100);
                cleanupCaches();
                cache = new File(getApplicationData(), luid((int)(Math.random() * 100) + 100));
                retry--;
            }
            assert retry > 0 : "first=" + first + " second=" + cache;
            File lock = new File(cache, ".lock");
            try { lock.createNewFile(); } catch (IOException e) { throw new Error(e); }
            lock.deleteOnExit();
            cache.deleteOnExit();
            cleanupCaches();
        }
        return cache;
    }

    /**
     * searches and destroyes all caches older than 3 days
     */

    private static void cleanupCaches() {
        final long TOO_OLD = 3L * 24L * (3600 * 1000); // 3 days
        //final long TOO_OLD = 3L * (60 * 1000); // 3 minutes
        File data = getApplicationData();
        File[] dirs = data.listFiles();
        if (dirs == null) {
            return;
        }
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            // paranoia - because the code is about to do rmdirs()
            // as a minimum: /tmp/com.zipeg/
            if (dir.exists() && dir.toString().length() > "/tmp/com.zipeg".length() &&
                dir.toString().indexOf("com.zipeg") > 0) {
                long delta; // how many milliseconds ago it was modified?
                File lock = new File(dir, ".lock");
                if (lock.exists()) {
                    delta = System.currentTimeMillis() - lock.lastModified();
                    Debug.traceln(lock + " " + delta / (3600 * 1000) + " hrs old " + new Date(lock.lastModified()));
                } else {
                    delta = System.currentTimeMillis() - dir.lastModified();
                    Debug.traceln(dir + " " + delta / (3600 * 1000) + " hrs old " + new Date(dir.lastModified()));
                    Debug.traceln(dir + "/.lock absent");
                }
                if (delta > TOO_OLD) {
                    lock.delete();
                    rmdirs(dir);
                    Debug.traceln("removed " + dir);
                }
            }
        }
        Debug.traceln();
    }

    private static InputStream getVersionFileInputStream() throws FileNotFoundException {
        File file = getVersionFile(); // always try the Content/version first
        return file.canRead() ? new FileInputStream(file) :
               Resources.getResourceAsStream("version.txt");
    }

    public static File getVersionFile() {
        String url = Resources.class.getResource("Util.class").toString();
        // jar:file:/Users/~/xepec.com/svn/src/trunk/zipeg/Zipeg.app/Contents/Resources/Java/zipeg.jar!/...
        int ix = url.indexOf(".jar!/");
        if (ix > 0) { // running from jar
            ix = url.lastIndexOf('/', ix);
            assert ix >= 0;
            url = url.substring(0, ix + 1) + "version.txt";
        } else {
            ix = url.lastIndexOf("/classes/");
            url = url.substring(0, ix) + "/Zipeg.app/Contents/Resources/Java/version.txt";
        }
        ix = url.indexOf("file:");
        if (ix >= 0) {
            url = url.substring(ix + 5);
        }
        return new File(url);
    }

    public static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
//          assert false : "" + file;
//          throw new Error(e);
        }
    }

    /** Constructs primitive plural form of English nouns
     * @param n number of things
     * @param s singular noun for a thing
     * @return string like "123 items" or "21 file"
     */
    public static String plural(int n, String s) {
        return n + " " + (n != 11 && n % 10 == 1 ? s : (s + "s"));
    }

    // TODO: 1. isMac() 2. On Win32 does not work with Unicode names

    public static void openDoc(String filepath) {
        assert isWindows() : "only on Win32";
        try {
            Runtime.getRuntime().exec("rundll32.exe shell32.dll,ShellExec_RunDLL " + filepath);
          } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void openDocWith(String filepath) {
        assert isWindows() : "only on Win32";
        try {
            Runtime.getRuntime().exec("rundll32.exe shell32.dll,OpenAs_RunDLL " + filepath);
          } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void previewImage(String file) {
        try {
            if (isMac()) {
                Runtime.getRuntime().exec(new String[]{"open", "/Applications/Preview.app", file});
            } else if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 shimgvw.dll,ImageView_Fullscreen " + file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param value number to format
     * @return string formated with "," or "." in 3 decimal places.
     * E.g. 153248 becomes "153,248" or "153.248" depending on default locale.
     */
    public static String formatNumber(long value) {
        return NumberFormat.getInstance().format(value);
    }

    /**
     * @param bytes number to format
     * @return for numbers <= 10KB returns number of bytes formated with comas
     *         for numbers > 10KB returns number of kilobytes
     *         for 0 returns empty string
     */
    public static String formatKB(long bytes) {
        String v;
        if (bytes > 10 * KB) {
            v = formatNumber(bytes / KB) + " KB";
        } else if (bytes > 0) {
            v = formatNumber(bytes) + " bytes";
        } else {
            v = "";
        }
        return v;
    }

    /**
     * @param bytes number to format
     * @return for numbers > 10MB return number of megabytes formated with comas
     *         for smaller numbers return formatKB()
     */
    public static String formatMB(long bytes) {
        if (bytes > 10 * MB) {
            return formatNumber(bytes / MB) + " MB";
        }
        else {
            return formatKB(bytes);
        }
    }

    public static void close(InputStream s) {
        if (s != null) {
            try { s.close(); } catch (IOException x) { throw new Error(x); }
        }
    }

    public static void close(OutputStream s) {
        if (s != null) {
            try { s.close(); } catch (IOException x) { throw new Error(x); }
        }
    }

    public static byte[] readBytes(InputStream s) throws IOException {
        byte[] b = new byte[s.available()];
        int i = 0;
        int n = b.length;
        while (n > 0) {
            int k = s.read(b, i, n);
            assert k >= 0 && k <= n : "expected " + n + " read " + k + " available " + b.length;
            n -= k;
            i += k;
        }
        return b;
    }

    public static byte[] readFile(File f) {
        InputStream s = null;
        try {
            s = new FileInputStream(f);
            return readBytes(s);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            close(s);
        }
    }

    /**
     * callMainOnNewProcess copy out single class (should not have package dependencies)
     * and starts new process calling main(String[] args) from that class
     * @param cls  class name e.g. "CopyAndRestart"
     * @param args arguments to pass to CopyAndRestart.main()
     */
    public static void callMainOnNewProcess(String cls, String[] args) {
        File wd = new File(getCanonicalPath(new File(".")));
        InputStream i = null;
        FileOutputStream o = null;
        try {
            String c = cls + ".class";
            i = Util.class.getResource(c).openStream();
            File car = new File(wd, "com/zipeg/" + c);
            car.delete();
            car.getParentFile().mkdirs();
            car.createNewFile();
            o = new FileOutputStream(car);
            copyStream(i, o);
            close(i);
            i = null;
            close(o);
            o = null;
            String[] a = new String[args.length + 2];
            a[0] = getJava();
            a[1] = "com.zipeg." + cls;
            System.arraycopy(args, 0, a, 2, args.length);
            Runtime.getRuntime().exec(a);
/*
            Process p = Runtime.getRuntime().exec(a);
            p.waitFor();
*/
        } catch (Throwable e) { // IOException or InterruptedException
            throw new Error(e);
        } finally {
            close(i);
            close(o);
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16*1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * TODO: make atomic
     * Copy full content of existing file "from" into <b>new</b> file "to".
     * On OSX does not preserve resource fork and file finder attributes. Use
     * only when absolutely sure that those are not needed.
     * @param from source file (must exist and be readable)
     * @param to destination file (must not exist)
     * @throws IOException if failed to copy or close files
     */
    public static void copyFile(File from, File to) throws IOException {
        if (to.exists()) {
            throw new IOException("cannot copy file: \"" + from +
                                  "\" over existing file: \"" + to + "\"");
        }
        RandomAccessFile fin = null;
        FileChannel cin = null;
        RandomAccessFile fout = null;
        FileChannel cout = null;
        to.createNewFile();
        try {
            fin = new RandomAccessFile(from, "r");
            fout = new RandomAccessFile(to, "rw");
            cin = fin.getChannel();
            cout = fout.getChannel();
            cout.transferFrom(cin, 0, fin.length());
        } finally {
            if (cin != null) cin.close();
            if (cout != null) cout.close();
            if (fin != null) fin.close();
            if (fout != null) fout.close();
        }
    }

    /**
     * TODO: make atomic
     * Copy full content of existing file "from" into <b>new</b> file "to"
     * preserving resource fork and dir attrs. Does not copy over!
     * @param from source file (must exist and be readable)
     * @param to destination file (must not exist)
     * @throws IOException if failed to copy or close files
     */
    private static void cpFile(File from, File to) throws IOException {
        assert isMac() : " cpFile uses OSX specific options \"cp -n -p\"";
        String f = getCanonicalPath(from);
        String t = getCanonicalPath(to);
        Process cp = Runtime.getRuntime().exec(new String[]{"cp", "-n", "-p", f, t});
        try {
            int res = cp.waitFor();
            if (res != 0) {
                throw new IOException("error=" + res + " while copy \"" + f + "\" \"" + t + "\"");
            }
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Renames file and if fails copy full content of existing file "from" into <b>new</b> file "to".
     * @param from source file (must exist and be readable)
     * @param to destination file (must not exist)
     * @throws IOException if failed to copy or close files
     */
    public static void renameOrCopyFile(File from, File to) throws IOException {
        if (!from.renameTo(to)) { // this java call preserves resource fork and dir attrs
            if (isMac()) {
                cpFile(from, to); // slower but correct
            } else {
                copyFile(from, to);
            }
            if (!from.delete()) {
                throw new IOException("failed to delete: " + from);
            }
        }
    }

    public static boolean rmdirs(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        boolean b = true;
        File[] list = dir.listFiles();
        if (list != null) { // dir stopped being directory (e.g. deleted from another process)
            for (int i = 0; i < list.length; i++) {
                b = (list[i].isDirectory() ? rmdirs(list[i]) : list[i].delete()) && b;
            }
        }
        b = dir.delete() && b;
        return b;
    }

    public static Object callStatic(String method, Object[] params) {
        try {
            int ix = method.lastIndexOf('.');
            String cls = method.substring(0, ix);
            String mtd = method.substring(ix + 1);
            Class[] signature;
            if (params.length == 0) {
                signature = new Class[]{};
            } else {
                signature = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    signature[i] = params[i].getClass();
                }
            }
            return Class.forName(cls).getMethod(mtd, signature).invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    public static Method getDeclaredMethod(String method, Class[] s) {
        try {
            int ix = method.lastIndexOf('.');
            String cls = method.substring(0, ix);
            String mtd = method.substring(ix + 1);
            Method m = Class.forName(cls).getDeclaredMethod(mtd, s);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            Debug.printStackTrace("no such method " + method, e);
            return null;
        } catch (ClassNotFoundException e) {
            Debug.printStackTrace("class not found " + method, e);
            return null;
        }
    }

    public static Object call(Method m, Object that, Object[] params) {
        try {
            return m == null ? null : m.invoke(that, params);
        } catch (IllegalAccessException e) {
            Debug.printStackTrace("failed to call " + m, e);
            return null;
        } catch (InvocationTargetException e) {
            Debug.printStackTrace("failed to call " + m, e);
            return null;
        }
    }

    public static void openUrl(String url) {
        if (isMac) {
            callStatic("com.apple.eio.FileManager.openURL", new Object[]{url});
        } else {
            try {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendMail(String email, String title, String body) {
        try {
            final int MAX_LENGTH = 1200;
            if (isWindows && body.length() > MAX_LENGTH) {
                body = body.substring(0, MAX_LENGTH);
            }
            body = spaces(URLEncoder.encode(body, "UTF-8"));
            title = spaces(URLEncoder.encode(title, "UTF-8"));
            openUrl("mailto:" + email + "?subject=" + title + "&body=" + body);
        }
        catch (IOException e) {
            // noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    /**
     * HTTP GET
     * @param url to get from
     * @param headers Map of none-null String -> String value pairs (params itself can be null)
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    public static boolean getFromUrl(String url, Map headers, Map reply, ByteArrayOutputStream body)
            throws IOException {
        return httpGetPost(false, url, headers, reply, body);
    }

    /**
     * HTTP POST
     * @param url to post to
     * @param headers Map of none-null String -> String value pairs (params itself can be null)
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    public static boolean postToUrl(String url, Map headers, Map reply, ByteArrayOutputStream body)
            throws IOException {
        return httpGetPost(true, url, headers, reply, body);
    }

    /**
     * HTTP GET/POST
     * @param post true if POST is requested
     * @param url to get from
     * @param headers Map of none-null String -> String value pairs (params itself can be null)
     * @param reply headers from http server (ignored if reply == null)
     * @param body of a reply will be written there if body is not null
     * @return true if request was successful
     * @throws IOException on i/o errors
     */
    static boolean httpGetPost(boolean post, String url, Map headers, Map reply, ByteArrayOutputStream body)
            throws IOException {
        StringBuffer content = new StringBuffer(2 * 1024);
        if (headers != null) {
            for (Iterator i = headers.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry)i.next();
                assert e.getKey() instanceof String;
                assert e.getValue() instanceof String;
                String value = URLEncoder.encode((String)e.getValue(), "UTF-8");
                if (content.length() == 0) {
                    content.append(e.getKey()).append('=').append(value);
                } else {
                    content.append('&').append(e.getKey()).append('=').append(value);
                }
            }
            if (!post) {
                url = url + "&" + content;
            }
        }
        URLConnection conn = new URL(url).openConnection();
        conn.setUseCaches(false);
        conn.setDoInput(true);
        if (post) {
            conn.setDoOutput(true);
        }
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "Mozilla/4.0");
        conn.setDefaultUseCaches(false);
        conn.setRequestProperty("Cache-Control", "max-age=0");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Cache-Control", "no-store");
        conn.setRequestProperty("Pragma", "no-cache");
        if (post) {
            conn.setRequestProperty("Content-Length", "" + content.length());
            DataOutputStream printout = new DataOutputStream(conn.getOutputStream());
            if (content.length() > 0) {
                printout.writeBytes(content.toString());
            }
            printout.flush();
            close(printout);
        }
        Map fields = conn.getHeaderFields();
        if (reply != null) {
            reply.putAll(fields);
        }
        String response = getHttpReplyHeaderString(fields, null);
        DataInputStream input = new DataInputStream(conn.getInputStream());
        int len = (int)getHttpReplyHeaderLong(fields, "Content-Length");
        if (len > 0) {
            byte[] buf = new byte[len];
            input.readFully(buf);
            if (body != null) body.write(buf);
        }
        close(input);
        return responseCode(response) == 200; // "HTTP/1.1 200 OK"
    }

    private static String getHttpReplyHeaderString(Map fields, Object key) {
        if (fields == null) {
            return "";
        }
        Object value = fields.get(key);
        if (!(value instanceof List)) {
            return "";
        }
        List list = (List)value;
        if (list.size() <= 0) {
            return "";
        }
        return (String)list.get(0);
    }

    private static long getHttpReplyHeaderLong(Map fields, Object key) {
        String s = getHttpReplyHeaderString(fields, key);
        try {
            return s.length() > 0 ? Long.decode(s).longValue() : -1;
        } catch (NumberFormatException x) {
            return -1;
        }
    }

    private static int responseCode(String s) {
        // simple parser for : "HTTP/1.1 200 OK"
        if (s == null) return -1;
        StringTokenizer st = new StringTokenizer(s);
        if (!st.hasMoreTokens()) return -1;
        st.nextToken(); // HTTP/1.1
        if (!st.hasMoreTokens()) return -1;
        try{
            return Integer.decode(st.nextToken()).intValue();
        } catch (NumberFormatException x) {
            return -1;
        }
    }

    private static float parseOsVersion() {
        String v = System.getProperty("os.version");
        // Mac: 10.4.8
        // Windows: 5.1
        if (!isMac() && !isWindows()) {
            assert false : v + " debug me on Linux";
        }
        int ix = v.indexOf('.');
        if (ix > 0) {
            String s = v.substring(ix + 1);
            v = v.substring(0, ix + 1) + s.replaceAll("\\.", "").replaceAll("_", "");
        }
        ix = 0;
        while (ix < v.length() && (Character.isDigit(v.charAt(ix)) ||
                                   v.charAt(ix) == '.')) {
            ix++;
        }
        v = v.substring(0, ix);
        return Float.parseFloat(v);
    }

    private static String readVersionAndRevision() {
        Properties p = new Properties();
        String v = "0.0.0.0 (development)";
        InputStream is = null;
        try {
            is = getVersionFileInputStream();
            if (is != null) {
                p.load(is);
                String s = p.getProperty("version").trim();
                int ix = s == null ? -1 : s.lastIndexOf('.');
                if (s != null && ix > 0) {
                    revision = Integer.decode(s.substring(ix + 1)).intValue();
                    v = s;
                }
            }
        } catch (IOException iox) {
            /* ignore */
        } catch (NumberFormatException nfx) {
            /* ignore */
        } finally {
            close(is);
        }
        Debug.traceln("zipeg " + v);
        return v;
    }

    private static String spaces(String s) {
        StringBuffer r = new StringBuffer(s.length());
        int i = 0;
        for (;;) {
            int j = s.indexOf('+', i);
            if (j < 0) break;
            r.append(s.substring(i, j));
            r.append("%20");
            i = j + 1;
        }
        r.append(s.substring(i));
        return r.toString();
    }

    private static float parseJavaVersion() {
        String v = System.getProperty("java.version");
        int ix = v.indexOf('.');
        if (ix > 0) {
            String s = v.substring(ix + 1);
            v = v.substring(0, ix + 1) + s.replaceAll("\\.", "").replaceAll("_", "");
        }
        ix = 0;
        while (ix < v.length() && (Character.isDigit(v.charAt(ix)) ||
                                   v.charAt(ix) == '.')) {
            ix++;
        }
        v = v.substring(0, ix);
        return Float.parseFloat(v);
    }

    public static boolean isPreviewable(String filename) {
        String name = filename.toLowerCase();
        return  name.endsWith(".jpg")   ||
                name.endsWith(".jpeg")  ||
                name.endsWith(".png")   ||
                name.endsWith(".bmp")   ||
                name.endsWith(".pdf") && Util.isMac() ||
                name.endsWith(".rtf") && Util.isMac() ||
                name.endsWith(".txt") && Util.isMac() ||
                name.endsWith(".tiff")  ||
                name.endsWith(".tif");
    }

    public static boolean isArchiveFileType(String filename) {
        String name = filename.toLowerCase();
        return  name.endsWith(".zip")   ||
                name.endsWith(".cbz")   ||
                name.endsWith(".rar")   ||
                name.endsWith(".cbr")   ||
                name.endsWith(".7z")    ||
                name.endsWith(".arj")   ||
                name.endsWith(".bz2")   ||
                name.endsWith(".cab")   ||
                name.endsWith(".lzh")   ||
                name.endsWith(".lha")   ||
                name.endsWith(".chm")   ||
                name.endsWith(".gzip")  ||
                name.endsWith(".bz2")   ||
                name.endsWith(".bzip2") ||
                name.endsWith(".gz")    ||
                name.endsWith(".tar")   ||
                name.endsWith(".tz")    ||
                name.endsWith(".tgz")   ||
                name.endsWith(".tbz")   ||
                name.endsWith(".tbz2")  ||
                name.endsWith(".rpm")   ||
                name.endsWith(".cpio")  ||
                name.endsWith(".cpgz")  ||
                name.endsWith(".iso")   ||
                name.endsWith(".nsis")  ||
                name.endsWith(".z")     ||
                name.endsWith(".war")   ||
                name.endsWith(".ear")   ||
                name.endsWith(".zap")   ||
                name.endsWith(".zpg")   ||
                name.endsWith(".jar");
    }

    public static boolean isCompositeArchive(String filename) {
        String name = filename.toLowerCase();
        int ix = name.indexOf(".tar.");
        return ix > 0 && isArchiveFileType(name.substring(ix + 1)) ||
                name.endsWith(".tz") ||
                name.endsWith(".tgz") ||
                name.endsWith(".tbz") ||
                name.endsWith(".tbz2") ||
                name.endsWith(".cpgz");
    }

    /**
     * creates document modal dialog and centers it in the owner window
     * @param parent window
     * @return modal JDialog
     */
    public static JDialog createDocumentModalDialog(final JFrame parent) {
        Throwable x = null;
        JDialog d = null;
        boolean documentModalSheet = Util.isMac() && Util.getJavaVersion() >= 1.6 && parent != null;
        if (documentModalSheet) {
            try {
                Class mt = Class.forName("java.awt.Dialog$ModalityType");
                Field dm = mt.getField("DOCUMENT_MODAL");
                Class[] sig = new Class[]{Window.class, mt};
                Constructor c = JDialog.class.getConstructor(sig);
                Object[] params = new Object[]{parent, dm.get(null)};
                d = (JDialog)c.newInstance(params);
            } catch (ClassNotFoundException e) { x = e;
            } catch (NoSuchFieldException e) { x = e;
            } catch (NoSuchMethodException e) { x = e;
            } catch (IllegalAccessException e) { x = e;
            } catch (InvocationTargetException e) { x = e;
            } catch (InstantiationException e) { x = e;
            }
            if (x != null) {
                throw new Error(x);
            }
        }
        if (d == null) {
            d = new JDialog(parent) {
                public void validate() {
                    super.validate();
                    if (getParent() != null) {
                        try {
                            setLocationRelativeTo(getParent());
                        } catch (Throwable t) {
                            // known not to work sometimes
                        }
                    }

                }
            };
            d.setModal(true);
        }
        final JDialog dlg = d;
        if (documentModalSheet) {
            parent.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
            dlg.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
        } else {
            // in case validate() overwrite fails:
            dlg.addComponentListener(new ComponentAdapter() {
                public void componentShown(ComponentEvent e) {
                    Window owner = parent != null ? parent : dlg.getOwner();
                    assert owner != null : "search for SwingUtilities.getSharedOwnerFrame() usage";
                    dlg.setLocationRelativeTo(owner);
                    dlg.removeComponentListener(this);
                }
            });
        }
        return dlg;
    }

    /**
     * @return localy unique id for the user
     */
    public static String luid() {
        return luid(1);
    }

    private static String luid(int inc) {
        assert IdlingEventQueue.isDispatchThread();
        Preferences user = Preferences.userNodeForPackage(Zipeg.class).node(System.getProperty("user.name"));
        int id = user.getInt("luid", 100000) + inc;
        user.putInt("luid", id);
        try { user.flush(); } catch (BackingStoreException e) { throw new Error(e); }
        return "" + id;
    }

    /**
     * See: http://www.opengroup.org/onlinepubs/009629399/apdxa.htm
     *      http://www.opengroup.org/dce/info/draft-leach-uuids-guids-01.txt
     * @return random UUID
     */
    public static String uuid() {
        return new UUID().toString();
    }

    private static class UUID {

        private static Random random = null;
        private long msb; // most significant bits
        private long lsb; // least significant bits

        UUID() {
            byte[] bytes = new byte[16];
            getRandom().nextBytes(bytes);
            bytes[6]  &= 0x0f;  /* clear version        */
            bytes[6]  |= 0x40;  /* set to version 4     */
            bytes[8]  &= 0x3f;  /* clear variant        */
            bytes[8]  |= 0x80;  /* set to IETF variant  */
            for (int i=0; i <  8; i++) msb = (msb << 8) | (bytes[i] & 0xff);
            for (int i=8; i < 16; i++) lsb = (lsb << 8) | (bytes[i] & 0xff);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof UUID)) return false;
            UUID id = (UUID)obj;
            return msb == id.msb && lsb == id.lsb;
        }

        public String toString() {
            return (hex(msb >> 32, 8) + "-" + hex(msb >> 16, 4) + "-" +
                    hex(msb, 4) + "-" + hex(lsb >> 48, 4) + "-" + hex(lsb, 12));
        }

        private static String hex(long val, int digits) {
            long hi = 1L << (digits * 4); // make sure we have leading zeros
            return Long.toHexString(hi | (val & (hi - 1))).substring(1);
        }

        private static Random getRandom() {
            if (random == null) {
                try {
                    random = SecureRandom.getInstance("SHA1PRNG", "SUN");
                } catch (Throwable e) {
                    random = new Random();
                }
            }
            return random;
        }
    }

}
