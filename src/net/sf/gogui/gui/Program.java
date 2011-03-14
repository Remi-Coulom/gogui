// Program.java

package net.sf.gogui.gui;

import java.util.ArrayList;
import java.util.prefs.Preferences;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.util.PrefUtil;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.StringUtil;

/** Command line and other information to run a GTP engine. */
public final class Program
{
    public String m_label;

    public String m_name;

    public String m_version;

    public String m_command;

    public String m_workingDirectory;

    public Program(Program program)
    {
        copyFrom(program);
    }

    public Program(String label, String name, String version, String command,
                   String workingDirectory)
    {
        init(label, name, version, command, workingDirectory);
    }

    public void copyFrom(Program program)
    {
        init(program.m_label, program.m_name, program.m_version,
             program.m_command, program.m_workingDirectory);
    }

    /** Find program info for a given command in a list of program infos.
        @return The program info or null if no program with this comamnd
        exists */
    public static Program findProgram(ArrayList<Program> programs,
                                      String command)
    {
        for (Program p : programs)
            if (p.m_command.equals(command))
                return p;
        return null;
    }

    public static ArrayList<Program> load()
    {
        ArrayList<Program> programs = new ArrayList<Program>();
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
            String workingDirectory = prefs.get("working-directory", "");
            programs.add(new Program(label, name, version, command,
                                     workingDirectory));
        }
        return programs;
    }

    public static void save(ArrayList<Program> programs)
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
            Program p = programs.get(i);
            prefs.put("label", p.m_label);
            prefs.put("name", p.m_name);
            prefs.put("version", p.m_version);
            prefs.put("command", p.m_command);
            prefs.put("working-directory", p.m_workingDirectory);
        }
    }

    /** Suggest and set a label derived from program name without
        collision with an existing array of programs. */
    public void setUniqueLabel(ArrayList<Program> programs)
    {
        String label = m_name;
        if (StringUtil.isEmpty(label))
            label = i18n("LB_UNKNOWN_PROGRAM");
        String tryLabel = label;
        boolean alreadyExists = false;
        for (int i = 0; i < programs.size(); ++i)
            if (tryLabel.equals((programs.get(i)).m_label))
            {
                alreadyExists = true;
                break;
            }
        if (! alreadyExists)
        {
            m_label = tryLabel;
            return;
        }
        for (int i = 2; ; ++i)
        {
            tryLabel = label + " (" + i + ")";
            alreadyExists = false;
            for (int j = 0; j < programs.size(); ++j)
                if (tryLabel.equals((programs.get(j)).m_label))
                {
                    alreadyExists = true;
                    break;
                }
            if (! alreadyExists)
            {
                m_label = tryLabel;
                return;
            }
        }
    }

    /** Update program information if changed.
        Useful, if a program was replaced, and reports a different name or
        version than at the last invocation.
        @param name Program name at current invovation (may be null)
        @param version Program name at current invovation (may be null)
        @return true, if name or version program was updated */
    public boolean updateInfo(String name, String version)
    {
        if (name == null)
            name = "";
        if (version == null)
            version = "";
        boolean changed = false;
        if (! ObjectUtil.equals(m_name, name))
        {
            m_name = name;
            changed = true;
        }
        if (! ObjectUtil.equals(m_version, version))
        {
            m_version = version;
            changed = true;
        }
        return changed;
    }

    private void init(String label, String name, String version,
                      String command, String workingDirectory)
    {
        m_label = label.trim();
        m_name = name.trim();
        m_version = version.trim();
        m_command = command.trim();
        m_workingDirectory = workingDirectory.trim();
    }
}
