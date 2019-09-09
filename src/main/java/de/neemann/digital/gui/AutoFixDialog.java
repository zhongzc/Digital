/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.gui;

import de.neemann.digital.core.BitsException;
import de.neemann.digital.core.element.ElementAttributes;
import de.neemann.digital.draw.elements.VisualElement;
import de.neemann.digital.draw.library.ElementNotFoundException;
import de.neemann.digital.gui.components.modification.ModifyAttribute;
import de.neemann.digital.lang.Lang;
import de.neemann.gui.ErrorMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper to create the autofix dialog
 */
public final class AutoFixDialog {

    private AutoFixDialog() {
    }

    /**
     * Creates the dialog
     *
     * @param main    the main window
     * @param message the error message
     * @param cause   the error cause
     * @return a dialog or null if there are no valid fixes
     */
    public static ErrorMessage.ErrorDialog create(Main main, String message, Exception cause) {
        if (cause instanceof BitsException) {
            BitsException bitsException = (BitsException) cause;
            if (!bitsException.hasFix())
                return null;

            ArrayList<ValidFix> validFixes = new ArrayList<>();
            for (BitsException.Fix f : bitsException.getFixes()) {
                ElementAttributes bitSource = f.getBitSource().getAttr();
                List<VisualElement> l = main.getCircuitComponent().getCircuit().getElements(v -> v.getElementAttributes() == bitSource);
                if (l.size() == 1) {
                    VisualElement visualElement = l.get(0);

                    String elementName = visualElement.getElementName();
                    try {
                        elementName = main.getLibrary().getElementType(elementName, visualElement.getElementAttributes()).getTranslatedName();
                    } catch (ElementNotFoundException e) {
                        // do nothing on error
                    }

                    validFixes.add(new ValidFix(visualElement, elementName, f));
                }
            }
            if (!validFixes.isEmpty())
                return createDialog(main, message, cause, validFixes);
        }
        return null;
    }

    private static ErrorMessage.ErrorDialog createDialog(Main main, String message, Exception cause, ArrayList<ValidFix> validFixes) {
        ErrorMessage.ErrorDialog dialog = new ErrorMessage(message).addCause(cause).createDialog(main);

        JPanel p = new JPanel(new GridLayout(0, 1));
        for (ValidFix f : validFixes) {
            JButton button = new JButton(new FixAction(main, dialog, f));
            JPanel bp = new JPanel();
            bp.add(button);
            p.add(bp);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton button = new JButton(new AbstractAction(Lang.get("ok")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.dispose();
            }
        });
        buttons.add(button);
        p.add(buttons);


        dialog.add(p, BorderLayout.SOUTH);
        dialog.pack();

        return dialog;
    }

    private static final class ValidFix {
        private final VisualElement visualElement;
        private final String elementName;
        private final BitsException.Fix fix;

        private ValidFix(VisualElement visualElement, String elementName, BitsException.Fix fix) {
            this.visualElement = visualElement;
            this.elementName = elementName;
            this.fix = fix;
        }

        private String getName() {
            String label = visualElement.getElementAttributes().getLabel();
            if (!label.isEmpty())
                return elementName + " (" + label + ")";
            return elementName;
        }
    }

    private static final class FixAction extends AbstractAction {
        private final Main main;
        private final ErrorMessage.ErrorDialog dialog;
        private final ValidFix f;

        private FixAction(Main main, ErrorMessage.ErrorDialog dialog, ValidFix f) {
            super(Lang.get("msg_autoFix_N_N_N_N",
                    f.getName(),
                    Lang.get(f.fix.getBitSource().getKey().getLangKey()),
                    f.fix.getBitSource().get(),
                    f.fix.getBitsToFixTheProblem()));
            this.main = main;
            this.dialog = dialog;
            this.f = f;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ModifyAttribute<Integer> m = new ModifyAttribute<>(
                    f.visualElement,
                    f.fix.getBitSource().getKey(),
                    f.fix.getBitsToFixTheProblem());
            main.getCircuitComponent().modify(m);
            dialog.dispose();
        }
    }
}
