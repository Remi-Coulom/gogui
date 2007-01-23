//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.util.Platform;

/** Status bar. */
public class StatusBar
    extends JPanel
{
    public StatusBar(boolean withToPlayIcon)
    {
        super(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout());
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
        m_iconBox = Box.createHorizontalBox();
        panel.add(m_iconBox, BorderLayout.WEST);
        if (withToPlayIcon)
        {
            m_toPlayLabel = new JLabel();
            m_toPlayLabel.setBorder(UIManager.getBorder("TextField.border"));
            m_toPlayLabel.setMaximumSize(new Dimension(Short.MAX_VALUE,
                                                       Short.MAX_VALUE));
            setToPlay(GoColor.BLACK);
            m_iconBox.add(m_toPlayLabel);
            m_iconBox.add(GuiUtil.createSmallFiller());
        }
        else
            m_toPlayLabel = null;
        m_textField = new JTextField();
        m_textField.setEditable(false);
        m_textField.setFocusable(false);
        panel.add(m_textField, BorderLayout.CENTER);
    }

    public void clear()
    {
        setText("");
    }

    public void setText(String text)
    {
        m_textField.setText(text);
    }

    public void setToPlay(GoColor color)
    {
        if (color == GoColor.BLACK)
        {
            m_toPlayLabel.setIcon(m_iconBlack);
            m_toPlayLabel.setToolTipText("Black to play");
        }
        else
        {
            assert(color == GoColor.WHITE);
            m_toPlayLabel.setIcon(m_iconWhite);
            m_toPlayLabel.setToolTipText("White to play");
        }
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for use with serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private static final Icon m_iconBlack =
        GuiUtil.getIcon("gogui-black", "Black");

    private static final Icon m_iconWhite =
        GuiUtil.getIcon("gogui-white", "White");

    private Box m_iconBox;

    private final JLabel m_toPlayLabel;

    private final JTextField m_textField;
}

