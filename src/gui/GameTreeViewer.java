//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import game.*;
import go.*;
import utils.GuiUtils;
import utils.Platform;

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
        graphics.setColor(m_colorBackground);
        graphics.fillRect(0, 0, width, width);
        Move move = m_node.getMove();
        int halfSize = width / 2;
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
            String text = Integer.toString(m_moveNumber);
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
        if (m_node.getComment() != null
            && ! m_node.getComment().trim().equals(""))
        {
            graphics.setColor(m_colorLightBlue);
            graphics.drawLine(3, width + 2, width - 3, width + 2);
            graphics.drawLine(3, width + 4, width - 3, width + 4);
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

    private static final java.awt.Color m_colorBackground
        = UIManager.getColor("Label.background");

    private static final java.awt.Color m_colorLightBlue
        = new java.awt.Color(0.57f, 0.68f, 0.91f);

    private GameTreePanel m_gameTreePanel;

    private Node m_node;
}

//----------------------------------------------------------------------------

class GameTreePanel
    extends JPanel
    implements Scrollable
{
    public GameTreePanel(GameTreeViewer.Listener listener, boolean fastPaint)
    {
        super(new SpringLayout());
        m_fastPaint = fastPaint;
        setBackground(UIManager.getColor("Label.background"));
        m_nodeWidth = 25;
        m_nodeDist = 35;
        Font font = UIManager.getFont("Label.font");
        if (font != null)
        {
            Font derivedFont = font.deriveFont(font.getSize() * 0.7f);
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
        setOpaque(false);
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
                    GameNode gameNode = (GameNode)event.getSource();
                    if (event.isPopupTrigger())
                    {
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

    public void gotoNode(Node node)
    {
        if (m_listener != null)
            m_listener.gotoNode(node);
    }

    public boolean isCurrent(Node node)
    {
        return node == m_currentNode;
    }

    public void paintComponent(Graphics graphics)
    {
        if (m_gameTree == null)
            return;
        graphics.setColor(java.awt.Color.DARK_GRAY);
        drawGrid(graphics, m_gameTree.getRoot(),
                 m_margin + m_nodeWidth / 2, m_margin + m_nodeWidth / 2);
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
            SimpleDialogs.showError(this,
                                    "Could not show game tree\n" + 
                                    "Out of memory");
            update(gameTree, currentNode);
        }
        GameNode gameNode = getGameNode(currentNode);
        gameNode.repaint();
        setPreferredSize(new Dimension(m_maxX + m_nodeDist + m_margin,
                                       m_maxY + m_nodeDist + m_margin));
        revalidate();
        scrollToCurrent();
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
    }

    private boolean m_fastPaint;

    private int m_currentNodeX;

    private int m_currentNodeY;

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

    private Node m_currentNode;

    private Node m_popupNode;

    private HashMap m_map = new HashMap(500, 0.8f);

    private HashSet m_expanded = new HashSet(200);

    private MouseListener m_mouseListener;

    private java.awt.Point m_popupLocation;

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
        int lastYChild = y;
        boolean notExpanded =
            (numberChildren > 1 && ! m_expanded.contains(node));
        int maxChildren = numberChildren;
        if (notExpanded)
            maxChildren = Math.min(numberChildren, 1);
        for (int i = 0; i < maxChildren; ++i)
        {
            graphics.drawLine(x, lastYChild, x, yChild);
            graphics.drawLine(x, yChild, xChild, yChild);
            lastYChild = yChild;
            yChild = drawGrid(graphics, node.getChild(i), xChild, yChild);
            if (! notExpanded && i < numberChildren - 1)
                yChild += m_nodeDist;
        }
        if (notExpanded)
        {
            int d1 = m_nodeWidth / 2;
            int d2 = d1 + 7;
            graphics.drawLine(x, y, x, y + d2);
            graphics.drawLine(x, y + d2, x + d1, y + d2);
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
            gotoNode(m_currentNode);
        if (changed)
            update(m_gameTree, m_currentNode);
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

    private void scrollToCurrent()
    {
        scrollRectToVisible(new Rectangle(m_currentNodeX - 2 * m_nodeWidth,
                                          m_currentNodeY,
                                          5 * m_nodeWidth, 3 * m_nodeWidth));
    }

    public void showPopup(int x, int y, GameNode gameNode)
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
        item = new JMenuItem("Hide Subtree");
        if (! m_expanded.contains(node))
            item.setEnabled(false);
        item.setActionCommand("hide-subtree");
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

    private void showSubtree(Node node)
    {
        if (NodeUtils.subtreeGreaterThan(node, 10000)
            && ! SimpleDialogs.showQuestion(this,
                                            "Really show large subtree?"))
            return;
        boolean changed = false;
        int depth = NodeUtils.getDepth(node);
        while (node != null)
        {
            if (node.getNumberChildren() > 1 && m_expanded.add(node))
                changed = true;
            node = NodeUtils.nextNode(node, depth);
        }
        if (changed)
            update(m_gameTree, m_currentNode);
    }

    private void showVariations(Node node)
    {
        if (node.getNumberChildren() > 1 && m_expanded.add(node))
            update(m_gameTree, m_currentNode);
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
        public void gotoNode(Node node);

        public void cbAnalyze();

        public void cbGtpShell();

        public void toTop();
    }

    public GameTreeViewer(Listener listener, boolean fastPaint)
    {
        super("Game Tree");
        GuiUtils.setGoIcon(this);
        createMenuBar();
        Container contentPane = getContentPane();
        m_listener = listener;
        m_panel = new GameTreePanel(listener, fastPaint);
        m_scrollPane =
            new JScrollPane(m_panel,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JViewport viewport = m_scrollPane.getViewport();
        viewport.setBackground(UIManager.getColor("Label.background"));
        contentPane.add(m_scrollPane, BorderLayout.CENTER);
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
        else if (command.equals("gtp-shell"))
            m_listener.cbGtpShell();
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

    private JScrollPane m_scrollPane;

    private Listener m_listener;

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

    private JMenu createMenu(String name, int mnemonic)
    {
        JMenu menu = new JMenu(name);
        menu.setMnemonic(mnemonic);
        return menu;
    }

    private void createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createMenuWindows());
        setJMenuBar(menuBar);
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
}

//----------------------------------------------------------------------------

