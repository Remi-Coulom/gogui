//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package game;

//-----------------------------------------------------------------------------

public class GameTree
{
    public GameTree(int boardSize)
    {
        m_gameInformation.m_boardSize = boardSize;
    }

    public GameInformation getGameInformation()
    {
        return m_gameInformation;
    }

    public Node getRoot()
    {
        return m_root;
    }

    private GameInformation m_gameInformation = new GameInformation();

    private Node m_root = new Node();
}

//-----------------------------------------------------------------------------
