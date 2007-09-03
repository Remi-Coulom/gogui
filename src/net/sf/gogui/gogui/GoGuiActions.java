//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gogui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
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
    static abstract class GoGuiAction
        extends AbstractAction
    {
        public GoGuiAction(GoGuiActions actions, String name, String desc)
        {
            this(actions, name, desc, null, 0, null);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           String icon)
        {
            this(actions, name, desc, null, 0, icon);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           int accel, String icon)
        {
            this(actions, name, desc, accel, getShortcut(), icon);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           int accel)
        {
            this(actions, name, desc, accel, getShortcut(), null);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           int accel, int modifier, String icon)
        {
            this(actions, name, desc, Integer.valueOf(accel),
                 modifier, icon);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           int accel, int modifier)
        {
            this(actions, name, desc, accel, modifier, null);
        }

        public GoGuiAction(GoGuiActions actions, String name, String desc,
                           Integer accel, int modifier, String icon)
        {
            putValue(AbstractAction.NAME, name);
            setDescription(desc);
            if (accel != null)
            {
                KeyStroke keyStroke = getKeyStroke(accel.intValue(), modifier);
                putValue(AbstractAction.ACCELERATOR_KEY, keyStroke);
            }
            if (icon != null)
                putValue(AbstractAction.SMALL_ICON,
                         GuiUtil.getIcon(icon, name));
            actions.m_allActions.add(this);
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

    public final ArrayList<GoGuiAction> m_allActions
        = new ArrayList<GoGuiAction>();

    public final GoGuiAction m_actionAbout =
        new GoGuiAction(this, "About",
                        "Show information about GoGui, Go program and Java") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAbout(); } };

    public final GoGuiAction m_actionAddBookmark =
        new GoGuiAction(this, "Add Bookmark",
                        "Add bookmark at current position in current file",
                        KeyEvent.VK_B) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionAddBookmark(); } };

    public final GoGuiAction m_actionBackToMainVariation =
        new GoGuiAction(this, "Back to Main Variation",
                        "Go back to main variation", KeyEvent.VK_M) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackToMainVariation(); } };

    public final GoGuiAction m_actionBackward =
        new GoGuiAction(this, "Backward",
                        "Go one move backward", KeyEvent.VK_LEFT,
                        "gogui-previous") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(1); } };

    public final GoGuiAction m_actionBackwardTen =
        new GoGuiAction(this, "Backward 10",
                        "Go ten moves backward",
                        KeyEvent.VK_LEFT,
                        getShortcut() | ActionEvent.SHIFT_MASK,
                        "gogui-previous-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBackward(10); } };

    public final GoGuiAction m_actionBeginning =
        new GoGuiAction(this, "Beginning",
                        "Go to beginning of game", KeyEvent.VK_HOME,
                        "gogui-first") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBeginning(); } };

    public final GoGuiAction m_actionBoardSize9 =
        new GoGuiAction(this, "9", "Change board size to 9x9") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(9); } };

    public final GoGuiAction m_actionBoardSize11 =
        new GoGuiAction(this, "11", "Change board size to 11x11") {
                public void actionPerformed(ActionEvent e) {
                    m_goGui.actionBoardSize(11); } };

    public final GoGuiAction m_actionBoardSize13 =
        new GoGuiAction(this, "13", "Change board size to 13x13") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(13); } };

    public final GoGuiAction m_actionBoardSize15 =
        new GoGuiAction(this,  "15", "Change board size to 15x15") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(15); } };

    public final GoGuiAction m_actionBoardSize17 =
        new GoGuiAction(this, "17", "Change board size to 17x17") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(17); } };

    public final GoGuiAction m_actionBoardSize19 =
        new GoGuiAction(this, "19", "Change board size to 19x19") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSize(19); } };

    public final GoGuiAction m_actionBoardSizeOther =
        new GoGuiAction(this, "Other", "Change board size to other values") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionBoardSizeOther(); } };

    public final GoGuiAction m_actionClockHalt =
        new GoGuiAction(this, "Halt", "Halt clock") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockHalt(); } };

    public final GoGuiAction m_actionClockResume =
        new GoGuiAction(this, "Resume", "Resume clock") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockResume(); } };

    public final GoGuiAction m_actionClockRestore =
        new GoGuiAction(this, "Restore",
                        "Restore clock to time stored at current position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockRestore(); } };

    public final GoGuiAction m_actionClockStart =
        new GoGuiAction(this, "Start", "Start clock") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionClockStart(); } };

    public final GoGuiAction m_actionComputerBlack =
        new GoGuiAction(this, "Black", "Make computer play Black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, false); } };

    public final GoGuiAction m_actionComputerBoth =
        new GoGuiAction(this, "Both", "Make computer play both sides") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(true, true); } };

    public final GoGuiAction m_actionComputerNone =
        new GoGuiAction(this, "None", "Make computer play no side") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, false); } };

    public final GoGuiAction m_actionComputerWhite =
        new GoGuiAction(this, "White", "Make computer play White") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionComputerColor(false, true); } };

    public final GoGuiAction m_actionEditBookmarks =
        new GoGuiAction(this, "Edit Bookmarks...", "Edit list of bookmarks") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditBookmarks(); } };

    public final GoGuiAction m_actionEditPrograms =
        new GoGuiAction(this, "Edit Programs...", "Edit list of programs") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEditPrograms(); } };

    public final GoGuiAction m_actionGoto =
        new GoGuiAction(this, "Go to Move...",
                        "Go to position after a move number", KeyEvent.VK_G) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGoto(); } };

    public final GoGuiAction m_actionGotoVariation =
        new GoGuiAction(this, "Go to Variation...",
                        "Go to beginning of a variation") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGotoVariation(); } };

    public final GoGuiAction m_actionDeleteSideVariations =
        new GoGuiAction(this, "Delete Side Variations",
                        "Delete all variations but the main variation") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDeleteSideVariations(); } };

    public final GoGuiAction m_actionDetachProgram =
        new GoGuiAction(this, "Detach",
                        "Detach Go program from current game and terminate it")
        {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDetachProgram(); } };

    public final GoGuiAction m_actionDocumentation =
        new GoGuiAction(this, "GoGui Help",
                        "Open GoGui manual", KeyEvent.VK_F1,
                        getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionDocumentation(); } };

    public final GoGuiAction m_actionEnd =
        new GoGuiAction(this, "End",
                        "Go to end of game", KeyEvent.VK_END, "gogui-last") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionEnd(); } };

    public final GoGuiAction m_actionExportSgfPosition =
        new GoGuiAction(this, "SGF Position...",
                        "Export position as SGF file") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportSgfPosition(); } };

    public final GoGuiAction m_actionExportLatexMainVariation =
        new GoGuiAction(this, "LaTeX Main Variation...",
                        "Export main variation as LaTeX PSGO file") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexMainVariation(); } };

    public final GoGuiAction m_actionExportLatexPosition =
        new GoGuiAction(this, "LaTeX Position...",
                        "Export position as LaTeX PSGO file") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportLatexPosition(); } };

    public final GoGuiAction m_actionExportTextPosition =
        new GoGuiAction(this, "Text Position...",
                        "Export position as text diagram") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPosition(); } };

    public final GoGuiAction m_actionExportTextPositionToClipboard =
        new GoGuiAction(this, "Text Position to Clipboard",
                        "Export position as text diagram to clipboard") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionExportTextPositionToClipboard(); } };

    public final GoGuiAction m_actionFind =
        new GoGuiAction(this, "Find in Comments...",
                        "Search for matching text in comments",
                        KeyEvent.VK_F) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFind(); } };

    public final GoGuiAction m_actionFindNext =
        new GoGuiAction(this, "Find Next", "Search for next match in comments",
                        KeyEvent.VK_F3, getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionFindNext(); } };

    public final GoGuiAction m_actionForward =
        new GoGuiAction(this, "Forward", "Go one move forward",
                        KeyEvent.VK_RIGHT, "gogui-next") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(1); } };

    public final GoGuiAction m_actionForwardTen =
        new GoGuiAction(this, "Forward 10", "Go ten moves forward",
                        KeyEvent.VK_RIGHT,
                        getShortcut() | ActionEvent.SHIFT_MASK,
                        "gogui-next-10") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionForward(10); } };

    public final GoGuiAction m_actionGameInfo =
        new GoGuiAction(this, "Game Info",
                        "Show and edit game information", KeyEvent.VK_I) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionGameInfo(); } };

    public final GoGuiAction m_actionShellSave =
        new GoGuiAction(this, "Save Log...", "Save GTP history") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSave(); } };

    public final GoGuiAction m_actionShellSaveCommands =
        new GoGuiAction(this, "Save Commands...",
                        "Save history of GTP commands") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSaveCommands(); } };

    public final GoGuiAction m_actionShellSendFile =
        new GoGuiAction(this, "Send File...", "Send file with GTP commands") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShellSendFile(); } };

    public final GoGuiAction m_actionHandicapNone =
        new GoGuiAction(this, "None", "Do not use handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(0); } };

    public final GoGuiAction m_actionHandicap2 =
        new GoGuiAction(this, "2", "Use two handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(2); } };

    public final GoGuiAction m_actionHandicap3 =
        new GoGuiAction(this, "3", "Use three handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(3); } };

    public final GoGuiAction m_actionHandicap4 =
        new GoGuiAction(this, "4", "Use four handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(4); } };

    public final GoGuiAction m_actionHandicap5 =
        new GoGuiAction(this, "5", "Use five handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(5); } };

    public final GoGuiAction m_actionHandicap6 =
        new GoGuiAction(this, "6", "Use six handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(6); } };

    public final GoGuiAction m_actionHandicap7 =
        new GoGuiAction(this, "7", "Use seven handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(7); } };

    public final GoGuiAction m_actionHandicap8 =
        new GoGuiAction(this, "8", "Use eight handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(8); } };

    public final GoGuiAction m_actionHandicap9 =
        new GoGuiAction(this, "9", "Use nine handicap stones") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionHandicap(9); } };

    public final GoGuiAction m_actionImportTextPosition =
        new GoGuiAction(this, "Text Position...",
                        "Import position as text diagram from file") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPosition(); } };
    
    public final GoGuiAction m_actionImportTextPositionFromClipboard =
        new GoGuiAction(this, "Text Position from Clipboard",
                        "Import position as text diagram from clipboard") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionImportTextPositionFromClipboard(); } };

    public final GoGuiAction m_actionInterrupt =
        new GoGuiAction(this, "Interrupt", "Interrupt", "gogui-interrupt") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionInterrupt(); } };

    public final GoGuiAction m_actionKeepOnlyPosition =
        new GoGuiAction(this, "Keep Only Position",
                        "Delete variations and moves and keep only the current position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionKeepOnlyPosition(); } };

    public final GoGuiAction m_actionMainWindowActivate =
        new GoGuiAction(this, "Main Window", "Activate main window",
                        KeyEvent.VK_F6, getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMainWindowActivate(); } };

    public final GoGuiAction m_actionMakeMainVariation =
        new GoGuiAction(this, "Make Main Variation",
                        "Make current variation the main variation") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionMakeMainVariation(); } };

    public final GoGuiAction m_actionNextEarlierVariation =
        new GoGuiAction(this, "Next Earlier Variation",
                        "Go to next earlier variation", KeyEvent.VK_DOWN,
                        getShortcut() | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextEarlierVariation(); } };

    public final GoGuiAction m_actionNextVariation =
        new GoGuiAction(this, "Next Variation", "Go to next variation",
                        KeyEvent.VK_DOWN, "gogui-down") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNextVariation(); } };

    public final GoGuiAction m_actionNewGame =
        new GoGuiAction(this, "New Game",
                        "Clear board and start new game", "gogui-newgame") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewGame(); } };

    public final GoGuiAction m_actionNewProgram =
        new GoGuiAction(this, "New Program...", "Add new Go program") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionNewProgram(); } };

    public final GoGuiAction m_actionOpen =
        new GoGuiAction(this, "Open...",
                        "Open", KeyEvent.VK_O, "document-open") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionOpen(); } };

    public final GoGuiAction m_actionPass =
        new GoGuiAction(this, "Pass", "Play a pass", KeyEvent.VK_F2,
                        getFunctionKeyShortcut(), "gogui-pass") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPass(); } };

    public final GoGuiAction m_actionPlay =
        new GoGuiAction(this, "Computer Play", "Make computer play",
                        KeyEvent.VK_F5, getFunctionKeyShortcut(),
                        "gogui-play") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(false); } };

    public final GoGuiAction m_actionPlaySingleMove =
        new GoGuiAction(this, "Play Single Move",
                        "Make computer play a move (do not change computer color)",
                        KeyEvent.VK_F5,
                        getFunctionKeyShortcut() | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPlay(true); } };

    public final GoGuiAction m_actionPreviousEarlierVariation =
        new GoGuiAction(this, "Previous Earlier Variation",
                        "Go to previous earlier variation",
                        KeyEvent.VK_UP,
                        getShortcut() | ActionEvent.SHIFT_MASK) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousEarlierVariation(); } };

    public final GoGuiAction m_actionPreviousVariation =
        new GoGuiAction(this, "Previous Variation",
                        "Go to previous variation", KeyEvent.VK_UP,
                        "gogui-up") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPreviousVariation(); } };

    public final GoGuiAction m_actionPrint =
        new GoGuiAction(this, "Print...", "Print current position",
                        KeyEvent.VK_P, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionPrint(); } };

    public final GoGuiAction m_actionReattachProgram =
        new GoGuiAction(this, "Reattach",
                        "Restart Go program and attach it to the currrent position",
                        KeyEvent.VK_T) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionReattachProgram(); } };

    public final GoGuiAction m_actionSave =
        new GoGuiAction(this, "Save", "Save", KeyEvent.VK_S, "document-save") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSave(); } };

    public final GoGuiAction m_actionSaveAs =
        new GoGuiAction(this, "Save As...", "Save As", "document-save-as") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSaveAs(); } };

    public final GoGuiAction m_actionScore =
        new GoGuiAction(this, "Score", "Score position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionScore(); } };

    public final GoGuiAction m_actionSetupBlack =
        new GoGuiAction(this, "Setup Black",
                        "Add black stones and set Black to play",
                        "gogui-setup-black") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(BLACK); } };

    public final GoGuiAction m_actionSetupWhite =
        new GoGuiAction(this, "Setup White",
                        "Add white stones and set White to play",
                        "gogui-setup-white") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionSetup(WHITE); } };

    public final GoGuiAction m_actionShowAnalyzeDialog =
        new GoGuiAction(this, "Analyze Commands",
                        "Show window with analyze commands",
                        KeyEvent.VK_F8, getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowAnalyzeDialog(); } };

    public final GoGuiAction m_actionShowShell =
        new GoGuiAction(this, "GTP Shell", "Show GTP shell window",
                        KeyEvent.VK_F9, getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowShell(); } };

    public final GoGuiAction m_actionShowTree =
        new GoGuiAction(this, "Tree Viewer", "Show game tree window",
                        KeyEvent.VK_F7, getFunctionKeyShortcut()) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionShowTree(); } };

    public final GoGuiAction m_actionToggleAutoNumber =
        new GoGuiAction(this, "Auto Number", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleAutoNumber(); } };

    public final GoGuiAction m_actionToggleBeepAfterMove =
        new GoGuiAction(this, "Play Sound",
                        "Play a sound after computer played a move") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleBeepAfterMove(); } };

    public final GoGuiAction m_actionToggleCompletion =
        new GoGuiAction(this, "Popup Completions", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCompletion(); } };

    public final GoGuiAction m_actionToggleCommentMonoFont =
        new GoGuiAction(this, "Monospace Comment Font",
                        "Use fixed width font for comment") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleCommentMonoFont(); } };

    public final GoGuiAction m_actionToggleShowCursor =
        new GoGuiAction(this, "Cursor", "Show cursor on board") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowCursor(); } };

    public final GoGuiAction m_actionToggleShowGrid =
        new GoGuiAction(this, "Grid Labels", "Show board grid labels") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowGrid(); } };

    public final GoGuiAction m_actionToggleShowInfoPanel =
        new GoGuiAction(this, "Info Panel",
                        "Show panel with comment and game information") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowInfoPanel(); } };

    public final GoGuiAction m_actionToggleShowLastMove =
        new GoGuiAction(this, "Last Move", "Mark last move on board") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowLastMove(); } };

    public final GoGuiAction m_actionToggleShowSubtreeSizes =
        new GoGuiAction(this, "Subtree Sizes", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowSubtreeSizes(); } };

    public final GoGuiAction m_actionToggleShowToolbar =
        new GoGuiAction(this, "Toolbar", "Show tool bar") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowToolbar(); } };

    public final GoGuiAction m_actionToggleShowVariations =
        new GoGuiAction(this, "Variation Labels",
                        "Label children moves with letters on board") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleShowVariations(); } };

    public final GoGuiAction m_actionToggleTimeStamp =
        new GoGuiAction(this, "Timestamp", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionToggleTimeStamp(); } };

    public final GoGuiAction m_actionTreeLabelsNumber =
        new GoGuiAction(this, "Move Number", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NUMBER); } };

    public final GoGuiAction m_actionTreeLabelsMove =
        new GoGuiAction(this, "Move", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_MOVE); } };

    public final GoGuiAction m_actionTreeLabelsNone =
        new GoGuiAction(this, "None", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeLabels(GameTreePanel.LABEL_NONE); } };

    public final GoGuiAction m_actionTreeSizeLarge =
        new GoGuiAction(this, "Large", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_LARGE); } };

    public final GoGuiAction m_actionTreeSizeNormal =
        new GoGuiAction(this, "Normal", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_NORMAL); } };

    public final GoGuiAction m_actionTreeSizeSmall =
        new GoGuiAction(this, "Small", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_SMALL); } };

    public final GoGuiAction m_actionTreeSizeTiny =
        new GoGuiAction(this, "Tiny", null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTreeSize(GameTreePanel.SIZE_TINY); } };

    public final GoGuiAction m_actionTruncate =
        new GoGuiAction(this, "Truncate",
                        "Truncate subtree including this position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncate(); } };

    public final GoGuiAction m_actionTruncateChildren =
        new GoGuiAction(this, "Truncate Children",
                        "Truncate all children of this position") {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionTruncateChildren(); } };

    public final GoGuiAction m_actionQuit =
        new GoGuiAction(this, "Quit", "Quit GoGui", KeyEvent.VK_Q, null) {
            public void actionPerformed(ActionEvent e) {
                m_goGui.actionQuit(); } };

    public GoGuiActions(GoGui goGui)
    {
        m_goGui = goGui;
    }

    public static void register(JComponent component, GoGuiAction action)
    {
        int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        InputMap inputMap = component.getInputMap(condition);
        ActionMap actionMap = component.getActionMap();
        KeyStroke keyStroke =
            (KeyStroke)action.getValue(AbstractAction.ACCELERATOR_KEY);
        if (keyStroke != null)
        {
            String name = (String)action.getValue(AbstractAction.NAME);
            inputMap.put(keyStroke, name);
            actionMap.put(name, action);
        }
    }

    public void registerAll(JComponent component)
    {
        for (int i = 0; i < m_allActions.size(); ++i)
            register(component, m_allActions.get(i));
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
        updateActionClockResume(clock);
        updateActionClockRestore(node, clock);
        updateActionClockStart(clock);
        m_actionComputerBlack.setEnabled(isProgramAttached);
        m_actionComputerBlack.setSelected(computerBlack && ! computerWhite);
        m_actionComputerBoth.setEnabled(isProgramAttached);
        m_actionComputerBoth.setSelected(computerBlack && computerWhite);
        m_actionComputerNone.setEnabled(isProgramAttached);
        m_actionComputerNone.setSelected(! computerBlack && ! computerWhite);
        m_actionComputerWhite.setEnabled(isProgramAttached);
        m_actionComputerWhite.setSelected(! computerBlack && computerWhite);
        m_actionDeleteSideVariations.setEnabled(isInMain && treeHasVariations);
        updateActionDetachProgram(isProgramAttached, name);
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
        updateActionInterrupt(isProgramAttached, isInterruptSupported,
                              isCommandInProgress, name);
        m_actionKeepOnlyPosition.setEnabled(hasFather || hasChildren);
        m_actionMakeMainVariation.setEnabled(! isInMain);
        m_actionNextEarlierVariation.setEnabled(hasNextEarlierVariation);
        m_actionNextVariation.setEnabled(hasNextVariation);
        updateActionPass(toMove);
        updateActionPlay(toMove, isProgramAttached, name);
        m_actionPlaySingleMove.setEnabled(isProgramAttached);
        m_actionPreviousVariation.setEnabled(hasPreviousVariation);
        m_actionPreviousEarlierVariation.setEnabled(hasPrevEarlierVariation);
        updateActionReattachProgram(isProgramAttached, name);
        updateActionSave(file, isModified);
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

    private void updateActionClockRestore(ConstNode node, ConstClock clock)
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

    private void updateActionClockResume(ConstClock clock)
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

    private void updateActionClockStart(ConstClock clock)
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

    private void updateActionDetachProgram(boolean isProgramAttached,
                                           String name)
    {
        m_actionDetachProgram.setEnabled(isProgramAttached);
        if (! isProgramAttached || name == null)
            m_actionDetachProgram.setName("Detach Program");
        else
            m_actionDetachProgram.setName("Detach " + name);
    }

    private void updateActionInterrupt(boolean isProgramAttached,
                                       boolean isInterruptSupported,
                                       boolean isCommandInProgress,
                                       String name)
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

    private void updateActionPass(GoColor toMove)
    {
        assert toMove.isBlackWhite();
        if (toMove == BLACK)
            m_actionPass.setDescription("Play a pass for Black");
        else
            m_actionPass.setDescription("Play a pass for White");
    }

    private void updateActionPlay(GoColor toMove, boolean isProgramAttached,
                                  String name)
    {
        m_actionPlay.setEnabled(isProgramAttached);
        String desc;
        if (name == null)
            name = "computer";
        if (toMove == BLACK)
            desc = "Make " + name + " play Black";
        else
            desc = "Make " + name + " play White";
        if (! isProgramAttached)
            desc = desc + " (no program attached)";
        m_actionPlay.setDescription(desc);
    }

    private void updateActionReattachProgram(boolean isProgramAttached,
                                             String name)
    {
        m_actionReattachProgram.setEnabled(isProgramAttached);
        if (! isProgramAttached || name == null)
            m_actionReattachProgram.setName("Reattach Program");
        else
            m_actionReattachProgram.setName("Reattach " + name);
    }

    private void updateActionSave(File file, boolean isModified)
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
