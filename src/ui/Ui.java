package ui;

import game.Database;
import game.Game;
import game.Inventory;
import game.Inventory.ItemStack;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;

import save.SaveFile;
import ui.table.Column;
import ui.table.Column.EditPredicate;
import ui.table.ReflectiveTableModel;
import ui.table.SpinnerEditor;
import ui.table.TableFactory;
import ui.table.TableFactory.Filter;
import analysis.Analysis;
import analysis.Analysis.Drop;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class Ui {

  public static final Set<Integer> PINNED_ITEMS = new HashSet<>(Arrays.asList(
      0x00015038, // pip-boy 3000
      0x00025B83, // pip-boy glove
      0x0002D3A5  // Food sanitizer
  ));

  public static class DropCountSpinner extends SpinnerEditor {
    
    @Override
    public Object getCellEditorValue() {
      Object count = super.getCellEditorValue();
      if (count instanceof Integer && ((Integer) count) == 0) {
        return null;
      } else {
        return count;
      }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
        int rowIndex, int columnIndex) {

      @SuppressWarnings("unchecked")
      Row row = ((ReflectiveTableModel<Row>) table.getModel()).getRows().get(
          table.convertRowIndexToModel(rowIndex));

      SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
      model.setMinimum(0);
      model.setMaximum(row.count);
      return super.getTableCellEditorComponent(table, value, isSelected, rowIndex, columnIndex);
    }
  }

  public static class BooleanEditor extends AbstractCellEditor implements TableCellEditor {

    private boolean value;
    
    @Override
    public Object getCellEditorValue() {
      return value ? true : null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
        int row, int column) {
      
      if (value instanceof Boolean) {
        this.value = !((Boolean) value).booleanValue();
      } else {
        this.value = true;
      }

      JLabel label = new JLabel();
      label.setBorder(new EmptyBorder(1, 1, 1, 1));
      label.setFont(table.getFont());
      label.setText(this.value ? "true" : "");
      label.setBackground(table.getSelectionBackground());
      label.setOpaque(true);
      label.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
          fireEditingStopped();
        }
      });
      return label;
    }
  }

  public static class PinnedEditPredicate implements EditPredicate {
    @Override
    public boolean cellEditable(Object row) {
      Row r = (Row) row;
      return !PINNED_ITEMS.contains(r.formId);
    }
  }

  public static class Row {

    @Column(width = 30, header = "#") Integer inventoryIndex;
    @Column(format = "0x%08X") Integer formId;
    @Column(width = 50) String type;
    @Column(width = 240) String description;
    @Column(width = 50, tableCellEditor = BooleanEditor.class, editPredicate = PinnedEditPredicate.class) Boolean pinned;
    @Column(width = 50) Boolean equipped;
    @Column(width = 50) Integer hotkey;
    @Column(width = 50, bold = true, tableCellEditor = DropCountSpinner.class) Integer dropCount;
    @Column(width = 50) Integer count;
    @Column() Integer baseValue;
    @Column(format = "%.1f") Float condition;
    @Column() Integer maxCondition;
    @Column(header = "Condition %", format = "%.1f%%") Float conditionPct;
    @Column() Integer sellValue;
    @Column() Integer totalSellValue;
    @Column() Float weight;
    @Column() Float totalWeight;
    @Column(header = "V/W", format = "%.1f", sort = SortOrder.ASCENDING) Float valueWeightRatio;

    ItemStack backingItemStack;
  }

  private static final String FRAME_TITLE = "Fallout 3 Save Analyzer";

  private static final int BOTTLE_CAPS_FORM_ID = 0xF;
  
  private final Database database;
  private final DirectoryWatcher directoryWatcher;
  private final Settings settings;

  private final List<Row> rows = new ArrayList<>();

  private Game game;
  private Inventory inventory;
  private Map<ItemStack, Integer> itemStackToRowIndex = new HashMap<>();

  // Ui elements
  private final JFrame frame;
  private final ChartPanel weightChartPanel;
  private final ChartPanel valueChartPanel;
  private final ChartPanel valueExclWeightlessChartPanel;
  private final JTable table;
  private final JLabel playerMaxCarryWeightLabel;
  private final JLabel totalInventorySellValueLabel;
  private final JLabel totalInventoryWeightLabel;
  private final JLabel overencumberanceLabel;

  private final JButton openSaveButton;
  private final JButton autoDropsButton;
  private final JButton generateDropScriptButton;
  
  private final JTextField targetWeightTextField;

  private JDialog undroppablesDialog;

  public Ui(Settings settings, Database database, DirectoryWatcher directoryWatcher) {
    
    this.database = database;
    this.directoryWatcher = directoryWatcher;
    this.settings = settings;

    frame = new JFrame(FRAME_TITLE);

    /************************************
     * Top panel
     ************************************/
    JPanel topPanel = new JPanel();
    topPanel.setLayout(new GridLayout());

    // General options box
    Box generalBox = Box.createVerticalBox();
    generalBox.setBorder(BorderFactory.createTitledBorder("General"));

    openSaveButton = new JButton("Open Save");
    openSaveButton.addActionListener(event -> {
      JFileChooser fileChooser = new JFileChooser(settings.savesDirectory);
      fileChooser.setFileFilter(new FileNameExtensionFilter("Fallout 3 Saves", "fos"));
      fileChooser.setDialogTitle("Select Save");
      int ret = fileChooser.showOpenDialog(frame);
      if (ret == JFileChooser.APPROVE_OPTION) {
        SaveFile saveFile;
        Game game;
        try {
          saveFile = SaveFile.load(fileChooser.getSelectedFile());
          game = new Game(saveFile);
        } catch (Exception e1) {
          e1.printStackTrace();
          return;
        }
        setGame(game, false);
      }
    });

    JCheckBox watchSavesFolderCheckbox = new JCheckBox("Watch Fallout saves directory");
    watchSavesFolderCheckbox.setToolTipText("Automatically reload the latest save");
    JCheckBox generateAutoDropScriptOnSave = new JCheckBox("Generate auto drop script on save");

    watchSavesFolderCheckbox.setSelected(settings.watchSaveDirectory);
    watchSavesFolderCheckbox.addActionListener(e -> {
      if (watchSavesFolderCheckbox.isSelected()) {
        Ui.this.directoryWatcher.start();
      } else {
        Ui.this.directoryWatcher.stop();
      }
      generateAutoDropScriptOnSave.setEnabled(watchSavesFolderCheckbox.isSelected());
      settings.watchSaveDirectory = watchSavesFolderCheckbox.isSelected();
      settings.save();
    });

    generateAutoDropScriptOnSave.setSelected(settings.generateAutoDropScriptOnSave);
    generateAutoDropScriptOnSave.addActionListener(e -> {
      settings.generateAutoDropScriptOnSave = generateAutoDropScriptOnSave.isSelected();
      settings.save();
    });

    generalBox.add(openSaveButton);
    generalBox.add(watchSavesFolderCheckbox);
    generalBox.add(generateAutoDropScriptOnSave);

    // Display options box
    Box displayOptionsBox = Box.createVerticalBox();
    displayOptionsBox.setBorder(BorderFactory.createTitledBorder("Display Options"));

    JCheckBox showWeightlessItemsCheckbox = new JCheckBox("Show Weightless Items");
    showWeightlessItemsCheckbox.setSelected(settings.showWeightlessItems);

    JCheckBox showEquippedItemsCheckbox = new JCheckBox("Show Equipped Items");
    showEquippedItemsCheckbox.setSelected(settings.showEquippedItems);

    JCheckBox showHotkeyedItemsCheckbox = new JCheckBox("Show Hotkeyed Items");
    showHotkeyedItemsCheckbox.setSelected(settings.showHotkeyedItems);
    
    JCheckBox showPinnedItemsCheckbox = new JCheckBox("Show Pinned Items");
    showPinnedItemsCheckbox.setSelected(settings.showPinnedItems);
    
    ActionListener saveDisplaySettingsListener = e -> {
      settings.showWeightlessItems = showWeightlessItemsCheckbox.isSelected();
      settings.showEquippedItems = showEquippedItemsCheckbox.isSelected();
      settings.showHotkeyedItems = showHotkeyedItemsCheckbox.isSelected();
      settings.showPinnedItems = showPinnedItemsCheckbox.isSelected();
      settings.save();
    };
    
    showWeightlessItemsCheckbox.addActionListener(saveDisplaySettingsListener);
    showEquippedItemsCheckbox.addActionListener(saveDisplaySettingsListener);
    showHotkeyedItemsCheckbox.addActionListener(saveDisplaySettingsListener);
    showPinnedItemsCheckbox.addActionListener(saveDisplaySettingsListener);

    displayOptionsBox.add(showWeightlessItemsCheckbox);
    displayOptionsBox.add(showEquippedItemsCheckbox);
    displayOptionsBox.add(showHotkeyedItemsCheckbox);
    displayOptionsBox.add(showPinnedItemsCheckbox);

    // Info box
    JPanel infoBox = new JPanel(new GridLayout(0, 2));
    infoBox.setBorder(BorderFactory.createTitledBorder("Inventory Info"));

    totalInventorySellValueLabel = new JLabel();
    playerMaxCarryWeightLabel = new JLabel();
    totalInventoryWeightLabel = new JLabel();
    overencumberanceLabel = new JLabel();    

    infoBox.add(new JLabel("Total Inventory Sell Value: "));
    infoBox.add(totalInventorySellValueLabel);
    
    infoBox.add(new JLabel("Player Max Carry Weight: "));
    infoBox.add(playerMaxCarryWeightLabel);
    
    infoBox.add(new JLabel("Total Inventory Weight: "));
    infoBox.add(totalInventoryWeightLabel);

    infoBox.add(new JLabel("Overencumberance: " + ""));
    infoBox.add(overencumberanceLabel);

    // Auto controls box
    Box autoBox = Box.createVerticalBox();
    autoBox.setBorder(BorderFactory.createTitledBorder("Auto Drops"));

    JCheckBox dropEquippedItemsCheckbox = new JCheckBox("Drop equipped items");
    dropEquippedItemsCheckbox.setSelected(settings.dropEquippedItems);

    JCheckBox dropHotkeyedItemsCheckbox = new JCheckBox("Drop hotkeyed items");
    dropHotkeyedItemsCheckbox.setSelected(settings.dropHotkeyedItems);

    JCheckBox dropPinnedItemsCheckbox = new JCheckBox("Drop pinned items");
    dropPinnedItemsCheckbox.setSelected(settings.dropPinnedItems);

    ActionListener saveDropSettingsListener = e -> {
      settings.dropEquippedItems = dropEquippedItemsCheckbox.isSelected();
      settings.dropHotkeyedItems = dropHotkeyedItemsCheckbox.isSelected();
      settings.dropPinnedItems = dropPinnedItemsCheckbox.isSelected();
      settings.save();
    };

    dropEquippedItemsCheckbox.addActionListener(saveDropSettingsListener);
    dropHotkeyedItemsCheckbox.addActionListener(saveDropSettingsListener);    
    dropPinnedItemsCheckbox.addActionListener(saveDropSettingsListener);    

    targetWeightTextField = new JTextField() {
      @Override
      protected Document createDefaultModel() {
          return new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) 
                throws BadLocationException {
              for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c < '0' || c > '9') {
                  return;
                }
              }
              super.insertString(offs, str, a);
            }
        };
      } 
    };
    targetWeightTextField.setMaximumSize(new Dimension(64, 300));

    autoDropsButton = new JButton("Auto Drops");
    autoDropsButton.setEnabled(false);
    autoDropsButton.setToolTipText("Automatically selects the least valuable items in the "
        + "inventory to drop to get to the target inventory weight.");

    generateDropScriptButton = new JButton("Generate Drop Script");
    generateDropScriptButton.setEnabled(false);
    generateDropScriptButton.setToolTipText("Generates a script in the Fallout 3 folder to drop "
        + "the items in the table that have a Drop Count. Execute the script in the game's "
        + "console with \"bat autodrop\".");

    autoBox.add(dropEquippedItemsCheckbox);
    autoBox.add(dropHotkeyedItemsCheckbox);
    autoBox.add(dropPinnedItemsCheckbox);
    autoBox.add(createHorizontalBox(
        new JLabel("Target weight:"), Box.createHorizontalStrut(5), targetWeightTextField));
    autoBox.add(createHorizontalBox(
        autoDropsButton, Box.createHorizontalStrut(5), generateDropScriptButton));
    
    for (Component c : autoBox.getComponents()) {
      ((JComponent) c).setAlignmentX(JComponent.LEFT_ALIGNMENT);
    }
    
    topPanel.add(generalBox);
    topPanel.add(displayOptionsBox);
    topPanel.add(infoBox);
    topPanel.add(autoBox);

    /************************************
     * Bottom panel
     ************************************/
    
    Box bottomPanel = Box.createHorizontalBox();
    JLabel itemCountLabel = createJLabel("", 80);
    JLabel rowCountLabel = createJLabel("", 80);
    JLabel totalSellValueLabel = createJLabel("", 80);
    JLabel totalWeightLabel = createJLabel("", 80);
    JLabel totalDropValueLabel = createJLabel("", 80);
    JLabel totalDropWeightLabel = createJLabel("", 80);

    bottomPanel.add(new JLabel("Items: "));
    bottomPanel.add(itemCountLabel);

    bottomPanel.add(new JLabel("Rows: "));
    bottomPanel.add(rowCountLabel);

    bottomPanel.add(new JLabel("Total Sell Value: "));
    bottomPanel.add(totalSellValueLabel);

    bottomPanel.add(new JLabel("Total Weight: "));
    bottomPanel.add(totalWeightLabel);

    bottomPanel.add(new JLabel("Total Drop Value: "));
    bottomPanel.add(totalDropValueLabel);

    bottomPanel.add(new JLabel("Total Drop Weight: "));
    bottomPanel.add(totalDropWeightLabel);

    /************************************
     * Table
     ************************************/

    table = TableFactory.makeTable(Row.class, rows, new Filter<Row>() {
      @Override
      public boolean include(Row row) {
        if (!showWeightlessItemsCheckbox.isSelected() && row.weight == 0) {
          return false;
        }
        if (!showEquippedItemsCheckbox.isSelected() && (row.equipped != null && row.equipped)) {
          return false;
        }
        if (!showHotkeyedItemsCheckbox.isSelected() && row.hotkey != null) {
          return false;
        }
        if (!showPinnedItemsCheckbox.isSelected() && row.pinned != null && row.pinned) {
          return false;
        }
        return true;
      }
    });
    table.setFont(new Font("DejaVu Sans Mono", Font.PLAIN, 12));
    
    ActionListener refreshListener = e -> updateTable();

    showWeightlessItemsCheckbox.addActionListener(refreshListener);
    showEquippedItemsCheckbox.addActionListener(refreshListener);
    showHotkeyedItemsCheckbox.addActionListener(refreshListener);
    showPinnedItemsCheckbox.addActionListener(refreshListener);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {

        int itemCount = 0;
        int totalSellValue = 0;
        float totalWeight = 0;
        int totalDropValue = 0;
        float totalDropWeight = 0;
        int[] selectedRows = table.getSelectedRows();

        for (int index : selectedRows) {
          int modelIndex = table.convertRowIndexToModel(index);
          Row row = rows.get(modelIndex);
          totalWeight += row.totalWeight;
          totalDropWeight += row.dropCount == null ? 0 : row.dropCount * row.weight;
          totalDropValue += row.dropCount == null ? 0 : row.dropCount * row.sellValue;
          totalSellValue += row.totalSellValue;
          itemCount += row.count;
        }

        itemCountLabel.setText(itemCount + "");
        rowCountLabel.setText(selectedRows.length + "");
        totalSellValueLabel.setText(totalSellValue + "");
        totalWeightLabel.setText(totalWeight + "");
        totalDropValueLabel.setText(totalDropValue + "");
        totalDropWeightLabel.setText(totalDropWeight + "");
      }
    });

    table.getModel().addTableModelListener(e -> {
      Set<Integer> pinnedFormIds = rows.stream()
          .filter(r -> r.pinned != null && r.pinned)
          .map(r -> r.formId)
          .collect(toSet());
      if (settings.setPinnedFormIds(pinnedFormIds)) {
        // only save if changed because the table changes a lot.
        settings.save();
      }
    });

    JScrollPane tableScrollpane = new JScrollPane(table);
    tableScrollpane.setPreferredSize(new Dimension(1400, 768));
    tableScrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    autoDropsButton.addActionListener(event -> {
      calculateDrops();
    });

    generateDropScriptButton.addActionListener(event -> {
      generateDropScript();
    });

    /************************************
     * Pie Charts
     ************************************/
    
    
    weightChartPanel = new ChartPanel(null);
    valueChartPanel = new ChartPanel(null);
    valueExclWeightlessChartPanel = new ChartPanel(null);
    
    
    /************************************
     * Tabbed Pane
     ************************************/

    JTabbedPane tabbedPane = new JTabbedPane();
    
    JPanel tableTabPanel = new JPanel(new BorderLayout());
    tableTabPanel.add(tableScrollpane, BorderLayout.CENTER);
    tableTabPanel.add(bottomPanel, BorderLayout.SOUTH);

    tabbedPane.addTab("Table", tableTabPanel);
    tabbedPane.addTab("Weight Chart", weightChartPanel);
    tabbedPane.addTab("Value Chart (all)", valueChartPanel);
    tabbedPane.addTab("Value Chart (excl. weightless)", valueExclWeightlessChartPanel);

    /************************************
     * Frame
     ************************************/

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(topPanel, BorderLayout.NORTH);
    frame.add(tabbedPane, BorderLayout.CENTER);

    frame.pack();
  }
  
  private void calculateDrops() {
    Set<Integer> pinnedFormIds = settings.getPinnedFormIds();

    int targetWeight;
    try {
      targetWeight = Integer.parseInt(targetWeightTextField.getText());
    } catch (NumberFormatException e) {
      targetWeight = game.getCarryWeight();
    }

    List<Drop> drops = Analysis.optimizeDrops(
        inventory,
        targetWeight,
        PINNED_ITEMS,
        e -> (!e.equipped || settings.dropEquippedItems) &&
             (e.hotkey == null || settings.dropHotkeyedItems) &&
             (!pinnedFormIds.contains(e.formId) || settings.dropPinnedItems));

    table.getSelectionModel().clearSelection();
    for (Row row : rows) {
      row.dropCount = null;
    }
    updateTable();

    for (Drop drop : drops) {
      int modelRowIndex = itemStackToRowIndex.get(drop.itemStack);
      Row row = rows.get(modelRowIndex);
      row.dropCount = drop.count;
      int viewRowIndex = table.convertRowIndexToView(modelRowIndex);
      table.getSelectionModel().addSelectionInterval(viewRowIndex, viewRowIndex);
    }
  }
  
  private void generateDropScript() {

    List<Drop> drops = rows.stream()
        .filter(r -> r.dropCount != null && r.dropCount > 0)
        .map(r -> new Drop(r.backingItemStack, r.dropCount))
        .collect(toList());

    DropScript dropScript = DropScript.generateDropScript(
        game.getSave().file.getName(), inventory, drops);

    try (FileWriter fw = new FileWriter(new File(settings.fallout3Directory, "autodrop.txt"))) {
      fw.write(dropScript.script);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (undroppablesDialog != null) {
      undroppablesDialog.setVisible(false);
      undroppablesDialog.dispose();
    }
    
    if (!dropScript.undroppableItems.isEmpty()) {

      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      
      JPanel gridTable = new JPanel();
      gridTable.setLayout(new GridLayout(0, 4));
      for (int i = 0; i < 4; i++) {
        gridTable.add(new JLabel());
      }
      gridTable.add(new JLabel("Description"));
      gridTable.add(new JLabel("Form ID"));
      gridTable.add(new JLabel("Sell Value"));
      gridTable.add(new JLabel("Drop Count"));

      for (Drop d : dropScript.undroppableItems) {
        gridTable.add(new JLabel(d.itemStack.description));  
        gridTable.add(new JLabel(String.format("0x%08X", d.itemStack.formId)));
        gridTable.add(new JLabel(d.itemStack.sellValue + ""));
        gridTable.add(new JLabel(d.count + ""));
      }

      panel.add(new JLabel("Could not auto drop some items. "
          + "These will need to be dropped manually:"), BorderLayout.NORTH);
      panel.add(gridTable, BorderLayout.CENTER);
      
      JOptionPane optionPane = new JOptionPane(panel, JOptionPane.INFORMATION_MESSAGE);
      undroppablesDialog = optionPane.createDialog(frame, "Could not drop some items");
      // Don't steal focus from the game.
      undroppablesDialog.setFocusableWindowState(false);
      undroppablesDialog.setVisible(true);
      undroppablesDialog.dispose();
    }
  }

  public void show() {
    frame.setVisible(true);
  }

  public void setGame(Game game, boolean maybeGenerateDropScript) {

    this.game = game;
    this.inventory = game.getPlayerInventory(database);

    updateTableRows();
    updateTable();
    updateWeightChart();
    updateValueChart();
    updateValueExclWeightlessChart();
    updateInventoryInfoLabels();
    
    frame.setTitle(FRAME_TITLE + " - " + game.getSave().file.toString());

    autoDropsButton.setEnabled(true);
    generateDropScriptButton.setEnabled(true);
    if (targetWeightTextField.getText().isEmpty()) {
      targetWeightTextField.setText(game.getCarryWeight() +  "");
    }

    if (maybeGenerateDropScript &&
        settings.watchSaveDirectory &&
        settings.generateAutoDropScriptOnSave) {
      calculateDrops();
      generateDropScript();
    }
  }

  private void updateInventoryInfoLabels() {
    double totalInventoryWeight = rows.stream().mapToDouble(r -> r.totalWeight).sum();
    int totalInventorySellValue = rows.stream()
        .filter(r -> r.formId != BOTTLE_CAPS_FORM_ID)
        .mapToInt(r -> r.totalSellValue)
        .sum();

    totalInventorySellValueLabel.setText(totalInventorySellValue + "");
    totalInventoryWeightLabel.setText(totalInventoryWeight + "");
    playerMaxCarryWeightLabel.setText(game.getCarryWeight() + "");
    overencumberanceLabel.setText((totalInventoryWeight - game.getCarryWeight()) + "");
  }
  
  private void updateTableRows() {
    rows.clear();
    itemStackToRowIndex.clear();

    Set<Integer> pinnedFormIds = new HashSet<>(settings.getPinnedFormIds());

    for (int i = 0; i < inventory.getInventory().size(); i++) {

      ItemStack entry = inventory.getInventory().get(i);

      itemStackToRowIndex.put(entry, i);
      
      Row row = new Row();
      
      row.backingItemStack = entry;

      boolean pinned = pinnedFormIds.contains(entry.formId) || PINNED_ITEMS.contains(entry.formId);
      
      row.inventoryIndex = entry.inventoryIndex;
      row.formId = entry.formId;
      row.type = entry.type;
      row.description = entry.description;
      row.equipped = entry.equipped ? true : null;
      row.pinned =  pinned ? true : null;
      row.hotkey = entry.hotkey;
      row.dropCount = null;
      row.count = entry.count;
      row.baseValue = entry.baseValue;
      row.condition = entry.condition;
      row.maxCondition = entry.maxCondition;
      row.weight = entry.weight;
      row.conditionPct = entry.conditionPercent * 100;
      row.sellValue = entry.sellValue;
      row.totalSellValue = entry.count * entry.sellValue;
      row.valueWeightRatio = entry.valueWeightRatio;
      row.totalWeight = entry.weight* entry.count;

      rows.add(row);
    }
  }
  
  private void updateTable() {
    ((AbstractTableModel) table.getModel()).fireTableDataChanged();
  }
  
  private void updateWeightChart() {
    updateChart(weightChartPanel, 
        inventory.getInventory().stream()
            .filter(i -> i.weight > 0)
            .collect(toMap(i -> i.formId, i -> i.count * i.weight, Float::sum)));
  }
  
  private void updateValueChart() {
    updateChart(valueChartPanel,
        inventory.getInventory().stream()
            .filter(i -> i.sellValue > 0 && i.formId != BOTTLE_CAPS_FORM_ID)
              .collect(toMap(i -> i.formId, i -> i.count * i.sellValue, Integer::sum)));
  }

  private void updateValueExclWeightlessChart() {
    updateChart(valueExclWeightlessChartPanel,
        inventory.getInventory().stream()
            .filter(i -> i.sellValue > 0 && i.formId != BOTTLE_CAPS_FORM_ID && i.weight > 0)
              .collect(toMap(i -> i.formId, i -> i.count * i.sellValue, Integer::sum)));
  }  
  
  private void updateChart(ChartPanel chartPanel, Map<Integer, ? extends Number> data) {
    DefaultPieDataset dataset = new DefaultPieDataset();

    data.forEach((formId, number) -> {
      dataset.setValue(formId, number);
    });
    dataset.sortByValues(org.jfree.util.SortOrder.ASCENDING);
    
    chartPanel.setChart(createChart(dataset));
  }

  @SuppressWarnings("rawtypes")
  private JFreeChart createChart(DefaultPieDataset dataset) {
    JFreeChart chart = ChartFactory.createPieChart(null, dataset);
    chart.setAntiAlias(true);
    chart.removeLegend();
    
    PiePlot plot = (PiePlot) chart.getPlot();
    plot.setCircular(true);
    plot.setDirection(Rotation.ANTICLOCKWISE);

    plot.setLabelGenerator(new PieSectionLabelGenerator() {
      @Override
      public String generateSectionLabel(PieDataset dataset, Comparable key) {
        return database.get((Integer) key).description;
      }
      
      @Override
      public AttributedString generateAttributedSectionLabel(PieDataset dataset, Comparable key) {
        return null;
      }
    });

    plot.setToolTipGenerator(new StandardPieToolTipGenerator() {
      @Override
      protected Object[] createItemArray(PieDataset dataset, Comparable key) {
        Object[] array = super.createItemArray(dataset, key);
        array[0] = database.get((Integer) key).description;
        return array;
      }
    });
    
    return chart;
  }

  private static JLabel createJLabel(String text, int width) {
    JLabel label = new JLabel(text);
    label.setMaximumSize(new Dimension(width, 300));
    return label;
  }
  
  private Box createHorizontalBox(Component... comps) {
    Box b = Box.createHorizontalBox();
    for (Component c : comps) {
      b.add(c);
    }
    return b;
  }
}
