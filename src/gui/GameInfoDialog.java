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
    extends JOptionPane
{
    public static void show(Component parent, GameInformation gameInformation)
    {
        GameInfoDialog gameInfoDialog = new GameInfoDialog(gameInformation);
        JDialog dialog =
            gameInfoDialog.createDialog(parent, "GoGui: Game Info");
        boolean done = false;
        while (! done)
        {
            dialog.show();
            Object value = gameInfoDialog.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return;
            done = gameInfoDialog.validate(parent);
        }
        dialog.dispose();
        gameInformation.m_playerBlack =
            getTextFieldContent(gameInfoDialog.m_playerBlack);
        gameInformation.m_blackRank =
            getTextFieldContent(gameInfoDialog.m_rankBlack);
        gameInformation.m_playerWhite =
            getTextFieldContent(gameInfoDialog.m_playerWhite);
        gameInformation.m_whiteRank =
            getTextFieldContent(gameInfoDialog.m_rankWhite);
        gameInformation.m_rules =
            getTextFieldContent(gameInfoDialog.m_rules);
        gameInformation.m_result =
            getTextFieldContent(gameInfoDialog.m_result);
        gameInformation.m_date =
            getTextFieldContent(gameInfoDialog.m_date);
        gameInformation.m_komi =
            Float.parseFloat(getTextFieldContent(gameInfoDialog.m_komi));
        String rules =
            getTextFieldContent(gameInfoDialog.m_rules).trim().toLowerCase();
        if (! rules.equals("chinese") && ! rules.equals("japanese"))
            SimpleDialogs.showWarning(parent, "Unknown rules");
    }

    private JPanel m_panel;

    private JTextField m_date;

    private JTextField m_komi;

    private JTextField m_playerBlack;

    private JTextField m_playerWhite;

    private JTextField m_rankBlack;

    private JTextField m_rankWhite;

    private JTextField m_result;

    private JTextField m_rules;

    private GameInfoDialog(GameInformation gameInformation)
    {
        m_panel = new JPanel(new GridLayout(0, 2, 0, 0));
        m_playerBlack = createEntry("Black player",
                                    gameInformation.m_playerBlack);
        m_rankBlack = createEntry("Black rank",
                                  gameInformation.m_blackRank);
        m_playerWhite = createEntry("White player",
                                    gameInformation.m_playerWhite);
        m_rankWhite = createEntry("White rank",
                                  gameInformation.m_whiteRank);
        m_date = createEntry("Date", gameInformation.m_date);
        m_rules = createEntry("Rules", gameInformation.m_rules);
        m_komi = createEntry("Komi", Float.toString(gameInformation.m_komi));
        m_result = createEntry("Result", gameInformation.m_result);
        setMessage(m_panel);
        setOptionType(OK_CANCEL_OPTION);
    }

    private JTextField createEntry(String labelText, String text)
    {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        m_panel.add(label);
        JTextField textField = new JTextField(text);
        m_panel.add(textField);
        return textField;
    }

    private static String getTextFieldContent(JTextField textField)
    {
        return textField.getText().trim();
    }

    private boolean validate(Component parent)
    {
        try
        {
            float komi = Float.parseFloat(getTextFieldContent(m_komi));
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, "Invalid komi value");
            return false;
        }
        return true;
    }
}

//-----------------------------------------------------------------------------
