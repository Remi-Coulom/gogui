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

/** AbstractAction with additional features.
    Keeps a global variable that stores all actions to allow to register
    the accelerator keys at windows and dialogs with a single function
    call (GuiAction.registerAll()).
    The action name may contain a mnemomic marked with a preceding '&amp;'
    (like in Qt). This mnemonic is intended for use in a menu, it is not set
    as the global mnemonic for the action, but only stored for later use
    (see GuiAction.getNameWithMnemonic(), GuiUtil.setTextAndMnemonic()). */
public abstract class GuiAction
    extends AbstractAction
{
    public static final ArrayList<GuiAction> s_allActions
        = new ArrayList<GuiAction>();

    public GuiAction(String name)
    {
        this(name, null, null, 0, null);
    }

    public GuiAction(String name, String desc)
    {
        this(name, desc, null, 0, null);
    }

    public GuiAction(String name, String desc, String icon)
    {
        this(name, desc, null, 0, icon);
    }

    public GuiAction(String name, String desc, int accel, String icon)
    {
        this(name, desc, accel, SHORTCUT, icon);
    }

    public GuiAction(String name, String desc, int accel)
    {
        this(name, desc, accel, SHORTCUT, null);
    }

    public GuiAction(String name, String desc, int accel, int modifier)
    {
        this(name, desc, accel, modifier, null);
    }

    public GuiAction(String name, String desc, Integer accel, int modifier,
                     String icon)
    {
        m_nameWithMnemonic = name;
        name = name.replace("&", "");
        putValue(AbstractAction.NAME, name);
        if (desc != null)
            putValue(AbstractAction.SHORT_DESCRIPTION, desc);
        if (accel != null)
            putValue(AbstractAction.ACCELERATOR_KEY,
                     getKeyStroke(accel.intValue(), modifier));
        if (icon != null)
            putValue(AbstractAction.SMALL_ICON, GuiUtil.getIcon(icon, name));
        s_allActions.add(this);
    }

    /** Get the name of the action with the mnemonic marked with a preceeding
        '&amp;' (like in Qt). */
    public String getNameWithMnemonic()
    {
        return m_nameWithMnemonic;
    }

    /** Register the accelerator key of an action at a component. */
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

    /** Register the accelerator keys of all actions at a component. */
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

    public void setSelected(boolean selected)
    {
        putValue("selected", Boolean.valueOf(selected));
    }

    private static final int SHORTCUT
        = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private String m_nameWithMnemonic;

    private static KeyStroke getKeyStroke(int keyCode, int modifier)
    {
        return KeyStroke.getKeyStroke(keyCode, modifier);
    }
}
