//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.sgf;

import net.sf.gogui.util.ErrorMessage;

/** SGF read error. */
public class SgfError
    extends ErrorMessage
{
    /** Constructor.
        @param message Error message.
    */
    public SgfError(String message)
    {
        super(message);
    }
    
    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
