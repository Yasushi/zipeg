package com.zipeg;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.*;
import java.awt.*;

public final class Debug {

    private static boolean debug;
    private static ExceptionHandler crashHandler;
    private static final int RESERVED = Util.MB;
    private static byte[] reserve = new byte[RESERVED];
    private static final Set exclude = new HashSet() {{
        add("user.name");
        add("java.runtime.name");
        add("swing.handleTopLevelPaint");
        add("sun.awt.exception.handler");
        add("sun.awt.noerasebackground");
        add("java.vendor.url.bug");
        add("file.separator");
        add("swing.aatext");
        add("java.vendor");
        add("sun.awt.erasebackgroundonresize");
        add("java.specification.vendor");
        add("java.vm.specification.version");
        add("java.awt.printerjob");
        add("java.class.version");
        add("sun.management.compiler");
        add("java.specification.name");
        add("user.variant");
        add("java.vm.specification.vendor");
        add("line.separator");
        add("java.endorsed.dirs");
        add("java.awt.graphicsenv");
        add("java.vm.specification.name");
        add("sun.java.launcher");
        add("path.separator");
        add("java.vendor.url");
        add("java.vm.name");
        add("java.vm.vendor");
    }};

    private Debug() {
    }

    static void init(boolean b) {
        debug = b;
        System.setProperty("sun.awt.exception.handler", ExceptionHandler.class.getName());
        crashHandler = new ExceptionHandler();
        for (int i = 0; i < reserve.length; i++) {
            reserve[i] = (byte)(i & 0xFF); // make memory commited
        }
    }

    public static void traceln(String s) {
        if (isDebug()) {
            System.err.println(s);
        }
    }

    public static void trace(String s) {
        if (isDebug()) {
            System.err.print(s);
        }
    }

    public static void traceln() {
        if (isDebug()) {
            System.err.println();
        }
    }

    public static void printStackTrace(String msg, Throwable t) {
        if (Debug.isDebug()) {
            Debug.traceln(msg);
            t.printStackTrace();
        }
    }

    public static void printStackTrace(Throwable t) {
        if (Debug.isDebug()) {
            t.printStackTrace();
        }
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void execute(Runnable r) {
        try {
            r.run();
        } catch (Throwable x) {
            if (crashHandler != null) {
                crashHandler.handle(x);
            } else {
                x.printStackTrace();
            }
        }
    }

    public static void releaseSafetyPool() {
        reserve = null;
        System.gc();
        Util.sleep(1000);
    }

    public static class ExceptionHandler {

        private static boolean reported;

        public void handle(final Throwable x) {
            if (this != crashHandler) {
                releaseSafetyPool();
                crashHandler.handle(x);
            } else if (!reported) {
                reported = true;
                report(x);
            }
        }

        public static void report(Throwable x) {
            int crash_count = 0;
            try {
                crash_count = Presets.getInt("crash.count", 0);
                crash_count++;
                Presets.putInt("crash.count", crash_count);
            } catch (Throwable ignore) {
                // ignore
            }
            if (Util.isMac()) {
                String cd = System.getProperty("user.dir");
                if (cd.toLowerCase().indexOf(".dmg/") >= 0) {
                    System.exit(0);
                }
            }
            Throwable cause = x;
            for (; ;) {
                if (cause instanceof InvocationTargetException &&
                    ((InvocationTargetException)cause).getTargetException() != null) {
                    cause = ((InvocationTargetException)cause).getTargetException();
                } else if (cause.getCause() != null) {
                    cause = cause.getCause();
                } else {
                    break;
                }
            }
            // noinspection CallToPrintStackTrace
            cause.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = null;
            String method;
            try {
                pw = new PrintWriter(sw) {
                    public void println() { super.write('\n'); }
                };
                Archive a = Zipeg.getArchive();
                if (a != null) {
                    String name = new File(a.getName()).getName();
                    pw.println("Archive: \"" + name + "\"\n");
                }
                String archive = Zipeg.getRecent(0) != null ? Zipeg.getRecent(0) : "";
                if (!"".equals(archive)) {
                    String name = new File(archive).getName();
                    pw.println("Last Archive: \"" + name + "\"\n");
                }
                method = printStackTrace(cause, pw);
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
            StringBuffer sb = sw.getBuffer();
            sb.append("\n\n");
            Map p = System.getProperties();
            for (Iterator j = p.keySet().iterator(); j.hasNext();) {
                Object key = j.next();
                if (!exclude.contains(key)) {
                    sb.append(key).append("=").append(p.get(key)).append("\n");
                }
            }
            String sep = File.separatorChar == '\\' ? "\\\\" : "/";
            String user = sep + p.get("user.name");
            String body = sb.toString();
            body = body.replaceAll(user, sep + "user");
            String subject = "[zipeg crash] " + Util.getVersion() + " " + shorten(cause.toString()) +
                             (method != null ? " at " + shorten(method) : "");
            if (isIgnorable(cause, body)) {
                reported = false;
                return;
            }
            try {
                String install_date = Presets.get("zipeg.install.date", "");
                int extract_count = Presets.getInt("extract.count", 0);
                int donate_count = Presets.getInt("donate.count", 0);
                int update_count = Presets.getInt("update.count", 0);
                String uuid = Presets.get("zipeg.uuid", "");
                body += "\r\ninstalled: " + install_date +
                        " [x" + extract_count + ":d" + donate_count +
                        ":c" + crash_count + ":u" + update_count + "] " +
                        uuid + "\r\n";
            } catch (Throwable ignore) {
                // ignore
            }
            report(subject, body);
            if (Util.getCacheDirectory().toString().indexOf("com.zipeg") > 0) {
                Util.rmdirs(Util.getCacheDirectory());
            }
            System.exit(1);
        }

        /**
         * writes crashlog into a file and starts itself on another instantiation of JVM to send it.
         * @param subject of email to send
         * @param body of email with crash log
         */
        private static void report(String subject, String body) {
            try {
                File file = new File(Util.getTmp(), "zipeg.crash." + System.currentTimeMillis() + ".log");
                String path = Util.getCanonicalPath(file);
                file = new File(path);
                file.createNewFile();
                PrintStream out = new PrintStream(new FileOutputStream(file));
                out.print(subject + "\n\n" + body);
                Util.close(out);
                String[] a = new String[7];
                a[0] = Util.getJava();
                a[1] = "-cp";
                a[2] = "zipeg.jar";
                a[3] = "com.zipeg.Zipeg";
                a[4] = "--report-crash";
                a[5] = subject;
                a[6] = path;
                Process p = Runtime.getRuntime().exec(a);
                if (isDebug()) {
                    p.waitFor();
                    Debug.trace("exit = " + p.exitValue());
                }
            } catch (Throwable x) {
                x.printStackTrace();
            }
        }

        private static String printStackTrace(Throwable t, PrintWriter s) {
            String method = null;
            String first = null;
            synchronized (t) {
                s.println(t);
                StackTraceElement[] trace = t.getStackTrace();
                for (int i = 0; i < trace.length; i++) {
                    String f = "" + trace[i];
                    if (first == null) {
                        first = trace[i].getClassName() + "." + trace[i].getMethodName();
                    }
                    if (f.startsWith("com.zipeg.")) {
                        f = f.substring("com.zipeg.".length());
                        if (method == null) {
                            method = trace[i].getClassName() + "." + trace[i].getMethodName();
                        }
                    }
                    else if (f.startsWith("java.util.")) {
                        f = f.substring("java.util.".length());
                    }
                    else if (f.startsWith("java.io.")) {
                        f = f.substring("java.io.".length());
                    }
                    else if (f.startsWith("java.awt.")) {
                        f = f.substring("java.awt.".length());
                    }
                    else if (f.startsWith("javax.swing.")) {
                        f = f.substring("javax.swing.".length());
                    }
                    f = f.replaceAll(".java:", ":");
                    if (f.indexOf("EventDispatchThread.pumpOneEventForHierarchy") >= 0) {
                        break; // cut bottom of the stack
                    }
                    s.println(f);
                }
                Throwable ourCause = t.getCause();
                if (ourCause != null) {
                    s.println("caused by: ");
                    printStackTrace(t, s);
                }
            }
            return method == null ? first : method;
        }

        private static String shorten(String message) {
            if (message.startsWith("java.lang.")) {
                return message.substring("java.lang.".length());
            } else if (message.startsWith("java.util.")) {
                return message.substring("java.util.".length());
            } else if (message.startsWith("java.io.")) {
                return message.substring("java.io.".length());
            } else if (message.startsWith("javax.swing.")) {
                return message.substring("javax.swing.".length());
            } else {
                return message;
            }
        }

        /*
         * Determines if the exception can be ignored.
         * https://www.limewire.org/fisheye/browse/limecvs/gui/com/limegroup/gnutella/gui/DefaultErrorCatcher.java?r=1.12
         * https://www.limewire.org/jira/browse/GUI-235
         */
        private static boolean isIgnorable(Throwable x, String msg) {
            if (msg.indexOf("RepaintManager") >= 0) {
                return true;
            }
            if (msg.indexOf("sun.awt.RepaintArea.paint") >= 0) {
                return true;
            }
            // http://groups.google.com.pk/group/Google-Web-Toolkit/browse_thread/thread/44df53c5c7ef6df2/2b68528d3fb70048?lnk=raot
            if (msg.indexOf("apple.awt.CGraphicsEnvironment.displayChanged") >= 0) {
                return true;
            }
            // http://lists.apple.com/archives/java-dev/2004/May/msg00192.html
            // http://www.thinkingrock.com.au/forum/viewtopic.php?p=4279&sid=0abfa9f43f7a52d33e3960f575973c5c
            // http://www.jetbrains.net/jira/browse/IDEADEV-8692
            // http://www.jetbrains.net/jira/browse/IDEADEV-9931
            // http://groups.google.com/group/comp.soft-sys.matlab/browse_thread/thread/5c56f6d6d08cb5e0/db152a7adc8b8e25?lnk=raot
            // display manager on OSX goes out of whack
            if (msg.indexOf("apple.awt.CWindow.displayChanged") >= 0) {
                return true;
            }
            if (x instanceof ArrayIndexOutOfBoundsException) {
                if (msg.indexOf("plaf.basic.BasicTabbedPaneUI.getTabBounds") >= 0)
                    return true;
            }
            if (x instanceof IndexOutOfBoundsException) {
                if (msg.indexOf("DefaultRowSorter.convertRowIndexToModel") >= 0) {
                    return true;
                }
            }
            // system clipboard can be held, preventing us from getting.
            // throws a RuntimeException through stuff we don't control...
            if (x instanceof IllegalStateException) {
                if (msg.indexOf("cannot open system clipboard") >= 0)
                    return true;
            }
            if (x instanceof IllegalComponentStateException) {
                if (msg.indexOf("component must be showing on the screen to determine its location") >= 0)
                    return true;
            }
            if (x instanceof NullPointerException) {
                if (msg.indexOf("MetalFileChooserUI") >= 0 ||
                    msg.indexOf("WindowsFileChooserUI") >= 0 ||
                    msg.indexOf("AquaDirectoryModel") >= 0 ||
                    msg.indexOf("SizeRequirements.calculateAlignedPositions") >= 0 ||
                    msg.indexOf("BasicTextUI.damageRange") >= 0 ||
                    msg.indexOf("null pData") >= 0 ||
                    msg.indexOf("disposed component") >= 0 ||
                    msg.indexOf("com.sun.java.swing.plaf.windows.XPStyle$Skin") >= 0 ||
                    msg.indexOf("FilePane$2.repaintListSelection") >= 0) {
                    return true;
                }
            }
            if (msg.indexOf("InternalError: Unable to bind") >= 0) {
                return true;
            }
            if (msg.indexOf("sun.awt.shell.Win32ShellFolder2.getFileSystemPath0") >= 0) {
                return true;
            }
            if (msg.indexOf("Could not get shell folder ID list") >= 0) {
                return true;
            }
            if (x instanceof InternalError) {
                if (msg.indexOf("getGraphics not implemented for this component") >= 0)
                    return true;
            }
            return msg.indexOf("ArrayIndexOutOfBoundsException: 3184") >= 0;
        }

    }
}