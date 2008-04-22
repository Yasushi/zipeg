package com.zipeg;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.Charset;
import java.util.*;

public final class Settings extends JTabbedPane {

    private boolean canceled;

    private Settings() {
        BasePanel general = new General();
        addTab(" General ", null, general, "General Application Settings");
        setMnemonicAt(0, 'G');
        BasePanel files = new FileSettings();
        addTab(" Files ", null, files,
               (Util.isMac() ? "Archive Open With Role Handler" : "File Type Associations:") +
               " how to setup file types that will be handled by Zipeg");
        setMnemonicAt(1, 'F');
        BasePanel advanced = new Advanced();
        addTab(" Advanced ", null, advanced, "Options For Advanced Users");
        setMnemonicAt(2, 'A');
        int i = Presets.getInt("Settings.tab", 0);
        if (0 <= i && i <= 2) {
            setSelectedIndex(i);
        }
        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = getSelectedIndex();
                if (0 <= i && i <= 2) {
                    Presets.putInt("Settings.tab", i);
                    Presets.sync();
                }
            }
        });
    }

    public static void showPreferences() {
        long oldFlags = Flags.getFlags();
        JDialog dlg = Util.createDocumentModalDialog(MainFrame.getTopFrame());
        Settings s = new Settings();
        dlg.setTitle(Util.isMac() ? "Zipeg Preferences" : "Zipeg Options");
        dlg.getContentPane().setLayout(new BorderLayout());
        dlg.getContentPane().add(s, BorderLayout.CENTER);
        dlg.getContentPane().add(s.createButtons(dlg), BorderLayout.SOUTH);
        dlg.pack();
        dlg.setResizable(false);
        dlg.setVisible(true); // nested dispatch loop
        if (!s.canceled) {
            for (int i = 0; i < s.getTabCount(); i++) {
                BasePanel panel = (BasePanel)s.getComponentAt(i);
                panel.saveSettings();
            }
            if (oldFlags != Flags.getFlags()) {
                Actions.postEvent("settingsChanged",
                                  new Object[]{new Long(oldFlags),
                                               new Long(Flags.getFlags())});
            }
            Presets.sync();
        }
        dlg.dispose();
    }

    private JPanel createButtons(final JDialog dlg) {
        JPanel buttons = new JPanel();
        JButton ok = new JButton("Apply");
        ok.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (checkSettings()) {
                    dlg.setVisible(false);
                }
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) { canceled = true; dlg.setVisible(false); }
        });
        ok.setDefaultCapable(true);
        dlg.getRootPane().setDefaultButton(ok);
        buttons.add(ok);
        buttons.add(cancel);
        return buttons;
    }

    private boolean checkSettings() {
        boolean check = true;
        for (int i = 0; i < getTabCount(); i++) {
            BasePanel panel = (BasePanel)getComponentAt(i);
            check = panel.checkSettings() && check;
        }
        return check;
    }

    private abstract class BasePanel extends JPanel {

        public Insets getInsets() {
            return new Insets(8, 8, 8, 8);
        }

        public Dimension getPreferredSize() {
            return new Dimension(500, 320);
        }

        protected abstract boolean checkSettings();
        protected abstract void saveSettings();
    }

    private final class General extends BasePanel {

        private JCheckBox promptAll;
        private JRadioButton sel;
        private JRadioButton all;
        private JRadioButton ask;
        JCheckBox playSounds;
        JCheckBox metal;
        JCheckBox dirsFirst;
        JCheckBox caseSensitive;

        General() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            if (!Flags.getFlag(Flags.PROMPT_CREATE_FOLDERS) ||
                !Flags.getFlag(Flags.PROMPT_EXTRACT_SELECTED)) {
                promptAll = new JCheckBox("Show All Prompts") {
                    {
                        addActionListener(new ActionListener(){
                            public void actionPerformed(ActionEvent e) {
                                if (isSelected()) {
                                    setEnabled(false);
                                }
                            }
                        });
                    }
                };
                promptAll.setSelected(false);
                add(promptAll);
            }
            dirsFirst = new JCheckBox("Group folders on top");
            dirsFirst.setSelected(Flags.getFlag(Flags.DIRECTORIES_FIRST));
            dirsFirst.setMnemonic(KeyEvent.VK_T);
            add(dirsFirst);
            add(Box.createVerticalStrut(5));
            caseSensitive = new JCheckBox("Case sensitive");
            caseSensitive.setSelected(Flags.getFlag(Flags.CASE_SENSITIVE));
            caseSensitive.setMnemonic(KeyEvent.VK_C);
            add(caseSensitive);
            add(Box.createVerticalStrut(10));

            add(new JLabel("What to Extract:"));
            sel = new JRadioButton("Selection");
            all = new JRadioButton("Whole archive");
            ask = new JRadioButton("Ask");
            sel.setMnemonic(KeyEvent.VK_S);
            all.setMnemonic(KeyEvent.VK_W);
            ask.setMnemonic('k');
            ButtonGroup extract = new ButtonGroup();
            extract.add(sel);
            extract.add(all);
            extract.add(ask);
            if (Flags.getFlag(Flags.EXTRACT_SELECTED)) {
                extract.setSelected(sel.getModel(), true);
            } else if (Flags.getFlag(Flags.EXTRACT_WHOLE)) {
                extract.setSelected(all.getModel(), true);
            } else if (Flags.getFlag(Flags.EXTRACT_ASK)) {
                extract.setSelected(ask.getModel(), true);
            } else {
                System.err.println("WARNING: invalid flags 0x" +
                                   Long.toHexString(Flags.getFlags()));
                extract.setSelected(sel.getModel(), true);
            }
            add(sel);
            add(all);
            add(ask);
            add(Box.createVerticalStrut(10));

            playSounds = new JCheckBox("Play Sounds");
            playSounds.setSelected(Flags.getFlag(Flags.PLAY_SOUNDS));
            playSounds.setMnemonic('P');
            add(playSounds);

            metal = new JCheckBox("Metal Look & Feel (effective on quit/restart)");
            metal.setSelected(Flags.getFlag(Flags.METAL));
            metal.setMnemonic('M');
            if (Util.isMac()) {
                add(metal);
            }
        }

        protected boolean checkSettings() {
            return true;
        }

        protected final void saveSettings() {
            if (promptAll != null && promptAll.isSelected()) {
                Flags.addFlag(Flags.PROMPT_CREATE_FOLDERS|
                              Flags.PROMPT_EXTRACT_SELECTED);
                promptAll = null;
            }
            long flag;
            if (sel.isSelected()) {
                flag = Flags.EXTRACT_SELECTED;
            } else if (all.isSelected()) {
                flag = Flags.EXTRACT_WHOLE;
            } else if (ask.isSelected()) {
                flag = Flags.EXTRACT_ASK;
            } else {
                Debug.traceln("WARNING: one of the buttons has to be selected");
                flag = Flags.EXTRACT_SELECTED;
            }
            Flags.removeFlag(Flags.EXTRACT_SELECTED|Flags.EXTRACT_WHOLE|Flags.EXTRACT_ASK);
            Flags.addFlag(flag);
            if (playSounds.isSelected()) {
                Flags.addFlag(Flags.PLAY_SOUNDS);
            } else {
                Flags.removeFlag(Flags.PLAY_SOUNDS);
            }
            if (metal.isSelected()) {
                Flags.addFlag(Flags.METAL);
            } else {
                Flags.removeFlag(Flags.METAL);
            }
            if (dirsFirst.isSelected()) {
                Flags.addFlag(Flags.DIRECTORIES_FIRST);
            } else {
                Flags.removeFlag(Flags.DIRECTORIES_FIRST);
            }
            if (caseSensitive.isSelected()) {
                Flags.addFlag(Flags.CASE_SENSITIVE);
            } else {
                Flags.removeFlag(Flags.CASE_SENSITIVE);
            }
        }
    }

    private final class FileSettings extends BasePanel {

        private JLabel label = new JLabel();
        private FileAssociations fa = new FileAssociations();

        FileSettings() {
            if (fa.isAvailable()) {
                fa.create(fa.getHandled(), false);
                setLayout(new BorderLayout());
                add(fa, BorderLayout.CENTER);
            } else {
                label.setText("<html><br>On Mac OS X prio 10.4 use <b>Finder</b> " +
                        "to associate Zipeg with archives.</html>");
                setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                add(label);
            }
        }

        protected boolean checkSettings() {
            return true;
        }

        protected final void saveSettings() {
            if (fa != null && fa.isAvailable() && fa.getHandled() != fa.getSelected()) {
                fa.setHandled(fa.getSelected());
            }
        }

    }

    private final class Advanced extends BasePanel {

        private JRadioButton docs;
        private JRadioButton arch;
        private JRadioButton last;
        private JRadioButton desk;
        private JCheckBox appendArchiveName;
        private JCheckBox autoDetectEncoding;
        private JComboBox fileNameCodePage;
        private JCheckBox closeAfterExtract;
        private JCheckBox autoOpenNested;


        Advanced() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel("Destination to extract files:"));
            String mydocs = Util.isMac() ? "<html><u>D</u>ocuments folder (<b>" +
                                           Util.getDocuments() + "</b>)</html>" :
                                           "<html><b>My <u>D</u>ocuments</b> folder</html>";
            docs = new JRadioButton(mydocs);
            arch = new JRadioButton("Next to archive file");
            last = new JRadioButton("Last used destination");
            desk = new JRadioButton("Desktop");
            docs.setMnemonic(KeyEvent.VK_D);
            arch.setMnemonic(KeyEvent.VK_T);
            last.setMnemonic(KeyEvent.VK_L);
            desk.setMnemonic(KeyEvent.VK_K);
            ButtonGroup location = new ButtonGroup();
            location.add(docs);
            location.add(arch);
            location.add(last);
            location.add(desk);
            appendArchiveName = new JCheckBox("Append archive name to the destination");
            appendArchiveName.setSelected(Flags.getFlag(Flags.APPEND_ARCHIVE_NAME));
            arch.addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent e) {
                    if (arch.isSelected()) {
                        appendArchiveName.setSelected(true);
                    }
                    appendArchiveName.setEnabled(!arch.isSelected());
                }
            });
            if (Flags.getFlag(Flags.LOCATION_ARCHIVE)) {
                location.setSelected(arch.getModel(), true);
            } else if (Flags.getFlag(Flags.LOCATION_DOCUMENTS)) {
                location.setSelected(docs.getModel(), true);
            } else if (Flags.getFlag(Flags.LOCATION_LAST)) {
                location.setSelected(last.getModel(), true);
            } else if (Flags.getFlag(Flags.LOCATION_DESKTOP)) {
                location.setSelected(desk.getModel(), true);
            } else {
                Debug.traceln("WARNING: invalid flags 0x" +
                                   Long.toHexString(Flags.getFlags()));
                location.setSelected(arch.getModel(), true);
            }
            add(arch);
            add(last);
            add(docs);
            add(desk);
            add(Box.createVerticalStrut(5));
            appendArchiveName.setMnemonic(KeyEvent.VK_R);
            add(appendArchiveName);
            add(Box.createVerticalStrut(5));

            autoOpenNested = new JCheckBox("Automatically open nested archives");
            autoOpenNested.setMnemonic('m');
            autoOpenNested.setSelected(!Flags.getFlag(Flags.DONT_OPEN_NESTED));
            add(autoOpenNested);
            add(Box.createVerticalStrut(15));

            boolean auto = !Flags.getFlag(Flags.FORCE_ENCODING);
            autoDetectEncoding = new JCheckBox("Autodetect filenames encoding");
            autoDetectEncoding.setSelected(auto);
            autoDetectEncoding.setMnemonic('o');
            add(autoDetectEncoding);
            // filename encodings:
            ArrayList charsets = new ArrayList();
            SortedMap map = Charset.availableCharsets();
            for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                Charset cs = (Charset)map.get(name);
                charsets.add(cs.displayName(Locale.getDefault()));
            }
            Object[] names = charsets.toArray(new String[charsets.size()]);
            fileNameCodePage = new JComboBox(names) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            int ix = charsets.indexOf(Presets.get("encoding", "UTF-8"));
            if (ix < 0) {
                ix = charsets.indexOf("UTF-8");
            }
            fileNameCodePage.setSelectedIndex(ix);
            fileNameCodePage.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel panel = new JPanel() {
                public Dimension getMaximumSize() { return getPreferredSize(); }
                public Insets getInsets() { return new Insets(0, 0, 0, 0); }
            };
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
            panel.add(Box.createHorizontalStrut(10));
            panel.add(new JLabel("  use: "));
            panel.add(fileNameCodePage);
            panel.add(new JLabel(" when undetected. "));
/*          looks bad on Macintosh
            panel.add(Box.createHorizontalStrut(10));
            JButton reset = new JButton("Restore");
            reset.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    fileNameCodePage.setSelectedItem("UTF-8");
                    autoDetectEncoding.setSelected(true);
                }
            });
            panel.add(reset);
*/
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(panel);
            JEditorPane info = new JEditorPane() {
                public Dimension getMaximumSize() { return getPreferredSize(); }
                public Insets getInsets() { return new Insets(0, 0, 0, 0); }
            };
            info.setContentType("text/html");
            info.setEditable(false);
            info.setOpaque(false);
            Font font = new JLabel().getFont();
            info.setText("<html>" +
                    "<font face=\"" + font.getName() + "\" size=\"-2\">" +
                    "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Using wrong encoding may garble filenames.<br>" +
                    "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;see: <a href=\"http://en.wikipedia.org/wiki/Code_page\">" +
                    "http://en.wikipedia.org/wiki/code_page</a> for details." +
                    "</font></html>");
            info.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        Util.openUrl(e.getURL().toString());
                    }
                }
            });
            add(info);

            add(Box.createVerticalStrut(15));
            closeAfterExtract = new JCheckBox(Util.isMac() ? "Quit after extract" :
                                                             "Exit after extract");
            closeAfterExtract.setMnemonic(Util.isMac() ? 'Q' : 'x');
            closeAfterExtract.setSelected(Flags.getFlag(Flags.CLOSE_AFTER_EXTRACT));
            add(closeAfterExtract);
        }

        protected boolean checkSettings() {
            if (Flags.getFlag(Flags.NO_APPEND_OK)) {
                return true;
            }
            if (!appendArchiveName.isSelected() && (desk.isSelected() || docs.isSelected())) {
                Settings.this.setSelectedComponent(this);
                String where = (desk.isSelected() ? Util.getDesktop() : Util.getDocuments()).getName();
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.add(new JLabel(
                        "<html><body>Extracting to <b>" + where + "</b> without appending archive name<br>" +
                        "may result in <u>way too many</u> files in your <b>" + where + "</b> folder.<br><br>" +
                        "Do you want to turn on Append Archive Name option now?<br>&nbsp;</body></html>"));
                JCheckBox cbx = new JCheckBox("Do not show this warning again.");
                panel.add(cbx);
                int i = JOptionPane.showConfirmDialog(MainFrame.getTopFrame(), panel,
                    "Zipeg Prompt: Extracting Too Many Files",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (cbx.isSelected()) {
                    Flags.addFlag(Flags.NO_APPEND_OK);
                }
                if (i == JOptionPane.YES_OPTION) {
                    appendArchiveName.setSelected(true);
                    return false;
                }
                return true;
            } else {
                return true;
            }
        }

        protected final void saveSettings() {
            long flag;
            if (arch.isSelected()) {
                flag = Flags.LOCATION_ARCHIVE;
            } else if (docs.isSelected()) {
                flag = Flags.LOCATION_DOCUMENTS;
            } else if (last.isSelected()) {
                flag = Flags.LOCATION_LAST;
            } else if (desk.isSelected()) {
                flag = Flags.LOCATION_DESKTOP;
            } else {
                Debug.traceln("WARNING: one of the buttons has to be selected");
                flag = Flags.LOCATION_DOCUMENTS;
            }
            Flags.removeFlag(Flags.LOCATION_ARCHIVE|Flags.LOCATION_DOCUMENTS|
                             Flags.LOCATION_LAST|Flags.LOCATION_DESKTOP);
            Flags.addFlag(flag);
            if (closeAfterExtract.isSelected()) {
                Flags.addFlag(Flags.CLOSE_AFTER_EXTRACT);
            } else {
                Flags.removeFlag(Flags.CLOSE_AFTER_EXTRACT);
            }
            if (appendArchiveName.isSelected()) {
                Flags.addFlag(Flags.APPEND_ARCHIVE_NAME);
            } else {
                Flags.removeFlag(Flags.APPEND_ARCHIVE_NAME);
            }
            boolean reopen = false;
            if (autoOpenNested.isSelected()) {
                Flags.removeFlag(Flags.DONT_OPEN_NESTED);
                Archive a = Zipeg.getArchive();
                if (a != null && Util.isCompositeArchive(a.getName())) {
                    reopen = true;
                }
            } else {
                Flags.addFlag(Flags.DONT_OPEN_NESTED);
            }
            String encoding = (String)fileNameCodePage.getSelectedItem();
            if (!Util.equals(Presets.get("encoding", "UTF-8"), encoding)) {
                Presets.put("encoding", encoding);
                reopen = true;
            }
            boolean force = !autoDetectEncoding.isSelected();
            reopen = reopen || (force != Flags.getFlag(Flags.FORCE_ENCODING));
            if (force) {
                Flags.addFlag(Flags.FORCE_ENCODING);
            } else {
                Flags.removeFlag(Flags.FORCE_ENCODING);
            }
            if (reopen) {
                Actions.postEvent("commandReopenArchive");
            }
        }

    }

}
