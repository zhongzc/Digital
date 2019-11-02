/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.tutorial;

import de.neemann.digiblock.core.*;
import de.neemann.digiblock.core.basic.XOr;
import de.neemann.digiblock.core.element.Element;
import de.neemann.digiblock.core.element.ElementTypeDescription;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.io.In;
import de.neemann.digiblock.core.io.Out;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.elements.PinException;
import de.neemann.digiblock.draw.elements.VisualElement;
import de.neemann.digiblock.draw.library.ElementNotFoundException;
import de.neemann.digiblock.draw.model.ModelCreator;
import de.neemann.digiblock.gui.Main;
import de.neemann.digiblock.gui.Settings;
import de.neemann.digiblock.gui.components.CircuitComponent;
import de.neemann.digiblock.gui.components.modification.ModifyInsertWire;
import de.neemann.digiblock.lang.Lang;
import de.neemann.digiblock.undo.Modification;
import de.neemann.digiblock.undo.Modifications;
import de.neemann.gui.LineBreaker;
import de.neemann.gui.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * The tutorial dialog.
 */
public class InitialTutorial extends JDialog implements CircuitComponent.TutorialListener {
    private static final ArrayList<Step> STEPS = new ArrayList<>();

    static {
        STEPS.add(new Step("tutorial1", (cc, mod, t) -> contains(cc, In.DESCRIPTION)));
        STEPS.add(new Step("tutorial2", (cc, mod, t) -> contains(cc, In.DESCRIPTION, In.DESCRIPTION)));
        STEPS.add(new Step("tutorial3", (cc, mod, t) -> contains(cc, In.DESCRIPTION, In.DESCRIPTION, XOr.DESCRIPTION)));
        STEPS.add(new Step("tutorial4", (cc, mod, t) -> contains(cc, In.DESCRIPTION, In.DESCRIPTION, XOr.DESCRIPTION, Out.DESCRIPTION)));
        STEPS.add(new Step("tutorial5", (cc, mod, t) -> contains(mod, ModifyInsertWire.class) || isWorking(cc)));
        STEPS.add(new Step("tutorial6", (cc, mod, t) -> isWorking(cc)));
        STEPS.add(new Step("tutorial7", (cc, mod, t) -> t.main.getModel() != null));
        STEPS.add(new Step("tutorial8", (cc, mod, t) -> outputIsHigh(t)));
        STEPS.add(new Step("tutorial9", (cc, mod, t) -> t.main.getModel() == null));
        STEPS.add(new Step("tutorial10", (cc, mod, t) -> isIONamed(cc, 1, t)));
        STEPS.add(new Step("tutorial11", (cc, mod, t) -> isIONamed(cc, 3, t)));
    }

    private final Main main;

    private static boolean outputIsHigh(InitialTutorial t) {
        Model model = t.main.getModel();
        if (model == null)
            return false;
        List<Node> nl = model.getNodes();
        if (nl.size() != 1)
            return false;

        Node n = nl.get(0);
        if (n instanceof Element) {
            Element e = (Element) n;
            try {
                final ObservableValues outputs = e.getOutputs();
                if (outputs.size() != 1)
                    return false;
                else
                    return outputs.get(0).getValue() != 0;
            } catch (PinException ex) {
                return false;
            }
        } else
            return false;
    }

    private static boolean isIONamed(CircuitComponent cc, int expected, InitialTutorial t) {
        HashSet<String> set = new HashSet<>();
        int num = 0;
        for (VisualElement ve : cc.getCircuit().getElements()) {
            if (ve.equalsDescription(In.DESCRIPTION) || ve.equalsDescription(Out.DESCRIPTION)) {
                String l = ve.getElementAttributes().getLabel();
                if (!l.isEmpty()) {
                    if (set.contains(l)) {
                        t.setTextByID("tutorialUniqueIdents");
                        return false;
                    }
                    set.add(l);
                    num++;
                }
            }
        }
        return num >= expected;
    }

    private static boolean isWorking(CircuitComponent cc) {
        if (cc.getCircuit().getElements().size() < 4)
            return false;
        try {
            new ModelCreator(cc.getCircuit(), cc.getLibrary()).createModel(false);
            return true;
        } catch (PinException | NodeException | ElementNotFoundException e) {
            return false;
        }
    }

    private static boolean contains(Modification<Circuit> mod, Class<? extends Modification> modifyClass) {
        if (mod == null)
            return false;
        if (mod.getClass() == modifyClass)
            return true;
        if (mod instanceof Modifications) {
            Modifications m = (Modifications) mod;
            for (Object i : m.getModifications())
                if (i.getClass() == modifyClass)
                    return true;
        }
        return false;
    }

    private static boolean contains(CircuitComponent cc, ElementTypeDescription... descriptions) {
        ArrayList<VisualElement> el = new ArrayList<>(cc.getCircuit().getElements());
        if (el.size() < descriptions.length)
            return false;
        for (ElementTypeDescription d : descriptions) {
            Iterator<VisualElement> it = el.iterator();
            while (it.hasNext()) {
                if (it.next().equalsDescription(d)) {
                    it.remove();
                    break;
                }
            }
        }
        return el.isEmpty();
    }


    private final JTextPane text;
    private final CircuitComponent circuitComponent;
    private int stepIndex;

    /**
     * Creates the tutorial dialog.
     *
     * @param main the main class
     */
    public InitialTutorial(Main main) {
        super(main, Lang.get("tutorial"), false);
        this.main = main;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        circuitComponent = main.getCircuitComponent();
        circuitComponent.setTutorialListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                circuitComponent.setTutorialListener(null);
            }
        });

        text = new JTextPane();
        text.setEditable(false);
        text.setFont(Screen.getInstance().getFont(1.2f));
        text.setPreferredSize(new Dimension(300, 400));

        getContentPane().add(new JScrollPane(text));
        getContentPane().add(new JButton(new AbstractAction(Lang.get("tutorialNotNeeded")) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                disableTutorial();
            }
        }), BorderLayout.SOUTH);

        pack();

        final Point ml = main.getLocation();
        setLocation(ml.x - getWidth(), ml.y);

        stepIndex = -1;
        incIndex();

    }

    private void disableTutorial() {
        Settings.getInstance().getAttributes().set(Keys.SETTINGS_SHOW_TUTORIAL, false);
        dispose();
    }

    private void incIndex() {
        do {
            stepIndex++;
        } while (stepIndex < STEPS.size()
                && STEPS.get(stepIndex).getChecker().accomplished(circuitComponent, null, this));
        if (stepIndex == STEPS.size()) {
            disableTutorial();
        } else {
            setTextByID(STEPS.get(stepIndex).getId());
        }
    }

    private void setTextByID(String id) {
        final String s = Lang.get(id);
        text.setText(new LineBreaker(1000).breakLines(s));
    }

    @Override
    public void modified(Modification<Circuit> modification) {
        if (STEPS.get(stepIndex).getChecker().accomplished(circuitComponent, modification, this))
            incIndex();
    }

    private static final class Step {
        private final String id;
        private final Checker checker;

        private Step(String id, Checker checker) {
            this.id = id;
            this.checker = checker;
        }

        public String getId() {
            return id;
        }

        public Checker getChecker() {
            return checker;
        }
    }

    private interface Checker {
        boolean accomplished(CircuitComponent circuitComponent, Modification<Circuit> modification, InitialTutorial t);
    }
}
