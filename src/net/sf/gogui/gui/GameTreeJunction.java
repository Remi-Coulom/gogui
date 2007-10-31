// GameTreeJunction.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

class GameTreeJunction
    extends JComponent
{
    public GameTreeJunction(int[] childrenDy, GameTreePanel gameTreePanel)
    {
        m_gameTreePanel = gameTreePanel;
        m_childrenDy = childrenDy;
        setOpaque(false);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        int fullSize = m_gameTreePanel.getNodeFullSize();
        int lastDy = childrenDy[childrenDy.length - 1];
        setPreferredSize(new Dimension(fullSize, lastDy));
    }

    public void paintComponent(Graphics graphics)
    {
        int size = m_gameTreePanel.getNodeSize();
        int fullSize = m_gameTreePanel.getNodeFullSize();
        int halfSize = size / 2;
        graphics.setColor(COLOR_GRID);
        int lastDy = m_childrenDy[m_childrenDy.length - 1];
        graphics.drawLine(halfSize, 0, halfSize, lastDy - fullSize);
        for (int i = 1; i < m_childrenDy.length; ++i)
        {
            int y = m_childrenDy[i] - fullSize;
            graphics.drawLine(halfSize, y, size, y + halfSize);
            graphics.drawLine(size, y + halfSize, fullSize, y + halfSize);
        }
    }

    private final int[] m_childrenDy;

    private final GameTreePanel m_gameTreePanel;

    private static final Color COLOR_GRID = new Color(148, 148, 148);
}
