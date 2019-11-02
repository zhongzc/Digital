/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.components.terminal;

import de.neemann.digiblock.core.Node;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.ObservableValues;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;

import javax.swing.*;

import java.awt.*;

import static de.neemann.digiblock.core.element.PinInfo.input;

/**
 * Component which represents a text terminal.
 */
public class Terminal extends Node implements Element {
    private static final boolean HIDE_DIALOG = GraphicsEnvironment.isHeadless() || System.getProperty("testdata") != null;

    /**
     * The terminal description
     */
    public static final ElementTypeDescription DESCRIPTION
            = new ElementTypeDescription(Terminal.class,
            input("D"),
            input("C").setClock(),
            input("en"))
            .addAttribute(Keys.TERM_WIDTH)
            .addAttribute(Keys.TERM_HEIGHT)
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL);

    private final String label;
    private final ElementAttributes attr;
    private TerminalDialog terminalDialog;
    private ObservableValue data;
    private ObservableValue clock;
    private boolean lastClock;
    private ObservableValue en;

    /**
     * Creates a new terminal instance
     *
     * @param attributes the attributes
     */
    public Terminal(ElementAttributes attributes) {
        label = attributes.getLabel();
        attr = attributes;
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        data = inputs.get(0);
        clock = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        en = inputs.get(2).addObserverToValue(this).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return ObservableValues.EMPTY_LIST;
    }

    @Override
    public void readInputs() throws NodeException {
        boolean clockVal = clock.getBool();
        if (!lastClock && clockVal && en.getBool()) {
            long value = data.getValue();
            if (value != 0 && !HIDE_DIALOG)
                SwingUtilities.invokeLater(() -> {
                    if (terminalDialog == null || !terminalDialog.isVisible()) {
                        terminalDialog = new TerminalDialog(getModel().getWindowPosManager().getMainFrame(), attr);
                        getModel().getWindowPosManager().register("terminal_" + label, terminalDialog);
                    }
                    terminalDialog.addChar((char) value);
                });
        }
        lastClock = clockVal;
    }

    @Override
    public void writeOutputs() throws NodeException {
    }

    /**
     * @return the terminal dialog
     */
    public TerminalDialog getTerminalDialog() {
        return terminalDialog;
    }
}
