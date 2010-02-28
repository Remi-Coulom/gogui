// SquareLayout.java

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/** Layout manager for 1:1 aspect ratio.
    Expects that the container to layout has only a single child component. */
public class SquareLayout
    implements LayoutManager
{
    /** Unused.
        Does nothing, because this class will automatically layout the single
        child component of a container.
        @param name Unused
        @param comp Unused */
    public void addLayoutComponent(String name, Component comp)
    {
    }

    /** Layout container.
        Contains an assertion that the container has exactly one child.
        This child is layout in the center of the container with  the maximum
        square size that fits into the container.
        @param parent The container to layout */
    public void layoutContainer(Container parent)
    {
        assert parent.getComponentCount() == 1;
        Dimension size = parent.getSize();
        Insets insets = parent.getInsets();
        size.width -= insets.left + insets.right;
        size.height -= insets.top + insets.bottom;
        int len = size.width < size.height ? size.width : size.height;
        int x = (size.width - len) / 2;
        int y = (size.height - len) / 2;
        parent.getComponent(0).setBounds(x + insets.left, y + insets.top,
                                         len, len);
    }

    /** Return minimum layout size.
        Contains an assertion that the container has exactly one child.
        @param parent The container to layout.
        @return The minimum size of the child. */
    public Dimension minimumLayoutSize(Container parent)
    {
        assert parent.getComponentCount() == 1;
        return parent.getComponent(0).getMinimumSize();
    }

    /** Return preferred layout size.
        Contains an assertion that the container has exactly one child.
        @param parent The container to layout.
        @return The preferred size of the child. */
    public Dimension preferredLayoutSize(Container parent)
    {
        assert parent.getComponentCount() == 1;
        return parent.getComponent(0).getPreferredSize();
    }

    /** Unused.
        Does nothing, because this class will automatically layout the single
        child component of a container.
        @param comp Unused */
    public void removeLayoutComponent(Component comp)
    {
    }
}
