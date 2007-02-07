//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.PrefUtil;

/** Command line and other information to run a GTP engine. */
public final class Program
{
    public Program(Program program)
    {
        copyFrom(program);
    }

    public Program(String name, String command)
    {
        init(name, command);
    }

    public void copyFrom(Program program)
    {
        init(program.m_name, program.m_command);
    }

    public static ArrayList load()
    {
        ArrayList programs = new ArrayList();
        Preferences prefs = PrefUtil.getNode("net/sf/gogui/gui/program");
        if (prefs == null)
            return programs;
        int size = prefs.getInt("size", 0);
        for (int i = 0; i < size; ++i)
        {
            prefs = PrefUtil.getNode("net/sf/gogui/gui/program/" + i);
            if (prefs == null)
                break;
            String name = prefs.get("name", null);
            if (name == null)
                break;
            String command = prefs.get("command", "");
            programs.add(new Program(name, command));
        }
        return programs;
    }

    public static void save(ArrayList programs)
    {
        Preferences prefs = PrefUtil.createNode("net/sf/gogui/gui/program");
        if (prefs == null)
            return;
        prefs.putInt("size", programs.size());
        for (int i = 0; i < programs.size(); ++i)
        {
            prefs = PrefUtil.createNode("net/sf/gogui/gui/program/" + i);
            if (prefs == null)
                break;
            Program p = (Program)programs.get(i);
            prefs.put("name", p.m_name);
            prefs.put("command", p.m_command);
        }
    }

    public String m_name;

    public String m_command;

    private void init(String name, String command)
    {
        m_name = name.trim();
        m_command = command.trim();
    }
}

