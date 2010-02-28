// GtpSynchronizer.java

package net.sf.gogui.gtp;

import java.util.ArrayList;
import java.util.List;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;
import static net.sf.gogui.go.GoColor.WHITE_BLACK;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.ObjectUtil;

/** Synchronizes a GTP engine with a Go board.
    Handles different capabilities of different engines.
    If GtpSynchronizer is used, no game state changing GTP commands (like
    clear_board, play, undo, komi, time_settings) should be sent to this
    engine outside this class. */
public class GtpSynchronizer
{
    /** Callback that is called after each change in the engine's move
        number.
        Necessary, because sending multiple undo or play commands can be
        a slow operation. */
    public interface Listener
    {
        void moveNumberChanged(int moveNumber);
    }

    public GtpSynchronizer(GtpClientBase gtp)
    {
        this(gtp, null, false);
    }

    public GtpSynchronizer(GtpClientBase gtp, Listener listener,
                           boolean fillPasses)
    {
        m_fillPasses = fillPasses;
        m_gtp = gtp;
        m_listener = listener;
        m_isOutOfSync = true;
        m_komi = null;
        m_timeSettings = null;
    }

    /** Did the last GtpSynchronizer.synchronize() fail? */
    public boolean isOutOfSync()
    {
        return m_isOutOfSync;
    }

    public void init(ConstBoard board, Komi komi, TimeSettings timeSettings)
        throws GtpError
    {
        initSupportedCommands();
        m_isOutOfSync = true;
        int size = board.getSize();
        m_engineState = null;
        m_gtp.sendBoardsize(size);
        m_engineState = new Board(size);
        m_gtp.sendClearBoard(size);
        sendGameInfo(komi, timeSettings);
        ConstBoard targetState = computeTargetState(board);
        setup(targetState);
        ArrayList<Move> moves = new ArrayList<Move>();
        for (int i = 0; i < targetState.getNumberMoves(); ++i)
            moves.add(targetState.getMove(i));
        play(moves);
        m_isOutOfSync = false;
    }

    public void synchronize(ConstBoard board, Komi komi,
                            TimeSettings timeSettings) throws GtpError
    {
        int size = board.getSize();
        ConstBoard targetState = computeTargetState(board);
        if (m_engineState == null || size != m_engineState.getSize()
            || isSetupDifferent(targetState))
        {
            init(board, komi, timeSettings);
            return;
        }
        m_isOutOfSync = true;
        ArrayList<Move> moves = new ArrayList<Move>();
        int numberUndo = computeToPlay(moves, targetState);
        if (numberUndo == 0 || m_isSupportedUndo || m_isSupportedGGUndo)
        {
            undo(numberUndo);
            // Send komi/time_settings before play commands, some engines
            // cannot handle them otherwise
            sendGameInfo(komi, timeSettings);
            play(moves);
            m_isOutOfSync = false;
        }
        else
            init(board, komi, timeSettings);
    }

    /** Send human move to engine.
        The move is not played on the board yet. This function is useful,
        if it should be first tested, if the engine accepts a move, before
        playing it on the board, e.g. after a new human move was entered. */
    public void updateHumanMove(ConstBoard board, Move move) throws GtpError
    {
        ConstBoard targetState = computeTargetState(board);
        assert findNumberCommonMoves(targetState)
            == targetState.getNumberMoves();
        if (m_fillPasses && m_engineState.getNumberMoves() > 0)
        {
            Move lastMove = m_engineState.getLastMove();
            GoColor c = move.getColor();
            if (lastMove.getColor() == c)
                play(Move.getPass(c.otherColor()));
        }
        play(move);
    }

    /** Update internal state after genmove.
        Needs to be called after each genmove (or kgs-genmove_cleanup)
        command. The computer move is expected to be already played on the
        board, but does not need to be transmitted to the program anymore,
        because genmove already executes the move at the program's side. */
    public void updateAfterGenmove(ConstBoard board)
    {
        Move move = board.getLastMove();
        assert move != null;
        m_engineState.play(move);
        try
        {
            ConstBoard targetState = computeTargetState(board);
            assert findNumberCommonMoves(targetState)
                == targetState.getNumberMoves();
        }
        catch (GtpError e)
        {
            // computeTargetState should not throw (no new setup
            assert false;
        }
    }

    private boolean m_fillPasses;

    private boolean m_isOutOfSync;

    private boolean m_isSupportedHandicap;

    private boolean m_isSupportedPlaySequence;

    private boolean m_isSupportedSetupPlayer;

    private boolean m_isSupportedGGUndo;

    private boolean m_isSupportedUndo;

    private boolean m_isSupportedSetup;

    private Komi m_komi;

    private TimeSettings m_timeSettings;

    private final Listener m_listener;

    private GtpClientBase m_gtp;

    private Board m_engineState;

    /** Computes all actions to execute.
        Replaces setup stones by moves, if setup is not supported.
        Fills in passes between moves of same color if m_fillPasses. */
    private Board computeTargetState(ConstBoard board) throws GtpError
    {
        int size = board.getSize();
        Board targetState = new Board(size);
        ConstPointList setupBlack = board.getSetup(BLACK);
        ConstPointList setupWhite = board.getSetup(WHITE);
        GoColor setupPlayer = board.getSetupPlayer();
        if (setupBlack.size() > 0 || setupWhite.size() > 0)
        {
            targetState.clear();
            boolean isHandicap = board.isSetupHandicap();
            if (isHandicap && m_isSupportedHandicap)
                targetState.setupHandicap(setupBlack);
            else if (m_isSupportedSetup)
                targetState.setup(setupBlack, setupWhite, setupPlayer);
            else
            {
                // Translate setup into moves
                // Send moves of color to move first, such that the right color
                // is to move after the setup (works only if there are setup
                // stones by both colors)
                List<GoColor> colors;
                if (setupPlayer == BLACK)
                    colors = BLACK_WHITE;
                else
                    colors = WHITE_BLACK;
                for (GoColor c : colors)
                {
                    for (GoPoint p : board.getSetup(c))
                    {
                        if (targetState.isCaptureOrSuicide(c, p))
                        {
                            String message =
                                "cannot transmit setup as " +
                                "move if stones are captured";
                            throw new GtpError(message);
                        }
                        targetState.play(Move.get(c, p));
                    }
                }
            }
        }
        for (int i = 0; i < board.getNumberMoves(); ++i)
        {
            Move move = board.getMove(i);
            GoColor toMove = targetState.getToMove();
            if (m_fillPasses && move.getColor() != toMove)
                targetState.play(Move.getPass(toMove));
            targetState.play(move);
        }
        return targetState;
    }

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo. */
    private int computeToPlay(ArrayList<Move> moves, ConstBoard targetState)
        throws GtpError
    {
        int numberCommonMoves = findNumberCommonMoves(targetState);
        int numberUndo = m_engineState.getNumberMoves() - numberCommonMoves;
        moves.clear();
        for (int i = numberCommonMoves; i < targetState.getNumberMoves(); ++i)
            moves.add(targetState.getMove(i));
        return numberUndo;
    }

    private int findNumberCommonMoves(ConstBoard targetState)
    {
        int i;
        for (i = 0; i < targetState.getNumberMoves(); ++i)
        {
            if (i >= m_engineState.getNumberMoves())
                break;
            Move move = (Move)targetState.getMove(i);
            if (! move.equals(m_engineState.getMove(i)))
                break;
        }
        return i;
    }

    private boolean isSetupDifferent(ConstBoard targetState)
    {
        if (m_engineState.isSetupHandicap() != targetState.isSetupHandicap())
            return true;
        if (! ObjectUtil.equals(m_engineState.getSetupPlayer(),
                                targetState.getSetupPlayer()))
            return true;
        for (GoColor c : BLACK_WHITE)
            if (! m_engineState.getSetup(c).equals(targetState.getSetup(c)))
                return true;
        return false;
    }

    private void initSupportedCommands()
    {
        m_isSupportedPlaySequence =
            GtpClientUtil.isPlaySequenceSupported(m_gtp);
        m_isSupportedUndo = isSupported("undo");
        m_isSupportedGGUndo = isSupported("gg-undo");
        m_isSupportedSetup = isSupported("gogui-setup");
        m_isSupportedSetupPlayer = isSupported("gogui-setup_player");
        m_isSupportedHandicap = isSupported("set_free_handicap");
    }

    private boolean isSupported(String command)
    {
        return m_gtp.isSupported(command);
    }

    private void play(Move move) throws GtpError
    {
        m_gtp.sendPlay(move);
        m_engineState.play(move);
    }

    private void play(ArrayList<Move> moves) throws GtpError
    {
        if (moves.isEmpty())
            return;
        if (moves.size() > 1 && m_isSupportedPlaySequence)
        {
            String cmd = GtpClientUtil.getPlaySequenceCommand(m_gtp, moves);
            m_gtp.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_engineState.play(moves.get(i));
        }
        else
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                play(moves.get(i));
                updateListener();
            }
        }
    }

    private void sendGameInfo(Komi komi, TimeSettings timeSettings)
    {
        if (! ObjectUtil.equals(komi, m_komi))
        {
            m_komi = komi;
            if (m_gtp.isSupported("komi") && komi != null)
            {
                try
                {
                    m_gtp.send("komi " + komi);
                }
                catch (GtpError e)
                {
                }
            }
        }
        if (! ObjectUtil.equals(timeSettings, m_timeSettings))
        {
            m_timeSettings = timeSettings;
            if (m_gtp.isSupported("time_settings"))
            {
                try
                {
                    m_gtp.send(GtpUtil.getTimeSettingsCommand(timeSettings));
                }
                catch (GtpError e)
                {
                }
            }
        }
    }

    private void setup(ConstBoard targetState) throws GtpError
    {
        ConstPointList setupBlack = targetState.getSetup(BLACK);
        ConstPointList setupWhite = targetState.getSetup(WHITE);
        GoColor setupPlayer = targetState.getSetupPlayer();
        if (setupBlack.size() == 0 && setupWhite.size() == 0)
            return;
        if (targetState.isSetupHandicap())
        {
            StringBuilder command = new StringBuilder(128);
            command.append("set_free_handicap");
            for (GoPoint p : setupBlack)
            {
                command.append(' ');
                command.append(p);
            }
            m_gtp.send(command.toString());
            m_engineState.setupHandicap(setupBlack);
        }
        else
        {
            StringBuilder command = new StringBuilder(128);
            command.append("gogui-setup");
            for (GoColor c : BLACK_WHITE)
            {
                for (GoPoint p : targetState.getSetup(c))
                {
                    command.append(' ');
                    command.append(Move.get(c, p));
                }
            }
            m_gtp.send(command.toString());
            m_engineState.setup(setupBlack, setupWhite, setupPlayer);
            if (setupPlayer != null && m_isSupportedSetupPlayer)
                m_gtp.send("gogui-setup_player "
                           + setupPlayer.getUppercaseLetter());
        }
    }

    private void undo(int n) throws GtpError
    {
        if (n == 0)
            return;
        if (m_isSupportedGGUndo && (n > 1 || ! m_isSupportedUndo))
        {
            m_gtp.send("gg-undo " + n);
            m_engineState.undo(n);
        }
        else
        {
            assert m_isSupportedUndo;
            for (int i = 0; i < n; ++i)
            {
                m_gtp.send("undo");
                m_engineState.undo();
                updateListener();
            }
        }
    }

    private void updateListener()
    {
        if (m_listener != null)
            m_listener.moveNumberChanged(m_engineState.getNumberMoves());
    }
}
