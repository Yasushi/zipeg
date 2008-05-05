package com.zipeg;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class Resources {

    /* On Mac OS X 10.5 non priviliged user may (and will) download application
       on Desktop or Downloads folder, start it and, while it is running,
       move it to /Applications folder. Unless jar file is kept open this
       move will proceed and next call to any ClassLoader.getResourceAsStream
       will fail. Thus, deliberatly keep jar file open.
    */
    private static JarFile jar;

    public static ImageIcon getImageIcon(String name) {
        return new ImageIcon(getImage(name));
    }

    public static boolean hasImageIcon(String name) {
        return  Resources.class.getResource("resources/" + name + ".png") != null;
    }

    public static ImageIcon getImageIcon(String name, int size) {
        return new ImageIcon(getImage(name, size));
    }

    public static Image getImage(String name, int size) {
        Image image = getImage(name);
        return ensureImage(image.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING));
    }

    public static Image getImage(String name) {
        String location = "resources/" + name + ".png";
        return ensureImage(Toolkit.getDefaultToolkit().createImage(readBytes(location)));
    }

    private static Image ensureImage(Image image) {
        MediaTracker mt = new MediaTracker(new JComponent() {});
        mt.addImage(image, 0);
        try {
            mt.waitForID(0);
        } catch (InterruptedException e) {
            /* ignore */
        } finally {
            mt.removeImage(image);
        }
        assert image.getWidth(null) > 0 : image.getWidth(null);
        assert image.getHeight(null) > 0 : image.getHeight(null);
        return image;
    }

    public static String getUrl(String name) {
        String location = "resources/" + name + ".png";
        java.net.URL url = Resources.class.getResource(location);
        assert  url != null : location;
        return url.toExternalForm();
    }

    public static byte[] getBytes(String name) {
        String location = "resources/" + name + ".png";
        return readBytes(location);
    }

    private static JarFile getJarFile(String u) throws IOException {
        if (jar == null) {
            int sep = u.lastIndexOf('!');
            String j = u.substring(0, sep);
            if (j.startsWith("jar:file:")) {
                j = j.substring("jar:file:".length());
            }
            if (j.startsWith("file:")) {
                j = j.substring("file:".length());
            }
            jar = new JarFile(j);
        }
        return jar;
    }

    public static InputStream getResourceAsStream(String location) {
        try {
            java.net.URL url = Resources.class.getResource(location);
            assert url != null : location;
            String u = url.getFile().replaceAll("%20", " ");
            InputStream s;
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4730642
            if (u.toLowerCase().indexOf(".jar!/") >= 0) {
                JarFile jar = getJarFile(u);
                ZipEntry ze = jar.getEntry(u.substring(u.lastIndexOf('!') + 2));
                s = jar.getInputStream(ze);
            } else {
                s = Resources.class.getResourceAsStream(location);
            }
            assert s != null : location;
            return s;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private static byte[] readBytes(String location) {
        InputStream s = getResourceAsStream(location);
        try {
            s = getResourceAsStream(location);
            assert s != null : location;
            return Util.readBytes(s);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            Util.close(s);
        }
    }

    public static BufferedImage asBufferedImage(Image img) {
        return asBufferedImage(img, 0, 0);
    }

    public static BufferedImage asBufferedImage(Image img, int dx, int dy) {
        assert img.getWidth(null) > 0 : img.getWidth(null);
        assert img.getHeight(null) > 0 : img.getHeight(null);
        int w = img.getWidth(null) + Math.max(dx, 0);
        int h = img.getHeight(null) + Math.max(dy, 0);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = null;
        try {
            g = bi.getGraphics();
            g.drawImage(img, dx, dy, null);
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return bi;
    }

    /** adjust Saturation (i == 1) or Brightness (i == 2)
     * @param bi image to adjust parameters of (must be ABGR
     * @param adjust value to adjust must be > 0.0f
     * @param i index of hsb to adjust
     */
    public static void adjustHSB(BufferedImage bi, float adjust, int i) {
        assert bi.getType() == BufferedImage.TYPE_INT_ARGB : "must be TYPE_INT_ARGB";
        int n = bi.getData().getNumBands();
        assert n == 4 : "must have alpha component";
        assert adjust > 0.0f;
        assert i > 0 : "adjusting hue is strange action with unpredictable color shift";
        int[] pixels = new int[bi.getWidth() * bi.getHeight() * n];
        float[] hsb = new float[3];
        bi.getData().getPixels(0, 0, bi.getWidth(), bi.getHeight(), pixels);
        int ix = 0;
        WritableRaster wr = bi.getRaster();
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int r = pixels[ix];
                int g = pixels[ix + 1];
                int b = pixels[ix + 2];
                int a = pixels[ix + 3];
                Color.RGBtoHSB(r, g, b, hsb);
                assert hsb[0] >= 0 && hsb[1] >= 0 && hsb[2] >= 0;
                hsb[i] = Math.max(0.0f, Math.min(hsb[i] * adjust, 1.0f));
                int c = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
                wr.getDataBuffer().setElem(ix / n, (c & 0xFFFFFF) | (a << 24));
                ix += n;
            }
        }
    }

}
