//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.event.CaretListener;
import javax.swing.text.Document;

//----------------------------------------------------------------------------

/** Component for displaying text.
    This is a workaround for the incomplete implementation of JTextPane of
    GCJ on FC4. If the system property "gogui.no-jtextpane" is set
    an implementation of GuiTextPane based on JTextArea is used and styles
    are ignored.
*/
public interface GuiTextPane
{
    public abstract void addCaretListener(CaretListener listener);

    public abstract void addStyle(String name, Color foreground,
                                  Color background);

    public abstract JComponent getComponent();

    public abstract Document getDocument();

    public abstract String getSelection();

    public abstract String getText();

    public abstract void markAll(Pattern pattern);

    public abstract void setTabFocusKeys();

    public abstract void setText(String text);
}

//----------------------------------------------------------------------------
