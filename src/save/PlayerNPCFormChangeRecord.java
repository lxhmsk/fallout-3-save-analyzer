package save;

import game.PlayerStats;

import java.util.ArrayList;
import java.util.List;

import save.SaveFile.LoadException;

/**
 * A FormChagneRecord specifically for the player.
 */
public class PlayerNPCFormChangeRecord extends FormChangeRecord {

  public final PlayerStats npcStats;
  public final List<FormIdIndex> spellList;
  
  private PlayerNPCFormChangeRecord(
      FormChangeRecord formChangeRecord, PlayerStats npcStats, List<FormIdIndex> spellList) {
    super(formChangeRecord);
    this.npcStats = npcStats;
    this.spellList = spellList;
  }

  public static PlayerNPCFormChangeRecord parseFrom(
      FormChangeRecord formChangeRecord) throws LoadException {

    Fo3ByteBuffer data = formChangeRecord.data;
    data.position(formChangeRecord.dataPosition);

    if (formChangeRecord.hasBaseData()) {
      data.skipBytes(24);
      data.assertPipe();
    }

    List<FormIdIndex> spellList = new ArrayList<>();
    if (formChangeRecord.hasSpellListChange()) {
      int count = data.readUvarint();
      for (int i = 0; i < count; i++) {
        spellList.add(data.readFormIdIndex());
      }
      data.readByte(); // list always ends with a zero byte?
    }

    PlayerStats stats;
    if (formChangeRecord.hasAttributes()) {
      int strength = data.readByte(false);
      int perception = data.readByte(false);
      int endurance = data.readByte(false);
      int charisma = data.readByte(false);
      int intelligence = data.readByte(false);
      int agility = data.readByte(false);
      int luck = data.readByte(true);
      stats = new PlayerStats(strength, perception, endurance, charisma, intelligence, agility, luck);
    } else {
      stats = null;
    }

    return new PlayerNPCFormChangeRecord(formChangeRecord, stats, spellList);
  } 
}
