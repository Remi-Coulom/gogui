//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import game.*;
import go.*;
import gtp.*;
import latex.*;
import sgf.*;
import utils.*;
import version.*;

//-----------------------------------------------------------------------------

class GoGui
    extends JFrame
    implements ActionListener, AnalyzeDialog.Callback, Board.Listener,
               GameTreeViewer.Listener, GtpShell.Callback
{
    GoGui(String program, Preferences prefs, String file, int move,
          String time, boolean verbose, boolean computerBlack,
          boolean computerWhite, boolean auto, String gtpFile,
          String gtpCommand, String initAnalyze)
        throws Gtp.Error
    {
        m_prefs = prefs;
        setPrefsDefaults(m_prefs);
        m_boardSize = prefs.getInt("boardsize");
        m_beepAfterMove = prefs.getBool("beep-after-move");
        m_file = file;
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_move = move;
        m_computerBlack = computerBlack;
        m_computerWhite = computerWhite;
        m_auto = auto;
        m_verbose = verbose;
        m_initAnalyze = initAnalyze;

        Container contentPane = getContentPane();        

        m_infoPanel = new JPanel(new BorderLayout());
        m_timeControl = new TimeControl();
        m_gameInfo = new GameInfo(m_timeControl);
        m_gameInfo.setBorder(utils.GuiUtils.createSmallEmptyBorder());
        m_infoPanel.add(m_gameInfo, BorderLayout.NORTH);

        m_board = new go.Board(m_boardSize);

        m_guiBoard = new Board(m_board);
        m_guiBoard.setListener(this);
        m_toolBar = new ToolBar(this, prefs);
        contentPane.add(m_toolBar, BorderLayout.NORTH);
        contentPane.add(createStatusBar(), BorderLayout.SOUTH);

        m_squareLayout = new SquareLayout();
        m_squareLayout.setPreferMultipleOf(2 + 2 * m_boardSize);
        m_boardPanel = new JPanel(m_squareLayout);
        m_boardPanel.add(m_guiBoard);

        Comment.Listener commentListener = new Comment.Listener()
            { public void changed() { cbCommentChanged(); } };
        m_comment = new Comment(commentListener);
        m_infoPanel.add(m_comment, BorderLayout.CENTER);
        m_splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                     m_boardPanel, m_infoPanel);
        m_splitPane.setResizeWeight(0.85);
        contentPane.add(m_splitPane, BorderLayout.CENTER);
        
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

        m_menuBar = new MenuBar(this);
        m_menuBar.selectBoardSizeItem(m_boardSize);
        m_menuBar.setBeepAfterMove(m_beepAfterMove);
        m_menuBar.setShowLastMove(m_prefs.getBool("show-last-move"));
        m_guiBoard.setShowLastMove(m_prefs.getBool("show-last-move"));
        m_menuBar.setShowCursor(m_prefs.getBool("show-cursor"));
        m_guiBoard.setShowCursor(m_prefs.getBool("show-cursor"));
        setJMenuBar(m_menuBar.getMenuBar());
        m_program = program;
        if (m_program == null)
        {
            m_toolBar.setComputerEnabled(false);
            m_menuBar.setComputerEnabled(false);
        }
        m_menuBar.setNormalMode();

        pack();
        m_guiBoard.requestFocusInWindow();
        setTitle("GoGui");
        restoreMainWindow();
        try
        {
            if (time != null)
                m_timeControl.setTime(time);
        }
        catch (TimeControl.Error e)
        {
            showWarning(e.getMessage());
        }
        Runnable callback = new Runnable()
            {
                public void run() { initialize(); }
            };
        SwingUtilities.invokeLater(callback);
    }
    
    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (m_commandInProgress
            && ! command.equals("about")
            && ! command.equals("beep-after-move")
            && ! command.equals("computer-black")
            && ! command.equals("computer-both")
            && ! command.equals("computer-none")
            && ! command.equals("computer-white")
            && ! command.equals("gtp-shell")
            && ! command.equals("help")
            && ! command.equals("interrupt")
            && ! command.equals("show-last-move")
            && ! command.equals("exit"))
            return;
        if (command.equals("about"))
            cbShowAbout();
        else if (command.equals("analyze"))
            cbAnalyze();
        else if (command.equals("attach-program"))
            cbAttachProgram();
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
            cbNewGame(command.substring("board-size-".length()));
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
        else if (command.equals("forward"))
            cbForward(1);
        else if (command.equals("forward-10"))
            cbForward(10);
        else if (command.equals("game-info"))
            cbGameInfo();
        else if (command.equals("goto"))
            cbGoto();
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
        else if (command.equals("show-gametree"))
            cbShowGameTree();
        else if (command.equals("show-last-move"))
            cbShowLastMove();
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
            Vector supportedCommands = null;
            m_analyzeDialog =
                new AnalyzeDialog(this, m_prefs,
                                  m_commandThread.getSupportedCommands());
            restoreSize(m_analyzeDialog, "window-analyze", m_boardSize);
            setTitle();
        }
        m_analyzeDialog.toTop();
    }

    public void cbAttachProgram()
    {        
        if (m_commandThread != null)
            return;
        String program = SelectProgram.select(this);
        if (program == null)
            return;
        attachProgram(program);
    }

    public void cbDetachProgram()
    {        
        if (m_commandThread == null)
            return;
        if (! showQuestion("Detach program?"))
            return;
        detachProgram();
    }

    public void cbGtpShell()
    {
        if (m_gtpShell != null)
            m_gtpShell.toTop();
    }

    public void clearAnalyzeCommand()
    {
        if (m_commandInProgress)
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

    public void fieldClicked(go.Point p, boolean modifiedSelect)
    {
        if (m_commandInProgress)
            return;
        if (m_setupMode && ! modifiedSelect)
        {
            if (m_board.getColor(p) != m_setupColor)
                m_board.play(new Move(p, m_setupColor));
            else
                m_board.play(new Move(p, go.Color.EMPTY));
            m_board.setToMove(m_setupColor);
            updateGameInfo(true);
            m_guiBoard.updateFromGoBoard();
            m_needsSave = true;
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
                if (! modifiedSelect)
                    pointListArg.remove(p);
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
            float komi = m_gameTree.getGameInformation().m_komi;
            m_scoreDialog.showScore(m_board.scoreGet(komi, getRules()));
            return;
        }
        else if (! modifiedSelect)
        {
            if (m_board.isSuicide(p, m_board.getToMove())
                && ! showQuestion("This point is a suicide move.\n" +
                                  "Do you really want to play there?"))
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
        Vector shortestPath = m_currentNode.getShortestPath(node);
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
                catch (Gtp.Error e)
                {
                    showGtpError(e);
                    break;
                }
            }
        }
        boardChangedBegin(false, false);
    }

    public static void main(String[] args)
    {
        boolean verbose = false;
        try
        {
            String options[] = {
                "analyze:",
                "auto",
                "command:",
                "computer-black",
                "computer-both",
                "computer-none",
                "config:",
                "file:",
                "gtpfile:",
                "help",
                "komi:",
                "move:",
                "rules:",
                "size:",
                "time:",
                "verbose",
                "version"
            };
            Options opt = new Options(args, options);
            opt.handleConfigOption();
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gogui.jar [options] program\n" +
                    "Graphical user interface for Go programs\n" +
                    "using the Go Text Protocol.\n" +
                    "\n" +
                    "-analyze name   initialize analyze command\n" +
                    "-auto           auto play games (if computer both)\n" +
                    "-command cmd    send GTP command at startup\n" +
                    "-computer-both  computer plays both sides\n" +
                    "-computer-black computer plays black\n" +
                    "-computer-none  computer plays no side\n" +
                    "-config         config file\n" +
                    "-file filename  load SGF file\n" +
                    "-gtpfile file   send GTP file at startup\n" +
                    "-help           display this help and exit\n" +
                    "-komi value     set komi\n" +
                    "-move n         load SGF file until move number\n" +
                    "-rules name     use rules (chinese|japanese)\n" +
                    "-size n         set board size\n" +
                    "-time spec      set time limits (min[+min/moves])\n" +
                    "-verbose        print debugging messages\n" +
                    "-version        print version and exit\n";
                System.out.print(helpText);
                System.exit(0);
            }
            if (opt.isSet("version"))
            {
                System.out.println("GoGui " + Version.get());
                System.exit(0);
            }
            Preferences prefs = new Preferences();
            String initAnalyze = opt.getString("analyze");
            boolean auto = opt.isSet("auto");
            boolean computerBlack = false;
            boolean computerWhite = true;
            if (opt.isSet("computer-none"))
                computerWhite = false;
            else if (opt.isSet("computer-black"))
            {
                computerBlack = true;
                computerWhite = false;
            }
            else if (opt.isSet("computer-both"))
                computerBlack = true;
            String file = opt.getString("file", "");
            String gtpFile = opt.getString("gtpfile", "");
            String gtpCommand = opt.getString("command", "");
            if (opt.contains("komi"))
                prefs.setFloat("komi", opt.getFloat("komi"));
            int move = opt.getInteger("move", -1);
            if (opt.contains("size"))
                prefs.setInt("boardsize", opt.getInteger("size"));
            String rules = opt.getString("rules", "");
            prefs.setString("rules", rules);
            String time = opt.getString("time", null);
            verbose = opt.isSet("verbose");
            Vector arguments = opt.getArguments();
            String program = null;
            if (arguments.size() == 1)
            {
                program = (String)arguments.get(0);
                SelectProgram.addHistory(program);
            }
            else if (arguments.size() > 1)
                throw new Exception("Only one program argument allowed.");
            GoGui gui = new GoGui(program, prefs, file, move, time,
                                  verbose, computerBlack, computerWhite, auto,
                                  gtpFile, gtpCommand, initAnalyze);
        }
        catch (AssertionError e)
        {
            SimpleDialogs.showError(null, "Assertion error");
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
        {
            SimpleDialogs.showError(null, StringUtils.formatException(e));
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            SimpleDialogs.showError(null, StringUtils.formatException(t));
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public boolean sendGtpCommand(String command, boolean sync)
        throws Gtp.Error
    {
        if (m_commandInProgress || m_commandThread == null)
            return false;
        if (sync)
        {
            m_commandThread.sendCommand(command);
            return true;
        }
        Runnable callback = new Runnable()
            {
                public void run() { sendGtpCommandContinue(); }
            };
        beginLengthyCommand();
        m_commandThread.sendCommand(command, callback);
        return true;
    }

    public void sendGtpCommandContinue()
    {
        m_commandThread.getException();
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
        if (m_commandInProgress)
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

    private boolean m_analyzeAutoRun;

    private boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    private boolean m_commandInProgress;

    private boolean m_isRootExecuted;

    private boolean m_lostOnTimeShown;

    private boolean m_needsSave;

    private boolean m_resigned;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_verbose;

    private int m_boardSize;

    private int m_currentNodeExecuted;

    private int m_handicap;

    private int m_move;

    private go.Board m_board;

    private go.Color m_setupColor;

    private Board m_guiBoard;

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

    private JSplitPane m_splitPane;

    private MenuBar m_menuBar;

    private Node m_currentNode;

    private AnalyzeCommand m_analyzeCommand;

    private String m_file;

    private String m_gtpCommand;

    private String m_gtpFile;

    private String m_initAnalyze;

    private String m_name = "";

    private String m_program;

    private String m_version = "";

    private AnalyzeDialog m_analyzeDialog;    

    private Preferences m_prefs;

    private ScoreDialog m_scoreDialog;

    private SquareLayout m_squareLayout;

    private TimeControl m_timeControl;

    private ToolBar m_toolBar;

    static
    {
        // Must be set before static initializers of apple.awt are executed.
        System.setProperty("apple.awt.brushMetalLook", "true");
    }

    private void addPlayerComputerToGameInfo(go.Color color)
    {
        if (m_commandThread == null)
            return;
        String name = m_name;
        if (m_version != null && ! m_version.equals(""))
            name = name +  ":" + m_version;
        GameInformation gameInformation = m_gameTree.getGameInformation();
        if (color == go.Color.BLACK
            && (gameInformation.m_playerBlack == null
                || gameInformation.m_playerBlack.trim().equals("")))
            gameInformation.m_playerBlack = name;
        if (color == go.Color.WHITE
            && (gameInformation.m_playerWhite == null
                || gameInformation.m_playerWhite.trim().equals("")))
            gameInformation.m_playerWhite = name;
    }

    private void analyzeBegin(boolean checkComputerMove, boolean resetBoard)
    {
        if (m_commandThread == null)
            return;
        if (m_analyzeCommand.isPointArgMissing())
            return;
        showStatus("Running " + m_analyzeCommand.getResultTitle() + " ...");
        String command =
            m_analyzeCommand.replaceWildCards(m_board.getToMove());
        runLengthyCommand(command,
                          new AnalyzeContinue(checkComputerMove, resetBoard));
    }

    private void analyzeContinue(boolean checkComputerMove, boolean resetBoard)
    {
        endLengthyCommand();
        String title = m_analyzeCommand.getResultTitle();
        try
        {
            if (resetBoard)
                resetBoard();
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            String response = m_commandThread.getResponse();
            AnalyzeShow.show(m_analyzeCommand, m_guiBoard, m_board, response);
            int type = m_analyzeCommand.getType();
            boolean statusContainsResponse = false;
            if (type == AnalyzeCommand.STRING
                || type == AnalyzeCommand.HSTRING
                || type == AnalyzeCommand.HPSTRING
                || type == AnalyzeCommand.PSTRING
                || type == AnalyzeCommand.VAR
                || type == AnalyzeCommand.VARW
                || type == AnalyzeCommand.VARB
                || type == AnalyzeCommand.VARP
                || type == AnalyzeCommand.VARPO)
            {
                if (response.indexOf("\n") < 0)
                {
                    showStatus(title + ": " + response);
                    statusContainsResponse = true;
                }
                else
                {
                    boolean highlight =
                        (type == AnalyzeCommand.HSTRING
                         || type == AnalyzeCommand.HPSTRING);
                    new AnalyzeTextOutput(this, title, response, highlight);
                }
            }
            if (! statusContainsResponse)
                showStatus(title);
            if (checkComputerMove)
                checkComputerMove();
        }
        catch(Gtp.Error e)
        {                
            showGtpError(e);
            showStatus(title);
            return;
        }
    }

    private void attachProgram(String program)
    {
        program = program.trim();
        if (program.equals(""))
            return;
        m_program = program;
        m_gtpShell = new GtpShell("GoGui", this, m_prefs);
        m_gtpShell.setProgramCommand(program);
        try
        {
            Gtp gtp = new Gtp(m_program, m_verbose, m_gtpShell);
            m_commandThread = new CommandThread(gtp);
            m_commandThread.start();
        }
        catch (Gtp.Error e)
        {
            SimpleDialogs.showError(this,
                                    e.getMessage() + "\n"
                                    + "See GTP shell for any error messages\n"
                                    + "printed by the program.");
            return;
        }
        m_menuBar.setComputerEnabled(true);
        m_toolBar.setComputerEnabled(true);
        try
        {
            setFastUpdate(true);
            m_name = m_commandThread.sendCommand("name").trim();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
            return;
        }
        finally
        {
            setFastUpdate(false);
        }
        try
        {
            m_commandThread.queryProtocolVersion();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
        try
        {
            m_version = m_commandThread.queryVersion();
            m_gtpShell.setProgramVersion(m_version);
            m_commandThread.querySupportedCommands();
            m_commandThread.queryInterruptSupport();
        }
        catch (Gtp.Error e)
        {
        }
        restoreSize(m_gtpShell, "window-gtpshell", m_boardSize);
        if (m_prefs.getBool("show-gtpshell"))
            m_gtpShell.toTop();
        if (m_prefs.getBool("show-analyze"))
            cbAnalyze();
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
        executeRoot();
        gotoNode(oldCurrentNode);
        setTitle();
    }    

    private boolean backward(int n)
    {
        setFastUpdate(true);
        try
        {
            for (int i = 0; i < n; ++i)
            {
                if (m_currentNode.getFather() == null)
                    return false;
                undoCurrentNode();
                m_currentNode = m_currentNode.getFather();
                m_currentNodeExecuted =
                    m_currentNode.getNumberAddStonesAndMoves();
            }
            computerNone();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
            return false;
        }
        finally
        {
            setFastUpdate(false);
        }
        return true;
    }

    private void beginLengthyCommand()
    {
        setBoardCursor(Cursor.WAIT_CURSOR);
        boolean isInterruptSupported =
            (m_commandThread.isInterruptSupported()
             || (m_computerBlack && m_computerWhite));
        m_menuBar.setCommandInProgress(isInterruptSupported);
        m_toolBar.setCommandInProgress(isInterruptSupported);
        m_gtpShell.setCommandInProgess(true);
        m_commandInProgress = true;
    }

    private void boardChangedBegin(boolean doCheckComputerMove,
                                   boolean gameTreeChanged)
    {
        m_guiBoard.updateFromGoBoard();
        updateGameInfo(gameTreeChanged);
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_gameTree, m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        clearStatus();
        if (m_commandThread != null
            && isCurrentNodeExecuted()
            && m_analyzeCommand != null
            && m_analyzeAutoRun
            && ! m_analyzeCommand.isPointArgMissing())
            analyzeBegin(doCheckComputerMove, true);
        else
        {
            resetBoard();
            if (doCheckComputerMove)
                checkComputerMove();
        }
    }

    private void cbBeepAfterMove()
    {
        m_beepAfterMove = m_menuBar.getBeepAfterMove();
        m_prefs.setBool("beep-after-move", m_beepAfterMove);
    }

    private void cbBeginning()
    {
        backward(m_currentNode.getDepth());
        boardChangedBegin(false, false);
    }

    private void cbBackward(int n)
    {
        backward(n);
        boardChangedBegin(false, false);
    }

    private void cbBoardSizeOther()
    {
        String value =
            JOptionPane.showInputDialog(this, "Board size",
                                        Integer.toString(m_boardSize));
        if (value == null)
            return;
        int boardSize = -1;
        try
        {
            boardSize = Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
        }
        if (boardSize <= 0)
        {
            showError("Invalid board size");
            return;
        }
        cbNewGame(boardSize);
    }
    
    private void cbCommentChanged()
    {
        m_needsSave = true;
        if (m_gameTreeViewer != null)
            m_gameTreeViewer.redrawCurrentNode();
    }

    private void cbComputerBoth()
    {
        computerBoth();
        checkComputerMove();
    }

    private void cbEnd()
    {
        forward(m_currentNode.getNodesLeft());
        boardChangedBegin(false, false);
    }

    private void cbForward(int n)
    {
        forward(n);
        boardChangedBegin(false, false);
    }

    private void cbGameInfo()
    {
        GameInformation gameInformation = m_gameTree.getGameInformation();
        GameInfoDialog.show(this, gameInformation);
        if (gameInformation.m_komi != m_prefs.getFloat("komi"))
        {
            m_prefs.setFloat("komi", gameInformation.m_komi);
            setKomi(gameInformation.m_komi);
        }
        if (! gameInformation.m_rules.equals(m_prefs.getString("rules")))
        {
            m_prefs.setString("rules", gameInformation.m_rules);
            setRules();
        }
    }

    private void cbGoto()
    {
        String value = JOptionPane.showInputDialog(this, "Move number");
        if (value == null || value.equals(""))
            return;
        try
        {
            int gotoNumber = Integer.parseInt(value);
            int moveNumber = m_currentNode.getMoveNumber();
            int movesLeft = m_currentNode.getMovesLeft();
            if (gotoNumber < 0 || gotoNumber > moveNumber + movesLeft)
            {
                showError("Invalid move number");
                return;
            }
            int numberNodes = 0;
            Node node = m_currentNode;
            if (gotoNumber < moveNumber)
            {
                while (node.getFather() != null && moveNumber > gotoNumber)
                {
                    if (node.getMove() != null)
                        --moveNumber;
                    ++numberNodes;
                    node = node.getFather();
                }
                backward(numberNodes);
            }
            else
            {
                while (node.getChild() != null && moveNumber < gotoNumber)
                {
                    if (node.getMove() != null)
                        ++moveNumber;
                    ++numberNodes;
                    node = node.getChild();
                }
                forward(numberNodes);
            }
            boardChangedBegin(false, false);
        }
        catch (NumberFormatException e)
        {
            showError("Invalid move number");
        }
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
            m_help = new Help(u);
            restoreSize(m_help, "window-help", m_boardSize);
        }
        m_help.toTop();
    }

    private void cbInterrupt()
    {
        if (! m_commandInProgress || m_commandThread == null
            || m_commandThread.isProgramDead())
            return;
        if (m_computerBlack && m_computerWhite)
            computerNone();
        if (! m_commandThread.isInterruptSupported())
            return;
        if (! showQuestion("Interrupt command?"))
            return;
        sendInterrupt();
    }

    private void cbKeepOnlyMainVariation()
    {
        if (! m_currentNode.isInMainVariation())
            return;
        if (! showQuestion("Delete all variations except main?"))
            return;
        m_gameTree.keepOnlyMainVariation();
        m_needsSave = true;
        boardChangedBegin(false, true);
    }

    private void cbKeepOnlyPosition()
    {
        if (! showQuestion("Delete all moves?"))
            return;
        GameInformation gameInformation = m_gameTree.getGameInformation();
        m_gameTree = new GameTree(m_boardSize, gameInformation.m_komi, null,
                                  gameInformation.m_rules);
        Node root = m_gameTree.getRoot();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            go.Color color = m_board.getColor(point);
            if (color == go.Color.BLACK)
                root.addBlack(point);
            else if (color == go.Color.WHITE)
                root.addWhite(point);
        }
        root.setToMove(m_board.getToMove());
        m_board.initSize(m_boardSize);
        executeRoot();
        m_needsSave = true;
        boardChangedBegin(false, true);
    }

    private void cbMakeMainVariation()
    {
        if (! showQuestion("Make current node to main variation?"))
            return;
        m_currentNode.makeMainVariation();
        m_needsSave = true;
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

    private void cbNewGame(String size)
    {
        try
        {
            cbNewGame(Integer.parseInt(size));
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
    }

    private void cbNextVariation()
    {
        Node node = m_currentNode.getNextVariation();
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
        loadFile(file, -1);
    }

    private void cbOpenRecent()
    {
        if (m_needsSave && ! checkSaveGame())
            return;
        File file = m_menuBar.getSelectedRecent();
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
        if (m_board.getToMove() == go.Color.BLACK)
            computerBlack();
        else
            computerWhite();
        generateMove();
        m_timeControl.startMove(m_board.getToMove());
    }

    private void cbPreviousVariation()
    {
        Node node = m_currentNode.getPreviousVariation();
        if (node != null)
            gotoNode(node);
    }

    private void cbPrint()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(m_guiBoard);
        if (job.printDialog())
        {
            try
            {
                job.print();
            }
            catch (Exception e)
            {
                showError("Printing failed", e);
            }
            showInfo("Printing done");
        }
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
            if (file.exists())
                if (! showQuestion("Overwrite " + file + "?"))
                    return;
            savePosition(file);
            if (m_currentNode.getFather() == null
                && m_currentNode.getChild() == null)
                m_needsSave = false;
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
        go.Point[] isDeadStone = null;
        if (m_commandThread != null
            && m_commandThread.isCommandSupported("final_status_list"))
        {
            Runnable callback = new Runnable()
                {
                    public void run() { cbScoreContinue(); }
                };
            showStatus("Scoring ...");
            runLengthyCommand("final_status_list dead", callback);
        }
        else
            initScore(null);
    }

    private void cbScoreContinue()
    {
        endLengthyCommand();
        clearStatus();
        go.Point[] isDeadStone = null;
        try
        {
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            isDeadStone = Gtp.parsePointList(m_commandThread.getResponse(),
                                             m_boardSize);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
        initScore(isDeadStone);
    }    

    private void cbScoreDone(boolean accepted)
    {
        m_scoreDialog.setVisible(false);
        if (accepted)
        {
            float komi = m_gameTree.getGameInformation().m_komi;
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
                m_gameTree = new GameTree(m_boardSize, 0, null, null);
                m_currentNode = m_gameTree.getRoot();
                m_currentNode.addBlack(m_board.getPoint(0, 0));
                m_timeControl.reset();
                updateGameInfo(true);
            }
            resetBoard();
            m_setupMode = true;
            m_toolBar.enableAll(false, null);
            showStatus("Setup black.");
            m_setupColor = go.Color.BLACK;
        }
        else
            setupDone();
    }

    private void cbSetupBlack()
    {
        showStatus("Setup black.");
        m_setupColor = go.Color.BLACK;
        m_board.setToMove(m_setupColor);
        updateGameInfo(false);
    }

    private void cbSetupWhite()
    {
        showStatus("Setup white.");
        m_setupColor = go.Color.WHITE;
        m_board.setToMove(m_setupColor);
        updateGameInfo(false);
    }

    private void cbShowAbout()
    {
        AboutDialog.show(this, m_name, m_version);
    }

    private void cbShowCursor()
    {
        boolean showCursor = m_menuBar.getShowCursor();
        m_guiBoard.setShowCursor(showCursor);
        m_prefs.setBool("show-cursor", showCursor);
        m_guiBoard.repaint();
    }

    private void cbShowGameTree()
    {
        if (m_gameTreeViewer == null)
        {
            m_gameTreeViewer = new GameTreeViewer(this);
            restoreSize(m_gameTreeViewer, "window-gametree", m_boardSize);
        }
        m_gameTreeViewer.update(m_gameTree, m_currentNode);
        m_gameTreeViewer.toTop();
    }

    private void cbShowLastMove()
    {
        boolean showLastMove = m_menuBar.getShowLastMove();
        m_guiBoard.setShowLastMove(showLastMove);
        m_prefs.setBool("show-last-move", showLastMove);
    }

    private void cbTruncate()
    {
        if (m_currentNode.getFather() == null)
        {
            showError("Cannot truncate root node.");
            return;
        }
        if (! showQuestion("Truncate current node and all its children?"))
            return;
        Node oldCurrentNode = m_currentNode;
        backward(1);
        m_currentNode.removeChild(oldCurrentNode);
        m_needsSave = true;
        boardChangedBegin(false, true);
    }

    private void checkComputerMove()
    {
        m_timeControl.startMove(m_board.getToMove());
        if (m_commandThread == null || ! isCurrentNodeExecuted())
            return;
        if (m_computerBlack && m_computerWhite)
        {
            if (m_board.bothPassed() || m_resigned)
            {
                if (m_auto)
                {
                    newGame(m_boardSize);
                    checkComputerMove();
                    return;
                }
                else
                {
                    showInfo("Game finished");
                    computerNone();
                }
                return;
            }
            else
                generateMove();
        }
        else
        {
            if (computerToMove() && ! m_resigned)
                generateMove();
        }
        m_timeControl.startMove(m_board.getToMove());
    }

    private boolean checkCurrentNodeExecuted()
    {
        if (m_commandThread == null)
            return true;
        if (! isCurrentNodeExecuted())
        {
            showError("Cannot go forward from\n" +
                      "partially executed game node");
            return false;
        }
        return true;
    }

    private boolean checkSaveGame()
    {
        int result =
            JOptionPane.showConfirmDialog(this, "Save game?", "Question",
                                          JOptionPane.YES_NO_CANCEL_OPTION);
        switch (result)
        {
        case 0:
            return saveDialog();
        case 1:
            m_needsSave = false;
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

    private void close()
    {
        if (m_setupMode)
            setupDone();
        if (m_needsSave && ! checkSaveGame())
            return;
        saveSession();
        if (m_commandThread != null)
            detachProgram();
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
        assert(m_commandThread != null);
        endLengthyCommand();
        if (m_beepAfterMove)
            java.awt.Toolkit.getDefaultToolkit().beep();
        try
        {
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            m_timeControl.stopMove();
            String response = m_commandThread.getResponse();
            go.Color toMove = m_board.getToMove();
            addPlayerComputerToGameInfo(toMove);
            if (response.toLowerCase().equals("resign"))
            {
                if (! (m_computerBlack && m_computerWhite))
                    showInfo("Computer resigns");
                m_resigned = true;
                setResult((toMove == go.Color.BLACK ? "W" : "B") + "+Resign");
            }
            else
            {
                go.Point p = Gtp.parsePoint(response, m_boardSize);
                Move move = new Move(p, toMove);
                m_needsSave = true;
                m_board.play(move);
                Node node = new Node(move);
                m_currentNode.append(node);
                m_currentNode = node;
                m_currentNodeExecuted = 1;
                if (move.getPoint() == null
                    && ! (m_computerBlack && m_computerWhite))
                    showInfo("Computer passed");
                fileModified();
                m_resigned = false;
            }
            boardChangedBegin(true, true);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
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
        if (m_commandInProgress)
        {
            if (! showQuestion("Kill program?"))
                return;
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
                    m_commandThread.sendCommand("quit");
                }
                catch (Gtp.Error e)
                {
                }
                m_commandThread.close();
            }
        }
        m_commandThread = null;
        m_toolBar.setComputerEnabled(false);
        m_menuBar.setComputerEnabled(false);
        m_gtpShell.dispose();
        m_gtpShell = null;
        if (m_analyzeDialog != null)
        {
            m_analyzeDialog.dispose();
            m_analyzeDialog = null;
        }
        setTitle();
        // If this node was only partially executed due to a error of the Go
        // program, we undo and execute it again
        if (! isCurrentNodeExecuted())
        {
            try
            {
                undoCurrentNode();
                executeCurrentNode();
            }
            catch (Gtp.Error e)
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
        m_gtpShell.setCommandInProgess(false);
        m_commandInProgress = false;
        if (m_analyzeCommand != null
            && (m_analyzeCommand.needsPointArg()
                || m_analyzeCommand.needsPointListArg()))
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
        else
            setBoardCursorDefault();
    }

    private void executeCurrentNode() throws Gtp.Error
    {
        m_currentNodeExecuted = 0;
        Vector moves = m_currentNode.getAddStonesAndMoves();
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
            catch (Gtp.Error error)
            {
                showGtpError(error);
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
        catch (Gtp.Error error)
        {
            showGtpError(error);
            return false;
        }
        return true;
    }

    private void fileModified()
    {
        if (m_loadedFile != null)
        {
            m_loadedFile = null;
            setTitle();
        }
    }

    private void forward(int n)
    {
        if (! checkCurrentNodeExecuted())
            return;
        setFastUpdate(true);
        try
        {
            for (int i = 0; i < n && m_currentNode.getNumberChildren() > 0;
                 ++i)
            {
                m_currentNode = m_currentNode.getChild();
                executeCurrentNode();
            }
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
        setFastUpdate(false);
    }

    private void generateMove()
    {
        go.Color toMove = m_board.getToMove();
        String command = m_commandThread.getCommandGenmove(toMove);
        showStatus("Computer is thinking ...");
        Runnable callback = new Runnable()
            {
                public void run() { computerMoved(); }
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
            try
            {
                setFastUpdate(true);
                m_needsSave = true;
                play(move);
            }
            finally
            {
                setFastUpdate(false);
            }
            if (point != null)
            {
                m_guiBoard.updateFromGoBoard(point);
                m_guiBoard.paintImmediately(point);
            }
            m_timeControl.stopMove();
            go.Color color = move.getColor();
            if (m_board.getMoveNumber() > 0
                && m_timeControl.lostOnTime(color)
                && ! m_lostOnTimeShown)
            {
                showInfo(color.toString() + " lost on time.");
                m_lostOnTimeShown = true;
            }
            m_resigned = false;
            boardChangedBegin(true, true);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void initGame(int size)
    {
        if (size != m_boardSize)
        {
            m_boardSize = size;
            m_guiBoard.initSize(size);
            m_squareLayout.setPreferMultipleOf(2 + 2 * size);
            pack();
            restoreMainWindow();
            if (m_gtpShell != null)
                restoreSize(m_gtpShell, "window-gtpshell", m_boardSize);
            if (m_analyzeDialog != null)
                restoreSize(m_analyzeDialog, "window-analyze", m_boardSize);
            if (m_gameTreeViewer != null)
                restoreSize(m_gameTreeViewer, "window-gametree", m_boardSize);
        }
        Vector handicap = m_board.getHandicapStones(m_handicap);
        if (handicap == null)
            showWarning("Handicap stone locations are not\n" +
                        "defined for this board size.");
        m_gameTree = new GameTree(size, m_prefs.getFloat("komi"), handicap,
                                  m_prefs.getString("rules"));
        m_board.newGame();        
        m_guiBoard.updateFromGoBoard();
        resetBoard();
        m_timeControl.reset();
        m_timeControl.startMove(go.Color.BLACK);
        m_lostOnTimeShown = false;
        m_needsSave = false;
        m_resigned = false;
    }

    private void initialize()
    {
        File file = null;
        if (! m_file.equals(""))
            newGameFile(m_boardSize, new File(m_file), m_move);
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
        if (m_prefs.getBool("show-gametree"))
            cbShowGameTree();
        if (! m_initAnalyze.equals(""))
        {
            AnalyzeCommand analyzeCommand =
                AnalyzeCommand.get(this, m_initAnalyze);
            if (analyzeCommand == null)
                showError("Unknown analyze command \"" + m_initAnalyze + "\"");
            else
                initAnalyzeCommand(analyzeCommand, true);
        }
        updateGameInfo(true);
        setVisible(true);
        m_guiBoard.setFocus();
        setTitleFromProgram();
    }

    private void initScore(go.Point[] isDeadStone)
    {
        resetBoard();
        m_guiBoard.scoreBegin(isDeadStone);
        m_guiBoard.repaint();
        m_scoreMode = true;
        if (m_scoreDialog == null)
        {
            m_scoreDialog = new ScoreDialog(this, this);
            m_scoreDialog.setLocationRelativeTo(this);
            Dimension size = getSize();
            m_scoreDialog.setLocation(size.width, 0);
        }
        float komi = m_gameTree.getGameInformation().m_komi;
        m_scoreDialog.showScore(m_board.scoreGet(komi, getRules()));
        m_scoreDialog.setVisible(true);
        m_menuBar.setScoreMode();
        showStatus("Please mark dead groups.");
    }

    private boolean isCurrentNodeExecuted()
    {
        int numberAddStonesAndMoves =
            m_currentNode.getNumberAddStonesAndMoves();
        if (! m_isRootExecuted
            || m_currentNodeExecuted != numberAddStonesAndMoves)
            return false;
        return true;
    }

    private void loadFile(File file, int move)
    {
        try
        {
            m_menuBar.addRecent(file);
            m_menuBar.saveRecent();
            java.io.Reader fileReader = new FileReader(file);
            sgf.Reader reader = new sgf.Reader(fileReader, file.toString());
            GameInformation gameInformation =
                reader.getGameTree().getGameInformation();
            initGame(gameInformation.m_boardSize);
            m_gameTree = reader.getGameTree(); 
            executeRoot();
            if (move > 0)
                forward(move);            
            m_loadedFile = file;
            setTitle();
            SimpleDialogs.setLastFile(file);
            computerNone();
            boardChangedBegin(false, true);
        }
        catch (FileNotFoundException e)
        {
            showError("File not found:\n" + file);
        }
        catch (sgf.Reader.Error e)
        {
            showError("Could not read file.", e);
        }
    }

    private void newGame(int size)
    {
        initGame(size);
        executeRoot();
        updateGameInfo(true);
        m_guiBoard.updateFromGoBoard();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_gameTree, m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        setTitleFromProgram();
    }

    private void newGameFile(int size, File file, int move)
    {
        initGame(size);
        loadFile(new File(m_file), move);
        m_timeControl.halt();
        updateGameInfo(true);
        m_guiBoard.updateFromGoBoard();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_gameTree, m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
    }

    private void play(Move move) throws Gtp.Error
    {
        if (! checkCurrentNodeExecuted())
            return;
        Node node = new Node(move);
        m_currentNode.append(node);
        m_currentNode = node;
        try
        {
            executeCurrentNode();
        }
        catch (Gtp.Error error)
        {
            m_currentNode = node.getFather();
            m_currentNode.removeChild(node);
            throw error;
        }
    }

    private void resetBoard()
    {
        clearStatus();
        m_guiBoard.resetBoard();
        m_guiBoard.repaint();
    }
    
    private void restoreMainWindow()
    {
        restoreSize(this, "window-gogui", m_boardSize);
        String name = "splitpane-position-" + m_boardSize;
        if (m_prefs.contains(name))
        {
            int dividerLocation = m_prefs.getInt(name);
            m_splitPane.setDividerLocation(dividerLocation);
        }
    }

    private void restoreSize(Window window, String name, int size)
    {
        name = name + "-" + size;
        if (! m_prefs.contains(name))
            return;
        String[] tokens = StringUtils.tokenize(m_prefs.getString(name));
        if (tokens.length < 4)
            return;
        try
        {
            int x = Integer.parseInt(tokens[0]);
            int y = Integer.parseInt(tokens[1]);
            int width = Integer.parseInt(tokens[2]);
            int height = Integer.parseInt(tokens[3]);
            if (window instanceof GtpShell)
                ((GtpShell)window).setFinalSize(x, y, width, height);
            else
            {
                window.setBounds(x, y, width, height);
                window.validate();
            }
        }
        catch (NumberFormatException e)
        {
        }
    }

    private void runLengthyCommand(String cmd, Runnable callback)
    {
        assert(m_commandThread != null);
        beginLengthyCommand();
        m_commandThread.sendCommand(cmd, callback);
    }

    private void save(File file) throws FileNotFoundException
    {
        String gameComment = null;
        if (m_commandThread != null)
            gameComment =
                "Program command:\n" + m_commandThread.getProgramCommand();
        OutputStream out = new FileOutputStream(file);
        if (FileUtils.hasExtension(file, "tex"))
        {
            String title = FileUtils.removeExtension(new File(file.getName()),
                                                     "tex");
            new latex.Writer(title, out, m_gameTree, false, null, null, null);
        }
        else
        {
            new sgf.Writer(out, m_board, m_gameTree, file, "GoGui",
                           Version.get(), gameComment);
            m_menuBar.addRecent(file);
            m_menuBar.saveRecent();
        }
    }

    private boolean saveDialog()
    {
        try
        {
            File file;
            while (true)
            {
                file = SimpleDialogs.showSaveSgf(this);
                if (file == null)
                    return false;
                if (file.exists())
                    if (! showQuestion("Overwrite " + file + "?"))
                        continue;
                break;
            }
            save(file);
            m_loadedFile = file;
            setTitle();
            m_needsSave = false;
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
        saveSize(this, "window-gogui");
        if (m_help != null)
            saveSize(m_help, "window-help");
        if (m_gameTreeViewer != null)
            saveSizeAndVisible(m_gameTreeViewer, "gametree");
        if (m_commandThread != null)
        {
            saveSizeAndVisible(m_gtpShell, "gtpshell");
            saveSizeAndVisible(m_analyzeDialog, "analyze");
        }
        if (GuiUtils.isNormalSizeMode(this))
        {
            String name = "splitpane-position-" + m_boardSize;
            m_prefs.setInt(name, m_splitPane.getDividerLocation());
        }
    }

    private void saveSize(JFrame window, String name)
    {
        if (! GuiUtils.isNormalSizeMode(window))
            return;
        name = name + "-" + m_boardSize;
        java.awt.Point location = window.getLocation();
        Dimension size = window.getSize();
        String value = Integer.toString(location.x) + " " + location.y
            + " " + size.width + " " + size.height;
        m_prefs.setString(name, value);
    }

    private void saveSizeAndVisible(JFrame window, String name)
    {
        if (window != null)
            saveSize(window, "window-" + name);
        boolean isVisible = (window != null && window.isVisible());
        m_prefs.setBool("show-" + name, isVisible);
    }

    private void sendGtpString(String commands)
    {        
        commands = commands.replaceAll("\\\\n", "\n");
        m_gtpShell.sendGtp(new StringReader(commands));
    }

    private void sendInterrupt()
    {
        if (! m_commandInProgress)
            return;
        showStatus("Interrupting ...");
        try
        {
            m_commandThread.sendInterrupt();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
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

    private void setFastUpdate(boolean fastUpdate)
    {
        if (m_commandThread != null)
        {
            assert(m_commandThread.getFastUpdate() != fastUpdate);
            m_commandThread.setFastUpdate(fastUpdate);
            m_gtpShell.setFastUpdate(fastUpdate);
        }
    }

    private void setKomi(float komi)
    {
        if (m_commandThread == null)
            return;
        try
        {
            if (m_commandThread.isCommandSupported("komi"))
                m_commandThread.sendCommand("komi " + komi);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }
    
    private static void setPrefsDefaults(Preferences prefs)
    {
        prefs.setBoolDefault("beep-after-move", true);
        prefs.setIntDefault("boardsize", 19);
        prefs.setFloatDefault("komi", 0);
        prefs.setStringDefault("rules", "Chinese");
        prefs.setBoolDefault("show-analyze", false);
        prefs.setBoolDefault("show-gtpshell", false);
        prefs.setBoolDefault("show-gametree", false);
        prefs.setBoolDefault("show-cursor", true);
        prefs.setBoolDefault("show-last-move", false);
    }

    private void setResult(String result)
    {
        String oldResult = m_gameTree.getGameInformation().m_result;
        if (! (oldResult == null || oldResult.equals("")
               || oldResult.equals(result))
            && ! showQuestion("Overwrite old result " + oldResult + "\n" +
                              "with new score " + result + "?"))
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
        catch (Gtp.Error e)
        {
        }
    }

    private void setTimeSettings()
    {
        if (m_commandThread == null)
            return;
        if (! m_timeControl.isInitialized())
            return;
        if (! m_commandThread.isCommandSupported("time_settings"))
        {
            showError("Command time_settings not supported");
            return;
        }
        long preByoyomi = m_timeControl.getPreByoyomi() * 60;
        long byoyomi = 0;
        long byoyomiMoves = 0;
        if (m_timeControl.getUseByoyomi())
        {
            byoyomi = m_timeControl.getByoyomi() * 60;
            byoyomiMoves = m_timeControl.getByoyomiMoves();
        }
        try
        {
            m_commandThread.sendCommand("time_settings " + preByoyomi + " "
                                        + byoyomi + " " + byoyomiMoves);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void setTitle()
    {
        String appName = "GoGui";        
        if (m_commandThread != null && ! m_name.equals(""))
            appName = StringUtils.capitalize(m_name);
        if (m_gtpShell != null)
            m_gtpShell.setAppName(appName);
        if (m_analyzeDialog != null)
            m_analyzeDialog.setAppName(appName);
        String gameName = null;
        GameInformation gameInformation = m_gameTree.getGameInformation();
        String playerBlack = gameInformation.m_playerBlack;
        String playerWhite = gameInformation.m_playerWhite;
        if (playerBlack != null && ! playerBlack.trim().equals("")
            && playerWhite != null && ! playerWhite.trim().equals(""))
        {
            playerBlack = StringUtils.capitalize(playerBlack);
            playerWhite = StringUtils.capitalize(playerWhite);
            String blackRank = gameInformation.m_blackRank;
            String whiteRank = gameInformation.m_whiteRank;
            if (blackRank != null && ! blackRank.trim().equals(""))
                playerBlack = playerBlack + " [" + blackRank + "]";
            if (whiteRank != null && ! whiteRank.trim().equals(""))
                playerWhite = playerWhite + " [" + whiteRank + "]";
            gameName = playerBlack + " vs " + playerWhite;
        }
        else if (m_loadedFile != null)
            gameName= m_loadedFile.getName();
        if (gameName != null)
            setTitle(gameName + " - " + appName);
        else
            setTitle(appName);        
    }

    private void setTitleFromProgram()
    {
        if (m_commandThread == null)
            return;
        if (m_commandThread.isCommandSupported("gogui_title"))
        {
            try
            {
                String title = m_commandThread.sendCommand("gogui_title");
                setTitle(title);
            }
            catch (Gtp.Error e)
            {
            }
        }
    }

    private void setup(Move move) throws Gtp.Error
    {
        if (m_commandThread != null)
            m_commandThread.sendCommandPlay(move);
        m_board.setup(move);
    }

    private void setupDone()
    {
        setFastUpdate(true);
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
        m_gameTree = new GameTree(size, m_prefs.getFloat("komi"), null,
                                  m_prefs.getString("rules"));
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
        if (m_board.getToMove() != toMove)
            m_currentNode.setToMove(toMove);
        executeRoot();
        fileModified();
        updateGameInfo(true);
        boardChangedBegin(false, false);
        setFastUpdate(false);
    }

    private void showError(String message, Exception e)
    {
        SimpleDialogs.showError(this, message, e);
    }

    private void showError(String message)
    {
        SimpleDialogs.showError(this, message);
    }

    private void showGtpError(Gtp.Error e)
    {
        showGtpError(this, e);
    }

    private void showGtpError(Component frame, Gtp.Error e)
    {        
        String message = e.getMessage().trim();
        if (message.length() == 0)
            message = "Command failed";
        else
            message = StringUtils.capitalize(message);
        SimpleDialogs.showError(frame, message);
    }

    private void showInfo(String message)
    {
        SimpleDialogs.showInfo(this, message);
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
                   + " (last point with right button or Ctrl key down).");
    }

    private void showStatusSelectTarget()
    {
        showStatus("Select a target for " + m_analyzeCommand.getLabel() + ".");
    }

    private void showWarning(String message)
    {
        SimpleDialogs.showWarning(this, message);
    }

    private void undoCurrentNode() throws Gtp.Error
    {
        for ( ; m_currentNodeExecuted > 0; --m_currentNodeExecuted)
        {
            if (m_commandThread != null)
                m_commandThread.sendCommand("undo");
            m_board.undo();
        }
    }

    private void updateGameInfo(boolean gameTreeChanged)
    {
        m_gameInfo.update(m_currentNode, m_board);
        if (m_gameTreeViewer != null)
        {
            if (gameTreeChanged)
                m_gameTreeViewer.update(m_gameTree, m_currentNode);
            else
                m_gameTreeViewer.update(m_currentNode);
        }
        m_comment.setNode(m_currentNode);
    }
}

//-----------------------------------------------------------------------------
