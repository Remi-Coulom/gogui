//----------------------------------------------------------------------------
// GoGuiActions.java
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import javax.swing.KeyStroke;
import net.sf.gogui.game.Clock;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGame;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.gui.ConstGuiBoard;
import net.sf.gogui.gui.GameTreePanel;
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
    abstract class Action
        extends AbstractAction
    {
        public Action(String name)
        {
            this(name, null, null, 0, null);
        }

        public Action(String name, String desc)
        {
            this(name, desc, null, 0, null);
        }

        public Action(String name, String desc, String icon)
        {
            this(name, desc, null, 0, icon);
        }

        public Action(String name, String desc, int accel, String icon)
        {
            this(name, desc, accel, SHORTCUT, icon);
        }

        public Action(String name, String desc, int accel)
        {
            this(name, desc, accel, SHORTCUT, null);
        }

        public Action(String name, String desc, int accel, int modifier)
        {
            this(name, desc, accel, modifier, null);
        }

        public Action(String name, String desc, Integer accel, int modifier,
                      String icon)
        {
            putValue(AbstractAction.NAME, name);
            if (desc != null)
                setDescription(desc);
            if (accel != null)
                putValue(AbstractAction.ACCELERATOR_KEY,
                         getKeyStroke(accel.intValue(), modifier));
            if (icon != null)
                putValue(AbstractAction.SMALL_ICON,
                         GuiUtil.getIcon(icon, name));
            m_allActions.add(this);
        }

        public final void setDescription(String desc)
        {
            putValue(AbstractAction.SHORT_DESCRIPTION, desc);
        }

        public void setName(String name)
        {
            putValue(AbstractAction.NAME, name);
        }

        public void setSelected(boolean selected)
        {
            putValue("selected", Boolean.valueOf(selected));
        }

        /** Serial version to suppress compiler warning.
            Contains a marker comment for serialver.sf.net
        */
        private static final long serialVersionUID = 0L; // SUID
    }

    public final ArrayList<Action> m_allActions
        = new ArrayList<Action>();

    public final Action m_actionAbout =
        new Action("About") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAbout(); } };

    public final Action m_actionAddBookmark =
        new Action("Add Bookmark", null, KeyEvent.VK_B) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAddBookmark(); } };

    public final Action m_actionBackToMainVariation =
        new Action("Back to Main Variation", null, KeyEvent.VK_M) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackToMainVariation(); } };

    public final Action m_actionBackward =
        new Action("Backward", "Go one move backward", KeyEvent.VK_LEFT,
                   "gogui-previous") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(1); } };

    public final Action m_actionBackwardTen =
        new Action("Backward 10", "Go ten moves backward", KeyEvent.VK_LEFT,
                   SHORTCUT | ActionEvent.SHIFT_MASK, "gogui-previous-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(10); } };

    public final Action m_actionBeginning =
        new Action("Beginning", "Go to beginning of game", KeyEvent.VK_HOME,
                   "gogui-first") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBeginning(); } };

    public final Action m_actionBoardSize9 =
        new Action("9") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(9); } };

    public final Action m_actionBoardSize11 =
        new Action("11") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(11); } };

    public final Action m_actionBoardSize13 =
        new Action("13") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(13); } };

    public final Action m_actionBoardSize15 =
        new Action("15") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(15); } };

    public final Action m_actionBoardSize17 =
        new Action("17") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(17); } };

    public final Action m_actionBoardSize19 =
        new Action("19") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(19); } };

    public final Action m_actionBoardSizeOther =
        new Action("Other") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSizeOther(); } };

    public final Action m_actionClockHalt =
        new Action("Halt") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockHalt(); } };

    public final Action m_actionClockResume =
        new Action("Resume") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockResume(); } };

    public final Action m_actionClockRestore =
        new Action("Restore") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockRestore(); } };

    public final Action m_actionClockStart =
        new Action("Start") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockStart(); } };

    public final Action m_actionComputerBlack =
        new Action("Black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, false); } };

    public final Action m_actionComputerBoth =
        new Action("Both") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, true); } };

    public final Action m_actionComputerNone =
        new Action("None") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, false); } };

    public final Action m_actionComputerWhite =
        new Action("White") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, true); } };

    public final Action m_actionEditBookmarks =
        new Action("Edit Bookmarks...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditBookmarks(); } };

    public final Action m_actionEditPrograms =
        new Action("Edit Programs...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditPrograms(); } };

    public final Action m_actionGoto =
        new Action("Go to Move...", null, KeyEvent.VK_G) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGoto(); } };

    public final Action m_actionGotoVariation =
        new Action("Go to Variation...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoVariation(); } };

    public final Action m_actionDeleteSideVariations =
        new Action("Delete Side Variations") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDeleteSideVariations(); } };

    public final Action m_actionDetachProgram =
        new Action("Detach Program")
        {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDetachProgram(); } };

    public final Action m_actionDocumentation =
        new Action("GoGui Help", null, KeyEvent.VK_F1, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDocumentation(); } };

    public final Action m_actionEnd =
        new Action("End", "Go to end of game", KeyEvent.VK_END, "gogui-last") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEnd(); } };

    public final Action m_actionExportSgfPosition =
        new Action("SGF Position...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportSgfPosition(); } };

    public final Action m_actionExportLatexMainVariation =
        new Action("LaTeX Main Variation...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexMainVariation(); } };

    public final Action m_actionExportLatexPosition =
        new Action("LaTeX Position...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexPosition(); } };

    public final Action m_actionExportPng =
        new Action("PNG Image...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportPng(); } };

    public final Action m_actionExportTextPosition =
        new Action("Text Position...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPosition(); } };

    public final Action m_actionExportTextPositionToClipboard =
        new Action("Text Position to Clipboard") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPositionToClipboard(); } };

    public final Action m_actionFind =
        new Action("Find in Comments...", null, KeyEvent.VK_F) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFind(); } };

    public final Action m_actionFindNext =
        new Action("Find Next", null, KeyEvent.VK_F3, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFindNext(); } };

    public final Action m_actionForward =
        new Action("Forward", "Go one move forward", KeyEvent.VK_RIGHT,
                   "gogui-next") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(1); } };

    public final Action m_actionForwardTen =
        new Action("Forward 10", "Go ten moves forward", KeyEvent.VK_RIGHT,
                   SHORTCUT | ActionEvent.SHIFT_MASK, "gogui-next-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(10); } };

    public final Action m_actionGameInfo =
        new Action("Game Info", null, KeyEvent.VK_I) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGameInfo(); } };

    public final Action m_actionShellSave =
        new Action("Save Log...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSave(); } };

    public final Action m_actionShellSaveCommands =
        new Action("Save Commands...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSaveCommands(); } };

    public final Action m_actionShellSendFile =
        new Action("Send File...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSendFile(); } };

    public final Action m_actionHandicapNone =
        new Action("None") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(0); } };

    public final Action m_actionHandicap2 =
        new Action("2") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(2); } };

    public final Action m_actionHandicap3 =
        new Action("3") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(3); } };

    public final Action m_actionHandicap4 =
        new Action("4") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(4); } };

    public final Action m_actionHandicap5 =
        new Action("5") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(5); } };

    public final Action m_actionHandicap6 =
        new Action("6") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(6); } };

    public final Action m_actionHandicap7 =
        new Action("7") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(7); } };

    public final Action m_actionHandicap8 =
        new Action("8") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(8); } };

    public final Action m_actionHandicap9 =
        new Action("9") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(9); } };

    public final Action m_actionImportTextPosition =
        new Action("Text Position...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPosition(); } };

    public final Action m_actionImportTextPositionFromClipboard =
        new Action("Text Position from Clipboard") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPositionFromClipboard(); } };

    public final Action m_actionInterrupt =
        new Action("Interrupt", null, "gogui-interrupt") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionInterrupt(); } };

    public final Action m_actionKeepOnlyPosition =
        new Action("Keep Only Position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionKeepOnlyPosition(); } };

    public final Action m_actionMainWindowActivate =
        new Action("Main Window", null, KeyEvent.VK_F6, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMainWindowActivate(); } };

    public final Action m_actionMakeMainVariation =
        new Action("Make Main Variation") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMakeMainVariation(); } };

    public final Action m_actionNextEarlierVariation =
        new Action("Next Earlier Variation", null, KeyEvent.VK_DOWN,
                   SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextEarlierVariation(); } };

    public final Action m_actionNextVariation =
        new Action("Next Variation", "Go to next variation", KeyEvent.VK_DOWN,
                   "gogui-down") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextVariation(); } };

    public final Action m_actionNewGame =
        new Action("New Game", "Clear board and start new game",
                   "gogui-newgame") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewGame(); } };

    public final Action m_actionNewProgram =
        new Action("New Program...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewProgram(); } };

    public final Action m_actionOpen =
        new Action("Open...", "Open", KeyEvent.VK_O, "document-open") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionOpen(); } };

    public final Action m_actionPass =
        new Action("Pass", "Play a pass", KeyEvent.VK_F2, FUNCTION_KEY,
                   "gogui-pass") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPass(); } };

    public final Action m_actionPlay =
        new Action("Computer Play", "Make computer play", KeyEvent.VK_F5,
                   FUNCTION_KEY, "gogui-play") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(false); } };

    public final Action m_actionPlaySingleMove =
        new Action("Play Single Move", null, KeyEvent.VK_F5,
                   FUNCTION_KEY | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(true); } };

    public final Action m_actionPreviousEarlierVariation =
        new Action("Previous Earlier Variation", null, KeyEvent.VK_UP,
                   SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousEarlierVariation(); } };

    public final Action m_actionPreviousVariation =
        new Action("Previous Variation", "Go to previous variation",
                   KeyEvent.VK_UP, "gogui-up") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousVariation(); } };

    public final Action m_actionPrint =
        new Action("Print...", null, KeyEvent.VK_P, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPrint(); } };

    public final Action m_actionReattachProgram =
        new Action("Reattach Program", null, KeyEvent.VK_T) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachProgram(); } };

    public final Action m_actionReattachWithParameters =
        new Action("Reattach With Parameters", null, KeyEvent.VK_T,
                   SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachWithParameters(); } };

    public final Action m_actionSave =
        new Action("Save", "Save", KeyEvent.VK_S, "document-save") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSave(); } };

    public final Action m_actionSaveAs =
        new Action("Save As...", "Save As", "document-save-as") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveAs(); } };

    public final Action m_actionSaveParameters =
        new Action("Save Parameters...") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveParameters(); } };

    public final Action m_actionScore =
        new Action("Score") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionScore(); } };

    public final Action m_actionSetupBlack =
        new Action("Setup Black", "Add black stones and set Black to play",
                   "gogui-setup-black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(BLACK); } };

    public final Action m_actionSetupWhite =
        new Action("Setup White", "Add white stones and set White to play",
                   "gogui-setup-white") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(WHITE); } };

    public final Action m_actionShowAnalyzeDialog =
        new Action("Analyze Commands", null, KeyEvent.VK_F8, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowAnalyzeDialog(); } };

    public final Action m_actionShowShell =
        new Action("GTP Shell", null, KeyEvent.VK_F9, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowShell(); } };

    public final Action m_actionShowTree =
        new Action("Tree Viewer", null, KeyEvent.VK_F7, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowTree(); } };

    public final Action m_actionToggleAutoNumber =
        new Action("Auto Number") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleAutoNumber(); } };

    public final Action m_actionToggleBeepAfterMove =
        new Action("Play Sound") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleBeepAfterMove(); } };

    public final Action m_actionToggleCompletion =
        new Action("Popup Completions") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCompletion(); } };

    public final Action m_actionToggleCommentMonoFont =
        new Action("Monospace Comment Font") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCommentMonoFont(); } };

    public final Action m_actionToggleShowCursor =
        new Action("Cursor") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowCursor(); } };

    public final Action m_actionToggleShowGrid =
        new Action("Grid Labels") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowGrid(); } };

    public final Action m_actionToggleShowInfoPanel =
        new Action("Info Panel") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowInfoPanel(); } };

    public final Action m_actionToggleShowLastMove =
        new Action("Last Move") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowLastMove(); } };

    public final Action m_actionToggleShowSubtreeSizes =
        new Action("Subtree Sizes") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowSubtreeSizes(); } };

    public final Action m_actionToggleShowToolbar =
        new Action("Toolbar") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowToolbar(); } };

    public final Action m_actionToggleShowVariations =
        new Action("Variation Labels") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowVariations(); } };

    public final Action m_actionToggleTimeStamp =
        new Action("Timestamp") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleTimeStamp(); } };

    public final Action m_actionTreeLabelsNumber =
        new Action("Move Number") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NUMBER); } };

    public final Action m_actionTreeLabelsMove =
        new Action("Move") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_MOVE); } };

    public final Action m_actionTreeLabelsNone =
        new Action("None") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NONE); } };

    public final Action m_actionTreeSizeLarge =
        new Action("Large") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_LARGE); } };

    public final Action m_actionTreeSizeNormal =
        new Action("Normal") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_NORMAL); } };

    public final Action m_actionTreeSizeSmall =
        new Action("Small") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_SMALL); } };

    public final Action m_actionTreeSizeTiny =
        new Action("Tiny") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_TINY); } };

    public final Action m_actionTruncate =
        new Action("Truncate") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncate(); } };

    public final Action m_actionTruncateChildren =
        new Action("Truncate Children") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncateChildren(); } };

    public final Action m_actionQuit =
        new Action("Quit", null, KeyEvent.VK_Q, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionQuit(); } };

    public GoGuiActions(GoGui goGui)
    {
        m_goGui = goGui;
    }

    public static void register(JComponent component, Action action)
    {
        KeyStroke keyStroke =
            (KeyStroke)action.getValue(AbstractAction.ACCELERATOR_KEY);
        if (keyStroke != null)
        {
            String name = (String)action.getValue(AbstractAction.NAME);
            InputMap inputMap =
                component.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            inputMap.put(keyStroke, name);
            component.getActionMap().put(name, action);
        }
    }

    public void registerAll(JComponent component)
    {
        for (Action action : m_allActions)
            register(component, action);
    }

    public void update()
    {
        ConstGame game = m_goGui.getGame();
        int handicap = m_goGui.getHandicapDefault();
        boolean setupMode = m_goGui.isInSetupMode();
        GoColor setupColor = m_goGui.getSetupColor();
        File file = m_goGui.getFile();
        boolean isModified = m_goGui.isModified();
        ConstGuiBoard guiBoard = m_goGui.getGuiBoard();
        String name = m_goGui.getProgramName();
        ConstNode node = game.getCurrentNode();
        boolean hasFather = (node.getFatherConst() != null);
        boolean hasChildren = node.hasChildren();
        boolean hasNextVariation = (NodeUtil.getNextVariation(node) != null);
        boolean hasPreviousVariation =
            (NodeUtil.getPreviousVariation(node) != null);
        boolean hasNextEarlierVariation =
            (NodeUtil.getNextEarlierVariation(node) != null);
        boolean hasPrevEarlierVariation =
            (NodeUtil.getPreviousEarlierVariation(node) != null);
        boolean isInMain = NodeUtil.isInMainVariation(node);
        boolean treeHasVariations = game.getTree().hasVariations();
        boolean isCommandInProgress = m_goGui.isCommandInProgress();
        boolean isProgramAttached = m_goGui.isProgramAttached();
        boolean isInterruptSupported = m_goGui.isInterruptSupported();
        boolean computerBlack = m_goGui.isComputerColor(BLACK);
        boolean computerWhite = m_goGui.isComputerColor(WHITE);
        boolean computerBoth = (computerBlack && computerWhite);
        boolean hasPattern = (m_goGui.getPattern() != null);
        int numberPrograms = m_goGui.getNumberPrograms();
        ConstClock clock = game.getClock();
        int boardSize = game.getSize();
        GoColor toMove = game.getToMove();
        m_actionBackToMainVariation.setEnabled(! isInMain);
        m_actionBackward.setEnabled(hasFather);
        m_actionBackwardTen.setEnabled(hasFather);
        m_actionBeginning.setEnabled(hasFather);
        m_actionBoardSize9.setSelected(boardSize == 9);
        m_actionBoardSize11.setSelected(boardSize == 11);
        m_actionBoardSize13.setSelected(boardSize == 13);
        m_actionBoardSize15.setSelected(boardSize == 15);
        m_actionBoardSize17.setSelected(boardSize == 17);
        m_actionBoardSize19.setSelected(boardSize == 19);
        m_actionBoardSizeOther.setSelected(boardSize < 9 || boardSize > 19
                                           || boardSize % 2 == 0);
        m_actionClockHalt.setEnabled(clock.isRunning());
        updateClockResume(clock);
        updateClockRestore(node, clock);
        updateClockStart(clock);
        m_actionComputerBlack.setEnabled(isProgramAttached);
        m_actionComputerBlack.setSelected(computerBlack && ! computerWhite);
        m_actionComputerBoth.setEnabled(isProgramAttached);
        m_actionComputerBoth.setSelected(computerBoth);
        m_actionComputerNone.setEnabled(isProgramAttached);
        m_actionComputerNone.setSelected(! computerBlack && ! computerWhite);
        m_actionComputerWhite.setEnabled(isProgramAttached);
        m_actionComputerWhite.setSelected(! computerBlack && computerWhite);
        m_actionDeleteSideVariations.setEnabled(isInMain && treeHasVariations);
        updateDetachProgram(isProgramAttached, name);
        m_actionEditPrograms.setEnabled(numberPrograms > 0);
        m_actionEnd.setEnabled(hasChildren);
        m_actionFindNext.setEnabled(hasPattern);
        m_actionForward.setEnabled(hasChildren);
        m_actionForwardTen.setEnabled(hasChildren);
        m_actionGoto.setEnabled(hasFather || hasChildren);
        m_actionGotoVariation.setEnabled(hasFather || hasChildren);
        m_actionHandicapNone.setSelected(handicap == 0);
        m_actionHandicap2.setSelected(handicap == 2);
        m_actionHandicap3.setSelected(handicap == 3);
        m_actionHandicap4.setSelected(handicap == 4);
        m_actionHandicap5.setSelected(handicap == 5);
        m_actionHandicap6.setSelected(handicap == 6);
        m_actionHandicap7.setSelected(handicap == 7);
        m_actionHandicap8.setSelected(handicap == 8);
        m_actionHandicap9.setSelected(handicap == 9);
        updateInterrupt(isProgramAttached, isInterruptSupported,
                        isCommandInProgress, name);
        m_actionKeepOnlyPosition.setEnabled(hasFather || hasChildren);
        m_actionMakeMainVariation.setEnabled(! isInMain);
        m_actionNextEarlierVariation.setEnabled(hasNextEarlierVariation);
        m_actionNextVariation.setEnabled(hasNextVariation);
        updatePass(toMove);
        updatePlay(toMove, isProgramAttached, computerBoth, name);
        m_actionPlaySingleMove.setEnabled(isProgramAttached);
        m_actionPreviousVariation.setEnabled(hasPreviousVariation);
        m_actionPreviousEarlierVariation.setEnabled(hasPrevEarlierVariation);
        m_actionReattachProgram.setEnabled(isProgramAttached);
        m_actionReattachWithParameters.setEnabled(isProgramAttached);
        updateSave(file, isModified);
        m_actionSetupBlack.setSelected(setupMode
                                       && setupColor == BLACK);
        m_actionSetupWhite.setSelected(setupMode
                                       && setupColor == WHITE);
        m_actionShellSave.setEnabled(isProgramAttached);
        m_actionShellSaveCommands.setEnabled(isProgramAttached);
        m_actionShellSendFile.setEnabled(isProgramAttached);
        m_actionShowAnalyzeDialog.setEnabled(isProgramAttached);
        m_actionShowShell.setEnabled(isProgramAttached);
        m_actionToggleAutoNumber.setSelected(m_goGui.getAutoNumber());
        m_actionToggleBeepAfterMove.setEnabled(isProgramAttached);
        m_actionToggleBeepAfterMove.setSelected(m_goGui.getBeepAfterMove());
        boolean commentMonoFont = m_goGui.getCommentMonoFont();
        m_actionToggleCommentMonoFont.setSelected(commentMonoFont);
        m_actionToggleCompletion.setSelected(m_goGui.getCompletion());
        m_actionToggleShowCursor.setSelected(guiBoard.getShowCursor());
        m_actionToggleShowGrid.setSelected(guiBoard.getShowGrid());
        m_actionToggleShowInfoPanel.setSelected(m_goGui.isInfoPanelShown());
        m_actionToggleShowLastMove.setSelected(m_goGui.getShowLastMove());
        boolean showSubtreeSizes = m_goGui.getShowSubtreeSizes();
        m_actionToggleShowSubtreeSizes.setSelected(showSubtreeSizes);
        m_actionToggleShowToolbar.setSelected(m_goGui.isToolbarShown());
        m_actionToggleShowVariations.setSelected(m_goGui.getShowVariations());
        m_actionToggleTimeStamp.setSelected(m_goGui.getTimeStamp());
        m_actionTreeLabelsNumber.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.LABEL_NUMBER);
        m_actionTreeLabelsMove.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.LABEL_MOVE);
        m_actionTreeLabelsNone.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.LABEL_NONE);
        m_actionTreeSizeLarge.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.SIZE_LARGE);
        m_actionTreeSizeNormal.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.SIZE_NORMAL);
        m_actionTreeSizeSmall.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.SIZE_SMALL);
        m_actionTreeSizeTiny.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.SIZE_TINY);
        m_actionTruncate.setEnabled(hasFather);
        m_actionTruncateChildren.setEnabled(hasChildren);
    }

    private final GoGui m_goGui;

    private static final int SHORTCUT
        = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /** Shortcut modifier for function keys.
        0, unless platform is Mac.
    */
    private static final int FUNCTION_KEY = (Platform.isMac() ? SHORTCUT : 0);

    private static KeyStroke getKeyStroke(int keyCode, int modifier)
    {
        return KeyStroke.getKeyStroke(keyCode, modifier);
    }

    private void updateClockRestore(ConstNode node, ConstClock clock)
    {
        boolean enabled = false;
        String desc = null;
        if (clock.isInitialized())
        {
            enabled = true;
            Clock tempClock = new Clock();
            tempClock.setTimeSettings(clock.getTimeSettings());
            NodeUtil.restoreClock(node, tempClock);
            StringBuilder buffer = new StringBuilder();
            buffer.append("Restore saved time (B ");
            buffer.append(tempClock.getTimeString(BLACK));
            buffer.append(", W ");
            buffer.append(tempClock.getTimeString(WHITE));
            buffer.append(')');
            desc = buffer.toString();
        }
        m_actionClockRestore.setEnabled(enabled);
        m_actionClockRestore.setDescription(desc);
    }

    private void updateClockResume(ConstClock clock)
    {
        boolean enabled = false;
        String desc = null;
        if (! clock.isRunning() && clock.getToMove() != null)
        {
            enabled = true;
            StringBuilder buffer = new StringBuilder();
            buffer.append("Resume clock (B ");
            buffer.append(clock.getTimeString(BLACK));
            buffer.append(", W ");
            buffer.append(clock.getTimeString(WHITE));
            buffer.append(')');
            desc = buffer.toString();
        }
        m_actionClockResume.setEnabled(enabled);
        m_actionClockResume.setDescription(desc);
    }

    private void updateClockStart(ConstClock clock)
    {
        boolean enabled = false;
        String desc = null;
        if (! clock.isRunning() && clock.getToMove() == null)
        {
            enabled = true;
            StringBuilder buffer = new StringBuilder();
            buffer.append("Start clock (B ");
            buffer.append(clock.getTimeString(BLACK));
            buffer.append(", W ");
            buffer.append(clock.getTimeString(WHITE));
            buffer.append(')');
            desc = buffer.toString();
        }
        m_actionClockStart.setEnabled(enabled);
        m_actionClockStart.setDescription(desc);
    }

    private void updateDetachProgram(boolean isProgramAttached, String name)
    {
        m_actionDetachProgram.setEnabled(isProgramAttached);
        if (! isProgramAttached || name == null)
            m_actionDetachProgram.setName("Detach Program");
        else
            m_actionDetachProgram.setName("Detach " + name);
    }

    private void updateInterrupt(boolean isProgramAttached,
                                 boolean isInterruptSupported,
                                 boolean isCommandInProgress, String name)
    {
        String desc;
        if (isProgramAttached)
        {
            if (name == null)
                name = "program";
            if (! isInterruptSupported)
                desc = "Interrupt (not supported by " + name + ")";
            else if (! isCommandInProgress)
                desc = "Interrupt " + name + " (no command running)";
            else
                desc = "Interrupt " + name;
        }
        else
            desc = "Interrupt (no program attached)";
        m_actionInterrupt.setDescription(desc);
        m_actionInterrupt.setEnabled(isProgramAttached
                                     && isInterruptSupported);
    }

    private void updatePass(GoColor toMove)
    {
        assert toMove.isBlackWhite();
        m_actionPass.setDescription("Play a pass for "
                                    + toMove.getCapitalizedName());
    }

    private void updatePlay(GoColor toMove, boolean isProgramAttached,
                            boolean computerBoth, String name)
    {
        m_actionPlay.setEnabled(isProgramAttached);
        String desc;
        if (name == null)
            name = "computer";
        if (computerBoth)
            desc = "Continue play (" + name + " both)";
        else
        {
            desc = "Make " + name + " play " + toMove.getCapitalizedName();
            if (! isProgramAttached)
                desc = desc + " (no program attached)";
        }
        m_actionPlay.setDescription(desc);
    }

    private void updateSave(File file, boolean isModified)
    {
        String desc = "Save";
        if (file != null)
        {
            desc = desc + " (" + file + ")";
            if (! isModified)
                desc = desc + " (not modified)";
        }
        m_actionSave.setDescription(desc);
    }
}
