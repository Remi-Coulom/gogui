package net.sf.gogui.gtp;

import java.awt.*;
import java.util.Objects;

/** Parameters (width, height, geometry) of the board, given by gogui-rules_board_size command */
public final class BoardParameters {

    public BoardParameters(int mSize) {
        m_dimension = new Dimension(mSize, mSize);
        m_geometry = m_geometryOptions[0];
    }

    public BoardParameters(int mWidth, int mHeight, String mGeometry) {
        m_dimension = new Dimension(mWidth, mHeight);
        m_geometry = getValidGeometry(mGeometry);
    }

    public Dimension getDimension() { return m_dimension; }
    /** Needed for compatibility with square boards
     * Will eventually be removed */
    public int size() { return m_dimension.width; }
    public String geometry() { return m_geometry; }

    /** Factory method for creating a BoardParameters.
     @param input String with the board parameters given by the gogui-rules_board_size command
     @return Unique reference to a BoardParameters with parsed values */
    public static BoardParameters get(String input)
    {
        String[] splitResponse = input.trim().split("\\s+");
        if (splitResponse.length < 1)
            return new BoardParameters(-1, -1, "rect");

        int width = Integer.parseInt(splitResponse[0]);
        if (splitResponse.length < 2)
            return new BoardParameters(width, width, "rect");

        try {
            if (splitResponse.length < 3)
                return new BoardParameters(width, Integer.parseInt(splitResponse[1]), "rect");
            return new BoardParameters(width, Integer.parseInt(splitResponse[1]), BoardParameters.getValidGeometry(splitResponse[2]));
        } catch (NumberFormatException e) {
            return new BoardParameters(width, width, BoardParameters.getValidGeometry(splitResponse[1]));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BoardParameters that = (BoardParameters) o;
        return m_dimension.width == that.m_dimension.width &&
                m_dimension.height == that.m_dimension.height &&
                Objects.equals(m_geometry, that.m_geometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_dimension.width, m_dimension.height, m_geometry);
    }

    /** The dimensions of the board
     *  The width of the board is given by the gogui-rules_board_size command
     *  The height of the board defaults as the width if not given by gogui-rules_board_size command
     * */
    private final Dimension m_dimension;

    /** The geometry of the board (rect by default or hex for hexagonal games)*/
    private final String m_geometry;

    /** The possible geometry options for the board */
    private static final String[]  m_geometryOptions = {"rect", "hex"};

    public static String getValidGeometry(String geometry) {
        for (String option : m_geometryOptions) {
            if (option.equals(geometry)) {
                return geometry;
            }
        }
        return m_geometryOptions[0];
    }
}
