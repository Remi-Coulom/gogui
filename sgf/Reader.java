//=============================================================================
// $Id$
// $Source$
//=============================================================================

package sgf;

//=============================================================================

import java.io.*;
import java.util.*;
import board.*;

//=============================================================================

public class Reader
{
    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public Reader(File file)
        throws Error
    {
        try
        {
            m_in = new BufferedReader(new FileReader(file));
            m_tokenizer = new StreamTokenizer(m_in);
            m_file = file;
            m_boardSize = 19;
            m_moves.clear();
            m_tokenizer.nextToken();
            if (m_tokenizer.ttype != '(')
                loadError("No root tree found.");
            readNextNode(true);
            while (readNextNode(false));
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

    public int getBoardSize()
    {
        return m_boardSize;
    }

    public float getKomi()
    {
        return m_komi;
    }

    public Vector getMoves()
    {
        return m_moves;
    }

    public Vector getSetupBlack()
    {
        return m_setupBlack;
    }

    public Vector getSetupWhite()
    {
        return m_setupWhite;
    }

    private int m_boardSize;
    private float m_komi;
    private File m_file;
    private java.io.Reader m_in;
    private StreamTokenizer m_tokenizer;
    private Vector m_moves = new Vector(361, 361);
    private Vector m_setupBlack = new Vector(128, 64);
    private Vector m_setupWhite = new Vector(128, 64);

    private void discardSubtree() throws IOException, Error
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == '(')
            discardSubtree();
        else if (ttype == ')')
            return;
        else if (ttype != ';')
            loadError("Next node expected");
        else
            while (readNextProp(false));
    }
    
    private void loadError(String message) throws Error
    {
        // Note: lineno() does not work correctly for Unix line endings
        // (Sun Java 1.4.0 Linux)
        String s = m_file + ":" + m_tokenizer.lineno() + ":\n" + message;
        throw new Error(s);
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
            loadError("Floating point number expected.");
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
            loadError("Number expected.");
        }
        return i;
    }

    private Point parsePoint(String s) throws Error
    {
        s = s.trim().toLowerCase();
        if (s.equals("tt"))
            return null;
        int x = s.charAt(0) - 'a';
        int y = m_boardSize - (s.charAt(1) - 'a') - 1;
        if (x < 0 || x >= m_boardSize || y < 0 || y >= m_boardSize)
            loadError("Invalid coordinates.");
        return new Point(x, y);
    }

    private boolean readNextNode(boolean isRoot) throws IOException, Error
    {
        m_tokenizer.nextToken();
        int ttype = m_tokenizer.ttype;
        if (ttype == '(')
            discardSubtree();
        else if (ttype == ')')
            return false;
        else if (ttype != ';')
            loadError("Next node expected");
        while (readNextProp(isRoot));
        return true;
    }
    
    private boolean readNextProp(boolean isRoot) throws IOException, Error
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
                loadError("Property '" + p + "' has no value.");
            String v = (String)values.get(0);
            if (p.equals("AB"))
            {
                if (m_moves.size() > 0)
                    loadError("Setup stones after moves not supported.");
                for (int i = 0; i < values.size(); ++i)
                    m_setupBlack.add(parsePoint((String)values.get(i)));
            }
            else if (p.equals("AW"))
            {
                if (m_moves.size() > 0)
                    loadError("Setup stones after moves not supported.");
                for (int i = 0; i < values.size(); ++i)
                    m_setupWhite.add(parsePoint((String)values.get(i)));
            }
            else if (p.equals("B"))
            {
                m_moves.add(new Move(parsePoint(v), Color.BLACK));
            }
            else if (p.equals("GM"))
            {
                if (! v.trim().equals("1"))
                    loadError("Not a Go game.");
            }
            else if (p.equals("KM"))
            {
                m_komi = parseFloat(v);
            }
            else if (p.equals("SZ"))
            {
                if (! isRoot)
                    loadError("Size property outside root node.");
                m_boardSize = parseInt(v);
            }
            else if (p.equals("W"))
            {
                m_moves.add(new Move(parsePoint(v), Color.WHITE));
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
                loadError("Property value incomplete.");
            if (! quoted && c == ']')
                break;
            v += (char)c;
            quoted = (c == '\\');
        }
        return v;
    }
}

//=============================================================================
