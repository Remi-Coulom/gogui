//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import utils.*;

//-----------------------------------------------------------------------------

class AnalyzeCommand
    extends JComboBox
    implements PopupMenuListener
{
    public interface Callback
    {
        public void clearAnalyzeCommand();

        public void initAnalyzeCommand(int type, String label, String command,
                                       String title, double scale);

        public void setAnalyzeCommand(int type, String label, String command,
                                      String title, double scale);
    }

    public static class Error extends Exception
    {
        public Error(String s)
        {
            super(s);
        }
    }    

    public static final int NONE        = 0;
    public static final int STRING      = 1;    
    public static final int DOUBLEBOARD = 2;
    public static final int POINTLIST   = 3;
    public static final int STRINGBOARD = 4;
    public static final int COLORBOARD  = 5;

    AnalyzeCommand(Callback callback, Preferences prefs) throws Error
    {
        setEnabled(false);
        m_callback = callback;
        m_prefs = prefs;
        m_commands = new Vector(32, 32);
        read();
        int numberCommands = m_commands.size();
        addItem("[No command]");
        for (int i = 0; i < numberCommands; ++i)
            addItem(StringUtils.split(getCommand(i), '/')[1]);
        setSelectedIndex(0);
        addPopupMenuListener(this);
        String startCommand = prefs.getAnalyzeCommand();
        if (startCommand != null && ! startCommand.equals(""))
        {
            for (int i = 0; i < getItemCount(); ++i)
                if (getItemAt(i).toString().equals(startCommand))
                {
                    setSelectedIndex(i);
                    if (prefs.getAnalyzeCommandEnabled())
                        setEnabled(true);
                    setCommand(true);
                    return;
                }
            throw new Error("Unknown analyze command: " + startCommand);
        }
    }

    public void popupMenuCanceled(PopupMenuEvent e)
    {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
        setCommand(false);
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
    }

    public void setAnalyzeCommand()
    {
        if (isEnabled())
            setCommand(false);
        else
        {
            m_prefs.setAnalyzeCommandEnabled(false);
            m_callback.clearAnalyzeCommand();
        }
    }

    private Callback m_callback;

    private Preferences m_prefs;

    private Vector m_commands;

    private String getCommand(int index)
    {
        return (String)m_commands.get(index);
    }

    private static Vector getDefaults()
    {
        String s[] = {
            "plist/All Legal/all_legal %m",
            "string/Dragon Data/dragon_data %p",
            "string/Estimate Score/estimate_score",
            "string/Final Score/final_score",
            "sboard/Final Status/final_status %p",
            "dboard/Influence Black/influence %m/black/0.01",
            "dboard/Influence White/influence %m/white/-0.01",
            "sboard/Influence Regions/influence %m/regions",
            "none/ShowBoard/showboard",
            "string/Worm Data/worm_data %p"
        };
        Vector result = new Vector(32, 32);
        for (int i = 0; i < s.length; ++i)
            result.add(s[i]);
        return result;
    }

    private File getDir()
    {
        String home = System.getProperty("user.home");
        return new File(home, ".gogui");
    }

    private File getFile()
    {
        return new File(getDir(), "analyze-commands");
    }

    private void read() throws Error
    {
        m_commands = getDefaults();
        try
        {
            File file = getFile();
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = in.readLine();
            int number = 1;
            while (line != null)
            {
                if (line.length() > 0 && line.charAt(0) != '#')
                {
                    if (StringUtils.split(line, '/').length < 3)
                        throw new Error("Invalid line " + number + " in\n"
                                        + file);
                    if (! m_commands.contains(line))
                        m_commands.add(line);
                }                
                line = in.readLine();
                ++number;
            }
        }
        catch (IOException e)
        {
            save();
        }
    }

    private void save() throws Error
    {
        File dir = getDir();
        if (! dir.exists())
            dir.mkdir();
        File file = getFile();
        try
        {
            PrintWriter out = new PrintWriter(new FileOutputStream(file));
            out.println("# GoGui Analyze Commands");
            for (int i = 0; i < m_commands.size(); ++i)
                out.println(getCommand(i));
            out.close();
        }
        catch (FileNotFoundException e)
        {
            throw new Error("File " + file + " not found.");
        }
    }

    private void setCommand(boolean init)
    {
        m_prefs.setAnalyzeCommandEnabled(true);
        int index = getSelectedIndex();
        if (index == 0)
        {
            m_callback.clearAnalyzeCommand();
            return;
        }
        String analyzeCommand = getCommand(index - 1);
        double scale = 1.0;
        String title = null;
        int type = NONE;
        StringBuffer buffer = new StringBuffer(analyzeCommand);
        String array[] = StringUtils.split(buffer.toString(), '/');
        String typeStr = array[0];
        if (typeStr.equals("cboard"))
            type = AnalyzeCommand.COLORBOARD;
        else if (typeStr.equals("dboard"))
            type = AnalyzeCommand.DOUBLEBOARD;
        else if (typeStr.equals("sboard"))
            type = AnalyzeCommand.STRINGBOARD;
        else if (typeStr.equals("plist"))
            type = AnalyzeCommand.POINTLIST;
        else if (typeStr.equals("string"))
            type = AnalyzeCommand.STRING;
        String label = array[1];
        String command = array[2];
        if (array.length > 3)
            title = array[3];
        if (array.length > 4)
            scale = Double.parseDouble(array[4]);
        if (init && m_prefs.getAnalyzeCommandEnabled())
            m_callback.initAnalyzeCommand(type, label, command, title, scale);
        else
            m_callback.setAnalyzeCommand(type, label, command, title, scale);
        m_prefs.setAnalyzeCommand(label);
    }
}

//-----------------------------------------------------------------------------
