//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

//-----------------------------------------------------------------------------

public class Platform
{
    /** Check if the platform is Mac OS X */
    public static boolean isPlatformMacOSX()
    {
        // According to the article "Tailoring Java Apllications for Mac OS X"
        // (Technical Note TN2042) it is better to check for mrj.version than
        // to parse os.name
        return (System.getProperty("mrj.version") != null);
    }
}

//-----------------------------------------------------------------------------
