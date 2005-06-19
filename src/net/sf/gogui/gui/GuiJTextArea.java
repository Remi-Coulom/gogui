//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

//----------------------------------------------------------------------------

/** Implementation of GuiTextPane using a JTextArea for GCJ. */
class GuiJTextArea
    extends JTextArea
    implements GuiTextPane
{
    public GuiJTextArea()
    {
    }

    public void addStyle(String name, Color foreground, Color background)
    {
    }

    public JComponent getComponent()
    {
        return this;
    }

    public String getSelection()
    {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        Document doc = getDocument();
        try
        {
            return doc.getText(start, end - start);
        }
        catch (BadLocationException e)
        {
            assert(false);
            return "";
        }   
    }

    public void markAll(Pattern pattern)
    {
    }

    public void setTabFocusKeys()
    {
        // The implementation like in GuiJTextPane should work, but
        // doesn't work with GCJ on FC4 (throws runtime exception)
    }

    public void setText(String text)
    {
        super.setText(text);
        setCaretPosition(0);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}

//----------------------------------------------------------------------------
