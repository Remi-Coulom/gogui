//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpregress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.gtp.Gtp;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.utils.FileUtils;
import net.sf.gogui.utils.StringUtils;
import net.sf.gogui.version.Version;

//----------------------------------------------------------------------------

/** Runs GTP regression tests. */
public class GtpRegress
    implements Gtp.IOCallback
{
    GtpRegress(String program, String[] tests, String output,
               boolean longOutput, boolean verbose, boolean fileComments)
        throws Exception
    {
        m_result = true;
        m_program = program;
        m_longOutput = longOutput;
        m_verbose = verbose;
        m_fileComments = fileComments;
        m_prefix = "";
        if (! output.equals(""))
        {
            File file = new File(output);
            if (! file.exists())
                file.mkdir();
            m_prefix = output + File.separator;
        }
        for (int i = 0; i < tests.length; ++i)
        {
            if (tests.length > 1)
                m_outPrefix = tests[i] + " ";
            else
                m_outPrefix = "";
            runTest(tests[i]);
        }
        writeSummary();
    }

    /** Return true if tests completed with no unexpected failures. */
    public boolean getResult()
    {
        return m_result;
    }

    public void receivedInvalidResponse(String s)
    {
        printOutLine("invalid", "Invalid response: " + s + " [...]");
    }

    public void receivedResponse(boolean error, String s)
    {
    }

    public void receivedStdErr(String s)
    {
        printOut("stderr", s, -1);
    }

    public void sentCommand(String s)
    {
    }

    /** Exception thrown if Go program died. */
    private static class ProgramIsDeadException
        extends Exception
    {
        public String getMessage()
        {
            return "Program died";
        }

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    /** Information about one test and its result. */
    private static class Test
    {
        public int m_id;

        public int m_lastSgfMove;

        public boolean m_expectedFail;

        public boolean m_fail;

        public String m_command;

        public String m_required;

        public String m_response;

        public String m_lastSgf;

        public Test(int id, String command, boolean fail,
                    boolean expectedFail, String required, String response,
                    String lastSgf, int lastSgfMove)
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

    /** Information about test results of one test file. */
    private static class TestSummary
    {
        public File m_file;

        public int m_numberTests;

        public int m_programDied;

        public int m_otherErrors;

        public int m_unexpectedFails;

        public int m_expectedFails;

        public int m_expectedPasses;

        public int m_unexpectedPasses;

        public long m_timeMillis;

        public double m_cpuTime;

        public int getNumberPasses()
        {
            return m_expectedPasses + m_unexpectedPasses;
        }
    }

    private boolean m_fileComments;

    private boolean m_lastError;

    private boolean m_lastTestFailed;

    private boolean m_longOutput;

    private boolean m_result;

    private boolean m_verbose;

    private int m_lastCommandId;

    private int m_lastId;

    private int m_lastSgfMove;

    private int m_otherErrors;

    private File m_file;

    private PrintStream m_out;

    private final Set m_dataFiles = new TreeSet();

    private static final String m_colorError = "#ffa954";

    private static final String m_colorHeader = "#91aee8";

    private static final String m_colorInfo = "#e0e0e0";

    private static final String m_colorLightBackground = "#e0e0e0";

    private static final String m_colorGrayBackground = "#e0e0e0";

    private static final String m_colorGreen = "#5eaf5e";

    private static final String m_colorRed = "#ff5454";

    private String m_currentStyle;

    private String m_lastCommand;

    private String m_lastFullResponse;

    private String m_lastResponse;

    private String m_lastSgf;

    private String m_name;

    private String m_outFileName;

    private String m_outFileRelativeName;

    private String m_outPrefix;

    private String m_prefix;

    private String m_program;

    private String m_relativePath;

    private String m_version;

    private final Vector m_tests = new Vector();

    private final Vector m_testSummaries = new Vector();

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
        if (m_currentStyle != null)
            m_out.print("</span>");
        m_out.print("</pre>\n" +
                    "</body>\n");
        m_out.close();
    }

    private int getId(String line)
    {
        line = line.replaceAll("\\t", "\n");
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

    private TestSummary getTestSummary(long timeMillis, double cpuTime)
    {
        TestSummary summary = new TestSummary();
        summary.m_file = m_file;
        summary.m_timeMillis = timeMillis;
        summary.m_cpuTime = cpuTime;
        summary.m_otherErrors = m_otherErrors;
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

    private synchronized void handleLastResponse()
    {
        if (m_lastCommandId >= 0)
        {
            boolean fail = false;
            if (m_lastError)
            {
                printOutLine("fail", m_lastFullResponse);
                if (m_lastResponse.equals(""))
                    System.out.println(m_outPrefix
                                       + Integer.toString(m_lastCommandId)
                                       + " unexpected FAIL");
                else
                    System.out.println(m_outPrefix
                                       + Integer.toString(m_lastCommandId)
                                       + " unexpected FAIL: '"
                                       + m_lastResponse + "'");
                fail = true;
            }
            else
                printOutLine("test", m_lastFullResponse);
            m_tests.add(new Test(m_lastCommandId, m_lastCommand, fail, false,
                                 "", m_lastResponse, m_lastSgf,
                                 m_lastSgfMove));
            m_lastTestFailed = fail;
        }
        else
        {
            if (m_lastError)
            {
                printOutLine("error", m_lastFullResponse);
                ++m_otherErrors;
            }
            else
                printOutLine(null, m_lastFullResponse);
        }
    }

    private void handleLine(String line)
        throws ProgramIsDeadException
    {
        line = line.trim();
        if (line.startsWith("#?"))
        {
            if (m_lastFullResponse == null)
            {
                System.err.println("Warning: " + m_file + ": Response pattern"
                                   + " without preceding test command: "
                                   + line);
                printOutLine("comment", line);
                return;
            }
            printOutLine("test", line);
            handleTest(line.substring(2).trim());
            m_lastFullResponse = null;
            return;
        }
        if (line.startsWith("#>") && m_fileComments)
        {
            printOutLine(null, line);
            if (m_lastFullResponse != null)
                handleLastResponse();
            printDataFile(line.substring(2).trim());
            m_lastFullResponse = null;
            return;
        }
        if (m_lastFullResponse != null)
        {
            handleLastResponse();
            m_lastFullResponse = null;
        }
        if (line.equals(""))
            printOutLine(null, line);
        else if (line.startsWith("#"))
            printOutLine("comment", line);
        else
        {
            line = line.replaceAll("\\t", " ");
            m_lastCommandId = getId(line);
            if (m_lastCommandId < 0)
                m_lastCommand = line;
            else
            {
                int index = line.indexOf(" ");
                m_lastCommand = line.substring(index + 1);
                m_lastId = m_lastCommandId;
            }
            printOutLine(m_lastCommandId >= 0 ? "test" : "command", line,
                         m_lastCommandId);
            checkLastSgf(line);
            m_lastError = false;
            assert(m_lastFullResponse == null);
            try
            {
                m_lastResponse = m_gtp.sendCommand(line);
            }
            catch (GtpError error)
            {
                m_lastError = true;
                m_lastResponse = error.getMessage();
                if (m_gtp.isProgramDead())
                    throw new ProgramIsDeadException();
            }
            m_lastFullResponse = m_gtp.getFullResponse();
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
            handleLastResponse();
            return;
        }
        patternString =
            patternString.substring(1, patternString.length() - 1).trim();
        String expectedResponse = patternString;
        boolean notPattern = false;
        if (patternString.startsWith("!"))
        {
            notPattern = true;
            patternString = patternString.substring(1);
        }
        boolean fail = false;
        String response = "";
        int index = m_lastFullResponse.indexOf(" ");
        if (index >= 0)
            response = m_lastFullResponse.substring(index).trim();
        if (m_lastError)
            fail = true;
        else
        {
            Pattern pattern
                = Pattern.compile(patternString,
                                  Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            if ((! matcher.matches() && ! notPattern)
                || (matcher.matches() && notPattern))
                fail = true;
        }
        if (fail  && ! expectedFail)
            m_result = false;
        m_lastTestFailed = fail;
        String style = null;
        if (fail && ! expectedFail)
            style = "fail";
        else if (! fail && expectedFail)
            style = "pass";
        else
            style = "test";
        printOutLine(style, m_lastFullResponse);
        if (m_longOutput)
        {
            // Output compatible with eval.sh in GNU Go
            if (fail && ! expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " FAILED: Correct '"
                                   + expectedResponse + "', got '" + response
                                   + "'");
            else if (fail && expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " failed: Correct '"
                                   + expectedResponse + "', got '" + response
                                   + "'");
            else if (! fail && expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " PASSED");
            else if (! fail && ! expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " passed");
        }
        else
        {
            // Output compatible with regress.sh in GNU Go
            if (fail && ! expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " unexpected FAIL: Correct '"
                                   + expectedResponse + "', got '" + response
                                   + "'");
            else if (! fail && expectedFail)
                System.out.println(m_outPrefix
                                   + Integer.toString(m_lastCommandId)
                                   + " unexpected PASS!");
        }
        m_tests.add(new Test(m_lastCommandId, m_lastCommand, fail,
                             expectedFail, expectedResponse, response,
                             m_lastSgf, m_lastSgfMove));
    }

    private void initOutFile()
        throws Exception
    {
        m_outFileRelativeName =
            FileUtils.replaceExtension(m_file, "tst", "out.html");
        m_outFileName = m_prefix + m_outFileRelativeName;        
        File file = new File(m_outFileName);
        File parent = file.getParentFile();
        if (parent != null && ! parent.exists())
            parent.mkdir();
        m_currentStyle = null;
        m_out = new PrintStream(new FileOutputStream(file));
        m_out.print("<html>\n" +
                    "<head>\n" +
                    "<title>Output: " + m_file + "</title>\n" +
                    "<meta name=\"generator\" content=\"GtpRegress "
                    + Version.get() + "\">\n" +
                    "<style type=\"text/css\">\n" +
                    "<!--\n" +
                    "span.comment { color:#999999; }\n" +
                    "span.fail { font-weight:bold; color:" + m_colorRed
                    + "; }\n" +
                    "span.error { font-weight:bold; color:" + m_colorError
                    + "; }\n" +
                    "span.stderr { font-style: italic; color:#666666; }\n" +
                    "span.invalid { background:" + m_colorRed + ";}\n" +
                    "span.pass { font-weight:bold; color:" + m_colorGreen
                    + "; }\n" +
                    "span.test { font-weight:bold; }\n" +
                    "-->\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body bgcolor=\"white\" text=\"black\"" +
                    " link=\"#0000ee\" vlink=\"#551a8b\">\n" +
                    "<table border=\"0\" width=\"100%\" bgcolor=\""
                    + m_colorHeader + "\">\n" +
                    "<tr><td>\n" +
                    "<h1>Output: " + m_file + "</h1>\n" +
                    "</td></tr>\n" +
                    "</table>\n" +
                    "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                    + "\">\n");
        writeInfo(m_out, false);
        m_out.print("</table>\n" +
                    "<pre>\n");
    }

    private void printDataFile(String filename)
    {
        try
        {
            if (filename.equals(""))
                filename = m_prefix
                    + FileUtils.replaceExtension(m_file, "tst", "dat");
            else
                filename = m_prefix + filename;
            File file = new File(filename);
            if (! m_dataFiles.contains(file))
            {
                if (file.exists())
                    file.delete();
                m_dataFiles.add(file);
            }
            FileOutputStream outputStream = new FileOutputStream(file, true);
            PrintStream out = new PrintStream(outputStream);
            out.println(m_lastId + " " +  (m_lastTestFailed ? "1" : "0")
                        + " " + m_lastResponse);
            out.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Could not open file '" + filename + "'");
        }
    }

    private synchronized void printOut(String style, String line, int id)
    {
        if (line == null)
            return;
        line = line.replaceAll("&", "&amp;");
        line = line.replaceAll(">", "&gt;");
        line = line.replaceAll("<", "&lt;");
        if (style != null
            && (style.equals("command") || style.equals("test")))
        {
            Pattern pattern = Pattern.compile("\\S*\\.[Ss][Gg][Ff]");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find())
            {
                String sgf = matcher.group();
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(line.substring(0, matcher.start()));
                stringBuffer.append("<a href=\"");
                stringBuffer.append(m_relativePath + sgf);
                stringBuffer.append("\">");
                stringBuffer.append(sgf);
                stringBuffer.append("</a>");
                stringBuffer.append(line.substring(matcher.end()));
                line = stringBuffer.toString();
            }
        }
        if (style != m_currentStyle)
        {
            if (m_currentStyle != null)
                m_out.print("</span>");
            m_out.print("<span class=\"" + style + "\">");
            m_currentStyle = style;
        }
        if (id >= 0)
            m_out.print("<a name=\"" + id + "\">");            
        m_out.print(line);
        if (id >= 0)
            m_out.print("</a>");            
    }

    private synchronized void printOutLine(String style, String line, int id)
    {
        if (line == null)
            return;
        if (! line.endsWith("\n"))
            line = line + "\n";
        printOut(style, line, id);
    }

    private synchronized void printOutSeparator()
    {
        m_out.println("<hr>");
    }

    private synchronized void printOutLine(String style, String line)
    {
        printOutLine(style, line, -1);
    }

    private String sendCommand(String command) throws GtpError
    {
        printOutLine(null, command);
        try
        {
            return m_gtp.sendCommand(command);
        }
        finally
        {
            printOutLine(null, m_gtp.getFullResponse());
        }
    }

    private double getCpuTime()
    {
        try
        {
            return Double.parseDouble(sendCommand("cputime"));
        }
        catch (GtpError e)
        {
            return 0;
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private TestSummary getTotalSummary()
    {
        TestSummary total = new TestSummary();
        for (int i = 0; i < m_testSummaries.size(); ++i)
        {
            TestSummary summary = (TestSummary)m_testSummaries.get(i);
            total.m_numberTests += summary.m_numberTests;
            total.m_otherErrors += summary.m_otherErrors;
            total.m_unexpectedFails += summary.m_unexpectedFails;
            total.m_expectedFails += summary.m_expectedFails;
            total.m_expectedPasses += summary.m_expectedPasses;
            total.m_unexpectedPasses += summary.m_unexpectedPasses;
            total.m_timeMillis += summary.m_timeMillis;
            total.m_cpuTime += summary.m_cpuTime;
        }
        return total;
    }

    private void runTest(String test)
        throws Exception
    {
        m_tests.clear();
        m_dataFiles.clear();
        m_otherErrors = 0;
        m_file = new File(test);
        m_relativePath =
            FileUtils.getRelativePath(new File(m_prefix), m_file);
        FileReader fileReader = new FileReader(m_file);
        BufferedReader reader = new BufferedReader(fileReader);
        initOutFile();
        m_gtp = new Gtp(m_program, m_verbose, this);
        m_lastSgf = null;
        try
        {
            m_name = sendCommand("name");
        }
        catch (GtpError e)
        {
            m_name = "";
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_version = sendCommand("version");
        }
        catch (GtpError e)
        {
            m_version = "";
        }
        double cpuTime = getCpuTime();
        long timeMillis = System.currentTimeMillis();
        printOutSeparator();
        String line;
        while (true)
        {
            line = reader.readLine();
            if (line == null)
                break;
            handleLine(line);
        }
        timeMillis = System.currentTimeMillis() - timeMillis;
        printOutSeparator();
        cpuTime = getCpuTime() - cpuTime;
        if (m_lastFullResponse != null)
            handleLastResponse();
        if (! m_gtp.isProgramDead())
            sendCommand("quit");
        m_gtp.waitForExit();
        reader.close();
        finishOutFile();
        TestSummary testSummary = getTestSummary(timeMillis, cpuTime);
        m_testSummaries.add(testSummary);
        writeTestSummary(testSummary);
    }

    private String truncate(String string)
    {
        int maxLength = 25;
        if (string.length() < maxLength)
            return string.trim();
        return string.substring(0, maxLength).trim() + "...";
    }

    private void writeInfo(PrintStream out, boolean withName)
    {
        String host;
        try
        {
            host = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            host = "?";
        }
        if (withName)
            out.print("<tr><th align=\"left\">Name:</th><td>" + m_name
                      + "</td></tr>\n" +
                      "<tr><th align=\"left\">Version:</th><td>" + m_version
                      + "</td></tr>\n");
        out.print("<tr><th align=\"left\">Date:</th><td>"
                  + StringUtils.getDate()
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Host:</th><td>" + host
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\" valign=\"top\">Command:</th>\n" +
                  "<td valign=\"top\"><tt>" + m_program
                  + "</tt></td></tr>\n");
    }

    private void writeSummary()
        throws FileNotFoundException
    {
        File file = new File(m_prefix + "index.html");
        PrintStream out = new PrintStream(new FileOutputStream(file));
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Regression Test Summary</title>\n" +
                  "<meta name=\"generator\" content=\"GtpRegress "
                  + Version.get() + "\">\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + m_colorHeader + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>Regression Test Summary</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n");
        writeInfo(out, true);
        out.print("</table>\n" +
                  "<p>\n" +
                  "<table width=\"100%\">\n" +
                  "<colgroup>\n" +
                  "<col width=\"20%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "<col width=\"10%\">\n" +
                  "</colgroup>\n" +
                  "<thead align=\"center\">\n" +
                  "<tr bgcolor = \"" + m_colorHeader + "\">\n" +
                  "<th>File</th>\n" +
                  "<th>Tests</th>\n" +
                  "<th>FAIL</th>\n" +
                  "<th>fail</th>\n" +
                  "<th>PASS</th>\n" +
                  "<th>pass</th>\n" +
                  "<th>Error</th>\n" +
                  "<th>Time</th>\n" +
                  "<th>CpuTime</th>\n" +
                  "</tr>\n" +
                  "</thead>\n");
        for (int i = 0; i < m_testSummaries.size(); ++i)
        {
            TestSummary summary = (TestSummary)m_testSummaries.get(i);
            writeSummaryRow(out, summary, true, false);
        }
        writeSummaryRow(out, getTotalSummary(), true, true);
        out.print("</table>\n" +
                  "</p>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeSummaryRow(PrintStream out, TestSummary summary,
                                 boolean withFileName, boolean foot)
    {
        File file = summary.m_file;
        if (foot)
        {
            out.print("<tfoot align=\"center\">\n");
            out.print("<tr align=\"center\" bgcolor=\""
                      + m_colorHeader + "\">\n");
        }
        else
            out.print("<tr align=\"center\" bgcolor=\""
                      + m_colorGrayBackground + "\">\n");
        if (withFileName)
        {
            if (foot)
                out.print("<td><b>Total</b></td>");
            else
                out.print("<td><a href=\""
                          + FileUtils.replaceExtension(file, "tst", "html")
                          + "\">" + file + "</a></td>");
        }
        double time = ((double)summary.m_timeMillis) / 1000F;
        NumberFormat format = StringUtils.getNumberFormat(1);
        String colorAttrUnexpectedFails = "";
        if (summary.m_unexpectedFails > 0)
            colorAttrUnexpectedFails = " bgcolor=\"" + m_colorRed + "\"";
        String colorAttrUnexpectedPasses = "";
        if (summary.m_unexpectedPasses > 0)
            colorAttrUnexpectedPasses = " bgcolor=\"" + m_colorGreen + "\"";
        String colorAttrOtherErrors = "";
        if (summary.m_otherErrors > 0)
            colorAttrOtherErrors = " bgcolor=\"" + m_colorError + "\"";
        out.print("<td>" + summary.m_numberTests + "</td>\n" +
                  "<td" + colorAttrUnexpectedFails + ">"
                  + summary.m_unexpectedFails + "</td>\n" +
                  "<td>" + summary.m_expectedFails + "</td>\n" +
                  "<td" + colorAttrUnexpectedPasses + ">\n"
                  + summary.m_unexpectedPasses + "</td>\n" +
                  "<td>" + summary.m_expectedPasses + "</td>\n" +
                  "<td" + colorAttrOtherErrors + ">\n"
                  + summary.m_otherErrors + "</td>\n" +
                  "<td>" + format.format(time) + "</td>\n" +
                  "<td>" + format.format(summary.m_cpuTime) + "</td>\n" +
                  "</tr>\n");
        if (foot)
            out.print("</tfoot>\n");
    }

    private void writeTestSummary(TestSummary summary)
        throws FileNotFoundException
    {
        if (m_longOutput)
        {
            // Output compatible with eval.sh in GNU Go
            System.out.println("Summary: " + summary.getNumberPasses()
                               + "/" + summary.m_numberTests + " passes. "
                               + summary.m_unexpectedPasses
                               + " unexpected passes, "
                               + summary.m_unexpectedFails
                               + " unexpected failures");
        }
        File file =
            new File(m_prefix
                     + FileUtils.replaceExtension(m_file, "tst", "html"));
        PrintStream out = new PrintStream(new FileOutputStream(file));
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Summary: " + m_file + "</title>\n" +
                  "<meta name=\"generator\" content=\"GtpRegress "
                  + Version.get() + "\">\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + m_colorHeader + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>Summary: " + m_file + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + m_colorInfo
                  + "\">\n");
        writeInfo(out, true);
        out.print("<tr><th align=\"left\">Output:</th><td><a href=\""
                  + m_outFileRelativeName + "\">"
                  + m_outFileRelativeName + "</a></td></tr>\n" +
                  "</table>\n" +
                  "<p>\n" +
                  "<table width=\"100%\">\n" +
                  "<colgroup>\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "<col width=\"12%\">\n" +
                  "</colgroup>\n" +
                  "<thead align=\"center\">\n" +
                  "<tr bgcolor=\"" + m_colorHeader + "\">\n" +
                  "<th>Tests</th>\n" +
                  "<th>FAIL</th>\n" +
                  "<th>fail</th>\n" +
                  "<th>PASS</th>\n" +
                  "<th>pass</th>\n" +
                  "<th>Error</th>\n" +
                  "<th>Time</th>\n" +
                  "<th>CpuTime</th>\n" +
                  "</tr>\n" +
                  "</thead>\n");
        writeSummaryRow(out, summary, false, false);
        out.print("</table>\n" +
                  "</p>\n" +
                  "<p>\n" +
                  "<table width=\"100%\">\n" +
                  "<thead>\n" +
                  "<tr bgcolor=\"" + m_colorHeader + "\">\n" +
                  "<th>ID</th>\n" +
                  "<th>Status</th>\n" +
                  "<th>Command</th>\n" +
                  "<th>Output</th>\n" +
                  "<th>Required</th>\n" +
                  "<th>Last SGF</th>\n" +
                  "</tr>\n" +
                  "</thead>\n");
        for (int i = 0; i < m_tests.size(); ++i)
        {
            Test t = (Test)m_tests.get(i);
            String rowBackground = m_colorLightBackground;
            String statusColor = rowBackground;
            String status = null;
            if (t.m_fail && t.m_expectedFail)
            {
                status = "fail";
            }
            else if (t.m_fail && ! t.m_expectedFail)
            {
                statusColor = m_colorRed;
                status = "FAIL";
            }
            else if (! t.m_fail && t.m_expectedFail)
            {
                statusColor = m_colorGreen;
                status = "PASS";
            }
            else if (! t.m_fail && ! t.m_expectedFail)
            {
                status = "pass";
            }
            else
                assert(false);
            String lastSgf = "";
            if (t.m_lastSgf != null)
            {
                lastSgf = "<a href=\"" + m_relativePath + t.m_lastSgf + "\">"
                    + t.m_lastSgf + "</a>";
                if (t.m_lastSgfMove != -1)
                    lastSgf += "&nbsp;" + t.m_lastSgfMove;
            }
            String command = t.m_command.replaceAll(" ", "&nbsp;");
            out.print("<tr bgcolor=\"" + rowBackground + "\">\n" +
                      "<td align=\"right\"><a href=\"" + m_outFileRelativeName
                      + "#" + t.m_id + "\">" + t.m_id + "</a></td>\n" +
                      "<td align=\"center\" bgcolor=\"" + statusColor
                      + "\">" + status + "</td>\n" +
                      "<td>" + command + "</td>\n" +
                      "<td align=\"center\">" + truncate(t.m_response)
                      + "</td>\n" +
                      "<td align=\"center\">" + truncate(t.m_required)
                      + "</td>\n" +
                      "<td>" + lastSgf + "</td>\n" +
                      "</tr>\n");
        }
        out.print("</table>\n" +
                  "</p>\n" +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }
}
    
//----------------------------------------------------------------------------
