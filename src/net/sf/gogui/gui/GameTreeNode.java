//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import net.sf.gogui.game.Node;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;

//----------------------------------------------------------------------------

class GameTreeNode
    extends JComponent
{
    public GameTreeNode(Node node, int moveNumber,
                        GameTreePanel gameTreePanel,
                        MouseListener mouseListener, Font font)
    {
        m_gameTreePanel = gameTreePanel;
        m_node = node;
        m_moveNumber = moveNumber;
        addMouseListener(mouseListener);
        setOpaque(true);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        if (font != null)
            setFont(font);
    }

    public Node getNode()
    {
        return m_node;
    }

    public Dimension getPreferredSize()
    {
        return m_gameTreePanel.getPreferredNodeSize();
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = null;
        if (graphics instanceof Graphics2D)
            graphics2D = (Graphics2D)graphics;
        if (graphics2D != null && ! m_gameTreePanel.getFastPaint())
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        int width = m_gameTreePanel.getNodeWidth();
        int height = m_gameTreePanel.getNodeHeight();
        graphics.setColor(GameTreePanel.m_background);
        graphics.fillRect(0, 0, width, height);
        int halfSize = width / 2;
        if (m_node.getNumberChildren() > 1)
        {
            graphics.setColor(Color.DARK_GRAY);
            if (m_gameTreePanel.isExpanded(m_node))
                graphics.drawLine(halfSize, width, halfSize, height);
            else
            {
                int d1 = width / 2;
                int d2 = (height - width) / 2;
                graphics.drawLine(halfSize, width, halfSize, width + d2);
                graphics.drawLine(halfSize, width + d2, halfSize + d1,
                                  width + d2);
            }
        }
        Move move = m_node.getMove();
        if (m_node.getNumberAddBlack() + m_node.getNumberAddWhite() > 0)
        {
            graphics.setColor(Color.black);
            graphics.fillOval(0, 0, halfSize, halfSize);
            graphics.fillOval(halfSize, halfSize, halfSize, halfSize);
            graphics.setColor(Color.white);
            graphics.fillOval(halfSize, 0, halfSize, halfSize);
            graphics.fillOval(0, halfSize, halfSize, halfSize);
        }
        else if (move == null)
        {
            graphics.setColor(m_colorLightBlue);
            int[] xPoints = { halfSize, width, halfSize, 0 };
            int[] yPoints = { 0, halfSize, width, halfSize };
            graphics.fillPolygon(xPoints, yPoints, 4);
        }        
        else
        {
            if (move.getColor() == GoColor.BLACK)
                graphics.setColor(Color.black);
            else
                graphics.setColor(Color.white);
            graphics.fillOval(0, 0, width, width);
            drawText(graphics);
        }
        if (m_node.getComment() != null
            && ! m_node.getComment().trim().equals(""))
        {
            graphics.setColor(m_colorLightBlue);
            int y = width + (height - width) / 4;
            int d = width / 5;
            graphics.drawLine(d, y, width - d, y);
        }
        if (m_gameTreePanel.isCurrent(m_node))
        {
            graphics.setColor(Color.red);
            int d = width / 6;
            int w = width;
            graphics.drawLine(d, d, 2 * d, d);
            graphics.drawLine(d, d, d, 2 * d);
            graphics.drawLine(d, w - 2 * d - 1, d, w - d - 1);
            graphics.drawLine(d, w - d - 1, 2 * d, w - d - 1);
            graphics.drawLine(w - 2 * d - 1, d, w - d - 1, d);
            graphics.drawLine(w - d - 1, d, w - d - 1, 2 * d);
            graphics.drawLine(w - d - 1, w - d - 1, w - d - 1, w - 2 * d - 1);
            graphics.drawLine(w - d - 1, w - d - 1, w - 2 * d - 1, w - d - 1);
        }
    }

    private final int m_moveNumber;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private static final Color m_colorLightBlue = new Color(103, 122, 164);

    private final GameTreePanel m_gameTreePanel;

    private final Node m_node;

    private void drawText(Graphics graphics)
    {
        int labelMode = m_gameTreePanel.getLabelMode();
        if (labelMode == GameTreePanel.LABEL_NONE)
            return;
        Move move = m_node.getMove();
        int width = m_gameTreePanel.getNodeWidth();
        String text;
        if (labelMode == GameTreePanel.LABEL_MOVE)
        {
            if (move.getPoint() == null)
                return;
            text = move.getPoint().toString();
        }
        else
            text = Integer.toString(m_moveNumber);
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        int textHeight = graphics.getFont().getSize();
        int xText = (width - textWidth) / 2;
        int yText = textHeight + (width - textHeight) / 2;
        if (move.getColor() == GoColor.BLACK)
            graphics.setColor(Color.white);
        else
            graphics.setColor(Color.black);
        graphics.drawString(text, xText, yText);
    }
}

//----------------------------------------------------------------------------
