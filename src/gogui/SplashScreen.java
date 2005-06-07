//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gogui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;

//----------------------------------------------------------------------------
 
public class SplashScreen
    extends Frame
{
    public static final void main(String [] args)
    {
        m_splash = new SplashScreen();
        m_splash.setUndecorated(true);
        m_splash.center(0, 0);
        m_splash.setVisible(true);         
        ImageLoader loader = new ImageLoader(m_splash);
        try
        {
            Class [] mainArgs = new Class[1];
            mainArgs[0] = Class.forName("[Ljava.lang.String;");
            Class mainClass = Class.forName("gogui.Main");
            Method mainMethod = mainClass.getMethod("main", mainArgs);
            assert((mainMethod.getModifiers() & Modifier.STATIC) != 0);
            assert(mainMethod.getReturnType() == void.class); 
            Object[] objArgs = new Object[1];
            objArgs[0] = (Object)args;
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
            URL url = classLoader.getResource("images/splash.png");
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
            m_splash.center(m_image.getWidth(null), m_image.getHeight(null));
            m_splash.repaint();
        }

        private Thread m_thread;

        private SplashScreen m_splash; 
    }
     
    /** Image was loaded. */
    private static boolean m_loaded = false;

    private static Image m_image;

    private static SplashScreen m_splash;

    private static void fatalError(Exception e)
    {
        e.printStackTrace();
        System.exit(-1);
    }

    private void center(int width, int height)
    {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((size.width - width) / 2, (size.height - height) / 2,
                  width, height);
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
