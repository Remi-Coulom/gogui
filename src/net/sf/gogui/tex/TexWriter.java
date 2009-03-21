// TexWriter.java

package net.sf.gogui.tex;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.StringUtil;

/** Write a game or board position in PSGO style to a stream. */
public class TexWriter
{
    public TexWriter(String title, OutputStream out, ConstBoard board,
                     String[][] markLabel, boolean[][] mark,
                     boolean[][] markTriangle, boolean[][] markCircle,
                     boolean[][] markSquare, boolean[][] markSelect)
    {
        m_out = new PrintStream(out);
        printBeginDocument();
        if (! StringUtil.isEmpty(title))
            m_out.println("\\section*{" + escape(title) + "}");
        printBeginPSGo(board.getSize());
        printPosition(board, markLabel, mark, markTriangle, markCircle,
                      markSquare, markSelect);
        printEndPSGo();
        m_out.println("\\\\");
        m_out.print(board.getToMove().getCapitalizedName());
        m_out.println(" to play");
        printEndDocument();
        m_out.close();
    }

    public TexWriter(String title, OutputStream out, ConstGameTree tree)
    {
        m_out = new PrintStream(out);
        printBeginDocument();
        if (! StringUtil.isEmpty(title))
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
                + (color == BLACK ? "black" : "white") + "}");
    }

    private void printBeginDocument()
    {
        m_out.println("\\documentclass{article}");
        m_out.println("\\usepackage{psgo} % version 0.14 or newer");
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
        if (color == BLACK)
            m_out.print("{black}");
        else
        {
            assert color == WHITE;
            m_out.print("{white}");
        }
    }

    private void printCoordinates(GoPoint point)
    {
        assert point != null;
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
        StringBuilder comment = new StringBuilder();
        int size = tree.getBoardSize();
        ConstNode firstMoveAtPoint[][] = new ConstNode[size][size];
        ArrayList<ConstNode> needsComment = new ArrayList<ConstNode>();
        boolean blackToMove = true;
        m_out.println("\\setcounter{gomove}{0}");
        ConstNode node = tree.getRootConst();
        while (node != null)
        {
            for (GoColor c : BLACK_WHITE)
                for (GoPoint stone : node.getSetup(c))
                    printStone(c, stone, null);
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
                (blackToMove && color != BLACK)
                || (! blackToMove && color != WHITE);
            boolean isPass = (point == null);
            if (isPass || firstMoveAtPoint[point.getX()][point.getY()] != null)
            {
                needsComment.add(node);
                m_out.print("\\pass");
                if (! isPass)
                {
                    m_out.print(" % \\move");
                    printCoordinates(point);
                }
                m_out.println(" % " + (blackToMove ? "B " : "W ") + moveNumber);
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
            node = needsComment.get(i);
            int moveNumber = NodeUtil.getMoveNumber(node);
            Move move = node.getMove();
            GoPoint point = move.getPoint();
            GoColor color = move.getColor();
            if (comment.length() > 0)
                comment.append(" \\enspace\n");
            comment.append(getStoneInTextString(moveNumber, color));
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
        for (GoPoint p : board)
        {
            GoColor color = board.getColor(p);
            int x = p.getX();
            int y = p.getY();
            StringBuilder buffer = new StringBuilder(128);
            if (mark != null && mark[x][y])
                buffer.append("\\markma");
            if (markTriangle != null && markTriangle[x][y])
                buffer.append("\\marktr");
            if (markCircle != null && markCircle[x][y])
                buffer.append("\\markcr");
            if (markSquare != null && markSquare[x][y])
                buffer.append("\\marksq");
            if (markLabel != null && ! StringUtil.isEmpty(markLabel[x][y]))
            {
                buffer.append("\\marklb{");
                buffer.append(markLabel[x][y]);
                buffer.append('}');
            }
            if (markSelect != null && markSelect[x][y])
                buffer.append("\\marksl");
            String markup = null;
            if (buffer.length() > 0)
                markup = buffer.toString();
            if (color == EMPTY)
            {
                if (markup != null)
                {
                    m_out.print("\\markpos{" + markup + "}");
                    printCoordinates(p);
                    m_out.print("\n");
                }
            }
            else
                printStone(color, p, markup);
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
