//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import game.*;
import go.*;

//-----------------------------------------------------------------------------

class GameNode
    extends JComponent
{
    public GameNode(Node node, int moveNumber, int width, int height,
                    GameTreeViewer.Listener listener)
    {
        m_node = node;
        m_moveNumber = moveNumber;
        m_width = width;
        m_height = height;
        m_listener = listener;
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
                public void mouseClicked(MouseEvent event)
                {
                    if (m_listener != null)
                        m_listener.gotoNode(m_node);
                }
            };
        addMouseListener(mouseAdapter);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(m_width, m_height);
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(java.awt.Color.lightGray);
        graphics.fillRect(0, 0, m_width, m_width);
        Move move = m_node.getMove();
        int halfSize = m_width / 2;
        if (m_node.getNumberAddBlack() + m_node.getNumberAddWhite() > 0)
        {
            graphics.setColor(java.awt.Color.black);
            graphics.fillOval(0, 0, halfSize, halfSize);
            graphics.fillOval(halfSize, halfSize, halfSize, halfSize);
            graphics.setColor(java.awt.Color.white);
            graphics.fillOval(halfSize, 0, halfSize, halfSize);
            graphics.fillOval(0, halfSize, halfSize, halfSize);
        }
        else if (move == null)
        {
            graphics.setColor(java.awt.Color.darkGray);
            int[] xPoints = { halfSize, m_width, halfSize, 0 };
            int[] yPoints = { 0, halfSize, m_width, halfSize };
            graphics.fillPolygon(xPoints, yPoints, 4);
        }        
        else
        {
            if (move.getColor() == go.Color.BLACK)
                graphics.setColor(java.awt.Color.black);
            else
                graphics.setColor(java.awt.Color.white);
            graphics.fillOval(0, 0, m_width, m_width);
            String text = Integer.toString(m_moveNumber);
            int textWidth = graphics.getFontMetrics().stringWidth(text);
            int textHeight = graphics.getFont().getSize();
            int xText = (m_width - textWidth) / 2;
            int yText = textHeight + (m_width - textHeight) / 2;
            if (move.getColor() == go.Color.BLACK)
                graphics.setColor(java.awt.Color.white);
            else
                graphics.setColor(java.awt.Color.black);
            graphics.drawString(text, xText, yText);
        }
        if (m_node.getComment() != null
            && ! m_node.getComment().trim().equals(""))
        {
            graphics.setColor(java.awt.Color.black);
            graphics.drawLine(3, m_width + 2, m_width - 3, m_width + 2);
            graphics.drawLine(3, m_width + 4, m_width - 3, m_width + 4);
            graphics.drawLine(3, m_width + 6, m_width - 3, m_width + 6);
        }
        if (m_isCurrent)
        {
            graphics.setColor(java.awt.Color.red);
            int d = m_width / 6;
            int w = m_width;
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

    public void setCurrentNode(boolean isCurrent)
    {
        m_isCurrent = isCurrent;
    }

    private boolean m_isCurrent;

    private int m_moveNumber;

    public int m_width;

    public int m_height;

    private GameTreeViewer.Listener m_listener;

    private Node m_node;
}

//-----------------------------------------------------------------------------

class GameTreePanel
    extends JPanel
    implements Scrollable
{
    public GameTreePanel(GameTreeViewer.Listener listener)
    {
        super(new SpringLayout());
        setBackground(java.awt.Color.lightGray);
        m_nodeSize = 25;
        m_nodeDist = 35;
        Font font = UIManager.getFont("Label.font");
        if (font != null)
        {
            m_nodeSize = font.getSize() * 2;
            if (m_nodeSize % 2 == 0)
                ++m_nodeSize;
            m_nodeDist = font.getSize() * 3;
            if (m_nodeDist % 2 == 0)
                ++m_nodeDist;
        }
        setOpaque(false);
        m_listener = listener;
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(m_nodeDist * 10, m_nodeDist * 3);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction)
    {
        return m_nodeDist * 10;
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
        if (m_gameTree == null)
            return;
        drawGrid(graphics, m_gameTree.getRoot(),
                 m_margin + m_nodeSize / 2, m_margin + m_nodeSize / 2);
        super.paintComponent(graphics);
    }

    public void redrawCurrentNode()
    {
        GameNode gameNode = getGameNode(m_currentNode);
        gameNode.repaint();
    }

    public void update(GameTree gameTree, Node currentNode)
    {
        assert(currentNode != null);
        m_gameTree = gameTree;
        m_currentNode = currentNode;
        removeAll();
        m_map.clear();
        m_maxX = 0;
        m_maxY = 0;
        createNodes(this, m_gameTree.getRoot(), 0, 0, m_margin, m_margin, 0);
        GameNode gameNode = getGameNode(currentNode);
        gameNode.setCurrentNode(true);
        SpringLayout layout = (SpringLayout)getLayout();
        setPreferredSize(new Dimension(m_maxX + m_nodeDist + m_margin,
                                       m_maxY + m_nodeDist + m_margin));
        revalidate();
        scrollRectToVisible(new Rectangle(m_currentNodeX - 2 * m_nodeSize,
                                          m_currentNodeY,
                                          5 * m_nodeSize, 3 * m_nodeSize));
    }

    private int m_currentNodeX;

    private int m_currentNodeY;

    private int m_nodeSize;

    private static final int m_margin = 15;

    private int m_maxX;

    private int m_maxY;

    private int m_nodeDist;

    private Font m_font;

    private GameTree m_gameTree;

    private GameTreeViewer.Listener m_listener;

    private Node m_currentNode;

    private HashMap m_map = new HashMap();

    private int createNodes(Component father, Node node, int x, int y,
                            int dx, int dy, int moveNumber)
    {
        m_maxX = Math.max(x, m_maxX);
        m_maxY = Math.max(y, m_maxY);
        if (node.getMove() != null)
            ++moveNumber;
        GameNode gameNode = new GameNode(node, moveNumber,
                                         m_nodeSize, m_nodeDist, m_listener);
        m_map.put(node, gameNode);
        add(gameNode);
        SpringLayout layout = (SpringLayout)getLayout();
        layout.putConstraint(SpringLayout.WEST, gameNode, dx,
                             SpringLayout.WEST, father);
        layout.putConstraint(SpringLayout.NORTH, gameNode, dy,
                             SpringLayout.NORTH, father);
        int numberChildren = node.getNumberChildren();
        dx = m_nodeDist;
        dy = 0;
        for (int i = 0; i < numberChildren; ++i)
        {
            dy += createNodes(gameNode, node.getChild(i),
                              x + dx, y + dy, dx, dy, moveNumber);
            if (i < numberChildren - 1)
                dy += m_nodeDist;
        }
        if (node == m_currentNode)
        {
            m_currentNodeX = x;
            m_currentNodeY = y;
        }        
        return dy;
    }

    private int drawGrid(Graphics graphics, Node node, int x, int y)
    {
        int numberChildren = node.getNumberChildren();
        int offset = m_nodeSize / 2;        
        int xChild = x + m_nodeDist;
        int yChild = y;
        for (int i = 0; i < numberChildren; ++i)
        {
            graphics.setColor(java.awt.Color.DARK_GRAY);
            graphics.drawLine(x, y, x, yChild);
            graphics.drawLine(x, yChild, xChild, yChild);
            yChild = drawGrid(graphics, node.getChild(i), xChild, yChild);
            if (i < numberChildren - 1)
                yChild += m_nodeDist;
        }
        return yChild;
    }

    private GameNode getGameNode(Node node)
    {
        return (GameNode)m_map.get(node);
    }
}

//-----------------------------------------------------------------------------

public class GameTreeViewer
    extends JDialog
{
    public interface Listener
    {
        public abstract void gotoNode(Node node);
    }

    public GameTreeViewer(Frame owner, Listener listener)
    {
        super(owner, "GoGui: Game Tree");
        Container contentPane = getContentPane();
        m_panel = new GameTreePanel(listener);
        m_scrollPane =
            new JScrollPane(m_panel,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        m_scrollPane.getViewport().setBackground(java.awt.Color.lightGray);
        contentPane.add(m_scrollPane, BorderLayout.CENTER);
        pack();
        m_listener = listener;
    }

    public void redrawCurrentNode()
    {
        m_panel.redrawCurrentNode();
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

    private Listener m_listener;
}

//-----------------------------------------------------------------------------

