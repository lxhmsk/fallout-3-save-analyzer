package analysis;

import game.Inventory;
import game.Inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class Analysis {

  public static class Drop {
    public final ItemStack itemStack;
    public final int count;

    public Drop(ItemStack itemStack, int count) {
      this.itemStack = itemStack;
      this.count = count;
    }
  }
  
  /**
   * Returns the items from the given inventory to drop to get the total weight of the inventory
   * below the given target weight.
   *
   * @param inventory inventory to drop from
   * @param database item database
   * @param targetWeight weight to get the inventory under
   * @param pinnedFormIds item form ids to never drop
   * @param dropEquippedItems whether to include equipped items in the drops
   * @param dropHotkeyedItems whether to include hotkeyed items in the drops
   * @return a list of items to drop
   */
  public static List<Drop> optimizeDrops(
      Inventory inventory,
      int targetWeight,
      Set<Integer> pinnedFormIds,
      Predicate<ItemStack> filter) {

    if (inventory.getInventory().isEmpty()) {
      return Collections.emptyList();
    }

    float totalWeight = inventory.getTotalWeight();
    // round down
    int dropWeight = (int)(totalWeight - targetWeight);
    if (dropWeight <= 0) {
      return Collections.emptyList(); 
    }

    List<ItemStack> filteredSortedEntries = inventory.getInventory().stream()
        .filter(filter
            .and(e -> e.weight > 0)
            .and(e -> !pinnedFormIds.contains(e.formId)))
        .sorted(comparing(e -> e.valueWeightRatio))
        .collect(toList());

    List<Drop> drops = new ArrayList<>();

    float weight = 0;
    for (ItemStack entry : filteredSortedEntries) {

      // Calculate how many items will fit in the remaining space
      int count = Math.min((int)((dropWeight - weight) / entry.weight), entry.count);
      if (count == 0) {
        break;
      }

      drops.add(new Drop(entry, count));
      weight += entry.weight * count;
    }

    // weight cannot be greater than dropWeight because the above for-loop breaks before
    // adding anything that would make it go over dropWeight. If weight == dropWeight,
    // then the last item hit the drop weight target perfectly and there's no need for
    // further optimization.
    if (weight < dropWeight) {

      int index;
      Drop lastDrop;
      if (drops.isEmpty()) {
        index = 0;
        lastDrop = null;
      } else {
        index = drops.size() - 1;      
        lastDrop = drops.get(index);
        if (lastDrop.count == lastDrop.itemStack.count) {
          // All of the items in this item stack fit into the remaining space, so go to the next
          // stack.
          index++;
        }
      }

      float remainingWeight = dropWeight - weight;

      // Find the lowest value item in the remaining items that is at least as heavy as the
      // remaining weight to be dropped.
      ItemStack leastExpensive = filteredSortedEntries.stream()
          .skip(index)
          .filter(e -> e.weight >= remainingWeight)
          .min(comparing(e -> e.sellValue))
          .orElse(null);

      if (leastExpensive != null) {
        if (lastDrop != null && lastDrop.itemStack == leastExpensive) {
          drops.set(drops.size() - 1, new Drop(leastExpensive, lastDrop.count + 1));
        } else {
          drops.add(new Drop(leastExpensive, 1));
        }
      }
    }

    return drops;
  }
}
