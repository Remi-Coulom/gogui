//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------

class ToolBarButton
    extends JButton
    implements MouseListener
{
    public ToolBarButton(ImageIcon icon)
    {
        super(icon);
        init();
    }

    public ToolBarButton(String text)
    {
        super(text);
        init();
    }

    public void mouseClicked(MouseEvent event)
    {
    }

    public void mouseEntered(MouseEvent event)
    {
        JButton button = (JButton)event.getSource();
        button.setBorderPainted(true);
    }
    
    public void mouseExited(MouseEvent event)
    {
        JButton button = (JButton)event.getSource();
        button.setBorderPainted(false);
    }

    public void mousePressed(MouseEvent event)
    {
    }

    public void mouseReleased(MouseEvent event)
    {
    }

    private void init()
    {
        setBorder(BorderFactory.createRaisedBevelBorder());
        addMouseListener(this);
        setBorderPainted(false);
        Insets insets = new Insets(1, 1, 1, 1);
        setMargin(insets);
    }
};

//-----------------------------------------------------------------------------
