//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

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
        int n = 0;
        while (n < args.length)
        {
            String s = args[n];
            ++n;
            if (s.length() > 0 && s.charAt(0) == '-')
            {
                String spec = getSpec(s.substring(1));
                if (spec.length() > 0
                    && spec.substring(spec.length() - 1).equals(":"))
                {
                    if (n >= args.length)
                        throw new Exception("Option " + s + " needs value.");
                    String value = args[n];
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
    
    public boolean contains(String option)
    {
        return getValue(option) != null;
    }

    public Vector getArguments()
    {
        return m_args;
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

    private Vector m_args = new Vector();
    private Map m_map = new TreeMap();

    private String getSpec(String option) throws Exception
    {
        if (m_map.containsKey(option))
            return option;
        else if (m_map.containsKey(option + ":"))
            return option + ":";
        throw new Exception("Unknown option " + option);
    }

    private String getValue(String option)
    {
        assert(isValidOption(option));
        if (m_map.containsKey(option))
            return (String)m_map.get(option);
        return (String)m_map.get(option + ":");
    }

    private boolean isValidOption(String option)
    {
        return (m_map.containsKey(option) || m_map.containsKey(option + ":"));
    }
}

//-----------------------------------------------------------------------------
