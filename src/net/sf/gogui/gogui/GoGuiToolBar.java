//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.net.URL;
import javax.swing.Action;
import javax.swing.AbstractAction;
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
        addRepeatButton(actions.m_actionBackward, actions.m_actionBackwardTen);
        addRepeatButton(actions.m_actionForward, actions.m_actionForwardTen);
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

/** Button that triggers repeated actions when hold down for a longer time. */
class RepeatButton
    extends JButton
{
    /** Constructor.
        @param command1 The default command used for clicks
        @param command2 The command used for the repeated commands
    */
    public RepeatButton(AbstractAction action, AbstractAction repeatAction)
    {
        setAction(action);
        m_action = action;
        m_repeatAction = repeatAction;
        m_timer = new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e)
                {
                    ++m_count;
                    m_repeatAction.actionPerformed(null);
                }
            });
        m_timer.stop();
        addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event)
                {
                    m_timer.stop();
                    if (contains(event.getPoint()) && m_count == 0)
                        m_action.actionPerformed(null);
                    enableAction();
                }

                public void mousePressed(MouseEvent event)
                {
                    disableAction();
                    m_count = 0;
                    m_timer.start();
                }
            });
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private int m_count;

    private Timer m_timer;

    private AbstractAction m_action;

    private AbstractAction m_repeatAction;

    private void disableAction()
    {
        setAction(null);
        setIcon((Icon)m_action.getValue(AbstractAction.SMALL_ICON));
    }

    private void enableAction()
    {
        setAction(m_action);
        if (getIcon() != null)
            setText(null);
    }
}
