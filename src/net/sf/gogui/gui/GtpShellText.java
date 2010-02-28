// GtpShellText.java

package net.sf.gogui.gui;

import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import net.sf.gogui.game.Clock;

class GtpShellText
    extends JTextPane
{
    public GtpShellText(int historyMin, int historyMax, boolean timeStamp)
    {
        GuiUtil.setMonospacedFont(this);
        m_startTime = System.currentTimeMillis();
        m_timeStamp = timeStamp;
        m_historyMin = historyMin;
        m_historyMax = historyMax;
        GuiUtil.addStyle(this, "error", Color.red);
        GuiUtil.addStyle(this, "warning", Color.decode("#ff7000"));
        GuiUtil.addStyle(this, "output", null, null, true);
        GuiUtil.addStyle(this, "log", new Color(0.5f, 0.5f, 0.5f));
        GuiUtil.addStyle(this, "livegfx", Color.decode("#5498B0"));
        GuiUtil.addStyle(this, "time", new Color(0, 0, 0.5f));
        GuiUtil.addStyle(this, "invalid", new Color(1.0f, 0.58f, 0.25f));
        setEditable(false);
    }

    public void appendComment(String text)
    {
        m_isLastTextNonGTP = false;
        appendText(text, "log");
    }

    public void appendError(String text)
    {
        m_isLastTextNonGTP = false;
        appendTimeStamp();
        appendText(text, "error");
    }

    public void appendInput(String text)
    {
        m_isLastTextNonGTP = false;
        appendTimeStamp();
        appendText(text, null);
    }

    public void appendInvalidResponse(String text)
    {
        m_isLastTextNonGTP = true;
        appendText(text, "invalid");
    }

    public void appendLog(String text, boolean isLiveGfx, boolean isWarning)
    {
        m_isLastTextNonGTP = true;
        if (isLiveGfx)
            appendText(text, "livegfx");
        else if (isWarning)
            appendText(text, "warning");
        else
            appendText(text, "log");
    }

    public void appendOutput(String text)
    {
        m_isLastTextNonGTP = false;
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
            assert false;
            return "";
        }
    }

    /** Check if last text appended is not part of the GTP streams.
        Returns true, if last text is standard error of program or invalid
        response lines. */
    public boolean isLastTextNonGTP()
    {
        return m_isLastTextNonGTP;
    }

    public void setPositionToEnd()
    {
        int length = getDocument().getLength();
        setCaretPosition(length);
    }

    public void setTimeStamp(boolean enable)
    {
        m_timeStamp = enable;
    }

    private boolean m_isLastTextNonGTP;

    private boolean m_timeStamp;

    private final int m_historyMin;

    private final int m_historyMax;

    private int m_lines;

    private int m_truncated;

    private final long m_startTime;

    private void appendText(String text, String style)
    {
        assert SwingUtilities.isEventDispatchThread();
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
            assert false;
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
            assert truncateIndex != -1;
            doc.remove(0, truncateIndex);
            m_lines -= truncateLines;
            m_truncated += truncateLines;
        }
        catch (BadLocationException e)
        {
            assert false;
        }
    }
}
