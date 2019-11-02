/*
 * Copyright (c) 2018 Ivan Deras
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.hdl.verilog2.lib;

import de.neemann.digiblock.hdl.hgs.HGSEvalException;
import de.neemann.digiblock.hdl.model2.HDLNode;
import de.neemann.digiblock.hdl.printer.CodePrinter;
import java.io.IOException;

/**
 * Responsible of generating the Verilog code.
 *
 * @author ideras
 */
public interface VerilogElement {
    /**
     * Prints the entity to the printer if not allrady written.
     *
     * @param out  the output to print the code to
     * @param node the node to print
     * @return the verilog name of the node
     * @throws HGSEvalException HGSEvalException
     * @throws IOException      IOException
     */
    String print(CodePrinter out, HDLNode node) throws HGSEvalException, IOException;

    /**
     * Write the generic map of this node
     *
     * @param out  the output to write to
     * @param node the node
     * @throws IOException IOException
     */
    void writeGenericMap(CodePrinter out, HDLNode node) throws IOException;
}
