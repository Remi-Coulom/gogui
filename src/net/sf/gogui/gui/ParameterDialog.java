//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.NoSuchElementException;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.UNINITIALIZED_VALUE;
import static javax.swing.JOptionPane.VALUE_PROPERTY;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.sf.gogui.gtp.GtpError;
import static net.sf.gogui.gui.GuiUtil.SMALL_PAD;
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
        final ArrayList<Parameter> parameters = parseResponse(response);
        Component mainComponent = createMainComponent(parameters);
        final Object options[] = { "Ok", "Cancel" };
        final JOptionPane optionPane =
            new JOptionPane(mainComponent, PLAIN_MESSAGE, OK_CANCEL_OPTION,
                            null, options, options[0]);
        final JDialog dialog = new JDialog(owner, title, true);
        dialog.setContentPane(optionPane);

        // not automatically double-buffered on Linux Java 1.5 (?)
        optionPane.setDoubleBuffered(true);

        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    String prop = event.getPropertyName();
                    if (dialog.isVisible() && event.getSource() == optionPane
                        && prop.equals(VALUE_PROPERTY))
                    {
                        Object value = optionPane.getValue();
                        if (ObjectUtil.equals(value, UNINITIALIZED_VALUE))
                            return;
                        if (ObjectUtil.equals(value, options[0]))
                        {
                            for (int i = 0; i < parameters.size(); ++i)
                            {
                                Parameter parameter = parameters.get(i);
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
                                    optionPane.setValue(UNINITIALIZED_VALUE);
                                    parameter.getComponent().requestFocus();
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

        public abstract Component getComponent();

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
                m_bool = (Integer.parseInt(value) != 0);
            }
            catch (NumberFormatException e)
            {
                m_bool = false;
            }
            m_panel =
                new JPanel(new FlowLayout(FlowLayout.LEFT, SMALL_PAD, 0));
            m_checkBox = new JCheckBox(getLabel(), m_bool);
            m_panel.add(m_checkBox);
        }

        public String getNewValue()
        {
            if (m_checkBox.isSelected())
                return "1";
            return "0";
        }

        public boolean isChanged()
        {
            return m_checkBox.isSelected() != m_bool;
        }

        public Component getComponent()
        {
            return m_panel;
        }

        private boolean m_bool;

        private final JCheckBox m_checkBox;

        private final JPanel m_panel;
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
            String[] labels = new String[args.length - 1];
            m_initialIndex = 0;
            int maxLength = 0;
            for (int i = 1; i < args.length; ++i)
            {
                String item = args[i];
                if (item.equals(value))
                    m_initialIndex = i - 1;
                maxLength = max(item.length(), maxLength);
                m_items[i - 1] = item;
                labels[i - 1] = StringUtil.capitalize(item.replace('_', ' '));
            }
            m_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                                                SMALL_PAD, SMALL_PAD));
            m_panel.add(new JLabel(getLabel() + ":"));
            m_comboBox = new JComboBox(labels);
            if (maxLength < TEXTFIELD_LEN)
            {
                String protoType = "X";
                while (protoType.length() < TEXTFIELD_LEN)
                    protoType = protoType + "X";
                m_comboBox.setPrototypeDisplayValue(protoType);
            }
            m_comboBox.setSelectedIndex(m_initialIndex);
            m_panel.add(m_comboBox);


        }

        public String getNewValue()
        {
            return m_items[m_comboBox.getSelectedIndex()];
        }

        public boolean isChanged()
        {
            return (m_comboBox.getSelectedIndex() != m_initialIndex);
        }

        public Component getComponent()
        {
            return m_panel;
        }

        private int m_initialIndex;

        private String[] m_items;

        private JPanel m_panel;

        private JComboBox m_comboBox;
    }

    private static class StringParameter
        extends Parameter
    {
        public StringParameter(String key, String value)
        {
            super(key, value);
            m_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT,
                                                SMALL_PAD, SMALL_PAD));
            m_panel.add(new JLabel(getLabel() + ":"));
            m_textField = new JTextField(TEXTFIELD_LEN);
            m_textField.setText(value);
            m_panel.add(m_textField);
        }

        public String getNewValue()
        {
            return m_textField.getText().trim();
        }

        public boolean isChanged()
        {
            return ! getNewValue().equals(getValue());
        }

        public Component getComponent()
        {
            return m_panel;
        }

        private final JTextField m_textField;

        private final JPanel m_panel;
    }

    private static ArrayList<Parameter> parseResponse(String response)
    {
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        BufferedReader reader =
            new BufferedReader(new StringReader(response));
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
            Scanner scanner = new Scanner(line);
            String type;
            try
            {
                type = scanner.next("^\\[[^\\]]*\\]");
                line = line.substring(type.length()).trim();
                type = type.substring(1, type.length() - 1);
            }
            catch (NoSuchElementException e)
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

    private static Component
        createMainComponent(ArrayList<Parameter> parameters)
    {
        int numberParameters = parameters.size();
        Box outerBox = Box.createHorizontalBox();
        int i = 0;
        int numberColumns = 0;
        Box box = null;
        while (i < numberParameters)
        {
            if (i % 30 == 0)
            {
                if (box != null)
                {
                    if (numberColumns > 0)
                    {
                        outerBox.add(GuiUtil.createFiller());
                        outerBox.add(new JSeparator(SwingConstants.VERTICAL));
                        outerBox.add(GuiUtil.createFiller());
                    }
                    outerBox.add(box);
                    ++numberColumns;
                }
                box = Box.createVerticalBox();
            }
            box.add((parameters.get(i)).getComponent());
            ++i;
        }
        if (box != null)
        {
            if (numberColumns > 0)
            {
                outerBox.add(GuiUtil.createFiller());
                outerBox.add(new JSeparator(SwingConstants.VERTICAL));
                outerBox.add(GuiUtil.createFiller());
            }
            outerBox.add(box);
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
