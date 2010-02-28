// RadialGradientPaint.java

package net.sf.gogui.boardpainter;

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
    /** Create a radial gradient paint.
        @param center The center point.
        @param radius1 The radius along the first axis of the ellipse.
        @param radius2 The radius along the second axis of the ellipse.
        @param focus Focus shift away from the center along second radius
        normalized to interval between zero and one.
        @param color1 First color.
        @param color2 Second color. */
    public RadialGradientPaint(Point2D center, Point2D radius1,
                               Point2D radius2, double focus, Color color1,
                               Color color2)
    {
        m_center = center;
        m_radius1 = radius1;
        m_radius2 = radius2;
        m_focus = focus;
        m_color1 = color1;
        m_color2 = color2;
        int alpha1 = color1.getAlpha();
        int alpha2 = color2.getAlpha();
        if ((alpha1 & alpha2) == 0xff)
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
        Point2D transformedCenter = xform.transform(m_center, null);
        Point2D transformedRadius1 = xform.deltaTransform(m_radius1, null);
        Point2D transformedRadius2 = xform.deltaTransform(m_radius2, null);
        if (m_cachedContext != null
            && transformedCenter.equals(m_transformedCenter)
            && transformedRadius1.equals(m_transformedRadius1)
            && transformedRadius2.equals(m_transformedRadius2))
            return m_cachedContext;
        m_transformedCenter = (Point2D)transformedCenter.clone();
        m_transformedRadius1 = (Point2D)transformedRadius1.clone();
        m_transformedRadius2 = (Point2D)transformedRadius2.clone();
        m_cachedContext =
            new RadialGradientContext(transformedCenter, transformedRadius1,
                                      transformedRadius2, m_focus,
                                      m_color1, m_color2);
        return m_cachedContext;
    }

    public int getTransparency()
    {
        return m_transparency;
    }

    private final int m_transparency;

    private Point2D m_transformedCenter;

    private Point2D m_transformedRadius1;

    private Point2D m_transformedRadius2;

    private RadialGradientContext m_cachedContext;

    private final double m_focus;

    private final Point2D m_center;

    private final Point2D m_radius1;

    private final Point2D m_radius2;

    private final Color m_color1;

    private final Color m_color2;
}
