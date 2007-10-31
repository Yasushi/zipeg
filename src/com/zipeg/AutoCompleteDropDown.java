package com.zipeg;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
import java.io.File;

/**
 * Keyboard input (ENTER) is broken in drop down ComboBoxes
 * on Mac OS X in 1.4.2 and 1.5. Only fixed in 1.6
 * Thus full reimplementation of JComboBox...
 */

public class AutoCompleteDropDown extends JTextField  {

    private final AutoCompleterFilter filter = new AutoCompleterFilter();
    private JList popup;
    private JScrollPane sp;
    private KeyListener keyListener = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                showPopup();
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_TAB) {
                String text = getText();
                if (new File(text).isDirectory() && !text.endsWith(File.separator)) {
                    text += File.separator;
                    setText(text);
                    select(text.length(), text.length());
                }
                else if (new File(text).exists()) {
                    select(text.length(), text.length());
                }
            }
        }
    };

    AutoCompleteDropDown() {
        setFilter(filter);
    }

    protected void setCoboBoxModel(MutableComboBoxModel model) {
        popup = new JList(model) {
            public Insets getInsets() {
                return new Insets(0, 2, 0, 2);
            }
        };
        popup.setFont(getFont());
        popup.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        popup.setFocusable(true);
        popup.setOpaque(false);
        Color bg = popup.getBackground();
        popup.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 192));
        sp = new JScrollPane(popup);
        sp.setFocusable(false);
        sp.setVisible(false);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        popup.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    hidePopup(true);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePopup(false);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && popup.getSelectedIndex() == 0) {
                    hidePopup(false);
                }
            }

            public void keyTyped(KeyEvent event) {
                char ch = event.getKeyChar();
                if (ch >= KeyEvent.VK_SPACE || ch == KeyEvent.VK_BACK_SPACE) {
                    String text = getNonSelectedText();
                    if (ch == KeyEvent.VK_BACK_SPACE) {
                        if (text.length() > 0)
                        text = text.substring(0, text.length() - 1);
                    } else {
                        text += ch;
                    }
                    hidePopup(false);
                    setText(text);
                }
            }
        });
    }

    public void updateUI() {
        super.updateUI();
        setFilter(filter);
    }

    public void setText(String text) {
        setFilter(null);
        super.setText(text);
        setFilter(filter);
    }

    public void addNotify() {
        super.addNotify();
        addKeyListener(keyListener);
    }

    public void removeNotify() {
        removeKeyListener(keyListener);
        super.removeNotify();
    }

    private void hidePopup(boolean enter) {
        requestFocus();
        int ix = popup.getSelectedIndex();
        if (enter && 0 <= ix && ix <= popup.getModel().getSize()) {
            Object selected = popup.getModel().getElementAt(ix);
            setText(selected.toString());
        }
    }

    private void showPopup() {
        int n = popup.getModel().getSize();
        if (popup.getModel().getSize() == 0) {
            return;
        }
        Rectangle cell = popup.getCellBounds(0, 0);
        int h = Math.min(cell.height * n, MainFrame.getInstance().getHeight() / 2);
        Insets insets = sp.getBorder().getBorderInsets(sp);
        h = (h / cell.height) * cell.height + insets.top + insets.bottom;
        sp.setSize(new Dimension(getWidth() + 4, h));
        final JComponent gp = (JComponent)MainFrame.getInstance().getGlassPane();
        gp.add(sp);
        gp.setVisible(true);
        sp.setVisible(true);
        sp.revalidate();
        sp.repaint();
        sp.setFocusable(true);
        popup.requestFocus();
        int ix = getClosestMatch(getText());
        popup.setSelectedIndex(ix);
        popup.ensureIndexIsVisible(ix);
        Point pt = SwingUtilities.convertPoint(getParent(), getX() - 2, getY() + getHeight(), gp);
        sp.setLocation(pt);
        popup.addFocusListener(new FocusAdapter(){
            public void focusLost(FocusEvent e) {
                popup.removeFocusListener(this);
                sp.setVisible(false);
                gp.remove(sp);
                gp.setVisible(false);
            }
        });
    }

    private int getClosestMatch(String text) {
        int ix = -1;
        for (int i = 0; i < popup.getModel().getSize(); i++) {
            Object e = popup.getModel().getElementAt(i);
            if (e.toString().equals(text)) {
                ix = i;
                break;
            }
        }
        if (ix < 0) {
            for (int i = 0; i < popup.getModel().getSize(); i++) {
                Object e = popup.getModel().getElementAt(i);
                if (e.toString().startsWith(text)) {
                    ix = i;
                    break;
                }
            }
        }
        if (ix < 0) {
            ix = 0;
            for (int i = 0; i < popup.getModel().getSize(); i++) {
                Object e = popup.getModel().getElementAt(i);
                if (e.toString().toLowerCase().startsWith(text.toLowerCase())) {
                    ix = i;
                    break;
                }
            }
        }
        return ix;
    }

    private String getNonSelectedText() {
        String text = getText();
        int s = getSelectionStart();
        int e = getSelectionEnd();
        if (0 <= s && s <= e) {
            if (e > text.length()) e = text.length();
            if (s > text.length()) s = text.length();
            if (s < text.length()) {
                text = text.substring(0, s) +
                      (e >= text.length() ? "" : text.substring(e));

            }
        }
        return text;
    }

    private void setFilter(DocumentFilter f) {
        AbstractDocument doc = (AbstractDocument)getDocument();
        doc.setDocumentFilter(f);
    }

    private class AutoCompleterFilter extends DocumentFilter {

        public void replace(DocumentFilter.FilterBypass filterBypass, int offset, int length,
                            String string, AttributeSet attributeSet)
                throws BadLocationException {
            super.replace(filterBypass, offset, length, string, attributeSet);
            String text = getText();
            Document doc = filterBypass.getDocument();
            int ix = -1;
            for (int k = 0; k < popup.getModel().getSize(); k++) {
                String item = popup.getModel().getElementAt(k).toString();
                if (item.equalsIgnoreCase(text)) {
                    ix = k;
                    filterBypass.replace(0, text.length(), item, attributeSet);
                    break;
                }
                if (item.length() >= text.length() &&
                    item.toLowerCase().startsWith(text.toLowerCase())) {
                    filterBypass.replace(0, text.length(), item, attributeSet);
                    select(text.length(), doc.getLength());
                    ix = k;
                    break;
                }
            }
            if (ix >= 0) {
                if (ix < popup.getModel().getSize()) {
                    popup.setSelectedIndex(ix);
                }
            }
        }

    }

}
