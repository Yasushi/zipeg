package com.zipeg;

import java.util.*;
import java.io.File;
import java.awt.*;
import javax.swing.*;

public class DestinationComboBox extends AutoCompleteDropDown {

    private ArrayList files = new ArrayList();
    private String parent = null;

    DestinationComboBox() {
        setCoboBoxModel(new Model());
    }

    private ArrayList getFiles() {
        String text = getText();
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if (0 <= s && s <= e && e == text.length()) {
            text = text.substring(0, s);
        }
        File file = new File(text);
        String n = file.isDirectory() ? text : file.getParent();
        File dir = n == null ? null : new File(n);
        while (dir != null && !dir.isDirectory()) {
            String p = dir.getParent();
            dir = p == null ? null : new File(p);
        }
        if (dir == null) {
            dir = new File(File.separator); // root
        }
        if (Util.getCanonicalPath(dir).equals(parent) && files.size() > 0) {
            return files;
        }
        files.clear();
        File[] list = dir.listFiles();
        for (int i = 0; list != null && i < list.length; i++) {
            if (list[i].isDirectory() && !list[i].isHidden() &&
               !list[i].getName().startsWith(".")) {
                files.add(Util.getCanonicalPath(list[i]));
            }
        }
        parent = Util.getCanonicalPath(dir);
        return files;
    }

    public Dimension getMaximumSize() {
        Dimension p = getPreferredSize();
        Dimension x = super.getMaximumSize();
        return new Dimension(x.width, p.height);
    }

    private final class Model extends DefaultComboBoxModel {

        public int getSize() {
            return getFiles().size();
        }

        public Object getElementAt(int index) {
            return getFiles().get(index);
        }

    }

}
