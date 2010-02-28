// GameFileUtil.java

package net.sf.gogui.gamefile;

import java.io.File;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.util.ErrorMessage;

/** Static utility functions. */
public final class GameFileUtil
{
    /** Load position from SGF or XML file.
        @param file The file.
        @param maxMove A move number (or -1 for last position)
        @return Position of main variation before that move number or last
        position if game has less moves (same convention as in the loadsgf GTP
        command).
        @throws ErrorMessage If loading fails. */
    public static ConstBoard load(File file, int maxMove) throws ErrorMessage
    {
        GameReader reader = new GameReader(file);
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

    /** Make constructor unavailable; class is for namespace only. */
    private GameFileUtil()
    {
    }
}
