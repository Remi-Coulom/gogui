// GuiMenu.java

package net.sf.gogui.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

/** Menu that checks for unique mnemonics.
    Prints a warning to System.err if a mnemonic is used twice in the same
    submenu. */
public class GuiMenu
    extends JMenu
{
    public GuiMenu(String label)
    {
        // Parse label that has the mnemonic marked with a preceding'&amp;'
        // (like in Qt)
        int pos = label.indexOf('&');
        label = label.replace("&", "");
        setText(label);
        if (pos >= 0 && pos < label.length())
        {
            String mnemomic = label.substring(pos, pos + 1).toUpperCase();
            KeyStroke keyStroke = KeyStroke.getKeyStroke(mnemomic);
            int code = keyStroke.getKeyCode();
            setMnemonic(code);
            setDisplayedMnemonicIndex(pos);
        }
    }

    public JMenuItem add(JMenuItem item)
    {
        super.add(item);
        item.setToolTipText(null);
        item.setIcon(null);
        int mnemonic = item.getMnemonic();
        if (mnemonic > 0)
        {
            if (m_mnemonics.contains(mnemonic))
                System.err.println("Warning: duplicate mnemonic item: "
                                   + item.getText());
            m_mnemonics.add(mnemonic);
        }
        return item;
    }

    public JMenuItem add(GuiAction action)
    {
        JMenuItem item = new JMenuItem(action);
        item.setMnemonic(action.getMenuMnemonic());
        item.setDisplayedMnemonicIndex(action.getMenuDisplayedMnemonicIndex());
        return add(item);
    }

    public JMenuItem addRadioItem(ButtonGroup group, GuiAction action)
    {
        JMenuItem item = new GuiRadioButtonMenuItem(action);
        item.setIcon(null);
        group.add(item);
        return add(item);
    }

    public JMenuItem addCheckBoxItem(GuiAction action)
    {
        return add(new GuiCheckBoxMenuItem(action));
    }

    private final ArrayList<Integer> m_mnemonics = new ArrayList<Integer>();
}

/** Radio menu item with additional "selected" action property. */
class GuiRadioButtonMenuItem
    extends JRadioButtonMenuItem
{
    public GuiRadioButtonMenuItem(GuiAction action)
    {
        super(action);
        action.addPropertyChangeListener(new PropertyChangeListener() {
                public void  propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("selected"))
                        setSelected(((Boolean)e.getNewValue()).booleanValue());
                }
            });
        setMnemonic(action.getMenuMnemonic());
        setDisplayedMnemonicIndex(action.getMenuDisplayedMnemonicIndex());
    }
}

/** Checkbox item with additional "selected" action property. */
class GuiCheckBoxMenuItem
    extends JCheckBoxMenuItem
{
    public GuiCheckBoxMenuItem(GuiAction action)
    {
        super(action);
        action.addPropertyChangeListener(new PropertyChangeListener() {
                public void  propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("selected"))
                        setSelected(((Boolean)e.getNewValue()).booleanValue());
                }
            });
        setMnemonic(action.getMenuMnemonic());
        setDisplayedMnemonicIndex(action.getMenuDisplayedMnemonicIndex());
    }
}
