package com.zipeg;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.io.*;
import java.awt.*;

public class License {

    private static final Dimension SIZE = new Dimension(490, 360);

    public static void showLicence(boolean accept) {
        String location = "resources/license.html";
        try {
            InputStream i = License.class.getResource(location).openStream();
            StringWriter w = new StringWriter(i.available());
            int c;
            for (;;) {
                c = i.read();
                if (c < 0) break;
                w.write(c);
            }
            JEditorPane pane = new JEditorPane() {
                public void addNotify() {
                    super.addNotify();
                    EventQueue.invokeLater(new Runnable(){
                        public void run() { scrollRectToVisible(new Rectangle(0, 0, 1, 1)); }
                    });
                }
            };
            pane.setContentType("text/html");
            pane.setText(w.toString());
            pane.setOpaque(false);
            JScrollPane sp = new JScrollPane(pane) {
                public Dimension getMaximumSize() { return SIZE; }
            };
            if (Util.isMac()) {
                sp.setBorder(null);
            }
            sp.setSize(SIZE);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            pane.setEditable(false);
            pane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        Util.openUrl(e.getURL().toString());
                    }
                }
            });
            Object[] options = accept ? new Object[]{"  I Accept  ", " I Decline "} :
                                        new Object[]{ " OK " };
            int r = JOptionPane.showOptionDialog(MainFrame.getTopFrame(), sp,
                "Zipeg: License Agreement", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[options.length - 1]);
            if (r != 0 && accept) {
                System.exit(1);
            }
        } catch (IOException e) {
            throw new Error("error reading: " + location, e);
        }
    }

}
