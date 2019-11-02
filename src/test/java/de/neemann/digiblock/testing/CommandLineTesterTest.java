/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.testing;

import de.neemann.digiblock.integration.Resources;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class CommandLineTesterTest extends TestCase {

    public void test74181() throws IOException {
        File source = new File(Resources.getRoot(), "../../main/dig/lib/DIL Chips/74xx/arithmetic/74181.dig");
        CommandLineTester tester = new CommandLineTester(source);
        int errors = tester.execute();
        assertEquals(0, errors);
        assertEquals(32, tester.getTestsPassed());
    }

    public void testFailing() throws IOException {
        File source = new File(Resources.getRoot(), "dig/failingTest.dig");
        CommandLineTester tester = new CommandLineTester(source);
        int errors = tester.execute();
        assertEquals(1, errors);
        assertEquals(0, tester.getTestsPassed());
    }

    public void testExternalTests() throws IOException {
        File source = new File(Resources.getRoot(), "dig/failingTest.dig");
        CommandLineTester tester = new CommandLineTester(source)
                .useTestCasesFrom(new File(Resources.getRoot(), "../../main/dig/sequential/Counter-D.dig"));

        int errors = tester.execute();
        assertEquals(0, errors);
        assertEquals(1, tester.getTestsPassed());
    }

}