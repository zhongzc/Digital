/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.elements;

import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.ObservableValues;
import de.neemann.digiblock.lang.Lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE;

/**
 * A list of pins
 */
public class Pins implements Iterable<Pin> {

    private final HashMap<String, Pin> inputs;
    private final HashMap<String, Pin> outputs;
    private final ArrayList<Pin> allPins;

    /**
     * creates a new instance
     */
    public Pins() {
        inputs = new HashMap<>();
        outputs = new HashMap<>();
        allPins = new ArrayList<>();
    }

    /**
     * Adds a pin to this list
     *
     * @param pin the pin
     * @return this for call chaning
     */
    public Pins add(Pin pin) {
        if (pin.getDirection() == Pin.Direction.input)
            inputs.put(pin.getName(), pin);
        else
            outputs.put(pin.getName(), pin);
        allPins.add(pin);
        return this;
    }

    @Override
    public Iterator<Pin> iterator() {
        return allPins.iterator();
    }

    /**
     * Binds the outputs to the pins.
     * The {@link Pin#setValue(ObservableValue)} method is called with one of the given outputs
     *
     * @param outs outputs
     * @throws PinException thrown if pin not found
     */
    public void bindOutputsToOutputPins(ObservableValues outs) throws PinException {
        for (ObservableValue o : outs) {
            Pin pin = outputs.get(o.getName());
            if (pin == null)
                throw new PinException(Lang.get("err_pin_N_unknown", o.getName()));
            pin.setValue(o);
        }
    }

    /**
     * @return a map of inputs
     */
    public HashMap<String, Pin> getInputs() {
        return inputs;
    }

    /**
     * @return the number of pins
     */
    public int size() {
        return allPins.size();
    }

    /**
     * Returns a requested pin
     *
     * @param index the pins index
     * @return the pin
     */
    public Pin get(int index) {
        return allPins.get(index);
    }

    /**
     * @return false if there are two pins next to each other in horizontal direction
     */
    boolean autoWireCompatible() {
        for (Pin p1 : allPins)
            for (Pin p2 : allPins) {
                if (p1.getPos().add(SIZE, 0).equals(p2.getPos()))
                    return false;
            }
        return true;
    }
}
