//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import javax.swing.JComponent;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Move;

class GameTreeNode
    extends JComponent
{
    public GameTreeNode(ConstNode node, int moveNumber,
                        GameTreePanel gameTreePanel,
                        MouseListener mouseListener, Font font,
                        Dimension size)
    {
        m_gameTreePanel = gameTreePanel;
        m_node = node;
        m_moveNumber = moveNumber;
        addMouseListener(mouseListener);
        setOpaque(false);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        if (font != null)
            setFont(font);
        setPreferredSize(size);
    }

    public ConstNode getNode()
    {
        return m_node;
    }

    public void paintComponent(Graphics graphics)
    {
        int size = m_gameTreePanel.getNodeSize();
        int fullSize = m_gameTreePanel.getNodeFullSize();
        int halfSize = size / 2;
        int numberChildren = m_node.getNumberChildren();
        boolean isExpanded = m_gameTreePanel.isExpanded(m_node);
        if (m_gameTreePanel.isCurrent(m_node))
        {
            graphics.setColor(COLOR_CURSOR);
            graphics.fillRect(0, 0, size, fullSize - 1);
        }
        graphics.setColor(Color.DARK_GRAY);
        if ((numberChildren > 1 &&
             (isExpanded || ! m_gameTreePanel.getShowSubtreeSizes()))
            || numberChildren == 1)
            graphics.drawLine(size, halfSize, fullSize, halfSize);
        if (numberChildren > 1)
        {
            if (isExpanded)
                graphics.drawLine(halfSize, size, halfSize, fullSize);
            else
            {
                int d = size * 7 / 10;
                graphics.drawLine(d, d, size, size);
                graphics.drawLine(size, size, fullSize - size / 5, size);
            }
        }
        Move move = m_node.getMove();
        if (m_node.hasSetup())
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
            graphics.setColor(COLOR_LIGHT_BLUE);
            int[] xPoints = { halfSize, size, halfSize, 0 };
            int[] yPoints = { 0, halfSize, size, halfSize };
            graphics.fillPolygon(xPoints, yPoints, 4);
        }        
        else
        {
            if (move.getColor() == GoColor.BLACK)
                graphics.setColor(Color.black);
            else
                graphics.setColor(Color.white);
            graphics.fillOval(0, 0, size, size);
            drawText(graphics);
        }
        if (m_node.hasComment())
        {
            graphics.setColor(COLOR_LIGHT_BLUE);
            int y = size + (fullSize - size) / 4;
            int d = size / 5;
            graphics.drawLine(d, y, size - d, y);
        }
    }

    private final int m_moveNumber;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private static final Color COLOR_LIGHT_BLUE = new Color(103, 122, 164);

    private static final Color COLOR_CURSOR = new Color(142, 168, 226);

    private final GameTreePanel m_gameTreePanel;

    private final ConstNode m_node;

    private void drawText(Graphics graphics)
    {
        int labelMode = m_gameTreePanel.getLabelMode();
        if (labelMode == GameTreePanel.LABEL_NONE)
            return;
        Move move = m_node.getMove();
        int size = m_gameTreePanel.getNodeSize();
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
        int xText = (size - textWidth) / 2;
        int yText = textHeight + (size - textHeight) / 2;
        if (move.getColor() == GoColor.BLACK)
            graphics.setColor(Color.white);
        else
            graphics.setColor(Color.black);
        graphics.drawString(text, xText, yText);
    }
}

