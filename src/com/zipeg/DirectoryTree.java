package com.zipeg;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;

public final class DirectoryTree extends JTree {

    private final DefaultTreeCellRenderer uicr;
    private boolean lastFocused;
    private Color selection;
    private Color dimmed;

    private final TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) { updateStatusBar(); }
    };

    private final FocusListener focusListener = new FocusAdapter() {
        public void focusGained(FocusEvent e) {
            updateStatusBar();
            repaint();
        }
        public void focusLost(FocusEvent e) {
            lastFocused = e.isTemporary();
            repaint();
        }
    };

    DirectoryTree() {
        super(new DefaultTreeModel(null)); // new DefaultMutableTreeNode()?
        uicr = (DefaultTreeCellRenderer)getCellRenderer();
        uicr.setLeafIcon(uicr.getDefaultClosedIcon());
        setCellRenderer(new CellRenderer());
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        setRootVisible(false);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        if (Util.isMac()) {
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.META_MASK), "up");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Event.META_MASK), "up");
        }
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK), "up");
        getActionMap().put("enter", new AbstractAction(){
            public void actionPerformed(ActionEvent e) { enter(); }
        });
        getActionMap().put("up", new AbstractAction(){
            public void actionPerformed(ActionEvent e) { up(); }
        });
        setDropTarget(new DropTarget());
    }

    private void enter() {
        TreeNode node = (TreeNode)getLastSelectedPathComponent();
        if (node != null) {
            expandPath(getSelectionModel().getSelectionPath());
            Actions.postEvent("enterDirectory", node);
        }
    }

    private void up() {
        TreePath path = getSelectionModel().getSelectionPath();
        TreePath parent = path == null ? null : path.getParentPath();
        if (parent != null) {
            getSelectionModel().setSelectionPath(parent);
        }
    }

    public void addNotify() {
        super.addNotify();
        Actions.addListener(this);
        addFocusListener(focusListener);
        addTreeSelectionListener(treeSelectionListener);
    }

    public void removeNotify() {
        removeTreeSelectionListener(treeSelectionListener);
        removeFocusListener(focusListener);
        Actions.removeListener(this);
        super.removeNotify();
    }

    public Insets getInsets() {
        return new Insets(4, 4, 4, 4);
    }

    public Dimension getMinimumSize() {
        return new Dimension(140, 100);
    }

    public boolean isLastFocused() {
        return lastFocused || hasFocus();
    }

    public List getSelected() {
        DefaultTreeModel model = (DefaultTreeModel)getModel();
        TreePath path = getSelectionPath();
        if (path != null) {
            TreeNode selected = (TreeNode)path.getLastPathComponent();
            if (selected != model.getRoot()) {
                List list = new LinkedList();
                ((TreeElement)selected).collectDescendants(list);
                return list;
            }
        }
        return null;
    }

    public void extractionCompleted(Object params) { // {error, quit} or {null, quit}
        if (isLastFocused()) {
            updateStatusBar();
        }
    }

    public void updateCommandState(Map m) {
        if (isRootVisible()) {
            m.put("commandActionsExtract", Boolean.TRUE);
            m.put("commandActionsAdd", Boolean.TRUE);
            m.put("commandFileClose", Boolean.TRUE);
            m.put("commandFilePrint", Boolean.TRUE);
        }
        TreeNode node = (TreeNode)getLastSelectedPathComponent();
        if (node != null && node.getParent() != null) {
            m.put("commandGoEnclosingFolder", Boolean.TRUE);
        }
    }

    private void updateStatusBar() {
        if (MainFrame.getInstance().inProgress() ||
           !MainFrame.getInstance().isVisible() ||
            Zipeg.getArchive() == null ||
           !Zipeg.getArchive().isOpen()) {
            return;
        }
        // TODO: (Leo) and even after that the archive async closing
        // can still be racing the status bar. Think of it and make sure
        // archive is never closed from the background thread.
        TreeNode node = (TreeNode)getLastSelectedPathComponent();
        TreeElement element = node == null ? null : (TreeElement)node;
        File file = element == null ? null : new File(element.getFile());
        if (file != null) {
            Actions.postEvent("setStatus", file.toString());
            long bytes = element.getDescendantSize();
            long files = element.getDescendantFileCount();
            long dirs  = element.getDescendantDirectoryCount();
            String v = Util.formatMB(bytes);
            String s = (dirs <= 1 ? "" : Util.formatNumber(dirs) + " folders  ") +
                       (files <= 1 ? "" : Util.formatNumber(files) + " items  ") + v;
            Actions.postEvent("setInfo", s);
        }
    }

    public void selectFolder(Object treeElement) {
        TreePath path = getSelectionPath();
        if (path != null) {
            // do not request focus if child is selected
            TreeNode selected = (TreeNode)path.getLastPathComponent();
            for (int i = 0; i < selected.getChildCount(); i++) {
                TreeNode child = selected.getChildAt(i);
                if (child == treeElement) {
                    path = path.pathByAddingChild(child);
                    setSelectionPath(path);
                    scrollPathToVisible(path);
                    return;
                }
            }
            requestFocus();
            path = path.getParentPath();
            while (path != null) {
                TreeNode parent = (TreeNode)path.getLastPathComponent();
                if (parent == treeElement) {
                    setSelectionPath(path);
                    scrollPathToVisible(path);
                    return;
                }
                path = path.getParentPath();
            }
        }
    }

    public void archiveOpened(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        assert param != null;
        Archive a = (Archive)param;
        setModel(new DefaultTreeModel(a.getRoot()));
        setRootVisible(true);
        if (a.isNested()) {
            MainFrame.getInstance().setTitle(a.getParentName() + " [" + new File(a.getName()).getName() + "]");
        } else {
            MainFrame.getInstance().setTitle(a.getName());
        }
        setSelectionInterval(0, 0);
        Util.invokeLater(200, new Runnable(){
            public void run() {
                requestFocus();
            }
        });
    }

    public void commandFileClose() {
        getSelectionModel().clearSelection();
        setModel(new DefaultTreeModel(null));
        MainFrame.getInstance().setTitle(null);
        setRootVisible(false);
    }

    private class  CellRenderer implements TreeCellRenderer {

        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean focused) {
            JLabel cr = (JLabel)uicr.getTreeCellRendererComponent(tree, value, selected,
                                                                  expanded, leaf, row, focused);
            if (leaf && selected) {
                cr.setIcon(uicr.getOpenIcon());
            }
            if (selected) {
                if (dimmed == null) {
                    selection = uicr.getBackgroundSelectionColor();
                    dimmed = new Color(selection.getRed(),
                                       selection.getGreen(),
                                       selection.getBlue(),
                                       40);
                }
                uicr.setBackgroundSelectionColor(isLastFocused() ? selection : dimmed);
                if (!isLastFocused()) {
                    uicr.setForeground(uicr.getTextNonSelectionColor());
                }
            }
            return cr;
        }

    }

}
