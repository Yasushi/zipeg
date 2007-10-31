package com.zipeg;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;

public final class CrashLog {

    private static String name;
    private static String email;
    private static String extra;

    private CrashLog() {
    }

    public static void report(String message, String log) {
        try {
            File file = new File(log);
            InputStream input = new FileInputStream(file);
            int len = (int)file.length();
            byte[] bytes = new byte[len];
            int n = input.read(bytes, 0, len);
            assert n == len;
            Util.close(input);
            String body = new String(bytes);
            send(message, body);
            file.delete();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void send(String subject, String body) {
        assert IdlingEventQueue.isDispatchThread();
        if (!askNameAndEmail(body)) {
            return;
        }
        Map headers = new HashMap();
        char[] c = new char[15];
        c[1] = 'r';
        c[0] = 'c';
        c[2] = 'a';
        c[3] = 's';
        c[5] = '@';
        c[4] = 'h';
        c[6] = 'z';
        c[7] = 'i';
        c[8] = 'p';
        c[9] = 'e';
        c[10] = 'g';
        c[12] = 'c';
        c[11] = '.';
        c[13] = 'o';
        c[14] = 'm';
        headers.put("subject", subject);
        headers.put("email", new String(c));
        headers.put("name", new String(c));
        if (email == null) {
            email = "";
        }
        if (name == null || name.length() == 0) {
            name = email;
        }
        if (!name.equals(email) && name.length() > 0) {
            email = name + " <" + email + ">";
        }
        if (email.length() > 0) {
            body = "> From: " + email + "\n\n" + body;
        }
        headers.put("body", body + "\n" + extra);
        try {
            char[] m = new char[8];
            m[1] ='a';
            m[0] ='m';
            m[4] ='.';
            m[2] ='i';
            m[6] ='h';
            m[3] ='l';
            m[5] ='p';
            m[7] ='p';
            ByteArrayOutputStream reply = new ByteArrayOutputStream();
            Util.httpGetPost(true, "http://www.zipeg.com/" + new String(m), headers, null, reply);
            System.err.println(reply.toString());
        } catch (IOException e) {
            Util.sendMail(new String(c), subject, body);
        }
    }

    private static boolean askNameAndEmail(String body) {
        JTextField nameField = new TextField();
        JPanel nameBox = new JPanel();
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.X_AXIS));
        nameBox.add(new JLabel("Your name: "));
        nameBox.add(nameField);
        nameBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField emailField = new TextField();
        JPanel emailBox = new JPanel();
        emailBox.setLayout(new BoxLayout(emailBox, BoxLayout.X_AXIS));
        emailBox.add(new JLabel("Your email:  "));
        emailBox.add(emailField);
        emailBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("<html>Zipeg experienced technical difficulties and cannot continue.<br>" +
                "Sending this report will help to diagnose the problem.</html>");
        Dimension size = new Dimension(label.getPreferredSize().width + 16, label.getPreferredSize().height * 4);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("<html>Your name and email are <b>optional</b>, " +
                "but providing them will help<br>Zipeg support to help you.</html>"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(nameBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(emailBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("<html>If you can, describe how this problem could be reproduced:</html>"));
        panel.add(Box.createVerticalStrut(5));

        JEditorPane text = new JEditorPane();
        JScrollPane sp1 = new JScrollPane(text);
        sp1.setPreferredSize(size);
        sp1.setMaximumSize(size);
        sp1.setMinimumSize(size);
        sp1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp1.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sp1);

        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setText("<html><body><pre>" + body + "</pre><body></html>");
        pane.setOpaque(false);
        JScrollPane sp2 = new JScrollPane(pane);
        sp2.setPreferredSize(size);
        sp2.setMaximumSize(size);
        sp2.setMinimumSize(size);
        sp2.setOpaque(false);
        sp2.getViewport().setOpaque(false);
        sp2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setEditable(false);
        sp2.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Crash log details:"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(sp2);

        panel.add(Box.createVerticalStrut(10));
        Object[] options = new Object[]{"  Send  ", " Cancel "};
        int r = JOptionPane.showOptionDialog(null, panel,
            "Zipeg: Crash Report", JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        email = emailField.getText();
        name = nameField.getText();
        extra = text.getText();
        return r == 0;
    }

    private static class TextField extends JTextField {
        TextField() {
            super(15);
        }

        public Dimension getPreferredSize() {
            Dimension s = super.getPreferredSize();
            return new Dimension(200, s.height);
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

}
