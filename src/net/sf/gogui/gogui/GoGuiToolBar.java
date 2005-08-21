//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.event.ActionListener;
import java.net.URL;
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
        m_buttonPass = addButton("pass.png", "pass", "Pass");
        m_buttonEnter = addButton("next.png", "play", "Play");
        m_buttonInterrupt = addButton("stop.png", "interrupt", "Interrupt");
        addSeparator();
        m_buttonBeginning = addButton("beginning.png", "beginning",
                                      "Beginning");
        m_buttonBackward10 = addButton("backward10.png", "backward-10",
                                       "Backward 10");
        m_buttonBackward = addButton("back.png", "backward", "Backward");
        m_buttonForward = addButton("forward.png", "forward",
                                    "Forward");
        m_buttonForward10 = addButton("forward10.png", "forward-10",
                                      "Forward 10");
        m_buttonEnd = addButton("end.png", "end", "End");
        m_buttonNextVariation =
            addButton("down.png", "next-variation", "Next Variation");
        m_buttonPreviousVariation =
            addButton("up.png", "previous-variation", "Previous Variation");
        setFloatable(false);
    }

    public void setComputerEnabled(boolean enabled)
    {
        m_computerButtonsEnabled = enabled;
        m_buttonEnter.setEnabled(enabled);
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
        m_buttonBeginning.setEnabled(hasFather);
        m_buttonBackward.setEnabled(hasFather);
        m_buttonBackward10.setEnabled(hasFather);
        m_buttonForward.setEnabled(hasChildren);
        m_buttonForward10.setEnabled(hasChildren);
        m_buttonEnd.setEnabled(hasChildren);
        m_buttonNextVariation.setEnabled(hasNextVariation);
        m_buttonPreviousVariation.setEnabled(hasPreviousVariation);
    }

    public void enableAll(boolean enable, Node node)
    {
        m_buttonBeginning.setEnabled(enable);
        m_buttonBackward.setEnabled(enable);
        m_buttonBackward10.setEnabled(enable);
        m_buttonEnd.setEnabled(enable);
        m_buttonEnter.setEnabled(enable);
        m_buttonForward.setEnabled(enable);
        m_buttonForward10.setEnabled(enable);
        m_buttonInterrupt.setEnabled(false);
        m_buttonNew.setEnabled(enable);
        m_buttonNextVariation.setEnabled(enable);
        m_buttonOpen.setEnabled(enable);
        m_buttonPass.setEnabled(enable);
        m_buttonPreviousVariation.setEnabled(enable);
        m_buttonSave.setEnabled(enable);
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
        m_buttonInterrupt.setEnabled(true);
    }

    private boolean m_computerButtonsEnabled = true;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final JButton m_buttonBeginning;

    private final JButton m_buttonBackward;

    private final JButton m_buttonBackward10;

    private final JButton m_buttonEnd;

    private final JButton m_buttonEnter;

    private final JButton m_buttonForward;

    private final JButton m_buttonForward10;

    private final JButton m_buttonInterrupt;

    private final JButton m_buttonNew;

    private final JButton m_buttonNextVariation;

    private final JButton m_buttonOpen;

    private final JButton m_buttonPass;

    private final JButton m_buttonPreviousVariation;

    private final JButton m_buttonSave;

    private JButton addButton(String icon, String command, String toolTip)
    {
        JButton button = new JButton();
        button.setActionCommand(command);
        button.setToolTipText(toolTip);
        button.addActionListener(m_listener);
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/images/" + icon);
        if (url == null)
            button.setText(command);
        else
            button.setIcon(new ImageIcon(url, command));
        button.setEnabled(false);
        button.setFocusable(false);
        add(button);
        return button;
    }
}

//----------------------------------------------------------------------------
