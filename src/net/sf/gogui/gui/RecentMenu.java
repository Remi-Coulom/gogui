//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

//----------------------------------------------------------------------------

class RecentMenuItem
    extends JMenuItem
{
    public RecentMenuItem(String label, String value,
                          ActionListener listener)
    {
        super(label);
        m_label = label;
        m_value = value;
        addActionListener(listener);
    }

    public String getRecentMenuLabel()
    {
        return m_label;
    }

    public String getRecentMenuValue()
    {
        return m_value;
    }

    public void setRecentMenuLabel(String label)
    {
        setText(label);
        m_label = label;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private String m_label;

    private final String m_value;
}

//----------------------------------------------------------------------------

/** Menu for recent item.
    Handles removing duplicates and storing the items between sessions.
*/
public final class RecentMenu
{
    public interface Callback
    {
        void itemSelected(String label, String value);
    }

    public RecentMenu(String label, File file, Callback callback)
    {
        assert(callback != null);
        assert(file != null);
        m_file = file;
        m_callback = callback;
        m_menu = new JMenu(label);
        m_listener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    RecentMenuItem item = (RecentMenuItem)event.getSource();
                    String label = item.getRecentMenuLabel();
                    String value = item.getRecentMenuValue();
                    m_callback.itemSelected(label, value);
                }
            };
        load();
    }

    public void add(String label, String value, boolean save)
    {
        for (int i = 0; i < getCount(); ++i)
            if (getValue(i).equals(value))
                m_menu.remove(i);
        JMenuItem item = new RecentMenuItem(label, value, m_listener);
        m_menu.add(item, 0);
        while (getCount() > m_maxItems)
            m_menu.remove(getCount() - 1);
        if (save)
            save();
    }

    public int getCount()
    {
        return m_menu.getItemCount();
    }

    /** Don't modify the items in this menu! */
    public JMenu getMenu()
    {
        return m_menu;
    }

    public String getValue(int i)
    {
        return getItem(i).getRecentMenuValue();
    }

    public void save()
    {
        Properties props = new Properties();
        int count = getCount();
        for (int i = 0; i < count; ++i)
        {
            props.setProperty("label_" + (count - i - 1), getLabel(i));
            props.setProperty("value_" + (count - i - 1), getValue(i));
        }
        try
        {
            FileOutputStream out = new FileOutputStream(m_file);
            props.store(out, null);
            out.close();
        }
        catch (IOException e)
        {
        }
    }

    public void setLabel(int i, String label)
    {
        getItem(i).setRecentMenuLabel(label);
    }

    private final int m_maxItems = 20;

    private final ActionListener m_listener;

    private final Callback m_callback;

    private final File m_file;

    private final JMenu m_menu;

    private RecentMenuItem getItem(int i)
    {
        return (RecentMenuItem)m_menu.getItem(i);
    }

    private String getLabel(int i)
    {
        return getItem(i).getRecentMenuLabel();
    }

    private void load()
    {
        Properties props = new Properties();
        try
        {
            props.load(new FileInputStream(m_file));
        }
        catch (IOException e)
        {
            return;
        }
        m_menu.removeAll();
        for (int i = 0; i < m_maxItems; ++i)
        {
            String label = props.getProperty("label_" + i);
            String value = props.getProperty("value_" + i);
            if (label == null || value == null)
                continue;
            add(label, value, false);
        }
    }
}

//----------------------------------------------------------------------------
