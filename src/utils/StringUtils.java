//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.text.*;
import java.util.*;

//-----------------------------------------------------------------------------

public class StringUtils
{
    /** Format a string for a dialog message.
        Trims the string and changes first character to uppercase.
    */
    public static String formatMessage(String message)
    {
        message = message.trim();
        if (message.equals(""))
            return message;
        StringBuffer buffer = new StringBuffer(message);
        char first = buffer.charAt(0);
        if (! Character.isUpperCase(first))
            buffer.setCharAt(0, Character.toUpperCase(first));
        return buffer.toString();
    }

    /** Format a string for a window title.
        Trims the string and changes first character to uppercase.
    */
    public static String formatTitle(String title)
    {
        title = title.trim();
        if (title.equals(""))
            return title;
        char c = title.charAt(0);
        if (Character.isUpperCase(c))
            return title;
        StringBuffer buffer = new StringBuffer(title);
        buffer.setCharAt(0, Character.toUpperCase(c));
        return buffer.toString();
    }    

    public static NumberFormat getNumberFormat(int maximumFractionDigits)
    {
        NumberFormat format = NumberFormat.getInstance(new Locale("C"));
        format.setMaximumFractionDigits(maximumFractionDigits);
        format.setGroupingUsed(false);
        return format;
    }

    /** Split string into words.
        Allows " for words containing whitespaces.
    */
    public static String[] tokenize(String string)
    {
        Vector vector = new Vector();
        boolean escape = false;
        boolean inString = false;
        StringBuffer token = new StringBuffer();
        for (int i = 0; i < string.length(); ++i)
        {
            char c = string.charAt(i);
            if (c == '"' && ! escape)
            {
                if (inString)
                {
                    vector.add(token.toString());
                    token = new StringBuffer();
                }
                inString = ! inString;
            }
            else if (Character.isWhitespace(c) && ! inString)
            {
                if (token.length() > 0)
                {
                    vector.add(token.toString());
                    token = new StringBuffer();
                }
            }
            else
                token.append(c);
            escape = (c == '\\' && ! escape);
        }
        if (token.length() > 0)
            vector.add(token.toString());
        int size = vector.size();
        String result[] = new String[size];
        for (int i = 0; i < size; ++i)
            result[i] = (String)vector.get(i);
        return result;
    }

    public static String[] split(String s, char separator)
    {
        int count = 1;
        int pos = -1;
        while ((pos = s.indexOf(separator, pos + 1)) >= 0)
            ++count;
        String result[] = new String[count];
        pos = 0;
        int newPos;
        int i = 0;
        while ((newPos = s.indexOf(separator, pos)) >= 0)
        {
            result[i] = s.substring(pos, newPos);
            ++i;
            pos = newPos + 1;
        }
        result[i] = s.substring(pos);
        return result;
    }
}

//-----------------------------------------------------------------------------
