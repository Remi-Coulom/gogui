//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.util.*;

//----------------------------------------------------------------------------

public class MessageQueue
{
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

    public void put(Object object)
    {
        synchronized(this)
        {
            m_queue.add(object);
            notify();
        }
    }

    private Vector m_queue = new Vector();
}

//----------------------------------------------------------------------------
