package ui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SpinnerEditor extends DefaultCellEditor {

  protected final JSpinner spinner;
  protected final JSpinner.DefaultEditor editor;
  protected final JTextField textField;

  private boolean valueSet;

  public SpinnerEditor() {
    super(new JTextField());

    spinner = new JSpinner();
    editor = ((JSpinner.DefaultEditor) spinner.getEditor());

    textField = editor.getTextField();
    textField.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent fe) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (valueSet) {
              textField.setCaretPosition(1);
            }
          }
        });
      }

      public void focusLost(FocusEvent fe) {
      }
    });

    textField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        stopCellEditing();
      }
    });
  }
  
  // Prepares the spinner component and returns it.
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int row, int column) {
    clearError();
    if (!valueSet) {
      if (value == null) {
        value = 0;
      }
      spinner.setValue(value);
    }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        textField.requestFocus();
      }
    });
    return spinner;
  }

  public boolean isCellEditable(EventObject eo) {
    if (eo instanceof KeyEvent) {
      KeyEvent ke = (KeyEvent) eo;
      textField.setText(String.valueOf(ke.getKeyChar()));
      valueSet = true;
    } else {
      valueSet = false;
    }
    return true;
  }

  // Returns the spinners current value.
  public Object getCellEditorValue() {
    return spinner.getValue();
  }

  private void setError() {
    spinner.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
  }

  private void clearError() {
    spinner.setBorder(null);
  }

  public boolean stopCellEditing() {
    try {
      editor.commitEdit();
      spinner.commitEdit();
    } catch (java.text.ParseException e) {
      setError();
      return false;
    }
    boolean ret = super.stopCellEditing();
    if (!ret) {
      setError();
    }
    return ret;
  }
}