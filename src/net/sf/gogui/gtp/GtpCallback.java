// GtpCallback.java

package net.sf.gogui.gtp;

/** Callback function for a command.
    It should throw a GtpError for creating a failure response,
    and write the response into the string buffer of the GtpCommand. */
public interface GtpCallback
{
    void run(GtpCommand cmd) throws GtpError;
}
