package save;


public class FormIdTable {

  private final int formIdTableSize;
  private final int[] formIdTable;

  private FormIdTable(int formIdTableSize, int[] formIdTable) {
    this.formIdTableSize = formIdTableSize;
    this.formIdTable = formIdTable;
  }

  /**
   * Form id -> form id index or -1 if not in the table.
   */
  public FormIdIndex findFormIdIndexByFormId(int formId) {
    for (int i = 0; i < formIdTable.length; i++) {
      if (formIdTable[i] == formId) {
        return new FormIdIndex(i);
      }
    }
    return null;
  }

  /**
   * Form id index -> form id, or -1 if not in the table.
   */
  public int findFormIdByFormIdIndex(FormIdIndex formIdIndex) {

    // Upper 2 bits indicate the type of formID.
    // See http://www.uesp.net/wiki/Tes5Mod:Save_File_Format#FormID
    if (((formIdIndex.formIdIndex >> 22) & 0b11) == 0) {
      return formIdTable[formIdIndex.formIdIndex];
    } else {
      // Not in the form id table, it's either default or created.
      return -1;
    }
  }

  static FormIdTable load(Fo3ByteBuffer data, FileLocations fileLocations) {

    data.position(fileLocations.formIdTableAddress);

    int formIdTableSize = data.readInt(false);

    // Apparently all the form indexes in the save file are off by one?
    int[] formIdTable = new int[formIdTableSize + 1];

    for (int i = 0; i < formIdTableSize; i++) {
      int formId = data.readInt(false);
      formIdTable[i + 1] = formId;
    }

    return new FormIdTable(formIdTableSize, formIdTable);
  }

  @Override
  public String toString() {
    return "Form ID table size: " + formIdTableSize;
  }
}
