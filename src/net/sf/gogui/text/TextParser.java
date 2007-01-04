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
                    throw new ParseError("Expected start of position");
                if (isBoardRow(line, true))
                    break;
            }
            m_board = new Board(m_size);
            parseBoardRow(line, m_size - 1);
            for (int y = m_size - 2; y >= 0; --y)
            {
                line = readLine();
                // Allow one parse failure per line in case long lines were
                // wrapped
                if (! isBoardRow(line, false))
                    line = readLine();
                parseBoardRow(line, y);
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

    private int m_size;

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

    private boolean isBlack(char c)
    {
        if (m_charBlack != null)
            return (c == m_charBlack.charValue());
        if (c == 'X' || c == '@' || c == '#' || c =='x')
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
            m_size = size;
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
        if (x != m_size)
            throw new ParseError("Could not determine board size");
    }

    private String readLine() throws ParseError
    {
        String line;
        try
        {
            line = m_reader.readLine();
        }
        catch (IOException e)
        {
            line = null;
        }
        if (line == null)
            throw new ParseError("Could not find position");
        return line;
    }
}
