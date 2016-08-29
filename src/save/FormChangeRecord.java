package save;


public class FormChangeRecord {

  /** Position of the record in the file */
  public final int position;
  public final FormIdIndex formIdIndex;
  public final int changeFlags;

  /**
   * See http://www.uesp.net/wiki/Tes5Mod:Save_File_Format#Change_Form
   */
  public final byte formType;

  public final byte version;
  public final int size;

  /** Position of the data for this record in the file */
  public final int dataPosition;

  protected final Fo3ByteBuffer data;

  FormChangeRecord(int position, FormIdIndex formIdIndex, int changeFlags, byte formType,
      byte version, int size, int dataPosition, Fo3ByteBuffer data) {
    this.position = position;
    this.formIdIndex = formIdIndex;
    this.changeFlags = changeFlags;
    this.formType = formType;
    this.version = version;
    this.size = size;
    this.dataPosition = dataPosition;
    this.data = data;
  }

  protected FormChangeRecord(FormChangeRecord base) {
    this(
        base.position,
        base.formIdIndex,
        base.changeFlags,
        base.formType,
        base.version,
        base.size,
        base.dataPosition,
        base.data);
  }

  public boolean hasInventoryChange() {
    // TES4 had this as bit 27, but it appears to be bit 5 in FO3
    //
    // See http://www.uesp.net/wiki/Tes4Mod:Save_File_Format/Player_Data
    // and determine_flags.py.
    return hasFlag(0b0010_0000);
  }

  public boolean hasBaseData() {
    return hasFlag(1 << 1);
  }
  
  public boolean hasSpellListChange() {
    return hasFlag(1 << 4);
  }
  
  public boolean hasAttributes() {
    return hasFlag(1 << 2);
  }

  private boolean hasFlag(int flag) {
    return (changeFlags & flag) != 0;
  }

  @Override
  public String toString() {
    return new StringBuilder()
       .append(String.format("Position: 0x%08X\n", position))
       .append(String.format("Form Id Index: 0x%08X\n", formIdIndex))
       .append(String.format("Change Flags: 0x%08X\n", changeFlags))
       .append(String.format("Form Type: 0x%02X\n", formType))
       .append(String.format("Version: 0x%02X\n", version))
       .append(String.format("Size: 0x%08X\n", size))
       .append(String.format("Data Position: 0x%08X\n", dataPosition))
        .toString();
  }

  public Fo3ByteBuffer getData() {
    Fo3ByteBuffer d = new Fo3ByteBuffer(data);
    d.position(dataPosition);
    return d;
  }

  public String dumpAscii() {
    return getData().dumpAscii(size, true);
  }
  
  public String dumpBytes() {
    return getData().dumpBytes(size, false);
  }
}