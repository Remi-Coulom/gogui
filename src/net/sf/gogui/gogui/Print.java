//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Component;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import net.sf.gogui.gui.SimpleDialogs;

//----------------------------------------------------------------------------

/** Print a printable. */
public class Print
{
    public static void run(Component parent, Printable printable)
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(printable);
        if (! job.printDialog())
            return;
        try
        {
            job.print();
        }
        catch (Exception e)
        {
            SimpleDialogs.showError(parent, "Printing failed");
        }
        SimpleDialogs.showInfo(parent, "Printing done");
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Print()
    {
    }
}

//----------------------------------------------------------------------------
