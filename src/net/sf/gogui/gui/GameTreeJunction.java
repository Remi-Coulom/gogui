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

class GameTreeJunction
    extends JComponent
{
    public GameTreeJunction(int[] childrenDy, GameTreePanel gameTreePanel)
    {
        m_gameTreePanel = gameTreePanel;
        m_childrenDy = childrenDy;
        setOpaque(true);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        int nodeWidth = m_gameTreePanel.getNodeWidth();
        int lastDy = childrenDy[childrenDy.length - 1];
        setPreferredSize(new Dimension(nodeWidth, lastDy));
    }

    public void paintComponent(Graphics graphics)
    {
        int width = m_gameTreePanel.getNodeWidth();
        int height = m_gameTreePanel.getNodeHeight();
        graphics.setColor(GameTreePanel.m_background);
        graphics.fillRect(0, 0, width, height);
        int halfSize = width / 2;
        int halfHeight = height / 2;
        graphics.setColor(Color.DARK_GRAY);
        int lastDy = m_childrenDy[m_childrenDy.length - 1];
        graphics.drawLine(halfSize, 0, halfSize, lastDy - height);
        for (int i = 1; i < m_childrenDy.length; ++i)
        {
            int y = m_childrenDy[i] - height;
            graphics.drawLine(halfSize, y, width, y + halfSize);
        }
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final int[] m_childrenDy;

    private final GameTreePanel m_gameTreePanel;

}

//----------------------------------------------------------------------------
