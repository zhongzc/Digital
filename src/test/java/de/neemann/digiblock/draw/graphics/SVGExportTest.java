/*
 * Copyright (c) 2019 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.graphics;

import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.elements.PinException;
import de.neemann.digiblock.draw.library.ElementNotFoundException;
import de.neemann.digiblock.integration.ToBreakRunner;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SVGExportTest extends TestCase {

    private static ByteArrayOutputStream export(String file, ExportFactory creator) throws NodeException, PinException, IOException, ElementNotFoundException {
        Circuit circuit = new ToBreakRunner(file).getCircuit();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Export(circuit, creator).export(baos);
        return baos;
    }

    public void testSVGExportLabel() throws NodeException, PinException, IOException, ElementNotFoundException {
        ElementAttributes attr = new ElementAttributes()
                .set(SVGSettings.LATEX, true);
        ByteArrayOutputStream baos
                = export("dig/export/labels.dig",
                (out) -> new GraphicSVG(out, attr));

        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(actual.contains("$A$"));
        assertTrue(actual.contains("$Y_n$"));
    }

    public void testSVGExportLabel2() throws NodeException, PinException, IOException, ElementNotFoundException {
        ElementAttributes attr = new ElementAttributes()
                .set(SVGSettings.LATEX, true)
                .set(SVGSettings.PINS_IN_MATH_MODE, false);
        ByteArrayOutputStream baos
                = export("dig/export/labels2.dig",
                (out) -> new GraphicSVG(out, attr));

        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertFalse(actual.contains("$A$"));
        assertTrue(actual.contains("$Y_n$"));
    }

    public void testSVGExportLabel3() throws NodeException, PinException, IOException, ElementNotFoundException {
        ElementAttributes attr = new ElementAttributes()
                .set(SVGSettings.LATEX, true)
                .set(SVGSettings.PINS_IN_MATH_MODE, true);
        ByteArrayOutputStream baos
                = export("dig/export/labels2.dig",
                (out) -> new GraphicSVG(out, attr));

        String actual = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(actual.contains("$A$"));
        assertTrue(actual.contains("$Y_n$"));
    }


}