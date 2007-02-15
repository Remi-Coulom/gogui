//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.Komi;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.game.TimeSettings;

/** Dialog for editing game settings and other information. */
public final class GameInfoDialog
    extends JOptionPane
{
    public static void show(Component parent, GameInformation info,
                            MessageDialogs messageDialogs)
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
            done = gameInfo.validate(parent, messageDialogs);
        }
        dialog.dispose();
        gameInfo.updateGameInfo(info);
    }

    private static class PlayerInfo
    {
        public Box m_box;

        public JTextField m_name;
        
        public JTextField m_rank;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private TimeField m_byoyomi;

    private JTextField m_byoyomiMoves;

    private JTextField m_date;

    private JTextField m_komi;

    private PlayerInfo m_black;

    private PlayerInfo m_white;

    private TimeField m_preByoyomi;

    private JTextField m_result;

    private JTextField m_rules;

    private GameInfoDialog(GameInformation info)
    {
        Box outerBox = Box.createVerticalBox();
        m_black = createPlayerInfo(GoColor.BLACK, "gogui-black-16x16",
                                   "Black", info);
        m_black.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerBox.add(m_black.m_box);
        outerBox.add(GuiUtil.createFiller());
        m_white = createPlayerInfo(GoColor.WHITE, "gogui-white-16x16",
                                   "White", info);
        m_white.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerBox.add(m_white.m_box);
        outerBox.add(GuiUtil.createFiller());
        outerBox.add(GuiUtil.createFiller());
        Box box = Box.createHorizontalBox();
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerBox.add(box);
        JPanel labels =
            new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        box.add(labels);
        box.add(GuiUtil.createSmallFiller());
        JPanel values =
            new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        box.add(values);
        m_result = createEntry("Result", 12, info.getResult(),
                               "Result of the game", labels, values);
        m_date = createEntry("Date", 12, info.getDate(),
                             "Date when the game was played", labels, values);
        m_rules = createEntry("Rules", 12, info.getRules(),
                              "Used rules for the game", labels, values);
        String komi = "";
        if (info.getKomi() != null)
            komi = info.getKomi().toString();
        m_komi = createEntry("Komi", 12, komi,
                             "Komi value (compensation for Black's first " +
                             "move advantage)",
                             labels, values);
        createTime(info.getTimeSettings(), labels, values);

        setMessage(outerBox);
        setOptionType(OK_CANCEL_OPTION);
    }

    private JTextField createEntry(String labelText, int cols, String text,
                                   String toolTipText, JComponent labels,
                                   JComponent values)
    {
        Box boxLabel = Box.createHorizontalBox();
        boxLabel.add(Box.createHorizontalGlue());
        JLabel label = new JLabel(labelText + ":");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        boxLabel.add(label);
        labels.add(boxLabel);
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JTextField field = new JTextField(cols);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setToolTipText(toolTipText);
        field.setText(text);
        fieldPanel.add(field);
        values.add(fieldPanel);
        return field;
    }

    private void createTime(TimeSettings timeSettings, JComponent labels,
                            JComponent values)
    {
        Box boxLabel = Box.createHorizontalBox();
        boxLabel.add(Box.createHorizontalGlue());
        JLabel label = new JLabel("Time:");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        boxLabel.add(label);
        labels.add(boxLabel);
        Box boxValue = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boxValue.add(Box.createVerticalGlue());
        boxValue.add(panel);
        boxValue.add(Box.createVerticalGlue());
        m_preByoyomi = new TimeField(3, "Main time");
        if (timeSettings != null)
            m_preByoyomi.setTime(timeSettings.getPreByoyomi());
        panel.add(m_preByoyomi);
        panel.add(new JLabel(" + "));
        m_byoyomi = new TimeField(2, "Byoyomi (overtime) period");
        if (timeSettings != null && timeSettings.getUseByoyomi())
            m_byoyomi.setTime(timeSettings.getByoyomi());
        panel.add(m_byoyomi);
        panel.add(new JLabel(" / "));
        m_byoyomiMoves = new JTextField(2);
        m_byoyomiMoves.setToolTipText("Moves per byoyomi (overtime) period");
        m_byoyomiMoves.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomiMoves = timeSettings.getByoyomiMoves();
            m_byoyomiMoves.setText(Integer.toString(byoyomiMoves));
        }
        panel.add(m_byoyomiMoves);
        panel.add(new JLabel(" moves"));
        values.add(boxValue);
    }

    private PlayerInfo createPlayerInfo(GoColor c, String icon, String name,
                                        GameInformation info)
    {
        PlayerInfo playerInfo = new PlayerInfo();
        Box box = Box.createHorizontalBox();
        JLabel label = new JLabel(GuiUtil.getIcon(icon, name));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        box.add(label);
        box.add(GuiUtil.createFiller());
        playerInfo.m_box = box;
        playerInfo.m_name = new JTextField(18);
        playerInfo.m_name.setText(info.getPlayer(c));
        box.add(playerInfo.m_name);
        playerInfo.m_name.setHorizontalAlignment(JTextField.CENTER);
        playerInfo.m_name.setToolTipText(name + " player name");
        box.add(GuiUtil.createFiller());
        playerInfo.m_rank = new JTextField(5);
        playerInfo.m_rank.setHorizontalAlignment(JTextField.CENTER);
        playerInfo.m_rank.setToolTipText(name + " player rank");
        box.add(playerInfo.m_rank);
        playerInfo.m_rank.setText(info.getRank(c));
        box.setAlignmentY(Component.CENTER_ALIGNMENT);
        return playerInfo;
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
        info.setPlayer(GoColor.BLACK, getTextFieldContent(m_black.m_name));
        info.setPlayer(GoColor.WHITE, getTextFieldContent(m_white.m_name));
        info.setRank(GoColor.BLACK, getTextFieldContent(m_black.m_rank));
        info.setRank(GoColor.WHITE, getTextFieldContent(m_white.m_rank));
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

    private boolean validate(Component parent, MessageDialogs messageDialogs)
    {
        if (! validateKomi(parent, m_komi, messageDialogs))
            return false;
        if (! m_preByoyomi.validateTime(parent, messageDialogs))
            return false;
        if (! m_byoyomi.validateTime(parent, messageDialogs))
            return false;
        if (! validatePosIntOrEmpty(parent, m_byoyomiMoves,
                                    "Invalid time settings", messageDialogs))
            return false;
        if (m_byoyomi.isEmpty() != isEmpty(m_byoyomiMoves))
        {
            messageDialogs.showError(parent,
                                     "Invalid byoyomi settings",
                                     "You need to specify both the byoyomi "
                                     + "time period and the number of byoyomi "
                                     + "moves.",
                                     false);
            return false;
        }
        return true;
    }

    private boolean validateKomi(Component parent, JTextField textField,
                                 MessageDialogs messageDialogs)
    {
        String text = getTextFieldContent(textField);
        try
        {
            Komi.parseKomi(text);
        }
        catch (Komi.InvalidKomi e)
        {
            messageDialogs.showError(parent, "Invalid komi",
                                    "The komi has to be a number.", false);
            return false;
        }
        return true;
    }

    private boolean validatePosIntOrEmpty(Component parent,
                                          JTextField textField,
                                          String errorMessage,
                                          MessageDialogs messageDialogs)
    {
        try
        {
            String content = getTextFieldContent(textField);
            if (content.trim().equals(""))
                return true;
            int value = Integer.parseInt(content);
            if (value <= 0)
            {
                messageDialogs.showError(parent, errorMessage,
                                        "The entered value needs to be a " +
                                        "positive number.", false);
                return false;
            }
        }
        catch (NumberFormatException e)
        {
            messageDialogs.showError(parent, errorMessage,
                                    "The entered value needs to be a " +
                                    "number.", false);
            return false;
        }
        return true;
    }
}

class TimeField
    extends Box
{
    public TimeField(int cols, String toolTipText)
    {        
        super(BoxLayout.Y_AXIS);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(Box.createVerticalGlue());
        add(panel);
        add(Box.createVerticalGlue());
        m_textField = new JTextField(cols);
        m_textField.setHorizontalAlignment(JTextField.RIGHT);
        m_textField.setToolTipText(toolTipText);
        panel.add(m_textField);
        panel.add(GuiUtil.createSmallFiller());
        String[] units = { "min", "sec" };
        m_comboBox = new JComboBox(units);
        panel.add(m_comboBox);
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

    public boolean validateTime(Component parent,
                                MessageDialogs messageDialogs)
    {
        try
        {
            if (isEmpty())
                return true;
            int value = Integer.parseInt(m_textField.getText());
            if (value <= 0)
            {
                messageDialogs.showError(parent, "Invalid time",
                                        "The entered value needs to be a " +
                                        "positive number.", false);
                return false;
            }
        }
        catch (NumberFormatException e)
        {
            messageDialogs.showError(parent, "Invalid time",
                                    "The entered value needs to be a " +
                                    "number.", false);
            return false;
        }
        return true;
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private JTextField m_textField;

    private JComboBox m_comboBox;
}
