//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.util.ErrorMessage;

/** Static utility functions. */
public final class SgfUtil
{
    /** Load position from SGF file.
        @param file The file.
        @param maxMove A move number (or -1 for last position)
        @return Position of main variation before that move number or last
        position if game has less moves (same convention as in the loadsgf GTP
        command).
        @throws ErrorMessage If loading fails.
    */
    public static ConstBoard loadSgf(File file, int maxMove)
        throws ErrorMessage
    {
        try
        {
            FileInputStream fileStream = new FileInputStream(file);
            SgfReader reader = new SgfReader(fileStream, file, null, 0);
            GameTree tree = reader.getTree();
            Game game = new Game(tree);
            ConstNode node = tree.getRoot();
            int moveNumber = 0;
            while (true)
            {
                if (node.getMove() != null)
                {
                    ++moveNumber;
                    if (maxMove >= 0 && moveNumber >= maxMove)
                        break;
                }
                ConstNode child = node.getChildConst();
                if (child == null)
                    break;
                node = child;
            }
            game.gotoNode(node);
            return game.getBoard();
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        catch (SgfReader.SgfError e)
        {
            throw new ErrorMessage(e.getMessage());
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private SgfUtil()
    {
    }
}

