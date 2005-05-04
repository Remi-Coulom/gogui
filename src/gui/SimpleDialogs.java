//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import utils.Platform;
import utils.StringUtils;

//----------------------------------------------------------------------------

/** Simple message dialogs and file selectors. */
public class SimpleDialogs
{
    public static final int FILE_OPEN = 0;

    public static final int FILE_SAVE = 1;

    public static final int FILE_SELECT = 2;

    public static void showError(Component frame, String message)
    {
        String title = "Error";
        if (frame == null)
            title = title + " - " + m_appName;
        JOptionPane.showMessageDialog(frame, message, title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    public static void showError(Component frame, String message, Exception e)
    {
        showError(frame, message + "\n" + StringUtils.printException(e));
    }

    public static void showInfo(Component frame, String message)
    {
        String title = "Info";
        if (frame == null)
            title = title + " - " + m_appName;
        JOptionPane.showMessageDialog(frame, message, title,
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public static File showOpen(Component parent, String title)
    {
        return showFileChooser(parent, FILE_OPEN, null, false, title);
    }

    public static File showOpenSgf(Component parent)
    {
        return showFileChooser(parent, FILE_OPEN, null, true, null);
    }

    public static boolean showQuestion(Component frame, String message)
    {
        String title = "Question";
        if (frame == null)
            title = title + " - " + m_appName;
        int r = JOptionPane.showConfirmDialog(frame, message, title,
                                              JOptionPane.YES_NO_OPTION);
        return (r == 0);
    }

    public static File showSaveSgf(Frame parent)
    {
        File file = showFileChooser(parent, FILE_SAVE, m_lastFile, true,
                                    null);
        if (Platform.isMac())
            // Overwrite warning is already part of FileDialog
            return file;
        while (file != null)
        {
            if (file.exists())
                if (! showQuestion(parent, "Overwrite " + file + "?"))
                {
                    file = showFileChooser(parent, FILE_SAVE, null, true,
                                           null);
                    continue;
                }
            break;
        }
        return file;
    }

    /** File selection, unknown whether for load or save. */
    public static File showSelectFile(Component parent, String title)
    {
        return showFileChooser(parent, FILE_SELECT, m_lastFile, false, title);
    }

    public static void showWarning(Component parent, String message)
    {
        String title = "Warning";
        if (parent == null)
            title = title + " - " + m_appName;
        JOptionPane.showMessageDialog(parent, message, title,
                                      JOptionPane.WARNING_MESSAGE);
    }

    public static void setLastFile(File file)
    {
        m_lastFile = file;
    }

    private static final String m_appName = "GoGui";

    private static File m_lastFile;

    private static File getUserDir()
    {
        String userDir = System.getProperties().getProperty("user.home");
        return new File(userDir);
    }

    private static File showFileChooser(Component parent, int type,
                                        File lastFile, boolean setSgfFilter,
                                        String title)
    {
        if (Platform.isMac() && parent instanceof Frame)
            return showFileChooserAWT((Frame)parent, type, lastFile,
                                      setSgfFilter, title);
        return showFileChooserSwing(parent, type, lastFile, setSgfFilter,
                                    title);
    }

    private static File showFileChooserAWT(Frame parent, int type,
                                           File lastFile,
                                           boolean setSgfFilter, String title)
    {
        if (m_lastFile == null)
            m_lastFile = getUserDir();
        FileDialog dialog = new FileDialog(parent);
        if (title == null)
        {
            switch (type)
            {
            case FILE_OPEN:
                title = "Open";
                break;
            case FILE_SAVE:
                title = "Save";
                break;
            default:
                title = "Select";
            }
        }
        dialog.setTitle(title);
        int mode = FileDialog.LOAD;
        if (type == FILE_SAVE)
            mode = FileDialog.SAVE;
        dialog.setMode(mode);
        /* Commented out, because there is no way to change the filter by the
           user (at least not on Linux)
        if (setSgfFilter)
            dialog.setFilenameFilter(new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        return name.toLowerCase().endsWith("sgf");
                    }
                });
        */
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        if (dialog.getFile() == null)
            return null;
        return new File(dialog.getDirectory(), dialog.getFile());
    }

    private static File showFileChooserSwing(Component parent, int type,
                                             File lastFile,
                                             boolean setSgfFilter,
                                             String title)
    {
        if (m_lastFile == null)
            m_lastFile = getUserDir();
        JFileChooser chooser = new JFileChooser(m_lastFile);
        chooser.setMultiSelectionEnabled(false);
        javax.swing.filechooser.FileFilter sgfFilter = new sgf.Filter();
        chooser.addChoosableFileFilter(sgfFilter);
        if (type == FILE_SAVE)
        {
            chooser.addChoosableFileFilter(new latex.Filter());
            if (lastFile != null && lastFile.isFile() && lastFile.exists())
                chooser.setSelectedFile(lastFile);
        }
        if (setSgfFilter)
            chooser.setFileFilter(sgfFilter);
        else
            chooser.setFileFilter(chooser.getAcceptAllFileFilter());
        int ret;
        switch (type)
        {
        case FILE_SAVE:
            ret = chooser.showSaveDialog(parent);
            break;
        case FILE_OPEN:
            ret = chooser.showOpenDialog(parent);
            break;
        default:
            if (title != null)
                chooser.setDialogTitle(title);
            ret = chooser.showDialog(parent, "Select");
            break;
        }
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        File file = chooser.getSelectedFile();
        m_lastFile = file;
        return file;
    }
}

//----------------------------------------------------------------------------
