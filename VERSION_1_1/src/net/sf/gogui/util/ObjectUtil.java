// ObjectUtil.java

package net.sf.gogui.util;

/** Utils for using class java.lang.Object. */
public final class ObjectUtil
{
    /** Compare including the case that arguments can be null. */
    public static boolean equals(Object object1, Object object2)
    {
        if (object1 == null && object2 == null)
            return true;
        if (object1 == null && object2 != null)
            return false;
        if (object1 != null && object2 == null)
            return false;
        assert object1 != null && object2 != null;
        return object1.equals(object2);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private ObjectUtil()
    {
    }
}
