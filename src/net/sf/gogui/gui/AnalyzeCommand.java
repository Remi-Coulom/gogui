//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.StringUtil;

/** Analyze command.
    See GoGui documentation, chapter "Analyze Commands"
*/
public class AnalyzeCommand
{
    public static final int BWBOARD = 0;

    public static final int CBOARD = 1;

    public static final int DBOARD = 2;

    public static final int EPLIST = 3;

    public static final int GFX = 4;

    public static final int HSTRING = 5;

    public static final int HPSTRING = 6;

    public static final int NONE = 7;

    public static final int PARAM = 8;

    public static final int PLIST = 9;

    public static final int PSTRING = 10;

    public static final int PSPAIRS = 11;

    public static final int STRING = 12;

    public static final int SBOARD = 13;

    public static final int VAR = 14;

    public static final int VARB = 15;

    public static final int VARC = 16;

    public static final int VARP = 17;

    public static final int VARPO = 18;

    public static final int VARW = 19;

    public AnalyzeCommand(String line)
    {
        String array[] = line.split("/");
        String typeStr = array[0];        
        if (typeStr.equals("bwboard"))
            m_type = AnalyzeCommand.BWBOARD;
        else if (typeStr.equals("cboard"))
            m_type = AnalyzeCommand.CBOARD;
        else if (typeStr.equals("dboard"))
            m_type = AnalyzeCommand.DBOARD;
        else if (typeStr.equals("eplist"))
            m_type = AnalyzeCommand.EPLIST;
        else if (typeStr.equals("gfx"))
            m_type = AnalyzeCommand.GFX;
        else if (typeStr.equals("hstring"))
            m_type = AnalyzeCommand.HSTRING;
        else if (typeStr.equals("hpstring"))
            m_type = AnalyzeCommand.HPSTRING;
        else if (typeStr.equals("param"))
            m_type = AnalyzeCommand.PARAM;
        else if (typeStr.equals("plist"))
            m_type = AnalyzeCommand.PLIST;
        else if (typeStr.equals("pspairs"))
            m_type = AnalyzeCommand.PSPAIRS;
        else if (typeStr.equals("pstring"))
            m_type = AnalyzeCommand.PSTRING;
        else if (typeStr.equals("string"))
            m_type = AnalyzeCommand.STRING;
        else if (typeStr.equals("sboard"))
            m_type = AnalyzeCommand.SBOARD;
        else if (typeStr.equals("var"))
            m_type = AnalyzeCommand.VAR;
        else if (typeStr.equals("varb"))
            m_type = AnalyzeCommand.VARB;
        else if (typeStr.equals("varc"))
            m_type = AnalyzeCommand.VARC;
        else if (typeStr.equals("varp"))
            m_type = AnalyzeCommand.VARP;
        else if (typeStr.equals("varpo"))
            m_type = AnalyzeCommand.VARPO;
        else if (typeStr.equals("varw"))
            m_type = AnalyzeCommand.VARW;
        else
            m_type = AnalyzeCommand.NONE;
        m_label = array[1];
        m_command = array[2];
    }

    public AnalyzeCommand(int type, String label, String command)
    {
        m_type = type;
        m_label = label;
        m_command = command;
    }

    public AnalyzeCommand cloneCommand()
    {
        AnalyzeCommand command =
            new AnalyzeCommand(m_type, m_label, m_command);
        command.m_colorArg = m_colorArg;
        command.m_fileArg = m_fileArg;
        command.m_optStringArg = m_optStringArg;
        command.m_stringArg = m_stringArg;
        command.m_pointArg = m_pointArg;
        command.m_pointListArg = m_pointListArg;
        return command;
    }

    public static AnalyzeCommand get(Frame owner, String label)
    {
        ArrayList commands = new ArrayList(128);
        ArrayList labels = new ArrayList(128);
        try
        {
            read(commands, labels, null, null);
        }
        catch (Exception e)
        {            
            SimpleDialogs.showError(owner, e.getMessage());
        }
        int index = labels.indexOf(label);
        if (index < 0)
            return null;
        return new AnalyzeCommand((String)commands.get(index));
    }

    public String getLabel()
    {
        return m_label;
    }

    public GoColor getColorArg()
    {
        return m_colorArg;
    }

    public GoPoint getPointArg()
    {
        return m_pointArg;
    }

    public ArrayList getPointListArg()
    {
        return m_pointListArg;
    }

    public int getType()
    {
        return m_type;
    }

    public String getResultTitle()
    {
        StringBuffer buffer = new StringBuffer(m_label);
        if (needsColorArg() && m_colorArg != null)
        {
            if (m_colorArg == GoColor.BLACK)
                buffer.append(" Black");
            else
            {
                assert(m_colorArg == GoColor.WHITE);
                buffer.append(" White");
            }
        }
        if (needsPointArg() && m_pointArg != null)
        {
            buffer.append(' ');
            buffer.append(m_pointArg.toString());
        }
        else if (needsPointListArg())
        {
            for (int i = 0; i < m_pointListArg.size(); ++i)
            {
                buffer.append(' ');
                buffer.append(((GoPoint)(m_pointListArg.get(i))).toString());
            }
        }
        if (needsStringArg() && m_stringArg != null)
        {
            buffer.append(' ');
            buffer.append(m_stringArg);
        }
        return buffer.toString();
    }

    public boolean isPointArgMissing()
    {
        if (needsPointArg())
            return (m_pointArg == null);
        if (needsPointListArg())
            return m_pointListArg.isEmpty();
        return false;
    }

    /** Should the response be shown as text.
        Returns true for types that should be shown (not necessarily only)
        as text to the user.
        That is string and variation commands.
    */
    public static boolean isTextType(int type)
    {
        return type == STRING
            || type == HSTRING
            || type == HPSTRING
            || type == PSTRING
            || type == VAR
            || type == VARC
            || type == VARW
            || type == VARB
            || type == VARP
            || type == VARPO;
    }

    public boolean needsColorArg()
    {
        return (m_command.indexOf("%c") >= 0);
    }

    public boolean needsFileArg()
    {
        return (m_command.indexOf("%f") >= 0);
    }

    public boolean needsOnlyPointArg()
    {
        return (needsPointArg()
                && ! needsColorArg()
                && ! needsFileArg()
                && ! needsPointListArg()
                && ! needsStringArg()
                && ! needsOptStringArg());
    }

    public boolean needsOnlyPointAndColorArg()
    {
        return (needsPointArg() && needsColorArg()
                && ! needsFileArg()
                && ! needsPointListArg()
                && ! needsStringArg()
                && ! needsOptStringArg());
    }

    public boolean needsPointArg()
    {
        return (m_command.indexOf("%p") >= 0);
    }

    public boolean needsPointListArg()
    {
        return (m_command.indexOf("%P") >= 0 || m_type == EPLIST);
    }

    public boolean needsStringArg()
    {
        return (m_command.indexOf("%s") >= 0);
    }

    public boolean needsOptStringArg()
    {
        return (m_command.indexOf("%o") >= 0);
    }

    public static void read(ArrayList commands, ArrayList labels,
                            ArrayList supportedCommands,
                            String programAnalyzeCommands)
        throws ErrorMessage
    {
        commands.clear();
        labels.clear();
        if (programAnalyzeCommands != null)
        {
            Reader stringReader = new StringReader(programAnalyzeCommands);
            BufferedReader reader = new BufferedReader(stringReader);
            readConfig(reader, "program response to gogui_analyze_commands",
                       commands, labels, supportedCommands);
            return;
        }
        String resource = "net/sf/gogui/config/analyze-commands";
        URL url = ClassLoader.getSystemClassLoader().getResource(resource);
        if (url == null)
            return;
        try
        {
            InputStream inputStream = url.openStream();
            Reader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            readConfig(reader, "builtin default commands", commands, labels,
                       supportedCommands);
        }
        catch (IOException e)
        {
            throw new ErrorMessage(e.getMessage());
        }
        ArrayList files = getFiles();
        for (int i = 0; i < files.size(); ++i)
        {
            File file = (File)files.get(i);
            try
            {
                Reader fileReader = new FileReader(file);
                BufferedReader reader = new BufferedReader(fileReader);
                readConfig(reader, file.getName(), commands, labels,
                           supportedCommands);
            }
            catch (FileNotFoundException e)
            {
                throw new ErrorMessage("File " + file + " not found");
            }
        }
    }

    public String replaceWildCards(GoColor toMove)
    {
        String result = m_command.replaceAll("%m", toMove.toString());
        if (needsPointArg() && m_pointArg != null)
            result = result.replaceAll("%p", m_pointArg.toString());
        if (needsPointListArg())
        {
            String pointList = GoPoint.toString(m_pointListArg);
            if (m_type == EPLIST && m_pointListArg.size() > 0)
                result = result + ' ' + pointList;
            else
                result = result.replaceAll("%P", pointList);
        }
        if (needsFileArg())
        {
            String fileArg = m_fileArg.toString();
            if (fileArg.indexOf(' ') >= 0)
                fileArg = "\"" + fileArg + "\"";
            result = result.replaceAll("%f", fileArg);
        }
        if (needsStringArg())
        {
            assert(m_stringArg != null);
            result = result.replaceAll("%s", m_stringArg);
        }
        if (needsOptStringArg())
        {
            assert(m_optStringArg != null);
            result = result.replaceAll("%o", m_optStringArg);
        }
        if (needsColorArg())
        {
            String colorString = "empty";
            if (m_colorArg == GoColor.BLACK)
                colorString = "b";
            else if (m_colorArg == GoColor.WHITE)
                colorString = "w";
            result = result.replaceAll("%c", colorString);
        }
        return result;
    }

    public void setColorArg(GoColor color)
    {
        assert(needsColorArg());
        m_colorArg = color;
    }

    public void setFileArg(File file)
    {
        assert(needsFileArg());
        m_fileArg = file;
    }

    public void setPointArg(GoPoint point)
    {
        m_pointArg = point;
    }

    public void setPointListArg(ArrayList pointList)
    {
        m_pointListArg = pointList;
    }

    public void setStringArg(String value)
    {
        assert(needsStringArg());
        m_stringArg = value;
    }

    public void setOptStringArg(String value)
    {
        assert(needsOptStringArg());
        m_optStringArg = value;
    }

    private final int m_type;

    private GoColor m_colorArg;

    private File m_fileArg;

    private final String m_label;

    private String m_optStringArg;

    private final String m_command;

    private String m_stringArg;

    private GoPoint m_pointArg;

    private ArrayList m_pointListArg = new ArrayList();

    private static File getDir()
    {
        String home = System.getProperty("user.home");
        return new File(home, ".gogui");
    }

    private static ArrayList getFiles()
    {
        ArrayList result = new ArrayList();
        File[] files = getDir().listFiles();
        if (files == null)
            return result;
        String s = new File(getDir(), "analyze-commands").toString();
        for (int i = 0; i < files.length; ++i)
        {
            File f = files[i];
            if (f.toString().startsWith(s) && ! f.toString().endsWith("~"))
                result.add(f);
        }
        return result;
    }

    private static void readConfig(BufferedReader reader, String name,
                                   ArrayList commands, ArrayList labels,
                                   ArrayList supportedCommands)
        throws ErrorMessage
    {
        try
        {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null)
            {
                ++lineNumber;
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#')
                {
                    String array[] = line.split("/");
                    if (array.length < 3 || array.length > 5)
                        throw new ErrorMessage("Error in " + name + " line "
                                               + lineNumber);
                    if (supportedCommands != null)
                    {
                        String[] cmdArray
                            = StringUtil.splitArguments(array[2].trim());
                        if (cmdArray.length == 0
                            || ! supportedCommands.contains(cmdArray[0]))
                            continue;
                    }
                    String label = array[1];
                    if (labels.contains(label))
                        continue;
                    labels.add(label);
                    commands.add(line);
                }                
            }
        }
        catch (IOException e)
        {
            throw new ErrorMessage("Error reading " + name);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                throw new ErrorMessage("Error reading " + name);
            }
        }
    }
}

