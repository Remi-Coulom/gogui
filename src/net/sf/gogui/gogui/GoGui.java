// GoGui.java

package net.sf.gogui.gogui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
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
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.gamefile.GameFile;
import net.sf.gogui.gamefile.GameReader;
import net.sf.gogui.gamefile.GameWriter;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.CountScore;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.go.Score;
import net.sf.gogui.go.Score.ScoringMethod;
import static net.sf.gogui.gogui.I18n.i18n;
import net.sf.gogui.gtp.AnalyzeCommand;
import net.sf.gogui.gtp.AnalyzeDefinition;
import net.sf.gogui.gtp.AnalyzeType;
import net.sf.gogui.gtp.AnalyzeUtil;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpClientUtil;
import net.sf.gogui.gtp.GtpCommand;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpResponseFormatError;
import net.sf.gogui.gtp.GtpSynchronizer;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.gui.AnalyzeDialog;
import net.sf.gogui.gui.AnalyzeShow;
import net.sf.gogui.gui.BoardSizeDialog;
import net.sf.gogui.gui.Bookmark;
import net.sf.gogui.gui.BookmarkEditor;
import net.sf.gogui.gui.Comment;
import net.sf.gogui.gui.ConstGuiBoard;
import net.sf.gogui.gui.ContextMenu;
import net.sf.gogui.gui.FindDialog;
import net.sf.gogui.gui.GameInfoDialog;
import net.sf.gogui.gui.GameInfoPanel;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.gui.GameTreeViewer;
import net.sf.gogui.gui.GtpShell;
import net.sf.gogui.gui.GuiAction;
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
import net.sf.gogui.gui.TimeLeftDialog;
import net.sf.gogui.sgf.SgfError;
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
import net.sf.gogui.util.LineReader;
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
               ContextMenu.Listener, LiveGfx.Listener
{
    public enum ShowVariations
    {
        CHILDREN,

        SIBLINGS,

        NONE
    }

    public GoGui(String program, File file, int move, String time,
                 boolean verbose, boolean initComputerColor,
                 boolean computerBlack, boolean computerWhite, boolean auto,
                 boolean register, String gtpFile, String gtpCommand,
                 File analyzeCommandsFile)
        throws GtpError, ErrorMessage
    {
        int boardSize = m_prefs.getInt("boardsize", GoPoint.DEFAULT_SIZE);
        m_beepAfterMove = m_prefs.getBoolean("beep-after-move", true);
        m_initialFile = file;
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_analyzeCommandsFile = analyzeCommandsFile;
        m_move = move;
        m_register = register;
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
        m_showInfoPanel = true;
        m_showToolbar = false;

        Container contentPane = getContentPane();
        m_innerPanel = new JPanel(new BorderLayout());
        contentPane.add(m_innerPanel, BorderLayout.CENTER);
        m_toolBar = new GoGuiToolBar(this);

        m_infoPanel = new JPanel(new BorderLayout());
        m_game = new Game(boardSize);
        m_gameInfoPanel = new GameInfoPanel(m_game);
        m_gameInfoPanel.setBorder(GuiUtil.createSmallEmptyBorder());
        m_infoPanel.add(m_gameInfoPanel, BorderLayout.NORTH);
        m_guiBoard = new GuiBoard(boardSize);
        m_showAnalyzeText = new ShowAnalyzeText(this, m_guiBoard);

        m_statusBar = new StatusBar();
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
                    GoGui.this.textSelected(text);
                }
            };
        m_comment = new Comment(commentListener);
        boolean monoFont = m_prefs.getBoolean("comment-font-fixed", false);
        m_comment.setMonoFont(monoFont);
        m_infoPanel.add(m_comment, BorderLayout.CENTER);
        m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     m_guiBoard, m_infoPanel);
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
                    actionSendFile(file);
                }
            };
        m_menuBar = new GoGuiMenuBar(m_actions, recentListener, recentGtp,
                                     this);
        // enums are stored as int's for compatibility with earlier versions
        // of GoGui
        try
        {
            m_treeLabels =
                GameTreePanel.Label.values()[
                         m_prefs.getInt("gametree-labels",
                                        GameTreePanel.Label.NUMBER.ordinal())];
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            m_treeLabels = GameTreePanel.Label.NUMBER;
        }
        try
        {
            m_treeSize =
                GameTreePanel.Size.values()[
                         m_prefs.getInt("gametree-size",
                                        GameTreePanel.Size.NORMAL.ordinal())];
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            m_treeSize = GameTreePanel.Size.NORMAL;
        }

        try
        {
            m_showVariations =
                ShowVariations.values()[
                           m_prefs.getInt("show-variations",
                                          ShowVariations.CHILDREN.ordinal())];
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            m_showVariations = ShowVariations.CHILDREN;
        }

        m_showSubtreeSizes =
            m_prefs.getBoolean("gametree-show-subtree-sizes", false);
        m_autoNumber = m_prefs.getBoolean("gtpshell-autonumber", false);
        m_commandCompletion =
            ! m_prefs.getBoolean("gtpshell-disable-completions", false);
        m_timeStamp = m_prefs.getBoolean("gtpshell-timestamp", false);
        m_showLastMove = m_prefs.getBoolean("show-last-move", true);
        m_showMoveNumbers = m_prefs.getBoolean("show-move-numbers", false);
        boolean showCursor = m_prefs.getBoolean("show-cursor", false);
        boolean showGrid = m_prefs.getBoolean("show-grid", false);
        m_guiBoard.setShowCursor(showCursor);
        m_guiBoard.setShowGrid(showGrid);
        setJMenuBar(m_menuBar);
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
        AboutDialog.show(this, getProgramLabel(), m_version, command,
                         m_messageDialogs);
    }

    public void actionAddBookmark()
    {
        if (m_gameFile == null)
        {
            showError(i18n("MSG_CANNOT_SET_BOOKMARK_NO_FILE"),
                      i18n("MSG_CANNOT_SET_BOOKMARK_NO_FILE_2"),
                      false);
            return;
        }
        if (isModified())
        {
            showError(i18n("MSG_CANNOT_SET_BOOKMARK_MODIFIED"),
                      i18n("MSG_CANNOT_SET_BOOKMARK_MODIFIED_2"),
                      false);
            return;
        }
        if (getCurrentNode().getFatherConst() != null
            && getCurrentNode().getMove() == null)
        {
            showError(i18n("MSG_CANNOT_SET_BOOKMARK_NODE"),
                      i18n("MSG_CANNOT_SET_BOOKMARK_NODE_2"),
                      false);
            return;
        }
        String variation = NodeUtil.getVariationString(getCurrentNode());
        int move = NodeUtil.getMoveNumber(getCurrentNode());
        Bookmark bookmark = new Bookmark(m_gameFile.m_file, move, variation);
        BookmarkEditor editor = new BookmarkEditor();
        bookmark = editor.editItem(this, i18n("TIT_ADD_BOOKMARK"), bookmark,
                                   true, m_messageDialogs);
        if (bookmark == null)
            return;
        m_bookmarks.add(bookmark);
        m_menuBar.setBookmarks(m_bookmarks);
        Bookmark.save(m_bookmarks);
    }

    public void actionAttachProgram(int index)
    {
        m_prefs.putInt("program", index);
        actionAttachProgram(m_programs.get(index));
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
        if (size < 1 || size > GoPoint.MAX_SIZE)
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
        m_game.resumeClock();
        updateViews(false);
    }

    public void actionClockStart()
    {
        if (getClock().isRunning())
            return;
        m_game.startClock();
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
        String disableKey = "net.sf.gogui.gogui.GoGui.delete-side-variations";
        if (! m_messageDialogs.showQuestion(disableKey, this,
                                            i18n("MSG_DELETE_VARIATIONS"),
                                            i18n("MSG_DELETE_VARIATIONS_2"),
                                            i18n("LB_DELETE"),
                                            false))
            return;
        m_game.keepOnlyMainVariation();
        boardChangedBegin(false, true);
    }

    public void actionDetachProgram()
    {
        if (m_gtp == null)
            return;
        if (isCommandInProgress()
            && ! showQuestion(format(i18n("MSG_TERMINATE_COMMAND_IN_PROGRESS"),
                                     getProgramLabel()),
                              i18n("MSG_TERMINATE_COMMAND_IN_PROGRESS_2"),
                              i18n("LB_TERMINATE"), true))
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

    public void actionHelp()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/doc/index.html");
        if (url == null)
        {
            showError(i18n("MSG_HELP_NOT_FOUND"), "");
            return;
        }
        if (m_help == null)
        {
            m_help = new Help(url, m_messageDialogs,
                              i18n("TIT_HELP") + " - " + i18n("LB_GOGUI"));
            m_session.restoreSize(m_help.getWindow(), "help");
        }
        m_help.getWindow().setVisible(true);
        m_help.getWindow().toFront();
    }

    public void actionEditBookmarks()
    {
        BookmarkEditor editor = new BookmarkEditor();
        ObjectListEditor<Bookmark> listEditor =
            new ObjectListEditor<Bookmark>();
        if (! listEditor.edit(this, i18n("TIT_EDIT_BOOKMARKS"), m_bookmarks,
                              editor, m_messageDialogs))
            return;
        m_menuBar.setBookmarks(m_bookmarks);
        Bookmark.save(m_bookmarks);
    }

    public void actionEditLabel(GoPoint point)
    {
        String value = getCurrentNode().getLabel(point);
        Object message = format(i18n("MSG_EDIT_LABEL"), point);
        value = (String)JOptionPane.showInputDialog(this, message,
                                                    i18n("TIT_EDIT_LABEL"),
                                                    JOptionPane.PLAIN_MESSAGE,
                                                    null, null, value);
        if (value == null)
            return;
        m_game.setLabel(point, value);
        m_guiBoard.setLabel(point, value);
        updateViews(false);
    }

    public void actionEditPrograms()
    {
        ProgramEditor editor = new ProgramEditor();
        ObjectListEditor<Program> listEditor = new ObjectListEditor<Program>();
        if (! listEditor.edit(this, i18n("TIT_EDIT_PROGRAMS"), m_programs,
                              editor, m_messageDialogs))
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
        File file = showSave(i18n("TIT_EXPORT_LATEX"));
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title =
                FileUtil.removeExtension(new File(file.getName()), "tex");
            new TexWriter(title, out, getTree());
        }
        catch (FileNotFoundException e)
        {
            showError(i18n("MSG_EXPORT_FAILED"), e);
        }
    }

    public void actionExportLatexPosition()
    {
        File file = showSave(i18n("TIT_EXPORT_LATEX_POSITION"));
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title = FileUtil.removeExtension(new File(file.getName()),
                                                     "tex");
            new TexWriter(title, out, getBoard(),
                          GuiBoardUtil.getLabels(m_guiBoard),
                          GuiBoardUtil.getMark(m_guiBoard),
                          GuiBoardUtil.getMarkTriangle(m_guiBoard),
                          GuiBoardUtil.getMarkCircle(m_guiBoard),
                          GuiBoardUtil.getMarkSquare(m_guiBoard),
                          GuiBoardUtil.getSelects(m_guiBoard));
        }
        catch (FileNotFoundException e)
        {
            showError(i18n("MSG_EXPORT_FAILED"), e);
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
            showError(i18n("MSG_EXPORT_FAILED"), e);
        }
    }

    public void actionExportTextPosition()
    {
        File file = showSave(i18n("MSG_EXPORT_TEXT"));
        if (file == null)
            return;
        try
        {
            String text = BoardUtil.toString(getBoard(), false, false);
            PrintStream out = new PrintStream(file);
            out.print(text);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            showError(i18n("MSG_EXPORT_FAILED"), e);
        }
    }

    public void actionExportPng()
    {
        ExportPng.run(this, m_guiBoard, m_prefs, m_messageDialogs);
    }

    public void actionExportTextPositionToClipboard()
    {
        GuiUtil.copyToClipboard(BoardUtil.toString(getBoard(), false, false));
    }

    public void actionFind()
    {
        if (! checkStateChangePossible())
            return;
        Pattern pattern = FindDialog.run(this, m_comment.getSelectedText(),
                                         m_messageDialogs);
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
        showStatus(i18n("STAT_FIND_SEARCHING_COMMENTS"));
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        ConstNode root = getTree().getRootConst();
                        ConstNode currentNode = getCurrentNode();
                        ConstNode node =
                            NodeUtil.findInComments(currentNode, m_pattern);
                        boolean cancel = false;
                        if (node == null && getCurrentNode() != root)
                        {
                            unprotectGui();
                            if (showQuestion(i18n("MSG_FIND_CONTINUE"),
                                             i18n("MSG_FIND_CONTINUE_2"),
                                             i18n("LB_FIND_CONTINUE"), false))
                            {
                                protectGui();
                                node = root;
                                if (! NodeUtil.commentContains(node,
                                                               m_pattern))
                                    node =
                                        NodeUtil.findInComments(node,
                                                                m_pattern);
                            }
                            else
                                cancel = true;
                        }
                        if (! cancel)
                        {
                            if (node == null)
                            {
                                unprotectGui();
                                showInfo(i18n("MSG_FIND_NOT_FOUND"),
                                         format(i18n("MSG_FIND_NOT_FOUND_2"),
                                                m_pattern),
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

    public void actionFindNextComment()
    {
        if (! checkStateChangePossible())
            return;
        protectGui();
        showStatus(i18n("STAT_FIND_SEARCHING_COMMENTS"));
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        ConstNode root = getTree().getRootConst();
                        ConstNode currentNode = getCurrentNode();
                        ConstNode node = NodeUtil.findNextComment(currentNode);
                        boolean cancel = false;
                        if (node == null && getCurrentNode() != root)
                        {
                            unprotectGui();
                            if (showQuestion(i18n("MSG_FIND_CONTINUE"),
                                             i18n("MSG_FIND_CONTINUE_2"),
                                             i18n("LB_FIND_CONTINUE"), false))
                            {
                                protectGui();
                                node = root;
                                if (! node.hasComment())
                                    node = NodeUtil.findNextComment(node);
                            }
                            else
                                cancel = true;
                        }
                        if (! cancel)
                        {
                            if (node == null)
                            {
                                unprotectGui();
                                showInfo(i18n("MSG_FIND_NO_COMMENT_FOUND"),
                                         null, false);
                            }
                            else
                            {
                                gotoNode(node);
                                boardChangedBegin(false, false);
                            }
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
        ConstNode node = m_game.getGameInfoNode();
        GameInfo info = new GameInfo(node.getGameInfoConst());
        GameInfoDialog.show(this, info, m_messageDialogs);
        m_game.setGameInfo(info, node);
        currentNodeChanged(); // updates komi, time settings
        Komi prefsKomi = getPrefsKomi();
        Komi komi = info.getKomi();
        if (komi != null && ! komi.equals(prefsKomi) && info.getHandicap() == 0)
            m_prefs.put("komi", komi.toString());
        if (info.getTimeSettings() != null
            && ! info.getTimeSettings().equals(m_timeSettings))
        {
            TimeSettings timeSettings = info.getTimeSettings();
            m_game.setTimeSettings(timeSettings);
            m_timeSettings = timeSettings;
        }
        setTitle();
        updateViews(false);
    }

    public void actionGotoBookmark(int i)
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        if (i < 0 || i >= m_bookmarks.size())
            return;
        Bookmark bookmark = m_bookmarks.get(i);
        if (! loadFile(bookmark.m_file, -1))
            return;
        updateViews(true);
        String variation = bookmark.m_variation;
        ConstNode node = getTree().getRootConst();
        if (! variation.equals(""))
        {
            node = NodeUtil.findByVariation(node, variation);
            if (node == null)
            {
                showError(i18n("MSG_BOOKMARK_INVALID_VARIATION"), "");
                return;
            }
        }
        node = NodeUtil.findByMoveNumber(node, bookmark.m_move);
        if (node == null)
        {
            showError(i18n("MSG_BOOKMARK_INVALID_MOVE_NUMBER"), "");
            return;
        }
        actionGotoNode(node);
    }

    public void actionGotoMove()
    {
        if (! checkStateChangePossible())
            return;
        ConstNode node = MoveNumberDialog.show(this, getCurrentNode(),
                                               m_messageDialogs);
        if (node == null)
            return;
        actionGotoNode(node);
    }

    public void actionGotoNode(ConstNode node)
    {
        boolean protectGui = (m_gtp != null);
        actionGotoNode(node, protectGui);
    }

    private void actionGotoNode(final ConstNode node, final boolean protectGui)
    {
        if (! checkStateChangePossible())
            return;
        if (protectGui)
            protectGui();
        Runnable runnable = new Runnable() {
                public void run() {
                    gotoNode(node);
                    boardChangedBegin(false, false);
                    if (protectGui)
                        unprotectGui();
                }
            };
        if (protectGui)
            SwingUtilities.invokeLater(runnable);
        else
            runnable.run();
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
            showInfo(i18n("MSG_HANDICAP_NEXT_GAME"),
                     i18n("MSG_HANDICAP_NEXT_GAME_2"), true);
        else
        {
            m_computerBlack = false;
            m_computerWhite = false;
            newGame(getBoardSize());
            updateViews(true);
        }
    }

    public void actionImportSgfFromClipboard()
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        String text = GuiUtil.getClipboardText();
        if (text == null)
        {
            showError(i18n("MSG_NO_TEXT_IN_CLIPBOARD"), "", false);
            return;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes());
        try
        {
            SgfReader reader = new SgfReader(in, null, null, 0);
            GameTree tree = reader.getTree();
            m_game.init(tree);
        }
        catch (SgfError e)
        {
            showError(i18n("MSG_IMPORT_FAILED"), e);
        }
        m_guiBoard.initSize(getBoard().getSize());
        initGtp();
        m_computerBlack = false;
        m_computerWhite = false;
        boardChangedBegin(false, true);
    }

    public void actionImportTextPosition()
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        File file = FileDialogs.showOpen(this, i18n("TIT_IMPORT_TEXT"));
        if (file == null)
            return;
        try
        {
            importTextPosition(new FileReader(file));
        }
        catch (FileNotFoundException e)
        {
            showError(i18n("MSG_FILE_NOT_FOUND"), "", false);
        }
    }

    public void actionImportTextPositionFromClipboard()
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        String text = GuiUtil.getClipboardText();
        if (text == null)
            showError(i18n("MSG_NO_TEXT_IN_CLIPBOARD"), "", false);
        else
            importTextPosition(new StringReader(text));
    }

    public void actionInterrupt()
    {
        if (m_gtp == null || m_gtp.isProgramDead() || ! isCommandInProgress())
            return;
        if (m_interrupt.run(this, m_gtp, m_messageDialogs))
        {
            showStatus(i18n("STAT_INTERRUPT"));
            m_interruptComputerBoth = true;
        }
    }

    public void actionKeepOnlyPosition()
    {
        if (! checkStateChangePossible())
            return;
        if (! showQuestion(i18n("MSG_KEEP_ONLY_POSITION"),
                           i18n("MSG_KEEP_ONLY_POSITION_2"),
                           i18n("LB_DELETE"), true))
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
        String disableKey = "net.sf.gogui.gogui.GoGui.make-main-variation";
        if (! m_messageDialogs.showQuestion(disableKey, this,
                                            i18n("MSG_MAKE_MAIN_VAR"),
                                            i18n("MSG_MAKE_MAIN_VAR_2"),
                                            i18n("LB_MAKE_MAIN_VAR"), false))
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
        updateViews(false);
    }

    public void actionNewGame()
    {
        actionNewGame(getBoardSize());
    }

    public void actionNewGame(int size)
    {
        if (! checkStateChangePossible())
            return;
        if (! checkSaveGame())
            return;
        setFile(null);
        newGame(size);
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
        m_newProgram = new Program("", "", "", "", "");
        final ProgramEditor editor = new ProgramEditor();
        m_newProgram =
            editor.editItem(this, i18n("TIT_NEW_PROGRAM"), m_newProgram, true,
                            false, m_messageDialogs);
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
                                                       i18n("TIT_NEW_PROGRAM"),
                                                       m_newProgram, true,
                                                       false,
                                                       m_messageDialogs);
                        if (m_newProgram == null)
                            return;
                        SwingUtilities.invokeLater(this);
                        return;
                    }
                    m_newProgram.m_name = m_gtp.getLabel();
                    m_newProgram.m_version = m_version;
                    m_newProgram.setUniqueLabel(m_programs);
                    m_newProgram = editor.editItem(GoGui.this,
                                                   i18n("TIT_NEW_PROGRAM"),
                                                   m_newProgram, false, true,
                                                   m_messageDialogs);
                    if (m_newProgram == null)
                    {
                        actionDetachProgram();
                        return;
                    }
                    m_programs.add(m_newProgram);
                    m_program = m_newProgram;
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
        if (! showOptionalQuestion("pass", i18n("MSG_PASS"),
                                   i18n("MSG_PASS_2"), i18n("LB_PASS"), false))
            return;
        humanMoved(Move.getPass(getToMove()));
    }

    public void actionPlay(boolean isSingleMove)
    {
        if (! checkStateChangePossible())
            return;
        if (! synchronizeProgram())
            return;
        if (! isSingleMove && ! isComputerBoth())
        {
            m_computerBlack = false;
            m_computerWhite = false;
            if (getToMove() == BLACK)
                m_computerBlack = true;
            else
                m_computerWhite = true;
        }
        generateMove(isSingleMove);
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
                        attachNewProgram(m_programCommand, m_program);
                    }
                    finally
                    {
                        unprotectGui();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionReattachWithParameters()
    {
        if (m_gtp == null)
            return;
        if (! checkCommandInProgress())
            return;
        final boolean fromSnapshot =
            (isProgramDead() && m_parameterSnapshot != null);
        if (! fromSnapshot)
        {
            if (! checkHasParameterCommands())
                return;
        }
        protectGui();
        Runnable runnable = new Runnable() {
                public void run() {
                    try
                    {
                        File file;
                        if (fromSnapshot)
                            file = m_parameterSnapshot;
                        else
                        {
                            try
                            {
                                file = File.createTempFile("gogui-param",
                                                           ".gtp");
                            }
                            catch (IOException e)
                            {
                                showError(i18n("MSG_PARAM_TMP_FILE_ERROR"), e);
                                return;
                            }
                            if (! saveParameters(file))
                                return;
                        }
                        if (! attachNewProgram(m_programCommand, m_program))
                            return;
                        sendGtpFile(file);
                    }
                    finally
                    {
                        unprotectGui();
                    }
                }
            };
        SwingUtilities.invokeLater(runnable);
    }

    public void actionRestoreParameters()
    {
        if (m_gtp == null)
            return;
        if (! checkCommandInProgress())
            return;
        if (m_parameterSnapshot == null)
            return;
        sendGtpFile(m_parameterSnapshot);
    }

    public void actionSave()
    {
        if (! isModified())
            return;
        if (m_gameFile == null)
            actionSaveAs();
        else
        {
            File file = m_gameFile.m_file;
            if (file.exists())
            {
                String mainMessage = format(i18n("MSG_REPLACE_FILE"),
                                            file.getName());
                String optionalMessage = i18n("MSG_REPLACE_FILE_2");
                String disableKey = "net.sf.gogui.GoGui.overwrite";
                if (! m_messageDialogs.showQuestion(disableKey, this,
                                                    mainMessage,
                                                    optionalMessage,
                                                    i18n("LB_REPLACE_FILE"),
                                                    true))
                    return;
            }
            save(m_gameFile);
        }
        updateViews(false);
    }

    public void actionSaveAs()
    {
        saveDialog();
        updateViews(false);
    }

    public void actionSaveCommands()
    {
        if (m_shell == null)
            return;
        m_shell.saveCommands(this);
    }

    public void actionSaveLog()
    {
        if (m_shell == null)
            return;
        m_shell.saveLog(this);
    }

    public void actionSaveParameters()
    {
        if (m_gtp == null)
            return;
        if (! checkHasParameterCommands())
            return;
        File file = showSave(i18n("TIT_SAVE_PARAM"));
        if (file == null)
            return;
        saveParameters(file);
    }

    public void actionSnapshotParameters()
    {
        if (m_gtp == null)
            return;
        if (! checkCommandInProgress())
            return;
        if (! checkHasParameterCommands())
            return;
        if (m_parameterSnapshot == null)
            try
            {
                m_parameterSnapshot =
                    File.createTempFile("gogui-param", ".gtp");
            }
            catch (IOException e)
            {
                showError(i18n("MSG_PARAM_TMP_FILE_ERROR"), e);
                return;
            }
        saveParameters(m_parameterSnapshot);
        updateViews(false);
    }

    public void actionScore()
    {
        if (m_scoreMode)
            return;
        if (! checkStateChangePossible())
            return;
        boolean programReady = (m_gtp != null && synchronizeProgram());
        if (m_gtp == null || ! programReady)
        {
            String disableKey = "net.sf.gogui.gogui.GoGui.score-no-program";
            String optionalMessage;
            if (m_gtp == null)
                optionalMessage = "MSG_SCORE_NO_PROGRAM";
            else
                optionalMessage = "MSG_SCORE_CANNOT_USE_PROGRAM";
            m_messageDialogs.showInfo(disableKey, this,
                                      i18n("MSG_SCORE_MANUAL"),
                                      i18n(optionalMessage), true);
            updateViews(false);
            initScore(null);
            return;
        }
        if (m_gtp.isSupported("final_status_list"))
        {
            Runnable callback = new Runnable() {
                    public void run() {
                        scoreContinue();
                    }
                };
            runLengthyCommand("final_status_list dead", callback);
        }
        else
        {
            String disableKey =
                "net.sf.gogui.gogui.GoGui.score-not-supported";
            String optionalMessage;
            String name = getProgramName();
            optionalMessage = format(i18n("MSG_SCORE_NO_SUPPORT"), name);
            m_messageDialogs.showInfo(disableKey, this,
                                      i18n("MSG_SCORE_MANUAL"),
                                      optionalMessage, true);
            updateViews(false);
            initScore(null);
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
        if (! checkCommandInProgress())
            return;
        if (GtpUtil.isStateChangingCommand(command))
        {
            showError(i18n("MSG_BOARD_CHANGING_COMMAND"), "", false);
            return;
        }
        if (! synchronizeProgram())
            return;
        Runnable callback = new Runnable() {
                public void run() {
                    endLengthyCommand(isCritical, showError);
                }
            };
        m_gtp.send(command, callback);
        beginLengthyCommand();
    }

    public void actionSendFile()
    {
        if (! checkStateChangePossible())
            return;
        if (m_shell == null)
            return;
        File file = FileDialogs.showOpen(this, i18n("TIT_CHOOSE_GTP_FILE"));
        if (file == null)
            return;
        actionSendFile(file);
    }

    public void actionSendFile(File file)
    {
        if (file == null)
            return;
        if (! checkStateChangePossible())
            return;
        if (m_shell == null)
            return;
        if (! synchronizeProgram())
            return;
        sendGtpFile(file);
        m_menuBar.addRecentGtp(file);
        updateViews(false);
    }

    public void actionSetAnalyzeCommand(AnalyzeCommand command)
    {
        actionSetAnalyzeCommand(command, false, true, true, false);
    }

    public void actionSetAnalyzeCommand(AnalyzeCommand command,
                                        boolean autoRun, boolean clearBoard,
                                        boolean oneRunOnly,
                                        boolean reuseTextWindow)
    {
        if (! synchronizeProgram())
            return;
        if (! checkStateChangePossible())
            return;
        initAnalyzeCommand(command, autoRun, clearBoard, reuseTextWindow);
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
            if (m_analyzeCommand.getType() == AnalyzeType.EPLIST)
                GuiBoardUtil.setSelect(m_guiBoard,
                                       m_analyzeCommand.getPointListArg(),
                                       true);
            toFront();
            return;
        }
        analyzeBegin(false);
    }

    public void actionSetShowVariations(ShowVariations mode)
    {
        m_showVariations = mode;
        m_prefs.putInt("show-variations", m_showVariations.ordinal());
        resetBoard();
        updateViews(false);
    }

    public void actionSetTimeLeft()
    {
        TimeLeftDialog.show(this, m_game, getCurrentNode(), m_messageDialogs);
        updateViews(false);
    }

    public void actionSetup(GoColor color)
    {
        assert color.isBlackWhite();
        if (! checkCommandInProgress())
            return;
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
            m_setupNodeCreated = (node.getMove() != null || node.hasChildren());
            if (m_setupNodeCreated)
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
            if (m_setupColor == BLACK)
                showStatus(i18n("STAT_SETUP_BLACK"));
            else
                showStatus(i18n("STAT_SETUP_WHITE"));
        }
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
        showShell();
    }

    public void actionShowTree()
    {
        if (m_gameTreeViewer == null)
        {
            createTree();
            updateViews(false);
        }
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

    public void actionToggleShowMoveNumbers()
    {
        if (m_showMoveNumbers)
            m_guiBoard.clearAllLabels();
        m_showMoveNumbers = ! m_showMoveNumbers;
        m_prefs.putBoolean("show-move-numbers", m_showMoveNumbers);
        updateFromGoBoard();
        updateViews(false);
    }

    public void actionToggleShowSubtreeSizes()
    {
        m_showSubtreeSizes = ! m_showSubtreeSizes;
        m_prefs.putBoolean("gametree-show-subtree-sizes", m_showSubtreeSizes);
        if (m_gameTreeViewer == null)
            updateViews(false);
        else
        {
            m_gameTreeViewer.setShowSubtreeSizes(m_showSubtreeSizes);
            updateViews(true);
        }
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

    public void actionToggleTimeStamp()
    {
        m_timeStamp = ! m_timeStamp;
        if (m_shell != null)
            m_shell.setTimeStamp(m_timeStamp);
        m_prefs.putBoolean("gtpshell-timestamp", m_timeStamp);
        updateViews(false);
    }

    public void actionTreeLabels(GameTreePanel.Label mode)
    {
        m_treeLabels = mode;
        m_prefs.putInt("gametree-labels", mode.ordinal());
        if (m_gameTreeViewer == null)
            updateViews(false);
        else
        {
            m_gameTreeViewer.setLabelMode(mode);
            updateViews(true);
        }
    }

    public void actionTreeSize(GameTreePanel.Size mode)
    {
        m_treeSize = mode;
        m_prefs.putInt("gametree-size", mode.ordinal());
        if (m_gameTreeViewer == null)
            updateViews(false);
        else
        {
            m_gameTreeViewer.setSizeMode(mode);
            updateViews(true);
        }
    }

    public void actionTruncate()
    {
        if (! checkStateChangePossible())
            return;
        if (! getCurrentNode().hasFather())
            return;
        String disableKey = "net.sf.gogui.gogui.GoGui.truncate";
        if (! m_messageDialogs.showQuestion(disableKey, this,
                                            i18n("MSG_TRUNCATE"),
                                            i18n("MSG_TRUNCATE_2"),
                                            i18n("LB_TRUNCATE"), false))
            return;
        m_game.truncate();
        actionGotoNode(getCurrentNode());
        boardChangedBegin(false, true);
    }

    public void actionTruncateChildren()
    {
        if (! checkStateChangePossible())
            return;
        int numberChildren = getCurrentNode().getNumberChildren();
        if (numberChildren == 0)
            return;
        String disableKey = "net.sf.gogui.gogui.GoGui.truncate-children";
        if (! m_messageDialogs.showQuestion(disableKey, this,
                                            i18n("MSG_TRUNCATE_CHILDREN"),
                                            i18n("MSG_TRUNCATE_CHILDREN_2"),
                                            i18n("LB_TRUNCATE"), false))
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

    /** Get name of currently attached program.
        @return Name or null, if no program is attached or name is not
        known. */
    public String getProgramName()
    {
        String name = null;
        if (m_gtp != null)
            name = m_gtp.getName();
        if (name == null)
            name = i18n("MSG_UNKNOWN_PROGRAM_NAME");
        return name;
    }

    public int getNumberPrograms()
    {
        return m_programs.size();
    }

    /** Get label for currently attached program.
        @return Label from Program instance, if program was created with a
        Program instance, otherwise label as in GtpClientBase#getLabel; null
        if no program is attached. */
    public String getProgramLabel()
    {
        if (m_gtp == null)
            return null;
        else if (m_program != null && ! StringUtil.isEmpty(m_program.m_label))
            return m_program.m_label;
        else
            return m_gtp.getLabel();
    }

    public GoColor getSetupColor()
    {
        return m_setupColor;
    }

    public boolean getShowLastMove()
    {
        return m_showLastMove;
    }

    public boolean getShowMoveNumbers()
    {
        return m_showMoveNumbers;
    }

    public boolean getShowSubtreeSizes()
    {
        return m_showSubtreeSizes;
    }

    public ShowVariations getShowVariations()
    {
        return m_showVariations;
    }

    public boolean getTimeStamp()
    {
        return m_timeStamp;
    }

    public GameTreePanel.Label getTreeLabels()
    {
        return m_treeLabels;
    }

    public GameTreePanel.Size getTreeSize()
    {
        return m_treeSize;
    }

    /** Return whether the currently attached program has analyze commands of
        type "param". */
    public boolean hasParameterCommands()
    {
        if (m_analyzeCommands == null)
            return false;
        return AnalyzeUtil.hasParameterCommands(m_analyzeCommands);
    }

    public boolean hasParameterSnapshot()
    {
        return m_parameterSnapshot != null;
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
        if (color == BLACK)
            return m_computerBlack;
        assert color == WHITE;
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
        if (m_setupMode)
        {
            if (! checkCommandInProgress())
                return;
            GoColor color;
            if (modifiedSelect)
                color = m_setupColor.otherColor();
            else
                color = m_setupColor;
            if (getBoard().getColor(p) == color)
                color = EMPTY;
            setup(p, color);
            updateViews(false);
        }
        else if (m_analyzeCommand != null && m_analyzeCommand.needsPointArg()
                 && ! modifiedSelect)
        {
            if (! checkCommandInProgress())
                return;
            m_analyzeCommand.setPointArg(p);
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(p, true);
            analyzeBegin(false);
        }
        else if (m_analyzeCommand != null
                 && m_analyzeCommand.needsPointListArg())
        {
            if (! checkCommandInProgress())
                return;
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
        }
        else if (m_scoreMode && ! modifiedSelect)
        {
            if (! checkCommandInProgress())
                return;
            GuiBoardUtil.scoreSetDead(m_guiBoard, m_countScore, getBoard(), p);
            Komi komi = getGameInfo().getKomi();
            m_scoreDialog.showScore(m_countScore, komi);
        }
        else if (modifiedSelect)
            m_guiBoard.contextMenu(p);
        else
        {
            if (getBoard().getColor(p) != EMPTY)
                return;
            if (! checkCommandInProgress())
                return;
            if (getBoard().isSuicide(getToMove(), p)
                && ! showQuestion(i18n("MSG_SUICIDE"), i18n("MSG_SUICIDE_2"),
                                  i18n("LB_SUICIDE"),false))
                return;
            else if (getBoard().isKo(p)
                     && ! showQuestion(i18n("MSG_ILLEGAL_KO"),
                                       i18n("MSG_ILLEGAL_KO_2"),
                                       i18n("LB_ILLEGAL_KO"), false))
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
        if (m_gameFile == null)
            return null;
        return m_gameFile.m_file;
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

    /** Callback for selected text.
        This is a callback for text selection exents in different components.
        It parses the text for valid points and marks them on the board. */
    public void textSelected(String text)
    {
        if (text == null)
            text = "";
        PointList points = GtpUtil.parsePointString(text, getBoardSize());
        GuiBoardUtil.showPointList(m_guiBoard, points);
    }

    public void initAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                   boolean clearBoard, boolean reuseTextWindow)
    {
        if (! synchronizeProgram())
            return;
        m_analyzeCommand = command;
        m_analyzeAutoRun = autoRun;
        m_analyzeClearBoard = clearBoard;
        m_analyzeReuseTextWindow = reuseTextWindow;
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

    /** Return if a program is currently attached.
        Also returns true, if a program is attached but dead, which can be
        checked with isProgramDead() */
    public boolean isProgramAttached()
    {
        return (m_gtp != null);
    }

    public boolean isProgramDead()
    {
        return (m_gtp != null && m_gtp.isProgramDead());
    }

    public void showLiveGfx(String text)
    {
        assert SwingUtilities.isEventDispatchThread();
        // The live gfx events can arrive delayed, we don't want to allow
        // them to paint on the board, if no command is currently running
        if (! isCommandInProgress())
            return;
        m_guiBoard.clearAll();
        GuiBoardUtil.updateFromGoBoard(m_guiBoard, getBoard(), false, false);
        AnalyzeShow.showGfx(text, m_guiBoard, m_statusBar, null);
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
        }

        public void run()
        {
            String name = getProgramName();
            String mainMessage = format(i18n("MSG_INVALID_RESPONSE"), name);
            String disableKey = "net.sf.gogui.gogui.GoGui.invalid-response";
            String optionalMessage =
                format(i18n("MSG_INVALID_NOSTATUS_RESPONSE"), name);
            m_messageDialogs.showWarning(disableKey, GoGui.this, mainMessage,
                                         optionalMessage, true);
        }
    }

    private static class LoadFileRunnable
        implements GuiUtil.ProgressRunnable
    {
        public LoadFileRunnable(File file)
        {
            m_file = file;
        }

        public GameTree getTree()
        {
            return m_reader.getTree();
        }

        public String getWarnings()
        {
            return m_reader.getWarnings();
        }

        public GameFile getGameFile()
        {
            return m_reader.getFile();
        }

        public void run(ProgressShow progressShow) throws Throwable
        {
            m_reader = new GameReader(m_file, progressShow);
        }

        private final File m_file;

        private GameReader m_reader;
    }

    private boolean m_analyzeAutoRun;

    private boolean m_analyzeClearBoard;

    private boolean m_analyzeOneRunOnly;

    private boolean m_analyzeReuseTextWindow;

    private boolean m_autoNumber;

    private boolean m_commandCompletion;

    /** Automatically register program in Program menu if GoGui was invoked
        with the option -program */
    private final boolean m_register;

    private boolean m_timeStamp;

    private final boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    /** State variable used between generateMove and computerMoved.
        Flag is set in actionInterrupt. */
    private boolean m_interruptComputerBoth;

    /** State variable used between generateMove and computerMoved. */
    private boolean m_isSingleMove;

    private boolean m_lostOnTimeShown;

    private boolean m_resigned;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_showInfoPanel;

    private boolean m_showLastMove;

    private boolean m_showMoveNumbers;

    private boolean m_showSubtreeSizes;

    private boolean m_showToolbar;

    private ShowVariations m_showVariations;

    private boolean m_setupNodeCreated;

    private final boolean m_verbose;

    private int m_handicap;

    private final int m_move;

    private GameTreePanel.Label m_treeLabels;

    private GameTreePanel.Size m_treeSize;

    private final GuiBoard m_guiBoard;

    private GuiGtpClient m_gtp;

    private final Comment m_comment;

    private final Interrupt m_interrupt = new Interrupt();

    /** File corresponding to the current game. */
    private GameFile m_gameFile;

    private File m_initialFile;

    private final GameInfoPanel m_gameInfoPanel;

    private GtpShell m_shell;

    private GameTreeViewer m_gameTreeViewer;

    private Help m_help;

    private final JPanel m_infoPanel;

    private final JPanel m_innerPanel;

    private final JSplitPane m_splitPane;

    private final GoGuiMenuBar m_menuBar;

    private final Game m_game;

    private GoColor m_setupColor;

    private final MessageDialogs m_messageDialogs = new MessageDialogs();

    private Pattern m_pattern;

    private final File m_analyzeCommandsFile;

    private AnalyzeCommand m_analyzeCommand;

    private final Session m_session =
        new Session("net/sf/gogui/gogui/session");

    private final CountScore m_countScore = new CountScore();

    private final StatusBar m_statusBar;

    private final String m_gtpCommand;

    private final String m_gtpFile;

    private String m_lastAnalyzeCommand;

    private String m_programCommand;

    private String m_titleFromProgram;

    private String m_version = "";

    private AnalyzeDialog m_analyzeDialog;

    private final Preferences m_prefs =
        Preferences.userNodeForPackage(getClass());

    private ScoreDialog m_scoreDialog;

    private ArrayList<AnalyzeDefinition> m_analyzeCommands;

    /** Program information.
        Can be null even if a program is attached, if only m_programName
        is known. */
    private Program m_program;

    /** Program currently being edited in actionNewProgram() */
    private Program m_newProgram;

    private final ThumbnailCreator m_thumbnailCreator =
        new ThumbnailCreator(false);

    private TimeSettings m_timeSettings;

    private final GoGuiActions m_actions = new GoGuiActions(this);

    private final GoGuiToolBar m_toolBar;

    private ArrayList<Bookmark> m_bookmarks;

    private ArrayList<Program> m_programs;

    private ShowAnalyzeText m_showAnalyzeText;

    /** Snapshot used in actionSnapshotParameters and actionRestoreParameters. */
    private File m_parameterSnapshot;

    private void analyzeBegin(boolean checkComputerMove)
    {
        if (m_gtp == null || m_analyzeCommand == null
            || m_analyzeCommand.isPointArgMissing()
            || ! synchronizeProgram())
            return;
        GoColor toMove = getToMove();
        m_lastAnalyzeCommand = m_analyzeCommand.replaceWildCards(toMove);
        runLengthyCommand(m_lastAnalyzeCommand,
                          new AnalyzeContinue(checkComputerMove));
        showStatus(format(i18n("STAT_RUNNING"),
                          m_analyzeCommand.getResultTitle()));
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
            StringBuilder showTextBuffer = new StringBuilder(256);
            AnalyzeShow.show(m_analyzeCommand, m_guiBoard, m_statusBar,
                             getBoard(), response, showTextBuffer);
            AnalyzeType type = m_analyzeCommand.getType();
            GoPoint pointArg = null;
            if (m_analyzeCommand.needsPointArg())
                pointArg = m_analyzeCommand.getPointArg();
            else if (m_analyzeCommand.needsPointListArg())
            {
                ConstPointList list = m_analyzeCommand.getPointListArg();
                if (list.size() > 0)
                    pointArg = list.get(list.size() - 1);
            }
            if (type == AnalyzeType.PARAM)
                ParameterDialog.editParameters(m_lastAnalyzeCommand, this,
                                               title, response, m_gtp,
                                               m_messageDialogs);
            boolean isTextType = m_analyzeCommand.isTextType();
            String showText = null;
            if (showTextBuffer.length() > 0)
                showText = showTextBuffer.toString();
            else if (isTextType)
                showText = response;
            if (showText != null)
            {
                if (showText.indexOf("\n") < 0)
                {
                    if (isTextType && showText.trim().equals(""))
                        showText = i18n("STAT_ANALYZE_TEXT_EMPTY_RESPONSE");
                    showStatus(format(i18n("STAT_ANALYZE_TEXT_RESPONSE"),
                                      title, showText));
                }
                else
                {
                    m_showAnalyzeText.show(type, pointArg, title, showText,
                                           m_analyzeReuseTextWindow);
                }
            }
            if ("".equals(m_statusBar.getText()) && type != AnalyzeType.PARAM)
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

    private boolean attachNewProgram(String command, Program program)
    {
        if (m_gtp != null)
        {
            saveSession();
            detachProgram();
        }
        if (! attachProgram(command, program, false))
        {
            m_prefs.putInt("program", -1);
            if (m_gtp == null || m_gtp.isProgramDead())
                if (! m_shell.isVisible() && m_shell.isLastTextNonGTP())
                    showShell();
            updateViews(false);
            return false;
        }
        if (m_shell != null && m_session.isVisible("shell"))
            m_shell.setVisible(true);
        if (m_session.isVisible("analyze"))
            createAnalyzeDialog();
        toFrontLater();
        updateViews(false);
        return true;
    }

    /** Attach program.
        @param programCommand Command line for running program.
        @param program Program information (may be null)
        @param register Create an entry for this program in the Program menu.
        @return true if program was successfully attached. */
    private boolean attachProgram(String programCommand, Program program,
                                  boolean register)
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
        GuiAction.registerAll(m_shell.getLayeredPane());
        m_shell.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    saveSession();
                    hideShell();
                }
            });
        restoreSize(m_shell, "shell");
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
                        GuiUtil.invokeAndWait(runnable);
                }
            };
        GtpClient.IOCallback ioCallback = new GtpClient.IOCallback()
            {
                public void receivedInvalidResponse(String s)
                {
                    if (m_shell == null)
                        return;
                    boolean invokeLater = true;
                    m_shell.receivedInvalidResponse(s, invokeLater);
                }

                public void receivedResponse(boolean error, String s)
                {
                    if (m_shell == null)
                        return;
                    boolean invokeLater = true;
                    m_shell.receivedResponse(error, s, invokeLater);
                }

                public void receivedStdErr(String s)
                {
                    if (m_shell == null)
                        return;
                    m_lineReader.add(s);
                    while (m_lineReader.hasLines())
                    {
                        String line = m_lineReader.getLine();
                        boolean isLiveGfx = m_liveGfx.handleLine(line);
                        boolean isWarning =
                            line.startsWith("warning:")
                            || line.startsWith("Warning:")
                            || line.startsWith("WARNING:");
                        boolean invokeLater = true;
                        m_shell.receivedStdErr(line, invokeLater, isLiveGfx,
                                               isWarning);
                    }
                }

                public void sentCommand(String s)
                {
                    if (m_shell != null)
                        m_shell.sentCommand(s);
                }

                private final LineReader m_lineReader = new LineReader();

                private LiveGfx m_liveGfx = new LiveGfx(GoGui.this);
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
            showStatusImmediately(i18n("STAT_ATTACHING_PROGRAM"));
            File workingDirectory = null;
            if (program != null
                && ! StringUtil.isEmpty(program.m_workingDirectory))
                workingDirectory = new File(program.m_workingDirectory);
            GtpClient gtp =
                new GtpClient(m_programCommand, workingDirectory,
                              m_verbose, ioCallback);
            gtp.setInvalidResponseCallback(invalidResponseCallback);
            gtp.setAutoNumber(m_autoNumber);
            m_gtp = new GuiGtpClient(gtp, this, synchronizerCallback,
                                     m_messageDialogs);
            m_gtp.queryName();
            m_gtp.queryProtocolVersion();
            try
            {
                m_version = m_gtp.queryVersion();
                m_shell.setProgramVersion(m_version);
                m_gtp.querySupportedCommands();
                m_gtp.queryInterruptSupport();
                if (m_program == null)
                {
                    m_program =
                        Program.findProgram(m_programs, programCommand);
                    if (m_program == null && m_register)
                    {
                        m_program = new Program("", m_gtp.getName(), m_version,
                                                programCommand, "");
                        m_program.setUniqueLabel(m_programs);
                        m_programs.add(m_program);
                        m_menuBar.setPrograms(m_programs);
                        Program.save(m_programs);
                    }
                }
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
            try
            {
                String programAnalyzeCommands
                    = GtpClientUtil.getAnalyzeCommands(m_gtp);
                m_analyzeCommands
                    = AnalyzeDefinition.read(m_gtp.getSupportedCommands(),
                                             m_analyzeCommandsFile,
                                             programAnalyzeCommands);
            }
            catch (ErrorMessage e)
            {
                showError(i18n("MSG_COULD_NOT_READ_ANALYZE_CONFIGURATION"), e);
            }
            restoreSize(m_shell, "shell");
            m_shell.setProgramName(getProgramLabel());
            ArrayList<String> supportedCommands =
                m_gtp.getSupportedCommands();
            m_shell.setInitialCompletions(supportedCommands);
            if (! m_gtp.isGenmoveSupported())
            {
                m_computerBlack = false;
                m_computerWhite = false;
            }
            initGtp();
            if (! m_gtpFile.equals(""))
                sendGtpFile(new File(m_gtpFile));
            if (! m_gtpCommand.equals(""))
                sendGtpString(m_gtpCommand);
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
        showStatus(format(i18n("STAT_THINKING"), getProgramName()));
        updateViews(false);
    }

    private void boardChangedBegin(boolean doCheckComputerMove,
                                   boolean gameTreeChanged)
    {
        updateFromGoBoard();
        updateViews(gameTreeChanged);
        if (m_analyzeDialog != null)
            m_analyzeDialog.setSelectedColor(getToMove());
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
            showError(i18n("MSG_CANNOT_EXECUTE_WHILE_THINKING"),
                      i18n("MSG_CANNOT_EXECUTE_WHILE_THINKING_2"), false);
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
                    updateViews(true, true);
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

    private boolean checkHasParameterCommands()
    {
        if (! AnalyzeUtil.hasParameterCommands(m_analyzeCommands))
        {
            String optionalMessage =
                format(i18n("MSG_NO_PARAM_COMMANDS_2"), getProgramName());
            showError(i18n("MSG_NO_PARAM_COMMANDS"), optionalMessage);
            return false;
        }
        return true;
    }

    private void checkLostOnTime(GoColor color)
    {
        if (getClock().lostOnTime(color)
            && ! getClock().lostOnTime(color.otherColor())
            && ! m_lostOnTimeShown)
        {
            String result = color.otherColor().getUppercaseLetter() + "+Time";
            String mainMessage;
            String optionalMessage;
            if (color == BLACK)
            {
                mainMessage = i18n("MSG_LOST_ON_TIME_BLACK");
                optionalMessage = format(i18n("MSG_LOST_ON_TIME_BLACK_2"),
                                         result);
            }
            else
            {
                mainMessage = i18n("MSG_LOST_ON_TIME_WHITE");
                optionalMessage = format(i18n("MSG_LOST_ON_TIME_WHITE_2"),
                                         result);
            }
            showInfo(mainMessage, optionalMessage, false);
            setResult(result);
            m_lostOnTimeShown = true;
        }
    }

    private boolean checkSaveGame()
    {
        return checkSaveGame(false);
    }

    /** Ask for saving file if it was modified.
        @return true If file was not modified, user chose not to save it
        or file was saved successfully */
    private boolean checkSaveGame(boolean isProgramTerminating)
    {
        if (! isModified())
            return true;
        String mainMessage = i18n("MSG_SAVE_CURRENT");
        String optionalMessage = i18n("MSG_SAVE_CURRENT_2");
        int result;
        String disableKey = null;
        if (! isProgramTerminating)
            disableKey = "net.sf.gogui.gogui.GoGui.save";
        result = m_messageDialogs.showYesNoCancelQuestion(disableKey, this,
                                                          mainMessage,
                                                          optionalMessage,
                                                          i18n("LB_DONT_SAVE"),
                                                          i18n("LB_SAVE"));
        switch (result)
        {
        case 0:
            m_game.clearModified();
            return true;
        case 1:
            if (m_gameFile == null)
                return saveDialog();
            else
                return save(m_gameFile);
        case 2:
            return false;
        default:
            assert false;
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
            String name = getProgramName();
            if (response.equalsIgnoreCase("resign"))
            {
                String result =
                    toMove.otherColor().getUppercaseLetter() + "+Resign";
                if (! m_auto)
                {
                    String mainMessage = format(i18n("MSG_RESIGN"), name);
                    String optionalMessage =
                        format(i18n("MSG_RESIGN_2"), result);
                    showInfo(mainMessage, optionalMessage, false);
                }
                m_resigned = true;
                setResult(result);
            }
            else
            {
                GoPoint point = GtpUtil.parsePoint(response, getBoardSize());
                ConstBoard board = getBoard();
                if (point != null)
                {
                    if (board.getColor(point) != EMPTY)
                    {
                        String mainMessage =
                            format(i18n("MSG_NONEMPTY"), name);
                        String optionalMessage =
                            format(i18n("MSG_NONEMPTY_2"), name);
                        showWarning(mainMessage, optionalMessage, true);
                        m_computerBlack = false;
                        m_computerWhite = false;
                    }
                    else if (board.isKo(point))
                    {
                        String mainMessage =
                            format(i18n("MSG_VIOLATE_KO"), name);
                        showWarning(mainMessage, i18n("MSG_VIOLATE_KO_2"),
                                    true);
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
                    String mainMessage =
                        format(i18n("MSG_PROGRAM_PASS"), name);
                    String optionalMessage =
                        format(i18n("MSG_PROGRAM_PASS_2"), name);
                    m_messageDialogs.showInfo(disableKey, this, mainMessage,
                                              optionalMessage, false);
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
        if (getToMove() == BLACK)
            return m_computerBlack;
        else
            return m_computerWhite;
    }

    private void createAnalyzeDialog()
    {
        m_analyzeDialog = new AnalyzeDialog(this, this, m_analyzeCommands,
                                            m_gtp, m_messageDialogs);
        m_analyzeDialog.setReuseTextWindow(
                        m_prefs.getBoolean("analyze-reuse-text-window", false));
        GuiAction.registerAll(m_analyzeDialog.getLayeredPane());
        m_analyzeDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionDisposeAnalyzeDialog();
                }
            });
        m_analyzeDialog.setBoardSize(getBoardSize());
        m_analyzeDialog.setSelectedColor(getToMove());
        restoreSize(m_analyzeDialog, "analyze");
        m_analyzeDialog.setVisible(true);
    }

    private ContextMenu createContextMenu(GoPoint point)
    {
        boolean noProgram = (m_gtp == null);
        return new ContextMenu(point, m_guiBoard.getMark(point),
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
        GuiAction.registerAll(m_gameTreeViewer.getLayeredPane());
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
        String path = file.getAbsolutePath();
        if (! path.startsWith("/tmp") && ! path.startsWith("/var/tmp"))
        {
            try
            {
                m_thumbnailCreator.create(file);
            }
            catch (ErrorMessage e)
            {
            }
        }
    }

    private void currentNodeChanged()
    {
        updateFromGoBoard();
    }

    private void detachProgram()
    {
        if (m_gtp != null)
            showStatusImmediately(i18n("STAT_DETACHING"));
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
        restoreBoardCursor();
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
        if (command.length() < 20)
            return command;
        GtpCommand cmd = new GtpCommand(command);
        return cmd.getCommand() + " [...]";
    }

    private void generateMove(boolean isSingleMove)
    {
        if (! synchronizeProgram())
            return;
        GoColor toMove = getToMove();
        ConstNode node = getCurrentNode();
        ConstNode father = node.getFatherConst();
        ConstGameInfo info = getGameInfo();
        String playerToMove = info.get(StringInfoColor.NAME, toMove);
        String playerOther =
            info.get(StringInfoColor.NAME, toMove.otherColor());
        String name = getProgramLabel();
        if (! isSingleMove && m_gameFile == null && playerToMove == null
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
            if (toMove == BLACK)
                command += " b";
            else if (toMove == WHITE)
                command += " w";
            else
                assert false;
        }
        else
        {
            command = m_gtp.getCommandGenmove(toMove);
        }
        m_isSingleMove = isSingleMove;
        m_interruptComputerBoth = false;
        Runnable callback = new Runnable()
            {
                public void run()
                {
                    computerMoved();
                }
            };
        if (getClock().isInitialized()
            && NodeUtil.isTimeLeftKnown(getCurrentNode(), toMove))
            GtpUtil.sendTimeLeft(m_gtp, getClock(), toMove);
        m_game.startClock();
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

    private ConstGameInfo getGameInfo()
    {
        return m_game.getGameInfo(getCurrentNode());
    }

    private Komi getPrefsKomi()
    {
        try
        {
            String s = m_prefs.get("komi", "6.5");
            return Komi.parseKomi(s);
        }
        catch (InvalidKomiException e)
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

    /** Change current node.
        Automatically restores the clock, or halts it, if no time settings
        are known. */
    private void gotoNode(ConstNode node)
    {
        // GameTreeViewer is not disabled in score mode
        if (m_scoreMode)
            return;
        m_game.gotoNode(node);
        if (getClock().isInitialized())
            m_game.restoreClock();
        else
            m_game.haltClock();
        currentNodeChanged();
    }

    private void hideShell()
    {
        if (m_shell == null)
            return;
        saveSession();
        m_shell.setVisible(false);
    }

    private void humanMoved(Move move)
    {
        GoPoint p = move.getPoint();
        if (p != null)
            paintImmediately(p, move.getColor(), true);
        if (m_gtp != null && ! isComputerNone() && ! isOutOfSync()
            && ! m_gtp.isProgramDead())
        {
            synchronizeProgram();
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
            showError(i18n("MSG_IMPORT_FAILED"), e);
        }
        m_guiBoard.initSize(getBoard().getSize());
        initGtp();
        m_computerBlack = false;
        m_computerWhite = false;
        boardChangedBegin(false, true);
    }

    private void initGame(int size)
    {
        int oldSize = getBoardSize();
        if (size != oldSize)
        {
            // Clear analyze command when board size changes, because eplist
            // could contain points out of board)
            clearAnalyzeCommand();
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
        Komi komi = (m_handicap == 0 ? getPrefsKomi() : new Komi(0));
        ConstPointList handicap = Board.getHandicapStones(size, m_handicap);
        if (handicap == null)
            showWarning(i18n("MSG_HANDICAP_UNDEFINED"),
                        format(i18n("MSG_HANDICAP_UNDEFINED_2"), m_handicap,
                               size), false);
        m_game.init(size, komi, handicap, m_prefs.get("rules", ""),
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
                ConstGameInfo info = getGameInfo();
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
        m_guiBoard.setListener(this);
        m_guiBoard.addMouseWheelListener(new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    // Silently ignore mouse wheel events if command in
                    // progress because it is easy to generate multiple events
                    // while using the wheel and if an analyze command is
                    // enabled to automatically run after each board change,
                    // actionForward() and actionBackward() would pop up an
                    // error dialog if the analyze command is still in progress
                    if (isCommandInProgress())
                        return;
                    int n = e.getWheelRotation();
                    int mod = e.getModifiers();
                    int scale = (mod == ActionEvent.SHIFT_MASK ? 10 : 1);
                    if (n > 0)
                        actionForward(scale * n);
                    else
                        actionBackward(-scale * n);
                }
            });

        GuiUtil.removeKeyBinding(m_splitPane, "F8");
        GuiAction.registerAll(getLayeredPane());

        m_bookmarks = Bookmark.load();
        m_menuBar.setBookmarks(m_bookmarks);
        m_programs = Program.load();
        m_menuBar.setPrograms(m_programs);
        if (m_programCommand == null)
        {
            int index = m_prefs.getInt("program", -1);
            if (index >= 0 && index < m_programs.size())
            {
                m_program = m_programs.get(index);
                m_programCommand = m_program.m_command;
            }
        }
        if (m_initialFile == null)
            newGame(getBoardSize());
        else
            newGameFile(getBoardSize(), m_move);
        if (! m_prefs.getBoolean("show-info-panel", true))
            showInfoPanel(false);
        if (m_prefs.getBoolean("show-toolbar", true))
            showToolbar(true);
        restoreMainWindow(getBoardSize());
        // Attaching a program can take some time, so we want to make
        // the window visible, but not draw the window content yet
        getLayeredPane().setVisible(false);
        setVisible(true);
        if (m_programCommand != null)
        {
            attachProgram(m_programCommand, m_program, m_register);
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
        setTitleFromProgram();
        updateViews(true);
        getLayeredPane().setVisible(true);
        unprotectGui();
        toFrontLater();
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
            ScoringMethod scoringMethod = getGameInfo().parseRules();
            m_scoreDialog = new ScoreDialog(this, this, scoringMethod);
        }
        restoreLocation(m_scoreDialog, "score");
        Komi komi = getGameInfo().getKomi();
        m_scoreDialog.showScore(m_countScore, komi);
        m_scoreDialog.setVisible(true);
        showStatus(i18n("STAT_SCORE"));
    }

    private boolean isComputerBoth()
    {
        return (m_computerBlack && m_computerWhite);
    }

    private boolean isComputerNone()
    {
        return ! (m_computerBlack || m_computerWhite);
    }

    private boolean isOutOfSync()
    {
        return (m_gtp != null && m_gtp.isOutOfSync());
    }

    private boolean loadFile(File file, int move)
    {
        try
        {
            LoadFileRunnable runnable = new LoadFileRunnable(file);
            if (file.length() > 500000)
            {
                newGame(getBoardSize()); // Frees space if already large tree
                GuiUtil.runProgress(this, i18n("LB_LOADING"), runnable);
            }
            else
                runnable.run(null);
            GameTree tree = runnable.getTree();
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
            setFile(runnable.getGameFile());
            FileDialogs.setLastFile(file);
            String warnings = runnable.getWarnings();
            if (warnings != null)
            {
                String optionalMessage =
                    i18n("MSG_FILE_FORMAT_WARNING_2")
                    + "\n(" +
                    warnings.replaceAll("\n\\z", "").replaceAll("\n", ")\n(")
                    + ")";
                showWarning(i18n("MSG_FILE_FORMAT_WARNING"), optionalMessage,
                            true);
            }
            m_computerBlack = false;
            m_computerWhite = false;
            createThumbnail(file);
        }
        catch (FileNotFoundException e)
        {
            showError(i18n("MSG_FILE_NOT_FOUND"), e);
            return false;
        }
        catch (SgfError e)
        {
            showError(i18n("MSG_COULD_NOT_READ_FILE"), e);
            return false;
        }
        catch (ErrorMessage e)
        {
            showError(i18n("MSG_COULD_NOT_READ_FILE"), e);
            return false;
        }
        catch (Throwable t)
        {
            t.printStackTrace();
            assert false;
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
        if (! loadFile(m_initialFile, move))
            m_gameFile = null;
    }

    /** Paint point immediately to pretend better responsiveness.
        Necessary because waiting for a repaint of the Go board can be slow
        due to the updating game tree or response to GTP commands. */
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
                    assert SwingUtilities.isEventDispatchThread();
                    actionAbout();
                    return true;
                }

                public boolean handleOpenFile(String filename)
                {
                    assert SwingUtilities.isEventDispatchThread();
                    if (! checkSaveGame())
                        return true;
                    loadFile(new File(filename), -1);
                    boardChangedBegin(false, true);
                    return true;
                }

                public boolean handleQuit()
                {
                    assert SwingUtilities.isEventDispatchThread();
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

    private void restoreBoardCursor()
    {
        if (m_analyzeCommand != null
            && (m_analyzeCommand.needsPointArg()
                || m_analyzeCommand.needsPointListArg()))
            setBoardCursor(Cursor.HAND_CURSOR);
        else
            setBoardCursorDefault();
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
        assert m_gtp != null;
        m_gtp.send(cmd, callback);
        beginLengthyCommand();
    }

    /** Save game to file.
        @return true If successfully saved. */
    private boolean save(GameFile gameFile)
    {
        try
        {
            new GameWriter(gameFile, getTree(), i18n("LB_GOGUI"),
                           Version.get());
        }
        catch (ErrorMessage e)
        {
            showError(i18n("MSG_SAVING_FAILED"), e);
            return false;
        }
        m_menuBar.addRecent(gameFile.m_file);
        createThumbnail(gameFile.m_file);
        setFile(gameFile);
        m_game.clearModified();
        updateViews(false);
        return true;
    }

    private boolean saveDialog()
    {
        File file = FileDialogs.showSaveSgf(this, m_messageDialogs);
        if (file == null)
            return false;
        GameFile gameFile = new GameFile();
        gameFile.m_file = file;
        if (FileUtil.hasExtension(file, "xml"))
            gameFile.m_format = GameFile.Format.XML;
        else
            gameFile.m_format = GameFile.Format.SGF;
        return save(gameFile);
    }

    private boolean saveParameters(File file)
    {
        try
        {
            GtpClientUtil.saveParameters(m_gtp, m_analyzeCommands, file);
        }
        catch (ErrorMessage e)
        {
            showError(i18n("MSG_COULD_NOT_SAVE_PARAMETERS"), e);
            return false;
        }
        return true;
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, getBoard(), i18n("LB_GOGUI"), Version.get());
        m_menuBar.addRecent(file);
        updateViews(false);
    }

    private void saveSession()
    {
        if (m_shell != null)
            m_shell.saveHistory();
        if (m_analyzeDialog != null)
        {
            m_analyzeDialog.saveRecent();
            m_prefs.putBoolean("analyze-reuse-text-window",
                               m_analyzeDialog.getReuseTextWindow());
        }
        if (! isVisible()) // can that happen?
            return;
        if (m_help != null)
            m_session.saveSize(m_help.getWindow(), "help");
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
        // GoGui's program logic does currently not depend on syncing the
        // preferences to disk immediately, but we do it anyway to work around
        // a bug in OpenJDK 1.6.0_20 on Linux (Ubuntu 10.10), which fails to
        // perform the automatic syncing of class Preferences on shutdown of
        // the VM (probably because of a BadWindow X Error on window closing)
        try
        {
            m_prefs.sync();
        }
        catch (BackingStoreException e)
        {
            System.err.println(e.getMessage());
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
        updateViews(false);
        initScore(isDeadStone);
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
                        showError(i18n("MSG_BOARD_CHANGING_COMMAND"), "");
                        break;
                    }
                    try
                    {
                        m_gtp.send(line);
                    }
                    catch (GtpError e)
                    {
                        showError(e);
                        if (m_gtp.isProgramDead()
                            || ! showQuestion(i18n("MSG_CONTINUE_SEND"), "",
                                              i18n("LB_CONTINUE_SEND"), false))
                            break;
                    }
                }
                catch (IOException e)
                {
                    showError(i18n("MSG_COULD_NOT_READ_FILE"), e);
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
            showError(i18n("MSG_FILE_NOT_FOUND"), e);
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

    private void setFile(GameFile gameFile)
    {
        m_gameFile = gameFile;
        setTitle();
    }

    private void setMinimumSize()
    {
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
    }

    private void setResult(String result)
    {
        String oldResult = getGameInfo().get(StringInfo.RESULT);
        if (! (oldResult == null || oldResult.equals("")
               || oldResult.equals(result))
            && ! showQuestion(format(i18n("MSG_REPLACE_RESULT"), oldResult,
                                     result),
                              i18n("MSG_REPLACE_RESULT_2"),
                              i18n("LB_REPLACE_RESULT"), false))
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
        String appName = i18n("LB_GOGUI");
        if (m_gtp != null)
            appName = getProgramLabel();
        String filename = null;
        if (m_gameFile != null)
        {
            filename = m_gameFile.m_file.getName();
            // On the Mac, a modified document is indicated by setting the
            // windowModified property in updateViews()
            if (isModified() && ! Platform.isMac())
                filename = filename + "*";
        }
        ConstGameInfo info = getGameInfo();
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
            String name = getProgramLabel();
            String nameBlack = info.get(StringInfoColor.NAME, BLACK);
            String nameWhite = info.get(StringInfoColor.NAME, WHITE);
            if (! appName.equals(i18n("LB_GOGUI"))
                && (ObjectUtil.equals(nameBlack, name)
                    || ObjectUtil.equals(nameWhite, name)))
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
            m_titleFromProgram = GtpClientUtil.getTitle(m_gtp);
        if (m_titleFromProgram != null)
            setTitle(m_titleFromProgram);
    }

    private void setup(GoPoint point, GoColor color)
    {
        assert point != null;
        m_game.setup(point, color);
    }

    private void setupDone()
    {
        if (! m_setupMode)
            return;
        m_setupMode = false;
        ConstNode currentNode = getCurrentNode();
        if (currentNode.hasSetup() || m_setupColor != getToMove())
            m_game.setToMove(m_setupColor);
        else if (m_setupNodeCreated && currentNode.isEmpty()
                 && currentNode.hasFather())
            m_game.truncate();
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
        String name = getProgramName();
        String mainMessage = format(i18n("MSG_INVALID_RESPONSE"), name);
        String optionalMessage =
            format(i18n("MSG_INVALID_RESPONSE_2"), name, e.getMessage());
        showError(mainMessage, optionalMessage, true);
    }

    private void showError(GtpError e, boolean isCritical)
    {
        String name = getProgramName();
        String mainMessage;
        String optionalMessage;
        if (m_gtp != null && m_gtp.isProgramDead())
        {
            if (m_gtp.wasKilled())
                mainMessage = format(i18n("MSG_PROGRAM_TERMINATED"), name);
            else
                mainMessage = i18n("MSG_PROGRAM_TERMINATED_UNEXPECTEDLY");
            boolean hasErrorOutput = m_shell.isLastTextNonGTP();
            boolean anyResponses = m_gtp.getAnyCommandsResponded();
            if (hasErrorOutput && ! anyResponses)
                optionalMessage =
                    format(i18n("MSG_PROGRAM_TERMINATED_2"), name);
            else if (hasErrorOutput && anyResponses)
                optionalMessage =
                    format(i18n("MSG_PROGRAM_TERMINATED_3"), name);
            else
                optionalMessage = i18n("MSG_PROGRAM_TERMINATED_4");
        }
        else if (e instanceof GtpClient.ExecFailed)
        {
            mainMessage = i18n("MSG_COULD_NOT_EXECUTE");
            if (StringUtil.isEmpty(e.getMessage()))
                optionalMessage = i18n("MSG_COULD_NOT_EXECUTE_2");
            else
                optionalMessage =
                    format(i18n("MSG_COULD_NOT_EXECUTE_3"), e.getMessage());
        }
        else
        {
            mainMessage = i18n("MSG_COMMAND_FAILED");
            if (e.getMessage().trim().equals(""))
                optionalMessage =
                    format(i18n("MSG_COMMAND_FAILED_2"), e.getCommand());
            else
                optionalMessage =
                    format(i18n("MSG_COMMAND_FAILED_3"), e.getCommand(),
                           e.getMessage());
        }
        showError(mainMessage, optionalMessage, isCritical);
        updateViews(false); // If program died, menu items need to be updated
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
        if (m_resigned)
            return;
        String disableKey = "net.sf.gogui.gogui.GoGui.game-finished";
        m_messageDialogs.showInfo(disableKey, this,
                                  i18n("MSG_GAME_FINISHED"),
                                  i18n("MSG_GAME_FINISHED_2"), false);
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

    private File showSave(String title)
    {
        return FileDialogs.showSave(this, title, m_messageDialogs);
    }

    private void showShell()
    {
        if (m_gtp == null)
            return;
        if (m_shell.isVisible())
            m_shell.toFront();
        else
        {
            restoreSize(m_shell, "shell");
            m_shell.setVisible(true);
        }
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
        showStatus(format(i18n("STAT_SELECT_POINTLIST"),
                          m_analyzeCommand.getLabel()));
    }

    private void showStatusSelectTarget()
    {
        showStatus(format(i18n("STAT_SELECT_TARGET"),
                          m_analyzeCommand.getResultTitle()));
    }

    private void showToolbar(boolean enable)
    {
        if (enable == m_showToolbar)
            return;
        m_prefs.putBoolean("show-toolbar", enable);
        m_showToolbar = enable;
        if (enable)
            getContentPane().add(m_toolBar, BorderLayout.NORTH);
        else
            getContentPane().remove(m_toolBar);
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

    private boolean synchronizeProgram()
    {
        if (m_gtp == null)
        {
            showError(i18n("MSG_NO_PROGRAM_ATTACHED"), "", false);
            return false;
        }
        if (! checkCommandInProgress())
            return false;
        String name = getProgramName();
        if (m_gtp.isProgramDead())
        {
            String mainMessage = format(i18n("MSG_PROGRAM_TERMINATED"), name);
            String optionalMessage = "";
            if (m_shell.isLastTextNonGTP())
            {
                showShell();
                optionalMessage =
                    format(i18n("MSG_PROGRAM_TERMINATED_CHECK_GTP"), name);
            }
            else
            {
                showShell();
                optionalMessage =
                    format(i18n("MSG_PROGRAM_TERMINATED_REATTACH"), name);
            }
            showError(mainMessage, optionalMessage, false);
            // If program died, menu items need to be updated
            updateViews(false);
            return false;
        }
        boolean wasOutOfSync = isOutOfSync();
        try
        {
            ConstGameInfo info = getGameInfo();
            m_gtp.synchronize(getBoard(), info.getKomi(),
                              info.getTimeSettings());
        }
        catch (GtpError e)
        {
            if (wasOutOfSync)
            {
                String mainMessage = format(i18n("MSG_OUT_OF_SYNC"), name);
                String optionalMessage = format(i18n("MSG_OUT_OF_SYNC_2"),
                                                name);
                showError(mainMessage, optionalMessage, false);
            }
            else
            {
                String mainMessage = format(i18n("MSG_NOSYNC"), name);
                String command = null;
                if (e.getCommand() != null)
                    command = formatCommand(e.getCommand());
                String message = e.getMessage();
                String response = null;
                if (! message.trim().equals(""))
                    response = message;
                String optionalMessage;
                if (command == null)
                    optionalMessage =
                        format(i18n("MSG_NOSYNC_ERROR"), name, message);
                else if (response == null)
                    optionalMessage =
                        format(i18n("MSG_NOSYNC_FAILURE"),
                               command, name);
                else
                    optionalMessage =
                        format(i18n("MSG_NOSYNC_FAILURE_RESPONSE"),
                               command, name, response);
                showWarning(mainMessage, optionalMessage, true);
                // If the program died, menu items need to be updated
                updateViews(false);
            }
            return false;
        }
        return true;
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

    private void updateViews(boolean gameTreeChanged)
    {
        updateViews(gameTreeChanged, false);
    }

    /** Update all views.
        @param gameTreeChanged If nodes were added to or removed from the game
        tree, which will trigger a full and potentially slow game tree update
        @param sync Update game tree within the event handler if the gameTree
        has changed. */
    private void updateViews(boolean gameTreeChanged, boolean sync)
    {
        m_actions.update();
        m_menuBar.update(isProgramAttached(), isTreeShown(), isShellShown());
        m_gameInfoPanel.update();
        m_comment.setComment(getCurrentNode().getComment());
        updateFromGoBoard();
        updateGuiBoard();
        getRootPane().putClientProperty("windowModified",
                                        Boolean.valueOf(isModified()));
        setTitle();
        GoGuiUtil.updateMoveText(m_statusBar, getGame());
        m_statusBar.setSetupMode(m_setupMode);
        if (m_setupMode)
            m_statusBar.setToPlay(m_setupColor);
        if (m_gameTreeViewer != null)
        {
            if (gameTreeChanged)
            {
                if (sync)
                    m_gameTreeViewer.update(getTree(), getCurrentNode());
                else
                {
                    protectGui();
                    showStatus(i18n("STAT_UPDATING_TREE"));
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
            else
                m_gameTreeViewer.update(getCurrentNode());
        }
    }

    private void updateFromGoBoard()
    {
        boolean showLastMove =
            (m_showLastMove
             && ! (m_showVariations == ShowVariations.SIBLINGS
                   && NodeUtil.hasSiblingMoves(getCurrentNode())));
        GuiBoardUtil.updateFromGoBoard(m_guiBoard, getBoard(), m_showLastMove,
                                       m_showMoveNumbers);
        if (! showLastMove || getCurrentNode().getMove() == null)
            m_guiBoard.markLastMove(null);
    }

    private void updateGuiBoard()
    {
        if (m_showVariations == ShowVariations.CHILDREN)
        {
            ConstPointList moves = NodeUtil.getChildrenMoves(getCurrentNode());
            GuiBoardUtil.showMoves(m_guiBoard, moves);
        }
        else if (m_showVariations == ShowVariations.SIBLINGS
                 && NodeUtil.hasSiblingMoves(getCurrentNode()))
        {
            ConstNode father = getCurrentNode().getFatherConst();
            if (father != null)
            {
                ConstPointList moves = NodeUtil.getChildrenMoves(father);
                if (moves.size() > 1)
                    GuiBoardUtil.showMoves(m_guiBoard, moves);
            }
        }
        GuiBoardUtil.showMarkup(m_guiBoard, getCurrentNode());
    }
}
