package com.zipeg;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public final class Resources {

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
        return image.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
    }

    public static Image getImage(String name) {
        String location = "resources/" + name + ".png";
        return Toolkit.getDefaultToolkit().createImage(readBytes(location));
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

    private static byte[] readBytes(String location) {
        InputStream s = null;
        try {
            s = Resources.class.getResourceAsStream(location);
            assert  s != null : location;
            return Util.readBytes(s);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            Util.close(s);
        }
    }

}
