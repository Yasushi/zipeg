package com.zipeg;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileView;
import javax.swing.tree.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.text.*;
import java.io.*;
import java.lang.reflect.Method;

public final class ItemList extends JTable implements TreeSelectionListener {

    private static final String[] TITLES = new String[] {
        "Name",
        "Size",
        "Compressed",
        "Time",
        "Comment"
    };

    private static final int HORIZONTAL_SPACING = 8;
    private static final String PASSWD = "\u2021"; // double dagger
    private static final TreeElement[] EMPTY = new TreeElement[]{};
    private static final Map iconTypeCache = new HashMap();
    private static final Map iconFileCache = new HashMap();
    private static final int W;
    private static final int H;
    private static FileView fv;
    private static final Icon fileIcon;
    private static final Icon folderIcon;
    private TreeElement parent;
    private TreeElement[] entries = EMPTY;
    private DateFormat shortdate = new SimpleDateFormat(); // uses Locale.getDefault()
    private DateFormat longdate = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
    private Color even;
    private Color odd;
    private Color selection;
    private Color selection_dimmed;
    private boolean lastFocused;
    private DefaultTableCellRenderer renderer = new CellRenderer();
    private AbstractTableModel model = new ItemListModel();
    private FileDragGestureRecognizer recognizer;
    private LinkedList prefetch;
    private int sortDirection = 0;
    private int sortColumn = 0;
    private TransparentWindow dragWindow;
    // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4126630
    //     http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4833524
    private boolean popupTrigger;

    private final ListSelectionListener listSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) { selectionChanged(e); }
    };

    private FocusListener focusListener = new FocusAdapter() {
        public void focusGained(FocusEvent e) {
            updateStatusBar();
            repaint();
        }
        public void focusLost(FocusEvent e) {
            lastFocused = e.isTemporary();
            repaint();
        }
    };

    private MouseListener mouseListener = new MouseAdapter() {

        public void mouseClicked(MouseEvent e) {
            if (popupTrigger && e.getClickCount() == 1) {
                showContextMenu(e.getPoint());
            } else {
                click(e);
            }
            popupTrigger = false;
        }

        public void mousePressed(MouseEvent e) {
            popupTrigger = e.isPopupTrigger();
        }

        public void mouseReleased(MouseEvent e) {
            if (!popupTrigger) {
                popupTrigger = e.isPopupTrigger();
            }
        }

    };

    protected void processMouseEvent(MouseEvent evt) {
        if (Util.isMac() && (evt.isPopupTrigger() || popupTrigger)) {
            // workaround for Mac Lost Selection
            int[] selectedRows = getSelectedRows();
            ListSelectionModel model = getSelectionModel();
            int[] rows = getSelectedRows();
            super.processMouseEvent(evt);
            for (int i = 0; i < selectedRows.length; i++) {
                model.addSelectionInterval(rows[i], rows[i]);
            }
        } else {
            super.processMouseEvent(evt);
        }
    }

    private ChangeListener changeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
            sizeColumns();
        }
    };

    static {
        fileIcon = UIManager.getIcon("FileView.fileIcon");
        Icon folder = UIManager.getIcon("Tree.closedIcon"); // looks better than "FileView.directoryIcon"
        if (folder == null) {
            folder = UIManager.getIcon("FileView.directoryIcon"); // gtk does not have Tree.closedIcon
        }
        folderIcon = folder;
        assert fileIcon != null;
        assert folderIcon != null;
        W = Math.max(folderIcon.getIconWidth(), fileIcon.getIconWidth());
        H = Math.max(folderIcon.getIconHeight(), fileIcon.getIconHeight());
    }

    ItemList() {
        setModel(model);
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        setRequestFocusEnabled(true);
        getTableHeader().setReorderingAllowed(false); // for now
        setIntercellSpacing(new Dimension(0, 1)); // important: keep w == 0.
        even = getBackground();
        float[] hsb = new float[3];
        Color.RGBtoHSB(even.getRed(), even.getGreen(), even.getBlue(), hsb);
        if (hsb[2] < 0.5f) hsb[2] += 0.05f; else hsb[2] -= 0.05f;
        odd = new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
        for (int i = 0; i < getColumnCount(); i++) {
            getColumn(TITLES[i]).setCellRenderer(renderer);
        }
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        if (Util.isMac()) {
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.META_MASK), "up");
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Event.META_MASK), "back");
        } else {
            getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "up");
        }
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Event.ALT_MASK), "back");
        getActionMap().put("enter", new AbstractAction(){
            public void actionPerformed(ActionEvent e) { enter(); }
        });
        getActionMap().put("up", new AbstractAction(){
            public void actionPerformed(ActionEvent e) { back(true); }
        });
        getActionMap().put("back", new AbstractAction(){
            public void actionPerformed(ActionEvent e) { back(false); }
        });
        setDragEnabled(true);
        setTransferHandler(new FileTransferHandler());
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6579129
        // setAutoCreateRowSorter since 1.5
        Method setAutoCreateRowSorter;
        try {
            Class[] signature = new Class[]{boolean.class};
            setAutoCreateRowSorter = JTable.class.getMethod("setAutoCreateRowSorter", signature);
        } catch (NoSuchMethodException e) {
            setAutoCreateRowSorter = null;
        }
        if (setAutoCreateRowSorter != null) {
            Util.call(setAutoCreateRowSorter, this, new Object[]{Boolean.FALSE});
        }
        getTableHeader().addMouseListener(new HeaderMouseHandler());
        getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(this.tableHeader.getDefaultRenderer()));
    }

    public void addNotify() {
        super.addNotify();
        Actions.addListener(this);
        JViewport vp = (JViewport)getParent();
        vp.addChangeListener(changeListener);
        addMouseListener(mouseListener);
        addFocusListener(focusListener);
        getSelectionModel().addListSelectionListener(listSelectionListener);
    }

    public void removeNotify() {
        getSelectionModel().removeListSelectionListener(listSelectionListener);
        removeFocusListener(focusListener);
        removeMouseListener(mouseListener);
        JViewport vp = (JViewport)getParent();
        vp.removeChangeListener(changeListener);
        Actions.removeListener(this);
        super.removeNotify();
    }

    public boolean isLastFocused() {
        return lastFocused || hasFocus();
    }

    public List getSelected() {
        int[] rows = getSelectedRows();
        List list = new LinkedList();
        for (int i = 0; i < rows.length; i++) {
            entries[rows[i]].collectDescendants(list);
        }
        return list.size() == 0 ? null : list;
    }

    public void extractionCompleted(Object params) { // {error, quit} or {null, quit}
        if (isLastFocused()) {
            updateStatusBar();
        }
    }

    public Dimension getMinimumSize() {
        return new Dimension(200, 100);
    }

    public void enterDirectory(Object param) {
        assert param instanceof TreeElement;
        assert param == parent; // I have one crash report from 2.0.0.777 with this failing....
        requestFocus();
    }

    public void valueChanged(TreeSelectionEvent e) {
        parent = null;
        entries = EMPTY;
        JTree tree = (JTree)e.getSource();
        TreeNode node = (TreeNode)tree.getLastSelectedPathComponent();
        repopulate((TreeElement)node);
    }

    public void settingsChanged(Object p) {
        repopulate(parent);
    }

    public void archiveOpened(Object param) {
        iconFileCache.clear();
    }

    private void repopulate(TreeElement p) {
        if (p != null) {
            parent = p;
            int n = parent.getChildrenCount();
            entries = new TreeElement[n];
            Object[] values = new Object[n];
            prefetch = new LinkedList();
            int k = 0;
            if (Flags.getFlag(Flags.DIRECTORIES_FIRST)) {
                for (Iterator i = parent.getChildren(); i.hasNext(); ) {
                    TreeElement c = (TreeElement)i.next();
                    if (c.isDirectory()) {
                        entries[k] = c;
                        values[k] = getColumnValue(c, sortColumn);
                        k++;
                    }
                }
                sort(entries, values, 0, k);
                int m = k;
                for (Iterator i = parent.getChildren(); i.hasNext(); ) {
                    TreeElement c = (TreeElement)i.next();
                    if (!c.isDirectory()) {
                        entries[k] = c;
                        values[k] = getColumnValue(c, sortColumn);
                        k++;
                    }
                }
                sort(entries, values, m, k);
            } else {
                for (Iterator i = parent.getChildren(); i.hasNext(); ) {
                    entries[k] = (TreeElement)i.next();
                    values[k] = getColumnValue(entries[k], sortColumn);
                    k++;
                }
                sort(entries, values, 0, k);
            }
            if (entries.length > 0) {
                for (int i = 0; i < entries.length; i++) {
                    prefetch.add(new Integer(i));
                }
                prefetch(parent);
            }
        }
        for (int i = 0; i < getColumnCount(); i++) {
            TableColumn tc = getColumn(TITLES[i]);
            tc.setMinWidth(20);
        }
        model.fireTableDataChanged();
        clearSelection();
        if (entries.length > 0) {
            getSelectionModel().setAnchorSelectionIndex(0);
            getSelectionModel().addSelectionInterval(0, 0);
            scrollToVisible(0);
        }
    }

    private void sort(TreeElement[] e, final Object[] v, int from, int to) {
        assert e.length == v.length : "entires " + e.length + " values " + v.length;
        if (sortDirection == 0 || to <= from + 1) {
            return;
        }
        Integer[] order = new Integer[e.length];
        for (int i = 0; i < e.length; i++) {
            order[i] = new Integer(i);
        }
        final boolean ignoreCase = !Flags.getFlag(Flags.CASE_SENSITIVE);
        Arrays.sort(order, from, to, new Comparator() {
            public int compare(Object ix1, Object ix2) {
                Object o1 = v[((Integer)ix1).intValue()];
                Object o2 = v[((Integer)ix2).intValue()];
                int r;
                if (o1 == null && o2 == null) {
                    r = 0;
                } else if (o1 == null) {
                    r = -1;
                } else if (o2 == null) {
                    r = 1;
                } else if (o1 instanceof String) {
                    r = ignoreCase ? ((String)o1).compareToIgnoreCase((String)o2) :
                            ((String)o1).compareTo((String)o2);
                } else if (o1 instanceof Long) {
                    r = ((Long)o1).compareTo((Long)o2);
                } else if (o1 instanceof Date) {
                    r = ((Date)o1).compareTo((Date)o2);
                } else {
                    assert false : "type " + o1.getClass().getName();
                    r = 0;
                }
//              Debug.trace("[" + ix1 + "]=" + o1 + " [" + ix2 + "]=" + o2 + " " + (r * sortDirection));
                return r * sortDirection;
            }
        });
        TreeElement[] s = new TreeElement[e.length];
        for (int i = from; i < to; i++) {
            s[i] = e[order[i].intValue()];
        }
        System.arraycopy(s, from, e, from, to - from);
    }

    private Object getColumnValue(TreeElement e, int c) {
        switch (c) {
            case 0: return e.getName();
            case 1: return new Long(getElementSize(e));
            case 2: return new Long(getCompressedSize(e));
            case 3: if (e.getTime() > 0) {
                        return new Date(e.getTime());
                    } else {
                        return new Date(0);
                    }
            case 4: return e.getComment();
            default: assert false;
                    return null;
        }
    }

    private void scrollToVisible(int rowIndex) {
        JViewport viewport = (JViewport)getParent();
        Rectangle r0 = getCellRect(rowIndex, 0, true);
        Rectangle r1 = getCellRect(rowIndex, getColumnCount() - 1, true);
        Rectangle rect = r0.union(r1);
        Point pt = viewport.getViewPosition();
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);
        viewport.scrollRectToVisible(rect);
    }

    public void updateCommandState(Map m) {
        int min = selectionModel.getMinSelectionIndex();
        int max = selectionModel.getMaxSelectionIndex();
        if (entries.length > 0 && 0 <= min && min <= max) {
            m.put("commandEditCopy", Boolean.TRUE);
            m.put("commandEditCut", Boolean.TRUE);
            m.put("commandEditDelete", Boolean.TRUE);
        }
        Archive a = Zipeg.getArchive();
        if (a != null && isLastFocused() && getSelectedRowCount() > 0) {
            m.put("commandActionsPreview", getSelectedCached().size() > 0 ? Boolean.TRUE : Boolean.FALSE);
        }
        if (parent != null && isLastFocused()) {
            if (((TreeNode)parent).getParent() != null) {
                m.put("commandGoEnclosingFolder", Boolean.TRUE);
            }
            m.put("commandGoBack", Boolean.TRUE);
        }
    }

    public void commandGoEnclosingFolder() {
        back(true);
    }

    public void commandGoBack() {
        back(false);
    }

    public void commandActionsPreview() {
        openOrPreview(false);
    }


    private void openWith() {
        ArrayList selected = getSelectedCached();
        if (selected.size() == 1) {
            File file = (File)selected.get(0);
            openDocWith(file.getAbsolutePath());
        }
    }

    private void openOrPreview(boolean open) {
        ArrayList selected = getSelectedCached();
        ArrayList preview = new ArrayList();
        if (!open) {
            separate(selected, preview);
        }
        if (selected.isEmpty() && preview.isEmpty()) {
            return;
        }
        if (Util.isMac()) {
            try {
                openListOnMac(selected,  true);
                openListOnMac(preview, false);
            } catch (IOException e) {
                throw new Error(e);
            }
        } else {
            for (Iterator i = preview.iterator(); i.hasNext(); ) {
                File temp = (File)i.next();
                assert temp.exists() && !temp.isDirectory();
                previewImage(Util.getCanonicalPath(temp));
            }
            for (Iterator i = selected.iterator(); i.hasNext(); ) {
                File temp = (File)i.next();
                assert temp.exists() && !temp.isDirectory();
                openDoc(temp.getAbsolutePath());
            }
        }
    }

    private void openDoc(String file) {
        final long mask = Registry.SEE_MASK_UNICODE|Registry.SEE_MASK_NOCLOSEPROCESS|
                Registry.SEE_MASK_FLAG_DDEWAIT|Registry.SEE_MASK_ASYNCOK;
        long r = Registry.getInstance().shellExecute(mask, "open", file, null, null);
        if (1 < r && r <= 32) {
            Debug.traceln("shellExecute " + r);
            Util.openDoc(file);
        }
    }


    private void openDocWith(String file) {
        final long mask = Registry.SEE_MASK_UNICODE|Registry.SEE_MASK_NOCLOSEPROCESS|
                Registry.SEE_MASK_FLAG_DDEWAIT|Registry.SEE_MASK_ASYNCOK;
        long r = Registry.getInstance().shellExecute(mask, "open", "rundll32.exe",
                "shell32.dll,OpenAs_RunDLL " + file, null);
        if (1 < r && r <= 32) {
            Debug.traceln("shellExecute " + r);
            Util.openDocWith(file);
        }
    }

    private static void previewImage(String file) {
        if (Util.isWindows()) {
            final long mask = Registry.SEE_MASK_UNICODE|Registry.SEE_MASK_NOCLOSEPROCESS|
                    Registry.SEE_MASK_FLAG_DDEWAIT|Registry.SEE_MASK_ASYNCOK|Registry.SEE_MASK_WAITFORINPUTIDLE;
            long r = Registry.getInstance().shellExecute(mask, "open", "rundll32.exe",
                    "shimgvw.dll,ImageView_Fullscreen " + file, null);
            if (1 < r && r <= 32) {
                Debug.traceln("shellExecute " + r);
                Util.previewImage(file);
            }
        } else {
            Util.previewImage(file);
        }
    }

    private static void addFile(Map m, String k, File f) {
        ArrayList a = (ArrayList)m.get(k);
        if (a == null) {
            a = new ArrayList(10);
            m.put(k, a);
        }
        a.add(f);
    }

    private void openListOnMac(ArrayList files, boolean open) throws IOException {
        if (files.isEmpty()) {
            return;
        }
        Map v2f = new HashMap();
        if (!open) {
            // group files by viewer application
            for (Iterator j = files.iterator(); j.hasNext(); ) {
                File file = (File)j.next();
                assert file.exists() && !file.isDirectory();
                Object[] p = new Object[]{Util.getCanonicalPath(file)};
                String viewer = (String)Util.callStatic("com.zipeg.mac.MacSpecific.getCocoaApplicationForFile", p);
                addFile(v2f, viewer, file);
            }
        } else {
            // put all files into "null" key
            v2f.put(null, files);
        }
        for (Iterator j = v2f.keySet().iterator(); j.hasNext(); ) {
            String viewer = (String)j.next();
            ArrayList fs = (ArrayList)v2f.get(viewer);
            assert fs.size() > 0;
            String[] cmd = new String[fs.size() + 1 + (open ? 0 : 2)];
            int k = 0;
            cmd[k++] = "/usr/bin/open";
            if (!open) {
                cmd[k++] = viewer == null ? "-b" : "-a";
                cmd[k++] = viewer == null ? "com.apple.Preview" : viewer;
            }
            for (Iterator i = fs.iterator(); i.hasNext(); ) {
                File temp = (File)i.next();
                assert temp.exists() && !temp.isDirectory();
                cmd[k++] = Util.getCanonicalPath(temp);
            }
            assert Util.isMac();
            Process p = Runtime.getRuntime().exec(cmd,
                    Util.getEnvFilterOutMacCocoaCFProcessPath());
            try {
                int exit = p.waitFor();
                if (exit != 0) {
                    Debug.traceln("exit code=" + exit);
                }
            } catch (InterruptedException e) {
                throw new Error(e);
            }
        }
    }

    private void separate(ArrayList open, ArrayList preview) {
        for (Iterator i = open.iterator(); i.hasNext(); ) {
            File f = (File)i.next();
            if (Util.isPreviewable(f.getName())) {
                i.remove();
                preview.add(f);
            }
        }
    }

    private ArrayList getSelectedCached() {
        ArrayList selected = new ArrayList();
        Archive a = Zipeg.getArchive();
        int[] rows = getSelectedRows();
        if (a != null && rows != null) {
            for (int i = 0; i < rows.length; i++) {
                if (!entries[rows[i]].isDirectory()) {
                    File file = new File(entries[rows[i]].getFile());
                    CacheEntry ce = (CacheEntry)Cache.getInstance().get(file);
                    if (ce != null && ce.temp != null && ce.temp.canRead()) {
                        selected.add(ce.temp);
                    }
                    else {
                        selected.clear(); // all files must be cached
                        break;
                    }
                }
            }
        }
        return selected;
    }

    private void selectionChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        updateStatusBar();
        int anchor = selectionModel.getAnchorSelectionIndex();
//      Debug.traceln(anchor + " " + e.getFirstIndex() + ".." + e.getLastIndex());
        if (selectionModel.isSelectedIndex(anchor)) {
            Integer ix = new Integer(anchor);
            if (prefetch.remove(ix)) {
                prefetch.addFirst(ix);
            }
        }
    }

    private void prefetch(final TreeElement p) {
        assert IdlingEventQueue.isDispatchThread();
        Archive a = Zipeg.getArchive();
        if (p != parent || prefetch.isEmpty() || a == null) {
            repaint();
            return;
        }
        int ix = ((Integer)prefetch.removeFirst()).intValue();
        final TreeElement element = entries[ix];
//      Debug.traceln("prefetch " + ix);
        TreeElement r = (TreeElement)a.getRoot();
        boolean nested = !Flags.getFlag(Flags.DONT_OPEN_NESTED);
        if (nested && r != null && r.getDescendantFileCount() == 1 &&
            Util.isArchiveFileType(element.getFile())) {
            Debug.traceln("skip prefetching for nested archive: " + element.getFile());
            return;
        }
        CacheController.getInstance().enqueue(a, element, new Runnable(){
            public void run() {
                assert IdlingEventQueue.isDispatchThread();
                File file = new File(element.getFile());
                CacheEntry ce = (CacheEntry)Cache.getInstance().get(file);
                if (ce != null && ce.thumb != null) {
                    createThumbIcon(file, ce.thumb);
                }
                IdlingEventQueue.invokeOnIdle(new Runnable(){
                    public void run() {
                        prefetch(p);
                    }
                });
            }
        });
    }

    private void createThumbIcon(File file, File thumb) {
        if (iconFileCache.containsKey(file)) {
            return;
        }
        BufferedImage th = ScaledImage.createThumbnail(thumb);
        if (th != null && th.getWidth() > 0 && th.getHeight() > 0) {
            int dw = W;
            int dh = H;
            BufferedImage bi = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            int sw = th.getWidth();
            int sh = th.getHeight();
            if (sw < sh) {
                dh = dw * sh / sw;
            } else {
                dw = dh * sw / sh;
            }
            Graphics g = bi.getGraphics();
            int x = (W - dw) / 2;
            int y = (H - dh) / 2;
//          Debug.traceln(x + "," + y + " " + dw + " x " + dh);
            g.drawImage(th, x, y, dw, dh, 0, 0, sw, sh, null);
            g.dispose();
            iconFileCache.put(file, new ImageIcon(bi));
        }
    }

    private void updateStatusBar() {
        if (MainFrame.getInstance().inProgress()) {
            return;
        }
        int anchor = selectionModel.getAnchorSelectionIndex();
        if (selectionModel.isSelectedIndex(anchor)) {
            File last = new File(entries[anchor].getFile());
            if (getSelectedRowCount() == 1) {
                String status = last.toString();
                if (entries[anchor].isEncrypted()) {
                    if (entries[anchor].getError() != null) {
                        status += " " + PASSWD + " invalid password? (" + entries[anchor].getError() + ")";
                    } else {
                        status += " " + PASSWD + " password protected";
                    }
                }
                Actions.postEvent("setStatus", status);
                if (entries[anchor].isDirectory()) {
                    int files = entries[anchor].getDescendantFileCount();
                    int dirs = entries[anchor].getDescendantDirectoryCount();
                    long bytes = entries[anchor].getDescendantSize();
                    String v = Util.formatMB(bytes);
                    String s = (dirs <= 1 ? "" : Util.formatNumber(dirs) + " folders  ") +
                               (files <= 1 ? "" : Util.formatNumber(files) + " items  ") + v;
                    Actions.postEvent("setInfo", s);
                } else {
                    long bytes = getElementSize(entries[anchor]);
                    String v = Util.formatMB(bytes);
                    Actions.postEvent("setInfo", v);
                }
            }
            else {
                Actions.postEvent("setStatus", "Multiple files selected");
                Actions.postEvent("setInfo", "");
            }
        } else {
            Actions.postEvent("setStatus", "");
            Actions.postEvent("setInfo", "");
        }
    }

    private void click(MouseEvent e) {
        if (e.getClickCount() >= 2) {
            enter();
        }
    }

    private void showContextMenu(Point p) {
        int n = getSelectedRowCount();
        int r = getSelectedRow();
        final TreeElement e = n == 1 && r >= 0 ? entries[r] : null;
        if (n == 0) {
            return;
        }
        ArrayList actions = new ArrayList();
        actions.add("&Extract");
        if (e != null && Util.isArchiveFileType(e.getName())) {
            actions.add("Open_inside_&Zipeg");
        }
        ArrayList cached = getSelectedCached();
        if (cached.size() > 0) {
            actions.add("&Open");
        }
        if (n == 1 && e != null && !e.isDirectory() && cached.size() == 1 && Util.isWindows()) {
            actions.add("Open_wit&h...");
        }
        ArrayList preview = new ArrayList();
        separate(cached, preview);
        if (preview.size() > 0) {
            actions.add("Pre&view");
        }
        Actions.showContextMenu(this, p,
            (String[])actions.toArray(new String[actions.size()]),
            new Actions.MenuItemListener() {
                public void run(String cmd) {
                    Debug.traceln(cmd);
                    if ("commandOpeninsideZipeg".equalsIgnoreCase(cmd)) {
                        Zipeg.getArchive().extractAndOpen(e);
                    } else if ("commandExtract".equalsIgnoreCase(cmd)) {
                        Zipeg.extractList(getSelected(), false);
                    } else if ("commandOpenWith...".equalsIgnoreCase(cmd)) {
                        openWith();
                    } else if ("commandOpen".equalsIgnoreCase(cmd)) {
                        openOrPreview(true);
                    } else if ("commandPreview".equalsIgnoreCase(cmd)) {
                        openOrPreview(false);
                    }
                }
            });
    }

    private void enter() {
        Archive a = Zipeg.getArchive();
        int n = getSelectedRowCount();
        int r = getSelectedRow();
        TreeElement e = n == 1 && r >= 0 ? entries[r] : null;
        if (e != null && e.isDirectory()) {
            Actions.postEvent("selectFolder", entries[r]);
        } if (a != null && e != null && !e.isDirectory() &&
            Util.isArchiveFileType(entries[r].getFile())) {
            a.extractAndOpen(e);
        } else {
            commandActionsPreview();
        }
    }

    private void back(boolean up) {
        if (parent instanceof TreeNode) {
            TreeNode p = (TreeNode)parent;
            TreeNode go = up ? p.getParent() : p;
            if (go instanceof TreeElement) {
                Actions.postEvent("selectFolder", go);
            }
        }
    }

    private void sizeColumns() {
        int n = columnModel.getColumnCount();
        if (n == 0)
            return;
        // set some minimum width, this is necessary to avoid a  strange
        // runaway effect we were seeing.
        // also keep this really small, otherwise we see some  other strange effects
        int columnMinimumWidth = 1;
        int allColumnsWidth = n * columnMinimumWidth;
        Dimension size = getParent().getSize();
        int ourWidth = (size.width < allColumnsWidth) ? allColumnsWidth : size.width;
        // set minimum widths where necessary
        for (int i = 0; i < n; i++) {
            int colWidth = columnModel.getColumn(i).getWidth();
            if (colWidth < columnMinimumWidth) {
                colWidth = columnMinimumWidth;
                // this is necessary in order to be able to change column widths by dragging:
                columnModel.getColumn(i).setWidth(colWidth);
                // if we ignore the next line the column width changes but the heading
                // width does not
                columnModel.getColumn(i).setPreferredWidth(colWidth);
            }
        }
        // calc the total column width, interestingly totalColumnWidth
        // will not equal to what columnModel.getTotalColumnWidth() returns
        int totalColumnWidth = 0;
        for (int i = 0; i < (n - 1); i++) {
            totalColumnWidth += columnModel.getColumn(i).getWidth();
        }
        // now adjust the width of the last column
        if (totalColumnWidth < ourWidth) {
            int lastColumnWidth = ourWidth - totalColumnWidth;
            columnModel.getColumn(n - 1).setWidth(lastColumnWidth); // necessary
            columnModel.getColumn(n - 1).setPreferredWidth(lastColumnWidth); // necessary
        }
        Dimension xpreferredSize = getParent().getPreferredSize();
        setPreferredScrollableViewportSize(new Dimension(ourWidth, xpreferredSize.height));
    }

    private class ItemListModel extends AbstractTableModel {
        public String getColumnName(int c) { return TITLES[c]; }
        public int getRowCount() { return entries.length; }
        public int getColumnCount() { return TITLES.length; }
        public boolean isCellEditable(int row, int column) { return false; }
        public void setValueAt(Object value, int row, int col) {
            throw new Error("not implemented");
        }
        public Object getValueAt(int r, int c) {
            TreeElement e = entries[r];
            switch (c) {
                case 0: return e.getName() + (e.isEncrypted() ? " " + PASSWD : "");
                case 1: return new Long(getElementSize(e));
                case 2: return new Long(getCompressedSize(e));
                case 3: if (e.getTime() > 0) {
                            return new Date(e.getTime());
                        } else {
                            return "";
                        }
                case 4: return e.getComment();
                default: assert false;
            }
            throw new Error("column=" + c + "?");
        }
    }

    private static long getElementSize(TreeElement e) {
        if (e.isDirectory()) {
            return e.getDescendantFileCount();
        } else {
            return e.getSize() + e.getResourceForkSize();
        }
    }

    private static long getCompressedSize(TreeElement e) {
        if (e.isDirectory()) {
            return e.getDescendantFileCount();
        } else {
            return e.getCompressedSize() + e.getResourceForkCompressedSize();
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {

        public void setValue(Object value) {
            if (value instanceof Date) {
                value = " " + shortdate.format((Date)value);
            }
            super.setValue(value);
            setBorder(null);
        }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected, boolean focus,
                                                       int r, int c) {
            File file = new File(entries[r].getFile());
            if (c == 0) {
                setIcon(entries[r].isDirectory() ? folderIcon : getFileTypeIcon(file));
            } else {
                setIcon(null);
            }
            int a = v instanceof Number? SwingConstants.RIGHT : SwingConstants.LEFT;
            boolean isSize = c == 1;
            boolean isCompressedSize = c == 2;
            if (v instanceof Long && (isSize || isCompressedSize)) {
                long value = ((Long)v).longValue();
                if (entries[r].isDirectory()) {
                    v = isCompressedSize ? " " : (Util.formatNumber(value) + " ");
                } else if (value <= 0) {
                    v = "";
                } else if (value > 1024) {
                    v = Util.formatNumber(value / 1024) + " KB ";
                } else {
                    v = Util.formatNumber(value) + " ";
                }
            }
            JLabel p = (JLabel)super.getTableCellRendererComponent(t, v, selected, focus, r, c);
            p.setHorizontalAlignment(a);
            if (!selected) {
                p.setBackground(r % 2 == 0 ? even : odd);
            } else {
                if (selection == null) {
                    selection = p.getBackground();
                    selection_dimmed = new Color(selection.getRed(),
                                                 selection.getGreen(),
                                                 selection.getBlue(), 40);
                }
                p.setBackground(isLastFocused() ? selection : selection_dimmed);
                if (!isLastFocused()) {
                    p.setForeground(t.getForeground());
                }
            }
            Dimension d = getPreferredSize();
            d.width += HORIZONTAL_SPACING;
            TableColumn tc = getColumn(TITLES[c]);
            if (d.width > tc.getMinWidth()) {
                tc.setMinWidth(d.width);
            }
            p.setToolTipText(makeToolTip(r));
            return p;
        }

        private String makeToolTip(int r) {
            TreeElement e = entries[r];
            File file = new File(e.getFile());
            CacheEntry ce = (CacheEntry)Cache.getInstance().get(file);
            String uri = ce == null || ce.thumb == null ? null : ce.thumb.toURI().toString();
            int w = ce == null || ce.image == null ? 0 : ce.image.getWidth();
            int h = ce == null || ce.image == null ? 0 : ce.image.getHeight();
            boolean dir = e.isDirectory();
            long size = getElementSize(e);
            long compressed = dir ? 0 : getCompressedSize(e);
            Date date = new Date(e.getTime());
            return "<html><body>" +
                    "<table>" +
                    "<tr><td>" +
                        "<i>" + (dir ? "directory:" : "file:") + "</i><br><b>" +
                        e.getFile() + "</b><br>" +
                        "<i>" + (dir ? "items:" : "size:") + "</i><br><b>" +
                        Util.formatNumber(size) + "</b>" + (dir ? "" : " bytes") + "<br>" +
                        (dir || compressed <= 0 ? "" :
                            ("<i>compressed:</i><br><b>" +
                            Util.formatNumber(compressed) + "</b> bytes<br>")
                        ) +
                        (e.getTime() > 0 ?
                         ("<i>last modified:</i><br><b>" + longdate.format(date) + "</b><br>") :
                          ""
                        ) +
                        (e.getError() != null ?
                         ("<font color=red>" + e.getError() + (e.isEncrypted() ? " (invalid password?)" : "") + "</font><br>") :
                          ""
                        ) +
                        (e.isEncrypted() ?
                         ("<b>" + PASSWD + " password protected</b><br>") :
                          ""
                        ) +
                        "<table width=200>" +
                        (e.getComment() == null ? "" :
                        "<tr><td>" +
                        e.getComment() +
                        "</td></tr>") +
                        (uri == null ? "" :
                            "<tr><td>" +
                            "<img src=\"" + uri +"\" width=" + w + " height=" + h + ">" +
                            "</td></tr>"
                        ) +
                        "</table>" +
                    "</td></tr>" +
                    "</table>" +
                    "</body></html>";
        }

    }

    private static Icon getFileTypeIcon(File file) {
        try {
            Icon icon = (Icon)iconFileCache.get(file);
            if (icon != null) {
                return icon;
            }
            String filename = file.getName();
            int ix = filename.lastIndexOf(".");
            String extension = ix > 0 ? filename.substring(ix) : "";
            icon = (Icon)iconTypeCache.get(extension);
            if (icon == null) {
                File f = File.createTempFile("icon", extension);
                if (fv == null) {
                    FileChooser fc = FileChooser.getInstance();
                    fv = fc.getUI().getFileView(fc);
                }
                icon = fv.getIcon(f);
                if (icon == null) {
                    icon = fileIcon;
                }
                f.delete();
                iconTypeCache.put(extension, icon);
            }
            return icon;
        } catch (IOException e) {
            return fileIcon;
        }
    }

    // TODO: add text and html lists to the FileTransferable (it will help Finder)

    private static class FileTransferable implements Transferable {

        private ArrayList files;
        private String plain;
        private String html;

        private static final DataFlavor[] flavors = new DataFlavor[9];

        static {
            try {
                flavors[0] = Util.isWindows() ?
                             new DataFlavor("application/x-java-file-list;charset=Unicode;class=java.util.List")
                             : DataFlavor.javaFileListFlavor;
                // html
                flavors[1] = new DataFlavor("text/html;class=java.lang.String");
                flavors[2] = new DataFlavor("text/html;class=java.io.Reader");
                flavors[3] = new DataFlavor("text/html;charset=unicode;class=java.io.InputStream");
                // plain
                flavors[4] = new DataFlavor("text/plain;class=java.lang.String");
                flavors[5] = new DataFlavor("text/plain;class=java.io.Reader");
                flavors[6] = new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream");
                // string
                flavors[7] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.String");
                flavors[8] = DataFlavor.stringFlavor;


            } catch (ClassNotFoundException cle) {
                Debug.traceln("error initializing FileTranserable");
            }
        }

        FileTransferable(ArrayList list) {
            files = new ArrayList();
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                File f = (File)i.next();
                if (Util.isMac()) {
                    try { // Finder expects filepath to be UTF-8 encoded
                        byte[] utf8 = Util.getCanonicalPath(f).getBytes("UTF-8");
                        String s = new String(utf8);
                        f = new File(s);
                    } catch (UnsupportedEncodingException e) {
                        throw new Error(e);
                    }
                }
                files.add(f);
            }
            StringBuffer plainBuf = new StringBuffer();
            StringBuffer htmlBuf = new StringBuffer();
            htmlBuf.append("<html>\n<body>\n<ul>\n");
            for (Iterator i = list.iterator(); i.hasNext();) {
                File file = (File)i.next();
                String val = file == null ? "" : file.getName();
                plainBuf.append(val).append("\n");
                htmlBuf.append("  <li>").append(val).append("\n");
            }
            if (plainBuf.length() > 0) {
                plainBuf.deleteCharAt(plainBuf.length() - 1);
            }
            htmlBuf.append("</ul>\n</body>\n</html>");
            plain = plainBuf.toString();
            html = htmlBuf.toString();
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
//          Debug.traceln("isDataFlavorSupported " + flavor);
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
//          Debug.traceln("getTransferData " + flavor + " " + flavor.equals(DataFlavor.javaFileListFlavor));
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return files;
            } else if (flavor.getMimeType().indexOf("/html") >= 0) {
                return html;
            } else if (DataFlavor.stringFlavor.equals(flavor) || flavor.getMimeType().indexOf("/plain") >= 0) {
                return plain;
            }
            return null;
        }
    }

    private class FileTransferHandler extends TransferHandler {

        public void exportAsDrag(JComponent comp, InputEvent e, int action) {
            int srcActions = getSourceActions(comp);
            int dragAction = srcActions & action;
            if (!(e instanceof MouseEvent)) {
                dragAction = NONE;
            }
            if (dragAction != NONE) {
                if (recognizer == null) {
                    FileDragGestureListener fgl = new FileDragGestureListener();
                    recognizer = new FileDragGestureRecognizer(fgl);
                    recognizer.getDragSource().addDragSourceMotionListener(fgl);
                }
                recognizer.gestured(comp, (MouseEvent)e, srcActions, dragAction);
            } else {
                exportDone(comp, null, NONE);
            }
        }

        public int getSourceActions(JComponent c) {
	    return COPY;
	}

        private void cleanupIconView() {
            if (!new File(".", "cleanup.compiled.scpt").canRead()) {
                return;
            }
            IdlingEventQueue.invokeLater(new Runnable(){
                public void run() {
                    try {
                        Process p = Runtime.getRuntime().exec(
                                new String[]{"osascript", "cleanup.compiled.scpt"},
                                Util.getEnvFilterOutMacCocoaCFProcessPath());
                        p.waitFor();
                    } catch (Exception e) {
                        if (Debug.isDebug()) {
                            throw new Error(e);
                        }
                    }
                }
            });
        }

        public void exportDone(JComponent comp, Transferable data, int action) {
            super.exportDone(comp, data, action);
            // on Mac OS X 10.4 for all Java 1.4.2, 1.5 and 1.6 if drop goes
            // into Icon View of any folder or desktop the items are droped on top of each other
            if (action != NONE && Util.isMac() && getSelectedCached().size() > 1) {
                // tidy up (aka `Snap To Grid` or Finder/View/Clean Up) Icon View of Finder drop target
                // TODO: it is possible to pass set of names (last names) of
                // dropped files/folders to the AppleScript and optimize it N^2 algorithm
                cleanupIconView();
            }
        }

    }

    private static class FileDragGestureRecognizer extends DragGestureRecognizer {

	FileDragGestureRecognizer(DragGestureListener dgl) {
	    super(DragSource.getDefaultDragSource(), null, DnDConstants.ACTION_COPY, dgl);
	}

	void gestured(JComponent c, MouseEvent e, int srcActions, int action) {
	    setComponent(c);
            setSourceActions(srcActions);
	    appendEvent(e);
	    fireDragGestureRecognized(action, e.getPoint());
	}

        protected void registerListeners() { }

        protected void unregisterListeners() { }

    }

    private class FileDragGestureListener extends DragSourceAdapter
                                          implements DragGestureListener {

        public void dragGestureRecognized(DragGestureEvent evt) {
            ArrayList selected = getSelectedCached();
            if (selected.size() == 0) {
                return;
            }
            // unselect directories before drag
            int[] rows = getSelectedRows();
            for (int i = 0; i < rows.length; i++) {
                if (entries[rows[i]].isDirectory()) {
                    getSelectionModel().removeSelectionInterval(rows[i], rows[i]);
                }
            }
            if (DragSource.isDragImageSupported()) {
                evt.startDrag(DragSource.DefaultCopyDrop, createDragImage(rows),
                        new Point(16, 0),
                        new FileTransferable(selected),
                        this);
            } else {
                dragWindow = new TransparentWindow(createDragImage(rows), 0, 0);
                evt.startDrag(DragSource.DefaultCopyDrop, null,
                              new Point(0, 0), new FileTransferable(selected), this);
            }
        }

        private BufferedImage createDragImage(int rows[]) {
            int n = Math.min(rows.length, 8);
            Rectangle rc = getCellRect(0, 0, true);
            int h = rc.height * n;
            BufferedImage bi = new BufferedImage(rc.width, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)bi.getGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
            Color c = null;
            for (int i = 0; i < n; i++) {
                int row = rows[i];
                g.translate(0, i * rc.height);
                if (n != rows.length && i == n - 1) {
                    g.setColor(c);
                    g.drawString("...", 20, rc.height / 2);
                } else {
                    TableCellRenderer cr = getCellRenderer(row, 0);
                    Object v = getValueAt(row, 0);
                    JComponent r = (JComponent)cr.getTableCellRendererComponent(ItemList.this, v, false, false, row, 0);
                    boolean db = r.isDoubleBuffered();
                    boolean op = r.isOpaque();
                    r.setDoubleBuffered(false); // xxx
                    r.setOpaque(false);
                    r.setBounds(0, 0, rc.width, rc.height);
                    r.paint(g);
                    r.setDoubleBuffered(db);
                    r.setOpaque(op);
                    c = r.getForeground();
                }
                g.translate(0, -i * rc.height);
            }
            g.dispose();
            return bi;
        }

        public void dragDropEnd(DragSourceDropEvent dsde) {
            if (dragWindow != null) {
                dragWindow.dispose();
            }
            DragSourceContext dsc = dsde.getDragSourceContext();
            JComponent c = (JComponent)dsc.getComponent();
	    FileTransferHandler fth = (FileTransferHandler)c.getTransferHandler();
            if (dsde.getDropSuccess()) {
                fth.exportDone(c, dsc.getTransferable(), dsde.getDropAction());
	    } else {
                fth.exportDone(c, null, DnDConstants.ACTION_NONE);
            }
        }

        public void dragMouseMoved(DragSourceDragEvent dsde) {
            if (dragWindow != null) {
                dragWindow.setLocation(dsde.getX() + 32, dsde.getY());
                dragWindow.repaint(1);
            }
        }

    }

    private class HeaderMouseHandler extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            JTableHeader h = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = columnModel.getColumn(viewColumn).getModelIndex();
            if (column == -1) {
                return;
            }
            if (column != sortColumn) {
                sortColumn = column;
                sortDirection = 1;
            } else {
                sortDirection = (sortDirection + 2) % 3 - 1;
            }
            getTableHeader().repaint();
            repopulate(parent);
        }
    }

    private static class Arrow implements Icon {

        private boolean descending;
        private int size;

        public Arrow(boolean descending, int size) {
            this.descending = descending;
            this.size = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D)g;
            Color s = g2d.getColor();
            Composite z = null;
            int dx = size;
            int dy = descending ? dx : -dx;
            y = y + size + (descending ? -dy : 0);
            int shift = descending ? 1 : -1;
            if (Util.isMac()) {
                g2d.translate(x + size / 2, y);
                g2d.setColor(Color.GRAY);
                for (int i = 0; i < size; i++) {
                    int cx = (size - i) / 2;
                    if (!descending) {
                        g2d.drawLine(-cx, -i, cx, -i);
                    } else {
                        g2d.drawLine(-cx, i, cx, i);
                    }
                }
                g2d.translate(-(x + size / 2), -y);
                z = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33f));
            }
            g2d.translate(x, y);
            Color color = c == null ? Color.GRAY : c.getBackground();
            // Right diagonal.
            g2d.setColor(color.darker());
            g2d.drawLine(dx / 2, dy, 0, 0);
            g2d.drawLine(dx / 2, dy + shift, 0, shift);
            // Left diagonal.
            g2d.setColor(color.brighter());
            g2d.drawLine(dx / 2, dy, dx, 0);
            g2d.drawLine(dx / 2, dy + shift, dx, shift);
            // Horizontal line.
            if (descending) {
                g2d.setColor(color.darker().darker());
            } else {
                g2d.setColor(color.brighter().brighter());
            }
            g2d.drawLine(dx, 0, 0, 0);
            g2d.translate(-x, -y);
            g2d.setColor(s);
            if (Util.isMac()) {
                g2d.setComposite(z);
            }
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }


    private class SortableHeaderRenderer implements TableCellRenderer {

        private TableCellRenderer tableCellRenderer;

        public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component c;
            try {
                c = tableCellRenderer.getTableCellRendererComponent(table,
                        value, isSelected, hasFocus, row, column);
            } catch (NullPointerException x) {
                // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6428968
                // and  http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6578189
                /*  XPDefaultRenderer.XPDefaultRenderer.getTableCellRendererComponent(...) {
                        ...
                        setBorder(new EmptyBorder(skin.getContentMargin()));
                        return this;
                    }
                */
                JLabel tcr = (JLabel)tableCellRenderer;
                tcr.setBorder(new EmptyBorder(new Insets(4,4,4,4)));
                c = tcr;
            }
            if (c instanceof JLabel) {
                JLabel l = (JLabel)c;
                l.setHorizontalTextPosition(JLabel.RIGHT);
                int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
            }
            return c;
        }
    }

    private Icon getHeaderRendererIcon(int column, int size) {
        if (column != sortColumn || sortDirection == 0) {
            return null;
        }
        int n = size * 3 / 4 - (Util.isMac() ? 2 : 0);
        return new Arrow(sortDirection == -1, n);
    }

}