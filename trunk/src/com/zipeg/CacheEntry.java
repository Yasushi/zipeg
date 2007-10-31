package com.zipeg;

import java.awt.image.BufferedImage;
import java.io.File;

public class CacheEntry {

    public TreeElement element;
    public BufferedImage image;
    public File thumb;
    public File temp;

    public void delete() {
        assert IdlingEventQueue.isDispatchThread();
        if (image != null) {
            image.flush();
            image = null;
        }
        if (temp != null && temp.exists()) {
            if (!temp.delete()) {
                temp.deleteOnExit();
                temp = null;
            }
        }
        if (thumb != null && thumb.exists()) {
            if (!thumb.delete()) {
                thumb.deleteOnExit();
                thumb = null;
            }

        }
    }

}
