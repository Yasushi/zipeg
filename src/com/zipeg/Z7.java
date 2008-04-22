package com.zipeg;

import org.mozilla.universalchardet.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.nio.charset.*;

public class Z7 implements Archiver {

    private Zip7Entry[] entries;
    private long archive;
    private String filename;
    private String encoding;
    private boolean utf8; // decode as UTF8
    private boolean sbc;  // single byte encoding
    private Map map;
    private boolean encrypted;
    private int decodeErrorCount = 0;
    private char[] nameBuffer = new char[16*1024];
    private static boolean loaded;

    public Z7(File file) throws IOException {
        this(file, null);
    }

    public Z7(File file, String password) throws IOException {
        if (archive != 0) {
            throw new IOException("archive already opened");
        }
        filename = Util.getCanonicalPath(file);
        if (!file.canRead()) {
            throw new IOException("cannot read \"" + filename + "\"");
        }
        archive = openArchive(filename, password == null ? "" : password);
        if (archive == 0) {
            throw new IOException("unsupported format");
        }
        try {
            if (needsPassword(archive) != 0) {
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            Zipeg.redownload();
        }
        long n = getArchiveSize(archive);
        if (n >= Integer.MAX_VALUE / 2) {
            throw new IOException("archive is too big");
        }
        if (n <= 0) {
            throw new IOException("archive is empty");
        }
        entries = new Zip7Entry[(int)n];
        map = new LinkedHashMap(entries.length + entries.length / 3, 0.75f, false);
        boolean[] b = new boolean[5];
        long[] d = new long[9];
        String[] s = new String[6];
        char[][] chars = new char[(int)n][];
        sbc = true; // single byte encoding
        for (int i = 0; i < n; i++) {
            getItem(archive, i, b, d, s);
            String path = s[kPath];
            chars[i] = path.toCharArray();
            sbc = sbc && isSBC(chars[i]);
        }
        if (!Flags.getFlag(Flags.FORCE_ENCODING)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            for (int i = 0; i < n && bytes.size() < 16 * 1024; i++) {
                append(bytes, chars[i], sbc);
            }
            String en = getCharset(bytes.toByteArray());
            Debug.traceln("encoding: " + en + " sbc=" + sbc);
            Charset cs = en == null ? null : forName(en);
            if (sbc && cs == null && file.getName().toLowerCase().endsWith("zip")) {
                cs = guessForLegacyZips();
            }
            encoding = cs != null ? cs.name() : Presets.get("encoding", "UTF-8");
        } else {
            encoding = Presets.get("encoding", "UTF-8");
        }
        utf8 = "UTF-8".equalsIgnoreCase(encoding);
        boolean win = Util.isWindows();
        for (int i = 0; i < n; i++) {
            getItem(archive, i, b, d, s);
            String path = s[kPath];
            String name = decodeFileName(path);
            if (win) {
                name = decodeWindowsName(name);
            }
            if (b[kIsFolder] && !name.endsWith("/")) {
                name += '/';
            }
            Zip7Entry e = new Zip7Entry(name);
            e.index = i;
            e.isFolder = b[kIsFolder];
            e.isAnti = b[kIsAnti];
            e.isSolid = b[kIsSolid];
            e.isEncrypted = b[kIsEncrypted];
            e.isCommented = b[kIsCommented];
            e.setSize(d[kSize]);
            e.setCompressedSize(d[kPackedSize]);
            e.creationTime = d[kCreationTime];
            e.setTime(d[kLastWriteTime]);
            e.lastAccessTime = d[kLastAccessTime];
            e.attributes = d[kAttributes];
            e.position = d[kPosition];
            e.block = d[kBlock];
            e.setCrc(d[kCRC]);
            e.method7 = s[kMethod];
            e.setComment(s[kComment]);
            e.hostOS = s[kHostOS];
            e.user = s[kUser];
            e.group = s[kGroup];
            entries[i] = e;
            map.put(name, e);
            encrypted = encrypted || e.isEncrypted;
        }
    }

    public boolean isEncrypted() {
        return needsPassword(archive) != 0 || encrypted;
    }

    public boolean isDirEncrypted() {
        return needsPassword(archive) != 0;
    }

    public void close() {
        if (archive != 0) {
            closeArchive(archive);
            entries = null;
            archive = 0;
        }
    }

    public int size() {
        return archive == 0 ? 0 : entries.length;
    }

    public ZipEntry getEntry(String path) {
        return (ZipEntry)map.get(path);
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return null; // TODO: not implemented
    }

    public Iterator getEntries() {
        return new Iterator() {

            int size = size();
            int i = 0;

            public boolean hasNext() {
                return i < size;
            }

            public Object next() {
                return entries[i++];
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    public void extractEntry(ZipEntry e, File file, String pwd) throws IOException {
        if (!(e instanceof Zip7Entry)) {
            assert e.isDirectory() : "placeholder entry is not for directory: " + e.getName();
            if (e.isDirectory()) {
                file.mkdirs();
            } else {
                file.createNewFile();
            }
        } else {
            Zip7Entry z7e = (Zip7Entry)e;
            boolean exists = file.exists();
            File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
                // TODO: do it for all non-existing parents
                ZipEntry p = getEntry(new File(z7e.getName()).getParent());
                if (p != null) {
                    parent.setLastModified(p.getTime());
                }
            }
            long r = extractItem(archive, z7e.index,
                                 Util.getCanonicalPath(file), pwd == null ? "" : pwd);
            if (r != 0) {
                if (!exists) {
                    file.delete();
                }
                z7e.error = getMessage(r);
                throw new IOException(z7e.error + " extracting item \"" + e.getName() + "\"");
            }
        }
    }

    public String getName() {
        return filename;
    }

    public class Zip7Entry extends ZipEntry {
        private long index;
        private boolean isFolder;
        private boolean isAnti;
        private boolean isSolid;
        private boolean isEncrypted;
        private boolean isCommented;
        private long creationTime;
        private long lastAccessTime;
        private long attributes;
        private long position;
        private long block;
        private long time7; // to avoid Java2Dos convertion
        private String method7;
        private String hostOS;
        private String user;
        private String group;
        private long size7;
        private String error;

        public Zip7Entry(String name) {
            super(name);
        }

        public void setTime(long time) {
            time7 = time;
        }

        public long getTime() {
            return time7;
        }

        public void setSize(long size) {
            size7 = size;
        }

        public long getSize() {
            return size7;
        }

        public boolean isAnti() {
            return isAnti;
        }

        public boolean isSolid() {
            return isSolid;
        }

        public boolean isEncrypted() {
            return isEncrypted;
        }

        public boolean isCommented() {
            return isCommented;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public long getAttributes() {
            return attributes;
        }

        public long getPosition() {
            return position;
        }

        public long getBlock() {
            return block;
        }

        public String getHostOS() {
            return hostOS;
        }

        public String getUser() {
            return user;
        }

        public String getGroup() {
            return group;
        }

        public String getMethodString() {
            return method7;
        }

        /**
         * @return not null string if item had a decompression error
         */
        public String getError() {
            return error;
        }


        public String toString() {
            return "Zip7Entry[\"" + getName() + (isFolder ? "\" dir " : "\" ") +
                    size() + " packed " + getCompressedSize() +
                    " " + new Date(getTime()) + "]";
        }
    }

    public static void loadLibrary() {
        assert IdlingEventQueue.isDispatchThread();
        if (!loaded) {
            try {
                System.loadLibrary(getLibraryName());
            } catch (UnsatisfiedLinkError e) {
                Zipeg.redownload();
            }
            loaded = true;
        }
    }

    public static String getLibraryName() {
        String p = Util.getProcessor();
        if (Util.isMac()) {
            String cpu = "powerpc".equals(p) ? "ppc" : p;
            return "7za-osx-" + cpu;
        } else {
            // TODO: Linux?
            return "7za-win-i386";
//          return "7za-win-i386-dbg";
        }
    }

    // booleans:
    private static final int kIsFolder = 0;
    private static final int kIsAnti = 1;
    private static final int kIsSolid = 2;
    private static final int kIsEncrypted = 3;
    private static final int kIsCommented = 4;
    // longs:
    private static final int kSize = 0;              // UI8
    private static final int kPackedSize = 1;        // UI8
    private static final int kCreationTime = 2;      // FILETIME
    private static final int kLastWriteTime = 3;     // FILETIME
    private static final int kLastAccessTime = 4;    // FILETIME
    private static final int kAttributes = 5;        // UI4
    private static final int kPosition = 6;          // UI4
    private static final int kBlock = 7;             // UI4
    private static final int kCRC = 8;               // UI4
    // strings:
    private static final int kPath = 0;
    private static final int kMethod = 1;
    private static final int kComment = 2;
    private static final int kHostOS = 3;
    private static final int kUser = 4;
    private static final int kGroup = 5;

    private native long openArchive(String path, String password);
    private native long needsPassword(long a); // RAR 3.0 encrypted filenames
    private native void closeArchive(long a);
    private native long getArchiveSize(long a);
    private native void getItem(long a, long ix, boolean[] bools, long[] longs, Object[] strings);
    private native long extractItem(long a, long ix, String filepath, String password);

    private static String getMessage(long r) {
        // see 7zip :: IArchive.h
        if (r == 1) { // NArchive::NExtract::NOperationResult::kUnSupportedMethod:
            return "Unsupported Method of Compression";
        } else if (r  == 2) { // NArchive::NExtract::NOperationResult::kCRCError:
            return "Data Error";
        } else if (r == 3) { // NArchive::NExtract::NOperationResult::kDataError:
            return "CRC Error";
        } else if (r == 0x8007000E) { // E_OUTOFMEMORY
            return "Out of Memory";
        } else if (r == 0x80004005) { // E_FAIL
            return "Generic Failure";
        } else if (r == 0x80004004) { // E_ABORT
            return "Aborted";
        } else if (r == 0x80020009) { // DISP_E_EXCEPTION
            return "Internal Invocation Exception";
        } else {
            return "Unknown Error 0x" + Long.toHexString(r);
        }

    }

    /* unused\de\spieleck\app\ngramj\lm\CategorizerImpl.java actually
       can be used to determine the language of the filenames (and it works).
       But it is heave machinery. For now the simplest aproach is to resort
       to IBM850 which covers all most of the bases not covered by universalchardet.
     */

    private static Charset guessForLegacyZips() {
        // http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt
        // http://www.kostis.net/charsets/trans130/cpdos.htm
        String lang = System.getProperty("user.language").toLowerCase().substring(0, 2);
        // language is better guess than country e.g. in Switzerland they speak three languages
        if ("el".equals(lang)) { // Greek
            return forName("IBM737");
        } else if ("de".equals(lang) || "da".equals(lang) || "nl".equals(lang) || "fi".equals(lang) ||
                "fo".equals(lang) || "fr".equals(lang) || "is".equals(lang) || "it".equals(lang) ||
                "es".equals(lang) || "ga".equals(lang) || "no".equals(lang) || "pt".equals(lang) ||
                "sv".equals(lang)) {
            // Danish, Dutch, English, Faeroese, Finnish, French, German, Icelandic,
            // Irish, Italian, Norwegian, Portuguese, Spanish and Swedish.
            return forName("IBM850");
        } else if ("sq".equals(lang) || "cs".equals(lang) || "hu".equals(lang) || "pl".equals(lang) ||
                "ro".equals(lang) || "sh".equals(lang) || "sk".equals(lang) ||
                "sl".equals(lang)) {
            // Albanian, Czech, Hungarian, Polish, Romanian, (Serbo-)Croatian, Slovak, Slovenian
            return forName("IBM852");
        } else if ("af".equals(lang) || "ca".equals(lang) || "eo".equals(lang) || "gl".equals(lang) ||
                "mt".equals(lang) || "tr".equals(lang)) {
            // Afrikaans, Catalan, Esperanto, Galician, Maltese, Turkish.
            return forName("IBM853");
        } else if ("ar".equals(lang)) {
            return forName("IBM864");
        } else {
/*
            if (Util.getJavaVersion() >= 1.5) {
                return (Charset)Util.callStatic("java.nio.charset.Charset.defaultCharset", Util.VOID);
            }
*/          // practically the IBM850 is better choice even when than defaultCharset
            // think that sbc "english" charset is nested inside IBM855 Western European
            return forName("IBM850"); // most common for Western Euripean ZIPs
        }
    }

    private static Charset forName(String name) {
        try {
            return Charset.forName(name);
        } catch (UnsupportedCharsetException x) {
            /* ignore */
        }
        try { // 1.4 does not support "IBMxxx" charsets. Do it yourself approach.
            int cp = 0;
            if (name.toUpperCase().startsWith("IBM")) {
                cp = Integer.decode(name.substring(3)).intValue();
            }
            return cp == 0 ? null : new IBMCharset(cp);
        } catch (Exception x) {
            return null;
        }
    }

    private static boolean isSBC(char[] a) {
        for (int i = 0; i < a.length; i++) {
            int c = (a[i]) & 0xFFFF;
            if (c < 0 || c > 255) {
                return false;
            }
        }
        return true;
    }

    private void append(ByteArrayOutputStream bytes, char[] chars, boolean isSBC) {
        if (isSBC) {
            for (int i = 0; i < chars.length; i++) {
                bytes.write((byte)chars[i]);
            }
        } else {
            for (int i = 0; i < chars.length; i++) {
                bytes.write((byte)chars[i]);
                bytes.write((byte)(chars[i] >> 8));
            }
        }
        bytes.write(' ');
    }

    private static String getCharset(byte[] bytes) {
        final String[] r = new String[]{null};
        UniversalDetector detector = new UniversalDetector(
                new CharsetListener() {
                    public void report(String name) {
                        Debug.traceln("charset = " + name);
                        r[0] = name;
                    }
                }
        );
        for (int i = 0; i < bytes.length && r[0] == null; i += 1024) {
            int len = i + 1024 < bytes.length ? 1024 : bytes.length - i;
            detector.handleData(bytes, i, len);
        }
        detector.dataEnd();
        return r[0];
    }

    private static final char[] INVALID = ":*?|\"<>\\".toCharArray();

    private String decodeFileName(String s) {
        try {
            if (!sbc) {
                // TODO: may be necessry to do UTF-16BE vs UTF-16LE
                return s; // utf-16 (aka UNICODE)
            } else if (utf8) {
                byte[] bytes = new byte[s.length()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte)s.charAt(i);
                }
                s = new String(bytes, "UTF-8");
            } else {
                s = new String(s.getBytes("ISO-8859-1"), encoding);
            }
        } catch (UnsupportedEncodingException e) {
            /* ignore - will return the same filename */
        }
        return s;
    }

    private String decodeWindowsName(String s) {
        s = s.trim(); // windows names do not have leading/trailing spaces
        int len = s.length();
        if (s.indexOf('\\') >= 0) {
            if (nameBuffer.length < len) {
                nameBuffer = new char[len * 2];
            }
            s.getChars(0, len, nameBuffer, 0);
            for (int k = 0; k < len; k++) {
                if (nameBuffer[k] == '\\') {
                    nameBuffer[k] = '/';
                }
            }
            s = new String(nameBuffer, 0, len);
        }
        // some archive paths do start with "C:/" contrary to the spec
        if (s.length() > 3 && s.substring(1,3).equals(":/")) {
            s = s.substring(2);
        }
        boolean invalid = false;
        for (int i = 0; i < INVALID.length; i++) {
            if (s.indexOf(INVALID[i]) >= 0) {
                s = s.replace(INVALID[i], '_');
                invalid = true;
            }
        }
        if (invalid) {
            int ix = s.lastIndexOf('.');
            if (ix > 0) {
                decodeErrorCount++;
                assert s.equals(s.substring(0, ix) + s.substring(ix));
                File test = new File(Util.getCacheDirectory(), s);
                try {
                    test.getCanonicalPath();
                    s = s.substring(0, ix) + "(" + decodeErrorCount + ")" + s.substring(ix);
                } catch (IOException e) {
                    s = "unable_to_decode_file_name_(" + decodeErrorCount + ")" + s.substring(ix);
                }
            }
        }
        return s;
    }

}
