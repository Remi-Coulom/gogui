//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import javax.swing.*;
import game.*;
import go.*;

//-----------------------------------------------------------------------------

class GameTreePanel
    extends JComponent
    implements Scrollable
{
    public GameTreePanel()
    {
        Font font = UIManager.getFont("Label.font");        
        if (font != null)
        {
            font = font.deriveFont(m_nodeSize / 2);
            if (font != null)
                setFont(font);
        }
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(m_nodeDist * 10, m_nodeDist * 3);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction)
    {
        return m_nodeDist;
    }

    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
    
    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect,
                                          int orientation, int direction)
    {
        return m_nodeDist;
    }

    public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        if (m_gameTree == null)
            return;
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        paintNode(graphics, m_gameTree.getRoot(),
                  m_nodeDist / 2, m_nodeDist / 2, 0);
    }

    public void update(GameTree gameTree, Node currentNode)
    {
        m_gameTree = gameTree;
        m_currentNode = currentNode;
        findPreferredSize();
        revalidate();
        scrollRectToVisible(new Rectangle(m_currentNodeX, m_currentNodeY,
                                          m_nodeSize, m_nodeSize));
    }

    private int m_maxX = m_nodeDist * 10;

    private int m_maxY = m_nodeDist * 3;

    private int m_currentNodeX;

    private int m_currentNodeY;

    private static final int m_nodeSize = 25;
    
    private static final int m_nodeDist = 35;

    private GameTree m_gameTree;
    
    private Node m_currentNode;

    private void findPreferredSize()
    {
        if (m_gameTree == null)
            return;
        m_maxX = 0;
        m_maxY = 0;
        findPreferredSize(m_gameTree.getRoot(),
                          m_nodeDist / 2, m_nodeDist / 2);
        setPreferredSize(new Dimension(m_maxX, m_maxY));
    }

    private int findPreferredSize(Node node, int x, int y)
    {
        if (x + m_nodeDist > m_maxX)
            m_maxX = x + m_nodeDist;
        if (y + m_nodeDist > m_maxY)
            m_maxY = y + m_nodeDist;
        int numberChildren = node.getNumberChildren();
        int xChild = x + m_nodeDist;
        int yChild = y;
        for (int i = 0; i < numberChildren; ++i)
        {
            yChild = findPreferredSize(node.getChild(i), xChild, yChild);
            if (i < numberChildren - 1)
                yChild += m_nodeDist;
        }
        if (node == m_currentNode)
        {
            m_currentNodeX = x;
            m_currentNodeY = y;
        }
        return yChild;
    }

    private int paintNode(Graphics graphics, Node node, int x, int y,
                           int moveNumber)
    {
        Move move = node.getMove();
        if (move != null)
            ++moveNumber;            
        int numberChildren = node.getNumberChildren();
        int xChild = x + m_nodeDist;
        int yChild = y;
        int offset = m_nodeSize / 2;
        int totalWidth = 0;
        for (int i = 0; i < numberChildren; ++i)
        {
            graphics.setColor(java.awt.Color.DARK_GRAY);
            graphics.drawLine(x + offset, y + offset,
                              x + offset, yChild + offset);
            graphics.drawLine(x + offset, yChild + offset,
                              xChild + offset, yChild + offset);
            yChild = paintNode(graphics, node.getChild(i), xChild, yChild,
                               moveNumber);
            if (i < numberChildren - 1)
                yChild += m_nodeDist;
        }
        if (move == null)
        {
            graphics.setColor(java.awt.Color.blue);
            int margin = 7;
            graphics.fillRect(x + margin, y + margin,
                              m_nodeSize - 2 * margin + 1,
                              m_nodeSize - 2 * margin + 1);
        }
        else
        {
            if (move.getColor() == go.Color.BLACK)
                graphics.setColor(java.awt.Color.black);
            else
                graphics.setColor(java.awt.Color.white);
            graphics.fillOval(x, y, m_nodeSize, m_nodeSize);
            String text = Integer.toString(moveNumber);
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            int textHeight = graphics.getFont().getSize();
            int xText = x + (m_nodeSize - textWidth) / 2;
            int yText = y + textHeight + (m_nodeSize - textHeight) / 2;
            if (move.getColor() == go.Color.BLACK)
                graphics.setColor(java.awt.Color.white);
            else
                graphics.setColor(java.awt.Color.black);
            graphics.drawString(text, xText, yText);
        }
        if (node == m_currentNode)
        {
            graphics.setColor(java.awt.Color.red);
            graphics.drawRect(x, y, m_nodeSize, m_nodeSize);
        }
        return yChild;
    }
}

//-----------------------------------------------------------------------------

public class GameTreeViewer
    extends JDialog
{
    public GameTreeViewer(Frame owner)
    {
        super(owner, "GoGui: Game Tree");
        Container contentPane = getContentPane();
        m_panel = new GameTreePanel();
        m_scrollPane =
            new JScrollPane(m_panel,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        contentPane.add(m_scrollPane, BorderLayout.CENTER);
        pack();
    }

    public void toTop()
    {
        setVisible(true);
        toFront();
    }

    public void update(GameTree gameTree, Node currentNode)
    {
        m_panel.update(gameTree, currentNode);
        repaint();
    }

    private GameTreePanel m_panel;

    private JScrollPane m_scrollPane;
}

//-----------------------------------------------------------------------------

