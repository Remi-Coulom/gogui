//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.util.*;

//----------------------------------------------------------------------------

public class MessageQueue
{
    public boolean isEmpty()
    {
        synchronized(this)
        {
            return m_queue.isEmpty();
        }
    }

    public Object getIfAvaliable()
    {
        synchronized(this)
        {
            if (m_queue.isEmpty())
                return null;
            return m_queue.remove(0);
        }
    }

    public void put(Object object)
    {
        synchronized(this)
        {
            m_queue.add(object);
            notify();
        }
    }

    public Object unsynchronizedPeek()
    {
        assert(Thread.currentThread().holdsLock(this));
        if (m_queue.isEmpty())
            return null;
        return m_queue.get(0);
    }

    public Object waitFor()
    {
        synchronized(this)
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
    }

    public Object waitFor(long timeout)
    {
        synchronized(this)
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
    }

    private Vector m_queue = new Vector();
}

//----------------------------------------------------------------------------
