//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import game.*;
import utils.*;

//----------------------------------------------------------------------------

public class Comment
    extends JScrollPane
    implements DocumentListener
{
    public interface Listener
    {
        public void changed();
    }

    public Comment(Listener listener)
    {
        m_listener = listener;
        m_textPane = new JTextPane();
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style styleMarked = doc.addStyle("marked", def);
        StyleConstants.setBackground(styleMarked,
                                     java.awt.Color.decode("#38d878"));
        StyleConstants.setForeground(styleMarked, java.awt.Color.white);
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        setPreferredSize(new Dimension(20 * fontSize, 10 * fontSize));
        m_textPane.getDocument().addDocumentListener(this);
        Set forwardSet  = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("TAB"));
        int forwardId = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
        m_textPane.setFocusTraversalKeys(forwardId, forwardSet);
        Set backwardSet  = new HashSet();
        backwardSet.add(KeyStroke.getKeyStroke("shift TAB"));
        int backwardId = KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
        m_textPane.setFocusTraversalKeys(backwardId, backwardSet);
        setViewportView(m_textPane);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void changedUpdate(DocumentEvent e) 
    {
        copyContentToNode();
    }

    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }

    public String getSelectedText()
    {
        return m_textPane.getSelectedText();
    }

    public void insertUpdate(DocumentEvent e)
    {
        copyContentToNode();
    }

    public void markAll(Pattern pattern)
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        try
        {
            CharSequence text = doc.getText(0, doc.getLength());
            Matcher matcher = pattern.matcher(text);
            while (matcher.find())
            {
                int start = matcher.start();
                int end = matcher.end();
                Style style = doc.getStyle("marked");
                doc.setCharacterAttributes(start, end - start, style, true);
            }
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }

    public void removeUpdate(DocumentEvent e)
    {
        copyContentToNode();
    }

    public void setNode(Node node)
    {
        m_node = node;
        String text = node.getComment();
        if (text == null)
            text = "";
        // setText() generates a remove and insert event, and
        // we don't want to notify the listener about that yet.
        m_duringSetText = true;
        m_textPane.setText(text);
        m_duringSetText = false;
        m_textPane.setCaretPosition(0);
        copyContentToNode();
    }

    private boolean m_duringSetText;

    private JTextPane m_textPane;

    private Listener m_listener;

    private Node m_node;

    private void copyContentToNode()
    {
        if (m_duringSetText)
            return;
        String text = m_textPane.getText().trim();
        if (m_node == null)
            return;
        String comment = m_node.getComment();
        if (comment == null)
            comment = "";
        else
            comment = comment.trim();
        if (! comment.equals(text))
        {
            m_node.setComment(text);
            m_listener.changed();
        }
    }
}
