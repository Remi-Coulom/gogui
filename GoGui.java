//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import board.*;
import gtp.*;
import sgf.*;
import utils.*;

//-----------------------------------------------------------------------------

class GoGui
    extends JFrame
    implements ActionListener, AnalyzeCommand.Callback, Board.Listener,
               Gtp.StdErrCallback, GtpShell.Callback, WindowListener
{
    GoGui(String program, int size, String file, int move,
          String analyzeCommand, boolean gtpShell, String time,
          boolean verbose, boolean fillPasses, boolean computerNone)
        throws Gtp.Error, AnalyzeCommand.Error
    {
        m_boardSize = size;
        m_file = file;
        m_fillPasses = fillPasses;
        m_move = move;
        m_computerNoneOption = computerNone;
        m_verbose = verbose;

        if (program != null && ! program.equals(""))
        {
            // Must be created before m_gtp (stderr callback of Gtp!)
            m_gtpShell = new GtpShell(null, "GoGui", this);
            Gtp gtp = new Gtp(program, m_verbose);
            m_gtpShell.setProgramCommand(gtp.getProgramCommand());
            gtp.setStdErrCallback(this);
            m_commandThread = new CommandThread(gtp, m_gtpShell);
            m_commandThread.start();
        }

        Container contentPane = getContentPane();        
        m_toolBar = new ToolBar(this, analyzeCommand, this);
        contentPane.add(m_toolBar, BorderLayout.NORTH);
        m_timeControl = new TimeControl();
        m_board = new Board(m_boardSize);
        m_board.setListener(this);
        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new SquareLayout());
        boardPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        boardPanel.add(m_board);
        contentPane.add(boardPanel, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(utils.GuiUtils.createSmallEmptyBorder());
        Dimension pad = new Dimension(0, utils.GuiUtils.PAD);
        m_gameInfo = new GameInfo(m_board, m_timeControl);
        infoPanel.add(m_gameInfo);
        infoPanel.add(Box.createRigidArea(pad));
        infoPanel.add(createStatusBar());
        contentPane.add(infoPanel, BorderLayout.SOUTH);

        addWindowListener(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(new GoIcon());
        m_menuBars = new MenuBars(this);
        m_menuBars.selectBoardSizeItem(m_boardSize);
        setJMenuBar(m_menuBars.getNormalMenu());

        if (program == null || program.equals(""))
            m_toolBar.disableComputerButtons();

        pack();
        setVisible(true);
        ++m_instanceCount;
        if (m_gtpShell != null)
            DialogUtils.center(m_gtpShell, this);
        if (gtpShell && m_gtpShell != null)
        {
            m_gtpShell.toTop();
        }

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
            && ! command.equals("computer-black")
            && ! command.equals("computer-both")
            && ! command.equals("computer-none")
            && ! command.equals("computer-white")
            && ! command.equals("interrupt")
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
        else if (command.equals("beginning"))
            cbBeginning();
        else if (command.startsWith("board-size-"))
            cbNewGame(command.substring(new String("board-size-").length()));
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
        else if (command.equals("gtp-shell"))
            cbGtpShell();
        else if (command.startsWith("handicap-"))
            cbHandicap(command.substring(new String("handicap-").length()));
        else if (command.equals("help"))
            cbHelp();
        else if (command.equals("interrupt"))
            cbInterrupt();
        else if (command.equals("komi"))
            cbKomi();
        else if (command.equals("load"))
            cbLoad();
        else if (command.equals("new-game"))
            cbNewGame(m_boardSize);
        else if (command.equals("open-with-program"))
            cbOpenWithProgram();
        else if (command.equals("pass"))
            cbPass();
        else if (command.equals("play"))
            cbPlay();
        else if (command.equals("rules-chinese"))
            cbRules(Board.RULES_CHINESE);
        else if (command.equals("rules-japanese"))
            cbRules(Board.RULES_JAPANESE);
        else if (command.equals("save"))
            cbSave();
        else if (command.equals("save-position"))
            cbSavePosition();
        else if (command.equals("score"))
            cbScore();
        else if (command.equals("score-done"))
            cbScoreDone();
        else if (command.equals("setup"))
            cbSetup();
        else if (command.equals("setup-black"))
            cbSetupBlack();
        else if (command.equals("setup-done"))
            cbSetupDone();
        else if (command.equals("setup-white"))
            cbSetupWhite();
        else
            assert(false);
    }
    
    public void clearAnalyzeCommand()
    {
        m_analyzeCmd = null;
        if (m_analyzeRequestPoint)
        {
            m_analyzeRequestPoint = false;
            setBoardCursorDefault();
        }
        resetBoard();
        clearStatus();
    }

    public void fieldClicked(board.Point p)
    {
        if (m_commandInProgress)
            return;
        if (m_analyzeRequestPoint)
        {
            m_analyzePointArg = p;
            m_board.clearAllCrossHair();
            m_board.setCrossHair(p, true);
            analyzeBegin();
            return;
        }
        if (m_setupMode)
        {
            if (m_board.getColor(p) != m_setupColor)
                m_board.play(new Move(p, m_setupColor));
            else
                m_board.play(new Move(p, board.Color.EMPTY));
            return;
        }
        if (m_scoreMode)
        {
            m_board.scoreSetDead(p);
            return;
        }
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
        try
        {
            String options[] =
                {
                    "analyze:",
                    "computer-none",
                    "file:",
                    "fillpasses",
                    "gtpshell",
                    "help",
                    "move:",
                    "size:",
                    "time:",
                    "verbose"
                };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar GoGui.jar [options] program\n" +
                    "Graphical user interface for Go programs\n" +
                    "using the Go Text Protocol.\n" +
                    "\n" +
                    "  -analyze name   initialize analzye command\n" +
                    "  -computer-none  computer plays no side\n" +
                    "  -gtpshell       open GTP shell at startup\n" +
                    "  -file filename  load SGF file\n" +
                    "  -fillpasses     never send subsequent moves of\n" +
                    "                  the same color to the program\n" +
                    "  -help           display this help and exit\n" +
                    "  -move           load SGF file until move number\n" +
                    "  -size           set board size\n" +
                    "  -time           set time limits (min[+min/moves])\n" +
                    "  -verbose        print debugging messages\n";
                System.out.print(helpText);
                System.exit(0);
            }
            String analyzeCommand = opt.getString("analyze", null);
            boolean computerNone = opt.isSet("computer-none");
            String file = opt.getString("file", "");
            boolean fillPasses = opt.isSet("fillpasses");
            boolean gtpShell = opt.isSet("gtpshell");
            int move = opt.getInteger("move", -1);
            int size = opt.getInteger("size", 19);
            String time = opt.getString("time", null);
            boolean verbose = opt.isSet("verbose");
            Vector arguments = opt.getArguments();
            String program = null;
            if (arguments.size() == 1)
                program = (String)arguments.get(0);
            else if (arguments.size() > 1)
            {
                System.err.println("Only one program argument allowed.");
                System.exit(-1);
            }
            else
                program = SelectProgram.select(null);
            
            GoGui gui = new GoGui(program, size, file, move, analyzeCommand,
                                  gtpShell, time, verbose, fillPasses,
                                  computerNone);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            SimpleDialogs.showError(null, msg);
            t.printStackTrace();
            System.exit(-1);
        }
    }

    public void receivedStdErr(String s)
    {
        assert(m_gtpShell != null);
        Runnable r = new UpdateGtpShellStdErr(m_gtpShell, s);
        try
        {
            SwingUtilities.invokeAndWait(r);
        }
        catch (InterruptedException e)
        {
        }
        catch (java.lang.reflect.InvocationTargetException e)
        {
        }
    }

    public boolean sendGtpCommand(String command) throws Gtp.Error
    {
        if (m_commandInProgress)
            return false;
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

    public void initAnalyzeCommand(int type, String label, String command,
                                   String title, double scale)
    {
        m_analyzeType = type;
        m_analyzeTitle = title;
        m_analyzeLabel = label;
        m_analyzeCmd = command;
        m_analyzeScale = scale;
        m_analyzeRequestPoint = false;
        if (m_analyzeCmd.indexOf("%p") >= 0)
        {
            m_analyzeRequestPoint = true;
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
            showStatus("Please select a field.");
        }
    }

    public void setAnalyzeCommand(int type, String label, String command,
                                  String title, double scale)
    {
        initAnalyzeCommand(type, label, command, title, scale);
        if (m_commandInProgress)
            return;
        if (m_analyzeCmd.indexOf("%p") < 0)
            analyzeBegin();
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

    private static class UpdateGtpShellStdErr implements Runnable
    {
        public UpdateGtpShellStdErr(GtpShell gtpShell, String text)
        {
            m_gtpShell = gtpShell;
            m_text = new String(text);
        }

        public void run()
        {
            m_gtpShell.receivedStdErr(m_text);
        }

        private String m_text;
        private GtpShell m_gtpShell;
    }

    private boolean m_analyzeRequestPoint;
    private boolean m_boardNeedsReset;
    private boolean m_computerBlack;
    private boolean m_computerWhite;
    private boolean m_computerNoneOption;
    private boolean m_commandInProgress;
    private boolean m_fillPasses;
    private boolean m_lostOnTimeShown;
    private boolean m_scoreMode;
    private boolean m_setupMode;
    private boolean m_verbose;
    private int m_analyzeType = AnalyzeCommand.NONE;
    private int m_boardSize;
    private int m_handicap;
    private static int m_instanceCount;
    private int m_move;
    private double m_analyzeScale;
    private board.Color m_setupColor;
    private board.Point m_analyzePointArg;
    private board.Score m_score;
    private Board m_board;
    private CommandThread m_commandThread;
    private GameInfo m_gameInfo;
    private GoGui m_gui;
    private GtpShell m_gtpShell;
    private JLabel m_statusLabel;
    private MenuBars m_menuBars;
    private String m_analyzeCmd;
    private String m_analyzeLabel;
    private String m_analyzeTitle;
    private String m_file;
    private String m_name = "Unknown Go Program";
    private String m_pid;
    private String m_version = "?";
    private TimeControl m_timeControl;
    private ToolBar m_toolBar;
    private Vector m_commandList = new Vector(128, 128);

    private void analyzeBegin()
    {
        StringBuffer buffer = new StringBuffer(m_analyzeCmd);
        StringUtils.replace(buffer, "%m", m_board.getToMove().toString());
        if (m_analyzeCmd.indexOf("%p") >= 0)
        {
            if (m_analyzePointArg == null)
                return;
            StringUtils.replace(buffer, "%p",
                                m_analyzePointArg.toString());
        }
        showStatus("Running analyze command...");
        Runnable callback = new Runnable()
            {
                public void run() { analyzeContinue(); }
            };
        runLengthyCommand(buffer.toString(), callback);
    }

    private void analyzeContinue()
    {
        endLengthyCommand();
        try
        {
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            String answer = m_commandThread.getAnswer();
            if (m_analyzeType == AnalyzeCommand.COLORBOARD)
            {
                String board[][] = Gtp.parseStringBoard(answer,
                                                        m_analyzeTitle,
                                                        m_boardSize);
                showColorBoard(board);
            }
            else if (m_analyzeType == AnalyzeCommand.DOUBLEBOARD)
            {
                double board[][] = Gtp.parseDoubleBoard(answer, m_analyzeTitle,
                                                        m_boardSize);
                showDoubleBoard(board, m_analyzeScale);
            }
            else if (m_analyzeType == AnalyzeCommand.POINTLIST)
            {
                board.Point pointList[] = Gtp.parsePointList(answer);
                showPointList(pointList);
            }
            else if (m_analyzeType == AnalyzeCommand.STRINGBOARD)
            {
                String board[][] = Gtp.parseStringBoard(answer,
                                                        m_analyzeTitle,
                                                        m_boardSize);
                showStringBoard(board);
            }
            StringBuffer title = new StringBuffer(m_analyzeLabel);
            if (m_analyzePointArg != null)
            {
                title.append(" ");
                title.append(m_analyzePointArg.toString());
            }
            if (m_analyzeType == AnalyzeCommand.STRING)
            {
                if (answer.indexOf("\n") < 0)
                {
                    title.append(": ");
                    title.append(answer);
                    showStatus(title.toString());
                }
                else
                {
                    JDialog dialog = new JDialog(this, "GoGui: " + title);
                    JLabel label = new JLabel(title.toString());
                    Container contentPane = dialog.getContentPane();
                    contentPane.add(label, BorderLayout.NORTH);
                    JTextArea textArea = new JTextArea(answer, 17, 40);
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN,
                                              getFont().getSize()));
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    contentPane.add(scrollPane, BorderLayout.CENTER);
                    dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    dialog.pack();
                    dialog.setVisible(true);
                    if (m_analyzeRequestPoint)
                        showStatus("Please select a field.");
                    else
                        clearStatus();
                }
            }
            else
                showStatus(title.toString());
            if (! m_analyzeRequestPoint)
                checkComputerMove();
        }
        catch(Gtp.Error e)
        {                
            showGtpError(e);
            if (m_analyzeRequestPoint)
                showStatus("Please select a field.");
            else
                clearStatus();
            return;
        }
    }

    private void backward(int n)
    {
        try
        {
            for (int i = 0; i < n; ++i)
            {
                if (m_board.getMoveNumber() == 0)
                    break;
                if (m_commandThread != null)
                    m_commandThread.sendCommand("undo");
                m_board.undo();
            }
            computerNone();
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void beginLengthyCommand()
    {
        setBoardCursor(Cursor.WAIT_CURSOR);
        m_menuBars.setCommandInProgress(true);
        m_toolBar.enableAll(false, null);
        m_gtpShell.setEnabled(false);
        m_commandInProgress = true;
    }

    private void boardChanged()
    {
        resetBoard();
        m_gameInfo.update();
        m_toolBar.updateGameButtons(m_board);
        clearStatus();
        if (m_analyzeCmd != null)
            analyzeBegin();
        else
            checkComputerMove();
    }

    private void cbAnalyze()
    {        
        m_toolBar.toggleAnalyze();
    }

    private void cbBeginning()
    {
        backward(m_board.getMoveNumber());
    }

    private void cbBackward(int n)
    {
        backward(n);
    }

    private void cbComputerBoth()
    {
        computerBoth();
        checkComputerMove();
    }

    private void cbEnd()
    {
        forward(m_board.getNumberSavedMoves() - m_board.getMoveNumber());
    }

    private void cbForward(int n)
    {
        forward(n);
    }

    private void cbGtpShell()
    {
        if (m_gtpShell != null)
            m_gtpShell.toTop();
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
                newGame(m_boardSize, m_board.getKomi());
                boardChanged();
            }
        }
        catch (NumberFormatException e)
        {
            assert(false);
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
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
        Help help = new Help(null, u);
    }

    private void cbInterrupt()
    {
        if (m_computerBlack && m_computerWhite)
            computerNone();
        if (! m_commandInProgress)
            showError("No command in progress.");
        else if (m_commandThread == null)
            showError("No program loaded.");
        else if (m_commandThread.isProgramDead())
            showError("Program is dead.");
        else if (m_pid.equals(""))
            showError("Program does not support interrupting.\n" +
                      "See documentation section \"Interrupting programs\".");
        else
        {
            if (! showQuestion("Interrupt command?"))
                return;
            runCommand("kill -INT " + m_pid);
        }
    }

    private void cbKomi()
    {
        Object obj =
            JOptionPane.showInputDialog(this, "Komi value",
                                        "GoGui: Set komi",
                                        JOptionPane.PLAIN_MESSAGE,
                                        null, null,
                                        Float.toString(m_board.getKomi()));
        float komi =  Float.parseFloat((String)obj);
        setKomi(komi);
    }

    private void cbLoad()
    {
        File file = SimpleDialogs.showOpenSgf(this);
        if (file == null)
            return;
        loadFile(file, -1);
    }

    private void cbNewGame(int size)
    {
        try
        {
            if (! checkAbortGame())
                return;
            newGame(size, m_board.getKomi());
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
            m_menuBars.selectBoardSizeItem(m_boardSize);
        }
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

    private void cbOpenWithProgram()
    {
        try
        {
            String program = SelectProgram.select(this);
            if (program == null)
                return;
            File file = File.createTempFile("gogui-", "*.sgf");
            save(file);
            GoGui gui = new GoGui(program, m_boardSize, file.toString(),
                                  m_board.getMoveNumber(), null,
                                  false, null, m_verbose, m_fillPasses,
                                  m_computerNoneOption);

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
        if (m_board.getToMove() == board.Color.BLACK)
        {
            m_menuBars.setComputerBlack();
            computerBlack();
        }
        else
        {
            m_menuBars.setComputerWhite();
            computerWhite();
        }
        checkComputerMove();
    }

    private void cbRules(int rules)
    {
        m_board.setRules(rules);
        setRules();
    }

    private void cbSave()
    {
        File file = SimpleDialogs.showSaveSgf(this);
        if (file == null)
            return;
        try
        {
            if (file.exists())
                if (! showQuestion("Overwrite " + file + "?"))
                    return;
            save(file);
            showInfo("Game saved.");
        }
        catch (FileNotFoundException e)
        {
            showError(e.getMessage());
        }
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
        }
        catch (FileNotFoundException e)
        {
            showError(e.getMessage());
        }
    }

    private void cbScore()
    {
        resetBoard();
        m_board.scoreBegin();
        m_scoreMode = true;
        setJMenuBar(m_menuBars.getScoreMenu());
        m_toolBar.enableAll(false, null);
        pack();
        showStatus("Please remove dead groups.");
    }

    private void cbScoreDone()
    {
        m_score = m_board.scoreGet();
        int black = m_score.m_territoryBlack;
        int white = m_score.m_territoryWhite;
        clearStatus();
        String rules =
            (m_score.m_rules == Board.RULES_JAPANESE ? "Japanese" : "Chinese");
        showInfo("Territory Black: " + black + "\n" +
                 "Territory White: " + white + "\n" +
                 "Area Black: " + m_score.m_areaBlack + "\n" +
                 "Area White: " + m_score.m_areaWhite + "\n" +
                 "Captured Black: " + m_score.m_capturedBlack + "\n" +
                 "Captured Black: " + m_score.m_capturedWhite + "\n" +
                 "Komi: " + m_board.getKomi() + "\n" +
                 "Result Chinese: " + m_score.m_resultChinese + "\n" +
                 "Result Japanese: " + m_score.m_resultJapanese + "\n" +
                 "Rules: " + rules + "\n" +
                 "\n" +
                 "Game result:\n" + 
                 m_score.formatResult());
        m_board.clearAll();
        m_scoreMode = false;
        setJMenuBar(m_menuBars.getNormalMenu());
        m_toolBar.enableAll(true, m_board);
        pack();
    }

    private void cbSetup()
    {
        resetBoard();
        m_setupMode = true;
        setJMenuBar(m_menuBars.getSetupMenu());
        m_toolBar.enableAll(false, null);
        pack();
        showStatus("Setup black.");
        m_setupColor = board.Color.BLACK;
    }

    private void cbSetupBlack()
    {
        showStatus("Setup black.");
        m_setupColor = board.Color.BLACK;
    }

    private void cbSetupDone()
    {
        try
        {
            m_setupMode = false;
            setJMenuBar(m_menuBars.getNormalMenu());
            pack();
            m_toolBar.enableAll(true, m_board);
            int size = m_board.getBoardSize();
            board.Color color[][] = new board.Color[size][size];
            for (int i = 0; i < m_board.getNumberPoints(); ++i)
            {
                board.Point p = m_board.getPoint(i);
                color[p.getX()][p.getY()] = m_board.getColor(p);
            }
            board.Color toMove = m_board.getToMove().otherColor();
            newGame(size, m_board.getKomi());
            Vector moves = new Vector(m_board.getNumberPoints());
            for (int i = 0; i < m_board.getNumberPoints(); ++i)
            {
                board.Point p = m_board.getPoint(i);
                int x = p.getX();
                int y = p.getY();
                board.Color c = color[x][y];
                if (c != board.Color.EMPTY)
                {
                    moves.add(new Move(p, c));
                }
            }
            if (m_fillPasses)
                moves = fillPasses(moves);
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                if (m_commandThread != null)
                    m_commandThread.sendCommandPlay(m);
                m_board.setup(m);
            }
            if (m_board.getToMove() != toMove)
            {
                Move m = new Move(null, m_board.getToMove());
                if (m_commandThread != null)
                    m_commandThread.sendCommandPlay(m);
                m_board.play(m);
            }
            computerNone();
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void cbSetupWhite()
    {
        showStatus("Setup white.");
        m_setupColor = board.Color.WHITE;
    }

    private void cbShowAbout()
    {
        String message =
            m_name + "\n" +
            "Version " + m_version;
        SimpleDialogs.showAbout(this, message);
    }

    private boolean checkAbortGame()
    {
        if (! m_board.isModified())
            return true;
        return showQuestion("Abort game?");
    }
    
    private void checkComputerMove()
    {
        if (m_computerBlack && m_computerWhite)
        {
            if (m_board.bothPassed())
            {
                showInfo("Game finished.");
                computerNone();
                return;
            }
            else
                generateMove();
        }
        else
        {
            if (computerToMove())
                generateMove();
        }
        m_timeControl.startMove(m_board.getToMove());
    }

    private void clearStatus()
    {
        showStatus(" ");
    }

    private void close()
    {
        if (m_commandInProgress)
        {
            String message = "Interrupt command and exit?";
            if (! showQuestion(message))
                return;
        }
        else
        {
            if (m_commandThread != null && ! m_commandThread.isProgramDead())
                try
                {
                    m_commandThread.sendCommand("quit");
                }
                catch(Gtp.Error e)
                {
                }
        }
        if (m_gtpShell != null)
            m_gtpShell.saveHistory();
        dispose();
        assert(m_instanceCount > 0);
        if (--m_instanceCount == 0)
            System.exit(0);
    }

    private void computerBlack()
    {
        m_computerBlack = true;
        m_computerWhite = false;
        m_menuBars.setComputerBlack();
    }

    private void computerBoth()
    {
        m_computerBlack = true;
        m_computerWhite = true;
        m_menuBars.setComputerBoth();
    }

    private void computerMoved()
    {
        assert(m_commandThread != null);
        endLengthyCommand();
        //java.awt.Toolkit.getDefaultToolkit().beep();
        try
        {
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            board.Point p = Gtp.parsePoint(m_commandThread.getAnswer());
            board.Color toMove = m_board.getToMove();
            Move m = new Move(p, toMove);
            m_board.play(m);
            m_timeControl.stopMove();
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void computerNone()
    {
        m_computerBlack = false;
        m_computerWhite = false;
        m_menuBars.setComputerNone();
    }

    private boolean computerToMove()
    {
        if (m_board.getToMove() == board.Color.BLACK)
            return m_computerBlack;
        else
            return m_computerWhite;
    }

    private void computerWhite()
    {
        m_computerBlack = false;
        m_computerWhite = true;
        m_menuBars.setComputerWhite();
    }

    private JComponent createStatusBar()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0));
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
        m_menuBars.setCommandInProgress(false);
        m_toolBar.enableAll(true, m_board);
        if (m_gtpShell != null)
            m_gtpShell.setEnabled(true);
        m_commandInProgress = false;
        if (m_analyzeRequestPoint)
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
        else
            setBoardCursorDefault();
    }

    private Vector fillPasses(Vector moves)
    {
        assert(m_fillPasses);
        Vector result = new Vector(moves.size() * 2);
        if (moves.size() == 0)
            return result;
        board.Color toMove = m_board.getToMove();
        for (int i = 0; i < moves.size(); ++i)
        {
            Move m = (Move)moves.get(i);
            if (m.getColor() != toMove)
                result.add(new Move(null, toMove));
            result.add(m);
            toMove = m.getColor().otherColor();
        }
        return result;
    }

    private void forward(int n)
    {
        try
        {
            for (int i = 0; i < n; ++i)
            {
                int moveNumber = m_board.getMoveNumber();
                if (moveNumber >= m_board.getNumberSavedMoves())
                    break;
                Move m = m_board.getMove(moveNumber);
                if (m_commandThread != null)
                    m_commandThread.sendCommandPlay(m);
                m_board.play(m);
            }
            computerNone();
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void generateMove()
    {
        board.Color toMove = m_board.getToMove();
        String command = m_commandThread.getCommandGenmove(toMove);
        showStatus("Computer is thinking...");
        Runnable callback = new Runnable()
            {
                public void run() { computerMoved(); }
            };
        runLengthyCommand(command, callback);
    }

    private void humanMoved(Move m)
    {
        try
        {
            board.Point p = m.getPoint();
            if (p != null && m_board.getColor(p) != board.Color.EMPTY)
                    return;
            if (m_commandThread != null)
                m_commandThread.sendCommandPlay(m);
            m_board.play(m);
            m_timeControl.stopMove();
            if (m_board.getMoveNumber() > 0
                && m_timeControl.lostOnTime(m.getColor())
                && ! m_lostOnTimeShown)
            {
                showInfo(m.getColor().toString() + " lost on time.");
                m_lostOnTimeShown = true;
            }
            boardChanged();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void initialize()
    {
        try
        {
            if (m_commandThread != null)
            {
                m_name = m_commandThread.sendCommand("name", 30000).trim();
                m_gtpShell.setProgramName(m_name);
                setTitle(m_name);
            }
            if (m_commandThread != null)
            {
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
                    m_version = m_commandThread.sendCommand("version").trim();
                    queryCommandList();
                    if (m_commandList.contains("gogui_sigint"))
                        m_pid =
                            m_commandThread.sendCommand("gogui_sigint").trim();
                }
                catch (Gtp.Error e)
                {
                }
                m_gtpShell.setInitialCompletions(m_commandList);
            }
            if (m_commandThread == null || m_computerNoneOption)
                computerNone();
            else
                computerWhite();
            File file = null;
            if (! m_file.equals(""))
                newGame(m_boardSize, new File(m_file), m_move, 0);
            else
            {
                newGame(m_boardSize, null, -1, 0);
                boardChanged();
            }
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
            showStatus(e.getMessage());
        }
    }

    private void loadFile(File file, int move)
    {
        try
        {
            sgf.Reader reader = new sgf.Reader(file);
            int boardSize = reader.getBoardSize();
            newGame(boardSize, 0);
            Vector moves = new Vector(361, 361);
            Vector setupBlack = reader.getSetupBlack();
            for (int i = 0; i < setupBlack.size(); ++i)
            {
                board.Point p = (board.Point)setupBlack.get(i);
                moves.add(new Move(p, board.Color.BLACK));
            }
            Vector setupWhite = reader.getSetupWhite();
            for (int i = 0; i < setupWhite.size(); ++i)
            {
                board.Point p = (board.Point)setupWhite.get(i);
                moves.add(new Move(p, board.Color.WHITE));
            }
            if (m_fillPasses)
                moves = fillPasses(moves);
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                m_board.setup(m);
                m_commandThread.sendCommandPlay(m);
            }
            int numberMoves = reader.getMoves().size();
            board.Color toMove = reader.getToMove();
            if (numberMoves > 0)
                toMove = reader.getMove(0).getColor();
            if (toMove != m_board.getToMove())
            {
                if (m_fillPasses)
                {
                    Move m = new Move(null, m_board.getToMove());
                    m_board.setup(m);
                    m_commandThread.sendCommandPlay(m);
                }
                else
                    m_board.setToMove(toMove);
            }

            moves.clear();
            for (int i = 0; i < numberMoves; ++i)
                moves.add(reader.getMove(i));
            if (m_fillPasses)
                moves = fillPasses(moves);
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                m_board.play(m);
            }
            while (m_board.getMoveNumber() > 0)
                m_board.undo();
            
            if (move > 0)
                forward(move);
        }
        catch (sgf.Reader.Error e)
        {
            showError("Could not read file\n" + e.getMessage());
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void newGame(int size, float komi) throws Gtp.Error
    {
        newGame(size, (File)null, -1, komi);
    }

    private void newGame(int size, File file, int move, float komi)
        throws Gtp.Error
    {
        if (m_commandThread != null)
        {
            m_commandThread.sendCommandsClearBoard(size);
        }
        if (size != m_boardSize)
        {
            m_boardSize = size;
            m_board.initSize(size);
            pack();
        }
        m_board.newGame();
        resetBoard();
        m_timeControl.reset();
        m_lostOnTimeShown = false;
        m_score = null;
        if (file != null)
        {
            loadFile(new File(m_file), move);
            m_gameInfo.update();
        }
        else
        {
            setHandicap();
            setKomi(komi);
            setRules();
        }
    }

    private void queryCommandList()
    {
        assert(m_commandThread != null);
        m_gtpShell.setProgramVersion(m_version);
        try
        {
            String s = m_commandThread.sendCommandListCommands();
            BufferedReader r = new BufferedReader(new StringReader(s));
            try
            {
                while (true)
                {
                    String line = r.readLine();
                    if (line == null)
                        break;
                    m_commandList.add(line);
                }
            }
            catch (IOException e)
            {
            }
        }
        catch (Gtp.Error e)
        {
        }
    }

    private void resetBoard()
    {
        if (! m_boardNeedsReset)
            return;
        clearStatus();
        m_board.clearAll();
        m_boardNeedsReset = false;
    }
    
    private void runCommand(String command)
    {
        Runtime runtime = Runtime.getRuntime();
        try
        {
            Process process = runtime.exec(command);
            int result = process.waitFor();
            if (result != 0)
                showError("Command \"" + command + "\" returned " +
                          result + ".");
        }
        catch (IOException e)
        {
            showError(e.getMessage());
        }
        catch (InterruptedException e)
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
            if (m_computerBlack)
                playerBlack = m_name + ":" + m_version;
            if (m_computerWhite)
                playerWhite = m_name + ":" + m_version;
            gameComment =
                "Program command: " + m_commandThread.getProgramCommand();
        }
        sgf.Writer w = new sgf.Writer(file, m_board, m_handicap,
                                      playerBlack, playerWhite,
                                      gameComment, m_score);
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        sgf.Writer w = new sgf.Writer(file, m_board);
    }

    private void setHandicap() throws Gtp.Error
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
            board.Point p = (board.Point)handicap.get(i);
            moves.add(new Move(p, board.Color.BLACK));
        }
        if (m_fillPasses)
            moves = fillPasses(moves);
        for (int i = 0; i < moves.size(); ++i)
        {
            Move m = (Move)moves.get(i);
            m_commandThread.sendCommandPlay(m);
            m_board.setup(m);
        }
    }

    private void setBoardCursor(int type)
    {
        Cursor cursor = Cursor.getPredefinedCursor(type);
        m_board.setCursor(cursor);
    }

    private void setBoardCursorDefault()
    {
        m_board.setCursor(Cursor.getDefaultCursor());
    }

    private void setKomi(float komi)
    {
        m_board.setKomi(komi);
        if (m_commandThread == null)
            return;
        try
        {
            m_commandThread.sendCommand("komi " + komi);
        }
        catch (Gtp.Error e)
        {
            showWarning("Program does not accept komi\n" +
                        "(" + e.getMessage() + ").");
        }
    }

    private void setRules()
    {
        if (m_commandThread == null)
            return;
        if (! m_commandList.contains("scoring_system"))
            return;
        try
        {
            int rules = m_board.getRules();
            String s = (rules == Board.RULES_JAPANESE ? "territory" : "area");
            m_commandThread.sendCommand("scoring_system " + s);
        }
        catch (Gtp.Error e)
        {
            showWarning("Program does not accept scoring system\n" +
                        "(" + e.getMessage() + ").");
        }
    }

    private void showColorBoard(String[][] board) throws Gtp.Error
    {
        m_board.showColorBoard(board);
        m_boardNeedsReset = true;
    }

    private void showDoubleBoard(double[][] board, double scale)
    {
        m_board.showDoubleBoard(board, scale);
        m_boardNeedsReset = true;
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
        SimpleDialogs.showError(frame, e.getMessage());
    }

    private void showInfo(String message)
    {
        SimpleDialogs.showInfo(this, message);
    }

    private boolean showQuestion(String message)
    {
        return SimpleDialogs.showQuestion(this, message);
    }

    private void showPointList(board.Point pointList[]) throws Gtp.Error
    {
        m_board.showPointList(pointList);
        m_boardNeedsReset = true;
    }

    private void showStatus(String text)
    {
        m_statusLabel.setText(text);
        m_statusLabel.repaint();
    }

    private void showStringBoard(String[][] board) throws Gtp.Error
    {
        m_board.showStringBoard(board);
        m_boardNeedsReset = true;
    }

    private void showWarning(String message)
    {
        SimpleDialogs.showWarning(this, message);
    }
}

//-----------------------------------------------------------------------------
