//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyListener;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.event.CaretListener;
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
    extends JPanel
{
    public GuiTextPane(boolean fast)
    {
        super(new BorderLayout());
        if (fast || Platform.isGnuClasspath())
        {
            m_textArea = new JTextArea();
            m_textComponent = m_textArea;
            m_textArea.setLineWrap(true);
            m_textArea.setWrapStyleWord(true);
        }
        else
        {
            m_textPane = new JTextPane();
            m_textComponent = m_textPane;
        }
        add(m_textComponent);
    }

    public void addCaretListener(CaretListener listener)
    {
        m_textComponent.addCaretListener(listener);
    }

    public void addKeyListener(KeyListener listener)
    {
        m_textComponent.addKeyListener(listener);
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
            StyleConstants.setForeground(style, background);
        StyleConstants.setBold(style, bold);
        if (m_noLineSpacing)
            StyleConstants.setLineSpacing(style, 0f);
    }

    public Document getDocument()
    {
        return m_textComponent.getDocument();
    }

    public String getSelectedText()
    {
        return m_textComponent.getSelectedText();
    }

    public int getSelectionEnd()
    {
        return m_textComponent.getSelectionEnd();
    }

    public int getSelectionStart()
    {
        return m_textComponent.getSelectionStart();
    }

    Style getStyle(String name)
    {
        if (m_textPane == null)
            return null;
        return m_textPane.getStyledDocument().getStyle(name);
    }

    public String getText()
    {
        return m_textComponent.getText();
    }

    public void setCaretPosition(int position)
    {
        m_textComponent.setCaretPosition(position);
    }

    public void setEditable(boolean editable)
    {
        m_textComponent.setEditable(editable);
    }
    public void setFocusTraversalKeys(int id, Set keystrokes)
    {
        m_textComponent.setFocusTraversalKeys(id, keystrokes);
    }

    public void setMonospacedFont()
    {
        GuiUtils.setMonospacedFont(m_textComponent);
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

    public void setText(String text)
    {
        m_textComponent.setText(text);
    }

    private boolean m_noLineSpacing;

    private JTextArea m_textArea;

    private JTextComponent m_textComponent;

    private JTextPane m_textPane;
}

//----------------------------------------------------------------------------
