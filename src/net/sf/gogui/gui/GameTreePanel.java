//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import net.sf.gogui.game.GameTree;
import net.sf.gogui.game.Node;
import net.sf.gogui.game.NodeUtil;

/** Panel displaying a game tree. */
public class GameTreePanel
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

    public static final Color BACKGROUND = new Color(192, 192, 192);

    public GameTreePanel(JDialog owner, GameTreeViewer.Listener listener,
                         int labelMode, int sizeMode)
    {
        super(new SpringLayout());
        m_owner = owner;
        setBackground(BACKGROUND);
        m_labelMode = labelMode;
        m_sizeMode = sizeMode;
        computeSizes(sizeMode);
        setFocusable(false);
        setFocusTraversalKeysEnabled(false);
        setAutoscrolls(true);
        addMouseMotionListener(new GameTreePanel.MouseMotionListener());
        m_listener = listener;
        m_mouseListener = new MouseAdapter()
            {
                public void mouseClicked(MouseEvent event)
                {
                    if (event.getButton() != MouseEvent.BUTTON1)
                        return;
                    GameTreeNode gameNode = (GameTreeNode)event.getSource();
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
                        GameTreeNode gameNode
                            = (GameTreeNode)event.getSource();
                        int x = event.getX();
                        int y = event.getY();
                        showPopup(x, y, gameNode);
                    }
                }

                public void mouseReleased(MouseEvent event)
                {
                    if (event.isPopupTrigger())
                    {
                        GameTreeNode gameNode
                            = (GameTreeNode)event.getSource();
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

    public int getLabelMode()
    {
        return m_labelMode;
    }
    
    public int getNodeFullSize()
    {
        return m_nodeFullSize;
    }
    
    public int getNodeSize()
    {
        return m_nodeSize;
    }
    
    public Dimension getPreferredScrollableViewportSize()
    {
        return new Dimension(m_nodeFullSize * 10, m_nodeFullSize * 3);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction)
    {
        int result;
        if (orientation == SwingConstants.VERTICAL)
            result = visibleRect.height;
        else
            result = visibleRect.width;
        result = (result / m_nodeFullSize) * m_nodeFullSize;
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
        return m_nodeFullSize;
    }

    public boolean getShowSubtreeSizes()
    {
        return m_showSubtreeSizes;
    }

    public int getSizeMode()
    {
        return m_sizeMode;
    }
    
    public void gotoNode(Node node)
    {
        if (m_listener != null)
            m_listener.cbGotoNode(node);
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
        GuiUtil.setAntiAlias(graphics);
        super.paintComponent(graphics);
    }

    public void redrawCurrentNode()
    {
        GameTreeNode gameNode = getGameTreeNode(m_currentNode);
        gameNode.repaint();
    }

    public void scrollToCurrent()
    {
        scrollRectToVisible(new Rectangle(m_currentNodeX - 2 * m_nodeSize,
                                          m_currentNodeY, 5 * m_nodeSize,
                                          3 * m_nodeSize));
    }

    public void setLabelMode(int mode)
    {
        switch (mode)
        {
        case LABEL_NUMBER:
        case LABEL_MOVE:
        case LABEL_NONE:
            m_labelMode = mode;
            break;
        default:
            assert(false);
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

    public void setShowSubtreeSizes(boolean showSubtreeSizes)
    {
        m_showSubtreeSizes = showSubtreeSizes;
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
            }
            break;
        default:
            assert(false);
            break;
        }
    }

    /** Faster than update if a new node was added as the first child. */
    public void addNewSingleChild(Node node)
    {
        assert(node.getNumberChildren() == 0);
        Node father = node.getFather();
        assert(father != null);
        assert(father.getNumberChildren() == 1);
        GameTreeNode fatherGameNode = getGameTreeNode(father);
        assert(! isExpanded(father));
        int moveNumber = NodeUtil.getMoveNumber(node);
        GameTreeNode gameNode =
            new GameTreeNode(node, moveNumber, this, m_mouseListener, m_font,
                             m_preferredNodeSize);
        m_map.put(node, gameNode);
        add(gameNode);
        putConstraint(fatherGameNode, gameNode, m_nodeFullSize, 0);
        gameNode.setLocation(fatherGameNode.getX() + m_nodeFullSize,
                             fatherGameNode.getY());
        gameNode.setSize(m_nodeFullSize, m_nodeFullSize);
        m_maxX = Math.max(fatherGameNode.getX() + 2 * m_nodeFullSize, m_maxX);
        setPreferredSize(new Dimension(m_maxX + m_nodeFullSize + MARGIN,
                                       m_maxY + m_nodeFullSize + MARGIN));
    }

    public void showPopup()
    {
        if (m_currentNode == null)
            return;
        scrollToCurrent();
        GameTreeNode gameNode = getGameTreeNode(m_currentNode);        
        if (gameNode == null)
            return;
        showPopup(gameNode.getWidth() / 2, gameNode.getHeight() / 2,
                  gameNode);
    }

    public void update(GameTree gameTree, Node currentNode, int minWidth,
                       int minHeight)
    {
        assert(currentNode != null);
        m_minWidth = minWidth;
        m_minHeight = minHeight;
        boolean gameTreeChanged = (gameTree != m_gameTree);
        if (gameTreeChanged)
            m_expanded.clear();
        ensureVisible(currentNode);
        m_gameTree = gameTree;
        m_currentNode = currentNode;
        removeAll();
        m_map.clear();
        m_maxX = minWidth;
        m_maxY = minHeight;
        try
        {
            Node root = m_gameTree.getRoot();
            createNodes(this, root, 0, 0, MARGIN, MARGIN, 0);
            if (gameTreeChanged)
            {
                if (NodeUtil.subtreeGreaterThan(root, 10000))
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
            update(gameTree, currentNode, minWidth, minHeight);
        }
        setPreferredSize(new Dimension(m_maxX + m_nodeFullSize + MARGIN,
                                       m_maxY + m_nodeFullSize + MARGIN));
        revalidate();
        scrollToCurrent();
        if (m_scrollPane != null)
            m_scrollPane.requestFocusInWindow();
    }

    public void update(Node currentNode, int minWidth, int minHeight)
    {
        assert(currentNode != null);
        if (ensureVisible(currentNode))
        {
            update(m_gameTree, currentNode, minWidth, minHeight);
            return;
        }
        GameTreeNode gameNode = getGameTreeNode(m_currentNode);
        gameNode.repaint();
        gameNode = getGameTreeNode(currentNode);
        Point location = gameNode.getLocation();
        m_currentNodeX = location.x;
        m_currentNodeY = location.y;
        gameNode.repaint();
        m_currentNode = currentNode;
        scrollToCurrent();
        if (m_scrollPane != null)
            m_scrollPane.requestFocusInWindow();
    }

    private static class MouseMotionListener
        extends MouseMotionAdapter
    {
        public void mouseDragged(MouseEvent event)
        {
            int x = event.getX();
            int y = event.getY();
            JPanel panel = (JPanel)event.getSource();
            Rectangle rectangle = new Rectangle(x, y, 1, 1);
            panel.scrollRectToVisible(rectangle);
        }
    }

    private boolean m_showSubtreeSizes;

    private int m_currentNodeX;

    private int m_currentNodeY;

    private int m_labelMode;

    private int m_minHeight;

    private int m_minWidth;

    private int m_sizeMode;

    private int m_nodeSize;

    private int m_nodeFullSize;

    private static final int MARGIN = 15;

    private int m_maxX;

    private int m_maxY;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private Dimension m_preferredNodeSize;

    private Font m_font;

    private GameTree m_gameTree;

    private final GameTreeViewer.Listener m_listener;

    private final JDialog m_owner;

    /** Used for focus workaround on Mac Java 1.4.2 if not null. */
    private JScrollPane m_scrollPane;

    private Node m_currentNode;

    private Node m_popupNode;

    private final HashMap m_map = new HashMap(500, 0.8f);

    private final HashSet m_expanded = new HashSet(200);

    private final MouseListener m_mouseListener;

    private Point m_popupLocation;

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
        m_nodeSize = 25;
        m_nodeFullSize = 35;
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
            m_nodeSize = font.getSize() * 2;
            if (m_nodeSize % 2 == 0)
                ++m_nodeSize;
            m_nodeFullSize = font.getSize() * 3;
            if (m_nodeFullSize % 2 == 0)
                ++m_nodeFullSize;
        }
        m_font = font;
        m_preferredNodeSize = new Dimension(m_nodeFullSize, m_nodeFullSize);
    }

    private int createNodes(Component father, Node node, int x, int y,
                            int dx, int dy, int moveNumber)
    {
        m_maxX = Math.max(x, m_maxX);
        m_maxY = Math.max(y, m_maxY);
        if (node.getMove() != null)
            ++moveNumber;
        GameTreeNode gameNode =
            new GameTreeNode(node, moveNumber, this, m_mouseListener, m_font,
                             m_preferredNodeSize);
        m_map.put(node, gameNode);
        add(gameNode);
        putConstraint(father, gameNode, dx, dy);
        int numberChildren = node.getNumberChildren();
        dx = m_nodeFullSize;
        dy = 0;
        int maxChildren = numberChildren;
        boolean notExpanded =
            (numberChildren > 1 && ! m_expanded.contains(node));
        if (notExpanded)
        {
            if (! m_showSubtreeSizes)
                maxChildren = Math.min(numberChildren, 1);
            else
            {
                maxChildren = 0;
                String text = Integer.toString(NodeUtil.subtreeSize(node));
                int estimatedWidth = text.length() * m_nodeFullSize / 3;
                m_maxX = Math.max(x + estimatedWidth, m_maxX);
                JLabel label = new JLabel(text);
                label.setFont(m_font);
                add(label);
                putConstraint(gameNode, label, dx, m_nodeFullSize / 2);
            }
        }
        if (maxChildren > 0)
        {
            int[] childrenDy = new int[maxChildren];
            for (int i = 0; i < maxChildren; ++i)
            {
                childrenDy[i] = dy;
                dy += createNodes(gameNode, node.getChild(i),
                                  x + dx, y + dy, dx, dy, moveNumber);
                if (! notExpanded && i < numberChildren - 1)
                    dy += m_nodeFullSize;
            }
            if (maxChildren > 1)
            {
                GameTreeJunction junction =
                    new GameTreeJunction(childrenDy, this);
                add(junction);
                putConstraint(gameNode, junction, 0, m_nodeFullSize);
            }
        }
        if (node == m_currentNode)
        {
            m_currentNodeX = x;
            m_currentNodeY = y;
        }
        return dy;
    }

    private GameTreeNode getGameTreeNode(Node node)
    {
        return (GameTreeNode)m_map.get(node);
    }

    private boolean ensureVisible(Node node)
    {
        boolean changed = false;
        boolean showSubtreeSizes = getShowSubtreeSizes();
        while (node != null)
        {
            Node father = node.getFather();
            if (father != null
                && (father.getChild() != node
                    || (showSubtreeSizes && father.getNumberChildren() > 1)))
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
        update(m_gameTree, m_currentNode, getWidth(), getHeight());
    }

    private void hideSubtree(Node root)
    {
        boolean changed = false;
        boolean currentChanged = false;
        int depth = NodeUtil.getDepth(root);
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
            node = NodeUtil.nextNode(node, depth);
        }
        if (currentChanged)
        {
            gotoNode(m_currentNode);
            root = m_currentNode;
        }
        if (changed)
        {
            update(m_gameTree, m_currentNode, m_minWidth, m_minHeight);
            scrollTo(root);
        }
    }

    private void nodeInfo(Point location, Node node)
    {
        String nodeInfo = NodeUtil.nodeInfo(node);
        String title = "Node Info";
        TextViewer textViewer = new TextViewer(m_owner, title, nodeInfo, true,
                                               null);
        textViewer.setLocation(location);
        textViewer.setVisible(true);
    }

    private void putConstraint(Component father, Component son,
                               int west, int north)
    {
        SpringLayout layout = (SpringLayout)getLayout();
        layout.putConstraint(SpringLayout.WEST, son, west,
                             SpringLayout.WEST, father);
        layout.putConstraint(SpringLayout.NORTH, son, north,
                             SpringLayout.NORTH, father);
    }

    private void scrollTo(Node node)
    {
        if (node == null)
            return;
        GameTreeNode gameNode = getGameTreeNode(node);
        Rectangle rectangle = new Rectangle();
        rectangle.x = gameNode.getLocation().x;
        rectangle.y = gameNode.getLocation().y;
        // Make rectangle large so that children are visible
        rectangle.width = 3 * m_nodeFullSize;
        rectangle.height = 3 * m_nodeFullSize;
        scrollRectToVisible(rectangle);
    }

    private void showPopup(int x, int y, GameTreeNode gameNode)
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
                    else if (command.equals("scroll-to-current"))
                        scrollTo(m_currentNode);
                    else if (command.equals("tree-info"))
                        treeInfo(m_popupLocation, m_popupNode);
                    else
                        assert(false);
                }
            };
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item;
        if (node != m_currentNode)
        {
            item = new JMenuItem("Go To");
            item.setActionCommand("goto");
            item.addActionListener(listener);
            popup.add(item);
            item = new JMenuItem("Scroll to Current");
            item.setActionCommand("scroll-to-current");
            item.addActionListener(listener);
            popup.add(item);
            popup.addSeparator();
        }
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
        // For com.jgoodies.looks
        popup.putClientProperty("jgoodies.noIcons", Boolean.TRUE);
        popup.show(gameNode, x, y);
        m_popupLocation = popup.getLocationOnScreen();
    }

    private void showSubtree(Node root)
    {
        if (NodeUtil.subtreeGreaterThan(root, 10000)
            && ! SimpleDialogs.showQuestion(m_owner,
                                            "Really expand large subtree?"))
            return;
        boolean changed = false;
        Node node = root;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            if (node.getNumberChildren() > 1 && m_expanded.add(node))
                changed = true;
            node = NodeUtil.nextNode(node, depth);
        }
        if (changed)
        {
            update(m_gameTree, m_currentNode, m_minWidth, m_minHeight);
            // Game node could have disappeared, because after out of memory
            // error all nodes are hidden but main variation
            if (getGameTreeNode(root) == null)
            {
                ensureVisible(root);
                update(m_gameTree, m_currentNode, m_minWidth, m_minHeight);
            }
            scrollTo(root);
        }
    }

    private void showVariations(Node node)
    {
        if (node.getNumberChildren() > 1 && m_expanded.add(node))
        {
            update(m_gameTree, m_currentNode, m_minWidth, m_minHeight);
            scrollTo(node);
        }
    }

    private void treeInfo(Point location, Node node)
    {
        String treeInfo = NodeUtil.treeInfo(node);
        String title = "Subtree Info";
        TextViewer textViewer = new TextViewer(m_owner, title, treeInfo, true,
                                               null);
        textViewer.setLocation(location);
        textViewer.setVisible(true);
    }
}

