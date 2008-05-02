// I18n.java

package net.sf.gogui.game;

import java.util.Locale;
import java.util.ResourceBundle;

/** Internationalization for this package. */
final class I18n
{
    public static String i18n(String key)
    {
        return s_bundle.getString(key);
    }

    private static ResourceBundle s_bundle =
        ResourceBundle.getBundle("net.sf.gogui.game.text",
                                 Locale.getDefault());

    /** Make constructor unavailable; class is for namespace only. */
    private I18n()
    {
    }
}
