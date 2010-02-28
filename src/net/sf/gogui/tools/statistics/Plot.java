// Plot.java

package net.sf.gogui.tools.statistics;

import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import javax.imageio.ImageIO;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.util.Table;
import net.sf.gogui.util.TableUtil;

/** Produce a PNG plot from table data. */
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
        GuiUtil.setAntiAlias(m_graphics2D);
        Font font = m_graphics2D.getFont();
        if (font != null)
        {
            font = font.deriveFont((float)(font.getSize() * 0.8));
            m_graphics2D.setFont(font);
        }
        m_metrics = m_graphics2D.getFontMetrics();
        m_fontHeight = m_metrics.getHeight();
        m_left = 4 * m_fontHeight;
        if (m_title == null)
            m_top = (int)(m_fontHeight * 0.5);
        else
            m_top = (int)(m_fontHeight * 1.7);
        m_right = m_imgWidth - (int)(m_fontHeight * 0.5);
        m_bottom = m_imgHeight - (int)(m_fontHeight * 1.5);
        m_width = m_right - m_left;
        m_height = m_bottom - m_top;
        initScale(table, columnX, columnY);
        drawBackground();
        drawGrid();
        drawData(table, columnX, columnY, errorColumn, m_withBars);
        m_graphics2D.dispose();
        ImageIO.write(image, "png", file);
    }

    /** Set number format for x-axis.
        @param format The format. */
    public void setFormatX(DecimalFormat format)
    {
        m_formatX = format;
    }

    /** Set number format for y-axis.
        @param format The format. */
    public void setFormatY(DecimalFormat format)
    {
        m_formatY = format;
    }

    /** Set plot style to bars.
        Default is plotting points connected by lines.
        @param barWidth The width of each bar. */
    public void setPlotStyleBars(double barWidth)
    {
        m_withBars = true;
        m_barWidth = barWidth;
    }

    /** Don't connect plotted points with lines. */
    public void setPlotStyleNoLines()
    {
        m_noLines = true;
    }

    /** Disable drawing the zero axis for the y-coordinates. */
    public void setNoPlotYZero()
    {
        m_plotYZero = false;
    }

    /** Enable drawing of solid lines at certain x-intervals.
        NOTE: The new implementation does no longer use solid lines, but
        changes white and gray background color at the solid line interval.
        @param solidLineInterval The interval for the solid lines. */
    public void setSolidLineInterval(double solidLineInterval)
    {
        m_solidLineInterval = solidLineInterval;
        m_useSolidLineInterval = true;
    }

    /** Set x-label intervals.
        By default, every x-tic (grid line) gets an x-label.
        NOTE: Misleading name, should be tics per x-label.
        @param xLabelPerTic The number of tics per x-label. */
    public void setXLabelPerTic(int xLabelPerTic)
    {
        m_xLabelPerTic = xLabelPerTic;
    }

    /** Plot only x labels for 0 and 1. */
    public void setXLabelsBool()
    {
        m_xLabelsBool = true;
        setXMin(-5);
        setXMax(5);
        setXTics(1);
    }

    /** Set maximum x value.
        @param max The maximum. */
    public void setXMax(double max)
    {
        m_maxX = max;
        m_autoXMax = false;
    }

    /** Set minimum x value.
        @param min The minimum. */
    public void setXMin(double min)
    {
        m_minX = min;
        m_autoXMin = false;
    }

    /** Set x-tics.
        Sets the grid line distance for the x-axis.
        @param tics The distance. */
    public void setXTics(double tics)
    {
        m_xTics = tics;
        m_autoXTics = false;
    }

    /** Set maximum y value.
        @param max The maximum. */
    public void setYMax(double max)
    {
        m_maxY = max;
        m_autoYMax = false;
    }

    /** Set minimum x value.
        @param min The minimum. */
    public void setYMin(double min)
    {
        m_minY = min;
        m_autoYMin = false;
    }

    /** Set y-tics.
        Sets the grid line distance for the y-axis.
        @param tics The distance. */
    public void setYTics(double tics)
    {
        m_yTics = tics;
        m_autoYTics = false;
    }

    /** Set plot title.
        @param title The title. */
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

    private boolean m_plotYZero = true;

    private boolean m_useSolidLineInterval = false;

    private boolean m_withBars;

    private boolean m_xLabelsBool;

    private int m_fontHeight;

    private int m_bottom;

    private int m_height;

    private final int m_imgHeight;

    private final int m_imgWidth;

    private int m_left;

    private final int m_precision;

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

    private final Color m_color;

    private DecimalFormat m_formatX;

    private DecimalFormat m_formatY;

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
                          String errorColumn, boolean withBars)
    {
        m_graphics2D.setColor(m_color);
        Point last = null;
        int barWidthPixels = getPoint(m_barWidth, 0).x
            - getPoint(0, 0).x - 2;
        for (int row = 0; row < table.getNumberRows(); ++row)
        {
            try
            {
                double x = table.getDouble(columnX, row);
                double y = table.getDouble(columnY, row);
                Point point = getPoint(x, y);
                if (withBars)
                {
                    Point bottom = getPoint(x, 0);
                    m_graphics2D.fillRect(point.x - barWidthPixels / 2 + 1,
                                          point.y,
                                          barWidthPixels,
                                          bottom.y - point.y);
                }
                else if (last != null && ! m_noLines)
                    m_graphics2D.drawLine(last.x, last.y, point.x, point.y);
                if (errorColumn != null)
                {
                    double err = table.getDouble(errorColumn, row);
                    Point top = getPoint(x, y + err);
                    Point bottom = getPoint(x, y - err);
                    m_graphics2D.drawLine(top.x, top.y, bottom.x, bottom.y);
                }
                if (! withBars)
                    m_graphics2D.fillRect(point.x - 1, point.y - 1, 3, 3);
                last = point;
            }
            catch (Table.InvalidElement e)
            {
                last = null;
            }
            catch (Table.InvalidLocation e)
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
        if (m_formatX == null)
            m_formatX = getFormat(m_onlyIntValuesX);
        if (m_formatY == null)
            m_formatY = getFormat(m_onlyIntValuesY);
        for (double x = m_xTicsMin; x < m_maxX; x += m_xLabelPerTic * m_xTics)
        {
            if (m_xLabelsBool && Math.round(x) != 0 && Math.round(x) != 1)
                continue;
            Point bottom = getPoint(x, m_minY);
            String label;
            label = m_formatX.format(x);
            m_graphics2D.setColor(Color.GRAY);
            m_graphics2D.drawLine(bottom.x, bottom.y, bottom.x, bottom.y + 3);
            m_graphics2D.setColor(Color.BLACK);
            drawString(label, bottom.x,
                       m_bottom + (m_imgHeight - m_bottom) / 2);
        }
        for (double y = m_yTicsMin; y < m_maxY; y += m_yTics)
        {
            if (! m_plotYZero && Math.round(y) == 0)
                continue;
            Point point = getPoint(m_minX, y);
            String label;
            label = m_formatY.format(y);
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

    private DecimalFormat getFormat(boolean onlyIntValues)
    {
        DecimalFormat format = new DecimalFormat();
        format.setGroupingUsed(false);
        if (onlyIntValues)
            format.setMaximumFractionDigits(0);
        else
            format.setMaximumFractionDigits(m_precision);
        return format;
    }

    private Point getPoint(double x, double y)
    {
        int intX = (int)(m_left + (x - m_minX) / m_xRange * m_width);
        int intY = (int)(m_bottom - (y - m_minY) / m_yRange * m_height);
        return new Point(intX, intY);
    }

    /** Find tics interval.
        Tries to respect maxNumberTics, as long as there are at least two
        visible tics. */
    private double getTics(double range, int maxNumberTics)
    {
        maxNumberTics = Math.max(maxNumberTics, 2);
        double maxTics = range / 2.1; // Make sure 2 tics are visible
        double tics = 1;
        if (range / maxNumberTics < 1)
        {
            while (range / (tics / 2) < maxNumberTics || tics > maxTics)
            {
                tics /= 2;
                if (range / (tics / 2) > maxNumberTics && tics < maxTics)
                    break;
                tics /= 2;
                if (range / (tics / 2.5) > maxNumberTics && tics < maxTics)
                    break;
                tics /= 2.5;
            }
        }
        else
        {
            while (range / tics > maxNumberTics && tics * 2 < maxTics)
            {
                tics *= 2;
                if (range / tics <= maxNumberTics || tics * 2.5 > maxTics)
                    break;
                tics *= 2.5;
                if (range / tics <= maxNumberTics || tics * 2 > maxTics)
                    break;
                tics *= 2;
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
            try
            {
                String xValue = table.get(columnX, row);
                String yValue = table.get(columnY, row);
                if (xValue == null || yValue == null
                    || ! TableUtil.isNumberValue(yValue))
                    continue;
                if (! TableUtil.isBoolValue(yValue))
                    m_onlyBoolValues = false;
                if (! TableUtil.isIntValue(xValue))
                    m_onlyIntValuesX = false;
                if (! TableUtil.isIntValue(yValue))
                    m_onlyIntValuesY = false;
                double x = Double.parseDouble(xValue);
                double y = Double.parseDouble(yValue);
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            catch (Table.InvalidLocation e)
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
        if (m_maxX - m_minX < Double.MIN_VALUE)
        {
            m_minX -= 1.1;
            m_maxX += 1.1;
        }
        m_xRange = m_maxX - m_minX;
        if (m_autoXTics)
        {
            double absMax = Math.max(Math.abs(m_minX), Math.abs(m_maxX));
            final double log10 = Math.log(10);
            int maxLength = (int)(Math.log(absMax) / log10) + m_precision + 3;
            int maxPixels = (int)(maxLength * (0.7 * m_fontHeight));
            int numberTics = m_width / maxPixels;
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
        if (m_autoYMin && m_minY > 0 && m_minY < 0.3 * m_maxY)
            m_minY = 0;
        // Avoid empty ranges
        if (m_maxY - m_minY < Double.MIN_VALUE)
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
                int maxNumberTics = (int)(m_height / (1.5 * m_fontHeight));
                m_yTics = getTics(m_yRange, maxNumberTics);
                if (m_onlyIntValuesY)
                    m_yTics = Math.max(1, m_yTics);
            }
        }
        if (! m_onlyBoolValues)
            m_yTicsMin = getTicsMin(m_yTics, m_minY);
    }
}
