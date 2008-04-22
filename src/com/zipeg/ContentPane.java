package com.zipeg;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.io.File;
import java.io.FilenameFilter;

public final class ContentPane extends JPanel {

    private DestinationComboBox dir;
    private DirectoryTree tree;
    private ItemList items;
    JScrollPane west;
    JScrollPane center;
    JComponent splitter;
    private int drag = 0;
    private static final int SPLITTER_WIDTH = 4;
    private StatusBar sb;

    ContentPane() {
        super(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(null);
        top.add(Actions.createToolBar(), BorderLayout.NORTH);
        dir = new DestinationComboBox();
        File unzipped = new File(Util.getDocuments(), "unarchived");
        String s = Presets.get("destination", Util.getCanonicalPath(unzipped));
        dir.setText(s);
        dir.setAlignmentY(Component.CENTER_ALIGNMENT);
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(" Extract to: "), BorderLayout.WEST);

        JPanel combo = new JPanel();
        combo.setLayout(new BoxLayout(combo, BoxLayout.Y_AXIS));
        combo.add(Box.createVerticalStrut(Util.isMac() ? 4 : 2));
        combo.add(dir);
        combo.add(Box.createVerticalGlue());
        combo.setAlignmentY(Component.CENTER_ALIGNMENT);
        p.add(combo, BorderLayout.CENTER);

        JButton browse = Util.isWindows() ? new JButton(" Browse... ") : new JButton(" Choose... ");
        browse.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Actions.postEvent("chooseDestination");
            }
        });
        browse.setAlignmentY(Component.CENTER_ALIGNMENT);

        JPanel button = new JPanel();
        button.setLayout(new BoxLayout(button, BoxLayout.X_AXIS));
        button.add(Box.createHorizontalStrut(3));
        button.add(browse);
        button.add(Box.createHorizontalStrut(3));
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
        p.add(button, BorderLayout.EAST);

        JComponent spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(0, 4));
        spacer.setOpaque(false);
        p.add(spacer, BorderLayout.SOUTH);
        top.add(p, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        tree = new DirectoryTree();
        JScrollPane sp = wrap(tree);
        sp.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, new Resizer());
        add(sp, BorderLayout.WEST);
        west = sp;
        items = new ItemList();
        center = wrap(items);
        center.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        splitter = new JComponent() {
            public Dimension getPreferredSize() {
                return new Dimension(SPLITTER_WIDTH, Integer.MAX_VALUE);
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            public Cursor getCursor() {
                return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
            }
        };
        JPanel middle = new JPanel(new BorderLayout());
        middle.add(splitter, BorderLayout.WEST);
        middle.add(center, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);
        sb = new StatusBar();
        add(sb, BorderLayout.SOUTH);
        setDropTarget(new DropTarget());
        restoreLayout();
        updateDestination(null);
    }

    public void addNotify() {
        super.addNotify();
        tree.addTreeSelectionListener(items);
        Actions.addListener(this);
    }

    public void removeNotify() {
        saveDestination();
        Actions.removeListener(this);
        tree.removeTreeSelectionListener(items);
        super.removeNotify();
    }

    public Insets getInsets() {
        Insets i = super.getInsets();
        if (Util.isMac() && Util.getJavaVersion() < 1.6 && Util.getOsVersion() < 10.5) {
            i.left += 5;
            i.right += 5;
        }
        return i;
    }

    public boolean inProgress() {
        return sb.inProgress();
    }

    public void archiveOpened(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        assert param instanceof Archive;
        Archive a = (Archive)param;
        updateDestination(a);
    }

    public void addDropTargetAdapter(DropTargetAdapter dta) {
        try {
            getDropTarget().addDropTargetListener(dta);
            tree.getDropTarget().addDropTargetListener(dta);
            items.getDropTarget().addDropTargetListener(dta);
        } catch (TooManyListenersException e) {
            throw new Error(e);
        }
    }

    /** method to enable commands state
     * @param map command ids (like "commandFileOpen" to Boolean.TRUE/FALSE
     * @noinspection UnusedDeclaration
     */
    public static void updateCommandState(Map map) {
    }

    private static String getNameWithoutExtension(File a) {
        String name = a.getName();
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        // tar special treatment because of .tar.gz named archives
        if (name.length() > 4 && name.toLowerCase().endsWith(".tar")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void updateDestination(Archive a) {
        File dst = null;
        File file = a == null ? null : new File(a.isNested() ? a.getParentName() : a.getName());
        boolean append = a != null && Flags.getFlag(Flags.APPEND_ARCHIVE_NAME);
        if (Flags.getFlag(Flags.LOCATION_ARCHIVE) && a != null) {
            dst  = Flags.getFlag(Flags.APPEND_ARCHIVE_NAME) ?
                    new File(file.getParent(), getNameWithoutExtension(file)) :
                    file.getParentFile();
        } else if (Flags.getFlag(Flags.LOCATION_DOCUMENTS)) {
            if (append) {
                dst = new File(Util.getDocuments(), getNameWithoutExtension(file));
            } else {
                dst = Util.getDocuments();
            }
        } else if (Flags.getFlag(Flags.LOCATION_DESKTOP)) {
            if (append) {
                dst = new File(Util.getDesktop(), getNameWithoutExtension(file));
            } else {
                dst = Util.getDesktop();
            }
        } else if (Flags.getFlag(Flags.LOCATION_LAST)) {
            return; // nothing
        }
        if (dst == null) {
            return;
        }
        if (append) {
            File d = dst; // check if file with same name exists
            for (int i = 1; i < 9; i++) {
                if (!dst.exists()) {
                    break;
                }
                dst = new File(Util.getCanonicalPath(d) + "." + i);
            }
        }
        dir.setText(Util.getCanonicalPath(dst));
    }

    public void settingsChanged(Object params) {
        Object[] p = (Object[])params;
        long was = ((Long)p[0]).longValue();
        long now = ((Long)p[1]).longValue();
        long mask = Flags.LOCATION_LAST | Flags.LOCATION_ARCHIVE | Flags.LOCATION_DOCUMENTS | Flags.LOCATION_DESKTOP;
        Archive a = Zipeg.getArchive();
        if (((was ^ now) & mask) != 0) {
            updateDestination(a);
        }
    }

    public List getSelected() {
        if (tree.isLastFocused()) {
            return tree.getSelected();
        } else if (items.isLastFocused()) {
            return items.getSelected();
        } else {
            List list = tree.getSelected();
            return list == null ? items.getSelected() : list;
        }
    }

    public String getDestination() {
        return dir.getText();
    }

    public void saveDestination() {
        Presets.put("destination", dir.getText());
        Presets.sync();
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;
        if (drag != 0) {
            g2d.setPaint(Color.BLACK);
            JScrollPane sp = (JScrollPane)tree.getParent().getParent();
            int x = sp.getWidth() + drag;
            int y = SwingUtilities.convertPoint(sp, new Point(0,0), this).y;
            int h = sp.getHeight();
            GradientPaint gradient = new GradientPaint(x, 0, Color.GRAY, x + 16, 0, Color.WHITE, true);
            g2d.setPaint(gradient);
            Composite cs = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
            g2d.fill(new Rectangle(x - 16, y, 16, h));
            g2d.setComposite(cs);
        }
    }

    public void chooseDestination() {
        String dst = getDestination();
        File target = new File(dst);
        if (Util.isMac()) {
            System.setProperty("apple.awt.use-file-dialog-package", "false");
            System.setProperty("JFileChooser.packageIsTraversable", "true");
            System.setProperty("JFileChooser.appBundleIsTraversable", "true");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            FileDialog open = new FileDialog(MainFrame.getInstance(), "Zipeg: Extract to Folder", FileDialog.LOAD);
            if (target.isDirectory()) {
                open.setDirectory(target.getAbsolutePath());
            }
            open.setFilenameFilter(new FilenameFilter(){
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });
            open.setVisible(true);
            if (open.getFile() != null) {
                File file = new File(open.getDirectory(), open.getFile());
                dst = file.getAbsolutePath();
            } else {
                dst = null;
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        } else {
            FileChooser fc = FileChooser.getInstance();
            fc.setCurrentDirectory(target.isDirectory() ? target : Util.getDesktop());
            fc.setFileHidingEnabled(true);
            fc.setDialogTitle("Zipeg: Destination Folder");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int r = fc.showOpenDialog(ContentPane.this);
            if (r == JFileChooser.APPROVE_OPTION) {
                dir.setText(Util.getCanonicalPath(fc.getSelectedFile()));
                if (fc.getSelectedFile().isDirectory()) {
                    dst = fc.getSelectedFile().toString();
                } else {
                    dst = Util.getCanonicalPath(fc.getSelectedFile());
                }
            } else {
                dst = null;
            }
        }
        if (dst != null) {
            dir.setText(dst);
            saveDestination();
        }
    }

    private class Resizer extends JComponent {

        private Point down = null;

        private final MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (down != null) {
                    drag = e.getX() - down.x;
                    int ww = west.getWidth() + drag;
                    int mw = tree.getMinimumSize().width;
                    if (ww < mw) {
                        drag += mw - ww;
                    }
                    int wc = center.getWidth() - drag;
                    int mc = items.getMinimumSize().width;
                    if (wc < mc) {
                        drag -= mc - wc;
                    }
                    resize();
                }
            }
        };

        private final MouseListener mouseListener = new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    // TODO: simplify me
                    int wp = tree.getPreferredSize().width;
                    int ww = west.getViewport().getWidth();
                    int cp = items.getPreferredSize().width;
                    int cw = center.getViewport().getWidth();
                    if (wp > ww) {
                        int delta = wp - ww;
                        int wc = center.getWidth() - delta;
                        int mc = items.getMinimumSize().width;
                        if (wc < mc) {
                            delta -= mc - wc;
                            wc = mc;
                        }
                        ww = west.getWidth() + delta;
                        west.setPreferredSize(new Dimension(ww, west.getPreferredSize().height));
                        center.setPreferredSize(new Dimension(wc, center.getPreferredSize().height));
                        ContentPane.this.revalidate();
                        ContentPane.this.repaint();
                        saveLayout();
                    } else if (cp > cw) {
                        int delta = cp - cw;
                        ww = west.getWidth() - delta;
                        int mw = tree.getMinimumSize().width;
                        if (ww < mw) {
                            delta -= mw - ww;
                            ww = mw;
                        }
                        int wc = center.getWidth() + delta;
                        west.setPreferredSize(new Dimension(ww, west.getPreferredSize().height));
                        center.setPreferredSize(new Dimension(wc, center.getPreferredSize().height));
                        ContentPane.this.revalidate();
                        ContentPane.this.repaint();
                        saveLayout();
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                down = e.getPoint();
                repaint();
            }

            public void mouseReleased(MouseEvent e) {
                resize();
                down = null;
                repaint();
            }

        };

        private void resize() {
            if (drag != 0) {
                // TODO: simplify me
                int ww = west.getWidth() + drag;
                int mw = tree.getMinimumSize().width;
                if (ww < mw) {
                    drag += mw - ww;
                    ww = mw;
                }
                int wc = center.getWidth() - drag;
                int mc = items.getMinimumSize().width;
                if (wc < mc) {
                    drag -= mc - wc;
                    wc = mc;
                }
                west.setPreferredSize(new Dimension(ww, west.getPreferredSize().height));
                center.setPreferredSize(new Dimension(wc, center.getPreferredSize().height));
                ContentPane.this.revalidate();
                ContentPane.this.repaint();
                saveLayout();
                drag = 0;
                ContentPane.this.repaint();
            }
        }

        public void addNotify() {
            super.addNotify();
            addMouseListener(mouseListener);
            addMouseMotionListener(mouseMotionListener);
            splitter.addMouseMotionListener(mouseMotionListener);
            splitter.addMouseListener(mouseListener);
        }

        public void removeNotify() {
            splitter.removeMouseListener(mouseListener);
            splitter.removeMouseMotionListener(mouseMotionListener);
            removeMouseMotionListener(mouseMotionListener);
            removeMouseListener(mouseListener);
            super.removeNotify();
        }

        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            int w = getWidth();
            int h = getHeight();
            Paint p =g2d.getPaint();
            GradientPaint gradient = down != null ?
                    new GradientPaint(0, 0, Color.GRAY, w, 0, Color.WHITE, true) :
                    new GradientPaint(0, 0, Color.WHITE, w, 0, Color.GRAY, true);
            g2d.setPaint(gradient);
            Composite cs = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2d.fill(new Rectangle(getSize()));
            g2d.setPaint(Color.BLACK);
            g2d.drawLine(w / 2 - 2, 4, w / 2 - 2, h - 5);
            g2d.drawLine(w / 2 + 1, 4, w / 2 + 1, h - 5);
            g2d.setComposite(cs);
            g2d.setPaint(p);
        }

        public Cursor getCursor() {
            return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        }

    }

    private void saveLayout() {
        Presets.putInt("west.width", west.getPreferredSize().width);
        Presets.putInt("west.height", west.getPreferredSize().height);
        Presets.putInt("center.width", center.getPreferredSize().width);
        Presets.putInt("center.height", center.getPreferredSize().height);
        Presets.sync();
    }

    private void restoreLayout() {
        int width = Presets.getInt("west.width", tree.getMinimumSize().width);
        int height = Presets.getInt("west.height", tree.getMinimumSize().height);
        west.setPreferredSize(new Dimension(width, height));
        width = Presets.getInt("center.width", items.getMinimumSize().width);
        height = Presets.getInt("center.height", items.getMinimumSize().height);
        center.setPreferredSize(new Dimension(width, height));
    }

    private JScrollPane wrap(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getViewport().setBackground(c.getBackground());
        sp.setBorder(null);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        // TODO: BLT scroll mode does not work correctly. Why?
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        return sp;
    }

}
