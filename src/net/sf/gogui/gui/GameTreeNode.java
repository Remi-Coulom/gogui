// GameTreeNode.java

package net.sf.gogui.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.font.LineMetrics;
import javax.swing.JComponent;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.BLACK_WHITE_EMPTY;
import net.sf.gogui.go.Move;
import static net.sf.gogui.gui.I18n.i18n;

class GameTreeNode
    extends JComponent
{
    public GameTreeNode(ConstNode node, int moveNumber,
                        GameTreePanel gameTreePanel,
                        MouseListener mouseListener, Font font,
                        Image imageBlack, Image imageWhite, Image imageSetup,
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
        m_imageBlack = imageBlack;
        m_imageWhite = imageWhite;
        m_imageSetup = imageSetup;
        updateToolTip();
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
            graphics.fillRect(0, 0, size, size > 10 ? size : fullSize - 1);
        }
        graphics.setColor(COLOR_GRID);
        if (numberChildren > 0)
            graphics.drawLine(size, halfSize, fullSize, halfSize);
        if (numberChildren > 1 && isExpanded)
            graphics.drawLine(halfSize, size, halfSize, fullSize);
        Move move = m_node.getMove();
        if (m_node.hasSetup())
            graphics.drawImage(m_imageSetup, 0, 0, null);
        else if (move == null)
        {
            graphics.setColor(COLOR_GRID);
            int[] xPoints = { halfSize, size, halfSize, 0 };
            int[] yPoints = { 0, halfSize, size, halfSize };
            graphics.fillPolygon(xPoints, yPoints, 4);
        }
        else
        {
            if (move.getColor() == BLACK)
                graphics.drawImage(m_imageBlack, 0, 0, null);
            else
                graphics.drawImage(m_imageWhite, 0, 0, null);
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

    public final void updateToolTip()
    {
        StringBuilder toolTip = new StringBuilder(128);
        Move move = m_node.getMove();
        GoColor player = m_node.getPlayer();
        if (move != null)
        {
            toolTip.append(m_moveNumber);
            toolTip.append(' ');
            toolTip.append(move);
        }
        else if (m_node.hasSetup() || player != null)
        {
            toolTip.append(i18n("TT_NODE_SETUP"));
            toolTip.append(" (");
            boolean anyStones = false;
            for (GoColor c : BLACK_WHITE_EMPTY)
            {
                int n = m_node.getSetup(c).size();
                if (n == 0)
                    continue;
                if (anyStones)
                    toolTip.append(", ");
                anyStones = true;
                toolTip.append(c.getUppercaseLetter());
                toolTip.append(' ');
                toolTip.append(n);
            }
            if (player != null)
            {
                if (anyStones)
                    toolTip.append(", ");
                toolTip.append(i18n("TT_NODE_PLAYER"));
                toolTip.append(' ');
                toolTip.append(player.getUppercaseLetter());
            }
            toolTip.append(')');
        }
        String comment = NodeUtil.getCommentStart(m_node, false, 80);
        if (comment != null)
        {
            comment = comment.replaceAll("\n *\n", "\n");
            comment = comment.replaceAll("\n", "<br>");
            if (comment.length() > 50)
            {
                toolTip.append("<p width=\"250\">");
                toolTip.append(comment);
                toolTip.append("</p>");
            }
            else
            {
                toolTip.append("<p>");
                toolTip.append(comment);
                toolTip.append("</p>");
            }
        }
        if (toolTip.length() > 0)
            setToolTipText("<html>" + toolTip.toString() + "</html>");
    }

    private final int m_moveNumber;

    private static final Color COLOR_LIGHT_BLUE = new Color(103, 122, 164);

    private static final Color COLOR_CURSOR = new Color(142, 168, 226);

    private static final Color COLOR_GRID = new Color(148, 148, 148);

    private final GameTreePanel m_gameTreePanel;

    private final ConstNode m_node;

    private final Image m_imageBlack;

    private final Image m_imageWhite;

    private final Image m_imageSetup;

    private void drawText(Graphics graphics)
    {
        GameTreePanel.Label labelMode = m_gameTreePanel.getLabelMode();
        if (labelMode == GameTreePanel.Label.NONE)
            return;
        Move move = m_node.getMove();
        int size = m_gameTreePanel.getNodeSize();
        String text;
        if (labelMode == GameTreePanel.Label.MOVE)
        {
            if (move.getPoint() == null)
                return;
            text = move.getPoint().toString();
        }
        else
            text = Integer.toString(m_moveNumber);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        LineMetrics lineMetrics = fontMetrics.getLineMetrics(text, graphics);
        int textWidth = fontMetrics.stringWidth(text);
        int ascent = (int)lineMetrics.getAscent();
        int xText = (size - textWidth) / 2;
        int yText = (ascent + size) / 2;
        if (move.getColor() == BLACK)
            graphics.setColor(Color.white);
        else
            graphics.setColor(Color.black);
        graphics.drawString(text, xText, yText);
    }
}
