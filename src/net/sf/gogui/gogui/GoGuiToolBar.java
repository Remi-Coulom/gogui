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
        m_buttonPlay = addOptionalButton("next.png", "play", "Play");
        m_buttonPass = addButton("pass.png", "pass", "Pass");
        m_buttonInterrupt =
            addOptionalButton("stop.png", "interrupt", "Interrupt");
        addSeparator();
        m_buttonBeginning =
            addOptionalButton("beginning.png", "beginning", "Beginning");
        m_buttonBackward10 =
            addOptionalButton("backward10.png", "backward-10", "Backward 10");
        m_buttonBackward =
            addOptionalButton("back.png", "backward", "Backward");
        m_buttonForward
            = addOptionalButton("forward.png", "forward", "Forward");
        m_buttonForward10 =
            addOptionalButton("forward10.png", "forward-10", "Forward 10");
        m_buttonEnd = addOptionalButton("end.png", "end", "End");
        addSeparator();
        m_buttonNextVariation =
            addOptionalButton("down.png", "next-variation", "Next Variation");
        m_buttonPreviousVariation =
            addOptionalButton("up.png", "previous-variation",
                              "Previous Variation");
        setRollover(true);
        setFloatable(false);
        // For com.jgoodies.looks
        putClientProperty("jgoodies.headerStyle", "Both");
    }

    public void setComputerEnabled(boolean enabled)
    {
        m_computerButtonsEnabled = enabled;
        m_buttonPlay.setSameDisabledIcon(enabled);
        m_buttonInterrupt.setSameDisabledIcon(enabled);
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
        m_buttonBeginning.setSameDisabledIcon(hasFather);
        m_buttonBackward.setSameDisabledIcon(hasFather);
        m_buttonBackward10.setSameDisabledIcon(hasFather);
        m_buttonForward.setSameDisabledIcon(hasChildren);
        m_buttonForward10.setSameDisabledIcon(hasChildren);
        m_buttonEnd.setSameDisabledIcon(hasChildren);
        m_buttonNextVariation.setSameDisabledIcon(hasNextVariation);
        m_buttonPreviousVariation.setSameDisabledIcon(hasPreviousVariation);
        paintImmediately(getVisibleRect());
    }

    public void enableAll(boolean enable, Node node)
    {
        setEnabled(m_buttonBeginning, enable);
        setEnabled(m_buttonBackward, enable);
        setEnabled(m_buttonBackward10, enable);
        setEnabled(m_buttonEnd, enable);
        setEnabled(m_buttonForward, enable);
        setEnabled(m_buttonForward10, enable);
        setEnabled(m_buttonInterrupt, false);
        setEnabled(m_buttonNew, enable);
        setEnabled(m_buttonNextVariation, enable);
        setEnabled(m_buttonOpen, enable);
        setEnabled(m_buttonPreviousVariation, enable);
        setEnabled(m_buttonSave, enable);
        // Play and pass buttons are always enabled, see
        //setCommandInProgress()
        //setEnabled(m_buttonPlay, enable);
        //setEnabled(m_buttonPass, enable);
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
        // Enable play and pass to avoid wrong rendering of rollover effect,
        // if mouse stays over button. Need to discard the events in
        // GoGui.cbPlay
        setEnabled(m_buttonPlay, true);
        setEnabled(m_buttonPass, true);
        setEnabled(m_buttonInterrupt, true);
    }

    private boolean m_computerButtonsEnabled = true;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final OptionalButton m_buttonBeginning;

    private final OptionalButton m_buttonBackward;

    private final OptionalButton m_buttonBackward10;

    private final OptionalButton m_buttonEnd;

    private final OptionalButton m_buttonPlay;

    private final OptionalButton m_buttonForward;

    private final OptionalButton m_buttonForward10;

    private final OptionalButton m_buttonInterrupt;

    private final JButton m_buttonNew;

    private final OptionalButton m_buttonNextVariation;

    private final JButton m_buttonOpen;

    private final JButton m_buttonPass;

    private final OptionalButton m_buttonPreviousVariation;

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
        button.setFocusable(false);
        add(button);
        return button;
    }

    private OptionalButton addOptionalButton(String icon, String command,
                                                 String toolTip)
    {
        Icon imageIcon = getIcon(icon, command);
        OptionalButton button = new OptionalButton(imageIcon);
        addButton(button, command, toolTip);
        return button;
    }

    private Icon getIcon(String name, String command)
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/images/" + name);
        return new ImageIcon(url, command);
    }

    private static void setEnabled(JButton button, boolean enabled)
    {
        button.setEnabled(enabled);
    }
}

//----------------------------------------------------------------------------

/** Toolbar button with optional same disabled icon.
    Can use the same icon in disabled state to avoid too much toolbar
    flickering if button is disabled during command in progress.
*/  
class OptionalButton
    extends JButton
{
    public OptionalButton(Icon icon)
    {
        setIcon(icon);
        m_icon = icon;
        m_disabledIcon = getDisabledIcon();
    }

    public void setSameDisabledIcon(boolean sameDisabledIcon)
    {
        if (sameDisabledIcon)
            setDisabledIcon(m_icon);
        else
            setDisabledIcon(m_disabledIcon);
        setEnabled(sameDisabledIcon);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private Icon m_icon;

    private Icon m_disabledIcon;
}

//----------------------------------------------------------------------------
