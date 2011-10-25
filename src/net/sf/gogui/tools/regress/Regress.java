// Regress.java

package net.sf.gogui.tools.regress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.gogui.gtp.GtpClient;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.gtp.GtpUtil;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;
import net.sf.gogui.util.HtmlUtil;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;

/** Runs GTP regression tests. */
public class Regress
    implements GtpClient.IOCallback
{
    /** Constructor.
        @param gtpFile File with GTP commands to send at startup or
        <code>null</code> for no file. */
    public Regress(String program, ArrayList<String> tests, String output,
                   boolean longOutput, boolean verbose, File gtpFile)
        throws Exception
    {
        tests = RegressUtil.expandTestSuites(tests);
        RegressUtil.checkFiles(tests);
        m_result = true;
        m_program = program;
        m_longOutput = longOutput;
        m_verbose = verbose;
        m_gtpFile = gtpFile;
        if (output.equals(""))
            m_prefix = "";
        else
        {
            File file = new File(output);
            if (! file.exists())
                if (! file.mkdir())
                    throw new ErrorMessage("Could not create output directory '"
                                           + output + "'");
            m_prefix = output + File.separator;
        }
        initOutNames(tests);
        for (int i = 0; i < tests.size(); ++i)
        {
            String test = tests.get(i);
            if (tests.size() > 1)
                m_outPrefix = test + " ";
            else
                m_outPrefix = "";
            runTest(test);
        }
        writeSummary();
        writeData();
    }

    /** Return true if tests completed with no unexpected failures. */
    public boolean getResult()
    {
        return m_result;
    }

    public void receivedInvalidResponse(String s)
    {
        printOutLine("invalid", "Invalid response: " + s);
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

        /** See Regress#m_outName */
        public String m_outName;

        public int m_numberTests;

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

    private boolean m_lastError;

    private final boolean m_longOutput;

    private boolean m_result;

    private final boolean m_verbose;

    private int m_lastCommandId;

    private int m_lastSgfMove;

    private int m_otherErrors;

    private File m_testFile;

    private PrintStream m_out;

    private static final String COLOR_ERROR = "#ffa954";

    private static final String COLOR_HEADER = "#91aee8";

    private static final String COLOR_INFO = "#e0e0e0";

    private static final String COLOR_BG_LIGHT = "#e0e0e0";

    private static final String COLOR_BG_GRAY = "#e0e0e0";

    private static final String COLOR_GREEN = "#5eaf5e";

    private static final String COLOR_RED = "#ff5454";

    /** Output file of the current test.
        The file contains an HTML formatted log of the GTP streams and
        the standard error of Go program. */
    private File m_outFile;

    private final File m_gtpFile;

    private String m_currentStyle;

    private String m_lastCommand;

    private String m_lastFullResponse;

    private String m_lastResponse;

    private String m_lastSgf;

    private String m_name;

    /** Name of m_outFile and the summary file of the test without directory
        and file extension for the current test. */
    private String m_outName;

    private String m_outFileRelativeName;

    private String m_outPrefix;

    private final String m_prefix;

    private final String m_program;

    /** Relative URI path between m_outFile and the directory of the current
        test. */
    private String m_relativePath;

    private String m_version;

    /** Name of m_outFile and the summary file of the test without directory
        and file extension for the all tests. */
    private TreeMap<String,String> m_outNames;

    private final ArrayList<Test> m_tests = new ArrayList<Test>();

    private final ArrayList<TestSummary> m_testSummaries
        = new ArrayList<TestSummary>();

    private GtpClient m_gtp;

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
                assert false;
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
                    HtmlUtil.getFooter("gogui-regress") +
                    "</body>\n" +
                    "</html>\n");
        m_out.close();
    }

    private int getId(String line)
    {
        line = line.replaceAll("\\t", "\n");
        int index = line.indexOf(' ');
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
        summary.m_file = m_testFile;
        summary.m_outName = m_outName;
        summary.m_timeMillis = timeMillis;
        summary.m_cpuTime = cpuTime;
        summary.m_otherErrors = m_otherErrors;
        for (int i = 0; i < m_tests.size(); ++i)
        {
            Test t = m_tests.get(i);
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
        throws ErrorMessage, ProgramIsDeadException
    {
        line = line.trim();
        if (line.startsWith("#?"))
        {
            if (m_lastFullResponse == null)
                throw new ErrorMessage(m_testFile
                                       + ": Response pattern"
                                       + " without preceding test command: "
                                       + line);
            printOutLine("test", line);
            handleTest(line.substring(2).trim());
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
                int index = line.indexOf(' ');
                m_lastCommand = line.substring(index + 1);
            }
            printOutLine(m_lastCommandId >= 0 ? "test" : "command", line,
                         m_lastCommandId);
            checkLastSgf(line);
            m_lastError = false;
            assert m_lastFullResponse == null;
            try
            {
                m_lastResponse = m_gtp.send(line);
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

    private void handleTest(String patternString) throws ErrorMessage
    {
        boolean expectedFail = false;
        if (StringUtil.isEmpty(patternString))
        {
            handleLastResponse();
            return;
        }
        if (patternString.endsWith("*"))
        {
            expectedFail = true;
            patternString =
                patternString.substring(0, patternString.length() - 1);
        }
        if (! patternString.startsWith("["))
            throw new ErrorMessage(m_testFile
                                   + ": Pattern has no opening bracket: "
                                   + patternString);
        if (! patternString.endsWith("]"))
            throw new ErrorMessage(m_testFile
                                   + ": Pattern has no closing bracket: "
                                   + patternString);
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
        int index = m_lastFullResponse.indexOf(' ');
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

    /** Compute unique names for output directory.
        Appends a number, if tests with same name in different directories
        exist. */
    private void initOutNames(ArrayList<String> tests)
    {
        m_outNames = new TreeMap<String,String>();
        for (int i = 0; i < tests.size(); ++i)
        {
            String test = tests.get(i);
            File testFile = new File(test);
            String name =
                FileUtil.removeExtension(new File(testFile.getName()), "tst");
            if (m_outNames.containsValue(name))
                for (int j = 2; ; ++j)
                {
                    String testName = name + "_" + j;
                    if (! m_outNames.containsValue(testName))
                    {
                        name = testName;
                        break;
                    }
                }
            m_outNames.put(test, name);
        }
    }

    private void initOutFile()
        throws Exception
    {
        m_outFileRelativeName = m_outName + ".out.html";
        m_outFile = new File(m_prefix + m_outFileRelativeName);
        File parent = m_outFile.getParentFile();
        if (parent != null && ! parent.exists())
            if (! parent.mkdir())
                throw new ErrorMessage("Could not create directory '"
                                       + parent + "'");
        m_currentStyle = null;
        m_out = new PrintStream(m_outFile);
        m_out.print("<html>\n" +
                    "<head>\n" +
                    "<title>Output: " + m_testFile + "</title>\n" +
                    HtmlUtil.getMeta("gogui-regress") +
                    "<style type=\"text/css\">\n" +
                    "<!--\n" +
                    "body { margin:0; }\n" +
                    "span.comment { color:#999999; }\n" +
                    "span.fail { font-weight:bold; color:" + COLOR_RED
                    + "; }\n" +
                    "span.error { font-weight:bold; color:" + COLOR_ERROR
                    + "; }\n" +
                    "span.stderr { color:#666666; }\n" +
                    "span.invalid { background:" + COLOR_RED + ";}\n" +
                    "span.pass { font-weight:bold; color:" + COLOR_GREEN
                    + "; }\n" +
                    "span.test { font-weight:bold; }\n" +
                    "-->\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body bgcolor=\"white\" text=\"black\"" +
                    " link=\"#0000ee\" vlink=\"#551a8b\">\n" +
                    "<table border=\"0\" width=\"100%\" bgcolor=\""
                    + COLOR_HEADER + "\" border=\"0\">\n" +
                    "<tr><td>\n" +
                    "<h1>Output: " + m_testFile + "</h1>\n" +
                    "</td></tr>\n" +
                    "</table>\n" +
                    "<table width=\"100%\" bgcolor=\"" + COLOR_INFO
                    + "\">\n");
        writeInfo(m_out, false);
        m_out.print("</table>\n" +
                    "<pre style=\"margin:1em\">\n");
    }

    private synchronized void printOut(String style, String line, int id)
    {
        if (line == null || line.length() == 0)
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
                StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append(line.substring(0, matcher.start()));
                stringBuffer.append("<a href=\"");
                stringBuffer.append(m_relativePath);
                stringBuffer.append(sgf);
                stringBuffer.append("\">");
                stringBuffer.append(sgf);
                stringBuffer.append("</a>");
                stringBuffer.append(line.substring(matcher.end()));
                line = stringBuffer.toString();
            }
        }
        if ((style == null && m_currentStyle != null)
            || (style != null && m_currentStyle == null)
            || (style != null && m_currentStyle != null
                && ! style.equals(m_currentStyle)))
        {
            if (m_currentStyle != null)
                m_out.print("</span>");
            if (style != null)
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
        if (m_currentStyle != null)
            m_out.print("</span>");
        m_out.println("</pre>\n" +
                      "<hr style=\"margin:1em\" size=\"1\">\n" +
                      "<pre style=\"margin:1em\">");
    }

    private synchronized void printOutLine(String style, String line)
    {
        printOutLine(style, line, -1);
    }

    private String send(String command) throws GtpError
    {
        printOutLine(null, command);
        try
        {
            return m_gtp.send(command);
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
            return Double.parseDouble(send("cputime"));
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

    private String getTimeString(double seconds)
    {
        NumberFormat format1 = StringUtil.getNumberFormat(1);
        StringBuilder buffer = new StringBuilder(16);
        buffer.append(format1.format(seconds));
        buffer.append("&nbsp;(");
        buffer.append(StringUtil.formatTime((long)seconds));
        buffer.append(')');
        return buffer.toString();
    }

    private TestSummary getTotalSummary()
    {
        TestSummary total = new TestSummary();
        for (int i = 0; i < m_testSummaries.size(); ++i)
        {
            TestSummary summary = m_testSummaries.get(i);
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

    private void queryNameAndVersion() throws GtpError
    {
        try
        {
            m_name = send("name");
        }
        catch (GtpError e)
        {
            m_name = "";
            if (m_gtp.isProgramDead())
                throw e;
        }
        try
        {
            m_version = send("version");
        }
        catch (GtpError e)
        {
            m_version = "";
        }
    }

    private void runTest(String test) throws Exception
    {
        m_tests.clear();
        m_otherErrors = 0;
        m_testFile = new File(test);
        m_outName = m_outNames.get(test);
        initOutFile();
        File testFileDir = m_testFile.getAbsoluteFile().getParentFile();
        m_relativePath = FileUtil.getRelativeURI(m_outFile, testFileDir);
        if (! m_relativePath.equals("") && ! m_relativePath.endsWith("/"))
            m_relativePath = m_relativePath + "/";
        FileReader fileReader = new FileReader(m_testFile);
        BufferedReader reader = new BufferedReader(fileReader);
        try
        {
            m_gtp = new GtpClient(m_program, testFileDir, m_verbose, this);
            if (m_gtpFile != null)
                sendGtpFile();
            m_lastSgf = null;
            queryNameAndVersion();
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
            if (m_lastFullResponse != null)
            {
                handleLastResponse();
                m_lastFullResponse = null;
            }
            printOutSeparator();
            cpuTime = getCpuTime() - cpuTime;
            if (m_lastFullResponse != null)
            {
                handleLastResponse();
                m_lastFullResponse = null;
            }
            if (! m_gtp.isProgramDead())
            {
                send("quit");
                m_gtp.close();
            }
            m_gtp.waitForExit();
            finishOutFile();
            TestSummary testSummary = getTestSummary(timeMillis, cpuTime);
            m_testSummaries.add(testSummary);
            writeTestSummary(testSummary);
        }
        finally
        {
            reader.close();
        }
    }

    private void sendGtpFile() throws ErrorMessage
    {
        Reader reader;
        try
        {
            reader = new FileReader(m_gtpFile);
        }
        catch (FileNotFoundException e)
        {
            throw new ErrorMessage("GTP file not found: " + m_gtpFile);
        }
        java.io.BufferedReader in;
        in = new BufferedReader(reader);
        try
        {
            while (true)
            {
                try
                {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    if (! GtpUtil.isCommand(line))
                        continue;
                    send(line);
                }
                catch (IOException e)
                {
                    throw new ErrorMessage("Error reading GTP file: "
                                           + e.getMessage());
                }
                catch (GtpError e)
                {
                    throw new ErrorMessage("GTP command '" + e.getCommand()
                                           + "' from file " + m_gtpFile
                                           + " failed: " + e.getMessage());
                }
            }
            printOutSeparator();
        }
        finally
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
            }
        }
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
        String host = Platform.getHostInfo();
        if (withName)
            out.print("<tr><th align=\"left\">Name:</th><td>" + m_name
                      + "</td></tr>\n" +
                      "<tr><th align=\"left\">Version:</th><td>" + m_version
                      + "</td></tr>\n");
        out.print("<tr><th align=\"left\">Date:</th><td>"
                  + StringUtil.getDate()
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\">Host:</th><td>" + host
                  + "</td></tr>\n" +
                  "<tr><th align=\"left\" valign=\"top\">Command:</th>\n" +
                  "<td valign=\"top\"><tt>" + m_program
                  + "</tt></td></tr>\n");
        if (m_gtpFile != null)
            out.print("<tr><th align=\"left\">GtpFile:</th><td>" + m_gtpFile
                      + "</td></tr>\n");
    }

    /** Write text based data file with summary information. */
    private void writeData() throws FileNotFoundException
    {
        File file = new File(m_prefix + "summary.dat");
        PrintStream out = new PrintStream(file);
        NumberFormat format1 = StringUtil.getNumberFormat(1);
        TestSummary s = getTotalSummary();
        double time = ((double)s.m_timeMillis) / 1000F;
        out.print("#Tests\tFAIL\tfail\tPASS\tpass\tError\tTime\tCpuTime\n" +
                  + s.m_numberTests + "\t"
                  + s.m_unexpectedFails + "\t"
                  + s.m_expectedFails + "\t"
                  + s.m_unexpectedPasses + "\t"
                  + s.m_expectedPasses + "\t"
                  + s.m_otherErrors + "\t"
                  + format1.format(time) + "\t"
                  + format1.format(s.m_cpuTime) + "\t"
                  + "\n");
        out.close();
    }

    private void writeSummary()
        throws FileNotFoundException
    {
        File file = new File(m_prefix + "index.html");
        PrintStream out = new PrintStream(file);
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Regression Test Summary</title>\n" +
                  HtmlUtil.getMeta("gogui-regress") +
                  "<style type=\"text/css\">\n" +
                  "<!--\n" +
                  "body { margin:0; }\n" +
                  "-->\n" +
                  "</style>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + COLOR_HEADER + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>Regression Test Summary</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + COLOR_INFO
                  + "\">\n");
        writeInfo(out, true);
        out.print("</table>\n" +
                  "<table width=\"100%\" border=\"0\" cellpadding=\"0\""
                  + "cellspacing=\"1\">\n" +
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
                  "<tr bgcolor = \"" + COLOR_HEADER + "\">\n" +
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
            TestSummary summary = m_testSummaries.get(i);
            writeSummaryRow(out, summary, true, false);
        }
        writeSummaryRow(out, getTotalSummary(), true, true);
        out.print("</table>\n" +
                  HtmlUtil.getFooter("gogui-regress") +
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
                      + COLOR_HEADER + "\">\n");
        }
        else
            out.print("<tr align=\"center\" bgcolor=\""
                      + COLOR_BG_GRAY + "\">\n");
        if (withFileName)
        {
            if (foot)
                out.print("<td><b>Total</b></td>");
            else
                out.print("<td><a href=\"" + summary.m_outName + ".html" +
                          "\">" + file + "</a></td>");
        }
        double time = ((double)summary.m_timeMillis) / 1000F;
        String colorAttrUnexpectedFails = "";
        if (summary.m_unexpectedFails > 0)
            colorAttrUnexpectedFails = " bgcolor=\"" + COLOR_RED + "\"";
        String colorAttrUnexpectedPasses = "";
        if (summary.m_unexpectedPasses > 0)
            colorAttrUnexpectedPasses = " bgcolor=\"" + COLOR_GREEN + "\"";
        String colorAttrOtherErrors = "";
        if (summary.m_otherErrors > 0)
            colorAttrOtherErrors = " bgcolor=\"" + COLOR_ERROR + "\"";
        out.print("<td>" + summary.m_numberTests + "</td>\n" +
                  "<td" + colorAttrUnexpectedFails + ">"
                  + summary.m_unexpectedFails + "</td>\n" +
                  "<td>" + summary.m_expectedFails + "</td>\n" +
                  "<td" + colorAttrUnexpectedPasses + ">\n"
                  + summary.m_unexpectedPasses + "</td>\n" +
                  "<td>" + summary.m_expectedPasses + "</td>\n" +
                  "<td" + colorAttrOtherErrors + ">\n"
                  + summary.m_otherErrors + "</td>\n" +
                  "<td align=\"right\">" + getTimeString(time) + "</td>\n" +
                  "<td align=\"right\">" + getTimeString(summary.m_cpuTime) +
                  "</td>\n" +
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
        File file = new File(m_prefix + m_outName + ".html");
        PrintStream out = new PrintStream(file);
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>Summary: " + m_testFile + "</title>\n" +
                  HtmlUtil.getMeta("gogui-regress") +
                  "<style type=\"text/css\">\n" +
                  "<!--\n" +
                  "body { margin:0; }\n" +
                  "-->\n" +
                  "</style>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"blue\""
                  + " vlink=\"purple\" alink=\"red\">\n" +
                  "<table border=\"0\" width=\"100%\" bgcolor=\""
                  + COLOR_HEADER + "\">\n" +
                  "<tr><td>\n" +
                  "<h1>Summary: " + m_testFile + "</h1>\n" +
                  "</td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" bgcolor=\"" + COLOR_INFO
                  + "\">\n");
        writeInfo(out, true);
        out.print("<tr><th align=\"left\">Output:</th><td><a href=\""
                  + m_outFileRelativeName + "\">"
                  + m_outFileRelativeName + "</a></td></tr>\n" +
                  "</table>\n" +
                  "<table width=\"100%\" border=\"0\" cellpadding=\"0\""
                  + " cellspacing=\"1\">\n" +
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
                  "<tr bgcolor=\"" + COLOR_HEADER + "\">\n" +
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
                  "<table width=\"100%\" border=\"0\" cellpadding=\"0\""
                  + " cellspacing=\"1\">\n" +
                  "<thead>\n" +
                  "<tr bgcolor=\"" + COLOR_HEADER + "\">\n" +
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
            Test t = m_tests.get(i);
            String rowBackground = COLOR_BG_LIGHT;
            String statusColor = rowBackground;
            String status = null;
            if (t.m_fail && t.m_expectedFail)
            {
                status = "fail";
            }
            else if (t.m_fail && ! t.m_expectedFail)
            {
                statusColor = COLOR_RED;
                status = "FAIL";
            }
            else if (! t.m_fail && t.m_expectedFail)
            {
                statusColor = COLOR_GREEN;
                status = "PASS";
            }
            else if (! t.m_fail && ! t.m_expectedFail)
            {
                status = "pass";
            }
            else
                assert false;
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
                  HtmlUtil.getFooter("gogui-regress") +
                  "</body>\n" +
                  "</html>\n");
        out.close();
    }
}
