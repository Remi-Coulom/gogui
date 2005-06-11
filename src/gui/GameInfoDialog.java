//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import game.GameInformation;
import game.TimeSettings;

//----------------------------------------------------------------------------

/** Dialog for editing game settings and other information. */
public class GameInfoDialog
    extends JOptionPane
{
    /** Returns false if nothing was changed. */
    public static boolean show(Component parent,
                               GameInformation gameInformation)
    {
        GameInfoDialog gameInfoDialog = new GameInfoDialog(gameInformation);
        JDialog dialog = gameInfoDialog.createDialog(parent, "Game Info");
        boolean done = false;
        while (! done)
        {
            dialog.setVisible(true);
            Object value = gameInfoDialog.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return false;
            done = gameInfoDialog.validate(parent);
        }
        dialog.dispose();
        boolean changed = false;
        String black = getTextFieldContent(gameInfoDialog.m_playerBlack);
        if (! black.equals(gameInformation.m_playerBlack))
        {
            gameInformation.m_playerBlack = black;
            changed = true;
        }
        String white = getTextFieldContent(gameInfoDialog.m_playerWhite);
        if (! white.equals(gameInformation.m_playerWhite))
        {
            gameInformation.m_playerWhite = white;
            changed = true;
        }
        String rankBlack = getTextFieldContent(gameInfoDialog.m_rankBlack);
        if (! rankBlack.equals(gameInformation.m_blackRank))
        {
            gameInformation.m_blackRank = rankBlack;
            changed = true;
        }
        String rankWhite = getTextFieldContent(gameInfoDialog.m_rankWhite);
        if (! rankWhite.equals(gameInformation.m_whiteRank))
        {
            gameInformation.m_whiteRank = rankWhite;
            changed = true;
        }
        String rules = getTextFieldContent(gameInfoDialog.m_rules);
        if (! rules.equals(gameInformation.m_rules))
        {
            gameInformation.m_rules = rules;
            changed = true;
        }
        String result = getTextFieldContent(gameInfoDialog.m_result);
        if (! result.equals(gameInformation.m_result))
        {
            gameInformation.m_result = result;
            changed = true;
        }
        String date = getTextFieldContent(gameInfoDialog.m_date);
        if (! date.equals(gameInformation.m_date))
        {
            gameInformation.m_date = date;
            changed = true;
        }
        double komi =
            Double.parseDouble(getTextFieldContent(gameInfoDialog.m_komi));
        if (komi != gameInformation.m_komi)
        {
            gameInformation.m_komi = komi;
            changed = true;
        }
        String preByoyomiContent
            = getTextFieldContent(gameInfoDialog.m_preByoyomi);
        String byoyomiContent
            = getTextFieldContent(gameInfoDialog.m_byoyomi);
        String byoyomiMovesContent
            = getTextFieldContent(gameInfoDialog.m_byoyomiMoves);
        if (! preByoyomiContent.equals(""))
        {            
            long preByoyomi = Integer.parseInt(preByoyomiContent) * 60000L;
            long byoyomi = -1;
            int byoyomiMoves = -1;
            if (! byoyomiContent.equals(""))
                byoyomi = Integer.parseInt(byoyomiContent) * 60000L;
            if (! byoyomiMovesContent.equals(""))
                byoyomiMoves = Integer.parseInt(byoyomiMovesContent);
            TimeSettings timeSettings = gameInformation.m_timeSettings;
            if (byoyomi > 0 && byoyomiMoves > 0)
            {
                if (timeSettings == null
                    || preByoyomi != timeSettings.getPreByoyomi()
                    || byoyomi != timeSettings.getByoyomi()
                    || byoyomiMoves != timeSettings.getByoyomiMoves())
                {
                    gameInformation.m_timeSettings
                        = new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
                    changed = true;
                }
            }
            else
            {
                if (timeSettings == null
                    || preByoyomi != timeSettings.getPreByoyomi())
                {
                    gameInformation.m_timeSettings
                        = new TimeSettings(preByoyomi);
                    changed = true;
                }
            }
        }
        else
        {
            if (gameInformation.m_timeSettings != null)
            {
                gameInformation.m_timeSettings = null;
                changed = true;
            }
        }
        return changed;
    }

    private JPanel m_panelLeft;

    private JPanel m_panelRight;

    private JTextField m_byoyomi;

    private JTextField m_byoyomiMoves;

    private JTextField m_date;

    private JTextField m_komi;

    private JTextField m_playerBlack;

    private JTextField m_playerWhite;

    private JTextField m_preByoyomi;

    private JTextField m_rankBlack;

    private JTextField m_rankWhite;

    private JTextField m_result;

    private JTextField m_rules;

    private GameInfoDialog(GameInformation gameInformation)
    {
        JPanel panel = new JPanel(new BorderLayout(GuiUtils.PAD, 0));
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, 0));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight = new JPanel(new GridLayout(0, 1, 0, 0));
        panel.add(m_panelRight, BorderLayout.CENTER);
        m_playerBlack = createEntry("Black player:",
                                    gameInformation.m_playerBlack);
        m_rankBlack = createEntry("Black rank:",
                                  gameInformation.m_blackRank);
        m_playerWhite = createEntry("White player:",
                                    gameInformation.m_playerWhite);
        m_rankWhite = createEntry("White rank:",
                                  gameInformation.m_whiteRank);
        m_date = createEntry("Date:", gameInformation.m_date);
        m_rules = createEntry("Rules:", gameInformation.m_rules);
        m_komi = createEntry("Komi:",
                             Double.toString(gameInformation.m_komi));
        m_result = createEntry("Result:", gameInformation.m_result);
        createTimeEntry(gameInformation.m_timeSettings);
        setMessage(panel);
        setOptionType(OK_CANCEL_OPTION);
    }

    private JTextField createEntry(String labelText, String text)
    {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        m_panelLeft.add(label);
        JTextField textField = new JTextField(text);
        m_panelRight.add(textField);
        return textField;
    }

    private void createTimeEntry(TimeSettings timeSettings)
    {
        JLabel label = new JLabel("Time:");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        m_panelLeft.add(label);
        FlowLayout layout = new FlowLayout(FlowLayout.LEFT, 0, 0);
        JPanel panel = new JPanel(layout);
        m_preByoyomi = new JTextField(2);
        m_preByoyomi.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null)
        {
            int preByoyomi = (int)(timeSettings.getPreByoyomi() / 60000L);
            m_preByoyomi.setText(Integer.toString(preByoyomi));
        }
        panel.add(m_preByoyomi);
        panel.add(new JLabel("min +"));
        m_byoyomi = new JTextField(2);
        m_byoyomi.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomi = (int)(timeSettings.getByoyomi() / 60000L);
            m_byoyomi.setText(Integer.toString(byoyomi));
        }
        panel.add(m_byoyomi);
        panel.add(new JLabel("min /"));
        m_byoyomiMoves = new JTextField(2);
        m_byoyomiMoves.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomiMoves = timeSettings.getByoyomiMoves();
            m_byoyomiMoves.setText(Integer.toString(byoyomiMoves));
        }
        panel.add(m_byoyomiMoves);
        panel.add(new JLabel("moves"));
        m_panelRight.add(panel);
    }

    private static String getTextFieldContent(JTextField textField)
    {
        return textField.getText().trim();
    }

    private boolean validate(Component parent)
    {
        if (! validateDouble(parent, m_komi, "Invalid komi"))
            return false;
        if (! validatePosIntOrEmpty(parent, m_preByoyomi,
                                    "Invalid time settings"))
            return false;
        if (! validatePosIntOrEmpty(parent, m_byoyomi,
                                    "Invalid time settings"))
            return false;
        if (! validatePosIntOrEmpty(parent, m_byoyomiMoves,
                                    "Invalid time settings"))
            return false;
        return true;
    }

    private boolean validateDouble(Component parent, JTextField textField,
                                   String errorMessage)
    {
        try
        {
            Double.parseDouble(getTextFieldContent(textField));
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, errorMessage);
            return false;
        }
        return true;
    }

    private boolean validatePosIntOrEmpty(Component parent,
                                          JTextField textField,
                                          String errorMessage)
    {
        try
        {
            String content = getTextFieldContent(textField);
            if (content.equals(""))
                return true;
            int value = Integer.parseInt(content);
            if (value <= 0)
                return false;
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, errorMessage);
            return false;
        }
        return true;
    }
}

//----------------------------------------------------------------------------
