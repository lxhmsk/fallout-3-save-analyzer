package save;

public class FileLocations {

  public final int formIdTableAddress;
  public final int unknownTableAddress;
  public final int globalDataTable1Address;
  public final int formChangeRecordsTableAddress;
  public final int globalDataTable2Address;
  public final int globalDataTable1Count;
  public final int globalDataTable2Count;
  public final int formChangeRecordsCount;

  private FileLocations(
      int formIdTableAddress, int unknownTableAddress, int globalDataTable1Address,
      int formChangeRecordsTableAddress, int globalDataTable2Address, int globalDataTable1Count,
      int globalDataTable2Count, int formChangeRecordsCount) {

    this.formIdTableAddress = formIdTableAddress;
    this.unknownTableAddress = unknownTableAddress;
    this.globalDataTable1Address = globalDataTable1Address;
    this.formChangeRecordsTableAddress = formChangeRecordsTableAddress;
    this.globalDataTable2Address = globalDataTable2Address;
    this.globalDataTable1Count = globalDataTable1Count;
    this.globalDataTable2Count = globalDataTable2Count;
    this.formChangeRecordsCount = formChangeRecordsCount;
  }
  
  static FileLocations load(Fo3ByteBuffer data) {

    // This is close to the file location table described for Skyrim:
    // http://www.uesp.net/wiki/Tes5Mod:Save_File_Format#File_Location_Table
    // Notably, there doesn't appear to be a "global data table 3". 

    int formIdTableAddress = data.readInt(false);

    // Unknown table
    int unknownTableAddress = data.readInt(false);

    // Address of global data table 1. First global (0x00) is game stats struct.
    int globalDataTable1Address = data.readInt(false);
    
    // Address of form change records.
    int formChangeRecordsTableAddress = data.readInt(false);
    
    // Address of global data table 2.
    int globalDataTable2Address = data.readInt(false);
    
    // Global data counts. These appear to always be 0xC and 0x1 
    int globalDataTable1Count = data.readInt(false);
    int globalDataTable2Count = data.readInt(false);
    
    int formChangeRecordsCount = data.readInt(false);

    // These are all zeros?
    data.skipBytes(0x4E);
    
    return new FileLocations(
        formIdTableAddress,
        unknownTableAddress,
        globalDataTable1Address,
        formChangeRecordsTableAddress,
        globalDataTable2Address,
        globalDataTable1Count,
        globalDataTable2Count,
        formChangeRecordsCount);
  }

  @Override
  public String toString() {

    return new StringBuilder()
        .append(String.format("Form Id Table Address: 0x%08X\n", formIdTableAddress))
        .append(String.format("Unknown Table Address: 0x%08X\n", unknownTableAddress))
        .append(String.format("Global Data Table1 Address: 0x%08X\n", globalDataTable1Address))
        .append(String.format("Form Change Records Table Address: 0x%08X\n", formChangeRecordsTableAddress))
        .append(String.format("Global Data Table2 Address: 0x%08X\n", globalDataTable2Address))
        .append(String.format("Global Data Table1 Count: %s\n", globalDataTable1Count))
        .append(String.format("Global Data Table2 Count: %s\n", globalDataTable2Count))
        .append(String.format("Form Change Records Count: %s\n", formChangeRecordsCount))
        .toString();
  }
}
