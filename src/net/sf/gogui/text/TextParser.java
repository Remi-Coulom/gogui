// TextParser.java

package net.sf.gogui.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;

/** Parse Go positions from ASCII text.
    Can handle a variety of formats. Black stones can be represented by 'X',
    'x', '@' or '#'; white stones by 'O' or 'o' (however one representation
    must be used consistently); '.', ',' and '+' are interpreted as empty
    points. Space characters are allowed between the points; leading numbers
    (or '|', '&gt;' and '$' characters) are ignored, as well as single
    inserted invalid lines (to support appended text after the row that was
    wrapped). Non-rectangular positions will be read into the smallest
    containing square board size at the top left position. If a a line
    contains the string "b|black|w|white to play|move" (case-insensitive), it
    will be used to set the current player in the position. */
public class TextParser
{
    public TextParser()
    {
    }

    /** Get board with parsed position.
        Only valid after calling parse. */
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
            m_board = new Board(m_width);
            checkToPlay(line);
            parseBoardRow(line, m_board.getSize() - 1);
            int i = 2;
            while (true)
            {
                line = readLine();
                if (line == null)
                    break;
                checkToPlay(line);
                if (! isBoardRow(line, false))
                {
                    // Allow one failure if long lines were wrapped
                    line = readLine();
                    if (line != null)
                        checkToPlay(line);
                }
                if (line == null || ! isBoardRow(line, false))
                    break;
                if (m_board.getSize() - i < 0)
                    increaseBoardSize();
                parseBoardRow(line, m_board.getSize() - i);
                ++i;
            }
            if (i != m_board.getSize() + 1)
                // Assume that non-square positions are anchored at lower-left
                // corner
                shiftBoardDown(m_board.getSize() + 1 - i);
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

    private void checkToPlay(String line)
    {
        line = line.toLowerCase();
        if (line.contains("black to play") || line.contains("b to play")
            || line.contains("black to move") || line.contains("b to move"))
            m_board.setToMove(BLACK);
        if (line.contains("white to play") || line.contains("w to play")
            || line.contains("white to move") || line.contains("w to move"))
            m_board.setToMove(WHITE);
    }

    /** Ignore characters at beginning of line.
        Ignores characters that are sometimes left of the actual position.
        The included characters are digits, pipe symbols (edge) and dollar
        signs. */
    private int ignoreBeginning(String line)
    {
        int i;
        for (i = 0; i < line.length(); ++i)
        {
            char c = line.charAt(i);
            if (! Character.isSpaceChar(c) && ! Character.isDigit(c)
                && c != '$' && c != '|' && c != '>')
                break;
        }
        return i;
    }

    /** Increase boardsize by one.
        Keep existing position (shifted upward by one line) */
    private void increaseBoardSize()
    {
        int newSize = m_board.getSize() + 1;
        Board newBoard = new Board(newSize);
        PointList black = new PointList();
        PointList white = new PointList();
        for (GoPoint p : m_board)
        {
            GoColor c = m_board.getColor(p);
            p = p.up(newSize);
            if (c == BLACK)
                black.add(p);
            else if (c == WHITE)
                white.add(p);
        }
        newBoard.setup(black, white, m_board.getToMove());
        m_board = newBoard;
    }

    private boolean isBlack(char c)
    {
        if (m_charBlack != null)
            return (c == m_charBlack.charValue());
        if (c == 'X' || c == '@' || c == '#' || c == 'x')
        {
            m_charBlack = c;
            return true;
        }
        return false;
    }

    private static boolean isEmpty(char c)
    {
        return (c == '.' || c == ',' || c == '+');
    }

    private boolean isWhite(char c)
    {
        if (m_charWhite != null)
            return (c == m_charWhite.charValue());
        if (c == 'o' || c == 'O')
        {
            m_charWhite = c;
            return true;
        }
        return false;
    }

    private boolean isBoardRow(String line, boolean initSize)
        throws ParseError
    {
        int size = 0;
        for (int i = ignoreBeginning(line); i < line.length(); ++i)
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
        if (size < 3 || size > GoPoint.MAX_SIZE)
            return false;
        if (initSize)
            m_width = size;
        return true;
    }

    private void parseBoardRow(String line, int y) throws ParseError
    {
        int x = 0;
        for (int i = ignoreBeginning(line); i < line.length(); ++i)
        {
            char c = line.charAt(i);
            if (Character.isSpaceChar(c))
                continue;
            if (isBlack(c))
            {
                if (x >= m_board.getSize())
                    increaseBoardSize();
                PointList black =
                    new PointList(m_board.getSetup(BLACK));
                PointList white =
                    new PointList(m_board.getSetup(WHITE));
                black.add(GoPoint.get(x, y));
                m_board.setup(black, white, m_board.getToMove());
                ++x;
            }
            else if (isWhite(c))
            {
                if (x >= m_board.getSize())
                    increaseBoardSize();
                PointList black =
                    new PointList(m_board.getSetup(BLACK));
                PointList white =
                    new PointList(m_board.getSetup(WHITE));
                white.add(GoPoint.get(x, y));
                m_board.setup(black, white, m_board.getToMove());
                ++x;
            }
            else if (isEmpty(c))
            {
                if (x >= m_board.getSize())
                    increaseBoardSize();
                ++x;
            }
            else
                break;
        }
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

    private void shiftBoardDown(int deltaY)
    {
        int size = m_board.getSize();
        Board newBoard = new Board(size);
        PointList black = new PointList();
        PointList white = new PointList();
        for (int y = 0; y < size - deltaY; ++y)
            for (int x = 0; x < size; ++x)
            {
                GoColor c = m_board.getColor(GoPoint.get(x, y + deltaY));
                GoPoint p = GoPoint.get(x, y);
                if (c == BLACK)
                    black.add(p);
                else if (c == WHITE)
                    white.add(p);
            }
        newBoard.setup(black, white, m_board.getToMove());
        m_board = newBoard;
    }
}
