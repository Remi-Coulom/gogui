//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import go.*;

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
        m_normalMenuBar.add(createSettingsMenu());
        m_menuAnalyze = createAnalyzeMenu();
        m_normalMenuBar.add(m_menuAnalyze);
        m_normalMenuBar.add(createHelpMenu(true));

        m_setupMenuBar = new JMenuBar();
        m_setupMenuBar.add(createSetupMenu());
        m_setupMenuBar.add(createHelpMenu(false));

        m_scoreMenuBar = new JMenuBar();
        m_scoreMenuBar.add(createScoreMenu());
        m_scoreMenuBar.add(createHelpMenu(false));
    }

    public void disableComputerMenus()
    {
        m_menuAnalyze.setEnabled(false);
        m_menuComputerColor.setEnabled(false);
        m_itemComputerPlay.setEnabled(false);
    }

    public boolean getBeepAfterMove()
    {
        return m_itemBeepAfterMove.getState();
    }

    public boolean getShowLastMove()
    {
        return m_itemShowLastMove.getState();
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
    }

    public void selectRulesItem(int rules)
    {
        switch (rules)
        {
        case go.Board.RULES_JAPANESE:
            m_itemRulesJapanese.setSelected(true);
            break;
        case go.Board.RULES_CHINESE:
            m_itemRulesChinese.setSelected(true);
            break;
        default:
            assert false;
        }
    }

    public void setBeepAfterMove(boolean enable)
    {
        m_itemBeepAfterMove.setState(enable);
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
            m_itemAbout.setEnabled(true);
            m_itemBeepAfterMove.setEnabled(true);
            m_itemExit.setEnabled(true);
            m_itemGtpShell.setEnabled(true);
            m_itemHelp.setEnabled(true);
            m_itemShowLastMove.setEnabled(true);
            m_menuComputerColor.setEnabled(true);
            m_itemInterrupt.setEnabled(true);
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
            m_itemInterrupt.setEnabled(false);
        }
    }


    private static int m_possibleBoardSizes[] = { 9, 11, 13, 15, 17, 19 };

    private static int m_possibleHandicaps[] = { 0, 2, 3, 4, 5, 6, 7, 8, 9 };

    private ActionListener m_listener;

    private JCheckBoxMenuItem m_itemBeepAfterMove;

    private JCheckBoxMenuItem m_itemShowLastMove;

    private JMenu m_menuAnalyze;

    private JMenu m_menuComputerColor;

    private JMenuBar m_normalMenuBar;

    private JMenuBar m_scoreMenuBar;

    private JMenuBar m_setupMenuBar;

    private JMenuItem m_itemAbout;

    private JMenuItem[] m_itemBoardSize;

    private JMenuItem m_itemComputerBlack;

    private JMenuItem m_itemComputerBoth;

    private JMenuItem m_itemComputerNone;

    private JMenuItem m_itemComputerPlay;

    private JMenuItem m_itemComputerWhite;

    private JMenuItem m_itemExit;

    private JMenuItem m_itemGtpShell;

    private JMenuItem m_itemHelp;

    private JMenuItem m_itemInterrupt;

    private JMenuItem m_itemRulesChinese;

    private JMenuItem m_itemRulesJapanese;


    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, String command)
    {
        item.addActionListener(m_listener);
        item.setActionCommand(command);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
                                  String command)
    {
        item.setMnemonic(mnemonic);
        return addMenuItem(menu, item, command);
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, mnemonic, command);        
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  int accel, int modifier, String command)
    {
        JMenuItem item = new JMenuItem(label);
        KeyStroke k = KeyStroke.getKeyStroke(accel, modifier); 
        item.setAccelerator(k);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group, String label,
                                   String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, command);
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group, String label,
                                   int mnemonic, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private JMenu createAnalyzeMenu()
    {
        JMenu menu = new JMenu("Experts");
        menu.setMnemonic(KeyEvent.VK_E);
        addMenuItem(menu, "Analyze", KeyEvent.VK_A, KeyEvent.VK_F9, 0,
                    "analyze");
        m_itemGtpShell = addMenuItem(menu, "GTP shell", KeyEvent.VK_G,
                                     KeyEvent.VK_F8, 0, "gtp-shell");
        addMenuItem(menu, "Send GTP file", KeyEvent.VK_S, "gtp-file");
        return menu;
    }

    private JMenu createBoardMenu()
    {
        JMenu menu = new JMenu("Board");
        menu.setMnemonic(KeyEvent.VK_B);
        menu.add(createBoardSizeMenu());
        addMenuItem(menu, "Setup", KeyEvent.VK_S, "setup");
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
        menu.setMnemonic(KeyEvent.VK_C);
        m_itemComputerBlack = addRadioItem(menu, group, "Black", KeyEvent.VK_B,
                                           "computer-black");
        m_itemComputerWhite = addRadioItem(menu, group, "White", KeyEvent.VK_W,
                                           "computer-white");
        m_itemComputerBoth = addRadioItem(menu, group, "Both", KeyEvent.VK_T,
                                          "computer-both");
        m_itemComputerNone = addRadioItem(menu, group, "None", KeyEvent.VK_N,
                                          "computer-none");
        return menu;
    }

    private JMenu createFileMenu()
    {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(menu, "New", KeyEvent.VK_N, KeyEvent.VK_N,
                    ActionEvent.CTRL_MASK, "new-game");
        addMenuItem(menu, "Open...", KeyEvent.VK_O, KeyEvent.VK_O,
                    ActionEvent.CTRL_MASK,
                    "load");
        addMenuItem(menu, "Save...", KeyEvent.VK_S, KeyEvent.VK_S,
                    ActionEvent.CTRL_MASK,
                    "save");
        addMenuItem(menu, "Save position...", KeyEvent.VK_T, "save-position");
        menu.addSeparator();
        addMenuItem(menu, "Print...", KeyEvent.VK_P, KeyEvent.VK_P,
                    ActionEvent.CTRL_MASK,
                    "print");
        menu.addSeparator();
        addMenuItem(menu, "Open with program...", KeyEvent.VK_G,
                    "open-with-program");
        menu.addSeparator();
        m_itemExit = addMenuItem(menu, "Quit", KeyEvent.VK_Q, KeyEvent.VK_Q,
                                 ActionEvent.CTRL_MASK, "exit");
        return menu;
    }

    private JMenu createGameMenu()
    {
        JMenu menu = new JMenu("Game");
        menu.setMnemonic(KeyEvent.VK_G);
        addMenuItem(menu, "Komi...", KeyEvent.VK_K, "komi");
        menu.add(createRulesMenu());
        menu.add(createHandicapMenu());
        addMenuItem(menu, "Score", KeyEvent.VK_S, "score");
        menu.addSeparator();
        addMenuItem(menu, "Pass", KeyEvent.VK_P, KeyEvent.VK_F2, 0, "pass");
        m_itemComputerPlay = addMenuItem(menu, "Computer play", KeyEvent.VK_L,
                                         KeyEvent.VK_F5, 0, "play");
        m_itemInterrupt = addMenuItem(menu, "Interrupt", KeyEvent.VK_I,
                                      KeyEvent.VK_ESCAPE, 0, "interrupt");
        menu.addSeparator();
        addMenuItem(menu, "Beginning", KeyEvent.VK_N, KeyEvent.VK_HOME,
                    ActionEvent.CTRL_MASK,
                    "beginning");
        addMenuItem(menu, "Backward 10 moves", KeyEvent.VK_D, KeyEvent.VK_LEFT,
                    ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK,
                    "backward-10");
        addMenuItem(menu, "Backward", KeyEvent.VK_LEFT, KeyEvent.VK_LEFT,
                    ActionEvent.CTRL_MASK, "backward");
        addMenuItem(menu, "Forward", KeyEvent.VK_F, KeyEvent.VK_RIGHT,
                    ActionEvent.CTRL_MASK, "forward");
        addMenuItem(menu, "Forward 10 moves", KeyEvent.VK_O, KeyEvent.VK_RIGHT,
                    ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK,
                    "forward-10");
        addMenuItem(menu, "End", KeyEvent.VK_E, KeyEvent.VK_END,
                    ActionEvent.CTRL_MASK, "end");
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

    private JMenu createHelpMenu(boolean store)
    {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        JMenuItem itemHelp =
            addMenuItem(menu, "Contents", KeyEvent.VK_C, KeyEvent.VK_F1, 0,
                        "help");
        JMenuItem itemAbout = addMenuItem(menu, "About", KeyEvent.VK_A,
                                          "about");
        if (store)
        {
            m_itemHelp = itemHelp;
            m_itemAbout = itemAbout;
        }
        return menu;
    }

    private JMenu createRulesMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenu menu = new JMenu("Rules");
        m_itemRulesChinese =
            addRadioItem(menu, group, "Chinese", KeyEvent.VK_C,
                         "rules-chinese");
        m_itemRulesChinese.setSelected(true);
        m_itemRulesJapanese =
            addRadioItem(menu, group, "Japanese", KeyEvent.VK_J,
                         "rules-japanese");
        return menu;
    }

    private JMenu createSettingsMenu()
    {
        JMenu menu = new JMenu("Settings");
        m_itemBeepAfterMove = new JCheckBoxMenuItem("Beep after move");
        m_itemBeepAfterMove.addActionListener(m_listener);
        m_itemBeepAfterMove.setActionCommand("beep-after-move");
        menu.add(m_itemBeepAfterMove);
        m_itemShowLastMove = new JCheckBoxMenuItem("Show last move");
        m_itemShowLastMove.addActionListener(m_listener);
        m_itemShowLastMove.setActionCommand("show-last-move");
        m_itemShowLastMove.setState(true);
        menu.add(m_itemShowLastMove);
        return menu;
    }

    private JMenu createSetupMenu()
    {
        JMenu menu = new JMenu("Setup");
        menu.setMnemonic(KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        JMenuItem item = addRadioItem(menu, group, "Black", KeyEvent.VK_B,
                                      "setup-black");
        item.setSelected(true);
        addRadioItem(menu, group, "White", KeyEvent.VK_W, "setup-white");
        menu.addSeparator();
        addMenuItem(menu, "Done", KeyEvent.VK_D, "setup-done");
        return menu;
    }

    private JMenu createScoreMenu()
    {
        JMenu menu = new JMenu("Score");
        menu.setMnemonic(KeyEvent.VK_S);
        addMenuItem(menu, "Done", KeyEvent.VK_D, "score-done");
        return menu;
    }
}

//-----------------------------------------------------------------------------
