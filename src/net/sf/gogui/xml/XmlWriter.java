//----------------------------------------------------------------------------
// XmlWriter.java
//----------------------------------------------------------------------------

package net.sf.gogui.xml;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import java.util.Map;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstSgfProperties;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.util.HtmlUtil;

/** Write a game or board position in XML style to a stream.
    This class uses Jago's XML format, see http://www.rene-grothmann.de/jago
*/
public class XmlWriter
{
    /** Construct writer and write tree.
        @param usePass Write pass moves with empty at-property. This extension
        is used by GoSVG
        (http://homepage.ntlworld.com/daniel.gilder/gosvg.html), but not
        supported by Jago (which does not write pass moves using move
        elements, but adds a comment "Pass")
    */
    public XmlWriter(OutputStream out, ConstGameTree tree, String application,
                     boolean usePass)
    {
        m_usePass = usePass;
        try
        {
            m_out = new PrintStream(out, false, "UTF-8");
            m_out.print("<?xml version='1.0' encoding='utf-8'?>\n");
        }
        catch (UnsupportedEncodingException e)
        {
            // Every Java implementation is required to support UTF-8
            assert false;
            m_out = new PrintStream(out, false);
            m_out.print("<?xml version='1.0'?>\n");
        }
        ConstNode root = tree.getRootConst();
        ConstGameInfo info = tree.getGameInfoConst(root);
        m_out.print("<!DOCTYPE Go SYSTEM \"go.dtd\">\n" +
                    "<Go>\n" +
                    "<GoGame>\n");
        m_boardSize = tree.getBoardSize();
        printGameInfo(application, info);
        m_out.print("<Nodes>\n");
        printNode(root, true);
        m_out.print("</Nodes>\n" +
                    "</GoGame>\n" +
                    "</Go>\n");
        m_out.close();
    }

    private final boolean m_usePass;

    private int m_boardSize;

    private PrintStream m_out;

    private String getSgfPoint(GoPoint p)
    {
        if (p == null)
            return "";
        int x = 'a' + p.getX();
        int y = 'a' + (m_boardSize - p.getY() - 1);
        return "" + (char)x + (char)y;
    }

    private void printComment(String comment)
    {
        if (comment == null)
            return;
        // TODO: Insert paragraphs if necessary
        m_out.print("<Comment>" + HtmlUtil.escape(comment) + "</Comment>\n");
    }

    private void printGameInfo(String application, ConstGameInfo info)
    {
        m_out.print("<Information>\n" +
                    "<Application>" + application + "</Application>\n" +
                    "<BoardSize>" + m_boardSize + "</BoardSize>\n");
        printInfo("WhitePlayer", info.get(StringInfoColor.NAME, WHITE));
        printInfo("BlackPlayer", info.get(StringInfoColor.NAME, BLACK));
        printInfo("WhiteRank", info.get(StringInfoColor.RANK, WHITE));
        printInfo("BlackRank", info.get(StringInfoColor.RANK, BLACK));
        printInfo("Date", info.get(StringInfo.DATE));
        printInfo("Rules", info.get(StringInfo.RULES));
        if (info.getHandicap() > 0)
            printInfo("Handicap", Integer.toString(info.getHandicap()));
        if (info.getKomi() != null)
            printInfo("Komi", info.getKomi().toString());
        if (info.getTimeSettings() != null)
            printInfo("Time", info.getTimeSettings().toString());
        printInfo("Result", info.get(StringInfo.RESULT));
        printInfo("WhiteTeam", info.get(StringInfoColor.TEAM, WHITE));
        printInfo("BlackTeam", info.get(StringInfoColor.TEAM, BLACK));
        printInfo("User", info.get(StringInfo.USER));
        printInfo("Copyright", info.get(StringInfo.COPYRIGHT));
        printInfo("Annotation", info.get(StringInfo.ANNOTATION));
        printInfo("Source", info.get(StringInfo.SOURCE));
        printInfo("Round", info.get(StringInfo.ROUND));
        m_out.print("</Information>\n");
    }

    private void printInfo(String tag, String value)
    {
        if (value == null)
            return;
        m_out.print("<" + tag + ">" + value + "</" + tag + ">\n");
    }

    private void printMarkup(ConstNode node)
    {
        printMarkup(node, MarkType.MARK, "");
        printMarkup(node, MarkType.CIRCLE, " type=\"circle\"");
        printMarkup(node, MarkType.SQUARE, " type=\"square\"");
        printMarkup(node, MarkType.TRIANGLE, " type=\"triangle\"");
        printMarkup(node, MarkType.TERRITORY_BLACK, " territory=\"black\"");
        printMarkup(node, MarkType.TERRITORY_WHITE, " territory=\"white\"");
        ConstPointList pointList = node.getMarkedConst(MarkType.SELECT);
        if (pointList == null)
            return;
        for (GoPoint p : pointList)
            m_out.print("<SGF type=\"SL\"><Arg>" + getSgfPoint(p)
                        + "</Arg></SGF>\n");
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        if (labels != null)
            for (Map.Entry<GoPoint,String> e : labels.entrySet())
                m_out.print("<Mark at=\"" + e.getKey() + "\" label=\""
                            + e.getValue() + "\"/>\n");
    }

    private void printMarkup(ConstNode node, MarkType type, String attributes)
    {
        ConstPointList pointList = node.getMarkedConst(type);
        if (pointList == null)
            return;
        for (GoPoint p : pointList)
            m_out.print("<Mark at=\"" + p + "\"" + attributes + "/>\n");
    }

    private void printNode(ConstNode node, boolean isRoot)
    {
        // TODO: Warning, if game information node for this node is not root
        Move move = node.getMove();
        String comment = node.getComment();
        ConstSgfProperties sgfProperties = node.getSgfPropertiesConst();
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        boolean hasMarkup = (labels != null && ! labels.isEmpty());
        if (! hasMarkup)
            for (MarkType type : EnumSet.allOf(MarkType.class))
            {
                ConstPointList pointList = node.getMarkedConst(type);
                if (pointList != null && ! node.getMarkedConst(type).isEmpty())
                {
                    hasMarkup = true;
                    break;
                }
            }
        boolean isPass = (move != null && move.getPoint() == null);
        boolean hasSetup = node.hasSetup();
        boolean needsNode =
            (move == null || (isPass && ! m_usePass)
             || sgfProperties != null || hasSetup || hasMarkup);
        boolean isEmptyRoot =
            (isRoot && move == null && sgfProperties == null && ! hasSetup
             && ! hasMarkup);
        if (needsNode && ! isEmptyRoot)
            m_out.print("<Node>\n");
        if (move != null)
        {
            GoPoint p = move.getPoint();
            if (p != null || m_usePass)
            {
                String at = (p == null ? "" : p.toString());
                GoColor c = move.getColor();
                int number = NodeUtil.getMoveNumber(node);
                if (c == BLACK)
                    m_out.print("<Black number=\"" + number + "\" at=\"" + at
                                + "\"/>\n");
                else if (c == WHITE)
                    m_out.print("<White number=\"" + number + "\" at=\"" + at
                                + "\"/>\n");
            }
            else if (comment == null || comment.equals(""))
                comment = "Pass";
        }
        printSetup(node);
        printMarkup(node);
        printComment(comment);
        printSgfProperties(node);
        if (needsNode && ! isEmptyRoot)
            m_out.print("</Node>\n");
        ConstNode father = node.getFatherConst();
        if (father != null && father.getChildConst() == node)
        {
            int numberChildren = father.getNumberChildren();
            for (int i = 1; i < numberChildren; ++i)
            {
                m_out.print("<Variation>\n");
                printNode(father.getChildConst(i), false);
                m_out.print("</Variation>\n");
            }
        }
        ConstNode child = node.getChildConst();
        if (child != null)
            printNode(child, false);
    }

    private void printSgfProperties(ConstNode node)
    {
        ConstSgfProperties sgfProperties = node.getSgfPropertiesConst();
        if (sgfProperties == null)
            return;
        for (String key : sgfProperties.getKeys())
        {
            m_out.print("<SGF type=\"" + key + "\">");
            int numberValues = sgfProperties.getNumberValues(key);
            for (int i = 0; i < numberValues; ++i)
                m_out.print("<Arg>" +
                            HtmlUtil.escape(sgfProperties.getValue(key, i))
                            + "</Arg>");
            m_out.print("</SGF>\n");
        }
    }

    private void printSetup(ConstNode node)
    {
        for (GoPoint p : node.getSetup(BLACK))
            m_out.print("<AddBlack at=\"" + p + "\"/>\n");
        for (GoPoint p : node.getSetup(WHITE))
            m_out.print("<AddWhite at=\"" + p + "\"/>\n");
        for (GoPoint p : node.getSetup(EMPTY))
            m_out.print("<Delete at=\"" + p + "\"/>\n");
    }
}
