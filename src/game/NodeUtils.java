//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package game;

//-----------------------------------------------------------------------------

public class NodeUtils
{
    public static Node findNextComment(Node node)
    {
        while (node != null)
        {
            Node father = node;
            node = node.getChild();
            if (node == null)
                node = father.getNextVariation();
            if (node != null && node.getComment() != null)
                break;
        }
        return node;
    }
}

//-----------------------------------------------------------------------------
