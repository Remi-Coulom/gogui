//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.util.ObjectUtil;

/** Dialog for editing game settings and other information. */
public final class GameInfoDialog
    extends JOptionPane
{
    public static void show(Component parent, GameInformation info)
    {
        GameInfoDialog gameInfo = new GameInfoDialog(info);
        JDialog dialog = gameInfo.createDialog(parent, "Game Info");
        boolean done = false;
        while (! done)
        {
            dialog.setVisible(true);
            Object value = gameInfo.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return;
            done = gameInfo.validate(parent);
        }
        dialog.dispose();
        gameInfo.updateGameInfo(info);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JPanel m_panelLeft;

    private final JPanel m_panelRight;

    private TimeField m_byoyomi;

    private JTextField m_byoyomiMoves;

    private JTextField m_date;

    private JTextField m_komi;

    private JTextField m_playerBlack;

    private JTextField m_playerWhite;

    private TimeField m_preByoyomi;

    private final JTextField m_rankBlack;

    private final JTextField m_rankWhite;

    private JTextField m_result;

    private JTextField m_rules;

    private GameInfoDialog(GameInformation info)
    {
        JPanel panel = new JPanel(new BorderLayout());
        m_panelLeft = new JPanel(new GridLayout(0, 1, 0, GuiUtil.SMALL_PAD));
        panel.add(m_panelLeft, BorderLayout.WEST);
        m_panelRight =
            new JPanel(new GridLayout(0, 1, 0, GuiUtil.SMALL_PAD));
        panel.add(m_panelRight, BorderLayout.CENTER);
        m_playerBlack = createEntry("Black player:",
                                    info.getPlayer(GoColor.BLACK));
        m_rankBlack = createEntry("Black rank:",
                                  info.getRank(GoColor.BLACK));
        m_playerWhite = createEntry("White player:",
                                    info.getPlayer(GoColor.WHITE));
        m_rankWhite = createEntry("White rank:",
                                  info.getRank(GoColor.WHITE));
        m_date = createEntry("Date:", info.getDate());
        m_rules = createEntry("Rules:", info.getRules());
        Komi komi = info.getKomi();
        m_komi = createEntry("Komi:", komi == null ? "" : komi.toString());
        m_result = createEntry("Result:", info.getResult());
        createTimeEntry(info.getTimeSettings());
        setMessage(panel);
        setOptionType(OK_CANCEL_OPTION);
    }

    private JTextField createEntry(String labelText, String text)
    {
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        m_panelLeft.add(label);
        JPanel panel = new JPanel();
        m_panelRight.add(panel);
        JTextField textField = new JTextField(25);
        textField.setText(text);
        panel.add(textField);
        return textField;
    }

    private void createTimeEntry(TimeSettings timeSettings)
    {
        JLabel label = new JLabel("Time:");
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        m_panelLeft.add(label);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        m_preByoyomi = new TimeField();
        if (timeSettings != null)
            m_preByoyomi.setTime(timeSettings.getPreByoyomi());
        panel.add(m_preByoyomi);
        panel.add(new JLabel(" + "));
        m_byoyomi = new TimeField();
        if (timeSettings != null && timeSettings.getUseByoyomi())
            m_byoyomi.setTime(timeSettings.getByoyomi());
        panel.add(m_byoyomi);
        panel.add(new JLabel(" / "));
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

    private boolean isEmpty(JTextField textField)
    {
        return getTextFieldContent(textField).equals("");
    }

    private void updateGameInfo(GameInformation info)
    {
        info.setPlayer(GoColor.BLACK, getTextFieldContent(m_playerBlack));
        info.setPlayer(GoColor.WHITE, getTextFieldContent(m_playerWhite));
        info.setRank(GoColor.BLACK, getTextFieldContent(m_rankBlack));
        info.setRank(GoColor.WHITE, getTextFieldContent(m_rankWhite));
        info.setRules(getTextFieldContent(m_rules));
        info.setResult(getTextFieldContent(m_result));
        info.setDate(getTextFieldContent(m_date));
        String komiText = getTextFieldContent(m_komi);
        Komi komi = null;
        try
        {
            komi = Komi.parseKomi(komiText);
        }
        catch (Komi.InvalidKomi e)
        {
            assert(false); // already validated
        }
        info.setKomi(komi);
        if (m_preByoyomi.isEmpty() && m_byoyomi.isEmpty()
            && isEmpty(m_byoyomiMoves))
            info.setTimeSettings(null);
        else
        {            
            long preByoyomi = m_preByoyomi.getTime();
            long byoyomi = -1;
            int byoyomiMoves = -1;
            if (! m_byoyomi.isEmpty())
                byoyomi = m_byoyomi.getTime();
            if (! isEmpty(m_byoyomiMoves))
                byoyomiMoves = Integer.parseInt(m_byoyomiMoves.getText());
            if (byoyomi > 0 && byoyomiMoves > 0)
            {
                TimeSettings settings =
                    new TimeSettings(preByoyomi, byoyomi, byoyomiMoves);
                info.setTimeSettings(settings);
            }
            else
            {
                TimeSettings settings = new TimeSettings(preByoyomi);
                info.setTimeSettings(settings);
            }
        }
    }

    private boolean validate(Component parent)
    {
        if (! validateKomi(parent, m_komi, "Invalid komi"))
            return false;
        if (! m_preByoyomi.validateTime(parent))
            return false;
        if (! m_byoyomi.validateTime(parent))
            return false;
        if (! validatePosIntOrEmpty(parent, m_byoyomiMoves,
                                    "Invalid time settings"))
            return false;
        if (m_byoyomi.isEmpty() != isEmpty(m_byoyomiMoves))
        {
            SimpleDialogs.showError(parent, "Invalid byoyomi settings");
            return false;
        }
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
            if (content.trim().equals(""))
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

class TimeField
    extends JPanel
{
    public TimeField()
    {
        m_textField = new JTextField(2);
        m_textField.setHorizontalAlignment(JTextField.RIGHT);
        add(m_textField);
        String[] units = { "min", "sec" };
        m_comboBox = new JComboBox(units);
        add(m_comboBox);
    }

    public boolean isEmpty()
    {
        return m_textField.getText().trim().equals("");
    }

    public long getTime()
    {
        try
        {
            long units;
            if (m_comboBox.getSelectedItem().equals("min"))
                units = 60000;
            else
                units = 1000;
            return units * Long.parseLong(m_textField.getText());
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    public void setTime(long millis)
    {
        long seconds = millis / 1000L;
        if (seconds % 60 == 0)
        {
            m_textField.setText(Long.toString(seconds / 60L));
            m_comboBox.setSelectedItem("min");
        }
        else
        {
            m_textField.setText(Long.toString(seconds));
            m_comboBox.setSelectedItem("sec");
        }
    }

    public boolean validateTime(Component parent)
    {
        try
        {
            if (isEmpty())
                return true;
            int value = Integer.parseInt(m_textField.getText());
            if (value <= 0)
                return false;
        }
        catch (NumberFormatException e)
        {
            SimpleDialogs.showError(parent, "Invalid time");
            return false;
        }
        return true;
    }

    private JTextField m_textField;

    private JComboBox m_comboBox;
}
