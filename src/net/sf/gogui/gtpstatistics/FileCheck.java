//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.util.ErrorMessage;

/** Check that SGF files meet the requirements for a GtpStatistics run. */
public class FileCheck
{
    public FileCheck(ArrayList sgfFiles, int size, boolean allowSetup)
        throws ErrorMessage
    {
        m_size = size;
        m_allowSetup = allowSetup;
        for (int i = 0; i < sgfFiles.size(); ++i)
        {
            m_file = new File((String)sgfFiles.get(i));
            checkFile();
        }
    }

    private final boolean m_allowSetup;

    private final int m_size;

    private File m_file;

    private void checkFile() throws ErrorMessage
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(m_file);
        }
        catch (FileNotFoundException e)
        {
            throwError("file not found");
        }
        SgfReader reader = new SgfReader(in, m_file, null, 0);
        GameTree tree = reader.getGameTree();
        if (tree.getBoardSize() != m_size)
            throwError("size is not " + m_size);
        ConstNode root = tree.getRoot();
        GoColor toMove = GoColor.BLACK;
        for (ConstNode node = root; node != null; node = node.getChildConst())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
            {
                if (m_allowSetup)
                {
                    if (node == root)
                        toMove = GoColor.EMPTY;
                    else
                        throw new ErrorMessage("setup stones"
                                               + " in non-root position");
                }
                else
                    throw new ErrorMessage("contains setup stones");
            }
            Move move = node.getMove();
            if (move != null)
            {
                if (toMove == GoColor.EMPTY)
                    toMove = move.getColor();
                if (move.getColor() != toMove)
                    throwError("non-alternating moves");
                toMove = toMove.otherColor();
            }
        }
    }

    private void throwError(String reason) throws ErrorMessage
    {
        throw new ErrorMessage(m_file + ": " + reason);
    }
}

