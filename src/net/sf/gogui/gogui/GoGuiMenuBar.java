//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gui.Bookmark;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.gui.RecentFileMenu;
import net.sf.gogui.util.Platform;

/** Menu bar for GoGui. */
public class GoGuiMenuBar
{
    public interface BookmarkListener
    {
        void actionGotoBookmark(int i);
    }

    public GoGuiMenuBar(ActionListener listener, GoGuiActions actions,
                        RecentFileMenu.Callback recentCallback,
                        RecentFileMenu.Callback recentGtpCallback,
                        GoGuiMenuBar.BookmarkListener bookmarkListener)
    {
        m_listener = listener;
        m_bookmarkListener = bookmarkListener;
        m_menuBar = new JMenuBar();
        m_menuFile = createMenuFile(actions, recentCallback);
        m_menuBar.add(m_menuFile);
        m_menuBar.add(createMenuGame(actions));
        m_menuBar.add(createMenuEdit(actions));
        m_menuBar.add(createMenuGo(actions));
        m_menuShell = createMenuShell(actions, recentGtpCallback);
        m_menuBar.add(m_menuShell);
        m_menuBookmarks = createMenuBookMarks(actions);
        m_menuBar.add(m_menuBookmarks);
        m_menuSettings = createMenuSettings(actions);
        m_menuBar.add(m_menuSettings);
        m_menuHelp = createMenuHelp(actions);
        m_menuBar.add(m_menuHelp);
        setHeaderStyleSingle(true);
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
        m_recent.add(file);
        m_recent.updateEnabled();
    }

    public void addRecentGtp(File file)
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
        m_recentGtp.add(file);
        m_recentGtp.updateEnabled();
    }

    public boolean getAutoNumber()
    {
        return m_itemAutoNumber.isSelected();        
    }

    public boolean getTimeStamp()
    {
        return m_itemTimeStamp.isSelected();        
    }

    public boolean getCommandCompletion()
    {
        return m_itemCommandCompletion.isSelected();
    }

    public int getGameTreeLabels()
    {
        if (m_itemGameTreeNumber.isSelected())
            return GameTreePanel.LABEL_NUMBER;
        if (m_itemGameTreeMove.isSelected())
            return GameTreePanel.LABEL_MOVE;
        return GameTreePanel.LABEL_NONE;
    }

    public int getGameTreeSize()
    {
        if (m_itemGameTreeLarge.isSelected())
            return GameTreePanel.SIZE_LARGE;
        if (m_itemGameTreeSmall.isSelected())
            return GameTreePanel.SIZE_SMALL;
        if (m_itemGameTreeTiny.isSelected())
            return GameTreePanel.SIZE_TINY;
        return GameTreePanel.SIZE_NORMAL;
    }

    public JMenuBar getMenuBar()
    {
        return m_menuBar;
    }

    public boolean getShowSubtreeSizes()
    {
        return m_itemShowSubtreeSizes.isSelected();
    }

    public void setAutoNumber(boolean enable)
    {
        m_itemAutoNumber.setSelected(enable);        
    }

    public void setBookmarks(ArrayList bookmarks)
    {
        for (int i = 0; i < m_bookmarkItems.size(); ++i)
            m_menuBookmarks.remove((JMenuItem)m_bookmarkItems.get(i));
        if (m_bookmarksSeparator != null)
        {
            m_menuBookmarks.remove(m_bookmarksSeparator);
            m_bookmarksSeparator = null;
        }
        if (bookmarks.size() == 0)
            return;
        m_bookmarksSeparator = new JSeparator();
        m_menuBookmarks.add(m_bookmarksSeparator);
        for (int i = 0; i < bookmarks.size(); ++i)
        {
            Bookmark bookmark = (Bookmark)bookmarks.get(i);
            JMenuItem item = new JMenuItem(bookmark.m_name);
            final int bookmarkIndex = i;
            item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        m_bookmarkListener.actionGotoBookmark(bookmarkIndex);
                    } } );
            m_menuBookmarks.add(item);
            m_bookmarkItems.add(item);
        }
    }

    public void setCommandCompletion(boolean enable)
    {
        m_itemCommandCompletion.setSelected(enable);
    }
    public void setGameTreeLabels(int mode)
    {
        switch (mode)
        {
        case GameTreePanel.LABEL_NUMBER:
            m_itemGameTreeNumber.setSelected(true);
            break;
        case GameTreePanel.LABEL_MOVE:
            m_itemGameTreeMove.setSelected(true);
            break;
        case GameTreePanel.LABEL_NONE:
            m_itemGameTreeNone.setSelected(true);
            break;
        default:
            break;
        }
    }

    public void setGameTreeSize(int mode)
    {
        switch (mode)
        {
        case GameTreePanel.SIZE_LARGE:
            m_itemGameTreeLarge.setSelected(true);
            break;
        case GameTreePanel.SIZE_NORMAL:
            m_itemGameTreeNormal.setSelected(true);
            break;
        case GameTreePanel.SIZE_SMALL:
            m_itemGameTreeSmall.setSelected(true);
            break;
        case GameTreePanel.SIZE_TINY:
            m_itemGameTreeTiny.setSelected(true);
            break;
        default:
            break;
        }
    }

    /** Is it a single menu bar or does a tool bar exist? */
    public void setHeaderStyleSingle(boolean isSingle)
    {
        // For com.jgoodies.looks
        getMenuBar().putClientProperty("jgoodies.headerStyle",
                                       isSingle ? "Single" : "Both");
    }

    public void setTimeStamp(boolean enable)
    {
        m_itemTimeStamp.setSelected(enable);        
    }

    public void setNormalMode()
    {
        m_recent.updateEnabled();
        m_recentGtp.updateEnabled();
    }

    public void setShowSubtreeSizes(boolean enable)
    {
        m_itemShowSubtreeSizes.setSelected(enable);
    }

    private boolean m_findNextEnabled;

    private boolean m_isComputerDisabled;

    private static final int SHORTCUT =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private final ActionListener m_listener;

    private final BookmarkListener m_bookmarkListener;

    private JCheckBoxMenuItem m_itemAutoNumber;

    private JCheckBoxMenuItem m_itemCommandCompletion;

    private JCheckBoxMenuItem m_itemShowSubtreeSizes;

    private JCheckBoxMenuItem m_itemTimeStamp;

    private JMenuChecked m_menuComputerColor;

    private final JMenuChecked m_menuBookmarks;

    private final JMenuChecked m_menuFile;

    private final JMenuChecked m_menuHelp;

    private final JMenuChecked m_menuShell;

    private final JMenuChecked m_menuSettings;

    private final JMenuBar m_menuBar;

    private JMenuItem m_itemGameTreeLarge;

    private JMenuItem m_itemGameTreeMove;

    private JMenuItem m_itemGameTreeNormal;

    private JMenuItem m_itemGameTreeNone;

    private JMenuItem m_itemGameTreeNumber;

    private JMenuItem m_itemGameTreeSmall;

    private JMenuItem m_itemGameTreeTiny;

    private JSeparator m_bookmarksSeparator;

    private RecentFileMenu m_recent;

    private RecentFileMenu m_recentGtp;

    private final ArrayList m_bookmarkItems = new ArrayList();

    private JMenuChecked createBoardSizeMenu(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Board Size", KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        menu.addRadioItem(group, actions.m_actionBoardSize9);
        menu.addRadioItem(group, actions.m_actionBoardSize11);
        menu.addRadioItem(group, actions.m_actionBoardSize13);
        menu.addRadioItem(group, actions.m_actionBoardSize15);
        menu.addRadioItem(group, actions.m_actionBoardSize17);
        menu.addRadioItem(group, actions.m_actionBoardSize19);
        menu.addRadioItem(group, actions.m_actionBoardSizeOther);
        return menu;
    }

    private JMenuChecked createClockMenu(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Clock", KeyEvent.VK_K);
        menu.addItem(actions.m_actionClockHalt, KeyEvent.VK_H);
        menu.addItem(actions.m_actionClockResume, KeyEvent.VK_R);
        menu.addItem(actions.m_actionClockRestore, KeyEvent.VK_S);
        return menu;
    }

    private JMenuChecked createComputerColorMenu(GoGuiActions actions)
    {
        ButtonGroup group = new ButtonGroup();
        JMenuChecked menu = createMenu("Computer Color", KeyEvent.VK_C);
        menu.addRadioItem(group, actions.m_actionComputerBlack, KeyEvent.VK_B);
        menu.addRadioItem(group, actions.m_actionComputerWhite, KeyEvent.VK_W);
        menu.addRadioItem(group, actions.m_actionComputerBoth, KeyEvent.VK_T);
        menu.addRadioItem(group, actions.m_actionComputerNone, KeyEvent.VK_N);
        return menu;
    }

    private JMenuChecked createHandicapMenu(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Handicap", KeyEvent.VK_H);
        ButtonGroup group = new ButtonGroup();
        menu.addRadioItem(group, actions.m_actionHandicapNone);
        menu.addRadioItem(group, actions.m_actionHandicap2);
        menu.addRadioItem(group, actions.m_actionHandicap3);
        menu.addRadioItem(group, actions.m_actionHandicap4);
        menu.addRadioItem(group, actions.m_actionHandicap5);
        menu.addRadioItem(group, actions.m_actionHandicap6);
        menu.addRadioItem(group, actions.m_actionHandicap7);
        menu.addRadioItem(group, actions.m_actionHandicap8);
        menu.addRadioItem(group, actions.m_actionHandicap9);
        return menu;
    }

    private JMenuChecked createMenu(String name, int mnemonic)
    {
        JMenuChecked menu = new JMenuChecked(name, m_listener);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private JMenuChecked createMenuBookMarks(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Bookmarks", KeyEvent.VK_B);
        menu.addItem(actions.m_actionAddBookmark, KeyEvent.VK_A);
        menu.addItem(actions.m_actionEditBookmarks, KeyEvent.VK_E);
        return menu;
    }

    private JMenuChecked createMenuConfigureBoard(GoGuiActions actions)
    {
        JMenuChecked menu = new JMenuChecked("Configure Board", m_listener);
        menu.setMnemonic(KeyEvent.VK_B);
        GoGuiCheckBoxMenuItem itemShowCursor =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowCursor);
        menu.addItem(itemShowCursor, KeyEvent.VK_C);
        GoGuiCheckBoxMenuItem itemShowGrid =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowGrid);
        menu.addItem(itemShowGrid, KeyEvent.VK_G);
        GoGuiCheckBoxMenuItem itemShowLastMove =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowLastMove);
        menu.addItem(itemShowLastMove, KeyEvent.VK_L);
        GoGuiCheckBoxMenuItem itemShowVariations =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowVariations);
        menu.addItem(itemShowVariations, KeyEvent.VK_V);
        GoGuiCheckBoxMenuItem itemBeepAfterMove =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleBeepAfterMove);
        menu.addItem(itemBeepAfterMove, KeyEvent.VK_B);
        GoGuiCheckBoxMenuItem itemToggleCommentMonoFont =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleCommentMonoFont);
        menu.addItem(itemToggleCommentMonoFont, KeyEvent.VK_F);
        return menu;
    }

    private JMenuChecked createMenuConfigureShell(GoGuiActions actions)
    {
        JMenuChecked menu = new JMenuChecked("Configure Shell", m_listener);
        menu.setMnemonic(KeyEvent.VK_H);
        m_itemCommandCompletion = new JCheckBoxMenuItem("Popup Completions");
        menu.addItem(m_itemCommandCompletion, KeyEvent.VK_P,
                     "command-completion");
        m_itemAutoNumber = new JCheckBoxMenuItem("Auto Number");
        menu.addItem(m_itemAutoNumber, KeyEvent.VK_A, "auto-number");
        m_itemTimeStamp = new JCheckBoxMenuItem("Timestamp");
        menu.addItem(m_itemTimeStamp, KeyEvent.VK_T, "timestamp");
        return menu;
    }

    private JMenuChecked createMenuConfigureTree(GoGuiActions actions)
    {
        JMenuChecked menu = new JMenuChecked("Configure Tree", m_listener);
        menu.setMnemonic(KeyEvent.VK_E);
        JMenuChecked menuLabel = createMenu("Labels", KeyEvent.VK_L);
        ButtonGroup group = new ButtonGroup();
        m_itemGameTreeNumber =
            menuLabel.addRadioItem(group, "Move Number", KeyEvent.VK_N,
                                   "gametree-number");
        m_itemGameTreeMove =
            menuLabel.addRadioItem(group, "Move", KeyEvent.VK_M,
                                   "gametree-move");
        m_itemGameTreeNone =
            menuLabel.addRadioItem(group, "None", KeyEvent.VK_O,
                                   "gametree-none");
        menu.add(menuLabel);
        JMenuChecked menuSize = createMenu("Size", KeyEvent.VK_S);
        group = new ButtonGroup();
        m_itemGameTreeLarge =
            menuSize.addRadioItem(group, "Large", KeyEvent.VK_L,
                                  "gametree-large");
        m_itemGameTreeNormal =
            menuSize.addRadioItem(group, "Normal", KeyEvent.VK_N,
                                  "gametree-normal");
        m_itemGameTreeSmall =
            menuSize.addRadioItem(group, "Small", KeyEvent.VK_S,
                                  "gametree-small");
        m_itemGameTreeTiny =
            menuSize.addRadioItem(group, "Tiny", KeyEvent.VK_T,
                                  "gametree-tiny");
        menu.add(menuSize);
        m_itemShowSubtreeSizes = new JCheckBoxMenuItem("Show Subtree Sizes");
        menu.addItem(m_itemShowSubtreeSizes, KeyEvent.VK_S,
                     "gametree-show-subtree-sizes");
        return menu;
    }

    private JMenuChecked createMenuEdit(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Edit", KeyEvent.VK_E);
        menu.addItem(actions.m_actionFind, KeyEvent.VK_F);
        menu.addItem(actions.m_actionFindNext, KeyEvent.VK_N);
        menu.addSeparator();
        menu.addItem(actions.m_actionGameInfo, KeyEvent.VK_G);
        menu.add(createBoardSizeMenu(actions));
        menu.add(createHandicapMenu(actions));
        menu.addSeparator();
        menu.addItem(actions.m_actionMakeMainVariation, KeyEvent.VK_M);
        menu.addItem(actions.m_actionDeleteSideVariations, KeyEvent.VK_D);
        menu.addItem(actions.m_actionKeepOnlyPosition, KeyEvent.VK_K);
        menu.addItem(actions.m_actionTruncate, KeyEvent.VK_T);
        menu.addItem(actions.m_actionTruncateChildren, KeyEvent.VK_C);
        menu.addSeparator();
        menu.addItem(actions.m_actionSetup, KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        menu.addRadioItem(group, actions.m_actionSetupBlack, KeyEvent.VK_B);
        menu.addRadioItem(group, actions.m_actionSetupWhite, KeyEvent.VK_W);
        return menu;
    }

    private JMenuChecked createMenuExport(GoGuiActions actions)
    {
        JMenuChecked menu = new JMenuChecked("Export", m_listener);
        menu.setMnemonic(KeyEvent.VK_E);
        menu.addItem(actions.m_actionExportSgfPosition, KeyEvent.VK_S);
        menu.addItem(actions.m_actionExportLatexMainVariation, KeyEvent.VK_L);
        menu.addItem(actions.m_actionExportLatexPosition, KeyEvent.VK_P);
        menu.addItem(actions.m_actionExportTextPosition, KeyEvent.VK_T);
        menu.addItem(actions.m_actionExportTextPositionToClipboard,
                     KeyEvent.VK_C);
        return menu;
    }

    private JMenuChecked createMenuFile(GoGuiActions actions,
                                        RecentFileMenu.Callback callback)
    {
        JMenuChecked menu = createMenu("File", KeyEvent.VK_F);
        menu.addItem(actions.m_actionOpen, KeyEvent.VK_O);
        menu.add(createRecentMenu(callback));
        menu.addItem(actions.m_actionSave, KeyEvent.VK_S);
        menu.addItem(actions.m_actionSaveAs, KeyEvent.VK_A);
        menu.addSeparator();
        menu.add(createMenuImport(actions));
        menu.add(createMenuExport(actions));
        menu.addSeparator();
        menu.addItem(actions.m_actionPrint, KeyEvent.VK_P);
        menu.addSeparator();
        menu.addItem(actions.m_actionAttachProgram, KeyEvent.VK_T);
        menu.addItem(actions.m_actionDetachProgram, KeyEvent.VK_D);
        menu.addItem(actions.m_actionQuit, KeyEvent.VK_Q);
        return menu;
    }

    private JMenuChecked createMenuGame(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Game", KeyEvent.VK_A);
        menu.addItem(actions.m_actionNewGame, KeyEvent.VK_N);
        menu.addSeparator();
        m_menuComputerColor = createComputerColorMenu(actions);
        menu.add(m_menuComputerColor);
        menu.addItem(actions.m_actionPlay, KeyEvent.VK_L);
        menu.addItem(actions.m_actionPlaySingleMove, KeyEvent.VK_S);
        menu.addItem(actions.m_actionInterrupt, KeyEvent.VK_T);
        menu.addSeparator();
        menu.addItem(actions.m_actionPass, KeyEvent.VK_P);
        menu.add(createClockMenu(actions));
        menu.addItem(actions.m_actionScore, KeyEvent.VK_O);
        return menu;
    }

    private JMenuChecked createMenuGo(GoGuiActions actions)
    {
        int shiftMask = java.awt.event.InputEvent.SHIFT_MASK;
        JMenuChecked menu = createMenu("Go", KeyEvent.VK_G);
        menu.addItem(actions.m_actionBeginning, KeyEvent.VK_B);
        menu.addItem(actions.m_actionBackwardTen, KeyEvent.VK_W);
        menu.addItem(actions.m_actionBackward, KeyEvent.VK_K);
        menu.addItem(actions.m_actionForward, KeyEvent.VK_F);
        menu.addItem(actions.m_actionForwardTen, KeyEvent.VK_R);
        menu.addItem(actions.m_actionEnd, KeyEvent.VK_E);
        menu.addItem(actions.m_actionGoto, KeyEvent.VK_O);
        menu.addSeparator();
        menu.addItem(actions.m_actionNextVariation, KeyEvent.VK_N);
        menu.addItem(actions.m_actionPreviousVariation, KeyEvent.VK_P);
        menu.addItem(actions.m_actionNextEarlierVariation, KeyEvent.VK_X);
        menu.addItem(actions.m_actionPreviousEarlierVariation, KeyEvent.VK_L);
        menu.addItem(actions.m_actionBackToMainVariation, KeyEvent.VK_M);
        menu.addItem(actions.m_actionGotoVariation, KeyEvent.VK_V);
        return menu;
    }

    private JMenuChecked createMenuHelp(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Help", KeyEvent.VK_H);
        menu.addItem(actions.m_actionDocumentation, KeyEvent.VK_G);
        menu.addItem(actions.m_actionAbout, KeyEvent.VK_A);
        return menu;
    }

    private JMenuChecked createMenuImport(GoGuiActions actions)
    {
        JMenuChecked menu = new JMenuChecked("Import", m_listener);
        menu.setMnemonic(KeyEvent.VK_I);
        menu.addItem(actions.m_actionImportTextPosition, KeyEvent.VK_T);
        menu.addItem(actions.m_actionImportTextPositionFromClipboard,
                     KeyEvent.VK_C);
        return menu;
    }

    private JMenuChecked createMenuShell(GoGuiActions actions,
                                         RecentFileMenu.Callback callback)
    {
        JMenuChecked menu = createMenu("Shell", KeyEvent.VK_L);
        menu.addItem(actions.m_actionShellSave, KeyEvent.VK_L);
        menu.addItem(actions.m_actionShellSaveCommands, KeyEvent.VK_C);
        menu.addItem(actions.m_actionShellSendFile, KeyEvent.VK_F);
        m_recentGtp = new RecentFileMenu("Send Recent",
                                         "net/sf/gogui/recentgtpfiles",
                                         callback);
        m_recentGtp.getMenu().setMnemonic(KeyEvent.VK_R);
        menu.add(m_recentGtp.getMenu());
        return menu;
    }

    private JMenuChecked createMenuSettings(GoGuiActions actions)
    {
        JMenuChecked menu = createMenu("Settings", KeyEvent.VK_S);
        GoGuiCheckBoxMenuItem itemToggleShowToolbar =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowToolbar);
        menu.addItem(itemToggleShowToolbar, KeyEvent.VK_T);
        GoGuiCheckBoxMenuItem itemToggleShowInfoPanel =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowInfoPanel);
        menu.addItem(itemToggleShowInfoPanel, KeyEvent.VK_I);
        menu.addSeparator();
        GoGuiCheckBoxMenuItem itemToggleShowTree =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowTree);
        menu.addItem(itemToggleShowTree, KeyEvent.VK_R);
        GoGuiCheckBoxMenuItem itemToggleShowShell =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowShell);
        menu.addItem(itemToggleShowShell, KeyEvent.VK_S);
        GoGuiCheckBoxMenuItem itemToggleShowAnalyzeDialog =
            new GoGuiCheckBoxMenuItem(actions.m_actionToggleShowAnalyzeDialog);
        menu.addItem(itemToggleShowAnalyzeDialog, KeyEvent.VK_A);
        menu.addSeparator();
        menu.add(createMenuConfigureBoard(actions));
        menu.add(createMenuConfigureTree(actions));
        menu.add(createMenuConfigureShell(actions));
        return menu;
    }

    private JMenu createRecentMenu(RecentFileMenu.Callback callback)
    {
        m_recent = new RecentFileMenu("Open Recent",
                                      "net/sf/gogui/recentfiles",
                                      callback);
        JMenu menu = m_recent.getMenu();
        menu.setMnemonic(KeyEvent.VK_R);
        return menu;
    }

    /** Get shortcut modifier for function keys.
        Returns 0, unless platform is Mac.
    */
    private static int getFunctionKeyShortcut()
    {
        if (Platform.isMac())
            return SHORTCUT;
        return 0;
    }
}

/** Menu with assertions for unique mnemonics and accelerators. */
class JMenuChecked
    extends JMenu
{
    public JMenuChecked(String text, ActionListener listener)
    {
        super(text);
        m_listener = listener;
    }

    public JMenuItem addItem(JMenuItem item, String command)
    {
        item.addActionListener(m_listener);
        item.setActionCommand(command);
        add(item);
        return item;
    }

    public JMenuItem addItem(JMenuItem item, int mnemonic)
    {
        item.setIcon(null);
        setMnemonic(item, mnemonic);
        add(item);
        return item;
    }

    public JMenuItem addItem(AbstractAction action, int mnemonic)
    {
        JMenuItem item = new JMenuItem(action);
        item.setIcon(null);
        setMnemonic(item, mnemonic);
        add(item);
        return item;
    }

    public JMenuItem addItem(JMenuItem item, int mnemonic, String command)
    {
        setMnemonic(item, mnemonic);
        return addItem(item, command);
    }

    public JMenuItem addItem(String label, int mnemonic, String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addItem(item, mnemonic, command);        
    }

    public JMenuItem addItem(JMenuItem item, int mnemonic, int accel,
                             int modifier, String command)
    {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(accel, modifier); 
        assert(! s_accelerators.contains(keyStroke));
        s_accelerators.add(keyStroke);
        item.setAccelerator(keyStroke);
        return addItem(item, mnemonic, command);
    }

    public JMenuItem addItem(String label, int mnemonic, int accel,
                             int modifier, String command)
    {
        return addItem(new JMenuItem(label), mnemonic, accel, modifier,
                       command);
    }

    public JMenuItem addRadioItem(ButtonGroup group, String label,
                                  String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addItem(item, command);
    }

    public JMenuItem addRadioItem(ButtonGroup group, String label,
                                  int mnemonic, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addItem(item, mnemonic, command);
    }

    public JMenuItem addRadioItem(ButtonGroup group, AbstractAction action,
                                  int mnemonic)
    {
        JMenuItem item = addRadioItem(group, action);
        setMnemonic(item, mnemonic);
        return item;
    }

    public JMenuItem addRadioItem(ButtonGroup group, AbstractAction action)
    {
        JMenuItem item = new GoGuiRadioButtonMenuItem(action);
        group.add(item);
        item.setIcon(null);
        add(item);
        return item;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final ArrayList m_mnemonics = new ArrayList();

    private static ArrayList s_accelerators = new ArrayList();

    private void setMnemonic(JMenuItem item, int mnemonic)
    {
        item.setMnemonic(mnemonic);
        Integer integer = new Integer(mnemonic);
        if (m_mnemonics.contains(integer))
        {
            System.err.println("Warning: duplicate mnemonic item "
                               + item.getText());
            assert(false);
        }
        m_mnemonics.add(integer);
    }
}

/** Radio menu item with additional "selected" action property. */
class GoGuiRadioButtonMenuItem
    extends JRadioButtonMenuItem
{
    public GoGuiRadioButtonMenuItem(AbstractAction action)
    {
        super(action);
        action.addPropertyChangeListener(new PropertyChangeListener() {
                public void  propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("selected"))
                        setSelected(((Boolean)e.getNewValue()).booleanValue());
                } } );
    }
}

/** Checkbox item with additional "selected" action property. */
class GoGuiCheckBoxMenuItem
    extends JCheckBoxMenuItem
{
    public GoGuiCheckBoxMenuItem(AbstractAction action)
    {
        super(action);
        action.addPropertyChangeListener(new PropertyChangeListener() {
                public void  propertyChange(PropertyChangeEvent e) {
                    if (e.getPropertyName().equals("selected"))
                        setSelected(((Boolean)e.getNewValue()).booleanValue());
                } } );
    }
}
