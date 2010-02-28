// StringUtil.java

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
        StringBuilder buffer = new StringBuilder(message);
        char first = buffer.charAt(0);
        if (! Character.isUpperCase(first))
            buffer.setCharAt(0, Character.toUpperCase(first));
        return buffer.toString();
    }

    /** Format elapsed time as [[h+]:[mm]]:ss. */
    public static String formatTime(long seconds)
    {
        StringBuilder buffer = new StringBuilder(8);
        if (seconds < 0)
        {
            buffer.append('-');
            seconds *= -1;
        }
        long hours = seconds / 3600;
        if (hours > 0)
        {
            if (hours > 9999)
                // Extremely large numbers are likely a problem in
                // Date.getTime(), as it can happen when running in the
                // netbeans profiler (version 5.5)
                return "--:--";
            buffer.append(hours);
            buffer.append(':');
        }
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
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

    /** Return the current time and date as a string using a long format.
        The time and date is formatted using DateFormat.LONG and
        Locale.ENGLISH. */
    public static String getDate()
    {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                           DateFormat.LONG,
                                                           Locale.ENGLISH);
        Date date = Calendar.getInstance().getTime();
        return format.format(date);
    }

    /** Return the current time and date as a string using a short format.
        The time and date is formatted using DateFormat.SHORT and
        Locale.ENGLISH. */
    public static String getDateShort()
    {
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                           DateFormat.SHORT,
                                                           Locale.ENGLISH);
        Date date = Calendar.getInstance().getTime();
        return format.format(date);
    }

    /** Get default encoding. */
    public static String getDefaultEncoding()
    {
        String encoding = System.getProperty("file.encoding");
        // Java 1.5 docs for System.getProperties do not guarantee the
        // existance of file.encoding
        if (encoding != null)
            return encoding;
        OutputStreamWriter out =
            new OutputStreamWriter(new ByteArrayOutputStream());
        return out.getEncoding();
    }

    /** Return a printable error message for an exception.
        Returns the error message is for instances of ErrorMessage or
        for other exceptions the class name with the exception message
        appended, if not empty. */
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
        no grouping, locale ENGLISH. */
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
        for (int i = 0; i < s.length(); ++i)
            if (! Character.isWhitespace(s.charAt(i)))
                return false;
        return true;
    }

    /** Print exception to standard error.
        Prints the class name and message to standard error.
        For exceptions of type Error or RuntimeException, a stack trace
        is printed in addition.
        @return A slightly differently formatted error message
        for display in an error dialog. */
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
        Allows " for words containing whitespaces. */
    public static String[] splitArguments(String string)
    {
        assert string != null;
        ArrayList<String> result = new ArrayList<String>();
        boolean escape = false;
        boolean inString = false;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < string.length(); ++i)
        {
            char c = string.charAt(i);
            if (c == '"' && ! escape)
            {
                if (inString)
                {
                    result.add(token.toString());
                    token.setLength(0);
                }
                inString = ! inString;
            }
            else if (Character.isWhitespace(c) && ! inString)
            {
                if (token.length() > 0)
                {
                    result.add(token.toString());
                    token.setLength(0);
                }
            }
            else
                token.append(c);
            escape = (c == '\\' && ! escape);
        }
        if (token.length() > 0)
            result.add(token.toString());
        return result.toArray(new String[result.size()]);
    }

    /** Trim trailing whitespaces. */
    public static String trimTrailing(String s)
    {
        int i;
        for (i = s.length() - 1; i >= 0; --i)
            if (! Character.isWhitespace(s.charAt(i)))
                break;
        if (i <= 0 || i == s.length() - 1)
            return s;
        return s.substring(0, i + 1);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private StringUtil()
    {
    }
}
