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
            AnalyzeCommand.Callback callback) throws AnalyzeCommand.Error
    {
        m_listener = listener;
        m_buttonNew = addButton("icons/NewBoard.png", "new-game", "New game");
        m_buttonOpen = addButton(m_prefix + "Open.png", "load", "Load game");
        m_buttonSave = addButton(m_prefix + "Save.png", "save", "Save game");
        add(new JToolBar.Separator());
        m_buttonBeginning = addButton(m_prefix + "VCRBegin.png", "beginning",
                                      "Beginning of game");
        m_buttonBackward10 = addButton(m_prefix + "VCRRewind.png",
                                       "backward-10", "Take back 10 moves");
        m_buttonBackward = addButton(m_prefix + "VCRBack.png", "backward",
                                     "Take back move");
        m_buttonForward = addButton(m_prefix + "VCRForward.png", "forward",
                                    "Replay move");
        m_buttonForward10 = addButton(m_prefix + "VCRFastForward.png",
                                      "forward-10", "Replay 10 moves");
        m_buttonEnd = addButton(m_prefix + "VCREnd.png", "end", "End of game");
        add(new JToolBar.Separator());
        m_buttonPass = addButton(m_prefix + "Delete.png", "pass", "Pass");
        m_buttonEnter = addButton(m_prefix + "Enter.png", "play",
                                  "Computer play");
        m_buttonStop = addButton(m_prefix + "Stop.png", "interrupt",
                                      "Interrupt");
        add(new JToolBar.Separator());
        m_buttonGtpShell = addButton(m_prefix + "Computer.png", "gtp-shell",
                                     "GTP shell");
        add(new JToolBar.Separator());
        m_buttonAnalyze = addButton(m_prefix + "Gearwheel.png", "analyze",
                                    "Enable analyze command");
        m_analyzeCommand = new AnalyzeCommand(callback, prefs);
        add(m_analyzeCommand);
    }

    public void disableComputerButtons()
    {
        m_computerButtonsEnabled = false;
        m_buttonEnter.setEnabled(false);
        m_buttonGtpShell.setEnabled(false);
        m_buttonAnalyze.setEnabled(false);
        m_buttonStop.setEnabled(false);
        m_analyzeCommand.setEnabled(false);
    }

    public void toggleAnalyze()
    {
        m_analyzeCommand.setEnabled(! m_analyzeCommand.isEnabled());
        m_analyzeCommand.setAnalyzeCommand();
    }

    public void updateGameButtons(Board board)
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

    public void enableAll(boolean enable, Board board)
    {
        assert(m_enable != enable);
        m_enable = enable;
        m_buttonAnalyze.setEnabled(enable);
        m_buttonBeginning.setEnabled(enable);
        m_buttonBackward.setEnabled(enable);
        m_buttonBackward10.setEnabled(enable);
        m_buttonForward.setEnabled(enable);
        m_buttonForward10.setEnabled(enable);
        m_buttonEnd.setEnabled(enable);
        m_buttonEnter.setEnabled(enable);
        m_buttonGtpShell.setEnabled(enable);
        m_buttonNew.setEnabled(enable);
        m_buttonOpen.setEnabled(enable);
        m_buttonPass.setEnabled(enable);
        m_buttonSave.setEnabled(enable);
        if (enable)
        {
            if (! m_computerButtonsEnabled)
                disableComputerButtons();
            m_analyzeCommand.setEnabled(m_analyzeWasEnabled);
            updateGameButtons(board);
        }
        else
        {
            m_analyzeWasEnabled = m_analyzeCommand.isEnabled();
            m_analyzeCommand.setEnabled(false);
        }
    }

    private boolean m_analyzeWasEnabled;
    private boolean m_computerButtonsEnabled = true;
    private boolean m_enable = true;
    private ActionListener m_listener;
    private AnalyzeCommand m_analyzeCommand;
    private JButton m_buttonAnalyze;
    private JButton m_buttonEnter;
    private JButton m_buttonBackward;
    private JButton m_buttonBackward10;
    private JButton m_buttonBeginning;
    private JButton m_buttonForward;
    private JButton m_buttonForward10;
    private JButton m_buttonEnd;
    private JButton m_buttonGtpShell;
    private JButton m_buttonStop;
    private JButton m_buttonNew;
    private JButton m_buttonOpen;
    private JButton m_buttonPass;
    private JButton m_buttonSave;
    private static final String m_prefix = "org/javalobby/icons/20x20png/";

    private JButton addButton(String icon, String command, String toolTip)
    {
        URL u = getClass().getClassLoader().getResource(icon);
        JButton button = new JButton(new ImageIcon(u));
        button.setToolTipText(toolTip);
        button.setActionCommand(command);
        button.addActionListener(m_listener);
        add(button);
        return button;
    }
}

//-----------------------------------------------------------------------------
