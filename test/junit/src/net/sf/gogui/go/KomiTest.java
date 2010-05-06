// KomiTest.java

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

    public void testEquals() throws InvalidKomiException
    {
        assertTrue((new Komi(0)).equals(new Komi(0)));
        assertTrue((new Komi(6.5)).equals(new Komi(6.5)));
        assertFalse((new Komi(0)).equals(new Komi(6.5)));
        assertFalse((new Komi(6.5)).equals(new Komi(6)));
        assertFalse((new Komi(6.5)).equals(new Komi(5.5)));
        assertFalse((new Komi(6.5)).equals(null));
    }

    public void testHashCode() throws InvalidKomiException
    {
        assertEquals((new Komi(6.5)).hashCode(), (new Komi(6.5)).hashCode());
        assertEquals((new Komi(1)).hashCode(), (new Komi(1)).hashCode());
        assertFalse((new Komi(6.5)).hashCode() == (new Komi(0)).hashCode());
        assertFalse((new Komi(6.5)).hashCode() == (new Komi(6)).hashCode());
    }

    public void testParseKomi() throws InvalidKomiException
    {
        assertEquals(new Komi(5.5), Komi.parseKomi("5.5"));
        assertEquals(new Komi(5.5), Komi.parseKomi("5,5"));
        assertEquals(new Komi(0), Komi.parseKomi("0"));
        assertNull(Komi.parseKomi(""));
    }

    public void testParseKomiInvalidNotANumber()
    {
        try
        {
            Komi.parseKomi("foo");
        }
        catch (InvalidKomiException e)
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
        catch (InvalidKomiException e)
        {
            return;
        }
        fail();
    }

    public void testToDouble() throws InvalidKomiException
    {
        double delta = 1e-10;
        assertEquals(-0.5, (new Komi(-0.5)).toDouble(), delta);
        assertEquals(-2, (new Komi(-2)).toDouble(), delta);
        assertEquals(0, (new Komi(0)).toDouble(), delta);
        assertEquals(5, (new Komi(5)).toDouble(), delta);
        assertEquals(0.5, (new Komi(0.5)).toDouble(), delta);
        assertEquals(5.5, (new Komi(5.5)).toDouble(), delta);
        assertEquals(5.75, (new Komi(5.75)).toDouble(), delta);
    }

    public void testToString() throws InvalidKomiException
    {
        assertEquals("-5", (new Komi(-5)).toString());
        assertEquals("-5.5", (new Komi(-5.5)).toString());
        assertEquals("-1", (new Komi(-1)).toString());
        assertEquals("-0.5", (new Komi(-0.5)).toString());
        assertEquals("0", (new Komi(0)).toString());
        assertEquals("5", (new Komi(5)).toString());
        assertEquals("0.5", (new Komi(0.5)).toString());
        assertEquals("6.5", (new Komi(6.5)).toString());
        assertEquals("6.25", (new Komi(6.25)).toString());
    }
}