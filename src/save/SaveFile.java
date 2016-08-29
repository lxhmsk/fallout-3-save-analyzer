package save;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SaveFile {

  public static class LoadException extends Exception {

    public LoadException(String msg) {
      super(msg);
    }

    public LoadException(String fmt, Object... args) {
      super(String.format(fmt, args));
    }
  }

  public final File file;
  
  // These are in the order that they appear in
  // the save file.
  
  public final Header header;

  public final List<String> plugins;

  public final FileLocations fileLocations;

  public final MiscStats miscStats;

  public final FormChangeRecordTable formChangeRecordTable;

  public final FormIdTable formIdTable;

  private SaveFile(File file, Header header, List<String> plugins,
      FileLocations fileLocations, MiscStats miscStats,
      FormChangeRecordTable formChangeRecordsTable, FormIdTable formIdTable) {
    this.file = file;
    this.header = header;
    this.plugins = plugins;
    this.fileLocations = fileLocations;
    this.miscStats = miscStats;
    this.formChangeRecordTable = formChangeRecordsTable;
    this.formIdTable = formIdTable;
  }

  public FormChangeRecord getFormChangeRecord(int formId) {
    FormIdIndex formIdIndex = formIdTable.findFormIdIndexByFormId(formId);
    if (formIdIndex == null) {
      return null;
    }

    FormChangeRecord formChangeRecord = formChangeRecordTable.get(formIdIndex);
    return formChangeRecord;
  }
  
  
  public static SaveFile load(File file) throws LoadException, IOException {
    return load(new Fo3ByteBuffer(file));
  }

  public static SaveFile load(Fo3ByteBuffer data) throws LoadException {
    // It is necessary to load the header, screenshot,
    // and plugins to get to the file location table,
    // because there isn't a pointer to it in the save file.
    Header header = Header.load(data);
    loadScreenshot(data, header);
    List<String> plugins = loadPlugins(data);
    FileLocations fileLocations = FileLocations.load(data);

    // Only reading the first part of the globals
    MiscStats miscStats = MiscStats.load(data, fileLocations);

    FormChangeRecordTable formChangeRecordTable = FormChangeRecordTable.load(data, fileLocations);

    FormIdTable formIdTable = FormIdTable.load(data, fileLocations);

    return new SaveFile(
        data.getFile(),
        header,
        plugins,
        fileLocations,
        miscStats,
        formChangeRecordTable,
        formIdTable);
  }

  private static void loadScreenshot(Fo3ByteBuffer data, Header header) {

    int screenshotSize = header.screenshotHeight * header.screenshotWidth * 3;

    data.skipBytes(screenshotSize);
  }

  private static List<String> loadPlugins(Fo3ByteBuffer data) throws LoadException {
    // unknown, always 0x15?
    data.assertByte(0x15, false);

    List<String> plugins = new ArrayList<>();
    
    // Plugin struct size
    data.readInt(false);
    byte pluginCount = data.readByte();

    for (byte i = 0; i < pluginCount; i++) {
      // Plugin name
      plugins.add(data.readBString());
    }
    
    return plugins;
  }
}
