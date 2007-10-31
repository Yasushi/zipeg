package com.zipeg;

import java.util.*;

public interface TreeElement {

    /**
     * @return same as getName
     */
    String toString();

    /**
     * @return short (last) name of the element
     */
    String getName();

    /**
     * @return true if item in the archive is encrypted
     */
    public boolean isEncrypted();

    /**
     * @return not null string if item had a decompression error
     */
    public String getError();

    /**
     * @return pathname with '/' as path seprators
     */
    String getFile();

    /**
     * @return true if the element is directory
     */
    boolean isDirectory();

    /**
     * @return size in bytes of uncompressed file
     */
    long getSize();

    /**
     * @return size in bytes of uncompressed resource fork of a file
     */
    long getResourceForkSize();

    /**
     * @return size in bytes of compressed file
     */
    long getCompressedSize();

    /**
     * @return size in bytes of compressed resource fork
     */
    long getResourceForkCompressedSize();

    /**
     * @return element last modification time
     */
    long getTime();

    /**
     * @return same as getName
     */
    String getComment();

    /**
     * @return cumulative number of all not directory descendants
     */
    int getDescendantFileCount();

    /**
     * @return cumulative number of all directory descendants
     */
    int getDescendantDirectoryCount();

    /**
     * @return cumulative size in bytes of all not directory descendants
     */
    long getDescendantSize();

    /**
     * @param list adds all non directory descendants of this element to the list
     */
    void collectDescendants(List list);

    /**
     * @return number of all children
     */
    int getChildrenCount();

    /**
     * @return all children iterator in alphabetic order of names
     */
    Iterator getChildren();

}
