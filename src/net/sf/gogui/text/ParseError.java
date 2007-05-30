//----------------------------------------------------------------------------
// $Id: GtpError.java 3542 2006-10-07 22:54:46Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.text;

import net.sf.gogui.util.ErrorMessage;

/** Exception indicating the failure of TextParser. */
public class ParseError
    extends ErrorMessage
{
    public ParseError(String s)
    {
        super(s);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
