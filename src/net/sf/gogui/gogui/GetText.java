// GetText.java

package net.sf.gogui.gogui;

import java.util.Locale;
import java.util.ResourceBundle;

class GetText
{
    public static String get(String key)
    {
        return m_bundle.getString(key);
    }

    private static ResourceBundle m_bundle =
        ResourceBundle.getBundle("net.sf.gogui.gogui.text",
                                 Locale.getDefault());

    /** Make constructor unavailable; class is for namespace only. */
    private GetText()
    {
    }
}

