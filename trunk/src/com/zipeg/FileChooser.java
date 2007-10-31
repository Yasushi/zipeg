package com.zipeg;

import javax.swing.*;
import java.awt.*;

public class FileChooser extends JFileChooser {

    private static FileChooser instance;

    static {
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4525475
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        if (Util.isWindows()) {
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6372808
            Util.callStatic("com.zipeg.win.Win32ShellFolderManager6372808.workaround", Util.NONE);
        }
    }

    public static FileChooser getInstance() {
        if (instance == null) {
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4711700
            int retry = 32;
            long time = System.currentTimeMillis();
            for (;;) {
                try {
                    FileChooser fc = new FileChooser() {

                        protected JDialog createDialog(Component parent) throws HeadlessException {
                            JDialog dlg = super.createDialog(parent);
                            if (Util.isMac() && Util.getJavaVersion() >= 1.6) {
                                dlg.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
                                dlg.getRootPane().putClientProperty("apple.awt.windowAlpha", "0.75");
                            }
                            return dlg;
                        }

                        public int showDialog(Component parent, String approveButtonText) {
                            try {
                                IdlingEventQueue.setInsideFileChooser(true);
                                return super.showDialog(parent, approveButtonText);
                            } finally {
                                // bug in apple.laf.AquaFileChooserUI.MacListSelectionModel.
                                // isSelectableInListIndex
                                IdlingEventQueue.setInsideFileChooser(false);
                            }
                        }

                    };
                    time = System.currentTimeMillis() - time;
                    Debug.traceln("new FileChooser() " + time + " ms ");
                    instance = fc;
                    break;
                } catch (final Throwable x) {
                    time = System.currentTimeMillis() - time;
                    Debug.trace("new JFileChooser() " + time + " ms " + x.getMessage());
                    --retry;
                    if (retry == 0) {
                        throw new Error(x);
                    }
                    else {
                        Util.sleep(100);
                    }
                }
            }

        }
        return instance;
    }

}
