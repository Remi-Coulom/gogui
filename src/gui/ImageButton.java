//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import javax.swing.ImageIcon;
import javax.swing.JButton;

//----------------------------------------------------------------------------

class ImageButton
    extends JButton
{
    ImageButton(String imageResourceName, String altText, String toolTipText)
    {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        java.net.URL url = classLoader.getResource(imageResourceName);
        if (url != null)
            setIcon(new ImageIcon(url));
        else if (altText != null && ! altText.equals(""))
            setText(altText);
        if (toolTipText != null && ! toolTipText.equals(""))
            setToolTipText(toolTipText);
    }
}

//----------------------------------------------------------------------------

