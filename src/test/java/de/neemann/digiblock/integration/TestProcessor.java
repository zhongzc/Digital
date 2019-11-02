/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.integration;

import de.neemann.digiblock.core.Model;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.memory.DataField;
import de.neemann.digiblock.core.memory.RAMDualPort;
import de.neemann.digiblock.core.memory.RAMSinglePort;
import de.neemann.digiblock.core.memory.ROM;
import de.neemann.digiblock.core.memory.importer.Importer;
import de.neemann.digiblock.draw.elements.PinException;
import de.neemann.digiblock.draw.library.ElementNotFoundException;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 */
public class TestProcessor extends TestCase {

    private ToBreakRunner createProcessor(String program) throws IOException, PinException, NodeException, ElementNotFoundException {
        ToBreakRunner runner = new ToBreakRunner("../../main/dig/processor/Processor.dig", false);
        Model model = runner.getModel();

        ROM rom = null;
        for (ROM r : model.findNode(ROM.class)) {
            if (r.isProgramMemory())
                rom = r;
        }
        assertNotNull(rom);

        rom.setData(Importer.read(new File(Resources.getRoot(), program), rom.getDataBits()));

        runner.getModel().init(true);
        return runner;
    }

    private ToBreakRunner createProcessorMux(String program) throws IOException, PinException, NodeException, ElementNotFoundException {
        ToBreakRunner runner = new ToBreakRunner("../../main/dig/processor/ProcessorMux.dig", false);
        Model model = runner.getModel();

        ObservableValue instr = model.getInput("Instr");
        ObservableValue pc = model.getOutput("PC");
        assertNotNull(instr);
        assertNotNull(pc);

        DataField data = Importer.read(new File(Resources.getRoot(), program), 16);
        pc.addObserverToValue(() -> instr.setValue(data.getDataWord((int) pc.getValue()))).fireHasChanged();

        runner.getModel().init(true);
        return runner;
    }

    /**
     * Loads the simulated processor, and loads a program that calculates the 15th
     * fibonacci number with a simple recursive algorithm. The result (610) is stored in the first RAM word.
     *
     * @throws IOException   IOException
     * @throws NodeException NodeException
     * @throws PinException  PinException
     */
    public void testFibonacci() throws IOException, NodeException, PinException, ElementNotFoundException {
        RAMSinglePort ram =
                createProcessor("programs/fibonacci.hex")
                        .runToBreak(98644)
                        .getSingleNode(RAMSinglePort.class);

        assertEquals(610, ram.getMemory().getDataWord(0));
    }

    /**
     * Loads the simulated processor, and loads a program that calculates the 15th
     * fibonacci number with a simple recursive algorithm. The result (610) is stored in the first RAM word.
     *
     * @throws IOException   IOException
     * @throws NodeException NodeException
     * @throws PinException  PinException
     */
    public void testFibonacciMux() throws IOException, NodeException, PinException, ElementNotFoundException {
        ToBreakRunner processor = createProcessorMux("programs/fibonacci.hex");
        processor.getModel().getInput("reset").setBool(false);
        RAMDualPort ram = processor
                .runToBreak(98644)
                .getSingleNode(RAMDualPort.class);

        assertEquals(610, ram.getMemory().getDataWord(0));
    }


    /**
     * Loads the simulated processor, and loads a processors self test.
     * If a 2 is written to memory address 0x100 test was passed!
     *
     * @throws IOException   IOException
     * @throws NodeException NodeException
     * @throws PinException  PinException
     */
    public void testProcessorSelfTest() throws IOException, NodeException, PinException, ElementNotFoundException {
        RAMSinglePort ram =
                createProcessor("programs/selftest.hex")
                        .runToBreak(700)
                        .getSingleNode(RAMSinglePort.class);

        assertEquals(2, ram.getMemory().getDataWord(256));
    }

    /**
     * Loads the simulated processor, and loads a processors self test.
     * If a 2 is written to memory address 0x100 test was passed!
     *
     * @throws IOException   IOException
     * @throws NodeException NodeException
     * @throws PinException  PinException
     */
    public void testProcessorSelfTestMux() throws IOException, NodeException, PinException, ElementNotFoundException {
        ToBreakRunner processor = createProcessorMux("programs/selftest.hex");
        processor.getModel().getInput("reset").setBool(false);
        RAMDualPort ram = processor
                .runToBreak(700)
                .getSingleNode(RAMDualPort.class);

        assertEquals(2, ram.getMemory().getDataWord(256));
    }

}
