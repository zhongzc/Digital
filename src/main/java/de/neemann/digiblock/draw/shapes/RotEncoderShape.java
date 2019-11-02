/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.shapes;

import de.neemann.digiblock.core.Observer;
import de.neemann.digiblock.core.SyncAccess;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementAttributes;
import de.neemann.digiblock.core.element.PinDescriptions;
import de.neemann.digiblock.draw.elements.IOState;
import de.neemann.digiblock.draw.elements.Pin;
import de.neemann.digiblock.draw.elements.Pins;
import de.neemann.digiblock.draw.graphics.*;
import de.neemann.digiblock.draw.graphics.Polygon;
import de.neemann.digiblock.gui.components.CircuitComponent;

import java.awt.*;

import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE;
import static de.neemann.digiblock.draw.shapes.GenericShape.SIZE2;


/**
 * The rotary encoder shape
 */
public class RotEncoderShape implements Shape {
    private static final Style KNOB = Style.NORMAL.deriveStyle(Style.MAXLINETHICK, true, Color.DARK_GRAY);
    private static final Style MARKER = Style.NORMAL.deriveStyle(Style.MAXLINETHICK, false, Color.LIGHT_GRAY);
    private static final Vector CENTER = new Vector(SIZE2 - SIZE * 2, SIZE2);
    private final String label;
    private final PinDescriptions outputs;
    private int state;

    /**
     * Creates a new instance
     *
     * @param attr    the attributes
     * @param inputs  the inputs
     * @param outputs the outputs
     */
    public RotEncoderShape(ElementAttributes attr, PinDescriptions inputs, PinDescriptions outputs) {
        this.outputs = outputs;
        this.label = attr.getLabel();
    }

    @Override
    public Pins getPins() {
        return new Pins()
                .add(new Pin(new Vector(0, 0), outputs.get(0)))
                .add(new Pin(new Vector(0, SIZE), outputs.get(1)));
    }

    @Override
    public InteractorInterface applyStateMonitor(IOState ioState, Observer guiObserver) {
        ioState.getOutput(0).addObserverToValue(guiObserver);
        return new InteractorInterface() {

            private int initialState;
            private boolean initial;

            @Override
            public boolean clicked(CircuitComponent cc, Point pos, IOState ioState, Element element, SyncAccess modelSync) {
                return false;
            }

            @Override
            public boolean pressed(CircuitComponent cc, Point pos, IOState ioState, Element element, SyncAccess modelSync) {
                initial = true;
                return false;
            }

            @Override
            public boolean released(CircuitComponent cc, Point pos, IOState ioState, Element element, SyncAccess modelSync) {
                return false;
            }

            @Override
            public boolean dragged(CircuitComponent cc, Point posOnScreen, Vector pos, Transform trans, IOState ioState, Element element, SyncAccess modelSync) {
                if (ioState != null) {
                    Vector p = pos.sub(trans.transform(CENTER));
                    final int dist = p.x * p.x + p.y * p.y;
                    if (dist > 100 && dist < 900) {
                        int s = (int) (Math.atan2(p.y, p.x) / Math.PI * 16);
                        if (initial) {
                            initialState = s;
                            initial = false;
                        } else {
                            if (s != initialState) {
                                state += s - initialState;
                                initialState = s;
                                modelSync.access(() -> {
                                    boolean a = (state & 2) != 0;
                                    boolean b = ((state + 1) & 2) != 0;
                                    ioState.getOutput(0).setBool(a);
                                    ioState.getOutput(1).setBool(b);
                                });
                                return true;
                            }
                        }
                    } else
                        initial = true;
                }
                return false;
            }
        };
    }

    @Override
    public void drawTo(Graphic graphic, Style heighLight) {
        graphic.drawPolygon(new Polygon(true)
                .add(0, -SIZE)
                .add(0, SIZE * 2)
                .add(-SIZE * 3, SIZE * 2)
                .add(-SIZE * 3, -SIZE), Style.NORMAL);

        final int r = SIZE;
        graphic.drawCircle(CENTER.add(-r, -r), CENTER.add(r, r), KNOB);

        final double alpha = state / 16.0 * Math.PI;
        int x = (int) Math.round(SIZE * Math.cos(alpha));
        int y = (int) Math.round(SIZE * Math.sin(alpha));

        graphic.drawLine(CENTER, CENTER.add(x, y), MARKER);

        Vector textPos = CENTER.add(0, SIZE2 * 3 + 4);
        graphic.drawText(textPos, textPos.add(1, 0), label, Orientation.CENTERTOP, Style.NORMAL);
    }

}
