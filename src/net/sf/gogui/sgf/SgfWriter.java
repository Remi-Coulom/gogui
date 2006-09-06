//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Iterator;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.StringUtil;

//----------------------------------------------------------------------------

/** Write in SGF format. */
public class SgfWriter
{
    /** Write game tree in SGF format.
        @param out Output stream.
        @param gameTree Game tree to write.
        @param application Application name for AP property.
        @param version If not null, version appended to application name in
        AP property.
    */
    public SgfWriter(OutputStream out, GameTree gameTree,
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
        TimeSettings timeSettings = gameInformation.m_timeSettings;
        printHeader(application, version, handicap, date, playerBlack,
                    playerWhite, rankBlack, rankWhite, result, komi, rules,
                    timeSettings);
        printNewLine();
        printNode(gameTree.getRoot(), true);
        print(")");
        m_out.println(m_buffer.toString());
        m_out.close();
    }

    /** Write position in SGF format.
        @param out Output stream.
        @param board Position to write.
        @param application Application name for AP property.
        @param version If not null, version appended to application name in
        AP property.
    */
    public SgfWriter(OutputStream out, ConstBoard board, String application,
                     String version)
    {        
        m_size = board.getSize();
        m_out = new PrintStream(out);
        print("(");
        printHeader(application, version);
        printNewLine();
        printPosition(board);
        print(")");
        m_out.println(m_buffer.toString());
        m_out.close();
    }

    private static final int STRINGBUF_CAPACITY = 128;

    private final StringBuffer m_buffer = new StringBuffer(STRINGBUF_CAPACITY);

    private final int m_size;

    private final PrintStream m_out;

    private String getEscaped(String text)
    {
        return getEscaped(text, false);
    }

    private String getEscaped(String text, boolean escapeColon)
    {
        StringBuffer result = new StringBuffer(2 * text.length());
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            String specialCharacters;
            if (escapeColon)
                specialCharacters = "]:\\";
            else
                specialCharacters = "]\\";
            if (specialCharacters.indexOf(c) >= 0)
            {
                result.append('\\');
                result.append(c);
            }
            else if (c != '\n' && Character.isWhitespace(c))
                result.append(' ');
            else
                result.append(c);
        }
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

    private String getPoint(GoPoint p)
    {
        if (p == null)
            return "";
        int x = 'a' + p.getX();
        int y = 'a' + (m_size - p.getY() - 1);
        return "" + (char)x + (char)y;
    }

    private String getPointValue(GoPoint point)
    {
        return "[" + getPoint(point) + "]";
    }

    private String getPointList(ArrayList v)
    {
        StringBuffer buffer = new StringBuffer(STRINGBUF_CAPACITY);
        for (int i = 0; i < v.size(); ++i)
            buffer.append(getPointValue((GoPoint)v.get(i)));
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
        final int maxCharPerLine = 78;
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

    private void printHeader(String application, String version)
    {
        String appName = application;
        if (version != null && ! version.equals(""))
            appName = appName + ":" + version;
        print(";FF[4]CA[" + getEscaped(StringUtil.getDefaultEncoding())
              + "]AP[" + getEscaped(appName) + "]");
        if (m_size != 19)
            print("SZ[" + m_size + "]");
    }

    private void printHeader(String application, String version, int handicap,
                             String date, String playerBlack,
                             String playerWhite, String rankBlack,
                             String rankWhite, String result, double komi,
                             String rules, TimeSettings timeSettings)
    {
        printHeader(application, version);
        if (handicap > 0)
            print("HA[" + handicap + "]");
        else
            print("KM[" + GameInformation.roundKomi(komi) + "]");
        if (rules != null && ! rules.equals(""))
            print("RU[" + getEscaped(rules) + "]");
        if (timeSettings != null)
        {
            print("TM[" + timeSettings.getPreByoyomi() / 1000 + "]");
            if (timeSettings.getUseByoyomi())
                // I'd really like to use OM/OP properties from FF[3] for
                // overtime, because the content of OT in FF[4] is not
                // standardized. At least SgfReader should be able to parse
                // the output of SgfWriter and maybe a few other ones that
                // are commonly used
                print("OT[" + timeSettings.getByoyomiMoves() + " moves / "
                      + timeSettings.getByoyomi() / 1000 + " sec]");
        }
        if (playerBlack != null && ! playerBlack.equals(""))
            print("PB[" + getEscaped(playerBlack) + "]");
        if (playerWhite != null && ! playerWhite.equals(""))
            print("PW[" + getEscaped(playerWhite) + "]");
        if (rankBlack != null && ! rankBlack.equals(""))
            print("BR[" + getEscaped(rankBlack) + "]");
        if (rankWhite != null && ! rankWhite.equals(""))
            print("WR[" + getEscaped(rankWhite) + "]");
        if (date != null && ! date.equals(""))
            print("DT[" + getEscaped(date) + "]");
        if (result != null && ! result.equals(""))
            print("RE[" + result + "]");
    }

    private void printLabels(Node node)
    {
        Map labels = node.getLabels();
        if (labels == null)
            return;
        StringBuffer buffer = new StringBuffer(STRINGBUF_CAPACITY);
        buffer.append("LB");
        Iterator i = labels.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry entry = (Map.Entry)i.next();
            GoPoint point = (GoPoint)entry.getKey();
            String value = (String)entry.getValue();
            buffer.append('[');
            buffer.append(getPoint(point));
            buffer.append(':');
            buffer.append(getEscaped(value, true));
            buffer.append(']');
        }
        print(buffer.toString());
    }

    private void printMarked(Node node, String property, MarkType type)
    {
        ArrayList marked = node.getMarked(type);
        if (marked != null)
            print(property + getPointList(marked));
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
            String point = getPointValue(move.getPoint());
            if (move.getColor() == GoColor.BLACK)
                print("B" + point);
            else
                print("W" + point);
        }
        if (node.getNumberAddBlack() > 0)
        {
            StringBuffer buffer = new StringBuffer(STRINGBUF_CAPACITY);
            buffer.append("AB");
            for (int i = 0; i < node.getNumberAddBlack(); ++i)
                buffer.append(getPointValue(node.getAddBlack(i)));
            print(buffer.toString());
        }
        if (node.getNumberAddWhite() > 0)
        {
            StringBuffer buffer = new StringBuffer(STRINGBUF_CAPACITY);
            buffer.append("AW");
            for (int i = 0; i < node.getNumberAddWhite(); ++i)
                buffer.append(getPointValue(node.getAddWhite(i)));
            print(buffer.toString());
        }
        if (node.getNumberAddEmpty() > 0)
        {
            StringBuffer buffer = new StringBuffer(STRINGBUF_CAPACITY);
            buffer.append("AE");
            for (int i = 0; i < node.getNumberAddEmpty(); ++i)
                buffer.append(getPointValue(node.getAddEmpty(i)));
            print(buffer.toString());
        }
        String comment = node.getComment();
        if (comment != null && ! comment.trim().equals(""))
        {
            print("C[" + getEscaped(comment) + "]");
        }
        if (! Double.isNaN(node.getTimeLeft(GoColor.BLACK)))
        {
            print("BL[" + node.getTimeLeft(GoColor.BLACK) + "]");
        }
        if (node.getMovesLeft(GoColor.BLACK) >= 0)
        {
            print("OB[" + node.getMovesLeft(GoColor.BLACK) + "]");
        }
        if (! Double.isNaN(node.getTimeLeft(GoColor.WHITE)))
        {
            print("WL[" + node.getTimeLeft(GoColor.WHITE) + "]");
        }
        if (node.getMovesLeft(GoColor.WHITE) >= 0)
        {
            print("OW[" + node.getMovesLeft(GoColor.WHITE) + "]");
        }
        if (node.getPlayer() != GoColor.EMPTY)
            printToPlay(node.getPlayer());
        printMarked(node, "MA", MarkType.MARK);
        printMarked(node, "CR", MarkType.CIRCLE);
        printMarked(node, "SQ", MarkType.SQUARE);
        printMarked(node, "TR", MarkType.TRIANGLE);
        printMarked(node, "SL", MarkType.SELECT);
        printMarked(node, "TB", MarkType.TERRITORY_BLACK);
        printMarked(node, "TW", MarkType.TERRITORY_WHITE);
        printLabels(node);
        if (! Double.isNaN(node.getValue()))
        {
            print("V[" + node.getValue() + "]");
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
                print(label + value);
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

    private void printPosition(ConstBoard board)
    {
        int numberPoints = board.getNumberPoints();
        ArrayList black = new ArrayList(numberPoints);
        ArrayList white = new ArrayList(numberPoints);
        for (int i = 0; i < numberPoints; ++i)
        {
            GoPoint p = board.getPoint(i);
            GoColor c = board.getColor(p);
            if (c == GoColor.BLACK)
                black.add(p);
            else if (c == GoColor.WHITE)
                white.add(p);
        }
        printSetup(black, white);
        printNewLine();
        printToPlay(board.getToMove());
    }

    private void printSetup(ArrayList black, ArrayList white)
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

    private void printToPlay(GoColor color)
    {
        if (color == GoColor.BLACK)
            print("PL[B]");
        else
            print("PL[W]");
    }
}

//----------------------------------------------------------------------------
