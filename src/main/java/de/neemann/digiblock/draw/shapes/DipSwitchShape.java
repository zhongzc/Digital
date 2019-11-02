/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.shapes;

import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.Observer;
import de.neemann.digiblock.core.SyncAccess;
import de.neemann.digiblock.core.Value;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.element.PinDescriptions;
import de.neemann.digiblock.draw.elements.IOState;
import de.neemann.digiblock.draw.elements.Pin;
import de.neemann.digiblock.draw.elements.Pins;
import de.neemann.digiblock.draw.graphics.*;
import de.neemann.digiblock.draw.graphics.Polygon;
import de.neemann.digiblock.gui.components.CircuitComponent;

import java.awt.*;

import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE2;
import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE;

/**
 * The input shape
 */
public class DipSwitchShape implements Shape {

    private static final Polygon FRAME = new Polygon(true)
            .add(-1, SIZE2)
            .add(-SIZE * 3 + 1, SIZE2)
            .add(-SIZE * 3 + 1, -SIZE2)
            .add(-1, -SIZE2);

    private static final Polygon ON = new Polygon(true)
            .add(-5, SIZE2 - 4)
            .add(-SIZE - SIZE2, SIZE2 - 4)
            .add(-SIZE - SIZE2, -SIZE2 + 4)
            .add(-5, -SIZE2 + 4);

    private static final Polygon OFF = new Polygon(true)
            .add(-SIZE * 3 + 5, SIZE2 - 4)
            .add(-SIZE - SIZE2, SIZE2 - 4)
            .add(-SIZE - SIZE2, -SIZE2 + 4)
            .add(-SIZE * 3 + 5, -SIZE2 + 4);

    private static final Style STYLE = Style.THIN.deriveFillStyle(Color.GRAY);

    private final String label;
    private final PinDescriptions outputs;
    private final boolean defValue;
    private IOState ioState;
    private Value value;

    /**
     * Creates a new instance
     *
     * @param attr    the attributes
     * @param inputs  the inputs
     * @param outputs the outputs
     */
    public DipSwitchShape(ElementAttributes attr, PinDescriptions inputs, PinDescriptions outputs) {
        this.outputs = outputs;
        this.label = attr.getLabel();
        defValue = attr.get(Keys.DIP_DEFAULT);
    }

    @Override
    public Pins getPins() {
        return new Pins().add(new Pin(new Vector(0, 0), outputs.get(0)));
    }

    @Override
    public Interactor applyStateMonitor(IOState ioState, Observer guiObserver) {
        this.ioState = ioState;
        ioState.getOutput(0).addObserverToValue(guiObserver);
        return new Interactor() {
            @Override
            public boolean clicked(CircuitComponent cc, Point pos, IOState ioState, Element element, SyncAccess modelSync) {
                ObservableValue value = ioState.getOutput(0);
                modelSync.access(() -> value.setValue(1 - value.getValue()));
                return true;
            }
        };
    }

    @Override
    public void readObservableValues() {
        if (ioState != null)
            value = ioState.getOutput(0).getCopy();
    }

    @Override
    public void drawTo(Graphic graphic, Style heighLight) {
        graphic.drawPolygon(FRAME, Style.NORMAL);
        Vector pos = new Vector(-SIZE * 3 - SIZE2, 0);
        if (label.length() > 0)
            graphic.drawText(pos, pos.add(-1, 0), label, Orientation.LEFTCENTER, Style.NORMAL);

        boolean on = defValue;
        if (value != null)
            on = value.getBool();

        if (on)
            graphic.drawPolygon(ON, STYLE);
        else
            graphic.drawPolygon(OFF, STYLE);
    }
}
