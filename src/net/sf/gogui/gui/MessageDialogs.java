//----------------------------------------------------------------------------
// $Id: SimpleDialogs.java 4309 2007-02-14 22:49:26Z enz $
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Locale;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sf.gogui.sgf.SgfFilter;
import net.sf.gogui.thumbnail.ThumbnailCreator;
import net.sf.gogui.thumbnail.ThumbnailPlatform;
import net.sf.gogui.util.Platform;
import net.sf.gogui.util.StringUtil;

/** Simple message dialogs. */
public final class MessageDialogs
{
    public static void showError(Component frame, String mainMessage,
                                 String optionalMessage)
    {
        showError(frame, mainMessage, optionalMessage, true);
    }

    public static void showError(Component frame, String mainMessage,
                                 String optionalMessage,
                                 boolean isCritical)
    {
        int type;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        else
            type = JOptionPane.ERROR_MESSAGE;
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(frame, "Error", mainMessage, optionalMessage, type,
             options, defaultOption);
    }

    public static void showError(Component frame, String message, Exception e)
    {
        showError(frame, message, e, true);
    }

    public static void showError(Component frame, String message, Exception e,
                                 boolean isCritical)
    {
        showError(frame, message, StringUtil.getErrorMessage(e), isCritical);
    }

    public static void showInfo(Component frame, String mainMessage,
                                String optionalMessage)
    {        
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(frame, "Information", mainMessage, optionalMessage,
             JOptionPane.INFORMATION_MESSAGE, options, defaultOption);
    }

    public static boolean showQuestion(Component frame, String mainMessage,
                                       String optionalMessage)
    {
        Object[] options = { "Yes", "No" };
        Object defaultOption = options[1];
        int type = JOptionPane.QUESTION_MESSAGE;
        Object result = show(frame, "Question", mainMessage, optionalMessage,
                             type, options, defaultOption);
        return (result == options[0]);
    }

    public static int showYesNoCancelQuestion(Component parent,
                                              String mainMessage,
                                              String optionalMessage)
    {
        Object[] options = { "Yes", "No", "Cancel" };
        Object defaultOption = options[2];
        int type = JOptionPane.QUESTION_MESSAGE;
        Object value = show(parent, "Question", mainMessage, optionalMessage,
                            type, options, defaultOption);
        int result;
        if (value == options[0])
            result = 0;
        else if (value == options[1])
            result = 1;
        else
        {
            assert(value == options[2] || value == null
                   || value.equals(new Integer(-1)));
            result = 2;
        }
        return result;
    }

    public static void showWarning(Component parent, String mainMessage,
                                   String optionalMessage, boolean isCritical)
    {
        int type;
        if (! isCritical)
            type = JOptionPane.PLAIN_MESSAGE;
        else
            type = JOptionPane.WARNING_MESSAGE;
        Object[] options = { "Close" };
        Object defaultOption = options[0];
        show(parent, "Error", mainMessage, optionalMessage, type,
             options, defaultOption);
    }

    public static boolean showWarningQuestion(Component parent,
                                              String mainMessage,
                                              String optionalMessage)
    {
        Object[] options = { "Yes", "No" };
        Object defaultOption = options[1];
        int type = JOptionPane.QUESTION_MESSAGE;
        Object result = show(parent, "Question", mainMessage, optionalMessage,
                             type, options, defaultOption);
        return (result == options[0]);
    }

    private static void addFiller(JComponent component)
    {
        Box.Filler filler = GuiUtil.createFiller();
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.add(filler);
    }

    private static Object show(Component parent, String title,
                               String mainMessage, String optionalMessage,
                               int messageType, Object[] options,
                               Object defaultOption)
    {
        Box box = Box.createVerticalBox();
        JLabel label = new JLabel(mainMessage);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        Font labelFont = UIManager.getFont("Label.font");
        Font labelFontBold = labelFont.deriveFont(Font.BOLD);
        label.setFont(labelFontBold);
        addFiller(box);
        box.add(label);
        int columns = Math.min(30, optionalMessage.length());
        JTextArea textArea = new JTextArea(optionalMessage, 0, columns);
        textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setForeground(UIManager.getColor("Label.foreground"));
        textArea.setBackground(UIManager.getColor("Label.background"));
        textArea.setFont(labelFont);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        addFiller(box);
        box.add(textArea);
        addFiller(box);
        addFiller(box);
        if (Platform.isMac())
            // Don't show icons on Mac, proplem with icon generation in
            // Quaqua 3.7.2
            messageType = JOptionPane.PLAIN_MESSAGE;
        JOptionPane optionPane =
            new JOptionPane(box, messageType, JOptionPane.DEFAULT_OPTION,
                            null, options, defaultOption);
        JDialog dialog = optionPane.createDialog(parent, title);
        // Workaround for Sun Bug ID 4545951 (still in Linux JDK 1.5.0_04-b05)
        box.invalidate();
        dialog.pack();
        dialog.setVisible(true);
        dialog.dispose();
        return optionPane.getValue();
    }
}
