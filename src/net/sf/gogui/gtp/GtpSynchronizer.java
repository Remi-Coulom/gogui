//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

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

    public void init(ConstBoard board) throws GtpError
    {
        initSupportedCommands();
        m_isOutOfSync = true;
        int size = board.getSize();
        m_gtp.sendBoardsize(size);
        if (m_board == null)
            m_board = new Board(size);
        else
            m_board.init(size);
        m_gtp.sendClearBoard(size);
        ArrayList toExecuteAll = computeToExecuteAll(board);
        doPlacements(toExecuteAll);
        m_isOutOfSync = false;
    }

    public void synchronize(ConstBoard board) throws GtpError
    {
        int size = board.getSize();
        if (m_board == null || size != m_board.getSize()
            || getHandicap(board) != getHandicap(m_board))
        {
            init(board);
            return;
        }
        m_isOutOfSync = true;
        ArrayList toExecuteAll = computeToExecuteAll(board);
        ArrayList toExecuteMissing = new ArrayList();
        int numberUndo = computeToExecuteMissing(toExecuteMissing, board);
        if (m_isSupportedUndo || m_isSupportedGGUndo || numberUndo == 0)
        {
            undoPlacements(numberUndo);
            doPlacements(toExecuteMissing);
            m_isOutOfSync = false;
        }
        else
            init(board);
    }

    /** Send human move to engine.
        The move is not played on the board yet. This function is useful,
        if it should be first tested, if the engine accepts a move, before
        playing it on the board, e.g. after a new human move was entered.
    */
    public void updateHumanMove(ConstBoard board, Move move) throws GtpError
    {
        ArrayList toExecuteAll = computeToExecuteAll(board);
        assert(m_board.getNumberPlacements() == toExecuteAll.size());
        assert(findNumberCommonMoves(toExecuteAll) == toExecuteAll.size());
        GoColor toMove = m_board.getToMove();
        if (m_fillPasses && toMove != move.getColor())
            play(Move.getPass(toMove));
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
        m_board.play(move);
        try
        {
            ArrayList toExecuteAll = computeToExecuteAll(board);
            assert(m_board.getNumberPlacements() == toExecuteAll.size());
            assert(findNumberCommonMoves(toExecuteAll) == toExecuteAll.size());
        }
        catch (GtpError e)
        {
            // computeToExecuteAll should not throw (no new setup
            assert(false);
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

    /** Board representing the engine state. */
    private Board m_board;

    private final Listener m_listener;

    private GtpClientBase m_gtp;

    /** Computes all placements to execute.
        Replaces setup stones by moves, if setup is not supported.
        Fills in passes between moves of same color if m_fillPasses.
    */
    private ArrayList computeToExecuteAll(ConstBoard board) throws GtpError
    {
        ArrayList toExecuteAll = new ArrayList();
        Board tempBoard = new Board(board.getSize());
        for (int i = 0; i < board.getNumberPlacements(); ++i)
        {
            Board.Placement placement = board.getPlacement(i);
            if (placement instanceof Board.Setup)
            {
                Board.Setup setup = (Board.Setup)placement;
                if (m_isSupportedSetup
                    || (placement instanceof Board.SetupHandicap
                        && m_isSupportedHandicap))
                {
                    toExecuteAll.add(placement);
                    tempBoard.doPlacement(placement);
                    continue;
                }
                // Translate into moves if possible
                ConstPointList setupBlack = setup.getBlack();
                ConstPointList setupWhite = setup.getWhite();
                ConstPointList setupEmpty = setup.getEmpty();
                if (setupEmpty.size() > 0)
                    throw new GtpError("cannot transmit setup empty as move");
                for (int j = 0; j < setupBlack.size(); ++j)
                {
                    GoPoint p = setupBlack.get(j);
                    if (tempBoard.isCaptureOrSuicide(p, GoColor.BLACK))
                        throw new GtpError("cannot transmit setup as move "
                                           + "if stones are captured");
                    placement = new Board.Play(Move.get(p, GoColor.BLACK));
                    toExecuteAll.add(placement);
                    tempBoard.doPlacement(placement);
                }
                for (int j = 0; j < setupWhite.size(); ++j)
                {
                    GoPoint p = setupWhite.get(j);
                    if (tempBoard.isCaptureOrSuicide(p, GoColor.WHITE))
                        throw new GtpError("cannot transmit setup as move "
                                           + "if stones are captured");
                    placement = new Board.Play(Move.get(p, GoColor.WHITE));
                    toExecuteAll.add(placement);
                    tempBoard.doPlacement(placement);
                }
            }
            else if (placement instanceof Board.Play)
            {
                GoColor toMove = tempBoard.getToMove();
                if (m_fillPasses
                    && ((Board.Play)placement).getMove().getColor() != toMove)
                    toExecuteAll.add(new Board.Play(Move.getPass(toMove)));
                toExecuteAll.add(placement);
                tempBoard.doPlacement(placement);
            }
        }
        return toExecuteAll;
    }

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo.
    */
    private int computeToExecuteMissing(ArrayList toExecuteMissing,
                                        ConstBoard board) throws GtpError
    {
        ArrayList toExecuteAll = computeToExecuteAll(board);
        int numberCommonMoves = findNumberCommonMoves(toExecuteAll);
        int numberUndo = m_board.getNumberPlacements() - numberCommonMoves;
        toExecuteMissing.clear();
        for (int i = numberCommonMoves; i < toExecuteAll.size(); ++i)
        {
            Board.Placement placement = (Board.Placement)toExecuteAll.get(i);
            toExecuteMissing.add(placement);
        }
        return numberUndo;
    }

    private void doPlacements(ArrayList placements) throws GtpError
    {
        for (int i = 0; i < placements.size(); ++i)
        {
            Board.Placement placement = (Board.Placement)placements.get(i);
            if (placement instanceof Board.SetupHandicap
                && m_isSupportedHandicap)
                doSetupHandicap((Board.SetupHandicap)placement);
            else if (placement instanceof Board.Setup)
                doSetup((Board.Setup)placement);
            else if (placement instanceof Board.Play)
            {
                ArrayList sequence = new ArrayList();
                while (true)
                {
                    sequence.add(((Board.Play)placement).getMove());
                    if (i == placements.size() - 1
                        || ! ((Board.Placement)placements.get(i + 1)
                              instanceof Board.Play))
                        break;
                    ++i;
                    placement = (Board.Placement)placements.get(i);
                }
                playSequence(sequence);
            }
            else
                assert(false);
        }
    }

    private void doSetup(Board.Setup setup) throws GtpError
    {
        ConstPointList black = setup.getBlack();
        ConstPointList white = setup.getWhite();
        ConstPointList empty = setup.getEmpty();
        GoColor toMove = setup.getToMove();        
        if (black.size() + white.size() + empty.size() > 0)
        {
            StringBuffer command = new StringBuffer(128);
            command.append("gogui-setup");
            for (int i = 0; i < black.size(); ++i)
            {
                command.append(" b ");
                command.append(black.get(i));
            }
            for (int i = 0; i < white.size(); ++i)
            {
                command.append(" w ");
                command.append(white.get(i));
            }
            for (int i = 0; i < empty.size(); ++i)
            {
                command.append(" e ");
                command.append(empty.get(i));
            }        
            m_gtp.send(command.toString());
        }
        m_board.setup(black, white, empty, toMove);
        if (toMove != null && m_isSupportedSetupPlayer)
            m_gtp.send("gogui-setup_player "
                       + (toMove == GoColor.BLACK ? "b" : "w"));
    }

    private void doSetupHandicap(Board.SetupHandicap setup) throws GtpError
    {
        StringBuffer command = new StringBuffer(128);
        command.append("set_free_handicap");
        for (int i = 0; i < setup.getBlack().size(); ++i)
        {
            command.append(' ');
            command.append(setup.getBlack().get(i));
        }
        m_gtp.send(command.toString());
        m_board.setupHandicap(setup.getBlack());
    }

    private int findNumberCommonMoves(ArrayList toExecuteAll)
    {
        int i;
        for (i = 0; i < toExecuteAll.size(); ++i)
        {
            if (i >= m_board.getNumberPlacements())
                break;
            Board.Placement placement = ((Board.Placement)toExecuteAll.get(i));
            if (! placement.equals(m_board.getPlacement(i)))
                break;
        }
        return i;
    }

    private static int getHandicap(ConstBoard board)
    {
        int handicap = 0;
        if (board.getNumberPlacements() == 0)
            return 0;
        Board.Placement placement = board.getPlacement(0);
        if (! (placement instanceof Board.SetupHandicap))
            return 0;
        return ((Board.SetupHandicap)placement).getBlack().size();
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
        m_board.play(move);
    }

    private void playSequence(ArrayList moves) throws GtpError
    {
        if (moves.size() > 1 && m_isSupportedPlaySequence)
        {
            String cmd = GtpClientUtil.getPlaySequenceCommand(m_gtp, moves);
            m_gtp.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_board.play((Move)moves.get(i));
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

    private void undoPlacements(int n) throws GtpError
    {
        assert(n >= 0);
        if (n == 0)
            return;
        int size = m_board.getNumberPlacements();
        for (int i = size - 1; i >= size - n; --i)
        {
            Board.Placement placement = m_board.getPlacement(i);
            if (placement instanceof Board.Setup)
            {
                Board.Setup setup = (Board.Setup)placement;
                if (setup.getBlack().size() + setup.getWhite().size()
                    + setup.getEmpty().size() > 0)
                    m_gtp.send("gogui-undo_setup");
                m_board.undo();
                if (setup.getToMove() != null && m_isSupportedSetupPlayer)
                {
                    GoColor toMove = m_board.getToMove();
                    m_gtp.send("gogui-setup_player "
                               + (toMove == GoColor.BLACK ? "b" : "w"));
                }
            }
            else if (placement instanceof Board.Play)
            {
                int numberUndo = 0;
                while (true)
                {
                    ++numberUndo;
                    if (i == size - n
                        || ! (m_board.getPlacement(i - 1)
                              instanceof Board.Play))
                        break;
                    --i;
                }
                undoMoves(numberUndo);
            }
            else
                assert(false);
        }
    }

    private void undoMoves(int n) throws GtpError
    {
        if (m_isSupportedGGUndo && (n > 1 || ! m_isSupportedUndo))
        {
            m_gtp.send("gg-undo " + n);
            m_board.undo(n);
        }
        else
        {
            assert(m_isSupportedUndo);
            for (int i = 0; i < n; ++i)
            {
                m_gtp.send("undo");
                m_board.undo();
                updateListener();
            }
        }
    }

    private void updateListener()
    {
        if (m_listener != null)
            m_listener.moveNumberChanged(m_board.getNumberPlacements());
    }
}
