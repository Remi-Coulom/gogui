//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import board.*;

//-----------------------------------------------------------------------------

class MenuBars
{
    public MenuBars(ActionListener listener)
    {
        m_listener = listener;

        m_normalMenuBar = new JMenuBar();
        m_normalMenuBar.add(createFileMenu());
        m_normalMenuBar.add(createGameMenu());
        m_normalMenuBar.add(createBoardMenu());
        m_normalMenuBar.add(createAnalyzeMenu());
        m_normalMenuBar.add(Box.createHorizontalGlue());
        m_normalMenuBar.add(createHelpMenu());

        m_setupMenuBar = new JMenuBar();
        m_setupMenuBar.add(createSetupMenu());
        m_setupMenuBar.add(Box.createHorizontalGlue());
        m_setupMenuBar.add(createHelpMenu());

        m_scoreMenuBar = new JMenuBar();
        m_scoreMenuBar.add(createScoreMenu());
        m_scoreMenuBar.add(Box.createHorizontalGlue());
        m_scoreMenuBar.add(createHelpMenu());
    }

    public void setComputerBlack()
    {
        m_itemComputerBlack.setSelected(true);
    }

    public void setComputerBoth()
    {
        m_itemComputerBoth.setSelected(true);
    }

    public void setComputerNone()
    {
        m_itemComputerNone.setSelected(true);
    }

    public void setComputerWhite()
    {
        m_itemComputerWhite.setSelected(true);
    }

    public JMenuBar getNormalMenu()
    {
        return m_normalMenuBar;
    }

    public JMenuBar getScoreMenu()
    {
        return m_scoreMenuBar;
    }

    public JMenuBar getSetupMenu()
    {
        return m_setupMenuBar;
    }

    public void selectBoardSizeItem(int size)
    {
        for (int i = 0; i < m_possibleBoardSizes.length; ++i)
            if (m_possibleBoardSizes[i] == size)
            {
                m_itemBoardSize[i].setSelected(true);
                return;
            }
        assert(false);
    }

    public void setCommandInProgress(boolean commandInProgress)
    {
        if (commandInProgress)
        {
            for (int i = 0; i < m_normalMenuBar.getMenuCount(); ++i)
            {
                JMenu menu = m_normalMenuBar.getMenu(i);
                if (menu != null)
                    for (int j = 0; j < menu.getItemCount(); ++j)
                        if (menu.getItem(j) != null)
                            menu.getItem(j).setEnabled(false);
            }
            m_itemExit.setEnabled(true);
            m_menuComputerColor.setEnabled(true);
        }
        else
        {
            for (int i = 0; i < m_normalMenuBar.getMenuCount(); ++i)
            {
                JMenu menu = m_normalMenuBar.getMenu(i);
                if (menu != null)
                    for (int j = 0; j < menu.getItemCount(); ++j)
                        if (menu.getItem(j) != null)
                            menu.getItem(j).setEnabled(true);
            }
        }
    }

    private static int m_possibleBoardSizes[] = { 9, 11, 13, 15, 17, 19 };
    private static int m_possibleHandicaps[] = { 0, 2, 3, 4, 5, 6, 7, 8, 9 };
    private ActionListener m_listener;
    private JMenu m_menuComputerColor;
    private JMenuBar m_normalMenuBar;
    private JMenuBar m_scoreMenuBar;
    private JMenuBar m_setupMenuBar;
    private JMenuItem[] m_itemBoardSize;
    private JMenuItem m_itemComputerBlack;
    private JMenuItem m_itemComputerBoth;
    private JMenuItem m_itemComputerNone;
    private JMenuItem m_itemComputerWhite;
    private JMenuItem m_itemExit;

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, String command)
    {
        item.addActionListener(m_listener);
        item.setActionCommand(command);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, String label, String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, command);        
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int accel,
                                  String command)
    {
        JMenuItem item = new JMenuItem(label);
        KeyStroke k = KeyStroke.getKeyStroke(accel, ActionEvent.CTRL_MASK); 
        item.setAccelerator(k);
        return addMenuItem(menu, item, command);
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group,
                                   String label, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, command);
    }

    private JMenu createAnalyzeMenu()
    {
        JMenu menu = new JMenu("Experts");
        menu.setMnemonic(KeyEvent.VK_E);
        addMenuItem(menu, "Analyze", KeyEvent.VK_Y, "analyze");
        addMenuItem(menu, "GTP shell", KeyEvent.VK_C, "gtp-shell");
        return menu;
    }

    private JMenu createBoardMenu()
    {
        JMenu menu = new JMenu("Board");
        menu.setMnemonic(KeyEvent.VK_B);
        menu.add(createBoardSizeMenu());
        addMenuItem(menu, "Setup", "setup");
        return menu;
    }

    private JMenu createBoardSizeMenu()
    {
        JMenu menu = new JMenu("Size");
        ButtonGroup group = new ButtonGroup();
        int n = m_possibleBoardSizes.length;
        m_itemBoardSize = new JMenuItem[n];
        for (int i = 0; i < n; ++i)
        {
            String s = Integer.toString(m_possibleBoardSizes[i]);
            JMenuItem item = addRadioItem(menu, group, s, "board-size-" + s);
            m_itemBoardSize[i] = item;
        }
        return menu;
    }

    private JMenu createComputerColorMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenu menu = new JMenu("Computer color");
        m_itemComputerBlack = addRadioItem(menu, group, "Black",
                                           "computer-black");
        m_itemComputerWhite = addRadioItem(menu, group, "White",
                                           "computer-white");
        m_itemComputerBoth = addRadioItem(menu, group, "Both",
                                          "computer-both");
        m_itemComputerNone = addRadioItem(menu, group, "None",
                                          "computer-none");
        return menu;
    }

    private JMenu createFileMenu()
    {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "Load game", "load");
        addMenuItem(menu, "Save game", "save");
        addMenuItem(menu, "Save position", "save-position");
        menu.addSeparator();
        addMenuItem(menu, "Print", "print");
        menu.addSeparator();
        addMenuItem(menu, "Open with program", "open-with-program");
        menu.addSeparator();
        m_itemExit = addMenuItem(menu, "Exit", KeyEvent.VK_X, "exit");
        return menu;
    }

    private JMenu createGameMenu()
    {
        JMenu menu = new JMenu("Game");
        menu.setMnemonic(KeyEvent.VK_G);
        addMenuItem(menu, "New game", "new-game");
        addMenuItem(menu, "Komi", "komi");
        menu.add(createRulesMenu());
        menu.add(createHandicapMenu());
        addMenuItem(menu, "Score game", "score");
        menu.addSeparator();
        addMenuItem(menu, "Pass", KeyEvent.VK_P, "pass");
        addMenuItem(menu, "Computer play", KeyEvent.VK_L, "play");
        menu.addSeparator();
        addMenuItem(menu, "Backward", KeyEvent.VK_B, "backward");
        addMenuItem(menu, "Forward", KeyEvent.VK_F, "forward");
        addMenuItem(menu, "Beginning", KeyEvent.VK_A, "beginning");
        addMenuItem(menu, "End", KeyEvent.VK_E, "end");
        menu.addSeparator();
        m_menuComputerColor = createComputerColorMenu();
        menu.add(m_menuComputerColor);
        return menu;
    }

    private JMenu createHandicapMenu()
    {
        JMenu menu = new JMenu("Handicap");
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < m_possibleHandicaps.length; ++i)
        {
            String s = Integer.toString(m_possibleHandicaps[i]);
            JMenuItem item = addRadioItem(menu, group, s, "handicap-" + s);
            if (m_possibleHandicaps[i] == 0)
                item.setSelected(true);
        }
        return menu;
    }

    private JMenu createHelpMenu()
    {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        addMenuItem(menu, "Contents", "help");
        addMenuItem(menu, "About", "about");
        return menu;
    }

    private JMenu createRulesMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenu menu = new JMenu("Rules");
        JMenuItem item =
            addRadioItem(menu, group, "Chinese", "rules-chinese");
        item.setSelected(true);
        addRadioItem(menu, group, "Japanese", "rules-japanese");
        return menu;
    }

    private JMenu createSetupMenu()
    {
        JMenu menu = new JMenu("Setup");
        menu.setMnemonic(KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        JMenuItem item = addRadioItem(menu, group, "Black", "setup-black");
        item.setSelected(true);
        addRadioItem(menu, group, "White", "setup-white");
        menu.addSeparator();
        addMenuItem(menu, "Done", "setup-done");
        return menu;
    }

    private JMenu createScoreMenu()
    {
        JMenu menu = new JMenu("Score");
        menu.setMnemonic(KeyEvent.VK_S);
        addMenuItem(menu, "Done", "score-done");
        return menu;
    }
}

//-----------------------------------------------------------------------------
