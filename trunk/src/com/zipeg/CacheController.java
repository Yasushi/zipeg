package com.zipeg;

import javax.imageio.ImageIO;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

public class CacheController {

    private final static CacheController instance = new CacheController();

    private CacheController() {
    }

    public static CacheController getInstance() {
        return instance;
    }

    public void enqueue(Archive a, TreeElement element, final Runnable done) {
        assert IdlingEventQueue.isDispatchThread();
        // must occupy slot now to prevent multiple requests on the queue
        final File file = new File(element.getFile());
        CacheEntry c = (CacheEntry)Cache.getInstance().get(file);
        if (element.isDirectory() || element.getSize() > 100 * Util.MB || c != null) {
            done.run();
        }
        else {
//          Cache.getInstance().remove(file);
            File tmp = new File(a.getCacheDirectory(), element.getFile());
            if (tmp.exists()) {
                // this means the archive has a duplicate file.
                // do not prefetch second version of it.
                done.run();
                return;
            }
            final CacheEntry ce = new CacheEntry();
            ce.element = element;
            ce.temp = tmp;
            ce.temp.deleteOnExit();
            Cache.getInstance().put(file, ce);
            Runnable extracted = new Runnable() {
                public void run() {
                    assert !IdlingEventQueue.isDispatchThread();
                    try {
                        if (Zipeg.getArchive() != null && ce.temp != null) {
                            assert file != null;
                            if (ce.temp.canRead() && isSupportedImageType(file)) {
                                extractThumbnail(ce);
                            }
                            EventQueue.invokeLater(new Runnable(){
                                public void run() {
                                    assert IdlingEventQueue.isDispatchThread();
                                    done.run();
//                                  Debug.traceln("extracted " + ce.temp);
                                }
                            });
                        }
                    } catch (Throwable ignore) {
                        // ignore
                    }
                }
            };
            Zipeg.getArchive().extract(element, extracted);
        }
    }

    private void extractThumbnail(CacheEntry ce) {
        assert !IdlingEventQueue.isDispatchThread();
        RandomAccessFile raf = null;
        FileChannel channel = null;
        Throwable x = null;
        try {
//          Debug.traceln("extractThumbnail " + file + " tmp " + ce.temp);
            byte[] thumb;
            FileInputStream in = null;
            try {
                in = new FileInputStream(ce.temp);
                thumb = Exif.read(in);
            } catch (IOException iox) {
//              Debug.traceln("warning: failed to read EXIF from " + file);
                thumb = null;
            } finally {
                Util.close(in);
            }
            if (thumb != null) {
                ByteArrayInputStream is = new ByteArrayInputStream(thumb, 0, thumb.length);
                ce.image = ImageIO.read(is);
                ce.thumb = makeThumbFile(ce.temp);
                ce.thumb.deleteOnExit();
                raf = new RandomAccessFile(ce.thumb, "rw");
                channel = raf.getChannel();
                channel.write(ByteBuffer.wrap(thumb));
                channel.close();
                channel = null;
                raf.close();
                raf = null;
            } else {
                ce.image = ScaledImage.createThumbnail(ce.temp);
                if (ce.image != null) {
                    String type = ce.image.getType() != BufferedImage.TYPE_3BYTE_BGR ? "png" : "jpg";
                    ce.thumb = makeThumbFile(ce.temp);
                    ce.thumb.deleteOnExit();
                    ImageIO.write(ce.image, type, ce.thumb);
                }
            }
        } catch (Throwable t) {
            x = t;
        } finally {
            if (channel != null) {
                try { channel.close();  } catch (IOException iox) { /* ignore */ }
            }
            if (raf != null) {
                try { raf.close(); } catch (IOException iox) { /* ignore */ }
            }
        }
        if (x != null) {
            ce.image = null;
            if (ce.thumb != null) {
                ce.thumb.delete();
            }
            ce.thumb = null;
            // Actions.reportError(x.getMessage());
            // throw new Error(x);
            Debug.traceln("CacheController.extractThumbnail " + x.getMessage() +
                          " while reading " + ce.temp);
        }
    }

    private static File makeThumbFile(File tmp) {
        File dir = new File(tmp.getParent() + ".t");
        dir.mkdirs();
        return new File(dir, tmp.getName());
    }

    private static String getExtension(File file) {
        String fname = file.getName().toLowerCase();
        int ix = fname.lastIndexOf('.');
        return ix >= 0 ? fname.substring(ix + 1) : "";
    }

    private static boolean isSupportedImageType(File file) {
        String ext = getExtension(file);
        // TIFFImageReader does not implement subsampling and is slow clumsy and bad...
        return ext.length() > 0 &&
               ("jpg".equals(ext) || "jpeg".equals(ext) ||
//              "tif".equals(ext) || "tiff".equals(ext) ||
                "png".equals(ext) || "gif".equals(ext) ||
                "bmp".equals(ext));
    }
}
