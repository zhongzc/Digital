/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.draw.shapes;

import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.Observer;
import de.neemann.digital.core.Value;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.core.element.Keys;
import de.neemann.digital.core.element.PinDescription;
import de.neemann.digital.core.element.PinDescriptions;
import de.neemann.digital.core.io.ButtonLED;
import de.neemann.digital.draw.elements.IOState;
import de.neemann.digital.draw.elements.Pin;
import de.neemann.digital.draw.elements.Pins;
import de.neemann.digital.draw.graphics.Graphic;
import de.neemann.digital.draw.graphics.Style;
import de.neemann.digital.draw.graphics.Vector;

import static de.neemann.digital.draw.shapes.GenericShape.SIZE2;
import static de.neemann.digital.draw.shapes.OutputShape.OUT_SIZE;

/**
 * The shape used for the button combined with a LED.
 */
public class ButtonLEDShape extends ButtonShape {
    private final PinDescription input;
    private final PinDescription output;
    private final Style color;
    private ObservableValue inputValue;
    private Value ledValue;
    private ButtonLED button;

    /**
     * Creates a new instance
     *
     * @param attr    the attributes
     * @param inputs  the inputs
     * @param outputs the outputs
     */
    public ButtonLEDShape(ElementAttributes attr, PinDescriptions inputs, PinDescriptions outputs) {
        super(attr, inputs, outputs);
        input = inputs.get(0);
        output = outputs.get(0);
        color = Style.NORMAL.deriveStyle(0, true, attr.get(Keys.COLOR));
    }

    @Override
    public Pins getPins() {
        return new Pins()
                .add(new Pin(new Vector(0, 0), output))
                .add(new Pin(new Vector(0, 20), input));
    }

    @Override
    public InteractorInterface applyStateMonitor(IOState ioState, Observer guiObserver) {
        inputValue = ioState.getInput(0);
        button = (ButtonLED) ioState.getElement();
        return super.applyStateMonitor(ioState, guiObserver);
    }

    @Override
    public void readObservableValues() {
        if (inputValue != null)
            ledValue = inputValue.getCopy();
    }

    @Override
    public void drawTo(Graphic graphic, Style heighLight) {
        super.drawTo(graphic, heighLight);

        if (ledValue == null || ledValue.getBool()) {
            Vector center;
            if (button != null && button.isPressed()) {
                center = new Vector(-OUT_SIZE - 1, 0);
            } else
                center = new Vector(-OUT_SIZE - 1 - ButtonShape.HEIGHT, -ButtonShape.HEIGHT);
            graphic.drawCircle(center.add(-SIZE2, -SIZE2), center.add(SIZE2, SIZE2), color);
        }
    }
}
