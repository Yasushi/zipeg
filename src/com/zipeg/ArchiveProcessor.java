package com.zipeg;

import javax.swing.tree.TreeNode;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.io.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

public final class ArchiveProcessor implements Archive {

    private static final String ROOT = "/";
    private static final String MACOSX = "__MACOSX/";
    private static final Integer ZERO = new Integer(0);
    private static final int YES = 0, NO = 1, ALL = 2, NONE = 3;
    private static final ArrayList emptyArrayList = new ArrayList();
    private ArchiveData data;
    private final LinkedList queue = new LinkedList();
    private final Thread thread;
    private float lastProgress;

    private static class IntValue {
        int value;
    }

    private static class LongValue {
        long value;
    }

    private static class ArchiveData {
        Archiver zipfile;
        String password;
        File cache;
        ArchiveTreeNode root;
        String[] entries;
        Map children;   // ix -> Set(ix)
        Map extensions; // ".ext" -> IntValue
        // number of children that are directories
        Map dirsCount;  // ix -> IntValue
        // number of children that are directories
        Map filesCount;  // ix -> IntValue
        // sum of size() of all non-directories children
        Map fileBytes;  // ix -> LongValue
        File parent;  // parent archive for nested archives
    }

    private ArchiveProcessor() {
        Z7.loadLibrary();
        thread = new Thread(new Runnable() {
            public void run() {
                Debug.execute(new Runnable() {
                    public void run() {
                        doWork();
                        IdlingEventQueue.reportThreadExit();
                    }
                });
            }
        });
        thread.setName("ArchiveProcessor");
        thread.setDaemon(false);
        // wait till the thread has started and ready to recieve commands
        synchronized (queue) {
            try {
                thread.start();
                queue.wait();
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }
    }

    public int getExtensionCounter(String dotExtension) {
        assert IdlingEventQueue.isDispatchThread();
        IntValue c = (IntValue)data.extensions.get(dotExtension);
        return c == null ? 0 : c.value;
    }

    public void close() {
        assert IdlingEventQueue.isDispatchThread();
        enqueue("stop");
        try {
            synchronized (thread) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    public boolean isOpen() {
        return data != null;
    }

    public boolean isNested() {
        return data != null && data.parent != null;
    }

    public String getParentName() {
        return data != null && data.parent != null ? Util.getCanonicalPath(data.parent) : getName();
    }

    public TreeNode getRoot() {
        assert IdlingEventQueue.isDispatchThread();
        return data.root;
    }

    public String getName() {
        assert IdlingEventQueue.isDispatchThread();
        return data == null || data.zipfile == null ? "" : data.zipfile.getName();
    }

    public boolean isEncrypted() {
        assert IdlingEventQueue.isDispatchThread();
        return !(data == null || data.zipfile == null) && data.zipfile.isEncrypted();
    }

    public File getCacheDirectory() {
        assert IdlingEventQueue.isDispatchThread();
        return data.cache;
    }

    public void extract(List treeElements, File directory, boolean quit) {
        assert IdlingEventQueue.isDispatchThread();
        enqueue("extract", new Object[]{treeElements, directory, quit ? Boolean.TRUE : Boolean.FALSE});
    }

    public void extract(TreeElement element, Runnable done) {
        assert IdlingEventQueue.isDispatchThread();
        enqueue("extract", new Object[]{element, done, Boolean.FALSE});
    }

    public void extractAndOpen(TreeElement element) {
        assert IdlingEventQueue.isDispatchThread();
        enqueue("extractAndOpen", new Object[]{element, createCacheDirectory()});
    }

    private void doWork() {
        assert !IdlingEventQueue.isDispatchThread();
        try {
            synchronized (queue) {
                queue.notifyAll(); // notify constructor
                assert queue.isEmpty();
                queue.wait();
            }
            boolean die = false;
            while (!die) {
                try {
                    IdlingEventQueue.reportThreadIsWorking();
                    for (;;) {
                        Object r;
                        Object[] params = null;
                        synchronized (queue) {
                            r = queue.isEmpty() ? null : queue.removeFirst();
                            if ("open".equals(r) || "extract".equals(r) || "extractAndOpen".equals(r)) {
                                params = (Object[])queue.removeFirst();
                            }
                        }
                        if (r == null) {
                            break;
                        } else if ("stop".equals(r)) {
                            die = true;
                            break;
                        } else if ("open".equals(r)) {
                            File file  = (File)params[0];
                            File cache = (File)params[1];
                            File parent = (File)params[2];
                            load(file, cache, parent);
                            if (data == null) {
                                die = true;
                                break;
                            }
                        } else if ("extract".equals(r)) {
                            if (params[1] instanceof File) {
                                doExtract((List)params[0], (File)params[1], (Boolean)params[2]);
                            } else {
                                doExtract((TreeElement)params[0], (Runnable)params[1]);
                            }
                        } else if ("extractAndOpen".equals(r)) {
                            List list = new ArrayList(1);
                            list.add(params[0]);
                            doExtract(list, (File)params[1], null, true, Boolean.FALSE);
                        } else {
                            assert false : r;
                        }
                    }
                    IdlingEventQueue.reportThreadIsIdling();
                    if (!die) {
                        synchronized (queue) {
                            if (queue.isEmpty()) {
                                queue.wait();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Actions.reportError(e.getMessage());
                    throw new Error(e);
                }
            }
        } catch (Throwable e) {
            while (e.getCause() != null) {
                e = e.getCause();
            }
            e.printStackTrace();
            String error = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            Actions.reportError("Fatal error: " + error);
            final Error rethrow = new Error(e);
            IdlingEventQueue.invokeLater(new Runnable(){
                public void run() { throw rethrow; }
            });
        } finally {
            closeZipFile();
        }
    }

    private void closeZipFile() {
        assert !IdlingEventQueue.isDispatchThread() : "must be called from background thread only";
        if (data != null) {
            File zip = data.zipfile != null ? new File(data.zipfile.getName()) : null;
            if (data.zipfile != null) {
                try {
                    data.zipfile.close();
                } catch (IOException e) {
                    Actions.reportError("failed to close " + e.getMessage());
                }
            }
            if (isNested() && zip != null) {
                boolean b = zip.delete(); // after close
                Debug.traceln("deleted nested archive: (" + b + ") " + zip);
            }
            if (data.cache != null) {
                Util.rmdirs(data.cache);
            }
            data.root = null;
            data.zipfile = null;
            data = null;
        }
    }

    private void enqueue(Object o) {
        synchronized (queue) {
            queue.add(o);
            queue.notifyAll();
        }
    }

    private void enqueue(Object o1, Object o2) {
        synchronized (queue) {
            queue.add(o1);
            queue.add(o2);
            queue.notifyAll();
        }
    }

    public static void open(File file) {
        open(file, null);
    }

    private static void open(File file, File parent) {
        open(file, createCacheDirectory(), parent);
    }

    private static void open(File file, File cache, File parent) {
        new ArchiveProcessor().enqueue("open", new Object[]{file, cache, parent});
    }

    private void setProgress(int i, int max) {
        assert !IdlingEventQueue.isDispatchThread();
        assert i <= max;
        float f = i / (float)max;
        if (f > lastProgress + 0.01) {
            Actions.postEvent("setProgress", new Float(f));
            lastProgress = f;
        }
    }

    private static File createCacheDirectory() {
        assert IdlingEventQueue.isDispatchThread();
        File cache = Util.getCacheDirectory();
        int retry = 32;
        while (retry > 0) {
            File cacheDirectory = new File(cache, Util.luid());
            if (!cacheDirectory.exists()) {
                cacheDirectory.mkdirs();
                if (cacheDirectory.isDirectory()) {
                    return cacheDirectory;
                }
            }
            retry--;
        }
        return null;
    }

    private void load(File file, File cache, File parentArchive) {
        assert !IdlingEventQueue.isDispatchThread();
        lastProgress = 0;
        Actions.postEvent("setStatus", "opening archive: " + file + " ...");
        Actions.postEvent("setProgress", new Float(0.01));
        long time = System.currentTimeMillis();
        int entryCount = 0;
        try {
            data = new ArchiveData();
            data.zipfile = new Z7(file);
            data.parent = parentArchive;
            data.cache = cache;
            if (data.zipfile.isDirEncrypted() || data.zipfile.isEncrypted()) {
                data.password = askPassword();
                if (data.zipfile.isDirEncrypted()) {
                    data.zipfile.close();
                    data.zipfile = new Z7(file, data.password);
                }
            }
            entryCount = data.zipfile.size();
            int m = (entryCount + 1) * 2;
            int i = 1;
            int maxLength = ROOT.length();
            data.entries = new String[entryCount + 1];
            data.entries[0] = ROOT; // root
            // assume about 10 files per directory (experimental result)
            // and double the hashmap size to minimize collisions and rehash
            HashMap dirs = new HashMap(Math.min(10, entryCount / 5));
            dirs.put(ROOT, ZERO);
            int r = 0; // number of resource forks in archive
            for (Iterator e = data.zipfile.getEntries(); e.hasNext();) {
                try {
                    ZipEntry entry = (ZipEntry)e.next();
                    String name = entry != null ? entry.getName() : null;
                    // the is PKZIP "optimization" for Zero length files
                    // that stores the names but not entries
                    if (entry != null && name != null && data.zipfile.getEntry(name) != null) {
                        int n = name.length();
                        maxLength = Math.max(maxLength, n);
                        boolean dir = name.charAt(n - 1) == '/';
                        boolean skip = isAppleDouble(name) || isDSStore(name);
                        if (!skip) {
                            if (Util.isMac()) {
                                assert !name.startsWith(MACOSX);
                            }
                            if (dir && dirs.containsKey(name)) {
                                // such zip files happend to be in the field.
                                // skip double entry for the dirctory.
                            }
                            else {
                                data.entries[i] = name;
                                if (dir) {
                                    dirs.put(name, new Integer(i));
                                }
                                i++;
                                setProgress(i, m);
                            }
                        } else {
                            r++;
                        }
                    }
                } catch (InternalError ie) {
                    break;
                }
            }
            m -= r;
            int N = i;
            char[] buff = new char[maxLength];
            // some archives may have parent directories missing
            int j = i;
            Map extra = new HashMap();
            for (int k = 0; k < N; k++) {
                String name = data.entries[k];
                int n = name.length();
                name.getChars(0, n, buff, 0);
                String parent = getParent(name, buff, n);
                while (!ROOT.equals(parent) && !extra.containsKey(parent) &&
                                               !dirs.containsKey(parent)) {
                    extra.put(parent, new Integer(j));
                    j++;
                    parent = getParent(parent, buff, parent.length());
                }
            }
            if (extra.size() > 0 || N != data.entries.length) {
                String[] entries = new String[N + extra.size()];
                System.arraycopy(data.entries, 0, entries, 0, N);
                for (Iterator x = extra.entrySet().iterator(); x.hasNext();) {
                    Map.Entry e = (Map.Entry)x.next();
                    String name = (String)e.getKey();
                    Integer ix = (Integer)e.getValue();
                    assert entries[ix.intValue()] == null;
                    entries[ix.intValue()] = name;
                    assert !dirs.containsKey(name);
                    dirs.put(name, ix);
                }
                data.entries = entries;
                m += extra.size();
            }
            ArrayList sorted = new ArrayList(dirs.size());
            sorted.addAll(dirs.keySet());
            Collections.sort(sorted);
            int d32 = dirs.size() * 3 / 2; // 1.5 of dirs.size()
            data.children = new HashMap(d32);
            data.dirsCount = new HashMap(d32);
            data.fileBytes = new HashMap(d32);
            data.filesCount = new HashMap(d32);
            data.extensions = new HashMap(1024);
            for (int ix = 1; ix < data.entries.length; ix++) {
                Integer index = new Integer(ix);
                String name = data.entries[ix];
                int n = name.length();
                boolean dir = name.charAt(n - 1) == '/';
                name.getChars(0, n, buff, 0);
                String parent = getParent(name, buff, n);
                Integer pix = (Integer)dirs.get(parent);
                assert pix != null : "parent " + parent + " is missing";
                Set childs = (Set)data.children.get(pix);
                if (childs == null) {
                    data.children.put(pix, childs = new HashSet());
                }
                assert !childs.contains(index);
                childs.add(index);
                if (!dir) {
                    ZipEntry entry = data.zipfile.getEntry(name);
                    LongValue sum = (LongValue)data.fileBytes.get(pix);
                    if (sum == null) {
                        data.fileBytes.put(pix, sum = new LongValue());
                    }
                    sum.value += entry.getSize();
                } else {
                    IntValue cnt = (IntValue)data.dirsCount.get(pix);
                    if (cnt == null) {
                        data.dirsCount.put(pix, cnt = new IntValue());
                    }
                    cnt.value++;
                }
                String ext = getExtension(name, buff, n);
                if (ext != null) {
                    IntValue c = (IntValue)data.extensions.get(ext);
                    if (c == null) {
                        data.extensions.put(ext, c = new IntValue());
                    }
                    c.value++;
                }
                i++;
                setProgress(i, m);
            }
            // TODO: need to account for resource forks
            calculateCumulateCounts(ZERO);
            data.root = new ArchiveTreeNode(null, ZERO);
            data.root.fillCache();
            data.root.path = data.zipfile.getName();
            data.root.name = new File(data.root.path).getName();
            Actions.postEvent("archiveOpened", this);
        } catch (IOException io) {
            Actions.reportError("failed to open archive\n\"" + file + "\"\n" +
                                io.getMessage());
            closeZipFile();
        } catch (OutOfMemoryError oom) {
            Debug.releaseSafetyPool();
            Actions.reportError("failed to open archive\n\"" + file + "\"\n" +
                              "archive is too big. (" + entryCount + " items)");
            closeZipFile();
        } finally {
            Actions.postEvent("setProgress", new Float(0));
            Actions.postEvent("setStatus", "");
            Actions.postEvent("setInfo", "");
            time = System.currentTimeMillis() - time;
            if (data != null && data.zipfile != null) {
                Debug.traceln("entryCount " + entryCount + " time " + time + " milli");
            }
        }
    }

    private static boolean isAppleDouble(String name) {
        if (name.startsWith(MACOSX)) return true;
        int n = name.length();
        if (name.charAt(n - 1) == '/') return false;
        int k = n - 1;
        while (k >= 0 && name.charAt(k) != '/') {
            k--;
        }
        return k + 2 < n && name.charAt(k + 1) == '.' && name.charAt(k + 2) == '_';
    }

    private static boolean isDSStore(String name) {
        return !Util.isMac() && (name.endsWith("/.DS_Store") || ".DS_Store".equals(name));
    }

    private long getFileBytes(Integer ix) {
        if (data == null) {
            return 0;
        }
        LongValue bytes = (LongValue)data.fileBytes.get(ix);
        return bytes == null ? 0 : bytes.value;
    }

    private int getDirsCount(Integer ix) {
        if (data == null) {
            return 0;
        }
        IntValue c = (IntValue)data.dirsCount.get(ix);
        return c == null ? 0 : c.value;
    }

    private int getFilesCount(Integer ix) {
        if (data == null) {
            return 0;
        }
        IntValue c = (IntValue)data.filesCount.get(ix);
        if (c != null) return c.value;
        Set s = (Set)data.children.get(ix);
        return s == null ? 0 : s.size() - getDirsCount(ix);
    }

    private void calculateCumulateCounts(Integer ix) {
        if (data == null) {
            return;
        }
        Set c = (Set)data.children.get(ix);
        if (c == null) {
            return;
        }
        IntValue files = (IntValue)data.filesCount.get(ix);
        if (files == null) {
            long s = 0;
            int f = getFilesCount(ix);
            int d = 0;
            for (Iterator i = c.iterator(); i.hasNext();) {
                Integer cix = (Integer)i.next();
                calculateCumulateCounts(cix);
                s += getFileBytes(cix);
                f += getFilesCount(cix);
                d += getDirsCount(cix);
            }
            LongValue bytes = (LongValue)data.fileBytes.get(ix);
            if (bytes == null) {
                data.fileBytes.put(ix, bytes = new LongValue());
            }
            bytes.value += s;
            IntValue dirs = (IntValue)data.dirsCount.get(ix);
            if (dirs == null) {
                data.dirsCount.put(ix, dirs = new IntValue());
            }
            dirs.value += d;
            data.filesCount.put(ix, files = new IntValue());
            files.value += f;
        }
        assert data.fileBytes.get(ix) instanceof LongValue;
        assert data.filesCount.get(ix) instanceof IntValue;
        assert data.dirsCount.get(ix) instanceof IntValue;
    }

    private String getParent(String name, char[] buff, int n) {
        for (int i = n - 2; i >= 0; i--) {
            if (buff[i] == '/') {
                return name.substring(0, i + 1);
            }
        }
        return ROOT;
    }

    private String getExtension(String name, char[] buff, int n) {
        int m = Math.max(0, n - 8); // maximum extension length 8
        for (int i = n - 1; i >= m; i--) {
            if (buff[i] == '/') {
                return null;
            } else if (buff[i] == '.') {
                return name.substring(i);
            }
        }
        return "";
    }

    private void doExtract(final TreeElement e, Runnable done) {
        assert !IdlingEventQueue.isDispatchThread();
        assert e != null;
        List list = new LinkedList() {{ add(e); }};
        doExtract(list, data.cache, done, false, Boolean.FALSE);
    }

    private void doExtract(List list, File dir, Boolean quit) {
        assert !Util.getCanonicalPath(dir).startsWith(Util.getCanonicalPath(data.cache));
        doExtract(list, dir, null, false, quit);
    }

    private static String askPassword() throws IOException {
        final String[] r = new String[1];
        try {
            IdlingEventQueue.invokeAndWait(new Runnable(){
                public void run() {
                    r[0] = MainFrame.getPassword();
                }
            });
        } catch (InterruptedException e) {
            r[0] = null;
        } catch (InvocationTargetException e) {
            r[0] = null;
        }
        if (r[0] == null || r[0].length() == 0) {
            throw new IOException("password is required to open archive");
        }
        return r[0];
    }

    private Iterator iterateList(final List list) {

        return new Iterator() {

            final Iterator i = list.iterator();

            public boolean hasNext() {
                return i.hasNext();
            }

            public Object next() {
                return ((ArchiveTreeNode)i.next()).entry;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    private Iterator iterateAll() {

        return new Iterator() {

            Iterator e = data.zipfile.getEntries();
            ZipEntry next = e.hasNext() ? (ZipEntry)e.next() : null;
            {
                skipDirs();
            }

            public boolean hasNext() {
                return next != null;
            }

            public Object next() {
                Object r = next;
                next = e.hasNext() ? (ZipEntry)e.next() : null;
                skipDirs();
                return r;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            private void skipDirs() {
                while (next != null && (next.isDirectory() || isAppleDouble(next.getName()))) {
                    next = e.hasNext() ? (ZipEntry)e.next() : null;
                }
            }

        };
    }

    private Iterator iterateAllDirs() {

        return new Iterator() {

            Iterator e = data.zipfile.getEntries();
            ZipEntry next = e.hasNext() ? (ZipEntry)e.next() : null;
            {
                skipNoneDirs();
            }

            public boolean hasNext() {
                return next != null;
            }

            public Object next() {
                Object r = next;
                next = e.hasNext() ? (ZipEntry)e.next() : null;
                skipNoneDirs();
                return r;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            private void skipNoneDirs() {
                while (next != null && (!next.isDirectory() || isAppleDouble(next.getName()))) {
                    next = e.hasNext() ? (ZipEntry)e.next() : null;
                }
            }

        };
    }

    private void doExtract(List list, final File dir, Runnable done, boolean open, Boolean quit) {
        assert !IdlingEventQueue.isDispatchThread();
        String error = null;
        Set set = null;
        if (list != null) {
            set = new HashSet(list.size() * 2);
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                TreeElement e = (TreeElement)i.next();
                assert !e.isDirectory() : e.getFile();
                set.add(e.getFile());
            }
        }
        int n = list == null ? data.zipfile.size() : set.size();
        long time = System.currentTimeMillis();
        File created = null; // last created file;
        String filename = data.zipfile.getName(); // for error reporting
        String message = "";
        int[] lastPropmt = new int[]{done == null ? YES : ALL};
        int extracted = 0;
        int resources = 0;
        try {
            if (done == null) {
                Actions.postEvent("setStatus", "extracting files...");
                Actions.postEvent("setInfo", "");
                Actions.postEvent("setProgress", new Float(0.01));
            }
            int i = 0;
            float last = 0;
            Iterator e = list != null && list.size() < data.entries.length / 2 ?
                         iterateList(list) : iterateAll();
            while (e.hasNext() && (set == null || !set.isEmpty())) {
                ZipEntry entry = (ZipEntry)e.next();
                assert !entry.getName().startsWith(MACOSX);
                assert !entry.isDirectory();
                boolean unpack = set == null || set.contains(entry.getName());
                if (set != null) {
                    set.remove(entry.getName());
                }
                if (unpack && !entry.isDirectory()) {
                    i++;
                    float progress = (float)i / n;
                    if (done == null && progress > last + 0.01) {
                        Actions.postEvent("setStatus", entry.getName());
                        Actions.postEvent("setInfo", (int)(100 * progress) + "%");
                        Actions.postEvent("setProgress", new Float(progress));
                        last = progress;
                    }
                    final File file = new File(dir, entry.getName());
                    filename = Util.getCanonicalPath(file);
                    if (done != null) {
                        created = file;
                    }
                    assert !file.getName().startsWith("._");
                    ZipEntry res = null;
                    // use cached file as a source only if:
                    // 1. No resource fork or dir attrs on OSX
                    // 2. It is not extraction into cache.
                    File cached = done == null && res == null ?
                                  new File(data.cache, entry.getName()) : null;
                    if (filename.startsWith(Util.getCanonicalPath(data.cache))) {
                        assert cached == null : "paranoia";
                    }
                    if (Util.isMac()) {
                        res = getResourceFork(entry.getName());
                    }
                    extracted += unzip(entry, cached, file, lastPropmt);
                    if (lastPropmt[0] == NONE) {
                        break;
                    }
                    if (res != null) {
                        String rpath = res.getName();
                        if (rpath.startsWith(MACOSX)) {
                            rpath = rpath.substring(MACOSX.length());
                        }
                        File rfile = new File(dir, rpath);
                        resources += unzip(res, null, rfile, lastPropmt);
                    }
                }
            }
            if (resources > 0) {
                Process p = Runtime.getRuntime().exec(
                            new String[]
                            {"/System/Library/CoreServices/FixupResourceForks",
                             "-q", Util.getCanonicalPath(dir)},
                            Util.getEnvFilterOutMacCocoaCFProcessPath());
                int ec = p.waitFor();
                if (ec != 0) {
                    message = "error: fixing up resource forks for " +
                               Util.getCanonicalPath(dir) + " error code " + ec;
                    extracted -= resources;
                }
            }
            // process empty directories:
            if (list == null) {
                for (Iterator dirs = iterateAllDirs(); dirs.hasNext(); ) {
                    ZipEntry entry = (ZipEntry)dirs.next();
                    final File file = new File(dir, entry.getName());
                    try {
                        data.zipfile.extractEntry(entry, file, data.password);
                    } catch (IOException ignore) {
                        // ignore
                    }
                    if (!file.exists()) {
                        filename = Util.getCanonicalPath(file);
                        new File(filename).mkdirs();
                    }
                }
            }
            if (extracted > 0 && message.length() == 0) {
                message = "Successfully Extracted " + Util.plural(extracted, "item");
            }
        } catch (IOException io) {
            if (done == null) { // all errors are silently ignored for the cache extraction
                error = io.getMessage();
                if (lastPropmt[0] != NONE) {
                    Actions.reportError("failed to extract file\n\"" + filename + "\"\n" +
                                      error);
                    message = "error: " + error;
                    if (extracted > 0) {
                        message += "<br><font color=green>extracted " +
                                    Util.plural(extracted, "item") + ".</font>";
                    }
                } else {
                    message = "error: " + error;
                }
            } else { // done != null file was requested for the cache
                if (created != null && created.exists()) {
                    created.delete();
                }
            }
        } catch (InterruptedException e) {
            throw new Error(e);
        } finally {
            if (open && error == null) {
                assert done == null;
                // IMPORTANT: Do not setProgress to 0.0 and do NOT postEvent("extractionCompleted")
                // if the QuitAfterExtract option is set this will lead to application exiting.
                Actions.postEvent("setProgress", new Float(0.5));
                Actions.postEvent("setStatus", "");
                Actions.postEvent("setInfo", "");
                ArchiveTreeNode node = (ArchiveTreeNode)list.get(0);
                final File file = new File(dir, node.entry.getName());
                final File parent = isNested() ? data.parent : new File(data.zipfile.getName());
                try {
                    IdlingEventQueue.invokeLater(new Runnable() {
                        public void run() {
                            open(file, dir, parent);
                        }
                    });
                } catch (Exception e) {
                    // ignore because cannot throw here anymore
                }
            } else if (done == null) {
                Actions.postEvent("setProgress", new Float(0));
                Actions.postEvent("setStatus", "");
                Actions.postEvent("setInfo", "");
                Actions.postEvent("extractionCompleted", new Object[]{error, quit});
                Actions.postEvent("setMessage", message);
            } else {
                done.run();
            }
            if (Debug.isDebug()) {
                time = System.currentTimeMillis() - time;
                if (list != null && list.size() == 1) {
                    Debug.traceln("extract time " + time + " milli for " + list.get(0));
                } else {
                    Debug.traceln("extract time " + time + " milli");
                }
            }
        }
    }

    private int unzip(ZipEntry entry, File cached, File file, int[] lastPropmt)
            throws IOException {
        if (file.exists() && lastPropmt[0] != ALL) {
            lastPropmt[0] = askOverwrite(file);
            if (lastPropmt[0] == NONE) {
                throw new IOException("canceled by user request");
            }
            if (lastPropmt[0] == NO) {
                return 0;
            }
        }
        if (file.exists()) {
            assert !file.getAbsolutePath().startsWith(data.cache.getAbsolutePath()) :
                    "unziping temp file twice; why?";
            moveToTrash(file);
        }
        // assert !file.exists() : "moveToTrashFailed for " + file;
        if (file.exists()) {
            // both moveToTrash() and force delete() failed
            throw new IOException("file is locked.");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs(); // TODO: set time from archive
        }
        if (cached != null && cached.canRead()) {
            Util.copyFile(cached, file);
        } else if (entry instanceof Z7.Zip7Entry) {
            Z7.Zip7Entry z7e = (Z7.Zip7Entry)entry;
            data.zipfile.extractEntry(z7e, file, z7e.isEncrypted() ? data.password : null);
        } else {
            data.zipfile.extractEntry(entry, file, data.password);
        }
        if (entry.getTime() > 0) {
            file.setLastModified(entry.getTime());
        }
/*
        TODO: renameTo does not work :-(
        if (Util.isMac() && "Icon[0D]".equals(file.getName())) {
            try {
                File icon = new File(file.getParent(), "Icon\r");
                icon.delete();
                file.renameTo(icon);
            } catch (Throwable ignore) {
                // ignore
            }
        }
*/
        return 1;
    }

    private static void moveToTrash(File file) throws IOException {
        if (Util.isMac()) {
            File trash = new File(Util.getHome(), ".Trash");
            assert trash.isDirectory();
            String path = Util.getCanonicalPath(file);
            if (path.startsWith(Util.getHome())) {
                path = path.substring(Util.getHome().length());
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            assert path.length() > 0;
            File trashed = new File(trash, path);
            new File(trashed.getParent()).mkdirs();
            if (trashed.exists()) {
                int i = 1;
                for (;;) {
                    File f = new File(trashed.getParent(),  "copy " + i + " " + trashed.getName());
                    if (!f.exists()) {
                        trashed = f;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            assert !trashed.exists();
            Util.renameOrCopyFile(file, trashed);
        } else {
            if (Util.isWindows()) {
                try {
                    Registry.moveFileToRecycleBin(file.getAbsolutePath());
                } catch (IOException x) {
                    Debug.traceln("warning: moveToRecycleBin(" + file.getAbsolutePath() +
                                  ") failed " + x.getMessage());
                    file.delete();
                }
            } else {
                // TODO: does Linux has Trash can? Where is it?
                // Is it the same in different distros?
                file.delete(); // for now: Linux customers suppose to be tough guys
            }
        }
    }

    private ZipEntry getResourceFork(String path) {
        ZipEntry ze = data.zipfile.getEntry(formResourcePath(path, true));
        return ze != null ? ze : data.zipfile.getEntry(formResourcePath(path, false));
    }

    private String formResourcePath(String path, boolean prefix) {
        int ix = path.lastIndexOf('/');
        if (ix < 0 || ix == path.length() - 1) {
            path = "._" + path;
        } else {
            path = path.substring(0, ix + 1) + "._" + path.substring(ix + 1);
        }
        return prefix ? MACOSX + path : path;
    }

    private static int askOverwrite(final File file) {
        assert !IdlingEventQueue.isDispatchThread();
        final int[] result = new int[1];
        try {
            EventQueue.invokeAndWait(new Runnable(){
                public void run() {
                    assert IdlingEventQueue.isDispatchThread();
                    Object[] options = {" Yes ", "  No  ", " All ", "None"};
                    result[0] = JOptionPane.showOptionDialog(MainFrame.getTopFrame(),
                        "<html>Do you want to overwrite existing file?<br><br><b>" +
                        Util.getCanonicalPath(file) +
                        "</b><br>",
                        "Zipeg Prompt: Overwrite",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, // icon
                        options, options[ALL]);
                }
            });
        } catch (InterruptedException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        }
        return result[0];
    }

    private class ArchiveTreeNode implements TreeNode, TreeElement {

        private ArchiveTreeNode parent;
        private Integer ix;
        private ArrayList all;
        private ArrayList dirs;
        private ZipEntry entry;
        private ZipEntry res; // resource entry on MacOSX
        private String path;
        private String name;

        ArchiveTreeNode(ArchiveTreeNode p, Integer i) {
            parent = p;
            assert i != null;
            ix = i;
        }

        public int getChildCount() {
            fillCache();
            return dirs.size();
        }

        public TreeNode getChildAt(int ix) {
            fillCache();
            return (TreeNode)dirs.get(ix);
        }

        public TreeNode getParent() {
            return parent;
        }

        public boolean isDirectory() {
            fillCache();
            return path.charAt(path.length() - 1) == '/';
        }

        public int getIndex(TreeNode node) {
            fillCache();
            int ix = 0;
            for (Iterator i = dirs.iterator(); i.hasNext();) {
                if (node == i.next()) {
                    return ix;
                }
                ix++;
            }
            throw new Error("not found");
        }

        public boolean getAllowsChildren() {
            fillCache();
            return !isLeaf();
        }

        public boolean isLeaf() {
            fillCache();
            return dirs.size() == 0;
        }

        public Enumeration children() {
            fillCache();
            return new Enumeration(){

                Iterator i = dirs.iterator();

                public boolean hasMoreElements() {
                    return i.hasNext();
                }

                public Object nextElement() {
                    return i.next();
                }

            };
        }

        public String toString() {
            fillCache();
            return name;
        }

        public long getSize() {
            fillCache();
            return entry.getSize();
        }

        public long getResourceForkSize() {
            fillCache();
            return res == null ? 0 : res.getSize();
        }

        public long getCompressedSize() {
            fillCache();
            return entry.getCompressedSize();
        }

        public long getResourceForkCompressedSize() {
            fillCache();
            return res == null ? 0 : res.getCompressedSize();
        }

        public int getDescendantFileCount() {
            return getFilesCount(ix);
        }

        public int getDescendantDirectoryCount() {
            return getDirsCount(ix);
        }

        public long getDescendantSize() {
            return getFileBytes(ix);
        }

        public void collectDescendants(List list) {
            if (!isDirectory()) {
                list.add(this);
            } else {
                for (Iterator i = getChildren(); i.hasNext();) {
                    TreeElement c = (TreeElement)i.next();
                    c.collectDescendants(list);
                }
            }
        }

        public String getName() {
            fillCache();
            return name;
        }

        public boolean isEncrypted() {
            fillCache();
            return entry instanceof Z7.Zip7Entry && ((Z7.Zip7Entry)entry).isEncrypted();
        }

        public String getError() {
            fillCache();
            return entry instanceof Z7.Zip7Entry ? ((Z7.Zip7Entry)entry).getError() : null;
        }

        public String getFile() {
            fillCache();
            return path;
        }

        public long getTime() {
            fillCache();
            return entry.getTime();
        }

        public String getComment() {
            return entry.getComment();
        }

        public int getChildrenCount() {
            fillCache();
            return all.size();
        }

        public Iterator getChildren() {
            fillCache();
            return Collections.unmodifiableCollection(all).iterator();
        }

        private void fillCache() {
            if (dirs != null) {
                return;
            }
            path = data.entries[ix.intValue()];
            String n = path;
            boolean dir = n.charAt(n.length() - 1) == '/';
            if (dir) {
                n = n.substring(0, n.length() - 1);
            }
            int ps = n.lastIndexOf('/');
            name = ps >= 0 ? n.substring(ps + 1) : n;
            entry = data.zipfile.getEntry(path);
            res = Util.isMac() ? getResourceFork(path) : null;
            if (entry == null) { // happens for extra parents
                // e.g. "memtest86-3.1a.iso" has no entries for "/" and "[BOOT]"
                entry = new ZipEntry(path);
            }
            Set s = (Set)data.children.get(ix);
            ArrayList ids;
            if (s == null) {
                all  = emptyArrayList;
                dirs = emptyArrayList;
            } else {
                ids = new ArrayList(s);
                Collections.sort(ids, new Comparator(){
                    public int compare(Object o1, Object o2) {
                        String s1 = data.entries[((Integer)o1).intValue()];
                        String s2 = data.entries[((Integer)o2).intValue()];
                        return s1.compareTo(s2);
                    }
                });
                int d = 0;
                all = new ArrayList(ids.size());
                for (Iterator i = ids.iterator(); i.hasNext();) {
                    Integer cix = (Integer)i.next();
                    ArchiveTreeNode tn = new ArchiveTreeNode(this, cix);
                    all.add(tn);
                    String path = data.entries[cix.intValue()];
                    if (path.charAt(path.length() - 1) == '/') d++;
                }
                dirs = new ArrayList(d);
                for (Iterator i = all.iterator(); i.hasNext();) {
                    ArchiveTreeNode c = (ArchiveTreeNode)i.next();
                    String path = data.entries[c.ix.intValue()];
                    if (path.charAt(path.length() - 1) == '/') {
                        dirs.add(c);
                    }
                }
            }
        }

    }

}
