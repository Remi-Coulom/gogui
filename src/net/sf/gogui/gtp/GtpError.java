//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import net.sf.gogui.util.ErrorMessage;

//----------------------------------------------------------------------------

/** Exception indication the failure of a GTP command. */
public class GtpError
    extends ErrorMessage
{
    public GtpError(String s)
    {
        super(s);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}    

//----------------------------------------------------------------------------
