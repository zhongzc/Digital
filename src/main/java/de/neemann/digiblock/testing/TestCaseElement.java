/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.testing;

import de.neemann.digiblock.core.Model;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValues;
import de.neemann.digiblock.core.element.*;

/**
 * Dummy to represent the testdata in the circuit.
 */
public class TestCaseElement implements Element {

    /**
     * the used {@link ElementAttributes} key
     */
    public static final Key<TestCaseDescription> TESTDATA = new Key<>("Testdata", () -> new TestCaseDescription(""));

    /**
     * The TestCaseElement description
     */
    public static final ElementTypeDescription TESTCASEDESCRIPTION
            = new ElementTypeDescription("Testcase", TestCaseElement.class)
            .addAttribute(Keys.LABEL)
            .addAttribute(TESTDATA);

    /**
     * creates a new instance
     *
     * @param attributes the attributes
     */
    public TestCaseElement(ElementAttributes attributes) {
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
    }

    @Override
    public ObservableValues getOutputs() {
        return ObservableValues.EMPTY_LIST;
    }

    @Override
    public void registerNodes(Model model) {
    }
}
