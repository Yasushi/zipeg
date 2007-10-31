package com.zipeg;

import javax.swing.tree.TreeNode;
import java.util.*;
import java.io.File;

public interface Archive {

    /** @return true if archive is open */
    boolean isOpen();

    /**@return true if archive is open
     */
    boolean isNested();

    /** @return root element of the archive */
    TreeNode getRoot();

    /** @return archive absolute or relative pathname */
    String getName();

    /** @return for nested archives returns parent name. For root archives same as getName. */
    String getParentName();

    /** @return true if archive is encrypted */
    public boolean isEncrypted();

    /** @return archive cache directory */
    File getCacheDirectory();

    /**
     * extacts list of tree elements into specified directory.
     * posts progress events
     * @param treeElements if null the whole archive is extracted
     * @param directory to extract to
     * @param quit application after extraction? round trip via "extractionComplete" event
     */
    void extract(List treeElements, File directory, boolean quit);

    /**
     * extacts single element into cache direcotry. Does not post
     * any progress events.
     * @param element to extract
     * @param done done.run() will be called on the background thread
     */
    void extract(TreeElement element, Runnable done);

    /**
     * extacts single element which is known to be an archive
     * into new cache direcotry and opens that archive.
     * @param element to extract
     */
    void extractAndOpen(TreeElement element);

    /**
     * @param dotExtension e.g. ".mp3", ".jpg", ".png", ".java"
     * @return number of files in the archive with this extension
     */
    int getExtensionCounter(String dotExtension);

    /** close archive. Blocks till archive is closed. */
    void close();

}
