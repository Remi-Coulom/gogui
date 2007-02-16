//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.tex;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;

/** Write a game or board position in PSGO style to a stream. */
public class TexWriter
{
    public TexWriter(String title, OutputStream out, ConstBoard board,
                     boolean usePass, String[][] markLabel, boolean[][] mark,
                     boolean[][] markTriangle, boolean[][] markCircle,
                     boolean[][] markSquare, boolean[][] markSelect)
    {        
        m_out = new PrintStream(out);
        m_usePass = usePass;
        printBeginDocument();
        if (title != null && ! title.trim().equals(""))
            m_out.println("\\section*{" + escape(title) + "}");
        printBeginPSGo(board.getSize());
        printPosition(board, markLabel, mark, markTriangle, markCircle,
                      markSquare, markSelect);
        printEndPSGo();
        m_out.println("\\\\");
        String toMove =
            (board.getToMove() == GoColor.BLACK ? "Black" : "White");
        m_out.println(toMove + " to play");
        printEndDocument();
        m_out.close();
    }

    public TexWriter(String title, OutputStream out, ConstGameTree tree,
                     boolean usePass)
    {        
        m_out = new PrintStream(out);
        m_usePass = usePass;
        printBeginDocument();
        if (title != null && ! title.trim().equals(""))
            m_out.println("\\section*{" + escape(title) + "}");
        printBeginPSGo(tree.getBoardSize());
        String comment = printTree(tree);
        printEndPSGo();
        if (! comment.equals(""))
        {
            m_out.println("\\\\");
            m_out.println(comment);
        }
        printEndDocument();
        m_out.close();
    }

    private final boolean m_usePass;

    private final PrintStream m_out;

    /** Escape LaTeX special characters in text. */
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

    private String getStoneInTextString(int moveNumber, GoColor color)
    {
        return ("\\stone[" + moveNumber + "]{"
                + (color == GoColor.BLACK ? "black" : "white") + "}");
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

    private void printColor(GoColor color)
    {
        if (color == GoColor.BLACK)
            m_out.print("{black}");
        else
        {
            assert(color == GoColor.WHITE);
            m_out.print("{white}");
        }
    }

    private void printCoordinates(GoPoint point)
    {
        assert(point != null);
        String s = point.toString();
        m_out.print("{" + s.substring(0, 1).toLowerCase(Locale.ENGLISH) + "}{"
                    + s.substring(1) + "}");
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

    private String printTree(ConstGameTree tree)
    {
        StringBuffer comment = new StringBuffer();
        int size = tree.getBoardSize();
        ConstNode firstMoveAtPoint[][] = new ConstNode[size][size];
        ArrayList needsComment = new ArrayList();
        boolean blackToMove = true;
        m_out.println("\\setcounter{gomove}{0}");
        ConstNode node = tree.getRootConst();
        while (node != null)
        {
            for (GoColor c = GoColor.BLACK; c != null;
                 c = c.getNextBlackWhite())
            {
                ConstPointList stones = node.getAddStones(c);
                for (int i = 0; i < stones.size(); ++i)
                    printStone(c, stones.get(i), null);
            }
            Move move = node.getMove();
            if (move == null)
            {
                node = node.getChildConst();
                continue;
            }
            GoPoint point = move.getPoint();
            GoColor color = move.getColor();
            int moveNumber = NodeUtil.getMoveNumber(node);
            boolean isColorUnexpected =
                (blackToMove && color != GoColor.BLACK)
                || (! blackToMove && color != GoColor.WHITE);
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
            node = node.getChildConst();
        }
        for (int i = 0; i < needsComment.size(); ++i)
        {
            node = (Node)needsComment.get(i);
            Move move = node.getMove();
            GoPoint point = move.getPoint();
            GoColor color = move.getColor();
            if (comment.length() > 0)
                comment.append(" \\enspace\n");
            comment.append(getStoneInTextString(i + 1, color));
            if (point == null)
                comment.append("~pass");
            else
            {
                int x = point.getX();
                int y = point.getY();
                comment.append("~at~");
                ConstNode first = firstMoveAtPoint[x][y];
                GoColor firstMoveColor = first.getMove().getColor();
                int firstMoveNumber = NodeUtil.getMoveNumber(first);
                comment.append(getStoneInTextString(firstMoveNumber,
                                                    firstMoveColor));
            }
        }
        return comment.toString();
    }
    
    private void printPosition(ConstBoard board, String[][] markLabel,
                               boolean[][] mark, boolean[][] markTriangle,
                               boolean[][] markCircle, boolean[][] markSquare,
                               boolean[][] markSelect)
    {
        int numberPoints = board.getNumberPoints();
        for (int i = 0; i < numberPoints; ++i)
        {
            GoPoint point = board.getPoint(i);
            GoColor color = board.getColor(point);
            int x = point.getX();
            int y = point.getY();
            StringBuffer buffer = new StringBuffer();
            if (mark != null && mark[x][y])
                buffer.append("\\markma");
            if (markTriangle != null && markTriangle[x][y])
                buffer.append("\\marktr");
            if (markCircle != null && markCircle[x][y])
                buffer.append("\\markcr");
            if (markSquare != null && markSquare[x][y])
                buffer.append("\\marksq");
            if (markLabel != null && markLabel[x][y] != null
                     && ! markLabel[x][y].trim().equals(""))
            {
                buffer.append("\\marklb{");
                buffer.append(markLabel[x][y]);
                buffer.append("}");
            }
            if (markSelect != null && markSelect[x][y])
                buffer.append("\\marksl");
            String markup = null;
            if (buffer.length() > 0)
                markup = buffer.toString();
            if (color == GoColor.EMPTY)
            {
                if (markup != null)
                {
                    m_out.print("\\markpos{" + markup + "}");
                    printCoordinates(point);
                    m_out.print("\n");
                }
            }
            else
                printStone(color, point, markup);
        }
    }

    private void printStone(GoColor color, GoPoint point, String markup)
    {
        m_out.print("\\stone");
        if (markup != null)
            m_out.print("[" + markup + "]");
        printColor(color);
        printCoordinates(point);
        m_out.print("\n");
    }
}

