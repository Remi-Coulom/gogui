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
        print("(");
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
        printNewLine();
        printNode(gameTree.getRoot(), true);
        print(")");
        m_out.println(m_buffer.toString());
        m_out.close();
    }

    /** Save board position. */
    public Writer(OutputStream out, Board board, File file,
                  String application, String version)
    {        
        m_size = board.getSize();
        m_out = new PrintStream(out);
        print("(");
        printHeader(file, application, version);
        printNewLine();
        printPosition(board);
        print(")");
        m_out.println(m_buffer.toString());
        m_out.close();
    }

    private StringBuffer m_buffer = new StringBuffer(128);

    private int m_size;

    private PrintStream m_out;

    private static String getDefaultEncoding()
    {
        OutputStreamWriter out =
            new OutputStreamWriter(new ByteArrayOutputStream());
        return out.getEncoding();
    }

    private String getEscaped(String text)
    {
        StringBuffer result = new StringBuffer(2 * text.length());
        result.append('[');
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if ("]:\\".indexOf(c) >= 0)
            {
                result.append('\\');
                result.append(c);
            }
            else if (c != '\n' && Character.isWhitespace(c))
                result.append(' ');
            else
                result.append(c);
        }
        result.append(']');
        return result.toString();
    }

    private static int getMoveNumberInVariation(Node node)
    {
        int moveNumber = 0;
        while (node != null)
        {
            if (node.getMove() != null)
                ++moveNumber;
            node = node.getFather();
            if (node != null && node.getNumberChildren() > 1)
                break;
        }
        return moveNumber;
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

    private String getPoint(Point p)
    {
        if (p == null)
        {
            if (m_size <= 19)
                return "[tt]";
            else
                return "[]";
        }
        int x = 'a' + p.getX();
        int y = 'a' + (m_size - p.getY() - 1);
        return "[" + (char)x + (char)y + "]";
    }

    private String getPointList(Vector v)
    {
        StringBuffer buffer = new StringBuffer(128);
        for (int i = 0; i < v.size(); ++i)
            buffer.append(getPoint((Point)v.get(i)));
        return buffer.toString();
    }
    
    private void print(String text)
    {
        if (text.indexOf('\n') > 0)
        {
            printNewLine();
            m_buffer.append(text);
            printNewLine();
            return;
        }
        int maxCharPerLine = 78;
        if (m_buffer.length() + text.length() > maxCharPerLine)
            printNewLine();
        m_buffer.append(text);
    }

    private void printNewLine()
    {
        if (m_buffer.length() > 0)
        {
            m_out.println(m_buffer.toString());
            m_buffer.replace(0, m_buffer.length(), "");
        }
    }

    private void printHeader(File file, String application, String version)
    {
        String appName = application;
        if (version != null && ! version.equals(""))
            appName = appName + ":" + version;
        print(";FF[4]CA[" + getDefaultEncoding() + "]GM[1]GN["
              + getName(file) + "]AP[" + appName + "]SZ[" + m_size + "]");
    }

    private void printHeader(File file, String application, String version,
                             int handicap, String date, String playerBlack,
                             String playerWhite, String rankBlack,
                             String rankWhite, String result, double komi,
                             String rules)
    {
        printHeader(file, application, version);
        if (handicap > 0)
            print("HA[" + handicap + "]");
        else
            print("KM[" + komi + "]");
        if (rules != null && ! rules.equals(""))
            print("RU[" + rules + "]");
        if (result != null && ! result.equals(""))
            print("RE[" + result + "]");
        if (playerBlack != null)
            print("PB[" + playerBlack + "]");
        if (playerWhite != null)
            print("PW[" + playerWhite + "]");
        if (rankBlack != null)
            print("BR[" + rankBlack + "]");
        if (rankWhite != null)
            print("WR[" + rankWhite + "]");
        if (date != null)
            print("DT[" + date + "]");
    }

    private void printNode(Node node, boolean isRoot)
    {
        Move move = node.getMove();
        if (! isRoot)
        {
            if (move != null)
            {
                int moveNumber = getMoveNumberInVariation(node);
                if (moveNumber != 1 && moveNumber % 10 == 1)
                    printNewLine();
            }
            print(";");
        }
        if (move != null)
        {
            String point = getPoint(move.getPoint());
            if (move.getColor() == Color.BLACK)
                print("B" + point);
            else
                print("W" + point);
        }
        if (node.getNumberAddBlack() > 0)
        {
            StringBuffer buffer = new StringBuffer(128);
            buffer.append("AB");
            for (int i = 0; i < node.getNumberAddBlack(); ++i)
                buffer.append(getPoint(node.getAddBlack(i)));
            print(buffer.toString());
        }
        if (node.getNumberAddWhite() > 0)
        {
            StringBuffer buffer = new StringBuffer(128);
            buffer.append("AW");
            for (int i = 0; i < node.getNumberAddWhite(); ++i)
                buffer.append(getPoint(node.getAddWhite(i)));
            print(buffer.toString());
        }
        String comment = node.getComment();
        if (comment != null && ! comment.trim().equals(""))
        {
            print("C" + getEscaped(comment));
        }
        if (! Double.isNaN(node.getTimeLeftBlack()))
        {
            print("BL[" + node.getTimeLeftBlack() + "]");
        }
        if (node.getMovesLeftBlack() >= 0)
        {
            print("OB[" + node.getMovesLeftBlack() + "]");
        }
        if (! Double.isNaN(node.getTimeLeftWhite()))
        {
            print("WL[" + node.getTimeLeftWhite() + "]");
        }
        if (node.getMovesLeftWhite() >= 0)
        {
            print("OW[" + node.getMovesLeftWhite() + "]");
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
                print(label + getEscaped(value));
            }
        }
        int numberChildren = node.getNumberChildren();
        if (numberChildren == 0)
            return;
        if (numberChildren == 1)
        {
            printNode(node.getChild(), false);
            return;
        }
        for (int i = 0; i < numberChildren; ++i)
        {
            printNewLine();
            print("(");
            printNode(node.getChild(i), false);
            print(")");
        }
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
        printNewLine();
        printToPlay(board.getToMove());
    }

    private void printSetup(Vector black, Vector white)
    {
        if (black.size() > 0 || white.size() > 0)
        {
            if (black.size() > 0)
                print("AB" + getPointList(black));
            printNewLine();
            if (white.size() > 0)
                print("AW" + getPointList(white));
        }
    }

    private void printToPlay(Color color)
    {
        if (color == Color.BLACK)
            print("PL[B]");
        else
            print("PL[W]");
    }
}

//----------------------------------------------------------------------------
