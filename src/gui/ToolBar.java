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
import utils.Preferences;

//-----------------------------------------------------------------------------

class ToolBar
    extends JToolBar
{
    ToolBar(ActionListener listener, Preferences prefs)
    {
        m_listener = listener;
        m_buttonNew = addButton("filenew.png", "new-game", "New game");
        m_buttonOpen = addButton("fileopen.png", "open", "Load game");
        m_buttonSave = addButton("filesave2.png", "save", "Save game");
        add(new JToolBar.Separator());
        m_buttonBeginning = addButton("player_start.png", "beginning",
                                      "Beginning of game");
        m_buttonBackward10 = addButton("player_rew.png",
                                       "backward-10", "Take back 10 moves");
        m_buttonBackward = addButton("player_back.png", "backward",
                                     "Take back move");
        m_buttonForward = addButton("player_next.png", "forward",
                                    "Replay move");
        m_buttonForward10 = addButton("player_fwd.png", "forward-10",
                                      "Replay 10 moves");
        m_buttonEnd = addButton("player_end.png", "end", "End of game");
        add(new JToolBar.Separator());
        m_buttonPass = addButton("pass.png", "pass", "Pass");
        m_buttonEnter = addButton("next.png", "play", "Computer play");
        m_buttonInterrupt = addButton("stop.png", "interrupt", "Interrupt");
        add(new JToolBar.Separator());
        m_buttonGtpShell = addButton("openterm.png", "gtp-shell", "GTP shell");
    }

    public void disableComputerButtons()
    {
        m_computerButtonsEnabled = false;
        m_buttonEnter.setEnabled(false);
        m_buttonGtpShell.setEnabled(false);
        m_buttonInterrupt.setEnabled(false);
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
            updateGameButtons(board);
            m_buttonInterrupt.setEnabled(false);
        }
    }

    public void setCommandInProgress(boolean isInterruptSupported)
    {
        enableAll(false, null);
        if (isInterruptSupported)
            m_buttonInterrupt.setEnabled(true);
    }

    private boolean m_computerButtonsEnabled = true;

    private ActionListener m_listener;

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
        URL u = getClass().getClassLoader().getResource("images/" + icon);
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
