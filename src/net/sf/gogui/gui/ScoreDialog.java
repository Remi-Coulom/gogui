//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.game.GameInformation;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.Score;

//----------------------------------------------------------------------------

/** Dialog for displaying the game score and result. */
public class ScoreDialog
    extends JDialog
{
    public ScoreDialog(Frame owner, ActionListener listener)
    {
        super(owner, "Score");
        WindowAdapter windowAdapter = new WindowAdapter()
            {
                public void windowClosing(WindowEvent event)
                {
                    close();
                }
            };
        addWindowListener(windowAdapter);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        JPanel panelDetails =
            new JPanel(new GridLayout(0, 2, 0, GuiUtils.SMALL_PAD));
        m_territoryBlack = createEntry(panelDetails, "Territory Black:");
        m_territoryWhite = createEntry(panelDetails, "Territory White:");
        m_areaBlack = createEntry(panelDetails, "Area Black:");
        m_areaWhite = createEntry(panelDetails, "Area White:");
        m_capturedBlack = createEntry(panelDetails, "Captured Black:");
        m_capturedWhite = createEntry(panelDetails, "Captured White:");
        m_komi = createEntry(panelDetails, "Komi:");
        m_resultChinese = createEntry(panelDetails, "Result Chinese:");
        m_resultJapanese = createEntry(panelDetails, "Result Japanese:");
        m_rules = createEntry(panelDetails, "Rules:");
        JPanel panelResult = new JPanel(new GridLayout(0, 2, 0, 0));
        m_result = createEntry(panelResult, "Game result:");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(panelDetails);
        panel.add(GuiUtils.createFiller());
        panel.add(GuiUtils.createFiller());
        panel.add(panelResult);
        JButton okButton = new JButton("Ok");
        okButton.setActionCommand("score-done");
        okButton.addActionListener(listener);
        m_cancelButton = new JButton("Cancel");
        m_cancelButton.setActionCommand("score-cancel");
        m_cancelButton.addActionListener(listener);
        Object options[] = { okButton, m_cancelButton };
        JOptionPane optionPane = new JOptionPane(panel,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 null, options, options[0]);
        setContentPane(optionPane);
        pack();
    }

    public void showScore(Score score)
    {
        m_territoryBlack.setText(Integer.toString(score.m_territoryBlack));
        m_territoryWhite.setText(Integer.toString(score.m_territoryWhite));
        m_areaBlack.setText(Integer.toString(score.m_areaBlack));
        m_areaWhite.setText(Integer.toString(score.m_areaWhite));
        m_capturedBlack.setText(Integer.toString(score.m_capturedBlack));
        m_capturedWhite.setText(Integer.toString(score.m_capturedWhite));
        m_komi.setText(GameInformation.roundKomi(score.m_komi));
        m_resultChinese.setText(Score.formatResult(score.m_resultChinese));
        m_resultJapanese.setText(Score.formatResult(score.m_resultJapanese));
        m_rules.setText(score.m_rules == Board.RULES_JAPANESE ? "Japanese"
                        : "Chinese");
        m_result.setText(score.formatResult());
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JButton m_cancelButton;

    private final JTextField m_territoryBlack;

    private final JTextField m_territoryWhite;

    private final JTextField m_areaBlack;

    private final JTextField m_areaWhite;

    private final JTextField m_capturedBlack;

    private final JTextField m_capturedWhite;

    private final JTextField m_komi;

    private final JTextField m_resultChinese;

    private final JTextField m_resultJapanese;

    private final JTextField m_rules;

    private final JTextField m_result;

    private void close()
    {
        m_cancelButton.doClick();
        dispose();
    }

    private JTextField createEntry(JPanel panel, String text)
    {
        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        JTextField entry = new JTextField("        ");
        entry.setEditable(false);
        entry.setFocusable(false);
        entry.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(entry);
        return entry;
    }
}

//----------------------------------------------------------------------------
