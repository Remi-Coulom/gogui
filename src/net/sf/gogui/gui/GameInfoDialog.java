// GameInfoDialog.java

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
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import net.sf.gogui.go.InvalidKomiException;
import net.sf.gogui.go.Komi;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.game.GameInfo;
import net.sf.gogui.game.StringInfo;
import net.sf.gogui.game.StringInfoColor;
import net.sf.gogui.game.TimeSettings;

/** Dialog for editing game settings and other information. */
public final class GameInfoDialog
    extends JOptionPane
{
    public static void show(Component parent, GameInfo info,
                            MessageDialogs messageDialogs)
    {
        GameInfoDialog gameInfo = new GameInfoDialog(info);
        JDialog dialog = gameInfo.createDialog(parent, i18n("TIT_GAMEINFO"));
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

    private TimeField m_byoyomi;

    private JTextField m_byoyomiMoves;

    private final JTextField m_date;

    private final JTextField m_komi;

    private final PlayerInfo m_black;

    private final PlayerInfo m_white;

    private TimeField m_preByoyomi;

    private final JTextField m_result;

    private final JTextField m_rules;

    private GameInfoDialog(GameInfo info)
    {
        Box outerBox = Box.createVerticalBox();
        m_white = createPlayerInfo(WHITE, info);
        m_white.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerBox.add(m_white.m_box);
        outerBox.add(GuiUtil.createFiller());
        m_black = createPlayerInfo(BLACK, info);
        m_black.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerBox.add(m_black.m_box);
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
        m_result = createEntry("LB_GAMEINFO_RESULT", 12,
                               info.get(StringInfo.RESULT),
                               "TT_GAMEINFO_RESULT", labels, values);
        m_date = createEntry("LB_GAMEINFO_DATE", 12,
                             info.get(StringInfo.DATE),
                             "TT_GAMEINFO_DATE", labels, values);
        m_rules = createEntry("LB_GAMEINFO_RULES", 12,
                              info.get(StringInfo.RULES),
                              "TT_GAMEINFO_RULES", labels, values);
        String komi = "";
        if (info.getKomi() != null)
            komi = info.getKomi().toString();
        m_komi = createEntry("LB_GAMEINFO_KOMI", 12, komi,
                             "TT_GAMEINFO_KOMI",
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
        JLabel label = new JLabel(i18n(labelText));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        boxLabel.add(label);
        labels.add(boxLabel);
        JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JTextField field = new JTextField(cols);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setToolTipText(i18n(toolTipText));
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
        JLabel label = new JLabel(i18n("LB_GAMEINFO_TIME"));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        boxLabel.add(label);
        labels.add(boxLabel);
        Box boxValue = Box.createVerticalBox();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        boxValue.add(Box.createVerticalGlue());
        boxValue.add(panel);
        boxValue.add(Box.createVerticalGlue());
        m_preByoyomi = new TimeField(3, "TT_GAMEINFO_TIME_MAIN");
        if (timeSettings != null)
            m_preByoyomi.setTime(timeSettings.getPreByoyomi());
        panel.add(m_preByoyomi);
        panel.add(new JLabel(" + "));
        m_byoyomi = new TimeField(2, "TT_GAMEINFO_TIME_BYOYOMI");
        if (timeSettings != null && timeSettings.getUseByoyomi())
            m_byoyomi.setTime(timeSettings.getByoyomi());
        panel.add(m_byoyomi);
        panel.add(new JLabel(" / "));
        m_byoyomiMoves = new JTextField(2);
        m_byoyomiMoves.setToolTipText(i18n("TT_GAMEINFO_TIME_BYOYOMI_MOVES"));
        m_byoyomiMoves.setHorizontalAlignment(JTextField.RIGHT);
        if (timeSettings != null && timeSettings.getUseByoyomi())
        {
            int byoyomiMoves = timeSettings.getByoyomiMoves();
            m_byoyomiMoves.setText(Integer.toString(byoyomiMoves));
        }
        panel.add(m_byoyomiMoves);
        panel.add(new JLabel(" " + i18n("LB_GAMEINFO_TIME_MOVES")));
        values.add(boxValue);
    }

    private PlayerInfo createPlayerInfo(GoColor c, GameInfo info)
    {
        assert c.isBlackWhite();
        PlayerInfo playerInfo = new PlayerInfo();
        Box box = Box.createHorizontalBox();
        JLabel label;
        if (c == BLACK)
            label = new JLabel(GuiUtil.getIcon("gogui-black-16x16",
                                               i18n("LB_BLACK")));
        else
            label = new JLabel(GuiUtil.getIcon("gogui-white-16x16",
                                               i18n("LB_WHITE")));
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        box.add(label);
        box.add(GuiUtil.createFiller());
        playerInfo.m_box = box;
        playerInfo.m_name = new JTextField(18);
        playerInfo.m_name.setText(info.get(StringInfoColor.NAME, c));
        box.add(playerInfo.m_name);
        playerInfo.m_name.setHorizontalAlignment(JTextField.CENTER);
        if (c == BLACK)
            playerInfo.m_name.setToolTipText(i18n("TT_GAMEINFO_NAME_BLACK"));
        else
            playerInfo.m_name.setToolTipText(i18n("TT_GAMEINFO_NAME_WHITE"));
        box.add(GuiUtil.createFiller());
        playerInfo.m_rank = new JTextField(5);
        playerInfo.m_rank.setHorizontalAlignment(JTextField.CENTER);
        if (c == BLACK)
            playerInfo.m_rank.setToolTipText(i18n("TT_GAMEINFO_RANK_BLACK"));
        else
            playerInfo.m_rank.setToolTipText(i18n("TT_GAMEINFO_RANK_WHITE"));
        box.add(playerInfo.m_rank);
        playerInfo.m_rank.setText(info.get(StringInfoColor.RANK, c));
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

    private void updateGameInfo(GameInfo info)
    {
        info.set(StringInfoColor.NAME, BLACK,
                 getTextFieldContent(m_black.m_name));
        info.set(StringInfoColor.NAME, WHITE,
                 getTextFieldContent(m_white.m_name));
        info.set(StringInfoColor.RANK, BLACK,
                 getTextFieldContent(m_black.m_rank));
        info.set(StringInfoColor.RANK, WHITE,
                 getTextFieldContent(m_white.m_rank));
        info.set(StringInfo.RULES, getTextFieldContent(m_rules));
        info.set(StringInfo.RESULT, getTextFieldContent(m_result));
        info.set(StringInfo.DATE, getTextFieldContent(m_date));
        String komiText = getTextFieldContent(m_komi);
        Komi komi = null;
        try
        {
            komi = Komi.parseKomi(komiText);
        }
        catch (InvalidKomiException e)
        {
            assert false; // already validated
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
                byoyomiMoves =
                    Integer.parseInt(getTextFieldContent(m_byoyomiMoves));
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
                                    "MSG_GAMEINFO_INVALID_TIME",
                                    messageDialogs))
            return false;
        if (m_byoyomi.isEmpty() != isEmpty(m_byoyomiMoves))
        {
            messageDialogs.showError(parent,
                                     i18n("MSG_GAMEINFO_INVALID_BYOYOMI"),
                                     i18n("MSG_GAMEINFO_INVALID_BYOYOMI_2"),
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
        catch (InvalidKomiException e)
        {
            messageDialogs.showError(parent,
                                     i18n("MSG_GAMEINFO_INVALID_KOMI"),
                                     i18n("MSG_GAMEINFO_INVALID_KOMI_2"),
                                     false);
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
                messageDialogs.showError(parent, i18n(errorMessage),
                    i18n("MSG_GAMEINFO_NO_POSITIVE_NUMBER"),
                    false);
                return false;
            }
        }
        catch (NumberFormatException e)
        {
            messageDialogs.showError(parent, i18n(errorMessage),
                                     i18n("MSG_GAMEINFO_NO_NUMBER"),
                                     false);
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
        m_textField.setToolTipText(i18n(toolTipText));
        panel.add(m_textField);
        panel.add(GuiUtil.createSmallFiller());
        String[] units = { i18n("LB_GAMEINFO_MIN"),
                           i18n("LB_GAMEINFO_SEC") };
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
            if (m_comboBox.getSelectedIndex() == 0)
                units = 60000;
            else
                units = 1000;
            return units * Long.parseLong(m_textField.getText().trim());
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
            m_comboBox.setSelectedIndex(0);
        }
        else
        {
            m_textField.setText(Long.toString(seconds));
            m_comboBox.setSelectedIndex(1);
        }
    }

    public boolean validateTime(Component parent,
                                MessageDialogs messageDialogs)
    {
        try
        {
            if (isEmpty())
                return true;
            int value = Integer.parseInt(m_textField.getText().trim());
            if (value < 0)
            {
                messageDialogs.showError(parent,
                                      i18n("MSG_GAMEINFO_INVALID_TIME"),
                                      i18n("MSG_GAMEINFO_NO_POSITIVE_NUMBER"),
                                      false);
                return false;
            }
        }
        catch (NumberFormatException e)
        {
                messageDialogs.showError(parent,
                                         i18n("MSG_GAMEINFO_INVALID_TIME"),
                                         i18n("MSG_GAMEINFO_NO_NUMBER"),
                                         false);
            return false;
        }
        return true;
    }

    private final JTextField m_textField;

    private final JComboBox m_comboBox;
}
