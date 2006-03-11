//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.utils.Platform;

//----------------------------------------------------------------------------

/** Status bar. */
public class StatusBar
    extends JPanel
{
    public StatusBar()
    {
        super(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(1, 0));
        add(panel, BorderLayout.CENTER);
        // Workaround for Java 1.4.1 on Mac OS X: add some empty space
        // so that status bar does not overlap the window resize widget
        if (Platform.isMac())
        {
            Dimension dimension = new Dimension(20, 1);
            Box.Filler filler =
                new Box.Filler(dimension, dimension, dimension);
            add(filler, BorderLayout.EAST);
        }
        m_textField = new JTextField();
        m_textField.setEditable(false);
        panel.add(m_textField);
        clear();
    }

    public void clear()
    {
        setText(" ");
    }

    public void setText(String text)
    {
        m_textField.setText(text);
        m_textField.repaint();
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JTextField m_textField;
}

//----------------------------------------------------------------------------
