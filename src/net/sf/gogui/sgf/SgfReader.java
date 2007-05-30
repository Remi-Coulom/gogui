//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.ProgressShow;

class ByteCountInputStream
    extends InputStream
{
    public ByteCountInputStream(InputStream in)
    {
        m_in = in;
    }

    public long getCount()
    {
        return m_byteCount;
    }

    public int read() throws IOException
    {
        int result = m_in.read();
        if (result > 0)
            ++m_byteCount;
        return result;
    }

    public int read(byte[] b) throws IOException
    {
        int result = m_in.read(b);
        if (result > 0)
            m_byteCount += result;
        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        int result = m_in.read(b, off, len);
        if (result > 0)
            m_byteCount += result;
        return result;
    }

    private long m_byteCount;

    private final InputStream m_in;
}

/** SGF reader.
    @bug The error messages sometimes contain wrong line numbers, because of
    problems in StreamTokenizer.lineno(). Maybe the implementation should be
    replaced not using StreamTokenizer, because this class is a legacy class.
    (Does this happen only on Windows?)
*/
public final class SgfReader
{
    /** Read SGF file from stream.
        Default charset is ISO-8859-1 according to the SGF version 4 standard.
        The charset property is only respected if the stream is a
        FileInputStream, because it has to be reopened with a different
        encoding.
        @param in Stream to read from.
        @param file File name if input stream is a FileInputStream to allow
        reopening the stream after a charset change
        @param progressShow Callback to show progress, can be null
        @param size Size of stream if progressShow != null
        @throws SgfError If reading fails.
    */
    public SgfReader(InputStream in, File file, ProgressShow progressShow,
                     long size)
        throws SgfError
    {
        m_file = file;
        m_progressShow = progressShow;
        m_size = size;
        m_isFile = (in instanceof FileInputStream && file != null);
        if (progressShow != null)
            progressShow.showProgress(0);
        try
        {
            // SGF FF 4 standard defines ISO-8859-1 as default
            readSgf(in, "ISO-8859-1");
        }
        catch (SgfCharsetChanged e1)
        {
            try
            {
                in = new FileInputStream(file);
            }
            catch (IOException e2)
            {
                throw new SgfError("Could not reset SGF stream after"
                                   + " charset change.");
            }
            try
            {
                readSgf(in, m_newCharset);
            }
            catch (SgfCharsetChanged e3)
            {
                assert false;
            }
        }
    }

    /** Get game tree of loaded SGF file.
        @return The game tree.
    */
    public GameTree getTree()
    {
        return m_tree;
    }

    /** Get warnings that occurred during loading SGF file.
        @return String with warning messages or null if no warnings.
    */
    public String getWarnings()
    {
        if (m_warnings.isEmpty())
            return null;
        StringBuffer result = new StringBuffer(m_warnings.size() * 80);
        Iterator iter = m_warnings.iterator();
        while (iter.hasNext())
        {
            String s = (String)iter.next();
            result.append(s);
            result.append('\n');
        }
        return result.toString();
    }

    private static class SgfCharsetChanged
        extends Exception
    {
        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sf.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    private final boolean m_isFile;

    /** Has current node inconsistent FF3 overtime settings properties. */
    private boolean m_ignoreOvertime;

    private int m_lastPercent;

    private int m_boardSize;

    private int m_byoyomiMoves;

    private final long m_size;

    private long m_byoyomi;

    private long m_preByoyomi;

    private ByteCountInputStream m_byteCountInputStream;

    private java.io.Reader m_reader;

    private GameTree m_tree;

    private final ProgressShow m_progressShow;

    /** Contains strings with warnings. */
    private final TreeSet m_warnings = new TreeSet();

    private StreamTokenizer m_tokenizer;

    private final File m_file;

    private String m_newCharset;

    /** Pre-allocated temporary buffer for use within functions. */
    private final StringBuffer m_buffer = new StringBuffer(512);

    private final PointList m_pointList = new PointList();

    /** Map containing the properties of the current node. */
    private final Map m_props = new TreeMap();

    private void addSgfProperty(Node node, String property, ArrayList values)
    {
        m_buffer.setLength(0);
        for (int i = 0; i < values.size(); ++i)
        {
            m_buffer.append('[');
            m_buffer.append((String)values.get(i));
            m_buffer.append(']');
        }
        node.addSgfProperty(property, m_buffer.toString());
    }

    /** Apply some fixes for broken SGF files. */
    private void applyFixes()
    {
        Node root = m_tree.getRoot();
        GameInformation info = m_tree.getGameInformation(root);
        if (root.hasSetup() && root.getPlayer() == null)
        {
            if (info.getHandicap() > 0)
            {
                root.setPlayer(GoColor.WHITE);
            }
            else
            {
                boolean hasBlackChildMoves = false;
                boolean hasWhiteChildMoves = false;
                for (int i = 0; i < root.getNumberChildren(); ++i)
                {
                    Move move = root.getChild(i).getMove();
                    if (move == null)
                        continue;
                    if (move.getColor() == GoColor.BLACK)
                        hasBlackChildMoves = true;
                    if (move.getColor() == GoColor.WHITE)
                        hasWhiteChildMoves = true;
                }
                if (hasBlackChildMoves && ! hasWhiteChildMoves)
                    root.setPlayer(GoColor.BLACK);
                if (hasWhiteChildMoves && ! hasBlackChildMoves)
                    root.setPlayer(GoColor.WHITE);
            }
        }
    }

    private void checkEndOfFile() throws SgfError, IOException
    {
        while (true)
        {
            m_tokenizer.nextToken();
            int t = m_tokenizer.ttype;
            if (t == '(')
                throw getError("Multiple SGF trees not supported");
            else if (t == StreamTokenizer.TT_EOF)
                return;
            else if (t != ' ' && t != '\t' && t != '\n' && t != '\r')
            {
                setWarning("Extra text after SGF tree");
                return;
            }
        }
    }

    /** Check for obsolete long names for standard properties.
        @param property Property name
        @return Short standard version of the property or original property
    */
    private String checkForObsoleteLongProps(String property)
    {
        if (property.length() <= 2)
            return property;
        property = property.intern();
        String shortName = null;
        if (property == "ADDBLACK")
            shortName = "AB";
        else if (property == "ADDEMPTY")
            shortName = "AE";
        else if (property == "ADDWHITE")
            shortName = "AW";
        else if (property == "BLACK")
            shortName = "B";
        else if (property == "COMMENT")
            shortName = "C";
        else if (property == "DATE")
            shortName = "DT";
        else if (property == "GAME")
            shortName = "GM";
        else if (property == "HANDICAP")
            shortName = "HA";
        else if (property == "KOMI")
            shortName = "KM";
        else if (property == "PLAYERBLACK")
            shortName = "PB";
        else if (property == "PLAYERWHITE")
            shortName = "PW";
        else if (property == "PLAYER")
            shortName = "PL";
        else if (property == "RESULT")
            shortName = "RE";
        else if (property == "RULES")
            shortName = "RU";
        else if (property == "SIZE")
            shortName = "SZ";
        else if (property == "WHITE")
            shortName = "W";
        if (shortName != null)
        {
            setWarning("Verbose names for standard properties");
            return shortName;
        }
        return property;
    }

    private GameInformation createGameInformation(Node node)
    {
        return node.createGameInformation();
    }

    private void findRoot() throws SgfError, IOException
    {
        while (true)
        {
            m_tokenizer.nextToken();
            int t = m_tokenizer.ttype;
            if (t == '(')
            {
                // Better make sure that ( is followed by a node
                m_tokenizer.nextToken();
                t = m_tokenizer.ttype;
                if (t == ';')
                {
                    m_tokenizer.pushBack();
                    return;
                }
                else
                    setWarning("Extra text before SGF tree");
            }
            else if (t == StreamTokenizer.TT_EOF)
                throw getError("No root tree found");
            else
                setWarning("Extra text before SGF tree");
        }
    }

    private int getBoardSize()
    {
        if (m_boardSize == -1)
            m_boardSize = 19; // Default size for Go in the SGF standard
        return m_boardSize;
    }

    private SgfError getError(String message)
    {
        int lineNumber = m_tokenizer.lineno();
        if (m_file == null)
            return new SgfError(lineNumber + ": " + message);
        else
        {
            String s = m_file.getName() + ":" + lineNumber + ": " + message;
            return new SgfError(s);
        }
    }

    private GoPoint getPointList(int i)
    {
        return m_pointList.get(i);
    }

    private void handleProps(Node node, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        // Handle SZ property first to be able to parse points
        if (m_props.containsKey("SZ"))
        {
            ArrayList values = (ArrayList)m_props.get("SZ");
            m_props.remove("SZ");
            if (! isRoot)
                setWarning("Size property not in root node ignored");
            else
            {
                try
                {
                    int size = parseInt((String)values.get(0));
                    if (size <= 0 || size > GoPoint.MAX_SIZE)
                        setWarning("Invalid board size value");
                    assert m_boardSize == -1;
                    m_boardSize = size;
                }
                catch (NumberFormatException e)
                {
                    setWarning("Invalid board size value");
                }
            }
        }
        Iterator it = m_props.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            String p = (String)entry.getKey();
            ArrayList values = (ArrayList)entry.getValue();
            String v = (String)values.get(0);
            if (p == "AB")
            {
                parsePointList(values);
                node.addStones(GoColor.BLACK, m_pointList);
            }
            else if (p == "AE")
            {
                parsePointList(values);
                node.addStones(GoColor.EMPTY, m_pointList);
            }
            else if (p == "AW")
            {
                parsePointList(values);
                node.addStones(GoColor.WHITE, m_pointList);
            }
            else if (p == "B")
            {
                node.setMove(Move.get(GoColor.BLACK, parsePoint(v)));
            }
            else if (p == "BL")
            {
                try
                {
                    node.setTimeLeft(GoColor.BLACK, Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "BR")
                createGameInformation(node).setRank(GoColor.BLACK, v);
            else if (p == "C")
            {
                String comment;
                if (node.getComment() == null)
                    comment = v.trim();
                else
                    comment = node.getComment() + "\n" + v.trim();
                node.setComment(comment);
            }
            else if (p == "CA")
            {
                if (isRoot && m_isFile && m_newCharset == null)
                {
                    m_newCharset = v.trim();
                    if (Charset.isSupported(m_newCharset))
                        throw new SgfCharsetChanged();
                    else
                        setWarning("Unknown character set \"" + m_newCharset
                                   + "\"");
                }
            }
            else if (p == "CR")
                parseMarked(node, MarkType.CIRCLE, values);
            else if (p == "DT")
                createGameInformation(node).setDate(v);
            else if (p == "FF")
            {
                int format = -1;
                try
                {
                    format = Integer.parseInt(v);
                }
                catch (NumberFormatException e)
                {
                }
                if (format < 1 || format > 4)
                    setWarning("Unknown SGF file format version");
            }
            else if (p == "GM")
            {
                // Some SGF files contain GM[], interpret as GM[1]
                v = v.trim();
                if (! v.equals("") && ! v.equals("1"))
                    throw getError("Not a Go game");
            }
            else if (p == "HA")
            {
                // Some SGF files contain HA[], interpret as no handicap
                v = v.trim();
                if (! v.equals(""))
                {
                    try
                    {
                        int handicap = Integer.parseInt(v);
                        createGameInformation(node).setHandicap(handicap);
                    }
                    catch (NumberFormatException e)
                    {
                        setWarning("Invalid handicap value");
                    }
                }
            }
            else if (p == "KM")
                parseKomi(node, v);
            else if (p == "LB")
            {
                for (int i = 0; i < values.size(); ++i)
                {
                    String value = (String)values.get(i);
                    int pos = value.indexOf(':');
                    if (pos > 0)
                    {
                        GoPoint point = parsePoint(value.substring(0, pos));
                        String text = value.substring(pos + 1);
                        node.setLabel(point, text);
                    }
                }
            }
            else if (p == "MA" || p == "M")
                parseMarked(node, MarkType.MARK, values);
            else if (p == "OB")
            {
                try
                {
                    node.setMovesLeft(GoColor.BLACK, Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "OM")
                parseOverTimeMoves(v);
            else if (p == "OP")
                parseOverTimePeriod(v);
            else if (p == "OT")
                parseOverTime(node, v);
            else if (p == "OW")
            {
                try
                {
                    node.setMovesLeft(GoColor.WHITE, Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "PB")
                createGameInformation(node).setPlayer(GoColor.BLACK, v);
            else if (p == "PW")
                createGameInformation(node).setPlayer(GoColor.WHITE, v);
            else if (p == "PL")
                node.setPlayer(parseColor(v));
            else if (p == "RE")
                createGameInformation(node).setResult(v);
            else if (p == "RU")
                createGameInformation(node).setRules(v);
            else if (p == "SQ")
                parseMarked(node, MarkType.SQUARE, values);
            else if (p == "SL")
                parseMarked(node, MarkType.SELECT, values);
            else if (p == "TB")
                parseMarked(node, MarkType.TERRITORY_BLACK, values);
            else if (p == "TM")
                parseTime(v);
            else if (p == "TR")
                parseMarked(node, MarkType.TRIANGLE, values);
            else if (p == "W")
                node.setMove(Move.get(GoColor.WHITE, parsePoint(v)));
            else if (p == "TW")
                parseMarked(node, MarkType.TERRITORY_WHITE, values);
            else if (p == "V")
            {
                try
                {
                    node.setValue(Float.parseFloat(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "WL")
            {
                try
                {
                    node.setTimeLeft(GoColor.WHITE, Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "WR")
                createGameInformation(node).setRank(GoColor.WHITE, v);
            else if (p != "FF" && p != "GN" && p != "AP")
                addSgfProperty(node, p, values);
        }
    }

    private GoColor parseColor(String s) throws SgfError
    {
        GoColor color;
        s = s.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals("b") || s.equals("1"))
            color = GoColor.BLACK;
        else if (s.equals("w") || s.equals("2"))
            color = GoColor.WHITE;
        else
            throw getError("Invalid color value");
        return color;
    }

    private int parseInt(String s) throws SgfError
    {
        int i = -1;
        try
        {
            i = Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw getError("Number expected");
        }
        return i;
    }

    private void parseKomi(Node node, String value) throws SgfError
    {
        try
        {
            createGameInformation(node).setKomi(Komi.parseKomi(value));
        }
        catch (InvalidKomiException e)
        {
            setWarning("Invalid value for komi");
        }
    }

    private void parseMarked(Node node, MarkType type, ArrayList values)
        throws SgfError
    {
        parsePointList(values);
        for (int i = 0; i < m_pointList.size(); ++i)
            node.addMarked(getPointList(i), type);
    }

    private void parseOverTime(Node node, String value)
    {
        /* Used by SgfWriter */
        if (parseOverTime(value,
                          "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*sec\\s*",
                          true, 1000L))
            return;
        /* Used by Smart Go */
        if (parseOverTime(value,
                          "\\s*(\\d+)\\s*moves\\s*/\\s*(\\d+)\\s*min\\s*",
                          true, 60000L))
            return;
        /* Used by Kiseido Game Server, CGoban 2 */
        if (parseOverTime(value,
                          "\\s*(\\d+)x(\\d+)\\s*byo-yomi\\s*",
                          true, 1000L))
            return;
        /* Used by ? */
        if (parseOverTime(value,
                          "\\s*(\\d+)x(\\d+)\\s*",
                          true, 1000L))
            return;
        /* Used by Quarry, CGoban 2 */
        if (parseOverTime(value,
                          "\\s*(\\d+)/(\\d+)\\s*canadian\\s*",
                          true, 1000L))
            return;
        setWarning("overtime settings in unknown format");
        ArrayList values = new ArrayList();
        values.add(value);
        addSgfProperty(node, "OT", values);
    }

    private boolean parseOverTime(String value, String regex,
                                  boolean byoyomiMovesFirst,
                                  long timeUnitFactor)
    {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        long byoyomi;
        int byoyomiMoves;
        if (matcher.matches())
        {
            assert matcher.groupCount() == 2;
            try
            {
                String group1;
                String group2;
                if (byoyomiMovesFirst)
                {
                    group1 = matcher.group(1);
                    group2 = matcher.group(2);
                }
                else
                {
                    group1 = matcher.group(2);
                    group2 = matcher.group(1);
                }
                byoyomiMoves = Integer.parseInt(group1);
                byoyomi = (long)(Double.parseDouble(group2) * timeUnitFactor);
            }
            catch (NumberFormatException e)
            {
                // should not happen if patterns match only integer
                assert false;
                return false;
            }
        }
        else
            return false;
        m_byoyomi = byoyomi;
        m_byoyomiMoves = byoyomiMoves;
        return true;
    }

    /** FF3 OM property */
    private void parseOverTimeMoves(String value)
    {
        try
        {
            m_byoyomiMoves = Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi moves");
            m_ignoreOvertime = true;
        }
    }

    /** FF3 OP property */
    private void parseOverTimePeriod(String value)
    {
        try
        {
            m_byoyomi = (long)(Double.parseDouble(value) * 1000);
        }
        catch (NumberFormatException e)
        {
            setWarning("Invalid value for byoyomi time");
            m_ignoreOvertime = true;
        }
    }

    /** Parse point value.
        @return Point or null, if pass move
        @throw SgfError On invalid value
    */
    private GoPoint parsePoint(String s) throws SgfError
    {
        s = s.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals(""))
            return null;
        if (s.length() > 2
            || (s.length() == 2 && s.charAt(1) < 'a' || s.charAt(1) > 'z'))
        {
            // Try human-readable encoding as used by SmartGo
            try
            {
                return GoPoint.parsePoint(s, GoPoint.MAX_SIZE);
            }
            catch (InvalidPointException e)
            {
                throwInvalidCoordinates(s);
            }
        }
        else if (s.length() != 2)
            throwInvalidCoordinates(s);
        int boardSize = getBoardSize();
        if (s.equals("tt") && boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
        {
            if (x == boardSize && y == -1)
            {
                // Some programs encode pass moves, e.g. as jj for boardsize 9
                setWarning("Non-standard pass move encoding");
                return null;
            }
            throw getError("Coordinates \"" + s + "\" outside board size "
                           + boardSize);
        }
        return GoPoint.get(x, y);
    }

    private void parsePointList(ArrayList values) throws SgfError
    {
        m_pointList.clear();
        for (int i = 0; i < values.size(); ++i)
        {
            String value = (String)values.get(i);
            int pos = value.indexOf(':');
            if (pos < 0)
            {
                GoPoint point = parsePoint(value);
                if (point == null)
                    setWarning("Point list argument contains PASS");
                else
                    m_pointList.add(point);
            }
            else
            {
                GoPoint point1 = parsePoint(value.substring(0, pos));
                GoPoint point2 = parsePoint(value.substring(pos + 1));
                if (point1 == null || point2 == null)
                {
                    setWarning("Compressed point list contains PASS");
                    continue;
                }
                int xMin = Math.min(point1.getX(), point2.getX());
                int xMax = Math.max(point1.getX(), point2.getX());
                int yMin = Math.min(point1.getY(), point2.getY());
                int yMax = Math.max(point1.getY(), point2.getY());
                for (int x = xMin; x <= xMax; ++x)
                    for (int y = yMin; y <= yMax; ++y)
                        m_pointList.add(GoPoint.get(x, y));
            }
        }
    }

    /** FF4 TM property */
    private void parseTime(String value)
    {
        value = value.trim();
        if (value.equals("") || value.equals("-"))
            return;
        try
        {
            m_preByoyomi = (long)(Double.parseDouble(value) * 1000);
            return;
        }
        catch (NumberFormatException e1)
        {
        }
        try
        {
            Pattern pattern;
            Matcher matcher;
            // Pattern as written by CGoban 1.9.12
            pattern = Pattern.compile("(\\d{1,2}+):(\\d\\d)");
            matcher = pattern.matcher(value.trim());
            if (matcher.matches())
            {
                assert matcher.groupCount() == 2;
                m_preByoyomi =
                    (Integer.parseInt(matcher.group(1)) * 60L
                     + Integer.parseInt(matcher.group(2))) * 1000L;
                return;
            }
            pattern = Pattern.compile("(\\d+):(\\d\\d):(\\d\\d)");
            matcher = pattern.matcher(value.trim());
            if (matcher.matches())
            {
                assert matcher.groupCount() == 3;
                m_preByoyomi =
                    (Integer.parseInt(matcher.group(1)) * 3600L
                     + Integer.parseInt(matcher.group(2)) * 60L
                     + Integer.parseInt(matcher.group(3))) * 1000L;
                return;
            }
        }
        catch (NumberFormatException e2)
        {
            assert false; // patterns should match only valid integers
            return;
        }
        setWarning("Invalid value for time");
    }

    private Node readNext(Node father, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        if (m_progressShow != null)
        {
            int percent;
            if (m_size > 0)
            {
                long count = m_byteCountInputStream.getCount();
                percent = (int)(count * 100 / m_size);
            }
            else
                percent = 100;
            if (percent != m_lastPercent)
                m_progressShow.showProgress(percent);
            m_lastPercent = percent;
        }
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == '(')
        {
            Node node = father;
            while (node != null)
                node = readNext(node, false);
            return father;
        }
        if (ttype == ')')
            return null;
        if (ttype == StreamTokenizer.TT_EOF)
        {
            setWarning("Game tree not closed");
            return null;
        }
        if (ttype != ';')
            throw getError("Next node expected");
        Node son = new Node();
        if (father != null)
            father.append(son);
        m_ignoreOvertime = false;
        m_byoyomiMoves = -1;
        m_byoyomi = -1;
        m_preByoyomi = -1;
        m_props.clear();
        while (readProp(son));
        handleProps(son, isRoot);
        setTimeSettings(son);
        return son;
    }

    private boolean readProp(Node node)
        throws IOException, SgfError
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == StreamTokenizer.TT_WORD)
        {
            // Use intern() to allow fast comparsion with ==
            String p = m_tokenizer.sval.toUpperCase(Locale.ENGLISH).intern();
            ArrayList values = new ArrayList();
            String s;
            while ((s = readValue()) != null)
                values.add(s);
            if (values.isEmpty())
                throw getError("Property \"" + p + "\" has no value");
            p = checkForObsoleteLongProps(p);
            if (m_props.containsKey(p))
                setWarning("Duplicate property " + p + " in node");
            m_props.put(p, values);
            return true;
        }
        if (ttype != '\n')
            // Don't pushBack newline, will confuse lineno() (Bug 4942853)
            m_tokenizer.pushBack();
        return false;
    }

    private void readSgf(InputStream in, String charset)
        throws SgfError, SgfCharsetChanged
    {
        try
        {
            m_boardSize = -1;
            if (m_progressShow != null)
            {
                m_byteCountInputStream = new ByteCountInputStream(in);
                in = m_byteCountInputStream;
            }
            InputStreamReader reader;
            try
            {
                reader = new InputStreamReader(in, charset);
            }
            catch (UnsupportedEncodingException e)
            {
                // Should actually not happen, because this function is only
                // called with charset ISO-8859-1 (should be supported on every
                // Java platform according to Charset documentation) or with a
                // CA property value, which was already checked with
                // Charset.isSupported()
                setWarning("Character set \"" + charset + "\" not supported");
                reader = new InputStreamReader(in);
            }
            m_reader = new BufferedReader(reader);
            m_tokenizer = new StreamTokenizer(m_reader);
            findRoot();
            Node root = readNext(null, true);
            Node node = root;
            while (node != null)
                node = readNext(node, false);
            checkEndOfFile();
            getBoardSize(); // Set to default value if still unknown
            m_tree = new GameTree(m_boardSize, root);
            applyFixes();
        }
        catch (FileNotFoundException e)
        {
            throw new SgfError("File not found.");
        }
        catch (IOException e)
        {
            throw new SgfError("IO error");
        }
        catch (OutOfMemoryError e)
        {
            throw new SgfError("Out of memory");
        }
    }

    private String readValue() throws IOException, SgfError
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype != '[')
        {
            if (ttype != '\n')
                // Don't pushBack newline, will confuse lineno() (Bug 4942853)
                m_tokenizer.pushBack();
            return null;
        }
        m_buffer.setLength(0);
        boolean quoted = false;
        while (true)
        {
            int c = m_reader.read();
            if (c < 0)
                throw getError("Property value incomplete");
            if (quoted)
            {
                if (c != '\n' && c != '\r')
                {
                    m_buffer.append((char)c);
                    quoted = false;
                }
            }
            else
            {
                if (c == ']')
                    break;
                quoted = (c == '\\');
                if (! quoted)
                    m_buffer.append((char)c);
            }
        }
        return m_buffer.toString();
    }

    private void setTimeSettings(Node node)
    {
        TimeSettings s = null;
        if (m_preByoyomi > 0
            && (m_ignoreOvertime || m_byoyomi <= 0 || m_byoyomiMoves <= 0))
            s = new TimeSettings(m_preByoyomi);
        else if (m_preByoyomi <= 0 && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(0, m_byoyomi, m_byoyomiMoves);
        else if (m_preByoyomi > 0  && ! m_ignoreOvertime && m_byoyomi > 0
                 && m_byoyomiMoves > 0)
            s = new TimeSettings(m_preByoyomi, m_byoyomi, m_byoyomiMoves);
        if (s != null)
            node.createGameInformation().setTimeSettings(s);
    }

    private void setWarning(String message)
    {
        m_warnings.add(message);
    }

    private void throwInvalidCoordinates(String s) throws SgfError
    {
        throw getError("Invalid coordinates \"" + s + "\"");
    }
}
