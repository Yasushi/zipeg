package com.zipeg;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;

public final class About extends JOptionPane {

    private About() {
    }

    public static void showMessage() {
        JEditorPane info = new JEditorPane();
        info.setContentType("text/html");
        info.setEditable(false);
        info.setOpaque(false);
        Font font = new JLabel().getFont();
        ImageIcon icon = Resources.getImageIcon("zipeg64x64");
        info.setText(
                "<html><body>" +
                "<font face=\"" + font.getName() + "\">" +
                "<table width=256>" +
                 "<tr width=236>" +
                 "<td>" +
                  "<b>Zipeg</b> for " +
                    (Util.isMac() ? "OS X" : "Windows") +"<br>" +
                  "version " + Util.getVersion() + "<br>" +
                  "Copyright&nbsp;&copy;&nbsp;2006-2008&nbsp;Leo&nbsp;Kuznetsov<br>" +
                 "<a href=\"http://www.zipeg.com\">www.zipeg.com</a>" +
                 "</td>" +
                 "</tr>" +

                 "<tr width=236>" +
                 "<td>" +
                  "<font=\"" + font.getName() + "\" size=\"-1\" >" +
                  "<b>p7zip</b>&nbsp;-&nbsp;7zip&nbsp;plugin&nbsp;version&nbsp;4.43<br>" +
                  "Copyright&nbsp;&copy;&nbsp;2006&nbsp;Igor&nbsp;Pavlov<br>" +
                  "<a href=\"http://p7zip.sourceforge.net\">p7zip.sourceforge.net</a>" +
                  "</font>" +
                 "</td>" +
                 "</tr>" +

                 "<tr width=236>" +
                 "<td>" +
                  "<font=\"" + font.getName() + "\" size=\"-1\" >" +
                  "<b>juniversalchardet</b>&nbsp;version&nbsp;1.0.2<br>" +
                  "Shy&nbsp;Shalom,&nbsp;Kohei&nbsp;TAKETA<br>" +
                  "<a href=\"http://code.google.com/p/juniversalchardet\">http://code.google.com/p/juniversalchardet</a>" +
                  "</font>" +
                 "</td>" +
                 "</tr>" +

                 "<tr width=236>" +
                 "<td>" +
                  "<font=\"" + font.getName() + "\" size=\"-1\" >" +
                  "<b>The Oxygen Icon Theme</b><br>" +
                  "<a href=\"http://oxygen-icons.org\">http://oxygen-icons.org</a>" +
                  "</font>" +
                 "</td>" +
                 "</tr>" +

                "</table>" +
                "</font></body></html>");
        info.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Util.openUrl(e.getURL().toString());

                }
            }
        });
        info.revalidate();
        info.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0 &&
                    (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                    throw new Error("crash reporting test - please ignore");
                }
            }
        });
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(info, BorderLayout.CENTER);
        JLabel label = new JLabel(icon);
        label.setOpaque(false);
        panel.add(label, BorderLayout.WEST);
        panel.revalidate();
        showMessageDialog(MainFrame.getTopFrame(), panel, "About Zipeg", JOptionPane.PLAIN_MESSAGE);
    }

}
