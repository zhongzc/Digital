/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core.element;

import de.neemann.digiblock.core.memory.DataField;
import de.neemann.digiblock.hdl.hgs.Context;
import de.neemann.digiblock.hdl.hgs.HGSEvalException;
import de.neemann.digiblock.hdl.hgs.Parser;
import de.neemann.digiblock.hdl.hgs.ParserException;
import junit.framework.TestCase;

import java.io.IOException;

public class ElementAttributesTest extends TestCase {

    /**
     * Ensures that the ElementAttributes is accessible from within the template engine
     */
    public void testElementAttibutes() throws IOException, ParserException, HGSEvalException {
        ElementAttributes attr = new ElementAttributes().set(Keys.BITS, 5);
        final Context c = new Context().declareVar("elem", attr);
        new Parser("bits=<?=elem.Bits?>;").parse().execute(c);
        assertEquals("bits=5;", c.toString());
    }

    /**
     * Ensures that the DataField is accessible from within the template engine
     */
    public void testDataField() throws IOException, ParserException, HGSEvalException {
        DataField d = new DataField(5)
                .setData(0, 1)
                .setData(1, 7)
                .setData(2, 4)
                .setData(3, 8)
                .setData(4, 2);
        Context c= new Context().declareVar("d", d);
        new Parser("(<? for(i:=0;i<sizeOf(d);i++) { if (i>0) print(\"-\"); print(d[i]);} ?>)").parse().execute(c);
        assertEquals("(1-7-4-8-2)", c.toString());
    }

}