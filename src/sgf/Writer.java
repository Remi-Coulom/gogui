//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package sgf;

//-----------------------------------------------------------------------------

import java.io.*;
import java.text.*;
import java.util.*;
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

    public Writer(File file, Board board, int handicap, String playerBlack,
                  String playerWhite, String gameComment, Score score)
        throws FileNotFoundException
    {        
        FileOutputStream out = new FileOutputStream(file);
        m_out = new PrintStream(out);
        m_board = board;
        m_file = file;
        m_out.println("(");
        printHeader(handicap, playerBlack, playerWhite, gameComment, score);
        printSetup(m_board.getSetupStonesBlack(),
                   m_board.getSetupStonesWhite());
        if (m_board.getNumberSavedMoves() == 0)
            printToPlay();
        printMoves();        
        m_out.println(")");
        m_out.close();
    }

    public Writer(File file, Board board) throws FileNotFoundException
    {        
        FileOutputStream out = new FileOutputStream(file);
        m_out = new PrintStream(out);
        m_board = board;
        m_file = file;
        m_out.println("(");
        printHeader();
        printPosition();
        m_out.println(")");
        m_out.close();
    }

    private File m_file;

    private PrintStream m_out;

    private Board m_board;

    private void printHeader()
    {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String date =
            Integer.toString(year) + "-" +
            (month < 10 ? "0" : "") + month + "-" +
            (day < 10 ? "0" : "") + day;
        m_out.println(";\nFF[4]\nGM[1]\nAP[GoGui]\n" +
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

    private void printHeader(int handicap, String playerBlack,
                             String playerWhite, String gameComment,
                             go.Score score)
    {
        printHeader();
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
            m_out.println("GC[" + gameComment + "]");
    }

    private void printMoves()
    {
        int n = m_board.getNumberSavedMoves();
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
        printToPlay();
    }

    private void printSetup(Vector black, Vector white)
    {
        if (black.size() > 0 || white.size() > 0)
        {
            m_out.println(";");
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

    private void printToPlay()
    {
        if (m_board.getToMove() == Color.BLACK)
            m_out.println("PL[B]");
        else
            m_out.println("PL[W]");
    }
}

//-----------------------------------------------------------------------------
