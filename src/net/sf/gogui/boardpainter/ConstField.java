// ConstField.java

package net.sf.gogui.boardpainter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import net.sf.gogui.go.GoColor;

/** Const functions of Field.
    @see Field */
public interface ConstField
{
    void draw(Graphics graphics, int size, int x, int y, Image boardImage,
              int boardWidth);

    GoColor getColor();

    boolean getCursor();

    boolean getCrossHair();

    Color getFieldBackground();

    boolean getMark();

    boolean getMarkCircle();

    boolean getMarkSquare();

    boolean getMarkTriangle();

    boolean getSelect();

    GoColor getGhostStone();

    String getLabel();

    GoColor getTerritory();

    boolean isInfluenceSet();
}
