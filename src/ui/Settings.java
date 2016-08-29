package ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class Settings {

  private transient final File file;
  
  public boolean watchSaveDirectory = true;
  public boolean generateAutoDropScriptOnSave = true;
  public String savesDirectory;
  public String fallout3Directory;
  
  public boolean showWeightlessItems = true;
  public boolean showEquippedItems = true;
  public boolean showHotkeyedItems = true;
  public boolean showPinnedItems = true;
  
  public boolean dropEquippedItems = false;
  public boolean dropHotkeyedItems = false;
  public boolean dropPinnedItems = false;

  private String pinnedFormIds = "";


  public Set<Integer> getPinnedFormIds() {
    return Arrays.asList(pinnedFormIds.split(",")).stream()
        .filter(s -> !s.isEmpty())
        .map(s -> Integer.parseInt(s, 16))
        .collect(toSet());
  }

  public boolean setPinnedFormIds(Set<Integer> formIds) {
    String pinnedFormIds = formIds.stream()
        .sorted()
        .map(i -> Integer.toHexString(i))
        .collect(joining(","));
    boolean changed = this.pinnedFormIds.equals(pinnedFormIds);
    this.pinnedFormIds = pinnedFormIds;
    return changed;
  }
  
  public Settings(File file) {
    this.file = file;
  }

  public void save() {
    try (FileWriter fw = new FileWriter(file)) {
      for (Field f : getClass().getDeclaredFields()) {
        if (Modifier.isTransient(f.getModifiers())) {
          continue;
        }

        fw.write(f.getName());
        fw.write("=");
        fw.write(f.get(this) + "\n");
      }
    } catch (IOException | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public static Settings load(File file) {
    
    Map<String, Field> fields = Arrays.asList(Settings.class.getDeclaredFields()).stream()
        .filter(f -> !Modifier.isTransient(f.getModifiers()))
        .collect(toMap(f -> f.getName(), f -> f));

    Map<Class<?>, Function<String, Object>> deserializers = new HashMap<>();
    deserializers.put(boolean.class, Boolean::parseBoolean);
    deserializers.put(String.class, s -> s);
    
    Settings settings = new Settings(file);

    try (Scanner scan = new Scanner(file)) {
      while (scan.hasNextLine()) {
        String line = scan.nextLine().trim();
        if (line.isEmpty()) {
          continue;
        }
        String[] keyValue = line.split("=", 2);
        if (keyValue.length != 2) {
          continue;
        }
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();
        if (key.isEmpty() || value.isEmpty()) {
          continue;
        }
        Field f = fields.get(key);
        f.set(settings, deserializers.get(f.getType()).apply(value));
      }
    } catch (FileNotFoundException e) {
      return null;
    } catch (IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }

    return settings;
  }
  
}
