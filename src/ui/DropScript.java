package ui;

import game.Inventory;
import game.Inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import analysis.Analysis.Drop;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class DropScript {

  public final String script;
  public final List<Drop> undroppableItems;

  private DropScript(String script, List<Drop> undroppableItems) {
    this.script = script;
    this.undroppableItems = undroppableItems;
  }

  public static DropScript generateDropScript(
      String saveName, Inventory inventory, List<Drop> drops) {

    Map<Integer, Set<Float>> formIdsToConditions = new HashMap<>();
    for (ItemStack i : inventory) {
      formIdsToConditions.computeIfAbsent(i.formId, HashSet::new).add(i.condition);
    }

    Map<Integer, Integer> formIdsToDropCount = new HashMap<>();
    for (Drop d : drops) {
      formIdsToDropCount.merge(d.itemStack.formId, d.count, Integer::sum);
    }

    Map<Integer, String> formIdToDescription = inventory.getInventory().stream()
        .collect(toMap(i -> i.formId, i -> i.description, (a, b) -> a));

    Map<Integer, Integer> formIdsToInventoryCounts = inventory.getInventory().stream()
        .collect(toMap(i -> i.formId, i -> i.count, Integer::sum));
    
    int totalDropCount = 0;
    int totalDropSellValue = 0;
    float totalDropWeight = 0;
    for (Drop drop : drops) {
      totalDropCount += drop.count;
      totalDropSellValue += drop.count * drop.itemStack.sellValue;
      totalDropWeight += drop.count * drop.itemStack.weight;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("; Auto generated for ").append(saveName).append("\r\n");
    sb.append("; Type \"bat autodrop\" into the Fallout 3 console.\r\n");
    sb.append("; Items to drop: ").append(totalDropCount).append("\r\n");
    sb.append("; Sell value: ").append(totalDropSellValue).append("\r\n");
    sb.append("; Drop weight: ").append(totalDropWeight).append("\r\n");
    sb.append("\r\n");

    List<Integer> undroppableFromIds = new ArrayList<>();

    for (Entry<Integer, Integer> e : formIdsToDropCount.entrySet()) {
      int formId = e.getKey();
      int dropCount = e.getValue();
      
      // If all of the items in the inventory with the given form ID have the same
      // condition (or no condition), then any of them can be dropped. Or, if
      // all of the items in the inventory with this form ID are to be dropped,
      // then it doesn't matter which ones get dropped.
      if (formIdsToConditions.get(formId).size() == 1 ||
          dropCount == formIdsToInventoryCounts.get(formId)) {
        sb.append(String.format("player.Drop %08X %2d ; %s\r\n",
            formId, dropCount, formIdToDescription.get(formId)));
      } else {
        // If items with this form ID have different conditions, then it's not possible to
        // drop a specific item  (player.drop doesn't allow specifying which item to drop
        // if the player has multiple items by that form ID).
        undroppableFromIds.add(formId);
      }
    }

    List<Drop> undroppableItems = Collections.emptyList();

    if (!undroppableFromIds.isEmpty()) {

      undroppableItems = new ArrayList<>();

      Map<Integer, List<Drop>> formIdToDrops = drops.stream()
          .collect(groupingBy(d -> d.itemStack.formId));

      sb.append("\r\n");
      sb.append("; Cannot drop these items:\r\n");
      sb.append("; formId   count sellValue description\r\n");

      for (int formId : undroppableFromIds) {
        for (Drop drop : formIdToDrops.get(formId)) {
          undroppableItems.add(drop);
          sb.append(String.format("; %08X %5d %9d %s\r\n",
              drop.itemStack.formId,
              drop.count,
              drop.itemStack.sellValue,
              drop.itemStack.description));
        }
      }
    }

    return new DropScript(sb.toString(), undroppableItems);
  }
}
