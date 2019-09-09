/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.wiring;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;
import de.neemann.digital.core.stats.Countable;
import de.neemann.digital.lang.Lang;

import java.util.ArrayList;

import static de.neemann.digital.core.element.PinInfo.input;


/**
 * The Demultiplexer
 */
public class Demultiplexer extends Node implements Element, Countable {

    private final ValueSource selectorBits;
    private final ValueSource bits;
    private final long defaultValue;
    private final ObservableValues output;
    private ObservableValue selector;
    private ObservableValue input;

    private int oldSelectorValue;
    private int selectorValue;
    private long value;

    /**
     * The Demultiplexer description
     */

    public static final ElementTypeDescription DESCRIPTION = new ElementTypeDescription(Demultiplexer.class, input("sel"), input("in"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.SELECTOR_BITS)
            .addAttribute(Keys.FLIP_SEL_POSITON)
            .addAttribute(Keys.DEFAULT);

    /**
     * Creates a new instance
     *
     * @param attributes the attributes
     */
    public Demultiplexer(ElementAttributes attributes) {
        bits = attributes.getBitSource();
        this.selectorBits = attributes.getSource(Keys.SELECTOR_BITS);
        this.defaultValue = attributes.get(Keys.DEFAULT);
        int outputs = 1 << selectorBits.get();
        ArrayList<ObservableValue> o = new ArrayList<>(outputs);
        for (int i = 0; i < outputs; i++)
            o.add(new ObservableValue("out_" + i, bits).setValue(defaultValue).setDescription(Lang.get("elem_Demultiplexer_output", i)));
        output = new ObservableValues(o);
    }

    @Override
    public ObservableValues getOutputs() {
        return output;
    }

    @Override
    public void readInputs() throws NodeException {
        selectorValue = (int) selector.getValue();
        value = input.getValue();
    }

    @Override
    public void writeOutputs() throws NodeException {
        output.get(oldSelectorValue).setValue(defaultValue);
        output.get(selectorValue).setValue(value);
        oldSelectorValue = selectorValue;
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        selector = inputs.get(0).addObserverToValue(this).checkBits(selectorBits, this);
        input = inputs.get(1).addObserverToValue(this).checkBits(bits, this);
    }

    @Override
    public int getDataBits() {
        return bits.get();
    }

    @Override
    public int getAddrBits() {
        return selectorBits.get();
    }
}
