//----------------------------------------------------------------------------
// $Id: GoPoint.java 4607 2007-04-19 22:16:44Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.go;

/** Thrown if parsing a string representation of a GoPoint fails. */
public class InvalidPointException
    extends Exception
{
    /** Constructor.
        @param text The text that could not be parsed as a point.
    */
    public InvalidPointException(String text)
    {
        super("Invalid point \"" + text + "\"");
    }
    
    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sf.net
    */
    private static final long serialVersionUID = 0L; // SUID
}
