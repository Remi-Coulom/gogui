//----------------------------------------------------------------------------
// RegisterSpecialMacHandler.java
//----------------------------------------------------------------------------

package net.sf.gogui.specialmac;

import com.apple.eawt.*;
import net.sf.gogui.util.Platform;

class Listener
    extends ApplicationAdapter
{
    public Listener(Platform.SpecialMacHandler handler)
    {
        m_handler = handler;
    }

    public void handleAbout(ApplicationEvent event)
    {
        event.setHandled(m_handler.handleAbout());
    }

    public void handleOpenFile(ApplicationEvent event)
    {
        event.setHandled(m_handler.handleOpenFile(event.getFilename()));
    }

    public void handleQuit(ApplicationEvent event)
    {
        event.setHandled(m_handler.handleQuit());
    }

    private Platform.SpecialMacHandler m_handler;
}

/** Registration of handler for events from Mac Application menu.
    This class depends on com.apple and should therefore only
    loaded if the platform is Mac using reflection. */
public class RegisterSpecialMacHandler
{
    public RegisterSpecialMacHandler(Platform.SpecialMacHandler handler)
    {
        Application application = Application.getApplication();
        if (application == null)
        {
            // Can happen if the stub classes included in GoGui are in the
            // classpath. Should not happen in released versions.
            System.err.println(
              "Error: com.apple.eawt.Application.getApplication() returned " +
              "null.");
            return;
        }
        application.removePreferencesMenuItem();
        application.addAboutMenuItem();
        application.setEnabledAboutMenu(true);
        application.addApplicationListener(new Listener(handler));
    }
}
