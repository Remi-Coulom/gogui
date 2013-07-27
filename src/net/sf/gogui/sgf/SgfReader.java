// SgfReader.java

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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.MarkType;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.EMPTY;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.InvalidPointException;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Move;
import net.sf.gogui.go.PointList;
import net.sf.gogui.util.ByteCountInputStream;
import net.sf.gogui.util.ProgressShow;

/** SGF reader.
    @bug The error messages sometimes contain wrong line numbers, because of
    problems in StreamTokenizer.lineno(). Maybe the implementation should be
    replaced not using StreamTokenizer, because this class is a legacy class.
    (Does this happen only on Windows?) */
public final class SgfReader
{
    /** Read SGF file from stream.
        Default charset is ISO-8859-1 according to the SGF version 4 standard.
        The charset property is only respected if the stream is a
        FileInputStream, because it has to be reopened with a different
        encoding.
        The stream is closed after reading.
        @param in Stream to read from.
        @param file File name if input stream is a FileInputStream to allow
        reopening the stream after a charset change
        @param progressShow Callback to show progress, can be null
        @param size Size of stream if progressShow != null
        @throws SgfError If reading fails. */
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
                in.close();
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
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not close SGF stream");
            }
        }
    }

    /** Get game tree of loaded SGF file.
        @return The game tree. */
    public GameTree getTree()
    {
        return m_tree;
    }

    /** Get warnings that occurred during loading SGF file.
        @return String with warning messages or null if no warnings. */
    public String getWarnings()
    {
        if (m_warnings.isEmpty())
            return null;
        StringBuilder result = new StringBuilder(m_warnings.size() * 80);
        for (String s : m_warnings)
        {
            result.append(s);
            result.append('\n');
        }
        return result.toString();
    }

    private static class SgfCharsetChanged
        extends Exception
    {
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
    private final Set<String> m_warnings = new TreeSet<String>();

    private StreamTokenizer m_tokenizer;

    private final File m_file;

    private String m_newCharset;

    /** Pre-allocated temporary buffer for use within functions. */
    private final StringBuilder m_buffer = new StringBuilder(512);

    private final PointList m_pointList = new PointList();

    /** Map containing the properties of the current node. */
    private final Map<String,ArrayList<String>> m_props =
        new TreeMap<String,ArrayList<String>>();

    /** Apply some fixes for broken SGF files. */
    private void applyFixes()
    {
        Node root = m_tree.getRoot();
        GameInfo info = m_tree.getGameInfo(root);
        if (root.hasSetup() && root.getPlayer() == null)
        {
            if (info.getHandicap() > 0)
            {
                root.setPlayer(WHITE);
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
                    if (move.getColor() == BLACK)
                        hasBlackChildMoves = true;
                    if (move.getColor() == WHITE)
                        hasWhiteChildMoves = true;
                }
                if (hasBlackChildMoves && ! hasWhiteChildMoves)
                    root.setPlayer(BLACK);
                if (hasWhiteChildMoves && ! hasBlackChildMoves)
                    root.setPlayer(WHITE);
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
        These are still used in some old SGF files.
        @param property Property name
        @return Short standard version of the property or original property */
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
        else if (property == "BLACKRANK")
            shortName = "BR";
        else if (property == "COMMENT")
            shortName = "C";
        else if (property == "COPYRIGHT")
            shortName = "CP";
        else if (property == "DATE")
            shortName = "DT";
        else if (property == "EVENT")
            shortName = "EV";
        else if (property == "GAME")
            shortName = "GM";
        else if (property == "HANDICAP")
            shortName = "HA";
        else if (property == "KOMI")
            shortName = "KM";
        else if (property == "PLACE")
            shortName = "PC";
        else if (property == "PLAYERBLACK")
            shortName = "PB";
        else if (property == "PLAYERWHITE")
            shortName = "PW";
        else if (property == "PLAYER")
            shortName = "PL";
        else if (property == "RESULT")
            shortName = "RE";
        else if (property == "ROUND")
            shortName = "RO";
        else if (property == "RULES")
            shortName = "RU";
        else if (property == "SIZE")
            shortName = "SZ";
        else if (property == "WHITE")
            shortName = "W";
        else if (property == "WHITERANK")
            shortName = "WR";
        if (shortName != null)
            return shortName;
        return property;
    }

    private GameInfo createGameInfo(Node node)
    {
        return node.createGameInfo();
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

    private void handleProps(Node node, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        // Handle SZ property first to be able to parse points
        if (m_props.containsKey("SZ"))
        {
            ArrayList<String> values = m_props.get("SZ");
            m_props.remove("SZ");
            if (! isRoot)
                setWarning("Size property not in root node ignored");
            else
            {
                try
                {
                    int size = parseInt(values.get(0));
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
        for (Map.Entry<String,ArrayList<String>> entry : m_props.entrySet())
        {
            String p = entry.getKey();
            ArrayList<String> values = entry.getValue();
            String v = values.get(0);
            if (p == "AB")
            {
                parsePointList(values);
                node.addStones(BLACK, m_pointList);
            }
            else if (p == "AE")
            {
                parsePointList(values);
                node.addStones(EMPTY, m_pointList);
            }
            else if (p == "AN")
                set(node, StringInfo.ANNOTATION, v);
            else if (p == "AW")
            {
                parsePointList(values);
                node.addStones(WHITE, m_pointList);
            }
            else if (p == "B")
            {
                node.setMove(Move.get(BLACK, parsePoint(v)));
            }
            else if (p == "BL")
            {
                try
                {
                    node.setTimeLeft(BLACK, Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "BR")
                set(node, StringInfoColor.RANK, BLACK, v);
            else if (p == "BT")
                set(node, StringInfoColor.TEAM, BLACK, v);
            else if (p == "C")
                node.setComment(v);
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
            else if (p == "CP")
                set(node, StringInfo.COPYRIGHT, v);
            else if (p == "CR")
                parseMarked(node, MarkType.CIRCLE, values);
            else if (p == "DT")
                set(node, StringInfo.DATE, v);
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
                // Some SGF files contain HA[], interpret as unknown handicap
                v = v.trim();
                if (! v.equals(""))
                {
                    try
                    {
                        int handicap = Integer.parseInt(v);
                        if (handicap == 1 || handicap < 0)
                            setWarning("Invalid handicap value");
                        else
                            createGameInfo(node).setHandicap(handicap);
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
                    String value = values.get(i);
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
                    node.setMovesLeft(BLACK, Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "OM")
                parseOvertimeMoves(v);
            else if (p == "OP")
                parseOvertimePeriod(v);
            else if (p == "OT")
                parseOvertime(node, v);
            else if (p == "OW")
            {
                try
                {
                    node.setMovesLeft(WHITE, Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "PB")
                set(node, StringInfoColor.NAME, BLACK, v);
            else if (p == "PW")
                set(node, StringInfoColor.NAME, WHITE, v);
            else if (p == "PL")
                node.setPlayer(parseColor(v));
            else if (p == "RE")
                set(node, StringInfo.RESULT, v);
            else if (p == "RO")
                set(node, StringInfo.ROUND, v);
            else if (p == "RU")
                set(node, StringInfo.RULES, v);
            else if (p == "SO")
                set(node, StringInfo.SOURCE, v);
            else if (p == "SQ")
                parseMarked(node, MarkType.SQUARE, values);
            else if (p == "SL")
                parseMarked(node, MarkType.SELECT, values);
            else if (p == "TB")
                parseMarked(node, MarkType.TERRITORY_BLACK, values);
            else if (p == "TM")
                parseTime(node, v);
            else if (p == "TR")
                parseMarked(node, MarkType.TRIANGLE, values);
            else if (p == "US")
                set(node, StringInfo.USER, v);
            else if (p == "W")
                node.setMove(Move.get(WHITE, parsePoint(v)));
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
                    node.setTimeLeft(WHITE, Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p == "WR")
                set(node, StringInfoColor.RANK, WHITE, v);
            else if (p == "WT")
                set(node, StringInfoColor.TEAM, WHITE, v);
            else if (p != "FF" && p != "GN" && p != "AP")
                node.addSgfProperty(p, values);
        }
    }

    private GoColor parseColor(String s) throws SgfError
    {
        GoColor color;
        s = s.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals("b") || s.equals("1"))
            color = BLACK;
        else if (s.equals("w") || s.equals("2"))
            color = WHITE;
        else
            throw getError("Invalid color value");
        return color;
    }

    private int parseInt(String s) throws SgfError
    {
        int i = -1;
        try
        {
            i = Integer.parseInt(s.trim());
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
            Komi komi = Komi.parseKomi(value);
            createGameInfo(node).setKomi(komi);
            if (komi != null && ! komi.isMultipleOf(0.5))
                setWarning("Komi is not a multiple of 0.5");
        }
        catch (InvalidKomiException e)
        {
            setWarning("Invalid value for komi");
        }
    }

    private void parseMarked(Node node, MarkType type,
                             ArrayList<String> values)
        throws SgfError
    {
        parsePointList(values);
        for (GoPoint p : m_pointList)
            node.addMarked(p, type);
    }

    private void parseOvertime(Node node, String value)
    {
        SgfUtil.Overtime overtime = SgfUtil.parseOvertime(value);
        if (overtime == null)
            // Preserve information
            node.addSgfProperty("OT", value);
        else
        {
            m_byoyomi = overtime.m_byoyomi;
            m_byoyomiMoves = overtime.m_byoyomiMoves;
        }
    }

    /** FF3 OM property */
    private void parseOvertimeMoves(String value)
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
    private void parseOvertimePeriod(String value)
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
        @throw SgfError On invalid value */
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

    private void parsePointList(ArrayList<String> values) throws SgfError
    {
        m_pointList.clear();
        for (int i = 0; i < values.size(); ++i)
        {
            String value = values.get(i);
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

    /** TM property.
        According to FF4, TM needs to be a real value, but older SGF versions
        allow a string with unspecified content. We try to parse a few known
        formats. */
    private void parseTime(Node node, String value)
    {
        value = value.trim();
        if (value.equals("") || value.equals("-"))
            return;
        long preByoyomi = SgfUtil.parseTime(value);
        if (preByoyomi < 0)
        {
            setWarning("Unknown format in time property");
            node.addSgfProperty("TM", value); // Preserve information
        }
        else
            m_preByoyomi = preByoyomi;
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
        while (readProp());
        handleProps(son, isRoot);
        setTimeSettings(son);
        return son;
    }

    private boolean readProp() throws IOException, SgfError
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == StreamTokenizer.TT_WORD)
        {
            // Use intern() to allow fast comparsion with ==
            String p = m_tokenizer.sval.toUpperCase(Locale.ENGLISH).intern();
            ArrayList<String> values = new ArrayList<String>();
            String s;
            while ((s = readValue()) != null)
                values.add(s);
            if (values.isEmpty())
            {
                setWarning("Property \"" + p + "\" has no value");
                return true;
            }
            p = checkForObsoleteLongProps(p);
            if (m_props.containsKey(p))
                // Silently accept duplicate properties, as long as they have
                // the same value (only check for single value properties)
                if (m_props.get(p).size() > 1 || values.size() > 1
                    || ! values.get(0).equals(m_props.get(p).get(0)))
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
        Character last = null;
        while (true)
        {
            int c = m_reader.read();
            if (c < 0)
                throw getError("Property value incomplete");
            if (quoted)
            {
                if (c != '\n' && c != '\r')
                    m_buffer.append((char)c);
                last = Character.valueOf((char)c);
                quoted = false;
            }
            else
            {
                if (c == ']')
                    break;
                quoted = (c == '\\');
                if (! quoted)
                {
                    // Transform all linebreaks allowed in SGF (LF, CR, LFCR,
                    // CRLF) to a single '\n'
                    boolean isLinebreak = (c == '\n' || c == '\r');
                    boolean lastLinebreak =
                        (last != null && (last.charValue() == '\n'
                                          || last.charValue() == '\r'));
                    boolean filterSecondLinebreak =
                        (isLinebreak && lastLinebreak && c != last.charValue());
                    if (filterSecondLinebreak)
                        last = null;
                    else
                    {
                        if (isLinebreak)
                            m_buffer.append('\n');
                        else
                            m_buffer.append((char)c);
                        last = Character.valueOf((char)c);
                    }
                }
            }
        }
        return m_buffer.toString();
    }

    private void set(Node node, StringInfo type, String value)
    {
        GameInfo info = createGameInfo(node);
        info.set(type, value);
    }

    private void set(Node node, StringInfoColor type,
                                    GoColor c, String value)
    {
        GameInfo info = createGameInfo(node);
        info.set(type, c, value);
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
            node.createGameInfo().setTimeSettings(s);
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
