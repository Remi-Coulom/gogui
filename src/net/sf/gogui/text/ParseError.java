// ParseError.java

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
}
