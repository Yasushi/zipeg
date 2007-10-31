package com.zipeg;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

// http://forum.java.sun.com/thread.jspa?threadID=391403&messageID=3963007

public class TransparentWindow extends JWindow {

    private Image screen;
    private BufferedImage buffer;
    private BufferedImage image;
    private Graphics2D graphics;

    public TransparentWindow(BufferedImage img, int x, int y) {
        image = img;
        try {
            Robot robot = new Robot();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            screen = robot.createScreenCapture(new Rectangle(0, 0, d.width, d.height));
        }
        catch (AWTException e) {
            throw new Error(e);
        }
        setBounds(x, y, img.getWidth(), img.getHeight());
        setVisible(true);
        buffer = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        graphics = (Graphics2D)buffer.getGraphics();
    }

    public void dispose() {
        super.dispose();
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        if (buffer != null) {
            buffer.flush();
            buffer = null;
        }
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        graphics.drawImage(screen, 0, 0, getWidth(), getHeight(), getX(), getY(),
                           getX() + getWidth(), getY() + getHeight(), null);
        Composite c = g2d.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        graphics.drawImage(image, 0, 0, null);
        graphics.setComposite(c);
        g2d.drawImage(buffer, 0, 0, null);
    }

    public void update(Graphics g) {
        this.paint(g);
    }

}
