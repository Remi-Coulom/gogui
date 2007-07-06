//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.util.ArrayList;

/** Message queue for synchronized passing of messages between threads.
    @bug Queue has no limit on capacity; replace with
    java.util.concurrent.BlockingQueue once it is available in GNU classpath
*/
public class MessageQueue
{
    public boolean isEmpty()
    {
        synchronized (m_mutex)
        {
            return m_queue.isEmpty();
        }
    }

    public Object getIfAvaliable()
    {
        synchronized (m_mutex)
        {
            if (m_queue.isEmpty())
                return null;
            return m_queue.remove(0);
        }
    }

    /** Get mutex for locking from outside.
        Locking from outside is necessary if unsynchronizedPeek is used.
    */
    public Object getMutex()
    {
        return m_mutex;
    }

    public int getSize()
    {
        synchronized (m_mutex)
        {
            return m_queue.size();
        }
    }

    public void put(Object object)
    {
        synchronized (m_mutex)
        {
            m_queue.add(object);
            m_mutex.notifyAll();
        }
    }

    /** Unsynchronized peek at next object.
        Requires that the caller holds a lock on the message queue.
        @return Next object or null if none exists.
    */
    public Object unsynchronizedPeek()
    {
        assert Thread.holdsLock(m_mutex);
        if (m_queue.isEmpty())
            return null;
        return m_queue.get(0);
    }

    public Object waitFor()
    {
        synchronized (m_mutex)
        {
            if (m_queue.isEmpty())
            {
                try
                {
                    m_mutex.wait();
                }
                catch (InterruptedException e)
                {
                }
            }
            assert ! m_queue.isEmpty();
            return m_queue.remove(0);
        }
    }

    public Object waitFor(long timeout)
    {
        synchronized (m_mutex)
        {
            if (m_queue.isEmpty())
            {
                try
                {
                    m_mutex.wait(timeout);
                }
                catch (InterruptedException e)
                {
                }
            }
            if (m_queue.isEmpty())
                return null;
            return m_queue.remove(0);
        }
    }

    private final ArrayList<Object> m_queue = new ArrayList<Object>();

    private final Object m_mutex = new Object();
}
