//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.BoardUtils;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;
import net.sf.gogui.gui.AnalyzeCommand;
import net.sf.gogui.gui.AnalyzeDialog;
import net.sf.gogui.gui.AnalyzeShow;
import net.sf.gogui.gui.BoardSizeDialog;
import net.sf.gogui.gui.Clock;
import net.sf.gogui.gui.Bookmark;
import net.sf.gogui.gui.BookmarkDialog;
import net.sf.gogui.gui.Comment;
import net.sf.gogui.gui.CommandThread;
import net.sf.gogui.gui.ContextMenu;
import net.sf.gogui.gui.EditBookmarksDialog;
import net.sf.gogui.gui.FindDialog;
import net.sf.gogui.gui.GameInfo;
import net.sf.gogui.gui.GameInfoDialog;
import net.sf.gogui.gui.GameTreePanel;
import net.sf.gogui.gui.GameTreeViewer;
import net.sf.gogui.gui.GtpShell;
import net.sf.gogui.gui.GuiBoard;
import net.sf.gogui.gui.GuiBoardUtils;
import net.sf.gogui.gui.GuiUtils;
import net.sf.gogui.gui.Help;
import net.sf.gogui.gui.OptionalWarning;
import net.sf.gogui.gui.ParameterDialog;
import net.sf.gogui.gui.RecentFileMenu;
import net.sf.gogui.gui.SelectProgram;
import net.sf.gogui.gui.Session;
import net.sf.gogui.gui.ScoreDialog;
import net.sf.gogui.gui.SimpleDialogs;
import net.sf.gogui.gui.StatusBar;
import net.sf.gogui.gui.TextViewer;
import net.sf.gogui.gui.Utils;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.sgf.SgfWriter;
import net.sf.gogui.tex.TexWriter;
import net.sf.gogui.thumbnail.Thumbnail;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.Preferences;
import net.sf.gogui.utils.ProgressShow;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Graphical user interface to a Go program. */
public class GoGui
    extends JFrame
    implements ActionListener, AnalyzeDialog.Callback, GuiBoard.Listener,
               GameTreeViewer.Listener, GtpShell.Callback
{
    public GoGui(String program, Preferences prefs, String file, int move,
                 String time, boolean verbose, boolean computerBlack,
                 boolean computerWhite, boolean auto, String gtpFile,
                 String gtpCommand, String initAnalyze, boolean fastPaint)
        throws GtpError, ErrorMessage
    {
        m_fastPaint = fastPaint;
        m_prefs = prefs;
        m_boardSize = prefs.getInt("boardsize");
        m_beepAfterMove = prefs.getBool("beep-after-move");
        if (file != null)
            m_file = new File(file);
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_move = move;
        m_computerBlack = computerBlack;
        m_computerWhite = computerWhite;
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
        m_clock = new Clock();
        m_gameInfo = new GameInfo(m_clock);
        m_gameInfo.setBorder(GuiUtils.createSmallEmptyBorder());
        m_infoPanel.add(m_gameInfo, BorderLayout.NORTH);

        m_board = new Board(m_boardSize);

        m_guiBoard = new GuiBoard(m_boardSize, fastPaint);
        m_guiBoard.setListener(this);
        m_statusBar = new StatusBar();
        m_innerPanel.add(m_statusBar, BorderLayout.SOUTH);

        Comment.Listener commentListener = new Comment.Listener()
            {
                public void changed()
                {
                    cbCommentChanged();
                }

                public void textSelected(String text)
                {
                    if (text == null)
                        text = "";
                    GoPoint list[] =
                        GtpUtils.parsePointString(text, m_boardSize);
                    GuiBoardUtils.showPointList(m_guiBoard, list);
                }
            };
        m_comment = new Comment(commentListener, m_fastPaint);
        m_comment.setFontFixed(m_prefs.getBool("comment-font-fixed"));
        m_infoPanel.add(m_comment, BorderLayout.CENTER);
        m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     m_guiBoard, m_infoPanel);
        GuiUtils.removeKeyBinding(m_splitPane, "F8");
        m_splitPane.setResizeWeight(1);
        m_innerPanel.add(m_splitPane, BorderLayout.CENTER);
        WindowAdapter windowAdapter = new WindowAdapter()
            {
                public void windowClosing(WindowEvent event)
                {
                    close();
                }
            };
        addWindowListener(windowAdapter);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        GuiUtils.setGoIcon(this);
        RecentFileMenu.Callback recentCallback = new RecentFileMenu.Callback()
            {
                public void fileSelected(String label, File file)
                {
                    if (m_needsSave && ! checkSaveGame())
                        return;
                    loadFile(file, -1);
                }
            };
        RecentFileMenu.Callback recentGtp = new RecentFileMenu.Callback()
            {
                public void fileSelected(String label, File file)
                {
                    if (m_gtpShell == null)
                        return;
                    sendGtpFile(file);
                    m_menuBar.addRecentGtp(file);
                }
            };
        m_menuBar = new GoGuiMenuBar(this, recentCallback, recentGtp);
        m_menuBar.selectBoardSizeItem(m_boardSize);
        boolean onlySupported
            = m_prefs.getBool("analyze-only-supported-commands");
        m_menuBar.setAnalyzeOnlySupported(onlySupported);
        m_menuBar.setAnalyzeSort(m_prefs.getBool("analyze-sort"));
        m_menuBar.setGameTreeLabels(m_prefs.getInt("gametree-labels"));
        m_menuBar.setGameTreeSize(m_prefs.getInt("gametree-size"));
        boolean showSubtreeSizes =
            m_prefs.getBool("gametree-show-subtree-sizes");
        m_menuBar.setShowSubtreeSizes(showSubtreeSizes);
        m_menuBar.setAutoNumber(m_prefs.getBool("gtpshell-autonumber"));
        boolean completion
            = ! m_prefs.getBool("gtpshell-disable-completions");
        m_menuBar.setCommandCompletion(completion);
        m_menuBar.setCommentFontFixed(m_prefs.getBool("comment-font-fixed"));
        m_menuBar.setTimeStamp(m_prefs.getBool("gtpshell-timestamp"));
        m_menuBar.setBeepAfterMove(m_beepAfterMove);
        m_menuBar.setShowInfoPanel(m_showInfoPanel);
        m_menuBar.setShowToolbar(m_showToolbar);
        m_menuBar.setShowLastMove(m_prefs.getBool("show-last-move"));
        m_menuBar.setShowVariations(m_prefs.getBool("show-variations"));
        m_showLastMove = m_prefs.getBool("show-last-move");
        m_showVariations = m_prefs.getBool("show-variations");
        m_menuBar.setShowCursor(m_prefs.getBool("show-cursor"));
        m_menuBar.setShowGrid(m_prefs.getBool("show-grid"));
        m_guiBoard.setShowCursor(m_prefs.getBool("show-cursor"));
        m_guiBoard.setShowGrid(m_prefs.getBool("show-grid"));
        setJMenuBar(m_menuBar.getMenuBar());
        if (program != null)
            m_program = program;
        else if (m_prefs.contains("program"))
            m_program = m_prefs.getString("program");
        if (m_program != null && m_program.trim().equals(""))
            m_program = null;
        if (m_program == null)
        {
            m_toolBar.setComputerEnabled(false);
            m_menuBar.setComputerEnabled(false);
        }
        m_menuBar.setNormalMode();
        m_guiBoard.requestFocusInWindow();
        if (time != null)
            m_timeSettings = TimeSettings.parse(time);
        Runnable callback = new Runnable()
            {
                public void run()
                {
                    initialize();
                }
            };
        SwingUtilities.invokeLater(callback);
    }
    
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (isCommandInProgress()
            && ! command.equals("about")
            && ! command.equals("beep-after-move")
            && ! command.equals("comment-font-fixed")
            && ! command.equals("computer-black")
            && ! command.equals("computer-both")
            && ! command.equals("computer-none")
            && ! command.equals("computer-white")
            && ! command.equals("detach-program")
            && ! command.equals("gtpshell-save")
            && ! command.equals("gtpshell-save-commands")
            && ! command.equals("command-completion")
            && ! command.equals("auto-number")
            && ! command.equals("timestamp")
            && ! command.equals("help")
            && ! command.equals("interrupt")
            && ! command.equals("show-grid")
            && ! command.equals("show-shell")
            && ! command.equals("show-toolbar")
            && ! command.equals("show-tree")
            && ! command.equals("show-info-panel")
            && ! command.equals("show-last-move")
            && ! command.equals("exit"))
            return;
        if (command.equals("about"))
            cbAbout();
        else if (command.equals("add-bookmark"))
            cbAddBookmark();
        else if (command.equals("edit-bookmarks"))
            cbEditBookmarks();
        else if (command.equals("analyze"))
            cbAnalyze();
        else if (command.equals("analyze-only-supported"))
            cbAnalyzeOnlySupported();
        else if (command.equals("analyze-reload"))
            cbAnalyzeReload();
        else if (command.equals("analyze-sort"))
            cbAnalyzeSort();
        else if (command.equals("attach-program"))
            cbAttachProgram();
        else if (command.equals("auto-number"))
            cbAutoNumber();
        else if (command.equals("back-to-main-variation"))
            cbBackToMainVar();
        else if (command.equals("backward"))
            cbBackward(1);
        else if (command.equals("backward-10"))
            cbBackward(10);
        else if (command.equals("beep-after-move"))
            cbBeepAfterMove();
        else if (command.equals("beginning"))
            cbBeginning();
        else if (command.equals("board-size-other"))
            cbBoardSizeOther();
        else if (command.startsWith("board-size-"))
            cbBoardSize(command.substring("board-size-".length()));
        else if (command.startsWith("bookmark-"))
            cbBookmark(command.substring("bookmark-".length()));
        else if (command.equals("clock-halt"))
            cbClockHalt();
        else if (command.equals("clock-resume"))
            cbClockResume();
        else if (command.equals("clock-restore"))
            cbClockRestore();
        else if (command.equals("command-completion"))
            cbCommandCompletion();
        else if (command.equals("comment-font-fixed"))
            cbCommentFontFixed();
        else if (command.equals("computer-black"))
            computerBlack();
        else if (command.equals("computer-both"))
            cbComputerBoth();
        else if (command.equals("computer-none"))
            computerNone();
        else if (command.equals("computer-white"))
            computerWhite();
        else if (command.equals("detach-program"))
            cbDetachProgram();
        else if (command.equals("end"))
            cbEnd();
        else if (command.equals("exit"))
            close();
        else if (command.equals("export-ascii"))
            cbExportAscii();
        else if (command.equals("export-latex"))
            cbExportLatex();
        else if (command.equals("export-latex-position"))
            cbExportLatexPosition();
        else if (command.equals("export-sgf-position"))
            cbExportSgfPosition();
        else if (command.equals("find-in-comments"))
            cbFindInComments();
        else if (command.equals("find-next"))
            cbFindNext();
        else if (command.equals("forward"))
            cbForward(1);
        else if (command.equals("forward-10"))
            cbForward(10);
        else if (command.equals("game-info"))
            cbGameInfo();
        else if (command.equals("goto"))
            cbGoto();
        else if (command.equals("goto-variation"))
            cbGotoVariation();
        else if (command.equals("gametree-move"))
            cbGameTreeLabels(GameTreePanel.LABEL_MOVE);
        else if (command.equals("gametree-number"))
            cbGameTreeLabels(GameTreePanel.LABEL_NUMBER);
        else if (command.equals("gametree-none"))
            cbGameTreeLabels(GameTreePanel.LABEL_NONE);
        else if (command.equals("gametree-large"))
            cbGameTreeSize(GameTreePanel.SIZE_LARGE);
        else if (command.equals("gametree-normal"))
            cbGameTreeSize(GameTreePanel.SIZE_NORMAL);
        else if (command.equals("gametree-small"))
            cbGameTreeSize(GameTreePanel.SIZE_SMALL);
        else if (command.equals("gametree-tiny"))
            cbGameTreeSize(GameTreePanel.SIZE_TINY);
        else if (command.equals("gametree-show-subtree-sizes"))
            cbGameTreeShowSubtreeSizes();
        else if (command.equals("gtpshell-save"))
            cbGtpShellSave();
        else if (command.equals("gtpshell-save-commands"))
            cbGtpShellSaveCommands();
        else if (command.equals("gtpshell-send-file"))
            cbGtpShellSendFile();
        else if (command.startsWith("handicap-"))
            cbHandicap(command.substring("handicap-".length()));
        else if (command.equals("help"))
            cbHelp();
        else if (command.equals("interrupt"))
            cbInterrupt();
        else if (command.equals("keep-only-main-variation"))
            cbKeepOnlyMainVariation();
        else if (command.equals("keep-only-position"))
            cbKeepOnlyPosition();
        else if (command.equals("make-main-variation"))
            cbMakeMainVariation();
        else if (command.equals("next-variation"))
            cbNextVariation();
        else if (command.equals("next-earlier-variation"))
            cbNextEarlierVariation();
        else if (command.equals("new-game"))
            cbNewGame(m_boardSize);
        else if (command.equals("open"))
            cbOpen();
        else if (command.equals("pass"))
            cbPass();
        else if (command.equals("play"))
            cbPlay(false);
        else if (command.equals("play-single"))
            cbPlay(true);
        else if (command.equals("previous-variation"))
            cbPreviousVariation();
        else if (command.equals("previous-earlier-variation"))
            cbPreviousEarlierVariation();
        else if (command.equals("print"))
            cbPrint();
        else if (command.equals("save"))
            cbSave();
        else if (command.equals("save-as"))
            cbSaveAs();
        else if (command.equals("score"))
            cbScore();
        else if (command.equals("score-cancel"))
            cbScoreDone(false);
        else if (command.equals("score-done"))
            cbScoreDone(true);
        else if (command.equals("setup"))
            cbSetup();
        else if (command.equals("setup-black"))
            cbSetupBlack();
        else if (command.equals("setup-white"))
            cbSetupWhite();
        else if (command.equals("show-cursor"))
            cbShowCursor();
        else if (command.equals("show-grid"))
            cbShowGrid();
        else if (command.equals("show-info-panel"))
            cbShowInfoPanel();
        else if (command.equals("show-last-move"))
            cbShowLastMove();
        else if (command.equals("show-shell"))
            cbShowShell();
        else if (command.equals("show-toolbar"))
            cbShowToolbar();
        else if (command.equals("show-tree"))
            cbShowTree();
        else if (command.equals("show-variations"))
            cbShowVariations();
        else if (command.equals("timestamp"))
            cbTimeStamp();
        else if (command.equals("truncate"))
            cbTruncate();
        else if (command.equals("truncate-children"))
            cbTruncateChildren();
        else
            assert(false);
    }
    
    public void cbAnalyze()
    {        
        if (m_commandThread == null)
            return;
        if (m_menuBar.getShowAnalyze())
        {
            if (m_analyzeDialog == null)
            {
                boolean onlySupported = m_menuBar.getAnalyzeOnlySupported();
                boolean sort = m_menuBar.getAnalyzeSort();
                m_analyzeDialog =
                    new AnalyzeDialog(this, this, onlySupported, sort,
                                      m_commandThread.getSupportedCommands(),
                                      m_programAnalyzeCommands,
                                      m_commandThread);
                m_analyzeDialog.addWindowListener(new WindowAdapter()
                    {
                        public void windowClosing(WindowEvent e)
                        {
                            m_menuBar.setShowAnalyze(false);
                        }
                    });
                m_analyzeDialog.setBoardSize(m_board.getSize());
                restoreSize(m_analyzeDialog, "window-analyze");
                setTitle();
            }
            m_analyzeDialog.setVisible(true);
        }
        else
        {
            if (m_analyzeDialog != null)
                m_analyzeDialog.close();
            m_analyzeDialog = null;
        }
    }

    public void cbAnalyzeOnlySupported()
    {
        boolean onlySupported = m_menuBar.getAnalyzeOnlySupported();
        m_prefs.setBool("analyze-only-supported-commands", onlySupported);
        if (m_analyzeDialog != null)
            m_analyzeDialog.setOnlySupported(onlySupported);
    }

    public void cbAnalyzeReload()
    {
        if (m_analyzeDialog != null)
            m_analyzeDialog.reload();
    }

    public void cbAnalyzeSort()
    {
        boolean sort = m_menuBar.getAnalyzeSort();
        m_prefs.setBool("analyze-sort", sort);
        if (m_analyzeDialog != null)
            m_analyzeDialog.setSort(sort);
    }

    public void cbAttachProgram()
    {        
        String program = SelectProgram.select(this);
        if (program == null)
            return;
        if (m_commandThread != null)
            if (! cbDetachProgram())
                return;
        if (! attachProgram(program))
        {
            m_prefs.setString("program", "");
            return;
        }
        m_prefs.setString("program", m_program);
        if (m_gtpShell != null && m_prefs.getBool("show-gtpshell"))
        {
            m_menuBar.setShowShell(true);
            cbShowShell();
        }
        if (m_prefs.getBool("show-analyze"))
        {
            m_menuBar.setShowAnalyze(true);
            cbAnalyze();
        }
        toFrontLater();
    }

    public void cbAutoNumber(boolean enable)
    {
        if (m_commandThread != null)
            m_commandThread.setAutoNumber(enable);
    }

    public void cbBackward(int n)
    {
        backward(n);
        boardChangedBegin(false, false);
    }

    public void cbBeginning()
    {
        backward(NodeUtils.getDepth(m_currentNode));
        boardChangedBegin(false, false);
    }

    public void cbCommandCompletion()
    {
        if (m_gtpShell == null)
            return;
        boolean commandCompletion = m_menuBar.getCommandCompletion();
        m_gtpShell.setCommandCompletion(commandCompletion);
        m_prefs.setBool("gtpshell-disable-completions", ! commandCompletion);
    }

    public boolean cbDetachProgram()
    {        
        if (m_commandThread == null)
            return false;
        if (isCommandInProgress())
        {
            if (! showQuestion("Kill program?"))
                return false;
        }
        detachProgram();
        m_prefs.setString("program", "");
        return true;
    }

    public void cbEnd()
    {
        forward(NodeUtils.getNodesLeft(m_currentNode));
        boardChangedBegin(false, false);
    }

    public void cbForward(int n)
    {
        forward(n);
        boardChangedBegin(false, false);
    }

    public void cbGotoNode(Node node)
    {
        gotoNode(node);
        boardChangedBegin(false, false);
    }

    public void cbGtpShellSave()
    {
        if (m_gtpShell == null)
            return;
        m_gtpShell.saveLog(this);
    }

    public void cbGtpShellSaveCommands()
    {
        if (m_gtpShell == null)
            return;
        m_gtpShell.saveCommands(this);
    }

    public void cbGtpShellSendFile()
    {
        if (m_gtpShell == null)
            return;
        File file = SimpleDialogs.showOpen(this, "Choose GTP file");
        if (file == null)
            return;
        sendGtpFile(file);
        m_menuBar.addRecentGtp(file);
    }

    public void cbNextVariation()
    {
        Node node = NodeUtils.getNextVariation(m_currentNode);
        if (node != null)
            cbGotoNode(node);
    }

    public void cbNextEarlierVariation()
    {
        Node node = NodeUtils.getNextEarlierVariation(m_currentNode);
        if (node != null)
            cbGotoNode(node);
    }

    public void cbPreviousVariation()
    {
        Node node = NodeUtils.getPreviousVariation(m_currentNode);
        if (node != null)
            cbGotoNode(node);
    }

    public void cbPreviousEarlierVariation()
    {
        Node node = NodeUtils.getPreviousEarlierVariation(m_currentNode);
        if (node != null)
            cbGotoNode(node);
    }
    public void cbShowShell()
    {
        if (m_gtpShell == null)
            return;
        m_gtpShell.setVisible(m_menuBar.getShowShell());
    }

    public void cbShowInfoPanel()
    {
        if (GuiUtils.isNormalSizeMode(this))
        {
            if (m_showInfoPanel)
                m_comment.setPreferredSize(m_comment.getSize());
            m_guiBoard.setPreferredFieldSize(m_guiBoard.getFieldSize());
        }
        showInfoPanel();
    }

    public void cbShowToolbar()
    {
        if (GuiUtils.isNormalSizeMode(this))
        {
            if (m_showInfoPanel)
                m_comment.setPreferredSize(m_comment.getSize());
            m_guiBoard.setPreferredFieldSize(m_guiBoard.getFieldSize());
        }
        showToolbar();
    }

    public void cbShowTree()
    {
        if (m_menuBar.getShowTree())
        {
            if (m_gameTreeViewer == null)
            {
                m_gameTreeViewer
                    = new GameTreeViewer(this, this, m_fastPaint);
                m_gameTreeViewer.setLabelMode(m_menuBar.getGameTreeLabels());
                m_gameTreeViewer.setSizeMode(m_menuBar.getGameTreeSize());
                boolean showSubtreeSizes = m_menuBar.getShowSubtreeSizes();
                m_gameTreeViewer.setShowSubtreeSizes(showSubtreeSizes);
                restoreSize(m_gameTreeViewer, "window-gametree");
            }
            updateGameTree(true);
            if (m_gameTreeViewer != null) // updateGameTree can close viewer
                m_gameTreeViewer.setVisible(true);
            return;
        }
        else
            disposeGameTree();
    }

    public void clearAnalyzeCommand()
    {
        clearAnalyzeCommand(true);
    }

    public void clearAnalyzeCommand(boolean resetBoard)
    {
        if (m_analyzeCommand != null)
        {
            if (isCommandInProgress())
            {
                showError("Cannot clear analyze command\n" +
                          "while command in progress");
                return;
            }
            if (m_setupMode)
            {
                showError("Cannot clear analyze command\n" +
                          "in setup mode");
                return;
            }
            if (m_analyzeDialog != null)
                m_analyzeDialog.setRunButtonEnabled(true);
            m_analyzeCommand = null;
            setBoardCursorDefault();
        }
        if (resetBoard)
        {
            resetBoard();
            clearStatus();
        }
    }

    public void contextMenu(GoPoint point, Component invoker, int x, int y)
    {
        if (isCommandInProgress())
            return;
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

    public void disposeGameTree()
    {
        if (m_gameTreeViewer == null)
            return;
        m_gameTreeViewer.dispose();
        m_gameTreeViewer = null;
        m_menuBar.setShowTree(false);
    }

    public void fieldClicked(GoPoint p, boolean modifiedSelect)
    {
        if (isCommandInProgress())
            return;
        if (m_setupMode)
        {
            GoColor toMove = m_board.getToMove();
            GoColor color;
            if (modifiedSelect)
                color = toMove.otherColor();
            else if (m_board.getColor(p) == toMove)
                color = GoColor.EMPTY;
            else
                color = toMove;
            m_board.play(p, color);
            m_board.setToMove(toMove);
            updateGameInfo(true);
            updateFromGoBoard();
            setNeedsSave(true);
        }
        else if (m_analyzeCommand != null && m_analyzeCommand.needsPointArg()
                 && ! modifiedSelect)
        {
            m_analyzeCommand.setPointArg(p);
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(p, true);
            m_analyzeDialog.setRunButtonEnabled(true);
            analyzeBegin(false, false);
            return;
        }
        else if (m_analyzeCommand != null
                 && m_analyzeCommand.needsPointListArg())
        {
            ArrayList pointListArg = m_analyzeCommand.getPointListArg();
            if (pointListArg.contains(p))
            {
                pointListArg.remove(p);
                if (modifiedSelect)
                    pointListArg.add(p);
            }
            else
                pointListArg.add(p);
            m_guiBoard.clearAllSelect();
            GuiBoardUtils.setSelect(m_guiBoard, pointListArg, true);
            if (modifiedSelect && pointListArg.size() > 0)
                analyzeBegin(false, false);
            return;
        }
        else if (m_scoreMode && ! modifiedSelect)
        {
            GuiBoardUtils.scoreSetDead(m_guiBoard, m_board, p);
            double komi = m_gameTree.getGameInformation().m_komi;
            m_scoreDialog.showScore(m_board.scoreGet(komi, getRules()));
            return;
        }
        else if (modifiedSelect)
            m_guiBoard.contextMenu(p);
        else
        {
            if (m_board.isSuicide(p, m_board.getToMove())
                && ! showQuestion("Play suicide?"))
                return;
            else if (m_board.isKo(p)
                && ! showQuestion("Play illegal Ko move?"))
                return;
            Move move = Move.get(p, m_board.getToMove());
            humanMoved(move);
        }
    }

    public boolean sendGtpCommand(String command, boolean sync)
        throws GtpError
    {
        if (isCommandInProgress() || m_commandThread == null)
            return false;
        if (m_setupMode)
        {
            showError("Cannot send command in setup mode");
            return false;
        }
        if (sync)
        {
            m_commandThread.send(command);
            return true;
        }
        Runnable callback = new Runnable()
            {
                public void run()
                {
                    sendGtpCommandContinue();
                }
            };
        beginLengthyCommand();
        m_commandThread.send(command, callback);
        return true;
    }

    public void sendGtpCommandContinue()
    {
        endLengthyCommand();
    }

    public void initAnalyzeCommand(AnalyzeCommand command, boolean autoRun)
    {
        if (m_commandThread == null)
            return;
        m_analyzeCommand = command;
        m_analyzeAutoRun = autoRun;
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

    public void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                  boolean clearBoard, boolean oneRunOnly)
    {
        if (isCommandInProgress())
        {
            showError("Cannot run analyze command\n" +
                      "while command in progress");
            return;
        }
        if (m_setupMode)
        {
            showError("Cannot run analyze command\n" +
                      "in setup mode");
            return;
        }
        initAnalyzeCommand(command, autoRun);
        m_analyzeOneRunOnly = oneRunOnly;
        boolean needsPointArg = m_analyzeCommand.needsPointArg();
        if (needsPointArg && ! m_analyzeCommand.isPointArgMissing())
        {
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(m_analyzeCommand.getPointArg(), true);
            if (m_analyzeDialog != null)
                m_analyzeDialog.setRunButtonEnabled(true);
        }
        else if (needsPointArg || m_analyzeCommand.needsPointListArg())
        {
            m_guiBoard.clearAllSelect();
            if (m_analyzeDialog != null)
                m_analyzeDialog.setRunButtonEnabled(false);
            if (m_analyzeCommand.getType() == AnalyzeCommand.EPLIST)
                GuiBoardUtils.setSelect(m_guiBoard,
                                        m_analyzeCommand.getPointListArg(),
                                        true);
            toFront();
            return;
        }
        analyzeBegin(false, clearBoard);
    }    

    private class AnalyzeContinue
        implements Runnable
    {
        public AnalyzeContinue(boolean checkComputerMove,
                               boolean resetBoard)
        {
            m_checkComputerMove = checkComputerMove;
            m_resetBoard = resetBoard;
        }

        public void run()
        {
            analyzeContinue(m_checkComputerMove, m_resetBoard);
        }
        
        private final boolean m_checkComputerMove;

        private final boolean m_resetBoard;
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
            if (m_line.trim().equals(""))
                showWarning("Invalid empty response line");
            else
                showWarning("Invalid response:\n" + m_line);
        }
        
        private final String m_line;
    }

    private static class LoadFileRunnable
        implements GuiUtils.ProgressRunnable
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
            m_reader = new SgfReader(m_in, m_file.toString(), progressShow,
                                     m_file.length());
        }

        private final File m_file;

        private final FileInputStream m_in;

        private SgfReader m_reader;
    }

    private boolean m_analyzeAutoRun;

    private boolean m_analyzeOneRunOnly;

    private boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    private boolean m_fastPaint;

    private boolean m_ignoreInvalidResponses;

    /** State variable used between cbInterrupt and computerMoved. */
    private boolean m_interruptComputerBoth;

    private boolean m_isRootExecuted;

    /** State variable used between generateMove and computerMoved. */
    private boolean m_isSingleMove;

    private boolean m_lostOnTimeShown;

    private boolean m_needsSave;

    private boolean m_resigned;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_showInfoPanel;

    private boolean m_showLastMove;

    private boolean m_showToolbar;

    private boolean m_showVariations;

    private boolean m_verbose;

    private int m_boardSize;

    /** Counter for successfully executed moves/setup stones in current node.
        If the current node contains several moves/setup stones, it is
        necessary to count, how many were successfully executed, because if
        one of them fails, the program might not support undo, so we have to
        detect, that the current node was only partially executed.
    */
    private int m_currentNodeExecuted;

    private int m_handicap;

    private int m_move;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private Board m_board;

    private GuiBoard m_guiBoard;

    private Clock m_clock;

    private CommandThread m_commandThread;

    private Comment m_comment;

    /** Last loaded or saved file.
        If file was modified, the value is null.
    */
    private File m_loadedFile;

    private GameInfo m_gameInfo;    

    private GtpShell m_gtpShell;

    private GameTree m_gameTree;

    private GameTreeViewer m_gameTreeViewer;

    private Help m_help;

    private JPanel m_infoPanel;

    private JPanel m_innerPanel;

    private JSplitPane m_splitPane;

    private GoGuiMenuBar m_menuBar;

    private Node m_currentNode;

    private OptionalWarning m_gameFinishedMessage;

    private OptionalWarning m_overwriteWarning;

    private OptionalWarning m_passWarning;

    private Pattern m_pattern;

    private AnalyzeCommand m_analyzeCommand;

    private File m_file;

    private StatusBar m_statusBar;

    private String m_gtpCommand;

    private String m_gtpFile;

    private String m_initAnalyze;

    private String m_lastAnalyzeCommand;

    private String m_name;

    private String m_program;

    private String m_titleFromProgram;

    private String m_version = "";

    private AnalyzeDialog m_analyzeDialog;    

    private Preferences m_prefs;

    private ScoreDialog m_scoreDialog;

    private String m_programAnalyzeCommands;

    private Thumbnail m_thumbnail = new Thumbnail(false);

    private TimeSettings m_timeSettings;

    private GoGuiToolBar m_toolBar;

    private ArrayList m_bookmarks;

    private void analyzeBegin(boolean checkComputerMove, boolean resetBoard)
    {
        if (m_commandThread == null || m_analyzeCommand == null
            || m_analyzeCommand.isPointArgMissing())
            return;
        showStatus("Running " + m_analyzeCommand.getResultTitle() + "...");
        GoColor toMove = m_board.getToMove();
        m_lastAnalyzeCommand = m_analyzeCommand.replaceWildCards(toMove);
        runLengthyCommand(m_lastAnalyzeCommand,
                          new AnalyzeContinue(checkComputerMove, resetBoard));
    }

    private void analyzeContinue(boolean checkComputerMove,
                                 boolean resetBoard)
    {
        if (resetBoard)
            resetBoard();
        if (! endLengthyCommand())
            return;
        String title = m_analyzeCommand.getResultTitle();
        try
        {
            String response = m_commandThread.getResponse();
            AnalyzeShow.show(m_analyzeCommand, m_guiBoard, m_board, response);
            int type = m_analyzeCommand.getType();
            GoPoint pointArg = null;
            if (m_analyzeCommand.needsPointArg())
                pointArg = m_analyzeCommand.getPointArg();
            else if (m_analyzeCommand.needsPointListArg())
            {
                ArrayList list = m_analyzeCommand.getPointListArg();
                if (list.size() > 0)
                    pointArg = (GoPoint)list.get(list.size() - 1);
            }
            if (type == AnalyzeCommand.PARAM)
                ParameterDialog.editParameters(m_lastAnalyzeCommand, this,
                                               title, response,
                                               m_commandThread);
            boolean statusContainsResponse = false;
            if (AnalyzeCommand.isTextType(type))
            {
                if (response.indexOf("\n") < 0)
                {
                    showStatus(title + ": " + response);
                    statusContainsResponse = true;
                }
                else
                    showAnalyzeTextOutput(type, pointArg, title, response);
            }
            if (! statusContainsResponse && type != AnalyzeCommand.PARAM)
                showStatus(title);
            if (checkComputerMove)
                checkComputerMove();
        }
        catch (GtpError e)
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

    /** Attach program.
        @return true if program was successfully attached.
    */
    private boolean attachProgram(String program)
    {
        program = program.trim();
        if (program.equals(""))
            return false;
        m_program = program;
        if (m_gtpShell != null)
        {
            m_gtpShell.dispose();
            m_gtpShell = null;
        }
        m_gtpShell = new GtpShell(this, this, m_prefs, m_fastPaint);
        m_gtpShell.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    m_menuBar.setShowShell(false);
                }
            });
        m_gtpShell.setProgramCommand(program);
        m_gtpShell.setTimeStamp(m_menuBar.getTimeStamp());
        m_gtpShell.setCommandCompletion(m_menuBar.getCommandCompletion());
        m_ignoreInvalidResponses = false;
        GtpClient.InvalidResponseCallback invalidResponseCallback =
            new GtpClient.InvalidResponseCallback()
            {
                public void show(String line)
                {
                    if (m_ignoreInvalidResponses)
                        return;
                    m_ignoreInvalidResponses = true;
                    Runnable runnable = new ShowInvalidResponse(line);
                    if (SwingUtilities.isEventDispatchThread())
                        runnable.run();
                    else
                        invokeAndWait(runnable);
                }
            };
        try
        {
            GtpClient gtp = new GtpClient(m_program, m_verbose, m_gtpShell);
            gtp.setInvalidResponseCallback(invalidResponseCallback);
            gtp.setAutoNumber(m_menuBar.getAutoNumber());
            m_commandThread = new CommandThread(gtp, this);
            m_commandThread.start();
        }
        catch (GtpError e)
        {
            showError(e);
            m_toolBar.setComputerEnabled(false);
            m_menuBar.setComputerEnabled(false);
            return false;
        }
        m_menuBar.setComputerEnabled(true);
        m_toolBar.setComputerEnabled(true);
        m_name = null;
        m_titleFromProgram = null;
        try
        {
            m_name = m_commandThread.send("name").trim();
        }
        catch (GtpError e)
        {
        }
        if (m_name == null)
            m_name = "Unknown Program";
        try
        {
            m_commandThread.queryProtocolVersion();
        }
        catch (GtpError e)
        {
        }
        try
        {
            m_version = m_commandThread.queryVersion();
            m_gtpShell.setProgramVersion(m_version);
            m_commandThread.querySupportedCommands();
            m_commandThread.queryInterruptSupport();
        }
        catch (GtpError e)
        {
        }        
        boolean cleanupSupported
            = m_commandThread.isCommandSupported("kgs-genmove_cleanup")
            || m_commandThread.isCommandSupported("genmove_cleanup");
        m_menuBar.enableCleanup(cleanupSupported);
        initProgramAnalyzeCommands();
        restoreSize(m_gtpShell, "window-gtpshell");
        m_gtpShell.setProgramName(m_name);
        ArrayList supportedCommands =
            m_commandThread.getSupportedCommands();
        m_gtpShell.setInitialCompletions(supportedCommands);
        if (! m_gtpFile.equals(""))
            sendGtpFile(new File(m_gtpFile));
        if (! m_gtpCommand.equals(""))
            sendGtpString(m_gtpCommand);
        Node oldCurrentNode = m_currentNode;
        m_board.initSize(m_boardSize);
        if (executeRoot())
            gotoNode(oldCurrentNode);
        setTitle();
        return true;
    }    

    /** Go backward a number of nodes in the tree. */
    private boolean backward(int n)
    {
        if (n == 0)
            return true;
        int total = 0;
        Node node = m_currentNode;
        for (int i = 0; i < n && node != m_gameTree.getRoot(); ++i)
        {
            total += NodeUtils.getAllAsMoves(node).size();
            node = node.getFather();
        }
        try
        {
            if (m_commandThread != null && total > 1
                && m_commandThread.isCommandSupported("gg-undo"))
            {
                m_commandThread.send("gg-undo " + total);
                m_board.undo(total);
                m_currentNode = node;
                m_currentNodeExecuted
                    = NodeUtils.getAllAsMoves(m_currentNode).size();
            }
            else
            {
                for (int i = 0; i < n; ++i)
                {
                    if (m_currentNode.getFather() == null)
                        return false;
                    undoCurrentNode();
                    m_currentNode = m_currentNode.getFather();
                    m_currentNodeExecuted
                        = NodeUtils.getAllAsMoves(m_currentNode).size();
                    m_gameInfo.fastUpdateMoveNumber(m_currentNode);
                }
            }
        }
        catch (GtpError e)
        {
            showError(e);
            return false;
        }
        return true;
    }

    private void beginLengthyCommand()
    {
        setBoardCursor(Cursor.WAIT_CURSOR);
        m_menuBar.setCommandInProgress();
        m_toolBar.setCommandInProgress();
        m_gtpShell.setCommandInProgess(true);
    }

    private void boardChangedBegin(boolean doCheckComputerMove,
                                   boolean gameTreeChanged)
    {
        updateFromGoBoard();
        updateGameInfo(gameTreeChanged);
        m_toolBar.update(m_currentNode);
        updateMenuBar();
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        if (m_commandThread != null
            && isCurrentNodeExecuted()
            && m_analyzeCommand != null
            && m_analyzeAutoRun
            && ! m_analyzeCommand.isPointArgMissing())
            analyzeBegin(doCheckComputerMove, true);
        else
        {
            resetBoard();
            showToMove();
            if (doCheckComputerMove)
                checkComputerMove();
        }
    }

    private void cbAbout()
    {
        String protocolVersion = null;
        String command = null;
        if (m_commandThread != null)
        {
            protocolVersion =
                Integer.toString(m_commandThread.getProtocolVersion());
            command = m_commandThread.getProgramCommand();
        }
        AboutDialog.show(this, m_name, m_version, protocolVersion, command);
    }

    private void cbAddBookmark()
    {
        if (m_loadedFile == null)
        {
            showError("Cannot set bookmark if no file loaded");
            return;
        }
        if (m_needsSave)
        {
            showError("Cannot set bookmark if file modified");
            return;
        }
        if (m_currentNode.getFather() != null
            && m_currentNode.getMove() == null)
        {
            showError("Cannot set bookmark at non-root node without move");
            return;
        }
        String variation = NodeUtils.getVariationString(m_currentNode);
        int move = NodeUtils.getMoveNumber(m_currentNode);
        Bookmark bookmark = new Bookmark(m_loadedFile, move, variation);
        if (! BookmarkDialog.show(this, "Add Bookmark", bookmark, true))
            return;
        m_bookmarks.add(bookmark);
        m_menuBar.setBookmarks(m_bookmarks);
    }

    private void cbAutoNumber()
    {
        if (m_commandThread == null)
            return;
        boolean enable = m_menuBar.getAutoNumber();
        m_commandThread.setAutoNumber(enable);
        m_prefs.setBool("gtpshell-autonumber", enable);
    }

    private void cbBeepAfterMove()
    {
        m_beepAfterMove = m_menuBar.getBeepAfterMove();
        m_prefs.setBool("beep-after-move", m_beepAfterMove);
    }

    private void cbBackToMainVar()
    {
        Node node = NodeUtils.getBackToMainVariation(m_currentNode);
        cbGotoNode(node);
    }

    private void cbBoardSize(String size)
    {
        try
        {
            saveSession();
            cbNewGame(Integer.parseInt(size));
            m_clock.reset();
            m_clock.halt();
            m_gameInfo.updateTime();
            updateMenuBar();
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
    }

    private void cbBoardSizeOther()
    {
        int size = BoardSizeDialog.show(this, m_boardSize);
        if (size < 1 || size > GoPoint.MAXSIZE)
            return;
        saveSession();
        cbNewGame(size);
        m_clock.reset();
        m_clock.halt();
            updateMenuBar();
        m_gameInfo.updateTime();
    }
    
    private void cbBookmark(String number)
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        try
        {
            int n = Integer.parseInt(number);
            if (n < 0 || n >= m_bookmarks.size())
            {
                assert(false);
                return;
            }
            Bookmark bookmark = (Bookmark)m_bookmarks.get(n);
            File file = bookmark.m_file;
            if (m_loadedFile == null || ! file.equals(m_loadedFile))
                if (! loadFile(file, 0))
                    return;
            String variation = bookmark.m_variation;
            Node node = m_gameTree.getRoot();
            if (! variation.equals(""))
            {
                node = NodeUtils.findByVariation(node, variation);
                if (node == null)
                {
                    showError("Bookmark has invalid variation");
                    return;
                }
            }
            node = NodeUtils.findByMoveNumber(node, bookmark.m_move);
            if (node == null)
            {
                showError("Bookmark has invalid move number");
                return;
            }
            cbGotoNode(node);
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
    }

    private void cbClockHalt()
    {
        if (! m_clock.isRunning())
            return;
        m_clock.halt();
        updateMenuBar();
    }

    private void cbClockResume()
    {
        if (m_clock.isRunning())
            return;
        m_clock.startMove(m_board.getToMove());
        updateMenuBar();
    }

    private void cbClockRestore()
    {        
        GoColor color = m_board.getToMove();
        clockRestore(m_currentNode, color.otherColor());
        Node father = m_currentNode.getFather();
        if (father != null)
            clockRestore(father, color);
        m_gameInfo.updateTime();
        updateMenuBar();
    }

    private void cbCommentChanged()
    {
        setNeedsSave(true);
        if (m_gameTreeViewer != null)
            m_gameTreeViewer.redrawCurrentNode();
    }

    private void cbCommentFontFixed()
    {
        boolean fixed = m_menuBar.getCommentFontFixed();
        m_comment.setFontFixed(fixed);
        m_prefs.setBool("comment-font-fixed", fixed);
    }

    private void cbComputerBoth()
    {
        computerBoth();
        if (! isCommandInProgress())
            checkComputerMove();
    }

    private void cbEditBookmarks()
    {
        if (! EditBookmarksDialog.show(this, m_bookmarks))
            return;
        m_menuBar.setBookmarks(m_bookmarks);
    }

    private void cbExportAscii()
    {
        File file = SimpleDialogs.showSave(this, "Export Text Diagram");
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            BoardUtils.print(m_board, new PrintStream(out), false);
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e);
        }
    }

    private void cbExportSgfPosition()
    {
        File file = SimpleDialogs.showSaveSgf(this);
        if (file == null)
            return;
        try
        {
            savePosition(file);
        }
        catch (FileNotFoundException e)
        {
            showError("Could not save position", e);
        }
    }

    private void cbExportLatex()
    {
        File file = SimpleDialogs.showSave(this, "Export LaTeX");
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title = FileUtils.removeExtension(new File(file.getName()),
                                                     "tex");
            new TexWriter(title, out, m_gameTree, false);
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e);
        }
    }

    private void cbExportLatexPosition()
    {
        File file = SimpleDialogs.showSave(this, "Export LaTeX Position");
        if (file == null)
            return;
        try
        {
            OutputStream out = new FileOutputStream(file);
            String title = FileUtils.removeExtension(new File(file.getName()),
                                                     "tex");
            new TexWriter(title, out, m_board, false,
                          GuiBoardUtils.getLabels(m_guiBoard),
                          GuiBoardUtils.getMarkSquare(m_guiBoard),
                          GuiBoardUtils.getSelects(m_guiBoard));
        }
        catch (FileNotFoundException e)
        {
            showError("Export failed", e);
        }
    }

    private void cbFindInComments()
    {
        Pattern pattern = FindDialog.run(this, m_comment.getSelectedText());
        if (pattern == null)
            return;
        m_pattern = pattern;
        m_menuBar.enableFindNext(true);
        if (NodeUtils.commentContains(m_currentNode, m_pattern))
            m_comment.markAll(m_pattern);
        else
            cbFindNext();
    }

    private void cbFindNext()
    {
        if (m_pattern == null)
            return;
        Node root = m_gameTree.getRoot();
        Node node = NodeUtils.findInComments(m_currentNode, m_pattern);
        if (node == null)
            if (m_currentNode != root)
                if (showQuestion("End of tree reached. Continue from start?"))
                {
                    node = root;
                    if (! NodeUtils.commentContains(node, m_pattern))
                        node = NodeUtils.findInComments(node, m_pattern);
                }
        if (node == null)
        {
            showInfo("Not found");
            m_menuBar.enableFindNext(false);
        }
        else
        {
            m_comment.markAll(m_pattern);
            cbGotoNode(node);
        }
    }

    private void cbGameInfo()
    {
        GameInformation gameInformation = m_gameTree.getGameInformation();
        if (! GameInfoDialog.show(this, gameInformation))
            return;
        if (gameInformation.m_komi != m_prefs.getDouble("komi"))
        {
            m_prefs.setDouble("komi", gameInformation.m_komi);
            setKomi(gameInformation.m_komi);
        }
        if (gameInformation.m_rules != null
            && ! gameInformation.m_rules.equals(m_prefs.getString("rules")))
        {
            m_prefs.setString("rules", gameInformation.m_rules);
            setRules();
        }
        TimeSettings timeSettings = gameInformation.m_timeSettings;
        if (timeSettings == null)
            m_timeSettings = null;
        else
        {
            m_timeSettings = new TimeSettings(timeSettings);
            setTimeSettings();
        }
        setTitle();
    }

    private void cbGameTreeLabels(int mode)
    {
        m_prefs.setInt("gametree-labels", mode);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setLabelMode(mode);
            updateGameTree(true);
        }
    }

    private void cbGameTreeSize(int mode)
    {
        m_prefs.setInt("gametree-size", mode);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setSizeMode(mode);
            updateGameTree(true);
        }
    }

    private void cbGameTreeShowSubtreeSizes()
    {
        boolean enable = m_menuBar.getShowSubtreeSizes();
        m_prefs.setBool("gametree-show-subtree-sizes", enable);
        if (m_gameTreeViewer != null)
        {
            m_gameTreeViewer.setShowSubtreeSizes(enable);
            updateGameTree(true);
        }
    }

    private void cbGoto()
    {
        Node node = MoveNumberDialog.show(this, m_currentNode);
        if (node == null)
            return;
        cbGotoNode(node);
    }

    private void cbGotoVariation()
    {
        Node node = GotoVariationDialog.show(this, m_gameTree, m_currentNode);
        if (node == null)
            return;
        cbGotoNode(node);
    }

    private void cbHandicap(String handicap)
    {
        try
        {
            m_handicap = Integer.parseInt(handicap);
            if (m_board.isModified())
                showInfo("Handicap will take effect on next game.");
            else
            {
                computerBlack();
                newGame(m_boardSize);
            }
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
    }

    private void cbHelp()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("net/sf/gogui/doc/index.html");
        if (url == null)
        {
            showError("Help not found");
            return;
        }
        if (Platform.isGnuClasspath())
        {
            // JEditorPane does not display HTML in GNU classpath 0.90 yet,
            // so we invoke a browser. Cannot use internal files from JAR
            // though
            File file = null;
            String s = url.toString();
            int pos = s.indexOf("lib/gogui.jar!/");
            if (pos >= 0 && s.startsWith("jar:file:"))
            {
                String path = s.substring(9, pos);
                File file1 = new File(path, "doc/manual/html/index.html");
                File file2 = new File(path, "../doc/gogui/index.html");
                if (file1.exists())
                    file = file1;
                else if (file2.exists())
                    file = file2;
                else
                {
                    showError("Documentation not found in:\n"
                              + file1.toString() + "\n"
                              + file2.toString());
                    return;
                }
                
            }
            else
            {
                showError("Cannot show documentation " +
                          " (must run from gogui.jar)");
                return;
            }
            try
            {
                if (! Platform.openInExternalBrowser(file.toURL()))
                    showError("Could not open browser");
            }
            catch (MalformedURLException e)
            {
                assert(false);
            }
            return;
        }
        if (m_help == null)
        {
            m_help = new Help(this, url);
            restoreSize(m_help, "window-help");
        }
        m_help.setVisible(true);
        m_help.toFront();
    }

    private void cbInterrupt()
    {
        if (! isCommandInProgress() || m_commandThread == null
            || m_commandThread.isProgramDead())
            return;
        if (Interrupt.run(this, m_commandThread))
            showStatus("Interrupting...");
    }

    private void cbKeepOnlyMainVariation()
    {
        if (! NodeUtils.isInMainVariation(m_currentNode))
            return;
        if (! showQuestion("Delete all variations but main?"))
            return;
        m_gameTree.keepOnlyMainVariation();
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void cbKeepOnlyPosition()
    {
        if (! showQuestion("Delete all moves?"))
            return;
        GameInformation info = m_gameTree.getGameInformation();
        m_gameTree = NodeUtils.makeTreeFromPosition(info, m_board);
        m_board.initSize(m_boardSize);
        executeRoot();
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void cbMakeMainVariation()
    {
        if (! showQuestion("Make current to main variation?"))
            return;
        NodeUtils.makeMainVariation(m_currentNode);
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void cbNewGame(int size)
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        m_prefs.setInt("boardsize", size);
        fileInvalid();
        newGame(size);
        m_clock.startMove(GoColor.BLACK);
        updateMenuBar();
        boardChangedBegin(true, true);
    }

    private void cbOpen()
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        File file = SimpleDialogs.showOpenSgf(this);
        if (file == null)
            return;
        m_menuBar.addRecent(file);
        loadFile(file, -1);
    }

    private void cbPass()
    {
        if (isCommandInProgress())
            return;
        if (m_passWarning == null)
            m_passWarning = new OptionalWarning(this);
        if (! m_passWarning.show("Really pass?",
                                 JOptionPane.QUESTION_MESSAGE))
            return;
        humanMoved(Move.getPass(m_board.getToMove()));
    }

    private void cbPlay(boolean isSingleMove)
    {
        if (m_commandThread == null || isCommandInProgress())
            return;
        if (! checkCurrentNodeExecuted())
            return;
        if (! isSingleMove && ! isComputerBoth())
        {
            if (m_board.getToMove() == GoColor.BLACK)
                computerBlack();
            else
                computerWhite();
        }
        m_interruptComputerBoth = false;
        generateMove(isSingleMove);
        if (m_currentNode == m_gameTree.getRoot()
            && m_currentNode.getNumberChildren() == 0)
            m_clock.reset();
        m_clock.startMove(m_board.getToMove());
    }


    private void cbPrint()
    {
        Print.run(this, m_guiBoard);
    }

    private void cbSave()
    {
        if (m_loadedFile != null)
        {
            if (m_loadedFile.exists())
            {
                if (m_overwriteWarning == null)
                    m_overwriteWarning = new OptionalWarning(this);
                String message = "Overwrite " + m_loadedFile + "?";
                if (! m_overwriteWarning.show(message))
                    return;
            }
            save(m_loadedFile);
        }
        else
            saveDialog();
    }

    private void cbSaveAs()
    {
        saveDialog();
    }

    private void cbScore()
    {
        if (m_commandThread == null)
        {
            showInfo("No program is attached.\n" +
                     "Please mark dead groups manually.");
            initScore(null);
            return;
        }
        if (m_commandThread.isCommandSupported("final_status_list"))
        {
            Runnable callback = new Runnable()
                {
                    public void run()
                    {
                        cbScoreContinue();
                    }
                };
            showStatus("Scoring...");
            runLengthyCommand("final_status_list dead", callback);
        }
        else
        {
            showInfo(m_name + " does not support scoring.\n" +
                     "Please mark dead groups manually.");
            initScore(null);
        }
    }

    private void cbScoreContinue()
    {
        boolean success = endLengthyCommand();
        clearStatus();
        GoPoint[] isDeadStone = null;
        if (success)
        {
            String response = m_commandThread.getResponse();
            try
            {
                isDeadStone = GtpUtils.parsePointList(response, m_boardSize);
            }
            catch (GtpError error)
            {
                showError(error);
            }
        }
        initScore(isDeadStone);
    }    

    private void cbScoreDone(boolean accepted)
    {
        m_scoreDialog.setVisible(false);
        if (accepted)
        {
            double komi = m_gameTree.getGameInformation().m_komi;
            setResult(m_board.scoreGet(komi, getRules()).formatResult());
        }
        clearStatus();
        m_guiBoard.clearAll();
        m_scoreMode = false;
        m_toolBar.enableAll(true, m_currentNode);
        m_menuBar.setNormalMode();
    }

    private void cbSetup()
    {
        if (m_setupMode)
        {
            setupDone();
            return;
        }
        if (m_needsSave && ! checkSaveGame())
            return;
        m_menuBar.setSetupMode();
        m_showLastMove = false;
        if (m_gameTreeViewer != null)
        {
            // Create a dummy game tree, so that GameTreeDialog shows
            // a setup node
            m_gameTree = new GameTree(m_boardSize, 0, null, null, null);
            m_currentNode = m_gameTree.getRoot();
            m_currentNode.addBlack(GoPoint.get(0, 0));
            m_clock.reset();
            updateGameInfo(true);
        }
        resetBoard();
        m_setupMode = true;
        m_toolBar.enableAll(false, null);
        showStatus("Setup Black");
        m_board.setToMove(GoColor.BLACK);
    }

    private void cbSetupBlack()
    {
        showStatus("Setup Black");
        m_board.setToMove(GoColor.BLACK);
        updateGameInfo(false);
    }

    private void cbSetupWhite()
    {
        showStatus("Setup White");
        m_board.setToMove(GoColor.WHITE);
        updateGameInfo(false);
    }

    private void cbShowCursor()
    {
        boolean showCursor = m_menuBar.getShowCursor();
        m_guiBoard.setShowCursor(showCursor);
        m_prefs.setBool("show-cursor", showCursor);
    }

    private void cbShowGrid()
    {
        boolean showGrid = m_menuBar.getShowGrid();
        m_guiBoard.setShowGrid(showGrid);
        m_prefs.setBool("show-grid", showGrid);
    }

    private void cbShowLastMove()
    {
        m_showLastMove = m_menuBar.getShowLastMove();
        m_prefs.setBool("show-last-move", m_showLastMove);
        updateFromGoBoard();
        updateGameInfo(false);
    }

    private void cbShowVariations()
    {
        m_showVariations = m_menuBar.getShowVariations();
        m_prefs.setBool("show-variations", m_showVariations);
        resetBoard();
        updateGameInfo(false);
    }

    private void cbTimeStamp()
    {
        if (m_gtpShell == null)
            return;
        boolean enable = m_menuBar.getTimeStamp();
        m_gtpShell.setTimeStamp(enable);
        m_prefs.setBool("gtpshell-timestamp", enable);
    }

    private void cbTruncate()
    {
        if (m_currentNode.getFather() == null)
            return;
        if (! showQuestion("Truncate current?"))
            return;
        Node oldCurrentNode = m_currentNode;
        backward(1);
        m_currentNode.removeChild(oldCurrentNode);
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void cbTruncateChildren()
    {
        int numberChildren = m_currentNode.getNumberChildren();
        if (numberChildren == 0)
            return;
        if (! showQuestion("Truncate children?"))
            return;
        NodeUtils.truncateChildren(m_currentNode);
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void checkComputerMove()
    {
        if (m_commandThread == null || ! isCurrentNodeExecuted())
            return;
        int moveNumber = NodeUtils.getMoveNumber(m_currentNode);
        boolean bothPassed = (moveNumber >= 2 && m_board.bothPassed());
        if (bothPassed)
            m_menuBar.setCleanup(true);
        boolean gameFinished = (bothPassed || m_resigned);
        if (isComputerBoth())
        {
            if (gameFinished)
            {
                if (m_auto)
                {
                    newGame(m_boardSize);
                    checkComputerMove();
                    return;
                }
                m_clock.halt();
                showGameFinished();
                return;
            }
            generateMove(false);            
        }
        else
        {
            if (gameFinished)
            {
                m_clock.halt();
                showGameFinished();
                return;
            }
            else if (computerToMove())
                generateMove(false);
        }
    }

    private boolean checkCurrentNodeExecuted()
    {
        if (m_commandThread == null)
            return true;
        if (! isCurrentNodeExecuted())
        {
            Object[] options = { "Detach Program", "Cancel" };
            Object message =
                "Could not synchronize current\n" +
                "position with Go program";
            int n = JOptionPane.showOptionDialog(this, message, "Error",
                                                 JOptionPane.YES_NO_OPTION,
                                                 JOptionPane.ERROR_MESSAGE,
                                                 null, options, options[1]);
            if (n == 0)
                cbDetachProgram();
            return false;
        }
        return true;
    }

    /** Ask for saving file if it was modified.
        @return true If file was not modified, user chose not to save it
        or file was saved successfully
    */
    private boolean checkSaveGame()
    {
        int result =
            JOptionPane.showConfirmDialog(this, "Save current game?",
                                          "Question",
                                          JOptionPane.YES_NO_CANCEL_OPTION);
        switch (result)
        {
        case 0:
            if (m_loadedFile != null)
                return save(m_loadedFile);
            else
                return saveDialog();
        case 1:
            setNeedsSave(false);
            return true;
        case -1:
        case 2:
            return false;
        default:
            assert(false);
            return true;
        }
    }
    
    private void clearStatus()
    {
        m_statusBar.clear();
    }

    private void clockRestore(Node node, GoColor color)
    {
        Move move = node.getMove();
        if (move == null)
        {
            if (node == m_gameTree.getRoot())
                m_clock.reset();
            return;
        }
        if (move.getColor() != color)
            return;
        double timeLeft = m_currentNode.getTimeLeft(color);
        int movesLeft = m_currentNode.getMovesLeft(color);
        if (! Double.isNaN(timeLeft))
            m_clock.setTimeLeft(color, (long)(timeLeft * 1000), movesLeft);
    }

    private void close()
    {
        if (isCommandInProgress())
            if (! showQuestion("Kill program?"))
                return;
        if (m_setupMode)
            setupDone();
        if (m_needsSave && ! checkSaveGame())
            return;
        saveSession();        
        if (m_commandThread != null)
        {
            m_analyzeCommand = null;
            detachProgram();
        }
        dispose();
        m_prefs.save();
        System.exit(0);
    }

    private void computerBlack()
    {
        m_computerBlack = true;
        m_computerWhite = false;
        m_menuBar.setComputerBlack();
    }

    private void computerBoth()
    {
        m_computerBlack = true;
        m_computerWhite = true;
        m_menuBar.setComputerBoth();
    }

    private void computerMoved()
    {
        if (! endLengthyCommand())
            return;
        if (m_beepAfterMove)
            java.awt.Toolkit.getDefaultToolkit().beep();
        try
        {
            m_clock.stopMove();
            String response = m_commandThread.getResponse();
            GoColor toMove = m_board.getToMove();
            if (response.equalsIgnoreCase("resign"))
            {
                if (! isComputerBoth())
                    showInfo(m_name + " resigns");
                m_resigned = true;
                setResult((toMove == GoColor.BLACK ? "W" : "B") + "+Resign");
            }
            else
            {
                GoPoint point = GtpUtils.parsePoint(response, m_boardSize);
                if (point != null
                    && m_board.getColor(point) != GoColor.EMPTY)
                    showWarning("Program played move on non-empty point");
                Move move = Move.get(point, toMove);
                setNeedsSave(true);
                m_board.play(move);
                Node node = createNode(move);
                m_currentNode = node;
                m_currentNodeExecuted = 1;
                if (point == null && ! isComputerBoth())
                    showInfo(m_name + " passes");
                m_resigned = false;
            }
            m_clock.startMove(m_board.getToMove());
            updateMenuBar();
            boolean doCheckComputerMove
                = (! m_isSingleMove
                   && ! (isComputerBoth() && m_interruptComputerBoth));
            boolean gameTreeChanged = true;
            if (m_currentNode.getFather().getNumberChildren() == 1)
            {
                if (m_gameTreeViewer != null)
                    m_gameTreeViewer.addNewSingleChild(m_currentNode);
                gameTreeChanged = false;
            }
            boardChangedBegin(doCheckComputerMove, gameTreeChanged);
        }
        catch (GtpError e)
        {
            showError(e);
            clearStatus();
        }
    }

    private void computerNone()
    {
        m_computerBlack = false;
        m_computerWhite = false;
        m_menuBar.setComputerNone();
    }

    private boolean computerToMove()
    {
        if (m_board.getToMove() == GoColor.BLACK)
            return m_computerBlack;
        else
            return m_computerWhite;
    }

    private void computerWhite()
    {
        m_computerBlack = false;
        m_computerWhite = true;
        m_menuBar.setComputerWhite();
    }

    private Node createNode(Move move)
    {
        return Utils.createNode(m_currentNode, move, m_clock);
    }

    private ContextMenu createContextMenu(GoPoint point)
    {
        ContextMenu.Listener listener = new ContextMenu.Listener()
            {
                public void clearAnalyze()
                {
                    GoGui.this.clearAnalyzeCommand();
                }

                public void editLabel(GoPoint point)
                {
                    GoGui.this.editLabel(point);
                }

                public void mark(GoPoint point, MarkType type,
                                 boolean mark)
                {
                    GoGui.this.mark(point, type, mark);
                }

                public void setAnalyzeCommand(AnalyzeCommand command)
                {
                    GoGui.this.setAnalyzeCommand(command, false, true, true);
                }
            };
        ArrayList supportedCommands = null;
        boolean noProgram = (m_commandThread == null);
        if (! noProgram)
            supportedCommands = m_commandThread.getSupportedCommands();
        return new ContextMenu(point, noProgram, supportedCommands,
                               m_programAnalyzeCommands,
                               m_guiBoard.getMark(point),
                               m_guiBoard.getMarkCircle(point),
                               m_guiBoard.getMarkSquare(point),
                               m_guiBoard.getMarkTriangle(point),
                               listener);
    }

    private void createThumbnail(File file)
    {
        if (! Thumbnail.checkThumbnailSupport())
            return;
        // Thumbnail creation does not work on GNU classpath 0.90 yet
        if (Platform.isGnuClasspath())
            return;
        String path = file.getAbsolutePath();
        if (! path.startsWith("/tmp") && ! path.startsWith("/var/tmp"))
            m_thumbnail.create(file);
    }

    private void detachProgram()
    {
        if (isCommandInProgress())
        {
            m_commandThread.destroyGtp();
            m_commandThread.close();
        }
        else
        {
            if (m_commandThread != null && ! m_commandThread.isProgramDead())
            {
                // Some programs do not handle closing the GTP stream
                // correctly, so we send a quit before
                try
                {
                    if (m_commandThread.isCommandSupported("quit"))
                        m_commandThread.send("quit");
                }
                catch (GtpError e)
                {
                }
                m_commandThread.close();
            }
        }
        saveSession();
        if (m_analyzeCommand != null)
            clearAnalyzeCommand();
        m_commandThread = null;
        m_name = null;
        m_version = null;
        m_toolBar.setComputerEnabled(false);
        m_menuBar.setComputerEnabled(false);
        m_gtpShell.dispose();
        m_gtpShell = null;
        if (m_analyzeDialog != null)
        {
            m_analyzeDialog.saveRecent();
            m_analyzeDialog.dispose();
            m_analyzeDialog = null;
        }
        resetBoard();
        clearStatus();
        setTitle();
        // If this node was only partially executed due to a error of the Go
        // program, we undo and execute it again
        if (! isCurrentNodeExecuted())
        {
            try
            {
                undoCurrentNode();
                executeCurrentNode();
                updateFromGoBoard();
            }
            catch (GtpError e)
            {
                assert(false);
            }
        }
    }

    private void editLabel(GoPoint point)
    {
        String value = m_currentNode.getLabel(point);
        value = JOptionPane.showInputDialog(this, "Label " + point, value);
        if (value == null)
            return;
        m_currentNode.setLabel(point, value);
        m_guiBoard.setLabel(point, value);
        setNeedsSave(true);
        updateBoard();
    }

    private boolean endLengthyCommand()
    {
        clearStatus();
        m_menuBar.setNormalMode();
        m_toolBar.enableAll(true, m_currentNode);
        if (m_gtpShell != null)
            m_gtpShell.setCommandInProgess(false);
        if (m_analyzeCommand != null
            && (m_analyzeCommand.needsPointArg()
                || m_analyzeCommand.needsPointListArg()))
            setBoardCursor(Cursor.HAND_CURSOR);
        else
            setBoardCursorDefault();
        // Program could have been killed in cbInterrupt
        if (m_commandThread == null)
            return false;
        GtpError error = m_commandThread.getException();
        if (error != null)
        {
            showError(error);
            return false;
        }
        return true;
    }

    private void executeCurrentNode() throws GtpError
    {
        m_currentNodeExecuted = 0;
        ArrayList moves = NodeUtils.getAllAsMoves(m_currentNode);
        if (moves.size() > 1 && m_commandThread != null
            && m_commandThread.isCommandSupported("play_sequence"))
        {
            String cmd = GtpUtils.getPlaySequenceCommand(moves);
            m_commandThread.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_board.play((Move)moves.get(i));
            m_currentNodeExecuted = moves.size();
        }
        else
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                Move move = (Move)moves.get(i);
                if (m_commandThread != null)
                    m_commandThread.sendPlay(move);
                m_board.play(move);
                ++m_currentNodeExecuted;
            }
        }
        GoColor toMove = m_currentNode.getToMove();
        if (toMove != GoColor.EMPTY)
            m_board.setToMove(toMove);
    }

    private boolean executeRoot()
    {
        m_currentNode = m_gameTree.getRoot();
        m_currentNodeExecuted = 0;
        m_isRootExecuted = true;
        if (m_commandThread != null)
        {
            try
            {
                m_commandThread.sendBoardsize(m_boardSize);
                m_commandThread.sendClearBoard(m_boardSize);
            }
            catch (GtpError error)
            {
                showError(error);
                m_isRootExecuted = false;
                return false;
            }
        }
        GameInformation gameInformation = m_gameTree.getGameInformation();
        setKomi(gameInformation.m_komi);
        setRules();
        setTimeSettings();
        try
        {
            executeCurrentNode();
        }
        catch (GtpError error)
        {
            showError(error);
            return false;
        }
        return true;
    }

    private void fileInvalid()
    {
        if (m_loadedFile == null)
            return;
        m_loadedFile = null;
        setTitle();
    }

    private void forward(int n)
    {
        assert(n >= 0);
        if (! checkCurrentNodeExecuted())
            return;
        Node node = m_currentNode;
        for (int i = 0; i < n; ++i)
        {
            Node child = node.getChild();
            if (child == null)
                break;
            node = child;
        }
        gotoNode(node);
    }

    private void generateMove(boolean isSingleMove)
    {
        showStatus(m_name + " is thinking...");
        GoColor toMove = m_board.getToMove();
        String command;
        if (m_menuBar.getCleanup()
            && (m_commandThread.isCommandSupported("kgs-genmove_cleanup")
                || m_commandThread.isCommandSupported("genmove_cleanup")))
        {
            if (m_commandThread.isCommandSupported("genmove_cleanup"))
                command = "genmove_cleanup";
            else
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
            command = m_commandThread.getCommandGenmove(toMove);
            m_clock.startMove(toMove);
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

    private File getGoGuiFile(String name)
    {
        String home = System.getProperty("user.home", "");
        File dir = new File(home, ".gogui");
        if (! dir.exists())
            dir.mkdir();
        return new File(dir, name);
    }

    private int getRules()
    {
        int result = Board.RULES_UNKNOWN;
        String rules = m_gameTree.getGameInformation().m_rules;
        if (rules != null)
        {
            rules = rules.trim().toLowerCase();
            if (rules.equals("japanese"))
                result = Board.RULES_JAPANESE;
            else if (rules.equals("chinese"))
                result = Board.RULES_CHINESE;
        }
        return result;
    }

    private void gotoNode(Node node)
    {
        // GameTreeViewer is not disabled in score mode
        if (m_scoreMode)
            return;
        ArrayList nodes = new ArrayList();
        int numberUndo
            = NodeUtils.getShortestPath(m_currentNode, node, nodes);
        if (backward(numberUndo))
        {
            ArrayList moves = NodeUtils.getAllAsMoves(nodes);
            if (checkCurrentNodeExecuted()
                && moves.size() > 1
                && m_commandThread != null
                && m_commandThread.isCommandSupported("play_sequence"))
            {
                try
                {
                    String cmd = GtpUtils.getPlaySequenceCommand(moves);
                    m_commandThread.send(cmd);
                    for (int i = 0; i < moves.size(); ++i)
                        m_board.play((Move)moves.get(i));
                    m_currentNode = (Node)nodes.get(nodes.size() - 1);
                    m_currentNodeExecuted =
                        NodeUtils.getAllAsMoves(m_currentNode).size();
                }
                catch (GtpError e)
                {
                    showError(e);
                }
            }
            else
                for (int i = 0; i < nodes.size(); ++i)
                {
                    Node nextNode = (Node)nodes.get(i);
                    if (! checkCurrentNodeExecuted())
                        break;
                    assert(nextNode.isChildOf(m_currentNode));
                    m_currentNode = nextNode;
                    try
                    {
                        executeCurrentNode();
                    }
                    catch (GtpError e)
                    {
                        showError(e);
                        break;
                    }
                    m_gameInfo.fastUpdateMoveNumber(m_currentNode);
                }
        }
    }

    private void humanMoved(Move move)
    {
        try
        {
            GoPoint point = move.getPoint();
            if (point != null && m_board.getColor(point) != GoColor.EMPTY)
                return;
            m_clock.stopMove();
            if (point != null)
            {
                // Paint point immediately to pretend better responsiveness
                // because updating game tree or response to GTP play command
                // can be slow
                m_guiBoard.setColor(point, move.getColor());
                if (m_showLastMove)
                    m_guiBoard.markLastMove(point);
                m_guiBoard.paintImmediately(point);
            }
            boolean newNodeCreated = play(move);
            if (newNodeCreated)
                m_clock.startMove(m_board.getToMove());
            setNeedsSave(newNodeCreated);
            GoColor color = move.getColor();
            if (NodeUtils.getMoveNumber(m_currentNode) > 0
                && m_clock.lostOnTime(color)
                && ! m_lostOnTimeShown)
            {
                showInfo(color.toString() + " lost on time.");
                m_lostOnTimeShown = true;
            }
            m_resigned = false;
            boolean gameTreeChanged = newNodeCreated;
            if (newNodeCreated
                && m_currentNode.getFather().getNumberChildren() == 1)
            {
                if (m_gameTreeViewer != null)
                    m_gameTreeViewer.addNewSingleChild(m_currentNode);
                gameTreeChanged = false;
            }
            boardChangedBegin(true, gameTreeChanged);
        }
        catch (GtpError e)
        {
            updateFromGoBoard();
            showError(e);
        }
    }

    private void initGame(int size)
    {
        if (size != m_boardSize)
        {
            m_boardSize = size;
            m_board.initSize(size);
            m_guiBoard.initSize(size);
            m_guiBoard.setShowGrid(m_menuBar.getShowGrid());
            restoreMainWindow();
            if (m_gtpShell != null)
                restoreSize(m_gtpShell, "window-gtpshell");
            if (m_analyzeDialog != null)
            {
                restoreSize(m_analyzeDialog, "window-analyze");
                m_analyzeDialog.setBoardSize(size);
            }
            if (m_gameTreeViewer != null)
                restoreSize(m_gameTreeViewer, "window-gametree");
        }
        ArrayList handicap = m_board.getHandicapStones(m_handicap);
        if (handicap == null)
            showWarning("Handicap stone locations not\n" +
                        "defined for this board size");
        m_gameTree = new GameTree(size, m_prefs.getDouble("komi"), handicap,
                                  m_prefs.getString("rules"), m_timeSettings);
        m_board.newGame();        
        m_currentNode = m_gameTree.getRoot();
        m_currentNodeExecuted = 0;
        updateFromGoBoard();
        resetBoard();
        m_clock.reset();
        m_lostOnTimeShown = false;
        setNeedsSave(false);
        m_resigned = false;
        m_menuBar.enableFindNext(false);
    }

    private void initialize()
    {
        if (m_file == null)
            newGame(m_boardSize);
        else
            newGameFile(m_boardSize, m_move);
        if (! m_prefs.getBool("show-info-panel"))
        {
            m_menuBar.setShowInfoPanel(false);
            showInfoPanel();
        }
        if (m_prefs.getBool("show-toolbar"))
        {
            m_menuBar.setShowToolbar(true);
            showToolbar();
        }
        restoreMainWindow();
        // Attaching a program can take some time, so we want to make
        // the window visible, but not draw the window content yet
        getLayeredPane().setVisible(false);
        setVisible(true);
        m_bookmarks = Bookmark.load(getGoGuiFile("bookmarks"));
        m_menuBar.setBookmarks(m_bookmarks);
        m_toolBar.enableAll(true, m_currentNode);
        if (m_program != null)
            attachProgram(m_program);
        setTitle();
        if (m_commandThread == null
            || (! m_computerBlack && ! m_computerWhite))
            computerNone();
        else if (isComputerBoth())
            computerBoth();
        else  if (m_computerBlack)
            computerBlack();
        else
            computerWhite();
        updateGameInfo(true);
        registerSpecialMacHandler();
        // Children dialogs should be set visible after main window, otherwise
        // they get minimize window buttons and a taskbar entry (KDE 3.4)
        if (m_gtpShell != null && m_prefs.getBool("show-gtpshell"))
        {
            m_menuBar.setShowShell(true);
            cbShowShell();
        }
        if (m_prefs.getBool("show-gametree"))
        {
            m_menuBar.setShowTree(true);
            cbShowTree();
        }
        if (m_prefs.getBool("show-analyze"))
        {
            m_menuBar.setShowAnalyze(true);
            cbAnalyze();
        }
        if (! m_initAnalyze.equals(""))
        {
            AnalyzeCommand analyzeCommand =
                AnalyzeCommand.get(this, m_initAnalyze);
            if (analyzeCommand == null)
                showError("Unknown analyze command \"" + m_initAnalyze
                          + "\"");
            else
                initAnalyzeCommand(analyzeCommand, true);
        }
        setTitleFromProgram();
        getLayeredPane().setVisible(true);
        toFrontLater();
        checkComputerMove();
    }

    private void initProgramAnalyzeCommands()
    {
        m_programAnalyzeCommands = null;
        if (m_commandThread.isCommandSupported("gogui_analyze_commands"))
        {
            try
            {
                m_programAnalyzeCommands
                    = m_commandThread.send("gogui_analyze_commands");
            }
            catch (GtpError e)
            {
            }    
        }
    }

    private void initScore(GoPoint[] isDeadStone)
    {
        resetBoard();
        GuiBoardUtils.scoreBegin(m_guiBoard, m_board, isDeadStone);
        m_scoreMode = true;
        if (m_scoreDialog == null)
            m_scoreDialog = new ScoreDialog(this, this);
        double komi = m_gameTree.getGameInformation().m_komi;
        m_scoreDialog.showScore(m_board.scoreGet(komi, getRules()));
        m_scoreDialog.setVisible(true);
        m_menuBar.setScoreMode();
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

    private boolean isCommandInProgress()
    {
        if (m_commandThread == null)
            return false;
        return m_commandThread.isCommandInProgress();
    }
    
    private boolean isCurrentNodeExecuted()
    {
        int numberAddStonesAndMoves
            = NodeUtils.getAllAsMoves(m_currentNode).size();
        if (! m_isRootExecuted
            || m_currentNodeExecuted != numberAddStonesAndMoves)
            return false;
        return true;
    }

    private boolean loadFile(File file, int move)
    {
        try
        {
            FileInputStream in = new FileInputStream(file);
            LoadFileRunnable runnable = new LoadFileRunnable(in, file);
            if (file.length() > 500000)
            {
                newGame(m_boardSize); // Frees space if already large tree
                GuiUtils.runProgress(this, "Loading...", runnable);
            }
            else
                runnable.run(null);
            SgfReader reader = runnable.getReader();
            GameInformation gameInformation =
                reader.getGameTree().getGameInformation();
            initGame(gameInformation.m_boardSize);
            m_menuBar.addRecent(file);
            m_gameTree = reader.getGameTree();
            if (executeRoot())
                if (move > 0)
                    forward(move);            
            m_loadedFile = file;
            setTitle();
            String warnings = reader.getWarnings();
            if (warnings != null)
                showWarning("File " + file.getName() + ":\n" + warnings);
            SimpleDialogs.setLastFile(file);
            computerNone();
            createThumbnail(file);
            boardChangedBegin(false, true);
        }
        catch (FileNotFoundException e)
        {
            showError("File not found:\n" + file);
            return false;
        }
        catch (SgfReader.SgfError e)
        {
            showError("Could not read file:", e);
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

    public void mark(GoPoint point, MarkType type, boolean mark)
    {
        if (mark)
            m_currentNode.addMarked(point, type);
        else
            m_currentNode.removeMarked(point, type);
        if (type == MarkType.MARK)
            m_guiBoard.setMark(point, mark);
        else if (type == MarkType.CIRCLE)
            m_guiBoard.setMarkCircle(point, mark);
        else if (type == MarkType.SQUARE)
            m_guiBoard.setMarkSquare(point, mark);
        else if (type == MarkType.TRIANGLE)
            m_guiBoard.setMarkTriangle(point, mark);        
        setNeedsSave(true);
        updateBoard();
    }

    private void newGame(int size)
    {
        initGame(size);
        executeRoot();
        updateGameInfo(true);
        updateFromGoBoard();
        m_toolBar.update(m_currentNode);
        updateMenuBar();
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        setTitle();
        setTitleFromProgram();
        showToMove();
    }

    private void newGameFile(int size, int move)
    {
        initGame(size);
        loadFile(m_file, move);
        m_clock.reset();
        updateGameInfo(true);
        updateFromGoBoard();
        m_toolBar.update(m_currentNode);
        updateMenuBar();
        m_menuBar.selectBoardSizeItem(m_board.getSize());
    }

    /** @return true, if new node was created. */
    private boolean play(Move move) throws GtpError
    {
        if (! checkCurrentNodeExecuted())
            return false;
        boolean result = false;
        Node node = NodeUtils.getChildWithMove(m_currentNode, move);
        if (node == null)
        {
            result = true;
            node = createNode(move);
        }
        m_currentNode = node;
        try
        {
            executeCurrentNode();
        }
        catch (GtpError error)
        {
            m_currentNode = node.getFather();
            m_currentNode.removeChild(node);
            m_currentNodeExecuted
                = NodeUtils.getAllAsMoves(m_currentNode).size();
            throw error;
        }
        return result;
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
                    cbAbout();
                    return true;
                }
                
                public boolean handleOpenFile(String filename)
                {
                    assert(SwingUtilities.isEventDispatchThread());
                    if (m_needsSave && ! checkSaveGame())
                        return true;
                    loadFile(new File(filename), -1);
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
        updateBoard();
    }
    
    private void restoreMainWindow()
    {
        setState(Frame.NORMAL);
        Session.restoreLocation(this, m_prefs, "window-gogui", m_boardSize);
        Dimension preferredCommentSize = null;
        int fieldSize = -1;
        try
        {
            String name = "fieldsize-" + m_boardSize;
            if (m_prefs.contains(name))
            {
                String value = m_prefs.getString(name);
                fieldSize = Integer.parseInt(value);
                m_guiBoard.setPreferredFieldSize(new Dimension(fieldSize,
                                                               fieldSize));
            }
            name = "commentsize-" + m_boardSize;
            if (m_prefs.contains(name))
            {
                String[] args
                    = StringUtils.splitArguments(m_prefs.getString(name));
                int width = Integer.parseInt(args[0]);                
                int height = Integer.parseInt(args[1]);
                preferredCommentSize = new Dimension(width, height);
                m_comment.setPreferredSize(preferredCommentSize);
            }
        }
        catch (NumberFormatException e)
        {
        }
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

    private void restoreSize(Window window, String name)
    {
        Session.restoreSize(window, m_prefs, name, m_boardSize);
    }

    private void runLengthyCommand(String cmd, Runnable callback)
    {
        assert(m_commandThread != null);
        beginLengthyCommand();
        m_commandThread.send(cmd, callback);
    }

    /** Save game to file.
        @return true If successfully saved.
        @bug TexWriter and SgfWriter do not return an error if saving fails.
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
        new SgfWriter(out, m_gameTree, "GoGui", Version.get());
        m_menuBar.addRecent(file);
        createThumbnail(file);
        return true;
    }

    private boolean saveDialog()
    {
        File file = SimpleDialogs.showSaveSgf(this);
        if (file == null)
            return false;
        if (! save(file))
            return false;
        m_loadedFile = file;
        setTitle();
        setNeedsSave(false);
        return true;
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        new SgfWriter(out, m_board, "GoGui", Version.get());
        m_menuBar.addRecent(file);
    }

    private void saveSession()
    {
        Bookmark.save(m_bookmarks, getGoGuiFile("bookmarks"));
        if (m_gtpShell != null)
            m_gtpShell.saveHistory();
        if (m_analyzeDialog != null)
            m_analyzeDialog.saveRecent();
        Session.saveLocation(this, m_prefs, "window-gogui", m_boardSize);
        if (m_help != null)
            saveSize(m_help, "window-help");
        saveSizeAndVisible(m_gameTreeViewer, "gametree");
        if (m_commandThread != null)
        {
            saveSizeAndVisible(m_gtpShell, "gtpshell");
            saveSizeAndVisible(m_analyzeDialog, "analyze");
        }
        if (GuiUtils.isNormalSizeMode(this))
        {            
            String name = "fieldsize-" + m_boardSize;
            m_prefs.setInt(name, m_guiBoard.getFieldSize().width);
            name = "commentsize-" + m_boardSize;
            String value = Integer.toString(m_comment.getWidth()) + " "
                + Integer.toString(m_comment.getHeight());
            m_prefs.setString(name, value);
        }
    }

    private void saveSize(JDialog dialog, String name)
    {
        Session.saveSize(dialog, m_prefs, name, m_boardSize);
    }

    private void saveSizeAndVisible(JDialog dialog, String name)
    {
        Session.saveSizeAndVisible(dialog, m_prefs, name, m_boardSize);
    }

    private void sendGtp(Reader reader)
    {
        if (m_commandThread == null)
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
                    if (! m_gtpShell.send(line, this, true))
                        break;
                }
                catch (IOException e)
                {
                    showError("Error reading file");
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
            showError("File not found: " + e.getMessage());
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
        setCursor(m_infoPanel, type);
    }

    private void setBoardCursorDefault()
    {
        setCursorDefault(m_guiBoard);
        setCursorDefault(m_infoPanel);
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

    private void setKomi(double komi)
    {
        Utils.sendKomi(this, komi, m_name, m_commandThread);
    }

    private void setNeedsSave(boolean needsSave)
    {
        if (m_needsSave == needsSave)
            return;
        m_needsSave = needsSave;
        // Set Swing property on root window, good for e.g. Mac close
        // buttons (See Mac QA1146)
        getRootPane().putClientProperty("windowModified",
                                        Boolean.valueOf(needsSave));
        setTitle();
    }
    
    private void setResult(String result)
    {
        String oldResult = m_gameTree.getGameInformation().m_result;
        if (! (oldResult == null || oldResult.equals("")
               || oldResult.equals(result))
            && ! showQuestion("Overwrite old result " + oldResult + "\n" +
                              "with " + result + "?"))
            return;
        m_gameTree.getGameInformation().m_result = result;
    }

    private void setRules()
    {
        Utils.sendRules(getRules(), m_commandThread);
    }

    private void setTimeSettings()
    {
        if (m_commandThread == null)
            return;
        TimeSettings timeSettings =
            m_gameTree.getGameInformation().m_timeSettings;
        if (timeSettings == null)
            return;
        if (! m_commandThread.isCommandSupported("time_settings"))
            return;
        m_clock.setTimeSettings(timeSettings);
        String command = GtpUtils.getTimeSettingsCommand(timeSettings);
        try
        {
            m_commandThread.send(command);
        }
        catch (GtpError e)
        {
            showError(e);
        }
    }

    private void setTitle()
    {
        if (m_titleFromProgram != null)
        {
            setTitle(m_titleFromProgram);
            return;
        }
        String appName = "GoGui";        
        if (m_commandThread != null)
            appName = m_name;
        String filename = null;
        if (m_loadedFile != null)
        {
            filename = m_loadedFile.getName();
            if (m_needsSave)
                filename = filename + " [modified]";
        }
        String gameName = m_gameTree.getGameInformation().suggestGameName();
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
            setTitle(gameName + " - " + appName);
    }

    private void setTitleFromProgram()
    {
        m_titleFromProgram = null;
        if (m_commandThread == null)
            return;
        if (m_commandThread.isCommandSupported("gogui_title"))
        {
            try
            {
                m_titleFromProgram = m_commandThread.send("gogui_title");
                setTitle(m_titleFromProgram);
            }
            catch (GtpError e)
            {
            }
        }
    }

    private void setupDone()
    {
        m_setupMode = false;
        m_showLastMove = m_menuBar.getShowLastMove();
        m_menuBar.setNormalMode();
        m_toolBar.enableAll(true, m_currentNode);
        int size = m_board.getSize();
        GoColor color[][] = new GoColor[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            color[p.getX()][p.getY()] = m_board.getColor(p);
        }
        GoColor toMove = m_board.getToMove();
        m_boardSize = size;
        m_board.newGame();        
        m_gameTree = new GameTree(size, m_prefs.getDouble("komi"), null,
                                  m_prefs.getString("rules"), null);
        m_currentNode = m_gameTree.getRoot();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint point = m_board.getPoint(i);
            int x = point.getX();
            int y = point.getY();
            GoColor c = color[x][y];
            if (c == GoColor.BLACK)
                m_currentNode.addBlack(point);
            else if (c == GoColor.WHITE)
                m_currentNode.addWhite(point);
        }
        m_currentNode.setPlayer(toMove);
        executeRoot();
        fileInvalid();
        updateGameInfo(true);
        boardChangedBegin(false, false);
    }

    private void showAnalyzeTextOutput(int type, GoPoint pointArg,
                                       String title, String response)
    {
        boolean highlight = (type == AnalyzeCommand.HSTRING
                             || type == AnalyzeCommand.HPSTRING);
        TextViewer.Listener listener = null;
        if (type == AnalyzeCommand.PSTRING || type == AnalyzeCommand.HPSTRING)
        {
            listener = new TextViewer.Listener()
                {
                    public void textSelected(String text)
                    {
                        GoPoint list[] =
                            GtpUtils.parsePointString(text, m_boardSize);
                        GuiBoardUtils.showPointList(m_guiBoard, list);
                    }
                };
        }
        TextViewer textViewer = new TextViewer(this, title, response,
                                               highlight, listener,
                                               m_fastPaint);
        if (pointArg != null)
        {
            Point location = m_guiBoard.getLocationOnScreen(pointArg);
            textViewer.setLocation(location);
        }
        else
            textViewer.setLocationRelativeTo(this);
        textViewer.setVisible(true);
    }

    private void showError(String message, Exception e)
    {
        SimpleDialogs.showError(this, message, e);
    }

    private void showError(GtpError error)
    {        
        Utils.showError(this, m_name, error);
    }

    private void showError(String message)
    {
        SimpleDialogs.showError(this, message);
    }

    private void showGameFinished()
    {
        if (m_gameFinishedMessage == null)
            m_gameFinishedMessage = new OptionalWarning(this);
        m_gameFinishedMessage.show("Game finished",
                                   JOptionPane.INFORMATION_MESSAGE);
    }

    private void showInfo(String message)
    {
        SimpleDialogs.showInfo(this, message);
    }

    private void showInfoPanel()
    {
        boolean showInfoPanel = m_menuBar.getShowInfoPanel();
        if (showInfoPanel == m_showInfoPanel)
            return;
        m_prefs.setBool("show-info-panel", showInfoPanel);
        m_showInfoPanel = showInfoPanel;
        if (showInfoPanel)
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

    private boolean showQuestion(String message)
    {
        return SimpleDialogs.showQuestion(this, message);
    }

    private void showStatus(String text)
    {
        m_statusBar.setText(text);
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

    private void showToolbar()
    {
        boolean showToolbar = m_menuBar.getShowToolbar();
        if (showToolbar == m_showToolbar)
            return;
        m_prefs.setBool("show-toolbar", showToolbar);
        m_showToolbar = showToolbar;
        if (showToolbar)
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
        pack();
    }

    private void showToMove()
    {
        if (m_showInfoPanel)
        {
            clearStatus();
            return;
        }
        GoColor toMove = m_board.getToMove();
        if (toMove == GoColor.WHITE)
            showStatus("White to play");
        else if (toMove == GoColor.BLACK)
            showStatus("Black to play");
    }

    private void showWarning(String message)
    {
        SimpleDialogs.showWarning(this, message);
    }

    private void toFrontLater()
    {
        // Calling toFront() directly does not give the focus to this
        // frame, if dialogs are open
        SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    toFront();
                }
            });
    }

    private void undoCurrentNode() throws GtpError
    {
        if (m_commandThread != null
            && ! m_commandThread.isCommandSupported("undo"))
            throw new GtpError("Program does not support undo");
        for ( ; m_currentNodeExecuted > 0; --m_currentNodeExecuted)
        {
            if (m_commandThread != null)
                m_commandThread.send("undo");
            m_board.undo();
        }
        updateFromGoBoard();
    }

    private void updateBoard()
    {
        if (m_showVariations)
        {
            ArrayList childrenMoves
                = NodeUtils.getChildrenMoves(m_currentNode);
            GuiBoardUtils.showChildrenMoves(m_guiBoard, childrenMoves);
        }
        GuiBoardUtils.showMarkup(m_guiBoard, m_currentNode);
    }

    private void updateFromGoBoard()
    {
        GuiBoardUtils.updateFromGoBoard(m_guiBoard, m_board, m_showLastMove);
        if (m_currentNode.getMove() == null)
            m_guiBoard.markLastMove(null);
    }

    private void updateGameInfo(boolean gameTreeChanged)
    {
        m_gameInfo.update(m_currentNode, m_board);
        updateGameTree(gameTreeChanged);
        m_comment.setNode(m_currentNode);
        updateBoard();
        if (m_analyzeDialog != null)
            m_analyzeDialog.setSelectedColor(m_board.getToMove());
    }

    private void updateGameTree(boolean gameTreeChanged)
    {
        if (m_gameTreeViewer == null)
            return;
        if (! gameTreeChanged)
        {
            m_gameTreeViewer.update(m_currentNode);
            return;
        }
        m_gameTreeViewer.update(m_gameTree, m_currentNode);
    }

    private void updateMenuBar()
    {
        m_menuBar.update(m_gameTree, m_currentNode, m_clock);
    }
}

//----------------------------------------------------------------------------
