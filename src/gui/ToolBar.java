//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import go.*;

//-----------------------------------------------------------------------------

class ToolBar
    extends JToolBar
{
    ToolBar(ActionListener listener, Preferences prefs,
            Analyze.Callback callback) throws Analyze.Error
    {
        m_listener = listener;
        m_buttonNew = addButton("images/filenew.png", "new-game", "New game");
        m_buttonOpen = addButton("images/fileopen.png", "load", "Load game");
        m_buttonSave = addButton("images/filesave2.png", "save", "Save game");
        add(new JToolBar.Separator());
        m_buttonBeginning = addButton("images/player_start.png", "beginning",
                                      "Beginning of game");
        m_buttonBackward10 = addButton("images/player_rew.png",
                                       "backward-10", "Take back 10 moves");
        m_buttonBackward = addButton("images/player_back.png", "backward",
                                     "Take back move");
        m_buttonForward = addButton("images/player_next.png", "forward",
                                    "Replay move");
        m_buttonForward10 = addButton("images/player_fwd.png", "forward-10",
                                      "Replay 10 moves");
        m_buttonEnd = addButton("images/player_end.png", "end", "End of game");
        add(new JToolBar.Separator());
        m_buttonPass = addButton("images/button_cancel.png", "pass", "Pass");
        m_buttonEnter = addButton("images/next.png", "play", "Computer play");
        m_buttonInterrupt =
            addButton("images/stop.png", "interrupt", "Interrupt");
        add(new JToolBar.Separator());
        m_buttonGtpShell = addButton("images/openterm.png", "gtp-shell",
                                     "GTP shell");
        add(new JToolBar.Separator());
        m_buttonAnalyze = addButton("images/gear.png", "analyze",
                                    "Enable analyze command");
        m_analyze = new Analyze(callback, prefs);
        add(m_analyze);
    }

    public void disableComputerButtons()
    {
        m_computerButtonsEnabled = false;
        m_buttonEnter.setEnabled(false);
        m_buttonGtpShell.setEnabled(false);
        m_buttonAnalyze.setEnabled(false);
        m_buttonInterrupt.setEnabled(false);
        m_analyze.setEnabled(false);
    }

    public void toggleAnalyze()
    {
        m_analyze.setEnabled(! m_analyze.isEnabled());
        m_analyze.setAnalyzeCommand();
    }

    public void updateGameButtons(go.Board board)
    {
        int moveNumber = board.getMoveNumber();
        int numberSavedMoves = board.getNumberSavedMoves();
        m_buttonBeginning.setEnabled(moveNumber > 0);
        m_buttonBackward.setEnabled(moveNumber > 0);
        m_buttonBackward10.setEnabled(moveNumber > 0);
        m_buttonForward.setEnabled(moveNumber < numberSavedMoves);
        m_buttonForward10.setEnabled(moveNumber < numberSavedMoves);
        m_buttonEnd.setEnabled(moveNumber < numberSavedMoves);
    }

    public void enableAll(boolean enable, go.Board board)
    {
        m_buttonGtpShell.setEnabled(true);
        m_buttonAnalyze.setEnabled(enable);
        m_buttonBeginning.setEnabled(enable);
        m_buttonBackward.setEnabled(enable);
        m_buttonBackward10.setEnabled(enable);
        m_buttonForward.setEnabled(enable);
        m_buttonForward10.setEnabled(enable);
        m_buttonEnd.setEnabled(enable);
        m_buttonEnter.setEnabled(enable);
        m_buttonNew.setEnabled(enable);
        m_buttonOpen.setEnabled(enable);
        m_buttonPass.setEnabled(enable);
        m_buttonSave.setEnabled(enable);
        if (enable)
        {
            if (! m_computerButtonsEnabled)
                disableComputerButtons();
            m_analyze.setEnabled(m_analyzeWasEnabled);
            updateGameButtons(board);
            m_buttonInterrupt.setEnabled(false);
        }
        else
        {
            m_analyzeWasEnabled = m_analyze.isEnabled();
            m_analyze.setEnabled(false);
        }
    }

    public void setCommandInProgress()
    {
        enableAll(false, null);
        m_buttonInterrupt.setEnabled(true);
    }

    private boolean m_analyzeWasEnabled;

    private boolean m_computerButtonsEnabled = true;

    private ActionListener m_listener;

    private Analyze m_analyze;

    private JButton m_buttonAnalyze;

    private JButton m_buttonEnter;

    private JButton m_buttonBackward;

    private JButton m_buttonBackward10;

    private JButton m_buttonBeginning;

    private JButton m_buttonForward;

    private JButton m_buttonForward10;

    private JButton m_buttonEnd;

    private JButton m_buttonGtpShell;

    private JButton m_buttonInterrupt;

    private JButton m_buttonNew;

    private JButton m_buttonOpen;

    private JButton m_buttonPass;

    private JButton m_buttonSave;

    private JButton addButton(String icon, String command, String toolTip)
    {
        URL u = getClass().getClassLoader().getResource(icon);
        JButton button = new ToolBarButton(new ImageIcon(u));
        button.setToolTipText(toolTip);
        button.setActionCommand(command);
        button.addActionListener(m_listener);
        button.setEnabled(false);
        add(button);
        return button;
    }
}

//-----------------------------------------------------------------------------
