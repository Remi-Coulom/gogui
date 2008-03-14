// Print.java

package net.sf.gogui.gogui;

import java.awt.Component;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import net.sf.gogui.gui.MessageDialogs;
import static net.sf.gogui.gogui.I18n.i18n;

/** Print a printable. */
public final class Print
{
    public static void run(Component parent, Printable printable,
                           MessageDialogs messageDialogs)
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
            messageDialogs.showError(parent, i18n("MSG_PRINT_FAIL"), "");
        }
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Print()
    {
    }
}
