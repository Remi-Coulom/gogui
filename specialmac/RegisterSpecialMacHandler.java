//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package specialmac;

import com.apple.eawt.*;
import utils.Platform;

//----------------------------------------------------------------------------

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

    public void handleQuit(ApplicationEvent event)
    {
        event.setHandled(m_handler.handleQuit());
    }
}

//----------------------------------------------------------------------------

/** Registration of handler for events from Mac Application menu.
    This class depends on com.apple and should therefore only
    loaded if the platform is Mac using reflection.
 */
public class RegisterSpecialMacHandler
{
    public RegisterSpecialMacHandler(Platform.SpecialMacHandler handler)
    {
        m_handler = handler;
        Application application = Application.getApplication();
        application.addApplicationListener(new Listener(handler));
    }
}

//----------------------------------------------------------------------------
