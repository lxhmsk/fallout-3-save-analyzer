package save;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import save.SaveFile.LoadException;

public class FormChangeRecordTable implements Iterable<FormChangeRecord> {

  private final Map<FormIdIndex, FormChangeRecord> records;
  
  private FormChangeRecordTable(Map<FormIdIndex, FormChangeRecord> records) {
    this.records = records;
  }

  public FormChangeRecord get(FormIdIndex formIdIndex) {
    return records.get(formIdIndex);
  }
  
  static FormChangeRecordTable load(Fo3ByteBuffer data, FileLocations fileLocations)
      throws LoadException {

    Map<FormIdIndex, FormChangeRecord> formChangeRecords = new HashMap<>();

    data.position(fileLocations.formChangeRecordsTableAddress);

    for (int i = 0; i < fileLocations.formChangeRecordsCount; i++) {

      int position = data.position();

      FormIdIndex formIdIndex = data.readFormIdIndex(false);
      int changeFlags = data.readInt(false);
      byte formTypeAndDataSize = data.readByte(false);
      byte version = data.readByte(false);

      // The upper 2 bits of the 9th byte indicate how many
      // bytes are in the size field.
      // 0b00 = 1 byte, 0b01 = 2 bytes, 0b10 = 4 bytes
      int numSizeBytes = 1 << (0b11 & (formTypeAndDataSize >> 6)); 
      int size = data.readNumber(numSizeBytes);

      byte formType = (byte) (formTypeAndDataSize & 0b0011_1111);

      int dataPosition = data.position();

      FormChangeRecord formChangeRecord = new FormChangeRecord(
          position,
          formIdIndex,
          changeFlags,
          formType,
          version,
          size,
          dataPosition,
          data);

      FormChangeRecord prev = formChangeRecords.put(formIdIndex, formChangeRecord);

      if (prev != null) {
        throw new LoadException(
            "Conflicting form change records:\nprevious: %s\nconflict: %s",
            prev, formChangeRecord);
      }
      
      data.skipBytes(size);
    }

    return new FormChangeRecordTable(Collections.unmodifiableMap(formChangeRecords));
  }

  @Override
  public Iterator<FormChangeRecord> iterator() {
    return records.values().iterator();
  }
}
