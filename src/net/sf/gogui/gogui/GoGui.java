//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstGameInformation;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.CountScore;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.go.Score;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.AnalyzeCommand;
import net.sf.gogui.gui.AnalyzeDialog;
import net.sf.gogui.gui.AnalyzeShow;
import net.sf.gogui.gui.BoardSizeDialog;
import net.sf.gogui.gui.Bookmark;
import net.sf.gogui.gui.BookmarkEditor;
import net.sf.gogui.gui.Comment;
import net.sf.gogui.gui.ConstGuiBoard;
import net.sf.gogui.gui.ContextMenu;
import net.sf.gogui.gui.FindDialog;
import net.sf.gogui.gui.GameInfo;
import net.sf.gogui.gui.GameInfoDialog;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.gui.GameTreeViewer;
import net.sf.gogui.gui.GtpShell;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtil;
import net.sf.gogui.gui.GuiGtpClient;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.gui.FileDialogs;
import net.sf.gogui.gui.Help;
import net.sf.gogui.gui.LiveGfx;
import net.sf.gogui.gui.MessageDialogs;
import net.sf.gogui.gui.ObjectListEditor;
import net.sf.gogui.gui.ParameterDialog;
import net.sf.gogui.gui.Program;
import net.sf.gogui.gui.ProgramEditor;
import net.sf.gogui.gui.RecentFileMenu;
import net.sf.gogui.gui.Session;
import net.sf.gogui.gui.ScoreDialog;
import net.sf.gogui.gui.StatusBar;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.tex.TexWriter;
import net.sf.gogui.text.TextParser;
import net.sf.gogui.text.ParseError;
import net.sf.gogui.thumbnail.ThumbnailCreator;
import net.sf.gogui.thumbnail.ThumbnailPlatform;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.ProgressShow;
import net.sf.gogui.util.StringUtil;
import net.sf.gogui.version.Version;

/** Graphical user interface to a Go program. */
public class GoGui
    extends JFrame
    implements AnalyzeDialog.Listener, GuiBoard.Listener,
               GameTreeViewer.Listener, GtpShell.Listener,
               ScoreDialog.Listener, GoGuiMenuBar.Listener,
               ContextMenu.Listener
{
    public GoGui(String program, File file, int move, String time,
                 boolean verbose, boolean initComputerColor,
                 boolean computerBlack, boolean computerWhite, boolean auto,
                 String gtpFile, String gtpCommand, String initAnalyze,
                 File analyzeCommands)
        throws GtpError, ErrorMessage
    {
        int boardSize = m_prefs.getInt("boardsize", GoPoint.DEFAULT_SIZE);
        m_beepAfterMove = m_prefs.getBoolean("beep-after-move", true);
        m_file = file;
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_analyzeCommands = analyzeCommands;
        m_move = move;
        if (initComputerColor)
        {
            m_computerBlack = computerBlack;
            m_computerWhite = computerWhite;
        }
        else if (m_prefs.getBoolean("computer-none", false))
        {
            m_computerBlack = false;
            m_computerWhite = false;
        }
        else
        {
            m_computerBlack = false;
            m_computerWhite = true;
        }
        m_auto = auto;
        m_verbose = verbose;
        m_initAnalyze = initAnalyze;
        m_showInfoPanel = true;
        m_showToolbar = false;

        Container contentPane = getContentPane();        
        m_innerPanel = new JPanel(new BorderLayout());
        contentPane.add(m_innerPanel, BorderLayout.CENTER);
        m_toolBar = new GoGuiToolBar(this);

        m_infoPanel = new JPanel(new BorderLayout());
        m_game = new Game(boardSize);
        m_gameInfo = new GameInfo(m_game);
        m_gameInfo.setBorder(GuiUtil.createSmallEmptyBorder());
        m_infoPanel.add(m_gameInfo, BorderLayout.NORTH);
        m_guiBoard = new GuiBoard(boardSize);
        m_guiBoard.setListener(this);
        m_statusBar = new StatusBar(true);
        m_innerPanel.add(m_statusBar, BorderLayout.SOUTH);
        Comment.Listener commentListener = new Comment.Listener()
            {
                public void changed(String comment)
                {
                    m_game.setComment(comment);
                    // Cannot call updateViews, which calls
                    // Comment.setComment(), in comment callback
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                updateViews(false);
                            } });
                }

                public void textSelected(String text)
                {
                    if (text == null)
                        text = "";
                    PointList points =
                        GtpUtil.parsePointString(text, getBoardSize());
                    GuiBoardUtil.showPointList(m_guiBoard, points);
                }
            };
        m_comment = new Comment(commentListener);
        boolean monoFont = m_prefs.getBoolean("comment-font-fixed", false);
        m_comment.setMonoFont(monoFont);
        m_infoPanel.add(m_comment, BorderLayout.CENTER);
        m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     m_guiBoard, m_infoPanel);
        GuiUtil.removeKeyBinding(m_splitPane, "F8");
        m_splitPane.setResizeWeight(1);
        m_innerPanel.add(m_splitPane, BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
                public void windowActivated(WindowEvent e) {
                    m_guiBoard.requestFocusInWindow();
                }

                public void windowClosing(WindowEvent event) {
                    close();
                }
            });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        GuiUtil.setGoIcon(this);
        RecentFileMenu.Listener recentListener = new RecentFileMenu.Listener()
            {
                public void fileSelected(String label, File file) {
                    actionOpenFile(file);
                }
            };
        RecentFileMenu.Listener recentGtp = new RecentFileMenu.Listener() {
                public void fileSelected(String label, File file) {
                    actionShellSendFile(file);
                }
            };
        m_menuBar = new GoGuiMenuBar(m_actions, recentListener, recentGtp,
                                     this);
        m_treeLabels = m_prefs.getInt("gametree-labels",
                                      GameTreePanel.LABEL_NUMBER);
        m_treeSize = m_prefs.getInt("gametree-size",
                                    GameTreePanel.SIZE_NORMAL);
        m_showSubtreeSizes =
            m_prefs.getBoolean("gametree-show-subtree-sizes", false);
        m_autoNumber = m_prefs.getBoolean("gtpshell-autonumber", false);
        m_commandCompletion =
            ! m_prefs.getBoolean("gtpshell-disable-completions", false);
        m_timeStamp = m_prefs.getBoolean("gtpshell-timestamp", false);
        m_showLastMove = m_prefs.getBoolean("show-last-move", true);        
        m_showVariations = m_prefs.getBoolean("show-variations", false);
        boolean showCursor = m_prefs.getBoolean("show-cursor", false);
        boolean showGrid = m_prefs.getBoolean("show-grid", true);
        m_guiBoard.setShowCursor(showCursor);
        m_guiBoard.setShowGrid(showGrid);
        setJMenuBar(m_menuBar.getMenuBar());
        setMinimumSize();
        m_programCommand = program;
        if (m_programCommand != null && m_programCommand.trim().equals(""))
            m_programCommand = null;
        if (time != null)
            m_timeSettings = TimeSettings.parse(time);
        protectGui(); // Show wait cursor
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initialize();
                } });
    }
    
    public void actionAbout()
    {
        String command = null;
        if (m_gtp != null)
            command = m_gtp.getProgramCommand();
        AboutDialog.show(this, getProgramName(), m_version, command,
                         m_messageDialogs);
    }

    public void actionAddBookmark()
    {
        if (m_file == null)
        {
            showError("Cannot set bookmark if no file is loaded",
                      "Bookmarks can only be set in loaded files.",
                      false);
            return;
        }
        if (isModified())
        {
            showError("Cannot set bookmark in modified file",
                      "Bookmarks cannot be set in modified files.\n" +
                      "Save the file before setting a bookmark.",
                      false);
            return;
        }
        if (getCurrentNode().getFatherConst() != null
            && getCurrentNode().getMove() == null)
        {
            showError("Cannot set bookmark at this node.",
                      "Bookmarks can only be set at non-root nodes without "
                      + "moves", false);
            return;
        }
        String variation = NodeUtil.getVariationString(getCurrentNode());
        int move = NodeUtil.getMoveNumber(getCurrentNode());
        Bookmark bookmark = new Bookmark(m_file, move, variation);
        BookmarkEditor editor = new BookmarkEditor();
        bookmark = editor.editItem(this, "Add Bookmark", bookmark, true,
                                   m_messageDialogs);
        if (bookmark == null)
            return;
        m_bookmarks.add(bookmark);
        m_menuBar.setBookmarks(m_bookmarks);
        Bookmark.save(m_bookmarks);
    }

    public void actionAttachProgram(int index)
    {
        m_prefs.putInt("program", index);
        actionAttachProgram((Program)m_programs.get(index));
    }

    public void actionAttachProgram(final Program program)
    {
        if (! checkCommandInProgress())
            return;
        protectGui();
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        attachNewProgram(program.m_command, program);
                    }
                    finally
                    {
                        unprotectGui();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionBackToMainVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = NodeUtil.getBackToMainVariation(getCurrentNode());
        actionGotoNode(node);
    }

    public void actionBackward(int n)
    {
        if (! checkStateChangePossible())
            return;
        boolean protectGui = (m_gtp != null
                              && (n > 1 || ! m_gtp.isSupported("undo")));
        actionGotoNode(NodeUtil.backward(getCurrentNode(), n), protectGui);
    }

    public void actionBeginning()
    {
        if (! checkStateChangePossible())
            return;
        actionBackward(NodeUtil.getDepth(getCurrentNode()));
    }

    public void actionBoardSize(int size)
    {
        if (! checkCommandInProgress())
            return;
        actionNewGame(size);
        m_prefs.putInt("boardsize", size);
    }

    public void actionBoardSizeOther()
    {
        if (! checkCommandInProgress())
            return;
        int size = BoardSizeDialog.show(this, getBoardSize(),
                                        m_messageDialogs);
        if (size < 1 || size > GoPoint.MAXSIZE)
            return;
        actionBoardSize(size);
    }
    
    public void actionClearAnalyzeCommand()
    {
        if (! checkCommandInProgress())
            return;
        clearAnalyzeCommand();
    }

    public void actionClockHalt()
    {
        if (! getClock().isRunning())
            return;
        m_game.haltClock();
        updateViews(false);
    }

    public void actionClockResume()
    {
        if (getClock().isRunning())
            return;
        m_game.startClock();
        updateViews(false);
    }

    public void actionClockRestore()
    {        
        if (! getClock().isInitialized())
            return;
        m_game.restoreClock();
        m_gameInfo.updateTimeFromClock(getClock());
        updateViews(false);
    }

    public void actionComputerColor(boolean isBlack, boolean isWhite)
    {
        boolean computerNone = (! isBlack && ! isWhite);
        m_prefs.putBoolean("computer-none", computerNone);
        m_computerBlack = isBlack;
        m_computerWhite = isWhite;
        if (! isCommandInProgress())
            checkComputerMove();
        updateViews(false);
    }

    public void actionDeleteSideVariations()
    {
        if (! checkStateChangePossible())
            return;
        if (! NodeUtil.isInMainVariation(getCurrentNode()))
            return;
        if (! showQuestion("Delete variations?",
                           "All variations but the main variation will be " +
                           "deleted.", "Delete", false))
            return;
        m_game.keepOnlyMainVariation();
        boardChangedBegin(false, true);
    }

    public void actionDetachProgram()
    {        
        if (m_gtp == null)
            return;
        if (isCommandInProgress()
            && ! showQuestion("Terminate " + getProgramName() + "?",
                              "A command is in progress.", "Terminate", true))
            return;
        m_prefs.putInt("program", -1);
        protectGui();
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        saveSession();
                        detachProgram();
                        updateViews(false);
                    }
                    finally
                    {
                        unprotectGui();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionDisposeAnalyzeDialog()
    {        
        if (m_analyzeDialog != null)
        {
            clearAnalyzeCommand();
            saveSession();
            m_analyzeDialog.dispose();
            m_analyzeDialog = null;
        }
    }    

    public void actionDisposeTree()
    {
        if (m_gameTreeViewer != null)
        {
            saveSession();
            m_gameTreeViewer.dispose();
            m_gameTreeViewer = null;
            updateViews(false);
        }
    }

    public void actionDocumentation()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/doc/index.html");
        if (url == null)
        {
            showError("Help not found", "");
            return;
        }
        if (m_help == null)
        {
            m_help = new Help(url, m_messageDialogs);
            m_session.restoreSize(m_help, "help");
        }
        m_help.setVisible(true);
        m_help.toFront();
    }

    public void actionEditBookmarks()
    {
        BookmarkEditor editor = new BookmarkEditor();
        ObjectListEditor listEditor = new ObjectListEditor();
        if (! listEditor.edit(this, "Edit Bookmarks", m_bookmarks, editor,
                              m_messageDialogs))
            return;
        m_menuBar.setBookmarks(m_bookmarks);
        Bookmark.save(m_bookmarks);
    }

    public void actionEditLabel(GoPoint point)
    {
        String value = getCurrentNode().getLabel(point);
        value = JOptionPane.showInputDialog(this, "Label " + point, value);
        if (value == null)
            return;
        m_game.setLabel(point, value);
        m_guiBoard.setLabel(point, value);
        updateGuiBoard();
    }

    public void actionEditPrograms()
    {
        ProgramEditor editor = new ProgramEditor();
        ObjectListEditor listEditor = new ObjectListEditor();
        if (! listEditor.edit(this, "Edit Programs", m_programs, editor,
                              m_messageDialogs))
            return;
        m_menuBar.setPrograms(m_programs);
        m_prefs.putInt("program", -1);
        Program.save(m_programs);
    }

    public void actionEnd()
    {
        actionForward(NodeUtil.getNodesLeft(getCurrentNode()));
    }

    public void actionExportLatexMainVariation()
    {
        File file = FileDialogs.showSave(this, "Export LaTeX",
                                         m_messageDialogs);
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title = FileUtil.removeExtension(new File(file.getName()),
                                                     "tex");
            new TexWriter(title, out, getTree(), false);
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e, false);
        }
    }

    public void actionExportLatexPosition()
    {
        File file = FileDialogs.showSave(this, "Export LaTeX Position",
                                         m_messageDialogs);
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title = FileUtil.removeExtension(new File(file.getName()),
                                                     "tex");
            new TexWriter(title, out, getBoard(), false,
                          GuiBoardUtil.getLabels(m_guiBoard),
                          GuiBoardUtil.getMark(m_guiBoard),
                          GuiBoardUtil.getMarkTriangle(m_guiBoard),
                          GuiBoardUtil.getMarkCircle(m_guiBoard),
                          GuiBoardUtil.getMarkSquare(m_guiBoard),
                          GuiBoardUtil.getSelects(m_guiBoard));
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e, false);
        }
    }

    public void actionExportSgfPosition()
    {
        File file = FileDialogs.showSaveSgf(this, m_messageDialogs);
        if (file == null)
            return;
        try
        {
            savePosition(file);
        }
        catch (FileNotFoundException e)
        {
            showError("Could not save position", e, false);
        }
    }

    public void actionExportTextPosition()
    {
        File file = FileDialogs.showSave(this, "Export Text Position",
                                         m_messageDialogs);
        if (file == null)
            return;
        try
        {
            String text = BoardUtil.toString(getBoard(), false);
            PrintStream out = new PrintStream(new FileOutputStream(file));
            out.print(text);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e, false);
        }
    }

    public void actionExportTextPositionToClipboard()
    {
        GuiUtil.copyToClipboard(BoardUtil.toString(getBoard(), false));
    }

    public void actionFind()
    {
        if (! checkStateChangePossible())
            return;
        Pattern pattern = FindDialog.run(this, m_comment.getSelectedText());
        if (pattern == null)
            return;
        m_pattern = pattern;
        if (NodeUtil.commentContains(getCurrentNode(), m_pattern))
            m_comment.markAll(m_pattern);
        else
            actionFindNext();
    }

    public void actionFindNext()
    {
        if (! checkStateChangePossible())
            return;
        if (m_pattern == null)
            return;
        protectGui();
        showStatus("Searching pattern...");
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        ConstNode root = getTree().getRootConst();
                        ConstNode currentNode = getCurrentNode();
                        ConstNode node =
                            NodeUtil.findInComments(currentNode, m_pattern);
                        if (node == null)
                            if (getCurrentNode() != root)
                            {
                                unprotectGui();
                                String optionalMessage =
                                    "The end of the tree was reached. " +
                                    "Continue the search from the start of " +
                                    "the tree?";
                                if (showQuestion("Continue from start?",
                                                 optionalMessage, "Continue",
                                                 false))
                                {
                                    protectGui();
                                    node = root;
                                    if (! NodeUtil.commentContains(node,
                                                                   m_pattern))
                                        node =
                                            NodeUtil.findInComments(node,
                                                                    m_pattern);
                                }
                            }
                        if (node == null)
                        {
                            unprotectGui();
                            String optionalMessage =
                                "The search pattern \"" + m_pattern +
                                "\" was not found.";
                            showInfo("Pattern not found", optionalMessage,
                                     false);
                            m_pattern = null;
                        }
                        else
                        {
                            gotoNode(node);
                            boardChangedBegin(false, false);
                            m_comment.markAll(m_pattern);
                        }
                    }
                    finally
                    {
                        unprotectGui();
                        clearStatus();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionForward(int n)
    {
        if (! checkStateChangePossible())
            return;
        boolean protectGui = (m_gtp != null && n > 1);
        actionGotoNode(NodeUtil.forward(getCurrentNode(), n), protectGui);
    }

    public void actionGameInfo()
    {
        if (! checkCommandInProgress())
            // Changes in game info may send GTP commands
            return;
        ConstNode node = m_game.getGameInformationNode();
        GameInformation info =
            new GameInformation(node.getGameInformationConst());
        GameInfoDialog.show(this, info, m_messageDialogs);
        m_game.setGameInformation(info, node);
        currentNodeChanged(); // updates komi, time settings
        Komi prefsKomi = getPrefsKomi();
        Komi komi = info.getKomi();
        if (komi != null && ! komi.equals(prefsKomi))
            m_prefs.put("komi", komi.toString());
        if (info.getTimeSettings() != null
            && ! info.getTimeSettings().equals(m_timeSettings))
            m_timeSettings = info.getTimeSettings();
        setTitle();
        updateViews(false);
        m_gameInfo.updateTimeFromClock(getClock());
    }

    public void actionGoto()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = MoveNumberDialog.show(this, getCurrentNode(),
                                               m_messageDialogs);
        if (node == null)
            return;
        actionGotoNode(node);
    }

    public void actionGotoBookmark(int i)
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        if (i < 0 || i >= m_bookmarks.size())
            return;
        Bookmark bookmark = (Bookmark)m_bookmarks.get(i);
        File file = bookmark.m_file;
        if (m_file == null || ! file.equals(m_file))
            if (! loadFile(file, -1))
                return;
        updateViews(true);
        String variation = bookmark.m_variation;
        ConstNode node = getTree().getRootConst();
        if (! variation.equals(""))
        {
            node = NodeUtil.findByVariation(node, variation);
            if (node == null)
            {
                showError("Bookmark has invalid variation", "");
                return;
            }
        }
        node = NodeUtil.findByMoveNumber(node, bookmark.m_move);
        if (node == null)
        {
            showError("Bookmark has invalid move number", "");
            return;
        }
        actionGotoNode(node);
    }

    public void actionGotoNode(ConstNode node)
    {
        boolean protectGui = (m_gtp != null);
        actionGotoNode(node, protectGui);
    }

    public void actionGotoNode(final ConstNode node, final boolean protectGui)
    {
        if (! checkStateChangePossible())
            return;
        if (protectGui)
            protectGui();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {        
                    gotoNode(node);
                    boardChangedBegin(false, false);
                    if (protectGui)
                        unprotectGui();
                }
            });
    }

    public void actionGotoVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = GotoVariationDialog.show(this, getTree(),
                                                  getCurrentNode(),
                                                  m_messageDialogs);
        if (node == null)
            return;
        actionGotoNode(node);
    }

    public void actionHandicap(int handicap)
    {
        if (! checkCommandInProgress())
            return;
        m_handicap = handicap;
        if (isModified())
            showInfo("Handicap will take effect on next game.",
                     "You can change the handicap settings only directly " +
                     "after a new games was started.", true);
        else
        {
            m_computerBlack = false;
            m_computerWhite = false;
            newGame(getBoardSize());
            updateViews(true);
        }
    }

    public void actionHideShell()
    {
        if (m_shell == null)
            return;
        saveSession();
        m_shell.setVisible(false);
    }

    public void actionImportTextPosition()
    {
        if (! checkStateChangePossible())
            return;
        File file = FileDialogs.showOpen(this, "Import Text Position");
        if (file == null)
            return;
        try
        {
            importTextPosition(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            showError("File not found", "", false);
        }
    }

    public void actionImportTextPositionFromClipboard()
    {
        if (! checkStateChangePossible())
            return;
        String text = GuiUtil.getClipboardText();
        if (text == null)
            showError("No text selection in clipboard", "", false);
        else
            importTextPosition(new StringReader(text));
    }

    public void actionInterrupt()
    {
        if (! isCommandInProgress())
        {
            showError("Computer is not thinking", "", false);
            return;
        }
        if (m_gtp == null || m_gtp.isProgramDead())
            return;
        if (m_interrupt.run(this, m_gtp, m_messageDialogs))
            showStatus("Interrupting...");
    }

    public void actionKeepOnlyPosition()
    {
        if (! checkStateChangePossible())
            return;
        if (! showQuestion("Delete all moves?",
                           "All moves and variations will be deleted.",
                           "Delete", true))
            return;
        m_game.keepOnlyPosition();
        initGtp();
        boardChangedBegin(false, true);
    }

    public void actionMainWindowActivate()
    {
        requestFocus();
    }

    public void actionMakeMainVariation()
    {
        if (! checkStateChangePossible())
            return;
        if (! showQuestion("Make current to main variation?",
                           "The variations in the tree will be reordered.",
                           "Make Main Variation", false))
            return;
        m_game.makeMainVariation();
        boardChangedBegin(false, true);
    }

    public void actionMark(GoPoint point, MarkType type, boolean mark)
    {
        if (mark)
            m_game.addMarked(point, type);
        else
            m_game.removeMarked(point, type);
        if (type == MarkType.MARK)
            m_guiBoard.setMark(point, mark);
        else if (type == MarkType.CIRCLE)
            m_guiBoard.setMarkCircle(point, mark);
        else if (type == MarkType.SQUARE)
            m_guiBoard.setMarkSquare(point, mark);
        else if (type == MarkType.TRIANGLE)
            m_guiBoard.setMarkTriangle(point, mark);        
        updateGuiBoard();
    }

    public void actionNewGame()
    {
        actionNewGame(getBoardSize());
    }

    private void actionNewGame(int size)
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        setFile(null);
        newGame(size);
        m_gameInfo.updateTimeFromClock(getClock());
        if (m_gtp != null && ! m_gtp.isGenmoveSupported())
        {
            m_computerBlack = false;
            m_computerWhite = false;
        }
        else if (m_computerBlack || m_computerWhite)
        {
            // Set computer color to the color not to move to avoid automatic
            // move generation after starting a new game
            if (m_handicap == 0)
            {
                m_computerBlack = false;
                m_computerWhite = true;
            }
            else
            {
                m_computerBlack = true;
                m_computerWhite = false;
            }
        }
        boardChangedBegin(true, true);
    }

    public void actionNewProgram()
    {
        m_newProgram = new Program("", "", "", "");
        final ProgramEditor editor = new ProgramEditor();
        m_newProgram =
            editor.editItem(this, "New Program", m_newProgram, true,
                            m_messageDialogs);
        if (m_newProgram == null)
            return;
        protectGui();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    attachNewProgram(m_newProgram.m_command, m_newProgram);
                    unprotectGui();
                    if (m_gtp == null || m_gtp.isProgramDead())
                    {
                        m_newProgram = editor.editItem(GoGui.this,
                                                       "New Program",
                                                       m_newProgram, true,
                                                       m_messageDialogs);
                        if (m_newProgram == null)
                            return;
                        SwingUtilities.invokeLater(this);
                        return;
                    }
                    m_newProgram.m_name = getProgramName();
                    m_newProgram.m_version = m_version;
                    m_newProgram.setUniqueLabel(m_programs);
                    m_newProgram = editor.editItem(GoGui.this, "New Program",
                                                   m_newProgram, false,
                                                   m_messageDialogs);
                    if (m_newProgram == null)
                    {
                        actionDetachProgram();
                        return;
                    }
                    m_programs.add(m_newProgram);
                    m_prefs.putInt("program", m_programs.size() - 1);
                    m_menuBar.setPrograms(m_programs);
                    Program.save(m_programs);
                    updateViews(false);
                }
            });
    }

    public void actionNextEarlierVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = NodeUtil.getNextEarlierVariation(getCurrentNode());
        if (node != null)
            actionGotoNode(node);
    }

    public void actionNextVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = NodeUtil.getNextVariation(getCurrentNode());
        if (node != null)
            actionGotoNode(node);
    }

    public void actionOpen()
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        File file = FileDialogs.showOpenSgf(this);
        if (file == null)
            return;
        actionOpenFile(file);
    }

    public void actionOpenFile(final File file)
    {
        if (file == null)
            return;
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        final boolean protectGui = (m_gtp != null);
        if (protectGui)
            protectGui();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {        
                    loadFile(file, -1);
                    boardChangedBegin(false, true);
                    if (protectGui)
                        unprotectGui();
                }
            });
    }

    public void actionPass()
    {
        if (! checkStateChangePossible())
            return;
        String optionalMessage =
            "After a pass, it will be the opponent's turn.";
        if (! showOptionalQuestion("pass", "Play a pass?", optionalMessage,
                                   "Pass", false))
            return;
        humanMoved(Move.getPass(getToMove()));
    }

    public void actionPlay(boolean isSingleMove)
    {
        if (! checkStateChangePossible())
            return;
        if (! checkProgramReady())
            return;
        if (! isSingleMove && ! isComputerBoth())
        {
            m_computerBlack = false;
            m_computerWhite = false;
            if (getToMove() == GoColor.BLACK)
                m_computerBlack = true;
            else
                m_computerWhite = true;
        }
        m_interruptComputerBoth = false;
        generateMove(isSingleMove);
        if (getCurrentNode() == getTree().getRootConst()
            && ! getCurrentNode().hasChildren())
            m_game.resetClock();
        m_game.startClock();
    }

    public void actionPreviousEarlierVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node =
            NodeUtil.getPreviousEarlierVariation(getCurrentNode());
        if (node != null)
            actionGotoNode(node);
    }

    public void actionPreviousVariation()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = NodeUtil.getPreviousVariation(getCurrentNode());
        if (node != null)
            actionGotoNode(node);
    }

    public void actionPrint()
    {
        Print.run(this, m_guiBoard, m_messageDialogs);
    }

    public void actionReattachProgram()
    {
        if (m_gtp == null)
            return;
        if (! checkCommandInProgress())
            return;
        protectGui();
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        attachNewProgram(m_programCommand, null);
                    }
                    finally
                    {
                        unprotectGui();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionSave()
    {
        if (! isModified())
            return;
        if (m_file == null)
            actionSaveAs();
        else
        {
            if (m_file.exists())
            {
                String mainMessage = "Replace file \"" + m_file.getName()
                    + "\"?";
                String optionalMessage =
                    "If you overwrite the file with your changed version, " +
                    "the previous version will be lost.";
                String disableKey = "net.sf.gogui.GoGui.overwrite";
                if (! m_messageDialogs.showQuestion(disableKey, this,
                                                    mainMessage,
                                                    optionalMessage,
                                                    "Replace", true))
                    return;
            }
            save(m_file);
        }
        updateViews(false);
    }

    public void actionSaveAs()
    {
        saveDialog();
        updateViews(false);
    }

    public void actionScore()
    {
        if (m_scoreMode)
            return;
        if (! checkStateChangePossible())
            return;
        if (m_gtp == null)
        {
            String disableKey = "net.sf.gogui.gogui.GoGui.score-no-program";
            m_messageDialogs.showInfo(disableKey, this,
                                      "Please mark dead groups manually",
                                      "No program is attached.", true);
            initScore(null);
            updateViews(false);
            return;
        }
        if (m_gtp.isSupported("final_status_list"))
        {
            Runnable callback = new Runnable() {
                    public void run() {
                        scoreContinue();
                    }
                };
            showStatus("Scoring...");
            runLengthyCommand("final_status_list dead", callback);
        }
        else
        {
            String disableKey =
                "net.sf.gogui.gogui.GoGui.score-not-supported";
            m_messageDialogs.showInfo(disableKey, this,
                                      "Please mark dead groups manually",
                                      getProgramName()
                                      + " does not support scoring.", true);
            initScore(null);
            updateViews(false);
        }
    }

    public void actionScoreDone(Score score)
    {
        if (! m_scoreMode)
            return;
        scoreDone(score);
        updateViews(false);
    }

    public void actionSendCommand(String command, final boolean isCritical,
                                  final boolean showError)
    {
        if (GtpUtil.isStateChangingCommand(command))
        {
            showError("Cannot send board changing command", "", false);
            return;
        }
        if (! checkProgramReady())
            return;
        Runnable callback = new Runnable() {
                public void run() {
                    sendGtpCommandContinue(isCritical, showError);
                }
            };
        m_gtp.send(command, callback);
        beginLengthyCommand();
    }

    public void actionSetAnalyzeCommand(AnalyzeCommand command)
    {
        actionSetAnalyzeCommand(command, false, true, true);
    }

    public void actionSetAnalyzeCommand(AnalyzeCommand command,
                                        boolean autoRun, boolean clearBoard,
                                        boolean oneRunOnly)
    {
        if (! checkProgramReady())
            return;
        initAnalyzeCommand(command, autoRun, clearBoard);
        m_analyzeOneRunOnly = oneRunOnly;
        boolean needsPointArg = m_analyzeCommand.needsPointArg();
        if (needsPointArg && ! m_analyzeCommand.isPointArgMissing())
        {
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(m_analyzeCommand.getPointArg(), true);
        }
        else if (needsPointArg || m_analyzeCommand.needsPointListArg())
        {
            m_guiBoard.clearAllSelect();
            if (m_analyzeCommand.getType() == AnalyzeCommand.EPLIST)
                GuiBoardUtil.setSelect(m_guiBoard,
                                        m_analyzeCommand.getPointListArg(),
                                        true);
            toFront();
            return;
        }
        analyzeBegin(false);
    }    

    public void actionSetup(GoColor color)
    {
        assert(color.isBlackWhite());
        if (! checkCommandInProgress())
            return ;
        if (m_scoreMode)
            scoreDone(null);
        ConstNode node = getCurrentNode();
        if (m_setupMode)
        {
            if (color == m_setupColor)
            {
                setupDone();
                boardChangedBegin(false, true);
            }
            else
                m_setupColor = color;
            updateViews(false);
        }
        else
        {
            resetBoard();
            m_setupMode = true;
            m_setupColor = color;
            // Update views with setup enabled before the following dialog,
            // otherwise toggle buttons and menus will stay selected, if
            // entering setup mode was cancelled
            updateViews(false);
            boolean needsNewNode =
                (node.getMove() != null || node.hasChildren());
            if (needsNewNode)
            {
                String message = "Create new setup node?";
                if (! showOptionalQuestion("create-setup-node", message,
                                           "A new node for setup stones " +
                                           "will be created in the game tree.",
                                           "Create Node", false))
                {
                    m_setupMode = false;
                    updateViews(false);
                    return;
                }
            }
            if (needsNewNode)
            {
                m_game.createNewChild();
                currentNodeChanged();
                updateViews(true);
            }
            else
                updateViews(false);
        }
        if (m_setupMode)
        {
            if (m_setupColor == GoColor.BLACK)
                showStatus("Setup black stones");
            else
                showStatus("Setup white stones");
        }
    }

    public void actionShellSave()
    {
        if (m_shell == null)
            return;
        m_shell.saveLog(this);
    }

    public void actionShellSaveCommands()
    {
        if (m_shell == null)
            return;
        m_shell.saveCommands(this);
    }

    public void actionShellSendFile()
    {
        if (! checkStateChangePossible())
            return;
        if (m_shell == null)
            return;
        File file = FileDialogs.showOpen(this, "Choose GTP file");
        if (file == null)
            return;
        actionShellSendFile(file);
    }

    public void actionShellSendFile(File file)
    {
        if (file == null)
            return;
        if (! checkStateChangePossible())
            return;
        if (m_shell == null)
            return;
        sendGtpFile(file);
        m_menuBar.addRecentGtp(file);
        updateViews(false);
    }

    public void actionShowAnalyzeDialog()
    {        
        if (m_gtp == null)
            return;
        if (m_analyzeDialog == null)
            createAnalyzeDialog();
        else
            m_analyzeDialog.toFront();
    }

    public void actionShowShell()
    {
        if (m_gtp == null)
            return;
        if (! m_shell.isVisible())
        {
            restoreSize(m_shell, "shell");
            m_shell.setVisible(true);
        }
        else
            m_shell.toFront();
    }

    public void actionShowTree()
    {
        if (m_gameTreeViewer == null)
            createTree();
        else
            m_gameTreeViewer.toFront();
    }

    public void actionToggleBeepAfterMove()
    {
        m_beepAfterMove = ! m_beepAfterMove;
        m_prefs.putBoolean("beep-after-move", m_beepAfterMove);
    }

    public void actionToggleAutoNumber()
    {
        m_autoNumber = ! m_autoNumber;
        if (m_gtp != null)
            m_gtp.setAutoNumber(m_autoNumber);
    }

    public void actionToggleCommentMonoFont()
    {
        boolean monoFont = ! m_comment.getMonoFont();
        m_comment.setMonoFont(monoFont);
        m_prefs.putBoolean("comment-font-fixed", monoFont);
    }

    public void actionToggleCompletion()
    {
        m_commandCompletion = ! m_commandCompletion;
        if (m_shell != null)
            m_shell.setCommandCompletion(m_commandCompletion);
        m_prefs.putBoolean("gtpshell-disable-completions",
                           ! m_commandCompletion);
    }

    public void actionToggleShowCursor()
    {
        boolean showCursor = ! m_guiBoard.getShowCursor();
        m_guiBoard.setShowCursor(showCursor);
        m_prefs.putBoolean("show-cursor", showCursor);
    }

    public void actionToggleShowGrid()
    {
        boolean showGrid = ! m_guiBoard.getShowGrid();
        m_guiBoard.setShowGrid(showGrid);
        m_prefs.putBoolean("show-grid", showGrid);
    }

    public void actionToggleShowInfoPanel()
    {
        if (GuiUtil.isNormalSizeMode(this))
        {
            if (m_showInfoPanel)
                m_comment.setPreferredSize(m_comment.getSize());
            m_guiBoard.setPreferredFieldSize(m_guiBoard.getFieldSize());
        }
        showInfoPanel(! m_showInfoPanel);
        updateViews(false);
    }

    public void actionToggleShowLastMove()
    {
        m_showLastMove = ! m_showLastMove;
        m_prefs.putBoolean("show-last-move", m_showLastMove);
        updateFromGoBoard();
        updateViews(false);
    }

    public void actionToggleShowSubtreeSizes()
    {
        m_showSubtreeSizes = ! m_showSubtreeSizes;
        m_prefs.putBoolean("gametree-show-subtree-sizes", m_showSubtreeSizes);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setShowSubtreeSizes(m_showSubtreeSizes);
            updateViews(true);
        }
        else
            updateViews(false);
    }

    public void actionToggleShowToolbar()
    {
        if (GuiUtil.isNormalSizeMode(this))
        {
            if (m_showInfoPanel)
                m_comment.setPreferredSize(m_comment.getSize());
            m_guiBoard.setPreferredFieldSize(m_guiBoard.getFieldSize());
        }
        showToolbar(! m_showToolbar);
        updateViews(false);
    }

    public void actionToggleShowVariations()
    {
        m_showVariations = ! m_showVariations;
        m_prefs.putBoolean("show-variations", m_showVariations);
        resetBoard();
        updateViews(false);
    }

    public void actionToggleTimeStamp()
    {
        m_timeStamp = ! m_timeStamp;
        if (m_shell != null)
            m_shell.setTimeStamp(m_timeStamp);
        m_prefs.putBoolean("gtpshell-timestamp", m_timeStamp);
        updateViews(false);
    }

    public void actionTreeLabels(int mode)
    {
        m_treeLabels = mode;
        m_prefs.putInt("gametree-labels", mode);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setLabelMode(mode);
            updateViews(true);
        }
        else
            updateViews(false);
    }

    public void actionTreeSize(int mode)
    {
        m_treeSize = mode;
        m_prefs.putInt("gametree-size", mode);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setSizeMode(mode);
            updateViews(true);
        }
        else
            updateViews(false);
    }

    public void actionTruncate()
    {
        if (! checkStateChangePossible())
            return;
        if (! getCurrentNode().hasFather())
            return;
        if (! showQuestion("Truncate current?",
                           "The current node and all children nodes " +
                           " will be deleted from the game tree.",
                           "Truncate", false))
            return;
        m_game.truncate();
        boardChangedBegin(false, true);
    }

    public void actionTruncateChildren()
    {
        if (! checkStateChangePossible())
            return;
        int numberChildren = getCurrentNode().getNumberChildren();
        if (numberChildren == 0)
            return;
        if (! showQuestion("Truncate children?",
                           "All children nodes of this position" +
                           " will be deleted from the game tree", "Truncate",
                           false))
            return;
        m_game.truncateChildren();
        boardChangedBegin(false, true);
    }

    public void actionQuit()
    {
        close();
    }

    public boolean getAutoNumber()
    {
        return m_autoNumber;
    }

    public boolean getBeepAfterMove()
    {
        return m_beepAfterMove;
    }

    public boolean getCommentMonoFont()
    {
        return m_comment.getMonoFont();
    }

    public boolean getCompletion()
    {
        return m_commandCompletion;
    }

    public int getNumberPrograms()
    {
        return m_programs.size();
    }

    /** Get name of currently attached program.
        @return Name or null, if no program is attached
    */
    public String getProgramName()
    {
        if (m_gtp != null)
            return m_gtp.getProgramName();
        else
            return null;
    }

    public GoColor getSetupColor()
    {
        return m_setupColor;
    }

    public boolean getShowLastMove()
    {
        return m_showLastMove;
    }

    public boolean getShowSubtreeSizes()
    {
        return m_showSubtreeSizes;
    }

    public boolean getShowVariations()
    {
        return m_showVariations;
    }

    public boolean getTimeStamp()
    {
        return m_timeStamp;
    }

    public int getTreeLabels()
    {
        return m_treeLabels;
    }

    public int getTreeSize()
    {
        return m_treeSize;
    }

    public boolean isAnalyzeDialogShown()
    {
        return (m_analyzeDialog != null);
    }

    public boolean isCommandInProgress()
    {
        return (m_gtp != null && m_gtp.isCommandInProgress());
    }

    /** Check if computer plays a color (or both). */
    public boolean isComputerColor(GoColor color)
    {
        if (color == GoColor.BLACK)
            return m_computerBlack;
        assert(color == GoColor.WHITE);
        return m_computerWhite;
    }

    public boolean isInfoPanelShown()
    {
        return m_showInfoPanel;
    }

    public boolean isShellShown()
    {
        return (m_shell != null && m_shell.isVisible());
    }

    public boolean isToolbarShown()
    {
        return m_showToolbar;
    }

    public boolean isTreeShown()
    {
        return (m_gameTreeViewer != null);
    }

    public void contextMenu(GoPoint point, Component invoker, int x, int y)
    {
        if (m_setupMode
            || (m_analyzeCommand != null
                && m_analyzeCommand.needsPointListArg()))
        {
            fieldClicked(point, true);
            return;
        }
        ContextMenu contextMenu = createContextMenu(point);
        contextMenu.show(invoker, x, y);
    }

    public void fieldClicked(GoPoint p, boolean modifiedSelect)
    {
        if (isCommandInProgress() && modifiedSelect)
        {
            m_guiBoard.contextMenu(p);
            return;
        }
        if (! checkCommandInProgress())
            return;
        if (m_setupMode)
        {
            GoColor color;
            if (modifiedSelect)
                color = m_setupColor.otherColor();
            else
                color = m_setupColor;
            if (getBoard().getColor(p) == color)
                color = GoColor.EMPTY;
            setup(p, color);
            updateViews(false);
        }
        else if (m_analyzeCommand != null && m_analyzeCommand.needsPointArg()
                 && ! modifiedSelect)
        {
            m_analyzeCommand.setPointArg(p);
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(p, true);
            analyzeBegin(false);
            return;
        }
        else if (m_analyzeCommand != null
                 && m_analyzeCommand.needsPointListArg())
        {
            PointList pointListArg = m_analyzeCommand.getPointListArg();
            if (pointListArg.contains(p))
            {
                pointListArg.remove(p);
                if (modifiedSelect)
                    pointListArg.add(p);
            }
            else
                pointListArg.add(p);
            m_guiBoard.clearAllSelect();
            GuiBoardUtil.setSelect(m_guiBoard, pointListArg, true);
            if (modifiedSelect && pointListArg.size() > 0)
                analyzeBegin(false);
            return;
        }
        else if (m_scoreMode && ! modifiedSelect)
        {
            GuiBoardUtil.scoreSetDead(m_guiBoard, m_countScore, getBoard(), p);
            Komi komi = getGameInformation().getKomi();
            m_scoreDialog.showScore(m_countScore, komi);
            return;
        }
        else if (modifiedSelect)
            m_guiBoard.contextMenu(p);
        else
        {
            if (getBoard().isSuicide(getToMove(), p)
                && ! showQuestion("Play suicide?",
                                  "Playing at this point will leave the " +
                                  "stone without liberties and it will be " +
                                  "immediately captured. Suicide is not " +
                                  "allowed under all Go rule sets.",
                                  "Play Suicide", false))
                return;
            else if (getBoard().isKo(p)
                && ! showQuestion("Play illegal Ko move?",
                                  "This move violates the Ko rule, because " +
                                  "it repeats the previous position for " +
                                  "the color to play.",
                                  "Play Illegal Ko", false))
                return;
            Move move = Move.get(getToMove(), p);
            humanMoved(move);
        }
    }

    public GoGuiActions getActions()
    {
        return m_actions;
    }

    public File getFile()
    {
        return m_file;
    }

    public ConstGame getGame()
    {
        return m_game;
    }

    public ConstGuiBoard getGuiBoard()
    {
        return m_guiBoard;
    }

    public int getHandicapDefault()
    {
        return m_handicap;
    }

    public boolean getMonoFont()
    {
        return m_comment.getMonoFont();
    }

    /** Get currently used pattern for search in comments. */
    public Pattern getPattern()
    {
        return m_pattern;
    }

    public boolean isInSetupMode()
    {
        return m_setupMode;
    }

    public boolean sendGtpCommandSync(String command) throws GtpError
    {
        if (! checkProgramReady())
            return false;
        m_gtp.send(command);
        return true;
    }

    public void sendGtpCommandContinue(boolean isCritical,
                                       boolean showError)
    {
        endLengthyCommand(isCritical, showError);
    }

    public void initAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                   boolean clearBoard)
    {
        if (! checkProgramReady())
            return;
        m_analyzeCommand = command;
        m_analyzeAutoRun = autoRun;
        m_analyzeClearBoard = clearBoard;
        if (command.needsPointArg())
        {
            setBoardCursor(Cursor.HAND_CURSOR);
            showStatusSelectTarget();
        }
        else if (command.needsPointListArg())
        {
            setBoardCursor(Cursor.HAND_CURSOR);
            showStatusSelectPointList();
        }
    }

    public boolean isInterruptSupported()
    {
        return (m_gtp != null && m_gtp.isInterruptSupported());
    }

    public boolean isModified()
    {
        return m_game.isModified();
    }

    public boolean isProgramAttached()
    {
        return (m_gtp != null);
    }

    private class AnalyzeContinue
        implements Runnable
    {
        public AnalyzeContinue(boolean checkComputerMove)
        {
            m_checkComputerMove = checkComputerMove;
        }

        public void run()
        {
            analyzeContinue(m_checkComputerMove);
        }
        
        private final boolean m_checkComputerMove;
    }

    private class ShowInvalidResponse
        implements Runnable
    {
        public ShowInvalidResponse(String line)
        {
            m_line = line;
        }

        public void run()
        {
            String name = getProgramName();
            String mainMessage = name + " sent a malformed response";
            if (m_line.trim().equals(""))
            {
                String disableKey =
                    "net.sf.gogui.gogui.GoGui.invalid-empty-response";
                String optionalMessage =
                    "Empty lines before the response are not allowed" +
                    " by the GTP standard. This error can also occur if the" +
                    " program ended the last response with three newlines" +
                    " instead of two. This error can probably be" +
                    " ignored, but could indicate a more serious problem" +
                    " with the Go program. You should inform the author of " +
                    name + ".";
                m_messageDialogs.showWarning(disableKey, GoGui.this,
                                             mainMessage, optionalMessage, 
                                             true);
            }
            else
            {
                String disableKey =
                    "net.sf.gogui.gogui.GoGui.invalid-response";
                String optionalMessage =
                    "Text lines before the status character of the first" +
                    " response line are not allowed by the GTP standard." +
                    " This error can probably be ignored, but could indicate" +
                    " a more serious problem with the Go program. You " +
                    " should inform the author of " + name + ".";
                m_messageDialogs.showWarning(disableKey, GoGui.this,
                                             mainMessage, optionalMessage, 
                                             true);
            }
        }
        
        private final String m_line;
    }

    private static class LoadFileRunnable
        implements GuiUtil.ProgressRunnable
    {
        LoadFileRunnable(FileInputStream in, File file)
        {
            m_in = in;
            m_file = file;            
        }

        public SgfReader getReader()
        {
            return m_reader;
        }

        public void run(ProgressShow progressShow) throws Throwable
        {
            m_reader = new SgfReader(m_in, m_file, progressShow,
                                     m_file.length());
        }

        private final File m_file;

        private final FileInputStream m_in;

        private SgfReader m_reader;
    }

    private boolean m_analyzeAutoRun;

    private boolean m_analyzeClearBoard;

    private boolean m_analyzeOneRunOnly;

    private boolean m_autoNumber;

    private boolean m_commandCompletion;

    private boolean m_timeStamp;

    private final boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    /** State variable used between cbInterrupt and computerMoved. */
    private boolean m_interruptComputerBoth;

    /** State variable used between generateMove and computerMoved. */
    private boolean m_isSingleMove;

    private boolean m_lostOnTimeShown;

    private boolean m_resigned;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_showInfoPanel;

    private boolean m_showLastMove;

    private boolean m_showSubtreeSizes;

    private boolean m_showToolbar;

    private boolean m_showVariations;

    private final boolean m_verbose;

    private int m_handicap;

    private final int m_move;

    private int m_treeLabels;

    private int m_treeSize;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final GuiBoard m_guiBoard;

    private GuiGtpClient m_gtp;

    private final Comment m_comment;

    private final Interrupt m_interrupt = new Interrupt();

    /** File corresponding to the current game. */
    private File m_file;

    private final GameInfo m_gameInfo;    

    private GtpShell m_shell;

    private GameTreeViewer m_gameTreeViewer;

    private Help m_help;

    private final JPanel m_infoPanel;

    private final JPanel m_innerPanel;

    private final JSplitPane m_splitPane;

    private final GoGuiMenuBar m_menuBar;

    private Game m_game;

    private GoColor m_setupColor;

    private MessageDialogs m_messageDialogs = new MessageDialogs();

    private Pattern m_pattern;

    private File m_analyzeCommands;

    private AnalyzeCommand m_analyzeCommand;

    private final Session m_session =
        new Session("net/sf/gogui/gogui/session");

    private final CountScore m_countScore = new CountScore();

    private final StatusBar m_statusBar;

    private final String m_gtpCommand;

    private final String m_gtpFile;

    private final String m_initAnalyze;

    private String m_lastAnalyzeCommand;

    private String m_programCommand;

    private String m_titleFromProgram;

    private String m_version = "";

    private AnalyzeDialog m_analyzeDialog;    

    private final Preferences m_prefs =
        Preferences.userNodeForPackage(getClass());

    private ScoreDialog m_scoreDialog;

    private String m_programAnalyzeCommands;

    /** Program information.
        Can be null even if a program is attached, if only m_programName
        is known.
    */
    private Program m_program;

    /** Program currently being edited in actionNewProgram()
     */
    private Program m_newProgram;

    private final ThumbnailCreator m_thumbnailCreator =
        new ThumbnailCreator(false);

    private TimeSettings m_timeSettings;

    private final GoGuiActions m_actions = new GoGuiActions(this);

    private final GoGuiToolBar m_toolBar;

    private ArrayList m_bookmarks;

    private ArrayList m_programs;

    private void analyzeBegin(boolean checkComputerMove)
    {
        if (m_gtp == null || m_analyzeCommand == null
            || m_analyzeCommand.isPointArgMissing())
            return;
        GoColor toMove = getToMove();
        m_lastAnalyzeCommand = m_analyzeCommand.replaceWildCards(toMove);
        runLengthyCommand(m_lastAnalyzeCommand,
                          new AnalyzeContinue(checkComputerMove));
        showStatus("Running " + m_analyzeCommand.getResultTitle() + "...");
    }

    private void analyzeContinue(boolean checkComputerMove)
    {
        if (m_analyzeClearBoard)
            resetBoard();
        boolean isCritical = (m_gtp != null && m_gtp.isProgramDead());
        if (! endLengthyCommand(isCritical))
            return;
        clearStatus();
        if (m_analyzeCommand == null)
        {
            // Program was detached while running the analyze command
            resetBoard();
            return;
        }
        String title = m_analyzeCommand.getResultTitle();
        try
        {
            String response = m_gtp.getResponse();
            AnalyzeShow.show(m_analyzeCommand, m_guiBoard, m_statusBar,
                             getBoard(), response);
            int type = m_analyzeCommand.getType();
            GoPoint pointArg = null;
            if (m_analyzeCommand.needsPointArg())
                pointArg = m_analyzeCommand.getPointArg();
            else if (m_analyzeCommand.needsPointListArg())
            {
                ConstPointList list = m_analyzeCommand.getPointListArg();
                if (list.size() > 0)
                    pointArg = list.get(list.size() - 1);
            }
            if (type == AnalyzeCommand.PARAM)
                ParameterDialog.editParameters(m_lastAnalyzeCommand, this,
                                               title, response, m_gtp,
                                               m_messageDialogs);
            if (AnalyzeCommand.isTextType(type))
            {
                if (response.indexOf("\n") < 0)
                {
                    if (response.trim().equals(""))
                        response = "(empty response)";
                    showStatus(title + ": " + response);
                }
                else
                    GoGuiUtil.showAnalyzeTextOutput(this, m_guiBoard, type,
                                                    pointArg, title, response);
            }
            if ("".equals(m_statusBar.getText()) &&
                type != AnalyzeCommand.PARAM && type != AnalyzeCommand.NONE)
                showStatus(title);
            if (checkComputerMove)
                checkComputerMove();
        }
        catch (GtpResponseFormatError e)
        {                
            showStatus(title);
            showError(e);
        }
        finally
        {
            if (m_analyzeOneRunOnly)
                clearAnalyzeCommand(false);
        }
    }

    private void attachNewProgram(String command, Program program)
    {
        if (m_gtp != null)
        {
            saveSession();
            detachProgram();
        }
        if (! attachProgram(command, program))
        {
            m_prefs.putInt("program", -1);
            updateViews(false);
            return;
        }
        if (m_shell != null && m_session.isVisible("shell"))
            m_shell.setVisible(true);
        if (m_session.isVisible("analyze"))
            createAnalyzeDialog();
        toFrontLater();
        updateViews(false);
    }

    /** Attach program.
        @param programCommand Command line for running program.
        @param program Program information (may be null)
        @return true if program was successfully attached.
    */
    private boolean attachProgram(String programCommand, Program program)
    {
        programCommand = programCommand.trim();
        if (programCommand.equals(""))
            return false;
        m_program = program;
        m_programCommand = programCommand;
        if (m_shell != null)
        {
            m_shell.dispose();
            m_shell = null;
        }
        m_shell = new GtpShell(this, this, m_messageDialogs);
        m_actions.registerAll(m_shell.getLayeredPane());
        m_shell.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionHideShell();
                }
            });
        // Don't restore size yet, see workaround GtpShell.setFinalSize()
        m_session.restoreLocation(m_shell, "shell");
        m_shell.setProgramCommand(programCommand);
        m_shell.setTimeStamp(m_timeStamp);
        m_shell.setCommandCompletion(m_commandCompletion);
        GtpClient.InvalidResponseCallback invalidResponseCallback =
            new GtpClient.InvalidResponseCallback()
            {
                public void show(String line)
                {
                    Runnable runnable = new ShowInvalidResponse(line);
                    if (SwingUtilities.isEventDispatchThread())
                        runnable.run();
                    else
                        invokeAndWait(runnable);
                }
            };
        GtpClient.IOCallback ioCallback = new GtpClient.IOCallback()
            {
                public void receivedInvalidResponse(String s)
                {
                    if (m_shell != null)
                        m_shell.receivedInvalidResponse(s);
                }

                public void receivedResponse(boolean error, String s)
                {
                    if (m_shell != null)
                        m_shell.receivedResponse(error, s);
                }

                public void receivedStdErr(String s)
                {
                    if (m_shell != null)
                    {
                        m_shell.receivedStdErr(s);
                        m_liveGfx.receivedStdErr(s);
                    }
                }

                public void sentCommand(String s)
                {
                    if (m_shell != null)
                        m_shell.sentCommand(s);
                }

                private LiveGfx m_liveGfx =
                    new LiveGfx(getBoard(), m_guiBoard, m_statusBar);
            };
        GtpSynchronizer.Listener synchronizerCallback =
            new GtpSynchronizer.Listener() {
                public void moveNumberChanged(int moveNumber) {
                    String text = "[" + moveNumber + "]";
                    m_statusBar.immediatelyPaintMoveText(text);
                }
            };
        try
        {
            showStatusImmediately("Attaching program...");
            GtpClient gtp =
                new GtpClient(m_programCommand, m_verbose, ioCallback);
            gtp.setInvalidResponseCallback(invalidResponseCallback);
            gtp.setAutoNumber(m_autoNumber);
            m_gtp = new GuiGtpClient(gtp, this, synchronizerCallback);
            m_gtp.start();
            m_gtp.queryName();
            m_gtp.queryProtocolVersion();
            try
            {
                m_version = m_gtp.queryVersion();
                m_shell.setProgramVersion(m_version);
                m_gtp.querySupportedCommands();
                m_gtp.queryInterruptSupport();
            }
            catch (GtpError e)
            {
            }        
            if (m_program != null
                && m_program.updateInfo(getProgramName(), m_version))
            {
                Program.save(m_programs);
                m_menuBar.setPrograms(m_programs);
            }
            m_programAnalyzeCommands = m_gtp.getAnalyzeCommands();
            restoreSize(m_shell, "shell");
            m_shell.setProgramName(getProgramName());
            ArrayList supportedCommands =
                m_gtp.getSupportedCommands();
            m_shell.setInitialCompletions(supportedCommands);
            if (! m_gtpFile.equals(""))
                sendGtpFile(new File(m_gtpFile));
            if (! m_gtpCommand.equals(""))
                sendGtpString(m_gtpCommand);
            if (! m_gtp.isGenmoveSupported())
            {
                m_computerBlack = false;
                m_computerWhite = false;
            }
            initGtp();
            setTitle();
        }
        catch (GtpError e)
        {
            showError(e);
            return false;
        }
        finally
        {
            clearStatus();
        }
        currentNodeChanged();
        return true;
    }    

    private void beginLengthyCommand()
    {
        setBoardCursor(Cursor.WAIT_CURSOR);
        m_shell.setCommandInProgess(true);
        showStatus(getProgramName() + " is thinking...");
        updateViews(false);
    }

    private void boardChangedBegin(boolean doCheckComputerMove,
                                   boolean gameTreeChanged)
    {
        updateFromGoBoard();
        updateViews(gameTreeChanged);
        if (m_gtp != null
            && ! isOutOfSync()
            && m_analyzeCommand != null
            && m_analyzeAutoRun
            && ! m_analyzeCommand.isPointArgMissing())
            analyzeBegin(doCheckComputerMove);
        else
        {
            resetBoard();
            clearStatus();
            if (doCheckComputerMove)
                checkComputerMove();
        }
    }

    private boolean checkCommandInProgress()
    {
        if (isCommandInProgress())
        {
            showError("Cannot execute while computer is thinking",
                      "You need to wait until the command in "
                      + " progress is finished.",
                      false);
            return false;
        }
        return true;
    }

    private void checkComputerMove()
    {
        if (m_gtp == null || isOutOfSync() || m_gtp.isProgramDead())
            return;
        int moveNumber = NodeUtil.getMoveNumber(getCurrentNode());
        boolean bothPassed = (moveNumber >= 2 && getBoard().bothPassed());
        boolean gameFinished = (bothPassed || m_resigned);
        if (isComputerBoth())
        {
            if (gameFinished)
            {
                if (m_auto)
                {
                    newGame(getBoardSize());
                    checkComputerMove();
                    return;
                }
                m_game.haltClock();
                showGameFinished();
                return;
            }
            generateMove(false);            
        }
        else
        {
            if (gameFinished)
            {
                m_game.haltClock();
                showGameFinished();
                return;
            }
            else if (computerToMove())
                generateMove(false);
        }
    }

    private void checkLostOnTime(GoColor color)
    {
        if (getClock().lostOnTime(color)
            && ! getClock().lostOnTime(color.otherColor())
            && ! m_lostOnTimeShown)
        {
            String name = color.getCapitalizedName();
            String result = color.otherColor().getUppercaseLetter() + "+Time";
            String mainMessage = name + " lost on time";
            String optionalMessage =
                name + " run out of time. The result \"" + result +
                "\" was added to the game information.";
            showInfo(mainMessage, optionalMessage, false);
            setResult(result);
            m_lostOnTimeShown = true;
        }
    }

    /** Check if program is attached and ready to receive commands. */
    private boolean checkProgramReady()
    {
        if (m_gtp == null)
        {
            showError("No Go program is attached.", "", false);
            return false;
        }
        if (! checkCommandInProgress())
            return false;
        String name = getProgramName();
        if (m_gtp.isProgramDead())
        {
            String mainMessage = name + " has terminated";
            String optionalMessage =
                "Check the GTP shell window for error messages of " + name +
                " that might be helpful to find the reason for " +
                " this unexpected failure. " +
                "You can reattach " + name + " from the Program menu.";
            showError(mainMessage, optionalMessage, false);
            return false;
        }
        if (isOutOfSync())
        {
            showError(name + " is not in sync with current position",
                      "A previous command to synchronize " + name
                      + " with the current position failed. " +
                      "You won't be able to use " + name +
                      " until you go to a position " +
                      "that can be synchronized again.",
                      false);
            return false;
        }
        return true;
    }

    private boolean checkSaveGame()
    {
        return checkSaveGame(false);
    }

    /** Ask for saving file if it was modified.
        @return true If file was not modified, user chose not to save it
        or file was saved successfully
    */
    private boolean checkSaveGame(boolean isProgramTerminating)
    {
        if (! isModified())
            return true;
        String mainMessage = "Save current game?";
        String optionalMessage =
            "Your changes will be lost if you don't save them.";
        int result;
        String disableKey = null;
        if (! isProgramTerminating)
            disableKey = "net.sf.gogui.gogui.GoGui.save";
        result = m_messageDialogs.showYesNoCancelQuestion(disableKey, this,
                                                          mainMessage,
                                                          optionalMessage,
                                                          "Don't Save",
                                                          "Save");
        switch (result)
        {
        case 0:
            m_game.clearModified();
            return true;
        case 1:
            if (m_file == null)
                return saveDialog();
            else
                return save(m_file);
        case 2:
            return false;
        default:
            assert(false);
            return true;
        }
    }
    
    /** Check if command is in progress or setup or score mode. */
    private boolean checkStateChangePossible()
    {
        if (! checkCommandInProgress())
            return false;
        if (m_setupMode)
            setupDone();
        if (m_scoreMode)
            scoreDone(null);
        return true;
    }

    private void clearAnalyzeCommand()
    {
        clearAnalyzeCommand(true);
    }

    private void clearAnalyzeCommand(boolean resetBoard)
    {
        if (m_analyzeCommand != null)
        {
            m_analyzeCommand = null;
            setBoardCursorDefault();
        }
        if (resetBoard)
        {
            resetBoard();
            clearStatus();
        }
    }

    private void clearStatus()
    {
        m_statusBar.clear();
    }

    private void close()
    {
        if (! checkSaveGame(true))
            return;
        saveSession();
        setVisible(false);
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (m_gtp != null)
                    {
                        m_analyzeCommand = null;
                        detachProgram();
                    }
                    dispose();
                    System.exit(0);
                }
            });
    }

    private void computerMoved()
    {
        if (! endLengthyCommand())
            return;
        if (m_beepAfterMove)
            Toolkit.getDefaultToolkit().beep();
        GoColor toMove = getToMove();
        try
        {
            String response = m_gtp.getResponse();
            checkLostOnTime(toMove);
            boolean gameTreeChanged = false;
            if (response.equalsIgnoreCase("resign"))
            {
                String result =
                    toMove.otherColor().getUppercaseLetter() + "+Resign";
                if (! isComputerBoth())
                    showInfo(getProgramName() + " resigns",
                             "The result \"" + result
                             + "\" was added to the game information.", false);
                m_resigned = true;
                setResult(result);
            }
            else
            {
                GoPoint point = GtpUtil.parsePoint(response, getBoardSize());
                ConstBoard board = getBoard();
                if (point != null)
                {
                    if (board.getColor(point) != GoColor.EMPTY)
                    {
                        showWarning("Program played move on non-empty point",
                                    "", true);
                        m_computerBlack = false;
                        m_computerWhite = false;
                    }
                    else if (board.isKo(point))
                    {
                        showWarning("Program violated Ko rule", "", true);
                        m_computerBlack = false;
                        m_computerWhite = false;
                    }
                }
                Move move = Move.get(toMove, point);
                m_game.play(move);
                m_gtp.updateAfterGenmove(getBoard());
                if (point == null && ! isComputerBoth())
                {
                    String disableKey =
                        "net.sf.gogui.gogui.GoGui.computer-passed";
                    String name = getProgramName();
                    m_messageDialogs.showInfo(disableKey, this,
                                              name + " passes",
                                              name + " played a pass. " +
                                              "When both players pass in "
                                              + "succession, the game ends.",
                                              false);
                }
                m_resigned = false;
                gameTreeChanged = true;
                ConstNode currentNode = getCurrentNode();
                if (currentNode.getFatherConst().getNumberChildren() == 1)
                {
                    if (m_gameTreeViewer != null)
                        m_gameTreeViewer.addNewSingleChild(currentNode);
                    gameTreeChanged = false;
                }
            }
            boolean doCheckComputerMove
                = (! m_isSingleMove
                   && ! (isComputerBoth() && m_interruptComputerBoth));
            boardChangedBegin(doCheckComputerMove, gameTreeChanged);
        }
        catch (GtpResponseFormatError e)
        {
            showError(e);
            clearStatus();
        }
    }

    private boolean computerToMove()
    {
        if (getToMove() == GoColor.BLACK)
            return m_computerBlack;
        else
            return m_computerWhite;
    }

    private void createAnalyzeDialog()
    {
        m_analyzeDialog =
            new AnalyzeDialog(this, this, m_gtp.getSupportedCommands(),
                              m_analyzeCommands,
                              m_programAnalyzeCommands, m_gtp,
                              m_messageDialogs);
        m_actions.registerAll(m_analyzeDialog.getLayeredPane());
        m_analyzeDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionDisposeAnalyzeDialog();
                }
            });
        m_analyzeDialog.setBoardSize(getBoardSize());
        restoreSize(m_analyzeDialog, "analyze");
        m_analyzeDialog.setVisible(true);
    }

    private ContextMenu createContextMenu(GoPoint point)
    {
        ArrayList supportedCommands = null;
        boolean noProgram = (m_gtp == null);
        if (! noProgram)
            supportedCommands = m_gtp.getSupportedCommands();
        return new ContextMenu(point, noProgram, supportedCommands,
                               m_analyzeCommands,
                               m_programAnalyzeCommands,
                               m_guiBoard.getMark(point),
                               m_guiBoard.getMarkCircle(point),
                               m_guiBoard.getMarkSquare(point),
                               m_guiBoard.getMarkTriangle(point),
                               this);
    }

    private void createTree()
    {
        m_gameTreeViewer = new GameTreeViewer(this, this, m_messageDialogs);
        m_gameTreeViewer.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionDisposeTree();
                }
            });
        m_actions.registerAll(m_gameTreeViewer.getLayeredPane());
        m_gameTreeViewer.setLabelMode(m_treeLabels);
        m_gameTreeViewer.setSizeMode(m_treeSize);
        m_gameTreeViewer.setShowSubtreeSizes(m_showSubtreeSizes);
        restoreSize(m_gameTreeViewer, "tree");
        m_gameTreeViewer.update(getTree(), getCurrentNode());
        m_gameTreeViewer.setVisible(true);
    }

    private void createThumbnail(File file)
    {
        if (! ThumbnailPlatform.checkThumbnailSupport())
            return;
        // Thumbnail creation does not work on GNU classpath 0.90 yet
        if (Platform.isGnuClasspath())
            return;
        String path = file.getAbsolutePath();
        if (! path.startsWith("/tmp") && ! path.startsWith("/var/tmp"))
        {
            try
            {
                m_thumbnailCreator.create(file);
            }
            catch (ThumbnailCreator.Error e)
            {
            }
        }
    }

    private void currentNodeChanged()
    {
        updateFromGoBoard();
        if (m_gtp == null)
            return;
        boolean wasOutOfSync = isOutOfSync();
        try
        {
            ConstGameInformation info = getGameInformation();
            m_gtp.synchronize(getBoard(), info.getKomi(),
                              info.getTimeSettings());
        }
        catch (GtpError e)
        {
            if (! wasOutOfSync)
            {
                String name = getProgramName();
                String mainMessage =
                    "Could not synchronize position with " + name;
                String optionalMessage;
                if (e.getCommand() == null)
                {
                    optionalMessage =
                        "The current position could not be synchronized" +
                        " with " + name + " (" + e.getMessage() + ").\n";
                }
                else
                {
                    optionalMessage = formatCommand(e.getCommand());
                    optionalMessage = optionalMessage + " sent to " + name
                        + " failed.\n";
                    if (! e.getMessage().trim().equals(""))
                    {
                        optionalMessage = optionalMessage +
                            "The response was \"" + e.getMessage() + "\"";
                        if (! e.getMessage().endsWith("."))
                            optionalMessage = optionalMessage + ".";
                        optionalMessage = optionalMessage + "\n";
                    }
                }
                optionalMessage = optionalMessage
                    + "You will not be able to use functionality of "
                    + name + " in the current position.";
                showWarning(mainMessage, optionalMessage, true);
            }
        }
    }

    private void detachProgram()
    {
        if (m_gtp != null)            
            showStatusImmediately("Detaching program...");
        if (isCommandInProgress())
        {
            m_gtp.destroyGtp();
            m_gtp.close();
        }
        else
        {
            if (m_gtp != null && ! m_gtp.isProgramDead())
            {
                // Some programs do not handle closing the GTP stream
                // correctly, so we send a quit before
                try
                {
                    if (m_gtp.isSupported("quit"))
                        m_gtp.send("quit");
                }
                catch (GtpError e)
                {
                }
                m_gtp.close();
            }
        }
        m_gtp = null;
        if (m_analyzeCommand != null)
            clearAnalyzeCommand();
        m_version = null;
        m_shell.dispose();
        m_shell = null;
        if (m_analyzeDialog != null)
        {
            m_analyzeDialog.saveRecent();
            m_analyzeDialog.dispose();
            m_analyzeDialog = null;
        }
        resetBoard();
        clearStatus();
        setTitle();
    }

    private boolean endLengthyCommand()
    {
        return endLengthyCommand(true, true);
    }

    private boolean endLengthyCommand(boolean isCritical)
    {
        return endLengthyCommand(isCritical, true);
    }

    private boolean endLengthyCommand(boolean isCritical,
                                      boolean showError)
    {
        setBoardCursor(Cursor.DEFAULT_CURSOR);
        clearStatus();
        if (m_shell != null)
            m_shell.setCommandInProgess(false);
        // Program could have been killed in actionDetachProgram()
        if (m_gtp == null)
            return false;
        GtpError error = m_gtp.getException();
        updateViews(false);
        if (error != null && showError)
        {
            showError(error, isCritical);
            return false;
        }
        return true;
    }

    private String formatCommand(String command)
    {
        if (command == null)
            return "A command";
        if (command.length() < 20)
            return "The command \"" + command + "\"";
        GtpCommand cmd = new GtpCommand(command);
        return "The command \"" + cmd.getCommand() + " [...]\"";
    }

    private void generateMove(boolean isSingleMove)
    {
        GoColor toMove = getToMove();
        ConstNode node = getCurrentNode();
        ConstNode father = node.getFatherConst();
        ConstGameInformation info = getGameInformation();
        String playerToMove = info.getPlayer(toMove);
        String playerOther = info.getPlayer(toMove.otherColor());
        String name = getProgramName();
        if (! isSingleMove && m_file == null && playerToMove == null
            && (father == null
                || (! father.hasFather()
                    && (playerOther == null || playerOther.equals(name)))))
        {
            m_game.setPlayer(toMove, name);
            updateViews(false);
        }
        String command;
        if (NodeUtil.isInCleanup(getCurrentNode())
            && m_gtp.isSupported("kgs-genmove_cleanup"))
        {
            command = "kgs-genmove_cleanup";
            if (toMove == GoColor.BLACK)
                command += " b";
            else if (toMove == GoColor.WHITE)
                command += " w";
            else
                assert(false);
        }
        else
        {
            command = m_gtp.getCommandGenmove(toMove);
        }
        m_isSingleMove = isSingleMove;
        Runnable callback = new Runnable()
            {
                public void run()
                {
                    computerMoved();
                }
            };
        runLengthyCommand(command, callback);
    }

    private ConstBoard getBoard()
    {
        return m_game.getBoard();
    }

    private int getBoardSize()
    {
        return m_game.getSize();
    }

    private ConstClock getClock()
    {
        return m_game.getClock();
    }

    private ConstNode getCurrentNode()
    {
        return m_game.getCurrentNode();
    }

    private ConstGameInformation getGameInformation()
    {
        return m_game.getGameInformation(getCurrentNode());
    }

    private Komi getPrefsKomi()
    {
        try
        {
            String s = m_prefs.get("komi", "6.5");
            return Komi.parseKomi(s);
        }
        catch (Komi.InvalidKomi e)
        {
            return null;
        }
    }

    private GoColor getToMove()
    {
        return m_game.getToMove();
    }

    private ConstGameTree getTree()
    {
        return m_game.getTree();
    }

    private void gotoNode(ConstNode node)
    {
        // GameTreeViewer is not disabled in score mode
        if (m_scoreMode)
            return;
        m_game.gotoNode(node);
        currentNodeChanged();
    }

    private void humanMoved(Move move)
    {
        GoPoint point = move.getPoint();
        if (point != null && getBoard().getColor(point) != GoColor.EMPTY)
            return;
        if (point != null)
            paintImmediately(point, move.getColor(), true);
        if (m_gtp != null && ! isOutOfSync() && ! m_gtp.isProgramDead())
        {
            try
            {
                m_gtp.updateHumanMove(getBoard(), move);
            }
            catch (GtpError e)
            {
                showError(e);
                boardChangedBegin(false, false);
                return;
            }
        }
        boolean newNodeCreated = false;
        ConstNode node = NodeUtil.getChildWithMove(getCurrentNode(), move);
        if (node == null)
        {
            newNodeCreated = true;
            m_game.play(move);
        }
        else
        {
            m_game.haltClock();
            m_game.gotoNode(node);            
        }
        checkLostOnTime(move.getColor());
        m_resigned = false;
        boolean gameTreeChanged = newNodeCreated;
        ConstNode currentNode = getCurrentNode();
        if (newNodeCreated
            && currentNode.getFatherConst().getNumberChildren() == 1)
        {
            if (m_gameTreeViewer != null)
                m_gameTreeViewer.addNewSingleChild(currentNode);
            gameTreeChanged = false;
        }
        boardChangedBegin(true, gameTreeChanged);
    }

    private void importTextPosition(Reader reader)
    {
        try
        {
            TextParser parser = new TextParser();
            parser.parse(reader);
            GameTree tree =
                NodeUtil.makeTreeFromPosition(null, parser.getBoard());
            m_game.init(tree);
        }
        catch (ParseError e)
        {
            showError("Import failed", e);
        }
        m_guiBoard.initSize(getBoard().getSize());
        initGtp();
        boardChangedBegin(false, true);
    }

    private void initGame(int size)
    {
        // Not sure if it makes sense to keep analyze command if board is
        // cleared (we must clear it at least when board size changes,
        // because eplist could contain points out of board)
        clearAnalyzeCommand();
        int oldSize = getBoardSize();
        if (size != oldSize)
        {
            saveSession();
            m_guiBoard.initSize(size);
            restoreMainWindow(size);
            JLayeredPane layeredPane = getLayeredPane();
            if (layeredPane.isVisible())
            {
                // Loading a file with program attached can take long
                GuiUtil.paintImmediately(layeredPane);
            }            
        }
        ConstPointList handicap = Board.getHandicapStones(size, m_handicap);
        if (handicap == null)
        {
            String optionalMessage =
                "There is no standard definition for the location " +
                "of " + m_handicap + " handicap stones on boards of size "
                + size + ".\n" +
                "You need to do a manual setup or make White " +
                "play " + m_handicap + " passes instead.";
            showWarning("Handicap stone locations not defined",
                        optionalMessage, false);
        }
        m_game.init(size, getPrefsKomi(), handicap, m_prefs.get("rules", ""),
                    m_timeSettings);
        if (size != oldSize)
        {
            if (m_shell != null)
                restoreSize(m_shell, "shell");
            if (m_analyzeDialog != null)
            {
                restoreSize(m_analyzeDialog, "analyze");
                m_analyzeDialog.setBoardSize(size);
            }
            if (m_gameTreeViewer != null)
                restoreSize(m_gameTreeViewer, "tree");
        }
        updateFromGoBoard();
        resetBoard();
        m_game.resetClock();
        m_lostOnTimeShown = false;
        m_resigned = false;
        m_pattern = null;
    }

    private boolean initGtp()
    {
        if (m_gtp != null)
        {
            try
            {
                ConstGameInformation info = getGameInformation();
                m_gtp.initSynchronize(getBoard(), info.getKomi(),
                                      info.getTimeSettings());
            }
            catch (GtpError error)
            {
                showError(error);
                return false;
            }
        }
        currentNodeChanged();
        return ! isOutOfSync();
    }

    private void initialize()
    {
        m_bookmarks = Bookmark.load();
        m_menuBar.setBookmarks(m_bookmarks);
        m_programs = Program.load();
        m_menuBar.setPrograms(m_programs);
        if (m_programCommand == null)
        {
            int index = m_prefs.getInt("program", -1);
            if (index >= 0 && index < m_programs.size())
            {
                m_program = (Program)m_programs.get(index);
                m_programCommand = m_program.m_command;
            }
        }
        if (m_file == null)
            newGame(getBoardSize());
        else
            newGameFile(getBoardSize(), m_move);
        if (! m_prefs.getBoolean("show-info-panel", true))
            showInfoPanel(true);
        if (m_prefs.getBoolean("show-toolbar", true))
            showToolbar(true);
        restoreMainWindow(getBoardSize());
        // Attaching a program can take some time, so we want to make
        // the window visible, but not draw the window content yet
        getLayeredPane().setVisible(false);
        setVisible(true);
        if (m_programCommand != null)
        {
            attachProgram(m_programCommand, m_program);
            if (m_gtp == null || m_gtp.isProgramDead())
                m_prefs.putInt("program", -1);
        }
        setTitle();
        registerSpecialMacHandler();
        // Children dialogs should be set visible after main window, otherwise
        // they get minimize window buttons and a taskbar entry (KDE 3.4)
        if (m_shell != null && m_session.isVisible("shell"))
            m_shell.setVisible(true);
        if (m_session.isVisible("tree"))
            createTree();
        if (m_gtp != null && m_session.isVisible("analyze"))
            createAnalyzeDialog();
        if (! m_initAnalyze.equals(""))
        {
            AnalyzeCommand analyzeCommand =
                AnalyzeCommand.get(this, m_initAnalyze,
                                   m_gtp.getSupportedCommands(),
                                   m_analyzeCommands, m_programAnalyzeCommands,
                                   m_messageDialogs);
            if (analyzeCommand == null)
                showError("Unknown analyze command \"" + m_initAnalyze
                          + "\"",
                          "An analyze command with this label is not " +
                          "supported by " + getProgramName() + ".");
            else
                initAnalyzeCommand(analyzeCommand, false, true);
        }
        setTitleFromProgram();
        updateViews(true);
        getLayeredPane().setVisible(true);
        unprotectGui();
        toFrontLater();
        if (! m_initAnalyze.equals(""))
            analyzeBegin(true);
        else
            checkComputerMove();
    }

    private void initScore(ConstPointList deadStones)
    {
        resetBoard();
        GuiBoardUtil.scoreBegin(m_guiBoard, m_countScore, getBoard(),
                                deadStones);
        m_scoreMode = true;
        if (m_scoreDialog == null)
        {
            int scoringMethod = getGameInformation().parseRules();
            m_scoreDialog = new ScoreDialog(this, this, scoringMethod);
        }
        restoreLocation(m_scoreDialog, "score");
        Komi komi = getGameInformation().getKomi();
        m_scoreDialog.showScore(m_countScore, komi);
        m_scoreDialog.setVisible(true);
        showStatus("Please mark dead groups");
    }

    private void invokeAndWait(Runnable runnable)
    {
        try
        {
            SwingUtilities.invokeAndWait(runnable);
        }
        catch (InterruptedException e)
        {
            System.err.println("Thread interrupted");
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
            System.err.println("InvocationTargetException");
        }
    }

    private boolean isComputerBoth()
    {
        return (m_computerBlack && m_computerWhite);
    }

    private boolean isOutOfSync()
    {
        return (m_gtp != null && m_gtp.isOutOfSync());
    }

    private boolean loadFile(File file, int move)
    {
        try
        {
            FileInputStream in = new FileInputStream(file);
            LoadFileRunnable runnable = new LoadFileRunnable(in, file);
            if (file.length() > 500000)
            {
                newGame(getBoardSize()); // Frees space if already large tree
                GuiUtil.runProgress(this, "Loading...", runnable);
            }
            else
                runnable.run(null);
            SgfReader reader = runnable.getReader();
            GameTree tree = reader.getTree();
            initGame(tree.getBoardSize());
            m_menuBar.addRecent(file);
            m_game.init(tree);
            initGtp();
            if (move > 0)
            {
                ConstNode node =
                    NodeUtil.findByMoveNumber(getCurrentNode(), move); 
                if (node != null)
                    m_game.gotoNode(node);
            }
            setFile(file);
            String warnings = reader.getWarnings();
            if (warnings != null)
            {
                String optionalMessage =
                    "This file does not strictly follow the SGF standard. " +
                    "Some information might have been not read correctly " +
                    "or will be lost when modifying and saving the file.\n" +
                    "(" +
                    warnings.replaceAll("\n\\z", ")").replaceAll("\n", ")\n(");
                showWarning("Non-standard SGF file", optionalMessage, true);
            }
            FileDialogs.setLastFile(file);
            m_computerBlack = false;
            m_computerWhite = false;
            createThumbnail(file);
        }
        catch (FileNotFoundException e)
        {
            showError("File not found", e);
            return false;
        }
        catch (SgfReader.SgfError e)
        {
            showError("Could not read file", e);
            return false;
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assert(false);
            return false;
        }
        return true;
    }

    private void newGame(int size)
    {
        initGame(size);
        initGtp();
        updateFromGoBoard();
        setTitle();
        setTitleFromProgram();
        clearStatus();
    }

    private void newGameFile(int size, int move)
    {
        initGame(size);
        if (! loadFile(m_file, move))
            m_file = null;
    }

    /** Paint point immediately to pretend better responsiveness.
        Necessary because waiting for a repaint of the Go board can be slow
        due to the updating game tree or response to GTP commands.
    */
    private void paintImmediately(GoPoint point, GoColor color, boolean isMove)
    {
        m_guiBoard.setColor(point, color);
        if (isMove && m_showLastMove)
            m_guiBoard.markLastMove(point);
        m_guiBoard.paintImmediately(point);
    }

    private void protectGui()
    {
        getGlassPane().setVisible(true);
        setCursor(getGlassPane(), Cursor.WAIT_CURSOR);
    }

    private void registerSpecialMacHandler()
    {        
        if (! Platform.isMac())
            return;
        Platform.SpecialMacHandler handler = new Platform.SpecialMacHandler()
            {
                public boolean handleAbout()
                {
                    assert(SwingUtilities.isEventDispatchThread());
                    actionAbout();
                    return true;
                }
                
                public boolean handleOpenFile(String filename)
                {
                    assert(SwingUtilities.isEventDispatchThread());
                    if (! checkSaveGame())
                        return true;
                    loadFile(new File(filename), -1);
                    boardChangedBegin(false, true);
                    return true;
                }
                
                public boolean handleQuit()
                {
                    assert(SwingUtilities.isEventDispatchThread());
                    close();
                    // close() calls System.exit() if not cancelled
                    return false;
                }
            };
        Platform.registerSpecialMacHandler(handler);
    }

    private void resetBoard()
    {
        clearStatus();
        m_guiBoard.clearAll();
        updateFromGoBoard();
        updateGuiBoard();
    }
    
    private void restoreLocation(JDialog dialog, String name)
    {
        m_session.restoreLocation(dialog, this, name + "-" + getBoardSize());
    }

    private void restoreMainWindow(int size)
    {
        setState(Frame.NORMAL);
        m_session.restoreLocation(this, "main-" + size);
        String path = "windows/main/size-" + size + "/fieldsize";
        int fieldSize = m_prefs.getInt(path, -1);
        if (fieldSize > 0)
            m_guiBoard.setPreferredFieldSize(new Dimension(fieldSize,
                                                           fieldSize));
        path = "windows/main/size-" + size + "/comment";
        int width = m_prefs.getInt(path + "/width", -1);
        int height = m_prefs.getInt(path + "/height", -1);
        Dimension preferredCommentSize = null;
        if (width > 0 && height > 0)
        {
            preferredCommentSize = new Dimension(width, height);
            m_comment.setPreferredSize(preferredCommentSize);
        }
        else
            m_comment.setPreferredSize();
        m_splitPane.resetToPreferredSizes();
        pack();
        // To avoid smallish empty borders (less than one field size) on top
        // and bottom borders of the board we adjust the comment size slightly
        // if necessary
        if (m_infoPanel.getHeight() - m_guiBoard.getHeight() < 2 * fieldSize
            && preferredCommentSize != null && fieldSize > 0)
        {
            preferredCommentSize.height -= 2 * fieldSize;
            m_comment.setPreferredSize(preferredCommentSize);
            m_splitPane.resetToPreferredSizes();
            pack();
        }
    }

    private void restoreSize(JDialog dialog, String name)
    {
        m_session.restoreSize(dialog, this, name + "-" + getBoardSize());
    }

    private void runLengthyCommand(String cmd, Runnable callback)
    {
        assert(m_gtp != null);
        m_gtp.send(cmd, callback);
        beginLengthyCommand();
    }

    /** Save game to file.
        @return true If successfully saved.
    */
    private boolean save(File file)
    {
        OutputStream out;
        try
        {
            out = new FileOutputStream(file);
        }
        catch (FileNotFoundException e)
        {
            showError("Saving file failed", e);
            return false;
        }
        new SgfWriter(out, getTree(), "GoGui", Version.get());
        m_menuBar.addRecent(file);
        createThumbnail(file);
        setFile(file);
        m_game.clearModified();
        updateViews(false);
        return true;
    }

    private boolean saveDialog()
    {
        File file = FileDialogs.showSaveSgf(this, m_messageDialogs);
        if (file == null)
            return false;
        return save(file);
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, getBoard(), "GoGui", Version.get());
        m_menuBar.addRecent(file);
        updateViews(false);
    }

    private void saveSession()
    {        
        if (m_shell != null)
            m_shell.saveHistory();
        if (m_analyzeDialog != null)
            m_analyzeDialog.saveRecent();
        if (! isVisible()) // can that happen?
            return;
        if (m_help != null)
            m_session.saveSize(m_help, "help");
        saveSizeAndVisible(m_gameTreeViewer, "tree");
        if (m_gtp != null)
        {
            saveSizeAndVisible(m_shell, "shell");
            saveSizeAndVisible(m_analyzeDialog, "analyze");
        }
        m_session.saveLocation(this, "main-" + getBoardSize());
        if (GuiUtil.isNormalSizeMode(this))
        {            
            String name = "windows/main/size-" + getBoardSize() + "/fieldsize";
            int fieldSize = m_guiBoard.getFieldSize().width;
            if (fieldSize == 0) // BoardPainter was never invoked
                return;
            m_prefs.putInt(name, fieldSize);
            name = "windows/main/size-" + getBoardSize() + "/comment/width";
            m_prefs.putInt(name, m_comment.getWidth());
            name = "windows/main/size-" + getBoardSize() + "/comment/height";
            m_prefs.putInt(name, m_comment.getHeight());
        }
    }

    private void saveLocation(JDialog dialog, String name)
    {
        m_session.saveLocation(dialog, this, name + "-" + getBoardSize());
    }

    private void saveSizeAndVisible(JDialog dialog, String name)
    {
        int size = getBoardSize();
        if (dialog != null)
            m_session.saveSize(dialog, this, name + "-" + size);
        m_session.saveVisible(dialog, name);
    }

    private void scoreContinue()
    {
        boolean success = endLengthyCommand();
        clearStatus();
        PointList isDeadStone = null;
        if (success)
        {
            String response = m_gtp.getResponse();
            try
            {
                isDeadStone
                    = GtpUtil.parsePointList(response, getBoardSize());
            }
            catch (GtpResponseFormatError e)
            {
                showError(e);
            }
        }
        initScore(isDeadStone);
        updateViews(false);
    }    

    private void scoreDone(Score score)
    {
        if (! m_scoreMode)
            return;
        m_scoreMode = false;
        saveLocation(m_scoreDialog, "score");
        m_scoreDialog.setVisible(false);
        clearStatus();
        m_guiBoard.clearAll();
        if (score != null)
            setResult(score.formatResult());
    }

    private void sendGtp(Reader reader)
    {
        if (m_gtp == null)
            return;
        java.io.BufferedReader in;
        in = new BufferedReader(reader);
        try
        {
            while (true)
            {
                try
                {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    if (! GtpUtil.isCommand(line))
                        continue;
                    if (GtpUtil.isStateChangingCommand(line))
                    {
                        showError("Board changing commands not allowed",
                                  "");
                        break;
                    }
                    if (! sendGtpCommandSync(line))
                        break;
                }
                catch (IOException e)
                {
                    showError("Error reading file", e);
                    break;
                }
                catch (GtpError e)
                {
                    showError(e);
                    break;
                }
            }
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    private void sendGtpFile(File file)
    {
        try
        {
            sendGtp(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            showError("File not found", e);
        }
    }

    private void sendGtpString(String commands)
    {        
        commands = commands.replaceAll("\\\\n", "\n");
        sendGtp(new StringReader(commands));
    }

    private void setBoardCursor(int type)
    {
        setCursor(m_guiBoard, type);
    }

    private void setBoardCursorDefault()
    {
        setCursorDefault(m_guiBoard);
    }

    private void setCursor(Component component, int type)
    {
        Cursor cursor = Cursor.getPredefinedCursor(type);
        component.setCursor(cursor);
    }

    private void setCursorDefault(Component component)
    {
        component.setCursor(Cursor.getDefaultCursor());
    }

    private void setFile(File file)
    {
        m_file = file;
        setTitle();
    }

    private void setMinimumSize()
    {
        // setMinimumSize not available in Java 1.4
        /*
        int width = 128;
        int height = 32;
        Insets rootInsets = getRootPane().getInsets();
        int rootInsetsWidth = rootInsets.left + rootInsets.right;
        Dimension menuBarSize = getJMenuBar().getPreferredSize();
        width = Math.max(width, (int)menuBarSize.getWidth() + rootInsetsWidth);
        height = Math.max(height, (int)menuBarSize.getHeight());
        if (m_showToolbar)
        {
            Insets contentInsets = getContentPane().getInsets();
            int contentInsetsWidth = contentInsets.left + contentInsets.right;
            Dimension toolBarSize = m_toolBar.getPreferredSize();
            width = Math.max(width,
                             (int)toolBarSize.getWidth() + rootInsetsWidth
                             + contentInsetsWidth + GuiUtil.PAD);
            height += (int)toolBarSize.getHeight();
        }
        height += 224;
        setMinimumSize(new Dimension(width, height));
        */
    }

    private void setResult(String result)
    {
        String oldResult = getGameInformation().getResult();
        if (! (oldResult == null || oldResult.equals("")
               || oldResult.equals(result))
            && ! showQuestion("Replace old result " + oldResult + "\n" +
                              "with " + result + "?",
                              "The old result in the game information will " +
                              "be overwritten.", "Replace", false))
            return;
        m_game.setResult(result);
    }

    private void setTitle()
    {
        if (m_titleFromProgram != null)
        {
            setTitle(m_titleFromProgram);
            return;
        }
        String appName = "GoGui";        
        if (m_gtp != null)
            appName = getProgramName();
        String filename = null;
        if (m_file != null)
        {
            filename = m_file.getName();
            if (isModified())
                filename = filename + " [modified]";
        }
        else if (isModified())
            filename = "[modified]";
        ConstGameInformation info = getGameInformation();
        String gameName = info.suggestGameName();
        if (gameName != null)
        {
            if (filename != null)
                gameName = filename + "  " + gameName;
        }
        else if (filename != null)
            gameName = filename;
        if (gameName == null)
            setTitle(appName);        
        else
        {
            String name = getProgramName();
            if (! appName.equals("GoGui")
                && (ObjectUtil.equals(info.getPlayer(GoColor.BLACK), name)
                    || ObjectUtil.equals(info.getPlayer(GoColor.WHITE), name)))
                setTitle(gameName);
            else
                setTitle(gameName + " - " + appName);
        }
    }

    private void setTitleFromProgram()
    {
        if (m_gtp == null)
            m_titleFromProgram = null;
        else
            m_titleFromProgram = m_gtp.getTitleFromProgram();
        if (m_titleFromProgram != null)
            setTitle(m_titleFromProgram);
    }

    private void setup(GoPoint point, GoColor color)
    {
        assert(point != null);
        m_game.setup(point, color);
    }

    private void setupDone()
    {
        if (! m_setupMode)
            return;
        m_setupMode = false;
        if (getCurrentNode().hasSetup() || m_setupColor != getToMove())
            m_game.setToMove(m_setupColor);
        currentNodeChanged();
    }

    private void showError(String message, Exception e)
    {
        showError(message, e, true);
    }
    
    private void showError(String message, Exception e, boolean isCritical)
    {
        m_messageDialogs.showError(this, message, e, isCritical);
    }

    private void showError(GtpError error)
    {        
        showError(error, true);
    }

    private void showError(GtpResponseFormatError e)
    {        
        String mainMessage = "Invalid response";
        String optionalMessage =
            getProgramName() + " sent a response in an unexpected format ("
            + e.getMessage() + ").";
        showError(mainMessage, optionalMessage, true);
    }

    private void showError(GtpError e, boolean isCritical)
    {        
        String name = getProgramName();
        String mainMessage;
        String optionalMessage;
        if (m_gtp != null && m_gtp.isProgramDead())
        {
            mainMessage = name + " terminated unexpectedly";
            optionalMessage =
                "Check the GTP shell window for error messages of " + name +
                " that might be helpful to find the reason for " +
                " this unexpected failure.\n" +
                "You can reattach " + name + " from the Program menu.";
        }
        else if (e instanceof GtpClient.ExecFailed)
        {
            String program = ((GtpClient.ExecFailed)e).m_program;
            mainMessage = "Could not execute Go program";
            optionalMessage =
                "The Go program could not be executed using the command" +
                " \"" + program + "\"";
            if (! StringUtil.isEmpty(e.getMessage()))
                optionalMessage = optionalMessage +
                    " (" + e.getMessage() + ")";
            optionalMessage = optionalMessage
                + ".\nPlease correct the command for executing the program.";
        }
        else
        {
            mainMessage = "Command failed";
            optionalMessage = formatCommand(e.getCommand());
            optionalMessage = optionalMessage + " sent to " + name
                + " failed.";
            if (! e.getMessage().trim().equals(""))
            {
                optionalMessage = optionalMessage + " The response was \""
                    + e.getMessage() + "\"";
                if (! e.getMessage().endsWith("."))
                    optionalMessage = optionalMessage + ".";
            }
        }
        showError(mainMessage, optionalMessage, isCritical);
    }

    private void showError(String mainMessage, String optionalMessage)
    {
        showError(mainMessage, optionalMessage, true);
    }

    private void showError(String mainMessage, String optionalMessage,
                           boolean isCritical)
    {
        m_messageDialogs.showError(this, mainMessage, optionalMessage,
                                   isCritical);
    }

    private void showGameFinished()
    {
        String disableKey = "net.sf.gogui.gogui.GoGui.game-finished";
        String optionalMessage;
        if (m_resigned)
            optionalMessage =
                getProgramName() + " lost the game by resignation.";
        else
            optionalMessage =
                "The game is finished because both players passed. " +
                "Use Score from the Game menu to count the score " +
                "in final positions.";
        m_messageDialogs.showInfo(disableKey, this, "Game finished",
                                  optionalMessage, false);
    }

    private void showInfo(String mainMessage, String optionalMessage,
                          boolean isCritical)
    {
        m_messageDialogs.showInfo(this, mainMessage, optionalMessage,
                                  isCritical);
    }

    private void showInfoPanel(boolean enable)
    {
        if (enable == m_showInfoPanel)
            return;
        m_prefs.putBoolean("show-info-panel", enable);
        m_showInfoPanel = enable;
        if (enable)
        {
            m_innerPanel.remove(m_guiBoard);
            m_splitPane.add(m_guiBoard);
            m_innerPanel.add(m_splitPane);
        }
        else
        {
            m_splitPane.remove(m_guiBoard);
            m_innerPanel.remove(m_splitPane);
            m_innerPanel.add(m_guiBoard);
        }
        m_splitPane.resetToPreferredSizes();
        pack();
    }

    private boolean showOptionalQuestion(String id, String mainMessage,
                                         String optionalMessage,
                                         String destructiveOption,
                                         boolean isCritical)
    {
        String disableKey = "net.sf.gogui.gogui.GoGui" + id;
        return m_messageDialogs.showQuestion(disableKey, this, mainMessage,
                                             optionalMessage,
                                             destructiveOption, isCritical);
    }

    private boolean showQuestion(String mainMessage, String optionalMessage,
                                 String destructiveOption, boolean isCritical)
    {
        return m_messageDialogs.showQuestion(this, mainMessage,
                                             optionalMessage,
                                             destructiveOption, isCritical);
    }

    private void showStatus(String text)
    {
        m_statusBar.setText(text);
    }

    private void showStatusImmediately(String text)
    {
        m_statusBar.immediatelyPaintText(text);
    }

    private void showStatusSelectPointList()
    {
        showStatus("Select points for " + m_analyzeCommand.getLabel()
                   + " (last point with right button or modifier key down)");
    }

    private void showStatusSelectTarget()
    {
        showStatus("Select a target for "
                   + m_analyzeCommand.getResultTitle());
    }

    private void showToolbar(boolean enable)
    {
        if (enable == m_showToolbar)
            return;
        m_prefs.putBoolean("show-toolbar", enable);
        m_showToolbar = enable;
        if (enable)
        {
            getContentPane().add(m_toolBar, BorderLayout.NORTH);
            m_menuBar.setHeaderStyleSingle(false);
        }
        else
        {
            getContentPane().remove(m_toolBar);
            m_menuBar.setHeaderStyleSingle(true);
        }
        m_splitPane.resetToPreferredSizes();
        setMinimumSize();
        pack();
    }

    private void showWarning(String mainMessage, String optionalMessage,
                             boolean isCritical)
    {
        m_messageDialogs.showWarning(this, mainMessage, optionalMessage,
                                     isCritical);
    }

    private void toFrontLater()
    {
        // Calling toFront() directly does not give the focus to this
        // frame, if dialogs are open
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    requestFocus();
                    toFront();
                }
            });
    }

    private void unprotectGui()
    {
        getGlassPane().setVisible(false);
        setCursor(getGlassPane(), Cursor.DEFAULT_CURSOR);
    }

    /** Update all views.
        @param gameTreeChanged If nodes were added to or removed from the game
        tree, which will trigger a full and potentially slow game tree update
    */
    private void updateViews(boolean gameTreeChanged)
    {
        ConstGame game = getGame();
        m_actions.update();
        m_toolBar.update();
        m_menuBar.update(isProgramAttached(), isTreeShown(), isShellShown());
        m_gameInfo.update(game);
        m_comment.setComment(getCurrentNode().getComment());
        updateFromGoBoard();
        updateGuiBoard();
        getRootPane().putClientProperty("windowModified",
                                        Boolean.valueOf(isModified()));
        setTitle();
        if (m_analyzeDialog != null)
            m_analyzeDialog.setSelectedColor(getToMove());
        GoGuiUtil.updateMoveText(m_statusBar, game);
        m_statusBar.setSetupMode(m_setupMode);
        if (m_setupMode)
            m_statusBar.setToPlay(m_setupColor);
        m_statusBar.setScoreMode(m_scoreMode);
        if (m_gameTreeViewer != null)
        {
            if (! gameTreeChanged)
                m_gameTreeViewer.update(getCurrentNode());
            else
            {
                protectGui();
                showStatus("Updating game tree window...");
                Runnable runnable = new Runnable() {
                        public void run() {
                            try
                            {
                                m_gameTreeViewer.update(getTree(),
                                                        getCurrentNode());
                            }
                            finally
                            {
                                unprotectGui();
                                clearStatus();
                            }
                        }
                    };
                SwingUtilities.invokeLater(runnable);
            }
        }
    }

    private void updateFromGoBoard()
    {
        GuiBoardUtil.updateFromGoBoard(m_guiBoard, getBoard(), m_showLastMove);
        if (getCurrentNode().getMove() == null)
            m_guiBoard.markLastMove(null);
    }

    private void updateGuiBoard()
    {
        if (m_showVariations)
        {
            ConstPointList childrenMoves
                = NodeUtil.getChildrenMoves(getCurrentNode());
            GuiBoardUtil.showChildrenMoves(m_guiBoard, childrenMoves);
        }
        GuiBoardUtil.showMarkup(m_guiBoard, getCurrentNode());
    }
}
