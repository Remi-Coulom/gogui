//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class Options
{
    public Options(String[] args, String[] specs) throws Exception
    {
        for (int i = 0; i < specs.length; ++i)
        {
            String spec = specs[i];
            if (spec.length() > 0)
                m_map.put(spec, null);
        }
        parseArgs(args);
    }

    public boolean contains(String option)
    {
        return getValue(option) != null;
    }

    public Vector getArguments()
    {
        return m_args;
    }

    public float getFloat(String option) throws Exception
    {
        return getFloat(option, 0);
    }

    public float getFloat(String option, float defaultValue) throws Exception
    {
        String value = getString(option, Float.toString(defaultValue));
        if (value == null)
            return defaultValue;
        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e)
        {
            throw new Exception("Option -" + option + " needs float value.");
        }
    }

    public int getInteger(String option) throws Exception
    {
        return getInteger(option, 0);
    }

    public int getInteger(String option, int defaultValue) throws Exception
    {
        String value = getString(option, Integer.toString(defaultValue));
        if (value == null)
            return defaultValue;
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new Exception("Option -" + option + " needs integer value.");
        }
    }

    public int getInteger(String option, int defaultValue, int min)
        throws Exception
    {
        int value = getInteger(option, defaultValue);
        if (value < min)
            throw new Exception("Option -" + option + " must be greater than "
                                + min);
        return value;
    }

    public int getInteger(String option, int defaultValue, int min, int max)
        throws Exception
    {
        int value = getInteger(option, defaultValue);
        if (value < min || value > max)
            throw new Exception("Option -" + option + " must be in [" +
                                min + ".." + max + "]");
        return value;
    }

    public String getString(String option) throws Exception
    {
        return getString(option, "");
    }

    public String getString(String option, String defaultValue)
    {
        assert(isValidOption(option));
        String value = getValue(option);
        if (value == null)
            return defaultValue;
        return value;
    }

    public boolean isSet(String option) throws Exception
    {
        String value = getString(option, null);
        return (value != null);
    }

    /** Read options from a file given with the option "config".
        Requires that "config" is an allowed option.
    */
    public void handleConfigOption() throws Exception
    {
        if (! isSet("config"))
            return;
        String filename = getString("config");
        InputStream inputStream = new FileInputStream(filename);
        Reader reader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(reader);
        try
        {
            StringBuffer buffer = new StringBuffer(256);
            String line;
            while (true)
            {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                buffer.append(line);
                buffer.append(' ');
            }
            parseArgs(StringUtils.tokenize(buffer.toString()));
        }
        finally
        {
            bufferedReader.close();
        }
    }

    private Vector m_args = new Vector();

    private Map m_map = new TreeMap();

    private String getSpec(String option) throws Exception
    {
        if (m_map.containsKey(option))
            return option;
        else if (m_map.containsKey(option + ":"))
            return option + ":";
        throw new Exception("Unknown option -" + option);
    }

    private String getValue(String option)
    {
        assert(isValidOption(option));
        if (m_map.containsKey(option))
            return (String)m_map.get(option);
        return (String)m_map.get(option + ":");
    }

    private boolean isOptionKey(String s)
    {
        return (s.length() > 0 && s.charAt(0) == '-');
    }

    private boolean isValidOption(String option)
    {
        return (m_map.containsKey(option) || m_map.containsKey(option + ":"));
    }

    private boolean needsValue(String spec)
    {
        return (spec.length() > 0
                && spec.substring(spec.length() - 1).equals(":"));
    }

    private void parseArgs(String args[]) throws Exception
    {
        int n = 0;
        while (n < args.length)
        {
            String s = args[n];
            ++n;
            if (isOptionKey(s))
            {
                String spec = getSpec(s.substring(1));
                if (needsValue(spec))
                {
                    if (n >= args.length)
                        throw new Exception("Option " + s + " needs value.");
                    String value = args[n];
                    if (isOptionKey(value))
                        throw new Exception("Option " + s + " needs value.");
                    ++n;
                    m_map.put(spec, value);
                }
                else
                    m_map.put(spec, "1");
                
            }
            else
                m_args.add(s);
        }
    }

    private void putBoolOption(String spec)
    {
        m_map.put(spec, "1");
    }
}

//-----------------------------------------------------------------------------
