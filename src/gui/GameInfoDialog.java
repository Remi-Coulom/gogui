//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import game.*;
import go.*;
import utils.GuiUtils;

//-----------------------------------------------------------------------------

public class GameInfoDialog
    extends JPanel
{
    public static void show(Component parent, GameInformation gameInformation)
    {
        GameInfoDialog gameInfoDialog = new GameInfoDialog(gameInformation);
        int result =
            JOptionPane.showConfirmDialog(parent, gameInfoDialog,
                                          "GoGui: Game Information",
                                          JOptionPane.OK_CANCEL_OPTION,
                                          JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;
        gameInformation.m_playerBlack =
            getTextFieldContent(gameInfoDialog.m_playerBlack);
        gameInformation.m_playerWhite =
            getTextFieldContent(gameInfoDialog.m_playerWhite);
        gameInformation.m_blackRank =
            getTextFieldContent(gameInfoDialog.m_rankBlack);
        gameInformation.m_whiteRank =
            getTextFieldContent(gameInfoDialog.m_rankWhite);
        gameInformation.m_result =
            getTextFieldContent(gameInfoDialog.m_result);
        gameInformation.m_date =
            getTextFieldContent(gameInfoDialog.m_date);
        try
        {
            gameInformation.m_komi =
                Float.parseFloat(getTextFieldContent(gameInfoDialog.m_komi));
        }
        catch (NumberFormatException e)
        {
        }
    }

    private JTextField m_date;

    private JTextField m_komi;

    private JTextField m_playerBlack;

    private JTextField m_playerWhite;

    private JTextField m_rankBlack;

    private JTextField m_rankWhite;

    private JTextField m_result;

    private GameInfoDialog(GameInformation gameInformation)
    {
        super(new GridLayout(0, 2, 0, 0));
        m_playerBlack = createEntry("Black player",
                                    gameInformation.m_playerBlack);
        m_playerWhite = createEntry("White player",
                                    gameInformation.m_playerWhite);
        m_rankBlack = createEntry("Black rank",
                                  gameInformation.m_blackRank);
        m_rankWhite = createEntry("White rank",
                                  gameInformation.m_whiteRank);
        m_date = createEntry("Date", gameInformation.m_date);
        m_komi = createEntry("Komi", Float.toString(gameInformation.m_komi));
        m_result = createEntry("Result", gameInformation.m_result);
    }

    private JTextField createEntry(String labelText, String text)
    {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        add(label);
        JTextField textField = new JTextField(text);
        add(textField);
        return textField;
    }

    private static String getTextFieldContent(JTextField textField)
    {
        return textField.getText().trim();
    }
}

//-----------------------------------------------------------------------------
