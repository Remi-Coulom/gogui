//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class Preferences
{
    public Preferences()
    {
        load();
    }

    public boolean contains(String key)
    {
        return m_properties.getProperty(key) != null;
    }

    public boolean getBool(String key)
    {
        return (getInt(key) != 0);
    }

    public int getInt(String key)
    {
        try
        {
            return Integer.parseInt(getString(key));
        }
        catch (NumberFormatException e)
        {
            throwFormatError(key);
            return 0;
        }
    }

    public float getFloat(String key)
    {
        try
        {
            return Float.parseFloat(getString(key));
        }
        catch (NumberFormatException e)
        {
            throwFormatError(key);
            return 0f;
        }
    }

    public String getString(String key)
    {
        return m_properties.getProperty(key);
    }

    public void load()
    {
        m_changed = true;
        File file = getFilename();
        if (! file.exists())
            return;
        try
        {
            InputStream in = new FileInputStream(file);
            m_properties.load(in);
        }
        catch (FileNotFoundException e)
        {
            return;
        }
        catch (IOException e)
        {
            return;
        }
        m_changed = false;
    }

    public void save()
    {
        if (! m_changed)
            return;
        File file = getFilename();
        try
        {
            FileOutputStream out = new FileOutputStream(file);
            m_properties.store(out, "GoGui preferences");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not save " + file.toString());
        }
        catch (IOException e)
        {
            System.err.println("Could not save " + file.toString());
        }
        m_changed = false;
    }

    public void setBool(String key, boolean value)
    {
        setString(key, value ? "1" : "0");
    }

    public void setBoolDefault(String key, boolean value)
    {
        setStringDefault(key, value ? "1" : "0");
    }

    public void setFloat(String key, float value)
    {
        setString(key, Float.toString(value));
    }

    public void setFloatDefault(String key, float value)
    {
        setStringDefault(key, Float.toString(value));
    }

    public void setInt(String key, int value)
    {
        setString(key, Integer.toString(value));
    }

    public void setIntDefault(String key, int value)
    {
        setStringDefault(key, Integer.toString(value));
    }

    public void setString(String key, String value)
    {
        if (contains(key))
            if (getString(key).equals(value))
                return;
        m_properties.setProperty(key, value);
        m_changed = true;
    }

    public void setStringDefault(String key, String value)
    {
        if (contains(key))
            return;
        m_properties.setProperty(key, value);
        m_changed = true;
    }

    /** Properties changed since last load? */
    private boolean m_changed;

    private Properties m_properties = new Properties();

    private File getFilename()
    {
        String home = System.getProperty("user.home");
        File dir = new File(home, ".gogui");
        if (! dir.exists())
            dir.mkdir();
        return new File(dir, "config");        
    }

    private void throwFormatError(String key)
    {
        throw new Error("Invalid value in " + getFilename() + " for " + key);
    }
}

//-----------------------------------------------------------------------------
