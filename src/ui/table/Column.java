package ui.table;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.swing.SortOrder;
import javax.swing.table.TableCellEditor;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
  
  public static final String DEFAULT_FORMAT = "%s";
  public static final Class<? extends TableCellEditor> DEFAULT_TABLE_CELL_EDITOR
      = TableCellEditor.class;

  public interface EditPredicate {
    boolean cellEditable(Object row);
  }
  
  String header() default "";
  int width() default -1;
  String format() default DEFAULT_FORMAT;
  boolean bold() default false;
  Class<? extends EditPredicate> editPredicate() default EditPredicate.class; 
  Class<? extends TableCellEditor> tableCellEditor() default TableCellEditor.class;
  SortOrder sort() default SortOrder.UNSORTED;
}