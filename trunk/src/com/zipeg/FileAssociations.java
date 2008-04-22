package com.zipeg;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class FileAssociations extends JPanel {

    private final FileAssociationHandler handler;
    private JCheckBox[] cbx = new JCheckBox[FileAssociationHandler.ext2uti.length];
    private boolean canceled;
    private JButton ok;

    public FileAssociations() {
        if (Util.isMac()) {
            handler = DefaultRoleHandler.getInsance();
        } else {
            handler = Registry.getInstance();
        }
    }

    public boolean isAvailable() {
        return handler.isAvailable();
    }

    public void setHandled(long selected) {
        handler.setHandled(selected);
    }

    public long getHandled() {
        return handler.getHandled();
    }

    public void create(long selected, boolean apply) {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        int N = FileAssociationHandler.ext2uti.length;
        for (int i = 0; i < N; i++) {
            String ext = "&nbsp;." + FileAssociationHandler.ext2uti[i][0];
            String label = (String)FileAssociationHandler.ext2uti[i][1];
            String text =
                    "<html><body>" +
                    "   <table><tr><td width=50>" + ext + "</td><td>" + label + "</td></tr></table>" +
                    "</body></html>";
            cbx[i] = new JCheckBox(text) {
                public Insets getInsets() {
                    Insets i = super.getInsets();
                    if (Util.isWindows()) {
                        i = new Insets(0, 4, 0, 4);
                    }
                    return i;
                }
            };
            cbx[i].setBorder(null);
            cbx[i].setSelected(((1L << i) & selected) != 0);
            panel.add(cbx[i]);
        }
        JScrollPane sp = new JScrollPane(panel);
        sp.setOpaque(false);
        sp.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        sp.getViewport().setOpaque(false);
        JLabel title = new JLabel(apply ?
                "<html><body>&nbsp;Do you want to set <b>Zipeg</b> as default viewer for<br>" +
                "&nbsp;the file types (please check all that apply)?</body></html>" :
                "<html><body><b>Zipeg</b> is default viewer for these file types:</body></html>"
        );
        add(title, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(new JButton("All") {{
            addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < cbx.length; i++) {
                        cbx[i].setSelected(true);
                    }
                    if (ok != null) {
                        ok.requestFocus();
                    }
                }
            });
        }});
        buttons.add(new JButton("None") {{
            addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    for (int i = 0; i < cbx.length; i++) {
                        cbx[i].setSelected(false);
                    }
                    if (ok != null) {
                        ok.requestFocus();
                    }
                }
            });
        }});
        if (apply) {
            buttons.add(Box.createHorizontalStrut(96));
            ok = new JButton("OK") {{
                setDefaultCapable(true);
                addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        getDialog().dispose();
                    }
                });
            }};
            buttons.add(ok);
            buttons.add(Box.createHorizontalStrut(6));
            buttons.add(new JButton("Cancel") {{
                addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        canceled = true;
                        getDialog().dispose();
                    }
                });
            }});
        }
        add(buttons, BorderLayout.SOUTH);
        panel.revalidate();
    }

    public void setVisible(boolean b) {
        if (b) {
            getDialog().getRootPane().setDefaultButton(ok);
        }
        super.setVisible(b);
    }

    JDialog getDialog() {
        Container parent = getParent();
        while (parent != null && !(parent instanceof JDialog)) {
            parent = parent.getParent();
        }
        return (JDialog)parent;
    }

    public Insets getInsets() {
        return new Insets(4, 8, 4, 8);
    }

    public Dimension getPreferredSize() {
        return new Dimension(350, 280);
    }

    public boolean isCanceled() {
        return canceled;
    }

    public long getSelected() {
        long result = 0;
        for (int i = 0; i < cbx.length; i++) {
            if (cbx[i].isSelected()) {
                result |= (1L << i);
            }
        }
        return result;
    }

    public void askHandleAll() {
        if (!isHandleNone()) {
            askHandleAll(-1L);
        }
    }

    public boolean isHandleNone() {
        return Presets.getBoolean("FileAssociations:willinglyNone", false);
    }

    private void askHandleAll(long selected) {
        if (handler.isAvailable()) {
            JDialog dlg = Util.createDocumentModalDialog(MainFrame.getTopFrame());
            dlg.setTitle("Zipeg Prompt: Set File Type Associations");
            FileAssociations ui = new FileAssociations();
            ui.create(selected, true);
            dlg.setContentPane(ui);
            if (Util.isMac() && Util.getJavaVersion() >= 1.6) {
                dlg.getRootPane().putClientProperty("apple.awt.documentModalSheet", "true");
            }
            dlg.pack();
            dlg.setResizable(false);
            dlg.setVisible(true);
            if (!ui.isCanceled()) {
                if (ui.getSelected() == 0) {
                    Presets.putBoolean("FileAssociations:willinglyNone", true);
                }
                handler.setHandled(ui.getSelected());
            } else {
                Presets.putBoolean("FileAssociations:willinglyNone", true);
            }
            Presets.sync();
        }
    }

}
