// GoGuiActions.java

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
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
import net.sf.gogui.gui.GuiAction;
import net.sf.gogui.util.Platform;

/** Actions used in the GoGui tool bar and menu bar.
    This class has a cyclic dependency with class GoGui, however the
    dependency has a simple structure. The class contains actions that wrap a
    call to public functions of GoGui. There are also update functions that
    are used to enable actions or add additional information to their
    descriptions depending on the state of GoGui as far as it is accessible
    through public functions. */
public class GoGuiActions
{
    public final GuiAction m_actionAbout =
        new GuiAction(i18n("ACT_ABOUT")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAbout(); } };

    public final GuiAction m_actionAddBookmark =
        new GuiAction(i18n("ACT_ADD_BOOKMARK"), null, KeyEvent.VK_B) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAddBookmark(); } };

    public final GuiAction m_actionBackToMainVariation =
        new GuiAction(i18n("ACT_BACK_TO_MAIN_VARIATION"), null,
                   KeyEvent.VK_M) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackToMainVariation(); } };

    public final GuiAction m_actionBackward =
        new GuiAction(i18n("ACT_BACKWARD"), i18n("TT_BACKWARD"),
                   KeyEvent.VK_LEFT, "gogui-previous") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(1); } };

    public final GuiAction m_actionBackwardTen =
        new GuiAction(i18n("ACT_BACKWARD_TEN"), i18n("TT_BACKWARD_TEN"),
                   KeyEvent.VK_LEFT,
                   SHORTCUT | ActionEvent.SHIFT_MASK, "gogui-previous-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(10); } };

    public final GuiAction m_actionBeginning =
        new GuiAction(i18n("ACT_BEGINNING"), i18n("TT_BEGINNING"),
                   KeyEvent.VK_HOME, "gogui-first") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBeginning(); } };

    public final GuiAction m_actionBoardSize9 =
        new GuiAction(i18n("ACT_BOARDSIZE_9")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(9); } };

    public final GuiAction m_actionBoardSize11 =
        new GuiAction(i18n("ACT_BOARDSIZE_11")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(11); } };

    public final GuiAction m_actionBoardSize13 =
        new GuiAction(i18n("ACT_BOARDSIZE_13")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(13); } };

    public final GuiAction m_actionBoardSize15 =
        new GuiAction(i18n("ACT_BOARDSIZE_15")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(15); } };

    public final GuiAction m_actionBoardSize17 =
        new GuiAction(i18n("ACT_BOARDSIZE_17")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(17); } };

    public final GuiAction m_actionBoardSize19 =
        new GuiAction(i18n("ACT_BOARDSIZE_19")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(19); } };

    public final GuiAction m_actionBoardSizeOther =
        new GuiAction(i18n("ACT_BOARDSIZE_OTHER")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSizeOther(); } };

    public final GuiAction m_actionClockHalt =
        new GuiAction(i18n("ACT_CLOCK_HALT")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockHalt(); } };

    public final GuiAction m_actionClockResume =
        new GuiAction(i18n("ACT_CLOCK_RESUME")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockResume(); } };

    public final GuiAction m_actionClockStart =
        new GuiAction(i18n("ACT_CLOCK_START")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockStart(); } };

    public final GuiAction m_actionComputerBlack =
        new GuiAction(i18n("ACT_COMPUTER_BLACK")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, false); } };

    public final GuiAction m_actionComputerBoth =
        new GuiAction(i18n("ACT_COMPUTER_BOTH")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, true); } };

    public final GuiAction m_actionComputerNone =
        new GuiAction(i18n("ACT_COMPUTER_NONE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, false); } };

    public final GuiAction m_actionComputerWhite =
        new GuiAction(i18n("ACT_COMPUTER_WHITE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, true); } };

    public final GuiAction m_actionEditBookmarks =
        new GuiAction(i18n("ACT_EDIT_BOOKMARKS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditBookmarks(); } };

    public final GuiAction m_actionEditPrograms =
        new GuiAction(i18n("ACT_EDIT_PROGRAMS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditPrograms(); } };

    public final GuiAction m_actionGotoMove =
        new GuiAction(i18n("ACT_GOTO_MOVE"), null, KeyEvent.VK_G) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoMove(); } };

    public final GuiAction m_actionGotoVariation =
        new GuiAction(i18n("ACT_GOTO_VARIATION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoVariation(); } };

    public final GuiAction m_actionDeleteSideVariations =
        new GuiAction(i18n("ACT_DELETE_SIDE_VARIATIONS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDeleteSideVariations(); } };

    public final GuiAction m_actionDetachProgram =
        new GuiAction(i18n("ACT_DETACH_PROGRAM")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDetachProgram(); } };

    public final GuiAction m_actionEnd =
        new GuiAction(i18n("ACT_END"), i18n("TT_END"),
                   KeyEvent.VK_END, "gogui-last") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEnd(); } };

    public final GuiAction m_actionExportSgfPosition =
        new GuiAction(i18n("ACT_EXPORT_SGF_POSITION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportSgfPosition(); } };

    public final GuiAction m_actionExportLatexMainVariation =
        new GuiAction(i18n("ACT_EXPORT_LATEX_MAIN_VARIATION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexMainVariation(); } };

    public final GuiAction m_actionExportLatexPosition =
        new GuiAction(i18n("ACT_EXPORT_LATEX_POSITION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexPosition(); } };

    public final GuiAction m_actionExportPng =
        new GuiAction(i18n("ACT_EXPORT_PNG")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportPng(); } };

    public final GuiAction m_actionExportTextPosition =
        new GuiAction(i18n("ACT_EXPORT_TEXT_POSITION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPosition(); } };

    public final GuiAction m_actionExportTextPositionToClipboard =
        new GuiAction(i18n("ACT_EXPORT_TEXT_POSITION_TO_CLIPBOARD")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPositionToClipboard(); } };

    public final GuiAction m_actionFind =
        new GuiAction(i18n("ACT_FIND"), null, KeyEvent.VK_F) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFind(); } };

    public final GuiAction m_actionFindNext =
        new GuiAction(i18n("ACT_FIND_NEXT"), null, KeyEvent.VK_F3,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFindNext(); } };

    public final GuiAction m_actionFindNextComment =
        new GuiAction(i18n("ACT_FIND_NEXT_COMMENT"), null, KeyEvent.VK_F4,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFindNextComment(); } };

    public final GuiAction m_actionForward =
        new GuiAction(i18n("ACT_FORWARD"), i18n("TT_FORWARD"),
                      KeyEvent.VK_RIGHT, "gogui-next") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(1); } };

    public final GuiAction m_actionForwardTen =
        new GuiAction(i18n("ACT_FORWARD_TEN"), i18n("TT_FORWARD_TEN"),
                      KeyEvent.VK_RIGHT, SHORTCUT | ActionEvent.SHIFT_MASK,
                      "gogui-next-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(10); } };

    public final GuiAction m_actionGameInfo =
        new GuiAction(i18n("ACT_GAME_INFO"), null, KeyEvent.VK_I) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGameInfo(); } };

    public final GuiAction m_actionHandicapNone =
        new GuiAction(i18n("ACT_HANDICAP_NONE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(0); } };

    public final GuiAction m_actionHandicap2 =
        new GuiAction(i18n("ACT_HANDICAP_2")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(2); } };

    public final GuiAction m_actionHandicap3 =
        new GuiAction(i18n("ACT_HANDICAP_3")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(3); } };

    public final GuiAction m_actionHandicap4 =
        new GuiAction(i18n("ACT_HANDICAP_4")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(4); } };

    public final GuiAction m_actionHandicap5 =
        new GuiAction(i18n("ACT_HANDICAP_5")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(5); } };

    public final GuiAction m_actionHandicap6 =
        new GuiAction(i18n("ACT_HANDICAP_6")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(6); } };

    public final GuiAction m_actionHandicap7 =
        new GuiAction(i18n("ACT_HANDICAP_7")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(7); } };

    public final GuiAction m_actionHandicap8 =
        new GuiAction(i18n("ACT_HANDICAP_8")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(8); } };

    public final GuiAction m_actionHandicap9 =
        new GuiAction(i18n("ACT_HANDICAP_9")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(9); } };

    public final GuiAction m_actionHelp =
        new GuiAction(i18n("ACT_HELP"), null, KeyEvent.VK_F1,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHelp(); } };

    public final GuiAction m_actionImportTextPosition =
        new GuiAction(i18n("ACT_IMPORT_TEXT_POSITION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPosition(); } };

    public final GuiAction m_actionImportTextPositionFromClipboard =
        new GuiAction(i18n("ACT_IMPORT_TEXT_POSITION_FROM_CLIPBOARD")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPositionFromClipboard(); } };

    public final GuiAction m_actionImportSgfFromClipboard =
        new GuiAction(i18n("ACT_IMPORT_SGF_FROM_CLIPBOARD")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportSgfFromClipboard(); } };

    public final GuiAction m_actionInterrupt =
        (Platform.isMac() ?
         /* Don't use escape shortcut on Mac, produces a wrong
            shortcut label in the menu. Tested with Java 1.5.0_13 */
         new GuiAction(i18n("ACT_INTERRUPT")) {
             public void actionPerformed(ActionEvent e) {
                 m_goGui.actionInterrupt(); } }
         : new GuiAction(i18n("ACT_INTERRUPT"), null,
                      KeyEvent.VK_ESCAPE, 0) {
                 public void actionPerformed(ActionEvent e) {
                     m_goGui.actionInterrupt(); } });

    public final GuiAction m_actionKeepOnlyPosition =
        new GuiAction(i18n("ACT_KEEP_ONLY_POSITION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionKeepOnlyPosition(); } };

    public final GuiAction m_actionMainWindowActivate =
        new GuiAction(i18n("ACT_MAIN_WINDOW_ACTIVATE"), null,
                   KeyEvent.VK_F6, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMainWindowActivate(); } };

    public final GuiAction m_actionMakeMainVariation =
        new GuiAction(i18n("ACT_MAKE_MAIN_VARIATION")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMakeMainVariation(); } };

    public final GuiAction m_actionNextEarlierVariation =
        new GuiAction(i18n("ACT_NEXT_EARLIER_VARIATION"), null,
                   KeyEvent.VK_DOWN, SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextEarlierVariation(); } };

    public final GuiAction m_actionNextVariation =
        new GuiAction(i18n("ACT_NEXT_VARIATION"), i18n("TT_NEXT_VARIATION"),
                      KeyEvent.VK_DOWN, "gogui-down") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextVariation(); } };

    public final GuiAction m_actionNewGame =
        new GuiAction(i18n("ACT_NEW_GAME"), i18n("TT_NEW_GAME"),
                      "gogui-newgame") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewGame(); } };

    public final GuiAction m_actionNewProgram =
        new GuiAction(i18n("ACT_NEW_PROGRAM")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewProgram(); } };

    public final GuiAction m_actionOpen =
        new GuiAction(i18n("ACT_OPEN"), i18n("TT_OPEN"), KeyEvent.VK_O,
                      "document-open") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionOpen(); } };

    public final GuiAction m_actionPass =
        new GuiAction(i18n("ACT_PASS"), i18n("TT_PASS"),
                      KeyEvent.VK_F2, FUNCTION_KEY, "gogui-pass") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPass(); } };

    public final GuiAction m_actionPlay =
        new GuiAction(i18n("ACT_PLAY"), i18n("TT_PLAY"),
                      KeyEvent.VK_F5, FUNCTION_KEY, "gogui-play") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(false); } };

    public final GuiAction m_actionPlaySingleMove =
        new GuiAction(i18n("ACT_PLAY_SINGLE_MOVE"), null, KeyEvent.VK_F5,
                   FUNCTION_KEY | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(true); } };

    public final GuiAction m_actionPreviousEarlierVariation =
        new GuiAction(i18n("ACT_PREVIOUS_EARLIER_VARIATION"), null,
                   KeyEvent.VK_UP, SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousEarlierVariation(); } };

    public final GuiAction m_actionPreviousVariation =
        new GuiAction(i18n("ACT_PREVIOUS_VARIATION"),
                      i18n("TT_PREVIOUS_VARIATION"),
                      KeyEvent.VK_UP, "gogui-up") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousVariation(); } };

    public final GuiAction m_actionPrint =
        new GuiAction(i18n("ACT_PRINT"), null, KeyEvent.VK_P, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPrint(); } };

    public final GuiAction m_actionReattachProgram =
        new GuiAction(i18n("ACT_REATTACH_PROGRAM"), null, KeyEvent.VK_T) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachProgram(); } };

    public final GuiAction m_actionReattachWithParameters =
        new GuiAction(i18n("ACT_REATTACH_WITH_PARAMETERS"), null,
                      KeyEvent.VK_T, SHORTCUT | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachWithParameters(); } };

    public final GuiAction m_actionRestoreParameters =
        new GuiAction(i18n("ACT_RESTORE_PARAMETERS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionRestoreParameters(); } };

    public final GuiAction m_actionSave =
        new GuiAction(i18n("ACT_SAVE"), i18n("TT_SAVE"), KeyEvent.VK_S,
                      "document-save") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSave(); } };

    public final GuiAction m_actionSaveAs =
        new GuiAction(i18n("ACT_SAVE_AS"), i18n("TT_SAVE_AS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveAs(); } };

    public final GuiAction m_actionSaveCommands =
        new GuiAction(i18n("ACT_SAVE_COMMANDS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveCommands(); } };

    public final GuiAction m_actionSaveLog =
        new GuiAction(i18n("ACT_SAVE_LOG")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveLog(); } };

    public final GuiAction m_actionSaveParameters =
        new GuiAction(i18n("ACT_SAVE_PARAMETERS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveParameters(); } };

    public final GuiAction m_actionScore =
        new GuiAction(i18n("ACT_SCORE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionScore(); } };

    public final GuiAction m_actionSendFile =
        new GuiAction(i18n("ACT_SEND_FILE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSendFile(); } };

    public final GuiAction m_actionSetTimeLeft =
        new GuiAction(i18n("ACT_SET_TIME_LEFT"), null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetTimeLeft(); } };

    public final GuiAction m_actionSetupBlack =
        new GuiAction(i18n("ACT_SETUP_BLACK"), i18n("TT_SETUP_BLACK"),
                      "gogui-setup-black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(BLACK); } };

    public final GuiAction m_actionSetupWhite =
        new GuiAction(i18n("ACT_SETUP_WHITE"), i18n("TT_SETUP_WHITE"),
                      "gogui-setup-white") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(WHITE); } };

    public final GuiAction m_actionShowAnalyzeDialog =
        new GuiAction(i18n("ACT_ANALYZE_COMMANDS"), null, KeyEvent.VK_F8,
                   FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowAnalyzeDialog(); } };

    public final GuiAction m_actionShowShell =
        new GuiAction(i18n("ACT_GTP_SHELL"), null, KeyEvent.VK_F9, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowShell(); } };

    public final GuiAction m_actionShowTree =
        new GuiAction(i18n("ACT_TREE_VIEWER"), null, KeyEvent.VK_F7, FUNCTION_KEY) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowTree(); } };

    public final GuiAction m_actionShowVariationsChildren =
        new GuiAction(i18n("ACT_VARIATION_LABELS_CHILDREN")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetShowVariations(
                                           GoGui.ShowVariations.CHILDREN); } };

    public final GuiAction m_actionShowVariationsSiblings =
        new GuiAction(i18n("ACT_VARIATION_LABELS_SIBLINGS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetShowVariations(
                                           GoGui.ShowVariations.SIBLINGS); } };

    public final GuiAction m_actionShowVariationsNone =
        new GuiAction(i18n("ACT_VARIATION_LABELS_NONE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetShowVariations(
                                           GoGui.ShowVariations.NONE); } };

    public final GuiAction m_actionSnapshotParameters =
        new GuiAction(i18n("ACT_SNAPSHOT_PARAMETERS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSnapshotParameters(); } };

    public final GuiAction m_actionToggleAutoNumber =
        new GuiAction(i18n("ACT_AUTO_NUMBER")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleAutoNumber(); } };

    public final GuiAction m_actionToggleBeepAfterMove =
        new GuiAction(i18n("ACT_PLAY_SOUND")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleBeepAfterMove(); } };

    public final GuiAction m_actionToggleCompletion =
        new GuiAction(i18n("ACT_POPUP_COMPLETIONS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCompletion(); } };

    public final GuiAction m_actionToggleCommentMonoFont =
        new GuiAction(i18n("ACT_MONOSPACE_COMMENT_FONT")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCommentMonoFont(); } };

    public final GuiAction m_actionToggleShowCursor =
        new GuiAction(i18n("ACT_CURSOR")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowCursor(); } };

    public final GuiAction m_actionToggleShowGrid =
        new GuiAction(i18n("ACT_GRID_LABELS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowGrid(); } };

    public final GuiAction m_actionToggleShowInfoPanel =
        new GuiAction(i18n("ACT_INFO_PANEL")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowInfoPanel(); } };

    public final GuiAction m_actionToggleShowLastMove =
        new GuiAction(i18n("ACT_LAST_MOVE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowLastMove(); } };

    public final GuiAction m_actionToggleShowMoveNumbers =
        new GuiAction(i18n("ACT_MOVE_NUMBERS")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowMoveNumbers(); } };

    public final GuiAction m_actionToggleShowSubtreeSizes =
        new GuiAction(i18n("ACT_SUBTREE_SIZES")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowSubtreeSizes(); } };

    public final GuiAction m_actionToggleShowToolbar =
        new GuiAction(i18n("ACT_TOOLBAR")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowToolbar(); } };

    public final GuiAction m_actionToggleTimeStamp =
        new GuiAction(i18n("ACT_TIMESTAMP")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleTimeStamp(); } };

    public final GuiAction m_actionTreeLabelsNumber =
        new GuiAction(i18n("ACT_TREE_LABELS_MOVE_NUMBER")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.Label.NUMBER); } };

    public final GuiAction m_actionTreeLabelsMove =
        new GuiAction(i18n("ACT_TREE_LABELS_MOVE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.Label.MOVE); } };

    public final GuiAction m_actionTreeLabelsNone =
        new GuiAction(i18n("ACT_TREE_LABELS_NONE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.Label.NONE); } };

    public final GuiAction m_actionTreeSizeLarge =
        new GuiAction(i18n("ACT_TREE_SIZE_LARGE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.Size.LARGE); } };

    public final GuiAction m_actionTreeSizeNormal =
        new GuiAction(i18n("ACT_TREE_SIZE_NORMAL")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.Size.NORMAL); } };

    public final GuiAction m_actionTreeSizeSmall =
        new GuiAction(i18n("ACT_TREE_SIZE_SMALL")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.Size.SMALL); } };

    public final GuiAction m_actionTreeSizeTiny =
        new GuiAction(i18n("ACT_TREE_SIZE_TINY")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.Size.TINY); } };

    public final GuiAction m_actionTruncate =
        new GuiAction(i18n("ACT_TRUNCATE")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncate(); } };

    public final GuiAction m_actionTruncateChildren =
        new GuiAction(i18n("ACT_TRUNCATE_CHILDREN")) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncateChildren(); } };

    public final GuiAction m_actionQuit =
        new GuiAction(i18n("ACT_QUIT"), null, KeyEvent.VK_Q, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionQuit(); } };

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
        boolean isProgramDead = m_goGui.isProgramDead();
        boolean isInterruptSupported = m_goGui.isInterruptSupported();
        boolean computerBlack = m_goGui.isComputerColor(BLACK);
        boolean computerWhite = m_goGui.isComputerColor(WHITE);
        boolean computerBoth = (computerBlack && computerWhite);
        boolean hasPattern = (m_goGui.getPattern() != null);
        boolean hasParameterSnapshot = m_goGui.hasParameterSnapshot();
        int numberPrograms = m_goGui.getNumberPrograms();
        boolean hasParameterCommands = m_goGui.hasParameterCommands();
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
        updateClockStart(clock);
        updateSetTimeLeft(clock);
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
        m_actionReattachWithParameters.setEnabled(isProgramAttached
                                                  && hasParameterCommands
                                                  && (! isProgramDead
                                                     || hasParameterSnapshot));
        m_actionSnapshotParameters.setEnabled(isProgramAttached
                                              && ! isProgramDead
                                              && hasParameterCommands);
        m_actionRestoreParameters.setEnabled(isProgramAttached
                                            && ! isProgramDead
                                            && hasParameterCommands
                                            && hasParameterSnapshot);
        updateSave(file, isModified);
        m_actionSetupBlack.setSelected(setupMode
                                       && setupColor == BLACK);
        m_actionSetupWhite.setSelected(setupMode
                                       && setupColor == WHITE);
        m_actionSaveCommands.setEnabled(isProgramAttached);
        m_actionSaveLog.setEnabled(isProgramAttached);
        m_actionSaveParameters.setEnabled(isProgramAttached
                                          && ! isProgramDead
                                          && hasParameterCommands);
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
        m_actionToggleShowMoveNumbers.setSelected(m_goGui.getShowMoveNumbers());
        boolean showSubtreeSizes = m_goGui.getShowSubtreeSizes();
        m_actionToggleShowSubtreeSizes.setSelected(showSubtreeSizes);
        m_actionToggleShowToolbar.setSelected(m_goGui.isToolbarShown());
        m_actionShowVariationsChildren.setSelected(
                 m_goGui.getShowVariations() == GoGui.ShowVariations.CHILDREN);
        m_actionShowVariationsSiblings.setSelected(
                 m_goGui.getShowVariations() == GoGui.ShowVariations.SIBLINGS);
        m_actionShowVariationsNone.setSelected(
                 m_goGui.getShowVariations() == GoGui.ShowVariations.NONE);
        m_actionToggleTimeStamp.setSelected(m_goGui.getTimeStamp());
        m_actionTreeLabelsNumber.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.Label.NUMBER);
        m_actionTreeLabelsMove.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.Label.MOVE);
        m_actionTreeLabelsNone.setSelected(
                    m_goGui.getTreeLabels() == GameTreePanel.Label.NONE);
        m_actionTreeSizeLarge.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.Size.LARGE);
        m_actionTreeSizeNormal.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.Size.NORMAL);
        m_actionTreeSizeSmall.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.Size.SMALL);
        m_actionTreeSizeTiny.setSelected(
                    m_goGui.getTreeSize() == GameTreePanel.Size.TINY);
        m_actionTruncate.setEnabled(hasFather);
        m_actionTruncateChildren.setEnabled(hasChildren);
    }

    private final GoGui m_goGui;

    private static final int SHORTCUT
        = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /** Shortcut modifier for function keys.
        0, unless platform is Mac. */
    private static final int FUNCTION_KEY = (Platform.isMac() ? SHORTCUT : 0);

    private void updateClockResume(ConstClock clock)
    {
        boolean enabled = false;
        String desc = null;
        if (! clock.isRunning() && clock.getToMove() != null)
        {
            m_actionClockResume.setEnabled(true);
            m_actionClockResume.setDescription(i18n("TT_CLOCK_RESUME"),
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
            m_actionClockStart.setDescription(i18n("TT_CLOCK_START"),
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
                    m_actionInterrupt.setDescription(
                                     i18n("TT_INTERRUPT_UNSUPPORTED_UNKNOWN"));
                else
                    m_actionInterrupt.setDescription(
                                         i18n("TT_INTERRUPT_UNSUPPORTED_NAME"),
                                         name);
            }
            else if (! isCommandInProgress)
            {
                if (name == null)
                    m_actionInterrupt.setDescription(
                                       i18n("TT_INTERRUPT_NOCOMMAND_UNKNOWN"));
                else
                    m_actionInterrupt.setDescription(
                                           i18n("TT_INTERRUPT_NOCOMMAND_NAME"),
                                           name);
            }
            else
            {
                if (name == null)
                    m_actionInterrupt.setDescription(
                                                 i18n("TT_INTERRUPT_UNKNOWN"));
                else
                    m_actionInterrupt.setDescription(i18n("TT_INTERRUPT_NAME"),
                                                     name);
            }
        }
        else
            m_actionInterrupt.setDescription(i18n("TT_INTERRUPT_NOPROGRAM"));
        m_actionInterrupt.setEnabled(isProgramAttached
                                     && isInterruptSupported);
    }

    private void updatePass(GoColor toMove)
    {
        assert toMove.isBlackWhite();
        if (toMove == BLACK)
            m_actionPass.setDescription(i18n("TT_PASS_BLACK"));
        else
            m_actionPass.setDescription(i18n("TT_PASS_WHITE"));
    }

    private void updatePlay(GoColor toMove, boolean isProgramAttached,
                            boolean computerBoth, String name)
    {
        m_actionPlay.setEnabled(isProgramAttached);
        if (! isProgramAttached)
            m_actionPlay.setDescription(i18n("TT_PLAY_NOPROGRAM"));
        else if (computerBoth)
        {
            if (name == null)
                m_actionPlay.setDescription(i18n("TT_PLAY_BOTH_UNKNOWN"));
            else
                m_actionPlay.setDescription(i18n("TT_PLAY_BOTH_NAME"), name);
        }
        else
        {
            if (name == null)
            {
                if (toMove == BLACK)
                    m_actionPlay.setDescription(i18n("TT_PLAY_BLACK_UNKNOWN"));
                else
                    m_actionPlay.setDescription(i18n("TT_PLAY_WHITE_UNKNOWN"));
            }
            else
            {
                if (toMove == BLACK)
                    m_actionPlay.setDescription(i18n("TT_PLAY_BLACK_NAME"),
                                                name);
                else
                    m_actionPlay.setDescription(i18n("TT_PLAY_WHITE_NAME"),
                                                name);
            }
        }
    }

    private void updateSave(File file, boolean isModified)
    {
        m_actionSave.setEnabled(isModified);
        m_actionSaveAs.setEnabled(file != null);
        if (file == null)
            m_actionSave.setDescription(i18n("TT_SAVE"));
        else
        {
            if (isModified)
                m_actionSave.setDescription(i18n("TT_SAVE_FILE"), file);
            else
                m_actionSave.setDescription(i18n("TT_SAVE_FILE_NOTMODIFIED"),
                                            file);
        }
    }

    private void updateSetTimeLeft(ConstClock clock)
    {
        m_actionSetTimeLeft.setEnabled(clock.isInitialized());
    }
}
