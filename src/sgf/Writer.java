//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package sgf;

//-----------------------------------------------------------------------------

import java.io.*;
import java.text.*;
import java.util.*;
import game.*;
import go.*;

//-----------------------------------------------------------------------------

public class Writer
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    /** Save moves up to current position. */
    public Writer(OutputStream out, Board board, File file,
                  String application, String version, int handicap,
                  String playerBlack, String playerWhite, String gameComment,
                  Score score)
    {        
        m_out = new PrintStream(out);
        m_board = board;
        m_out.println("(");
        printHeader(file, application, version, handicap, playerBlack,
                    playerWhite, gameComment, score);
        printSetup(m_board.getSetupStonesBlack(),
                   m_board.getSetupStonesWhite());
        printToPlay(m_board.getToMove());
        printMoves();        
        m_out.println(")");
        m_out.close();
    }

    /** Save game tree. */
    public Writer(OutputStream out, Board board, GameTree gameTree, File file,
                  String application, String version, int handicap,
                  String playerBlack, String playerWhite, String gameComment,
                  Score score)
    {        
        m_out = new PrintStream(out);
        m_board = board;
        m_out.println("(");
        printHeader(file, application, version, handicap, playerBlack,
                    playerWhite, gameComment, score);
        printToPlay(board.getToMove());
        printNodes(gameTree.getRoot());        
        m_out.println(")");
        m_out.close();
    }

    /** Save board position. */
    public Writer(OutputStream out, Board board, File file,
                  String application, String version)
    {        
        m_out = new PrintStream(out);
        m_board = board;
        m_out.println("(");
        printHeader(file, application, version);
        printPosition();
        m_out.println(")");
        m_out.close();
    }

    private PrintStream m_out;

    private Board m_board;

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
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        DecimalFormat format = new DecimalFormat("00");
        String date = Integer.toString(year) + "-" + format.format(month) +
            "-" + format.format(day);
        String appName = application;
        if (version != null && ! version.equals(""))
            appName = appName + ":" + version;
        m_out.println(";\n" +
                      "FF[4]\n" +
                      "GM[1]\n" +
                      "GN[" + getName(file) + "]\n" +
                      "AP[" + appName + "]\n" +
                      "SZ[" + m_board.getSize() + "]");
        int rules = m_board.getRules();
        if (rules == go.Board.RULES_JAPANESE)
            m_out.println("RU[Japanese]");
        else
        {
            assert(rules == go.Board.RULES_CHINESE);
            m_out.println("RU[GOE]");
        }
        m_out.println("DT[" + date + "]");
    }

    private void printHeader(File file, String application, String version,
                             int handicap, String playerBlack,
                             String playerWhite, String gameComment,
                             Score score)
    {
        printHeader(file, application, version);
        if (handicap > 0)
            m_out.println("HA[" + handicap + "]");
        else
            m_out.println("KM[" + m_board.getKomi() + "]");
        if (score != null)
        {
            m_out.print("RE[");
            float result = score.m_result;
            if (result > 0)
                m_out.print("B+" + result);
            else if (result < 0)
                m_out.print("W+" + (-result));
            else
                m_out.print("0");
            m_out.println("]");
        }
        if (playerBlack != null)
            m_out.println("PB[" + playerBlack + "]");
        if (playerWhite != null)
            m_out.println("PW[" + playerWhite + "]");
        if (gameComment != null)
        {
            DateFormat format =
                DateFormat.getDateTimeInstance(DateFormat.FULL,
                                               DateFormat.FULL);
            Date date = Calendar.getInstance().getTime();
            m_out.println("GC[" + gameComment + "\nDate: " +
                          format.format(date) + "]");
        }
    }

    private void printMoves()
    {
        int n = m_board.getMoveNumber();
        for (int i = 0; i < n; ++i)
        {
            Move m = m_board.getMove(i);
            if (m.getColor() == Color.BLACK)
                m_out.print(";\nB");
            else
                m_out.print(";\nW");
            printPoint(m.getPoint());
            m_out.println();
        }
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
        if (! Float.isNaN(node.getTimeLeftBlack()))
        {
            m_out.println("BL[" + node.getTimeLeftBlack() + "]");
        }
        if (node.getMovesLeftBlack() >= 0)
        {
            m_out.println("OB[" + node.getMovesLeftBlack() + "]");
        }
        if (! Float.isNaN(node.getTimeLeftWhite()))
        {
            m_out.println("WL[" + node.getTimeLeftWhite() + "]");
        }
        if (node.getMovesLeftWhite() >= 0)
        {
            m_out.println("OW[" + node.getMovesLeftWhite() + "]");
        }
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
            if (m_board.getSize() <= 19)
                m_out.print("[tt]");
            else
                m_out.print("[]");
            return;
        }
        int x = 'a' + p.getX();
        int y = 'a' + (m_board.getSize() - p.getY() - 1);
        m_out.print("[" + (char)x + (char)y + "]");
    }

    private void printPointList(Vector v)
    {
        for (int i = 0; i < v.size(); ++i)
            printPoint((Point)v.get(i));
    }
    
    private void printPosition()
    {
        int numberPoints = m_board.getNumberPoints();
        Vector black = new Vector(numberPoints);
        Vector white = new Vector(numberPoints);
        for (int i = 0; i < numberPoints; ++i)
        {
            Point p = m_board.getPoint(i);
            Color c = m_board.getColor(p);
            if (c == Color.BLACK)
                black.add(p);
            else if (c == Color.WHITE)
                white.add(p);
        }
        printSetup(black, white);
        printToPlay(m_board.getToMove());
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

//-----------------------------------------------------------------------------
