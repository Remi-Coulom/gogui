//=============================================================================
// $Id$
// $Source$
//=============================================================================

import java.awt.*;
import java.awt.image.*;

//=============================================================================

class GoIcon
    extends BufferedImage
{
    public GoIcon()
    {
        super(64, 64, TYPE_BYTE_INDEXED);

        Graphics g = getGraphics();

        int width = getWidth();
        int height = getHeight();
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        g.setColor(new java.awt.Color(224, 160, 96));
        g.fillRect(0, 0, width, height);

        g.setColor(java.awt.Color.black);
        g.fillOval(halfWidth, 0, halfWidth, halfHeight);

        g.setColor(java.awt.Color.white);
        g.fillOval(0, halfHeight, halfWidth, halfHeight);
    }
}
