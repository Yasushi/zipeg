package com.zipeg;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.*;

public final class About extends JOptionPane {

    private About() {
    }

    public static void showMessage() {
        JEditorPane info = new JEditorPane();
        info.setContentType("text/html");
        info.setEditable(false);
        info.setOpaque(false);
        Font font = new JLabel().getFont();
        String location = "resources/zipeg64x64.png";
        URL url = Resources.class.getResource(location);
        assert  url != null : location;
        info.setText(
                "<html><body>" +
                "<font face=\"" + font.getName() + "\">" +
                "<table width=320 height=128>" +
                 "<tr width=300>" +
                 "<td width=70>" +
                 "<center><img width=64 height=64 src=\"" + url + "\" ></center>" +
                 "</td>" +
                 "<td>" +
                    "<table width=256>" +
                     "<tr width=236>" +
                     "<td>" +
                      "<b>Zipeg</b> for " + 
                        (Util.isMac() ? "OS X" : "Windows") +"<br>" +
                      "version " + Util.getVersion() + "<br>" +
                      "Copyright&nbsp;&copy;&nbsp;2006-2007&nbsp;Leo&nbsp;Kuznetsov<br>" +
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

                    "</table>" +
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
        showMessageDialog(MainFrame.getTopFrame(), info, "About Zipeg", JOptionPane.PLAIN_MESSAGE);
    }

}
