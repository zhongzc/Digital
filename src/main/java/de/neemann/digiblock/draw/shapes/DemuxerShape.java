/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.shapes;

import de.neemann.digiblock.core.Observer;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.element.PinDescriptions;
import de.neemann.digiblock.draw.elements.IOState;
import de.neemann.digiblock.draw.elements.Pin;
import de.neemann.digiblock.draw.elements.Pins;
import de.neemann.digiblock.draw.graphics.*;

import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE;

/**
 * The Demuxer shape
 */
public class DemuxerShape implements Shape {
    private final int outputCount;
    private final boolean hasInput;
    private final boolean flip;
    private final int height;
    private final PinDescriptions inputs;
    private final PinDescriptions outputs;
    private Pins pins;

    /**
     * Creates a new instance
     *
     * @param attr    the attributes
     * @param inputs  the inputs
     * @param outputs the outputs
     */
    public DemuxerShape(ElementAttributes attr, PinDescriptions inputs, PinDescriptions outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.flip = attr.get(Keys.FLIP_SEL_POSITON);
        outputCount = 1 << attr.get(Keys.SELECTOR_BITS);
        hasInput = inputs.size() > 1;
        height = hasInput || (outputCount <= 2) ? outputCount * SIZE : (outputCount - 1) * SIZE;
    }

    @Override
    public Pins getPins() {
        if (pins == null) {
            pins = new Pins();
            pins.add(new Pin(new Vector(SIZE, flip ? 0 : height), inputs.get(0)));
            if (outputCount == 2) {
                pins.add(new Pin(new Vector(SIZE * 2, 0 * SIZE), outputs.get(0)));
                pins.add(new Pin(new Vector(SIZE * 2, 2 * SIZE), outputs.get(1)));
            } else
                for (int i = 0; i < outputCount; i++) {
                    pins.add(new Pin(new Vector(SIZE * 2, i * SIZE), outputs.get(i)));
                }
            if (hasInput)
                pins.add(new Pin(new Vector(0, (outputCount / 2) * SIZE), inputs.get(1)));
        }
        return pins;
    }

    @Override
    public Interactor applyStateMonitor(IOState ioState, Observer guiObserver) {
        return null;
    }

    @Override
    public void drawTo(Graphic graphic, Style highLight) {
        graphic.drawPolygon(new Polygon(true)
                .add(1, 5)
                .add(SIZE * 2 - 1, -4)
                .add(SIZE * 2 - 1, height + 4)
                .add(1, height - 5), Style.NORMAL);
        graphic.drawText(new Vector(SIZE * 2 - 3, 2), new Vector(SIZE * 2, 2), "0", Orientation.RIGHTTOP, Style.SHAPE_PIN);
    }
}
