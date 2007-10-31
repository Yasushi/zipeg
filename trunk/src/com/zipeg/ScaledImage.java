package com.zipeg;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

import com.sun.imageio.plugins.jpeg.JPEG;

public class ScaledImage extends BufferedImage {

    public ScaledImage(int width, int height, int imageType) {
        super(width, height, imageType);
    }

    public static BufferedImage createThumbnail(File file) {
        if (!file.canRead())
            return null;
//      Debug.traceln("file: " + file);
        BufferedImage bi = null;
        ImageInputStream iis = null;
        ImageReader r = null;
        try {
            iis = ImageIO.createImageInputStream(file);
            if (iis != null) {
                Iterator readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    r = (ImageReader)readers.next();
                    boolean jpg = r.getClass().getName().indexOf("JPEGImageReader") >= 0;
                    r.setInput(iis);
                    ImageReadParam p = r.getDefaultReadParam();
                    int w = r.getWidth(0);
                    int h = r.getHeight(0);
                    if (w <= 0 || h <= 0) {
                        return null;
                    }
                    Dimension size = w < h ? new Dimension(120, 160) : new Dimension(160, 120);
                    if (w <= size.width && h <= size.height) {
                        size.width = w;
                        size.height = h;
                    }
                    Dimension dst = new Dimension(w, h);
                    int s = 0;
                    while ((dst.width > size.width || dst.height > size.height) &&
                           dst.width > 4 && dst.height > 4) {
                        s++;
                        dst.width = w / (1 << s);
                        dst.height = h / (1 << s);
                    }
                    if (s > 0) {
                        p.setSourceSubsampling(1 << s, 1 << s, 0, 0);
                    }
                    /* http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4705399
                       http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6236802
                       http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5073407
                    */
                    if (jpg) {
                        Object colorSpaceCode = null;
                        try {
                            Field f = r.getClass().getDeclaredField("colorSpaceCode");
                            f.setAccessible(true);
                            colorSpaceCode = f.get(r);
                        }
                        catch (NoSuchFieldException e) {
                            // ignore
                        }
                        catch (IllegalAccessException e) {
                            // ignore
                        }
                        if (colorSpaceCode instanceof Integer &&
                           (((Integer)colorSpaceCode).intValue() == JPEG.JCS_RGB ||
                            ((Integer)colorSpaceCode).intValue() == JPEG.JCS_GRAYSCALE)) {
                            /*  ImageIO fails to decode some non-YCbCr JPEGs
                                http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=6246622
                                ImageIO does not correctly read some standard JPG files
                                http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4881314
                                ImageIO.read improperly decodes Nikon Jpegs
                                http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372769
                            */
                            close(r, iis);
                            r = null;
                            iis = null;
                            return null;
                        }
                        if ((colorSpaceCode instanceof Integer) &&
                            ((Integer)colorSpaceCode).intValue() == JPEG.JCS_YCbCr) {
                            bi = r.read(0, p);
                        }
                    } else {
                        ImageTypeSpecifier type = null;
                        for (Iterator i = r.getImageTypes(0); i.hasNext();) {
                            ImageTypeSpecifier t = (ImageTypeSpecifier)i.next();
                            if (t.getColorModel().getColorSpace().isCS_sRGB()) {
                                type = t;
                            }
                        }
                        if (type == null) {
                            return null;
                        }
                        p.setDestinationType(type);
                        bi = r.read(0, p);
                    }
                    if (bi == null) {
                        return null;
                    }
                    if (bi.getType() == TYPE_CUSTOM) {
                        BufferedImage di;
                        if (bi.getSampleModel().getNumBands() == 3) {
                            di = new BufferedImage(bi.getWidth(), bi.getHeight(), TYPE_3BYTE_BGR);
                        }
                        else if (bi.getSampleModel().getNumBands() == 4) {
                            di = new BufferedImage(bi.getWidth(), bi.getHeight(), TYPE_4BYTE_ABGR);
                        }
                        else {
                            throw new IOException("numBands " + bi.getSampleModel().getNumBands() + " for " + file);
                        }
                        di.setData(bi.getData());
                        bi = di;
                    }
                }
            }
        }
        catch (Throwable ex) {
            Debug.traceln("ScaledImage.createThumbnail failed " + file + " " + ex.getMessage());
        }
        finally {
            close(r, iis);
            // noinspection UnusedAssignment
            r = null;
            // noinspection UnusedAssignment
            iis = null;
        }
        if (bi == null) {
            return null;
        }
//      Debug.traceln("ScaledImage.createThumbnail " + bi.getWidth() + "x" + bi.getHeight() + " " + file);
        int type = bi.getType() == TYPE_3BYTE_BGR ? TYPE_3BYTE_BGR : TYPE_4BYTE_ABGR;
        BufferedImage di = new BufferedImage(bi.getWidth(), bi.getHeight(), type);
        Graphics g = di.getGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return di;
    }

    private static void close(ImageReader reader, ImageInputStream stream) {
        if (reader != null) {
            reader.abort();
            reader.dispose();
        }
        if (stream != null) {
            try { stream.close(); } catch (IOException x) { /* ignore */ }
        }
    }

}
