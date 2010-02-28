// AnalyzeUtil.java

package net.sf.gogui.gtp;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.NoSuchElementException;

public final class AnalyzeUtil
{
    /** Result of AnalyzeUtil.parseParameterLine(). */
    public static final class Result
    {
        public ParameterType m_type;

        /** Complete type metainformation. */
        public String m_typeInfo;

        public String m_key;

        public String m_value;
    }

    /** Get command for setting a parameter.
        See chapter "Analyze Commands" of the GoGui documentation. */
    public static String getParameterCommand(String command, String key,
                                             String value)
    {
        return command + " " + key + " " + value;
    }

    public static boolean
        hasParameterCommands(ArrayList<AnalyzeDefinition> analyzeCommands)
    {
        for (AnalyzeDefinition definition : analyzeCommands)
            if (definition.getType() == AnalyzeType.PARAM)
                return true;
        return false;
    }

    /** Parse a line in the response of an analyze command of type "param".
        See chapter "Analyze Commands" of the GoGui documentation.
        @return The result or null, if line could not be parsed. */
    public static Result parseParameterLine(String line)
    {
        line = line.trim();
        if (line.startsWith("[") && line.endsWith("]"))
        {
            // Might be used as label for grouping parameters on tabbing
            // panes in a later version of GoGui, so we silently accept it
            return null;
        }
        Scanner scanner = new Scanner(line);
        Result result = new Result();
        try
        {
            result.m_typeInfo = scanner.next("^\\[[^\\]]*\\]");
            line = line.substring(result.m_typeInfo.length()).trim();
            result.m_typeInfo =
                result.m_typeInfo.substring(1, result.m_typeInfo.length() - 1);
        }
        catch (NoSuchElementException e)
        {
            // Treat unknown types as string for compatibiliy with future
            // types
            result.m_typeInfo = "string";
        }
        int pos = line.indexOf(' ');
        if (pos < 0)
        {
            result.m_key = line.trim();
            result.m_value = "";
        }
        else
        {
            result.m_key = line.substring(0, pos).trim();
            result.m_value = line.substring(pos + 1).trim();
        }
        if (result.m_typeInfo.equals("bool"))
            result.m_type = ParameterType.BOOL;
        else if (result.m_typeInfo.startsWith("list/"))
            result.m_type = ParameterType.LIST;
        else
            // Treat unknown types as string for compatibiliy with future
            // types
            result.m_type = ParameterType.STRING;
        return result;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private AnalyzeUtil()
    {
    }
}
