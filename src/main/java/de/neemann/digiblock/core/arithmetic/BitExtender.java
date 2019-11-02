/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core.arithmetic;

import de.neemann.digiblock.core.*;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.lang.Lang;

import static de.neemann.digiblock.core.element.PinInfo.input;

/**
 * Sign extension component.
 * Is not a node because it is just a special kind of wiring.
 */
public class BitExtender implements Element {

    /**
     * Description of the sign extend component.
     */
    public static final ElementTypeDescription DESCRIPTION
            = new ElementTypeDescription(BitExtender.class, input("in"))
            .addAttribute(Keys.ROTATE)
            .addAttribute(Keys.LABEL)
            .addAttribute(Keys.INPUT_BITS)
            .addAttribute(Keys.OUTPUT_BITS);

    private final ObservableValue out;
    private final int outBits;
    private final int inBits;

    /**
     * creates a new instance
     *
     * @param attr the components attributes
     */
    public BitExtender(ElementAttributes attr) {
        outBits = attr.get(Keys.OUTPUT_BITS);
        out = new ObservableValue("out", outBits).setPinDescription(DESCRIPTION);
        inBits = attr.get(Keys.INPUT_BITS);
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        final ObservableValue in = inputs.get(0).checkBits(inBits, null);
        if (inBits >= outBits)
            throw new NodeException(Lang.get("err_notMoreOutBitsThanInBits"));

        final long signMask = Bits.signedFlagMask(inBits);
        final long extendMask = ~Bits.mask(inBits);

        in.addObserver(new NodeWithoutDelay(out) {
            @Override
            public void hasChanged() {
                long inValue = in.getValue();
                if ((inValue & signMask) == 0)
                    out.setValue(inValue);
                else
                    out.setValue(inValue | extendMask);
            }
        }).hasChanged();
    }

    @Override
    public ObservableValues getOutputs() {
        return out.asList();
    }

    @Override
    public void registerNodes(Model model) {
        // has no nodes! Is just wiring
    }

}
