//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import sgf.Filter;

//-----------------------------------------------------------------------------

class SimpleDialogs
{
    public static void showAbout(Component frame, String message)
    {
        JOptionPane.showMessageDialog(frame, message,
                                      m_titlePrefix + "About",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component frame, String message)
    {
        JOptionPane.showMessageDialog(frame, message,
                                      m_titlePrefix + "Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component frame, String message)
    {
        JOptionPane.showMessageDialog(frame, message,
                                      m_titlePrefix + "Info",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public static File showOpen(Frame parent, String title)
    {
        String dir = System.getProperties().getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(dir);
        chooser.setDialogTitle(title);
        chooser.setMultiSelectionEnabled(false);
        int ret = chooser.showOpenDialog(parent);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        return chooser.getSelectedFile();
    }

    public static File showOpenSgf(Component frame)
    {
        return showSgfFileChooser(frame, false);
    }

    public static boolean showQuestion(Component frame, String message)
    {
        int r = JOptionPane.showConfirmDialog(frame, message,
                                              m_titlePrefix + "Question",
                                              JOptionPane.YES_NO_OPTION);
        return (r == 0);
    }

    public static File showSaveSgf(Component frame)
    {
        return showSgfFileChooser(frame, true);
    }

    public static void showWarning(Component frame, String message)
    {
        JOptionPane.showMessageDialog(frame, message,
                                      m_titlePrefix + "Warning",
                                      JOptionPane.WARNING_MESSAGE);
    }

    private static final String m_titlePrefix = "GoGui: ";
    private static String m_lastFile;

    private static File showSgfFileChooser(Component frame, boolean saveDialog)
    {
        if (m_lastFile == null)
            m_lastFile = System.getProperties().getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(m_lastFile);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new Filter());
        int ret;
        if (saveDialog)
            ret = chooser.showSaveDialog(frame);
        else
            ret = chooser.showOpenDialog(frame);
        if (ret != JFileChooser.APPROVE_OPTION)
            return null;
        File file = chooser.getSelectedFile();
        m_lastFile = file.toString();
        return file;
    }
}

//-----------------------------------------------------------------------------
