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
    extends ImageButton
    implements MouseListener
{
    public ToolBarButton(String imageResourceName, String altText,
                         String toolTipText)
    {
        super(imageResourceName, altText, toolTipText);
        setBorder(BorderFactory.createRaisedBevelBorder());
        addMouseListener(this);
        setBorderPainted(false);
        Insets insets = new Insets(1, 1, 1, 1);
        setMargin(insets);
        setFocusable(false);
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
};

//-----------------------------------------------------------------------------
