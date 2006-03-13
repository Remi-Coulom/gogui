//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import net.sf.gogui.utils.Platform;

//----------------------------------------------------------------------------

/** Internally uses a JTextPane or JTextArea.
    JTextArea is more lightweight and better supported in GNU Classpath 0.90,
    but does not provide support for syntax highlighting.
*/
public class GuiTextPane
{
    public GuiTextPane(boolean fast)
    {
        if (fast || Platform.isGnuClasspath())
        {
            JTextArea textArea = new JTextArea();
            m_textComponent = textArea;
            // Crashes with GNU classpath 0.90
            if (! Platform.isGnuClasspath())
            {
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
            }
            m_textPane = null;
        }
        else
        {
            m_textPane = new JTextPane();
            m_textComponent = m_textPane;
        }
    }

    public void addStyle(String name, Color foreground)
    {
        addStyle(name, foreground, null, false);
    }

    public void addStyle(String name, Color foreground, Color background,
                         boolean bold)
    {
        if (m_textPane == null)
            return;
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style style = doc.addStyle(name, def);
        if (foreground != null)
            StyleConstants.setForeground(style, foreground);
        if (background != null)
            StyleConstants.setBackground(style, background);
        StyleConstants.setBold(style, bold);
        if (m_noLineSpacing)
            StyleConstants.setLineSpacing(style, 0f);
    }

    JTextComponent get()
    {
        return m_textComponent;
    }

    Document getDocument()
    {
        return m_textComponent.getDocument();
    }

    Style getStyle(String name)
    {
        if (m_textPane == null)
            return null;
        return m_textPane.getStyledDocument().getStyle(name);
    }

    public void setNoLineSpacing()
    {
        m_noLineSpacing = true;
    }

    public void setStyle(int start, int length, String name)
    {
        if (m_textPane == null)
            return;
        StyledDocument doc = m_textPane.getStyledDocument();
        Style style;
        if (name == null)
        {
            StyleContext context = StyleContext.getDefaultStyleContext();
            style = context.getStyle(StyleContext.DEFAULT_STYLE);
        }
        else
            style = doc.getStyle(name);
        doc.setCharacterAttributes(start, length, style, true);
    }

    private boolean m_noLineSpacing;

    private final JTextComponent m_textComponent;

    private final JTextPane m_textPane;
}

//----------------------------------------------------------------------------
