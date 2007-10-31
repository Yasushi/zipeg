package com.zipeg;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.beans.*;

public final class Zipeg implements Runnable {

    private static boolean plafInitialized = setSystemLookAndFeel("Zipeg", Flags.getFlag(Flags.METAL));
    private static ArrayList args = null;
    private static Set options = new HashSet();
    private static Archive archive = null;
    private static boolean associationsChecked;
    private static Iterator testArchives;
    private static boolean test;
    private static final ArrayList recent = new ArrayList();

    private Zipeg() {
    }

    public static void main(String[] a) {
        assert plafInitialized; // to make sure it is referenced
        // starting from 1.2 version file moved inside jar. Keep this line
        // till absolutely sure no 1.0.1 clients left in the field.
        Util.getVersionFile().delete();
        // order of initialization is important.
        args = new ArrayList(Arrays.asList(a));
        if (args.contains("--clean")) {
            // reset preferences to initial state
            try {
                Presets.clear();
                return;
            } catch (BackingStoreException e) {
                throw new Error(e);
            }
        }
        // add start directory to java.library.path
        String pwd = System.getProperty("user.dir");
        String lp = System.getProperty("java.library.path");
        lp = lp == null || lp.length() == 0 ? pwd : lp + File.pathSeparator + pwd;
        System.setProperty("java.library.path", lp);

        parseOptions();
        Debug.init(options.contains("debug") || options.contains("g"));
        if (options.contains("uninstall-cleanup")) {
            FileAssociations fa = new FileAssociations();
            if (fa.isAvailable()) {
                fa.setHandled(0);
                System.exit(0);
            }
        }
        IdlingEventQueue.init();
        EventQueue.invokeLater(new Zipeg());
    }

    private static boolean checkDiskSpace() {
        final int REQUIRED = 32 * Util.MB;
        File file = null;
        try {
            file = File.createTempFile("test", "tmp");
            file.deleteOnExit();
            Class c = Class.forName("java.io.FileSystem");
            Field su = c.getField("SPACE_USABLE");
            su.setAccessible(true);
            final int SPACE_USABLE = su.getInt(null);
            Method getFileSystem = c.getMethod("getFileSystem", Util.VOID);
            getFileSystem.setAccessible(true);
            Object fs = getFileSystem.invoke(c, Util.NONE);
            Method getSpace = fs.getClass().getMethod("getSpace", new Class[]{File.class, int.class});
            getSpace.setAccessible(true);
            Long space = (Long)getSpace.invoke(fs, new Object[]{file, new Integer(SPACE_USABLE)});
            file.delete();
            return space.longValue() > REQUIRED;
        }
        catch (Throwable ignore) { // ClassNotFoundException, NoSuchMethodException, IllegalAccessException
                                   // InvocationTargetException, NativeMethodNotFound
            // ignore.printStackTrace();
        }
        try {
//          long time = System.currentTimeMillis();
            if (file == null) {
                file = File.createTempFile("test", "tmp");
            }
            FileOutputStream os = new FileOutputStream(file);
            FileChannel out = os.getChannel();
            out.position(REQUIRED);
            ByteBuffer bb = ByteBuffer.allocate(1);
            out.write(bb, REQUIRED);
            out.close();
            os.close();
            file.delete();
//          time = System.currentTimeMillis() - time;
//          Debug.traceln("checkDiskSpace(" + REQUIRED / Util.MB + "MB) " + time + " milliseconds");
            return true;
        } catch (IOException iox) {
            if (file != null) {
                file.delete();
            }
            return false;
        }
    }

    public void run() {
/*      for testing crash log UI
        try {
            new File("./test-crash.txt").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CrashLog.report("test", "./test-crash.txt");
*/
        if (options.contains("report-crash") && args.size() >= 2) {
            CrashLog.report((String)args.get(0), (String)args.get(1));
            System.exit(153);
        }
        Z7.loadLibrary();
        if (Util.isWindows()) {
            Registry.getInstance();
        } else if (Util.isMac() && Util.getOsVersion() >= 10.4) {
            DefaultRoleHandler.getInsance();
        }
        if (Util.isWindows()) {
            Registry.getInstance().initializeOLE();
        }
        boolean firstRun = options.contains("first-run");
        if (!firstRun && Updater.getUpdateFile().canRead()) {
            updateDownloaded(Updater.getUpdateFile());
        }
        if (firstRun && Util.isWindows()) {
            String installdir = System.getProperty("user.dir");
            if (!Registry.registerZipeg(installdir)) {
                JOptionPane.showMessageDialog(null,
                        "<html><body>Zipeg can be installed <b>only</b> by a user " +
                        "with <b>Administrator</b> privileges.<br><br>" +
                        "Please login as <b>Administrator</b> and reinstall the application, or<br>" +
                        "contact your support personel to install it for you.<br><br>" +
                        (Util.isWindows() && Util.getOsVersion() > 5.1 ?
                        "On Windows Vista &tm; you may need to right mouse<br>" +
                        "click zipeg-setup.exe and choose <b>Run As Administrator.</b><br><br>" : "") +
                        "<font color=red>Zipeg installation cannot continue.</font><br>" +
                        "</body></html>", "Zipeg Setup",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        if (!checkDiskSpace()) {
            JOptionPane.showMessageDialog(null,
                    "<html><body>Not enough free disk space left.<br>" +
                    "Zipeg cannot continue.<br>Try to empty " +
                    (Util.isMac() ? "Trash" : "Recycle") + " Bin,<br>" +
                    "and to restart the application." +
                    "</body></html>", "Zipeg",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (Util.isMac()) {
            checkRunningFromDmg();
        }
        Actions.addListener(this);
        final MainFrame frame = new MainFrame();
        assert !frame.isVisible();
        if (Util.isMac()) {
            // must be called after frame is created
            Util.callStatic("com.zipeg.mac.MacSpecific.initMacOSXInterface", Util.NONE);
        }
        showMainFrame();
        boolean b = Presets.getBoolean("licenseAccpeted", false);
        if (!b) {
            if (!Util.isWindows()) {
                // on Win32 license is accpeted at the installer
                License.showLicence(true);
            }
            Presets.putBoolean("licenseAccpeted", true);
        }
        loadRecent();
        if (firstRun) {
            // Only once create installation guid
            if (Presets.get("zipeg.uuid", null) == null) {
                Presets.put("zipeg.uuid", Util.uuid());
            }
            Util.sleep(2 * 1000);
//          JOptionPane.showMessageDialog(null, "first run", "Zipeg Setup", JOptionPane.INFORMATION_MESSAGE);
        }
        Updater.cleanUpdateFiles();
        Updater.checkForUpdate(false);
        checkAssociations();
        if (firstRun) {
            Actions.postEvent("setMessage", "Zipeg is installed and ready to be used.");
        }
        if (getArgumentCount() > 0) {
            IdlingEventQueue.invokeOnIdle(new Runnable() { // give UI time to paint itself
                public void run() {
                    open(new File(getArgument(0)));
                }
            });
        }
        if (hasOption("test")) {
            test();
        }
        DropTargetAdapter dta = new DropTargetAdapter() {

            public void dragOver(DropTargetDragEvent dtde) {
                if (dtde.getCurrentDataFlavorsAsList().contains(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
            }

            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable t = dtde.getTransferable();
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List files = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() == 1) {
                        File file = (File)files.get(0);
                        if (Util.isArchiveFileType(Util.getCanonicalPath(file))) {
                            open(file);
                        }
                    }
                } catch (UnsupportedFlavorException e) {
                    /* ignore */
                } catch (IOException e) {
                    /* ignore */
                }
            }
        };
        frame.addDropTargetAdapter(dta);
        if (Util.isWindows()) {
            workaroundMenuDropShadowBorder();
            workaroundDnDAutoscrollCursorHysteresis();
        }
        if (firstRun) {
            Util.invokeLater(3000, new Runnable() {
                public void run() { commandFileOpen(Util.getCanonicalPath(new File(".", "sample.zip"))); }
            });
        }
    }

    private void test() {
        File f = new File(Util.getHome(), "zipeg.test.txt");
        if (f.exists()) {
            try {
                ArrayList files = new ArrayList();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                for (;;) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    File a = new File(line);
                    if (!a.isDirectory() || a.canRead()) {
                        files.add(new File(line));
                    }
                }
                test = files.size() > 0;
                testArchives = files.iterator();
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }

    public static String getRecent(int ix) {
        return ix < recent.size() ? (String)recent.get(ix) : null;
    }

    private void checkRunningFromDmg() {
        String cd = Util.getCanonicalPath(new File(".")).toLowerCase();
        boolean mounted = cd.startsWith("/volumes/zipeg/zipeg");
        boolean desktop = cd.startsWith("/users/") && cd.indexOf("/desktop/") > 0;
        if (mounted || desktop) {
            File temp = null;
            Image install = null;
            FileOutputStream fos = null;
            try {
                install = Resources.getImage("mac.install");
                temp = File.createTempFile("zipeg-install", ".png");
                temp.delete();
                fos = new FileOutputStream(temp);
                fos.write(Resources.getBytes("mac.install"));
                fos.close();
                fos = null;
            } catch (IOException e) {
                if (temp != null) { temp.delete(); }
                temp = null;
            } finally {
                Util.close(fos);
            }
            int w = install == null ? 0 : install.getWidth(null);
            int h = install == null ? 0 : install.getHeight(null);
            assert temp == null || temp.canRead();
            String user = System.getProperty("user.name");
            JOptionPane.showMessageDialog(MainFrame.getTopFrame(),
                "<html><body>" +
                "<p>Zipeg cannot run from mounted DMG file or from Desktop.<br>" +
                "Please do following steps:<br>" +
                "Close this message box.<br>" +
                "Drag <b>Zipeg.app</b> into the /<b>Applications</b> folder.<br>" +
                "Eject Zipeg volume.<br>" +
                "Start <b>Zipeg</b> from /<b>Applications</b>.</p>" +
                (temp != null ?
                "<img src=\"file:" + Util.getCanonicalPath(temp) +"\" width=" + w + " height=" + h + "><br>" :
                "") +
                "<i>If your are not allowed to modify root </i><b>/Applications</b><i> folder<br>" +
                "create local </i><b>/Users/" + user + "/Applications/</b><i> folder and drag<br>" +
                "Zipeg there.</i></p>" +
                "</body></html>",
                "Zipeg Prompt: Running from DMG file",
                JOptionPane.ERROR_MESSAGE);
            if (temp != null) {
                temp.delete();
                temp.deleteOnExit(); // html pane holds it locked...
            }
            System.exit(-1);
        }
    }

    private static void parseOptions() {
        for (Iterator i = args.iterator(); i.hasNext();) {
            String opt = (String)i.next();
            if (opt.startsWith("--")) {
                options.add(opt.substring(2));
                i.remove();
            } else if (opt.startsWith("-")) {
                options.add(opt.substring(1));
                i.remove();
            }
        }
    }

    public boolean hasOption(String option) {
        return options.contains(option);
    }

    public int getArgumentCount() {
        return args.size();
    }

    public String getArgument(int i) {
        return ((String)args.get(i)).trim();
    }

    public void updateCommandState(Map m) {
        m.put("commandFileOpen", Boolean.TRUE);
        m.put("commandFileNew", Boolean.TRUE);
        m.put("commandFileExit", Boolean.TRUE);
        m.put("commandToolsOptions", Boolean.TRUE);
        m.put("commandHelpLicense", Boolean.TRUE);
        m.put("commandHelpIndex", Boolean.TRUE);
        m.put("commandHelpWeb", Boolean.TRUE);
        m.put("commandHelpDonate", Boolean.TRUE);
        m.put("commandHelpSupport", Boolean.TRUE);
        m.put("commandHelpAbout", Boolean.TRUE);
        m.put("commandHelpCheckForUpdate", Boolean.TRUE);
    }

    public static void commandHelpCheckForUpdate() {
        Updater.checkForUpdate(true);
    }

    public static void commandFileExit() {
        checkAssociations();
        MainFrame.getInstance().setVisible(false);
        commandFileClose(true);
        Debug.traceln("commandFileExit");
        if (!MainFrame.getInstance().inProgress()) {
            EventQueue.invokeLater(new Runnable(){
                // give others last chance to save preferrences
                public void run() {
                    MainFrame.getInstance().dispose();
                    Util.rmdirs(Util.getCacheDirectory());
                    System.exit(0);
                }
            });
        } else {
            // There is no known way to disable Quit menu item on Mac OS X
            // just do nothing while operation is in progress
            Util.invokeLater(1000, new Runnable(){
                public void run() { Actions.postEvent("commandFileExit"); }
            });
        }
    }

    private static void checkAssociations() {
        FileAssociations fa = new FileAssociations();
        if (fa.isAvailable() && !associationsChecked) {
            if (Util.isMac() ||
                Util.isWindows() && options.contains("first-run")) {
                associationsChecked = true;
                long handled = fa.getHandled();
                if (handled == 0 ||
                    handled == 1 && // only zip
                    (!Presets.getBoolean("zipOnlyOK", false))) {
                    fa.askHandleAll();
                    Presets.putBoolean("zipOnlyOK", true);
                }
            }
        }
    }

    public static void commandToolsOptions() {
        Debug.traceln("commandToolsOptions");
        Settings.showPreferences();
    }

    public static void commandHelpAbout() {
        Debug.traceln("commandHepAbout");
        About.showMessage();
    }

    public static void commandHelpIndex() {
        Debug.traceln("commandHepIndex");
        if (Util.isMac()) {
            Util.openUrl("http://www.zipeg.com/mac.faq.html");
        } else {
            Util.openUrl("http://www.zipeg.com/win.faq.html");
        }
    }

    public static void commandHelpWeb() {
        Debug.traceln("commandHepIndex");
        Util.openUrl("http://www.zipeg.com");
    }

    public static void commandHelpDonate() {
        Debug.traceln("commandHepDonate");
        if (Util.isMac()) {
            Util.openUrl("http://www.zipeg.com/mac.donate.html");
        } else {
            Util.openUrl("http://www.zipeg.com/win.donate.html");
        }
    }

    public static void commandHelpSupport() {
        Debug.traceln("commandHepDonate");
        if (Util.isMac()) {
            Util.openUrl("http://www.zipeg.com/mac.support.html");
        } else {
            Util.openUrl("http://www.zipeg.com/win.support.html");
        }
    }

    public static void commandHelpLicense() {
        License.showLicence(false);
    }

    public static void commandEditCut() {
        Debug.traceln("commandEditCut");
    }

    public static void commandEditCopy() {
        Debug.traceln("commandEditCopy");
    }

    public static void commandEditCopyPath() {
        Debug.traceln("commandEditCopy");
    }

    public static void commandEditPaste() {
        Debug.traceln("commandEditPaste");
    }

    public static void commandFileNew() {
        Debug.traceln("commandFileNew - not implemented");
    }

    public static void commandFilePrint() {
        Debug.traceln("commandFilePrint");
    }

    public static void commandFilePrint(String filename) {
        Debug.traceln("commandFilePrint(" + filename + ")");
    }

    public static void updateAvailable(Object param) {
        Object[] p = (Object[])param;
        updateAvailable((Integer)p[0], (Long)p[1],
                (String)p[2], (String)p[3], (String)p[4]);
    }

    private static int updating;

    /**
     * updateAvailable event handler is invoked from Updater.checkForUpdate
     * @param rev  revision
     * @param nextUpdate nextUpdate time when the query was issued
     * @param ver version
     * @param url URL of file to download
     * @param msg message
     */
    public static void updateAvailable(Integer rev, Long nextUpdate,
            String ver, String url, String msg) {
        assert IdlingEventQueue.isDispatchThread();
        if (updating > 0) {
            return;
        }
        updating++;
        if (rev.intValue() <= Util.getRevision()) {
            if (nextUpdate.longValue() == 1) {
                JOptionPane.showMessageDialog(MainFrame.getTopFrame(),
                        "<html><body>Your version of Zipeg is up to date." +
                        "</body></html>", "Zipeg: Check for Updates",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (askUpdate(ver, msg)) {
            Updater.download(url);
        }
        updating--;
    }

    /**
     * updateDownloaded event handler is invoked aftet update has been downloaded
     * @param param - file into which update has been downloaded
     */
    public static void updateDownloaded(Object param) {
        File file = (File)param;
        assert IdlingEventQueue.isDispatchThread();
        if (!file.canRead()) {
            Updater.cleanUpdateFiles();
            return;
        }
        if (!askRestart()) {
            return;
        }
        if (Util.isWindows()) {
            try {
                Runtime.getRuntime().exec(Util.getCanonicalPath(file));
            } catch (IOException e) {
                throw new Error(e);
            }
            System.exit(0);
        }
        else {
            File wd = new File(Util.getCanonicalPath(new File(".")));
            String location = "CopyAndRestart.class";
            InputStream i = null;
            FileOutputStream o = null;
            try {
                i = CopyAndRestart.class.getResource(location).openStream();
                File car = new File(".", "com/zipeg/CopyAndRestart.class");
                car.delete();
                car.getParentFile().mkdirs();
                car.createNewFile();
                o = new FileOutputStream(car);
                int n = i.available();
                byte[] buff = new byte[n];
                int k = i.read(buff);
                assert k == n;
                o.write(buff);
                Util.close(i);
                i = null;
                Util.close(o);
                o = null;
                String java = Util.getJava();
                Runtime.getRuntime().exec(new String[]{ java, "com.zipeg.CopyAndRestart",
                                          Util.getCanonicalPath(file),
                                          Util.getCanonicalPath(wd), java});
                System.exit(0);
            } catch (IOException e) {
                throw new Error(e);
            } finally {
                Util.close(i);
                Util.close(o);
            }
        }
    }

    private static boolean askRestart() {
        assert IdlingEventQueue.isDispatchThread();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("<html>New version has been downloaded successfully.<br>" +
                "Do you want to restart the application now?<br></html>"));
        return JOptionPane.showConfirmDialog(MainFrame.getTopFrame(), panel,
                "Zipeg: Update Downloaded",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }


    private static boolean askUpdate(String ver, String msg) {
        assert IdlingEventQueue.isDispatchThread();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("<html>new version " + ver + " is available<br>" +
                "Do you want to download it now?<br></html>"));
        if (msg != null && msg.length() > 0) {
            panel.add(new JLabel(msg));
        }
        return JOptionPane.showConfirmDialog(MainFrame.getTopFrame(), panel,
                "Zipeg: Update Available",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    public static void commandReopenArchive() {
        if (archive != null && archive.getName() != null) {
             commandFileOpen(archive.getName());
        }
        showMainFrame();
    }

    public static void commandFileOpen(String filename) {
        Debug.traceln("commandFileOpen(" + filename + ")");
        if (filename != null && new File(filename).exists()) {
            Zipeg.open(new File(filename));
        } else {
            Zipeg.commandFileOpen();
        }
    }

    public static void commandFileOpen() {
        if (MainFrame.getInstance().inProgress()) {
            return;
        }
        String dir = Presets.get("fileOpenFolder", Util.getCanonicalPath(Util.getDocuments()));
        if (Util.isMac()) {
            // see: http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/CocoaComponent.html
            System.setProperty("com.apple.eawt.CocoaComponent.CompatibilityMode", "false");
            System.setProperty("apple.awt.use-file-dialog-package", "false");
            System.setProperty("JFileChooser.packageIsTraversable", "true");
            System.setProperty("JFileChooser.appBundleIsTraversable", "true");
            FileDialog open = new FileDialog(MainFrame.getTopFrame(), "Zipeg: Open Archive", FileDialog.LOAD);
            open.setDirectory(dir);
            open.setFilenameFilter(new FilenameFilter(){
                public boolean accept(File dir, String name) {
                    return Util.isArchiveFileType(name);
                }
            });
            open.setVisible(true);
            if (open.getFile() != null) {
                File file = new File(open.getDirectory(), open.getFile());
                open(file.getAbsoluteFile());
                dir = file.getParent();
            }
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
        } else {
            javax.swing.filechooser.FileFilter archives = new javax.swing.filechooser.FileFilter(){
                public boolean accept(File f) {
                    try {
                        return f.isDirectory() || Util.isArchiveFileType(f.getName());
                    } catch (Throwable t) { // InterruptedException
                        return false;
                    }
                }
                public String getDescription() {
                    return "Archives (.zip .rar .arj .lha .7z .iso .jar ...)";
                }
            };
            JFileChooser fc = FileChooser.getInstance();
            if (Util.isMac()) {
                FilteredFileSystemView ffsv = new FilteredFileSystemView(fc, fc.getFileSystemView());
                fc.setFileSystemView(ffsv);
            } else if (Util.isWindows()) {
                // workaround for: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
                FilteredFileSystemView ffsv = new FilteredFileSystemView(fc, fc.getFileSystemView());
                fc.setFileSystemView(ffsv);
            }
            fc.setCurrentDirectory(new File(dir));
            fc.setFileHidingEnabled(true);
            fc.setDialogTitle("Zipeg: Open Archive");
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.addChoosableFileFilter(archives);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileFilter(archives);
            fc.setMultiSelectionEnabled(false);
            int ok = fc.showOpenDialog(MainFrame.getTopFrame());
            JComponent cp = (JComponent)MainFrame.getInstance().getContentPane();
            cp.paintImmediately(cp.getBounds());
            if (ok == JFileChooser.APPROVE_OPTION) {
                open(fc.getSelectedFile());
                dir = fc.getSelectedFile().getParent();
            }
        }
        if (dir != null) {
            Presets.put("fileOpenFolder", dir);
        }
    }

    public void commandActionsExtract() {
        assert IdlingEventQueue.isDispatchThread();
        Debug.traceln("commandActionsExtract");
        if (archive != null) {
            List list = MainFrame.getInstance().getSelected();
            if (list != null && list.size() == 0) {
                list = null; // extract all
            }
            if (list == null || Flags.getFlag(Flags.EXTRACT_WHOLE)) {
                list = null;
            } else if (!Flags.getFlag(Flags.EXTRACT_SELECTED)) {
                assert Flags.getFlag(Flags.EXTRACT_ASK);
                int i = askSelected(list);
                if (i < 0) {
                    return;
                } else if (i > 0) {
                    list = null;
                }
            }
            extractList(list, Flags.getFlag(Flags.CLOSE_AFTER_EXTRACT));
        }
    }

    public static void extractList(List list, boolean quit) {
        String destination = MainFrame.getInstance().getDestination();
        if (destination.length() == 0) {
            Actions.postEvent("chooseDestination");
            return;
        }
        File dst;
        if (Flags.getFlag(Flags.APPEND_ARCHIVE_NAME)) {
            TreeElement root = (TreeElement)archive.getRoot();
            String zipname = new File(archive.getName()).getName();
            int ix = zipname.lastIndexOf(".");
            if (ix > 0) zipname = zipname.substring(0, ix);
            String onlychild = root.getChildrenCount() == 1 ?
                    ((TreeElement)root.getChildren().next()).getName() : null;
            // do not append twice
            if (zipname.equals(onlychild)) {
                dst = new File(destination);
            } else if (destination.endsWith(File.separator + zipname)) {
                dst = new File(destination);
            } else {
                dst = new File(destination, zipname);
                if (dst.exists() && !dst.isDirectory()) {
                    dst = new File(destination, zipname + ".unziped");
                }
            }
        } else {
            dst = new File(destination);
        }
        if (dst.exists() && !dst.isDirectory()) {
            Actions.reportError("Destination: " + destination + "\n" +
                    "Cannot extract over existing file. " +
                    "Please specify another destination " +
                    "folder to extract to.");
            return;
        }
        if (!dst.exists()) {
            if (!askCreateFolder(dst)) {
                return;
            }
            String error = null;
            try {
                boolean b = dst.mkdirs();
                if (!b) {
                    error = "make direcory error";
                    if (dst.getParent() != null) {
                        error += ".\nCheck that " + dst.getParent() + "" +
                                " exists\nand you have sufficient" +
                                " rights to create folders at that location.";
                    }
                }
            } catch (SecurityException iox) {
                error = iox.getMessage();
            }
            if (error != null) {
                Actions.reportError("Failed to create folder:\n" + dst +
                        "\n" + error);
                return;
            }
        }
        archive.extract(list, dst, quit);
        if (Flags.getFlag(Flags.LOCATION_LAST)) {
            MainFrame.getInstance().saveDestination();
        }
    }

    private int askSelected(List selected) {
        if (selected == null) {
            return +1; // whole archive
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("<html><body>Extract:</html>"));
        JRadioButton sel = new JRadioButton("<html><body>" +
                                            Util.plural(selected.size(),
                                            "<b>selected</b> item"));
        JRadioButton all = new JRadioButton("<html><body><b>whole</b> archive</body></html>");
        ButtonGroup choice = new ButtonGroup();
        choice.add(all);
        choice.add(sel);
        panel.add(all);
        panel.add(sel);
        if (Presets.getBoolean("extractPreferSelected", false)) {
            choice.setSelected(sel.getModel(), true);
        } else {
            choice.setSelected(all.getModel(), true);
        }
        JCheckBox cbx = new JCheckBox("Always do the same without asking.");
        panel.add(cbx);
        int i = JOptionPane.showConfirmDialog(MainFrame.getTopFrame(), panel,
            "Zipeg Prompt: What to Extract?",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (cbx.isSelected()) {
            Flags.removeFlag(Flags.EXTRACT_ASK|Flags.EXTRACT_WHOLE|Flags.EXTRACT_SELECTED);
            Flags.addFlag(all.isSelected() ? Flags.EXTRACT_WHOLE : Flags.EXTRACT_SELECTED);
        }
        Presets.putBoolean("extractPreferSelected", sel.isSelected());
        if (i == JOptionPane.CANCEL_OPTION) {
            return -1;
        }
        return all.isSelected() ? +1 : 0;
    }

    private static boolean askCreateFolder(File dst) {
        int i = JOptionPane.YES_OPTION;
        if (Flags.getFlag(Flags.PROMPT_CREATE_FOLDERS)) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(new JLabel("<html><body>Folder <b>" + dst + "</b> does not exist.<br><br>" +
                "Do you want to create it?<br>&nbsp;</body></html>"));
            JCheckBox cbx = new JCheckBox("Always create folders without asking.");
            panel.add(cbx);
            i = JOptionPane.showConfirmDialog(MainFrame.getTopFrame(), panel,
                "Zipeg Prompt: Create Folder",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (cbx.isSelected()) {
                Flags.removeFlag(Flags.PROMPT_CREATE_FOLDERS);
            }
        }
        return i == JOptionPane.YES_OPTION;
    }

    public static Archive getArchive() {
        return archive;
    }

    public static void archiveOpened(Object param) {
        assert IdlingEventQueue.isDispatchThread();
        assert param != null;
        if (archive != null) {
            archive.close();
            Cache.getInstance().clear();
        }
        archive = (Archive)param;
        recent.add(0, archive.getName());
        while (recent.size() > 8) {
            recent.remove(recent.size() - 1);
        }
        saveRecent();
        showMainFrame();

        if (test) {
            IdlingEventQueue.invokeOnIdle(new Runnable(){
                public void run() {
                    if (testArchives.hasNext()) {
                        File f = (File)testArchives.next();
                        Debug.traceln("testing: " + f);
                        open(f);
                    }
                }
            });
        }

        if (!Flags.getFlag(Flags.DONT_OPEN_NESTED)) {
            TreeElement r = (TreeElement)archive.getRoot();
            if (r != null && r.getDescendantFileCount() == 1) {
                TreeElement c = getSingleDescendant(r);
                Debug.traceln("single descendant " + c);
                boolean composite = Util.isCompositeArchive(archive.getName());
                if (c != null && (composite || Util.isArchiveFileType(c.getFile()))) {
                    Debug.traceln("extract and open " + c);
                    archive.extractAndOpen(c);
                }
            }
        }
    }

    private static TreeElement getSingleDescendant(TreeElement root) {
        for (Iterator i = root.getChildren(); i.hasNext(); ) {
            TreeElement child = (TreeElement)i.next();
            if (child.getDescendantFileCount() > 0) {
                return getSingleDescendant(child);
            } else {
                if (!child.isDirectory()) {
                    return child;
                }
            }
        }
        return null;
    }
        
    private static void commandFileClose(boolean exiting) {
        assert IdlingEventQueue.isDispatchThread();
        if (Util.isMac()) {
            MainFrame.getInstance().setVisible(false);
        }
        if (!exiting) {
            Updater.checkForUpdate(false);
        }
        Debug.traceln("commandFileClose");
        IdlingEventQueue.invokeLater(new Runnable(){
            // give DirectoryTree chance to get rid of all ArchiveTreeNodes
            public void run() {
                if (archive != null) {
                    archive.close();
                    Cache.getInstance().clear();
                    archive = null;
                    System.gc();
                }
            }
        });
    }

    public static void commandFileClose() {
        commandFileClose(false);
    }

    public void extractionCompleted(Object param) { // {error, quit} or {null, quit}
        Object[] p = (Object[])param;
        String error = (String)p[0];
        Boolean quit = (Boolean)p[1];
        if (error == null && quit.booleanValue()) {
            Util.invokeLater(2000, new Runnable() {
                public void run() {
                    Actions.postEvent("commandFileExit");
                }
            });
        }
    }

    // Mac specific:

    public static void commandReOpenApplication() {
        Debug.traceln("commandReOpenApplication");
        Updater.checkForUpdate(false);
        showMainFrame();
    }

    public static void commandOpenAppication() {
        Debug.traceln("commandOpenApplication");
        Updater.checkForUpdate(false);
        showMainFrame();
    }

    private static void showMainFrame() {
        if (MainFrame.getInstance() != null) {
            MainFrame.getInstance().setVisible(true);
        }
    }

    private static void open(File file) {
        showMainFrame();
        if (MainFrame.getInstance().inProgress()) {
            return;
        }
        int count = Presets.getInt("extract.count", 0);
        Presets.putInt("extract.count", count + 1);
        ArchiveProcessor.open(file);
    }

    private static void saveRecent() {
        for (int i = 0; i < recent.size(); i++) {
            String name = (String)recent.get(i);
            Presets.put("recent.archive." + i, name);
        }
    }

    private static void loadRecent() {
        boolean done = false;
        recent.clear();
        for (int i = 0; !done; i++) {
            String name = Presets.get("recent.archive." + i, null);
            if (name != null) {
                recent.add(name);
            } else {
                done = true;
            }
        }
    }

    private void workaroundMenuDropShadowBorder() {
        Border defaultBorder = UIManager.getBorder("PopupMenu.border");
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                new DropShadowBorder(Color.WHITE, 0, false),
                defaultBorder));
        AWTEventListener listener = new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == ContainerEvent.COMPONENT_ADDED) {
                    Component c = ((ContainerEvent)e).getChild();

                    if (c instanceof JPopupMenu) {
                        JPopupMenu menu = (JPopupMenu)c;
                        menu.setOpaque(true);
                        ((JComponent)menu.getParent()).setOpaque(false);
                        ((JComponent)menu.getComponent()).setOpaque(false);
                    }

                    if (c instanceof JSeparator && c.getParent() instanceof JPopupMenu) {
                        JSeparator separator = (JSeparator)c;
                        separator.setOpaque(true);

                    }
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.CONTAINER_EVENT_MASK);
    }

    /*
    A bit more compex. Somebody replaces Integer(10) by Srring("10")
    and setDesktopProperty(null) by String("win.drag.x") later in the game.
    */
    private static void setDnDAutoscrollCursorHysteresis() {
        try {
            Toolkit t = Toolkit.getDefaultToolkit();
            Object value = t.getDesktopProperty("DnD.Autoscroll.cursorHysteresis");
            // in 1.7-ea String("win.drag.x")
            if (!(value instanceof Integer)) {
                Method setDesktopProperties = Toolkit.class.getDeclaredMethod(
                        "setDesktopProperty", new Class[]{String.class, Object.class});
                setDesktopProperties.setAccessible(true);
                setDesktopProperties.invoke(t,
                        new Object[]{"DnD.Autoscroll.cursorHysteresis", new Integer(10)});
            }
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private static void workaroundDnDAutoscrollCursorHysteresis() {
        // for 1.4 it is DnD.gestureMotionThreshold = Integer(5)
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4407536
        if (Util.getJavaVersion() >= 1.7) {
            Toolkit t = Toolkit.getDefaultToolkit();
            t.addPropertyChangeListener("DnD.Autoscroll.cursorHysteresis", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Debug.traceln(evt.getPropertyName() + "(" + evt.getOldValue() + ")" + "=" + evt.getNewValue());
                    setDnDAutoscrollCursorHysteresis();
                }
            });
            setDnDAutoscrollCursorHysteresis();
            Debug.traceln("getDesktopProperty(\"DnD.Autoscroll.cursorHysteresis\") = " +
                           t.getDesktopProperty("DnD.Autoscroll.cursorHysteresis"));
        }
    }

    static boolean setSystemLookAndFeel(String name, boolean metal) {
        /* it is very important that some system properties e.g. apple.awt.brushMetalLook
           are set before any code from ATW is executed. E.g. having static Dimension field
           that is initialized before setSystemLookAndFeel will make metal to disappear
           on the Macintosh. For this purpose setSystemLookAndFeel is actually called
           from static field initialization which will still not guarantee that it is
           executed before AWT initialized. If you experience lose of Brushed Metal L&F
           hunt via versions and see when AWT initialization kicked in the static section.
          */
        try {
            System.setProperty("swing.handleTopLevelPaint", "true");
            System.setProperty("sun.awt.noerasebackground", "true");
            if (Util.getJavaVersion() < 1.5) {
                System.setProperty("swing.disableFileChooserSpeedFix", "true");
            }
            // http://developer.apple.com/releasenotes/Java/java141/system_properties/chapter_4_section_3.html
            if (Util.isMac()) {
                if (metal) {
                    System.setProperty("apple.awt.showGrowBox", "true");
                    System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
                    if (Util.getJavaVersion() < 1.6) {
                        // GrowBox painting is broken in Java prio to 1.6 on Mac
                        // It took Apple few years to paint growbox correctly... :-(
                        System.setProperty("apple.awt.showGrowBox", "false");
                    }
                    System.setProperty("apple.awt.brushMetalLook", "true");
                    System.setProperty("apple.awt.brushMetalRounded", "true");
                }
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.graphics.UseQuartz", "true"); // DDX?
                System.setProperty("apple.awt.textantialiasing","true");
                System.setProperty("com.apple.mrj.application.live-resize", "true");
                System.setProperty("com.apple.macos.smallTabs","false");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
            }
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        } catch (Throwable e) {
            throw new Error(e);
        }
        return true;
    }


}

