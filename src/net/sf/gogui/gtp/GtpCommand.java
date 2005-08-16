//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import java.util.ArrayList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** GTP command. */
public class GtpCommand
{
    public GtpCommand(String line)
    {
        assert(! line.trim().equals(""));
        int len = line.length();
        StringBuffer buffer = new StringBuffer(len);
        boolean wasLastSpace = false;
        for (int i = 0; i < len; ++i)
        {
            char c = line.charAt(i);
            if (Character.isISOControl(c))
                continue;
            if (Character.isWhitespace(c))
            {
                if (! wasLastSpace)
                {
                    buffer.append(' ');
                    wasLastSpace = true;
                }
            }
            else
            {
                buffer.append(c);
                wasLastSpace = false;
            }
        }
        String[] array = StringUtils.splitArguments(buffer.toString());
        assert(array.length > 0);
        int commandIndex = 0;
        try
        {
            m_id = Integer.parseInt(array[0]);
            m_hasId = true;
            m_line = buffer.substring(array[0].length()).trim();
            commandIndex = 1;
        }
        catch (NumberFormatException e)
        {
            m_hasId = false;
            m_id = -1;
            m_line = buffer.toString();
        }
        if (commandIndex >= array.length)
            return;
        m_command = array[commandIndex];
        int nuArg = array.length - commandIndex - 1;
        m_arg = new String[nuArg];
        for (int i = 0; i < nuArg; ++i)
            m_arg[i] = array[commandIndex + i + 1];
        m_response = new StringBuffer();
    }

    public void checkArgNone() throws GtpError
    {
        checkNuArg(0);
    }

    public void checkNuArg(int n) throws GtpError
    {
        if (getNuArg() != n)
        {
            if (n == 0)
                throw new GtpError("no arguments allowed");
            if (n == 1)
                throw new GtpError("need argument");
            throw new GtpError("need " + n + " arguments");
        }
    }

    public void checkNuArgLessEqual(int n) throws GtpError
    {
        if (getNuArg() > n)
            throw new GtpError("too many arguments");
    }

    public boolean hasId()
    {
        return m_hasId; 
    }

    public String getArg(int i) throws GtpError
    {
        if (i >= getNuArg())
            throw new GtpError("missing argument " + (i + 1));
        return m_arg[i];
    }

    public String getArgLine()
    {
        int pos = m_line.indexOf(m_command) + m_command.length();
        return m_line.substring(pos).trim();
    }

    public String getArgToLower(int i) throws GtpError
    {
        return getArg(i).toLowerCase();
    }

    public GoColor getColorArg() throws GtpError
    {
        checkNuArg(1);
        return getColorArg(0);
    }

    public GoColor getColorArg(int i) throws GtpError
    {
        String arg = getArgToLower(i);
        if (arg.equals("b") || arg.equals("black"))
            return GoColor.BLACK;
        if (arg.equals("w") || arg.equals("white"))
            return GoColor.WHITE;
        throw new GtpError("argument " + (i + 1) + " must be black or white");
    }

    public String getCommand()
    {
        return m_command;
    }

    public double getDoubleArg() throws GtpError
    {
        checkNuArg(1);
        return getDoubleArg(0);
    }

    public double getDoubleArg(int i) throws GtpError
    {
        String arg = getArg(i);
        try
        {
            return Double.parseDouble(arg);
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("argument " + (i + 1) + " must be float");
        }
    }

    public int getIntArg() throws GtpError
    {
        checkNuArg(1);
        return getIntArg(0);
    }

    public int getIntArg(int i) throws GtpError
    {
        String arg = getArg(i);
        try
        {
            return Integer.parseInt(arg);
        }
        catch (NumberFormatException e)
        {
            throw new GtpError("argument " + (i + 1) + " must be integer");
        }
    }

    public int getIntArg(int i, int min, int max) throws GtpError
    {
        int n = getIntArg(i);
        if (n < min)
            throw new GtpError("argument " + (i + 1)
                               + " must be greater/equal " + min);
        if (n > max)
            throw new GtpError("argument " + (i + 1)
                               + " must be less/equal " + max);
        return n;
    }

    public GoPoint getPointArg(int i, int boardSize) throws GtpError
    {
        return GtpUtils.parsePoint(getArg(i), boardSize);
    }

    public ArrayList getPointListArg(int boardSize) throws GtpError
    {
        ArrayList pointList = new ArrayList();
        for (int i = 0; i < getNuArg(); ++i)
            pointList.add(getPointArg(i, boardSize));
        return pointList;
    }

    /** Full command line without ID. */
    public String getLine()
    {
        return m_line;
    }

    public int getNuArg()
    {
        return m_arg.length;
    }

    public StringBuffer getResponse()
    {
        return m_response;
    }

    public int getId()
    {
        return m_id;
    }

    public boolean isQuit()
    {
        return m_line.trim().toLowerCase().equals("quit");
    }

    public void setResponse(String response)
    {
        m_response.setLength(0);
        m_response.append(response);
    }

    private boolean m_hasId;
    
    private int m_id;
    
    private String m_line;

    private String m_command;

    private String[] m_arg;

    private StringBuffer m_response;
}

//----------------------------------------------------------------------------
