package save;

import game.PlayerStats;
import save.SaveFile.LoadException;

/**
 * A FormChagneRecord specifically for the player.
 */
public class PlayerACHRFormChangeRecord extends FormChangeRecord {

  public final RawInventory inventory;
  public final int experience;
  public final PlayerStats permanentStatChanges; 
  public final PlayerStats temporaryStatChanges;
  
  private PlayerACHRFormChangeRecord(
      FormChangeRecord formChangeRecord,
      RawInventory inventory,
      int experience,
      PlayerStats permanentStatChanges,
      PlayerStats temporaryStatChanges) {

    super(formChangeRecord);
    this.inventory = inventory;
    this.experience = experience;
    this.permanentStatChanges = permanentStatChanges;
    this.temporaryStatChanges = temporaryStatChanges;
    
  }

  public static PlayerACHRFormChangeRecord parseFrom(
      FormChangeRecord formChangeRecord) throws LoadException {

    // In TES4, the inventory is 905 bytes into the player record:
    // http://www.uesp.net/wiki/Tes4Mod:Save_File_Format/Player_Data
    //
    // In FO3, the records before the inventory vary in length.
    // The variable length records appear to start at 0x0489 bytes
    // into the record.

    Fo3ByteBuffer data = formChangeRecord.data;
    data.position(formChangeRecord.dataPosition);

    // Skip Moved struct
    data.skipBytes(28);

    // Temporary attribute changes.
    // In TES4 this is 876 bytes, but in F03 it's
    // 220 floats or 1100 bytes (220 * (4 bytes + pipe char))
    float[] temporaryAttributeChanges = new float[220];
    for (int i = 0; i < 220; i++) {
      // Fire resistance is the float at index 52.
      temporaryAttributeChanges[i] = data.readFloat();
    }

    PlayerStats temporaryStatChanges = new PlayerStats(
        (int) temporaryAttributeChanges[5],  // S
        (int) temporaryAttributeChanges[6],  // P
        (int) temporaryAttributeChanges[7],  // E
        (int) temporaryAttributeChanges[8],  // C
        (int) temporaryAttributeChanges[9],  // I
        (int) temporaryAttributeChanges[10], // A
        (int) temporaryAttributeChanges[11]  // L
    );

    PlayerStats permanentStatChanges = new PlayerStats(
        (int) temporaryAttributeChanges[78], // S
        (int) temporaryAttributeChanges[79], // P
        (int) temporaryAttributeChanges[80], // E
        (int) temporaryAttributeChanges[81], // C
        (int) temporaryAttributeChanges[82], // I
        (int) temporaryAttributeChanges[83], // A
        (int) temporaryAttributeChanges[84]  // L
    );
    
    int experience = (int) temporaryAttributeChanges[98];
    
    // Actor flag, which should be zero for the player.
    data.assertByte(0);

    // This looks like the scale. It's 1.0f for regular saves,
    // 0.4f for a save when the player is still a baby.
    data.readFloat();

    // A count of how many records are between the move record
    // and the inventory.
    int recordCount = data.readUvarint();

    for (int i = 0; i < recordCount; i++) {
      
      byte recordType = data.readByte();
      
      switch(recordType) {

      case 0x18:
        data.readFormIdIndex();
        // 3 floats, a location maybe?
        data.readFloat(false);
        data.readFloat(false);
        data.readFloat();
        data.readFloat(); // This is sometimes 3.141594
        break;

      case 0x5E: {
        // This is maybe "Game Only Extra"?
        // Seems to appear only if that bit is set
        // in the form change record flags.

        // Var int length of data
        int count = data.readUvarint();
        for (int c = 0; c < count; c++) {
          // Then repeated structs of form id index and some byte.
          data.readFormIdIndex();
          data.readByte();
        }
      } break;

      case 0x7C:
      case 0x1D: {
        int count = data.readUvarint();
        for (int c = 0; c < count; c++) {
          data.readFormIdIndex();
        }
      } break;

      case 0x74:
        // This is maybe "Enc Zone Extra"?
        // Seems to appear only if that bit is set
        // in the form change record flags.
        data.readFormIdIndex();
        break;

      case 0x60:
        data.readInt();
        break;

      default:
        throw new LoadException("Unknown record type 0x%02X before player inventory at 0x%08X",
            recordType, data.previousPosition());
      }
    }

    RawInventory inventory;
    if (formChangeRecord.hasInventoryChange()) {
      // The record should now have been parsed far enough to get to the inventory.
      inventory = RawInventory.load(data);
    } else {
      inventory = RawInventory.emptyInventory();
    }

    return new PlayerACHRFormChangeRecord(
        formChangeRecord, inventory, experience, permanentStatChanges, temporaryStatChanges);
  }
}
