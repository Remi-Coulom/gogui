//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import gtp.*;
import utils.*;

//-----------------------------------------------------------------------------

class AnalyzeTextOutput
    extends JDialog
{
    public AnalyzeTextOutput(Frame owner, String title, String response,
                             boolean highlight)
    {
        super(owner, "GoGui: " + title);
        setLocationRelativeTo(owner);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(GuiUtils.createSmallEmptyBorder());
        Container contentPane = getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);
        JLabel label = new JLabel(title);
        panel.add(label, BorderLayout.NORTH);
        m_textPane = new JTextPane();
        StyledDocument doc = m_textPane.getStyledDocument();
        try
        {
            doc.insertString(0, response, null);
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        m_textPane.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        JScrollPane scrollPane = new JScrollPane(m_textPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        KeyListener keyListener = new KeyAdapter()
            {
                public void keyReleased(KeyEvent e) 
                {
                    int c = e.getKeyCode();        
                    if (c == KeyEvent.VK_ESCAPE)
                        dispose();
                }
            };
        m_textPane.addKeyListener(keyListener);
        if (highlight)
            doSyntaxHighlight();
        m_textPane.setEditable(false);
        pack();
        setVisible(true);
    }

    private JTextPane m_textPane;

    private void doSyntaxHighlight()
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style styleTitle = doc.addStyle("title", def);
        StyleConstants.setBold(styleTitle, true);
        Style stylePoint = doc.addStyle("point", def);
        StyleConstants.setForeground(stylePoint, new Color(0.25f, 0.5f, 0.7f));
        Style styleNumber = doc.addStyle("number", def);
        StyleConstants.setForeground(styleNumber, new Color(0f, 0.54f, 0f));
        Style styleConst = doc.addStyle("const", def);
        StyleConstants.setForeground(styleConst, new Color(0.8f, 0f, 0f));
        Style styleColor = doc.addStyle("color", def);
        StyleConstants.setForeground(styleColor, new Color(0.54f, 0f, 0.54f));
        m_textPane.setEditable(true);
        highlight("number", "\\b-?[0-9]+\\.?+[0-9]*\\b");
        highlight("const", "\\b[A-Z_][A-Z_]+[A-Z]\\b");
        highlight("color",
                  "\\b([Bb][Ll][Aa][Cc][Kk]|[Ww][Hh][Ii][Tt][Ee])\\b");
        highlight("point", "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1[0-9]|[1-9]))\\b");
        highlight("title", "^\\S+:\\s*$");
        m_textPane.setEditable(false);
    }

    private void highlight(String style, String regex)
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        try
        {
            Matcher matcher = pattern.matcher(doc.getText(0, doc.getLength()));
            while (matcher.find())
            {
                int start = matcher.start();
                int end = matcher.end();
                doc.setCharacterAttributes(start, end - start,
                                           doc.getStyle(style), true);
            }
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//-----------------------------------------------------------------------------
