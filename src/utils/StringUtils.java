//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

//-----------------------------------------------------------------------------

public class StringUtils
{
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
