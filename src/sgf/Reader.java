//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package sgf;

//-----------------------------------------------------------------------------

import java.io.*;
import java.util.*;
import game.*;
import go.*;

//-----------------------------------------------------------------------------

public class Reader
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    /** @param name Name prepended to error messages */
    public Reader(java.io.Reader reader, String name)
        throws Error
    {
        try
        {
            m_in = new BufferedReader(reader);
            m_tokenizer = new StreamTokenizer(m_in);
            m_name = name;
            m_tokenizer.nextToken();
            if (m_tokenizer.ttype != '(')
                throw getError("No root tree found.");
            Node root = new Node();
            Node node = readNext(root, true);
            while (node != null)
                node = readNext(node, false);
            if (root.getNumberChildren() == 1)
                root = root.getChild();
            m_gameTree = new GameTree(m_gameInformation, root);
        }
        catch (FileNotFoundException e)
        {
            throw new Error("File not found.");
        }
        catch (IOException e)
        {
            throw new Error("Error while reading file.");
        }
    }

    public GameTree getGameTree()
    {
        return m_gameTree;
    }

    private java.io.Reader m_in;

    private GameInformation m_gameInformation = new GameInformation();

    private GameTree m_gameTree;

    private StreamTokenizer m_tokenizer;

    private String m_name;

    private Error getError(String message)
    {
        // Note: lineno() does not work correctly for Unix line endings
        // (Sun Java 1.4.0 Linux)
        int lineNumber = m_tokenizer.lineno() + 1;
        if (m_name != null)
        {
            String s = m_name + ":" + lineNumber + ": " + message;
            return new Error(s);
        }
        else
            return new Error(lineNumber + ": " + message);
    }

    private Color parseColor(String s) throws Error
    {
        Color c;
        s = s.trim().toLowerCase();
        if (s.equals("b") || s.equals("1"))
            c = Color.BLACK;
        else if (s.equals("w") || s.equals("2"))
            c = Color.WHITE;
        else
            throw getError("Invalid color value.");
        return c;
    }

    private float parseFloat(String s) throws Error
    {
        float f = 0;
        try
        {
            f = Float.parseFloat(s);
        }
        catch (NumberFormatException e)
        {
            throw getError("Floating point number expected.");
        }
        return f;
    }

    private int parseInt(String s) throws Error
    {
        int i = -1;
        try
        {
            i = Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw getError("Number expected.");
        }
        return i;
    }

    private Point parsePoint(String s) throws Error
    {
        s = s.trim().toLowerCase();
        if (s.equals(""))
            return null;
        if (s.length() < 2)
            throw getError("Invalid coordinates.");
        int boardSize = m_gameInformation.m_boardSize;
        if (s.equals("tt") && boardSize <= 19)
            return null;
        int x = s.charAt(0) - 'a';
        int y = boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize)
            throw getError("Invalid coordinates.");
        return new Point(x, y);
    }

    private Node readNext(Node father, boolean isRoot)
        throws IOException, Error
    {
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
        throws IOException, Error
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
                throw getError("Property '" + p + "' has no value.");
            String v = (String)values.get(0);
            if (p.equals("AB"))
            {
                for (int i = 0; i < values.size(); ++i)
                    node.addBlack(parsePoint((String)values.get(i)));
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
                    node.setTimeLeftBlack(Float.parseFloat(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            else if (p.equals("C"))
            {
                node.setComment(v);
            }
            else if (p.equals("GM"))
            {
                v = v.trim();
                // Value should be 1, but sgf2misc produces Go files
                // with empty value
                if (! v.equals("1") && ! v.equals(""))
                    throw getError("Not a Go game.");
            }
            else if (p.equals("KM"))
            {
                m_gameInformation.m_komi = parseFloat(v);
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
            else if (p.equals("PL"))
            {
                node.setToMove(parseColor(v));
            }
            else if (p.equals("SZ"))
            {
                if (! isRoot)
                    throw getError("Size property outside root node.");
                m_gameInformation.m_boardSize = parseInt(v);
            }
            else if (p.equals("W"))
            {
                node.setMove(new Move(parsePoint(v), Color.WHITE));
            }
            else if (p.equals("WL"))
            {
                try
                {
                    node.setTimeLeftWhite(Float.parseFloat(v));
                }
                catch (NumberFormatException e)
                {
                }
            }
            return true;
        }
        m_tokenizer.pushBack();
        return false;
    }

    private String readValue() throws IOException, Error
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype != '[')
        {
            m_tokenizer.pushBack();
            return null;
        }
        String v = "";
        boolean quoted = false;
        while (true)
        {
            int c = m_in.read();
            if (c < 0)
                throw getError("Property value incomplete.");
            if (! quoted && c == ']')
                break;
            quoted = (c == '\\');
            if (! quoted)
                v += (char)c;
        }
        return v;
    }
}

//-----------------------------------------------------------------------------
