//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import go.*;
import gtp.*;
import utils.*;

//-----------------------------------------------------------------------------

class Regression
    implements Gtp.IOCallback
{
    Regression(String program, String[] tests)
        throws Exception
    {
        m_program = program;
        for (int i = 0; i < tests.length; ++i)
            runTest(tests[i]);
        writeSummary();
        if (! m_gtp.isProgramDead())
            m_gtp.sendCommand("quit");
        m_gtp.waitForExit();
    }

    public static void main(String[] args)
    {
        try
        {
            String options[] = {
                "help"
            };
            Options opt = new Options(args, options);
            if (opt.isSet("help"))
            {
                printUsage(System.out);
                System.exit(0);
            }
            Vector arguments = opt.getArguments();
            int size = arguments.size();
            if (size < 2)
            {
                printUsage(System.err);
                System.exit(-1);
            }
            String program = (String)arguments.get(0);
            String tests[] = new String[size - 1];
            for (int i = 0; i <  size - 1; ++i)
                tests[i] = (String)arguments.get(i + 1);
            new Regression(program, tests);
        }
        catch (Error e)
        {
            String msg = e.getMessage();
            if (msg != null)
                System.err.println(msg);
            e.printStackTrace();
            System.exit(-1);
        }
        catch (RuntimeException e)
        {
            String msg = e.getMessage();
            if (msg != null)
                System.err.println(msg);
            e.printStackTrace();
            System.exit(-1);
        }
        catch (Throwable t)
        {
            String msg = t.getMessage();
            if (msg == null)
                msg = t.getClass().getName();
            System.err.println(msg);
            System.exit(-1);
        }
    }

    public void receivedResponse(boolean error, String s)
    {
    }

    public void receivedStdErr(String s)
    {
        printOutLine("stderr", s);
    }

    public void sentCommand(String s)
    {
    }

    private class ProgramIsDeadException
        extends Exception
    {
    }

    private class Test
    {
        public int m_id;

        public int m_lastSgfMove;

        public boolean m_expectedFail;

        public boolean m_fail;

        public String m_command;

        public String m_required;

        public String m_response;

        public String m_lastSgf;

        public Test(int id, String command, boolean fail, boolean expectedFail,
                    String required, String response, String lastSgf,
                    int lastSgfMove)
        {
            m_id = id;
            m_fail = fail;
            m_expectedFail = expectedFail;
            m_command = command;
            m_required = required;
            m_response = response;
            m_lastSgf = lastSgf;
            m_lastSgfMove = lastSgfMove;
        }
    }

    private class TestSummary
    {
        public File m_file;

        public int m_numberTests;

        public int m_programDied;

        public int m_errors;

        public int m_unexpectedFails;

        public int m_expectedFails;

        public int m_expectedPasses;

        public int m_unexpectedPasses;
    }

    private boolean m_lastError;

    private int m_lastId;

    private int m_lastSgfMove;

    private PrintStream m_out;

    private String m_lastCommand;

    private String m_lastResponse;

    private String m_lastSgf;

    private String m_outFileName;

    private String m_program;

    private Vector m_tests = new Vector();

    private Vector m_testSummaries = new Vector();

    private Gtp m_gtp;

    private void checkLastSgf(String line)
    {
        String regex =
            "[0-9]*\\s*loadsgf\\s+(\\S+\\.[Ss][Gg][Ff])\\s+([0-9]+)\\s*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches())
        {
            m_lastSgf = matcher.group(1);
            try
            {
                m_lastSgfMove = Integer.parseInt(matcher.group(2));
                return;
            }
            catch (NumberFormatException e)
            {
                assert(false);
            }
        }
        regex = "[0-9]*\\s*loadsgf\\s+(\\S+\\.[Ss][Gg][Ff])\\s*";
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(line);
        if (matcher.matches())
        {
            m_lastSgf = matcher.group(1);
            m_lastSgfMove = -1;
        }
        
    }

    private void finishOutFile()
    {
        m_out.print("</pre>\n" +
                    "</body>\n");
        m_out.close();
    }

    private int getId(String line)
    {
        line = StringUtils.replace(line, "\t", "\n");
        int index = line.indexOf(" ");
        if (index < 0)
            return -1;
        try
        {
            return Integer.parseInt(line.substring(0, index));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private TestSummary getTestSummary(File file)
    {
        TestSummary summary = new TestSummary();
        summary.m_file = file;
        for (int i = 0; i < m_tests.size(); ++i)
        {
            Test t = (Test)m_tests.get(i);
            ++summary.m_numberTests;
            if (t.m_fail && ! t.m_expectedFail)
                ++summary.m_unexpectedFails;
            else if (t.m_fail && t.m_expectedFail)
                ++summary.m_expectedFails;
            else if (! t.m_fail && ! t.m_expectedFail)
                ++summary.m_expectedPasses;
            else if (! t.m_fail && t.m_expectedFail)
                ++summary.m_unexpectedPasses;
        }
        return summary;
    }

    private void handleLine(String line)
        throws ProgramIsDeadException
    {
        line = line.trim();
        if (line.startsWith("#?"))
        {
            printOutLine("test", line);
            handleTest(line.substring(2).trim());
            m_lastResponse = null;
            return;
        }
        if (m_lastResponse != null)
        {
            printOutLastResponse();
            m_lastResponse = null;
        }
        if (line.equals(""))
            printOutLine(null, line);
        else if (line.startsWith("#"))
            printOutLine("comment", line);
        else
        {
            line = StringUtils.replace(line, "\t", " ");
            m_lastId = getId(line);
            if (m_lastId < 0)
                m_lastCommand = line;
            else
            {
                int index = line.indexOf(" ");
                m_lastCommand = line.substring(index + 1);
            }
            printOutLine("command", line, m_lastId);
            checkLastSgf(line);
            m_lastError = false;
            assert(m_lastResponse == null);
            try
            {
                m_gtp.sendCommand(line);
            }
            catch (Gtp.Error e)
            {
                if (m_gtp.isProgramDead())
                    throw new ProgramIsDeadException();
                m_lastError = true;
            }
            m_lastResponse = m_gtp.getFullResponse();
        }
    }

    private void handleTest(String patternString)
    {
        boolean expectedFail = false;
        if (patternString.endsWith("*"))
        {
            expectedFail = true;
            patternString =
                patternString.substring(0, patternString.length() - 1);
        }
        if (! patternString.startsWith("[")
            || ! patternString.endsWith("]"))
        {
            printOutLastResponse();
            return;
        }
        patternString = patternString.substring(1, patternString.length() - 1);
        String expectedResponse = patternString;
        boolean notPattern = false;
        if (patternString.startsWith("!"))
        {
            notPattern = true;
            patternString = patternString.substring(1);
        }
        boolean fail = false;
        String response = "";
        if (m_lastError)
            fail = true;
        else
        {
            Pattern pattern = Pattern.compile(patternString);
            int index = m_lastResponse.indexOf(" ");
            if (index < 0)
                response = "";
            else
                response = m_lastResponse.substring(index).trim();
            Matcher matcher = pattern.matcher(response);
            if ((! matcher.matches() && ! notPattern)
                || (matcher.matches() && notPattern))
                fail = true;
        }
        String style = null;
        if (fail && ! expectedFail)
            style = "fail";
        else if (! fail && expectedFail)
            style = "pass";
        printOutLine(style, m_lastResponse);
        m_tests.add(new Test(m_lastId, m_lastCommand, fail, expectedFail,
                             expectedResponse, response, m_lastSgf,
                             m_lastSgfMove));
    }

    private void initOutFile(File test)
        throws Exception
    {
        m_outFileName = FileUtils.replaceExtension(test, "tst", "out.html");
        File file = new File(m_outFileName);
        m_out = new PrintStream(new FileOutputStream(file));
        m_out.print("<html>\n" +
                    "<head>\n" +
                    "<title>Output " + test + "</title>\n" +
                    "<style type=\"text/css\">\n" +
                    "<!--" +
                    "span.command { font-weight:bold; }\n" +
                    "span.comment { color:#999999; }\n" +
                    "span.error { color:#ff0000; }\n" +
                    "span.fail { font-weight:bold; color:#ff0000; }\n" +
                    "span.stderr { font-style: italic; color:#999999; }\n" +
                    "span.pass { font-weight:bold; color:#009900; }\n" +
                    "span.test { font-weight:bold; }\n" +
                    "-->\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body bgcolor=\"white\" text=\"black\" link=\"#0000ee\"" +
                    " vlink=\"#551a8b\">\n" +
                    "<h1>Output " + test + "</h1>\n" +
                    "<hr>\n" +
                    "<pre>\n");
    }

    private synchronized void printOutLastResponse()
    {
        if (m_lastError)
            printOutLine("error", m_lastResponse);
        else
            printOutLine(null, m_lastResponse);
    }

    private synchronized void printOutLine(String style, String line)
    {
        if (! line.endsWith("\n"))
            line = line + "\n";
        line = StringUtils.replace(line, ">", "&gt;");
        line = StringUtils.replace(line, "<", "&lt;");
        line = StringUtils.replace(line, "&", "&amp;");
        if (style == "command")
        {
            Pattern pattern = Pattern.compile("\\S*\\.[Ss][Gg][Ff]");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find())
            {
                String sgf = matcher.group();
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(line.substring(0, matcher.start()));
                stringBuffer.append("<a href=\"");
                stringBuffer.append(sgf);
                stringBuffer.append("\">");
                stringBuffer.append(sgf);
                stringBuffer.append("</a>");
                stringBuffer.append(line.substring(matcher.end()));
                line = stringBuffer.toString();
            }
        }
        if (style != null)
            m_out.print("<span class=\"" + style + "\">");
        m_out.print(line);
        if (style != null)
            m_out.print("</span>");
    }

    private synchronized void printOutLine(String style, String line, int id)
    {
        if (id >= 0)
            m_out.print("<a name=\"" + id + "\">");
        printOutLine(style, line);
        if (id >= 0)
            m_out.print("</a>");
    }

    private static void printUsage(PrintStream out)
    {
        out.print("Usage: java -jar regression.jar program test.tst [...]\n" +
                  "\n" +
                  "  -help    display this help and exit\n");
    }

    private void runTest(String test)
        throws Exception
    {
        m_tests.clear();
        File file = new File(test);
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        initOutFile(file);
        m_gtp = new Gtp(m_program, false, this);
        m_lastSgf = null;
        String line;
        while (true)
        {
            line = reader.readLine();
            if (line == null)
                break;
            handleLine(line);
        }
        if (m_lastResponse != null)
            printOutLastResponse();
        reader.close();
        finishOutFile();
        TestSummary testSummary = getTestSummary(file);
        m_testSummaries.add(testSummary);
        writeTestSummary(file, testSummary);
    }

    private void writeSummaryRow(PrintStream out, TestSummary summary,
                                 boolean withFileName)
    {
        out.print("<tr align=\"center\">\n");
        if (withFileName)
            out.print("<td><a href=\""
                      + FileUtils.replaceExtension(summary.m_file, "tst",
                                                   "html")
                      + "\">" + summary.m_file + "</a></td>");
        out.print("<td>" + summary.m_numberTests + "</td>\n" +
                  "<td bgcolor=\"#"
                  + (summary.m_unexpectedFails > 0 ? "ff0000" : "white")
                  + "\">\n" + summary.m_unexpectedFails + "</td>\n" +
                  "<td>" + summary.m_expectedFails + "</td>\n" +
                  "<td bgcolor=\"#"
                  + (summary.m_unexpectedFails > 0 ? "00ff00" : "white")
                  + "\">\n" + summary.m_unexpectedPasses + "</td>\n" +
                  "<td>" + summary.m_expectedPasses + "</td>\n" +
                  "</tr>\n");
    }

    private void writeTestSummary(File test, TestSummary summary)
        throws FileNotFoundException
    {
        File file = new File(FileUtils.replaceExtension(test, "tst", "html"));
        PrintStream out = new PrintStream(new FileOutputStream(file));
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Summary " + test + "</title>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<h1>Summary " + test + "</h1>\n" +
                  "<hr>\n");
        out.print("<table border=\"1\">\n" +
                  "<colgroup>\n" +
                  "<col width=\"20%\">\n" +
                  "<col width=\"20%\">\n" +
                  "<col width=\"20%\">\n" +
                  "<col width=\"20%\">\n" +
                  "<col width=\"20%\">\n" +
                  "</colgroup>\n" +
                  "<tr align=\"center\">\n" +
                  "<th>Tests</th>" +
                  "<th>FAIL</th>" +
                  "<th>fail</th>" +
                  "<th>PASS</th>" +
                  "<th>pass</th>" +
                  "</tr>\n");
        writeSummaryRow(out, summary, false);
        out.print("</table>\n" +
                  "<hr>\n" +
                  "<table border=\"1\" style=\"font-size:small\">\n" +
                  "<tr>\n" +
                  "<th>Test ID</th>\n" +
                  "<th>Status</th>\n" +
                  "<th>Command</th>\n" +
                  "<th>Output</th>\n" +
                  "<th>Required</th>\n" +
                  "<th>Last SGF</th>\n" +
                  "</tr>\n");
        for (int i = 0; i < m_tests.size(); ++i)
        {
            Test t = (Test)m_tests.get(i);
            String statusColor = null;
            String status = null;
            if (t.m_fail && t.m_expectedFail)
            {
                statusColor = "white";
                status = "fail";
            }
            else if (t.m_fail && ! t.m_expectedFail)
            {
                statusColor = "#ff0000";
                status = "FAIL";
            }
            else if (! t.m_fail && t.m_expectedFail)
            {
                statusColor = "#00ff00";
                status = "PASS";
            }
            else if (! t.m_fail && ! t.m_expectedFail)
            {
                statusColor = "white";
                status = "pass";
            }
            else
                assert(false);
            String lastSgf = "";
            if (t.m_lastSgf != null)
            {
                lastSgf = "<a href=\"" + t.m_lastSgf + "\">" + t.m_lastSgf
                    + "</a>";
                if (t.m_lastSgfMove != -1)
                    lastSgf += "&nbsp;" + t.m_lastSgfMove;
            }
            String command = StringUtils.replace(t.m_command, " ", "&nbsp;");
            out.print("<tr>\n" +
                      "<td align=\"center\"><a href=\"" + m_outFileName + "#"
                      + t.m_id + "\">" + t.m_id + "</a></td>\n" +
                      "<td align=\"center\" bgcolor=\"" + statusColor
                      + "\">" + status + "</td>\n" +
                      "<td>" + command + "</td>\n" +
                      "<td>" + t.m_response + "</td>\n" +
                      "<td>" + t.m_required + "</td>\n" +
                      "<td>" + lastSgf + "</td>\n" +
                      "</tr>\n");
        }
        out.print("</table>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeSummary()
        throws FileNotFoundException
    {
        File file = new File("index.html");
        PrintStream out = new PrintStream(new FileOutputStream(file));
        String host = "?";
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                           DateFormat.FULL);
        Date date = Calendar.getInstance().getTime();
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Summary</title>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<h1>Summary</h1>\n" +
                  "<hr>\n" +
                  "<table>\n" +
                  "<tr><th align=\"left\">Date</th><td>" + format.format(date)
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Host</th><td>" + host
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Command</th><td><tt>" + m_program
                  + "</tt></td></tr>\n" +
                  "</table>\n" +
                  "<hr>\n" +
                  "<table border=\"1\">\n" +
                  "<colgroup>\n" +
                  "<col width=\"25%\">\n" +
                  "<col width=\"15%\">\n" +
                  "<col width=\"15%\">\n" +
                  "<col width=\"15%\">\n" +
                  "<col width=\"15%\">\n" +
                  "<col width=\"15%\">\n" +
                  "</colgroup>\n" +
                  "<tr align=\"center\">\n" +
                  "<th>File</th>" +
                  "<th>Tests</th>" +
                  "<th>FAIL</th>" +
                  "<th>fail</th>" +
                  "<th>PASS</th>" +
                  "<th>pass</th>" +
                  "</tr>\n");
        for (int i = 0; i < m_testSummaries.size(); ++i)
        {
            TestSummary summary = (TestSummary)m_testSummaries.get(i);
            writeSummaryRow(out, summary, true);
        }
        out.print("</table>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }
}
    
//-----------------------------------------------------------------------------
