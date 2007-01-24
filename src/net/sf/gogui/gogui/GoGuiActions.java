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
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
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
    public final AbstractAction m_actionAbout =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionAbout(); } },
             "About", "Show information about GoGui, Go program and Java");

    public final AbstractAction m_actionAttachProgram =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionAttachProgram(); } },
             "Attach Program...", "Attach Go program to current game",
             KeyEvent.VK_A, null);

    public final AbstractAction m_actionBackward =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBackward(1); } },
             "Backward", "Go one move backward", KeyEvent.VK_LEFT,
             "gogui-previous");

    public final AbstractAction m_actionBackwardTen =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBackward(10); } },
             "Backward 10", "Go ten moves backward",
             KeyEvent.VK_LEFT, getShortcut() | ActionEvent.SHIFT_MASK, null);

    public final AbstractAction m_actionBeginning =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBeginning(); } },
             "Beginning", "Go to beginning of game", KeyEvent.VK_HOME,
             "gogui-first");

    public final AbstractAction m_actionClockHalt =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionClockHalt(); } },
             "Halt", "Halt clock");

    public final AbstractAction m_actionClockResume =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionClockResume(); } },
             "Resume", "Resume clock");

    public final AbstractAction m_actionClockRestore =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionClockRestore(); } },
             "Restore", "Restore clock to time stored at current position");

    public final AbstractAction m_actionComputerBlack =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionComputerColor(true, false); } },
             "Black", "Make computer play Black");

    public final AbstractAction m_actionComputerBoth =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionComputerColor(true, true); } },
             "Both", "Make computer play both sides");

    public final AbstractAction m_actionComputerNone =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionComputerColor(false, false); } },
             "None", "Make computer play no side");

    public final AbstractAction m_actionComputerWhite =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionComputerColor(false, true); } },
             "White", "Make computer play White");

    public final AbstractAction m_actionDetachProgram =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionDetachProgram(); } },
             "Detach Program...",
             "Detach Go program from current game and terminate it");

    public final AbstractAction m_actionDocumentation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionDocumentation(); } },
             "GoGui Documentation", "Open GoGui manual", KeyEvent.VK_F1,
             getFunctionKeyShortcut());

    public final AbstractAction m_actionEnd =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionEnd(); } },
             "End", "Go to end of game", KeyEvent.VK_END, "gogui-last");

    public final AbstractAction m_actionExportSgfPosition =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionExportSgfPosition(); } },
             "SGF Position...", "Export position as SGF file");

    public final AbstractAction m_actionExportLatexMainVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionExportLatexMainVariation(); } },
             "LaTeX Main Variation...",
             "Export main variation as LaTeX PSGO file");

    public final AbstractAction m_actionExportLatexPosition =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionExportLatexPosition(); } },
             "LaTeX Position...", "Export position as LaTeX PSGO file");

    public final AbstractAction m_actionExportTextPosition =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionExportTextPosition(); } },
             "Text Position...", "Export position as text diagram");

    public final AbstractAction m_actionExportTextPositionToClipboard =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionExportTextPositionToClipboard(); } },
             "Text Position to Clipboard",
             "Export position as text diagram to clipboard");

    public final AbstractAction m_actionForward =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionForward(1); } },
             "Forward", "Go one move forward", KeyEvent.VK_RIGHT,
             "gogui-next");

    public final AbstractAction m_actionForwardTen =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionForward(10); } },
             "Forward 10", "Go ten moves forward", KeyEvent.VK_RIGHT,
             getShortcut() | ActionEvent.SHIFT_MASK, null);

    public final AbstractAction m_actionImportTextPosition =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionImportTextPosition(); } },
             "Text Position...",
             "Import position as text diagram from file");

    public final AbstractAction m_actionImportTextPositionFromClipboard =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionImportTextPositionFromClipboard(); } },
             "Text Position from Clipboard",
             "Import position as text diagram from clipboard");

    public final AbstractAction m_actionInterrupt =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionInterrupt(); } },
             "Interrupt", "Interrupt program", KeyEvent.VK_ESCAPE, 0,
             "gogui-interrupt");

    public final AbstractAction m_actionNextVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionNextVariation(); } },
             "Next Variation", "Go to next variation", KeyEvent.VK_DOWN,
             "gogui-down");

    public final AbstractAction m_actionNewGame =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionNewGame(); } },
             "New Game", "Clear board and begin new game", "gogui-newgame");

    public final AbstractAction m_actionOpen =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionOpen(); } },
             "Open...", "Open game", KeyEvent.VK_O, "document-open");

    public final AbstractAction m_actionPass =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPass(); } },
             "Pass", "Play a pass", KeyEvent.VK_F2,
             getFunctionKeyShortcut(), "gogui-pass");

    public final AbstractAction m_actionPlay =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPlay(false); } },
             "Play", "Make computer play", KeyEvent.VK_F5,
             getFunctionKeyShortcut(), "gogui-play");

    public final AbstractAction m_actionPlaySingleMove =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPlay(true); } },
             "Play Single Move",
             "Make computer play a move (do not change computer color)",
             KeyEvent.VK_F5,
             getFunctionKeyShortcut() | ActionEvent.SHIFT_MASK);

    public final AbstractAction m_actionPreviousVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPreviousVariation(); } },
             "Previous Variation", "Go to previous variation", KeyEvent.VK_UP,
             "gogui-up");

    public final AbstractAction m_actionPrint =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPrint(); } },
             "Print...", "Print current position", KeyEvent.VK_P, null);

    public final AbstractAction m_actionSave =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSave(); } },
             "Save", "Save game", KeyEvent.VK_S, "document-save");

    public final AbstractAction m_actionSaveAs =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSaveAs(); } },
             "Save As...", "Save game", "document-save-as");

    public final AbstractAction m_actionScore =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionScore(); } },
             "Score", "Score position");

    public final AbstractAction m_actionQuit =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionQuit(); } },
             "Quit", "Quit GoGui", KeyEvent.VK_Q, null);

    public GoGuiActions(GoGui goGui)
    {
        m_goGui = goGui;
    }

    public void update()
    {
        ConstGame game = m_goGui.getGame();
        ConstNode node = game.getCurrentNode();
        boolean hasFather = (node.getFatherConst() != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        boolean isProgramAttached = m_goGui.isProgramAttached();
        boolean computerBlack = m_goGui.isComputerColor(GoColor.BLACK);
        boolean computerWhite = m_goGui.isComputerColor(GoColor.WHITE);
        ConstClock clock = game.getClock();
        m_actionBackward.setEnabled(hasFather);
        m_actionBackwardTen.setEnabled(hasFather);
        m_actionBeginning.setEnabled(hasFather);
        m_actionClockHalt.setEnabled(clock.isRunning());
        m_actionClockResume.setEnabled(! clock.isRunning());
        m_actionClockRestore.setEnabled(NodeUtil.canRestoreTime(node, clock));
        m_actionComputerBlack.setEnabled(isProgramAttached);
        setSelected(m_actionComputerBlack, computerBlack && ! computerWhite);
        m_actionComputerBoth.setEnabled(isProgramAttached);
        setSelected(m_actionComputerBoth, computerBlack && computerWhite);
        m_actionComputerNone.setEnabled(isProgramAttached);
        setSelected(m_actionComputerNone, ! computerBlack && ! computerWhite);
        m_actionComputerWhite.setEnabled(isProgramAttached);
        setSelected(m_actionComputerWhite, ! computerBlack && computerWhite);
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

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc)
    {
        return init(action, name, desc, null, 0, null);
    }

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc, String icon)
    {
        return init(action, name, desc, null, 0, icon);
    }

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc, int accel, String icon)
    {
        return init(action, name, desc, new Integer(accel), getShortcut(),
                    icon);
    }

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc, int accel, int modifier,
                                       String icon)
    {
        return init(action, name, desc, new Integer(accel), modifier, icon);
    }

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc, int accel, int modifier)
    {
        return init(action, name, desc, new Integer(accel), modifier, null);
    }

    private static AbstractAction init(AbstractAction action, String name,
                                       String desc, Integer accel,
                                       int modifier, String icon)
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
        return action;
    }

    private static void setDescription(AbstractAction action, String desc)
    {
        action.putValue(AbstractAction.SHORT_DESCRIPTION, desc);
    }

    private static void setSelected(AbstractAction action, boolean selected)
    {
        action.putValue("selected", Boolean.valueOf(selected));
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
