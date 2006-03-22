//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.sf.gogui.sgf.SgfFilter;
import net.sf.gogui.thumbnail.Thumbnail;
import net.sf.gogui.utils.Platform;
import net.sf.gogui.utils.StringUtils;

//----------------------------------------------------------------------------

/** Simple message dialogs and file selectors. */
public final class SimpleDialogs
{
    /** Dialog type for opening a file. */
    public static final int FILE_OPEN = 0;

    /** Dialog type for saving to a file. */
    public static final int FILE_SAVE = 1;

    /** Dialog type for selecting a file.
        Use this type, if a file name should be selected, but it is not known
        what the file name is used for and if the file already exists.
    */
    public static final int FILE_SELECT = 2;

    public static void showError(Component frame, String message)
    {
        String title = "Error";
        if (frame == null)
            title = title + " - " + APP_NAME;
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
            title = title + " - " + APP_NAME;
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
            title = title + " - " + APP_NAME;
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
        return showFileChooserSave(parent, s_lastFile, true, null);
    }

    /** File selection, unknown whether for load or save. */
    public static File showSelectFile(Component parent, String title)
    {
        return showFileChooser(parent, FILE_SELECT, s_lastFile, false, title);
    }

    public static void showWarning(Component parent, String message)
    {
        String title = "Warning";
        if (parent == null)
            title = title + " - " + APP_NAME;
        JOptionPane.showMessageDialog(parent, message, title,
                                      JOptionPane.WARNING_MESSAGE);
    }

    public static void setLastFile(File file)
    {
        s_lastFile = file;
    }

    private static final String APP_NAME = "GoGui";

    private static File s_lastFile;

    /** Make constructor unavailable; class is for namespace only. */
    private SimpleDialogs()
    {
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
        if (Platform.isMac() && type != FILE_SELECT)
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
                assert(false);
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
        JFileChooser chooser;
        if (s_lastFile == null)
        {
            if (Platform.isMac())
                // user.dir is application directory on Mac, which is bad
                // I have not found a way to set it to user home in Info.plist
                // so I use null here, which sets is to the user home
                chooser = new JFileChooser((String)null);
            else
                chooser = new JFileChooser(System.getProperty("user.dir"));
        }
        else
            chooser = new JFileChooser(s_lastFile);
        chooser.setMultiSelectionEnabled(false);
        javax.swing.filechooser.FileFilter sgfFilter = new SgfFilter();
        chooser.addChoosableFileFilter(sgfFilter);
        if (type == FILE_SAVE)
        {
            if (lastFile != null && lastFile.isFile() && lastFile.exists())
                chooser.setSelectedFile(lastFile);
        }
        if (setSgfFilter)
        {
            chooser.setFileFilter(sgfFilter);
            // Thumbnail creation does not work on GNU classpath 0.90 yet
            if (Thumbnail.checkThumbnailSupport()
                && ! Platform.isGnuClasspath())
            {
                SgfPreview preview = new SgfPreview();
                chooser.setAccessory(preview);
                chooser.addPropertyChangeListener(preview);
            }
        }
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
        s_lastFile = file;
        return file;
    }
}

//----------------------------------------------------------------------------

class SgfPreview
    extends JPanel
    implements PropertyChangeListener
{    
    public SgfPreview()
    {
        setLayout(new BorderLayout());
        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
        previewPanel.add(Box.createVerticalGlue());
        add(previewPanel);
        m_imagePanel = new ImagePanel();
        previewPanel.add(m_imagePanel);
        m_description = new JLabel(" ");
        m_description.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewPanel.add(m_description);
        previewPanel.add(Box.createVerticalGlue());
        JPanel buttonPanel = new JPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        m_auto = new JCheckBox("Automatic preview");
        buttonPanel.add(m_auto);
        m_preview = new JButton("Preview");
        m_preview.setActionCommand("preview");
        ActionListener listener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    if (event.getActionCommand().equals("preview"))
                        preview();
                }
            };
        m_preview.addActionListener(listener);
        m_preview.setEnabled(false);
        buttonPanel.add(m_preview);
    }
    
    public void propertyChange(PropertyChangeEvent event)
    {
        String propertyName = event.getPropertyName();
        if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY))
        {
            m_file = null;
            m_preview.setEnabled(false);
            File file = (File)event.getNewValue();
            if (file != null)
            {
                String name = file.getAbsolutePath();
                if (name == null || ! name.toLowerCase().endsWith(".sgf"))
                    file = null;
            }
            if (file != null)
            {
                m_file = file;
                m_preview.setEnabled(true);
                if (m_auto.isSelected())
                    preview();
            }
        }
    }

    private class ImagePanel
        extends JPanel
    {
        public ImagePanel()
        {
            Dimension dimension = new Dimension(128 + 10, 128 + 10);
            setPreferredSize(dimension);
            setMaximumSize(dimension);
        }

        public void paintComponent(Graphics graphics)
        {
            graphics.setColor(getBackground());
            graphics.fillRect(0, 0, getWidth(), getHeight());
            if (m_image != null)
            {
                int imageWidth = m_image.getWidth(null);
                int imageHeight = m_image.getHeight(null);
                int x = (getWidth() - imageWidth) / 2;
                int y = (getHeight() - imageHeight) / 2;
                graphics.drawImage(m_image, x, y, imageWidth, imageHeight,
                                   null);
            }
        }

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sourceforge.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private File m_file;

    private JButton m_preview;

    private JCheckBox m_auto;

    private JLabel m_description;
    
    private Image m_image;

    private ImagePanel m_imagePanel;

    private Thumbnail m_thumbnail = new Thumbnail(false);

    public void preview()
    {
        if (m_file == null)
            return;
        m_thumbnail.create(m_file);
        File thumbnail = m_thumbnail.getLastThumbnail();
        if (thumbnail != null)
        {
            ImageIcon icon = new ImageIcon(thumbnail.toString());
            m_image = icon.getImage();
        }
        else
        {
            SimpleDialogs.showError(this, "Preview generation failed:\n" +
                                    m_thumbnail.getLastError());
            m_image = null;
        }
        String description = m_thumbnail.getLastDescription();
        if (! description.equals(""))
            m_description.setText(description);
        else
            m_description.setText("");
        m_imagePanel.repaint();
        m_preview.setEnabled(false);
    }
}

//----------------------------------------------------------------------------
