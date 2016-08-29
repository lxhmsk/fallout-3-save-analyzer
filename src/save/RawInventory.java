package save;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import save.RawInventory.RawInventoryEntry;
import save.RawInventory.RawInventoryEntry.RawItemInfo;
import save.SaveFile.LoadException;

/**
 * The inventory as it is presented in the save file.
 */
public class RawInventory implements Iterable<RawInventoryEntry> {

  static RawInventory emptyInventory() {
    return new RawInventory(Collections.<RawInventoryEntry>emptyList());
  }

  public static class RawInventoryEntry {

    public static class RawItemInfo {

      private Float condition = null;
      private boolean equipped = false;
      private int count = 1;
      private Integer hotkey = null;
      private FormIdIndex ownerFormIdIndex = null;
      private FormIdIndex scriptFormIdIndex = null;

      private List<FormIdIndex> unknown1 = new ArrayList<>();
      private List<Float> unknown2 = new ArrayList<>();
      private Boolean unknown3 = null;

      public Float getCondition() {
        return condition;
      }
      public boolean getEquipped() {
        return equipped;
      }
      public int getCount() {
        return count;
      }
      public Integer getHotkey() {
        return hotkey;
      }
      public FormIdIndex getOwnerFormIdIndex() {
        return ownerFormIdIndex;
      }
      public FormIdIndex getScriptFormIdIndex() {
        return scriptFormIdIndex;
      }
      public List<FormIdIndex> getUnknown1() {
        return unknown1;
      }
      public List<Float> getUnknown2() {
        return unknown2;
      }
      public Boolean getUnknown3() {
        return unknown3;
      }
    }

    public final int inventoryIndex;
    public final FormIdIndex formIdIndex;
    public final int count;

    public final List<RawItemInfo> itemInfos = new ArrayList<>();
    
    RawInventoryEntry(FormIdIndex formIdIndex, int inventoryIndex, int count) {
      this.formIdIndex = formIdIndex;
      this.inventoryIndex = inventoryIndex;
      this.count = count;
    }
  }

  private final List<RawInventoryEntry> inventory;

  private RawInventory(List<RawInventoryEntry> inventory) {
    this.inventory = inventory;
  }

  public List<RawInventoryEntry> getEntries() {
    return inventory;
  }

  @Override
  public Iterator<RawInventoryEntry> iterator() {
    return inventory.iterator();
  }
  
  private static final byte INVENTORY_TAG_CONDITION = 0x25;
  private static final byte INVENTORY_TAG_CONDITION_COUNT = 0x24;
  private static final byte INVENTORY_TAG_EQUIPPED = 0x16;
  private static final byte INVENTORY_TAG_OWNER = 0x21;
  private static final byte INVENTORY_TAG_HOTKEY = 0x4A;
  private static final byte INVENTORY_TAG_SCRIPT = 0x0D;

  private static final byte INVENTORY_TAG_UNKNOWN1 = 0x1C;
  private static final byte INVENTORY_TAG_UNKNOWN2 = 0x30;
  private static final byte INVENTORY_TAG_UNKNOWN3 = 0x3E;

  /**
   * Loads an inventory record from the given data. The buffer must be
   * positioned at the start of the inventory (i.e., the first byte of the
   * inventory size varint).
   */
  public static RawInventory load(Fo3ByteBuffer data) throws LoadException {

    // The inventory record is almost the same as in TES4:
    // http://www.uesp.net/wiki/Tes4Mod:Save_File_Format/Inventory
    //
    // Except the counts are variable-length integers. The bottom 2 bits of the first byte
    // indicate how many bytes are in the number:
    // 0b00 = 1 byte, 0b01 = 2 bytes, 0b10 = 4 bytes.

    int inventoryCount = data.readUvarint();

    List<RawInventoryEntry> inventory = new ArrayList<>();
    for (int i = 0; i < inventoryCount; i++) {

      FormIdIndex formIdIndex = data.readFormIdIndex();
      int itemCount = data.readInt();

      RawInventoryEntry entry= new RawInventoryEntry(formIdIndex, i, itemCount);
      inventory.add(entry);

      int itemInfoCount = data.readUvarint();
      for (int j = 0; j < itemInfoCount; j++) {

        RawItemInfo item = new RawItemInfo();
        entry.itemInfos.add(item);

        int tagCount = data.readUvarint();
        for (int k = 0; k < tagCount; k++) {

          byte tag = data.readByte();
          
          switch (tag) {
          
          case INVENTORY_TAG_CONDITION:
            item.condition = data.readFloat();
            break;

          case INVENTORY_TAG_CONDITION_COUNT:
            item.count = data.readShort();
            break;
          
          case INVENTORY_TAG_EQUIPPED:
            item.equipped = true;
            break;
            
          case INVENTORY_TAG_HOTKEY:
            item.hotkey = Integer.valueOf(data.readByte());
            break;
            
          case INVENTORY_TAG_OWNER:
            item.ownerFormIdIndex = data.readFormIdIndex();
            break;

          case INVENTORY_TAG_SCRIPT:
            item.scriptFormIdIndex = data.readFormIdIndex();

            int variableCount = data.readUvarint();

            for (int v = 0; v < variableCount; v++) {

              // Unlike in TES4, where the variable index and variable type are stored
              // in separate shorts, F03 appears to put them in one int. 
              // See http://www.uesp.net/wiki/Tes4Mod:Save_File_Format/Properties

              int variableIndexAndFlags = data.readInt();

              // How many bits are in the flags?
              int variableFlags = variableIndexAndFlags & 0xFF000000;

              if (variableFlags == 0) {
                // variable is a 64 bit float
                data.readDouble();
              } else if (variableFlags == 0x8000_0000) {
                // variable is a form id index
                data.readFormIdIndex();
              } else {
                throw new LoadException(
                    "Unknown variable flags in script at 0x%08X: index and flags: 0x%08X",
                    data.previousPosition(), variableIndexAndFlags);
              }
            }

            // Always zeros?
            data.assertByte(0);
            data.assertByte(0);
            break;

          case INVENTORY_TAG_UNKNOWN1:
            // Don't know what this is, but it appears to be followed by a form id index,
            // and there can be more than one.
            item.unknown1.add(data.readFormIdIndex());
            break;

          case INVENTORY_TAG_UNKNOWN2:
            // Don't know what this is, but it appears to be followed by some float.
            item.unknown2.add(data.readFloat());
            break;

          case INVENTORY_TAG_UNKNOWN3:
            // Don't know what this is, but it appears to have no data, and appears
            // with items that have a count of zero.
            item.unknown3 = true;
            break;

          default:
            throw new LoadException(
                "Unknown inventory tag at 0x%08X: 0x%02X", data.previousPosition(), tag);
          }
        }
      } 
    }

    return new RawInventory(inventory);
  }
}
