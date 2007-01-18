//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.go;

public final class KomiTest
    extends junit.framework.TestCase
{
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(suite());
    }

    public static junit.framework.Test suite()
    {
        return new junit.framework.TestSuite(KomiTest.class);
    }

    public void testEquals() throws Komi.InvalidKomi
    {
        assertTrue((new Komi(0)).equals(new Komi(0)));
        assertTrue((new Komi(6.5)).equals(new Komi(6.5)));
        assertFalse((new Komi(0)).equals(new Komi(6.5)));
        assertFalse((new Komi(6.5)).equals(new Komi(6)));
        assertFalse((new Komi(6.5)).equals(new Komi(5.5)));
        assertFalse((new Komi(6.5)).equals(null));
    }

    public void testHashCode() throws Komi.InvalidKomi
    {
        assertEquals((new Komi(6.5)).hashCode(), (new Komi(6.5)).hashCode());
        assertEquals((new Komi(1)).hashCode(), (new Komi(1)).hashCode());
        assertFalse((new Komi(6.5)).hashCode() == (new Komi(0)).hashCode());
        assertFalse((new Komi(6.5)).hashCode() == (new Komi(6)).hashCode());
    }

    public void testInvalid1()
    {
        try
        {
            new Komi(-0.4);
        }
        catch (Komi.InvalidKomi e)
        {
            return;
        }
        fail();
    }

    public void testInvalid2()
    {
        try
        {
            new Komi(-10);
        }
        catch (Komi.InvalidKomi e)
        {
            return;
        }
        fail();
    }

    public void testParseKomi() throws Komi.InvalidKomi
    {
        assertEquals(new Komi(5.5), Komi.parseKomi("5.5"));
        assertEquals(new Komi(0), Komi.parseKomi("0"));
        assertNull(Komi.parseKomi(""));
    }

    public void testParseKomiInvalidNegative()
    {
        try
        {
            Komi.parseKomi("-5");
        }
        catch (Komi.InvalidKomi e)
        {
            return;
        }
        fail();
    }

    public void testParseKomiInvalidNotANumber()
    {
        try
        {
            Komi.parseKomi("foo");
        }
        catch (Komi.InvalidKomi e)
        {
            return;
        }
        fail();
    }

    public void testParseKomiInvalidTwoNumbers()
    {
        try
        {
            Komi.parseKomi("5.5 6.5");
        }
        catch (Komi.InvalidKomi e)
        {
            return;
        }
        fail();
    }

    public void testToDouble() throws Komi.InvalidKomi
    {
        double delta = 1e-10;
        assertEquals(0, (new Komi(0)).toDouble(), delta);
        assertEquals(5, (new Komi(5)).toDouble(), delta);
        assertEquals(0.5, (new Komi(0.5)).toDouble(), delta);
        assertEquals(5.5, (new Komi(5.5)).toDouble(), delta);

        // Test rounding
        assertEquals(5, (new Komi(5.1)).toDouble(), delta);
        assertEquals(5, (new Komi(4.9)).toDouble(), delta);
        assertEquals(5.5, (new Komi(5.4)).toDouble(), delta);
        assertEquals(5.5, (new Komi(5.6)).toDouble(), delta);
        assertEquals(0, (new Komi(-0.002)).toDouble(), delta);
    }

    public void testToString() throws Komi.InvalidKomi
    {
        assertEquals("0", (new Komi(0)).toString());
        assertEquals("5", (new Komi(5)).toString());
        assertEquals("0.5", (new Komi(0.5)).toString());
        assertEquals("6.5", (new Komi(6.5)).toString());

        // Test rounding
        assertEquals("6.5", (new Komi(6.4)).toString());
        assertEquals("6.5", (new Komi(6.6)).toString());
        assertEquals("5", (new Komi(4.9)).toString());
        assertEquals("5", (new Komi(5.1)).toString());
        assertEquals("0", (new Komi(-0.002)).toString());
    }
}