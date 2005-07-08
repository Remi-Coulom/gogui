//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import net.sf.gogui.sgf.SgfFilter;
import net.sf.gogui.tex.TexFilter;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.StringUtils;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

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

    public static File showSave(Component parent, String title)
    {
        return showFileChooserSave(parent, null, false, title);
    }

    public static File showSaveSgf(Frame parent)
    {
        return showFileChooserSave(parent, m_lastFile, true, null);
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

    /** Make constructor unavailable; class is for namespace only. */
    private SimpleDialogs()
    {
    }

    private static File getUserDir()
    {
        String userDir = System.getProperties().getProperty("user.home");
        return new File(userDir);
    }

    /** Find first parent that is a Frame.
        @return null If no such parent.
    */
    private static Frame findParentFrame(Component component)
    {
        while (component != null)
            if (component instanceof Frame)
                return (Frame)component;
            else
                component = component.getParent();
        return null;
    }

    private static File showFileChooser(Component parent, int type,
                                        File lastFile, boolean setSgfFilter,
                                        String title)
    {
        if (Platform.isMac())
        {
            Frame frame = findParentFrame(parent);
            return showFileChooserAWT(frame, type, setSgfFilter, title);
        }
        return showFileChooserSwing(parent, type, lastFile, setSgfFilter,
                                    title);
    }

    private static File showFileChooserSave(Component parent,
                                            File lastFile,
                                            boolean setSgfFilter,
                                            String title)
    {
        File file = showFileChooser(parent, FILE_SAVE, lastFile, setSgfFilter,
                                    title);
        if (Platform.isMac())
            // Overwrite warning is already part of FileDialog
            return file;
        while (file != null)
        {
            if (file.exists()
                && ! showQuestion(parent, "Overwrite " + file + "?"))
            {
                file = showFileChooser(parent, FILE_SAVE, lastFile,
                                       setSgfFilter, title);
                continue;
            }
            break;
        }
        return file;
    }

    private static File showFileChooserAWT(Frame parent, int type,
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
        javax.swing.filechooser.FileFilter sgfFilter = new SgfFilter();
        chooser.addChoosableFileFilter(sgfFilter);
        if (type == FILE_SAVE)
        {
            chooser.addChoosableFileFilter(new TexFilter());
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
