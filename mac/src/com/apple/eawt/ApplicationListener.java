package com.apple.eawt;

import java.util.EventListener;

/** Stub class for compiling net.sf.gogui.specialmac. */
public interface ApplicationListener
    extends EventListener
{
    public void handleAbout(ApplicationEvent event);

    public void handleOpenFile(ApplicationEvent event);

    public void handleQuit(ApplicationEvent event);
}
