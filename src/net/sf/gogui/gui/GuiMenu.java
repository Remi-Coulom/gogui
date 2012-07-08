// GuiMenu.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/** JMenu with additional features.
    - Supports marking the mnemonics in the label with a preceeding '&amp;'
      (like in Qt).
    - Checks the added menu items for unique mnemonics and prints a warning to
      System.err if a mnemonic is used twice.
    - Contains a workaround for a bug in Ubuntu 12.04 that made the menu
      use unreadable color combinations in the default Ubuntu theme Ambiance. */
public class GuiMenu
    extends JMenu
{
    public GuiMenu(String text)
    {
        GuiUtil.setTextAndMnemonic(this, text);
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
        GuiUtil.setTextAndMnemonic(item, action.getNameWithMnemonic());
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

    public void remove(JMenuItem item)
    {
        int mnemonic = item.getMnemonic();
        if (mnemonic > 0)
            m_mnemonics.remove(Integer.valueOf(mnemonic));
        super.remove(item);
    }

    public void updateUI()
    {
        super.updateUI();
        // Workaround for Ubuntu bug #932274 (Regression: Unreadable menu bar
        // with Ambiance theme in Java/Swing GTK L&F)
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf != null)
        {
            Class<? extends LookAndFeel> lafClass = laf.getClass();
            if ("GTKLookAndFeel".equals(lafClass.getSimpleName()))
            {
                String themeName = null;
                try
                {
                    Method method =
                        lafClass.getDeclaredMethod("getGtkThemeName");
                    method.setAccessible(true);
                    Object theme = method.invoke(laf);
                    if (theme != null)
                        themeName = theme.toString();
                }
                catch (Exception e)
                {
                }
                if ("Ambiance".equalsIgnoreCase(themeName))
                {
                    // Hard-coding the colors is not nice. It might look bad
                    // in future versions of the Ambiance theme. But it is also
                    // not nice of Ubuntu not to fix a critical bug that breaks
                    // all Java Swing applications in the default Ubuntu theme
                    // for such a long time and we don't know a better
                    // workaround.
                    setForeground(new Color(223, 219, 210));
                    setBackground(new Color(60, 59, 55));
                }
            }
        }
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
        GuiUtil.setTextAndMnemonic(this, action.getNameWithMnemonic());
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
        GuiUtil.setTextAndMnemonic(this, action.getNameWithMnemonic());
    }
}
