//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

/** Context menu for fields on board. */
public class ContextMenu
    extends JPopupMenu
{
    public interface Listener
    {
        void setAnalyzeCommand(AnalyzeCommand command);
    }

    public ContextMenu(Vector supportedCommands, Listener listener)
    {
        m_listener = listener;
        Vector commands = new Vector();
        Vector labels = new Vector();
        try
        {
            AnalyzeCommand.read(commands, labels, supportedCommands);
        }
        catch (Exception e)
        {
        }
        m_actionListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    Listener listener = ContextMenu.this.m_listener;
                    if (listener == null)
                        return;
                    String actionCommand = event.getActionCommand();
                    if (actionCommand.equals("cancel"))
                    {
                        ContextMenu.this.setVisible(false);
                        return;
                    }
                    int index = Integer.parseInt(actionCommand);
                    AnalyzeCommand command = getCommand(index);
                    command.setPointArg(m_pointArg);
                    listener.setAnalyzeCommand(command);
                }
            };
        for (int i = 0; i < commands.size(); ++i)
        {
            String line = (String)commands.get(i);
            AnalyzeCommand command = new AnalyzeCommand(line);
            if (command.needsOnlyPointArg())
                addCommand(command);
            else if (command.needsOnlyPointAndColorArg())
                addColorCommand(command);
        }
        addSeparator();
        JMenuItem item = new JMenuItem("Cancel");
        item.addActionListener(m_actionListener);
        item.setActionCommand("cancel");
        add(item);
    }

    public GoPoint getPointArg()
    {
        return m_pointArg;
    }

    public boolean isEmpty()
    {
        return (m_commands.size() == 0);
    }

    public void setPointArg(GoPoint point)
    {
        m_pointArg = point;
        for (int i = 0; i < m_abstractButtons.size(); ++i)
        {
            AbstractButton item = (AbstractButton)m_abstractButtons.get(i);
            String label = item.getText();
            int index = label.lastIndexOf(' ');
            if (index >= 0)
            {
                String base = label.substring(0, index + 1);
                item.setText(base + point.toString());
            }
        }
    }

    private ActionListener m_actionListener;

    private GoPoint m_pointArg;

    private Listener m_listener;

    /** Vector<AnalyzeCommand> */
    private Vector m_commands = new Vector();

    /** Vector<AbstractButton> */
    private Vector m_abstractButtons = new Vector();

    private void addColorCommand(AnalyzeCommand command)
    {
        String label = command.getLabel();
        JMenu menu = new JMenu(label + " A1");
        command.setColorArg(GoColor.BLACK);
        JMenuItem item = createItem(command, "Black");
        menu.add(item);
        command = new AnalyzeCommand(command);
        command.setColorArg(GoColor.WHITE);
        item = createItem(command, "White");
        menu.add(item);
        add(menu);
        m_abstractButtons.add(menu);
    }

    private void addCommand(AnalyzeCommand command)
    {        
        JMenuItem item = createItem(command, command.getLabel() + " A1");
        add(item);
        m_abstractButtons.add(item);
    }

    private JMenuItem createItem(AnalyzeCommand command, String label)
    {
        assert(! m_commands.contains(command));
        m_commands.add(command);
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(m_actionListener);
        item.setActionCommand(Integer.toString(m_commands.size() - 1));
        return item;
    }

    private AnalyzeCommand getCommand(int index)
    {
        return (AnalyzeCommand)m_commands.get(index);
    }
}

//----------------------------------------------------------------------------
