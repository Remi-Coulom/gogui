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
    implements ActionListener, AnalyzeCallback, Board.Listener,
               GtpShell.Callback, WindowListener
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
        m_rememberWindowSizes = prefs.getBool("remember-window-sizes");
        m_file = file;
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_move = move;
        m_computerBlack = computerBlack;
        m_computerWhite = computerWhite;
        m_auto = auto;
        m_verbose = verbose;
        m_initAnalyze = initAnalyze;

        if (program != null)
        {
            program = program.trim();
            if (! program.equals(""))
            {
                m_program = program;
                m_gtpShell = new GtpShell(null, "GoGui", this, prefs);
                m_gtpShell.setProgramCommand(program);
            }
        }

        Container contentPane = getContentPane();        

        m_infoPanel = new JPanel();
        m_infoPanel.setLayout(new BoxLayout(m_infoPanel, BoxLayout.Y_AXIS));
        m_infoPanel.setBorder(utils.GuiUtils.createSmallEmptyBorder());
        m_comment = new Comment();
        m_infoPanel.add(m_comment);
        m_timeControl = new TimeControl();
        Dimension pad = new Dimension(0, utils.GuiUtils.PAD);
        m_gameInfo = new GameInfo(m_timeControl);
        m_infoPanel.add(m_gameInfo);
        m_infoPanel.add(Box.createRigidArea(pad));
        m_infoPanel.add(createStatusBar());

        m_board = new go.Board(m_boardSize);
        m_board.setKomi(prefs.getFloat("komi"));
        m_board.setRules(prefs.getInt("rules"));

        m_gameTree = new GameTree(m_boardSize);
        m_currentNode = m_gameTree.getRoot();
        m_currentNodeExecuted = 0;

        m_guiBoard = new Board(m_board);
        m_guiBoard.setListener(this);
        m_toolBar = new ToolBar(this, prefs);
        contentPane.add(m_toolBar, BorderLayout.NORTH);

        contentPane.add(m_infoPanel, BorderLayout.SOUTH);

        m_squareLayout = new SquareLayout();
        m_squareLayout.setPreferMultipleOf(m_boardSize + 2);
        m_boardPanel = new JPanel(m_squareLayout);
        m_boardPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        m_boardPanel.add(m_guiBoard);
        contentPane.add(m_boardPanel, BorderLayout.CENTER);
        
        addWindowListener(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(new GoIcon());

        m_menuBar = new MenuBar(this);
        m_menuBar.selectBoardSizeItem(m_boardSize);
        m_menuBar.selectRulesItem(m_board.getRules());
        m_menuBar.setBeepAfterMove(m_beepAfterMove);
        m_menuBar.setRememberSizes(m_rememberWindowSizes);
        setJMenuBar(m_menuBar.getMenuBar());
        if (m_program == null)
        {
            m_toolBar.disableComputerButtons();
            m_menuBar.disableComputer();
        }
        m_menuBar.setNormalMode();

        pack();
        m_guiBoard.requestFocusInWindow();
        setTitle("GoGui");
        if (m_rememberWindowSizes)
            restoreSize(this, "window-gogui", m_boardSize);
        ++m_instanceCount;
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
            && ! command.equals("remember-sizes")
            && ! command.equals("show-last-move")
            && ! command.equals("exit"))
            return;
        if (command.equals("about"))
            cbShowAbout();
        else if (command.equals("analyze"))
            cbAnalyze();
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
        else if (command.equals("end"))
            cbEnd();
        else if (command.equals("exit"))
            close();
        else if (command.equals("forward"))
            cbForward(1);
        else if (command.equals("forward-10"))
            cbForward(10);
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
        else if (command.equals("komi"))
            cbKomi();
        else if (command.equals("next-variation"))
            cbVariation(true);
        else if (command.equals("new-game"))
            cbNewGame(m_boardSize);
        else if (command.equals("open"))
            cbOpen();
        else if (command.equals("open-recent"))
            cbOpenRecent();
        else if (command.equals("open-with-program"))
            cbOpenWithProgram();
        else if (command.equals("pass"))
            cbPass();
        else if (command.equals("play"))
            cbPlay();
        else if (command.equals("previous-variation"))
            cbVariation(false);
        else if (command.equals("print"))
            cbPrint();
        else if (command.equals("remember-sizes"))
            cbRememberSizes();
        else if (command.equals("rules-chinese"))
            cbRules(go.Board.RULES_CHINESE);
        else if (command.equals("rules-japanese"))
            cbRules(go.Board.RULES_JAPANESE);
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
                new AnalyzeDialog(null, this, m_prefs,
                                  m_commandThread.getSupportedCommands());
            if (m_rememberWindowSizes)
                restoreSize(m_analyzeDialog, "window-analyze", m_boardSize);
            setTitle();
        }
        m_analyzeDialog.toTop();
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
            updateGameInfo();
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
            m_scoreDialog.showScore(m_board.scoreGet());
            return;
        }
        else if (! modifiedSelect)
            humanMoved(new Move(p, m_board.getToMove()));
    }

    public static void main(String[] args)
    {
        try
        {
            String s = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(s);
        }
        catch (Exception e)
        {
        }
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
                "gtpshell",
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
            if (rules.equals("chinese"))
                prefs.setInt("rules", go.Board.RULES_CHINESE);
            else if (rules.equals("japanese"))
                prefs.setInt("rules", go.Board.RULES_JAPANESE);
            else if (! rules.equals(""))
                throw new Exception("Invalid rules argument \""
                                    + rules + "\"");
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
            else
                program = SelectProgram.select(null);
            
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
            String msg = e.getMessage();
            if (msg == null)
                msg = e.getClass().getName();
            SimpleDialogs.showError(null, msg);
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            SimpleDialogs.showError(null, msg);
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

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        close();
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
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

    private class NewGameContinue
        implements Runnable
    {
        public NewGameContinue(int size)
        {
            m_size = size;
        }

        public void run()
        {
            newGameContinue(m_size);
        }
        
        private int m_size;
    }

    private boolean m_analyzeAutoRun;

    private boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    private boolean m_commandInProgress;

    private boolean m_lostOnTimeShown;

    private boolean m_needsSave;

    private boolean m_rememberWindowSizes;

    private boolean m_resigned;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_verbose;

    private int m_boardSize;

    private int m_currentNodeExecuted;

    private int m_handicap;

    private static int m_instanceCount;

    private int m_move;

    private go.Board m_board;

    private go.Color m_setupColor;

    private go.Score m_score;

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

    /** Preferences.
        Preferences are shared between instances created with
        "Open with program", the last instance of GoGui saves them.
    */
    private Preferences m_prefs;

    private ScoreDialog m_scoreDialog;

    private SquareLayout m_squareLayout;

    private TimeControl m_timeControl;

    private ToolBar m_toolBar;

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

    private void backward(int n)
    {
        setFastUpdate(true);
        try
        {
            for (int i = 0; i < n && m_currentNode.getFather() != null; ++i)
            {
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
            return;
        }
        finally
        {
            setFastUpdate(false);
        }
        boardChangedBegin(false);
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

    private void boardChangedBegin(boolean doCheckComputerMove)
    {
        m_guiBoard.updateFromGoBoard();
        updateGameInfo();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        clearStatus();
        if (m_commandThread != null
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
    }

    private void cbBackward(int n)
    {
        backward(n);
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
            showError("Invalid board size.");
            return;
        }
        cbNewGame(boardSize);
    }
    
    private void cbComputerBoth()
    {
        computerBoth();
        checkComputerMove();
    }

    private void cbEnd()
    {
        forward(m_currentNode.getNodesLeft());
        boardChangedBegin(false);
    }

    private void cbForward(int n)
    {
        forward(n);
        boardChangedBegin(false);
    }

    private void cbGoto()
    {
        String value = JOptionPane.showInputDialog(this, "Move number");
        if (value == null || value.equals(""))
            return;
        try
        {
            int moveNumber = Integer.parseInt(value);
            if (moveNumber < 0 || moveNumber > m_board.getNumberSavedMoves())
            {
                showError("Invalid move number.");
                return;
            }
            if (moveNumber < m_board.getMoveNumber())
                backward(m_board.getMoveNumber() - moveNumber);
            else
                forward(moveNumber - m_board.getMoveNumber());
            boardChangedBegin(false);
        }
        catch (NumberFormatException e)
        {
            showError("Invalid move number.");
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
            showError("Help not found.");
            return;
        }
        if (m_help == null)
        {
            m_help = new Help(null, u);
            if (m_rememberWindowSizes)
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

    private void cbKomi()
    {
        Object obj =
            JOptionPane.showInputDialog(this, "Komi value",
                                        "GoGui: Set komi",
                                        JOptionPane.PLAIN_MESSAGE,
                                        null, null,
                                        Float.toString(m_board.getKomi()));
        if (obj == null)
            return;
        float komi = Float.parseFloat((String)obj);
        m_board.setKomi(komi);
        m_prefs.setFloat("komi", komi);
        setKomi();
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

    private void cbOpenWithProgram()
    {
        try
        {
            String program = SelectProgram.select(this);
            if (program == null)
                return;
            File file = File.createTempFile("gogui-", "*.sgf");
            save(file);
            GoGui gui = new GoGui(program, m_prefs, file.toString(),
                                  m_board.getMoveNumber(), null,
                                  m_verbose, false, false, false, "", "", "");

        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            showError(msg);
        }
    }

    private void cbPass()
    {
        humanMoved(new Move(null, m_board.getToMove()));
    }

    private void cbPlay()
    {
        if (m_board.getToMove() == go.Color.BLACK)
            computerBlack();
        else
            computerWhite();
        generateMove();
        m_timeControl.startMove(m_board.getToMove());
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
                showError("Printing failed.", e);
            }
            showInfo("Printing done.");
        }
    }

    private void cbRememberSizes()
    {
        m_rememberWindowSizes = m_menuBar.getRememberSizes();
        m_prefs.setBool("remember-window-sizes", m_rememberWindowSizes);
    }

    private void cbRules(int rules)
    {
        m_board.setRules(rules);
        m_prefs.setInt("rules", rules);
        setRules();
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
            showInfo("Position saved.");
            if (m_board.getNumberSavedMoves() == 0)
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
            showStatus("Scoring...");
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
        if (m_scoreDialog != null)
        {
            m_scoreDialog.dispose();
            m_scoreDialog = null;
        }
        if (accepted)
            m_score = m_board.scoreGet();
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
            m_menuBar.setSetupMode();
            if (m_needsSave && ! checkSaveGame())
            {
                m_menuBar.setNormalMode();
                return;
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
        updateGameInfo();
    }

    private void cbSetupWhite()
    {
        showStatus("Setup white.");
        m_setupColor = go.Color.WHITE;
        m_board.setToMove(m_setupColor);
        updateGameInfo();
    }

    private void cbShowAbout()
    {
        String message =
            "GoGui " + Version.get() + "\n" +
            "\n" +
            "Graphical interface to programs that play the game of Go.\n" +
            "Copyright 2003-2004, Markus Enzenberger\n" +
            "http://gogui.sourceforge.net\n";
        SimpleDialogs.showAbout(this, message);
    }

    private void cbShowCursor()
    {
        m_guiBoard.setShowCursor(m_menuBar.getShowCursor());
        m_guiBoard.repaint();
    }

    private void cbShowGameTree()
    {
        if (m_gameTreeViewer == null)
        {
            m_gameTreeViewer = new GameTreeViewer(null);
            if (m_rememberWindowSizes)
                restoreSize(m_gameTreeViewer, "window-gametree", m_boardSize);
        }
        m_gameTreeViewer.toTop();
    }

    private void cbShowLastMove()
    {
        m_guiBoard.setShowLastMove(m_menuBar.getShowLastMove());
    }

    private void cbTruncate()
    {
        int numberSavedMoves = m_board.getNumberSavedMoves();
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber == numberSavedMoves)
        {
            showError("No moves to truncate.");
            return;
        }
        if (moveNumber == numberSavedMoves - 1)
        {
            if (! showQuestion("Truncate move " + (moveNumber + 1) + "?"))
                return;
        }
        else if (! showQuestion("Truncate moves " + (moveNumber + 1) + " to "
                                + numberSavedMoves + "?"))
            return;
        m_board.truncate();
        updateGameInfo();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_currentNode);
    }

    private void cbVariation(boolean forward)
    {
        if (m_currentNode.getFather() == null
            || m_currentNode.getFather().getNumberChildren() < 2)
            return;
        setFastUpdate(true);
        try
        {
            undoCurrentNode();
            computerNone();
            Node oldChild = m_currentNode;
            m_currentNode = m_currentNode.getFather();
            m_currentNodeExecuted = m_currentNode.getNumberAddStonesAndMoves();
            for (int i = 0; i < m_currentNode.getNumberChildren(); ++i)
                if (m_currentNode.getChild(i) == oldChild)
                {
                    if (forward)
                    {
                        ++i;
                        if (i >= m_currentNode.getNumberChildren())
                            i = 0;
                    }
                    else
                    {
                        --i;
                        if (i < 0)
                            i = m_currentNode.getNumberChildren() - 1;
                    }
                    m_currentNode = m_currentNode.getChild(i);
                    break;
                }
            executeCurrentNode();
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
        boardChangedBegin(false);
    }

    private void checkComputerMove()
    {
        if (m_commandThread == null)
            return;
        if (m_computerBlack && m_computerWhite)
        {
            if (m_board.bothPassed() || m_resigned)
            {
                if (m_auto)
                {
                    newGame(m_boardSize);
                    return;
                }
                else
                {
                    showInfo("Game finished.");
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

    private boolean checkSaveGame()
    {
        int result =
            JOptionPane.showConfirmDialog(this, "Save game?",
                                          "GoGui: Question",
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
        saveSession();
        dispose();
        assert(m_instanceCount > 0);
        if (--m_instanceCount == 0)
        {
            m_prefs.save();
            System.exit(0);
        }
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
            if (response.toLowerCase().equals("resign"))
            {
                if (! (m_computerBlack && m_computerWhite))
                    showInfo("The computer resigns.");
                m_resigned = true;
            }
            else
            {
                go.Point p = Gtp.parsePoint(response, m_boardSize);
                go.Color toMove = m_board.getToMove();
                Move move = new Move(p, toMove);
                if (m_board.willModifyGame(move))
                    m_needsSave = true;
                m_board.play(move);
                Node node = new Node(move);
                m_currentNode.append(node);
                m_currentNode = node;
                m_currentNodeExecuted = 1;
                if (move.getPoint() == null
                    && ! (m_computerBlack && m_computerWhite))
                    showInfo("The computer passed.");
                fileModified();
                m_resigned = false;
            }
            boardChangedBegin(true);
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
        JPanel panel = new JPanel(new GridLayout(1, 0));
        JLabel label = new JLabel();
        label.setBorder(BorderFactory.createLoweredBevelBorder());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        m_statusLabel = label;
        clearStatus();
        return panel;
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
        if (m_currentNodeExecuted !=
            m_currentNode.getNumberAddStonesAndMoves())
        {
            showError("Cannot go forward from partially executed game node.");
            return;
        }
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
        showStatus("Computer is thinking...");
        Runnable callback = new Runnable()
            {
                public void run() { computerMoved(); }
            };
        runLengthyCommand(command, callback);
    }

    private void humanMoved(Move move)
    {
        try
        {
            go.Point p = move.getPoint();
            if (p != null && m_board.getColor(p) != go.Color.EMPTY)
                    return;
            try
            {
                setFastUpdate(true);
                if (m_board.getNumberSavedMoves() == m_board.getMoveNumber()
                    || m_board.willModifyGame(move))
                    m_needsSave = true;
                play(move);
            }
            finally
            {
                setFastUpdate(false);
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
            boardChangedBegin(true);            
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
            m_squareLayout.setPreferMultipleOf(size + 2);
            pack();
            if (m_rememberWindowSizes)
            {
                restoreSize(this, "window-gogui", m_boardSize);
                if (m_gtpShell != null)
                    restoreSize(m_gtpShell, "window-gtpshell", m_boardSize);
                if (m_analyzeDialog != null)
                    restoreSize(m_analyzeDialog, "window-analyze",
                                m_boardSize);
            }
        }
        m_gameTree = new GameTree(size);
        m_currentNode = m_gameTree.getRoot();
        m_currentNodeExecuted = 0;
        m_board.newGame();        
        m_guiBoard.updateFromGoBoard();
        resetBoard();
        m_timeControl.reset();
        m_lostOnTimeShown = false;
        m_score = null;
        m_needsSave = false;
        m_resigned = false;
    }

    private void initialize()
    {
        try
        {
            m_toolBar.enableAll(true, m_currentNode);
            if (m_program != null)
            {
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
                                            + "See GTP shell for any error"
                                            + " messages\n"
                                            + "printed by the program.");
                }
            }
            if (m_commandThread != null)
            {
                if (m_rememberWindowSizes)
                    restoreSize(m_gtpShell, "window-gtpshell", m_boardSize);
                try
                {
                    setFastUpdate(true);
                    m_name = m_commandThread.sendCommand("name").trim();
                }
                finally
                {
                    setFastUpdate(false);
                }
                m_gtpShell.setProgramName(m_name);
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
                Vector supportedCommands =
                    m_commandThread.getSupportedCommands();
                m_gtpShell.setInitialCompletions(supportedCommands);
                if (! m_gtpFile.equals(""))
                    m_gtpShell.sendGtpFile(new File(m_gtpFile));
                if (! m_gtpCommand.equals(""))
                    sendGtpString(m_gtpCommand);
            }
            setTitle();
            setTitleFromProgram();
            if (m_commandThread == null
                || (! m_computerBlack && ! m_computerWhite))
                computerNone();
            else if (m_computerBlack && m_computerWhite)
                computerBoth();
            else  if (m_computerBlack)
                computerBlack();
            else
                computerWhite();
            setRules();
            if (m_prefs.getBool("show-gametree"))
                cbShowGameTree();
            File file = null;
            if (! m_file.equals(""))
                newGameFile(m_boardSize, new File(m_file), m_move);
            else
            {
                setKomi();
                newGame(m_boardSize);
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
            if (m_commandThread != null)
            {
                if (m_prefs.getBool("show-analyze"))
                    cbAnalyze();
                if (m_prefs.getBool("show-gtpshell"))
                    m_gtpShell.toTop();
            }
            setVisible(true);
            m_guiBoard.setFocus();
        }
        catch (Gtp.Error e)
        {
            setVisible(true);
            if (m_gtpShell != null)
                m_gtpShell.toTop();
            SimpleDialogs.showError(this,
                                    e.getMessage() + "\n"
                                    + "See GTP shell for any error messages\n"
                                    + "printed by the program.");
            showStatus(e.getMessage());            
        }
    }

    private void initScore(go.Point[] isDeadStone)
    {
        resetBoard();
        m_guiBoard.scoreBegin(isDeadStone);
        m_guiBoard.repaint();
        m_scoreMode = true;
        m_scoreDialog = new ScoreDialog(this, m_board.scoreGet());
        m_scoreDialog.setLocationRelativeTo(this);
        Dimension size = getSize();
        m_scoreDialog.setLocation(size.width, 0);
        m_scoreDialog.setVisible(true);
        m_menuBar.setScoreMode();
        showStatus("Please mark dead groups.");
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
            if (m_commandThread != null)
            {
                m_commandThread.sendCommandBoardsize(m_boardSize);
                m_commandThread.sendCommandClearBoard(m_boardSize);
            }
            m_board.setKomi(gameInformation.m_komi);
            setKomi();
            m_currentNode = m_gameTree.getRoot();
            executeCurrentNode();
            if (move > 0)
                forward(move);            
            m_loadedFile = file;
            setTitle();
            SimpleDialogs.setLastFile(file);
            computerNone();
            boardChangedBegin(false);
        }
        catch (FileNotFoundException e)
        {
            showError("File not found:\n" + file);
        }
        catch (sgf.Reader.Error e)
        {
            showError("Could not read file\n" + file, e);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void newGame(int size)
    {
        if (m_commandThread != null)
        {
            try
            {
                m_commandThread.sendCommandBoardsize(size);
            }
            catch (Gtp.Error e)
            {
                showGtpError(e);
                return;
            }
            String command = m_commandThread.getCommandClearBoard(size);
            Runnable callback = new NewGameContinue(size);
            showStatus("Starting new game...");
            beginLengthyCommand();
            m_commandThread.sendCommand(command, callback);
            return;
        }
        initGame(size);
        setHandicap();
        updateGameInfo();
        m_guiBoard.updateFromGoBoard();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
    }

    private void newGameContinue(int size)
    {
        endLengthyCommand();
        clearStatus();
        Gtp.Error e = m_commandThread.getException();
        if (e != null)
        {
            showGtpError(e);
            return;
        }
        setTitleFromProgram();
        initGame(size);
        setHandicap();
        setTimeSettings();
        updateGameInfo();
        m_guiBoard.updateFromGoBoard();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
        checkComputerMove();
    }

    private void newGameFile(int size, File file, int move)
    {
        initGame(size);
        loadFile(new File(m_file), move);
        updateGameInfo();
        m_guiBoard.updateFromGoBoard();
        m_toolBar.updateGameButtons(m_currentNode);
        m_menuBar.updateGameMenuItems(m_currentNode);
        m_menuBar.selectBoardSizeItem(m_board.getSize());
    }

    private void play(Move move) throws Gtp.Error
    {
        Node node = new Node(move);
        m_currentNode.append(node);
        m_currentNode = node;
        executeCurrentNode();
    }

    private void resetBoard()
    {
        clearStatus();
        m_guiBoard.resetBoard();
        m_guiBoard.repaint();
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
                window.setBounds(x, y, width, height);
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
        String playerBlack = null;
        String playerWhite = null;
        String gameComment = null;
        if (m_commandThread != null)
        {
            String name = m_name;
            if (m_version != null && ! m_version.equals(""))
                name = name +  ":" + m_version;
            if (m_computerBlack)
                playerBlack = name;
            if (m_computerWhite)
                playerWhite = name;
            gameComment =
                "Program command:\n" + m_commandThread.getProgramCommand();
        }
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
                           Version.get(), m_handicap, playerBlack, playerWhite,
                           gameComment, m_score);
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
            showInfo("Game saved.");
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
        if (m_rememberWindowSizes)
        {
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
        }
    }

    private void saveSize(Window window, String name)
    {
        if (window instanceof Frame)
        {
            int state = ((Frame)window).getExtendedState();
            int mask = Frame.MAXIMIZED_BOTH | Frame.MAXIMIZED_VERT
                | Frame.MAXIMIZED_HORIZ | Frame.ICONIFIED;
            if ((state & mask) != 0)
                return;
        }
        name = name + "-" + m_boardSize;
        java.awt.Point location = window.getLocation();
        Dimension size = window.getSize();
        String value = Integer.toString(location.x) + " " + location.y
            + " " + size.width + " " + size.height;
        m_prefs.setString(name, value);
    }

    private void saveSizeAndVisible(Window window, String name)
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
        showStatus("Interrupting...");
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

    private void setHandicap()
    {
        Vector handicap = m_board.getHandicapStones(m_handicap);
        if (handicap == null)
        {
            showWarning("Handicap stone locations are\n" +
                        "not defined for this board size.");
            return;
        }
        Vector moves = new Vector(handicap.size());
        for (int i = 0; i < handicap.size(); ++i)
        {
            go.Point p = (go.Point)handicap.get(i);
            moves.add(new Move(p, go.Color.BLACK));
        }
        try
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                setup(m);
            }
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void setKomi()
    {
        if (m_commandThread == null)
            return;
        try
        {
            if (m_commandThread.isCommandSupported("komi"))
                m_commandThread.sendCommand("komi " + m_board.getKomi());
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
        prefs.setBoolDefault("remember-window-sizes", true);
        prefs.setIntDefault("rules", go.Board.RULES_JAPANESE);
        prefs.setBoolDefault("show-analyze", false);
        prefs.setBoolDefault("show-gtpshell", false);
        prefs.setBoolDefault("show-gametree", false);
    }

    private void setRules()
    {
        if (m_commandThread == null)
            return;
        if (! m_commandThread.isCommandSupported("scoring_system"))
            return;
        try
        {
            int rules = m_board.getRules();
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
        if (! m_name.equals(""))
        {
            String title = StringUtils.formatTitle(m_name);
            if (m_loadedFile != null)
                setTitle(title + ": " + m_loadedFile.getName());
            else
                setTitle(title);
            if (m_gtpShell != null)
                m_gtpShell.setTitlePrefix(title);
            if (m_analyzeDialog != null)
                m_analyzeDialog.setTitlePrefix(title);
        }
        else if (m_loadedFile != null)
            setTitle(m_loadedFile.getName());
        else
            setTitle("GoGui");
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
        try
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
            if (m_commandThread != null)
            {
                m_commandThread.sendCommandBoardsize(size);
                m_commandThread.sendCommandClearBoard(size);
            }
            m_board.newGame();        
            m_gameTree = new GameTree(size);
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
            executeCurrentNode();
            fileModified();
            boardChangedBegin(false);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
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
            message = StringUtils.formatMessage(message);
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

    private void updateGameInfo()
    {
        m_gameInfo.update(m_currentNode, m_board);
        if (m_gameTreeViewer != null)
            m_gameTreeViewer.update(m_gameTree, m_currentNode);
        m_comment.setText(m_currentNode.getComment());
    }
}

//-----------------------------------------------------------------------------
