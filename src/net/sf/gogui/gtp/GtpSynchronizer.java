//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.Placement;

//----------------------------------------------------------------------------

/** Synchronizes a GTP engine with a Go board.
    The GTP engine is given in form of a CommandThread.
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

    public GtpSynchronizer(GtpClientBase gtp, Callback callback)
    {
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
        m_movesToExecute.clear();
        for (int i = 0; i < board.getNumberPlacements(); ++i)
        {
            Placement placement = board.getPlacement(i);
            // Treat setup stones as moves
            Move move = Move.get(placement.getPoint(), placement.getColor());
            m_movesToExecute.add(move);
        }
        execute(m_movesToExecute);
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
        int numberUndo = computeDifference(m_movesToExecute, board);
        boolean undoSupported =
            (isCommandSupported("undo") || isCommandSupported("gg-undo"));
        if (undoSupported || numberUndo == 0)
        {
            undo(numberUndo);
            execute(m_movesToExecute);
            m_isOutOfSync = false;
        }
        else
            init(board);
    }

    /** Send human move to engine.
        The move is not played on the board yet.
    */
    public void updateHumanMove(ConstBoard board, Move move) throws GtpError
    {
        int n = board.getNumberPlacements();
        assert(m_board.getNumberPlacements() == n);
        assert(findNumberCommonMoves(board) == n);
        execute(move);
    }

    /** Update internal state after genmove.
        The computer move is already played on the board.
    */
    public void updateAfterGenmove(ConstBoard board)
    {
        int n = board.getNumberPlacements() - 1;
        assert(m_board.getNumberPlacements() == n);
        assert(findNumberCommonMoves(board) == n);
        Placement placement = board.getPlacement(n);
        assert(! placement.isSetup());
        Move move = Move.get(placement.getPoint(), placement.getColor());
        m_board.play(move);
    }

    private boolean m_isOutOfSync;

    /** Board representing the engine state. */
    private Board m_board;

    private final Callback m_callback;

    private GtpClientBase m_gtp;

    /** Local variable in some functions.
        This variable is a member for reusing between function calls.
    */
    private final ArrayList m_movesToExecute = new ArrayList(400);

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo.
    */
    private int computeDifference(ArrayList movesToExecute, ConstBoard board)
    {
        int numberCommonMoves = findNumberCommonMoves(board);
        int numberUndo = m_board.getNumberPlacements() - numberCommonMoves;
        movesToExecute.clear();
        for (int i = numberCommonMoves; i < board.getNumberPlacements(); ++i)
        {
            Placement placement = board.getPlacement(i);
            // Treat setup stones as moves
            Move move = Move.get(placement.getPoint(), placement.getColor());
            m_movesToExecute.add(move);
        }
        return numberUndo;
    }

    private void execute(ArrayList moves) throws GtpError
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
                execute((Move)moves.get(i));
                if (m_callback != null)
                    m_callback.run(m_board.getNumberPlacements());
            }
        }
    }

    private void execute(Move move) throws GtpError
    {
        m_gtp.sendPlay(move);
        m_board.play(move);
    }

    private int findNumberCommonMoves(ConstBoard board)
    {
        int numberPlacements = board.getNumberPlacements();
        int numberEngineMoves = m_board.getNumberPlacements();
        int i;
        for (i = 0; i < numberPlacements; ++i)
        {
            if (i >= numberEngineMoves)
                break;
            if (! board.getPlacement(i).equals(m_board.getPlacement(i)))
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

//----------------------------------------------------------------------------
