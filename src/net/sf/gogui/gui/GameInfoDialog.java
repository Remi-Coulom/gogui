//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

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
import net.sf.gogui.go.Komi;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.util.ObjectUtil;

/** Dialog for editing game settings and other information. */
public final class GameInfoDialog
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
        if (! black.equals(gameInformation.getPlayerBlack()))
        {
            gameInformation.setPlayerBlack(black);
            changed = true;
        }
        String white = getTextFieldContent(gameInfoDialog.m_playerWhite);
        if (! white.equals(gameInformation.getPlayerWhite()))
        {
            gameInformation.setPlayerWhite(white);
            changed = true;
        }
        String rankBlack = getTextFieldContent(gameInfoDialog.m_rankBlack);
        if (! rankBlack.equals(gameInformation.getRankBlack()))
        {
            gameInformation.setRankBlack(rankBlack);
            changed = true;
        }
        String rankWhite = getTextFieldContent(gameInfoDialog.m_rankWhite);
        if (! rankWhite.equals(gameInformation.getRankWhite()))
        {
            gameInformation.setRankWhite(rankWhite);
            changed = true;
        }
        String rules = getTextFieldContent(gameInfoDialog.m_rules);
        if (! rules.equals(gameInformation.getRules()))
        {
            gameInformation.setRules(rules);
            changed = true;
        }
        String result = getTextFieldContent(gameInfoDialog.m_result);
        if (! result.equals(gameInformation.getResult()))
        {
            gameInformation.setResult(result);
            changed = true;
        }
        String date = getTextFieldContent(gameInfoDialog.m_date);
        if (! date.equals(gameInformation.getDate()))
        {
            gameInformation.setDate(date);
            changed = true;
        }
        String komiText = getTextFieldContent(gameInfoDialog.m_komi);
        Komi komi = null;
        try
        {
            komi = Komi.parseKomi(komiText);
        }
        catch (Komi.InvalidKomi e)
        {
            assert(false); // already validated
        }
        if (! ObjectUtil.equals(komi, gameInformation.getKomi()))
        {
            gameInformation.setKomi(komi);
            changed = true;
        }
        String preByoyomiContent
            = getTextFieldContent(gameInfoDialog.m_preByoyomi);
        String byoyomiContent
            = getTextFieldContent(gameInfoDialog.m_byoyomi);
        String byoyomiMovesContent
            = getTextFieldContent(gameInfoDialog.m_byoyomiMoves);
        if (preByoyomiContent.equals(""))
        {
            if (gameInformation.getTimeSettings() != null)
            {
                gameInformation.setTimeSettings(null);
                changed = true;
            }
        }
        else
        {            
            long preByoyomi = Integer.parseInt(preByoyomiContent) * 60000L;
            long byoyomi = -1;
            int byoyomiMoves = -1;
            if (! byoyomiContent.equals(""))
                byoyomi = Integer.parseInt(byoyomiContent) * 60000L;
            if (! byoyomiMovesContent.equals(""))
                byoyomiMoves = Integer.parseInt(byoyomiMovesContent);
            TimeSettings timeSettings = gameInformation.getTimeSettings();
            if (byoyomi > 0 && byoyomiMoves > 0)
            {
                if (timeSettings == null
                    || preByoyomi != timeSettings.getPreByoyomi()
                    || byoyomi != timeSettings.getByoyomi()
                    || byoyomiMoves != timeSettings.getByoyomiMoves())
                {
                    TimeSettings newTimeSettings =
                        new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
                    gameInformation.setTimeSettings(newTimeSettings);
                    changed = true;
                }
            }
            else
            {
                if (timeSettings == null
                    || preByoyomi != timeSettings.getPreByoyomi())
                {
                    TimeSettings newTimeSettings =
                        new TimeSettings(preByoyomi);
                    gameInformation.setTimeSettings(newTimeSettings);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JPanel m_panelLeft;

    private final JPanel m_panelRight;

    private JTextField m_byoyomi;

    private JTextField m_byoyomiMoves;

    private JTextField m_date;

    private JTextField m_komi;

    private JTextField m_playerBlack;

    private JTextField m_playerWhite;

    private JTextField m_preByoyomi;

    private final JTextField m_rankBlack;

    private final JTextField m_rankWhite;

    private JTextField m_result;

    private JTextField m_rules;

    private GameInfoDialog(GameInformation gameInformation)
    {
        JPanel panel = new JPanel(new BorderLayout(GuiUtil.PAD, 0));
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, GuiUtil.SMALL_PAD));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight =
            new JPanel(new GridLayout(0, 1, 0, GuiUtil.SMALL_PAD));
        panel.add(m_panelRight, BorderLayout.CENTER);
        m_playerBlack = createEntry("Black player:",
                                    gameInformation.getPlayerBlack());
        m_rankBlack = createEntry("Black rank:",
                                  gameInformation.getRankBlack());
        m_playerWhite = createEntry("White player:",
                                    gameInformation.getPlayerWhite());
        m_rankWhite = createEntry("White rank:",
                                  gameInformation.getRankWhite());
        m_date = createEntry("Date:", gameInformation.getDate());
        m_rules = createEntry("Rules:", gameInformation.getRules());
        Komi komi = gameInformation.getKomi();
        m_komi = createEntry("Komi:", komi == null ? "" : komi.toString());
        m_result = createEntry("Result:", gameInformation.getResult());
        createTimeEntry(gameInformation.getTimeSettings());
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
        panel.add(new JLabel(" min + "));
        m_byoyomi = new JTextField(2);
        m_byoyomi.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomi = (int)(timeSettings.getByoyomi() / 60000L);
            m_byoyomi.setText(Integer.toString(byoyomi));
        }
        panel.add(m_byoyomi);
        panel.add(new JLabel(" min / "));
        m_byoyomiMoves = new JTextField(2);
        m_byoyomiMoves.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomiMoves = timeSettings.getByoyomiMoves();
            m_byoyomiMoves.setText(Integer.toString(byoyomiMoves));
        }
        panel.add(m_byoyomiMoves);
        panel.add(new JLabel(" moves"));
        m_panelRight.add(panel);
    }

    private static String getTextFieldContent(JTextField textField)
    {
        return textField.getText().trim();
    }

    private boolean validate(Component parent)
    {
        if (! validateKomi(parent, m_komi, "Invalid komi"))
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

    private boolean validateKomi(Component parent, JTextField textField,
                                 String errorMessage)
    {
        String text = getTextFieldContent(textField);
        try
        {
            Komi.parseKomi(text);
        }
        catch (Komi.InvalidKomi e)
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

