//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package game;

//-----------------------------------------------------------------------------

/** Game tree. */
public class GameTree
{
    public GameTree()
    {
        m_gameInformation = new GameInformation();
        m_root = new Node();
    }

    public GameTree(int boardSize)
    {
        m_gameInformation = new GameInformation();
        m_root = new Node();
        m_gameInformation.m_boardSize = boardSize;
    }

    public GameTree(GameInformation gameInformation, Node root)
    {
        m_gameInformation = gameInformation;
        m_root = root;
    }

    public GameInformation getGameInformation()
    {
        return m_gameInformation;
    }

    public Node getRoot()
    {
        return m_root;
    }

    private GameInformation m_gameInformation;

    private Node m_root;
}

//-----------------------------------------------------------------------------
