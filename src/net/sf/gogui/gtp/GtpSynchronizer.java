//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.Placement;

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
    public interface Callback
    {
        void run(int moveNumber);
    }

    public GtpSynchronizer(GtpClientBase gtp)
    {
        m_fillPasses = false;
        m_gtp = gtp;
        m_callback = null;
        m_isOutOfSync = true;
    }

    public GtpSynchronizer(GtpClientBase gtp, Callback callback,
                           boolean fillPasses)
    {
        m_fillPasses = fillPasses;
        m_gtp = gtp;
        m_callback = callback;
        m_isOutOfSync = true;
    }

    /** Did the last GtpSynchronizer.synchronize() fail? */
    public boolean isOutOfSync()
    {
        return m_isOutOfSync;
    }

    public void init(ConstBoard board) throws GtpError
    {
        m_isOutOfSync = true;
        int size = board.getSize();
        m_gtp.sendBoardsize(size);
        if (m_board == null)
            m_board = new Board(size);
        else
            m_board.init(size);
        m_gtp.sendClearBoard(size);
        computeToExecuteAll(board, m_toExecuteAll);
        doPlacements(m_toExecuteAll);
        m_isOutOfSync = false;
    }

    public void synchronize(ConstBoard board) throws GtpError
    {
        int size = board.getSize();
        if (m_board == null || size != m_board.getSize())
        {
            init(board);
            return;
        }
        m_isOutOfSync = true;
        computeToExecuteAll(board, m_toExecuteAll);
        int numberUndo = computeToExecuteMissing(m_toExecuteMissing, board);
        boolean undoSupported =
            (isCommandSupported("undo") || isCommandSupported("gg-undo"));
        if (undoSupported || numberUndo == 0)
        {
            undo(numberUndo);
            doPlacements(m_toExecuteMissing);
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
        computeToExecuteAll(board, m_toExecuteAll);
        assert(m_board.getNumberPlacements() == m_toExecuteAll.size());
        assert(findNumberCommonMoves(m_toExecuteAll) == m_toExecuteAll.size());
        GoColor toMove = m_board.getToMove();
        if (m_fillPasses && toMove != move.getColor())
            play(Move.get(null, toMove));
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
        Placement placement =
            board.getPlacement(board.getNumberPlacements() - 1);
        assert(! placement.isSetup());
        Move move = Move.get(placement.getPoint(), placement.getColor());
        m_board.play(move);
        try
        {
            computeToExecuteAll(board, m_toExecuteAll);
        }
        catch (GtpError e)
        {
            // computeToExecuteAll should not throw (no new setup
            assert(false);
        }
        assert(m_board.getNumberPlacements() == m_toExecuteAll.size());
        assert(findNumberCommonMoves(m_toExecuteAll) == m_toExecuteAll.size());
    }

    private boolean m_fillPasses;

    private boolean m_isOutOfSync;

    /** Board representing the engine state. */
    private Board m_board;

    /** Local variable; reused for efficiency. */
    private Board m_tempBoard = new Board(19);

    private final Callback m_callback;

    private GtpClientBase m_gtp;

    /** Local variable; reused for efficiency. */
    private final ArrayList m_sequence = new ArrayList(400);

    /** Local variable in some functions; reused for efficiency. */
    private final ArrayList m_toExecuteAll = new ArrayList(400);

    /** Local variable in some functions; reused for efficiency. */
    private final ArrayList m_toExecuteMissing = new ArrayList(400);

    /** Computes all placements to execute.
        Replaces setup stones by moves, if setup is not supported.
        Fills in passes between moves of same color if m_fillPasses.
    */
    private void computeToExecuteAll(ConstBoard board, ArrayList toExecuteAll)
        throws GtpError
    {
        toExecuteAll.clear();
        //boolean isSetupSupported = isCommandSupported("setup");
        boolean isSetupSupported = false; // TODO: implement setup command
        m_tempBoard.init(board.getSize());
        for (int i = 0; i < board.getNumberPlacements(); ++i)
        {
            Placement placement = board.getPlacement(i);
            GoColor color = placement.getColor();            
            GoPoint point = placement.getPoint();
            if (placement.isSetup() && ! isSetupSupported)
            {
                if (color == GoColor.EMPTY)
                    throw new GtpError("program does not support setup empty");
                if (m_tempBoard.isCaptureOrSuicide(point, color))
                    throw new GtpError("program does not support setup");
                placement = new Placement(point, color, false);
            }
            boolean isSetup = placement.isSetup();
            GoColor toMove = m_tempBoard.getToMove();
            if (m_fillPasses && ! isSetup && color != toMove)
                toExecuteAll.add(new Placement(null, toMove, false));
            toExecuteAll.add(placement);
            m_tempBoard.doPlacement(placement);
        }
    }

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo.
    */
    private int computeToExecuteMissing(ArrayList toExecuteMissing,
                                        ConstBoard board) throws GtpError
    {
        computeToExecuteAll(board, m_toExecuteAll);
        int numberCommonMoves = findNumberCommonMoves(m_toExecuteAll);
        int numberUndo = m_board.getNumberPlacements() - numberCommonMoves;
        toExecuteMissing.clear();
        for (int i = numberCommonMoves; i < m_toExecuteAll.size(); ++i)
        {
            Placement placement = (Placement)m_toExecuteAll.get(i);
            toExecuteMissing.add(placement);
        }
        return numberUndo;
    }

    private void doPlacements(ArrayList placements) throws GtpError
    {
        m_sequence.clear();
        for (int i = 0; i < placements.size(); ++i)
        {
            Placement placement = (Placement)placements.get(i);
            assert(! placement.isSetup()); // TODO: handle setup
            Move move = Move.get(placement.getPoint(), placement.getColor());
            m_sequence.add(move);
        }
        playSequence(m_sequence);
    }

    private void playSequence(ArrayList moves) throws GtpError
    {
        if (moves.size() > 1 && isCommandSupported("play_sequence"))
        {
            String cmd = GtpUtil.getPlaySequenceCommand(moves);
            m_gtp.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_board.play((Move)moves.get(i));
        }
        else
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                play((Move)moves.get(i));
                if (m_callback != null)
                    m_callback.run(m_board.getNumberPlacements());
            }
        }
    }

    private void play(Move move) throws GtpError
    {
        m_gtp.sendPlay(move);
        m_board.play(move);
    }

    private int findNumberCommonMoves(ArrayList toExecuteAll)
    {
        int i;
        for (i = 0; i < toExecuteAll.size(); ++i)
        {
            if (i >= m_board.getNumberPlacements())
                break;
            Placement placement = ((Placement)toExecuteAll.get(i));
            if (! placement.equals(m_board.getPlacement(i)))
                break;
        }
        return i;
    }

    private boolean isCommandSupported(String command)
    {
        return m_gtp.isCommandSupported(command);
    }

    private void undo(int n) throws GtpError
    {
        if (n == 0)
            return;
        assert(n > 0);
        if (isCommandSupported("gg-undo")
            && (n > 1 || ! isCommandSupported("undo")))
        {
            m_gtp.send("gg-undo " + n);
            m_board.undo(n);
        }
        else
        {
            if (! isCommandSupported("undo"))
                throw new GtpError("Program does not support undo");
            for (int i = 0; i < n; ++i)
            {
                m_gtp.send("undo");
                m_board.undo();
                if (m_callback != null)
                    m_callback.run(m_board.getNumberPlacements());
            }
        }
    }
}

