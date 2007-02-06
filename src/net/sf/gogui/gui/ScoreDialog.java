//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.CountScore;
import net.sf.gogui.go.Komi;
import net.sf.gogui.go.Score;

/** Dialog for displaying the game score and result. */
public class ScoreDialog
    extends JDialog
{
    public interface Listener
    {
        void actionScoreDone(Score score);
    }

    public ScoreDialog(Frame owner, final Listener listener, int initialRules)
    {
        super(owner, "Score");
        m_initialRules = initialRules;
        WindowAdapter windowAdapter = new WindowAdapter() {
                public void windowClosing(WindowEvent event) {
                    close();
                }
            };
        addWindowListener(windowAdapter);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        Box outerBox = Box.createVerticalBox();

        Box box = Box.createHorizontalBox();
        outerBox.add(box);
        JPanel labels = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        box.add(labels);
        box.add(GuiUtil.createSmallFiller());
        JPanel values = new JPanel(new GridLayout(0, 1, 0, GuiUtil.PAD));
        box.add(values);        

        String toolTipTerritory = "Points surrounded by %c";
        m_territory = createColorEntry("Territory", 3, toolTipTerritory,
                                       labels, values);
        String toolTypPrisoners =
            "Stones captured by %c (including dead stones on board)";
        m_prisoners = createColorEntry("Prisoners", 3, toolTypPrisoners,
                                       labels, values);
        String toolTipArea =
            "Points owned by %c (surrounded points and border stones)";
        m_area = createColorEntry("Area", 3, toolTipArea, labels, values);
        m_komi = createKomiEntry(3, labels, values);
        m_resultArea = createEntry("Result Area", 8,
                                      "Area score (area and komi)",
                                      labels, values);
        m_resultTerritory = createEntry("Result Territory", 8,
                                       "Territory score " +
                                       "(territory, prisoners, and komi)",
                                       labels, values);
        createRulesEntry(labels, values);
        m_result = createEntry("Result", 8, "Score used for game result",
                               labels, values);

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.actionScoreDone(m_score);
            } });
        m_cancelButton = new JButton("Cancel");
        m_cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    listener.actionScoreDone(null);
            } });
        Object options[] = { okButton, m_cancelButton };
        JOptionPane optionPane = new JOptionPane(outerBox,
                                                 JOptionPane.PLAIN_MESSAGE,
                                                 JOptionPane.OK_CANCEL_OPTION,
                                                 null, options, options[0]);
        setContentPane(optionPane);
        pack();
    }

    public void showScore(CountScore countScore, Komi komi)
    {
        int rules = m_initialRules;
        if (m_score != null)
            rules = m_score.m_rules;
        m_score = countScore.getScore(komi, rules);
        showScore();
    }

    private static class ColorFields
    {
        JTextField m_black;

        JTextField m_white;
    }

    private int m_initialRules;

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private final JButton m_cancelButton;

    private final ColorFields m_territory;

    private final ColorFields m_area;

    private final ColorFields m_prisoners;

    private final JTextField m_komi;

    private final JTextField m_resultArea;

    private final JTextField m_resultTerritory;

    private JComboBox m_rules;

    private final JTextField m_result;

    private Score m_score;

    private int m_boardRules;

    private static final ImageIcon m_iconBlack =
        GuiUtil.getIcon("gogui-black-16x16", "Black");

    private static final ImageIcon m_iconWhite =
        GuiUtil.getIcon("gogui-white-16x16", "White");

    private void close()
    {
        m_cancelButton.doClick();
        dispose();
    }

    private JComponent createEntryLabel(String text)
    {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JLabel label = new JLabel(text + ":");
        label.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(label);
        return panel;
    }

    private JTextField createEntry(String labelText, int cols, String toolTip,
                                   JComponent labels, JComponent values)
    {
        labels.add(createEntryLabel(labelText));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JTextField field = new JTextField(cols);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setToolTipText(toolTip);
        panel.add(field);
        values.add(panel);
        return field;
    }

    private ColorFields createColorEntry(String labelText, int cols,
                                         String toolTip,
                                         JComponent labels,
                                         JComponent values)
    {
        labels.add(createEntryLabel(labelText));
        ColorFields colorFields = new ColorFields();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.add(new JLabel(m_iconBlack));
        panel.add(GuiUtil.createSmallFiller());
        JTextField black = new JTextField(cols);
        black.setHorizontalAlignment(JTextField.CENTER);
        colorFields.m_black = black;
        black.setEditable(false);
        if (toolTip != null)
            black.setToolTipText(toolTip.replaceAll("%c", "Black"));
        panel.add(black);
        panel.add(GuiUtil.createFiller());
        panel.add(new JLabel(m_iconWhite));
        panel.add(GuiUtil.createSmallFiller());
        JTextField white = new JTextField(cols);
        white.setHorizontalAlignment(JTextField.CENTER);
        colorFields.m_white = white;
        white.setEditable(false);
        if (toolTip != null)
            white.setToolTipText(toolTip.replaceAll("%c", "White"));
        panel.add(white);
        values.add(panel);
        return colorFields;
    }

    private JTextField createKomiEntry(int cols, JComponent labels,
                                       JComponent values)
    {
        labels.add(createEntryLabel("Komi"));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.add(new JLabel(m_iconWhite));
        panel.add(GuiUtil.createSmallFiller());
        JTextField field = new JTextField(cols);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setEditable(false);
        field.setToolTipText("Komi value (compensation for Black's first " +
                             "move advantage)");
        panel.add(field);
        values.add(panel);
        return field;
    }

    private void createRulesEntry(JComponent labels, JComponent values)
    {
        labels.add(createEntryLabel("Scoring method"));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        Object[] options = { "Area", "Territory" };
        m_rules = new JComboBox(options);
        m_rules.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (m_score != null)
                    {
                        if (m_rules.getSelectedItem().equals("Territory"))
                            m_score.updateRules(Score.TERRITORY);
                        else
                            m_score.updateRules(Score.AREA);
                        showScore();
                    }
                }
            });
        m_rules.setToolTipText("Scoring method used for game result");
        panel.add(m_rules);
        values.add(panel);
    }

    private static void setTextInteger(JTextField field, int value)
    {
        field.setText(Integer.toString(value));
    }

    private void showScore()
    {
        if (m_score == null)
            return;
        setTextInteger(m_territory.m_black, m_score.m_territoryBlack);
        setTextInteger(m_territory.m_white, m_score.m_territoryWhite);
        setTextInteger(m_area.m_black, m_score.m_areaBlack);
        setTextInteger(m_area.m_white, m_score.m_areaWhite);
        setTextInteger(m_prisoners.m_black, m_score.m_capturedWhite);
        setTextInteger(m_prisoners.m_white, m_score.m_capturedBlack);
        if (m_score.m_komi != null)
            m_komi.setText(m_score.m_komi.toString());
        m_resultArea.setText(Score.formatResult(m_score.m_resultArea));
        m_resultTerritory.setText(Score.formatResult(m_score.m_resultTerritory));
        m_rules.setSelectedItem(m_score.m_rules == Score.TERRITORY ?
                                "Territory" : "Area");
        m_result.setText(m_score.formatResult());
    }
}

