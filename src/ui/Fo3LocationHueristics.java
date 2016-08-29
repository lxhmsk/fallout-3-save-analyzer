package ui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Fo3LocationHueristics {

  
  public static String guessFallout3Directory() {
    
    String[] programFiles = {
        "Program Files",
        "Program Files (x86)",
        "Programs"
    };
    
    String[] medium = {
        "/Steam/steamapps/common/",
        "/Bethesda Softworks/"
    };
    
    String[] falloutVersion = {
        "Fallout 3",
        "Fallout 3 goty"
    };

    for (File drive : File.listRoots()) {
      for (String p : programFiles) {
        for (String m : medium) {
          for (String f : falloutVersion) {
            Path path = Paths.get(drive.toString(), p, m, f, "Fallout3.exe");
            if (Files.exists(path)) {
              return path.toString();
            }
          }
        }
      }
    }

    return null;
  }
  
  public static String guessFallout3SavesDirectory() {
    
    String[] docs = {
        "My Documents",
        "Documents",
    };

    String home = System.getProperty("user.home");
    if (home == null) {
      return null;
    }

    for (String d : docs) {
      Path path = Paths.get(home, d, "my games/Fallout3/Saves");
      if (Files.exists(path)) {
        return path.toString();
      }
    }
    return null;
  }
}
