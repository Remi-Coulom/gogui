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
{
    public Comment()
    {
        super(new GridLayout(1, 0));
        m_textArea = new JTextArea();
        m_textArea.setRows(5);
        m_textArea.setLineWrap(true);
        m_textArea.setEditable(false);
        JScrollPane scrollPane
            = new JScrollPane(m_textArea,
                              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setText(String text)
    {
        if (text == null)
            text = "";
        m_textArea.setText(text);
        m_textArea.setCaretPosition(0);
    }

    private JTextArea m_textArea;
}
