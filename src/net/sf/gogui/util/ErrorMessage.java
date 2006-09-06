//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

//----------------------------------------------------------------------------

/** Error with error message.
    ErrorMessage are exceptions with a message meaningful for presentation
    to the user.
*/
public class ErrorMessage
    extends Exception
{
    /** Constructor.
        @param message The error message text.
    */
    public ErrorMessage(String message)
    {
        super(message);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID
}

//----------------------------------------------------------------------------
