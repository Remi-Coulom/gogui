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

    public final AbstractAction m_actionBackToMainVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBackToMainVariation(); } },
             "Back to Main Variation", "Go back to main variation",
             KeyEvent.VK_M);

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

    public final AbstractAction m_actionBoardSize9 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(9); } },
             "9", "Change board size to 9x9");

    public final AbstractAction m_actionBoardSize11 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(11); } },
             "11", "Change board size to 11x11");

    public final AbstractAction m_actionBoardSize13 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(13); } },
             "13", "Change board size to 13x13");

    public final AbstractAction m_actionBoardSize15 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(15); } },
             "15", "Change board size to 15x15");

    public final AbstractAction m_actionBoardSize17 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(17); } },
             "17", "Change board size to 17x17");

    public final AbstractAction m_actionBoardSize19 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(19); } },
             "19", "Change board size to 19x19");

    public final AbstractAction m_actionBoardSizeOther =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSizeOther(); } },
             "Other", "Change board size to other values");

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

    public final AbstractAction m_actionGoto =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionGoto(); } },
             "Go to Move...", "Go to position after a move number",
             KeyEvent.VK_G);

    public final AbstractAction m_actionGotoVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionGotoVariation(); } },
             "Go to Variation...", "Go to beginning of a variation");

    public final AbstractAction m_actionDeleteSideVariations =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionDeleteSideVariations(); } },
             "Delete side variations",
             "Delete all variations but the main variation");

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

    public final AbstractAction m_actionFind =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionFind(); } },
             "Find in Comments...", "Search for matching text in comments",
             KeyEvent.VK_F);

    public final AbstractAction m_actionFindNext =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionFindNext(); } },
             "Find Next", "Search for next match in comments",
             KeyEvent.VK_F3, getFunctionKeyShortcut());

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

    public final AbstractAction m_actionGameInfo =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionGameInfo(); } },
             "Game Info", "Show and edit game information", KeyEvent.VK_I);

    public final AbstractAction m_actionHandicapNone =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(0); } },
             "None", "Do not use handicap stones");

    public final AbstractAction m_actionHandicap2 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(2); } },
             "2", "Use two handicap stones");

    public final AbstractAction m_actionHandicap3 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(3); } },
             "3", "Use three handicap stones");

    public final AbstractAction m_actionHandicap4 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(4); } },
             "4", "Use four handicap stones");

    public final AbstractAction m_actionHandicap5 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(5); } },
             "5", "Use five handicap stones");

    public final AbstractAction m_actionHandicap6 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(6); } },
             "6", "Use six handicap stones");

    public final AbstractAction m_actionHandicap7 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(7); } },
             "7", "Use seven handicap stones");

    public final AbstractAction m_actionHandicap8 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(8); } },
             "8", "Use eight handicap stones");

    public final AbstractAction m_actionHandicap9 =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionHandicap(9); } },
             "9", "Use nine handicap stones");

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

    public final AbstractAction m_actionKeepOnlyPosition =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionKeepOnlyPosition(); } },
             "Keep only Position",
             "Delete variations and moves and keep only the current position");

    public final AbstractAction m_actionMakeMainVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionMakeMainVariation(); } },
             "Make Main Variation",
             "Make current variation the main variation");

    public final AbstractAction m_actionNextEarlierVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionNextEarlierVariation(); } },
             "Next Earlier Variation", "Go to next earlier variation",
             KeyEvent.VK_DOWN, getShortcut() | ActionEvent.SHIFT_MASK);

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

    public final AbstractAction m_actionPreviousEarlierVariation =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionPreviousEarlierVariation(); } },
             "Previous Earlier Variation", "Go to previous earlier variation",
             KeyEvent.VK_UP, getShortcut() | ActionEvent.SHIFT_MASK);

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

    public final AbstractAction m_actionSetup =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSetup(); } },
             "Setup", "Enter or leave setup mode");

    public final AbstractAction m_actionSetupBlack =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSetupColor(GoColor.BLACK); } },
             "Setup Black", "Change setup color to Black");

    public final AbstractAction m_actionSetupWhite =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionSetupColor(GoColor.WHITE); } },
             "Setup White", "Change setup color to White");

    public final AbstractAction m_actionTruncate =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionTruncate(); } },
             "Truncate", "Truncate subtree including this position");

    public final AbstractAction m_actionTruncateChildren =
        init(new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionTruncateChildren(); } },
             "Truncate Children", "Truncate all children of this position");

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
        int handicap = m_goGui.getHandicapDefault();
        boolean setupMode = m_goGui.isInSetupMode();
        GoColor setupColor = m_goGui.getSetupColor();
        ConstNode node = game.getCurrentNode();
        boolean hasFather = (node.getFatherConst() != null);
        boolean hasChildren = (node.getNumberChildren() > 0);
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        boolean hasNextEarlierVariation =
            (NodeUtil.getNextEarlierVariation(node) != null);
        boolean hasPrevEarlierVariation =
            (NodeUtil.getPreviousEarlierVariation(node) != null);
        boolean isInMain = NodeUtil.isInMainVariation(node);
        boolean treeHasVariations = game.getTree().hasVariations();
        boolean isProgramAttached = m_goGui.isProgramAttached();
        boolean computerBlack = m_goGui.isComputerColor(GoColor.BLACK);
        boolean computerWhite = m_goGui.isComputerColor(GoColor.WHITE);
        boolean hasPattern = (m_goGui.getPattern() != null);
        ConstClock clock = game.getClock();
        int boardSize = game.getSize();
        m_actionBackToMainVariation.setEnabled(! isInMain);
        m_actionBackward.setEnabled(hasFather);
        m_actionBackwardTen.setEnabled(hasFather);
        m_actionBeginning.setEnabled(hasFather);
        setSelected(m_actionBoardSize9, boardSize == 9);
        setSelected(m_actionBoardSize11, boardSize == 11);
        setSelected(m_actionBoardSize13, boardSize == 13);
        setSelected(m_actionBoardSize15, boardSize == 15);
        setSelected(m_actionBoardSize17, boardSize == 17);
        setSelected(m_actionBoardSize19, boardSize == 19);
        setSelected(m_actionBoardSizeOther,
                    boardSize < 9 || boardSize > 19 || boardSize % 2 == 0);
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
        m_actionDeleteSideVariations.setEnabled(isInMain && treeHasVariations);
        m_actionDetachProgram.setEnabled(isProgramAttached);
        m_actionEnd.setEnabled(hasChildren);
        m_actionFindNext.setEnabled(hasPattern);
        m_actionForward.setEnabled(hasChildren);
        m_actionForwardTen.setEnabled(hasChildren);
        m_actionGoto.setEnabled(hasFather || hasChildren);
        m_actionGotoVariation.setEnabled(hasFather || hasChildren);
        setSelected(m_actionHandicapNone, handicap == 0);
        setSelected(m_actionHandicap2, handicap == 2);
        setSelected(m_actionHandicap3, handicap == 3);
        setSelected(m_actionHandicap4, handicap == 4);
        setSelected(m_actionHandicap5, handicap == 5);
        setSelected(m_actionHandicap6, handicap == 6);
        setSelected(m_actionHandicap7, handicap == 7);
        setSelected(m_actionHandicap8, handicap == 8);
        setSelected(m_actionHandicap9, handicap == 9);
        m_actionInterrupt.setEnabled(isProgramAttached);
        m_actionKeepOnlyPosition.setEnabled(hasFather || hasChildren);
        m_actionMakeMainVariation.setEnabled(! isInMain);
        m_actionNextEarlierVariation.setEnabled(hasNextEarlierVariation);
        m_actionNextVariation.setEnabled(hasNextVariation);
        m_actionPlay.setEnabled(isProgramAttached);
        m_actionPreviousVariation.setEnabled(hasPreviousVariation);
        m_actionPreviousEarlierVariation.setEnabled(hasPrevEarlierVariation);
        m_actionSetupBlack.setEnabled(setupMode);
        m_actionSetupWhite.setEnabled(setupMode);
        setSelected(m_actionSetupBlack, setupColor == GoColor.BLACK);
        setSelected(m_actionSetupWhite, setupColor == GoColor.WHITE);
        m_actionTruncate.setEnabled(hasFather);
        m_actionTruncateChildren.setEnabled(hasChildren);
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
                                       String desc, int accel)
    {
        return init(action, name, desc, new Integer(accel), getShortcut(),
                    null);
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
