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
{
    public ToolBarButton(String imageResourceName, String altText,
                         String toolTipText)
    {
        super(imageResourceName, altText, toolTipText);
        setBorder(BorderFactory.createRaisedBevelBorder());
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
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
            };
        addMouseListener(mouseAdapter);
        setBorderPainted(false);
        Insets insets = new Insets(1, 1, 1, 1);
        setMargin(insets);
        setFocusable(false);
    }
};

//-----------------------------------------------------------------------------
