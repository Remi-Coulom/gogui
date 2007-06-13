//----------------------------------------------------------------------------
// $Id: GtpRegress.java 4678 2007-06-13 16:20:26Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.gtpregress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import net.sf.gogui.util.ErrorMessage;
import net.sf.gogui.util.FileUtil;

/** Utility functions used in this package. */
public class GtpRegressUtil
{
    /** Check if files exist.
        @param list List of file names (as strings)
        @throws ErrorMessage If one of the files is not a normal file or
        not readable.
    */
    public static void checkFiles(ArrayList list) throws ErrorMessage
    {
        for (int i = 0; i < list.size(); ++i)
        {
            File file = new File((String)list.get(i));
            if (! file.exists())
                throw new ErrorMessage("File not found: " + file);
            if (! file.isFile())
                throw new ErrorMessage("Not a normal file: " + file);
            if (! file.canRead())
                throw new ErrorMessage("No read permissions: " + file);
        }
    }

    /** Expands all test suites (names starting with '@') by the tests in
        the test suite file (without the '@').
        The file is expected to be in a format as used by
        FileUtil.readStringListFromFile.
        The expansion is done recursively.
        @param list List of test names (as strings)
        @returns List of test names (as strings) with all test suites
        expanded.
    */
    public static ArrayList expandTestSuites(ArrayList list) throws IOException
    {
        while (containsTestSuite(list))
        {
            ArrayList newList = new ArrayList();
            for (int i = 0; i < list.size(); ++i)
            {
                String name = (String)list.get(i);
                if (name.startsWith("@"))
                {
                    File file = new File(name.substring(1));
                    newList.addAll(FileUtil.readStringListFromFile(file));
                }
                else
                    newList.add(name);
            }
            list = newList;
        }
        return list;
    }

    /** Make constructor unavailable; class is for namespace only. */
    private GtpRegressUtil()
    {
    }

    private static boolean containsTestSuite(ArrayList list)
    {
        for (int i = 0; i < list.size(); ++i)
        {
            String name = (String)list.get(i);
            if (name.startsWith("@"))
                return true;
        }
        return false;
    }
}
