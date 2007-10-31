package com.zipeg;

import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import java.io.*;

public class FilteredFileSystemView extends FileSystemView {

    private FileSystemView delegate;
    private JFileChooser chooser;
    private final FileFilter all;


    public FilteredFileSystemView(JFileChooser fc, FileSystemView view) {
        this.delegate = view;
        this.chooser = fc;
        this.all = chooser.getAcceptAllFileFilter();
        assert this.all != null;
    }

    public File[] getFiles(File dir, boolean useFileHiding) {
        // this method is called from 2 threads on Mac
        // see AquaDirectoryModel.LoadFilesThread
        // The second call kicks in on CHANGE_FILTER property change.
        // Note that chooser.getFileFilter() is single variable
        // access and is hopefully "volatile" at least for now.
        File[] list = delegate.getFiles(dir, useFileHiding);
        int n = 0;
        FileFilter filter = chooser.getFileFilter();
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
        return files;
    }

    public boolean isHiddenFile(File f) {
        // same multithreading issues as in getFiles
        FileFilter filter = chooser.getFileFilter();
        if (!Util.isWindows()) {
            return !all.equals(filter) && f.getName().startsWith(".");
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

    private static boolean isLinkToArchive(File f) {
        byte[] content = Util.readFile(f);
        String s = new String(content).toLowerCase();
        return s.indexOf(".zip") >= 0 || s.indexOf(".cab") >= 0 || s.indexOf(".rar") >= 0 || s.indexOf(".tar") >= 0;
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
