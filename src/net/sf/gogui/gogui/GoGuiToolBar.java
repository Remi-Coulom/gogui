//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.Timer;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.util.Platform;

/** Tool bar for GoGui. */
public class GoGuiToolBar
    extends JToolBar
{
    /** Tool bar for GoGui. */
    public GoGuiToolBar(ActionListener listener)
    {
        m_listener = listener;
        m_buttonOpen = addButton("document-open.png", "open", "Open");
        m_buttonSave = addButton("document-save.png", "save", "Save");
        addSeparator();
        m_buttonNew = addButton("gogui-newgame.png", "new-game", "New Game");
        m_buttonPass = addButton("gogui-pass.png", "pass", "Pass");
        addSeparator();
        m_buttonPlay = addOptionalButton("gogui-play.png", "play", "Play");
        m_buttonInterrupt =
            addOptionalButton("gogui-interrupt.png", "interrupt", "Interrupt");
        addSeparator();
        m_buttonBeginning =
            addOptionalButton("gogui-first.png", "beginning", "Beginning");
        m_buttonBackward =
            addRepeatButton("gogui-previous.png", "backward", "backward-10",
                            "Backward");
        m_buttonForward
            = addRepeatButton("gogui-next.png", "forward", "forward-10",
                              "Forward");
        m_buttonEnd = addOptionalButton("gogui-last.png", "end", "End");
        addSeparator();
        m_buttonNextVariation =
            addOptionalButton("gogui-down.png", "next-variation",
                              "Next Variation");
        m_buttonPreviousVariation =
            addOptionalButton("gogui-up.png", "previous-variation",
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
    public void update(ConstNode node)
    {
        assert(node != null);
        boolean hasFather = (node.getFatherConst() != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        m_buttonBeginning.setSameDisabledIcon(hasFather);
        m_buttonBackward.setSameDisabledIcon(hasFather);
        m_buttonForward.setSameDisabledIcon(hasChildren);
        m_buttonEnd.setSameDisabledIcon(hasChildren);
        m_buttonNextVariation.setSameDisabledIcon(hasNextVariation);
        m_buttonPreviousVariation.setSameDisabledIcon(hasPreviousVariation);
        paintImmediately(getVisibleRect());
    }

    public void enableAll(boolean enable, ConstNode node)
    {
        setEnabled(m_buttonBeginning, enable);
        setEnabled(m_buttonBackward, enable);
        setEnabled(m_buttonEnd, enable);
        setEnabled(m_buttonForward, enable);
        setEnabled(m_buttonInterrupt, false);
        setEnabled(m_buttonNew, enable);
        setEnabled(m_buttonNextVariation, enable);
        setEnabled(m_buttonOpen, enable);
        setEnabled(m_buttonPreviousVariation, enable);
        setEnabled(m_buttonSave, enable);
        if (Platform.isMac())
        {
            // See setCommandInProgress()
            setEnabled(m_buttonPlay, enable);
            setEnabled(m_buttonPass, enable);
        }
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
        if (! Platform.isMac())
        {
            // Enable play and pass to avoid wrong rendering of rollover
            // effect on Linux and Windows, if mouse stays over button.
            // Need to discard the events in GoGui.cbPlay
            setEnabled(m_buttonPlay, true);
            setEnabled(m_buttonPass, true);
        }
        setEnabled(m_buttonInterrupt, true);
    }

    private boolean m_computerButtonsEnabled = true;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final OptionalButton m_buttonBeginning;

    private final RepeatButton m_buttonBackward;

    private final OptionalButton m_buttonEnd;

    private final OptionalButton m_buttonPlay;

    private final RepeatButton m_buttonForward;

    private final OptionalButton m_buttonInterrupt;

    private final JButton m_buttonNew;

    private final OptionalButton m_buttonNextVariation;

    private final JButton m_buttonOpen;

    private final JButton m_buttonPass;

    private final OptionalButton m_buttonPreviousVariation;

    private final JButton m_buttonSave;

    /** Add button with same icon for enabled and disabled state.
        Avoids too much flickering of the toolbar during the game
        (for buttons that are disabled while the computer is thinking).
    */
    private JButton addButton(String icon, String command, String toolTip)
    {
        JButton button = new JButton();
        Icon imageIcon = getIcon(icon, command);
        button.setIcon(imageIcon);
        button.setDisabledIcon(imageIcon);
        addButton(button, command, toolTip);
        return button;
    }

    private JButton addButton(JButton button, String toolTip)
    {
        button.setToolTipText(toolTip);
        button.setFocusable(false);
        add(button);
        return button;
    }

    private JButton addButton(JButton button, String command, String toolTip)
    {
        button.setActionCommand(command);
        button.addActionListener(m_listener);
        return addButton(button, toolTip);
    }

    private OptionalButton addOptionalButton(String icon, String command,
                                             String toolTip)
    {
        Icon imageIcon = getIcon(icon, command);
        OptionalButton button = new OptionalButton(imageIcon);
        addButton(button, command, toolTip);
        return button;
    }

    private RepeatButton addRepeatButton(String icon, String command1,
                                         String command2, String toolTip)
    {
        Icon imageIcon = getIcon(icon, command1);
        RepeatButton button = new RepeatButton(imageIcon, command1, command2,
                                               m_listener);
        addButton(button, toolTip);
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

    private final Icon m_icon;

    private final Icon m_disabledIcon;
}

/** Button that triggers repeated actions when hold down for a longer time. */
class RepeatButton
    extends OptionalButton
{
    /** Constructor.
        @param command1 The default command used for clicks
        @param command2 The command used for the repeated commands
    */
    public RepeatButton(Icon icon, String command1, String command2,
                        ActionListener listener)
    {
        super(icon);
        m_listener = listener;
        m_command1 = command1;
        m_command2 = command2;
        m_timer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent event)
                {
                    ++m_count;
                    fireActionCommand(m_command2);
                }
            });
        m_timer.stop();
        addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent event)
                {
                    m_count = 0;
                    m_timer.start();
                }

                public void mouseReleased(MouseEvent event)
                {
                    m_timer.stop();
                    if (contains(event.getPoint()) && m_count == 0)
                        fireActionCommand(m_command1);
                }
            });
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private int m_count;

    private Timer m_timer;

    private String m_command1;

    private String m_command2;

    ActionListener m_listener;

    private void fireActionCommand(String command)
    {
        ActionEvent event = new ActionEvent(this, 0, command);
        m_listener.actionPerformed(event);
    }
}
