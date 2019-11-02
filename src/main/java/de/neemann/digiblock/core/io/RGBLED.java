/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core.io;

import de.neemann.digiblock.core.Model;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValues;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.draw.elements.PinException;

import static de.neemann.digiblock.core.element.PinInfo.input;

/**
 * A RGB LED
 */
public class RGBLED implements Element {

    /**
     * The LED description
     */
    public static final ElementTypeDescription DESCRIPTION
            = new ElementTypeDescription(RGBLED.class, input("R"), input("G"), input("B"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.BITS)
            .addAttribute(Keys.LABEL);

    private final int bits;

    /**
     * Creates a new light bulb
     *
     * @param attr the attributes
     */
    public RGBLED(ElementAttributes attr) {
        bits = attr.getBits();
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        inputs.get(0).checkBits(bits, null, 0);
        inputs.get(1).checkBits(bits, null, 1);
        inputs.get(2).checkBits(bits, null, 2);
    }

    @Override
    public ObservableValues getOutputs() throws PinException {
        return ObservableValues.EMPTY_LIST;
    }

    @Override
    public void registerNodes(Model model) {
    }
}
