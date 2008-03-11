// GoGuiActions.java

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import static net.sf.gogui.gogui.I18n.i18n;
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
            putValue(AbstractAction.NAME, i18n(name));
            if (desc != null)
                putValue(AbstractAction.SHORT_DESCRIPTION,
                         i18n(desc));
            if (accel != null)
                putValue(AbstractAction.ACCELERATOR_KEY,
                         getKeyStroke(accel.intValue(), modifier));
            if (icon != null)
                putValue(AbstractAction.SMALL_ICON,
                         GuiUtil.getIcon(icon, i18n(name)));
            m_allActions.add(this);
        }

        public final void setDescription(String desc)
        {
            if (desc == null)
                putValue(AbstractAction.SHORT_DESCRIPTION, null);
            else
                putValue(AbstractAction.SHORT_DESCRIPTION, i18n(desc));
        }

        public void setDescription(String desc, Object... args)
        {
            putValue(AbstractAction.SHORT_DESCRIPTION,
                     MessageFormat.format(i18n(desc), args));
        }

        public void setName(String name)
        {
            putValue(AbstractAction.NAME, i18n(name));
        }

        public void setName(String name, Object... args)
        {
            putValue(AbstractAction.NAME,
                     MessageFormat.format(i18n(name), args));
        }

        public void setSelected(boolean selected)
        {
            putValue("selected", Boolean.valueOf(selected));
        }
    }

    public final ArrayList<Action> m_allActions
        = new ArrayList<Action>();

    public final Action m_actionAbout =
        new Action("ACTION_ABOUT") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAbout(); } };

    public final Action m_actionAddBookmark =
        new Action("ACTION_ADD_BOOKMARK", null, KeyEvent.VK_B) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAddBookmark(); } };

    public final Action m_actionBackToMainVariation =
        new Action("ACTION_BACK_TO_MAIN_VARIATION", null,
                   KeyEvent.VK_M) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackToMainVariation(); } };

    public final Action m_actionBackward =
        new Action("ACTION_BACKWARD", "TOOLTIP_BACKWARD",
                   KeyEvent.VK_LEFT, "gogui-previous") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(1); } };

    public final Action m_actionBackwardTen =
        new Action("ACTION_BACKWARD_TEN", "TOOLTIP_BACKWARD_TEN",
                   KeyEvent.VK_LEFT,
                   SHORTCUT | ActionEvent.SHIFT_MASK, "gogui-previous-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(10); } };

    public final Action m_actionBeginning =
        new Action("ACTION_BEGINNING", "TOOLTIP_BEGINNING",
                   KeyEvent.VK_HOME, "gogui-first") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBeginning(); } };

    public final Action m_actionBoardSize9 =
        new Action("ACTION_BOARDSIZE_9") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(9); } };

    public final Action m_actionBoardSize11 =
        new Action("ACTION_BOARDSIZE_11") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(11); } };

    public final Action m_actionBoardSize13 =
        new Action("ACTION_BOARDSIZE_13") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(13); } };

    public final Action m_actionBoardSize15 =
        new Action("ACTION_BOARDSIZE_15") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(15); } };

    public final Action m_actionBoardSize17 =
        new Action("ACTION_BOARDSIZE_17") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(17); } };

    public final Action m_actionBoardSize19 =
        new Action("ACTION_BOARDSIZE_19") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(19); } };

    public final Action m_actionBoardSizeOther =
        new Action("ACTION_BOARDSIZE_OTHER") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSizeOther(); } };

    public final Action m_actionClockHalt =
        new Action("ACTION_CLOCK_HALT") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockHalt(); } };

    public final Action m_actionClockResume =
        new Action("ACTION_CLOCK_RESUME") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockResume(); } };

    public final Action m_actionClockRestore =
        new Action("ACTION_CLOCK_RESTORE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockRestore(); } };

    public final Action m_actionClockStart =
        new Action("ACTION_CLOCK_START") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockStart(); } };

    public final Action m_actionComputerBlack =
        new Action("ACTION_COMPUTER_BLACK") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, false); } };

    public final Action m_actionComputerBoth =
        new Action("ACTION_COMPUTER_BOTH") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, true); } };

    public final Action m_actionComputerNone =
        new Action("ACTION_COMPUTER_NONE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, false); } };

    public final Action m_actionComputerWhite =
        new Action("ACTION_COMPUTER_WHITE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, true); } };

    public final Action m_actionEditBookmarks =
        new Action("ACTION_EDIT_BOOKMARKS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditBookmarks(); } };

    public final Action m_actionEditPrograms =
        new Action("ACTION_EDIT_PROGRAMS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditPrograms(); } };

    public final Action m_actionGotoMove =
        new Action("ACTION_GOTO_MOVE", null, KeyEvent.VK_G) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoMove(); } };

    public final Action m_actionGotoVariation =
        new Action("ACTION_GOTO_VARIATION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoVariation(); } };

    public final Action m_actionDeleteSideVariations =
        new Action("ACTION_DELETE_SIDE_VARIATIONS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDeleteSideVariations(); } };

    public final Action m_actionDetachProgram =
        new Action("ACTION_DETACH_PROGRAM")
        {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDetachProgram(); } };

    public final Action m_actionEnd =
        new Action("ACTION_END", "TOOLTIP_END",
                   KeyEvent.VK_END, "gogui-last") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEnd(); } };

    public final Action m_actionExportSgfPosition =
        new Action("ACTION_EXPORT_SGF_POSITION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportSgfPosition(); } };

    public final Action m_actionExportLatexMainVariation =
        new Action("ACTION_EXPORT_LATEX_MAIN_VARIATION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexMainVariation(); } };

    public final Action m_actionExportLatexPosition =
        new Action("ACTION_EXPORT_LATEX_POSITION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexPosition(); } };

    public final Action m_actionExportPng =
        new Action("ACTION_EXPORT_PNG") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportPng(); } };

    public final Action m_actionExportTextPosition =
        new Action("ACTION_EXPORT_TEXT_POSITION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPosition(); } };

    public final Action m_actionExportTextPositionToClipboard =
        new Action("ACTION_EXPORT_TEXT_POSITION_TO_CLIPBOARD") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPositionToClipboard(); } };

    public final Action m_actionFind =
        new Action("ACTION_FIND", null, KeyEvent.VK_F) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFind(); } };

    public final Action m_actionFindNext =
        new Action("ACTION_FIND_NEXT", null, KeyEvent.VK_F3,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFindNext(); } };

    public final Action m_actionForward =
        new Action("ACTION_FORWARD", "TOOLTIP_FORWARD",
                   KeyEvent.VK_RIGHT, "gogui-next") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(1); } };

    public final Action m_actionForwardTen =
        new Action("ACTION_FORWARD_TEN", "TOOLTIP_FORWARD_TEN",
                   KeyEvent.VK_RIGHT, SHORTCUT | ActionEvent.SHIFT_MASK,
                   "gogui-next-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(10); } };

    public final Action m_actionGameInfo =
        new Action("ACTION_GAME_INFO", null, KeyEvent.VK_I) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGameInfo(); } };

    public final Action m_actionHandicapNone =
        new Action("ACTION_HANDICAP_NONE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(0); } };

    public final Action m_actionHandicap2 =
        new Action("ACTION_HANDICAP_2") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(2); } };

    public final Action m_actionHandicap3 =
        new Action("ACTION_HANDICAP_3") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(3); } };

    public final Action m_actionHandicap4 =
        new Action("ACTION_HANDICAP_4") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(4); } };

    public final Action m_actionHandicap5 =
        new Action("ACTION_HANDICAP_5") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(5); } };

    public final Action m_actionHandicap6 =
        new Action("ACTION_HANDICAP_6") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(6); } };

    public final Action m_actionHandicap7 =
        new Action("ACTION_HANDICAP_7") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(7); } };

    public final Action m_actionHandicap8 =
        new Action("ACTION_HANDICAP_8") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(8); } };

    public final Action m_actionHandicap9 =
        new Action("ACTION_HANDICAP_9") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(9); } };

    public final Action m_actionHelp =
        new Action("ACTION_HELP", null, KeyEvent.VK_F1,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHelp(); } };

    public final Action m_actionImportTextPosition =
        new Action("ACTION_IMPORT_TEXT_POSITION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPosition(); } };

    public final Action m_actionImportTextPositionFromClipboard =
        new Action("ACTION_IMPORT_TEXT_POSITION_FROM_CLIPBOARD") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPositionFromClipboard(); } };

    public final Action m_actionInterrupt =
        new Action("ACTION_INTERRUPT", null, KeyEvent.VK_ESCAPE,
                   (Platform.isMac() ? SHORTCUT : 0), "gogui-interrupt") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionInterrupt(); } };

    public final Action m_actionKeepOnlyPosition =
        new Action("ACTION_KEEP_ONLY_POSITION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionKeepOnlyPosition(); } };

    public final Action m_actionMainWindowActivate =
        new Action("ACTION_MAIN_WINDOW_ACTIVATE", null,
                   KeyEvent.VK_F6, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMainWindowActivate(); } };

    public final Action m_actionMakeMainVariation =
        new Action("ACTION_MAKE_MAIN_VARIATION") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMakeMainVariation(); } };

    public final Action m_actionNextEarlierVariation =
        new Action("ACTION_NEXT_EARLIER_VARIATION", null,
                   KeyEvent.VK_DOWN, SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextEarlierVariation(); } };

    public final Action m_actionNextVariation =
        new Action("ACTION_NEXT_VARIATION", "TOOLTIP_NEXT_VARIATION",
                   KeyEvent.VK_DOWN, "gogui-down") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextVariation(); } };

    public final Action m_actionNewGame =
        new Action("ACTION_NEW_GAME", "TOOLTIP_NEW_GAME", "gogui-newgame") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewGame(); } };

    public final Action m_actionNewProgram =
        new Action("ACTION_NEW_PROGRAM") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewProgram(); } };

    public final Action m_actionOpen =
        new Action("ACTION_OPEN", "TOOLTIP_OPEN", KeyEvent.VK_O,
                   "document-open") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionOpen(); } };

    public final Action m_actionPass =
        new Action("ACTION_PASS", "TOOLTIP_PASS",
                   KeyEvent.VK_F2, FUNCTION_KEY, "gogui-pass") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPass(); } };

    public final Action m_actionPlay =
        new Action("ACTION_PLAY", "TOOLTIP_PLAY",
                   KeyEvent.VK_F5, FUNCTION_KEY, "gogui-play") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(false); } };

    public final Action m_actionPlaySingleMove =
        new Action("ACTION_PLAY_SINGLE_MOVE", null, KeyEvent.VK_F5,
                   FUNCTION_KEY | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(true); } };

    public final Action m_actionPreviousEarlierVariation =
        new Action("ACTION_PREVIOUS_EARLIER_VARIATION", null,
                   KeyEvent.VK_UP, SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousEarlierVariation(); } };

    public final Action m_actionPreviousVariation =
        new Action("ACTION_PREVIOUS_VARIATION", "TOOLTIP_PREVIOUS_VARIATION",
                   KeyEvent.VK_UP, "gogui-up") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousVariation(); } };

    public final Action m_actionPrint =
        new Action("ACTION_PRINT", null, KeyEvent.VK_P, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPrint(); } };

    public final Action m_actionReattachProgram =
        new Action("ACTION_REATTACH_PROGRAM", null, KeyEvent.VK_T) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachProgram(); } };

    public final Action m_actionReattachWithParameters =
        new Action("ACTION_REATTACH_WITH_PARAMETERS", null, KeyEvent.VK_T,
                   SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachWithParameters(); } };

    public final Action m_actionSave =
        new Action("ACTION_SAVE", "TOOLTIP_SAVE", KeyEvent.VK_S,
                   "document-save") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSave(); } };

    public final Action m_actionSaveAs =
        new Action("ACTION_SAVE_AS", "TOOLTIP_SAVE_AS", "document-save-as") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveAs(); } };

    public final Action m_actionSaveCommands =
        new Action("ACTION_SAVE_COMMANDS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveCommands(); } };

    public final Action m_actionSaveLog =
        new Action("ACTION_SAVE_LOG") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveLog(); } };

    public final Action m_actionSaveParameters =
        new Action("ACTION_SAVE_PARAMETERS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveParameters(); } };

    public final Action m_actionScore =
        new Action("ACTION_SCORE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionScore(); } };

    public final Action m_actionSendFile =
        new Action("ACTION_SEND_FILE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSendFile(); } };

    public final Action m_actionSetupBlack =
        new Action("ACTION_SETUP_BLACK", "TOOLTIP_SETUP_BLACK",
                   "gogui-setup-black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(BLACK); } };

    public final Action m_actionSetupWhite =
        new Action("ACTION_SETUP_WHITE", "TOOLTIP_SETUP_WHITE",
                   "gogui-setup-white") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(WHITE); } };

    public final Action m_actionShowAnalyzeDialog =
        new Action("ACTION_ANALYZE_COMMANDS", null, KeyEvent.VK_F8,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowAnalyzeDialog(); } };

    public final Action m_actionShowShell =
        new Action("ACTION_GTP_SHELL", null, KeyEvent.VK_F9, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowShell(); } };

    public final Action m_actionShowTree =
        new Action("ACTION_TREE_VIEWER", null, KeyEvent.VK_F7, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowTree(); } };

    public final Action m_actionSwitchLanguage =
        new Action("ACTION_SWITCH_LANGUAGE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSwitchLanguage(); } };

    public final Action m_actionToggleAutoNumber =
        new Action("ACTION_AUTO_NUMBER") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleAutoNumber(); } };

    public final Action m_actionToggleBeepAfterMove =
        new Action("ACTION_PLAY_SOUND") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleBeepAfterMove(); } };

    public final Action m_actionToggleCompletion =
        new Action("ACTION_POPUP_COMPLETIONS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCompletion(); } };

    public final Action m_actionToggleCommentMonoFont =
        new Action("ACTION_MONOSPACE_COMMENT_FONT") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCommentMonoFont(); } };

    public final Action m_actionToggleShowCursor =
        new Action("ACTION_CURSOR") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowCursor(); } };

    public final Action m_actionToggleShowGrid =
        new Action("ACTION_GRID_LABELS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowGrid(); } };

    public final Action m_actionToggleShowInfoPanel =
        new Action("ACTION_INFO_PANEL") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowInfoPanel(); } };

    public final Action m_actionToggleShowLastMove =
        new Action("ACTION_LAST_MOVE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowLastMove(); } };

    public final Action m_actionToggleShowSubtreeSizes =
        new Action("ACTION_SUBTREE_SIZES") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowSubtreeSizes(); } };

    public final Action m_actionToggleShowToolbar =
        new Action("ACTION_TOOLBAR") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowToolbar(); } };

    public final Action m_actionToggleShowVariations =
        new Action("ACTION_VARIATION_LABELS") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowVariations(); } };

    public final Action m_actionToggleTimeStamp =
        new Action("ACTION_TIMESTAMP") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleTimeStamp(); } };

    public final Action m_actionTreeLabelsNumber =
        new Action("ACTION_TREE_LABELS_MOVE_NUMBER") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NUMBER); } };

    public final Action m_actionTreeLabelsMove =
        new Action("ACTION_TREE_LABELS_MOVE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_MOVE); } };

    public final Action m_actionTreeLabelsNone =
        new Action("ACTION_TREE_LABELS_NONE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NONE); } };

    public final Action m_actionTreeSizeLarge =
        new Action("ACTION_TREE_SIZE_LARGE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_LARGE); } };

    public final Action m_actionTreeSizeNormal =
        new Action("ACTION_TREE_SIZE_NORMAL") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_NORMAL); } };

    public final Action m_actionTreeSizeSmall =
        new Action("ACTION_TREE_SIZE_SMALL") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_SMALL); } };

    public final Action m_actionTreeSizeTiny =
        new Action("ACTION_TREE_SIZE_TINY") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_TINY); } };

    public final Action m_actionTruncate =
        new Action("ACTION_TRUNCATE") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncate(); } };

    public final Action m_actionTruncateChildren =
        new Action("ACTION_TRUNCATE_CHILDREN") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncateChildren(); } };

    public final Action m_actionQuit =
        new Action("ACTION_QUIT", null, KeyEvent.VK_Q, null) {
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
        m_actionGotoMove.setEnabled(hasFather || hasChildren);
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
        m_actionSaveLog.setEnabled(isProgramAttached);
        m_actionSaveCommands.setEnabled(isProgramAttached);
        m_actionSendFile.setEnabled(isProgramAttached);
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
        if (! clock.isInitialized())
        {
            m_actionClockRestore.setEnabled(false);
            m_actionClockRestore.setDescription(null);
        }
        else
        {
            Clock tempClock = new Clock();
            tempClock.setTimeSettings(clock.getTimeSettings());
            NodeUtil.restoreClock(node, tempClock);
            m_actionClockRestore.setEnabled(true);
            m_actionClockRestore.setDescription("TOOLTIP_CLOCK_RESTORE",
                                                tempClock.getTimeString(BLACK),
                                                tempClock.getTimeString(WHITE));
        }
    }

    private void updateClockResume(ConstClock clock)
    {
        boolean enabled = false;
        String desc = null;
        if (! clock.isRunning() && clock.getToMove() != null)
        {
            m_actionClockResume.setEnabled(true);
            m_actionClockResume.setDescription("TOOLTIP_CLOCK_RESUME",
                                               clock.getTimeString(BLACK),
                                               clock.getTimeString(WHITE));
        }
        else
        {
            m_actionClockResume.setEnabled(false);
            m_actionClockResume.setDescription(null);
        }
    }

    private void updateClockStart(ConstClock clock)
    {
        if (! clock.isRunning() && clock.getToMove() == null)
        {
            m_actionClockStart.setEnabled(true);
            m_actionClockStart.setDescription("TOOLTIP_CLOCK_START",
                                               clock.getTimeString(BLACK),
                                               clock.getTimeString(WHITE));
        }
        else
        {
            m_actionClockStart.setEnabled(false);
            m_actionClockStart.setDescription(null);
        }
    }

    private void updateDetachProgram(boolean isProgramAttached, String name)
    {
        m_actionDetachProgram.setEnabled(isProgramAttached);
        if (! isProgramAttached || name == null)
            m_actionDetachProgram.setName("ACTION_DETACH_PROGRAM");
        else
            m_actionDetachProgram.setName("ACTION_DETACH_PROGRAM_NAME", name);
    }

    private void updateInterrupt(boolean isProgramAttached,
                                 boolean isInterruptSupported,
                                 boolean isCommandInProgress, String name)
    {
        String desc;
        if (isProgramAttached)
        {
            if (! isInterruptSupported)
            {
                if (name == null)
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_UNSUPPORTED_UNKNOWN");
                else
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_UNSUPPORTED_NAME",
                                                     name);
            }
            else if (! isCommandInProgress)
            {
                if (name == null)
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_NOCOMMAND_UNKNOWN");
                else
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_NOCOMMAND_NAME",
                                                     name);
            }
            else
            {
                if (name == null)
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_UNKNOWN");
                else
                    m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_NAME",
                                                     name);
            }
        }
        else
            m_actionInterrupt.setDescription("TOOLTIP_INTERRUPT_NOPROGRAM");
        m_actionInterrupt.setEnabled(isProgramAttached
                                     && isInterruptSupported);
    }

    private void updatePass(GoColor toMove)
    {
        assert toMove.isBlackWhite();
        if (toMove == BLACK)
            m_actionPass.setDescription("TOOLTIP_PASS_BLACK");
        else
            m_actionPass.setDescription("TOOLTIP_PASS_WHITE");
    }

    private void updatePlay(GoColor toMove, boolean isProgramAttached,
                            boolean computerBoth, String name)
    {
        m_actionPlay.setEnabled(isProgramAttached);
        if (! isProgramAttached)
            m_actionPlay.setDescription("TOOLTIP_PLAY_NOPROGRAM");
        else if (computerBoth)
        {
            if (name == null)
                m_actionPlay.setDescription("TOOLTIP_PLAY_BOTH_UNKNOWN");
            else
                m_actionPlay.setDescription("TOOLTIP_PLAY_BOTH_NAME", name);
        }
        else
        {
            if (name == null)
            {
                if (toMove == BLACK)
                    m_actionPlay.setDescription("TOOLTIP_PLAY_BLACK_UNKNOWN");
                else
                    m_actionPlay.setDescription("TOOLTIP_PLAY_WHITE_UNKNOWN");
            }
            else
            {
                if (toMove == BLACK)
                    m_actionPlay.setDescription("TOOLTIP_PLAY_BLACK_NAME",
                                                name);
                else
                    m_actionPlay.setDescription("TOOLTIP_PLAY_WHITE_NAME",
                                                name);
            }
        }
    }

    private void updateSave(File file, boolean isModified)
    {
        if (file == null)
            m_actionSave.setDescription("ACTION_SAVE");
        else
        {
            if (isModified)
                m_actionSave.setDescription("TOOLTIP_SAVE_FILE", file);
            else
                m_actionSave.setDescription("TOOLTIP_SAVE_FILE_NOTMODIFIED",
                                            file);
        }
    }
}
