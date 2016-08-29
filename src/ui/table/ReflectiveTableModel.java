package ui.table;

import java.awt.Component;
import java.awt.Font;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import ui.table.Column.EditPredicate;

public class ReflectiveTableModel<T> extends AbstractTableModel {

  private final List<T> rows;
  private final List<Field> columnFields;

  public ReflectiveTableModel(Class<? extends T> rowClass, List<T> rows) {
    this.rows = rows;
    columnFields = new ArrayList<>();
    for (Field f : rowClass.getDeclaredFields()) {
      if (f.isAnnotationPresent(Column.class)) {
        columnFields.add(f);
      }
    }
    for (Field f : columnFields) {
      f.setAccessible(true);
    }
  }

  private static class FormattingCellRenderer extends DefaultTableCellRenderer {
    final String format;
    final boolean bold;

    public FormattingCellRenderer(String format, boolean bold) {
      this.format = format;
      this.bold = bold;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
      
      if (value != null) {
        value = String.format(format, value);
      }

      JLabel comp = (JLabel) super.getTableCellRendererComponent(
          table, value, isSelected, hasFocus, row, column);
      
      if (bold) {
        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
      }
      return comp;
    }
  }

  public void setSettings(JTable table) {

    int sortColumn = -1;
    SortOrder sortOrder = null;
    
    for (int i = 0; i < columnFields.size(); i++) {

      TableColumn tableColumn = table.getColumnModel().getColumn(i);

      Field f = columnFields.get(i);
      Column c = f.getAnnotation(Column.class);

      FormattingCellRenderer fcr;
      if (c == null) {
        fcr = new FormattingCellRenderer(Column.DEFAULT_FORMAT, false);
      } else {
        fcr = new FormattingCellRenderer(c.format(), c.bold());
      }

      if (f.getType().isPrimitive() || Number.class.isAssignableFrom(f.getType())) {
        fcr.setHorizontalAlignment(SwingConstants.RIGHT);
      }
      tableColumn.setCellRenderer(fcr);

      if (c != null) {
        if (c.width() > 0) {
          tableColumn.setPreferredWidth(c.width());
        }

        if (c.tableCellEditor() != Column.DEFAULT_TABLE_CELL_EDITOR) {
          try {
            tableColumn.setCellEditor(c.tableCellEditor().newInstance());
          } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
        
        if (c.sort() != SortOrder.UNSORTED) {
          sortColumn = i;
          sortOrder = c.sort();
        }
        
      }
    }

    if (sortColumn != -1) {
      table.getRowSorter().setSortKeys(Arrays.asList(new SortKey(sortColumn, sortOrder)));
    }
  }

  @Override
  public int getRowCount() {
    return rows.size();
  }

  @Override
  public int getColumnCount() {
    return columnFields.size();
  }
 
  public String getColumnName(int column) {
    Field f = columnFields.get(column);
    Column c = f.getAnnotation(Column.class);
    if (c == null || c.header().isEmpty()) {
      String name = f.getName();
      String capitalizedFirst = name.substring(0, 1).toUpperCase();
      return capitalizedFirst + name.substring(1).replaceAll("([A-Z])", " $1");
    } else {
      return c.header();
    }
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return columnFields.get(columnIndex).getType();
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    Field f = columnFields.get(columnIndex);
    Column c = f.getAnnotation(Column.class);

    if (c == null) {
      return false;
    } else {
      if (c.editPredicate() == EditPredicate.class) {
        return c.tableCellEditor() != Column.DEFAULT_TABLE_CELL_EDITOR;
      } else {
        try {
          EditPredicate editPredicate = c.editPredicate().newInstance();
          return editPredicate.cellEditable(rows.get(rowIndex));
        } catch (Exception e) {
          e.printStackTrace();
          return false;
        }
      }
    }
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    try {
      return columnFields.get(columnIndex).get(rows.get(rowIndex));
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setValueAt(Object value, int row, int column) {
    try {
      columnFields.get(column).set(rows.get(row), value);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    fireTableCellUpdated(row, column);
  }

  public List<T> getRows() {
    return rows;
  }
}