//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.awt.PaintContext;
import java.awt.geom.Point2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
 
//----------------------------------------------------------------------------

/** Creates a raster with a radial color gradient. */
public class RadialGradientContext
    implements PaintContext
{
    public RadialGradientContext(Point2D point, java.awt.Color color1,
                                 Point2D radius, java.awt.Color color2)
    {
        m_point = point;
        m_red1 = color1.getRed();
        m_green1 = color1.getGreen();
        m_blue1 = color1.getBlue();
        m_alpha1 = color1.getAlpha();
        m_radius = radius.distance(0, 0);
        m_redDiff = color2.getRed() - m_red1;
        m_greenDiff = color2.getGreen() - m_green1;
        m_blueDiff = color2.getBlue() - m_blue1;
        m_alphaDiff = color2.getAlpha() - m_alpha1;
    }
    
    public void dispose()
    {
    }
    
    public ColorModel getColorModel()
    {
        return ColorModel.getRGBdefault();
    }

    public Raster getRaster(int x, int y, int width, int height)
    {
        if (m_raster != null && x == m_x && y == m_y && width == m_width
            && height == m_height)
            return m_raster;
        m_x = x;
        m_y = y;
        m_height = height;
        m_width = width;
        ColorModel colorModel = getColorModel();
        m_raster = colorModel.createCompatibleWritableRaster(width, height);
        int[] data = new int[width * height * 4];
        int index = -1;
        for (int j = 0; j < height; ++j)
            for (int i = 0; i < width; ++i)
            {
                double distance = m_point.distance(x + i, y + j);
                double ratio = Math.min(distance / m_radius, 1.0);
                data[++index] = (int)(m_red1 + ratio * m_redDiff);
                data[++index] = (int)(m_green1 + ratio * m_greenDiff);
                data[++index] = (int)(m_blue1 + ratio * m_blueDiff);
                data[++index] = (int)(m_alpha1 + ratio * m_alphaDiff);
            }
        m_raster.setPixels(0, 0, width, height, data);
        return m_raster;
    }

    private int m_red1;

    private int m_redDiff;

    private int m_green1;

    private int m_greenDiff;

    private int m_blue1;

    private int m_blueDiff;

    private int m_alpha1;

    private int m_alphaDiff;

    private int m_x;

    private int m_y;

    private int m_height;

    private int m_width;

    private double m_radius;

    private Point2D m_point;

    private WritableRaster m_raster;
}

//----------------------------------------------------------------------------
