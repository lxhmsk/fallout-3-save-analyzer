package game;

import game.Database.ItemData;
import game.Inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import save.FormIdIndex;
import save.FormIdTable;
import save.RawInventory;
import save.RawInventory.RawInventoryEntry;
import save.RawInventory.RawInventoryEntry.RawItemInfo;

public class Inventory implements Iterable<ItemStack> {

  public static class ItemStack {

    // Data from the inventory
    public final int inventoryIndex;
    public final FormIdIndex formIdIndex;
    public final int count;
    /** may be null */
    public final Float condition;
    public final boolean equipped;
    /** may be null */
    public final Integer hotkey;
    /** may be null */
    public final FormIdIndex ownerFormIdIndex;
    /** may be null */
    public final FormIdIndex scriptFormIdIndex;

    // Data from the database
    public final String description;
    public final String type;
    public final int baseValue;
    public final float weight;
    /** may be null */
    public final Integer maxCondition;

    // Data from the form id table
    public final int formId;

    // Derived
    public final int sellValue; 
    public final float valueWeightRatio;
    public final float conditionPercent;
    
    private ItemStack(int inventoryIndex, FormIdIndex formIdIndex, int count, Float condition,
        boolean equipped, Integer hotkey, FormIdIndex ownerFormIdIndex,
        FormIdIndex scriptFormIdIndex, ItemData itemData, int formId) {

      // From inventory
      this.inventoryIndex = inventoryIndex;
      this.formIdIndex = formIdIndex;
      this.count = count;
      this.condition = condition;
      this.equipped = equipped;
      this.hotkey = hotkey;
      this.ownerFormIdIndex = ownerFormIdIndex;
      this.scriptFormIdIndex = scriptFormIdIndex;

      // From database
      this.description = itemData.description;
      this.type = itemData.signature;
      this.baseValue = itemData.baseValue;
      this.weight = itemData.weight;
      this.maxCondition = itemData.maxCondition;

      // From id table
      this.formId = formId;

      // Derived
      this.sellValue = Game.calcSellValue(itemData, condition);

      if (weight == 0) {
        this.valueWeightRatio = 0;
      } else {
        this.valueWeightRatio = sellValue / weight;
      }
      
      if (condition == null || maxCondition == null) {
        conditionPercent = 1f;
      } else {
        conditionPercent = condition / maxCondition;
      }
    }
    
    @Override
    public String toString() {
      return description;
    }
  }

  private final List<ItemStack> inventory;

  private Inventory(List<ItemStack> inventory) {
    this.inventory = inventory;
  }

  @Override
  public Iterator<ItemStack> iterator() {
    return inventory.iterator();
  }

  public List<ItemStack> getInventory() {
    return inventory;
  }

  public float getTotalWeight() {
    float totalWeight = 0;
    for (ItemStack entry : this) {
      totalWeight += entry.weight * entry.count;
    }
    return totalWeight;
  }

  public static Inventory from(
      RawInventory rawInventory, FormIdTable formIdTable, Database database) {
    
    List<ItemStack> entries = new ArrayList<ItemStack>();
    
    for (RawInventoryEntry entry : rawInventory) {

      int formId = formIdTable.findFormIdByFormIdIndex(entry.formIdIndex);
      ItemData itemData = database.get(formId);
      
      int stackedItemsWithInfo = entry.itemInfos.stream()
          .mapToInt(i -> i.getCount()).sum();

      int itemsWithNoExtraInfo = entry.count - stackedItemsWithInfo;

      if (itemsWithNoExtraInfo > 0) {
        entries.add(new ItemStack(
            entry.inventoryIndex,
            entry.formIdIndex,
            itemsWithNoExtraInfo,
            itemData.maxCondition == null ? null : itemData.maxCondition.floatValue(),
            false,
            null,
            null,
            null,
            itemData,
            formId));
      }

      for (RawItemInfo itemInfo : entry.itemInfos) {
        entries.add(new ItemStack(
            entry.inventoryIndex,
            entry.formIdIndex,
            itemInfo.getCount(),
            itemInfo.getCondition(),
            itemInfo.getEquipped(),
            itemInfo.getHotkey(),
            itemInfo.getOwnerFormIdIndex(),
            itemInfo.getScriptFormIdIndex(),
            itemData,
            formId));
      }
    }
    
    return new Inventory(Collections.unmodifiableList(entries));
  }
}
