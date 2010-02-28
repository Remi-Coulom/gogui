// SgfError.java

package net.sf.gogui.sgf;

import net.sf.gogui.util.ErrorMessage;

/** SGF read error. */
public class SgfError
    extends ErrorMessage
{
    /** Constructor.
        @param message Error message. */
    public SgfError(String message)
    {
        super(message);
    }
}
