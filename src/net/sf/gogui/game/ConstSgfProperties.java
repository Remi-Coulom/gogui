// SgfProperties.java

package net.sf.gogui.game;

import java.util.ArrayList;

/** Const functions of game.SgfProperties.
    @see SgfProperties */
public interface ConstSgfProperties
{
    int getNumberValues(String key);

    ArrayList<String> getKeys();

    String getValue(String key, int index);

    boolean hasKey(String key);

    boolean isEmpty();
}
