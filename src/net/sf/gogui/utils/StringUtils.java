//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

//----------------------------------------------------------------------------

/** Static utility functions related to strings. */
public class StringUtils
{
    /** Capitalize the first word and trim whitespaces. */
    public static String capitalize(String message)
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

    public static String getDate()
    {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                           DateFormat.FULL);
        Date date = Calendar.getInstance().getTime();
        return format.format(date);
    }

    public static String getDateShort()
    {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                           DateFormat.SHORT);
        Date date = Calendar.getInstance().getTime();
        return format.format(date);
    }

    /** Get default encoding of OutputStreamWriter. */
    public static String getDefaultEncoding()
    {
        // Haven't found another way than constructing one (Java 1.4)
        OutputStreamWriter out =
            new OutputStreamWriter(new ByteArrayOutputStream());
        return out.getEncoding();
    }

    public static NumberFormat getNumberFormat(int maximumFractionDigits)
    {
        NumberFormat format = NumberFormat.getInstance(new Locale("C"));
        format.setMaximumFractionDigits(maximumFractionDigits);
        format.setGroupingUsed(false);
        return format;
    }

    /** Print exception to standard error.
        Prints the class name and message to standard error.
        For exceptions of type Error or RuntimeException, a stack trace
        is printed in addition.
        @return A slightly differently formatted error message
        for display in an error dialog.
    */
    public static String printException(Throwable exception)
    {
        String message = exception.getMessage();
        boolean hasMessage = (message != null && ! message.trim().equals(""));
        String className = exception.getClass().getName();
        boolean isSevere = (exception instanceof RuntimeException
                            || exception instanceof Error);     
        String result;
        if (exception instanceof ErrorMessage)
            result = message;
        else if (hasMessage)
            result = className + ":\n" + message;
        else
            result = className;
        System.err.println(result);
        if (isSevere)
            exception.printStackTrace();
        return result; 
    }

    /** Split string into tokens. */
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

    /** Split string into words.
        Allows " for words containing whitespaces.
    */
    public static String[] tokenize(String string)
    {
        assert(string != null);
        ArrayList vector = new ArrayList();
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

    /** Make constructor unavailable; class is for namespace only. */
    private StringUtils()
    {
    }
}

//----------------------------------------------------------------------------
