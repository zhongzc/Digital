/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.integration;

import de.neemann.digiblock.data.Value;
import de.neemann.digiblock.data.ValueTable;
import de.neemann.digiblock.draw.graphics.Export;
import de.neemann.digiblock.draw.graphics.GraphicsImage;
import de.neemann.digiblock.gui.components.data.ValueTableObserver;
import de.neemann.digiblock.testing.parser.TestRow;
import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

/**
 */
public class TestCaseDescription extends TestCase {

    /**
     * Runs a 4 bit counter build from JK flip flops 16 ticks.
     * The 4 output signals are recoded in a DataSet.
     *
     * @throws Exception
     */
    public void testData() throws Exception {
        ToBreakRunner toBreakRunner = new ToBreakRunner("dig/data.dig").runToBreak(29);

        // check recorded data
        ValueTable dataSet = toBreakRunner.getModel()
                .getObserver(ValueTableObserver.class)
                .getLogData();

        assertEquals(29, dataSet.getRows());
        int i = 0;
        for (TestRow ds : dataSet) {
            assertEquals((~i) & 1, ds.getValue(0).getValue()); // clock
            int s = i / 2 + 1;
            assertEquals(s & 1, ds.getValue(1).getValue());//q_0
            assertEquals((s >> 1) & 1, ds.getValue(2).getValue());//q_1
            assertEquals((s >> 2) & 1, ds.getValue(3).getValue());//q_2
            assertEquals((s >> 3) & 1, ds.getValue(4).getValue());//q_3
            i++;
        }

        // try to write data to graphics instance
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Export(toBreakRunner.getCircuit(),
                (out) -> new GraphicsImage(out, "PNG", 1))
                .export(baos);

        assertTrue(baos.size() > 15000);

        // export data to CSV
        StringWriter w = new StringWriter();
        dataSet.saveCSV(new BufferedWriter(w));
        assertEquals("\"step\",\"C\",\"q_0n\",\"q_1n\",\"q_2n\",\"q_3n\"\n" +
                "\"0\",\"1\",\"1\",\"0\",\"0\",\"0\"\n" +
                "\"1\",\"0\",\"1\",\"0\",\"0\",\"0\"\n" +
                "\"2\",\"1\",\"0\",\"1\",\"0\",\"0\"\n" +
                "\"3\",\"0\",\"0\",\"1\",\"0\",\"0\"\n" +
                "\"4\",\"1\",\"1\",\"1\",\"0\",\"0\"\n" +
                "\"5\",\"0\",\"1\",\"1\",\"0\",\"0\"\n" +
                "\"6\",\"1\",\"0\",\"0\",\"1\",\"0\"\n" +
                "\"7\",\"0\",\"0\",\"0\",\"1\",\"0\"\n" +
                "\"8\",\"1\",\"1\",\"0\",\"1\",\"0\"\n" +
                "\"9\",\"0\",\"1\",\"0\",\"1\",\"0\"\n" +
                "\"10\",\"1\",\"0\",\"1\",\"1\",\"0\"\n" +
                "\"11\",\"0\",\"0\",\"1\",\"1\",\"0\"\n" +
                "\"12\",\"1\",\"1\",\"1\",\"1\",\"0\"\n" +
                "\"13\",\"0\",\"1\",\"1\",\"1\",\"0\"\n" +
                "\"14\",\"1\",\"0\",\"0\",\"0\",\"1\"\n" +
                "\"15\",\"0\",\"0\",\"0\",\"0\",\"1\"\n" +
                "\"16\",\"1\",\"1\",\"0\",\"0\",\"1\"\n" +
                "\"17\",\"0\",\"1\",\"0\",\"0\",\"1\"\n" +
                "\"18\",\"1\",\"0\",\"1\",\"0\",\"1\"\n" +
                "\"19\",\"0\",\"0\",\"1\",\"0\",\"1\"\n" +
                "\"20\",\"1\",\"1\",\"1\",\"0\",\"1\"\n" +
                "\"21\",\"0\",\"1\",\"1\",\"0\",\"1\"\n" +
                "\"22\",\"1\",\"0\",\"0\",\"1\",\"1\"\n" +
                "\"23\",\"0\",\"0\",\"0\",\"1\",\"1\"\n" +
                "\"24\",\"1\",\"1\",\"0\",\"1\",\"1\"\n" +
                "\"25\",\"0\",\"1\",\"0\",\"1\",\"1\"\n" +
                "\"26\",\"1\",\"0\",\"1\",\"1\",\"1\"\n" +
                "\"27\",\"0\",\"0\",\"1\",\"1\",\"1\"\n" +
                "\"28\",\"1\",\"1\",\"1\",\"1\",\"1\"\n", w.toString());
    }
}
