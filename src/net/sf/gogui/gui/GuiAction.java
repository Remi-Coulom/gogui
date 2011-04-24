// GuiAction.java

package net.sf.gogui.gui;

import java.awt.Toolkit;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import javax.swing.KeyStroke;

public abstract class GuiAction
    extends AbstractAction
{
    public static final ArrayList<GuiAction> s_allActions
        = new ArrayList<GuiAction>();

    public GuiAction(String label)
    {
        this(label, null, null, 0, null);
    }

    public GuiAction(String label, String desc)
    {
        this(label, desc, null, 0, null);
    }

    public GuiAction(String label, String desc, String icon)
    {
        this(label, desc, null, 0, icon);
    }

    public GuiAction(String label, String desc, int accel, String icon)
    {
        this(label, desc, accel, SHORTCUT, icon);
    }

    public GuiAction(String label, String desc, int accel)
    {
        this(label, desc, accel, SHORTCUT, null);
    }

    public GuiAction(String label, String desc, int accel, int modifier)
    {
        this(label, desc, accel, modifier, null);
    }

    /** @param label The action name. May contain a mnemomic marked
        with the convention used in Qt (a preceding '&amp;'). This
        mnemonic is intended for use in a menu, it is not set as the
        global mnemonic for the action, but only stored for later use
        (see getMenuMnemonic() and getMenuDisplayedMnemonicIndex()). */
    public GuiAction(String label, String desc, Integer accel, int modifier,
                     String icon)
    {
        int pos = label.indexOf('&');
        label = label.replace("&", "");
        putValue(AbstractAction.NAME, label);
        m_menuMnemonic = 0;
        m_menuDisplayedMnemonicIndex = -1;
        if (pos >= 0 && pos < label.length())
        {
            String mnemomic = label.substring(pos, pos + 1).toUpperCase();
            KeyStroke keyStroke = KeyStroke.getKeyStroke(mnemomic);
            int code = keyStroke.getKeyCode();
            m_menuMnemonic = code;
            m_menuDisplayedMnemonicIndex = pos;
        }
        if (desc != null)
            putValue(AbstractAction.SHORT_DESCRIPTION, desc);
        if (accel != null)
            putValue(AbstractAction.ACCELERATOR_KEY,
                     getKeyStroke(accel.intValue(), modifier));
        if (icon != null)
            putValue(AbstractAction.SMALL_ICON,
                     GuiUtil.getIcon(icon, label));
        s_allActions.add(this);
    }

    public int getMenuMnemonic()
    {
        return m_menuMnemonic;
    }

    public int getMenuDisplayedMnemonicIndex()
    {
        return m_menuDisplayedMnemonicIndex;
    }

    public static void register(JComponent component, GuiAction action)
    {
        KeyStroke keyStroke =
            (KeyStroke)action.getValue(AbstractAction.ACCELERATOR_KEY);
        if (keyStroke != null)
        {
            String name = (String)action.getValue(AbstractAction.NAME);
            InputMap inputMap =
                component.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(keyStroke, name);
            component.getActionMap().put(name, action);
        }
    }

    public static void registerAll(JComponent component)
    {
        for (GuiAction action : s_allActions)
            register(component, action);
    }

    public final void setDescription(String desc)
    {
        if (desc == null)
            putValue(AbstractAction.SHORT_DESCRIPTION, null);
        else
            putValue(AbstractAction.SHORT_DESCRIPTION, desc);
    }

    public void setDescription(String desc, Object... args)
    {
        putValue(AbstractAction.SHORT_DESCRIPTION, format(desc, args));
    }

    public void setName(String name)
    {
        putValue(AbstractAction.NAME, name);
    }

    public void setName(String name, Object... args)
    {
        putValue(AbstractAction.NAME, format(name, args));
    }

    public void setSelected(boolean selected)
    {
        putValue("selected", Boolean.valueOf(selected));
    }

    private static final int SHORTCUT
        = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private int m_menuMnemonic;

    private int m_menuDisplayedMnemonicIndex;

    private static KeyStroke getKeyStroke(int keyCode, int modifier)
    {
        return KeyStroke.getKeyStroke(keyCode, modifier);
    }
}
