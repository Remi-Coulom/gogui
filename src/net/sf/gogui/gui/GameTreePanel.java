// GameTreePanel.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.ConstGameTree;
import net.sf.gogui.game.NodeUtil;
import static net.sf.gogui.gui.I18n.i18n;

/** Panel displaying a game tree. */
public class GameTreePanel
    extends JPanel
    implements Scrollable
{
    public enum Label
    {
        NUMBER,

        MOVE,

        NONE
    }

    public enum Size
    {
        LARGE,

        NORMAL,

        SMALL,

        TINY
    }

    public static final Color BACKGROUND = new Color(192, 192, 192);

    public GameTreePanel(JDialog owner, GameTreeViewer.Listener listener,
                         Label labelMode, Size sizeMode,
                         MessageDialogs messageDialogs)
    {
        super(new SpringLayout());
        m_messageDialogs = messageDialogs;
        m_owner = owner;
        setBackground(BACKGROUND);
        m_labelMode = labelMode;
        m_sizeMode = sizeMode;
        initSize(sizeMode);
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

    public ConstNode getCurrentNode()
    {
        return m_currentNode;
    }

    public Label getLabelMode()
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

    public Size getSizeMode()
    {
        return m_sizeMode;
    }

    public void gotoNode(ConstNode node)
    {
        if (m_listener != null)
            m_listener.actionGotoNode(node);
    }

    public boolean isCurrent(ConstNode node)
    {
        return node == m_currentNode;
    }

    public boolean isExpanded(ConstNode node)
    {
        return m_isExpanded.contains(node);
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
                                          m_currentNodeY - m_nodeSize,
                                          5 * m_nodeSize,
                                          3 * m_nodeSize));
    }

    public void setLabelMode(Label mode)
    {
        switch (mode)
        {
        case NUMBER:
        case MOVE:
        case NONE:
            m_labelMode = mode;
            break;
        default:
            assert false;
            break;
        }
    }

    /** Only used for a workaround on Mac Java 1.4.2,
        which causes the scrollpane to lose focus after a new layout of
        this panel. If scrollPane is not null, a requestFocusOnWindow will
        be called after each new layout */
    public void setScrollPane(JScrollPane scrollPane)
    {
        m_scrollPane = scrollPane;
    }

    public void setShowSubtreeSizes(boolean showSubtreeSizes)
    {
        m_showSubtreeSizes = showSubtreeSizes;
    }

    public void setSizeMode(Size mode)
    {
        switch (mode)
        {
        case LARGE:
        case NORMAL:
        case SMALL:
        case TINY:
            if (mode != m_sizeMode)
            {
                m_sizeMode = mode;
                initSize(m_sizeMode);
            }
            break;
        default:
            assert false;
            break;
        }
    }

    /** Faster than update if a new node was added as the first child. */
    public void addNewSingleChild(ConstNode node)
    {
        assert ! node.hasChildren();
        ConstNode father = node.getFatherConst();
        assert father != null;
        assert father.getNumberChildren() == 1;
        GameTreeNode fatherGameNode = getGameTreeNode(father);
        if (fatherGameNode == null)
        {
            assert false;
            return;
        }
        int moveNumber = NodeUtil.getMoveNumber(node);
        GameTreeNode gameNode = createNode(node, moveNumber);
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

    public void update(ConstGameTree tree, ConstNode currentNode,
                       int minWidth, int minHeight)
    {
        assert currentNode != null;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        m_minWidth = minWidth;
        m_minHeight = minHeight;
        boolean gameTreeChanged = (tree != m_tree);
        if (gameTreeChanged)
            m_isExpanded.clear();
        ensureVisible(currentNode);
        m_tree = tree;
        m_currentNode = currentNode;
        removeAll();
        m_map.clear();
        m_maxX = minWidth;
        m_maxY = minHeight;
        try
        {
            ConstNode root = m_tree.getRootConst();
            createNodes(this, root, 0, 0, MARGIN, MARGIN, 0);
            if (gameTreeChanged
                && ! NodeUtil.subtreeGreaterThan(root, 10000))
                showSubtree(root);
        }
        catch (OutOfMemoryError e)
        {
            m_isExpanded.clear();
            removeAll();
            m_messageDialogs.showError(m_owner,
                                       i18n("MSG_TREE_OUTOFMEM"),
                                       i18n("MSG_TREE_OUTOFMEM_2"));
            update(tree, currentNode, minWidth, minHeight);
        }
        setPreferredSize(new Dimension(m_maxX + m_nodeFullSize + MARGIN,
                                       m_maxY + m_nodeFullSize + MARGIN));
        revalidate();
        scrollToCurrent();
        if (m_scrollPane != null)
            m_scrollPane.requestFocusInWindow();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void update(ConstNode currentNode, int minWidth, int minHeight)
    {
        assert currentNode != null;
        if (ensureVisible(currentNode))
        {
            update(m_tree, currentNode, minWidth, minHeight);
            return;
        }
        GameTreeNode gameNode = getGameTreeNode(m_currentNode);
        if (gameNode == null)
        {
            // The following warning was previously an assert false.
            // But it can can happen, because GoGui does sometimes defer a full
            // update of the tree with SwingUtilities::invokeLater to be
            // able to show a busy cursor and it can happen that a lightweight
            // update (which assumes that the tree structure has not changed)
            // is called before the full update event is dispatched.
            // In the future, it would be easier to do all full updates in
            // the event dispatch tree, even if they take long, but then we
            // need another method to show the busy cursor while the UI is
            // non-responsive
            System.err.println("GameTreePanel: current node not found");
            return;
        }
        gameNode.repaint();
        gameNode = getGameTreeNode(currentNode);
        if (gameNode == null)
        {
            update(m_tree, currentNode, minWidth, minHeight);
            return;
        }
        Point location = gameNode.getLocation();
        m_currentNodeX = location.x;
        m_currentNodeY = location.y;
        gameNode.repaint();
        gameNode.updateToolTip();
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

    private Label m_labelMode;

    private int m_minHeight;

    private int m_minWidth;

    private Size m_sizeMode;

    private int m_nodeSize;

    private int m_nodeFullSize;

    private static final int MARGIN = 15;

    private int m_maxX;

    private int m_maxY;

    private Dimension m_preferredNodeSize;

    private Font m_font;

    private ConstGameTree m_tree;

    private final GameTreeViewer.Listener m_listener;

    private final JDialog m_owner;

    /** Used for focus workaround on Mac Java 1.4.2 if not null. */
    private JScrollPane m_scrollPane;

    private ConstNode m_currentNode;

    private ConstNode m_popupNode;

    private final HashMap<ConstNode,GameTreeNode> m_map =
        new HashMap<ConstNode,GameTreeNode>(500, 0.8f);

    private final HashSet<ConstNode> m_isExpanded
        = new HashSet<ConstNode>(200);

    private final MouseListener m_mouseListener;

    private Point m_popupLocation;

    private ImageIcon m_iconBlack;

    private ImageIcon m_iconWhite;

    private ImageIcon m_iconSetup;

    private final MessageDialogs m_messageDialogs;

    private JPopupMenu m_popup;

    private JMenuItem m_itemGoto;

    private JMenuItem m_itemScrollToCurrent;

    private JMenuItem m_itemHideSubtree;

    private JMenuItem m_itemShowSubtree;

    private JMenuItem m_itemShowChildren;

    private void initSize(Size sizeMode)
    {
        switch (sizeMode)
        {
        case LARGE:
            m_nodeSize = 32;
            m_nodeFullSize = 40;
            m_iconBlack = GuiUtil.getIcon("gogui-black-32x32", "");
            m_iconWhite = GuiUtil.getIcon("gogui-white-32x32", "");
            m_iconSetup = GuiUtil.getIcon("gogui-setup-32x32", "");
            break;
        case SMALL:
            m_nodeSize = 16;
            m_nodeFullSize = 20;
            m_iconBlack = GuiUtil.getIcon("gogui-black-16x16", "");
            m_iconWhite = GuiUtil.getIcon("gogui-white-16x16", "");
            m_iconSetup = GuiUtil.getIcon("gogui-setup-16x16", "");
            break;
        case TINY:
            m_nodeSize = 8;
            m_nodeFullSize = 10;
            m_iconBlack = GuiUtil.getIcon("gogui-black-8x8", "");
            m_iconWhite = GuiUtil.getIcon("gogui-white-8x8", "");
            m_iconSetup = GuiUtil.getIcon("gogui-setup-8x8", "");
            break;
        case NORMAL:
            m_nodeSize = 24;
            m_nodeFullSize = 30;
            m_iconBlack = GuiUtil.getIcon("gogui-black-24x24", "");
            m_iconWhite = GuiUtil.getIcon("gogui-white-24x24", "");
            m_iconSetup = GuiUtil.getIcon("gogui-setup-24x24", "");
        }


        m_font = new Font("Dialog", Font.PLAIN, (int)(0.4 * m_nodeSize));
        m_preferredNodeSize = new Dimension(m_nodeFullSize, m_nodeFullSize);
    }

    private GameTreeNode createNode(ConstNode node, int moveNumber)
    {
        return new GameTreeNode(node, moveNumber, this, m_mouseListener,
                                m_font, m_iconBlack.getImage(),
                                m_iconWhite.getImage(),
                                m_iconSetup.getImage(),
                                m_preferredNodeSize);
    }

    private int createNodes(Component father, ConstNode node, int x, int y,
                            int dx, int dy, int moveNumber)
    {
        m_maxX = Math.max(x, m_maxX);
        m_maxY = Math.max(y, m_maxY);
        if (node.getMove() != null)
            ++moveNumber;
        GameTreeNode gameNode = createNode(node, moveNumber);
        m_map.put(node, gameNode);
        add(gameNode);
        putConstraint(father, gameNode, dx, dy);
        int numberChildren = node.getNumberChildren();
        dx = m_nodeFullSize;
        dy = 0;
        boolean isExpanded = isExpanded(node);
        if (isExpanded)
        {
            int[] childrenDy = new int[numberChildren];
            for (int i = 0; i < numberChildren; ++i)
            {
                childrenDy[i] = dy;
                dy += createNodes(gameNode, node.getChildConst(i),
                                  x + dx, y + dy, dx, dy, moveNumber);
                if (i < numberChildren - 1)
                    dy += m_nodeFullSize;
            }
            if (numberChildren > 1)
            {
                GameTreeJunction junction =
                    new GameTreeJunction(childrenDy, this);
                add(junction);
                putConstraint(gameNode, junction, 0, m_nodeFullSize);
            }
        }
        else
        {
            if (m_showSubtreeSizes && node.hasChildren())
            {
                int subtreeSize = NodeUtil.subtreeSize(node) - 1;
                String text = Integer.toString(subtreeSize);
                // Use upper limit for textWidth
                int textWidth = text.length() + m_font.getSize();
                int textHeight = m_font.getSize();
                int pad = GuiUtil.SMALL_PAD;
                m_maxX = Math.max(x + textWidth + pad, m_maxX);
                JLabel label = new JLabel(text);
                label.setFont(m_font);
                add(label);
                putConstraint(gameNode, label, dx + pad,
                              (m_nodeSize - textHeight) / 2);
            }
        }
        if (node == m_currentNode)
        {
            m_currentNodeX = x;
            m_currentNodeY = y;
        }
        return dy;
    }

    private void createPopup()
    {
        m_popup = new JPopupMenu();
        ActionListener listener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    String command = event.getActionCommand();
                    if (command.equals("goto"))
                        gotoNode(m_popupNode);
                    else if (command.equals("show-variations"))
                        showChildren(m_popupNode);
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
                    else if (command.equals("cancel"))
                        m_popup.setVisible(false);
                    else
                        assert false;
                }
            };
        JMenuItem item;
        item = new JMenuItem(i18n("MN_TREE_GOTO"));
        item.setActionCommand("goto");
        item.addActionListener(listener);
        m_popup.add(item);
        m_itemGoto = item;
        item = new JMenuItem(i18n("MN_TREE_SCROLL_TO_CURRENT"));
        item.setActionCommand("scroll-to-current");
        item.addActionListener(listener);
        m_popup.add(item);
        m_itemScrollToCurrent = item;
        m_popup.addSeparator();
        item = new JMenuItem(i18n("MN_TREE_HIDE_SUBTREE"));
        m_itemHideSubtree = item;
        item.setActionCommand("hide-subtree");
        item.addActionListener(listener);
        m_popup.add(item);
        item = new JMenuItem(i18n("MN_TREE_HIDE_OTHERS"));
        item.setActionCommand("hide-others");
        item.addActionListener(listener);
        m_popup.add(item);
        item = new JMenuItem(i18n("MN_TREE_SHOW_CHILDREN"));
        m_itemShowChildren = item;
        item.setActionCommand("show-variations");
        item.addActionListener(listener);
        m_popup.add(item);
        item = new JMenuItem(i18n("MN_TREE_SHOW_SUBTREE"));
        m_itemShowSubtree = item;
        item.setActionCommand("show-subtree");
        item.addActionListener(listener);
        m_popup.add(item);
        m_popup.addSeparator();
        item = new JMenuItem(i18n("MN_TREE_NODE_INFO"));
        item.setActionCommand("node-info");
        item.addActionListener(listener);
        m_popup.add(item);
        item = new JMenuItem(i18n("MN_TREE_SUBTREE_STATISTICS"));
        item.setActionCommand("tree-info");
        item.addActionListener(listener);
        m_popup.add(item);
        m_popup.addSeparator();
        item = new JMenuItem(i18n("LB_CANCEL"));
        item.setActionCommand("cancel");
        item.addActionListener(listener);
        m_popup.add(item);
    }

    private GameTreeNode getGameTreeNode(ConstNode node)
    {
        return m_map.get(node);
    }

    private boolean ensureVisible(ConstNode node)
    {
        boolean changed = false;
        while (node != null)
        {
            ConstNode father = node.getFatherConst();
            if (father != null)
                if (m_isExpanded.add(father))
                    changed = true;
            node = father;
        }
        return changed;
    }

    private void hideOthers(ConstNode node)
    {
        m_isExpanded.clear();
        ensureVisible(node);
        update(m_tree, m_currentNode, getWidth(), getHeight());
    }

    private void hideSubtree(ConstNode root)
    {
        boolean changed = false;
        boolean currentChanged = false;
        int depth = NodeUtil.getDepth(root);
        ConstNode node = root;
        while (node != null)
        {
            if (node == m_currentNode)
            {
                m_currentNode = root;
                currentChanged = true;
                changed = true;
            }
            if (m_isExpanded.remove(node))
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
            update(m_tree, m_currentNode, getWidth(), getHeight());
            scrollTo(root);
        }
    }

    private void nodeInfo(Point location, ConstNode node)
    {
        String nodeInfo = NodeUtil.nodeInfo(node);
        String title = i18n("TIT_NODE_INFO");
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

    private void scrollTo(ConstNode node)
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
        ConstNode node = gameNode.getNode();
        m_popupNode = node;
        if (m_popup == null)
            createPopup();
        m_itemGoto.setEnabled(node != m_currentNode);
        m_itemScrollToCurrent.setEnabled(node != m_currentNode);
        boolean hasChildren = node.hasChildren();
        m_itemHideSubtree.setEnabled(hasChildren);
        m_itemShowSubtree.setEnabled(hasChildren);
        m_itemShowChildren.setEnabled(hasChildren);
        m_popup.show(gameNode, x, y);
        m_popupLocation = m_popup.getLocationOnScreen();
    }

    private void showSubtree(ConstNode root)
    {
        if (NodeUtil.subtreeGreaterThan(root, 10000))
        {
            String mainMessage = i18n("MSG_TREE_EXPAND_LARGE");
            String optionalMessage = i18n("MSG_TREE_EXPAND_LARGE_2");
            if (! m_messageDialogs.showWarningQuestion(m_owner, mainMessage,
                                                       optionalMessage,
                                                       i18n("LB_TREE_EXPAND"),
                                                       true))
                return;
        }
        boolean changed = false;
        ConstNode node = root;
        int depth = NodeUtil.getDepth(node);
        while (node != null)
        {
            if (m_isExpanded.add(node))
                changed = true;
            node = NodeUtil.nextNode(node, depth);
        }
        if (changed)
        {
            update(m_tree, m_currentNode, m_minWidth, m_minHeight);
            // Game node could have disappeared, because after out of memory
            // error all nodes are hidden but main variation
            if (getGameTreeNode(root) == null)
            {
                ensureVisible(root);
                update(m_tree, m_currentNode, m_minWidth, m_minHeight);
            }
            scrollTo(root);
        }
    }

    private void showChildren(ConstNode node)
    {
        if (m_isExpanded.add(node))
        {
            update(m_tree, m_currentNode, m_minWidth, m_minHeight);
            scrollTo(node);
        }
    }

    private void treeInfo(Point location, ConstNode node)
    {
        String treeInfo = NodeUtil.treeInfo(node);
        String title = i18n("TIT_SUBTREE_INFO");
        TextViewer textViewer = new TextViewer(m_owner, title, treeInfo, true,
                                               null);
        textViewer.setLocation(location);
        textViewer.setVisible(true);
    }
}
