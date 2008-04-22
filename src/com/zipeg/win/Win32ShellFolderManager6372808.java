package com.zipeg.win;

import sun.awt.shell.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.awt.*;

import com.zipeg.Util;
import com.zipeg.Debug;

// workaround for: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808

public class Win32ShellFolderManager6372808 extends Win32ShellFolderManager2 {

    private static Method getDrives;
    private static boolean patched;
    private static Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            ShellFolder shellFolder1 = (ShellFolder)o1;
            ShellFolder shellFolder2 = (ShellFolder)o2;
            boolean isDrive = shellFolder1.getPath().endsWith(":\\");
            if (isDrive ^ shellFolder2.getPath().endsWith(":\\")) {
                return isDrive ? -1 : 1;
            } else {
                return shellFolder1.getPath().compareTo(shellFolder2.getPath());
            }
        }
    };

    public Win32ShellFolderManager6372808() {
    }

    public static void workaround() {
        if (Util.isWindows()) {
            Toolkit kit = Toolkit.getDefaultToolkit();
            Class sfmc = (Class)kit.getDesktopProperty("Shell.shellFolderManager");
            if ("sun.awt.shell.Win32ShellFolderManager".equals(sfmc.getName()) ||
                "sun.awt.shell.Win32ShellFolderManager2".equals(sfmc.getName())) {
                Method setDesktopProperty = Util.getDeclaredMethod("java.awt.Toolkit.setDesktopProperty",
                                                              new Class[]{String.class, Object.class});
                Util.call(setDesktopProperty, kit,
                        new Object[]{"Shell.shellFolderManager",
                        Win32ShellFolderManager6372808.class});
            }
            getDrives = Util.getDeclaredMethod("sun.awt.shell.Win32ShellFolderManager2.getDrives", new Class[]{});
            patchShellFolderManager();
            Debug.traceln("Win32ShellFolderManager6372808 patched=" + patched);
        }
    }

    public Object get(String key) {
        if (key.equals("fileChooserComboBoxFolders")) {
            File desktop = Util.getDesktop();
            if (desktop == null || !desktop.exists()) {
                return super.get(key);
            }
            ArrayList folders = new ArrayList();
            File drives = (File)Util.call(getDrives, null, Util.NONE);
            folders.add(desktop);
            // Add all second level folders
            File[] secondLevelFolders = desktop.listFiles();
            Arrays.sort(secondLevelFolders);
            for (int j = 0; j < secondLevelFolders.length; j++) {
                File folder = secondLevelFolders[j];
                String name = folder.getName().toLowerCase();
                boolean isArchive = Util.isArchiveFileType(name);
                if (!isArchive && (folder.isDirectory() || !isFileSystem(folder))) {
                    folders.add(folder);
                    // Add third level for "My Computer"
                    if (drives != null && folder.equals(drives)) {
                        File[] thirdLevelFolders = folder.listFiles();
                        if (thirdLevelFolders != null) {
                            Arrays.sort(thirdLevelFolders, comparator);
                            for (int k = 0; k < thirdLevelFolders.length; k++) {
                                folders.add(thirdLevelFolders[k]);
                            }
                        }
                    }
                }
            }
            return folders.toArray(new File[folders.size()]);
        }
        try {
            return super.get(key);
        } catch (Throwable x) {
            Debug.printStackTrace("get(" + key + ")", x);
            return null;
        }
    }

    private static boolean isFileSystem(File folder) {
        Method isFileSystem = Util.getDeclaredMethod(folder.getClass().getName() + ".isFileSystem", new Class[]{});
        String name = folder.getName().toLowerCase();
        if (Util.isArchiveFileType(name)) {
            return false;
        }
        Boolean b = (Boolean)Util.call(isFileSystem, folder, Util.NONE);
//      Debug.traceln("isFileSystem(" + folder + ")=" + (b != null && b.booleanValue()));
        return b != null && b.booleanValue();
    }

    private static void patchShellFolderManager() {
        try {
            // 1.6, 1.7 staticly initialize ShellFolder.shellFolderManager field.
            // Order of initialization is too complex to be predictable.
            Field shellFolderManager = ShellFolder.class.getDeclaredField("shellFolderManager");
            shellFolderManager.setAccessible(true);
            Object sfm = shellFolderManager.get(null);
            if (sfm != null && !(sfm instanceof Win32ShellFolderManager6372808)) {
                shellFolderManager.set(null, new Win32ShellFolderManager6372808());
            }
            patched = true;
        } catch (Throwable t) {
            Debug.printStackTrace("failed to patch ShellFolder.shellFolderManager", t);
        }
    }

}
