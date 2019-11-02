/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.hdl.vhdl2;

import de.neemann.digiblock.core.ExceptionWithOrigin;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.extern.ProcessStarter;
import de.neemann.digiblock.draw.elements.PinException;
import de.neemann.digiblock.draw.library.ElementNotFoundException;
import de.neemann.digiblock.gui.Settings;
import de.neemann.digiblock.hdl.model2.HDLException;
import de.neemann.digiblock.hdl.printer.CodePrinter;
import de.neemann.digiblock.integration.FileScanner;
import de.neemann.digiblock.integration.Resources;
import de.neemann.digiblock.integration.TestExamples;
import de.neemann.digiblock.integration.ToBreakRunner;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VHDLSimulatorTest extends TestCase {
    private static final String GHDL = System.getProperty("ghdl", "ghdl");
    private int testBenches;

    /*
    public void testDebug() throws Exception {
        File file = new File(Resources.getRoot(), "dig/test/vhdl/lut2.dig");

        ToBreakRunner br = new ToBreakRunner(file);
        System.out.println(new VHDLGenerator(br.getLibrary(), new CodePrinterStr(true)).export(br.getCircuit()));

        checkVHDLExport(file);
    }/**/

    public void testInSimulator() throws Exception {
        File examples = new File(Resources.getRoot(), "/dig/test/vhdl");
        try {
            int tested = new FileScanner(this::checkVHDLExport).noOutput().scan(examples);
            assertEquals(57, tested);
            assertEquals(51, testBenches);
        } catch (FileScanner.SkipAllException e) {
            // if ghdl is not installed its also ok
        }
    }

    public void testInSimulator2() throws Exception {
        File examples = new File(Resources.getRoot(), "/dig/hdl");
        try {
            int tested = new FileScanner(this::checkVHDLExport).noOutput().scan(examples);
            assertEquals(47, tested);
        } catch (FileScanner.SkipAllException e) {
            // if ghdl is not installed its also ok
        }
    }

    public void testDistributedInSimulator() throws Exception {
        File examples = new File(Resources.getRoot(), "../../main/dig/hdl");
        try {
            int tested = new FileScanner(this::checkVHDLExport).noOutput().scan(examples);
            assertEquals(2, tested);
            assertEquals(1, testBenches);
        } catch (FileScanner.SkipAllException e) {
            // if ghdl is not installed its also ok
        }
    }

    public void testProcessorInSimulator() throws Exception {
        File file = new File(Resources.getRoot(), "../../main/dig/processor/HDLExample.dig");
        try {
            checkVHDLExport(file);
        } catch (FileScanner.SkipAllException e) {
            // if ghdl is not installed its also ok
        } catch (Exception e) {
            System.out.println(ExceptionWithOrigin.getOriginOf(e));
            throw e;
        }
    }

    public void testMultiplierInSimulator() throws Exception {
        File file = new File(Resources.getRoot(), "../../main/dig/combinatorial/Multiply8Bit.dig");
        try {
            checkVHDLExport(file);
        } catch (FileScanner.SkipAllException e) {
            // if ghdl is not installed its also ok
        } catch (Exception e) {
            System.out.println(ExceptionWithOrigin.getOriginOf(e));
            throw e;
        }
    }

    public void testGHDLInSimulator() throws Exception {
        try {
            ProcessStarter.start(null, GHDL, "--help");
        } catch (IOException e) {
            // ghdl is not installed, Ignore Test
            return;
        }

        Settings.getInstance().getAttributes().set(Keys.SETTINGS_GHDL_PATH, new File(GHDL));

        File source = new File(Resources.getRoot(), "dig/external/ghdl");

        int tested = new FileScanner(f -> {
            checkVHDLExport(f);
            // check simulation in Digiblock
            new TestExamples().check(f);
        }).noOutput().scan(source);
        assertEquals(4, tested);
    }


    private void checkVHDLExport(File file) throws PinException, NodeException, ElementNotFoundException, IOException, FileScanner.SkipAllException, HDLException {
        ToBreakRunner br = new ToBreakRunner(file);
        File dir = Files.createTempDirectory("digiblock_vhdl_" + getTime() + "_").toFile();
        try {
            File vhdlFile = new File(dir, file.getName()
                    .replace('.', '_')
                    .replace('-', '_')+ ".vhdl");
            CodePrinter out = new CodePrinter(vhdlFile);
            try (VHDLGenerator vhdl = new VHDLGenerator(br.getLibrary(), out)) {
                vhdl.export(br.getCircuit());
                ArrayList<File> testFiles = vhdl.getTestBenches();
                out.close();
                runGHDL(vhdlFile, testFiles);
            }
            ProcessStarter.removeFolder(dir);
        } finally {
            br.close();
        }
    }

    private void runGHDL(File vhdlFile, ArrayList<File> testFileWritten) throws IOException, FileScanner.SkipAllException, HDLException {
        checkWarn(vhdlFile, startProcess(vhdlFile.getParentFile(), GHDL, "-a", "--std=02", "--ieee=synopsys", vhdlFile.getName()));
        checkWarn(vhdlFile, startProcess(vhdlFile.getParentFile(), GHDL, "-e", "--std=02", "--ieee=synopsys", "main"));
        for (File testbench : testFileWritten) {
            String name = testbench.getName();
            checkWarn(testbench, startProcess(vhdlFile.getParentFile(), GHDL, "-a", "--std=02", "--ieee=synopsys", name));
            String module = name.substring(0, name.length() - 5);
            checkWarn(testbench, startProcess(vhdlFile.getParentFile(), GHDL, "-e", "--std=02", "--ieee=synopsys", module));
            String result = startProcess(vhdlFile.getParentFile(), GHDL, "-r", "--std=02", "--ieee=synopsys", module, "--vcd=" + module + ".vcd");
            if (result.contains("(assertion error)"))
                throw new HDLException("test bench " + name + " failed:\n" + result);
            checkWarn(testbench, result);
            testBenches++;
        }
    }

    private void checkWarn(File file, String result) {
        if (result.contains("warning")) {
            System.out.println(file);
            System.out.println(result);
        }
    }

    private String startProcess(File dir, String... args) throws IOException, FileScanner.SkipAllException {
        try {
            return ProcessStarter.start(dir, args);
        } catch (ProcessStarter.CouldNotStartProcessException e) {
            throw new FileScanner.SkipAllException("ghdl (https://github.com/tgingold/ghdl) is not installed! Add ghdl binary to the system path or set system property 'ghdl' to ghdl binary");
        }
    }

    private String getTime() {
        DateFormat f = new SimpleDateFormat("YY-MM-dd_HH-mm_ss");
        return f.format(new Date());
    }

}