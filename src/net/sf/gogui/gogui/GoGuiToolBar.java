// GoGuiToolBar.java

package net.sf.gogui.gogui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.JToggleButton;
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
        m_actions = actions;
        addButton(actions.m_actionOpen);
        addButton(actions.m_actionSave);
        addSeparator();
        addButton(actions.m_actionNewGame);
        addButton(actions.m_actionPass);
        addButton(actions.m_actionPlay);
        addSeparator();
        addToggleButton(actions.m_actionSetupBlack);
        addToggleButton(actions.m_actionSetupWhite);
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
        if (! Platform.isMac())
            setRollover(true);
        setFloatable(false);
    }

    private final GoGui m_goGui;

    private final GoGuiActions m_actions;

    private AbstractButton addButton(AbstractButton button)
    {
        button.setFocusable(false);
        add(button);
        return button;
    }

    private JButton addButton(AbstractAction action)
    {
        JButton button = new JButton(action);
        //button.putClientProperty("Quaqua.Button.style", "toolbar");
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
                }
            });
    }
}
