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
import go.*;
import gtp.*;
import latex.*;
import sgf.*;
import utils.*;

//-----------------------------------------------------------------------------

class GoGui
    extends JFrame
    implements ActionListener, Analyze.Callback, Board.Listener,
               GtpShell.Callback, WindowListener
{
    GoGui(String program, Preferences prefs, String file, int move,
          boolean gtpShell, String time, boolean verbose, boolean fillPasses,
          boolean computerBlack, boolean computerWhite, boolean auto,
          String gtpFile, String gtpCommand, String initAnalyze)
        throws Gtp.Error, Analyze.Error
    {
        m_program = program;
        if (program != null && ! program.equals(""))
        {
            m_gtpShell = new GtpShell(null, "GoGui", this, prefs);
            m_gtpShell.setProgramCommand(program);
        }
        m_prefs = prefs;
        m_boardSize = prefs.getBoardSize();
        m_beepAfterMove = prefs.getBeepAfterMove();
        m_file = file;
        m_fillPasses = fillPasses;
        m_gtpFile = gtpFile;
        m_gtpCommand = gtpCommand;
        m_move = move;
        m_computerBlack = computerBlack;
        m_computerWhite = computerWhite;
        m_auto = auto;
        m_verbose = verbose;
        m_initAnalyze = initAnalyze;

        Container contentPane = getContentPane();        

        JPanel infoPanel = new JPanel();

        m_timeControl = new TimeControl();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(utils.GuiUtils.createSmallEmptyBorder());
        Dimension pad = new Dimension(0, utils.GuiUtils.PAD);
        m_gameInfo = new GameInfo(m_timeControl);
        infoPanel.add(m_gameInfo);
        infoPanel.add(Box.createRigidArea(pad));
        infoPanel.add(createStatusBar());

        m_board = new go.Board(m_boardSize);
        m_board.setKomi(prefs.getKomi());
        m_board.setRules(prefs.getRules());
        m_guiBoard = new Board(m_board);
        m_guiBoard.setListener(this);
        m_gameInfo.setBoard(m_board);
        m_toolBar = new ToolBar(this, prefs, this);
        contentPane.add(m_toolBar, BorderLayout.NORTH);

        contentPane.add(infoPanel, BorderLayout.SOUTH);

        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new SquareLayout());
        boardPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        boardPanel.add(m_guiBoard);
        contentPane.add(boardPanel, BorderLayout.CENTER);
        
        addWindowListener(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(new GoIcon());
        m_menuBars = new MenuBars(this);
        m_menuBars.selectBoardSizeItem(m_boardSize);
        m_menuBars.selectRulesItem(m_board.getRules());
        m_menuBars.setBeepAfterMove(m_beepAfterMove);
        setJMenuBar(m_menuBars.getNormalMenu());

        if (program == null || program.equals(""))
        {
            m_toolBar.disableComputerButtons();
            m_menuBars.disableComputerMenus();
        }

        pack();
        setTitle("GoGui");
        setVisible(true);
        ++m_instanceCount;
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
        else if (command.equals("backward"))
            cbBackward(1);
        else if (command.equals("backward-10"))
            cbBackward(10);
        else if (command.equals("beep-after-move"))
            cbBeepAfterMove();
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
        else if (command.equals("gtp-file"))
            cbGtpFile();
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
        else if (command.equals("print"))
            cbPrint();
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
        else if (command.equals("show-last-move"))
            cbShowLastMove();
        else
            assert(false);
    }
    
    public void clearAnalyzeCommand()
    {
        m_analyzeCommand = null;
        if (m_analyzeRequestPoint)
        {
            m_analyzeRequestPoint = false;
            setBoardCursorDefault();
        }
        resetBoard();
        clearStatus();
    }

    public void fieldClicked(go.Point p)
    {
        if (m_commandInProgress)
        {
            return;
        }
        if (m_setupMode)
        {
            if (m_board.getColor(p) != m_setupColor)
                m_board.setup(new Move(p, m_setupColor));
            else
                m_board.setup(new Move(p, go.Color.EMPTY));
            m_board.setToMove(m_setupColor);
            m_gameInfo.update();
            m_guiBoard.update();
            m_guiBoard.repaint();
            return;
        }
        if (m_analyzeRequestPoint)
        {
            m_analyzePointArg = p;
            m_guiBoard.clearAllCrossHair();
            m_guiBoard.setCrossHair(p, true);
            analyzeBegin(false);
            return;
        }
        if (m_scoreMode)
        {
            m_guiBoard.scoreSetDead(p);
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
                "file:",
                "fillpasses",
                "gtpfile:",
                "gtpshell",
                "help",
                "komi:",
                "move:",
                "rules:",
                "size:",
                "time:",
                "verbose"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                String helpText =
                    "Usage: java -jar gogui.jar [options] program\n" +
                    "Graphical user interface for Go programs\n" +
                    "using the Go Text Protocol.\n" +
                    "\n" +
                    "  -analyze name   initialize analyze command\n" +
                    "  -auto           auto play games (if computer both)\n" +
                    "  -command cmd    send GTP command at startup\n" +
                    "  -computer-both  computer plays both sides\n" +
                    "  -computer-black computer plays black\n" +
                    "  -computer-none  computer plays no side\n" +
                    "  -file filename  load SGF file\n" +
                    "  -fillpasses     never send subsequent moves of\n" +
                    "                  the same color to the program\n" +
                    "  -gtpshell       open GTP shell at startup\n" +
                    "  -gtpfile file   send GTP file at startup\n" +
                    "  -help           display this help and exit\n" +
                    "  -komi value     set komi\n" +
                    "  -move n         load SGF file until move number\n" +
                    "  -rules name     use rules (chinese|japanese)\n" +
                    "  -size n         set board size\n" +
                    "  -time spec      set time limits (min[+min/moves])\n" +
                    "  -verbose        print debugging messages\n";
                System.out.print(helpText);
                System.exit(0);
            }
            Preferences prefs = new Preferences();
            if (opt.contains("analyze"))
                prefs.setAnalyzeCommand(opt.getString("analyze"));
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
            boolean fillPasses = opt.isSet("fillpasses");
            boolean gtpShell = opt.isSet("gtpshell");
            String gtpFile = opt.getString("gtpfile", "");
            String gtpCommand = opt.getString("command", "");
            if (opt.contains("komi"))
                prefs.setKomi(opt.getFloat("komi"));
            int move = opt.getInteger("move", -1);
            if (opt.contains("size"))
                prefs.setBoardSize(opt.getInteger("size"));
            String rules = opt.getString("rules", "");
            if (rules == "chinese")
                prefs.setRules(go.Board.RULES_CHINESE);
            else if (rules == "japanese")
                prefs.setRules(go.Board.RULES_JAPANESE);
            else if (rules != "")
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
            
            GoGui gui = new GoGui(program, prefs, file, move, gtpShell, time,
                                  verbose, fillPasses, computerBlack,
                                  computerWhite, auto, gtpFile, gtpCommand,
                                  initAnalyze);
        }
        catch (AssertionError e)
        {
            SimpleDialogs.showError(null, "Assertion error");
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
        if (m_commandInProgress)
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

    public void initAnalyzeCommand(Analyze.Command command)
    {
        m_analyzeCommand = command;
        m_analyzeRequestPoint = false;
        if (m_commandThread != null && command.needsPointArg())
        {
            m_analyzeRequestPoint = true;
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
            showStatus("Please select a field.");
        }
    }

    public void setAnalyzeCommand(Analyze.Command command)
    {
        initAnalyzeCommand(command);
        if (m_commandInProgress)
            return;
        if (! m_analyzeCommand.needsPointArg())
            analyzeBegin(false);
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

    private boolean m_analyzeRequestPoint;

    private boolean m_auto;

    private boolean m_beepAfterMove;

    private boolean m_boardNeedsReset;

    private boolean m_computerBlack;

    private boolean m_computerWhite;

    private boolean m_commandInProgress;

    private boolean m_fillPasses;

    private boolean m_lostOnTimeShown;

    private boolean m_resetBoardAfterAnalyze;

    private boolean m_scoreMode;

    private boolean m_setupMode;

    private boolean m_verbose;

    private int m_boardSize;

    private int m_handicap;

    private static int m_instanceCount;

    private int m_move;

    private go.Board m_board;

    private go.Color m_setupColor;

    private go.Point m_analyzePointArg;

    private go.Score m_score;

    private Board m_guiBoard;

    private CommandThread m_commandThread;

    private GameInfo m_gameInfo;

    private GoGui m_gui;

    private GtpShell m_gtpShell;

    private JLabel m_statusLabel;

    private MenuBars m_menuBars;

    private Analyze.Command m_analyzeCommand;

    private String m_file;

    private String m_gtpCommand;

    private String m_gtpFile;

    private String m_initAnalyze;

    private String m_loadedFile;

    private String m_name = "";

    private String m_pid;

    private String m_program;

    private String m_version = "";

    /** Preferences.
        Preferences are shared between instances created with
        "Open with program", the last instance of GoGui saves them.
    */
    private Preferences m_prefs;

    private TimeControl m_timeControl;

    private ToolBar m_toolBar;

    private void analyzeBegin(boolean resetBoardAfterAnalyze)
    {
        if (m_commandThread == null)
            return;
        m_resetBoardAfterAnalyze = resetBoardAfterAnalyze;
        if (m_analyzeCommand.needsPointArg() && m_analyzePointArg == null)
            return;
        showStatus("Running analyze command...");
        String command =
            m_analyzeCommand.replaceWildCards(m_board.getToMove(),
                                              m_analyzePointArg);
        Runnable callback = new Runnable()
            {
                public void run() { analyzeContinue(); }
            };
        runLengthyCommand(command, callback);
    }

    private void analyzeContinue()
    {
        endLengthyCommand();
        try
        {
            if (m_resetBoardAfterAnalyze)
                resetBoard();
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            String response = m_commandThread.getResponse();
            String title = m_analyzeCommand.getTitle();
            int type = m_analyzeCommand.getType();
            switch (type)
            {
            case Analyze.COLORBOARD:
                {
                    String board[][] = Gtp.parseStringBoard(response, title,
                                                            m_boardSize);
                    showColorBoard(board);
                    m_guiBoard.repaint();
                }
                break;
            case Analyze.DOUBLEBOARD:
                {
                    double board[][] = Gtp.parseDoubleBoard(response, title,
                                                            m_boardSize);
                    showDoubleBoard(board, m_analyzeCommand.getScale());
                    m_guiBoard.repaint();
                }
                break;
            case Analyze.POINTLIST:
                {
                    go.Point pointList[] = Gtp.parsePointList(response);
                    showPointList(pointList);
                    m_guiBoard.repaint();
                }
                break;
            case Analyze.STRINGBOARD:
                {
                    String board[][] = Gtp.parseStringBoard(response, title,
                                                            m_boardSize);
                    showStringBoard(board);
                    m_guiBoard.repaint();
                }
                break;
            }
            String resultTitle =
                m_analyzeCommand.getResultTitle(m_analyzePointArg);
            if (type == Analyze.STRING)
            {
                if (response.indexOf("\n") < 0)
                {
                    showStatus(resultTitle + ": " + response);
                }
                else
                {
                    JDialog dialog =
                        new JDialog(this, "GoGui: " + resultTitle);
                    JLabel label = new JLabel(resultTitle);
                    Container contentPane = dialog.getContentPane();
                    contentPane.add(label, BorderLayout.NORTH);
                    JTextArea textArea = new JTextArea(response, 17, 40);
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
                showStatus(resultTitle);
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
            if (m_commandThread != null)
            {
                m_commandThread.setFastUpdate(true);
                m_gtpShell.setFastUpdate(true);
            }
            for (int i = 0; i < n; ++i)
            {
                if (m_board.getMoveNumber() == 0)
                    break;
                if (m_commandThread != null)
                    m_commandThread.sendCommand("undo");
                m_board.undo();
            }
            if (m_commandThread != null)
            {
                m_commandThread.setFastUpdate(false);
                m_gtpShell.setFastUpdate(false);
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
        m_toolBar.setCommandInProgress();
        m_gtpShell.setInputEnabled(false);
        m_commandInProgress = true;
    }

    private void boardChanged()
    {
        m_guiBoard.update();
        m_guiBoard.repaint();
        m_gameInfo.update();
        m_toolBar.updateGameButtons(m_board);
        clearStatus();
        if (m_commandThread != null && m_analyzeCommand != null
            && ! (m_analyzeCommand.needsPointArg()
                  && m_analyzePointArg == null))
            analyzeBegin(true);
        else
        {
            resetBoard();
            checkComputerMove();
        }
    }

    private void cbAnalyze()
    {        
        m_toolBar.toggleAnalyze();
    }

    private void cbBeepAfterMove()
    {
        m_beepAfterMove = m_menuBars.getBeepAfterMove();
        m_prefs.setBeepAfterMove(m_beepAfterMove);
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
        computerNone();
        boardChanged();
    }

    private void cbForward(int n)
    {
        forward(n);
        computerNone();
        boardChanged();
    }

    private void cbGtpFile()
    {
        if (m_commandThread == null)
            return;
        File file = SimpleDialogs.showOpen(this, "Choose GTP file.");
        if (file != null)
            sendGtpFile(file);
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
            computerBlack();
            if (m_board.isModified())
                showInfo("Handicap will take effect on next game.");
            else
                newGame(m_boardSize);
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
        Help help = new Help(null, u);
    }

    private void cbInterrupt()
    {
        if (! m_commandInProgress || m_commandThread == null
            || m_commandThread.isProgramDead())
            return;
        if (m_computerBlack && m_computerWhite)
            computerNone();
        if (! showQuestion("Interrupt command?"))
            return;
        interrupt();
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
        m_prefs.setKomi(komi);
        setKomi();
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
        if (! checkAbortGame())
            return;
        m_prefs.setBoardSize(size);
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
                                  m_board.getMoveNumber(), false, null,
                                  m_verbose, m_fillPasses,
                                  false, false, false, "", "", "");

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

    private void cbRules(int rules)
    {
        m_board.setRules(rules);
        m_prefs.setRules(rules);
        setRules();
    }

    private void cbSave()
    {
        File file = SimpleDialogs.showSave(this);
        if (file == null)
            return;
        try
        {
            if (file.exists())
                if (! showQuestion("Overwrite " + file + "?"))
                    return;
            save(file);
            showInfo("Game saved.");
            m_loadedFile = file.toString();
            setTitle();
        }
        catch (FileNotFoundException e)
        {
            showError("Could not save game.", e);
        }
    }

    private void cbSavePosition()
    {
        File file = SimpleDialogs.showSave(this);
        if (file == null)
            return;
        try
        {
            if (file.exists())
                if (! showQuestion("Overwrite " + file + "?"))
                    return;
            savePosition(file);
            showInfo("Position saved.");
            m_loadedFile = file.toString();
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
            try
            {
                String response =
                    m_commandThread.sendCommand("final_status_list dead");
                isDeadStone = Gtp.parsePointList(response);
            }
            catch (Gtp.Error e)
            {
            }
        }
        resetBoard();
        m_guiBoard.scoreBegin(isDeadStone);
        m_scoreMode = true;
        Dimension size = getSize();
        setJMenuBar(m_menuBars.getScoreMenu());
        m_toolBar.enableAll(false, null);
        pack();
        setSize(size);
        showStatus("Please remove dead groups.");
    }

    private void cbScoreDone()
    {
        m_score = m_board.scoreGet();
        clearStatus();
        showInfo(m_score.formatDetailedResult());
        m_guiBoard.clearAll();
        m_scoreMode = false;
        Dimension size = getSize();
        setJMenuBar(m_menuBars.getNormalMenu());
        m_toolBar.enableAll(true, m_board);
        pack();
        setSize(size);
    }

    private void cbSetup()
    {
        resetBoard();
        m_setupMode = true;
        Dimension size = getSize();
        setJMenuBar(m_menuBars.getSetupMenu());
        m_toolBar.enableAll(false, null);
        pack();
        setSize(size);
        showStatus("Setup black.");
        m_setupColor = go.Color.BLACK;
    }

    private void cbSetupBlack()
    {
        showStatus("Setup black.");
        m_setupColor = go.Color.BLACK;
        m_board.setToMove(m_setupColor);
        m_gameInfo.update();
    }

    private void cbSetupDone()
    {
        try
        {
            m_setupMode = false;
            Dimension frameSize = getSize();
            setJMenuBar(m_menuBars.getNormalMenu());
            pack();
            setSize(frameSize);
            m_toolBar.enableAll(true, m_board);
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
            Vector moves = new Vector(m_board.getNumberPoints());
            for (int i = 0; i < m_board.getNumberPoints(); ++i)
            {
                go.Point p = m_board.getPoint(i);
                int x = p.getX();
                int y = p.getY();
                go.Color c = color[x][y];
                if (c != go.Color.EMPTY)
                    moves.add(new Move(p, c));
            }
            if (m_fillPasses)
                moves = Move.fillPasses(moves, m_board.getToMove());
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                setup(m);
            }
            if (m_board.getToMove() != toMove)
            {
                Move m = new Move(null, m_board.getToMove());
                setup(m);
            }
            computerNone();
            boardChanged();
            fileModified();
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
    }

    private void cbSetupWhite()
    {
        showStatus("Setup white.");
        m_setupColor = go.Color.WHITE;
        m_board.setToMove(m_setupColor);
        m_gameInfo.update();
    }

    private void cbShowAbout()
    {
        String message = "GoGui " + Version.m_version;
        SimpleDialogs.showAbout(this, message);
    }

    private void cbShowLastMove()
    {
        m_guiBoard.setShowLastMove(m_menuBars.getShowLastMove());
    }

    private boolean checkAbortGame()
    {
        if (! m_board.isModified())
            return true;
        return showQuestion("Abort game?");
    }
    
    private boolean checkModifyGame(Move move)
    {
        if (! m_board.willModifyGame(move))
            return true;
        return showQuestion("Move will modify the game.\n" +
                            "Proceed?");
    }
    
    private void checkComputerMove()
    {
        if (m_commandThread == null)
            return;
        if (m_computerBlack && m_computerWhite)
        {
            if (m_board.bothPassed())
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
            interrupt();
            m_commandThread.sendAsyncQuit();
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
        if (m_gtpShell != null)
            m_gtpShell.saveHistory();
        dispose();
        assert(m_instanceCount > 0);
        if (--m_instanceCount == 0)
        {
            try
            {
                m_prefs.save();
            }
            catch (Preferences.Error e)
            {
                showError("Could not save preferences.", e);
            }
            System.exit(0);
        }
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
        if (m_beepAfterMove)
            java.awt.Toolkit.getDefaultToolkit().beep();
        try
        {
            Gtp.Error e = m_commandThread.getException();
            if (e != null)
                throw e;
            go.Point p = Gtp.parsePoint(m_commandThread.getResponse());
            go.Color toMove = m_board.getToMove();
            Move m = new Move(p, toMove);
            m_board.play(m);
            m_timeControl.stopMove();
            if (m.getPoint() == null && ! (m_computerBlack && m_computerWhite))
                showInfo("The computer passed.");
            boardChanged();
            fileModified();
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
        m_menuBars.setComputerNone();
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
            m_gtpShell.setInputEnabled(true);
        m_commandInProgress = false;
        if (m_analyzeRequestPoint)
            setBoardCursor(Cursor.CROSSHAIR_CURSOR);
        else
            setBoardCursorDefault();
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
        try
        {
            if (m_commandThread != null)
            {
                m_commandThread.setFastUpdate(true);
                m_gtpShell.setFastUpdate(true);
            }
            for (int i = 0; i < n; ++i)
            {
                int moveNumber = m_board.getMoveNumber();
                if (moveNumber >= m_board.getNumberSavedMoves())
                    break;
                Move move = m_board.getMove(moveNumber);
                play(move);
            }
            if (m_commandThread != null)
            {
                m_commandThread.setFastUpdate(false);
                m_gtpShell.setFastUpdate(false);
            }
        }
        catch (Gtp.Error e)
        {
            showGtpError(e);
        }
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

    private void humanMoved(Move m)
    {
        try
        {
            if (! checkModifyGame(m))
                return;
            go.Point p = m.getPoint();
            if (p != null && m_board.getColor(p) != go.Color.EMPTY)
                    return;
            play(m);
            m_timeControl.stopMove();
            if (m_board.getMoveNumber() > 0
                && m_timeControl.lostOnTime(m.getColor())
                && ! m_lostOnTimeShown)
            {
                showInfo(m.getColor().toString() + " lost on time.");
                m_lostOnTimeShown = true;
            }
            boardChanged();
            fileModified();
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
            pack();
        }
        m_board.newGame();        
        m_guiBoard.update();
        resetBoard();
        m_timeControl.reset();
        m_lostOnTimeShown = false;
        m_score = null;
    }

    private void initialize()
    {
        try
        {
            m_toolBar.enableAll(true, m_board);
            if (m_program != null && ! m_program.equals(""))
            {
                Gtp gtp = new Gtp(m_program, m_verbose, m_gtpShell);
                m_commandThread = new CommandThread(gtp, m_gtpShell);
                m_commandThread.start();
            }
            if (m_commandThread != null)
            {
                m_name = m_commandThread.sendCommand("name", 30000).trim();
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
                    if (m_commandThread.isCommandSupported("gogui_sigint"))
                        m_pid =
                            m_commandThread.sendCommand("gogui_sigint").trim();
                }
                catch (Gtp.Error e)
                {
                }
                Vector supportedCommands =
                    m_commandThread.getSupportedCommands();
                m_gtpShell.setInitialCompletions(supportedCommands);
                if (! m_gtpFile.equals(""))
                    sendGtpFile(new File(m_gtpFile));
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
            File file = null;
            if (! m_file.equals(""))
                newGameFile(m_boardSize, new File(m_file), m_move);
            else
            {
                setKomi();
                newGame(m_boardSize);
            }
            if (! m_initAnalyze.equals(""))
                m_toolBar.setAnalyzeCommand(m_initAnalyze);
            m_guiBoard.requestFocus();
        }
        catch (Gtp.Error e)
        {
            SimpleDialogs.showError(this,
                                    e.getMessage() + "\n"
                                    + "See GTP shell for any error messages\n"
                                    + "printed by the program.");
            showStatus(e.getMessage());
        }
    }

    private void interrupt()
    {
        if (m_pid != null)
            runCommand("kill -INT " + m_pid);
        else
            m_commandThread.sendInterrupt();
    }

    private void loadFile(File file, int move)
    {
        try
        {
            java.io.Reader fileReader = new FileReader(file);
            sgf.Reader reader = new sgf.Reader(fileReader, file.toString());
            initGame(reader.getBoardSize());
            if (m_commandThread != null)
            {
                m_commandThread.sendCommandBoardsize(m_boardSize);
                m_commandThread.sendCommandClearBoard(m_boardSize);
            }
            m_board.setKomi(reader.getKomi());
            setKomi();
            Vector moves = new Vector(361, 361);
            Vector setupBlack = reader.getSetupBlack();
            for (int i = 0; i < setupBlack.size(); ++i)
            {
                go.Point p = (go.Point)setupBlack.get(i);
                moves.add(new Move(p, go.Color.BLACK));
            }
            Vector setupWhite = reader.getSetupWhite();
            for (int i = 0; i < setupWhite.size(); ++i)
            {
                go.Point p = (go.Point)setupWhite.get(i);
                moves.add(new Move(p, go.Color.WHITE));
            }
            if (m_fillPasses)
                moves = Move.fillPasses(moves, m_board.getToMove());
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                setup(m);
            }
            int numberMoves = reader.getMoves().size();
            go.Color toMove = reader.getToMove();
            if (numberMoves > 0)
                toMove = reader.getMove(0).getColor();
            if (toMove != m_board.getToMove())
            {
                Move m = new Move(null, m_board.getToMove());
                setup(m);
            }
            moves.clear();
            for (int i = 0; i < numberMoves; ++i)
                moves.add(reader.getMove(i));
            if (m_fillPasses)
                moves = Move.fillPasses(moves, m_board.getToMove());
            for (int i = 0; i < moves.size(); ++i)
            {
                Move m = (Move)moves.get(i);
                m_board.play(m);
            }
            while (m_board.getMoveNumber() > 0)
                m_board.undo();            
            if (move > 0)
                forward(move);
            m_loadedFile = file.toString();
            setTitle();
            computerNone();
            boardChanged();
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
        m_gameInfo.update();
        m_guiBoard.update();
        m_guiBoard.repaint();
    }

    private void newGameContinue(int size)
    {
        endLengthyCommand();
        showStatus(" ");
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
        m_gameInfo.update();
        m_guiBoard.update();
        m_guiBoard.repaint();
        checkComputerMove();
    }

    private void newGameFile(int size, File file, int move)
    {
        initGame(size);
        loadFile(new File(m_file), move);
        m_gameInfo.update();
        m_guiBoard.update();
    }

    private void play(Move move) throws Gtp.Error
    {
        if (m_commandThread != null)
            m_commandThread.sendCommandPlay(move);
        m_board.play(move);
    }
    
    private void resetBoard()
    {
        if (! m_boardNeedsReset)
            return;
        clearStatus();
        m_guiBoard.clearAll();
        m_guiBoard.repaint();
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
            showError("Could not run program.", e);
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
            new latex.Writer(out, m_board, false);
        else
            new sgf.Writer(out, m_board, file, "GoGui",
                           Version.m_version, m_handicap, playerBlack,
                           playerWhite, gameComment, m_score);
    }

    private void savePosition(File file) throws FileNotFoundException
    {
        OutputStream out = new FileOutputStream(file);
        if (FileUtils.hasExtension(file, "tex"))
            new latex.Writer(out, m_board, true);
        else
            new sgf.Writer(out, m_board, file, "GoGui",
                           Version.m_version);
    }

    private void sendGtp(java.io.Reader reader)
    {
        java.io.BufferedReader in;
        in = new BufferedReader(reader);
        while (true)
        {
            try
            {
                String line = in.readLine();
                if (line == null)
                {
                    in.close();
                    break;
                }
                m_gtpShell.sendCommand(line, this);
            }
            catch (IOException e)
            {
                showError("Sending commands aborted.", e);
                return;
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
            showError("Could not send commands.", e);
        }
    }

    private void sendGtpString(String commands)
    {        
        sendGtp(new StringReader(StringUtils.replace(commands, "\\n", "\n")));
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
        if (m_fillPasses)
            moves = Move.fillPasses(moves, m_board.getToMove());
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

    private void setBoardCursor(int type)
    {
        Cursor cursor = Cursor.getPredefinedCursor(type);
        assert(m_board != null);
        m_guiBoard.setCursor(cursor);
    }

    private void setBoardCursorDefault()
    {
        m_guiBoard.setCursor(Cursor.getDefaultCursor());
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
            return;
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
        }
    }

    private void setTitle()
    {
        if (m_loadedFile != null)
            setTitle(m_loadedFile);
        else if (! m_name.equals(""))
            setTitle(m_name);
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

    private void showColorBoard(String[][] board) throws Gtp.Error
    {
        m_guiBoard.showColorBoard(board);
        m_boardNeedsReset = true;
    }

    private void showDoubleBoard(double[][] board, double scale)
    {
        m_guiBoard.showDoubleBoard(board, scale);
        m_boardNeedsReset = true;
    }

    private void showError(String message, Exception e)
    {
        String m = e.getMessage();
        String c = e.getClass().getName();
        if (m == null)
            showError(message + "\n" + e.getClass().getName());
        else
            showError(message + "\n" + m);
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

    private void showPointList(go.Point pointList[]) throws Gtp.Error
    {
        m_guiBoard.showPointList(pointList);
        m_boardNeedsReset = true;
    }

    private void showStatus(String text)
    {
        m_statusLabel.setText(text);
        m_statusLabel.repaint();
    }

    private void showStringBoard(String[][] board) throws Gtp.Error
    {
        m_guiBoard.showStringBoard(board);
        m_boardNeedsReset = true;
    }

    private void showWarning(String message)
    {
        SimpleDialogs.showWarning(this, message);
    }
}

//-----------------------------------------------------------------------------
