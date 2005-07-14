//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gtpstatistics;

import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import net.sf.gogui.utils.Table;
import net.sf.gogui.utils.TableUtils;

//----------------------------------------------------------------------------

public class Plot
{
    public Plot(int imgWidth, int imgHeight, Color color, int precision)
    {
        m_precision = precision;
        m_color = color;
        m_imgWidth = imgWidth;
        m_imgHeight = imgHeight;
    }

    public void plot(File file, Table table, String columnX, String columnY,
                     String errorColumn)
        throws IOException
    {
        int type = BufferedImage.TYPE_INT_RGB;
        BufferedImage image
            = new BufferedImage(m_imgWidth, m_imgHeight, type);
        m_graphics2D = image.createGraphics();
        m_graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                      RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = m_graphics2D.getFont();
        if (font != null)
        {
            font = font.deriveFont((float)(font.getSize() * 0.8));
            m_graphics2D.setFont(font);
        }
        m_metrics = m_graphics2D.getFontMetrics();
        m_fontHeight = m_metrics.getHeight();
        m_left = 4 * m_fontHeight;
        if (m_title != null)
            m_top = (int)(m_fontHeight * 1.7);
        else
            m_top = (int)(m_fontHeight * 0.5);
        m_right = m_imgWidth - (int)(m_fontHeight * 0.5);
        m_bottom = m_imgHeight - (int)(m_fontHeight * 1.5);;
        m_width = m_right - m_left;
        m_height = m_bottom - m_top;
        initScale(table, columnX, columnY);
        drawBackground();
        drawGrid();
        drawData(table, columnX, columnY, errorColumn, false, m_withBars);
        m_graphics2D.dispose();
        ImageIO.write(image, "png", file);
    }

    public void setPlotStyleBars(double barWidth)
    {
        m_withBars = true;
        m_barWidth = barWidth;
    }

    public void setPlotStyleNoLines()
    {
        m_noLines = true;
    }

    public void setSolidLineInterval(double solidLineInterval)
    {
        m_solidLineInterval = solidLineInterval;
        m_useSolidLineInterval = true;
    }

    public void setXLabelPerTic(int xLabelPerTic)
    {
        m_xLabelPerTic = xLabelPerTic;
    }

    public void setXTics(double tics)
    {
        m_xTics = tics;
        m_autoXTics = false;
    }

    public void setXMax(double max)
    {
        m_maxX = max;
        m_autoXMax = false;
    }

    public void setXMin(double min)
    {
        m_minX = min;
        m_autoXMin = false;
    }

    public void setYMin(double min)
    {
        m_minY = min;
        m_autoYMin = false;
    }

    public void setYMax(double max)
    {
        m_maxY = max;
        m_autoYMax = false;
    }

    public void setYTics(double tics)
    {
        m_yTics = tics;
        m_autoYTics = false;
    }

    public void setTitle(String title)
    {
        m_title = title;
    }

    private boolean m_autoXMax = true;

    private boolean m_autoXMin = true;

    private boolean m_autoXTics = true;

    private boolean m_autoYMin = true;

    private boolean m_autoYMax = true;

    private boolean m_autoYTics = true;

    private boolean m_noLines = false;

    private boolean m_onlyBoolValues;

    private boolean m_onlyIntValuesX;

    private boolean m_onlyIntValuesY;

    private boolean m_useSolidLineInterval = false;

    private boolean m_withBars;

    private int m_fontHeight;

    private int m_bottom;

    private int m_height;

    private int m_imgHeight;

    private int m_imgWidth;

    private int m_left;

    private int m_precision;

    private int m_right;

    private int m_top;

    private int m_width;

    private int m_xLabelPerTic = 1;

    private double m_barWidth;

    private double m_minX;

    private double m_maxX;

    private double m_minY;

    private double m_maxY;

    private double m_solidLineInterval;

    private double m_xRange;

    private double m_xTics;

    private double m_xTicsMin;

    private double m_yRange;

    private double m_yTics;

    private double m_yTicsMin;

    private Color m_color = Color.decode("#ff5454");

    private FontMetrics m_metrics;

    private Graphics2D m_graphics2D;

    private String m_title;

    private void drawBackground()
    {
        m_graphics2D.setColor(Color.decode("#e0e0e0"));
        m_graphics2D.fillRect(0, 0, m_imgWidth, m_imgHeight);
        m_graphics2D.setColor(Color.WHITE);
        m_graphics2D.fillRect(m_left, m_top, m_width, m_height);
        m_graphics2D.setColor(Color.BLACK);
        if (m_title != null)
        {
            int width = m_metrics.stringWidth(m_title) + 10;
            int height = (int)(m_fontHeight * 1.4);
            int x = m_left + (m_width - width) / 2;
            int y = (m_top - height) / 2;
            m_graphics2D.setColor(Color.decode("#ffffe1"));
            m_graphics2D.fillRect(x, y, width, height);
            m_graphics2D.setColor(Color.DARK_GRAY);
            m_graphics2D.drawRect(x, y, width, height);
            drawString(m_title, m_left + m_width / 2, m_top / 2);
        }
    }

    private void drawData(Table table, String columnX, String columnY,
                          String errorColumn, boolean withLines,
                          boolean withBars)
    {
        m_graphics2D.setColor(m_color);
        Point last = null;
        int barWidthPixels = getPoint(m_barWidth, 0).x
            - getPoint(0, 0).x - 1;
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            try
            {
                String xValue = table.get(columnX, row);
                String yValue = table.get(columnY, row);
                if (xValue == null || yValue == null)
                {
                    last = null;
                    continue;
                }
                double x = Double.parseDouble(xValue);
                double y = Double.parseDouble(yValue);
                Point point = getPoint(x, y);
                if (withBars)
                {
                    Point bottom = getPoint(x, 0);
                    m_graphics2D.fillRect(point.x - barWidthPixels / 2,
                                          point.y,
                                          barWidthPixels,
                                          bottom.y - point.y);
                }
                else if (last != null && ! m_noLines)
                    m_graphics2D.drawLine(last.x, last.y, point.x, point.y);
                if (errorColumn != null)
                {
                    double err
                        = Double.parseDouble(table.get(errorColumn, row));
                    Point top = getPoint(x, y + err);
                    Point bottom = getPoint(x, y - err);
                    m_graphics2D.drawLine(top.x, top.y, bottom.x, bottom.y);
                }
                if (! withBars)
                    m_graphics2D.fillRect(point.x - 1, point.y - 1, 3, 3);
                last = point;
            }
            catch (NumberFormatException e)
            {
                last = null;
            }
        }
    }

    private void drawGrid()
    {
        Stroke oldStroke = m_graphics2D.getStroke();
        Stroke dottedStroke
            = new BasicStroke(1f, BasicStroke.CAP_ROUND,
                              BasicStroke.JOIN_ROUND, 1f, new float[] {2f},
                              0f);
        if (m_useSolidLineInterval)
        {
            double min =
                (int)(m_xTicsMin / m_solidLineInterval) * m_solidLineInterval;
            int n = 0;
            for (double x = min; x < m_maxX; x += m_solidLineInterval, ++n)
            {
                Point bottom = getPoint(x, m_minY);
                Point top = getPoint(x, m_maxY);
                if (n % 2 == 0)
                {
                    m_graphics2D.setColor(Color.decode("#f0f0f0"));
                    Point right = getPoint(x + m_solidLineInterval, m_maxY);
                    m_graphics2D.fillRect(top.x, top.y,
                                          Math.min(right.x - top.x,
                                                   m_right - top.x),
                                          bottom.y - top.y);
                }
            }
        }
        m_graphics2D.setStroke(dottedStroke);
        for (double x = m_xTicsMin; x < m_maxX; x += m_xTics)
        {
            Point bottom = getPoint(x, m_minY);
            Point top = getPoint(x, m_maxY);
            m_graphics2D.setColor(Color.LIGHT_GRAY);
            m_graphics2D.drawLine(top.x, top.y, bottom.x, bottom.y);
        }
        m_graphics2D.setStroke(oldStroke);
        m_graphics2D.setStroke(dottedStroke);
        for (double y = m_yTicsMin; y < m_maxY; y += m_yTics)
        {
            Point left = getPoint(m_minX, y);
            Point right = getPoint(m_maxX, y);
            m_graphics2D.setColor(Color.LIGHT_GRAY);
            m_graphics2D.drawLine(left.x, left.y, right.x, right.y);
        }
        m_graphics2D.setStroke(oldStroke);
        m_graphics2D.setColor(Color.GRAY);
        if (m_minX <= 0 && m_maxX >= 0)
        {
            Point top = getPoint(0, m_minY);
            Point bottom = getPoint(0, m_maxY);
            m_graphics2D.drawLine(top.x, top.y, bottom.x, bottom.y);
        }
        if (m_minY <= 0 && m_maxY >= 0)
        {
            Point left = getPoint(m_minX, 0);
            Point right = getPoint(m_maxX, 0);
            m_graphics2D.drawLine(left.x, left.y, right.x, right.y);
        }
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(0);
        format.setGroupingUsed(false);
        DecimalFormat format2 = new DecimalFormat();
        format2.setMaximumFractionDigits(m_precision);
        format2.setGroupingUsed(false);
        for (double x = m_xTicsMin; x < m_maxX; x += m_xLabelPerTic * m_xTics)
        {
            Point bottom = getPoint(x, m_minY);
            Point top = getPoint(x, m_maxY);
            String label;
            if (m_onlyIntValuesX)
                label = format.format(x);
            else
                label = format2.format(x);
            m_graphics2D.setColor(Color.GRAY);
            m_graphics2D.drawLine(bottom.x, bottom.y, bottom.x, bottom.y + 3);
            m_graphics2D.setColor(Color.BLACK);
            drawString(label, bottom.x,
                       m_bottom + (m_imgHeight - m_bottom) / 2);
        }
        for (double y = m_yTicsMin; y < m_maxY; y += m_yTics)
        {
            Point point = getPoint(m_minX, y);
            String label;
            if (m_onlyIntValuesY)
                label = format.format(y);
            else
                label = format2.format(y);
            m_graphics2D.setColor(Color.GRAY);
            m_graphics2D.drawLine(point.x, point.y, point.x - 3, point.y);
            m_graphics2D.setColor(Color.BLACK);
            drawStringRightAlign(label, m_left - 5, point.y);
        }
        m_graphics2D.setColor(Color.LIGHT_GRAY);
        m_graphics2D.drawRect(m_left, m_top, m_width, m_height);
        m_graphics2D.setColor(Color.GRAY);
        m_graphics2D.drawLine(m_left, m_top, m_left, m_bottom);
        m_graphics2D.drawLine(m_left, m_bottom, m_right, m_bottom);
        m_graphics2D.setStroke(oldStroke);
    }

    private void drawString(String string, int x, int y)
    {
        FontMetrics metrics = m_graphics2D.getFontMetrics();
        int width = metrics.stringWidth(string);
        int height = m_fontHeight;
        m_graphics2D.drawString(string, x - width / 2, y + height / 2);
    }

    private void drawStringRightAlign(String string, int x, int y)
    {
        FontMetrics metrics = m_graphics2D.getFontMetrics();
        int width = metrics.stringWidth(string);
        int height = m_fontHeight;
        m_graphics2D.drawString(string, x - width, y + height / 2);
    }

    private Point getPoint(double x, double y)
    {
        int intX = (int)(m_left + (x - m_minX) / m_xRange * m_width);
        int intY = (int)(m_bottom - (y - m_minY) / m_yRange * m_height);
        return new Point(intX, intY);
    }

    private double getTics(double range, int maxNumberTics)
    {
        final int minNumberTics = 3;
        maxNumberTics = Math.max(maxNumberTics, minNumberTics);
        double tics;
        if (range / maxNumberTics < 0.5)
        {
            tics = 0.5;
            while (range / (tics / 5) < maxNumberTics
                   || range / (tics / 5) < minNumberTics)
            {
                tics /= 5;
                if (range / (tics / 2) > maxNumberTics
                    && range / (tics / 2) >= minNumberTics)
                    break;
                tics /= 2;
            }
        }
        else
        {
            tics = 0.5;
            while (range / (tics * 2) > maxNumberTics
                   && range / (tics * 2) > minNumberTics)
            {
                tics *= 2;
                if (range / (tics * 5) <= maxNumberTics
                    || range / (tics * 5) < minNumberTics)
                    break;
                tics *= 5;
            }
        }
        return tics;
    }

    private double getTicsMin(double tics, double min)
    {
        double result = (int)(min / tics) * tics;
        if (result < min)
            result += tics;
        return result;
    }

    private void initScale(Table table, String columnX, String columnY)
    {
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        m_onlyBoolValues = true;
        m_onlyIntValuesX = true;
        m_onlyIntValuesY = true;
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            String xValue = table.get(columnX, row);
            String yValue = table.get(columnY, row);
            if (xValue == null || yValue == null
                || ! TableUtils.isNumberValue(yValue))
                continue;
            if (! TableUtils.isBoolValue(yValue))
                m_onlyBoolValues = false;
            if (! TableUtils.isIntValue(xValue))
                m_onlyIntValuesX = false;
            if (! TableUtils.isIntValue(yValue))
                m_onlyIntValuesY = false;
            try
            {
                double x = Double.parseDouble(xValue);
                double y = Double.parseDouble(yValue);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            catch (NumberFormatException e)
            {
            }
        }
        initScaleX(minX, maxX);
        initScaleY(minY, maxY);
    }

    private void initScaleX(double min, double max)
    {
        if (m_autoXMin)
            m_minX = min - 0.05 * (max - min);
        if (m_autoXMax)
            m_maxX = max + 0.05 * (max - m_minX);
        // Try to inlude 0 in plot
        if (m_minX > 0 && m_minX < 0.3 * m_maxX)
            m_minX = 0;
        // Avoid empty ranges
        if (m_minX == m_maxX)
        {
            m_minX -= 1.1;
            m_maxX += 1.1;
        }
        m_xRange = m_maxX - m_minX;
        if (m_autoXTics)
        {
            double absMax = Math.max(Math.abs(m_minX), Math.abs(m_maxX));
            final double log10 = Math.log(10);
            int maxLength = (int)(Math.log(absMax) / log10) + m_precision + 2;
            int maxPixels = (int)(maxLength * (0.7 * m_fontHeight));
            int numberTics = m_imgWidth / maxPixels;
            m_xTics = getTics(m_xRange, numberTics);
        }
        if (m_onlyIntValuesX)
            m_xTics = Math.max(1, m_xTics);
        m_xTicsMin = getTicsMin(m_xTics, m_minX);
    }

    private void initScaleY(double min, double max)
    {
        if (m_autoYMin)
        {
            if (m_onlyBoolValues)
                m_minY = 0;
            else
                m_minY = min;
        }
        if (m_autoYMax)
        {
            if (m_onlyBoolValues)
                m_maxY = 1.1;
            else
                m_maxY = max + 0.05 * (max - m_minY);
        }
        // Try to inlude 0 in plot
        if (m_autoYMin)
            if (m_minY > 0 && m_minY < 0.3 * m_maxY)
                m_minY = 0;
        // Avoid empty ranges
        if (m_minY == m_maxY)
        {
            m_minY -= 1.1;
            m_maxY += 1.1;
        }
        m_yRange = m_maxY - m_minY;
        if (m_autoYTics)
        {
            if (m_onlyBoolValues)
            {
                m_yTics = 1;
                m_yTicsMin = 0;
            }
            else
            {
                int maxNumberTics = (int)(m_imgHeight / (3 * m_fontHeight));
                m_yTics = getTics(m_yRange, maxNumberTics);
                if (m_onlyIntValuesY)
                    m_yTics = Math.max(1, m_yTics);
            }
        }
        if (! m_onlyBoolValues)
            m_yTicsMin = getTicsMin(m_yTics, m_minY);
    }
}

//----------------------------------------------------------------------------
