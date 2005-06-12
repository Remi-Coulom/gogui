//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Analyze command.
    See GoGui documentation, chapter "Analyze Commands"
*/
public class AnalyzeCommand
{
    public static final int BWBOARD = 0;

    public static final int CBOARD = 1;

    public static final int DBOARD = 2;

    public static final int EPLIST = 3;

    public static final int HSTRING = 4;

    public static final int HPSTRING = 5;

    public static final int NONE = 6;

    public static final int PARAM = 7;

    public static final int PLIST = 8;

    public static final int PSTRING = 9;

    public static final int PSPAIRS = 10;

    public static final int STRING = 11;

    public static final int SBOARD = 12;

    public static final int VAR = 13;

    public static final int VARB = 14;

    public static final int VARC = 15;

    public static final int VARP = 16;

    public static final int VARPO = 17;

    public static final int VARW = 18;

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
        if (array.length > 3)
            m_title = array[3];
        else
            m_title = null;
        if (array.length > 4)
            m_scale = Double.parseDouble(array[4]);
        else
            m_scale = 1.0;
    }

    public AnalyzeCommand(int type, String label, String command,
                          String title, double scale)
    {
        m_type = type;
        m_label = label;
        m_command = command;
        m_title = title;
        m_scale = scale;
    }

    public static AnalyzeCommand get(Frame owner, String label)
    {
        Vector commands = new Vector(128, 128);
        Vector labels = new Vector(128, 128);
        try
        {
            read(commands, labels, null);
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

    public Vector getPointListArg()
    {
        return m_pointListArg;
    }

    public double getScale()
    {
        return m_scale;
    }

    public String getTitle()
    {
        return m_title;
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
        return needsColorArg(m_command);
    }

    public static boolean needsColorArg(String command)
    {
        return (command.indexOf("%c") >= 0);
    }

    public boolean needsFileArg()
    {
        return (m_command.indexOf("%f") >= 0);
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

    public static void read(Vector commands, Vector labels,
                            Vector supportedCommands)
        throws Exception
    {
        commands.clear();
        labels.clear();
        Vector files = getFiles();
        File file = new File(getDir(), "analyze-commands");
        if (! files.contains(file))
        {
            copyDefaults(file);
            files = getFiles();
        }
        for (int i = 0; i < files.size(); ++i)
            readFile((File)files.get(i), commands, labels, supportedCommands);
    }

    public String replaceWildCards(GoColor toMove, GoColor color)
    {
        if (needsColorArg())
            setColorArg(color);
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
            result = result.replaceAll("%c", m_colorArg.toString());
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

    public void setPointListArg(Vector pointList)
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

    private final double m_scale;

    private GoColor m_colorArg;

    private File m_fileArg;

    private final String m_label;

    private String m_optStringArg;

    private final String m_command;

    private final String m_title;

    private String m_stringArg;

    private GoPoint m_pointArg;

    private Vector m_pointListArg = new Vector();

    private static void copyDefaults(File file)
    {
        String resource = "config/analyze-commands";
        URL url = ClassLoader.getSystemClassLoader().getResource(resource);
        if (url == null)
            return;
        try
        {
            InputStream in = url.openStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) >= 0)
                out.write(buffer, 0, n);
            in.close();
            out.close();
        }
        catch (IOException e)
        {
        }
    }

    private static File getDir()
    {
        String home = System.getProperty("user.home");
        return new File(home, ".gogui");
    }

    private static Vector getFiles()
    {
        Vector result = new Vector();
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

    private static void readFile(File file, Vector commands, Vector labels,
                                 Vector supportedCommands)
        throws Exception
    {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        int lineNumber = 0;
        while ((line = in.readLine()) != null)
        {
            ++lineNumber;
            line = line.trim();
            if (line.length() > 0 && line.charAt(0) != '#')
            {
                String array[] = line.split("/");
                if (array.length < 3 || array.length > 5)
                    throw new Exception("Error in " + file + " line "
                                        + lineNumber);
                if (supportedCommands != null)
                {
                    String[] cmdArray
                        = StringUtils.tokenize(array[2].trim());
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
        in.close();
    }
}

//----------------------------------------------------------------------------
