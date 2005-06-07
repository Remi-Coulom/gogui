//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import game.GameInformation;
import game.GameTree;
import game.Node;
import game.NodeUtils;
import game.TimeSettings;
import go.Move;
import gtp.Gtp;
import gtp.GtpError;
import gtp.GtpUtils;
import gui.AnalyzeCommand;
import gui.AnalyzeDialog;
import gui.AnalyzeShow;
import gui.BoardSizeDialog;
import gui.Clock;
import gui.Comment;
import gui.CommandThread;
import gui.FindDialog;
import gui.GameInfo;
import gui.GameInfoDialog;
import gui.GameTreeViewer;
import gui.GtpShell;
import gui.GuiUtils;
import gui.Help;
import gui.ParameterDialog;
import gui.SelectProgram;
import gui.Session;
import gui.ScoreDialog;
import gui.SimpleDialogs;
import gui.TextViewer;
import utils.FileUtils;
import utils.Platform;
import utils.Preferences;
import utils.ProgressShow;
import utils.SquareLayout;
import utils.StringUtils;
import version.Version;

//----------------------------------------------------------------------------

/** Graphical user interface to a Go program. */
class GoGui
    extends JFrame
    implements ActionListener, AnalyzeDialog.Callback, gui.Board.Listener,
               GameTreeViewer.Listener, GtpShell.Callback
{
    GoGui(String program, Preferences prefs, String file, int move,
          String time, boolean verbose, boolean computerBlack,
          boolean computerWhite, boolean auto, String gtpFile,
          String gtpCommand, String initAnalyze, boolean fastPaint)
        throws GtpError
    {
        m_fastPaint = fastPaint;
        m_prefs = prefs;
        setPrefsDefaults(m_prefs);
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
        m_showToolbar = true;

        Container contentPane = getContentPane();        
        m_innerPanel = new JPanel(new BorderLayout());
        contentPane.add(m_innerPanel, BorderLayout.CENTER);
        m_toolBar = new ToolBar(this);
        contentPane.add(m_toolBar, BorderLayout.NORTH);

        m_infoPanel = new JPanel(new BorderLayout());
        m_clock = new Clock();
        m_gameInfo = new GameInfo(m_clock);
        m_gameInfo.setBorder(GuiUtils.createSmallEmptyBorder());
        m_infoPanel.add(m_gameInfo, BorderLayout.NORTH);

        m_board = new go.Board(m_boardSize);

        m_guiBoard = new gui.Board(m_board, fastPaint);
        m_guiBoard.setListener(this);
        m_innerPanel.add(createStatusBar(), BorderLayout.SOUTH);

        m_squareLayout = new SquareLayout();
        m_squareLayout.setPreferMultipleOf(m_boardSize + 2);
        m_boardPanel = new JPanel(m_squareLayout);
        m_boardPanel.add(m_guiBoard);

        Comment.Listener commentListener = new Comment.Listener()
            {
                public void changed()
                {
                    cbCommentChanged();
                }

                public void textSelected(String text)
                {
                    go.Point list[] =
                        GtpUtils.parsePointString(text, m_boardSize);
                    m_guiBoard.showPointList(list);
                    m_guiBoard.repaint();
                }
            };
        m_comment = new Comment(commentListener);
        m_infoPanel.add(m_comment, BorderLayout.CENTER);
        m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     m_boardPanel, m_infoPanel);
        int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        InputMap splitPaneInputMap = m_splitPane.getInputMap(condition);
        // According to the docs, null should remove the action, but it does
        // not seem to work with Sun Java 1.4.2, new Object() works
        splitPaneInputMap.put(KeyStroke.getKeyStroke("F8"), new Object());
        m_splitPane.setResizeWeight(0.85);
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

        m_menuBar = new gogui.MenuBar(this);
        m_menuBar.selectBoardSizeItem(m_boardSize);
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
        setTitle("GoGui");
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
            && ! command.equals("computer-black")
            && ! command.equals("computer-both")
            && ! command.equals("computer-none")
            && ! command.equals("computer-white")
            && ! command.equals("detach-program")
            && ! command.equals("gtp-shell")
            && ! command.equals("help")
            && ! command.equals("interrupt")
            && ! command.equals("show-grid")
            && ! command.equals("show-toolbar")
            && ! command.equals("show-info-panel")
            && ! command.equals("show-last-move")
            && ! command.equals("exit"))
            return;
        if (command.equals("about"))
            cbAbout();
        else if (command.equals("analyze"))
            cbAnalyze();
        else if (command.equals("attach-program"))
            cbAttachProgram();
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
        else if (command.equals("clock-halt"))
            cbClockHalt();
        else if (command.equals("clock-resume"))
            cbClockResume();
        else if (command.equals("clock-restore"))
            cbClockRestore();
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
        else if (command.equals("gtp-shell"))
            cbGtpShell();
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
        else if (command.equals("open-recent"))
            cbOpenRecent();
        else if (command.equals("pass"))
            cbPass();
        else if (command.equals("play"))
            cbPlay();
        else if (command.equals("previous-variation"))
            cbPreviousVariation();
        else if (command.equals("previous-earlier-variation"))
            cbPreviousEarlierVariation();
        else if (command.equals("print"))
            cbPrint();
        else if (command.equals("save"))
            cbSave();
        else if (command.equals("save-position"))
            cbSavePosition();
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
        else if (command.equals("show-gametree"))
            cbShowGameTree();
        else if (command.equals("show-info-panel"))
            cbShowInfoPanel();
        else if (command.equals("show-last-move"))
            cbShowLastMove();
        else if (command.equals("show-toolbar"))
            cbShowToolbar();
        else if (command.equals("show-variations"))
            cbShowVariations();
        else if (command.equals("truncate"))
            cbTruncate();
        else
            assert(false);
    }
    
    public void cbAnalyze()
    {        
        if (m_commandThread == null)
            return;
        if (m_analyzeDialog == null)
        {
            m_analyzeDialog =
                new AnalyzeDialog(this, this, m_prefs,
                                  m_commandThread.getSupportedCommands(),
                                  m_commandThread);
            restoreSize(m_analyzeDialog, "window-analyze");
            setTitle();
        }
        m_analyzeDialog.toTop();
    }

    public void cbAttachProgram()
    {        
        if (m_commandThread != null)
            if (! cbDetachProgram())
                return;
        String program = SelectProgram.select(this);
        if (program == null)
            return;
        if (! attachProgram(program))
        {
            m_prefs.setString("program", "");
            return;
        }
        m_prefs.setString("program", m_program);
        if (m_gtpShell != null && m_prefs.getBool("show-gtpshell"))
            m_gtpShell.toTop();
        if (m_prefs.getBool("show-analyze"))
            cbAnalyze();
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
        else if (! showQuestion("Detach " + m_name + "?"))
            return false;
        detachProgram();
        m_prefs.setString("program", "");
        return true;
    }

    public void cbGtpShell()
    {
        if (m_gtpShell != null)
            m_gtpShell.toTop();
    }

    public void cbShowGameTree()
    {
        if (m_gameTreeViewer == null)
        {
            m_gameTreeViewer =
                new GameTreeViewer(this, this, m_fastPaint, m_prefs);
            restoreSize(m_gameTreeViewer, "window-gametree");
        }
        updateGameTree(true);
        if (m_gameTreeViewer != null) // updateGameTree can close viewer
            m_gameTreeViewer.toTop();
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

    public void clearAnalyzeCommand()
    {
        if (m_analyzeCommand == null)
            return;
        if (isCommandInProgress())
        {
            showError("Cannot clear analyze command\n" +
                      "while command in progress.");
            return;
        }
        if (m_setupMode)
        {
            showError("Cannot clear analyze command\n" +
                      "in setup mode.");
            return;
        }
        m_analyzeCommand = null;
        setBoardCursorDefault();
        resetBoard();
        clearStatus();
    }

    public void disposeGameTree()
    {
        if (m_gameTreeViewer == null)
            return;
        m_gameTreeViewer.dispose();
        m_gameTreeViewer = null;
    }

    public void fieldClicked(go.Point p, boolean modifiedSelect)
    {
        if (isCommandInProgress())
            return;
        if (m_setupMode)
        {
            go.Color color;
            if (modifiedSelect)
                color = m_setupColor.otherColor();
            else if (m_board.getColor(p) == m_setupColor)
                color = go.Color.EMPTY;
            else
                color = m_setupColor;
            m_board.play(new Move(p, color));
            m_board.setToMove(m_setupColor);
            updateGameInfo(true);
            m_guiBoard.updateFromGoBoard();
            // Paint point immediately to pretend better responsiveness
            m_guiBoard.paintImmediately(p);
            m_guiBoard.repaint();
            setNeedsSave(true);
        }
        else if (m_analyzeCommand != null && m_analyzeCommand.needsPointArg()
                 && ! modifiedSelect)
        {
            m_analyzeCommand.setPointArg(p);
            m_guiBoard.clearAllSelect();
            m_guiBoard.setSelect(p, true);
            m_guiBoard.repaint();
            analyzeBegin(false, false);
            return;
        }
        else if (m_analyzeCommand != null
                 && m_analyzeCommand.needsPointListArg())
        {
            Vector pointListArg = m_analyzeCommand.getPointListArg();
            if (pointListArg.contains(p))
            {
                pointListArg.remove(p);
                if (modifiedSelect)
                    pointListArg.add(p);
            }
            else
                pointListArg.add(p);
            m_guiBoard.clearAllSelect();
            for (int i = 0; i < pointListArg.size(); ++i)
                m_guiBoard.setSelect((go.Point)pointListArg.get(i), true);
            m_guiBoard.repaint();
            if (modifiedSelect && pointListArg.size() > 0)
                analyzeBegin(false, false);
            return;
        }
        else if (m_scoreMode && ! modifiedSelect)
        {
            m_guiBoard.scoreSetDead(p);
            m_guiBoard.repaint();
            double komi = m_gameTree.getGameInformation().m_komi;
            m_scoreDialog.showScore(m_board.scoreGet(komi, getRules()));
            return;
        }
        else if (! modifiedSelect)
        {
            if (m_board.isSuicide(p, m_board.getToMove())
                && ! showQuestion("Play suicide?"))
                return;
            Move move = new Move(p, m_board.getToMove());
            humanMoved(move);
        }
    }

    public void gotoNode(Node node)
    {
        // GameTreeViewer is not disabled in score mode
        if (m_scoreMode)
            return;
        Vector shortestPath = NodeUtils.getShortestPath(m_currentNode, node);
        for (int i = 0; i < shortestPath.size(); ++i)
        {
            Node nextNode = (Node)shortestPath.get(i);
            if (nextNode == m_currentNode)
            {
                if (! backward(1))
                    break;
            }
            else
            {
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
        boardChangedBegin(false, false);
    }

    public boolean sendGtpCommand(String command, boolean sync)
        throws GtpError
    {
        if (isCommandInProgress() || m_commandThread == null)
            return false;
        if (sync)
        {
            m_commandThread.sendCommand(command);
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
        m_commandThread.sendCommand(command, callback);
        return true;
    }

    public void sendGtpCommandContinue()
    {
        endLengthyCommand();
        // Program could have been killed in cbInterrupt
        if (m_commandThread == null)
            return;
        m_commandThread.getException();
    }

    public void initAnalyzeCommand(AnalyzeCommand command, boolean autoRun)
    {
        if (m_commandThread == null)
            return;
        m_analyzeCommand = command;
        m_analyzeAutoRun = autoRun;
        if (command.needsPointArg())
        {
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
            showStatusSelectTarget();
        }
        else if (command.needsPointListArg())
        {
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
            showStatusSelectPointList();
        }
    }

    public void setAnalyzeCommand(AnalyzeCommand command, boolean autoRun,
                                  boolean clearBoard)
    {
        if (isCommandInProgress())
        {
            showError("Cannot run analyze command\n" +
                      "while command in progress.");
            return;
        }
        if (m_setupMode)
        {
            showError("Cannot run analyze command\n" +
                      "in setup mode.");
            return;
        }
        initAnalyzeCommand(command, autoRun);
        if (m_analyzeCommand.needsPointArg()
            || m_analyzeCommand.needsPointListArg())
        {
            m_guiBoard.clearAllSelect();
            m_guiBoard.repaint();
            toTop();
        }
        else
            analyzeBegin(false, clearBoard);
    }    

    public void toTop()
    {
        setState(Frame.NORMAL);
        toFront();
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
        
        private boolean m_checkComputerMove;

        private boolean m_resetBoard;
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
            showError("Invalid response:\n" + m_line);
        }
        
        private String m_line;
    }

    private static class LoadFileRunnable
        implements GuiUtils.ProgressRunnable
    {
        LoadFileRunnable(FileInputStream in, File file)
        {
            m_in = in;
            m_file = file;            
        }

        public sgf.Reader getReader()
        {
            return m_reader;
        }

        public void run(ProgressShow progressShow) throws Throwable
        {
            m_reader = new sgf.Reader(m_in, m_file.toString(), progressShow,
                                      m_file.length());
        }

        private File m_file;

        private FileInputStream m_in;

        private sgf.Reader m_reader;
    }

    private boolean m_analyzeAutoRun;

    private boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    private boolean m_fastPaint;

    private boolean m_ignoreInvalidResponses;

    private boolean m_isRootExecuted;

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

    private int m_currentNodeExecuted;

    private int m_handicap;

    private int m_move;

    private go.Board m_board;

    private go.Color m_setupColor;

    private gui.Board m_guiBoard;

    private Clock m_clock;

    private CommandThread m_commandThread;

    private Comment m_comment;

    private File m_loadedFile;

    private GameInfo m_gameInfo;    

    private GtpShell m_gtpShell;

    private GameTree m_gameTree;

    private GameTreeViewer m_gameTreeViewer;

    private Help m_help;

    private JLabel m_statusLabel;

    private JPanel m_boardPanel;

    private JPanel m_infoPanel;

    private JPanel m_innerPanel;

    private JSplitPane m_splitPane;

    private gogui.MenuBar m_menuBar;

    private Node m_currentNode;

    private Pattern m_pattern;

    private AnalyzeCommand m_analyzeCommand;

    private File m_file;

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

    private SquareLayout m_squareLayout;

    private TimeSettings m_timeSettings;

    private ToolBar m_toolBar;

    private void analyzeBegin(boolean checkComputerMove, boolean resetBoard)
    {
        if (m_commandThread == null || m_analyzeCommand == null
            || m_analyzeCommand.isPointArgMissing())
            return;
        showStatus("Running " + m_analyzeCommand.getResultTitle() + " ...");
        go.Color toMove = m_board.getToMove();
        go.Color color = m_analyzeDialog.getSelectedColor();
        m_lastAnalyzeCommand
            = m_analyzeCommand.replaceWildCards(toMove, color);
        runLengthyCommand(m_lastAnalyzeCommand,
                          new AnalyzeContinue(checkComputerMove, resetBoard));
    }

    private void analyzeContinue(boolean checkComputerMove,
                                 boolean resetBoard)
    {
        endLengthyCommand();
        String title = m_analyzeCommand.getResultTitle();
        try
        {
            if (resetBoard)
                resetBoard();
            GtpError e = m_commandThread.getException();
            if (e != null)
                throw e;
            String response = m_commandThread.getResponse();
            AnalyzeShow.show(m_analyzeCommand, m_guiBoard, m_board, response);
            int type = m_analyzeCommand.getType();
            go.Point pointArg = null;
            if (m_analyzeCommand.needsPointArg())
                pointArg = m_analyzeCommand.getPointArg();
            else if (m_analyzeCommand.needsPointListArg())
            {
                Vector vector = m_analyzeCommand.getPointListArg();
                if (vector.size() > 0)
                    pointArg = (go.Point)vector.get(vector.size() - 1);
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
            return;
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
        m_gtpShell = new GtpShell(this, "GoGui", this, m_prefs);
        m_gtpShell.setProgramCommand(program);
        m_ignoreInvalidResponses = false;
        Gtp.InvalidResponseCallback invalidResponseCallback =
            new Gtp.InvalidResponseCallback()
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
            Gtp gtp = new Gtp(m_program, m_verbose, m_gtpShell);
            gtp.setInvalidResponseCallback(invalidResponseCallback);
            if (m_gtpShell.getAutoNumber())
                gtp.enableAutoNumber();
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
            m_name = m_commandThread.sendCommand("name").trim();
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
        restoreSize(m_gtpShell, "window-gtpshell");
        m_gtpShell.setProgramName(m_name);
        Vector supportedCommands =
            m_commandThread.getSupportedCommands();
        m_gtpShell.setInitialCompletions(supportedCommands);
        if (! m_gtpFile.equals(""))
            m_gtpShell.sendGtpFile(new File(m_gtpFile));
        if (! m_gtpCommand.equals(""))
            sendGtpString(m_gtpCommand);
        Node oldCurrentNode = m_currentNode;
        m_board.initSize(m_boardSize);
        if (executeRoot())
            gotoNode(oldCurrentNode);
        setTitle();
        return true;
    }    

    private boolean backward(int n)
    {
        try
        {
            for (int i = 0; i < n; ++i)
            {
                if (m_currentNode.getFather() == null)
                    return false;
                undoCurrentNode();
                m_currentNode = m_currentNode.getFather();
                m_currentNodeExecuted = m_currentNode.getAllAsMoves().size();
                m_gameInfo.fastUpdateMoveNumber(m_currentNode);
            }
            computerNone();
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
        m_guiBoard.updateFromGoBoard();
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

    private void cbBeepAfterMove()
    {
        m_beepAfterMove = m_menuBar.getBeepAfterMove();
        m_prefs.setBool("beep-after-move", m_beepAfterMove);
    }

    private void cbBeginning()
    {
        backward(NodeUtils.getDepth(m_currentNode));
        boardChangedBegin(false, false);
    }

    private void cbBackToMainVar()
    {
        Node node = NodeUtils.getBackToMainVariation(m_currentNode);
        gotoNode(node);
    }

    private void cbBackward(int n)
    {
        backward(n);
        boardChangedBegin(false, false);
    }

    private void cbBoardSize(String size)
    {
        try
        {
            saveSession();
            cbNewGame(Integer.parseInt(size));
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
    }

    private void cbBoardSizeOther()
    {
        int size = BoardSizeDialog.show(this, m_boardSize);
        if (size < 1)
            return;
        saveSession();
        cbNewGame(size);
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
        go.Color color = m_board.getToMove();
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

    private void cbComputerBoth()
    {
        computerBoth();
        if (! isCommandInProgress())
            checkComputerMove();
    }

    private void cbEnd()
    {
        forward(NodeUtils.getNodesLeft(m_currentNode));
        boardChangedBegin(false, false);
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
        Node node = NodeUtils.findInComments(m_currentNode, m_pattern);
        if (node == null)
        {
            Node root = m_gameTree.getRoot();
            if (m_currentNode == root)
            {
                showInfo("Not found");
                m_menuBar.enableFindNext(false);
                return;
            }
            if (! showQuestion("End of tree reached. Continue from start?"))
                return;
            node = NodeUtils.findInComments(root, m_pattern);
            if (node == null)
            {
                showInfo("Not found");
                m_menuBar.enableFindNext(false);
                return;
            }
        }
        gotoNode(node);
        m_comment.markAll(m_pattern);
    }

    private void cbForward(int n)
    {
        forward(n);
        boardChangedBegin(false, false);
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
        if (timeSettings != null)
        {
            m_timeSettings = new TimeSettings(timeSettings);
            setTimeSettings();
        }
        else
            m_timeSettings = null;
        setTitle();
    }

    private void cbGoto()
    {
        Node node = MoveNumberDialog.show(this, m_currentNode);
        if (node == null)
            return;
        gotoNode(node);
        boardChangedBegin(false, false);
    }

    private void cbGotoVariation()
    {
        Node node = GotoVariationDialog.show(this, m_gameTree, m_currentNode);
        if (node == null)
            return;
        gotoNode(node);
        boardChangedBegin(false, false);
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
        URL u = getClass().getClassLoader().getResource("doc/index.html");
        if (u == null)
        {
            showError("Help not found");
            return;
        }
        if (m_help == null)
        {
            m_help = new Help(this, u);
            restoreSize(m_help, "window-help");
        }
        m_help.toTop();
    }

    private void cbInterrupt()
    {
        if (! isCommandInProgress() || m_commandThread == null
            || m_commandThread.isProgramDead())
            return;
        if (m_computerBlack && m_computerWhite)
        {
            if (showQuestion("Stop computer play?"))
            {
                computerNone();
                showStatus("Waiting for current move to finish ...");
            }
            return;
        }
        if (Interrupt.run(this, m_commandThread))
            showStatus("Interrupting ...");
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
        fileModified();
        newGame(size);
    }

    private void cbNextVariation()
    {
        Node node = NodeUtils.getNextVariation(m_currentNode);
        if (node != null)
            gotoNode(node);
    }

    private void cbNextEarlierVariation()
    {
        Node node = NodeUtils.getNextEarlierVariation(m_currentNode);
        if (node != null)
            gotoNode(node);
    }

    private void cbOpen()
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        File file = SimpleDialogs.showOpenSgf(this);
        if (file == null)
            return;
        m_menuBar.addRecent(file);
        m_menuBar.saveRecent();
        loadFile(file, -1);
    }

    private void cbOpenRecent()
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        File file = m_menuBar.getSelectedRecent();
        m_menuBar.addRecent(file);
        m_menuBar.saveRecent();
        loadFile(file, -1);
    }

    private void cbPass()
    {
        humanMoved(new Move(null, m_board.getToMove()));
    }

    private void cbPlay()
    {
        if (m_commandThread == null)
            return;
        if (! checkCurrentNodeExecuted())
            return;
        if (m_board.getToMove() == go.Color.BLACK)
            computerBlack();
        else
            computerWhite();
        generateMove();
        if (m_currentNode == m_gameTree.getRoot()
            && m_currentNode.getNumberChildren() == 0)
            m_clock.reset();
        m_clock.startMove(m_board.getToMove());
    }

    private void cbPreviousVariation()
    {
        Node node = NodeUtils.getPreviousVariation(m_currentNode);
        if (node != null)
            gotoNode(node);
    }

    private void cbPreviousEarlierVariation()
    {
        Node node = NodeUtils.getPreviousEarlierVariation(m_currentNode);
        if (node != null)
            gotoNode(node);
    }

    private void cbPrint()
    {
        Print.run(this, m_guiBoard);
    }

    private void cbSave()
    {
        saveDialog();
    }

    private void cbSavePosition()
    {
        File file = SimpleDialogs.showSaveSgf(this);
        if (file == null)
            return;
        try
        {
            savePosition(file);
            if (NodeUtils.isRootWithoutChildren(m_currentNode))
                setNeedsSave(false);
            m_loadedFile = file;
            setTitle();
        }
        catch (FileNotFoundException e)
        {
            showError("Could not save position.", e);
        }
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
            showStatus("Scoring ...");
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
        endLengthyCommand();
        clearStatus();
        // Program could have been detached whil running final_score
        if (m_commandThread == null)
            return;
        go.Point[] isDeadStone = null;
        try
        {
            GtpError e = m_commandThread.getException();
            if (e != null)
                throw e;
            String response = m_commandThread.getResponse();
            isDeadStone = GtpUtils.parsePointList(response, m_boardSize);
        }
        catch (GtpError e)
        {
            showError(e);
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
        m_guiBoard.repaint();
        m_scoreMode = false;
        m_toolBar.enableAll(true, m_currentNode);
        m_menuBar.setNormalMode();
    }

    private void cbSetup()
    {
        if (! m_setupMode)
        {
            if (m_needsSave && ! checkSaveGame())
                return;
            m_menuBar.setSetupMode();
            if (m_gameTreeViewer != null)
            {
                // Create a dummy game tree, so that GameTreeDialog shows
                // a setup node
                m_gameTree = new GameTree(m_boardSize, 0, null, null, null);
                m_currentNode = m_gameTree.getRoot();
                m_currentNode.addBlack(m_board.getPoint(0, 0));
                m_clock.reset();
                updateGameInfo(true);
            }
            resetBoard();
            m_setupMode = true;
            m_toolBar.enableAll(false, null);
            showStatus("Setup Black");
            m_setupColor = go.Color.BLACK;
        }
        else
            setupDone();
    }

    private void cbSetupBlack()
    {
        showStatus("Setup Black");
        m_setupColor = go.Color.BLACK;
        m_board.setToMove(m_setupColor);
        updateGameInfo(false);
    }

    private void cbSetupWhite()
    {
        showStatus("Setup White");
        m_setupColor = go.Color.WHITE;
        m_board.setToMove(m_setupColor);
        updateGameInfo(false);
    }

    private void cbShowCursor()
    {
        boolean showCursor = m_menuBar.getShowCursor();
        m_guiBoard.setShowCursor(showCursor);
        m_prefs.setBool("show-cursor", showCursor);
        m_guiBoard.repaint();
    }

    private void cbShowGrid()
    {
        boolean showGrid = m_menuBar.getShowGrid();
        m_guiBoard.setShowGrid(showGrid);
        m_prefs.setBool("show-grid", showGrid);
        m_guiBoard.repaint();
    }

    private void cbShowLastMove()
    {
        m_showLastMove = m_menuBar.getShowLastMove();
        m_prefs.setBool("show-last-move", m_showLastMove);
        updateGameInfo(false);
    }

    private void cbShowVariations()
    {
        m_showVariations = m_menuBar.getShowVariations();
        m_prefs.setBool("show-variations", m_showVariations);
        resetBoard();
        updateGameInfo(false);
    }

    private void cbTruncate()
    {
        if (m_currentNode.getFather() == null)
        {
            showError("Cannot truncate root node.");
            return;
        }
        if (! showQuestion("Truncate current?"))
            return;
        Node oldCurrentNode = m_currentNode;
        backward(1);
        m_currentNode.removeChild(oldCurrentNode);
        setNeedsSave(true);
        boardChangedBegin(false, true);
    }

    private void checkComputerMove()
    {
        m_clock.startMove(m_board.getToMove());
        updateMenuBar();
        if (m_commandThread == null || ! isCurrentNodeExecuted())
            return;
        boolean gameFinished = (m_board.bothPassed() || m_resigned);
        if (m_computerBlack && m_computerWhite)
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
                showInfo("Game finished");
                computerNone();
                return;
            }
            generateMove();            
        }
        else
        {
            if (gameFinished)
            {
                m_clock.halt();
                showInfo("Game finished");
                computerNone();
                return;
            }
            else if (computerToMove())
            {
                generateMove();
            }
        }
        m_clock.startMove(m_board.getToMove());
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

    private boolean checkSaveGame()
    {
        int result =
            JOptionPane.showConfirmDialog(this, "Save current game?",
                                          "Question",
                                          JOptionPane.YES_NO_CANCEL_OPTION);
        switch (result)
        {
        case 0:
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
        showStatus(" ");
    }

    private void clockRestore(Node node, go.Color color)
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
        endLengthyCommand();
        // Program could have been killed in cbInterrupt
        if (m_commandThread == null)
            return;
        if (m_beepAfterMove)
            java.awt.Toolkit.getDefaultToolkit().beep();
        try
        {
            GtpError e = m_commandThread.getException();
            if (e != null)
                throw e;
            m_clock.stopMove();
            String response = m_commandThread.getResponse();
            go.Color toMove = m_board.getToMove();
            if (response.toLowerCase().equals("resign"))
            {
                if (! (m_computerBlack && m_computerWhite))
                    showInfo(m_name + " resigns");
                m_resigned = true;
                setResult((toMove == go.Color.BLACK ? "W" : "B") + "+Resign");
            }
            else
            {
                go.Point point = GtpUtils.parsePoint(response, m_boardSize);
                if (point != null
                    && m_board.getColor(point) != go.Color.EMPTY)
                    showWarning("Program played move on non-empty point");
                Move move = new Move(point, toMove);
                setNeedsSave(true);
                m_board.play(move);
                Node node = createNode(move);
                m_currentNode = node;
                m_currentNodeExecuted = 1;
                if (point == null && ! (m_computerBlack && m_computerWhite))
                    showInfo(m_name + " passed");
                fileModified();
                m_resigned = false;
            }
            boardChangedBegin(true, true);
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
        if (m_board.getToMove() == go.Color.BLACK)
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
        Node node = new Node(move);
        if (m_clock.isInitialized())
        {
            assert(! m_clock.isRunning());
            go.Color color = move.getColor();
            // Round time to seconds
            double timeLeft = m_clock.getTimeLeft(color) / 1000;
            if (color == go.Color.BLACK)
            {
                node.setTimeLeftBlack(timeLeft);
                if (m_clock.isInByoyomi(color))
                    node.setMovesLeftBlack(m_clock.getMovesLeft(color));
            }
            else
            {
                assert(color == go.Color.WHITE);
                node.setTimeLeftWhite(timeLeft);
                if (m_clock.isInByoyomi(color))
                    node.setMovesLeftWhite(m_clock.getMovesLeft(color));
            }
        }
        m_currentNode.append(node);
        return node;
    }

    private JComponent createStatusBar()
    {
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(1, 0));
        outerPanel.add(panel, BorderLayout.CENTER);
        // Workaround for Java 1.4.1 on Mac OS X: add some empty space
        // so that status bar does not overlap the window resize widget
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            outerPanel.add(filler, BorderLayout.EAST);
        }
        JLabel label = new JLabel();
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        m_statusLabel = label;
        clearStatus();
        return outerPanel;
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
                        m_commandThread.sendCommand("quit");
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
                m_guiBoard.updateFromGoBoard();
            }
            catch (GtpError e)
            {
                assert(false);
            }
        }
    }

    private void endLengthyCommand()
    {
        clearStatus();
        m_menuBar.setNormalMode();
        m_toolBar.enableAll(true, m_currentNode);
        if (m_gtpShell != null)
            m_gtpShell.setCommandInProgess(false);
        if (m_analyzeCommand != null
            && (m_analyzeCommand.needsPointArg()
                || m_analyzeCommand.needsPointListArg()))
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
        else
            setBoardCursorDefault();
    }

    private void executeCurrentNode() throws GtpError
    {
        m_currentNodeExecuted = 0;
        Vector moves = m_currentNode.getAllAsMoves();
        for (int i = 0; i < moves.size(); ++i)
        {
            Move move = (Move)moves.get(i);
            if (m_commandThread != null)
                m_commandThread.sendCommandPlay(move);
            m_board.play(move);
            ++m_currentNodeExecuted;
        }
        go.Color toMove = m_currentNode.getToMove();
        if (toMove != go.Color.EMPTY)
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
                m_commandThread.sendCommandBoardsize(m_boardSize);
                m_commandThread.sendCommandClearBoard(m_boardSize);
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

    private void fileModified()
    {
        if (m_loadedFile == null)
            return;
        m_loadedFile = null;
        setTitle();
    }

    private void forward(int n)
    {
        if (! checkCurrentNodeExecuted())
            return;
        try
        {
            for (int i = 0; i < n && m_currentNode.getNumberChildren() > 0;
                 ++i)
            {
                m_currentNode = m_currentNode.getChild();
                executeCurrentNode();
                m_gameInfo.fastUpdateMoveNumber(m_currentNode);
            }
        }
        catch (GtpError e)
        {
            showError(e);
        }
    }

    private void generateMove()
    {
        showStatus(m_name + " is thinking ...");
        go.Color toMove = m_board.getToMove();
        String command = m_commandThread.getCommandGenmove(toMove);
        Runnable callback = new Runnable()
            {
                public void run()
                {
                    computerMoved();
                }
            };
        runLengthyCommand(command, callback);
    }

    private int getRules()
    {
        int result = go.Board.RULES_UNKNOWN;
        String rules = m_gameTree.getGameInformation().m_rules;
        if (rules != null)
        {
            rules = rules.trim().toLowerCase();
            if (rules.equals("japanese"))
                result = go.Board.RULES_JAPANESE;
            else if (rules.equals("chinese"))
                result = go.Board.RULES_CHINESE;
        }
        return result;
    }

    private void humanMoved(Move move)
    {
        try
        {
            go.Point point = move.getPoint();
            if (point != null && m_board.getColor(point) != go.Color.EMPTY)
                return;
            m_clock.stopMove();
            boolean newNodeCreated = play(move);
            setNeedsSave(newNodeCreated);
            if (point != null)
            {
                m_guiBoard.updateFromGoBoard(point);
                if (m_showLastMove)
                    m_guiBoard.markLastMove(move.getPoint());
                // Paint point immediately to pretend better responsiveness
                m_guiBoard.paintImmediately(point);
            }
            go.Color color = move.getColor();
            if (m_board.getMoveNumber() > 0
                && m_clock.lostOnTime(color)
                && ! m_lostOnTimeShown)
            {
                showInfo(color.toString() + " lost on time.");
                m_lostOnTimeShown = true;
            }
            m_resigned = false;
            boardChangedBegin(true, newNodeCreated);
        }
        catch (GtpError e)
        {
            showError(e);
        }
    }

    private void initGame(int size)
    {
        if (size != m_boardSize)
        {
            m_boardSize = size;
            m_guiBoard.initSize(size);
            m_guiBoard.setShowGrid(m_menuBar.getShowGrid());
            m_squareLayout.setPreferMultipleOf(size + 2);
            restoreMainWindow();
            if (m_gtpShell != null)
                restoreSize(m_gtpShell, "window-gtpshell");
            if (m_analyzeDialog != null)
                restoreSize(m_analyzeDialog, "window-analyze");
            if (m_gameTreeViewer != null)
                restoreSize(m_gameTreeViewer, "window-gametree");
        }
        Vector handicap = m_board.getHandicapStones(m_handicap);
        if (handicap == null)
            showWarning("Handicap stone locations are not\n" +
                        "defined for this board size.");
        m_gameTree = new GameTree(size, m_prefs.getDouble("komi"), handicap,
                                  m_prefs.getString("rules"), m_timeSettings);
        m_board.newGame();        
        m_currentNode = m_gameTree.getRoot();
        m_currentNodeExecuted = 0;
        m_guiBoard.updateFromGoBoard();
        resetBoard();
        m_clock.reset();
        m_clock.startMove(go.Color.BLACK);
        m_lostOnTimeShown = false;
        setNeedsSave(false);
        m_resigned = false;
        m_menuBar.enableFindNext(false);
    }

    private void initialize()
    {
        if (m_file != null)
            newGameFile(m_boardSize, m_move);
        else
            newGame(m_boardSize);
        m_toolBar.enableAll(true, m_currentNode);
        if (m_program != null)
            attachProgram(m_program);
        setTitle();
        if (m_commandThread == null
            || (! m_computerBlack && ! m_computerWhite))
            computerNone();
        else if (m_computerBlack && m_computerWhite)
            computerBoth();
        else  if (m_computerBlack)
            computerBlack();
        else
            computerWhite();
        updateGameInfo(true);
        registerSpecialMacHandler();
        if (! m_prefs.getBool("show-info-panel"))
        {
            m_menuBar.setShowInfoPanel(false);
            showInfoPanel();
        }
        if (! m_prefs.getBool("show-toolbar"))
        {
            m_menuBar.setShowToolbar(false);
            showToolbar();
        }
        restoreMainWindow();
        setVisible(true);
        // Children dialogs should be set visible after main window, otherwise
        // they get minimize window buttons and a taskbar entry (KDE 3.4)
        if (m_gtpShell != null && m_prefs.getBool("show-gtpshell"))
            m_gtpShell.toTop();
        if (m_prefs.getBool("show-gametree"))
            cbShowGameTree();
        if (m_prefs.getBool("show-analyze"))
            cbAnalyze();
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
        toTop();
        m_guiBoard.setFocus();
        setTitleFromProgram();
        checkComputerMove();
    }

    private void initScore(go.Point[] isDeadStone)
    {
        resetBoard();
        m_guiBoard.scoreBegin(isDeadStone);
        m_guiBoard.repaint();
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

    private boolean isCurrentNodeExecuted()
    {
        int numberAddStonesAndMoves = m_currentNode.getAllAsMoves().size();
        if (! m_isRootExecuted
            || m_currentNodeExecuted != numberAddStonesAndMoves)
            return false;
        return true;
    }

    private boolean isCommandInProgress()
    {
        if (m_commandThread == null)
            return false;
        return m_commandThread.isCommandInProgress();
    }
    
    private void loadFile(File file, int move)
    {
        try
        {
            FileInputStream in = new FileInputStream(file);
            LoadFileRunnable runnable = new LoadFileRunnable(in, file);
            if (file.length() > 500000)
            {
                newGame(m_boardSize); // Frees space if already large tree
                GuiUtils.runProgress(this, "Loading ...", runnable);
            }
            else
                runnable.run(null);
            sgf.Reader reader = runnable.getReader();
            GameInformation gameInformation =
                reader.getGameTree().getGameInformation();
            initGame(gameInformation.m_boardSize);
            m_gameTree = reader.getGameTree();
            if (executeRoot())
                if (move > 0)
                    forward(move);            
            m_loadedFile = file;
            setTitle();
            String warnings = reader.getWarnings();
            if (warnings != null)
                showWarning("Warnings while reading SGF file:\n" + warnings);
            SimpleDialogs.setLastFile(file);
            computerNone();
            boardChangedBegin(false, true);
        }
        catch (Throwable t)
        {
            if (t instanceof FileNotFoundException)
            {
                showError("File not found:\n" + file);
            }
            else if (t instanceof sgf.Reader.SgfError)
            {
                showError("Could not read file:", (sgf.Reader.SgfError)t);
            }
            else
            {
                t.printStackTrace();
                assert(false);
            }
        }
    }

    private void newGame(int size)
    {
        initGame(size);
        executeRoot();
        updateGameInfo(true);
        m_guiBoard.updateFromGoBoard();
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
        m_clock.halt();
        updateGameInfo(true);
        m_guiBoard.updateFromGoBoard();
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
            m_currentNodeExecuted = m_currentNode.getAllAsMoves().size();
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
        m_guiBoard.resetBoard();
        m_guiBoard.updateFromGoBoard();
        updateBoard();
        m_guiBoard.repaint();
    }
    
    private void restoreMainWindow()
    {
        Session.restoreLocation(this, m_prefs, "window-gogui", m_boardSize);
        try
        {
            String name = "fieldsize-" + m_boardSize;
            if (m_prefs.contains(name))
            {
                String value = m_prefs.getString(name);
                int fieldSize = Integer.parseInt(value);
                m_guiBoard.setPreferredFieldSize(new Dimension(fieldSize,
                                                               fieldSize));
            }
            name = "commentsize-" + m_boardSize;
            if (m_prefs.contains(name))
            {
                String[] tokens
                    = StringUtils.tokenize(m_prefs.getString(name));
                int width = Integer.parseInt(tokens[0]);                
                int height = Integer.parseInt(tokens[1]);
                m_comment.setPreferredSize(new Dimension(width, height));
            }
        }
        catch (NumberFormatException e)
        {
        }
        m_splitPane.resetToPreferredSizes();
        pack();
    }

    private void restoreSize(Window window, String name)
    {
        Session.restoreSize(window, m_prefs, name, m_boardSize);
    }

    private void runLengthyCommand(String cmd, Runnable callback)
    {
        assert(m_commandThread != null);
        beginLengthyCommand();
        m_commandThread.sendCommand(cmd, callback);
    }

    private void save(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        if (FileUtils.hasExtension(file, "tex"))
        {
            String title = FileUtils.removeExtension(new File(file.getName()),
                                                     "tex");
            new latex.Writer(title, out, m_gameTree, false);
        }
        else
        {
            new sgf.Writer(out, m_gameTree, file, "GoGui", Version.get());
            m_menuBar.addRecent(file);
            m_menuBar.saveRecent();
        }
    }

    private boolean saveDialog()
    {
        try
        {
            File file = SimpleDialogs.showSaveSgf(this);
            if (file == null)
                return false;
            save(file);
            m_loadedFile = file;
            setTitle();
            setNeedsSave(false);
            return true;
        }
        catch (FileNotFoundException e)
        {
            showError("Could not save game.", e);
            return false;
        }
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        if (FileUtils.hasExtension(file, "tex"))
        {
            String title = FileUtils.removeExtension(new File(file.getName()),
                                                     "tex");
            new latex.Writer(title, out, m_board, false,
                             m_guiBoard.getStrings(), m_guiBoard.getMarkups(),
                             m_guiBoard.getSelects());
        }
        else
        {
            new sgf.Writer(out, m_board, file, "GoGui", Version.get());
            m_menuBar.addRecent(file);
            m_menuBar.saveRecent();
        }
    }

    private void saveSession()
    {
        if (m_gtpShell != null)
            m_gtpShell.saveHistory();
        m_menuBar.saveRecent();
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

    private void sendGtpString(String commands)
    {        
        commands = commands.replaceAll("\\\\n", "\n");
        m_gtpShell.sendGtp(new StringReader(commands));
    }

    private void setBoardCursor(int type)
    {
        setCursor(m_boardPanel, type);
        setCursor(m_infoPanel, type);
    }

    private void setBoardCursorDefault()
    {
        setCursorDefault(m_boardPanel);
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
        if (m_commandThread == null)
            return;
        try
        {
            if (m_commandThread.isCommandSupported("komi"))
                m_commandThread.sendCommand("komi " + komi);
        }
        catch (GtpError e)
        {
            showError(e);
        }
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
    
    private static void setPrefsDefaults(Preferences prefs)
    {
        prefs.setBoolDefault("beep-after-move", true);
        prefs.setIntDefault("boardsize", 19);
        prefs.setDoubleDefault("komi", 6.5);
        prefs.setStringDefault("rules", "Chinese");
        prefs.setBoolDefault("show-analyze", false);
        prefs.setBoolDefault("show-gtpshell", false);
        prefs.setBoolDefault("show-gametree", false);
        prefs.setBoolDefault("show-cursor", true);
        prefs.setBoolDefault("show-grid", true);
        prefs.setBoolDefault("show-info-panel", true);
        prefs.setBoolDefault("show-last-move", false);
        prefs.setBoolDefault("show-toolbar", true);
        prefs.setBoolDefault("show-variations", false);
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
        if (m_commandThread == null)
            return;
        int rules = getRules();
        if (rules == go.Board.RULES_UNKNOWN)
            return;
        if (! m_commandThread.isCommandSupported("scoring_system"))
            return;
        try
        {
            String s =
                (rules == go.Board.RULES_JAPANESE ? "territory" : "area");
            m_commandThread.sendCommand("scoring_system " + s);
        }
        catch (GtpError e)
        {
        }
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
        {
            showError("Command time_settings not supported");
            return;
        }
        long preByoyomi = timeSettings.getPreByoyomi() / 1000;
        long byoyomi = 0;
        long byoyomiMoves = 0;
        if (timeSettings.getUseByoyomi())
        {
            byoyomi = timeSettings.getByoyomi() / 1000;
            byoyomiMoves = timeSettings.getByoyomiMoves();
        }
        try
        {
            m_clock.setTimeSettings(m_timeSettings);
            m_commandThread.sendCommand("time_settings " + preByoyomi + " "
                                        + byoyomi + " " + byoyomiMoves);
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
        if (m_gtpShell != null)
            m_gtpShell.setAppName(appName);
        if (m_analyzeDialog != null)
            m_analyzeDialog.setAppName(appName);
        String gameName = null;
        GameInformation gameInformation = m_gameTree.getGameInformation();
        String playerBlack = gameInformation.m_playerBlack;
        String playerWhite = gameInformation.m_playerWhite;
        String filename = null;
        if (m_loadedFile != null)
        {
            String fileNoExt = FileUtils.removeExtension(m_loadedFile, "sgf");
            filename = new File(fileNoExt).getName();
            if (m_needsSave)
                filename = filename + " (Modified)";
        }
        boolean playerBlackKnown =
            (playerBlack != null && ! playerBlack.trim().equals(""));
        boolean playerWhiteKnown =
            (playerWhite != null && ! playerWhite.trim().equals(""));
        if (playerBlackKnown || playerWhiteKnown)
        {
            if (playerBlackKnown)
                playerBlack = StringUtils.capitalize(playerBlack);
            else
                playerBlack = "Unknown";
            if (playerWhiteKnown)
                playerWhite = StringUtils.capitalize(playerWhite);
            else
                playerWhite = "Unknown";
            String blackRank = gameInformation.m_blackRank;
            String whiteRank = gameInformation.m_whiteRank;
            if (blackRank != null && ! blackRank.trim().equals(""))
                playerBlack = playerBlack + " [" + blackRank + "]";
            if (whiteRank != null && ! whiteRank.trim().equals(""))
                playerWhite = playerWhite + " [" + whiteRank + "]";
            if (filename != null)
                gameName = filename + " - "
                    + playerBlack + " vs " + playerWhite;
            else
                gameName = playerBlack + " vs " + playerWhite;
        }
        else if (filename != null)
            gameName = filename;
        if (gameName != null)
            setTitle(gameName + " - " + appName);
        else
            setTitle(appName);        
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
                m_titleFromProgram =
                    m_commandThread.sendCommand("gogui_title");
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
        m_menuBar.setNormalMode();
        m_toolBar.enableAll(true, m_currentNode);
        int size = m_board.getSize();
        go.Color color[][] = new go.Color[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            color[p.getX()][p.getY()] = m_board.getColor(p);
        }
        go.Color toMove = m_board.getToMove();
        m_boardSize = size;
        m_board.newGame();        
        m_gameTree = new GameTree(size, m_prefs.getDouble("komi"), null,
                                  m_prefs.getString("rules"), null);
        m_currentNode = m_gameTree.getRoot();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            int x = point.getX();
            int y = point.getY();
            go.Color c = color[x][y];
            if (c == go.Color.BLACK)
                m_currentNode.addBlack(point);
            else if (c == go.Color.WHITE)
                m_currentNode.addWhite(point);
        }
        m_currentNode.setPlayer(toMove);
        executeRoot();
        fileModified();
        updateGameInfo(true);
        boardChangedBegin(false, false);
    }

    private void showAnalyzeTextOutput(int type, go.Point pointArg,
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
                        go.Point list[] =
                            GtpUtils.parsePointString(text, m_boardSize);
                        m_guiBoard.showPointList(list);
                        m_guiBoard.repaint();
                    }
                };
        }
        TextViewer textViewer =
            new TextViewer(this, title, response, highlight, listener);
        if (pointArg != null)
        {
            java.awt.Point location =
                m_guiBoard.getLocationOnScreen(pointArg);
            textViewer.setLocation(location);
        }
        textViewer.setVisible(true);
    }

    private void showError(String message, Exception e)
    {
        SimpleDialogs.showError(this, message, e);
    }

    private void showError(GtpError error)
    {        
        String message = error.getMessage().trim();
        if (message.length() == 0)
            message = "Command failed";
        else
            message = StringUtils.capitalize(message);
        SimpleDialogs.showError(this, message);
    }

    private void showError(String message)
    {
        SimpleDialogs.showError(this, message);
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
        if (! showInfoPanel)
        {
            m_splitPane.remove(m_boardPanel);
            m_innerPanel.remove(m_splitPane);
            m_innerPanel.add(m_boardPanel);
        }
        else
        {
            m_innerPanel.remove(m_boardPanel);
            m_splitPane.add(m_boardPanel);
            m_innerPanel.add(m_splitPane);
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
        m_statusLabel.setText(text);
        m_statusLabel.repaint();
    }

    private void showStatusSelectPointList()
    {
        showStatus("Select points for " + m_analyzeCommand.getLabel()
                   + " (last point with right button or Ctrl key down)");
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
            getContentPane().add(m_toolBar, BorderLayout.NORTH);
        else
            getContentPane().remove(m_toolBar);
        m_splitPane.resetToPreferredSizes();
        pack();
    }

    private void showToMove()
    {
        if (m_board.getToMove() == go.Color.WHITE)
            showStatus("White to play");
        else
            showStatus("Black to play");
    }

    private void showWarning(String message)
    {
        SimpleDialogs.showWarning(this, message);
    }

    private void undoCurrentNode() throws GtpError
    {
        if (m_commandThread != null
            && ! m_commandThread.isCommandSupported("undo"))
            throw new GtpError("Program does not support undo");
        for ( ; m_currentNodeExecuted > 0; --m_currentNodeExecuted)
        {
            if (m_commandThread != null)
                m_commandThread.sendCommand("undo");
            m_board.undo();
        }
        m_guiBoard.updateFromGoBoard();
    }

    private void updateBoard()
    {
        if (m_showVariations)
        {
            Vector childrenMoves = NodeUtils.getChildrenMoves(m_currentNode);
            m_guiBoard.showChildrenMoves(childrenMoves);
        }
        if (m_showLastMove &&
            (m_commandThread == null || isCurrentNodeExecuted()))
        {
            Move move = m_currentNode.getMove();
            if (move != null)
                m_guiBoard.markLastMove(move.getPoint());
            else
                m_guiBoard.markLastMove(null);
        }
        else
            m_guiBoard.markLastMove(null);
    }

    private void updateGameInfo(boolean gameTreeChanged)
    {
        m_gameInfo.update(m_currentNode, m_board);
        updateGameTree(gameTreeChanged);
        m_comment.setNode(m_currentNode);
        updateBoard();
        m_guiBoard.repaint();
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
