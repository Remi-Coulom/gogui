//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import game.GameTree;
import game.Node;
import game.NodeUtils;
import go.Move;
import utils.GuiUtils;
import utils.Platform;
import utils.Preferences;

//----------------------------------------------------------------------------

class GameNode
    extends JComponent
{
    public GameNode(Node node, int moveNumber, GameTreePanel gameTreePanel,
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
        Graphics2D graphics2D = (Graphics2D)graphics;
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
            graphics.setColor(java.awt.Color.DARK_GRAY);
            if (! m_gameTreePanel.isExpanded(m_node))
            {
                int d1 = width / 2;
                int d2 = (height - width) / 2;
                graphics.drawLine(halfSize, width, halfSize, width + d2);
                graphics.drawLine(halfSize, width + d2, halfSize + d1,
                                  width + d2);
            }
            else
                graphics.drawLine(halfSize, width, halfSize, height);
        }
        Move move = m_node.getMove();
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
            graphics.setColor(m_colorLightBlue);
            int[] xPoints = { halfSize, width, halfSize, 0 };
            int[] yPoints = { 0, halfSize, width, halfSize };
            graphics.fillPolygon(xPoints, yPoints, 4);
        }        
        else
        {
            if (move.getColor() == go.Color.BLACK)
                graphics.setColor(java.awt.Color.black);
            else
                graphics.setColor(java.awt.Color.white);
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
            graphics.setColor(java.awt.Color.red);
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

    private int m_moveNumber;

    private static final java.awt.Color m_colorLightBlue
        = new java.awt.Color(103, 122, 164);

    private GameTreePanel m_gameTreePanel;

    private Node m_node;

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
        if (move.getColor() == go.Color.BLACK)
            graphics.setColor(java.awt.Color.white);
        else
            graphics.setColor(java.awt.Color.black);
        graphics.drawString(text, xText, yText);
    }
}

//----------------------------------------------------------------------------

class GameTreePanel
    extends JPanel
    implements Scrollable
{
    public static final int LABEL_NUMBER = 0;

    public static final int LABEL_MOVE = 1;

    public static final int LABEL_NONE = 2;

    public static final int SIZE_LARGE = 0;

    public static final int SIZE_NORMAL = 1;

    public static final int SIZE_SMALL = 2;

    public static final int SIZE_TINY = 3;

    public static final java.awt.Color m_background =
        new java.awt.Color(192, 192, 192);

    public GameTreePanel(JFrame owner, GameTreeViewer.Listener listener,
                         boolean fastPaint, int labelMode, int sizeMode)
    {
        super(new SpringLayout());
        m_owner = owner;
        m_fastPaint = fastPaint;
        setBackground(m_background);
        m_labelMode = labelMode;
        m_sizeMode = sizeMode;
        computeSizes(sizeMode);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        setOpaque(true);
        setAutoscrolls(true);
        MouseMotionListener doScrollRectToVisible = new MouseMotionAdapter()
            {
                public void mouseDragged(MouseEvent e)
                {
                    Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
                    ((JPanel)e.getSource()).scrollRectToVisible(r);
                }
            };
        addMouseMotionListener(doScrollRectToVisible);
        m_listener = listener;
        m_mouseListener = new MouseAdapter()
            {
                public void mouseClicked(MouseEvent event)
                {
                    if (event.getButton() != MouseEvent.BUTTON1)
                        return;
                    GameNode gameNode = (GameNode)event.getSource();
                    if (event.getClickCount() == 2)
                    {
                        Node node = gameNode.getNode();
                        if (node.getNumberChildren() > 1)
                        {
                            if (m_expanded.contains(node))
                                hideSubtree(node);
                            else
                                showVariations(node);
                        }
                    }
                    else
                        gotoNode(gameNode.getNode());
                }

                public void mousePressed(MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        GameNode gameNode = (GameNode)event.getSource();
                        int x = event.getX();
                        int y = event.getY();
                        showPopup(x, y, gameNode);
                    }
                }

                public void mouseReleased(MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        GameNode gameNode = (GameNode)event.getSource();
                        int x = event.getX();
                        int y = event.getY();
                        showPopup(x, y, gameNode);
                    }
                }
            };
    }

    public Node getCurrentNode()
    {
        return m_currentNode;
    }

    public boolean getFastPaint()
    {
        return m_fastPaint;
    }
    
    public int getLabelMode()
    {
        return m_labelMode;
    }
    
    public int getNodeHeight()
    {
        return m_nodeHeight;
    }
    
    public int getNodeWidth()
    {
        return m_nodeWidth;
    }
    
    public Dimension getPreferredNodeSize()
    {
        return m_preferredNodeSize;
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(m_nodeDist * 10, m_nodeDist * 3);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction)
    {
        int result;
        if (orientation == SwingConstants.VERTICAL)
            result = visibleRect.height;
        else
            result = visibleRect.width;
        result = (result / m_nodeDist) * m_nodeDist;
        return result;
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

    public int getSizeMode()
    {
        return m_sizeMode;
    }
    
    public void gotoNode(Node node)
    {
        if (m_listener != null)
            m_listener.gotoNode(node);
    }

    public boolean isCurrent(Node node)
    {
        return node == m_currentNode;
    }

    public boolean isExpanded(Node node)
    {
        return m_expanded.contains(node);
    }

    public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        if (m_gameTree == null)
            return;
        graphics.setColor(java.awt.Color.DARK_GRAY);
        drawGrid(graphics, m_gameTree.getRoot(),
                 m_margin + m_nodeWidth / 2, m_margin + m_nodeWidth / 2);
    }

    public void redrawCurrentNode()
    {
        GameNode gameNode = getGameNode(m_currentNode);
        gameNode.repaint();
    }

    public void scrollToCurrent()
    {
        scrollRectToVisible(new Rectangle(m_currentNodeX - 2 * m_nodeWidth,
                                          m_currentNodeY,
                                          5 * m_nodeWidth, 3 * m_nodeWidth));
    }

    public void setLabelMode(int mode)
    {
        switch (mode)
        {
        case LABEL_NUMBER:
        case LABEL_MOVE:
        case LABEL_NONE:
            if (mode != m_labelMode)
            {
                m_labelMode = mode;
                update(m_gameTree, m_currentNode);
            }
            break;
        default:
            break;
        }
    }

    /** Only used for a workaround on Mac Java 1.4.2,
        which causes the scrollpane to lose focus after a new layout of
        this panel. If scrollPane is not null, a requestFocusOnWindow will
        be called after each new layout
    */
    public void setScrollPane(JScrollPane scrollPane)
    {
        m_scrollPane = scrollPane;
    }

    public void setSizeMode(int mode)
    {
        switch (mode)
        {
        case SIZE_LARGE:
        case SIZE_NORMAL:
        case SIZE_SMALL:
        case SIZE_TINY:
            if (mode != m_sizeMode)
            {
                m_sizeMode = mode;
                computeSizes(m_sizeMode);
                update(m_gameTree, m_currentNode);
            }
            break;
        default:
            break;
        }
    }

    public void update(GameTree gameTree, Node currentNode)
    {
        assert(currentNode != null);
        boolean gameTreeChanged = (gameTree != m_gameTree);
        if (gameTreeChanged)
            m_expanded.clear();
        ensureVisible(currentNode);
        m_gameTree = gameTree;
        m_currentNode = currentNode;
        removeAll();
        m_map.clear();
        m_maxX = 0;
        m_maxY = 0;
        try
        {
            Node root = m_gameTree.getRoot();
            createNodes(this, root, 0, 0, m_margin, m_margin, 0);
            if (gameTreeChanged)
            {
                if (NodeUtils.subtreeGreaterThan(root, 10000))
                    showVariations(root);
                else
                    showSubtree(root);
            }
        }
        catch (OutOfMemoryError e)
        {
            m_expanded.clear();
            removeAll();
            SimpleDialogs.showError(m_owner,
                                    "Could not show game tree\n" + 
                                    "Out of memory");
            update(gameTree, currentNode);
        }
        setPreferredSize(new Dimension(m_maxX + m_nodeDist + m_margin,
                                       m_maxY + m_nodeDist + m_margin));
        revalidate();
        scrollToCurrent();
        if (m_scrollPane != null)
            m_scrollPane.requestFocusInWindow();
    }

    public void update(Node currentNode)
    {
        assert(currentNode != null);
        if (ensureVisible(currentNode))
        {
            update(m_gameTree, currentNode);
            return;
        }
        GameNode gameNode = getGameNode(m_currentNode);
        gameNode.repaint();
        gameNode = getGameNode(currentNode);
        java.awt.Point location = gameNode.getLocation();
        m_currentNodeX = location.x;
        m_currentNodeY = location.y;
        gameNode.repaint();
        m_currentNode = currentNode;
        scrollToCurrent();
        if (m_scrollPane != null)
            m_scrollPane.requestFocusInWindow();
    }

    private boolean m_fastPaint;

    private int m_currentNodeX;

    private int m_currentNodeY;

    private int m_labelMode;

    private int m_sizeMode;

    private int m_nodeWidth;

    private int m_nodeHeight;

    private static final int m_margin = 15;

    private int m_maxX;

    private int m_maxY;

    private int m_nodeDist;

    private Dimension m_preferredNodeSize;

    private Font m_font;

    private GameTree m_gameTree;

    private GameTreeViewer.Listener m_listener;

    private JFrame m_owner;

    /** Used for focus workaround on Mac Java 1.4.2 if not null. */
    private JScrollPane m_scrollPane;

    private Node m_currentNode;

    private Node m_popupNode;

    private HashMap m_map = new HashMap(500, 0.8f);

    private HashSet m_expanded = new HashSet(200);

    private MouseListener m_mouseListener;

    private java.awt.Point m_popupLocation;

    private void computeSizes(int sizeMode)
    {
        double fontScale;
        switch (sizeMode)
        {
        case SIZE_LARGE:
            fontScale = 1.0;
            break;
        case SIZE_NORMAL:
            fontScale = 0.7;
            break;
        case SIZE_SMALL:
            fontScale = 0.5;
            break;
        case SIZE_TINY:
            fontScale = 0.2;
            break;
        default:
            fontScale = 0.7;
            assert(false);
        }
        m_nodeWidth = 25;
        m_nodeDist = 35;
        Font font = UIManager.getFont("Label.font");
        if (font != null)
        {
            Font derivedFont
                = font.deriveFont((float)(font.getSize() * fontScale));
            if (derivedFont != null)
                font = derivedFont;
        }
        if (font != null)
        {
            m_nodeWidth = font.getSize() * 2;
            if (m_nodeWidth % 2 == 0)
                ++m_nodeWidth;
            m_nodeDist = font.getSize() * 3;
            if (m_nodeDist % 2 == 0)
                ++m_nodeDist;
        }
        m_font = font;
        m_preferredNodeSize = new Dimension(m_nodeWidth, m_nodeDist);
    }

    private int createNodes(Component father, Node node, int x, int y,
                            int dx, int dy, int moveNumber)
    {
        m_maxX = Math.max(x, m_maxX);
        m_maxY = Math.max(y, m_maxY);
        if (node.getMove() != null)
            ++moveNumber;
        m_nodeHeight = m_nodeDist;
        GameNode gameNode =
            new GameNode(node, moveNumber, this, m_mouseListener, m_font);
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
        int maxChildren = numberChildren;
        boolean notExpanded =
            (numberChildren > 1 && ! m_expanded.contains(node));
        if (notExpanded)
            maxChildren = Math.min(numberChildren, 1);
        for (int i = 0; i < maxChildren; ++i)
        {
            dy += createNodes(gameNode, node.getChild(i),
                              x + dx, y + dy, dx, dy, moveNumber);
            if (! notExpanded && i < numberChildren - 1)
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
        int xChild = x + m_nodeDist;
        int yChild = y;
        int lastY = y + m_nodeWidth;
        boolean notExpanded =
            (numberChildren > 1 && ! m_expanded.contains(node));
        int maxChildren = numberChildren;
        if (notExpanded)
            maxChildren = Math.min(numberChildren, 1);
        for (int i = 0; i < maxChildren; ++i)
        {
            if (i > 0)
                graphics.drawLine(x, lastY, x, yChild);
            graphics.drawLine(x, yChild, xChild, yChild);
            lastY = yChild;
            yChild = drawGrid(graphics, node.getChild(i), xChild, yChild);
            if (! notExpanded && i < numberChildren - 1)
                yChild += m_nodeDist;
        }
        return yChild;
    }

    private GameNode getGameNode(Node node)
    {
        return (GameNode)m_map.get(node);
    }

    private boolean ensureVisible(Node node)
    {
        boolean changed = false;
        while (node != null)
        {
            Node father = node.getFather();
            if (father != null && father.getChild() != node)
                if (m_expanded.add(father))
                    changed = true;
            node = father;
        }
        return changed;
    }

    private void hideOthers(Node node)
    {
        m_expanded.clear();
        ensureVisible(node);
        update(m_gameTree, m_currentNode);
    }

    private void hideSubtree(Node root)
    {
        boolean changed = false;
        boolean currentChanged = false;
        int depth = NodeUtils.getDepth(root);
        Node node = root;
        while (node != null)
        {
            if (node == m_currentNode)
            {
                m_currentNode = root;
                currentChanged = true;
                changed = true;
            }
            if (m_expanded.remove(node))
                changed = true;
            node = NodeUtils.nextNode(node, depth);
        }
        if (currentChanged)
        {
            gotoNode(m_currentNode);
            root = m_currentNode;
        }
        if (changed)
        {
            update(m_gameTree, m_currentNode);
            scrollTo(root);
        }
    }

    private void nodeInfo(java.awt.Point location, Node node)
    {
        String nodeInfo = NodeUtils.nodeInfo(node);
        String title = "Node Info";
        TextViewer textViewer =
            new TextViewer(null, title, nodeInfo, true, null);
        textViewer.setLocation(location);
        textViewer.setVisible(true);
    }

    private void scrollTo(Node node)
    {
        GameNode gameNode = getGameNode(node);
        Rectangle rectangle = new Rectangle();
        rectangle.x = gameNode.getLocation().x;
        rectangle.y = gameNode.getLocation().y;
        rectangle.width = m_nodeWidth;
        rectangle.height = m_nodeDist;
        scrollRectToVisible(rectangle);
    }

    private void showPopup(int x, int y, GameNode gameNode)
    {
        Node node = gameNode.getNode();
        m_popupNode = node;
        ActionListener listener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    String command = event.getActionCommand();
                    if (command.equals("goto"))
                        gotoNode(m_popupNode);
                    else if (command.equals("show-variations"))
                        showVariations(m_popupNode);
                    else if (command.equals("show-subtree"))
                        showSubtree(m_popupNode);
                    else if (command.equals("hide-others"))
                        hideOthers(m_popupNode);
                    else if (command.equals("hide-subtree"))
                        hideSubtree(m_popupNode);
                    else if (command.equals("node-info"))
                        nodeInfo(m_popupLocation, m_popupNode);
                    else if (command.equals("tree-info"))
                        treeInfo(m_popupLocation, m_popupNode);
                    else
                        assert(false);
                }
            };
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item;
        item = new JMenuItem("Go To");
        item.setActionCommand("goto");
        item.addActionListener(listener);
        popup.add(item);
        popup.addSeparator();
        item = new JMenuItem("Hide Variations");
        if (node.getNumberChildren() == 0)
            item.setEnabled(false);
        item.setActionCommand("hide-subtree");
        item.addActionListener(listener);
        popup.add(item);
        item = new JMenuItem("Hide Others");
        item.setActionCommand("hide-others");
        item.addActionListener(listener);
        popup.add(item);
        item = new JMenuItem("Show Variations");
        if (m_expanded.contains(node) || node.getNumberChildren() <= 1)
            item.setEnabled(false);
        item.setActionCommand("show-variations");
        item.addActionListener(listener);
        popup.add(item);
        item = new JMenuItem("Show Subtree");
        if (node.getNumberChildren() == 0)
            item.setEnabled(false);
        item.setActionCommand("show-subtree");
        item.addActionListener(listener);
        popup.add(item);
        popup.addSeparator();
        item = new JMenuItem("Node Info");
        item.setActionCommand("node-info");
        item.addActionListener(listener);
        popup.add(item);
        item = new JMenuItem("Subtree Statistics");
        item.setActionCommand("tree-info");
        item.addActionListener(listener);
        popup.add(item);
        popup.show(gameNode, x, y);
        m_popupLocation = popup.getLocationOnScreen();
    }

    private void showSubtree(Node root)
    {
        if (NodeUtils.subtreeGreaterThan(root, 10000)
            && ! SimpleDialogs.showQuestion(m_owner,
                                            "Really expand large subtree?"))
            return;
        boolean changed = false;
        Node node = root;
        int depth = NodeUtils.getDepth(node);
        while (node != null)
        {
            if (node.getNumberChildren() > 1 && m_expanded.add(node))
                changed = true;
            node = NodeUtils.nextNode(node, depth);
        }
        if (changed)
        {
            update(m_gameTree, m_currentNode);
            scrollTo(root);
        }
    }

    private void showVariations(Node node)
    {
        if (node.getNumberChildren() > 1 && m_expanded.add(node))
        {
            update(m_gameTree, m_currentNode);
            scrollTo(node);
        }
    }

    private void treeInfo(java.awt.Point location, Node node)
    {
        String treeInfo = NodeUtils.treeInfo(node);
        String title = "Subtree Info";
        TextViewer textViewer =
            new TextViewer(null, title, treeInfo, true, null);
        textViewer.setLocation(location);
        textViewer.setVisible(true);
    }
}

//----------------------------------------------------------------------------

public class GameTreeViewer
    extends JFrame
    implements ActionListener
{
    public interface Listener
    {
        void gotoNode(Node node);

        void cbAnalyze();

        void cbGtpShell();

        void disposeGameTree();

        void toTop();
    }

    public GameTreeViewer(Listener listener, boolean fastPaint,
                          Preferences prefs)
    {
        super("Game Tree");
        m_prefs = prefs;
        setPrefsDefaults(prefs);
        int sizeMode = prefs.getInt("gametree-size");
        int labelMode = prefs.getInt("gametree-labels");
        GuiUtils.setGoIcon(this);
        createMenuBar(labelMode, sizeMode);
        Container contentPane = getContentPane();
        m_listener = listener;
        m_panel =
            new GameTreePanel(this, listener, fastPaint, labelMode, sizeMode);
        m_scrollPane =
            new JScrollPane(m_panel,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        m_panel.setScrollPane(m_scrollPane);
        m_scrollPane.setFocusable(true);
        m_scrollPane.setFocusTraversalKeysEnabled(false);
        JViewport viewport = m_scrollPane.getViewport();
        viewport.setBackground(GameTreePanel.m_background);
        contentPane.add(m_scrollPane, BorderLayout.CENTER);
        viewport.setFocusTraversalKeysEnabled(false);
        setFocusTraversalKeysEnabled(false);
        m_scrollPane.requestFocusInWindow();
        // Necessary for Mac Java 1.4.2, otherwise scrollpane will not have
        // focus after window is re-activated
        addWindowListener(new WindowAdapter()
            {
                public void windowActivated(WindowEvent e)
                {
                    m_scrollPane.requestFocusInWindow();
                }

                public void windowClosing(WindowEvent event)
                {
                    m_listener.disposeGameTree();
                }
            });
        pack();
    }

    public void actionPerformed(ActionEvent event)
    {
        String command = event.getActionCommand();
        if (command.equals("analyze"))
            m_listener.cbAnalyze();
        else if (command.equals("close"))
            setVisible(false);
        else if (command.equals("gogui"))
            m_listener.toTop();
        else if (command.equals("goto-current"))
            m_panel.scrollToCurrent();
        else if (command.equals("gtp-shell"))
            m_listener.cbGtpShell();
        else if (command.equals("label-move"))
            cbLabelMode(GameTreePanel.LABEL_MOVE);
        else if (command.equals("label-number"))
            cbLabelMode(GameTreePanel.LABEL_NUMBER);
        else if (command.equals("label-none"))
            cbLabelMode(GameTreePanel.LABEL_NONE);
        else if (command.equals("size-large"))
            cbSizeMode(GameTreePanel.SIZE_LARGE);
        else if (command.equals("size-normal"))
            cbSizeMode(GameTreePanel.SIZE_NORMAL);
        else if (command.equals("size-small"))
            cbSizeMode(GameTreePanel.SIZE_SMALL);
        else if (command.equals("size-tiny"))
            cbSizeMode(GameTreePanel.SIZE_TINY);
        else
            assert(false);
    }

    public void redrawCurrentNode()
    {
        m_panel.redrawCurrentNode();
    }

    public void toTop()
    {
        setState(Frame.NORMAL);
        setVisible(true);
        toFront();
    }

    public void update(GameTree gameTree, Node currentNode)
    {
        m_panel.update(gameTree, currentNode);
        repaint();
    }

    public void update(Node currentNode)
    {
        m_panel.update(currentNode);
    }

    private static final int m_shortcutKeyMask =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private GameTreePanel m_panel;

    private JMenuItem m_itemLabelNumber;

    private JMenuItem m_itemLabelMove;

    private JMenuItem m_itemLabelNone;

    private JMenuItem m_itemSizeLarge;

    private JMenuItem m_itemSizeNormal;

    private JMenuItem m_itemSizeSmall;

    private JMenuItem m_itemSizeTiny;

    private JScrollPane m_scrollPane;

    private Listener m_listener;

    private Preferences m_prefs;

    private JMenuItem addMenuItem(JMenu menu, JMenuItem item, int mnemonic,
                                  String command)
    {
        item.addActionListener(this);
        item.setActionCommand(command);
        item.setMnemonic(mnemonic);
        menu.add(item);
        return item;
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  String command)
    {
        JMenuItem item = new JMenuItem(label);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private JMenuItem addMenuItem(JMenu menu, String label, int mnemonic,
                                  int accel, int modifier, String command)
    {
        JMenuItem item = addMenuItem(menu, label, mnemonic, command);
        KeyStroke accelerator = KeyStroke.getKeyStroke(accel, modifier); 
        item.setAccelerator(accelerator);
        return item;
    }

    private JMenuItem addRadioItem(JMenu menu, ButtonGroup group,
                                   String label, int mnemonic, String command)
    {
        JMenuItem item = new JRadioButtonMenuItem(label);
        group.add(item);
        return addMenuItem(menu, item, mnemonic, command);
    }

    private void cbLabelMode(int mode)
    {
        m_prefs.setInt("gametree-labels", mode);
        m_panel.setLabelMode(mode);
    }

    private void cbSizeMode(int mode)
    {
        m_prefs.setInt("gametree-size", mode);
        m_panel.setSizeMode(mode);
    }

    private JMenu createMenu(String name, int mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private void createMenuBar(int labelMode, int sizeMode)
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createMenuWindows());
        menuBar.add(createMenuView(labelMode, sizeMode));
        setJMenuBar(menuBar);
    }

    private JMenu createMenuView(int labelMode, int sizeMode)
    {
        JMenu menu = createMenu("View", KeyEvent.VK_V);
        JMenu menuLabel = createMenu("Labels", KeyEvent.VK_L);
        ButtonGroup group = new ButtonGroup();
        m_itemLabelNumber = addRadioItem(menuLabel, group, "Move Number",
                                         KeyEvent.VK_N, "label-number");
        m_itemLabelMove = addRadioItem(menuLabel, group, "Move",
                                       KeyEvent.VK_M, "label-move");
        m_itemLabelNone = addRadioItem(menuLabel, group, "None",
                                       KeyEvent.VK_O, "label-none");
        switch (labelMode)
        {
        case GameTreePanel.LABEL_NUMBER:
            m_itemLabelNumber.setSelected(true);
            break;
        case GameTreePanel.LABEL_MOVE:
            m_itemLabelMove.setSelected(true);
            break;
        case GameTreePanel.LABEL_NONE:
            m_itemLabelNone.setSelected(true);
            break;
        default:
            break;
        }
        menu.add(menuLabel);
        JMenu menuSize = createMenu("Size", KeyEvent.VK_S);
        group = new ButtonGroup();
        m_itemSizeLarge = addRadioItem(menuSize, group, "Large",
                                        KeyEvent.VK_L, "size-large");
        m_itemSizeNormal = addRadioItem(menuSize, group, "Normal",
                                        KeyEvent.VK_N, "size-normal");
        m_itemSizeSmall = addRadioItem(menuSize, group, "Small",
                                        KeyEvent.VK_S, "size-small");
        m_itemSizeTiny = addRadioItem(menuSize, group, "Tiny",
                                      KeyEvent.VK_T, "size-tiny");
        switch (sizeMode)
        {
        case GameTreePanel.SIZE_LARGE:
            m_itemSizeLarge.setSelected(true);
            break;
        case GameTreePanel.SIZE_NORMAL:
            m_itemSizeNormal.setSelected(true);
            break;
        case GameTreePanel.SIZE_SMALL:
            m_itemSizeSmall.setSelected(true);
            break;
        case GameTreePanel.SIZE_TINY:
            m_itemSizeTiny.setSelected(true);
            break;
        default:
            break;
        }
        menu.add(menuSize);
        menu.addSeparator();
        addMenuItem(menu, "Go to Current", KeyEvent.VK_G, "goto-current");
        return menu;
    }

    private JMenu createMenuWindows()
    {
        int shortcutKeyMask = 0;
        if (Platform.isMac())
            shortcutKeyMask = m_shortcutKeyMask;
        JMenu menu = createMenu("Window", KeyEvent.VK_W);
        addMenuItem(menu, "Board", KeyEvent.VK_B, KeyEvent.VK_F6,
                    shortcutKeyMask, "gogui");
        addMenuItem(menu, "Analyze", KeyEvent.VK_A, KeyEvent.VK_F8,
                    shortcutKeyMask, "analyze");
        addMenuItem(menu, "GTP Shell", KeyEvent.VK_G, KeyEvent.VK_F9,
                    shortcutKeyMask, "gtp-shell");
        menu.addSeparator();
        addMenuItem(menu, "Close", KeyEvent.VK_C, KeyEvent.VK_W,
                    m_shortcutKeyMask, "close");
        return menu;
    }

    private static void setPrefsDefaults(Preferences prefs)
    {
        prefs.setIntDefault("gametree-labels", GameTreePanel.LABEL_NUMBER);
        prefs.setIntDefault("gametree-size", GameTreePanel.SIZE_NORMAL);
    }
}

//----------------------------------------------------------------------------

