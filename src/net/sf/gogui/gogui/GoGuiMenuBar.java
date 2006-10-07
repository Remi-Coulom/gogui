//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gui.Bookmark;
import net.sf.gogui.gui.Clock;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.gui.RecentFileMenu;
import net.sf.gogui.util.Platform;

/** Menu bar for GoGui. */
public class GoGuiMenuBar
{
    public GoGuiMenuBar(ActionListener listener,
                        RecentFileMenu.Callback recentCallback,
                        RecentFileMenu.Callback recentGtpCallback)
    {
        m_listener = listener;
        m_menuBar = new JMenuBar();
        m_menuFile = createMenuFile(recentCallback);
        m_menuBar.add(m_menuFile);
        m_menuBar.add(createMenuGame());
        m_menuBar.add(createMenuEdit());
        m_menuBar.add(createMenuGo());
        m_menuShell = createMenuShell(recentGtpCallback);
        m_menuBar.add(m_menuShell);
        m_menuBookmarks = createMenuBookMarks();
        m_menuBar.add(m_menuBookmarks);
        m_menuSettings = createMenuSettings();
        m_menuBar.add(m_menuSettings);
        m_menuHelp = createMenuHelp();
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

    public void enableCleanup(boolean enable)
    {
        m_itemCleanup.setEnabled(enable);
    }

    public void enableFindNext(boolean enable)
    {
        m_findNextEnabled = enable;
        m_itemFindNext.setEnabled(enable);
    }

    public boolean getAnalyzeOnlySupported()
    {
        return m_itemAnalyzeOnlySupported.isSelected();
    }

    public boolean getAnalyzeSort()
    {
        return m_itemAnalyzeSort.isSelected();
    }

    public boolean getAutoNumber()
    {
        return m_itemAutoNumber.isSelected();        
    }

    public boolean getTimeStamp()
    {
        return m_itemTimeStamp.isSelected();        
    }

    public boolean getCleanup()
    {
        return m_itemCleanup.isSelected();
    }

    public boolean getCommandCompletion()
    {
        return m_itemCommandCompletion.isSelected();
    }

    public boolean getCommentFontFixed()
    {
        return m_itemCommentFontFixed.isSelected();
    }

    public boolean getBeepAfterMove()
    {
        return m_itemBeepAfterMove.isSelected();
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

    public boolean getShowAnalyze()
    {
        return m_itemShowAnalyze.isSelected();
    }

    public boolean getShowCursor()
    {
        return m_itemShowCursor.isSelected();
    }

    public boolean getShowGrid()
    {
        return m_itemShowGrid.isSelected();
    }

    public boolean getShowInfoPanel()
    {
        return m_itemShowInfoPanel.isSelected();
    }

    public boolean getShowLastMove()
    {
        return m_itemShowLastMove.isSelected();
    }

    public boolean getShowShell()
    {
        return m_itemShowShell.isSelected();
    }

    public boolean getShowSubtreeSizes()
    {
        return m_itemShowSubtreeSizes.isSelected();
    }

    public boolean getShowToolbar()
    {
        return m_itemShowToolbar.isSelected();
    }

    public boolean getShowTree()
    {
        return m_itemShowTree.isSelected();
    }

    public boolean getShowVariations()
    {
        return m_itemShowVariations.isSelected();
    }

    public void setAnalyzeOnlySupported(boolean enable)
    {
        m_itemAnalyzeOnlySupported.setSelected(enable);        
    }

    public void setAnalyzeSort(boolean enable)
    {
        m_itemAnalyzeSort.setSelected(enable);        
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
            m_menuBookmarks.addItem(item, "bookmark-" + i);
            m_bookmarkItems.add(item);
        }
    }

    public void setComputerBlack()
    {
        m_itemComputerBlack.setSelected(true);
    }

    public void setComputerBoth()
    {
        m_itemComputerBoth.setSelected(true);
    }

    public void setComputerEnabled(boolean enabled)
    {
        m_isComputerDisabled = ! enabled;
        m_menuComputerColor.setEnabled(enabled);
        m_itemComputerPlay.setEnabled(enabled);
        m_itemComputerPlaySingle.setEnabled(enabled);
        m_itemBeepAfterMove.setEnabled(enabled);
        m_itemDetachProgram.setEnabled(enabled);
        enableAll(m_menuShell, enabled);
        if (enabled)
        {
            m_recent.updateEnabled();
            m_recentGtp.updateEnabled();
        }
        m_itemShowAnalyze.setEnabled(enabled);
        m_itemShowShell.setEnabled(enabled);
        if (! enabled)
            enableCleanup(false);
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
        for (int i = 0; i < s_possibleBoardSizes.length; ++i)
            if (s_possibleBoardSizes[i] == size)
            {
                m_itemBoardSize[i].setSelected(true);
                return;
            }
        m_itemBoardSizeOther.setSelected(true);
    }

    public void setBeepAfterMove(boolean enable)
    {
        m_itemBeepAfterMove.setSelected(enable);
    }

    public void setCleanup(boolean enable)
    {
        m_itemCleanup.setSelected(enable);
    }

    public void setCommandInProgress()
    {
        assert(! m_isComputerDisabled);
        disableAll();
        m_menuFile.setEnabled(true);
        m_itemDetachProgram.setEnabled(true);
        m_itemQuit.setEnabled(true);
        m_itemInterrupt.setEnabled(true);
        m_menuComputerColor.setEnabled(true);
        m_menuHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemHelp.setEnabled(true);
        m_menuSettings.setEnabled(true);
        m_itemBeepAfterMove.setEnabled(true);
        m_itemShowCursor.setEnabled(true);
        m_itemShowGrid.setEnabled(true);
        m_itemShowInfoPanel.setEnabled(true);
        m_itemShowLastMove.setEnabled(true);
        m_itemShowToolbar.setEnabled(true);
        m_itemShowVariations.setEnabled(true);
        m_itemShowAnalyze.setEnabled(true);
        m_itemShowShell.setEnabled(true);
        m_itemShowTree.setEnabled(true);
        m_itemSaveLog.setEnabled(true);
        m_itemSaveCommands.setEnabled(true);
        m_itemCommandCompletion.setEnabled(true);
        m_itemAutoNumber.setEnabled(true);
        m_itemTimeStamp.setEnabled(true);
    }

    public void setCommandCompletion(boolean enable)
    {
        m_itemCommandCompletion.setSelected(enable);
    }
    public void setCommentFontFixed(boolean enable)
    {
        m_itemCommentFontFixed.setSelected(enable);
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
        enableAll();
        m_recent.updateEnabled();
        m_recentGtp.updateEnabled();
        m_itemInterrupt.setEnabled(false);
        m_itemSetup.setSelected(false);
        m_itemSetupBlack.setEnabled(false);
        m_itemSetupWhite.setEnabled(false);
        m_itemFindNext.setEnabled(m_findNextEnabled);
        setComputerEnabled(! m_isComputerDisabled);
    }

    public void setSetupMode()
    {
        disableAll();
        m_itemSetup.setEnabled(true);
        m_itemSetupBlack.setEnabled(true);
        m_itemSetupWhite.setEnabled(true);
        m_itemSetupBlack.setSelected(true);
        m_itemAbout.setEnabled(true);
        m_itemQuit.setEnabled(true);
        m_itemHelp.setEnabled(true);
    }

    public void setScoreMode()
    {
        disableAll();
        m_itemHelp.setEnabled(true);
        m_itemAbout.setEnabled(true);
        m_itemQuit.setEnabled(true);
    }

    public void setShowAnalyze(boolean enable)
    {
        m_itemShowAnalyze.setSelected(enable);
    }

    public void setShowCursor(boolean enable)
    {
        m_itemShowCursor.setSelected(enable);
    }

    public void setShowGrid(boolean enable)
    {
        m_itemShowGrid.setSelected(enable);
    }

    public void setShowInfoPanel(boolean enable)
    {
        m_itemShowInfoPanel.setSelected(enable);
    }

    public void setShowLastMove(boolean enable)
    {
        m_itemShowLastMove.setSelected(enable);
    }

    public void setShowShell(boolean enable)
    {
        m_itemShowShell.setSelected(enable);
    }

    public void setShowSubtreeSizes(boolean enable)
    {
        m_itemShowSubtreeSizes.setSelected(enable);
    }

    public void setShowToolbar(boolean enable)
    {
        m_itemShowToolbar.setSelected(enable);
    }

    public void setShowTree(boolean enable)
    {
        m_itemShowTree.setSelected(enable);
    }

    public void setShowVariations(boolean enable)
    {
        m_itemShowVariations.setSelected(enable);
    }

    /** Enable/disable items according to current position. */
    public void update(GameTree gameTree, Node node, Clock clock)
    {
        Node father = node.getFather();
        boolean hasFather = (father != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasNextEarlierVariation =
            (NodeUtil.getNextEarlierVariation(node) != null);
        boolean hasPrevEarlierVariation =
            (NodeUtil.getPreviousEarlierVariation(node) != null);
        boolean hasPrevVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        boolean isInMain = NodeUtil.isInMainVariation(node);
        boolean treeHasVariations = gameTree.hasVariations();
        m_itemBeginning.setEnabled(hasFather);
        m_itemBackward.setEnabled(hasFather);
        m_itemBackward10.setEnabled(hasFather);
        m_itemForward.setEnabled(hasChildren);
        m_itemForward10.setEnabled(hasChildren);
        m_itemEnd.setEnabled(hasChildren);
        m_itemGoto.setEnabled(hasFather || hasChildren);
        m_itemGotoVar.setEnabled(hasFather || hasChildren);
        m_itemNextVariation.setEnabled(hasNextVariation);
        m_itemPreviousVariation.setEnabled(hasPrevVariation);
        m_itemNextEarlierVariation.setEnabled(hasNextEarlierVariation);
        m_itemPreviousEarlierBackward.setEnabled(hasPrevEarlierVariation);
        m_itemBackToMainVar.setEnabled(! isInMain);
        m_itemTruncate.setEnabled(hasFather);
        m_itemTruncateChildren.setEnabled(hasChildren);
        m_itemMakeMainVar.setEnabled(! isInMain);
        m_itemKeepOnlyMainVar.setEnabled(isInMain && treeHasVariations);
        m_itemKeepOnlyPosition.setEnabled(hasFather || hasChildren);
        m_itemClockHalt.setEnabled(clock.isRunning());
        m_itemClockResume.setEnabled(! clock.isRunning());
        boolean canRestoreClock = clock.isInitialized()
            && (canRestoreTime(node)
                || (father != null && canRestoreTime(father)));
        m_itemClockRestore.setEnabled(canRestoreClock);
        if (! NodeUtil.isInCleanup(node))
            setCleanup(false);
    }

    private boolean m_findNextEnabled;

    private boolean m_isComputerDisabled;

    private static int s_possibleBoardSizes[] = { 9, 11, 13, 15, 17, 19 };

    private static int s_possibleHandicaps[] = { 0, 2, 3, 4, 5, 6, 7, 8, 9 };

    private static final int SHORTCUT =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private final ActionListener m_listener;

    private JCheckBoxMenuItem m_itemAutoNumber;

    private JCheckBoxMenuItem m_itemBeepAfterMove;

    private JCheckBoxMenuItem m_itemCleanup;

    private JCheckBoxMenuItem m_itemCommandCompletion;

    private JCheckBoxMenuItem m_itemShowAnalyze;

    private JCheckBoxMenuItem m_itemShowCursor;

    private JCheckBoxMenuItem m_itemShowGrid;

    private JCheckBoxMenuItem m_itemShowLastMove;

    private JCheckBoxMenuItem m_itemShowShell;

    private JCheckBoxMenuItem m_itemShowSubtreeSizes;

    private JCheckBoxMenuItem m_itemShowTree;

    private JCheckBoxMenuItem m_itemShowVariations;

    private JCheckBoxMenuItem m_itemSetup;

    private JCheckBoxMenuItem m_itemTimeStamp;

    private JMenuChecked m_menuComputerColor;

    private final JMenuChecked m_menuBookmarks;

    private final JMenuChecked m_menuFile;

    private final JMenuChecked m_menuHelp;

    private final JMenuChecked m_menuShell;

    private final JMenuChecked m_menuSettings;

    private final JMenuBar m_menuBar;

    private JMenuItem m_itemAbout;

    private JMenuItem m_itemAnalyzeOnlySupported;

    private JMenuItem m_itemAnalyzeSort;

    private JMenuItem m_itemBackToMainVar;

    private JMenuItem m_itemBackward;

    private JMenuItem m_itemBackward10;

    private JMenuItem m_itemBeginning;

    private JMenuItem[] m_itemBoardSize;

    private JMenuItem m_itemBoardSizeOther;

    private JMenuItem m_itemClockHalt;

    private JMenuItem m_itemClockRestore;

    private JMenuItem m_itemClockResume;

    private JMenuItem m_itemCommentFontFixed;

    private JMenuItem m_itemComputerBlack;

    private JMenuItem m_itemComputerBoth;

    private JMenuItem m_itemComputerNone;

    private JMenuItem m_itemComputerPlay;

    private JMenuItem m_itemComputerPlaySingle;

    private JMenuItem m_itemComputerWhite;

    private JMenuItem m_itemDetachProgram;

    private JMenuItem m_itemEnd;

    private JMenuItem m_itemFindNext;

    private JMenuItem m_itemForward;

    private JMenuItem m_itemForward10;

    private JMenuItem m_itemGameTreeLarge;

    private JMenuItem m_itemGameTreeMove;

    private JMenuItem m_itemGameTreeNormal;

    private JMenuItem m_itemGameTreeNone;

    private JMenuItem m_itemGameTreeNumber;

    private JMenuItem m_itemGameTreeSmall;

    private JMenuItem m_itemGameTreeTiny;

    private JMenuItem m_itemGoto;

    private JMenuItem m_itemGotoVar;

    private JMenuItem m_itemHelp;

    private JMenuItem m_itemInterrupt;

    private JMenuItem m_itemKeepOnlyMainVar;

    private JMenuItem m_itemKeepOnlyPosition;

    private JMenuItem m_itemMakeMainVar;

    private JMenuItem m_itemNextVariation;

    private JMenuItem m_itemNextEarlierVariation;

    private JMenuItem m_itemPreviousVariation;

    private JMenuItem m_itemPreviousEarlierBackward;

    private JMenuItem m_itemQuit;

    private JMenuItem m_itemSetupBlack;

    private JMenuItem m_itemSetupWhite;

    private JMenuItem m_itemShowInfoPanel;

    private JMenuItem m_itemShowToolbar;

    private JMenuItem m_itemSaveCommands;

    private JMenuItem m_itemSaveLog;

    private JMenuItem m_itemTruncate;

    private JMenuItem m_itemTruncateChildren;

    private JSeparator m_bookmarksSeparator;

    private RecentFileMenu m_recent;

    private RecentFileMenu m_recentGtp;

    private final ArrayList m_bookmarkItems = new ArrayList();

    private boolean canRestoreTime(Node node)
    {
        return ! Double.isNaN(node.getTimeLeft(GoColor.BLACK))
            || ! Double.isNaN(node.getTimeLeft(GoColor.WHITE))
            || node.getFather() == null;
    }

    private JMenuChecked createBoardSizeMenu()
    {
        JMenuChecked menu = createMenu("Board Size", KeyEvent.VK_S);
        ButtonGroup group = new ButtonGroup();
        int n = s_possibleBoardSizes.length;
        m_itemBoardSize = new JMenuItem[n];
        for (int i = 0; i < n; ++i)
        {
            String s = Integer.toString(s_possibleBoardSizes[i]);
            JMenuItem item = menu.addRadioItem(group, s, "board-size-" + s);
            m_itemBoardSize[i] = item;
        }
        menu.addSeparator();
        JMenuItem item =
            menu.addRadioItem(group, "Other", "board-size-other");
        m_itemBoardSizeOther = item;
        return menu;
    }

    private JMenuChecked createClockMenu()
    {
        JMenuChecked menu = createMenu("Clock", KeyEvent.VK_K);
        m_itemClockHalt = menu.addItem("Halt", KeyEvent.VK_H, "clock-halt");
        m_itemClockResume = menu.addItem("Resume", KeyEvent.VK_R,
                                         "clock-resume");
        m_itemClockRestore = menu.addItem("Restore", KeyEvent.VK_S,
                                          "clock-restore");
        return menu;
    }

    private JMenuChecked createComputerColorMenu()
    {
        ButtonGroup group = new ButtonGroup();
        JMenuChecked menu = createMenu("Computer Color", KeyEvent.VK_C);
        m_itemComputerBlack = menu.addRadioItem(group, "Black", KeyEvent.VK_B,
                                                "computer-black");
        m_itemComputerWhite = menu.addRadioItem(group, "White", KeyEvent.VK_W,
                                                "computer-white");
        m_itemComputerBoth = menu.addRadioItem(group, "Both", KeyEvent.VK_T,
                                               "computer-both");
        m_itemComputerNone = menu.addRadioItem(group, "None", KeyEvent.VK_N,
                                               "computer-none");
        return menu;
    }

    private JMenuChecked createHandicapMenu()
    {
        JMenuChecked menu = createMenu("Handicap", KeyEvent.VK_H);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < s_possibleHandicaps.length; ++i)
        {
            String s = Integer.toString(s_possibleHandicaps[i]);
            JMenuItem item = menu.addRadioItem(group, s, "handicap-" + s);
            if (s_possibleHandicaps[i] == 0)
                item.setSelected(true);
        }
        return menu;
    }

    private JMenuChecked createMenu(String name, int mnemonic)
    {
        JMenuChecked menu = new JMenuChecked(name, m_listener);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private JMenuChecked createMenuBookMarks()
    {
        JMenuChecked menu = createMenu("Bookmarks", KeyEvent.VK_B);
        menu.addItem("Add Bookmark", KeyEvent.VK_A, KeyEvent.VK_B,
                     SHORTCUT, "add-bookmark");
        menu.addItem("Edit Bookmarks...", KeyEvent.VK_E,
                     "edit-bookmarks");
        return menu;
    }

    private JMenuChecked createMenuConfigureAnalyze()
    {
        JMenuChecked menu = new JMenuChecked("Configure Analyze", m_listener);
        menu.setMnemonic(KeyEvent.VK_N);
        m_itemAnalyzeOnlySupported =
            new JCheckBoxMenuItem("Only Supported Commands");
        menu.addItem(m_itemAnalyzeOnlySupported, KeyEvent.VK_O,
                     "analyze-only-supported");
        m_itemAnalyzeSort = new JCheckBoxMenuItem("Sort Alphabetically");
        menu.addItem(m_itemAnalyzeSort, KeyEvent.VK_S, "analyze-sort");
        menu.addSeparator();
        menu.addItem("Reload Configuration", KeyEvent.VK_R, "analyze-reload");
        return menu;
    }

    private JMenuChecked createMenuConfigureBoard()
    {
        JMenuChecked menu = new JMenuChecked("Configure Board", m_listener);
        menu.setMnemonic(KeyEvent.VK_B);
        m_itemShowCursor = new JCheckBoxMenuItem("Show Cursor");
        m_itemShowCursor.setSelected(true);
        menu.addItem(m_itemShowCursor, KeyEvent.VK_C,
                     "show-cursor");
        m_itemShowGrid = new JCheckBoxMenuItem("Show Grid");
        m_itemShowGrid.setSelected(true);
        menu.addItem(m_itemShowGrid, KeyEvent.VK_G, "show-grid");
        m_itemShowLastMove = new JCheckBoxMenuItem("Show Last Move");
        m_itemShowLastMove.setSelected(true);
        menu.addItem(m_itemShowLastMove, KeyEvent.VK_L, "show-last-move");
        m_itemShowVariations = new JCheckBoxMenuItem("Show Variations");
        m_itemShowVariations.setSelected(true);
        menu.addItem(m_itemShowVariations, KeyEvent.VK_V, "show-variations");
        m_itemBeepAfterMove = new JCheckBoxMenuItem("Beep After Move");
        menu.addItem(m_itemBeepAfterMove, KeyEvent.VK_B, "beep-after-move");
        m_itemCommentFontFixed =
            new JCheckBoxMenuItem("Fixed Size Comment Font");
        menu.addItem(m_itemCommentFontFixed, KeyEvent.VK_F,
                     "comment-font-fixed");
        return menu;
    }

    private JMenuChecked createMenuConfigureShell()
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

    private JMenuChecked createMenuConfigureTree()
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

    private JMenuChecked createMenuEdit()
    {
        JMenuChecked menu = createMenu("Edit", KeyEvent.VK_E);
        menu.addItem("Find in Comments...", KeyEvent.VK_F, KeyEvent.VK_F,
                     SHORTCUT, "find-in-comments");
        m_itemFindNext =
            menu.addItem("Find Next", KeyEvent.VK_N, KeyEvent.VK_F3,
                         getFunctionKeyShortcut(), "find-next");
        m_itemFindNext.setEnabled(false);
        menu.addSeparator();
        menu.addItem("Game Info", KeyEvent.VK_G, KeyEvent.VK_I,
                     SHORTCUT, "game-info");
        menu.add(createBoardSizeMenu());
        menu.add(createHandicapMenu());
        menu.addSeparator();
        m_itemMakeMainVar =
            menu.addItem("Make Main Variation", KeyEvent.VK_M,
                         "make-main-variation");
        m_itemKeepOnlyMainVar =
            menu.addItem("Delete Side Variations", KeyEvent.VK_D,
                         "keep-only-main-variation");
        m_itemKeepOnlyPosition =
            menu.addItem("Keep Only Position", KeyEvent.VK_K,
                         "keep-only-position");
        m_itemTruncate = menu.addItem("Truncate", KeyEvent.VK_T, "truncate");
        m_itemTruncateChildren
            = menu.addItem("Truncate Children", KeyEvent.VK_C,
                           "truncate-children");
        menu.addSeparator();
        m_itemSetup = new JCheckBoxMenuItem("Setup Mode");
        menu.addItem(m_itemSetup, KeyEvent.VK_S, "setup");
        ButtonGroup group = new ButtonGroup();
        m_itemSetupBlack = menu.addRadioItem(group, "Setup Black",
                                             KeyEvent.VK_B, "setup-black");
        m_itemSetupBlack.setSelected(true);
        m_itemSetupWhite = menu.addRadioItem(group, "Setup White",
                                             KeyEvent.VK_W, "setup-white");
        return menu;
    }

    private JMenuChecked createMenuExport()
    {
        JMenuChecked menu = new JMenuChecked("Export", m_listener);
        menu.setMnemonic(KeyEvent.VK_E);
        menu.addItem("SGF Position...", KeyEvent.VK_S, "export-sgf-position");
        menu.addItem("LaTeX Main Variation...", KeyEvent.VK_L,
                     "export-latex");
        menu.addItem("LaTeX Position...", KeyEvent.VK_P,
                     "export-latex-position");
        menu.addItem("Text Position...", KeyEvent.VK_T, "export-ascii");
        return menu;
    }

    private JMenuChecked createMenuFile(RecentFileMenu.Callback callback)
    {
        JMenuChecked menu = createMenu("File", KeyEvent.VK_F);
        menu.addItem("Open...", KeyEvent.VK_O, KeyEvent.VK_O,
                     SHORTCUT, "open");
        menu.add(createRecentMenu(callback));
        menu.addItem("Save", KeyEvent.VK_S, KeyEvent.VK_S,
                     SHORTCUT, "save");
        menu.addItem("Save As...", KeyEvent.VK_A, "save-as");
        menu.addSeparator();
        menu.add(createMenuExport());
        menu.addSeparator();
        menu.addItem("Print...", KeyEvent.VK_P, KeyEvent.VK_P,
                     SHORTCUT, "print");
        menu.addSeparator();
        menu.addItem("Attach Program...", KeyEvent.VK_T, KeyEvent.VK_A,
                     SHORTCUT, "attach-program");
        m_itemDetachProgram = menu.addItem("Detach Program",
                                           KeyEvent.VK_D, "detach-program");
        menu.addSeparator();
        m_itemQuit = menu.addItem("Quit", KeyEvent.VK_Q, KeyEvent.VK_Q,
                                  SHORTCUT, "exit");
        return menu;
    }

    private JMenuChecked createMenuGame()
    {
        JMenuChecked menu = createMenu("Game", KeyEvent.VK_A);
        menu.addItem("New Game", KeyEvent.VK_N, "new-game");
        menu.addSeparator();
        m_menuComputerColor = createComputerColorMenu();
        menu.add(m_menuComputerColor);
        m_itemComputerPlay = menu.addItem("Play", KeyEvent.VK_L,
                                          KeyEvent.VK_F5,
                                          getFunctionKeyShortcut(), "play");
        m_itemComputerPlaySingle
            = menu.addItem("Play Single Move", KeyEvent.VK_S,
                           KeyEvent.VK_F5,
                           getFunctionKeyShortcut() | ActionEvent.SHIFT_MASK,
                           "play-single");
        menu.addItem("Pass", KeyEvent.VK_P, KeyEvent.VK_F2,
                     getFunctionKeyShortcut(), "pass");
        m_itemInterrupt =
            menu.addItem("Interrupt", KeyEvent.VK_T, KeyEvent.VK_ESCAPE,
                         0, "interrupt");
        menu.add(createClockMenu());
        menu.addSeparator();
        m_itemCleanup = new JCheckBoxMenuItem("Cleanup");
        m_itemCleanup.setMnemonic(KeyEvent.VK_E);
        menu.add(m_itemCleanup);
        menu.addItem("Score", KeyEvent.VK_O, "score");
        return menu;
    }

    private JMenuChecked createMenuGo()
    {
        int shiftMask = java.awt.event.InputEvent.SHIFT_MASK;
        JMenuChecked menu = createMenu("Go", KeyEvent.VK_G);
        m_itemBeginning =
            menu.addItem("Beginning", KeyEvent.VK_B, KeyEvent.VK_HOME,
                         SHORTCUT, "beginning");
        m_itemBackward10 =
            menu.addItem("Backward 10", KeyEvent.VK_W, KeyEvent.VK_LEFT,
                         SHORTCUT | ActionEvent.SHIFT_MASK,
                         "backward-10");
        m_itemBackward =
            menu.addItem("Backward", KeyEvent.VK_K, KeyEvent.VK_LEFT,
                         SHORTCUT, "backward");
        m_itemForward =
            menu.addItem("Forward", KeyEvent.VK_F, KeyEvent.VK_RIGHT,
                         SHORTCUT, "forward");
        m_itemForward10 =
            menu.addItem("Forward 10", KeyEvent.VK_R, KeyEvent.VK_RIGHT,
                         SHORTCUT | ActionEvent.SHIFT_MASK,
                         "forward-10");
        m_itemEnd =
            menu.addItem("End", KeyEvent.VK_E, KeyEvent.VK_END,
                         SHORTCUT, "end");
        m_itemGoto =
            menu.addItem("Go to Move...", KeyEvent.VK_O, KeyEvent.VK_G,
                         SHORTCUT, "goto");
        menu.addSeparator();
        m_itemNextVariation =
            menu.addItem("Next Variation", KeyEvent.VK_N,
                         KeyEvent.VK_DOWN, SHORTCUT,
                         "next-variation");
        m_itemPreviousVariation =
            menu.addItem("Previous Variation", KeyEvent.VK_P,
                         KeyEvent.VK_UP, SHORTCUT,
                         "previous-variation");
        m_itemNextEarlierVariation =
            menu.addItem("Next Earlier Variation", KeyEvent.VK_X,
                         KeyEvent.VK_DOWN, SHORTCUT | shiftMask,
                         "next-earlier-variation");
        m_itemPreviousEarlierBackward =
            menu.addItem("Previous Earlier Variation", KeyEvent.VK_L,
                         KeyEvent.VK_UP, SHORTCUT | shiftMask,
                         "previous-earlier-variation");
        m_itemBackToMainVar =
            menu.addItem("Back to Main Variation", KeyEvent.VK_M,
                         KeyEvent.VK_M, SHORTCUT,
                         "back-to-main-variation");
        m_itemGotoVar =
            menu.addItem("Go to Variation...",
                         KeyEvent.VK_V, "goto-variation");
        return menu;
    }

    private JMenuChecked createMenuHelp()
    {
        JMenuChecked menu = createMenu("Help", KeyEvent.VK_H);
        JMenuItem itemHelp =
            menu.addItem("GoGui Documentation", KeyEvent.VK_G,
                         KeyEvent.VK_F1, getFunctionKeyShortcut(), "help");
        JMenuItem itemAbout = menu.addItem("About", KeyEvent.VK_A,
                                           "about");
        m_itemHelp = itemHelp;
        m_itemAbout = itemAbout;
        return menu;
    }

    private JMenuChecked createMenuShell(RecentFileMenu.Callback callback)
    {
        JMenuChecked menu = createMenu("Shell", KeyEvent.VK_L);
        m_itemSaveLog = menu.addItem("Save Log...", KeyEvent.VK_L,
                                     "gtpshell-save");
        m_itemSaveCommands = menu.addItem("Save Commands...",
                                          KeyEvent.VK_C,
                                          "gtpshell-save-commands");
        menu.addItem("Send File...", KeyEvent.VK_F,
                     "gtpshell-send-file");
        m_recentGtp = new RecentFileMenu("Send Recent",
                                         "net/sf/gogui/recentgtpfiles",
                                         callback);
        m_recentGtp.getMenu().setMnemonic(KeyEvent.VK_R);
        menu.add(m_recentGtp.getMenu());
        return menu;
    }

    private JMenuChecked createMenuSettings()
    {
        JMenuChecked menu = createMenu("Settings", KeyEvent.VK_S);
        m_itemShowToolbar = new JCheckBoxMenuItem("Show Toolbar");
        menu.addItem(m_itemShowToolbar, KeyEvent.VK_T,
                     "show-toolbar");
        m_itemShowInfoPanel = new JCheckBoxMenuItem("Show Info Panel");
        menu.addItem(m_itemShowInfoPanel, KeyEvent.VK_I,
                     "show-info-panel");
        menu.addSeparator();
        m_itemShowTree = new JCheckBoxMenuItem("Show Tree");
        menu.addItem(m_itemShowTree, KeyEvent.VK_R, KeyEvent.VK_F7,
                     getFunctionKeyShortcut(), "show-tree");
        m_itemShowShell = new JCheckBoxMenuItem("Show Shell");
        menu.addItem(m_itemShowShell, KeyEvent.VK_S, KeyEvent.VK_F8,
                     getFunctionKeyShortcut(), "show-shell");
        m_itemShowAnalyze = new JCheckBoxMenuItem("Show Analyze");
        menu.addItem(m_itemShowAnalyze, KeyEvent.VK_A, KeyEvent.VK_F9,
                     getFunctionKeyShortcut(), "analyze");
        menu.addSeparator();
        menu.add(createMenuConfigureBoard());
        menu.add(createMenuConfigureTree());
        menu.add(createMenuConfigureShell());
        menu.add(createMenuConfigureAnalyze());
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

    private void disableAll()
    {
        for (int i = 0; i < m_menuBar.getMenuCount(); ++i)
        {
            JMenu menu = m_menuBar.getMenu(i);
            if (menu != null)
                enableAll(menu, false);
        }
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
    }

    private void enableAll(JMenu menu, boolean enabled)
    {
        for (int i = 0; i < menu.getItemCount(); ++i)
            if (menu.getItem(i) != null)
                menu.getItem(i).setEnabled(enabled);
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

    public JMenuItem addItem(JMenuItem item, int mnemonic, String command)
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

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final ActionListener m_listener;

    private final ArrayList m_mnemonics = new ArrayList();

    private static ArrayList s_accelerators = new ArrayList();
}

