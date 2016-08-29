package ui;

import game.Database;
import game.Game;

import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import save.SaveFile;
import static java.util.Comparator.comparing;


public class UiMain {

  private static final File SETTINGS_FILE = new File("settings");

  private static File promptForDirectory(String dialogTitle) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setDialogTitle(dialogTitle);
    int result = fileChooser.showDialog(null, "Select");
    if (result == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile();
    } else {
      return null;
    }
  }

  private static Settings loadSettings() {

    Settings settings = Settings.load(SETTINGS_FILE);
    if (settings == null) {
      settings = new Settings(SETTINGS_FILE);
    }

    if (settings.fallout3Directory == null || !new File(settings.fallout3Directory).exists()) {

      settings.fallout3Directory = Fo3LocationHueristics.guessFallout3Directory();

      JOptionPane.showMessageDialog(null, "Could not find Fallout 3 directory");

      while (settings.fallout3Directory == null) {

        File f = promptForDirectory("Select Fallout 3 directory (contains Fallout3.exe)");
        if (f == null) {
          System.exit(-1);
        }

        if (Arrays.asList(f.list()).contains("Fallout3.exe")) {
          settings.fallout3Directory = f.toString();
        } else {
          JOptionPane.showMessageDialog(null, "Fallout3.exe not found");
        }
      }

      settings.save();
    }

    if (settings.savesDirectory == null || !new File(settings.savesDirectory).exists()) {

      settings.savesDirectory = Fo3LocationHueristics.guessFallout3SavesDirectory();

      if (settings.savesDirectory == null) {

        JOptionPane.showMessageDialog(null,
            "Could not find Fallout 3 saves directory (contains .fos files)");

        File f = promptForDirectory("Select Fallout 3 saves directory");
        if (f == null) {
          System.exit(-1);
        }

        settings.savesDirectory = f.toString();
      }

      settings.save();
    }

    return settings;
  }

  public static void main(String[] args) throws Exception {

    Settings settings = loadSettings();

    File savesDirectory = new File(settings.savesDirectory);

    File newestSave = Arrays.asList(savesDirectory.listFiles(f -> f.getName().endsWith(".fos")))
        .stream()
        .max(comparing(f -> f.lastModified()))
        .orElse(null);

    Database database = Database.load();
    DirectoryWatcher directoryWatcher = new DirectoryWatcher();

    Ui ui = new Ui(settings, database, directoryWatcher);
 
    if (newestSave != null) {      
      SaveFile saveFile = SaveFile.load(newestSave);
      Game game = new Game(saveFile);
      SwingUtilities.invokeLater(() -> ui.setGame(game, false));
    }
    ui.show();

    directoryWatcher.add(savesDirectory.toPath(), (p, e) -> {
      if (p.getFileName().toString().endsWith(".fos")) {
        try {
          SaveFile saveFile = SaveFile.load(p.toFile());
          Game game = new Game(saveFile);
          SwingUtilities.invokeLater(() -> ui.setGame(game, true));
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
    if (settings.watchSaveDirectory) {
      directoryWatcher.start();
    }
  }
}
