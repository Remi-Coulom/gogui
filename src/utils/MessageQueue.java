//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.util.Vector;

//----------------------------------------------------------------------------

/** Message queue for synchronized passing of messages between threads. */
public class MessageQueue
{
    public synchronized boolean isEmpty()
    {
        return m_queue.isEmpty();
    }

    public synchronized Object getIfAvaliable()
    {
        if (m_queue.isEmpty())
            return null;
        return m_queue.remove(0);
    }

    public synchronized int getSize()
    {
        return m_queue.size();
    }

    public synchronized void put(Object object)
    {
        m_queue.add(object);
        notifyAll();
    }

    /** Unsynchronized peek at next object.
        Requires that the caller holds a lock on the message queue.
        @return Next object or null if none exists.
    */
    public Object unsynchronizedPeek()
    {
        assert(Thread.currentThread().holdsLock(this));
        if (m_queue.isEmpty())
            return null;
        return m_queue.get(0);
    }

    public synchronized Object waitFor()
    {
        if (m_queue.isEmpty())
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }
        assert(! m_queue.isEmpty());
        return m_queue.remove(0);
    }

    public synchronized Object waitFor(long timeout)
    {
        if (m_queue.isEmpty())
        {
            try
            {
                wait(timeout);
            }
            catch (InterruptedException e)
            {
            }
        }
        if (m_queue.isEmpty())
            return null;
        return m_queue.remove(0);
    }

    private Vector m_queue = new Vector();
}

//----------------------------------------------------------------------------
