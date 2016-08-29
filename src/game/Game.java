package game;

import game.Database.ItemData;
import save.PlayerACHRFormChangeRecord;
import save.PlayerNPCFormChangeRecord;
import save.RawInventory;
import save.SaveFile;
import save.SaveFile.LoadException;

public class Game {

  private static final int PLAYER_NPC_FORM_ID = 0x0000_0007;
  private static final int PLAYER_FORM_ID = 0x0000_0014;

  private final SaveFile save;
  
  private final PlayerACHRFormChangeRecord playerACHRFormChangeRecord;
  private final PlayerNPCFormChangeRecord playerNPCFormChangeRecord;

  public Game(SaveFile save) throws LoadException {
    this.save = save;
    
    this.playerACHRFormChangeRecord =
        PlayerACHRFormChangeRecord.parseFrom(save.getFormChangeRecord(PLAYER_FORM_ID));

    this.playerNPCFormChangeRecord =
        PlayerNPCFormChangeRecord.parseFrom(save.getFormChangeRecord(PLAYER_NPC_FORM_ID));
  }

  public PlayerACHRFormChangeRecord getPlayerACHRFormChangeRecord() {
    return playerACHRFormChangeRecord;
  }
  
  public PlayerNPCFormChangeRecord getPlayerNPCFormChangeRecord() {
    return playerNPCFormChangeRecord;
  }
  
  public RawInventory getPlayerRawInventory() {
    return playerACHRFormChangeRecord.inventory;
  }

  public Inventory getPlayerInventory(Database database) {
    return Inventory.from(playerACHRFormChangeRecord.inventory, save.formIdTable, database);
  }
  
  public PlayerStats getPlayerStats() {
    return playerNPCFormChangeRecord.npcStats;
  }

  public PlayerStats getPlayerTemporaryStatChanges() {
    return playerACHRFormChangeRecord.temporaryStatChanges;
  }

  public PlayerStats getPlayerPermanentStatChanges() {
    return playerACHRFormChangeRecord.permanentStatChanges;
  }
  
  public int getPlayerStrength() {
    PlayerStats baseStats = getPlayerStats();
    if (baseStats == null) {
      return 0;
    }

    // Temporary stats, from enchantments from armor or from
    // buffs like alcohol.
    PlayerStats tempChanges = getPlayerTemporaryStatChanges();

    // Permanent stats, from Strength Bobble head, or Ant Strength
    PlayerStats permanentChanges = getPlayerPermanentStatChanges();
    
    PlayerStats stats = baseStats.add(permanentChanges).add(tempChanges);
    return stats.strength;
  }
  
  public int getCarryWeight() {
    return 150 + getPlayerStrength() * 10;
  }

  public SaveFile getSave() {
    return save;
  }
  
  public static int calcSellValue(int baseValue, Float condition, Integer maxCondition) {
    if (condition == null || maxCondition == null) {
      return baseValue;
    }
    float conditionPct = condition / maxCondition;
    // See http://fallout.wikia.com/wiki/Condition#Value
    return (int) Math.round(baseValue * Math.pow(conditionPct, 1.5));
  }
  
  public static int calcSellValue(ItemData itemData, Float condition) {
    return calcSellValue(itemData.baseValue, condition, itemData.maxCondition);
  }
}
