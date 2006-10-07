//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/** Creates a PaintContext for a radial gradient. */
public class RadialGradientPaint
    implements Paint
{
    public RadialGradientPaint(Point2D point, Color pointColor,
                               Point2D radius, Color backgroundColor)
    {
        assert(radius.distance(0, 0) > 0);
        m_point = point;
        m_radius = radius;
        m_pointColor = pointColor;
        m_backgroundColor = backgroundColor;
        int alphaPoint = pointColor.getAlpha();
        int alphaBackground = backgroundColor.getAlpha();
        if ((alphaPoint & alphaBackground) == 0xff)
            m_transparency = OPAQUE;
        else
            m_transparency = TRANSLUCENT;
    }
    
    public PaintContext createContext(ColorModel colorModel,
                                      Rectangle deviceBounds,
                                      Rectangle2D userBounds,
                                      AffineTransform xform,
                                      RenderingHints hints)
    {
        Point2D transformedPoint = xform.transform(m_point, null);
        Point2D transformedRadius = xform.deltaTransform(m_radius, null);
        if (m_cachedContext != null
            && transformedPoint.equals(m_transformedPoint)
            && transformedRadius.equals(m_transformedRadius))
            return m_cachedContext;
        m_transformedPoint = (Point2D)transformedPoint.clone();
        m_transformedRadius = (Point2D)transformedRadius.clone();
        m_cachedContext =
            new RadialGradientContext(transformedPoint, m_pointColor,
                                      transformedRadius, m_backgroundColor);
        return m_cachedContext;
    }

    public int getTransparency()
    {
        return m_transparency;
    }

    private final int m_transparency;

    private Point2D m_transformedPoint;

    private Point2D m_transformedRadius;

    private RadialGradientContext m_cachedContext;    

    private final Point2D m_point;

    private final Point2D m_radius;

    private final Color m_backgroundColor;

    private final Color m_pointColor;
}

