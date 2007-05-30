//----------------------------------------------------------------------------
// $Id: Komi.java 4599 2007-04-18 22:09:01Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.go;

import net.sf.gogui.util.ErrorMessage;

/** Exception thrown if parsing a komi from a string fails. */
public class InvalidKomiException
    extends ErrorMessage
{
    public InvalidKomiException(String s)
    {
        super("Invalid komi: " + s);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
