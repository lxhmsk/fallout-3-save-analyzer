package ui.table;

import java.util.List;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

public class TableFactory {

  public interface Filter<T> {
    boolean include(T t);
  }

  public static <T> JTable makeTable(Class<T> rowClass, List<T> rows, Filter<T> filter) {
    
    ReflectiveTableModel<T> reflectiveTableModel = new ReflectiveTableModel<T>(rowClass, rows);

    JTable table = new JTable(reflectiveTableModel);

    TableRowSorter<ReflectiveTableModel<T>> tableRowSorter =
        new TableRowSorter<>(reflectiveTableModel);
 
    tableRowSorter.setRowFilter(new RowFilter<ReflectiveTableModel<T>, Integer>() {
      @Override
      public boolean include(
          RowFilter.Entry<? extends ReflectiveTableModel<T>, ? extends Integer> entry) {
        return filter.include(rows.get(entry.getIdentifier()));
      }
    });

    table.setRowSorter(tableRowSorter);

    reflectiveTableModel.setSettings(table);

    return table;
  }
}
