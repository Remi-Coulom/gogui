package com.apple.eawt;

import java.util.EventObject;

/** Stub class for compiling net.sf.gogui.specialmac. */
public class ApplicationEvent
    extends EventObject
{
    ApplicationEvent(Object source)
    {
        super(source);
    }

    public String getFilename()
    {
        return null;
    }

    public void setHandled(boolean state)
    {
    }
}
