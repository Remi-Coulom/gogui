//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

import net.sf.gogui.utils.ErrorMessage;

//----------------------------------------------------------------------------

/** Exception indication the failure of a GTP command. */
public class GtpError
    extends ErrorMessage
{
    public GtpError(String s)
    {
        super(s);
    }
}    

//----------------------------------------------------------------------------
