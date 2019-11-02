/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core.memory;

import de.neemann.digiblock.core.*;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.stats.Countable;

import static de.neemann.digiblock.core.element.PinInfo.input;

/**
 * A simple register.
 */
public class Register extends Node implements Element, Countable {

    /**
     * The registers {@link ElementTypeDescription}
     */
    public static final ElementTypeDescription DESCRIPTION
            = new ElementTypeDescription(Register.class, input("D"), input("C").setClock(), input("en"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.INVERTER_CONFIG)
            .addAttribute(Keys.IS_PROGRAM_COUNTER)
            .addAttribute(Keys.VALUE_IS_PROBE);

    private final int bits;
    private final boolean isProbe;
    private final String label;
    private final boolean isProgramCounter;
    private ObservableValue dVal;
    private ObservableValue clockVal;
    private ObservableValue enableVal;
    private ObservableValue q;
    private boolean lastClock;
    private long value;

    /**
     * Creates a new instance
     *
     * @param attributes the elements attributes
     */
    public Register(ElementAttributes attributes) {
        super(true);
        bits = attributes.getBits();
        this.q = new ObservableValue("Q", bits).setPinDescription(DESCRIPTION);
        isProbe = attributes.get(Keys.VALUE_IS_PROBE);
        label = attributes.get(Keys.LABEL);
        isProgramCounter = attributes.get(Keys.IS_PROGRAM_COUNTER);
    }

    @Override
    public void readInputs() throws NodeException {
        boolean enable = enableVal.getBool();
        boolean clock = clockVal.getBool();
        if (clock && !lastClock && enable)
            value = dVal.getValue();
        lastClock = clock;
    }

    @Override
    public void writeOutputs() throws NodeException {
        q.setValue(value);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws BitsException {
        dVal = inputs.get(0).checkBits(bits, this);
        clockVal = inputs.get(1).addObserverToValue(this).checkBits(1, this);
        enableVal = inputs.get(2).checkBits(1, this);
    }

    @Override
    public ObservableValues getOutputs() {
        return q.asList();
    }

    @Override
    public void registerNodes(Model model) {
        super.registerNodes(model);
        if (isProbe)
            model.addSignal(new Signal(label, q, (v, z) -> {
                value = v;
                q.setValue(value);
            }));
    }

    /**
     * @return true if this register is the program counter
     */
    public boolean isProgramCounter() {
        return isProgramCounter;
    }

    /**
     * @return the value of this register
     */
    public long getValue() {
        return value;
    }

    @Override
    public int getDataBits() {
        return bits;
    }

}
