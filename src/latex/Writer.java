//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package latex;

//----------------------------------------------------------------------------

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;
import game.GameInformation;
import game.GameTree;
import game.Node;
import game.NodeUtils;
import go.Board;
import go.Color;
import go.Move;
import go.Point;

//----------------------------------------------------------------------------

/** Write a game or board position in PSGO style to a stream. */
public class Writer
{
    public Writer(String title, OutputStream out, Board board,
                  boolean usePass, String[][] strings,
                  boolean[][] markups, boolean[][] selects)
    {        
        m_out = new PrintStream(out);
        m_usePass = usePass;
        printBeginDocument();
        if (title != null && ! title.trim().equals(""))
            m_out.println("\\section*{" + escape(title) + "}");
        printBeginPSGo(board.getSize());
        printPosition(board, strings, markups, selects);
        printEndPSGo();
        m_out.println("\\\\");
        String toMove =
            (board.getToMove() == Color.BLACK ? "Black" : "White");
        m_out.println(toMove + " to play.");
        printEndDocument();
        m_out.close();
    }

    public Writer(String title, OutputStream out, GameTree gameTree,
                  boolean usePass)
    {        
        m_out = new PrintStream(out);
        m_usePass = usePass;
        printBeginDocument();
        if (title != null && ! title.trim().equals(""))
            m_out.println("\\section*{" + escape(title) + "}");
        printBeginPSGo(gameTree.getGameInformation().m_boardSize);
        String comment = printTree(gameTree);
        printEndPSGo();
        if (! comment.equals(""))
        {
            m_out.println("\\\\");
            m_out.println(comment);
        }
        printEndDocument();
        m_out.close();
    }

    private boolean m_usePass;

    private PrintStream m_out;

    /** Escape LaTeX special character in text. */
    private String escape(String text)
    {
        text = text.replaceAll("\\#", "\\\\#");
        text = text.replaceAll("\\$", "\\\\\\$");
        text = text.replaceAll("%", "\\\\%");
        text = text.replaceAll("\\&", "\\\\&");
        text = text.replaceAll("~", "\\\\~{}");
        text = text.replaceAll("_", "\\\\_");
        text = text.replaceAll("\\^", "\\\\^{}");
        text = text.replaceAll("\\\\", "\\$\\\\backslash\\$");
        text = text.replaceAll("\\{", "\\\\{");
        text = text.replaceAll("\\}", "\\\\}");
        return text;
    }

    private String getMarkers(String string, boolean markup, boolean select)
    {
        if ((string == null || string.equals("")) && ! markup && ! select)
            return null;
        StringBuffer result = new StringBuffer();
        if (string != null && ! string.equals(""))
            result.append("\\marklb{" + string + "}");
        if (markup)
            result.append("\\marksq");
        if (select)
            result.append("\\markdd");
        return result.toString();
    }

    private String getStoneInTextString(int moveNumber, Color color)
    {
        return ("\\stone[" + moveNumber + "]{"
                + (color == Color.BLACK ? "black" : "white") + "}");
    }

    private void printBeginDocument()
    {
        String requiredVersion = "0.12";
        if (m_usePass)
            requiredVersion = "0.14";
        m_out.println("\\documentclass{article}");
        m_out.println("\\usepackage{psgo} % version " + requiredVersion
                      + " or newer");
        m_out.println("\\pagestyle{empty}");
        m_out.println("\\begin{document}");
        m_out.println();
    }

    private void printBeginPSGo(int size)
    {
        m_out.println("\\begin{psgoboard}[" + size + "]");
    }

    private void printColor(Color color)
    {
        if (color == Color.BLACK)
            m_out.print("{black}");
        else
        {
            assert(color == Color.WHITE);
            m_out.print("{white}");
        }
    }

    private void printCoordinates(Point point)
    {
        assert(point != null);
        String s = point.toString();
        m_out.print("{" + s.substring(0, 1).toLowerCase() + "}{" +
                    s.substring(1) + "}");
    }

    private void printEndDocument()
    {
        m_out.println();
        m_out.println("\\end{document}");
    }

    private void printEndPSGo()
    {
        m_out.println("\\end{psgoboard}");
    }

    private String printTree(GameTree gameTree)
    {
        GameInformation gameInformation = gameTree.getGameInformation();
        StringBuffer comment = new StringBuffer();
        int size = gameInformation.m_boardSize;
        Node firstMoveAtPoint[][] = new Node[size][size];
        Vector needsComment = new Vector();
        boolean blackToMove = true;
        m_out.println("\\setcounter{gomove}{0}");
        Node node = gameTree.getRoot();
        while (node != null)
        {
            for (int i = 0; i < node.getNumberAddBlack(); ++i)
                printStone(Color.BLACK, node.getAddBlack(i), null, false,
                           false);
            for (int i = 0; i < node.getNumberAddWhite(); ++i)
                printStone(Color.WHITE, node.getAddWhite(i), null, false,
                           false);
            Move move = node.getMove();
            if (move == null)
            {
                node = node.getChild();
                continue;
            }
            Point point = move.getPoint();
            Color color = move.getColor();
            int moveNumber = NodeUtils.getMoveNumber(node);
            boolean isColorUnexpected =
                (blackToMove && color != Color.BLACK)
                || (! blackToMove && color != Color.WHITE);
            boolean isPass = (point == null);
            if (isPass
                || firstMoveAtPoint[point.getX()][point.getY()] != null)
            {
                needsComment.add(node);
                if (m_usePass)
                    m_out.print("\\pass");
                else
                    m_out.print("\\refstepcounter{gomove} \\toggleblackmove");
                if (isPass)
                {
                    if (! m_usePass)
                        m_out.print(" % \\pass");
                }
                else
                {
                    m_out.print(" % \\move");
                    printCoordinates(point);
                }
                m_out.println(" % " + (blackToMove ? "B " : "W ")
                              + moveNumber);
            }
            else
            {
                if (isColorUnexpected)
                {
                    m_out.println("\\toggleblackmove");
                    blackToMove = ! blackToMove;
                }
                m_out.print("\\move");
                printCoordinates(point);
                m_out.println(" % " + (blackToMove ? "B " : "W ")
                              + moveNumber);
                firstMoveAtPoint[point.getX()][point.getY()] = node;
            }
            blackToMove = ! blackToMove;
            node = node.getChild();
        }
        for (int i = 0; i < needsComment.size(); ++i)
        {
            node = (Node)needsComment.get(i);
            Move move = node.getMove();
            Point point = move.getPoint();
            Color color = move.getColor();
            if (comment.length() > 0)
                comment.append(" \\enspace\n");
            comment.append(getStoneInTextString(i + 1, color));
            if (point != null)
            {
                int x = point.getX();
                int y = point.getY();
                comment.append("~at~");
                Node first = firstMoveAtPoint[x][y];
                Color firstMoveColor = first.getMove().getColor();
                int firstMoveNumber = NodeUtils.getMoveNumber(first);
                comment.append(getStoneInTextString(firstMoveNumber,
                                                    firstMoveColor));
            }
            else
                comment.append("~pass");
        }
        return comment.toString();
    }
    
    private void printPosition(Board board, String[][] strings,
                               boolean[][] markups, boolean[][] selects)
    {
        int numberPoints = board.getNumberPoints();
        for (int i = 0; i < numberPoints; ++i)
        {
            Point point = board.getPoint(i);
            Color color = board.getColor(point);
            int x = point.getX();
            int y = point.getY();
            String string = null;
            if (strings != null)
                string = strings[x][y];
            boolean markup = (markups != null && markups[x][y]);
            boolean select = (selects != null && selects[x][y]);
            if (color != Color.EMPTY)
                printStone(color, point, string, markup, select);
            else
            {
                String markers = getMarkers(string, markup, select);
                if (markers != null)
                {
                    m_out.print("\\markpos{" + markers + "}");
                    printCoordinates(point);
                    m_out.print("\n");
                }
            }
        }
    }

    private void printStone(Color color, Point point, String string,
                            boolean markup, boolean select)
    {
        m_out.print("\\stone");
        String markers = getMarkers(string, markup, select);
        if (markers != null)
        m_out.print("[" + markers + "]");
        printColor(color);
        printCoordinates(point);
        m_out.print("\n");
    }
}

//----------------------------------------------------------------------------
