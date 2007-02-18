//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

/** Static utility functions related to strings. */
public final class StringUtil
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

    /** Format elapsed time as [[h+]:[mm]]:ss */
    public static String formatTime(long seconds)
    {
        StringBuffer buffer = new StringBuffer(8);
        if (seconds < 0)
        {
            buffer.append('-');
            seconds *= -1;
        }
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        if (hours > 0)
        {
            if (hours > 9999)
                // Extremely large numbers are like a problem in
                // Date.getTime(), as it can happen when running in the
                // netbeans profiler, and we don't want extremly long time
                // strings to change the layout of the time label
                buffer.append(">9999");
            else
                buffer.append(hours);
            buffer.append(':');
        }
        if (minutes >= 10)
            buffer.append(minutes);
        else
        {
            buffer.append('0');
            buffer.append(minutes);
        }
        buffer.append(':');
        if (seconds >= 10)
            buffer.append(seconds);
        else
        {
            buffer.append('0');
            buffer.append(seconds);
        }
        return buffer.toString();
    }

    public static String getDate()
    {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                           DateFormat.LONG);
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

    /** Return a printable error message for an exception.
        Returns the error message is for instances of ErrorMessage or
        for other exceptions the class name with the exception message
        appended, if not empty.
    */    
    public static String getErrorMessage(Throwable e)
    {
        String message = e.getMessage();
        boolean hasMessage = ! StringUtil.isEmpty(message);
        String className = e.getClass().getName();
        String result;
        if (e instanceof ErrorMessage)
            result = message;
        else if (hasMessage)
            result = className + ":\n" + message;
        else
            result = className;
        return result;
    }

    /** Return a number formatter with maximum fraction digits,
        no grouping, locale ENGLISH.
    */
    public static NumberFormat getNumberFormat(int maximumFractionDigits)
    {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(maximumFractionDigits);
        format.setGroupingUsed(false);
        return format;
    }

    /** Check if string is null, empty, or contains only whitespaces. */
    public static boolean isEmpty(String s)
    {
        if (s == null)
            return true;
        return (s.trim().length() == 0);
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
        String result = getErrorMessage(exception);
        System.err.println(result);
        boolean isSevere = (exception instanceof RuntimeException
                            || exception instanceof Error);     
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

    /** Split command line into arguments.
        Allows " for words containing whitespaces.
    */
    public static String[] splitArguments(String string)
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
    private StringUtil()
    {
    }
}

