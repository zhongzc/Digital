/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.testing;

import de.neemann.digiblock.core.Model;
import de.neemann.digiblock.core.Node;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.memory.DataField;
import de.neemann.digiblock.core.memory.ProgramMemory;
import de.neemann.digiblock.core.memory.RAMInterface;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.elements.PinException;
import de.neemann.digiblock.draw.library.ElementLibrary;
import de.neemann.digiblock.draw.library.ElementNotFoundException;
import de.neemann.digiblock.draw.model.ModelCreator;
import de.neemann.digiblock.draw.shapes.ShapeFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper to test circuits
 */
public class UnitTester {
    private final Model model;
    private boolean initCalled = false;

    /**
     * Creates a new instanve
     *
     * @param file the file to load
     * @throws IOException              IOException
     * @throws ElementNotFoundException ElementNotFoundException
     * @throws PinException             PinException
     * @throws NodeException            NodeException
     */
    public UnitTester(File file) throws IOException, ElementNotFoundException, PinException, NodeException {
        ElementLibrary library = new ElementLibrary();
        library.setRootFilePath(file.getParentFile());
        initLibrary(library);
        ShapeFactory shapeFactory = new ShapeFactory(library);
        Circuit circuit = Circuit.loadCircuit(file, shapeFactory);
        model = new ModelCreator(circuit, library).createModel(false);
    }

    /**
     * Overload this method if you have to modify the library.
     *
     * @param library the used library
     */
    protected void initLibrary(ElementLibrary library) {
    }

    /**
     * Writed data to a memory component
     *
     * @param filter the filter to identify the memory component
     * @param data   the data to write
     * @return this for chained calls
     * @throws TestException TestException
     */
    public UnitTester writeDataTo(MemoryFilter filter, DataField data) throws TestException {
        getMemory(filter).setProgramMemory(data);
        return this;
    }

    /**
     * Reruts the memory idetified by the filter
     *
     * @param filter the filter to identify the memory component
     * @return the memory component
     * @throws TestException TestException
     */
    public ProgramMemory getMemory(MemoryFilter filter) throws TestException {
        Node node = getNode(n -> n instanceof ProgramMemory && filter.accept((ProgramMemory) n));
        return (ProgramMemory) node;
    }

    /**
     * Used to get the RAM if there is only on in the circuit
     *
     * @return the RSM component
     * @throws TestException TestException
     */
    public RAMInterface getRAM() throws TestException {
        return getRAM(pm -> true);
    }

    /**
     * Used to get a RAM component from the circuit
     *
     * @param filter the filter to identify the memory component
     * @return the memory component
     * @throws TestException TestException
     */
    public RAMInterface getRAM(MemoryFilter filter) throws TestException {
        Node node = getNode(n -> n instanceof RAMInterface && filter.accept((RAMInterface) n));
        return (RAMInterface) node;
    }

    /**
     * Used to find a specific node in the circuit.
     *
     * @param filter the filter io identify the node.
     * @return the node
     * @throws TestException TestException
     */
    public Node getNode(Model.NodeFilter<Node> filter) throws TestException {
        List<Node> list = model.findNode(filter);
        if (list.size() == 0)
            throw new TestException("no node found");
        else if (list.size() > 1)
            throw new TestException("multiple nodes found");
        else {
            return list.get(0);
        }
    }

    /**
     * Runs the simulation until a break signal is detected
     *
     * @return this for chained calls
     * @throws TestException TestException
     * @throws NodeException NodeException
     */
    public UnitTester runToBreak() throws TestException, NodeException {
        if (!model.isRunToBreakAllowed())
            throw new TestException("model has no break or no clock element");

        getModel().runToBreak();
        return this;
    }

    /**
     * @return the model
     * @throws NodeException NodeException
     */
    public Model getModel() throws NodeException {
        if (!initCalled) {
            model.init();
            initCalled = true;
        }
        return model;
    }

    /**
     * Exception according to this test
     */
    public static final class TestException extends Exception {
        private TestException(String message) {
            super(message);
        }
    }

    /**
     * Filter to identify a memory component.
     */
    public interface MemoryFilter {
        /**
         * Used to identify the component
         *
         * @param pm the memory component
         * @return true if this component is to use
         */
        boolean accept(ProgramMemory pm);
    }
}
