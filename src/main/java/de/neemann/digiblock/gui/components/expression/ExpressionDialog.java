/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.components.expression;

import de.neemann.digiblock.analyse.expression.Expression;
import de.neemann.digiblock.analyse.expression.format.FormatToExpression;
import de.neemann.digiblock.analyse.parser.Parser;
import de.neemann.digiblock.builder.circuit.CircuitBuilder;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.library.ElementLibrary;
import de.neemann.digiblock.draw.shapes.ShapeFactory;
import de.neemann.digiblock.gui.Main;
import de.neemann.digiblock.gui.components.table.ShowStringDialog;
import de.neemann.digiblock.lang.Lang;
import de.neemann.gui.ErrorMessage;
import de.neemann.gui.ToolTipAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

/**
 * Dialog to enter an expression.
 * Creates a new frame with a circuit generated from the entered expression.
 */
public class ExpressionDialog extends JDialog {

    /**
     * Creates a new instance
     *
     * @param parent       the parent
     * @param library      the library to use
     * @param shapeFactory the shapeFactory used for new circuits
     * @param baseFilename filname used as base for file operations
     */
    public ExpressionDialog(Main parent, ElementLibrary library, ShapeFactory shapeFactory, File baseFilename) {
        super(parent, Lang.get("expression"), false);

        JTextField text = new JTextField("(C ∨ B) ∧ (A ∨ C) ∧ (B ∨ !C) * (C + !A)", 40);
        getContentPane().add(text, BorderLayout.CENTER);
        getContentPane().add(new JLabel(Lang.get("msg_enterAnExpression")), BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttons, BorderLayout.SOUTH);

        buttons.add(new ToolTipAction(Lang.get("btn_help")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new ShowStringDialog(
                        ExpressionDialog.this,
                        Lang.get("msg_expressionHelpTitle"),
                        Lang.get("msg_expressionHelp"))
                        .setVisible(true);
            }
        }.createJButton());

        buttons.add(new ToolTipAction(Lang.get("btn_create")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ArrayList<Expression> expList = new Parser(text.getText()).parse();
                    CircuitBuilder circuitBuilder = new CircuitBuilder(shapeFactory);
                    if (expList.size() == 1)
                        circuitBuilder.addCombinatorial("Y", expList.get(0));
                    else
                        for (Expression exp : expList)
                            circuitBuilder.addCombinatorial(FormatToExpression.defaultFormat(exp), exp);
                    Circuit circuit = circuitBuilder.createCircuit();
                    new Main.MainBuilder()
                            .setParent(parent)
                            .setLibrary(library)
                            .setCircuit(circuit)
                            .setBaseFileName(baseFilename)
                            .openLater();
                } catch (Exception ex) {
                    new ErrorMessage().addCause(ex).show(ExpressionDialog.this);
                }
            }
        }.setToolTip(Lang.get("btn_create_tt")).createJButton());

        pack();
        setLocationRelativeTo(parent);
    }
}
