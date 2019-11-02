/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core.flipflops;

import de.neemann.digiblock.core.BitsException;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.ObservableValues;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;

import static de.neemann.digiblock.core.element.PinInfo.input;

/**
 * The RS flip-flop
 */
public class FlipflopRSAsync extends FlipflopBit {

    /**
     * The RS-FF description
     */
    public static final ElementTypeDescription DESCRIPTION
            = new ElementTypeDescription("RS_FF_AS", FlipflopRSAsync.class, input("S"), input("R"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.DEFAULT)
            .addAttribute(Keys.INVERTER_CONFIG)
            .addAttribute(Keys.VALUE_IS_PROBE);


    private ObservableValue sVal;
    private ObservableValue rVal;
    private final ObservableValue qVal;
    private final ObservableValue qnVal;
    private boolean q;
    private boolean qn;

    /**
     * Creates a new instance
     *
     * @param attributes the attributes
     */
    public FlipflopRSAsync(ElementAttributes attributes) {
        super(attributes, DESCRIPTION);
        ObservableValues o = getOutputs();
        qVal = o.get(0);
        qnVal = o.get(1);
        q = qVal.getBool();
        qn = qnVal.getBool();
    }

    @Override
    public void readInputs() throws NodeException {
        boolean s = sVal.getBool();
        boolean r = rVal.getBool();

        if (s) {
            if (r) {
                q = true;
                qn = true;
            } else {
                q = true;
                qn = false;
            }
        } else {
            if (r) {
                q = false;
                qn = true;
            } else {
                if (q && qn) {
                    if (Math.random() < 0.5) {
                        q = false;
                    } else {
                        qn = false;
                    }
                }
            }
        }
    }

    @Override
    public void writeOutputs() throws NodeException {
        qVal.setBool(q);
        qnVal.setBool(qn);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws BitsException {
        sVal = inputs.get(0).addObserverToValue(this).checkBits(1, this, 0);
        rVal = inputs.get(1).addObserverToValue(this).checkBits(1, this, 1);
    }

}
