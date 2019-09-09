/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.draw.elements;

import de.neemann.digital.core.ExceptionWithOrigin;
import de.neemann.digital.core.FixableException;
import de.neemann.digital.core.element.ValueSource;
import de.neemann.digital.draw.model.Net;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown dealing with pins
 */
public class PinException extends ExceptionWithOrigin implements FixableException {
    private Net net;
    private List<Fix> fixes;

    /**
     * Creates a new instance
     *
     * @param message       the message
     * @param visualElement the visual element affected
     */
    public PinException(String message, VisualElement visualElement) {
        super(message);
        setVisualElement(visualElement);
    }

    /**
     * Creates a new instance
     *
     * @param message the message
     * @param net     the net affected
     */
    public PinException(String message, Net net) {
        super(message);
        this.net = net;
        setOrigin(net.getOrigin());
        setVisualElement(net.getVisualElement());
    }

    /**
     * Creates a new instance
     *
     * @param message the message
     */
    public PinException(String message) {
        super(message);
    }

    /**
     * @return the effected net
     */
    public Net getNet() {
        return net;
    }

    /**
     * Adds a possible fix to this error
     *
     * @param bitSource the wrong value
     * @param bits      the value that would fix the problem
     * @return this for chained calls
     */
    public PinException addFix(ValueSource bitSource, int bits) {
        if (bitSource != null && bits > 0) {
            if (fixes == null)
                fixes = new ArrayList<>();
            fixes.add(new FixableException.Fix(bitSource, bits));
        }
        return this;
    }


    @Override
    public List<Fix> getFixes() {
        return fixes;
    }
}
