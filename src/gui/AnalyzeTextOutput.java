//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import utils.*;

//----------------------------------------------------------------------------

public class AnalyzeTextOutput
    extends JDialog
{
    static public interface Listener
    {
        /** Callback if some text is selected.
            If text is unselected again this function will be called
            with the complete text content of the window.
        */
        public void textSelected(String text);
    }

    public AnalyzeTextOutput(Frame owner, String title, String response,
                             boolean highlight, Listener listener)
    {
        super(owner, title);
        setLocationRelativeTo(owner);
        m_listener = listener;
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
        CaretListener caretListener = new CaretListener()
            {
                public void caretUpdate(CaretEvent event)
                {
                    if (m_listener == null)
                        return;
                    int start = m_textPane.getSelectionStart();
                    int end = m_textPane.getSelectionEnd();
                    StyledDocument doc = m_textPane.getStyledDocument();
                    try
                    {
                        if (start == end)
                        {
                            String text = doc.getText(0, doc.getLength());
                            m_listener.textSelected(text);
                            return;
                        }
                        String text = doc.getText(start, end - start);
                        m_listener.textSelected(text);
                    }
                    catch (BadLocationException e)
                    {
                        assert(false);
                    }   
                }
            };
        m_textPane.addCaretListener(caretListener);
        if (highlight)
            doSyntaxHighlight();
        m_textPane.setCaretPosition(0);
        m_textPane.setEditable(false);
        pack();
        setVisible(true);
    }

    private JTextPane m_textPane;

    private Listener m_listener;

    private void doSyntaxHighlight()
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style styleTitle = doc.addStyle("title", def);
        StyleConstants.setBold(styleTitle, true);
        Style stylePoint = doc.addStyle("point", def);
        Color colorPoint = new Color(0.25f, 0.5f, 0.7f);
        StyleConstants.setForeground(stylePoint, colorPoint);
        Style styleNumber = doc.addStyle("number", def);
        Color colorNumber = new Color(0f, 0.54f, 0f);
        StyleConstants.setForeground(styleNumber, colorNumber);
        Style styleConst = doc.addStyle("const", def);
        Color colorConst = new Color(0.8f, 0f, 0f);
        StyleConstants.setForeground(styleConst, colorConst);
        Style styleColor = doc.addStyle("color", def);
        Color colorColor = new Color(0.54f, 0f, 0.54f);
        StyleConstants.setForeground(styleColor, colorColor);
        m_textPane.setEditable(true);
        highlight("number", "\\b-?\\d+\\.?\\d*\\b");
        highlight("const", "\\b[A-Z_][A-Z_]+[A-Z]\\b");
        highlight("color",
                  "\\b([Bb][Ll][Aa][Cc][Kk]|[Ww][Hh][Ii][Tt][Ee])\\b");
        highlight("point", "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1\\d|[1-9]))\\b");
        highlight("title", "^\\S+:(\\s|$)");
        m_textPane.setEditable(false);
    }

    private void highlight(String styleName, String regex)
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        try
        {
            CharSequence text = doc.getText(0, doc.getLength());
            Matcher matcher = pattern.matcher(text);
            while (matcher.find())
            {
                int start = matcher.start();
                int end = matcher.end();
                Style style = doc.getStyle(styleName);
                doc.setCharacterAttributes(start, end - start, style, true);
            }
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//----------------------------------------------------------------------------
