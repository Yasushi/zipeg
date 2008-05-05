package com.zipeg;

import java.util.Set;
import java.util.HashSet;

// see also:
// http://www.macosxhints.com/article.php?story=20031215144430486&query=Launch%2BServices%2Bdatabase
// /System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/LaunchServices.framework/Versions/Current/Support/lsregister
// /System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/LaunchServices.framework/Versions/Current/Support/lsregister -v -kill -r -f -domain local -domain system -domain user
// bash> open ~/Library/Preferences/com.apple.LaunchServices.plist
// bash> ll /Library/Caches/*Launch*


// Leopard: /System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/LaunchServices.framework/Versions/A/Support/lsregister


public class DefaultRoleHandler implements FileAssociationHandler {

    private static boolean loaded;
    private static boolean error;
    private static DefaultRoleHandler instance = null;
    private static final String bundleId = "com.zipeg.zipeg";
    private static final Set BOM = new HashSet() {{
        // see: /System/Library/CoreServices/BOMArchiveHelper.app/Contents/Info.plist
        // http://developer.apple.com/documentation/Carbon/Conceptual/understanding_utis/utilist/chapter_4_section_1.html
        add("cpio");    add("public.cpio-archive");
        add("cpgz");
        add("zip");     add("public.zip-archive");
        add("tar");     add("public.tar-archive");   add("org.gnu.gnu-tar-archive");
        add("tgz");     add("org.gnu.gnu-zip-tar-archive");
        add("tbz");
        add("tbz2");
        add("gz");      add("org.gnu.gnu-zip-archive");
        add("z");       add("com.public.z-archive");
        add("bz");
        add("bz2");     add("public.archive.bzip2");
    }};

    private DefaultRoleHandler() {
        loadLibrary();
    }

    public static DefaultRoleHandler getInsance() {
        if (instance == null) {
            instance = new DefaultRoleHandler();
        }
        return instance;
    }

    public boolean isAvailable() {
        loadLibrary();
        return loaded && !error;
    }

    private static final int
        kRoleNone = 0x00000001,
        kRoleViewer = 0x00000002,
        kRoleEditor = 0x00000004,
        kRoleShell = 0x00000008,
        kRoleAll = 0xFFFFFFFF;

    private static int[] roles = new int[]{kRoleNone, kRoleViewer, kRoleEditor, kRoleShell, kRoleAll};
    private static String[] roleNames = new String[]{"none", "viewer", "editor", "shell", "all"};

    /**
     * @param h handler
     * @return true if handler is my bundleId or is empty
     */
    private boolean isMineOrEmpty(String h) {
        return h == null || h.equalsIgnoreCase(bundleId) || h.length() == 0 || "null".equalsIgnoreCase(h);
    }

    public void setHandled(long selected) {
        long current = getHandled();
        Debug.traceln("\n>setHandled(" + Long.toHexString(selected) + ")");
        boolean changed = false;
        for (int i = 0; i < ext2uti.length; i++) {
            long bit = 1L << i;
            if ((bit & selected) == (bit & current)) {
                continue;
            }
            String uti = (String)ext2uti[i][3]; // unique type identifier
            String ext = (String)ext2uti[i][0];
            if ((bit & selected) != 0) {
                assert (bit & current) == 0;
                // save existing
//              Debug.trace("\tsave existing for: " + uti);
                for (int j = 0; j < roles.length; j++) {
                    String handler = getForContentType(uti, roles[j]);
//                  Debug.trace(" \t" + roleNames[j] + "=" + handler);
                    if (!isMineOrEmpty(handler)) {
                        Presets.put("drh." + roleNames[j] + ":" + uti, handler);
                        Presets.sync();
                    }
                }
//              Debug.traceln();
                setIgnoreCreator(uti, true);
                setForContentType(uti, bundleId, kRoleNone|kRoleViewer|kRoleEditor|kRoleShell);
                setForContentType(uti, bundleId, kRoleAll);
                String[] a = (String[])aliases.get(uti);
                for (int k = 0; a != null && k < a.length; k++) {
                    setIgnoreCreator(a[k], true);
                    setForContentType(a[k], bundleId, kRoleNone|kRoleViewer|kRoleEditor|kRoleShell);
                    setForContentType(a[k], bundleId, kRoleAll);
                }
            } else {
                assert (bit & current) != 0;
                assert (bit & selected) == 0;
                // restore saved
                String dominant = null;
                // try to find at least one non-empty handler first
                for (int j = 0; j < roles.length; j++) {
                    String handler = Presets.get("drh." + roleNames[j] + ":" + uti, null);
                    if (!isMineOrEmpty(handler) && dominant == null) {
                        dominant = handler;
                    }
                }
//              Debug.trace("\trestore saved for: " + uti + "(" + dominant + ")");
                for (int j = 0; j < roles.length; j++) {
                    String handler = Presets.get("drh." + roleNames[j] + ":" + uti, "");
                    if (isMineOrEmpty(handler)) {
                        if (dominant != null) {
                            handler = dominant;
                        } else {
                            // null and "" are not good -
                            // LSSetDefaultRoleHandlerForContentType( handler == null or "") is NOP.
                            handler = BOM.contains(uti) || BOM.contains(ext) ? "com.apple.bomarchivehelper" :
                                      " ";
                        }
                    }
                    setForContentType(uti, handler, roles[j]);
//                  Debug.trace(" \t" + roleNames[j] + "=" + handler);
                }
//              Debug.traceln();
            }
            changed = true;
        }
        if (changed) {
            try {
                Process p = Runtime.getRuntime().exec(
                        new String[]{"osascript", "fnotify.compiled.scpt"},
                        Util.getEnvFilterOutMacCocoaCFProcessPath());
                if (Debug.isDebug()) {
                    p.waitFor();
                    Debug.traceln("exit code " + p.exitValue());
                    assert p.exitValue() == 0 : "fnotify.compiled.scpt exit(" + p.exitValue() + ")";
                }
            } catch (Exception e) {
                if (Debug.isDebug()) {
                    throw new Error(e);
                }
            }
        }
//      Debug.traceln("<setHandled");
    }

    public long getHandled() {
//      Debug.traceln("\n>getHandled");
        long result = 0;
        for (int i = 0; i < ext2uti.length; i++) {
            String uti = (String)ext2uti[i][3];
            String handler = getForContentType(uti, kRoleViewer);
            if (bundleId.equalsIgnoreCase(handler)) {
                result |= (1L << i);
            }
/*
            Debug.trace(((result & (1L << i)) != 0 ? "+" : "-") + " getHandler: " + uti);
            for (int j = 0; j < roles.length; j++) {
                String h = getForContentType(uti, roles[j]);
                Debug.trace(" \t" + roleNames[j] + "=" + h);
            }
            Debug.traceln();
*/
        }
//      Debug.traceln("<getHandled(" + Long.toHexString(result) + ")");
        return result;
    }

    static void loadLibrary() {
        if (Util.getOsVersion() < 10.4) {
            // Mac OS 10.3.9 does not support LSCopyDefaultRoleHandlerForContentType
            error = true;
            return;
        }
        if (!loaded && !error) {
            try {
                System.loadLibrary(getLibraryName());
                loaded = true;
            } catch (Throwable ex) {
                error = true;
            }
        }
    }

    private static String getLibraryName() {
        String p = Util.getProcessor();
        assert Util.isMac();
        String cpu = "powerpc".equals(p) ? "ppc" : p;
        return "drh-osx-" + cpu;
    }

    // commented out methods are implemented and tested. just not used at the moment

    private native int setForContentType(String contentType, String bundle, int role);
//  private native int setForURLScheme(String contentType, String bundle);
    private native int setIgnoreCreator(String contentType, boolean b);
    private native String getForContentType(String contentType, int role);
//  private native String getForURLScheme(String contentType);
//  private native boolean getIgnoreCreator(String contentType);

    /*  DO NOT DO:
        iso	    public.iso-image       (do NOT associate)
        jar	    com.sun.java-archive   (do NOT associate)
        rpm         com.redhat.rpm-archive (do NOT associate)
    */
}

/*

http://developer.apple.com/documentation/Carbon/Reference/LaunchServicesReference/Reference/reference.html#//apple_ref/c/tdef/LSRolesMask

Constants

kLSRolesNone

    Requests the role None (the application cannot open the item, but provides an icon and a kind string for it).

    Available in Mac OS X v10.0 and later.
kLSRolesViewer

    Requests the role Viewer (the application can read and present the item, but cannot manipulate or save it).

    Available in Mac OS X v10.0 and later.
kLSRolesEditor

    Requests the role Editor (the application can read, present, manipulate, and save the item).

    Available in Mac OS X v10.0 and later.
kLSRolesShell

    Requests the role Shell (the application can execute the item).

    Available in Mac OS X v10.4 and later.
kLSRolesAll

    Accepts any role with respect to the item.

    Available in Mac OS X v10.0 and later.

Discussion

This bit mask is passed to functions that find the preferred application for a given item or
family of items (LSGetApplicationForItem, LSGetApplicationForURL, LSGetApplicationForInfo),
or that determine whether a given application can open a designated item (LSCanRefAcceptItem,
LSCanURLAcceptURL), to specify the application�s desired role or roles with respect to the item.
For example, to request only an editor application, specify kLSRolesEditor; if either an editor
or a viewer application is acceptable, specify kLSRolesEditor | kLSRolesViewer.

*/