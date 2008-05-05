package com.zipeg;

import java.io.*;
import java.util.*;

public class Updater {

    // NOTE: (problem to have) if server trafic becomes and issue make it 7 DAYS
    public static final long DAYS = 3L * 24L * 60 * 60 * 1000; // 3 DAYS = 3 * 24 hrs in milliseconds
    private static final long HOURS = 3L * 60 * 60 * 1000; // 3 HOURS
    private static final String UPDATE = "http://www.zipeg.com/downloads/update.txt";

    private static final Object checkNow = new Object();
    private static boolean requested;
    private static Thread checker;
    private static Thread downloader;


    /**
     * @param now true if immediate check requested by UI
     * event
     *      updateAvailable(Object[]{Int rev, String ver,
     *                               String url, String msg, boolean immediate})
     * will be posted to dispatch thread. nextUpdate == 1 indicates that this
     * message is sent in response to checkForUpdate(true) otherwise
     * nextUpdate time will be != 1
     */

    public static void checkForUpdate(boolean now) {
        assert IdlingEventQueue.isDispatchThread();
        if (now) {
            Presets.putLong("nextUpdate", System.currentTimeMillis() - 1);
            Presets.sync();
        }
        if (checker == null) {
            checker = newThread(new Checker(), "checker");
        }
        if (now) {
            synchronized (checkNow) {
                requested = now;
                checkNow.notify();
            }
        }
    }

    public static File getUpdateFile() {
        File wd = new File(Util.getCanonicalPath(new File(".")));
        return new File(wd, Util.isWindows() ? "zipeg-update.exe" : "zipeg-update.zip");
    }


    public static void cleanUpdateFiles() {
        getUpdateFile().delete();
        // DO NOT:  getUpdateFile().deleteOnExit(); // this will make update logic fail!
        File com = new File(Util.getCanonicalPath(new File(".")), "com");
        File zipeg = new File(com, "zipeg");
        if (zipeg.isDirectory() && zipeg.list().length == 0) {
            zipeg.delete();
            if (com.list().length == 0) {
                com.delete();
            }
        }
    }

    /**
     * Downloads file from url and posts
     *      updateDownloaded(File file)
     * event when done.
     * @param url to download file from.
     */
    public static void download(String url) {
        assert IdlingEventQueue.isDispatchThread();
        if (downloader == null) {
            downloader = newThread(new Downloader(url), "download");
        }
    }

    private static Thread newThread(Runnable r, String name) {
        assert IdlingEventQueue.isDispatchThread();
        Thread  thread = new Thread(r);
        thread.setName(name);
        thread.setDaemon(true); // jvm can exit and it is ok we will try later
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return thread;
    }

    private static void debugTrace(Throwable e) {
        if (Debug.isDebug()) {
            e.printStackTrace();
        }
    }

    private static class Checker implements Runnable {

        boolean immediate;

        public void run() {
            Debug.execute(new Runnable() {
                public void run() { check(); }
            });
        }

        public void check() {
            assert !IdlingEventQueue.isDispatchThread();
            for (;;) {
                synchronized (checkNow) {
                    try {
                        checkNow.wait(1000 * 10); // wakes up once in 10 seconds
                        immediate = requested;
                        requested = false;
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                long now = System.currentTimeMillis();
                long nextUpdate = Presets.getLong("nextUpdate", 0);
                if (nextUpdate == 0) {
                    // evenly space update
                    long delta = HOURS;
                    while (nextUpdate < now + delta) {
                        nextUpdate += delta;
                    }
                    Presets.putLong("nextUpdate", nextUpdate);
                    Debug.traceln("Warning: nextUpdate==0 putLong(nextUpdate, " + new Date(nextUpdate) + ")");
                    Presets.flushNow();
                    continue;
                }
                if (!immediate && now < nextUpdate) {
                    continue;
                }
                Debug.traceln("getLong(nextUpdate, " + new Date(nextUpdate) + ") lastUpdate " +
                        new Date(Presets.getLong("lastUpdate", 0)));
                Properties p = new Properties();
                Throwable x = null;
                boolean b = false;
                try {
                    ByteArrayOutputStream body = new ByteArrayOutputStream();
                    b = Util.getFromUrl(UPDATE, null, null, body);
                    if (b) {
                        p.load(new ByteArrayInputStream(body.toByteArray()));
                        final String ver = p.getProperty("version");
                        final String url = Util.isWindows() ? p.getProperty("url-win") : p.getProperty("url-mac");
                        final String msg = p.getProperty("message");
                        // from some crashes in the field p.getProperty("version") does return null sometimes...
                        // I believe this is due to Apache Mac OS X issues - sometimes it is not returning update.txt
                        // but some other text.
                        int ix = ver == null | url == null | msg == null ? -1 : ver.lastIndexOf('.');
                        int rev = ix >= 0 ? Integer.decode(ver.substring(ix + 1).trim()).intValue() : -1;
                        if (rev > 0) {
                            Presets.putLong("lastUpdate", now);
                            Actions.postEvent("updateAvailable",
                                    new Object[]{new Integer(rev),
                                                 ver, url, msg,
                                                 Boolean.valueOf(immediate)});
                        } else {
                            b = false;
                        }
                    }
                } catch (NumberFormatException e) {
                    x = e;
                } catch (IOException e) {
                    x = e;
                }
                if (nextUpdate < now - 100 * HOURS) {
                    nextUpdate = now;
                }
                // evenly space update
                long delta = b ? DAYS : HOURS;
                while (nextUpdate < now + delta) {
                    nextUpdate += delta;
                }
                Presets.putLong("nextUpdate", nextUpdate);
                Debug.traceln("putLong(nextUpdate, " + new Date(nextUpdate) + ")");
                Presets.flush();
                if (x != null) {
                    Debug.printStackTrace("", x);
                }
            }
        }

    }

    private static class Downloader implements Runnable {

        private String url;

        Downloader(String u) {
            url = u;
        }

        public void run() {
            try {
                // deliberatly unchecked by Debug.execute
                downloadFile(url);
            } finally {
                downloader = null;
            }
        }

        private static void downloadFile(String url) {
            File file = getUpdateFile();
            FileOutputStream os = null;
            try {
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                if (!Util.getFromUrl(url, null, null, body)) {
                    return;
                }
                File tmp = new File(file + ".tmp");
                tmp.delete();
                tmp.createNewFile();
                os = new FileOutputStream(tmp);
                os.write(body.toByteArray());
                Util.close(os);
                os = null;
                file.delete();
                tmp.renameTo(file);
                Actions.postEvent("updateDownloaded", file);
            } catch (IOException e) {
                debugTrace(e); // enough for background thread
            } finally {
                Util.close(os);
            }
        }

    }

}
