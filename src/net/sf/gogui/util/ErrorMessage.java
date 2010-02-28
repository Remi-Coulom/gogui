// ErrorMessage.java

package net.sf.gogui.util;

/** Error with error message.
    ErrorMessage are exceptions with a message meaningful for presentation
    to the user. */
public class ErrorMessage
    extends Exception
{
    /** Constructor.
        @param message The error message text. */
    public ErrorMessage(String message)
    {
        super(message);
    }
}
