//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package sgf;

//----------------------------------------------------------------------------

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import game.*;
import go.*;
import utils.ErrorMessage;
import utils.ProgressShow;

//----------------------------------------------------------------------------

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

    private InputStream m_in;
}

//----------------------------------------------------------------------------

public class Reader
{
    public static class SgfError
        extends ErrorMessage
    {
        public SgfError(String s)
        {
            super(s);
        }
    }    

    /** Read SGF file from stream.
        Default charset is ISO-8859-1.
        The charset property is only respected if the stream is a
        FileInputStream, because it has to be reopened with a different
        encoding.
        @param in Stream to read from.
        @param name Name prepended to error messages (must be the filename
        for FileInputStream to allow reopening the stream after a charset
        change)
        @param progressShow Callback to show progress, can be null
        @param size Size of stream if progressShow != null
    */
    public Reader(InputStream in, String name, ProgressShow progressShow,
                  long size)
        throws SgfError
    {
        m_name = name;
        m_progressShow = progressShow;
        m_size = size;
        m_isFile = (in instanceof FileInputStream);
        if (progressShow != null)
            progressShow.showProgress(0);
        try
        {
            readSgf(in, "ISO-8859-1");
        }
        catch (SgfCharsetChanged e1)
        {
            try
            {
                in = new FileInputStream(new File(name));
            }
            catch (IOException e2)
            {
                throw new SgfError("Could not reset SGF stream after"
                                   + " charset change");
            }
            try
            {
                readSgf(in, m_newCharset);
            }
            catch (SgfCharsetChanged e3)
            {
                assert(false);
            }
        }
    }

    public GameTree getGameTree()
    {
        return m_gameTree;
    }

    /** Returns string with warning messages or null if no warnings. */
    public String getWarnings()
    {
        String result = "";
        // More severe warnings first
        if (m_warningExtraText)
            result = result + "Extra text before game tree\n";
        if (m_warningFormat)
            result = result + "Unknown SGF file format version\n";
        if (m_warningInvalidBoardSize)
            result = result + "Invalid board size value\n";
        if (m_warningWrongPass)
            result = result + "Non-standard pass move encoding\n";
        if (m_warningSizeOutsideRoot)
            result = result + "Size property not in root node\n";
        if (m_warningInvalidHandicap)
            result = result + "Invalid handicap value\n";
        if (m_warningLongProps)
            result = result + "Verbose names for standard properties\n";
        if (m_warningGame)
            result = result + "Empty value for game type\n";
        if (result.equals(""))
            return null;
        return result;
    }

    private static class SgfCharsetChanged
        extends Exception
    {
    }

    private static final int CACHE_SIZE = 30;

    private boolean m_isFile;

    private boolean m_sizeFixed;

    private boolean m_warningExtraText;

    private boolean m_warningFormat;

    /** GM value should be 1, sgf2misc produces Go files with empty value */
    private boolean m_warningGame;

    private boolean m_warningInvalidBoardSize;

    private boolean m_warningInvalidHandicap;

    private boolean m_warningLongProps;

    private boolean m_warningSizeOutsideRoot;

    private boolean m_warningWrongPass;

    private int m_lastPercent;

    private long m_size;

    private ByteCountInputStream m_byteCountInputStream;

    private java.io.Reader m_reader;

    private GameInformation m_gameInformation;

    private GameTree m_gameTree;

    private Move[][] m_moveBlackCache = new Move[CACHE_SIZE][CACHE_SIZE];

    private Move[][] m_moveWhiteCache = new Move[CACHE_SIZE][CACHE_SIZE];

    private Move m_passBlackCache = new Move(null, Color.BLACK);

    private Move m_passWhiteCache = new Move(null, Color.WHITE);

    private Point[][] m_pointCache = new Point[CACHE_SIZE][CACHE_SIZE];

    private ProgressShow m_progressShow;

    private StreamTokenizer m_tokenizer;

    private String m_charset;

    private String m_name;

    private String m_newCharset;

    /** Apply some fixes for broken SGF files. */
    private void applyFixes()
    {
        Node root = m_gameTree.getRoot();
        GameInformation gameInformation = m_gameTree.getGameInformation();
        if ((root.getNumberAddWhite() + root.getNumberAddBlack() > 0)
            && root.getPlayer() == Color.EMPTY)
        {
            if (gameInformation.m_handicap > 0)
            {
                root.setPlayer(Color.WHITE);
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
                    if (move.getColor() == Color.BLACK)
                        hasBlackChildMoves = true;
                    if (move.getColor() == Color.WHITE)
                        hasWhiteChildMoves = true;
                }
                if (hasBlackChildMoves && ! hasWhiteChildMoves)
                    root.setPlayer(Color.BLACK);
                if (hasWhiteChildMoves && ! hasBlackChildMoves)
                    root.setPlayer(Color.WHITE);
            }
        }
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
                    m_warningExtraText = true;
            }
            else if (t == StreamTokenizer.TT_EOF)
                throw getError("No root tree found");
            else
                m_warningExtraText = true;
        }
    }

    private SgfError getError(String message)
    {
        int lineNumber = m_tokenizer.lineno();
        if (m_name != null)
        {
            String s = m_name + ":" + lineNumber + ": " + message;
            return new SgfError(s);
        }
        else
            return new SgfError(lineNumber + ": " + message);
    }

    private Move getMove(Point point, Color color)
    {
        if (point == null)
        {
            if (color == Color.BLACK)
                return m_passBlackCache;
            else
            {
                assert(color == Color.WHITE);
                return m_passWhiteCache;
            }
        }
        int x = point.getX();
        int y = point.getY();
        if (x < CACHE_SIZE && y < CACHE_SIZE)
        {
            if (color == Color.BLACK)
            {
                if (m_moveBlackCache[x][y] == null)
                    m_moveBlackCache[x][y] = new Move(point, color);
                return m_moveBlackCache[x][y];
            }
            else
            {
                assert(color == Color.WHITE);
                if (m_moveWhiteCache[x][y] == null)
                    m_moveWhiteCache[x][y] = new Move(point, color);
                return m_moveWhiteCache[x][y];
            }
        }
        else
            return new Move(point, color);
    }

    private Color parseColor(String s) throws SgfError
    {
        Color c;
        s = s.trim().toLowerCase();
        if (s.equals("b") || s.equals("1"))
            c = Color.BLACK;
        else if (s.equals("w") || s.equals("2"))
            c = Color.WHITE;
        else
            throw getError("Invalid color value");
        return c;
    }

    private double parseDouble(String s) throws SgfError
    {
        double f = 0;
        try
        {
            f = Double.parseDouble(s);
        }
        catch (NumberFormatException e)
        {
            throw getError("Floating point number expected");
        }
        return f;
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

    private Point parsePoint(String s) throws SgfError
    {
        s = s.trim().toLowerCase();
        if (s.equals(""))
            return null;
        if (s.length() < 2)
            throw getError("Invalid coordinates: " + s);
        int boardSize = m_gameInformation.m_boardSize;
        if (s.equals("tt") && boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
        {
            if (x == boardSize && y == -1)
            {
                m_warningWrongPass = true;
                return null;
            }
            throw getError("Invalid coordinates: " + s);
        }
        if (x < CACHE_SIZE && y < CACHE_SIZE)
        {
            if (m_pointCache[x][y] == null)
                m_pointCache[x][y] = new Point(x, y);
            return m_pointCache[x][y];
        }
        else
            return new Point(x, y);
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
        if (ttype != ';')
            throw getError("Next node expected");
        Node son = new Node();
        father.append(son);
        while (readProp(son, isRoot));
        return son;
    }
    
    private boolean readProp(Node node, boolean isRoot)
        throws IOException, SgfError, SgfCharsetChanged
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == StreamTokenizer.TT_WORD)
        {
            String p = m_tokenizer.sval.toUpperCase();
            Vector values = new Vector();
            String s;
            while ((s = readValue()) != null)
                values.add(s);
            if (values.size() == 0)
                throw getError("Property '" + p + "' has no value");
            String v = (String)values.get(0);
            if (p.equals("AB") || p.equals("ADDBLACK"))
            {
                if (p.equals("ADDBLACK"))
                    m_warningLongProps = true;
                for (int i = 0; i < values.size(); ++i)
                    node.addBlack(parsePoint((String)values.get(i)));
                m_sizeFixed = true;
            }
            else if (p.equals("AE") || p.equals("ADDEMPTY"))
            {
                if (p.equals("ADDEMPTY"))
                    m_warningLongProps = true;
                throw getError("Add empty not supported");
            }
            else if (p.equals("AW") || p.equals("ADDWHITE"))
            {
                if (p.equals("ADDWHITE"))
                    m_warningLongProps = true;
                for (int i = 0; i < values.size(); ++i)
                    node.addWhite(parsePoint((String)values.get(i)));
                m_sizeFixed = true;
            }
            else if (p.equals("B") || p.equals("BLACK"))
            {
                if (p.equals("BLACK"))
                    m_warningLongProps = true;
                node.setMove(getMove(parsePoint(v), Color.BLACK));
                m_sizeFixed = true;
            }
            else if (p.equals("BL"))
            {
                try
                {
                    node.setTimeLeftBlack(Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("BR"))
                m_gameInformation.m_blackRank = v;
            else if (p.equals("C") || p.equals("COMMENT"))
            {
                if (p.equals("COMMENT"))
                    m_warningLongProps = true;
                String comment;
                if (node.getComment() != null)
                    comment = node.getComment() + "\n" + v.trim();
                else
                    comment = v.trim();
                node.setComment(comment);
            }
            else if (p.equals("CA"))
            {
                if (isRoot && m_isFile && m_newCharset == null)
                {
                    m_newCharset = v.trim();
                    if (Charset.isSupported(m_newCharset))
                        throw new SgfCharsetChanged();
                }
            }
            else if (p.equals("DT") || p.equals("DATE"))
            {
                if (p.equals("DATE"))
                    m_warningLongProps = true;
                m_gameInformation.m_date = v;
            }
            else if (p.equals("FF"))
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
                {
                    m_warningFormat = true;
                }
            }
            else if (p.equals("GM") || p.equals("GAME"))
            {
                if (p.equals("GAME"))
                    m_warningLongProps = true;
                v = v.trim();
                if (v.equals(""))
                    m_warningGame = true;
                else if (! v.equals("1"))
                    throw getError("Not a Go game");
                
            }
            else if (p.equals("HA") || p.equals("HANDICAP"))
            {
                if (p.equals("HANDICAP"))
                    m_warningLongProps = true;
                try
                {
                    m_gameInformation.m_handicap = Integer.parseInt(v);
                }
                catch (NumberFormatException e)
                {
                    m_warningInvalidHandicap = true;
                }
            }
            else if (p.equals("KM") || p.equals("KOMI"))
            {
                if (p.equals("KOMI"))
                    m_warningLongProps = true;
                try
                {
                    m_gameInformation.m_komi = Double.parseDouble(v);
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("OB"))
            {
                try
                {
                    node.setMovesLeftBlack(Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("OW"))
            {
                try
                {
                    node.setMovesLeftWhite(Integer.parseInt(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("PB") || p.equals("PLAYERBLACK"))
            {
                if (p.equals("PLAYERBLACK"))
                    m_warningLongProps = true;
                m_gameInformation.m_playerBlack = v;
            }
            else if (p.equals("PW") || p.equals("PLAYERWHITE"))
            {
                if (p.equals("PLAYERWHITE"))
                    m_warningLongProps = true;
                m_gameInformation.m_playerWhite = v;
            }
            else if (p.equals("PL") || p.equals("PLAYER"))
            {
                if (p.equals("PLAYER"))
                    m_warningLongProps = true;
                node.setPlayer(parseColor(v));
            }
            else if (p.equals("RE") || p.equals("RESULT"))
            {
                if (p.equals("RESULT"))
                    m_warningLongProps = true;
                m_gameInformation.m_result = v;
            }
            else if (p.equals("RU") || p.equals("RULES"))
            {
                if (p.equals("RULES"))
                    m_warningLongProps = true;
                m_gameInformation.m_rules = v;
            }
            else if (p.equals("SZ") || p.equals("SIZE"))
            {
                if (p.equals("SIZE"))
                    m_warningLongProps = true;
                if (! isRoot)
                {
                    if (m_sizeFixed)
                        throw getError("Size property outside root node");
                    m_warningSizeOutsideRoot = true;
                }
                try
                {
                    m_gameInformation.m_boardSize = parseInt(v);
                }
                catch (NumberFormatException e)
                {
                    m_warningInvalidBoardSize = true;
                }
                m_sizeFixed = true;
            }
            else if (p.equals("W") || p.equals("WHITE"))
            {
                if (p.equals("WHITE"))
                    m_warningLongProps = true;
                node.setMove(getMove(parsePoint(v), Color.WHITE));
                m_sizeFixed = true;
            }
            else if (p.equals("WL"))
            {
                try
                {
                    node.setTimeLeftWhite(Double.parseDouble(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("WR"))
                m_gameInformation.m_whiteRank = v;
            else if (! p.equals("FF") && ! p.equals("GN") && ! p.equals("AP"))
                node.addSgfProperty(p, v);
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
            m_gameInformation = new GameInformation(19);
            m_sizeFixed = false;
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
                reader = new InputStreamReader(in);
            }
            m_reader = new BufferedReader(reader);
            m_tokenizer = new StreamTokenizer(m_reader);
            findRoot();
            Node root = new Node();
            Node node = readNext(root, true);
            while (node != null)
                node = readNext(node, false);
            if (root.getNumberChildren() == 1)
            {
                root = root.getChild();
                root.setFather(null);
            }
            m_gameTree = new GameTree(m_gameInformation, root);
            applyFixes();
        }
        catch (FileNotFoundException e)
        {
            throw new SgfError("File not found");
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
        StringBuffer value = new StringBuffer(32);
        boolean quoted = false;
        while (true)
        {
            int c = m_reader.read();
            if (c < 0)
                throw getError("Property value incomplete");
            if (! quoted && c == ']')
                break;
            quoted = (c == '\\');
            if (! quoted)
                value.append((char)c);
        }
        return value.toString();
    }
}

//----------------------------------------------------------------------------
