// XmlWriter.java

package net.sf.gogui.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.EnumSet;
import java.util.Map;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstSgfProperties;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.SgfProperties;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.EMPTY;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.ConstPointList;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.Move;
import net.sf.gogui.sgf.SgfUtil;
import net.sf.gogui.util.XmlUtil;

/** Write a game or board position in XML style to a stream.
    This class uses Jago's XML format, see http://www.rene-grothmann.de/jago
    It writes files that are valid XML documents according to the go.dtd
    from the Jago webpage (10/2007), see also the appendix "XML Format"
    of the GoGui documentation. */
public class XmlWriter
{
    /** Construct writer and write tree. */
    public XmlWriter(OutputStream out, ConstGameTree tree, String application)
    {
        try
        {
            m_out = new PrintStream(out, false, "UTF-8");
            m_out.print("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
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
        // Game name is not supported in game.GameInformation, but XmlReader
        // puts it into the SGF-Proprty "GN"
        String gameNameAtt = "";
        ConstSgfProperties sgfProperties = root.getSgfPropertiesConst();
        if (sgfProperties != null && sgfProperties.hasKey("GN")
            && sgfProperties.getNumberValues("GN") > 0)
            gameNameAtt = " name=\""
                + XmlUtil.escapeAttr(sgfProperties.getValue("GN", 0)) + "\"";
        m_out.print("<Go>\n" +
                    "<GoGame" + gameNameAtt + ">\n");
        m_boardSize = tree.getBoardSize();
        printGameInfo(application, info);
        m_out.print("<Nodes>\n");
        printNode(root, true);
        m_out.print("</Nodes>\n" +
                    "</GoGame>\n" +
                    "</Go>\n");
        m_out.close();
    }

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

    private void printElementWithParagraphs(String element, String value)
    {
        if (value == null)
            return;
        StringReader reader = new StringReader(value);
        m_out.print("<" + element + ">\n");
        boolean endsWithNewline = false;
        while (true)
        {
            String line = readLine(reader);
            if (line.equals(""))
                break;
            endsWithNewline = line.endsWith("\n");
            if (endsWithNewline)
                line = line.substring(0, line.length() - 1);
            if (line.equals(""))
                m_out.print("<P/>\n");
            else
                printElementLine("P", line);
        }
        if (endsWithNewline)
            m_out.print("<P/>\n");
        m_out.print("</" + element + ">\n");
    }

    private void printElementLine(String element, String text)
    {
        m_out.print("<" + element + ">" + XmlUtil.escapeText(text)
                    + "</" + element + ">\n");
    }

    private void printGameInfo(String application, ConstGameInfo info)
    {
        m_out.print("<Information>\n");
        if (application != null)
            printElementLine("Application", application);
        printElementLine("BoardSize", Integer.toString(m_boardSize));
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
        {
            long time = info.getTimeSettings().getPreByoyomi() / 1000L;
            printInfo("Time", Long.toString(time));
        }
        printInfo("Result", info.get(StringInfo.RESULT));
        printInfo("WhiteTeam", info.get(StringInfoColor.TEAM, WHITE));
        printInfo("BlackTeam", info.get(StringInfoColor.TEAM, BLACK));
        printInfo("User", info.get(StringInfo.USER));
        printInfo("Annotation", info.get(StringInfo.ANNOTATION));
        printInfo("Source", info.get(StringInfo.SOURCE));
        printInfo("Round", info.get(StringInfo.ROUND));
        printElementWithParagraphs("Copyright", info.get(StringInfo.COPYRIGHT));
        m_out.print("</Information>\n");
    }

    private void printInfo(String element, String value)
    {
        if (value == null)
            return;
        printElementLine(element, value);
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
        if (pointList != null)
            // There is no type select in the Mark element -> use SGF/SL
            for (GoPoint p : pointList)
                m_out.print("<SGF type=\"SL\"><Arg>" + getSgfPoint(p)
                            + "</Arg></SGF>\n");
        Map<GoPoint,String> labels = node.getLabelsUnmodifiable();
        if (labels != null)
            for (Map.Entry<GoPoint,String> e : labels.entrySet())
                m_out.print("<Mark at=\"" + e.getKey() + "\" label=\""
                            + XmlUtil.escapeAttr(e.getValue()) + "\"/>\n");
    }

    private void printMarkup(ConstNode node, MarkType type, String attributes)
    {
        ConstPointList pointList = node.getMarkedConst(type);
        if (pointList == null)
            return;
        for (GoPoint p : pointList)
            m_out.print("<Mark at=\"" + p + "\"" + attributes + "/>\n");
    }

    private void printMove(ConstNode node)
    {
        Move move = node.getMove();
        if (move == null)
            return;
        GoPoint p = move.getPoint();
        String at = (p == null ? "" : p.toString());
        GoColor c = move.getColor();
        int number = NodeUtil.getMoveNumber(node);
        String timeLeftAtt = "";
        double timeLeft = node.getTimeLeft(c);
        if (! Double.isNaN(timeLeft))
            timeLeftAtt = " timeleft=\"" + timeLeft + "\"";
        if (c == BLACK)
            m_out.print("<Black number=\"" + number + "\" at=\"" + at
                        + "\"" + timeLeftAtt + "/>\n");
        else if (c == WHITE)
            m_out.print("<White number=\"" + number + "\" at=\"" + at
                        + "\"" + timeLeftAtt + "/>\n");
        int movesLeft = node.getMovesLeft(c);
        // There is no movesleft attribute in Black/White -> use SGF/OW,OB
        if (movesLeft >= 0)
        {
            if (c == BLACK)
                m_out.print("<SGF type=\"OB\"><Arg>" + movesLeft
                            + "</Arg></SGF>\n");
            else if (c == WHITE)
                m_out.print("<SGF type=\"OW\"><Arg>" + movesLeft
                            + "</Arg></SGF>\n");
        }
    }

    private void printNode(ConstNode node, boolean isRoot)
    {
        Move move = node.getMove();
        String comment = node.getComment();
        SgfProperties sgfProps = NodeUtil.cleanSgfProps(node);
        // Game name is not supported in game.GameInformation, but XmlReader
        // puts it into the SGF-Proprty "N"
        String nameAtt = "";
        if (sgfProps.hasKey("N") && sgfProps.getNumberValues("N") > 0)
        {
            nameAtt = " name=\""
                + XmlUtil.escapeAttr(sgfProps.getValue("N", 0)) + "\"";
            sgfProps.remove("N");
        }

        // Preserve time left that cannot be written as timeleft attribute
        // of move element as SGF element
        if (! Double.isNaN(node.getTimeLeft(BLACK))
            && (move == null || move.getColor() != BLACK))
            sgfProps.add("BL", Double.toString(node.getTimeLeft(BLACK)));
        if (! Double.isNaN(node.getTimeLeft(WHITE))
            && (move == null || move.getColor() != WHITE))
            sgfProps.add("WL", Double.toString(node.getTimeLeft(WHITE)));

        ConstGameInfo info = node.getGameInfoConst();

        // Write overtime information as SGF element (no XML element exists)
        if (isRoot)
        {
            TimeSettings timeSettings = info.getTimeSettings();
            if (timeSettings != null)
            {
                String overtime = SgfUtil.getOvertime(timeSettings);
                if (overtime != null)
                    sgfProps.add("OT", overtime);
            }
        }

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
        boolean hasSetup = node.hasSetup() || node.getPlayer() != null;
        // Moves left are currently written as SGF element, which needs a Node
        boolean hasMovesLeft =
            (move != null && node.getMovesLeft(move.getColor()) != -1);
        boolean hasNonRootGameInfo = (info != null && ! isRoot);

        // Root is considered empty, even if it has game info, because
        // this is written in Information element
        boolean isEmptyButMoveOrComment
            = (sgfProps.isEmpty()
               && ! hasSetup && ! hasMarkup && ! hasMovesLeft
               && !  hasNonRootGameInfo);

        // Is a node element needed? (not if only move and comment)
        boolean needsNode = (! isEmptyButMoveOrComment || ! nameAtt.equals("")
                             || (move == null && comment != null));

        boolean isEmpty =
            (isEmptyButMoveOrComment && comment == null && move == null);

        if (isEmpty)
            m_out.print("<Node" + nameAtt + "/>\n");
        else
        {
            if (needsNode)
                m_out.print("<Node" + nameAtt + ">\n");
            printMove(node);
            printSetup(node);
            printMarkup(node);
            printElementWithParagraphs("Comment", comment);
            if (hasNonRootGameInfo)
                putGameInfoSgf(info, sgfProps);
            printSgfProperties(sgfProps);
            if (needsNode)
                m_out.print("</Node>\n");
        }

        ConstNode father = node.getFatherConst();
        if (father != null && father.getChildConst() == node)
        {
            int numberSiblings = father.getNumberChildren();
            for (int i = 1; i < numberSiblings; ++i)
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

    private void printSgfProperties(ConstSgfProperties sgfProps)
    {
        for (String key : sgfProps.getKeys())
        {
            m_out.print("<SGF type=\"" + key + "\">");
            int numberValues = sgfProps.getNumberValues(key);
            for (int i = 0; i < numberValues; ++i)
                m_out.print("<Arg>" +
                            XmlUtil.escapeText(sgfProps.getValue(key, i))
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
        GoColor player = node.getPlayer();
        // The BlackToPlay, WhiteToPlay elements in the DTD are not usable:
        // they don't have a legal parent and it is not clear why they
        // have a text content -> save player with a SGF property
        if (BLACK.equals(player))
            m_out.print("<SGF type=\"PL\"><Arg>B</Arg></SGF>\n");
        else if (WHITE.equals(player))
            m_out.print("<SGF type=\"PL\"><Arg>W</Arg></SGF>\n");
    }

    /** Put game information for non-root nodes into SGF properties.
        Game information for non-root nodes Not supported directly in XML. */
    private void putGameInfoSgf(ConstGameInfo info, SgfProperties sgfProps)
    {
        putGameInfoSgf(info, sgfProps, "PB", StringInfoColor.NAME, BLACK);
        putGameInfoSgf(info, sgfProps, "PW", StringInfoColor.NAME, WHITE);
        putGameInfoSgf(info, sgfProps, "BR", StringInfoColor.RANK, BLACK);
        putGameInfoSgf(info, sgfProps, "WR", StringInfoColor.RANK, WHITE);
        putGameInfoSgf(info, sgfProps, "BT", StringInfoColor.TEAM, BLACK);
        putGameInfoSgf(info, sgfProps, "WT", StringInfoColor.TEAM, WHITE);
        putGameInfoSgf(info, sgfProps, "DT", StringInfo.DATE);
        putGameInfoSgf(info, sgfProps, "RE", StringInfo.RESULT);
        putGameInfoSgf(info, sgfProps, "RO", StringInfo.ROUND);
        putGameInfoSgf(info, sgfProps, "RU", StringInfo.RULES);
        putGameInfoSgf(info, sgfProps, "US", StringInfo.USER);
        putGameInfoSgf(info, sgfProps, "CP", StringInfo.COPYRIGHT);
        putGameInfoSgf(info, sgfProps, "AN", StringInfo.ANNOTATION);
        if (info.getHandicap() > 0)
            putGameInfoSgf(sgfProps, "HA",
                           Integer.toString(info.getHandicap()));
        if (info.getKomi() != null)
            putGameInfoSgf(sgfProps, "KM", info.getKomi().toString());
        if (info.getTimeSettings() != null)
            putGameInfoSgf(sgfProps, "TM", info.getTimeSettings().toString());
    }

    private void putGameInfoSgf(SgfProperties sgfProps, String key,
                                String value)
    {
        sgfProps.add(key, value);
    }

    private void putGameInfoSgf(ConstGameInfo info, SgfProperties sgfProps,
                                String key, StringInfo type)
    {
        String value = info.get(type);
        if (value == null)
            return;
        sgfProps.add(key, value);
    }

    private void putGameInfoSgf(ConstGameInfo info, SgfProperties sgfProps,
                                String key, StringInfoColor type, GoColor c)
    {
        String value = info.get(type, c);
        if (value == null)
            return;
        sgfProps.add(key, value);
    }

    /** Reads a line without trimming the trailing newline. */
    private static String readLine(StringReader reader)
    {
        StringBuilder result = new StringBuilder();
        try
        {
            int c;
            do
            {
                c = reader.read();
                if (c == -1)
                    break;
                result.append((char)c);
            }
            while ((char)c != '\n');
        }
        catch (IOException e)
        {
            assert false;
        }
        return result.toString();
    }
}
