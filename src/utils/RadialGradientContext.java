//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.font.*;
import java.util.*;
import javax.swing.*;
 
//-----------------------------------------------------------------------------

public class RadialGradientContext
    implements PaintContext
{
    public RadialGradientContext(Point2D p, java.awt.Color c1, Point2D r,
                                 java.awt.Color c2)
    {
        m_point = p;
        m_color1 = c1;
        m_color2 = c2;
        m_radius = r;
    }
    
    public void dispose()
    {
    }
    
    public ColorModel getColorModel()
    {
        return ColorModel.getRGBdefault();
    }
  
    public Raster getRaster(int x, int y, int w, int h)
    {
        WritableRaster raster =
            getColorModel().createCompatibleWritableRaster(w, h);
        int[] data = new int[w * h * 4];
        for (int j = 0; j < h; ++j)
        {
            for (int i = 0; i < w; ++i)
            {
                double distance = m_point.distance(x + i, y + j);
                double radius = m_radius.distance(0, 0);
                double ratio = Math.min(distance / radius, 1.0);
                int base = (j * w + i) * 4;
                data[base + 0] =
                    (int)(m_color1.getRed() + ratio *
                          (m_color2.getRed() - m_color1.getRed()));
                data[base + 1] =
                    (int)(m_color1.getGreen() + ratio *
                          (m_color2.getGreen() - m_color1.getGreen()));
                data[base + 2] =
                    (int)(m_color1.getBlue() + ratio *
                          (m_color2.getBlue() - m_color1.getBlue()));
                data[base + 3] =
                    (int)(m_color1.getAlpha() + ratio *
                          (m_color2.getAlpha() - m_color1.getAlpha()));
            }
        }
        raster.setPixels(0, 0, w, h, data);
        return raster;
    }

    private Point2D m_point;

    private Point2D m_radius;

    private java.awt.Color m_color1;

    private java.awt.Color m_color2;
}

//-----------------------------------------------------------------------------
