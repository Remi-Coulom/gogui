//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package utils;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
 
//----------------------------------------------------------------------------

public class RadialGradientPaint
    implements Paint
{
    public RadialGradientPaint(Point2D point, java.awt.Color pointColor,
                               Point2D radius, java.awt.Color backgroundColor)
    {
        assert(radius.distance(0, 0) > 0);
        m_point = point;
        m_radius = radius;
        m_pointColor = pointColor;
        m_backgroundColor = backgroundColor;
    }
    
    public PaintContext createContext(ColorModel colorModel,
                                      Rectangle deviceBounds,
                                      Rectangle2D userBounds,
                                      AffineTransform xform,
                                      RenderingHints hints)
    {
        Point2D transformedPoint = xform.transform(m_point, null);
        Point2D transformedRadius = xform.deltaTransform(m_radius, null);
        return new RadialGradientContext(transformedPoint, m_pointColor,
                                         transformedRadius,
                                         m_backgroundColor);
    }
    
    public int getTransparency()
    {
        int alphaPoint = m_pointColor.getAlpha();
        int alphaBackground = m_backgroundColor.getAlpha();
        if ((alphaPoint & alphaBackground) == 0xff)
            return OPAQUE;
        return TRANSLUCENT;
    }

    private Point2D m_point;

    private Point2D m_radius;

    private java.awt.Color m_backgroundColor;

    private java.awt.Color m_pointColor;
}

//----------------------------------------------------------------------------
