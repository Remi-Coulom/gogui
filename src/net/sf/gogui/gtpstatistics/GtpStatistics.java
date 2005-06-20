//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Vector;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtils;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.sgf.SgfReader;
import net.sf.gogui.utils.ErrorMessage;
import net.sf.gogui.utils.Table;

//----------------------------------------------------------------------------

public class GtpStatistics
{
    public GtpStatistics(String program, Vector sgfFiles, boolean verbose)
        throws Exception
    {
        m_result = false;
        Vector columnHeaders = new Vector();
        columnHeaders.add("File");
        columnHeaders.add("Move");
        if (m_runRegGenMove)
            columnHeaders.add("reg_genmove");
        m_table = new Table(columnHeaders);
        m_gtp = new Gtp(program, verbose, null);
        for (int i = 0; i < sgfFiles.size(); ++i)
            handleFile((String)sgfFiles.get(i));
        FileWriter writer = new FileWriter("gtpstatistics.dat");
        m_table.save(writer);
        writer.close();
    }

    public boolean getResult()
    {
        return m_result;
    }

    private boolean m_result;

    private boolean m_runRegGenMove = true;

    private Gtp m_gtp;

    private Table m_table;

    private void handleFile(String name)
        throws ErrorMessage, FileNotFoundException, GtpError,
               SgfReader.SgfError
    {
        InputStream in = new FileInputStream(new File(name));
        SgfReader reader = new SgfReader(in, name, null, 0);
        GameTree tree = reader.getGameTree();
        GameInformation info = tree.getGameInformation();
        int size = info.m_boardSize;
        m_gtp.sendCommandBoardsize(size);
        m_gtp.sendCommandClearBoard(size);
        Node root = tree.getRoot();
        for (Node node = root; node != null; node = node.getChild())
        {
            if (node.getNumberAddWhite() + node.getNumberAddBlack() > 0)
                throw new ErrorMessage("File " + name
                                       + " contains setup stones");
            Move move = node.getMove();
            if (node == root && move == null)
                continue;
            m_table.startRow();
            m_table.set("File", name);
            m_table.set("Move", NodeUtils.getMoveNumber(node));
            handlePosition(move);
            if (move != null)
                m_gtp.sendCommandPlay(move);
        }
    }

    private void handlePosition(Move move) throws GtpError
    {
        if (m_runRegGenMove && move != null)
        {
            boolean result = runRegGenMove(move);
            m_table.set("reg_genmove", result ? "1" : "0");
        }
    }

    private boolean runRegGenMove(Move move) throws GtpError
    {
        String response = m_gtp.sendCommand("reg_genmove " + move.getColor());
        response = response.trim().toUpperCase();
        return (response.equals(GoPoint.toString(move.getPoint())));
    }
}
    
//----------------------------------------------------------------------------
