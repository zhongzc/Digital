/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.components.table;

import de.neemann.digiblock.analyse.AnalyseException;
import de.neemann.digiblock.analyse.ModelAnalyserInfo;
import de.neemann.digiblock.analyse.TruthTable;
import de.neemann.digiblock.analyse.TruthTableTableModel;
import de.neemann.digiblock.analyse.expression.Expression;
import de.neemann.digiblock.analyse.expression.ExpressionException;
import de.neemann.digiblock.analyse.expression.NamedExpression;
import de.neemann.digiblock.analyse.expression.Variable;
import de.neemann.digiblock.analyse.expression.format.FormatterException;
import de.neemann.digiblock.analyse.expression.modify.*;
import de.neemann.digiblock.analyse.format.TruthTableFormatter;
import de.neemann.digiblock.analyse.format.TruthTableFormatterTestCase;
import de.neemann.digiblock.analyse.quinemc.BoolTableByteArray;
import de.neemann.digiblock.builder.ATF150x.ATFDevice;
import de.neemann.digiblock.builder.ExpressionToFileExporter;
import de.neemann.digiblock.builder.Gal16v8.CuplExporter;
import de.neemann.digiblock.builder.Gal16v8.Gal16v8JEDECExporter;
import de.neemann.digiblock.builder.Gal22v10.Gal22v10CuplExporter;
import de.neemann.digiblock.builder.Gal22v10.Gal22v10JEDECExporter;
import de.neemann.digiblock.builder.circuit.CircuitBuilder;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.Key;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.graphics.text.ParseException;
import de.neemann.digiblock.draw.graphics.text.Parser;
import de.neemann.digiblock.draw.graphics.text.formatter.PlainTextFormatter;
import de.neemann.digiblock.draw.library.ElementLibrary;
import de.neemann.digiblock.draw.shapes.ShapeFactory;
import de.neemann.digiblock.gui.Main;
import de.neemann.digiblock.gui.SaveAsHelper;
import de.neemann.digiblock.gui.components.AttributeDialog;
import de.neemann.digiblock.gui.components.ElementOrderer;
import de.neemann.digiblock.gui.components.karnaugh.KarnaughMapDialog;
import de.neemann.digiblock.gui.components.table.hardware.GenerateCUPL;
import de.neemann.digiblock.gui.components.table.hardware.GenerateFile;
import de.neemann.digiblock.gui.components.table.hardware.HardwareDescriptionGenerator;
import de.neemann.digiblock.lang.Lang;
import de.neemann.digiblock.undo.ModifyException;
import de.neemann.digiblock.undo.UndoManager;
import de.neemann.gui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import static de.neemann.digiblock.analyse.ModelAnalyser.addOne;

/**
 * The dialog used to show the truth table.
 */
public class TableDialog extends JDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableDialog.class);
    private static final Preferences PREFS = Preferences.userRoot().node("dig").node("generator");
    private static final Color MYGRAY = new Color(230, 230, 230);
    private static final List<Key> LIST = new ArrayList<>();

    static {
        LIST.add(Keys.LABEL);
    }

    /**
     * Opens the given file in a new dialog
     *
     * @param file the file to open
     */
    public static void openFile(File file) {
        try {
            TruthTable tt = TruthTable.readFromFile(file);
            ElementLibrary library = new ElementLibrary();
            new ShapeFactory(library);
            SwingUtilities.invokeLater(() -> new TableDialog(null, tt, library, file).setVisible(true));
        } catch (IOException e) {
            new ErrorMessage().addCause(e).show();
        }
    }

    private final ExpressionComponent statusBar;
    private final JTable table;
    private final Font font;
    private final ElementLibrary library;
    private final ShapeFactory shapeFactory;
    private final ToolTipAction karnaughMenuAction;
    private final HashMap<String, HardwareDescriptionGenerator> availGenerators = new HashMap<>();
    private final JMenu hardwareMenu;
    private final TruthTableTableModel model;
    private JCheckBoxMenuItem createJK;
    private File filename;
    private int columnIndex;
    private AllSolutionsDialog allSolutionsDialog;
    private ExpressionListenerStore lastGeneratedExpressions;
    private KarnaughMapDialog kvMap;
    private JMenuItem lastUsedGenratorMenuItem;
    private Mouse mouse = Mouse.getMouse();
    private UndoManager<TruthTable> undoManager;

    /**
     * Creates a new instance
     *
     * @param parent     the parent frame
     * @param truthTable the table to show
     * @param library    the library to use
     * @param filename   the file name used to create the names of the created files
     */
    public TableDialog(Window parent, TruthTable truthTable, ElementLibrary library, File filename) {
        super(parent, Lang.get("win_table"));
        undoManager = new UndoManager<>(truthTable);
        this.library = library;
        this.shapeFactory = library.getShapeFactory();
        this.filename = filename;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        model = new TruthTableTableModel(undoManager);
        model.addTableModelListener(new CalculationTableModelListener());

        kvMap = new KarnaughMapDialog(this, (boolTable, row) -> model.incValue(boolTable, row));

        statusBar = new ExpressionComponent();
        font = Screen.getInstance().getFont(1.66f);
        statusBar.setFont(font);
        table = new JTable(model);
        JComboBox<String> comboBox = new JComboBox<>(TruthTableTableModel.STATENAMES);
        table.setDefaultEditor(Integer.class, new DefaultCellEditor(comboBox));
        table.setDefaultRenderer(Integer.class, new CenterDefaultTableCellRenderer());
        table.getTableHeader().setDefaultRenderer(new StringDefaultTableCellRenderer());
        table.setRowHeight(font.getSize() * 6 / 5);

        table.getInputMap().put(KeyStroke.getKeyStroke("0"), "0_ACTION");
        table.getActionMap().put("0_ACTION", new SetAction(0));
        table.getInputMap().put(KeyStroke.getKeyStroke("1"), "1_ACTION");
        table.getActionMap().put("1_ACTION", new SetAction(1));
        table.getInputMap().put(KeyStroke.getKeyStroke("X"), "X_ACTION");
        table.getActionMap().put("X_ACTION", new SetAction(2));

        new TableReorderManager(this, table);

        allSolutionsDialog = new AllSolutionsDialog(this, font);

        JTableHeader header = table.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (mouse.isSecondaryClick(event)) {
                    columnIndex = header.columnAtPoint(event.getPoint());
                    if (columnIndex != -1)
                        editColumnName(columnIndex, new Point(event.getXOnScreen(), event.getYOnScreen()));
                }
            }
        });

        JMenuBar bar = new JMenuBar();
        bar.add(createFileMenu());

        JMenu sizeMenu = new JMenu(Lang.get("menu_table_new"));

        JMenu combinatorial = new JMenu(Lang.get("menu_table_new_combinatorial"));
        sizeMenu.add(combinatorial);
        for (int i = 2; i <= 8; i++)
            combinatorial.add(new JMenuItem(new SizeAction(i)));
        JMenu sequential = new JMenu(Lang.get("menu_table_new_sequential"));
        sizeMenu.add(sequential);
        for (int i = 2; i <= 8; i++)
            sequential.add(new JMenuItem(new SizeSequentialAction(i)));
        if (Main.isExperimentalMode()) {
            JMenu sequentialBiDir = new JMenu(Lang.get("menu_table_new_sequential_bidir"));
            sizeMenu.add(sequentialBiDir);
            for (int i = 2; i <= 8; i++)
                sequentialBiDir.add(new JMenuItem(new SizeSequentialBidirectionalAction(i)));
        }
        bar.add(sizeMenu);

        JMenu edit = new JMenu(Lang.get("menu_edit"));
        bar.add(edit);

        addUndoRedo(edit);

        edit.addSeparator();

        edit.add(new ToolTipAction(Lang.get("menu_table_reorder_inputs")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> varNames = undoManager.getActual().getVarNames();
                if (new ElementOrderer<>(TableDialog.this, Lang.get("menu_table_reorder_inputs"), new ElementOrderer.ListOrder<>(varNames))
                        .addDeleteButton()
                        .addOkButton()
                        .showDialog()) {

                    try {
                        undoManager.apply(tt -> {
                            try {
                                new ReorderInputs(tt, varNames).reorder();
                                tableChanged();
                            } catch (ExpressionException ex) {
                                throw new ModifyException("failed to reorder", ex);
                            }
                        });
                    } catch (ModifyException e1) {
                        new ErrorMessage().addCause(e1).show(TableDialog.this);
                    }
                }
            }
        }.createJMenuItem());

        edit.add(new ToolTipAction(Lang.get("menu_table_columnsAddVariable")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    undoManager.apply(TruthTable::addVariable);
                    tableChanged();
                } catch (ModifyException e) {
                    new ErrorMessage().addCause(e).show(TableDialog.this);
                }
            }
        }.setToolTip(Lang.get("menu_table_columnsAddVariable_tt")).createJMenuItem());

        edit.add(new ToolTipAction(Lang.get("menu_table_reorder_outputs")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<String> resultNames = undoManager.getActual().getResultNames();
                if (new ElementOrderer<>(TableDialog.this, Lang.get("menu_table_reorder_outputs"), new ElementOrderer.ListOrder<>(resultNames))
                        .addDeleteButton()
                        .addOkButton()
                        .showDialog()) {
                    try {
                        undoManager.apply(tt -> {
                            try {
                                new ReorderOutputs(tt, resultNames).reorder();
                                tableChanged();
                            } catch (ExpressionException ex) {
                                throw new ModifyException("failed to reorder", ex);
                            }
                        });
                    } catch (ModifyException e1) {
                        new ErrorMessage().addCause(e1).show(TableDialog.this);
                    }
                }
            }
        }.createJMenuItem());

        edit.add(new ToolTipAction(Lang.get("menu_table_columnsAdd")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    undoManager.apply(TruthTable::addResult);
                    tableChanged();
                } catch (ModifyException e) {
                    new ErrorMessage().addCause(e).show(TableDialog.this);
                }

            }
        }.setToolTip(Lang.get("menu_table_columnsAdd_tt")).createJMenuItem());

        edit.addSeparator();

        createSetMenuEntries(edit);

        hardwareMenu = createCreateMenu();
        bar.add(hardwareMenu);
        checkLastUsedGenerator();

        JMenu karnaughMenu = new JMenu(Lang.get("menu_karnaughMap"));
        karnaughMenuAction = new ToolTipAction(Lang.get("menu_karnaughMap")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                kvMap.setVisible(true);
            }
        }.setToolTip(Lang.get("menu_karnaughMap_tt")).setAccelerator("F1");
        karnaughMenu.add(karnaughMenuAction.createJMenuItem());
        bar.add(karnaughMenu);

        setJMenuBar(bar);

        karnaughMenuAction.setEnabled(undoManager.getActual().getVars().size() <= 4);
        calculateExpressions();

        getContentPane().add(new JScrollPane(table));
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private void addUndoRedo(JMenu edit) {
        final ToolTipAction undo = new ToolTipAction(Lang.get("menu_undo")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (undoManager.undoAvailable()) {
                    try {
                        undoManager.undo();
                        tableChanged();
                    } catch (ModifyException e) {
                        new ErrorMessage().addCause(e).show(TableDialog.this);
                    }
                }
            }
        }.setAcceleratorCTRLplus("Z");
        final ToolTipAction redo = new ToolTipAction(Lang.get("menu_redo")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (undoManager.redoAvailable()) {
                    try {
                        undoManager.redo();
                        tableChanged();
                    } catch (ModifyException e) {
                        new ErrorMessage().addCause(e).show(TableDialog.this);
                    }
                }
            }
        }.setAcceleratorCTRLplus("Y");

        edit.add(undo.createJMenuItem());
        edit.add(redo.createJMenuItem());
        undoManager.addListener(() -> {
            undo.setEnabled(undoManager.undoAvailable());
            redo.setEnabled(undoManager.redoAvailable());
            if (undoManager.isModified())
                setTitle("*" + Lang.get("win_table"));
            else
                setTitle(Lang.get("win_table"));
        }).hasChanged();
    }

    /**
     * Called if table was modified.
     */
    public void tableChanged() {
        karnaughMenuAction.setEnabled(undoManager.getActual().getVars().size() <= 4);
        calculateExpressions();
        model.fireTableChanged();
    }

    private void editColumnName(int columnIndex, Point pos) {
        ElementAttributes attr = new ElementAttributes();
        final String name = model.getColumnName(columnIndex);
        attr.set(Keys.LABEL, name);
        ElementAttributes modified = new AttributeDialog(this, pos, LIST, attr).showDialog();
        if (modified != null) {
            final String newName = modified.get(Keys.LABEL).trim().replace(' ', '-');
            if (!newName.equals(name))
                model.setColumnName(columnIndex, newName);
        }
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu(Lang.get("menu_file"));

        fileMenu.add(new ToolTipAction(Lang.get("menu_open")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new MyFileChooser();
                if (TableDialog.this.filename != null)
                    fc.setSelectedFile(SaveAsHelper.checkSuffix(TableDialog.this.filename, "tru"));
                fc.setFileFilter(new FileNameExtensionFilter(Lang.get("msg_truthTable"), "tru"));
                if (fc.showOpenDialog(TableDialog.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        File file = fc.getSelectedFile();
                        TruthTable truthTable = TruthTable.readFromFile(file);
                        undoManager.setInitial(truthTable);
                        tableChanged();
                        TableDialog.this.filename = file;
                    } catch (IOException e1) {
                        new ErrorMessage().addCause(e1).show(TableDialog.this);
                    }
                }
            }
        });

        fileMenu.add(new ToolTipAction(Lang.get("menu_save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new MyFileChooser();
                if (TableDialog.this.filename != null)
                    fc.setSelectedFile(SaveAsHelper.checkSuffix(TableDialog.this.filename, "tru"));
                fc.setFileFilter(new FileNameExtensionFilter(Lang.get("msg_truthTable"), "tru"));
                new SaveAsHelper(TableDialog.this, fc, "tru").checkOverwrite(
                        file -> {
                            undoManager.getActual().save(file);
                            TableDialog.this.filename = file;
                        }
                );
            }
        });


        fileMenu.add(new ToolTipAction(Lang.get("menu_table_exportTableLaTeX")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final LaTeXExpressionListener laTeXExpressionListener = new LaTeXExpressionListener(undoManager.getActual());
                    ExpressionListener expressionListener = laTeXExpressionListener;
                    if (createJK.isSelected())
                        expressionListener = new ExpressionListenerJK(expressionListener);
                    lastGeneratedExpressions.replayTo(expressionListener);
                    expressionListener.close();

                    new ShowStringDialog(TableDialog.this, Lang.get("win_table_exportDialog"),
                            laTeXExpressionListener.toString()).setVisible(true);
                } catch (ExpressionException | FormatterException e1) {
                    new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e1).show(TableDialog.this);
                }
            }
        });

        fileMenu.add(new ToolTipAction(Lang.get("menu_table_createFunctionFixture")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ModelAnalyserInfo modelAnalyzerInfo = undoManager.getActual().getModelAnalyzerInfo();
                    if (modelAnalyzerInfo.isSequential())
                        JOptionPane.showMessageDialog(
                                TableDialog.this,
                                Lang.get("menu_table_createFunctionFixture_isSequential"));
                    TruthTableFormatter test = new TruthTableFormatterTestCase(modelAnalyzerInfo);
                    String testCase = test.format(undoManager.getActual());
                    new ShowStringDialog(TableDialog.this, Lang.get("win_table_exportDialog"),
                            testCase).setVisible(true);
                } catch (ExpressionException e1) {
                    new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e1).show(TableDialog.this);
                }
            }
        }.setToolTip(Lang.get("menu_table_createFunctionFixture_tt")).createJMenuItem());

        fileMenu.add(new ToolTipAction(Lang.get("menu_table_exportHex")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int res = JOptionPane.OK_OPTION;
                if (undoManager.getActual().getVars().size() > 20)
                    res = JOptionPane.showConfirmDialog(TableDialog.this, Lang.get("msg_tableHasManyRowsConfirm"));
                if (res == JOptionPane.OK_OPTION) {
                    JFileChooser fc = new MyFileChooser();
                    if (TableDialog.this.filename != null)
                        fc.setSelectedFile(SaveAsHelper.checkSuffix(TableDialog.this.filename, "hex"));
                    new SaveAsHelper(TableDialog.this, fc, "hex")
                            .checkOverwrite(file -> undoManager.getActual().saveHex(file));
                }
            }
        }.setToolTip(Lang.get("menu_table_exportHex_tt")).createJMenuItem());


        createJK = new JCheckBoxMenuItem(Lang.get("menu_table_JK"));
        createJK.addActionListener(e -> calculateExpressions());
        fileMenu.add(createJK);

        fileMenu.add(allSolutionsDialog.getReopenAction());

        return fileMenu;
    }

    private void createSetMenuEntries(JMenu edit) {
        edit.add(new ToolTipAction(Lang.get("menu_table_setXTo0")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> v > 1 ? 0 : v);
            }
        }.setToolTip(Lang.get("menu_table_setXTo0_tt")).createJMenuItem());
        edit.add(new ToolTipAction(Lang.get("menu_table_setXTo1")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> v > 1 ? 1 : v);
            }
        }.setToolTip(Lang.get("menu_table_setXTo1_tt")).createJMenuItem());
        edit.add(new ToolTipAction(Lang.get("menu_table_setAllToX")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> (byte) 2);
            }
        }.setToolTip(Lang.get("menu_table_setAllToX_tt")).createJMenuItem());
        edit.add(new ToolTipAction(Lang.get("menu_table_setAllTo0")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> (byte) 0);
            }
        }.setToolTip(Lang.get("menu_table_setAllTo0_tt")).createJMenuItem());
        edit.add(new ToolTipAction(Lang.get("menu_table_setAllTo1")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> (byte) 1);
            }
        }.setToolTip(Lang.get("menu_table_setAllTo1_tt")).createJMenuItem());
        edit.add(new ToolTipAction(Lang.get("menu_table_invert")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                modifyTable(v -> v > 1 ? v : (byte) (1 - v));
            }
        }.setToolTip(Lang.get("menu_table_invert_tt")).createJMenuItem());
    }

    private void modifyTable(BoolTableByteArray.TableModifier m) {
        try {
            undoManager.apply(truthTable -> truthTable.modifyValues(m));
            tableChanged();
        } catch (ModifyException e) {
            e.printStackTrace();
            new ErrorMessage().addCause(e).show(TableDialog.this);
        }
    }

    private JMenu createCreateMenu() {
        JMenu createMenu = new JMenu(Lang.get("menu_table_create"));
        createMenu.add(new ToolTipAction(Lang.get("menu_table_createCircuit")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(ExpressionModifier.IDENTITY);
            }
        }.setToolTip(Lang.get("menu_table_createCircuit_tt")).setAccelerator("F2").enableAcceleratorIn(table).createJMenuItem());

        createMenu.add(new ToolTipAction(Lang.get("menu_table_createCircuitJK")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(true, false, ExpressionModifier.IDENTITY);
            }
        }.setToolTip(Lang.get("menu_table_createCircuitJK_tt")).createJMenuItem());

        createMenu.add(new ToolTipAction(Lang.get("menu_table_createCircuitLUT")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(false, true, ExpressionModifier.IDENTITY);
            }
        }.setToolTip(Lang.get("menu_table_createCircuitLUT_tt")).createJMenuItem());

        createMenu.add(new ToolTipAction(Lang.get("menu_table_createTwo")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(new TwoInputs());
            }
        }.setToolTip(Lang.get("menu_table_createTwo_tt")).createJMenuItem());

        createMenu.add(new ToolTipAction(Lang.get("menu_table_createThree")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(new ThreeInputs());
            }
        }.setToolTip(Lang.get("menu_table_createThree_tt")).createJMenuItem());

        createMenu.add(new ToolTipAction(Lang.get("menu_table_createNAnd")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createCircuit(new NAnd());
            }
        }.setToolTip(Lang.get("menu_table_createNAnd_tt")).createJMenuItem());

        if (Main.isExperimentalMode()) {
            createMenu.add(new ToolTipAction(Lang.get("menu_table_createNAndTwo")) {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    createCircuit(new TwoInputs(), new NAnd());
                }
            }.setToolTip(Lang.get("menu_table_createNAndTwo_tt")).createJMenuItem());

            createMenu.add(new ToolTipAction(Lang.get("menu_table_createNOr")) {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    createCircuit(new NOr());
                }
            }.setToolTip(Lang.get("menu_table_createNOr_tt")).createJMenuItem());

            createMenu.add(new ToolTipAction(Lang.get("menu_table_createNOrTwo")) {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    createCircuit(new TwoInputs(), new NOr());
                }
            }.setToolTip(Lang.get("menu_table_createNOrTwo_tt")).createJMenuItem());

        }

        JMenu hardware = new JMenu(Lang.get("menu_table_create_hardware"));
        register(hardware, new GenerateCUPL(CuplExporter::new, "GAL16v8/CUPL"));
        register(hardware, new GenerateFile("jed", () -> new ExpressionToFileExporter(new Gal16v8JEDECExporter()),
                "GAL16v8/JEDEC", Lang.get("menu_table_create_jedec_tt")));
        register(hardware, new GenerateCUPL(Gal22v10CuplExporter::new, "GAL22v10/CUPL"));
        register(hardware, new GenerateFile("jed", () -> new ExpressionToFileExporter(new Gal22v10JEDECExporter()),
                "GAL22v10/JEDEC", Lang.get("menu_table_create_jedec_tt")));
        for (ATFDevice atfDev : ATFDevice.values()) {
            register(hardware, new GenerateCUPL(atfDev::getCuplExporter, "ATF150x/" + atfDev.getMenuName() + "/CUPL"));
            register(hardware, new GenerateFile("tt2",
                    () -> atfDev.createExpressionToFileExporter(TableDialog.this, getProjectName()),
                    "ATF150x/" + atfDev.getMenuName() + "/TT2, JEDEC",
                    Lang.get("menu_table_createTT2_tt")));
        }
        createMenu.add(hardware);

        return createMenu;
    }

    private void register(JMenu hardware, final HardwareDescriptionGenerator generator) {
        availGenerators.put(generator.getMenuPath(), generator);
        JMenu m = hardware;
        String path = generator.getMenuPath();
        StringTokenizer tok = new StringTokenizer(path, "/");
        while (tok.hasMoreTokens()) {
            String menuName = tok.nextToken();
            if (tok.hasMoreTokens()) {
                JMenu toUse = null;
                for (int i = 0; i < m.getMenuComponentCount(); i++) {
                    Component menuComponent = m.getMenuComponent(i);
                    if (menuComponent instanceof JMenu) {
                        JMenu menu = (JMenu) menuComponent;
                        if (menu.getText().equalsIgnoreCase(menuName))
                            toUse = menu;
                    }
                }
                if (toUse == null) {
                    toUse = new JMenu(menuName);
                    m.add(toUse);
                }
                m = toUse;
            } else {
                m.add(new HardwareToolTipAction(menuName, generator).createJMenuItem());
            }
        }
    }

    private void createCircuit(ExpressionModifier... modifier) {
        createCircuit(false, false, modifier);
    }

    private void createCircuit(boolean useJKff, boolean useLUTs, ExpressionModifier... modifier) {
        try {
            final ModelAnalyserInfo modelAnalyzerInfo = undoManager.getActual().getModelAnalyzerInfo();
            CircuitBuilder circuitBuilder = new CircuitBuilder(shapeFactory, undoManager.getActual().getVars())
                    .setUseJK(useJKff)
                    .setUseLUTs(useLUTs)
                    .setModelAnalyzerInfo(modelAnalyzerInfo);
            new BuilderExpressionCreator(circuitBuilder, modifier)
                    .setUseJKOptimizer(useJKff)
                    .create(lastGeneratedExpressions);
            Circuit circuit = circuitBuilder.createCircuit();

            new Main.MainBuilder()
                    .setParent(TableDialog.this)
                    .setLibrary(library)
                    .setCircuit(circuit)
                    .setBaseFileName(filename)
                    .openLater();
        } catch (ExpressionException | FormatterException | RuntimeException e) {
            new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e).show(this);
        }
    }

    /**
     * @return the used table model
     */
    public TruthTableTableModel getModel() {
        return model;
    }

    private String getProjectName() {
        if (filename == null)
            return "unknown";
        else return filename.getName();
    }

    /**
     * @return the all solutions dialog
     */
    public AllSolutionsDialog getAllSolutionsDialog() {
        return allSolutionsDialog;
    }

    private void setLastUsedGenerator(HardwareDescriptionGenerator generator) {
        if (lastUsedGenratorMenuItem != null)
            hardwareMenu.remove(lastUsedGenratorMenuItem);
        lastUsedGenratorMenuItem = new HardwareToolTipAction(generator.getMenuPath().replace("/", " → "), generator).createJMenuItem();
        hardwareMenu.add(lastUsedGenratorMenuItem);
        PREFS.put("gen", generator.getMenuPath());
    }

    private void checkLastUsedGenerator() {
        String lu = PREFS.get("gen", "");
        if (lu.length() > 0) {
            HardwareDescriptionGenerator gen = availGenerators.get(lu);
            if (gen != null)
                setLastUsedGenerator(gen);
        }
    }

    /**
     * @return the undoManager
     */
    public UndoManager<TruthTable> getUndoManager() {
        return undoManager;
    }

    private class CalculationTableModelListener implements TableModelListener {
        @Override
        public void tableChanged(TableModelEvent tableModelEvent) {
            calculateExpressions();
        }
    }

    private void calculateExpressions() {
        try {
            LOGGER.info("start optimization");
            ExpressionListener expressionListener = new OutputExpressionListener();

            if (createJK.isSelected())
                expressionListener = new ExpressionListenerJK(expressionListener);

            final TruthTable table = undoManager.getActual();
            if (table.getVars().size() >= 8) {
                ProgressDialog progress = new ProgressDialog(this);

                ExpressionListener finalExpressionListener = expressionListener;
                new Thread(() -> {
                    ExpressionListenerStore storage = new ExpressionListenerStore(null);
                    try {
                        new ExpressionCreator(table).setProgressListener(progress).create(storage);
                    } catch (ExpressionException | FormatterException | AnalyseException e) {
                        SwingUtilities.invokeLater(() -> {
                            progress.dispose();
                            lastGeneratedExpressions = null;
                            new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e).show(this);
                        });
                        return;
                    }

                    SwingUtilities.invokeLater(() -> {
                        try {
                            lastGeneratedExpressions = new ExpressionListenerStore(finalExpressionListener);
                            storage.replayTo(lastGeneratedExpressions);
                            lastGeneratedExpressions.close();
                            kvMap.setResult(table, lastGeneratedExpressions.getResults());
                        } catch (FormatterException | ExpressionException e) {
                            lastGeneratedExpressions = null;
                            new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e).show(this);
                        }
                    });

                }).start();
            } else {
                lastGeneratedExpressions = new ExpressionListenerStore(expressionListener);
                new ExpressionCreator(table).create(lastGeneratedExpressions);
                kvMap.setResult(table, lastGeneratedExpressions.getResults());
            }

            LOGGER.info("optimization finished");
        } catch (ExpressionException | FormatterException | AnalyseException e1) {
            lastGeneratedExpressions = null;
            new ErrorMessage(Lang.get("msg_errorDuringCalculation")).addCause(e1).show(this);
        }
    }

    /**
     * @return the last generated expressions
     */
    public ExpressionListenerStore getLastGeneratedExpressions() {
        return lastGeneratedExpressions;
    }

    private final class SizeAction extends AbstractAction {
        private int n;

        private SizeAction(int n) {
            super(Lang.get("menu_table_N_variables", n));
            this.n = n;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            undoManager.setInitial(new TruthTable(n).addResult());
            tableChanged();
        }
    }

    private final class SizeSequentialAction extends AbstractAction {
        private int n;

        private SizeSequentialAction(int n) {
            super(Lang.get("menu_table_N_variables", n));
            this.n = n;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ArrayList<Variable> vars = new ArrayList<>();
            for (int i = n - 1; i >= 0; i--)
                vars.add(new Variable("Q_" + i + "^n"));
            TruthTable truthTable = new TruthTable(vars);
            int i = n - 1;
            int rows = 1 << n;
            for (Variable v : vars) {
                BoolTableByteArray val = new BoolTableByteArray(rows);
                for (int n = 0; n < rows; n++)
                    val.set(n, ((n + 1) >> i) & 1);
                truthTable.addResult(addOne(v.getIdentifier()), val);
                i--;
            }

            undoManager.setInitial(truthTable);
            tableChanged();
        }
    }

    private final class SizeSequentialBidirectionalAction extends AbstractAction {
        private int n;

        private SizeSequentialBidirectionalAction(int n) {
            super(Lang.get("menu_table_N_variables", n));
            this.n = n;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ArrayList<Variable> vars = new ArrayList<>();
            vars.add(new Variable("D"));
            for (int i = n - 1; i >= 0; i--)
                vars.add(new Variable("Q_" + i + "^n"));
            TruthTable truthTable = new TruthTable(vars);
            int i = n - 1;
            int rows = 1 << (n + 1);
            for (int vi = 1; vi < vars.size(); vi++) {
                BoolTableByteArray val = new BoolTableByteArray(rows);
                for (int n = 0; n < rows; n++) {
                    if (n >= rows / 2)
                        val.set(n, ((n - 1) >> i) & 1);
                    else
                        val.set(n, ((n + 1) >> i) & 1);
                }
                truthTable.addResult(addOne(vars.get(vi).getIdentifier()), val);
                i--;
            }

            undoManager.setInitial(truthTable);
            tableChanged();
        }
    }

    private final class CenterDefaultTableCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(font);
            if (column < undoManager.getActual().getVars().size())
                label.setBackground(MYGRAY);
            else
                label.setBackground(Color.WHITE);

            if (value instanceof Integer) {
                int v = (int) value;
                if (v > 1)
                    label.setText("x");
            }

            return label;
        }
    }

    private static final class StringDefaultTableCellRenderer extends DefaultTableCellRenderer {
        private StringDefaultTableCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setBackground(MYGRAY);

            try {
                label.setText(PlainTextFormatter.format(new Parser(value.toString()).parse()));
            } catch (ParseException e) {
                label.setText(value.toString());
            }
            setBorder(BorderFactory.createRaisedBevelBorder());

            return label;
        }
    }

    private final class OutputExpressionListener implements ExpressionListener {
        private final ArrayList<Expression> expressions;

        private OutputExpressionListener() {
            expressions = new ArrayList<>();
        }

        @Override
        public void resultFound(String name, Expression expression) {
            if (name.endsWith("^n+1"))
                name = name.substring(0, name.length() - 4) + "^{n+1}";
            expressions.add(new NamedExpression(name, expression));
        }

        @Override
        public void close() {
            SwingUtilities.invokeLater(() -> {
                switch (expressions.size()) {
                    case 0:
                        statusBar.setVisible(false);
                        allSolutionsDialog.setNeeded(false);
                        break;
                    case 1:
                        statusBar.setVisible(true);
                        statusBar.setExpression(expressions.get(0));
                        allSolutionsDialog.setNeeded(false);
                        break;
                    default:
                        statusBar.setVisible(false);
                        allSolutionsDialog.setExpressions(expressions);
                        allSolutionsDialog.setNeeded(true);
                        toFront();
                }
            });
        }
    }

    private final class SetAction extends AbstractAction {
        private final int value;

        private SetAction(int value) {
            this.value = value;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int r = table.getSelectedRow();
            int c = table.getSelectedColumn();
            if (r < 0 || c < 0) {
                r = 0;
                c = undoManager.getActual().getVars().size();
            }
            model.setValueAt(value, r, c);

            c++;
            if (c >= table.getColumnCount()) {
                c = undoManager.getActual().getVars().size();
                r++;
                if (r >= model.getRowCount())
                    r = 0;
            }
            table.changeSelection(r, c, false, false);
        }
    }

    private final class HardwareToolTipAction extends ToolTipAction {
        private final HardwareDescriptionGenerator generator;

        private HardwareToolTipAction(String menuName, HardwareDescriptionGenerator generator) {
            super(menuName);
            this.generator = generator;
            setToolTip(generator.getDescription());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                generator.generate(TableDialog.this, filename, undoManager.getActual(), lastGeneratedExpressions);
                setLastUsedGenerator(generator);
            } catch (Exception e1) {
                new ErrorMessage(Lang.get("msg_errorDuringHardwareExport")).addCause(e1).show(TableDialog.this);
            }
        }
    }
}
