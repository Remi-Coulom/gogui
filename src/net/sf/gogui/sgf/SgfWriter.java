// SgfWriter.java

package net.sf.gogui.sgf;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstSgfProperties;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.ConstBoard;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.BLACK_WHITE_EMPTY;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.StringUtil;

/** Write in SGF format. */
public class SgfWriter
{
    public static final String ENCODING = "UTF-8";

    /** Write game tree in SGF format.
        @param out Output stream.
        @param tree Game tree to write.
        @param application Application name for AP property.
        @param version If not null, version appended to application name in
        AP property. */
    public SgfWriter(OutputStream out, ConstGameTree tree, String application,
                     String version)
    {
        try
        {
            m_out = new PrintStream(out, false, ENCODING);
        }
        catch (UnsupportedEncodingException e)
        {
            // UTF-8 should be supported by every Java implementation
            assert false;
        }
        print("(");
        m_size = tree.getBoardSize();
        printHeader(application, version);
        printNewLine();
        printNode(tree.getRootConst(), true);
        print(")");
        m_out.println(m_buffer.toString());
        m_out.close();
    }

    /** Write position in SGF format.
        @param out Output stream.
        @param board Position to write.
        @param application Application name for AP property.
        @param version If not null, version appended to application name in
        AP property. */
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

    private static final int MAX_CHARS_PER_LINE = 78;

    private final StringBuilder m_buffer
        = new StringBuilder(STRINGBUF_CAPACITY);

    private final int m_size;

    private PrintStream m_out;

    private String getEscaped(String text)
    {
        return getEscaped(text, false);
    }

    private String getEscaped(String text, boolean escapeColon)
    {
        StringBuilder result = new StringBuilder(2 * text.length());
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

    private static int getMoveNumberInVariation(ConstNode node)
    {
        int moveNumber = 0;
        while (node != null)
        {
            if (node.getMove() != null)
                ++moveNumber;
            node = node.getFatherConst();
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

    private String getPointList(ConstPointList v)
    {
        StringBuilder buffer = new StringBuilder(STRINGBUF_CAPACITY);
        for (int i = 0; i < v.size(); ++i)
            buffer.append(getPointValue(v.get(i)));
        return buffer.toString();
    }

    private boolean hasByoyomiInformation(ConstNode node)
    {
        ConstGameInfo info = node.getGameInfoConst();
        if (info == null)
            return false;
        TimeSettings settings = info.getTimeSettings();
        return (settings != null && settings.getUseByoyomi());
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
        if (m_buffer.length() + text.length() > MAX_CHARS_PER_LINE)
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
        StringBuilder header = new StringBuilder(128);
        header.append(";FF[4]CA[");
        header.append(getEscaped(ENCODING));
        header.append(']');
        if (application != null && ! application.equals(""))
        {
            String appName = application;
            if (version != null && ! version.equals(""))
                appName = appName + ":" + version;
            header.append("AP[");
            header.append(getEscaped(appName));
            header.append(']');
        }
        if (m_size != 19)
        {
            header.append("SZ[");
            header.append(m_size);
            header.append(']');
        }
        print(header.toString());
    }

    private void printGameInfo(ConstGameInfo info)
    {
        int handicap = info.getHandicap();
        Komi komi = info.getKomi();
        if (handicap > 0)
            print("HA[" + handicap + "]");
        if (komi != null && ! (handicap > 0 && komi.equals(new Komi(0))))
            print("KM[" + komi + "]");
        TimeSettings timeSettings = info.getTimeSettings();
        if (timeSettings != null)
        {
            print("TM[" + timeSettings.getPreByoyomi() / 1000 + "]");
            String overtime = SgfUtil.getOvertime(timeSettings);
            if (overtime != null)
                print("OT[" + overtime + "]");
        }
        printInfo("PB", info.get(StringInfoColor.NAME, BLACK));
        printInfo("PW", info.get(StringInfoColor.NAME, WHITE));
        printInfo("BR", info.get(StringInfoColor.RANK, BLACK));
        printInfo("WR", info.get(StringInfoColor.RANK, WHITE));
        printInfo("BT", info.get(StringInfoColor.TEAM, BLACK));
        printInfo("WT", info.get(StringInfoColor.TEAM, WHITE));
        printInfo("DT", info.get(StringInfo.DATE));
        printInfo("RE", info.get(StringInfo.RESULT));
        printInfo("RU", info.get(StringInfo.RULES));
        printInfo("US", info.get(StringInfo.USER));
        printInfo("CP", info.get(StringInfo.COPYRIGHT));
        printInfo("AN", info.get(StringInfo.ANNOTATION));
        printInfo("RO", info.get(StringInfo.ROUND));
        printInfo("SO", info.get(StringInfo.SOURCE));
        printNewLine();
    }

    private void printInfo(String label, String value)
    {
        if (value == null || value.equals(""))
            return;
        print(label + "[" + getEscaped(value) + "]");
    }

    private void printLabels(ConstNode node)
    {
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        if (labels == null)
            return;
        StringBuilder buffer = new StringBuilder(STRINGBUF_CAPACITY);
        buffer.append("LB");
        for (Map.Entry<GoPoint,String> entry : labels.entrySet())
        {
            GoPoint point = entry.getKey();
            String value = entry.getValue();
            buffer.append('[');
            buffer.append(getPoint(point));
            buffer.append(':');
            buffer.append(getEscaped(value, true));
            buffer.append(']');
        }
        print(buffer.toString());
    }

    private void printMarked(ConstNode node, String property, MarkType type)
    {
        ConstPointList marked = node.getMarkedConst(type);
        if (marked != null)
            print(property + getPointList(marked));
    }

    private void printNode(ConstNode node, boolean isRoot)
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
        ConstGameInfo info = node.getGameInfoConst();
        if (info != null)
            printGameInfo(info);
        if (move != null)
        {
            String point = getPointValue(move.getPoint());
            if (move.getColor() == BLACK)
                print("B" + point);
            else
                print("W" + point);
        }
        for (GoColor c : BLACK_WHITE_EMPTY)
        {
            ConstPointList points = node.getSetup(c);
            if (points.size() == 0)
                continue;
            StringBuilder buffer = new StringBuilder(STRINGBUF_CAPACITY);
            if (c == BLACK)
                buffer.append("AB");
            else if (c == WHITE)
                buffer.append("AW");
            else
                buffer.append("AE");
            for (GoPoint p : points)
                buffer.append(getPointValue(p));
            print(buffer.toString());
        }
        String comment = node.getComment();
        if (! StringUtil.isEmpty(comment))
            print("C[" + getEscaped(comment) + "]");
        if (! Double.isNaN(node.getTimeLeft(BLACK)))
            print("BL[" + node.getTimeLeft(BLACK) + "]");
        if (node.getMovesLeft(BLACK) >= 0)
            print("OB[" + node.getMovesLeft(BLACK) + "]");
        if (! Double.isNaN(node.getTimeLeft(WHITE)))
            print("WL[" + node.getTimeLeft(WHITE) + "]");
        if (node.getMovesLeft(WHITE) >= 0)
            print("OW[" + node.getMovesLeft(WHITE) + "]");
        if (node.getPlayer() != null)
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
            print("V[" + node.getValue() + "]");
        ConstSgfProperties sgfProps = NodeUtil.cleanSgfProps(node);
        if (sgfProps != null)
            for (String key : sgfProps.getKeys())
            {
                if (key.equals("OT") && hasByoyomiInformation(node))
                    continue;
                print(key);
                for (int i = 0; i < sgfProps.getNumberValues(key); ++i)
                    print("[" + sgfProps.getValue(key, i) + "]");
            }
        int numberChildren = node.getNumberChildren();
        if (numberChildren == 0)
            return;
        if (numberChildren == 1)
        {
            printNode(node.getChildConst(), false);
            return;
        }
        for (int i = 0; i < numberChildren; ++i)
        {
            printNewLine();
            print("(");
            printNode(node.getChildConst(i), false);
            print(")");
        }
    }

    private void printPosition(ConstBoard board)
    {
        PointList black = new PointList();
        PointList white = new PointList();
        for (GoPoint p : board)
        {
            GoColor c = board.getColor(p);
            if (c == BLACK)
                black.add(p);
            else if (c == WHITE)
                white.add(p);
        }
        printSetup(black, white);
        printNewLine();
        printToPlay(board.getToMove());
    }

    private void printSetup(ConstPointList black, ConstPointList white)
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
        if (color == BLACK)
            print("PL[B]");
        else
            print("PL[W]");
    }
}
