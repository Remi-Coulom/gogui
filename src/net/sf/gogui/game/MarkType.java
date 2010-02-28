// MarkType.java

package net.sf.gogui.game;

/** Markup types for points in nodes of a game tree. */
public enum MarkType
{
    MARK,

    CIRCLE,

    SQUARE,

    TRIANGLE,

    /** Selected (SGF markup type SL).
        Not that this markup cannot be saved in (Jago's) XML (only using a
        legacy SGF tag), so it is needed to display markup read from SGF,
        but is not actively supported in GoGui. */
    SELECT,

    TERRITORY_BLACK,

    TERRITORY_WHITE;
}
