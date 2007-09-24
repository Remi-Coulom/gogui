//----------------------------------------------------------------------------
// GtpResponseFormatError.java
//----------------------------------------------------------------------------

package net.sf.gogui.gtp;

/** Error used if parsing a GTP response fails.
    This error is used if the response to a GTP command is expected to be
    in a particular format (e.g. a point), but is in a different format.
*/
public class GtpResponseFormatError
    extends Exception
{
    public GtpResponseFormatError(String s)
    {
        super(s);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
