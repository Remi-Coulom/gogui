//----------------------------------------------------------------------------
// $Id: TexWriter.java 3622 2006-12-11 23:13:42Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

/** Parse Go positions from ASCII text. */
public class TextParser
{
    public TextParser()
    {   
    }

    /** Get board with parsed position.
        Only valid after calling parse.
    */
    public Board getBoard()
    {
        return m_board;
    }

    public void parse(Reader reader) throws ParseError
    {
        try
        {
            m_reader = new BufferedReader(reader);
            m_charBlack = null;
            m_charWhite = null;
            String line;
            while (true)
            {
                line = readLine();
                if (line == null)
                    throw new ParseError("could not find position");
                if (isBoardRow(line, true))
                    break;
            }
            int size = m_width;
            m_board = new Board(m_width);
            parseBoardRow(line, size - 1);
            int i = 2;
            while (true)
            {
                line = readLine();
                if (line == null)
                    break;
                if (! isBoardRow(line, false))
                    // Allow one failure if long lines were wrapped
                    line = readLine();
                if (line == null || ! isBoardRow(line, false))
                    break;
                if (size - i < 0)
                {
                    // Handle rectangular shape (height > width)
                    increaseBoardSize();
                    ++size;
                }    
                parseBoardRow(line, size - i);
                ++i;
            }
        }
        finally
        {
            try
            {
                m_reader.close();
            }
            catch (IOException e)
            {
            }
            m_reader = null;
        }
    }

    private int m_width;

    private Character m_charBlack;

    private Character m_charWhite;

    private Board m_board;

    private BufferedReader m_reader;

    private int ignoreNumbers(String line, int startPos)
    {
        int i;
        for (i = startPos; i < line.length(); ++i)
        {
            char c = line.charAt(i);
            if (! Character.isSpaceChar(c) && ! Character.isDigit(c))
                break;
        }
        return i;
    }

    /** Increase boardsize by one.
        Keep existing position (shifted upward by one line)
    */
    private void increaseBoardSize()
    {
        int newSize = m_board.getSize() + 1;
        Board newBoard = new Board(newSize);
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            GoPoint p = m_board.getPoint(i);
            newBoard.setup(p.up(newSize), m_board.getColor(p));
        }
        m_board = newBoard;
    }

    private boolean isBlack(char c)
    {
        if (m_charBlack != null)
            return (c == m_charBlack.charValue());
        if (c == 'X' || c == '@' || c == '#' || c == 'x')
        {
            m_charBlack = new Character(c);
            return true;
        }
        return false;
    }

    private static boolean isEmpty(char c)
    {
        return (c == '.' || c == '+');
    }

    private boolean isWhite(char c)
    {
        if (m_charWhite != null)
            return (c == m_charWhite.charValue());
        if (c == 'o' || c == 'O')
        {
            m_charWhite = new Character(c);
            return true;
        }
        return false;
    }

    private boolean isBoardRow(String line, boolean initSize)
        throws ParseError
    {
        int size = 0;
        for (int i = ignoreNumbers(line, 0); i < line.length(); ++i)
        {
            char c = line.charAt(i);
            if (Character.isSpaceChar(c))
                continue;
            if (isBlack(c) || isWhite(c) || isEmpty(c))
                ++size;
            else
                break;
        }
        // Don't try to parse boards smaller than 3x3
        if (size < 3 || size > GoPoint.MAXSIZE)
            return false;
        if (initSize)
            m_width = size;
        return true;
    }

    private void parseBoardRow(String line, int y) throws ParseError
    {
        int x = 0;
        for (int i = ignoreNumbers(line, 0); i < line.length(); ++i)
        {
            char c = line.charAt(i);
            if (Character.isSpaceChar(c))
                continue;
            if (isBlack(c))
            {
                m_board.setup(GoPoint.get(x, y), GoColor.BLACK);
                ++x;
            }
            else if (isWhite(c))
            {
                m_board.setup(GoPoint.get(x, y), GoColor.WHITE);
                ++x;
            }
            else if (isEmpty(c))
                ++x;
            else
                break;
        }
        if (x != m_width)
            // Rows with different widths
            throw new ParseError("Could not determine board size");
    }

    private String readLine() throws ParseError
    {
        try
        {
            return m_reader.readLine();
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
