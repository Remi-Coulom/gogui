//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

//----------------------------------------------------------------------------

/** Wait for multiple objects. */
public class WaitMultiple
{
    public WaitMultiple(Object[] objects)
    {
        synchronized (this)
        {
            m_waiters = new Waiter[objects.length];
            for (int i = 0; i < objects.length; ++i)
                m_waiters[i] = new Waiter(this, objects[i]);
        }
    }
    
    public Object waitFor()
    {
        synchronized (this)
        {
            if (m_ready == null)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException e)
                {
                    System.err.println("InterruptedException");
                }
            }
            Object ready = m_ready;
            m_ready = null;
            return ready;
        }
    }

    private void setReady(Object object)
    {
        synchronized (this)
        {
            m_ready = object;
            notify();
        }
    }

    private static class Waiter
        extends Thread
    {
        Waiter(WaitMultiple owner, Object object)
        {
            m_owner = owner;
            m_object = object;
            start();
        }
        
        public final void run()
        {
            synchronized (m_object)
            {
                while (true)
                {
                    try
                    {
                        m_object.wait();
                    }
                    catch (InterruptedException e)
                    {
                        System.err.println("InterruptedException");;
                    }
                    m_owner.setReady(m_object);
                }
            }
        }
    
        private Object m_object;

        private WaitMultiple m_owner;
    }

    private Object m_ready;

    private Waiter[] m_waiters;
}

//----------------------------------------------------------------------------
