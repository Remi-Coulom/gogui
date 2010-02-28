// InvalidPointException.java

package net.sf.gogui.go;

/** Thrown if parsing a string representation of a GoPoint fails. */
public class InvalidPointException
    extends Exception
{
    /** Constructor.
        @param text The text that could not be parsed as a point. */
    public InvalidPointException(String text)
    {
        super("Invalid point \"" + text + "\"");
    }
}
