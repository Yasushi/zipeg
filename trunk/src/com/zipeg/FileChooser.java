package com.zipeg;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;

public class FileChooser extends JFileChooser {

    private static FileChooser instance;
    private FilteredFileSystemView ffsv = null;
    private static final File[] EMPTY = new File[]{};

    static {
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4525475
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        if (Util.isWindows()) {
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
            Util.callStatic("com.zipeg.win.Win32ShellFolderManager6372808.workaround", Util.NONE);
        }
    }

    public static FileChooser getInstance() {
        if (instance == null) {
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4711700
            int retry = 32;
            long time = System.currentTimeMillis();
            for (;;) {
                try {
                    FileChooser fc = new FileChooser() {

                        protected JDialog createDialog(Component parent) throws HeadlessException {
                            JDialog dlg = super.createDialog(parent);
                            // TODO: this does not work. revisit when java 1.6 is out on Mac
                            if (Util.isMac() && Util.getJavaVersion() >= 1.6) {
                                dlg.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
                                dlg.getRootPane().putClientProperty("apple.awt.windowAlpha", "0.75");
                            }
                            return dlg;
                        }

                        public int showDialog(Component parent, String approveButtonText) {
                            try {
                                IdlingEventQueue.setInsideFileChooser(true);
                                return super.showDialog(parent, approveButtonText);
                            } finally {
                                // bug in apple.laf.AquaFileChooserUI.MacListSelectionModel.
                                // isSelectableInListIndex
                                IdlingEventQueue.setInsideFileChooser(false);
                            }
                        }

                    };
                    time = System.currentTimeMillis() - time;
                    Debug.traceln("new FileChooser() " + time + " ms ");
                    instance = fc;
                    break;
                } catch (final Throwable x) {
                    time = System.currentTimeMillis() - time;
                    Debug.trace("new JFileChooser() " + time + " ms " + x.getMessage());
                    --retry;
                    if (retry == 0) {
                        throw new Error(x);
                    }
                    else {
                        Util.sleep(100);
                    }
                }
            }
        }
        return instance;
    }

    FilteredFileSystemView getFilteredFileSystemView() {
        if (ffsv == null) {
            ffsv = new FilteredFileSystemView(super.getFileSystemView());
        }
        return ffsv;
    }

    // Following overrides try to minimize number of PropertyChange events
    // that javax.swing.plaf.basic.BasicDirectoryModel will receive.
    // On most property change events BasicDirectoryModel will create a file
    // loading background thread.

    public void setFileSystemView(FileSystemView fsv) {
        if (fsv != super.getFileSystemView()) {
            super.setFileSystemView(fsv);
        }
    }

    public void setCurrentDirectory(File dir) {
        if (!Util.equals(dir, super.getCurrentDirectory())) {
            super.setCurrentDirectory(dir);
        }
    }

    public void setFileView(FileView fileView) {
        if (fileView != super.getFileView()) {
            super.setFileView(fileView);
        }
    }

    public void setFileFilter(FileFilter filter) {
        if (filter != super.getFileFilter()) {
            super.setFileFilter(filter);
        }
    }

    public void setFileHidingEnabled(boolean b) {
        if (b != super.isFileHidingEnabled()) {
            super.setFileHidingEnabled(b);
        }
    }

    public void setFileSelectionMode(int mode) {
        if (mode != super.getFileSelectionMode()) {
            super.setFileSelectionMode(mode);
        }
    }

    public void setAcceptAllFileFilterUsed(boolean b) {
        if (b != super.isAcceptAllFileFilterUsed()) {
            super.setAcceptAllFileFilterUsed(b);
        }
    }

    private static boolean isLinkToArchive(File f) {
        try {
            if (f.length() > 32*1024) {
                return false;
            }
            byte[] content = Util.readFile(f);
            String s = new String(content).toLowerCase();
            return s.indexOf(".zip") >= 0 || s.indexOf(".cab") >= 0 || s.indexOf(".rar") >= 0 || s.indexOf(".tar") >= 0;
        } catch (Throwable x) {
            return false;
        }
    }

    public class FilteredFileSystemView extends FileSystemView {

        private FileSystemView delegate;


        public FilteredFileSystemView(FileSystemView view) {
            this.delegate = view;
        }

        public File[] getFiles(File dir, boolean useFileHiding) {
            // this method is called from MULTIPLE threads.
            // see brain dead implementation of javax.swing.plaf.basic.BasicDirectoryModel.
            // Note that chooser.getFileFilter() is single variable
            // access and is hopefully "volatile" at least for now.
            try {
                // poor man PropertyChange event coalescing solution:
                synchronized(this) { this.wait(50); }
            } catch (InterruptedException x) {
                //Debug.traceln("INTERRUPTED: getFiles " + dir + " thread " + Thread.currentThread().hashCode());
                return EMPTY;
            }
            if (Thread.currentThread().isInterrupted()) {
                //Debug.traceln("INTERRUPTED: getFiles " + dir + " thread " + Thread.currentThread().hashCode());
                return EMPTY;
            }
            //Debug.traceln(">getFiles " + dir + " thread " + Thread.currentThread().hashCode());
            File[] list = delegate.getFiles(dir, useFileHiding);
            int n = 0;
            FileFilter filter = FileChooser.this.getFileFilter();
            for (int i = 0; i < list.length && useFileHiding && filter != null; i++) {
                if (isHiddenFile(list[i]) || !filter.accept(list[i])) {
                    list[i] = null;
                } else {
                    n++;
                }
            }
            File[] files = new File[n];
            int j = 0;
            for (int i = 0; i < list.length && j < n; i++) {
                if (list[i] != null) {
                    files[j] = list[i];
                    j++;
                }
            }
            //Debug.traceln("<getFiles " + dir + " thread " + Thread.currentThread().hashCode());
            return files;
        }

        public boolean isHiddenFile(File f) {
            // same multithreading issues as in getFiles
            FileFilter filter = FileChooser.this.getFileFilter();
            if (!Util.isWindows()) {
                return filter != FileChooser.this.getAcceptAllFileFilter() && f.getName().startsWith(".");
            }
            return filter != null && !filter.accept(f);
        }

        public File createNewFolder(File containingDir) throws IOException {
            return delegate.createNewFolder(containingDir);
        }

        public boolean isRoot(File f) {
            return delegate.isRoot(f);
        }

        public Boolean isTraversable(File f) {
            if (Util.isWindows()) {
                // workaround for: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
                if (!f.isDirectory()) {
                    String name = f.getName().toLowerCase();
                    if (Util.isArchiveFileType(name)) {
                        return Boolean.FALSE;
                    } else if (name.endsWith(".lnk") && isLinkToArchive(f)) {
                        return Boolean.FALSE;
                    }
                }
            }
            return delegate.isTraversable(f);
        }

        public String getSystemDisplayName(File f) {
            return delegate.getSystemDisplayName(f);
        }

        public String getSystemTypeDescription(File f) {
            return delegate.getSystemTypeDescription(f);
        }

        public Icon getSystemIcon(File f) {
            return delegate.getSystemIcon(f);
        }

        public boolean isParent(File folder, File file) {
            return delegate.isParent(folder, file);
        }

        public File getChild(File parent, String fileName) {
            return delegate.getChild(parent, fileName);
        }

        public boolean isFileSystem(File f) {
            return delegate.isFileSystem(f);
        }

        public boolean isFileSystemRoot(File dir) {
            return delegate.isFileSystemRoot(dir);
        }

        public boolean isDrive(File dir) {
            return delegate.isDrive(dir);
        }

        public boolean isFloppyDrive(File dir) {
            return delegate.isFloppyDrive(dir);
        }

        public boolean isComputerNode(File dir) {
            return delegate.isComputerNode(dir);
        }

        public File[] getRoots() {
            return delegate.getRoots();
        }

        public File getHomeDirectory() {
            return delegate.getHomeDirectory();
        }

        public File getDefaultDirectory() {
            return delegate.getDefaultDirectory();
        }

        public File createFileObject(File dir, String filename) {
            return delegate.createFileObject(dir, filename);
        }

        public File createFileObject(String path) {
            return delegate.createFileObject(path);
        }

        public File getParentDirectory(File dir) {
            return delegate.getParentDirectory(dir);
        }

        protected File createFileSystemRoot(File f) {
            return super.createFileSystemRoot(f);
        }

    }


}