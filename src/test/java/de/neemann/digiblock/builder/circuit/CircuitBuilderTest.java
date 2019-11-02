/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.builder.circuit;

import de.neemann.digiblock.TestExecuter;
import de.neemann.digiblock.analyse.ModelAnalyser;
import de.neemann.digiblock.analyse.TruthTable;
import de.neemann.digiblock.analyse.expression.Constant;
import de.neemann.digiblock.analyse.expression.Expression;
import de.neemann.digiblock.analyse.expression.Variable;
import de.neemann.digiblock.core.element.Keys;
import de.neemann.digiblock.core.io.Const;
import de.neemann.digiblock.core.io.In;
import de.neemann.digiblock.core.io.Out;
import de.neemann.digiblock.core.memory.LookUpTable;
import de.neemann.digiblock.draw.elements.Circuit;
import de.neemann.digiblock.draw.elements.Tunnel;
import de.neemann.digiblock.draw.elements.VisualElement;
import de.neemann.digiblock.draw.library.ElementLibrary;
import de.neemann.digiblock.draw.model.ModelCreator;
import de.neemann.digiblock.draw.shapes.ShapeFactory;
import de.neemann.digiblock.gui.components.table.BuilderExpressionCreator;
import de.neemann.digiblock.gui.components.table.ExpressionCreator;
import de.neemann.digiblock.gui.components.table.ExpressionListenerStore;
import de.neemann.digiblock.integration.ToBreakRunner;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

import static de.neemann.digiblock.analyse.expression.Not.not;
import static de.neemann.digiblock.analyse.expression.Operation.and;
import static de.neemann.digiblock.analyse.expression.Operation.or;

/**
 *
 */
public class CircuitBuilderTest extends TestCase {

    public void testBuilderCombinatorial() throws Exception {

        Variable a = new Variable("a");
        Variable b = new Variable("b");

        // xor
        Expression y = and(or(a, b), not(and(a, b)));

        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .addCombinatorial("y", y)
                .createCircuit();

        ModelCreator m = new ModelCreator(circuit, library);

        TestExecuter te = new TestExecuter(m.createModel(false)).setUp(m);
        te.check(0, 0, 0);
        te.check(0, 1, 1);
        te.check(1, 0, 1);
        te.check(1, 1, 0);
    }

    public void testBuilderSequential() throws Exception {
        Variable y0 = new Variable("Y_0");
        Variable y1 = new Variable("Y_1");

        // counter
        Expression y0s = not(y0);
        Expression y1s = or(and(not(y0), y1), and(y0, not(y1)));

        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .addSequential("Y_0", y0s)
                .addSequential("Y_1", y1s)
                .createCircuit();

        ModelCreator m = new ModelCreator(circuit, library);
        TestExecuter te = new TestExecuter(m.createModel(false)).setUp(m);
        te.check(0, 0);
        te.checkC(1, 0);
        te.checkC(0, 1);
        te.checkC(1, 1);
        te.checkC(0, 0);
    }

    public void testBuilderSequentialLUT() throws Exception {
        Variable y0 = new Variable("Y_0");
        Variable y1 = new Variable("Y_1");
        Variable y2 = new Variable("Y_2");

        // counter
        Expression y0s = not(y0);
        Expression y1s = or(and(not(y0), y1), and(y0, not(y1)));
        Expression y2s = or(and(y0, y1, not(y2)), and(not(y0), y2), and(not(y1), y2));

        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .setUseLUTs(true)
                .addSequential("Y_0", y0s)
                .addSequential("Y_1", y1s)
                .addSequential("Y_2", y2s)
                .createCircuit();

        final ArrayList<VisualElement> el = circuit.getElements();
        assertEquals(19, el.size());
        assertEquals(1, el.stream().filter(visualElement -> visualElement.equalsDescription(LookUpTable.DESCRIPTION)).count());

        ModelCreator m = new ModelCreator(circuit, library);
        TestExecuter te = new TestExecuter(m.createModel(false)).setUp(m);
        te.check(0, 0, 0);
        te.checkC(1, 0, 0);
        te.checkC(0, 1, 0);
        te.checkC(1, 1, 0);
        te.checkC(0, 0, 1);
        te.checkC(1, 0, 1);
        te.checkC(0, 1, 1);
        te.checkC(1, 1, 1);
        te.checkC(0, 0, 0);
    }

    public void testBuilderSequentialJK_JequalsK() throws Exception {
        Variable y0 = new Variable("Y_0");
        Variable y1 = new Variable("Y_1");

        // counter
        Expression y0s = not(y0);
        Expression y1s = or(and(not(y0), y1), and(y0, not(y1)));

        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .setUseJK(true)
                .addSequential("Y_0", y0s)
                .addSequential("Y_1", y1s)
                .createCircuit();

        ModelCreator m = new ModelCreator(circuit, library);
        TestExecuter te = new TestExecuter(m.createModel(false)).setUp(m);
        te.check(0, 0);
        te.checkC(1, 0);
        te.checkC(0, 1);
        te.checkC(1, 1);
        te.checkC(0, 0);
    }

    public void testBuilderSequentialJK() throws Exception {
        Variable y0 = new Variable("Y_0");
        Variable y1 = new Variable("Y_1");

        // counter
        Expression y0s = not(y0);
        Expression y1s = or(not(y0), not(y1));

        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .setUseJK(true)
                .addSequential("Y_0", y0s)
                .addSequential("Y_1", y1s)
                .createCircuit();

        ModelCreator m = new ModelCreator(circuit, library);
        TestExecuter te = new TestExecuter(m.createModel(false)).setUp(m);
        te.check(0, 0);
        te.checkC(1, 1);
        te.checkC(0, 0);
        te.checkC(1, 1);
        te.checkC(0, 0);
    }

    public void testBuilderSequentialConstant() throws Exception {
        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .addSequential("Y_0", Constant.ONE)
                .addSequential("Y_1", Constant.ZERO)
                .createCircuit();

        final ArrayList<VisualElement> el = circuit.getElements();
        assertEquals(8, el.size());
        assertEquals(4, el.stream().filter(visualElement -> visualElement.equalsDescription(Tunnel.DESCRIPTION)).count());
        assertEquals(2, el.stream().filter(visualElement -> visualElement.equalsDescription(Const.DESCRIPTION)).count());
        assertEquals(2, el.stream().filter(visualElement -> visualElement.equalsDescription(Out.DESCRIPTION)).count());
    }

    public void testBuilderSequentialConstantJK() throws Exception {
        ElementLibrary library = new ElementLibrary();
        Circuit circuit = new CircuitBuilder(new ShapeFactory(library))
                .setUseJK(true)
                .addSequential("Y_0", Constant.ONE)
                .addSequential("Y_1", Constant.ZERO)
                .createCircuit();

        final ArrayList<VisualElement> el = circuit.getElements();
        assertEquals(8, el.size());
        assertEquals(4, el.stream().filter(visualElement -> visualElement.equalsDescription(Tunnel.DESCRIPTION)).count());
        assertEquals(2, el.stream().filter(visualElement -> visualElement.equalsDescription(Const.DESCRIPTION)).count());
        assertEquals(2, el.stream().filter(visualElement -> visualElement.equalsDescription(Out.DESCRIPTION)).count());
    }

    public void testBus() throws Exception {
        final ToBreakRunner runner = new ToBreakRunner("dig/circuitBuilder/busTest.dig", false);
        // create truth table incl. ModelAnalyzerInfo
        TruthTable tt = new ModelAnalyser(runner.getModel()).analyse();

        assertEquals(8, tt.getVars().size());
        assertEquals(8, tt.getResultCount());

        // create expressions based on truth table
        ExpressionListenerStore expr = new ExpressionListenerStore(null);
        new ExpressionCreator(tt).create(expr);

        // build a new circuit
        CircuitBuilder circuitBuilder = new CircuitBuilder(runner.getLibrary().getShapeFactory(), tt.getVars())
                .setModelAnalyzerInfo(tt.getModelAnalyzerInfo());
        new BuilderExpressionCreator(circuitBuilder).create(expr);
        Circuit circuit = circuitBuilder.createCircuit();

        // check
        List<VisualElement> in = circuit.getElements(v -> v.equalsDescription(In.DESCRIPTION));
        assertEquals(2, in.size());
        checkPin(in.get(0), "A", "1,2,3,4");
        checkPin(in.get(1), "B", "5,6,7,8");

        List<VisualElement> out = circuit.getElements(v -> v.equalsDescription(Out.DESCRIPTION));
        assertEquals(2, out.size());
        checkPin(out.get(0), "S", "9,10,11,12");
        checkPin(out.get(1), "U", "13,14,15,16");
    }

    private void checkPin(VisualElement e, String label, String pins) {
        assertEquals(label, e.getElementAttributes().getLabel());
        assertEquals(4, e.getElementAttributes().getBits());
        assertEquals(pins, e.getElementAttributes().get(Keys.PINNUMBER));
    }

    public void testIsXor() {
        assertFalse(CircuitBuilder.isXor(new long[]{0, 0, 0, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 0, 1, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 0, 1, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 0, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 0, 1}));
        assertTrue(CircuitBuilder.isXor(new long[]{0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 0, 0, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 0, 1, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 0, 1, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 1, 0, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 1, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 1, 1, 1}));

        assertTrue(CircuitBuilder.isXor(new long[]{0, 1, 1, 0, 1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{1, 1, 1, 0, 1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 0, 1, 0, 1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 0, 0, 1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 1, 1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 0, 0, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 0, 1, 1, 0, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 0, 1, 0, 1, 1}));
        assertFalse(CircuitBuilder.isXor(new long[]{0, 1, 1, 0, 1, 0, 0, 0}));
    }

    public void testIsXNor() {
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 0, 0, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 0, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 0, 1, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 1, 0, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 1, 0, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 1, 1, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 0}));
        assertTrue(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 1, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 1, 0, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 1, 0, 1}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 1, 1, 1}));

        assertTrue(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1, 0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{0, 0, 0, 1, 0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 1, 0, 1, 0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 1, 1, 0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 0, 0, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1, 1, 1, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1, 0, 0, 1, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1, 0, 1, 0, 0}));
        assertFalse(CircuitBuilder.isXNor(new long[]{1, 0, 0, 1, 0, 1, 1, 1}));

    }
}
