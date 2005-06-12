//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.utils;

//----------------------------------------------------------------------------

/** Error with error message.
    ErrorMessage are exceptions with a message meaningful for presentation
    to the user.
*/
public class ErrorMessage
    extends Exception
{
    public ErrorMessage(String s)
    {
        super(s);
    }
}

//----------------------------------------------------------------------------
