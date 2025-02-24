package net.sf.gogui.gtp;

import java.util.Objects;

/** Parameters (width, height, geometry) of the board, given by gogui-rules_board_size command */
public final class BoardParameters {

    public BoardParameters(int mSize) {
        m_width = mSize;
        m_height = mSize;
        m_geometry = m_geometryOptions[0];
    }

    public BoardParameters(int mWidth, int mHeight, String mGeometry) {
        m_width = mWidth;
        m_height = mHeight;
        m_geometry = getValidGeometry(mGeometry);
    }

    public int width() { return m_width; }
    public int height() { return m_height; }
    /** Needed for compatibility with square boards */
    public int size() { return m_width; }
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
        return m_width == that.m_width && m_height == that.m_height && Objects.equals(m_geometry, that.m_geometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_width, m_height, m_geometry);
    }

    /** The width of the board (given by gogui-rules_board_size command) */
    private final int m_width;

    /** The height of the board (Default as m_width if not given by gogui-rules_board_size command) */
    private final int m_height;

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
