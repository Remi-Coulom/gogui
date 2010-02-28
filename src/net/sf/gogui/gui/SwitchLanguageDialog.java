// SwitchLanguageDialog.java

package net.sf.gogui.gui;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import static net.sf.gogui.gui.I18n.i18n;

/** Show a dialog to switch the application language. */
public class SwitchLanguageDialog
{
    /** Constructor.
        To show the dialog, language options have to be added with #add(),
        the #run() needs to be called.
        @param owner The parent window of the dialog.
        @param messageDialogs For displaying messages.
        @param prefs The preferences
        @param key The preferences key for reading and storing the
        language setting */
    public SwitchLanguageDialog(Frame owner, MessageDialogs messageDialogs,
                                Preferences prefs, String key)
    {
        m_entries = new ArrayList<Entry>();
        m_owner = owner;
        m_messageDialogs = messageDialogs;
        m_prefs = prefs;
        m_key = key;
    }

    public void addLanguage(Locale locale, boolean isFullyLocalized)
    {
        Entry entry = new Entry();
        entry.m_locale = locale;
        entry.m_isFullyLocalized = isFullyLocalized;
        m_entries.add(entry);
    }

    public void run()
    {
        Object[] options = m_entries.toArray();
        if (options.length == 0)
            return;
        String current = m_prefs.get(m_key, Locale.getDefault().getLanguage());
        Object initialSelectionValue = options[0];
        for (int i = 0; i < options.length; ++i)
            if (((Entry)options[i]).m_locale.getLanguage().equals(current))
                initialSelectionValue = options[i];
        Object result =
            JOptionPane.showInputDialog(m_owner,
                                        i18n("MSG_SWITCHLANG_INPUT"),
                                        i18n("TIT_SWITCHLANG_INPUT"),
                                        JOptionPane.PLAIN_MESSAGE,
                                        null, options, initialSelectionValue);
        if (result == null)
            return;
        Entry entry = (Entry)result;
        m_prefs.put(m_key, entry.m_locale.getLanguage());
        if (entry.m_locale.getLanguage().equals(current))
            return;
        if (! entry.m_isFullyLocalized)
            m_messageDialogs.showInfo(m_owner,
                                      i18n("MSG_SWITCHLANG_NOT_FULL_SUPPORT"),
                                      i18n("MSG_SWITCHLANG_NOT_FULL_SUPPORT_2"),
                                      false);
        m_messageDialogs.showInfo(m_owner,
                                  i18n("MSG_SWITCHLANG_NEXT_START"),
                                  i18n("MSG_SWITCHLANG_NEXT_START_2"),
                                  false);

    }

    private static class Entry
    {
        public Locale m_locale;

        public boolean m_isFullyLocalized;

        public String toString()
        {
            return m_locale.getDisplayName();
        }
    }

    private final Frame m_owner;

    private final MessageDialogs m_messageDialogs;

    private final Preferences m_prefs;

    private final String m_key;

    private final ArrayList<Entry> m_entries;
}
