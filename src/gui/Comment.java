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
    extends JPanel
    implements DocumentListener
{
    public Comment()
    {
        super(new GridLayout(1, 0));
        m_textArea = new JTextArea();
        m_textArea.setRows(5);
        m_textArea.setLineWrap(true);
        m_textArea.getDocument().addDocumentListener(this);
        Set forwardSet  = new HashSet();
        forwardSet.add(KeyStroke.getKeyStroke("TAB"));
        int forwardId = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
        m_textArea.setFocusTraversalKeys(forwardId, forwardSet);
        Set backwardSet  = new HashSet();
        backwardSet.add(KeyStroke.getKeyStroke("shift TAB"));
        int backwardId = KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
        m_textArea.setFocusTraversalKeys(backwardId, backwardSet);
        JScrollPane scrollPane
            = new JScrollPane(m_textArea,
                              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
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
        m_textArea.setText(text);
        m_textArea.setCaretPosition(0);
    }

    private JTextArea m_textArea;

    private Node m_node;

    private void copyContentToNode()
    {
        if (m_node != null)
            m_node.setComment(m_textArea.getText());
    }
}
