//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import net.sf.gogui.gogui.GoGuiSettings;
import net.sf.gogui.utils.ErrorMessage;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;

//----------------------------------------------------------------------------
 
/** Wrapper for starting GoGui with a splash screen.
    Loads the GoGui class with the reflection API to mimimize the time until
    the splash screen is shown.
*/
public class SplashScreen
    extends Frame
{
    public static final void main(String [] args)
    {
        // Setting these Mac system properties here worked with older versions
        // of Mac Java, for newer ones, they have to be set when starting the
        // VM (see options in scripts and mac/Info.plist)
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "GoGui");
        System.setProperty("apple.awt.brushMetalLook", "true");
        GoGuiSettings settings;
        try
        {
            settings = new GoGuiSettings(args);
            if (settings.m_noStartup)
                return;
        }
        catch (ErrorMessage e)
        {
            System.err.println(e.getMessage());
            return;
        }
        m_splash = new SplashScreen();
        m_splash.setUndecorated(true);
        center(m_splash, 0, 0);
        m_splash.setVisible(true);         
        new ImageLoader(m_splash);
        try
        {
            Class [] mainArgs = new Class[1];
            mainArgs[0] = Class.forName("net.sf.gogui.gogui.GoGuiSettings");
            Class mainClass = Class.forName("net.sf.gogui.gogui.Main");
            Method mainMethod = mainClass.getMethod("main", mainArgs);
            assert((mainMethod.getModifiers() & Modifier.STATIC) != 0);
            assert(mainMethod.getReturnType() == void.class); 
            Object[] objArgs = new Object[1];
            objArgs[0] = settings;
            mainMethod.invoke(null, objArgs);
        }
        catch(Exception e)
        {
            fatalError(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void close()
    {
        if (m_splash != null)
            m_splash.setVisible(false);
        m_splash = null;
    }

    public void paint(Graphics graphics)
    {
        if (m_loaded)
            graphics.drawImage(m_image, 0, 0, null);
    }
 
    public void update(Graphics graphics)
    {
        paint(graphics);
    }
 
    private static class ImageLoader
        implements Runnable
    {
        public ImageLoader(SplashScreen splash)
        {
            m_splash = splash;
            m_thread = new Thread(this);
            m_thread.start();
        }
         
        public void run()
        {
            ClassLoader classLoader = getClass().getClassLoader();
            URL url =
                classLoader.getResource("net/sf/gogui/images/splash.png");
            m_image = Toolkit.getDefaultToolkit().createImage(url);
            MediaTracker tracker = new MediaTracker(m_splash);
            tracker.addImage(m_image, 0);
            try
            {
                tracker.waitForID(0);
            }
            catch (InterruptedException e)
            {
                fatalError(e);
            }        
            if (tracker.isErrorID(0))
            {
                printError("Error loading image");
                return;
            }
            m_loaded = true;
            center(m_splash, 400, 300);
            m_splash.repaint();
        }

        private final Thread m_thread;

        private final SplashScreen m_splash; 
    }
     
    /** Image was loaded. */
    private static boolean m_loaded = false;

    private static Image m_image;

    private static SplashScreen m_splash;

    private static void center(Component component, int width, int height)
    {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        component.setBounds((size.width - width) / 2,
                            (size.height - height) / 2,
                            width, height);
    }

    private static void fatalError(Exception e)
    {
        e.printStackTrace();
        System.exit(-1);
    }

    private static void fatalError(String message)
    {
        printError(message);
        System.exit(-1);
    }
 
    private static void printError(String message)
    {
        System.err.println("SplashScreen: " + message);
    }
 
}

//----------------------------------------------------------------------------
