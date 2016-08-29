package game;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Database {

  public static class ItemData {

    public final int formId;
    public final String description;
    public final String signature;
    public final int baseValue;
    public final float weight;
    /** may be null */
    public final Integer maxCondition;

    public ItemData(
        int formId,
        String signature,
        String description,
        int baseValue,
        float weight,
        Integer maxCondition) {

      this.formId = formId;
      this.signature = signature;
      this.description = description;
      this.baseValue = baseValue;
      this.weight = weight;
      this.maxCondition = maxCondition;
    }
  }

  public static final ItemData UNKNOWN_ITEM = new ItemData(-1, "????", "UNKNOWN", -1, -1, -1);

  private final Map<Integer, ItemData> items;

  private Database(Map<Integer, ItemData> items) {
    this.items = items;
  }

  public ItemData get(int formId) {
    ItemData itemData = items.get(formId);
    if (itemData == null) {
      return UNKNOWN_ITEM;
    } else {
      return itemData;
    }
  }

  public static Database load() throws IOException {
    Map<Integer, ItemData> items = new HashMap<>();
    try (Scanner scan = new Scanner(new File("items.txt"))) {
      while (scan.hasNext()) {
        String line = scan.nextLine();
        String[] data = line.split("\t");

        if (data.length < 6) {
          System.out.println("bad line in database: " + line);
          continue;
        }
        
        String formIdStr = data[0];
        String signature = data[1];
        String description = data[2];
        int baseValue = Integer.parseInt(data[3]);
        float weight = Float.parseFloat(data[4]);
        Integer maxCondition = Integer.parseInt(data[5]);
        if (maxCondition < 0) {
          maxCondition = null;
        }

        int formId = Integer.parseInt(formIdStr, 16);
        
        items.put(formId,
            new ItemData(formId, signature, description, baseValue, weight, maxCondition));
      }
    }
    return new Database(items);
  }
}
