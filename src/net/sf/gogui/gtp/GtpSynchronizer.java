//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.ObjectUtil;

/** Synchronizes a GTP engine with a Go board.
    Handles different capabilities of different engines.
    If GtpSynchronizer is used, no position changing GTP commands (like
    clear_board, play, undo) should be sent to this engine outside this
    class.
*/
public class GtpSynchronizer
{
    /** Callback that is called after each change in the engine's move
        number.
        Necessary, because sending multiple undo or play commands can be
        a slow operation.
    */
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
        m_engineState = new EngineState(-1);
        m_gtp.sendBoardsize(size);
        m_engineState.m_boardSize = size;
        m_gtp.sendClearBoard(size);
        EngineState targetState = computeTargetState(board);
        setup(targetState);
        play(targetState.m_moves);
        m_komi = null;
        m_timeSettings = null;
        sendGameInfo(komi, timeSettings);
        m_isOutOfSync = false;
    }

    public void synchronize(ConstBoard board, Komi komi,
                            TimeSettings timeSettings) throws GtpError
    {
        int size = board.getSize();
        EngineState targetState = computeTargetState(board);
        if (m_engineState == null || size != m_engineState.m_boardSize
            || m_engineState.isSetupDifferent(targetState))
        {
            init(board, komi, timeSettings);
            return;
        }
        m_isOutOfSync = true;
        ArrayList moves = new ArrayList();
        int numberUndo = computeToPlay(moves, targetState);
        if (numberUndo == 0 || m_isSupportedUndo || m_isSupportedGGUndo)
        {
            undo(numberUndo);
            play(moves);
            sendGameInfo(komi, timeSettings);
            m_isOutOfSync = false;
        }
        else
            init(board, komi, timeSettings);
    }

    /** Send human move to engine.
        The move is not played on the board yet. This function is useful,
        if it should be first tested, if the engine accepts a move, before
        playing it on the board, e.g. after a new human move was entered.
    */
    public void updateHumanMove(ConstBoard board, Move move) throws GtpError
    {
        EngineState targetState = computeTargetState(board);
        assert(findNumberCommonMoves(targetState)
               == targetState.m_moves.size());
        int engineMoves = m_engineState.m_moves.size();
        if (m_fillPasses && engineMoves > 0)
        {
            Move lastMove = (Move)m_engineState.m_moves.get(engineMoves - 1);
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
        because genmove already executes the move at the program's side.
    */
    public void updateAfterGenmove(ConstBoard board)
    {
        Move move = board.getLastMove();
        assert(move != null);
        m_engineState.m_moves.add(move);
        try
        {
            EngineState targetState = computeTargetState(board);
            assert(findNumberCommonMoves(targetState)
                   == targetState.m_moves.size());
        }
        catch (GtpError e)
        {
            // computeTargetState should not throw (no new setup
            assert(false);
        }
    }

    private static class EngineState
    {
        public int m_boardSize;

        public boolean m_isSetupHandicap;

        /** Black and white setup stones. */
        public PointList[] m_setup;

        public GoColor m_setupPlayer;

        public ArrayList m_moves;

        public EngineState(int boardSize)
        {
            m_boardSize = boardSize;
            clear();
        }

        public void clear()
        {
            m_isSetupHandicap = false;
            m_setup = new PointList[2];
            m_setup[0] = new PointList();
            m_setup[1] = new PointList();
            m_moves = new ArrayList();
        }

        PointList getSetup(GoColor c)
        {
            return m_setup[c.toInteger()];
        }

        public boolean isSetupDifferent(EngineState state)
        {
            if (m_isSetupHandicap != state.m_isSetupHandicap)
                return true;
            if (! ObjectUtil.equals(m_setupPlayer, state.m_setupPlayer))
                return true;
            for (GoColor c = GoColor.BLACK; c != null;
                 c = c.getNextBlackWhite())
                if (! getSetup(c).equals(state.getSetup(c)))
                    return true;
            return false;
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

    private EngineState m_engineState;

    /** Computes all actions to execute.
        Replaces setup stones by moves, if setup is not supported.
        Fills in passes between moves of same color if m_fillPasses.
    */
    private EngineState computeTargetState(ConstBoard board) throws GtpError
    {
        int size = board.getSize();
        EngineState engineState = new EngineState(size);
        Board tempBoard = new Board(size);
        for (int i = 0; i < board.getNumberActions(); ++i)
        {
            Board.Action action = board.getAction(i);
            if (action instanceof Board.Setup)
            {
                engineState.clear();
                Board.Setup setup = (Board.Setup)action;
                boolean isHandicap = (action instanceof Board.SetupHandicap);
                if (m_isSupportedSetup
                    || (isHandicap && m_isSupportedHandicap))
                {
                    tempBoard.doAction(action);
                    engineState.m_isSetupHandicap = isHandicap;
                    for (int j = 0; j < board.getNumberPoints(); ++j)
                    {
                        GoPoint p = board.getPoint(j);
                        GoColor c = tempBoard.getColor(p);
                        if (c != GoColor.EMPTY)
                            engineState.getSetup(c).add(p);
                    }
                    engineState.m_setupPlayer = setup.getToMove();
                }
                else
                {
                    tempBoard.doAction(action);
                    for (int j = 0; j < board.getNumberPoints(); ++j)
                    {
                        GoPoint p = board.getPoint(j);
                        GoColor c = tempBoard.getColor(p);
                        if (c != GoColor.EMPTY)
                        {
                            if (tempBoard.isCaptureOrSuicide(c, p))
                            {
                                String message =
                                    "cannot transmit setup as " +
                                    "move if stones are captured";
                                throw new GtpError(message);
                            }
                            Move move = Move.get(c, p);
                            engineState.m_moves.add(move);
                        }
                    }
                }
            }
            else if (action instanceof Board.Play)
            {
                GoColor toMove = tempBoard.getToMove();
                if (m_fillPasses
                    && ((Board.Play)action).getMove().getColor() != toMove)
                    engineState.m_moves.add(Move.getPass(toMove));
                Move move = ((Board.Play)action).getMove();
                engineState.m_moves.add(move);
                tempBoard.play(move);
            }
        }
        return engineState;
    }

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo.
    */
    private int computeToPlay(ArrayList moves, EngineState targetState)
        throws GtpError
    {
        int numberCommonMoves = findNumberCommonMoves(targetState);
        int numberUndo = m_engineState.m_moves.size() - numberCommonMoves;
        moves.clear();
        for (int i = numberCommonMoves; i < targetState.m_moves.size(); ++i)
            moves.add(targetState.m_moves.get(i));
        return numberUndo;
    }

    private int findNumberCommonMoves(EngineState targetState)
    {
        int i;
        for (i = 0; i < targetState.m_moves.size(); ++i)
        {
            if (i >= m_engineState.m_moves.size())
                break;
            Move move = (Move)targetState.m_moves.get(i);
            if (! move.equals(m_engineState.m_moves.get(i)))
                break;
        }
        return i;
    }

    private static int getHandicap(ConstBoard board)
    {
        if (board.getNumberActions() == 0)
            return 0;
        Board.Action action = board.getAction(0);
        if (! (action instanceof Board.SetupHandicap))
            return 0;
        return ((Board.SetupHandicap)action).getHandicapStones().size();
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
        m_engineState.m_moves.add(move);
    }

    private void play(ArrayList moves) throws GtpError
    {
        if (moves.size() == 0)
            return;
        if (moves.size() > 1 && m_isSupportedPlaySequence)
        {
            String cmd = GtpClientUtil.getPlaySequenceCommand(m_gtp, moves);
            m_gtp.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_engineState.m_moves.add((Move)moves.get(i));
        }
        else
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                play((Move)moves.get(i));
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
            if (timeSettings == null && m_timeSettings == null)
            {
                // Avoid sending "no time limit" settings, if not necessary
                // because it could confuse some programs
                // (see GtpUtil.getTimeSettingsCommand())
                m_timeSettings = timeSettings;
                return;
            }
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

    private void setup(EngineState targetState) throws GtpError
    {
        if (targetState.m_isSetupHandicap)
        {
            ConstPointList stones = targetState.getSetup(GoColor.BLACK);
            StringBuffer command = new StringBuffer(128);
            command.append("set_free_handicap");
            for (int i = 0; i < stones.size(); ++i)
            {
                command.append(' ');
                command.append(stones.get(i));
            }
            m_gtp.send(command.toString());
            m_engineState.m_isSetupHandicap = true;
            m_engineState.m_setup[GoColor.BLACK.toInteger()]
                = new PointList(stones);
            m_engineState.m_setupPlayer = GoColor.WHITE;
        }
        else
        {
            if (targetState.getSetup(GoColor.BLACK).size() > 0
                || targetState.getSetup(GoColor.WHITE).size() > 0)
            {
                StringBuffer command = new StringBuffer(128);
                command.append("gogui-setup");
                for (GoColor c = GoColor.BLACK; c != null;
                     c = c.getNextBlackWhite())
                {
                    ConstPointList stones = targetState.getSetup(c);
                    for (int i = 0; i < stones.size(); ++i)
                    {
                        if (c == GoColor.BLACK)
                            command.append(" b ");
                        else
                            command.append(" w ");
                        command.append(stones.get(i));
                    }
                }
                m_gtp.send(command.toString());
                m_engineState.m_isSetupHandicap = false;
                m_engineState.m_setup[GoColor.BLACK.toInteger()] =
                    new PointList(targetState.getSetup(GoColor.BLACK));
                m_engineState.m_setup[GoColor.WHITE.toInteger()] =
                    new PointList(targetState.getSetup(GoColor.WHITE));
            }
            GoColor player = targetState.m_setupPlayer;
            if (player != null && m_isSupportedSetupPlayer)
                m_gtp.send("gogui-setup_player "
                           + (player == GoColor.BLACK ? "b" : "w"));
            m_engineState.m_setupPlayer = player;
        }
    }

    private void undo(int n) throws GtpError
    {
        if (n == 0)
            return;
        if (m_isSupportedGGUndo && (n > 1 || ! m_isSupportedUndo))
        {
            m_gtp.send("gg-undo " + n);
            for (int i = 0; i < n; ++i)
                m_engineState.m_moves.remove(m_engineState.m_moves.size() - 1);
        }
        else
        {
            assert(m_isSupportedUndo);
            for (int i = 0; i < n; ++i)
            {
                m_gtp.send("undo");
                m_engineState.m_moves.remove(m_engineState.m_moves.size() - 1);
                updateListener();
            }
        }
    }

    private void updateListener()
    {
        if (m_listener != null)
            m_listener.moveNumberChanged(m_engineState.m_moves.size());
    }
}
