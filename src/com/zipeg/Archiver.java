package com.zipeg;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.Iterator;

public interface Archiver {
    ZipEntry getEntry(String name);
    InputStream getInputStream(ZipEntry entry) throws IOException;
    void extractEntry(ZipEntry entry, File dest, String pwd) throws IOException;
    String getName();
    Iterator getEntries();
    int size();
    void close() throws IOException;
    boolean isEncrypted();
    boolean isDirEncrypted();
}
