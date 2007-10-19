//----------------------------------------------------------------------------
// SgfUtil.java
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility functions used in this package. */
final class SgfUtil
{
    /** Parse value of TM property.
        According to FF4, TM needs to be a real value, but older SGF versions
        allow a string with unspecified content. We try to parse a few known
        formats.
        @return The (pre-byoyomi-)time in milliseconds or -1, if the
        format was not recognized
    */
    public static long parseTime(String value)
    {
        value = value.trim();
        try
        {
            return (long)(Double.parseDouble(value) * 1000);
        }
        catch (NumberFormatException e1)
        {
        }
        try
        {
            Pattern pattern;
            Matcher matcher;

            // Pattern as written by CGoban 1.9.12
            pattern = Pattern.compile("(\\d{1,2}):(\\d{2})");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 2;
                return (Integer.parseInt(matcher.group(1)) * 60000L
                        + Integer.parseInt(matcher.group(2)) * 1000L);
            }

            pattern = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 3;
                return (Integer.parseInt(matcher.group(1)) * 3600000L
                        + Integer.parseInt(matcher.group(2)) * 60000L
                        + Integer.parseInt(matcher.group(3)) * 1000L);
            }

            // Formats found in some games of
            // http://www.cs.ualberta.ca/~mmueller/go/honinbo.html
            pattern = Pattern.compile("(\\d+)\\s*(?:h|hours|hours\\s+each)");
            matcher = pattern.matcher(value);
            if (matcher.matches())
            {
                assert matcher.groupCount() == 1;
                return Integer.parseInt(matcher.group(1)) * 3600000L;
            }
        }
        catch (NumberFormatException e2)
        {
            assert false; // patterns should match only valid integers
        }
        return -1;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private SgfUtil()
    {
    }
}
