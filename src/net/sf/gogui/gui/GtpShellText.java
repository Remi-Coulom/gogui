//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import net.sf.gogui.game.Clock;

class GtpShellText
    extends GuiTextPane
{
    public GtpShellText(int historyMin, int historyMax, boolean timeStamp)
    {
        GuiUtil.setMonospacedFont(get());
        m_startTime = System.currentTimeMillis();
        m_timeStamp = timeStamp;
        m_historyMin = historyMin;
        m_historyMax = historyMax;
        setNoLineSpacing();
        addStyle("error", Color.red);
        addStyle("output", null, null, true);
        addStyle("log", new Color(0.5f, 0.5f, 0.5f));
        addStyle("time", new Color(0, 0, 0.5f));
        addStyle("invalid", Color.white, Color.red, false);
        get().setEditable(false);
    }

    public void appendComment(String text)
    {
        appendText(text, "log");
    }

    public void appendError(String text)
    {
        appendTimeStamp();
        appendText(text, "error");
    }

    public void appendInput(String text)
    {
        appendTimeStamp();
        appendText(text, null);
    }

    public void appendInvalidResponse(String text)
    {
        appendText(text, "invalid");
    }

    public void appendLog(String text)
    {
        appendText(text, "log");
    }

    public void appendOutput(String text)
    {
        appendTimeStamp();
        appendText(text, "output");
    }

    public static int findTruncateIndex(String text, int truncateLines)
    {
        int indexNewLine = 0;
        int lines = 0;
        while ((indexNewLine = text.indexOf('\n', indexNewLine)) != -1)
        {
            ++indexNewLine;
            ++lines;
            if (lines == truncateLines)
                return indexNewLine;
        }
        return -1;
    }

    public int getLinesTruncated()
    {
        return m_truncated;
    }

    public String getLog()
    {
        Document doc = getDocument();
        try
        {
            return doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            assert(false);
            return "";
        }
    }

    public void setPositionToEnd()
    {
        int length = getDocument().getLength();
        get().setCaretPosition(length);
    }

    public void setTimeStamp(boolean enable)
    {
        m_timeStamp = enable;
    }

    private boolean m_timeStamp;

    private final int m_historyMin;

    private final int m_historyMax;

    private int m_lines;

    private int m_truncated;

    private final long m_startTime;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private void appendText(String text, String style)
    {
        assert(SwingUtilities.isEventDispatchThread());
        if (text.equals(""))
            return;
        int indexNewLine = 0;
        while ((indexNewLine = text.indexOf('\n', indexNewLine)) != -1)
        {
            ++m_lines;
            ++indexNewLine;
        }
        Document doc = getDocument();
        Style s = null;
        if (style != null)
            s = getStyle(style);
        try
        {
            int length = doc.getLength();
            doc.insertString(length, text, s);
            setPositionToEnd();
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        if (m_lines > m_historyMax)
        {
            truncateHistory();
            setPositionToEnd();
        }
    }

    private void appendTimeStamp()
    {
        if (! m_timeStamp)
            return;
        long timeMillis = System.currentTimeMillis();
        double diff = (float)(timeMillis - m_startTime) / 1000;
        appendText(Clock.getTimeString(diff, -1) + " ", "time");
    }

    private void truncateHistory()
    {
        int truncateLines = m_lines - m_historyMin;
        Document doc = getDocument();
        try
        {
            String text = doc.getText(0, doc.getLength());
            int truncateIndex = findTruncateIndex(text, truncateLines);
            assert(truncateIndex != -1);
            doc.remove(0, truncateIndex);
            m_lines -= truncateLines;
            m_truncated += truncateLines;
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}
