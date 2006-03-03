//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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
        setOpaque(false);
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
        int size = m_gameTreePanel.getNodeSize();
        int fullSize = m_gameTreePanel.getNodeFullSize();
        int halfSize = size / 2;
        int numberChildren = m_node.getNumberChildren();
        boolean isExpanded = m_gameTreePanel.isExpanded(m_node);
        graphics.setColor(Color.DARK_GRAY);
        if ((numberChildren > 1 &&
             (isExpanded || ! m_gameTreePanel.getShowSubtreeSizes()))
            || numberChildren == 1)
            graphics.drawLine(halfSize, halfSize, fullSize, halfSize);
        if (numberChildren > 1)
        {
            if (isExpanded)
                graphics.drawLine(halfSize, size, halfSize, fullSize);
            else
            {
                graphics.drawLine(halfSize, halfSize, size, size);
                graphics.drawLine(size, size, fullSize - size / 5, size);
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
        if (m_node.getComment() != null
            && ! m_node.getComment().trim().equals(""))
        {
            graphics.setColor(m_colorLightBlue);
            int y = size + (fullSize - size) / 4;
            int d = size / 5;
            graphics.drawLine(d, y, size - d, y);
        }
        if (m_gameTreePanel.isCurrent(m_node))
        {
            graphics.setColor(Color.red);
            int d = size / 6;
            int w = size;
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

//----------------------------------------------------------------------------
