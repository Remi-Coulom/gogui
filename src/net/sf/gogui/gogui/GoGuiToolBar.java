//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;

//----------------------------------------------------------------------------

/** Tool bar for GoGui. */
public class GoGuiToolBar
    extends JToolBar
{
    /** Tool bar for GoGui. */
    public GoGuiToolBar(ActionListener listener)
    {
        m_listener = listener;
        m_buttonOpen = addButton("fileopen.png", "open", "Open");
        m_buttonSave = addButton("filesave2.png", "save", "Save");
        addSeparator();
        m_buttonNew = addButton("filenew.png", "new-game", "New Game");
        addSeparator();
        m_buttonEnter = addButton("next.png", "play", "Play");
        m_buttonPass = addButton("pass.png", "pass", "Pass");
        m_buttonInterrupt = addButton("stop.png", "interrupt", "Interrupt");
        addSeparator();
        m_buttonBeginning =
            addNavigationButton("beginning.png", "beginning", "Beginning");
        m_buttonBackward10 =
            addNavigationButton("backward10.png", "backward-10",
                                "Backward 10");
        m_buttonBackward =
            addNavigationButton("back.png", "backward", "Backward");
        m_buttonForward
            = addNavigationButton("forward.png", "forward", "Forward");
        m_buttonForward10 =
            addNavigationButton("forward10.png", "forward-10", "Forward 10");
        m_buttonEnd = addNavigationButton("end.png", "end", "End");
        addSeparator();
        m_buttonNextVariation =
            addNavigationButton("down.png", "next-variation",
                                "Next Variation");
        m_buttonPreviousVariation =
            addNavigationButton("up.png", "previous-variation",
                                "Previous Variation");
        setRollover(true);
        setFloatable(false);
        // For com.jgoodies.looks
        putClientProperty("jgoodies.headerStyle", "Both");
    }

    public void setComputerEnabled(boolean enabled)
    {
        m_computerButtonsEnabled = enabled;
        setEnabled(m_buttonEnter, enabled);
    }

    /** Enable/disable buttons according to current position. */
    public void update(Node node)
    {
        assert(node != null);
        boolean hasFather = (node.getFather() != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtils.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtils.getPreviousVariation(node) != null);
        setCanNavigate(m_buttonBeginning, hasFather);
        setCanNavigate(m_buttonBackward, hasFather);
        setCanNavigate(m_buttonBackward10, hasFather);
        setCanNavigate(m_buttonForward, hasChildren);
        setCanNavigate(m_buttonForward10, hasChildren);
        setCanNavigate(m_buttonEnd, hasChildren);
        setCanNavigate(m_buttonNextVariation, hasNextVariation);
        setCanNavigate(m_buttonPreviousVariation, hasPreviousVariation);
        paintImmediately(getVisibleRect());
    }

    public void enableAll(boolean enable, Node node)
    {
        setEnabled(m_buttonBeginning, enable);
        setEnabled(m_buttonBackward, enable);
        setEnabled(m_buttonBackward10, enable);
        setEnabled(m_buttonEnd, enable);
        setEnabled(m_buttonEnter, enable);
        setEnabled(m_buttonForward, enable);
        setEnabled(m_buttonForward10, enable);
        setEnabled(m_buttonInterrupt, false);
        setEnabled(m_buttonNew, enable);
        setEnabled(m_buttonNextVariation, enable);
        setEnabled(m_buttonOpen, enable);
        setEnabled(m_buttonPass, enable);
        setEnabled(m_buttonPreviousVariation, enable);
        setEnabled(m_buttonSave, enable);
        if (enable)
        {
            if (! m_computerButtonsEnabled)
                setComputerEnabled(false);
            update(node);
        }
    }

    public void setCommandInProgress()
    {
        enableAll(false, null);
        setEnabled(m_buttonInterrupt, true);
    }

    private boolean m_computerButtonsEnabled = true;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final NavigationButton m_buttonBeginning;

    private final NavigationButton m_buttonBackward;

    private final NavigationButton m_buttonBackward10;

    private final NavigationButton m_buttonEnd;

    private final JButton m_buttonEnter;

    private final NavigationButton m_buttonForward;

    private final NavigationButton m_buttonForward10;

    private final JButton m_buttonInterrupt;

    private final JButton m_buttonNew;

    private final NavigationButton m_buttonNextVariation;

    private final JButton m_buttonOpen;

    private final JButton m_buttonPass;

    private final NavigationButton m_buttonPreviousVariation;

    private final JButton m_buttonSave;

    private JButton addButton(String icon, String command, String toolTip)
    {
        JButton button = new JButton();
        Icon imageIcon = getIcon(icon, command);
        button.setIcon(imageIcon);
        button.setDisabledIcon(imageIcon);
        addButton(button, command, toolTip);
        return button;
    }

    private JButton addButton(JButton button, String command, String toolTip)
    {
        button.setActionCommand(command);
        button.setToolTipText(toolTip);
        button.addActionListener(m_listener);
        button.setEnabled(false);
        button.setFocusable(false);
        add(button);
        return button;
    }

    private NavigationButton addNavigationButton(String icon, String command,
                                                 String toolTip)
    {
        Icon imageIcon = getIcon(icon, command);
        NavigationButton button = new NavigationButton(imageIcon);
        addButton(button, command, toolTip);
        return button;
    }

    private Icon getIcon(String name, String command)
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/images/" + name);
        return new ImageIcon(url, command);
    }

    private static void setCanNavigate(NavigationButton button,
                                       boolean canNavigate)
    {
        button.setCanNavigate(canNavigate);
    }

    private static void setEnabled(JButton button, boolean enabled)
    {
        button.setEnabled(enabled);
    }
}

//----------------------------------------------------------------------------

class NavigationButton
    extends JButton
{
    public NavigationButton(Icon icon)
    {
        setIcon(icon);
        m_icon = icon;
        m_disabledIcon = getDisabledIcon();
    }

    public void setCanNavigate(boolean canNavigate)
    {
        if (canNavigate)
            setDisabledIcon(m_icon);
        else
            setDisabledIcon(m_disabledIcon);
        setEnabled(canNavigate);
    }

    private Icon m_icon;

    private Icon m_disabledIcon;
}

//----------------------------------------------------------------------------
