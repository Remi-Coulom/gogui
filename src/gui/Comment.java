//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import game.*;
import go.*;
import utils.*;

//-----------------------------------------------------------------------------

class Comment
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
        m_textArea = new JTextArea();
        m_textArea.setColumns(15);
        m_textArea.setLineWrap(true);
        m_textArea.setWrapStyleWord(true);
        m_textArea.getDocument().addDocumentListener(this);
        Set forwardSet  = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("TAB"));
        int forwardId = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
        m_textArea.setFocusTraversalKeys(forwardId, forwardSet);
        Set backwardSet  = new HashSet();
        backwardSet.add(KeyStroke.getKeyStroke("shift TAB"));
        int backwardId = KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
        m_textArea.setFocusTraversalKeys(backwardId, backwardSet);
        setViewportView(m_textArea);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void changedUpdate(DocumentEvent e) 
    {
        copyContentToNode();
    }

    public void insertUpdate(DocumentEvent e)
    {
        copyContentToNode();
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
        m_textArea.setText(text);
        m_duringSetText = false;
        m_textArea.setCaretPosition(0);
        copyContentToNode();
    }

    private boolean m_duringSetText;

    private JTextArea m_textArea;

    private Listener m_listener;

    private Node m_node;

    private void copyContentToNode()
    {
        if (m_duringSetText)
            return;
        String text = m_textArea.getText().trim();
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
