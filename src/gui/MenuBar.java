//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import go.*;

//-----------------------------------------------------------------------------

class MenuBar
    implements ActionListener
{
    public MenuBar(ActionListener listener)
    {
        m_listener = listener;
        m_menuBar = new JMenuBar();
        m_menuFile = createFileMenu();
        m_menuBar.add(m_menuFile);
        m_menuGame = createGameMenu();
        m_menuBar.add(m_menuGame);
        m_menuSetup = createSetupMenu();
        m_menuBar.add(m_menuSetup);
        m_menuSettings = createSettingsMenu();
        m_menuBar.add(m_menuSettings);
        m_menuWindows = createWindowsMenu();
        m_menuBar.add(m_menuWindows);
        m_menuHelp = createHelpMenu();
        m_menuBar.add(m_menuHelp);
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.startsWith("open-recent-"))
        {
            try
            {
                String indexString =
                    command.substring("open-recent-".length());
                int index = Integer.parseInt(indexString);
                m_selectedRecent = m_recent[index];
                assert(m_selectedRecent != null);
                m_listener.actionPerformed(new ActionEvent(this, 0,
                                                           "open-recent"));
            }
            catch (NumberFormatException e)
            {
                assert(false);
                return;
            }
        }
        else
            assert(false);
    }

    public void addRecent(File file)
    {
        try
        {
            File canonicalFile = file.getCanonicalFile();
            if (canonicalFile.exists())
                file = canonicalFile;
        }
        catch (IOException e)
        {
        }
        for (int i = 0; i < m_maxRecent; ++i)
        {
            if (m_recent[i] == null)
                break;
            if (m_recent[i].equals(file))
            {
                for (int j = i; j > 0; --j)
                    m_recent[j] = m_recent[j - 1];
                m_recent[0] = file;
                updateRecentMenu();
                return;
            }
        }
        for (int i = m_maxRecent - 1; i > 0; --i)
            m_recent[i] = m_recent[i - 1];
        m_recent[0] = file;
        updateRecentMenu();
    }

    public void disableComputer()
    {
        m_isComputerDisabled = true;
    }

    public boolean getBeepAfterMove()
    {
        return m_itemBeepAfterMove.isSelected();
    }

    public boolean getRememberSizes()
    {
        return m_itemRememberSizes.isSelected();
    }

    public JMenuBar getMenuBar()
    {
        return m_menuBar;
    }

    public File getSelectedRecent()
    {
        return m_selectedRecent;
    }

    public boolean getShowCursor()
    {
        return m_itemShowCursor.isSelected();
    }

    public boolean getShowLastMove()
    {
        return m_itemShowLastMove.isSelected();
    }

    public void saveRecent()
    {
        File file = getRecentFile();
        PrintStream out;
        try
        {
            out = new PrintStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        for (int i = 0; i < m_maxRecent; ++i)
            if (m_recent[i] != null)
                out.println(m_recent[i].toString());
        out.close();
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

    public void selectBoardSizeItem(int size)
    {
        for (int i = 0; i < m_possibleBoardSizes.length; ++i)
            if (m_possibleBoardSizes[i] == size)
            {
                m_itemBoardSize[i].setSelected(true);
                return;
            }
        m_itemBoardSizeOther.setSelected(true);
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
        m_itemBeepAfterMove.setSelected(enable);
    }

    public void setCommandInProgress(boolean isInterruptSupported)
    {
        assert(! m_isComputerDisabled);
        disableAll();
        m_menuFile.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_menuGame.setEnabled(true);
        if (isInterruptSupported)
            m_itemInterrupt.setEnabled(true);
        m_menuComputerColor.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemHelp.setEnabled(true);
        m_menuSettings.setEnabled(true);
        m_itemBeepAfterMove.setEnabled(true);
        m_itemRememberSizes.setEnabled(true);
        m_itemShowCursor.setEnabled(true);
        m_itemShowLastMove.setEnabled(true);
        m_menuWindows.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
    }

    public void setNormalMode()
    {
        enableAll();
        m_itemInterrupt.setEnabled(false);
        m_itemSetup.setSelected(false);
        m_itemSetupBlack.setEnabled(false);
        m_itemSetupWhite.setEnabled(false);
        if (m_isComputerDisabled)
        {
            disableMenu(m_menuWindows);
            m_menuComputerColor.setEnabled(false);
            m_itemComputerPlay.setEnabled(false);
            m_itemBeepAfterMove.setEnabled(false);
        }
    }

    public void setRememberSizes(boolean enable)
    {
        m_itemRememberSizes.setSelected(enable);
    }

    public void setSetupMode()
    {
        disableAll();
        m_menuSetup.setEnabled(true);
        m_itemSetup.setEnabled(true);
        m_itemSetupBlack.setEnabled(true);
        m_itemSetupWhite.setEnabled(true);
        m_itemSetupBlack.setSelected(true);
        m_menuFile.setEnabled(true);
        m_menuWindows.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
        m_itemHelp.setEnabled(true);
        if (m_isComputerDisabled)
            disableMenu(m_menuWindows);
    }

    public void setScoreMode()
    {
        disableAll();
        m_menuFile.setEnabled(true);
        m_menuWindows.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemExit.setEnabled(true);
        m_itemGtpShell.setEnabled(true);
        m_itemHelp.setEnabled(true);
        if (m_isComputerDisabled)
            disableMenu(m_menuWindows);
    }

    public void updateGameMenuItems(go.Board board)
    {
        int moveNumber = board.getMoveNumber();
        int numberSavedMoves = board.getNumberSavedMoves();
        m_itemBeginning.setEnabled(moveNumber > 0);
        m_itemBackward.setEnabled(moveNumber > 0);
        m_itemBackward10.setEnabled(moveNumber > 0);
        m_itemForward.setEnabled(moveNumber < numberSavedMoves);
        m_itemForward10.setEnabled(moveNumber < numberSavedMoves);
        m_itemEnd.setEnabled(moveNumber < numberSavedMoves);
        m_itemGoto.setEnabled(numberSavedMoves > 0);
        m_itemTruncate.setEnabled(moveNumber < numberSavedMoves);
    }

    private boolean m_isComputerDisabled;

    private final int m_maxRecent = 20;

    private int m_numberRecent;

    private static int m_possibleBoardSizes[] = { 9, 11, 13, 15, 17, 19 };

    private static int m_possibleHandicaps[] = { 0, 2, 3, 4, 5, 6, 7, 8, 9 };

    private ActionListener m_listener;

    private File[] m_recent = new File[m_maxRecent];

    private File m_selectedRecent;

    private JCheckBoxMenuItem m_itemBeepAfterMove;

    private JCheckBoxMenuItem m_itemRememberSizes;

    private JCheckBoxMenuItem m_itemShowCursor;

    private JCheckBoxMenuItem m_itemShowLastMove;

    private JCheckBoxMenuItem m_itemSetup;

    private JMenu m_menuComputerColor;

    private JMenu m_menuFile;

    private JMenu m_menuGame;

    private JMenu m_menuHelp;

    private JMenu m_menuRecent;

    private JMenu m_menuSettings;

    private JMenu m_menuSetup;

    private JMenuBar m_menuBar;

    private JMenu m_menuWindows;

    private JMenuItem m_itemAbout;

    private JMenuItem m_itemBackward;

    private JMenuItem m_itemBackward10;

    private JMenuItem m_itemBeginning;

    private JMenuItem[] m_itemBoardSize;

    private JMenuItem m_itemBoardSizeOther;

    private JMenuItem m_itemComputerBlack;

    private JMenuItem m_itemComputerBoth;

    private JMenuItem m_itemComputerNone;

    private JMenuItem m_itemComputerPlay;

    private JMenuItem m_itemComputerWhite;

    private JMenuItem m_itemEnd;

    private JMenuItem m_itemExit;

    private JMenuItem m_itemForward;

    private JMenuItem m_itemForward10;

    private JMenuItem m_itemGoto;

    private JMenuItem m_itemGtpShell;

    private JMenuItem m_itemHelp;

    private JMenuItem m_itemInterrupt;

    private JMenuItem m_itemRulesChinese;

    private JMenuItem m_itemRulesJapanese;

    private JMenuItem m_itemSetupBlack;

    private JMenuItem m_itemSetupWhite;

    private JMenuItem m_itemTruncate;

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

    private JMenu createBoardSizeMenu()
    {
        JMenu menu = new JMenu("Size");
        menu.setMnemonic(KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        int n = m_possibleBoardSizes.length;
        m_itemBoardSize = new JMenuItem[n];
        for (int i = 0; i < n; ++i)
        {
            String s = Integer.toString(m_possibleBoardSizes[i]);
            JMenuItem item = addRadioItem(menu, group, s, "board-size-" + s);
            m_itemBoardSize[i] = item;
        }
        menu.addSeparator();
        JMenuItem item = addRadioItem(menu, group, "Other",
                                      "board-size-other");
        m_itemBoardSizeOther = item;
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
                    "open");
        menu.add(createRecentMenu());
        addMenuItem(menu, "Save...", KeyEvent.VK_S, KeyEvent.VK_S,
                    ActionEvent.CTRL_MASK,
                    "save");
        addMenuItem(menu, "Save position...", KeyEvent.VK_T, "save-position");
        menu.addSeparator();
        addMenuItem(menu, "Print...", KeyEvent.VK_P, KeyEvent.VK_P,
                    ActionEvent.CTRL_MASK,
                    "print");
        menu.addSeparator();
        addMenuItem(menu, "Open with program...", KeyEvent.VK_W,
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
        menu.add(createBoardSizeMenu());
        addMenuItem(menu, "Komi...", KeyEvent.VK_K, "komi");
        menu.add(createRulesMenu());
        menu.add(createHandicapMenu());
        m_menuComputerColor = createComputerColorMenu();
        menu.add(m_menuComputerColor);
        menu.addSeparator();
        addMenuItem(menu, "Pass", KeyEvent.VK_P, KeyEvent.VK_F2, 0, "pass");
        m_itemComputerPlay = addMenuItem(menu, "Computer play", KeyEvent.VK_L,
                                         KeyEvent.VK_F5, 0, "play");
        m_itemInterrupt =
            addMenuItem(menu, "Interrupt", KeyEvent.VK_I, KeyEvent.VK_ESCAPE,
                        0, "interrupt");
        menu.addSeparator();
        m_itemBeginning =
            addMenuItem(menu, "Beginning", KeyEvent.VK_N, KeyEvent.VK_HOME,
                        ActionEvent.CTRL_MASK, "beginning");
        m_itemBackward10 =
            addMenuItem(menu, "Backward 10", KeyEvent.VK_D, KeyEvent.VK_LEFT,
                        ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK,
                        "backward-10");
        m_itemBackward =
            addMenuItem(menu, "Backward", KeyEvent.VK_B, KeyEvent.VK_LEFT,
                        ActionEvent.CTRL_MASK, "backward");
        m_itemForward =
            addMenuItem(menu, "Forward", KeyEvent.VK_F, KeyEvent.VK_RIGHT,
                        ActionEvent.CTRL_MASK, "forward");
        m_itemForward10 =
            addMenuItem(menu, "Forward 10", KeyEvent.VK_O, KeyEvent.VK_RIGHT,
                        ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK,
                        "forward-10");
        m_itemEnd =
            addMenuItem(menu, "End", KeyEvent.VK_E, KeyEvent.VK_END,
                        ActionEvent.CTRL_MASK, "end");
        m_itemGoto =
            addMenuItem(menu, "Goto...", KeyEvent.VK_G, KeyEvent.VK_G,
                        ActionEvent.CTRL_MASK, "goto");
        m_itemTruncate = addMenuItem(menu, "Truncate...", KeyEvent.VK_T,
                                     "truncate");
        menu.addSeparator();
        addMenuItem(menu, "Score", KeyEvent.VK_R, "score");
        return menu;
    }

    private JMenu createHandicapMenu()
    {
        JMenu menu = new JMenu("Handicap");
        menu.setMnemonic(KeyEvent.VK_H);
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
        JMenuItem itemHelp =
            addMenuItem(menu, "Contents", KeyEvent.VK_C, KeyEvent.VK_F1, 0,
                        "help");
        JMenuItem itemAbout = addMenuItem(menu, "About", KeyEvent.VK_A,
                                          "about");
        m_itemHelp = itemHelp;
        m_itemAbout = itemAbout;
        return menu;
    }

    private JMenu createRecentMenu()
    {
        m_menuRecent = new JMenu("Open recent");
        m_menuRecent.setMnemonic(KeyEvent.VK_R);
        loadRecent();
        updateRecentMenu();
        return m_menuRecent;
    }

    private JMenu createRulesMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenu menu = new JMenu("Rules");
        menu.setMnemonic(KeyEvent.VK_U);
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
        menu.setMnemonic(KeyEvent.VK_S);
        m_itemBeepAfterMove = new JCheckBoxMenuItem("Beep after move");
        addMenuItem(menu, m_itemBeepAfterMove, KeyEvent.VK_B,
                    "beep-after-move");
        m_itemRememberSizes = new JCheckBoxMenuItem("Remember window sizes");
        m_itemRememberSizes.setSelected(false);
        addMenuItem(menu, m_itemRememberSizes, KeyEvent.VK_R,
                    "remember-sizes");
        m_itemShowCursor = new JCheckBoxMenuItem("Show cursor");
        m_itemShowCursor.setSelected(true);
        addMenuItem(menu, m_itemShowCursor, KeyEvent.VK_S, "show-cursor");
        m_itemShowLastMove = new JCheckBoxMenuItem("Show last move");
        m_itemShowLastMove.setSelected(true);
        addMenuItem(menu, m_itemShowLastMove, KeyEvent.VK_L,
                    "show-last-move");
        return menu;
    }

    private JMenu createSetupMenu()
    {
        JMenu menu = new JMenu("Setup");
        menu.setMnemonic(KeyEvent.VK_E);
        m_itemSetup = new JCheckBoxMenuItem("Setup mode");
        addMenuItem(menu, m_itemSetup, KeyEvent.VK_S, "setup");
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();
        m_itemSetupBlack =
            addRadioItem(menu, group, "Black", KeyEvent.VK_B, "setup-black");
        m_itemSetupBlack.setSelected(true);
        m_itemSetupWhite =
            addRadioItem(menu, group, "White", KeyEvent.VK_W, "setup-white");
        return menu;
    }

    private JMenu createWindowsMenu()
    {
        JMenu menu = new JMenu("Windows");
        menu.setMnemonic(KeyEvent.VK_W);
        addMenuItem(menu, "Analyze", KeyEvent.VK_A, KeyEvent.VK_F8, 0,
                    "analyze");
        m_itemGtpShell = addMenuItem(menu, "GTP shell", KeyEvent.VK_G,
                                     KeyEvent.VK_F9, 0, "gtp-shell");
        return menu;
    }

    private void disableAll()
    {
        for (int i = 0; i < m_menuBar.getMenuCount(); ++i)
        {
            JMenu menu = m_menuBar.getMenu(i);
            if (menu != null)
                disableMenu(menu);
        }
    }

    private void disableMenu(JMenu menu)
    {
        menu.setEnabled(false);
        for (int i = 0; i < menu.getItemCount(); ++i)
            if (menu.getItem(i) != null)
                menu.getItem(i).setEnabled(false);
    }

    private void enableAll()
    {
        for (int i = 0; i < m_menuBar.getMenuCount(); ++i)
        {
            JMenu menu = m_menuBar.getMenu(i);
            if (menu != null)
            {
                menu.setEnabled(true);
                for (int j = 0; j < menu.getItemCount(); ++j)
                    if (menu.getItem(j) != null)
                        menu.getItem(j).setEnabled(true);
            }
        }
        m_menuRecent.setEnabled(m_numberRecent > 0);
    }

    private File getRecentFile()
    {
        String home = System.getProperty("user.home");
        return new File(new File(home, ".gogui"), "recent-files");
    }

    private void loadRecent()
    {
        m_numberRecent = 0;
        File file = getRecentFile();
        BufferedReader reader;
        try
        {
            reader = new BufferedReader(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        String line;
        try
        {
            while((line = reader.readLine()) != null)
            {
                if (m_numberRecent >= m_maxRecent - 1)
                    break;
                File recent = new File(line);
                if (! recent.exists())
                    continue;
                m_recent[m_numberRecent] = recent;
                ++m_numberRecent;
            }
        }
        catch (IOException e)
        {
        }
        try
        {
            reader.close();
        }
        catch (IOException e)
        {
        }
    }

    private void updateRecentMenu()
    {
        m_menuRecent.removeAll();
        m_numberRecent = 0;
        for (int i = 0; i < m_maxRecent; ++i)
        {
            File file = m_recent[i];
            if (file == null)
                break;
            ++m_numberRecent;
            JMenuItem item = new JMenuItem(file.getName());
            item.addActionListener(this);
            item.setActionCommand("open-recent-" + i);
            m_menuRecent.add(item);
        }
        m_menuRecent.setEnabled(m_numberRecent > 0);
    }
}

//-----------------------------------------------------------------------------
