//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package sgf;

//----------------------------------------------------------------------------

import java.io.*;
import java.util.*;
import game.*;
import go.*;

//----------------------------------------------------------------------------

public class Writer
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    /** Save game tree. */
    public Writer(OutputStream out, GameTree gameTree, File file,
                  String application, String version)
    {        
        m_out = new PrintStream(out);
        m_out.println("(");
        GameInformation gameInformation = gameTree.getGameInformation();
        m_size = gameInformation.m_boardSize;
        String result = gameInformation.m_result;
        String playerBlack = gameInformation.m_playerBlack;
        String playerWhite = gameInformation.m_playerWhite;
        String rankBlack = gameInformation.m_blackRank;
        String rankWhite = gameInformation.m_whiteRank;
        String date = gameInformation.m_date;
        String rules = gameInformation.m_rules;
        int handicap = gameInformation.m_handicap;
        double komi = gameInformation.m_komi;
        printHeader(file, application, version, handicap, date, playerBlack,
                    playerWhite, rankBlack, rankWhite, result, komi, rules);
        printNodes(gameTree.getRoot());        
        m_out.println(")");
        m_out.close();
    }

    /** Save board position. */
    public Writer(OutputStream out, Board board, File file,
                  String application, String version)
    {        
        m_size = board.getSize();
        m_out = new PrintStream(out);
        m_out.println("(");
        printHeader(file, application, version);
        printPosition(board);
        m_out.println(")");
        m_out.close();
    }

    private int m_size;

    private PrintStream m_out;

    private static String getDefaultEncoding()
    {
        OutputStreamWriter out =
            new OutputStreamWriter(new ByteArrayOutputStream());
        return out.getEncoding();
    }

    private static String getName(File file)
    {
        String result = file.getName();
        int len = result.length();
        if (len >= 4
            && result.substring(len - 4).toLowerCase().equals(".sgf"))
            result = result.substring(0, len - 4);
        return result;
    }

    private void printEscaped(String text)
    {
        m_out.print('[');
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if ("]:\\".indexOf(c) >= 0)
            {
                m_out.print('\\');
                m_out.print(c);
            }
            else if (c != '\n' && Character.isWhitespace(c))
                m_out.print(' ');
            else
                m_out.print(c);
        }
        m_out.print(']');
    }

    private void printHeader(File file, String application, String version)
    {
        String appName = application;
        if (version != null && ! version.equals(""))
            appName = appName + ":" + version;
        m_out.print(";\n" +
                    "FF[4]\n" +
                    "CA[" + getDefaultEncoding() + "]\n" +
                    "GM[1]\n" +
                    "GN[" + getName(file) + "]\n" +
                    "AP[" + appName + "]\n" +
                    "SZ[" + m_size + "]\n");
    }

    private void printHeader(File file, String application, String version,
                             int handicap, String date, String playerBlack,
                             String playerWhite, String rankBlack,
                             String rankWhite, String result, double komi,
                             String rules)
    {
        printHeader(file, application, version);
        if (handicap > 0)
            m_out.println("HA[" + handicap + "]");
        else
            m_out.println("KM[" + komi + "]");
        if (rules != null && ! rules.equals(""))
            m_out.println("RU[" + rules + "]");
        if (result != null && ! result.equals(""))
            m_out.println("RE[" + result + "]");
        if (playerBlack != null)
            m_out.println("PB[" + playerBlack + "]");
        if (playerWhite != null)
            m_out.println("PW[" + playerWhite + "]");
        if (rankBlack != null)
            m_out.println("BR[" + rankBlack + "]");
        if (rankWhite != null)
            m_out.println("WR[" + rankWhite + "]");
        if (date != null)
            m_out.println("DT[" + date + "]");
    }

    private void printNodes(Node node)
    {
        Move move = node.getMove();
        if (move != null)
        {
            if (move.getColor() == Color.BLACK)
                m_out.print(";\nB");
            else
                m_out.print(";\nW");
            printPoint(move.getPoint());
            m_out.println();
        }
        if (node.getNumberAddBlack() > 0)
        {
            m_out.print("AB");
            for (int i = 0; i < node.getNumberAddBlack(); ++i)
                printPoint(node.getAddBlack(i));
            m_out.println();
        }
        if (node.getNumberAddWhite() > 0)
        {
            m_out.print("AW");
            for (int i = 0; i < node.getNumberAddWhite(); ++i)
                printPoint(node.getAddWhite(i));
            m_out.println();
        }
        String comment = node.getComment();
        if (comment != null && ! comment.trim().equals(""))
        {
            m_out.print("C");
            printEscaped(comment);
            m_out.println();
        }
        if (! Double.isNaN(node.getTimeLeftBlack()))
        {
            m_out.println("BL[" + node.getTimeLeftBlack() + "]");
        }
        if (node.getMovesLeftBlack() >= 0)
        {
            m_out.println("OB[" + node.getMovesLeftBlack() + "]");
        }
        if (! Double.isNaN(node.getTimeLeftWhite()))
        {
            m_out.println("WL[" + node.getTimeLeftWhite() + "]");
        }
        if (node.getMovesLeftWhite() >= 0)
        {
            m_out.println("OW[" + node.getMovesLeftWhite() + "]");
        }
        if (node.getPlayer() != Color.EMPTY)
            printToPlay(node.getPlayer());
        Map sgfProperties = node.getSgfProperties();
        if (sgfProperties != null)
        {
            Iterator it = sgfProperties.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry entry = (Map.Entry)it.next();
                String label = (String)entry.getKey();
                String value = (String)entry.getValue();
                m_out.print(label);
                printEscaped(value);
                m_out.println();
            }
        }
        int numberChildren = node.getNumberChildren();
        if (numberChildren == 0)
            return;
        if (numberChildren == 1)
        {
            printNodes(node.getChild());
            return;
        }
        for (int i = 0; i < numberChildren; ++i)
        {
            m_out.println("(");
            printNodes(node.getChild(i));
            m_out.println(")");
        }
    }

    private void printPoint(Point p)
    {
        if (p == null)
        {
            if (m_size <= 19)
                m_out.print("[tt]");
            else
                m_out.print("[]");
            return;
        }
        int x = 'a' + p.getX();
        int y = 'a' + (m_size - p.getY() - 1);
        m_out.print("[" + (char)x + (char)y + "]");
    }

    private void printPointList(Vector v)
    {
        for (int i = 0; i < v.size(); ++i)
            printPoint((Point)v.get(i));
    }
    
    private void printPosition(Board board)
    {
        int numberPoints = board.getNumberPoints();
        Vector black = new Vector(numberPoints);
        Vector white = new Vector(numberPoints);
        for (int i = 0; i < numberPoints; ++i)
        {
            Point p = board.getPoint(i);
            Color c = board.getColor(p);
            if (c == Color.BLACK)
                black.add(p);
            else if (c == Color.WHITE)
                white.add(p);
        }
        printSetup(black, white);
        printToPlay(board.getToMove());
    }

    private void printSetup(Vector black, Vector white)
    {
        if (black.size() > 0 || white.size() > 0)
        {
            if (black.size() > 0)
            {
                m_out.print("AB");
                printPointList(black);
                m_out.println();
            }
            if (white.size() > 0)
            {
                m_out.print("AW");
                printPointList(white);
                m_out.println();
            }
        }
    }

    private void printToPlay(Color color)
    {
        if (color == Color.BLACK)
            m_out.println("PL[B]");
        else
            m_out.println("PL[W]");
    }
}

//----------------------------------------------------------------------------
