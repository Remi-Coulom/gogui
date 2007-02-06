//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;


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
        m_setupBlack = addToggleButton(actions.m_actionSetupBlack);
        m_setupWhite = addToggleButton(actions.m_actionSetupWhite);
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

    /** Workaround for bug in Java 1.6.
        Toggle buttons are not reset to unselected state correctly if the
        button lost its focus in the action handler (modal dialog in
        GoGui.actionSetup())
    */
    public void setToggleButtonsSelectedFalse()
    {
        m_setupBlack.setSelected(false);
        m_setupWhite.setSelected(false);
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

    private GoGuiToggleButton m_setupBlack;

    private GoGuiToggleButton m_setupWhite;

    private AbstractButton addButton(AbstractButton button)
    {
        button.setFocusable(false);
        add(button);
        return button;
    }

    private JButton addButton(AbstractAction action)
    {
        JButton button = new JButton(action);
        setAction(button, action);
        addButton(button);
        return button;
    }

    private GoGuiToggleButton addToggleButton(AbstractAction action)
    {
        GoGuiToggleButton button = new GoGuiToggleButton(action);
        setAction(button, action);
        addButton(button);
        return button;
    }

    private void setAction(AbstractButton button, Action action)
    {
        button.setAction(action);
        // Don't use text unless there is no icon
        if (button.getIcon() != null)
            button.setText(null);
    }
}

/** Toggle button with additional "selected" action property. */
class GoGuiToggleButton
    extends JToggleButton
{
    public GoGuiToggleButton(AbstractAction action)
    {
        super(action);
        action.addPropertyChangeListener(new PropertyChangeListener() {
                public void  propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("selected"))
                        setSelected(((Boolean)e.getNewValue()).booleanValue());
                } } );
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
