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

    private static class SgfCharsetChanged
        extends Exception
    {
    }

    private boolean m_isFile;

    private int m_lastPercent;

    private long m_size;

    private ByteCountInputStream m_byteCountInputStream;

    private java.io.Reader m_reader;

    private GameInformation m_gameInformation;

    private GameTree m_gameTree;

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

    private SgfError getError(String message)
    {
        int lineNumber = m_tokenizer.lineno() + 1;
        if (m_name != null)
        {
            String s = m_name + ":" + lineNumber + ": " + message;
            return new SgfError(s);
        }
        else
            return new SgfError(lineNumber + ": " + message);
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
            throw getError("Invalid coordinates: " + s);
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
            if (p.equals("AB"))
            {
                for (int i = 0; i < values.size(); ++i)
                    node.addBlack(parsePoint((String)values.get(i)));
            }
            else if (p.equals("AE"))
            {
                throw getError("Add empty not supported");
            }
            else if (p.equals("AW"))
            {
                for (int i = 0; i < values.size(); ++i)
                    node.addWhite(parsePoint((String)values.get(i)));
            }
            else if (p.equals("B"))
            {
                node.setMove(new Move(parsePoint(v), Color.BLACK));
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
            else if (p.equals("C"))
            {
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
            else if (p.equals("DT"))
                m_gameInformation.m_date = v;
            else if (p.equals("GM"))
            {
                v = v.trim();
                // Value should be 1, but sgf2misc produces Go files
                // with empty value
                if (! v.equals("1") && ! v.equals(""))
                    throw getError("Not a Go game");
            }
            else if (p.equals("HA"))
                m_gameInformation.m_handicap = Integer.parseInt(v);
            else if (p.equals("KM"))
                m_gameInformation.m_komi = parseDouble(v);
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
            else if (p.equals("PB"))
                m_gameInformation.m_playerBlack = v;
            else if (p.equals("PW"))
                m_gameInformation.m_playerWhite = v;
            else if (p.equals("PL"))
                node.setPlayer(parseColor(v));
            else if (p.equals("RE"))
                m_gameInformation.m_result = v;
            else if (p.equals("RU"))
                m_gameInformation.m_rules = v;
            else if (p.equals("SZ"))
            {
                if (! isRoot)
                    throw getError("Size property outside root node");
                m_gameInformation.m_boardSize = parseInt(v);
            }
            else if (p.equals("W"))
                node.setMove(new Move(parsePoint(v), Color.WHITE));
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
        m_tokenizer.pushBack();
        return false;
    }

    private void readSgf(InputStream in, String charset)
        throws SgfError, SgfCharsetChanged
    {
        try
        {
            m_gameInformation = new GameInformation(19);
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
            m_tokenizer.nextToken();
            if (m_tokenizer.ttype != '(')
                throw getError("No root tree found");
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
