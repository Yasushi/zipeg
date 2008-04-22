package com.zipeg;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.awt.event.*;
import java.awt.*;
import java.awt.dnd.*;

public final class MainFrame extends JFrame {

    private static final Rectangle DEFAULT = new Rectangle(48, 48, 800 - 48, 600 - 48);
    private static final Dimension SMALLEST = new Dimension(480, 240);
    private static MainFrame frame = null;
    private static JFrame offscreen; // hosts menu when main frame is invisible. see setVisible
    private static final LinkedList cursors = new LinkedList();
    private static Robot robot = null;
    static {
        try { robot = new Robot(); } catch (AWTException e) { robot = null; }
    }

    MainFrame() {
        assert frame == null;
        frame = this;
        setJMenuBar(Actions.createMenuBar());
        ContentPane cp = new ContentPane();
        setContentPane(cp);
        if (Flags.getFlag(Flags.METAL)) {
            Color transparent = new Color(255, 255, 255, 255);
            setBackground(transparent);
            cp.setBackground(transparent);
            cp.setOpaque(false);
        }
        setDefaultCloseOperation(Util.isMac() ? WindowConstants.HIDE_ON_CLOSE :
                                                WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(Resources.getImage("zipeg32x32"));
        super.setTitle("Zipeg");
        JComponent gp = (JComponent)MainFrame.getInstance().getGlassPane();
        gp.setLayout(null); // glass pane is user for AutoCompleteDropDown
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                if (!Util.isMac()) {
                    Actions.postEvent("commandFileExit");
                } else {
                    Actions.postEvent("commandFileClose");
                }
            }
        });
        cp.addComponentListener(new MinimumSizeLimiter());
        restoreLayout();
        addComponentListener(new ComponentAdapter(){
            public void componentResized(ComponentEvent e) { saveLayout(); }
            public void componentMoved(ComponentEvent e) { saveLayout(); }
        });
    }

    public void addNotify() {
        super.addNotify();
        Actions.addListener(this);
    }

    public void removeNotify() {
        Actions.removeListener(this);
        super.removeNotify();
    }

    public static JMenuBar getMenu() {
        JMenuBar mb = frame != null ? frame.getJMenuBar() : null;
        return mb == null ? mb : (offscreen != null ? offscreen.getJMenuBar() : null);
    }

    public void setVisible(boolean b) {
        if (Util.isMac()) { // keep Macintosh menu up
//          http://lists.apple.com/archives/java-dev/2003/Mar/msg00960.html
            if (offscreen == null) {
                offscreen = new JFrame();
                offscreen.setUndecorated(true);
                offscreen.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
                offscreen.setSize(0, 0);
                offscreen.setEnabled(false);
                offscreen.setVisible(true);
            }
            if (b) {
                JMenuBar mb = offscreen.getJMenuBar();
                if (mb != null) {
                    setJMenuBar(mb);
                    offscreen.setJMenuBar(null);
                }
            } else {
                JMenuBar mb = getJMenuBar();
                if (mb != null) {
                    offscreen.setJMenuBar(mb);
                    setJMenuBar(null);
                }
            }
        }
        super.setVisible(b);
    }


    public static MainFrame getInstance() {
        return frame;
    }

    public void dispose() {
        frame = null;
        super.dispose();
    }

    public List getSelected() {
        return ((ContentPane)getContentPane()).getSelected();
    }

    public String getDestination() {
        return ((ContentPane)getContentPane()).getDestination();
    }

    public void saveDestination() {
        ((ContentPane)getContentPane()).saveDestination();
    }

    public boolean inProgress() {
        return ((ContentPane)getContentPane()).inProgress();
    }

    public static JFrame getTopFrame() {
        if (frame != null && !frame.isVisible()) {
            frame.setVisible(true);
        }
        return frame != null && frame.isVisible() ? frame : null;
    }

    public void pushCursor(Cursor c) {
        assert IdlingEventQueue.isDispatchThread();
        cursors.add(getCursor());
        setCursor(c);
    }

    public void popCursor() {
        assert IdlingEventQueue.isDispatchThread();
        setCursor((Cursor)cursors.removeLast());
    }

    public void setTitle(String s) {
        if (s == null || "".equals(s)) {
            s = "Zipeg";
        } else {
            s = "Zipeg: " + s;
        }
        if (!s.equals(super.getTitle())) {
            super.setTitle(s);
        }
    }

    public static void showError(Object param) {
        String text = (String)param;
        // TODO: this is a good place to log errors.
        JOptionPane.showMessageDialog(MainFrame.getTopFrame(),
                text, "Zipeg: Error", JOptionPane.ERROR_MESSAGE);
    }

    public void updateCommandState(Map m) {
    }

    public void addDropTargetAdapter(DropTargetAdapter dta) {
        ((ContentPane)getContentPane()).addDropTargetAdapter(dta);
    }

    private class MinimumSizeLimiter extends ComponentAdapter {

        public void componentResized(ComponentEvent e) {
            Dimension size = getSize();
            if (size.width < SMALLEST.width || size.height < SMALLEST.height) {
                boolean limit = false;
                if (size.width < SMALLEST.width) {
                    size.width = SMALLEST.width;
                    limit = true;
                }
                if (size.height < SMALLEST.height) {
                    size.height = SMALLEST.height;
                    limit = true;
                }
                if (!limit || size.equals(getSize())) {
                    return;
                }
                setResizable(false);
                if (robot != null) {
                    robot.mouseRelease(InputEvent.BUTTON1_MASK |
                            InputEvent.BUTTON2_MASK |
                            InputEvent.BUTTON3_MASK);
                }
                final Dimension s = size;
                Util.invokeLater(100, new Runnable() {
                    public void run() {
                        setResizable(true);
                        setSize(s);
                    }
                });
            }
        }
    }

    private void saveLayout() {
        Presets.putInt("x", getX());
        Presets.putInt("y", getY());
        Presets.putInt("width", getWidth());
        Presets.putInt("height", getHeight());
        Presets.sync();
    }

    private void restoreLayout() {
        int x = Presets.getInt("x", DEFAULT.x);
        int y = Presets.getInt("y", DEFAULT.y);
        int width = Presets.getInt("width", DEFAULT.width);
        int height = Presets.getInt("height", DEFAULT.height);
        setBounds(new Rectangle(x, y, width, height));
    }

    public static String getPassword() {
        JTextField passwordField = new JPasswordField(20) {
            {
                final JPasswordField that = this;
                Util.invokeLater(200, new Runnable(){
                    public void run() {
                        that.requestFocus();
                        that.requestFocusInWindow();
                    }
                });
            }

            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

        };
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel("Enter password:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        label = new JLabel("<html>&nbsp;&nbsp;</html>");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(passwordField);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object[] ob = {panel};
        int result = JOptionPane.showConfirmDialog(
                        getTopFrame(),
                        ob,
                        "Zipeg: encrypted archive",
                        JOptionPane.DEFAULT_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            return passwordField.getText();
        } else {
            return null;
        }
    }

}
