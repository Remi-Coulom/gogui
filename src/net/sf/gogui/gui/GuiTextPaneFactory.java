//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

//----------------------------------------------------------------------------

/** See GuiTextPane. */
public class GuiTextPaneFactory
{
    public static GuiTextPane create()
    {
        String prop = System.getProperty("gogui.no-jtextpane", "false");
        if (prop.equals("false"))
            return new GuiJTextPane();
        else
            return new GuiJTextArea();
    }
}

//----------------------------------------------------------------------------
