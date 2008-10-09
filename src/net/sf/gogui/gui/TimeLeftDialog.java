// TimeLeftDialog.java

package net.sf.gogui.gui;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import static net.sf.gogui.gui.I18n.i18n;
import net.sf.gogui.game.Clock;
import net.sf.gogui.game.ConstClock;
import net.sf.gogui.game.ConstGameInfo;
import net.sf.gogui.game.ConstNode;
import net.sf.gogui.game.Game;
import net.sf.gogui.game.NodeUtil;
import net.sf.gogui.game.TimeSettings;
import net.sf.gogui.go.GoColor;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.go.GoColor.BLACK_WHITE;

public final class TimeLeftDialog
    extends JOptionPane
{
    public static void show(Component parent, Game game, ConstNode node,
                            MessageDialogs messageDialogs)
    {
        ConstGameInfo info = game.getGameInfoNode(node).getGameInfoConst();
        Clock clock = null;
        TimeSettings timeSettings = info.getTimeSettings();
        if (timeSettings != null)
        {
            clock = new Clock();
            clock.setTimeSettings(timeSettings);
            NodeUtil.restoreClock(node, clock);
        }
        TimeLeftDialog timeLeftDialog = new TimeLeftDialog(clock);
        JDialog dialog = timeLeftDialog.createDialog(parent,
                                                     i18n("TIT_TIME_LEFT"));
        boolean done = false;
        while (! done)
        {
            dialog.setVisible(true);
            Object value = timeLeftDialog.getValue();
            if (! (value instanceof Integer)
                || ((Integer)value).intValue() != JOptionPane.OK_OPTION)
                return;
            done = timeLeftDialog.validate(parent, messageDialogs);
        }
        for (GoColor c : BLACK_WHITE)
        {
            long timeLeft = timeLeftDialog.getTimeLeft(c);
            game.setTimeLeft(node, c, timeLeft / 1000L);
            int movesLeft = timeLeftDialog.getMovesLeft(c);
            if (movesLeft >= 0)
                game.setMovesLeft(node, c, movesLeft);
            game.restoreClock();
        }
        dialog.dispose();
    }

    private static class PlayerTime
    {
        public Box m_box;

        public JTextField m_timeLeft;

        public JTextField m_movesLeft;
    }

    private final PlayerTime m_black;

    private final PlayerTime m_white;

    private TimeLeftDialog(ConstClock clock)
    {
        Box box = Box.createVerticalBox();
        m_white = createPlayerTime(WHITE, clock);
        m_white.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(m_white.m_box);
        box.add(GuiUtil.createFiller());
        m_black = createPlayerTime(BLACK, clock);
        m_black.m_box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.add(m_black.m_box);
        setMessage(box);
        setOptionType(OK_CANCEL_OPTION);
    }

    private PlayerTime createPlayerTime(GoColor c, ConstClock clock)
    {
        assert c.isBlackWhite();
        PlayerTime playerInfo = new PlayerTime();
        Box box = Box.createHorizontalBox();
        JLabel label;
        String tooltipTimeLeft;
        String tooltipMovesLeft;
        if (c == BLACK)
        {
            label = new JLabel(GuiUtil.getIcon("gogui-black-16x16",
                                               i18n("LB_BLACK")));
            tooltipTimeLeft = "TT_TIMELEFT_TIME_BLACK";
            tooltipMovesLeft = "TT_TIMELEFT_MOVES_BLACK";
        }
        else
        {
            label = new JLabel(GuiUtil.getIcon("gogui-white-16x16",
                                               i18n("LB_WHITE")));
            tooltipTimeLeft = "TT_TIMELEFT_TIME_WHITE";
            tooltipMovesLeft = "TT_TIMELEFT_MOVES_WHITE";
        }
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        box.add(label);
        box.add(GuiUtil.createFiller());
        playerInfo.m_box = box;
        playerInfo.m_timeLeft = new JTextField(9);
        playerInfo.m_timeLeft.setToolTipText(i18n(tooltipTimeLeft));
        box.add(playerInfo.m_timeLeft);
        playerInfo.m_timeLeft.setHorizontalAlignment(JTextField.CENTER);
        box.add(GuiUtil.createFiller());
        playerInfo.m_movesLeft = new JTextField(3);
        playerInfo.m_movesLeft.setHorizontalAlignment(JTextField.CENTER);
        playerInfo.m_movesLeft.setToolTipText(i18n(tooltipMovesLeft));
        box.add(playerInfo.m_movesLeft);
        if (clock != null)
        {
            String timeLeft =
                Clock.getTimeString(clock.getTimeLeft(c) / 1000L, -1);
            playerInfo.m_timeLeft.setText(timeLeft);
            if (clock.getUseByoyomi() && clock.isInByoyomi(c))
            {
                String movesLeft = Integer.toString(clock.getMovesLeft(c));
                playerInfo.m_movesLeft.setText(movesLeft);
            }
        }
        box.setAlignmentY(Component.CENTER_ALIGNMENT);
        return playerInfo;
    }

    private int getMovesLeft(GoColor c)
    {
        JTextField textField =
            (c == BLACK ? m_black.m_movesLeft : m_white.m_movesLeft);
        if (isEmpty(textField))
            return -1;
        return Integer.parseInt(getTextFieldContent(textField));
    }

    private static String getTextFieldContent(JTextField textField)
    {
        return textField.getText().trim();
    }

    private long getTimeLeft(GoColor c)
    {
        JTextField textField =
            (c == BLACK ? m_black.m_timeLeft : m_white.m_timeLeft);
        long timeLeft = Clock.parseTimeString(getTextFieldContent(textField));
        assert timeLeft >= 0;
        return timeLeft;
    }

    private boolean isEmpty(JTextField textField)
    {
        return getTextFieldContent(textField).equals("");
    }

    private boolean validate(Component parent, MessageDialogs messageDialogs)
    {
        return (validatePosIntOrEmpty(parent, m_black.m_movesLeft,
                                      "MSG_TIMELEFT_INVALID_MOVESLEFT",
                                      messageDialogs)
                && validatePosIntOrEmpty(parent, m_white.m_movesLeft,
                                         "MSG_TIMELEFT_INVALID_MOVESLEFT",
                                         messageDialogs)
                && validateTime(parent, m_black.m_timeLeft,
                                "MSG_TIMELEFT_INVALID_TIMELEFT",
                                messageDialogs)
                && validateTime(parent, m_white.m_timeLeft,
                                "MSG_TIMELEFT_INVALID_TIMELEFT",
                                messageDialogs));
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
            if (value < 0)
            {
                messageDialogs.showError(parent, i18n(errorMessage),
                    i18n("MSG_TIMELEFT_NEGATIVE_NUMBER"),
                    false);
                return false;
            }
        }
        catch (NumberFormatException e)
        {
            messageDialogs.showError(parent, i18n(errorMessage),
                                     i18n("MSG_TIMELEFT_NO_NUMBER"),
                                     false);
            return false;
        }
        return true;
    }

    private boolean validateTime(Component parent,
                                 JTextField textField,
                                 String errorMessage,
                                 MessageDialogs messageDialogs)
    {
        long timeLeft = Clock.parseTimeString(getTextFieldContent(textField));
        if (timeLeft < 0)
        {
            messageDialogs.showError(parent, i18n(errorMessage),
                                     i18n("MSG_TIMELEFT_NO_TIME"),
                                     false);
            return false;
        }
        return true;
    }
}
