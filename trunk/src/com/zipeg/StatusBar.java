package com.zipeg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

public final class StatusBar extends JToolBar {

    private JLabel status;
    private JLabel info;
    private SubtleMessage message;
    private JPanel right;
    private JProgressBar progress;
    private final int height;
    private final Image bulbR = Resources.getImage("bulb_r", 16);
    private final Image bulbG = Resources.getImage("bulb_g", 16);
    private final boolean paintGrowBox = Util.isWindows() ||
            (Util.isMac() && Util.getJavaVersion() < 1.6 && Flags.getFlag(Flags.METAL));

    StatusBar() {
        setLayout(new BorderLayout());
        if (!Util.isMac()) {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(), this.getBorder()));
        } else {
            setBorder(null);
            setOpaque(false);
        }
        setFocusable(false);
        setFloatable(false);
        height = (int)(getFontMetrics(getFont()).getHeight() * (Util.isMac() ? 1.5 : 1.7));

        JPanel left = new JPanel(new BorderLayout());
        add(left, BorderLayout.WEST);
        add(new JPanel(), BorderLayout.CENTER);

        status = new JLabel("", SwingConstants.LEADING) {
            public Dimension getPreferredSize() {
                return new Dimension((StatusBar.this.getWidth() - 72) / 2, height);
            }
        };
        left.add(status, BorderLayout.WEST);
        status.setAlignmentY(BOTTOM_ALIGNMENT);
        left.add(new JPanel(), BorderLayout.CENTER);
        info = new JLabel("", SwingConstants.TRAILING);
        left.add(info, BorderLayout.EAST);
        info.setAlignmentY(BOTTOM_ALIGNMENT);

        left.add(new JPanel(), BorderLayout.CENTER);

        right = new JPanel(new BorderLayout());
        GrowBox box = new GrowBox();
        box.setAlignmentY(BOTTOM_ALIGNMENT);
        right.add(box, BorderLayout.EAST);
        add(right, BorderLayout.EAST);
    }

    public void addNotify() {
        super.addNotify();
        Actions.addListener(this);
    }

    public void removeNotify() {
        Actions.removeListener(this);
        super.removeNotify();
    }

    public Insets getInsets() {
        Insets i = super.getInsets();
        if (Util.isMac() && Util.getJavaVersion() < 1.6 && Util.getOsVersion() < 10.5) {
            i.top += 1;
            i.bottom += 2;
        }
        return i;
    }

    public Dimension getPreferredSize() {
        return new Dimension(Integer.MAX_VALUE, height);
    }

    /** method to enable commands state
     * @param map command ids (like "commandFileOpen" to Boolean.TRUE/FALSE
     * @noinspection UnusedDeclaration
     */
    public static void updateCommandState(Map map) {
    }

    private void createProgress() {
        // create and remove progress bar "on-demand" because otherwise
        // apple.laf.AquaProgressBarUI.Animator will keep posting messages.
        // BUG in 1.4.2 Animator is always posting messages even when there is no
        // progress bar in sight
        assert progress == null;
        progress = new JProgressBar();
        right.add(progress, BorderLayout.WEST);
        progress.setAlignmentY(BOTTOM_ALIGNMENT);
        progress.setPreferredSize(new Dimension(100, height));
        progress.setVisible(false);
        progress.setMinimum(0);
        progress.setMaximum(99);
        progress.setValue(0);
        progress.setVisible(true);
        right.revalidate();
        repaint();
    }

    private void removeProgress() {
        assert progress != null;
        right.remove(progress);
        right.revalidate();
        repaint();
        progress = null;
    }

    /** set the text into status panel of status bar
     * Do not call directly from background threads.
     * Use Actions.postEvent("setInfo", "text") instead.
     * @param param message. plain text or html
     */

    public void setStatus(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        String s = (String)param;
        if (!Util.equals(s, status.getText())) {
            status.setText(s);
        }
    }

    /** set the text into info panel of status bar
     * Do not call directly from background threads.
     * Use Actions.postEvent("setInfo", "text") instead.
     * @param param message. plain text or html
     */

    public void setInfo(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        String s = (String)param;
        if (!Util.equals(s, info.getText())) {
            info.setText(s);
        }
    }

    /** shows done or error message to the user
     * Do not call directly from background threads.
     * Use Actions.postEvent("showMessage", "message text") instead.
     * Message will be wraped into < html > tags and colored Red or Blue
     * depending on "error: " prefix.
     * @param param message. Error messages must start with "error: "
     */

    public void setMessage(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        String s = (String)param;
        if (message != null) {
            message.dismiss();
            message = null;
        }
        if (s.length() > 0) {
            message = new SubtleMessage(s);
        }
    }

    public void commandFileClose() {
        setMessage("");
        setStatus("");
        setInfo("");
    }

    public void setProgress(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        Float p = (Float)param;
        float f = p.floatValue();
        assert 0 <= f && f <= 1.0;
        if (f > 0) {
            setMessage("");
            if (progress == null) {
                createProgress();
                Cursor wait = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
                MainFrame.getInstance().pushCursor(wait);
                // because of:
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5023342
                //   MainFrame.getInstance().setEnabled(false);
                // cannot be used and even when used does not disable menus on Mac OS X
                Actions.setEnabled(false);
            }
            progress.setValue(Math.round(100 * f));
        } else if (progress != null) {
            removeProgress();
            Actions.setEnabled(true);
            MainFrame.getInstance().popCursor();
        }
    }

    public boolean inProgress() {
        return progress != null;
    }

    private class GrowBox extends JComponent {

        GrowBox() {
            setOpaque(false);
            setFocusable(false);
            setRequestFocusEnabled(false);
        }

        private final Color b = new JLabel().getBackground();
        private final Color[] lines = Util.isWindows() ? new Color[]{b.brighter(), b.darker()} :
                new Color[]{Color.DARK_GRAY, Color.WHITE, Color.LIGHT_GRAY, Color.GRAY};

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            int x = 0;
            Image bulb = MainFrame.getInstance().inProgress() ? bulbR : bulbG;
            int Y = Util.isMac() ? 2 : 0;
            if (Util.isMac() && Util.getJavaVersion() >= 1.6) {
                Y += 3; // no insets
            }
            g2d.drawImage(bulb, x, Y, null);
            x += bulb.getWidth(null);
            Color s = g2d.getColor();
            if (Util.isWindows()) {
                int n = getWidth() - 1;
                int y = 0;
                for (int dy = 0; dy < n; dy += 4) {
                    for (int dx = dy; dx > 0; dx -= 4) {
                        g2d.setColor(lines[0]);
                        g2d.fill(new Rectangle(n - dx + 1, y + dy + 1, 2, 2));
                        g2d.setColor(lines[1]);
                        g2d.fill(new Rectangle(n - dx, y + dy, 2, 2));
                    }
                }
            } else if (paintGrowBox && Util.isMac()) {
                int y = getHeight() - 21;
                for (int i = 0; i < 15; i++) {
                    Color c = lines[i % 4];
                    g2d.setColor(c);
                    g2d.drawLine(x + i, y + 14, x + 14, y + i);
                }
            }
            g2d.setColor(s);
        }

        public Dimension getPreferredSize() {
            return new Dimension(34, height);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    private static class SubtleMessage extends JLabel {

        private float transparency = 0.1f;
        private JComponent gp = (JComponent) MainFrame.getInstance().getGlassPane();
        private Sound sound;

        SubtleMessage(String message) {
            super(wrapMessage(message), CENTER);
            setOpaque(false);
            setFocusable(true);
            setRequestFocusEnabled(true);
            requestFocus(); // so ESC will work
            JComponent gp = (JComponent) MainFrame.getInstance().getGlassPane();
            gp.add(this);
            gp.setVisible(true);
            setBounds(gp.getBounds());
            repaint();
            if (Flags.getFlag(Flags.PLAY_SOUNDS)) {
                String r = isError(message) ? "resources/error.wav" : "resources/done.wav";
                sound = new Sound(Resources.getResourceAsStream(r), false);
            }
        }

        private boolean isError(String s) {
            return s.startsWith("error:");
        }

        private static String wrapMessage(String s) {
            if (s.startsWith("error:")) {
                return "<html><font size=6 color=red>" + s + "</font></html>";
            } else {
                return "<html><font size=6 color=blue>" + s + "</font></html>";
            }
        }

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            Composite c = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.abs(transparency)));
            super.paint(g);
            g2d.setComposite(c);
            if (transparency == 0) {
                close();
            } else if (Math.abs(transparency) < 1.0) {
                Util.invokeLater(100, new Runnable(){
                    public void run() {
                        if (transparency > 0) {
                            transparency = Math.min(1.0f, transparency + 0.05f);
                            repaint();
                        } else if (transparency < 0) {
                            transparency = Math.min(0, transparency + 0.05f);
                            repaint();
                        }
                    }
                });
            }
        }

        public void addNotify() {
            super.addNotify();
            gp.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    removeKeyListener(this);
                    dismiss();
                }
            });
            gp.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    removeMouseMotionListener(this);
                    dismiss();
                }
            });
            Util.invokeLater(5*1000, new Runnable(){
                public void run() { dismiss(); }
            });
        }

        public void removeNotify() {
            super.removeNotify();
        }

        private void dismiss() {
            if (transparency > 0) {
                transparency = -0.9f;
                repaint();
            }
        }

        private void close() {
            if (gp != null) {
                gp.remove(SubtleMessage.this);
                gp.setVisible(false);
                gp = null;
            }
            if (sound != null) {
                sound.stop();
                sound = null;
            }
        }
    }

}
