//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.util.ObjectUtil;
import net.sf.gogui.util.StringUtil;

/** Dialog for editing parameters in response to an analyze command of type
    <i>param</i>.
*/
public class ParameterDialog
{
    public static void editParameters(final String paramCommand, Frame owner,
                                      String title, String response,
                                      final GuiGtpClient gtp,
                                      final MessageDialogs messageDialogs)
    {
        final ArrayList parameters = parseResponse(response);
        Component mainComponent = createMainComponent(parameters);
        final Object options[] = { "Ok", "Cancel" };
        final JOptionPane optionPane =
            new JOptionPane(mainComponent, JOptionPane.PLAIN_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION,
                            null, options, options[0]);
        final JDialog dialog = new JDialog(owner, title, true);
        dialog.setContentPane(optionPane);

        // not automatically double-buffered on Linux Java 1.5 (?)
        optionPane.setDoubleBuffered(true);

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    String prop = event.getPropertyName();
                    if (dialog.isVisible() && event.getSource() == optionPane
                        && prop.equals(JOptionPane.VALUE_PROPERTY))
                    {
                        Object value = optionPane.getValue();
                        if (ObjectUtil.equals(value,
                                              JOptionPane.UNINITIALIZED_VALUE))
                            return;
                        if (ObjectUtil.equals(value, options[0]))
                        {
                            for (int i = 0; i < parameters.size(); ++i)
                            {
                                Parameter parameter =
                                    (Parameter)parameters.get(i);
                                if (! parameter.isChanged())
                                    continue;
                                try
                                {
                                    String command =
                                        getNewValueCommand(paramCommand,
                                                           parameter);
                                    gtp.send(command);
                                }
                                catch (GtpError e)
                                {
                                    showError(dialog, messageDialogs,
                                              parameter, e);
                                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                                    return;
                                }
                            }
                        }
                        dialog.setVisible(false);
                    }
                }
            });
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** Length of a textfield for editing string parameters. */
    private static final int TEXTFIELD_LEN = 13;

    private abstract static class Parameter
    {
        public Parameter(String key, String value)
        {
            m_key = key;
            m_value = value;
            m_label = StringUtil.capitalize(key.replace('_', ' '));
        }

        public String getKey()
        {
            return m_key;
        }

        public String getLabel()
        {
            return m_label;
        }

        public String getValue()
        {
            return m_value;
        }

        public abstract String getNewValue();

        public abstract boolean isChanged();

        public abstract void createComponents(int gridy, JPanel panel,
                                              GridBagLayout gridbag);

        private final String m_key;

        private final String m_label;

        private final String m_value;
    }

    private static class BoolParameter
        extends Parameter
    {
        public BoolParameter(String key, String value)
        {
            super(key, value);
            try
            {
                m_initialValue = (Integer.parseInt(value) != 0);
            }
            catch (NumberFormatException e)
            {
                m_initialValue = false;
            }
        }

        public String getNewValue()
        {
            if (m_checkBox.isSelected())
                return "1";
            return "0";
        }

        public boolean isChanged()
        {
            return (m_checkBox.isSelected() != m_initialValue);
        }

        public void createComponents(int gridy, JPanel panel,
                                     GridBagLayout gridbag)
        {
            m_checkBox = new JCheckBox(getLabel(), m_initialValue);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = gridy;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.weightx = 1.0;
            constraints.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(m_checkBox, constraints);
            panel.add(m_checkBox);
        }

        private boolean m_initialValue;

        private JCheckBox m_checkBox;
    }

    private static class ListParameter
        extends Parameter
    {
        public ListParameter(String type, String key, String value)
        {
            super(key, value);
            String[] args = type.split("/");
            assert args[0].equals("list");
            m_items = new String[args.length - 1];
            m_labels = new String[args.length - 1];
            m_initialIndex = 0;
            int maxLength = 0;
            for (int i = 1; i < args.length; ++i)
            {
                String item = args[i];
                if (item.equals(value))
                    m_initialIndex = i - 1;
                maxLength = Math.max(item.length(), maxLength);
                m_items[i - 1] = item;
                m_labels[i - 1] =
                    StringUtil.capitalize(item.replace('_', ' '));
            }
        }

        public String getNewValue()
        {
            return m_items[m_comboBox.getSelectedIndex()];
        }

        public boolean isChanged()
        {
            return (m_comboBox.getSelectedIndex() != m_initialIndex);
        }

        public void createComponents(int gridy, JPanel panel,
                                     GridBagLayout gridbag)
        {
            JLabel label = new JLabel(getLabel() + ":");
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = gridy;
            constraints.weightx = 1.0;
            constraints.ipadx = GuiUtil.SMALL_PAD;
            constraints.insets = new Insets(GuiUtil.SMALL_PAD, 0, 0, 0);
            constraints.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(label, constraints);
            panel.add(label);

            m_comboBox = new JComboBox(m_labels);
            m_comboBox.setSelectedIndex(m_initialIndex);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = gridy;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(GuiUtil.SMALL_PAD, 0, 0, 0);
            constraints.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(m_comboBox, constraints);
            panel.add(m_comboBox);
        }

        private int m_initialIndex;

        private String[] m_items;

        private String[] m_labels;

        private JComboBox m_comboBox;
    }

    private static class StringParameter
        extends Parameter
    {
        public StringParameter(String key, String value)
        {
            super(key, value);
        }

        public String getNewValue()
        {
            return m_textField.getText().trim();
        }

        public boolean isChanged()
        {
            return ! getNewValue().equals(getValue());
        }

        public void createComponents(int gridy, JPanel panel,
                                     GridBagLayout gridbag)
        {
            JLabel label = new JLabel(getLabel() + ":");
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = gridy;
            constraints.weightx = 1.0;
            constraints.ipadx = GuiUtil.SMALL_PAD;
            constraints.insets = new Insets(GuiUtil.SMALL_PAD, 0, 0, 0);
            constraints.anchor = GridBagConstraints.EAST;
            gridbag.setConstraints(label, constraints);
            panel.add(label);

            m_textField = new JTextField(TEXTFIELD_LEN);
            m_textField.setText(getValue());
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = gridy;
            constraints.weightx = 1.0;
            constraints.insets = new Insets(GuiUtil.SMALL_PAD, 0, 0, 0);
            gridbag.setConstraints(m_textField, constraints);
            panel.add(m_textField);
        }

        private JTextField m_textField;
    }

    private static ArrayList parseResponse(String response)
    {
        ArrayList parameters = new ArrayList();
        BufferedReader reader =
            new BufferedReader(new StringReader(response));
        Pattern pattern = Pattern.compile("^\\[[^\\]]*\\]");
        while (true)
        {
            String line = null;
            try
            {
                line = reader.readLine();
            }
            catch (IOException e)
            {
            }
            if (line == null)
                break;
            line = line.trim();
            if (line.startsWith("[") && line.endsWith("]"))
            {
                // Might be used as label for grouping parameters on tabbing
                // panes in a later version of GoGui, so we silently accept it
                continue;
            }

            String type;
            Matcher matcher = pattern.matcher(line);
            if (matcher.find())
            {
                type = line.substring(1, matcher.end() - 1);
                line = line.substring(matcher.end()).trim();
            }
            else
            {
                // Treat unknown types as string for compatibiliy with future
                // types
                type = "string";
            }
            int pos = line.indexOf(' ');
            String key;
            String value;
            if (pos < 0)
            {
                key = line.trim();
                value = "";
            }
            else
            {
                key = line.substring(0, pos).trim();
                value = line.substring(pos + 1).trim();
            }
            if (type.equals("bool"))
                parameters.add(new BoolParameter(key, value));
            else if (type.startsWith("list/"))
                parameters.add(new ListParameter(type, key, value));
            else
                // Treat unknown types as string for compatibiliy with future
                // types
                parameters.add(new StringParameter(key, value));
        }
        return parameters;
    }

    private static Component createMainComponent(ArrayList parameters)
    {
        int numberParameters = parameters.size();
        Box outerBox = Box.createHorizontalBox();
        int i = 0;
        int numberColumns = 0;
        JPanel panel = null;
        GridBagLayout gridbag = null;
        int gridy = 0;
        while (i < numberParameters)
        {
            if (i % 30 == 0)
            {
                if (panel != null)
                {
                    if (numberColumns > 0)
                    {
                        outerBox.add(GuiUtil.createFiller());
                        outerBox.add(new JSeparator(SwingConstants.VERTICAL));
                        outerBox.add(GuiUtil.createFiller());
                    }
                    outerBox.add(panel);
                    ++numberColumns;
                }
                gridbag = new GridBagLayout();
                panel = new JPanel(gridbag);
                gridy = 0;
            }
            Parameter parameter = (Parameter)parameters.get(i);
            parameter.createComponents(gridy, panel, gridbag);
            ++gridy;
            ++i;
        }
        if (panel != null)
        {
            if (numberColumns > 0)
            {
                outerBox.add(GuiUtil.createFiller());
                outerBox.add(new JSeparator(SwingConstants.VERTICAL));
                outerBox.add(GuiUtil.createFiller());
            }
            outerBox.add(panel);
        }
        return outerBox;
    }

    private static String getNewValueCommand(String paramCommand,
                                             Parameter parameter)
    {
        String key = parameter.getKey();
        String newValue = parameter.getNewValue();
        return paramCommand + " " + key + " " + newValue;
    }

    private static void showError(JDialog owner, MessageDialogs messageDialogs,
                                  Parameter parameter, GtpError e)
    {
        String mainMessage =
            "Could not change parameter \"" + parameter.getLabel() + "\"";
        String optionalMessage = StringUtil.capitalize(e.getMessage());
        messageDialogs.showError(owner, mainMessage, optionalMessage);
    }
}
