//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.util.ArrayList;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtils;

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

    public GtpSynchronizer(Callback callback)
    {
        m_callback = callback;
        int initialCapacity = 400;
        m_engineMoves = new ArrayList(initialCapacity);
        m_movesToExecute = new ArrayList(initialCapacity);
        setCommandThread(null);
    }

    /** Did the last GtpSynchronizer.synchronize() fail? */
    public boolean isOutOfSync()
    {
        return m_commandThread != null && m_isOutOfSync;
    }

    public void setCommandThread(CommandThread commandThread)
    {
        m_commandThread = commandThread;
        m_isOutOfSync = true;
        m_engineBoardSize = -1;
        m_engineMoves.clear();
    }

    public void init(ConstBoard board) throws GtpError
    {
        if (m_commandThread == null)
            return;
        int size = board.getSize();
        m_commandThread.sendBoardsize(size);
        m_engineBoardSize = size;
        // Not defined what the state between boardsize and clear_board in
        // GTP version 2 is, but it could be that moves are kept on board
        // (e.g. for keeping joseki moves while changing board size),
        // so we set m_engineBoardSize now, but clear the engine moves after
        // sendClearBoard()
        m_commandThread.sendClearBoard(size);
        m_engineMoves.clear();
        m_movesToExecute.clear();
        int moveNumber = board.getMoveNumber();
        for (int i = 0; i < moveNumber; ++i)
            m_movesToExecute.add(board.getMove(i));
        execute(m_movesToExecute);
        m_isOutOfSync = false;
    }

    public void synchronize(ConstBoard board) throws GtpError
    {
        if (m_commandThread == null)
            return;
        m_isOutOfSync = true;
        int size = board.getSize();
        if (size == m_engineBoardSize)
        {
            int numberUndo = computeDifference(m_movesToExecute, board);
            undo(numberUndo);
            execute(m_movesToExecute);
            m_isOutOfSync = false;
        }
        else
        {
            init(board);
            m_isOutOfSync = false;
        }
    }

    public void updateAfterGenmove(ConstBoard board)
    {
        if (m_commandThread == null)
            return;
        int n = board.getMoveNumber() - 1;
        assert(m_engineMoves.size() == n);
        assert(findNumberCommonMoves(board) == n);
        m_engineMoves.add(board.getMove(n));
    }

    private boolean m_isOutOfSync;

    private int m_engineBoardSize;

    private final Callback m_callback;

    private CommandThread m_commandThread;

    /** Move successfully executed at engine. */
    private final ArrayList m_engineMoves;

    /** Local variable in some functions.
        This variable is a member for reusing between function calls.
    */
    private final ArrayList m_movesToExecute;

    /** Compute number of moves to undo and moves to execute.
        @return Number of moves to undo.
    */
    private int computeDifference(ArrayList movesToExecute, ConstBoard board)
    {
        int numberCommonMoves = findNumberCommonMoves(board);
        int numberUndo = m_engineMoves.size() - numberCommonMoves;
        int moveNumber = board.getMoveNumber();
        movesToExecute.clear();
        for (int i = numberCommonMoves; i < moveNumber; ++i)
            movesToExecute.add(board.getMove(i));
        return numberUndo;
    }

    private void execute(ArrayList moves) throws GtpError
    {
        if (moves.size() > 1
            && m_commandThread.isCommandSupported("play_sequence"))
        {
            String cmd = GtpUtils.getPlaySequenceCommand(moves);
            m_commandThread.send(cmd);
            for (int i = 0; i < moves.size(); ++i)
                m_engineMoves.add((Move)moves.get(i));
        }
        else
        {
            for (int i = 0; i < moves.size(); ++i)
            {
                Move move = (Move)moves.get(i);
                m_commandThread.sendPlay(move);
                m_engineMoves.add((Move)moves.get(i));
                if (m_callback != null)
                    m_callback.run(m_engineMoves.size());
            }
        }
    }

    private int findNumberCommonMoves(ConstBoard board)
    {
        int moveNumber = board.getMoveNumber();
        int numberEngineMoves = m_engineMoves.size();
        int i;
        for (i = 0; i < moveNumber; ++i)
            if (i >= numberEngineMoves
                || board.getMove(i) != (Move)m_engineMoves.get(i))
                break;
        return i;
    }

    private void undo(int n) throws GtpError
    {
        if (n == 0)
            return;
        assert(n > 0);
        if (n > 1 && m_commandThread.isCommandSupported("gg-undo"))
        {
            m_commandThread.send("gg-undo " + n);
            for (int i = 0; i < n; ++i)                
                m_engineMoves.remove(m_engineMoves.size() - 1);
        }
        else
        {
            if (! m_commandThread.isCommandSupported("undo"))
                throw new GtpError("Program does not support undo");
            for (int i = 0; i < n; ++i)
            {
                m_commandThread.send("undo");
                m_engineMoves.remove(m_engineMoves.size() - 1);
                if (m_callback != null)
                    m_callback.run(m_engineMoves.size());
            }
        }
    }
}

//----------------------------------------------------------------------------
