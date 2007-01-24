//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.gui.GuiUtil;
import net.sf.gogui.util.Platform;

/** Actions used in the GoGui tool bar and menu bar.
    This class has a cyclic dependency with class GoGui, however the
    dependency has a simple structure. The class contains actions that wrap a
    call to public functions of GoGui and associate a name, icon, description
    and accelerator key. There are also update functions that are used to
    enable actions or add additional information to their descriptions
    depending on the state of GoGui as far as it is accessible through public
    functions.
*/
public class GoGuiActions
{
    public final AbstractAction m_actionAbout;

    public final AbstractAction m_actionAttachProgram;

    public final AbstractAction m_actionBackward;

    public final AbstractAction m_actionBackwardTen;

    public final AbstractAction m_actionBeginning;

    public final AbstractAction m_actionDetachProgram;

    public final AbstractAction m_actionDocumentation;

    public final AbstractAction m_actionEnd;

    public final AbstractAction m_actionForward;

    public final AbstractAction m_actionForwardTen;

    public final AbstractAction m_actionInterrupt;

    public final AbstractAction m_actionNextVariation;

    public final AbstractAction m_actionNewGame;

    public final AbstractAction m_actionOpen;

    public final AbstractAction m_actionPass;

    public final AbstractAction m_actionPlay;

    public final AbstractAction m_actionPreviousVariation;

    public final AbstractAction m_actionPrint;

    public final AbstractAction m_actionSave;

    public final AbstractAction m_actionSaveAs;

    public final AbstractAction m_actionQuit;

    public GoGuiActions(GoGui goGui)
    {
        m_goGui = goGui;

        m_actionAbout = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionAbout(); } };
        init(m_actionAbout, "About",
             "Show information about GoGui, Go program and Java environment");

        m_actionAttachProgram = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionAttachProgram(); } };
        init(m_actionAttachProgram, "Attach Program...",
             "Attach Go program to current game", KeyEvent.VK_A, null);

        m_actionBackward = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBackward(1); } };
        init(m_actionBackward, "Backward", "Go one move backward",
             KeyEvent.VK_LEFT, "gogui-previous");

        m_actionBackwardTen = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBackward(10); } };
        init(m_actionBackwardTen, "Backward 10", "Go ten moves backward",
             KeyEvent.VK_LEFT, getShortcut() | ActionEvent.SHIFT_MASK, null);

        m_actionBeginning = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBeginning(); } };
        init(m_actionBeginning, "Beginning", "Go to beginning of game",
             KeyEvent.VK_HOME, "gogui-first");

        m_actionDetachProgram = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionDetachProgram(); } };
        init(m_actionDetachProgram, "Detach Program...",
             "Detach Go program from current game and terminate it");

        m_actionDocumentation = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionDocumentation(); } };
        init(m_actionAbout, "GoGui Documentation", "Browse GoGui manual",
             KeyEvent.VK_F1, getFunctionKeyShortcut());

        m_actionEnd = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionEnd(); } };
        init(m_actionEnd, "End", "Go to end of game",
             KeyEvent.VK_END, "gogui-last");

        m_actionForward = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionForward(1); } };
        init(m_actionForward, "Forward", "Go one move forward",
             KeyEvent.VK_RIGHT, "gogui-next");

        m_actionForwardTen = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionForward(10); } };
        init(m_actionForwardTen, "Forward 10", "Go ten moves forward",
             KeyEvent.VK_RIGHT, getShortcut() | ActionEvent.SHIFT_MASK, null);

        m_actionInterrupt = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionInterrupt(); } };
        init(m_actionInterrupt, "Interrupt", "Interrupt program",
             KeyEvent.VK_ESCAPE, 0, "gogui-interrupt");

        m_actionNextVariation = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionNextVariation(); } };
        init(m_actionNextVariation, "Next Variation", "Go to next variation",
             KeyEvent.VK_DOWN, "gogui-down");

        m_actionNewGame = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionNewGame(); } };
        init(m_actionNewGame, "New Game", "Clear board and begin new game",
             "gogui-newgame");

        m_actionOpen = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionOpen(); } };
        init(m_actionOpen, "Open...", "Open game", KeyEvent.VK_O,
             "document-open");

        m_actionPass = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPass(); } };
        init(m_actionPass, "Pass", "Play a pass", KeyEvent.VK_F2,
             getFunctionKeyShortcut(), "gogui-pass");

        m_actionPlay = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPlay(false); } };
        init(m_actionPlay, "Play", "Make computer play", KeyEvent.VK_F5,
             getFunctionKeyShortcut(), "gogui-play");

        m_actionPreviousVariation = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPreviousVariation(); } };
        init(m_actionPreviousVariation, "Previous Variation",
             "Go to previous variation", KeyEvent.VK_UP, "gogui-up");

        m_actionPrint = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPrint(); } };
        init(m_actionPrint, "Print...", "Print current position",
             KeyEvent.VK_P, null);

        m_actionSave = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSave(); } };
        init(m_actionSave, "Save", "Save game", KeyEvent.VK_S,
             "document-save");

        m_actionSaveAs = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSaveAs(); } };
        init(m_actionSaveAs, "Save As...", "Save game", "document-save-as");

        m_actionQuit = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionQuit(); } };
        init(m_actionQuit, "Quit", "Quit GoGui", KeyEvent.VK_Q, null);

    }

    public void update()
    {
        ConstNode node = m_goGui.getGame().getCurrentNode();
        boolean hasFather = (node.getFatherConst() != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        boolean isProgramAttached = m_goGui.isProgramAttached();
        m_actionBackward.setEnabled(hasFather);
        m_actionBackwardTen.setEnabled(hasFather);
        m_actionBeginning.setEnabled(hasFather);
        m_actionDetachProgram.setEnabled(isProgramAttached);
        m_actionEnd.setEnabled(hasChildren);
        m_actionForward.setEnabled(hasChildren);
        m_actionForwardTen.setEnabled(hasChildren);
        m_actionInterrupt.setEnabled(isProgramAttached);
        m_actionNextVariation.setEnabled(hasNextVariation);
        m_actionPlay.setEnabled(isProgramAttached);
        m_actionPreviousVariation.setEnabled(hasPreviousVariation);
        updateFile(m_goGui.getFile());
    }

    private final GoGui m_goGui;

    private void init(AbstractAction action, String name, String desc)
    {
        init(action, name, desc, null, 0, null);
    }

    private void init(AbstractAction action, String name, String desc,
                      String icon)
    {
        init(action, name, desc, null, 0, icon);
    }

    private void init(AbstractAction action, String name, String desc,
                      int accel, String icon)
    {
        init(action, name, desc, new Integer(accel), getShortcut(), icon);
    }

    private void init(AbstractAction action, String name, String desc,
                      int accel, int modifier, String icon)
    {
        init(action, name, desc, new Integer(accel), modifier, icon);
    }

    private void init(AbstractAction action, String name, String desc,
                      int accel, int modifier)
    {
        init(action, name, desc, new Integer(accel), modifier, null);
    }

    private void init(AbstractAction action, String name, String desc,
                      Integer accel, int modifier, String icon)
    {
        action.putValue(AbstractAction.NAME, name);
        setDescription(action, desc);
        if (accel != null)
        {
            KeyStroke keyStroke = getKeyStroke(accel.intValue(), modifier);
            action.putValue(AbstractAction.ACCELERATOR_KEY, keyStroke);
        }
        if (icon != null)
            action.putValue(AbstractAction.SMALL_ICON,
                            GuiUtil.getIcon(icon, name));
    }

    private void setDescription(AbstractAction action, String desc)
    {
        action.putValue(AbstractAction.SHORT_DESCRIPTION, desc);
    }

    /** Get shortcut modifier for function keys.
        Returns 0, unless platform is Mac.
    */
    private static int getFunctionKeyShortcut()
    {
        if (Platform.isMac())
            return getShortcut();
        return 0;
    }

    private static KeyStroke getKeyStroke(int keyCode, int modifier)
    {
        return KeyStroke.getKeyStroke(keyCode, modifier);
    }

    private static int getShortcut()
    {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

    private void updateFile(File file)
    {
        String desc = "Save game";
        if (file != null)
            desc = desc + " (" + file + ")";
        setDescription(m_actionSave, desc);
        m_actionSave.setEnabled(file != null && m_goGui.isModified());
    }
}
