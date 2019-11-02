/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.testing;

import de.neemann.digiblock.testing.parser.LineCollector;
import de.neemann.digiblock.testing.parser.ParserException;
import junit.framework.TestCase;

import java.io.IOException;

/**
 */
public class TestDataTest extends TestCase {
    private static final String DATA1 = "A B\n0 0\n0 1\n1 0\n1 1";
    private static final String DATA2 = "A B\n0 0\n0 1\n1 0\n1 0";
    private static final String DATA3 = "A B\n0 0\n0 1\n1 0\n1 U";

    public void testSetDataNonParseable() throws Exception {
        TestCaseDescription td = new TestCaseDescription(DATA1);

        LineCollector cl = new LineCollector(td.getLines());

        assertEquals(4, cl.getLines().size());
        assertEquals(DATA1, td.getDataString());

        // try to set a non parsable string
        try {
            td.setDataString(DATA3);
            assertTrue(false);
        } catch (IOException | ParserException e) {
            assertTrue(true);
        }
        // TestData remains unchanged!
        assertEquals(DATA1, td.getDataString());
    }

    public void testSetDataParseable() throws Exception {
        TestCaseDescription td = new TestCaseDescription(DATA1);

        LineCollector cl = new LineCollector(td.getLines());

        assertEquals(4, cl.getLines().size());
        assertEquals(DATA1, td.getDataString());

        // try to set a parsable string
        td.setDataString(DATA2);
        // TestData has changed!
        assertEquals(DATA2, td.getDataString());
    }

}
