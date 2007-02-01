//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.Timer;

/** Tool bar for GoGui. */
public class GoGuiToolBar
    extends JToolBar
{
    /** Tool bar for GoGui. */
    public GoGuiToolBar(GoGui goGui)
    {
        m_goGui = goGui;
        GoGuiActions actions = m_goGui.getActions();
        addButton(actions.m_actionOpen);
        m_buttonSave = addButton(actions.m_actionSave);
        addSeparator();
        addButton(actions.m_actionNewGame);
        addButton(actions.m_actionPass);
        addSeparator();
        addButton(actions.m_actionPlay);
        addButton(actions.m_actionInterrupt);
        addSeparator();
        addButton(actions.m_actionBeginning);
        addButton(actions.m_actionBackwardTen);
        addButton(actions.m_actionBackward);
        addButton(actions.m_actionForward);
        addButton(actions.m_actionForwardTen);
        addButton(actions.m_actionEnd);
        addSeparator();
        addButton(actions.m_actionNextVariation);
        addButton(actions.m_actionPreviousVariation);
        setRollover(true);
        setFloatable(false);
        // For com.jgoodies.looks
        putClientProperty("jgoodies.headerStyle", "Both");
    }

    public void update()
    {
        GoGuiActions actions = m_goGui.getActions();
        if (m_goGui.getFile() == null)
            setAction(m_buttonSave, actions.m_actionSaveAs);
        else
            setAction(m_buttonSave, actions.m_actionSave);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    GoGui m_goGui;

    JButton m_buttonSave;

    private JButton addButton(JButton button)
    {
        button.setFocusable(false);
        add(button);
        return button;
    }

    private JButton addButton(AbstractAction action)
    {
        JButton button = new JButton(action);
        setAction(button, action);
        return addButton(button);
    }

    private void addRepeatButton(AbstractAction action,
                                 AbstractAction repeatAction)
    {
        RepeatButton button = new RepeatButton(action, repeatAction);
        // Don't use text unless there is no icon
        if (button.getIcon() != null)
            button.setText(null);
        addButton(button);
    }

    private void setAction(JButton button, Action action)
    {
        button.setAction(action);
        // Don't use text unless there is no icon
        if (button.getIcon() != null)
            button.setText(null);
    }
}
