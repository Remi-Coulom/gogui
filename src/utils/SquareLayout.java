//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package utils;

import java.awt.*;

//-----------------------------------------------------------------------------

/** Custom layout manager for 1:1 aspect ratio.
*/
public class SquareLayout
    implements LayoutManager
{
    public void addLayoutComponent(String name, Component comp)
    {
    }
    
    public void layoutContainer(Container parent)
    {
        assert(parent.getComponentCount() == 1);
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
    
    public Dimension minimumLayoutSize(Container parent)
    {
        assert(parent.getComponentCount() == 1);
        Component c = parent.getComponent(0);
        return c.getMinimumSize();
    }
    
    public Dimension preferredLayoutSize(Container parent)
    {
        assert(parent.getComponentCount() == 1);
        Component c = parent.getComponent(0);
        return c.getPreferredSize();
    }
    
    public void removeLayoutComponent(Component comp)
    {
    }
}

//-----------------------------------------------------------------------------
