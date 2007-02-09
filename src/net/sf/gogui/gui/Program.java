//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.util.ArrayList;
import java.util.prefs.Preferences;
import net.sf.gogui.util.PrefUtil;

/** Command line and other information to run a GTP engine. */
public final class Program
{
    public Program(Program program)
    {
        copyFrom(program);
    }

    public Program(String label, String name, String version, String command)
    {
        init(label, name, version, command);
    }

    public void copyFrom(Program program)
    {
        init(program.m_label, program.m_name, program.m_version,
             program.m_command);
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
            String label = prefs.get("label", null);
            if (label == null)
                break;
            String name = prefs.get("name", "");
            String version = prefs.get("version", "");
            String command = prefs.get("command", "");
            programs.add(new Program(label, name, version, command));
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
            prefs.put("label", p.m_label);
            prefs.put("name", p.m_name);
            prefs.put("version", p.m_version);
            prefs.put("command", p.m_command);
        }
    }

    public String m_label;

    public String m_name;

    public String m_version;

    public String m_command;

    private void init(String label, String name, String version,
                      String command)
    {
        m_label = label.trim();
        m_name = name.trim();
        m_version = version.trim();
        m_command = command.trim();
    }
}

