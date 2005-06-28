//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Vector;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;

//----------------------------------------------------------------------------

public class FileCheck
{
    public FileCheck(Vector sgfFiles, int size) throws ErrorMessage
    {
        m_size = size;
        for (int i = 0; i < sgfFiles.size(); ++i)
        {
            m_name = (String)sgfFiles.get(i);
            checkFile();
        }
    }

    private int m_size;

    private String m_name;

    private void checkFile() throws ErrorMessage
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(new File(m_name));
        }
        catch (FileNotFoundException e)
        {
            throwError("file not found");
        }
        SgfReader reader = new SgfReader(in, m_name, null, 0);
        GameTree tree = reader.getGameTree();
        GameInformation info = tree.getGameInformation();
        if (info.m_boardSize != m_size)
            throwError("size is not " + m_size);
        Node root = tree.getRoot();
        GoColor toMove = GoColor.BLACK;
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
                throwError("contains setup stones");
            Move move = node.getMove();
            if (move != null)
            {
                if (move.getColor() != toMove)
                    throwError("non-alternating moves");
                toMove = toMove.otherColor();
            }
        }
    }

    private void throwError(String reason) throws ErrorMessage
    {
        throw new ErrorMessage(m_name + ": " + reason);
    }
}

//----------------------------------------------------------------------------
