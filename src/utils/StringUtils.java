//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.util.*;

//-----------------------------------------------------------------------------

public class StringUtils
{
    /** Format a string for a dialog message.
        Trims the string, changes first character to uppercase
        and appends '.' if necessary.
    */
    public static String formatMessage(String message)
    {
        message = message.trim();
        if (message.equals(""))
            return message;
        StringBuffer buffer = new StringBuffer(message);
        char last = buffer.charAt(buffer.length() - 1);
        if (Character.isLetterOrDigit(last))
            buffer.append('.');
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

    public static String replace(String s, String oldStr, String newStr)
    {
        StringBuffer buffer = new StringBuffer(s);
        replace(buffer, oldStr, newStr);
        return new String(buffer);
    }

    public static void replace(StringBuffer buffer, String oldStr,
                               String newStr)
    {
        int newLen = newStr.length();
        int oldLen = oldStr.length();
        int idx = 0;
        while (true)
        {
            int i = buffer.toString().indexOf(oldStr, idx);
            if (i < 0)
                break;
            buffer.replace(i, i + oldLen, newStr);
            idx += newLen;
        }
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
