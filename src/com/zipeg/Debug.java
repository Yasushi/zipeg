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
            if (Util.getCacheDirectory().toString().indexOf("com.zipeg") > 0) {
                Util.rmdirs(Util.getCacheDirectory());
            }
            System.exit(1);
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
                    pw.println("Archive: \"" + a.getName() + "\"\n");
                }
                String archive = Zipeg.getRecent(0) != null ? Zipeg.getRecent(0) : "";
                if (!"".equals(archive)) {
                    pw.println("Last Archive: \"" + archive + "\"\n");
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
            String body = sb.toString();
            String subject = "[zipeg crash] " + Util.getVersion() + " " + shorten(cause.toString()) +
                             (method != null ? " at " + shorten(method) : "");
            if (isIgnorable(cause, body)) {
                reported = false;
                return;
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
        public static void report(String subject, String body) {
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
                if (isDebug()) {
                    Process p = Runtime.getRuntime().exec(a);
                    p.waitFor();
                    Debug.trace("exit = " + p.exitValue());
                } else {
                    Runtime.getRuntime().exec(a);
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
            if (msg.indexOf("RepaintManager") != -1)
                return true;
            if (msg.indexOf("sun.awt.RepaintArea.paint") != -1)
                return true;
            // display manager on OSX goes out of whack
            if (x instanceof ArrayIndexOutOfBoundsException) {
                if (msg.indexOf("apple.awt.CWindow.displayChanged") != -1)
                    return true;
                if (msg.indexOf("plaf.basic.BasicTabbedPaneUI.getTabBounds") != -1)
                    return true;
            }
            if (x instanceof IndexOutOfBoundsException) {
                if (msg.indexOf("DefaultRowSorter.convertRowIndexToModel") != -1) {
                    return true;
                }
            }
            // system clipboard can be held, preventing us from getting.
            // throws a RuntimeException through stuff we don't control...
            if (x instanceof IllegalStateException) {
                if (msg.indexOf("cannot open system clipboard") != -1)
                    return true;
            }
            if (x instanceof IllegalComponentStateException) {
                if (msg.indexOf("component must be showing on the screen to determine its location") != -1)
                    return true;
            }
            if (x instanceof NullPointerException) {
                if (msg.indexOf("MetalFileChooserUI") != -1)
                    return true;
                if (msg.indexOf("WindowsFileChooserUI") != -1)
                    return true;
                if (msg.indexOf("AquaDirectoryModel") != -1)
                    return true;
                if (msg.indexOf("SizeRequirements.calculateAlignedPositions") != -1)
                    return true;
                if (msg.indexOf("BasicTextUI.damageRange") != -1)
                    return true;
                if (msg.indexOf("null pData") != -1)
                    return true;
                if (msg.indexOf("disposed component") != -1)
                    return true;
                if (msg.indexOf("FilePane$2.repaintListSelection") != -1) {
                    return true;
                }
            }
            if (x instanceof InternalError) {
                if (msg.indexOf("getGraphics not implemented for this component") != -1)
                    return true;
            }
            return false;
        }

    }
}