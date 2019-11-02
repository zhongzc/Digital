/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.testing;

import de.neemann.digiblock.core.Model;
import de.neemann.digiblock.core.NodeException;
import de.neemann.digiblock.core.ObservableValue;
import de.neemann.digiblock.core.Signal;
import de.neemann.digiblock.core.wiring.Clock;
import de.neemann.digiblock.data.Value;
import de.neemann.digiblock.data.ValueTable;
import de.neemann.digiblock.lang.Lang;
import de.neemann.digiblock.testing.parser.Context;
import de.neemann.digiblock.testing.parser.LineEmitter;
import de.neemann.digiblock.testing.parser.ParserException;
import de.neemann.digiblock.testing.parser.TestRow;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Runs the test and stores the test results created by a single {@link TestCaseDescription} instance.
 */
public class TestExecutor {
    private static final int MAX_RESULTS = 1 << 10;
    private static final int ERR_RESULTS = MAX_RESULTS * 2;

    private final ArrayList<String> names;
    private final LineEmitter lines;
    private final ValueTable results;
    private boolean allPassed;
    private boolean toManyResults = false;
    private ArrayList<TestSignal> inputs;
    private ArrayList<TestSignal> outputs;
    private int visibleRows;

    /**
     * Creates a new testing result
     *
     * @param testCaseDescription the testing data
     * @throws TestingDataException DataException
     */
    public TestExecutor(TestCaseDescription testCaseDescription) throws TestingDataException {
        names = testCaseDescription.getNames();
        results = new ValueTable(names);
        visibleRows = 0;
        lines = testCaseDescription.getLines();
    }

    /**
     * Creates the result by comparing the testing vector with the given model-
     *
     * @param model the model to check
     * @return this for chained calls
     * @throws TestingDataException DataException
     * @throws NodeException        NodeException
     * @throws ParserException      ParserException
     */
    public TestExecutor create(Model model) throws TestingDataException, NodeException, ParserException {
        allPassed = true;
        HashSet<String> usedSignals = new HashSet<>();

        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        for (Signal s : model.getInputs()) {
            final int index = getIndexOf(s.getName());
            if (index >= 0) {
                inputs.add(new TestSignal(index, s.getValue()));
                addTo(usedSignals, s.getName());
            }
            ObservableValue outValue = s.getBidirectionalReader();
            if (outValue != null) {
                final String outName = s.getName() + "_out";
                final int inIndex = getIndexOf(outName);
                if (inIndex >= 0) {
                    outputs.add(new TestSignal(inIndex, outValue));
                    addTo(usedSignals, outName);
                }
            }
        }

        for (Clock c : model.getClocks()) {
            final int index = getIndexOf(c.getLabel());
            if (index >= 0) {
                inputs.add(new TestSignal(index, c.getClockOutput()));
                addTo(usedSignals, c.getLabel());
            }
        }

        for (Signal s : model.getOutputs()) {
            final int index = getIndexOf(s.getName());
            if (index >= 0) {
                outputs.add(new TestSignal(index, s.getValue()));
                addTo(usedSignals, s.getName());
            }
        }

        for (String name : names)
            if (!usedSignals.contains(name))
                throw new TestingDataException(Lang.get("err_testSignal_N_notFound", name));

        if (inputs.size() == 0)
            throw new TestingDataException(Lang.get("err_noTestInputSignalsDefined"));

        if (outputs.size() == 0)
            throw new TestingDataException(Lang.get("err_noTestOutputSignalsDefined"));

        model.init();

        lines.emitLines(new LineListenerResolveDontCare(values -> checkRow(model, values), inputs), new Context());

        return this;
    }

    private void addTo(HashSet<String> signals, String name) throws TestingDataException {
        if (signals.contains(name))
            throw new TestingDataException(Lang.get("err_nameUsedTwice_N", name));
        signals.add(name);
    }

    private void checkRow(Model model, TestRow testRow) {
        Value[] values = testRow.getValues();
        Value[] res = new Value[values.length];

        boolean clockIsUsed = false;
        // set all values except the clocks
        for (TestSignal in : inputs) {
            if (values[in.index].getType() != Value.Type.CLOCK) {
                values[in.index].copyTo(in.value);
            } else {
                clockIsUsed = true;
            }
            res[in.index] = values[in.index];
        }

        try {
            if (clockIsUsed) {  // a clock signal is used
                model.doStep();  // propagate all except clock
                addClockRow(values.length, testRow.getDescription());

                // set clock
                for (TestSignal in : inputs)
                    if (values[in.index].getType() == Value.Type.CLOCK) {
                        values[in.index].copyTo(in.value);
                        res[in.index] = values[in.index];
                    }

                // propagate clock change
                model.doStep();
                addClockRow(values.length, testRow.getDescription());

                // restore clock
                for (TestSignal in : inputs)   // invert the clock values
                    if (values[in.index].getType() == Value.Type.CLOCK) {
                        in.value.setBool(!in.value.getBool());
                        res[in.index] = new Value(in.value);
                    }
            }

            model.doStep();
        } catch (NodeException e) {
            allPassed = false;
            throw new RuntimeException("", e);
        } catch (RuntimeException e) {
            allPassed = false;
            throw e;
        }

        boolean ok = true;
        for (TestSignal out : outputs) {
            MatchedValue matchedValue = new MatchedValue(values[out.index], out.value);
            res[out.index] = matchedValue;
            if (!matchedValue.isPassed()) {
                allPassed = false;
                ok = false;
            }
        }

        if (visibleRows < (ok ? MAX_RESULTS : ERR_RESULTS)) {
            visibleRows++;
            results.add(new TestRow(res, testRow.getDescription()));
        } else
            toManyResults = true;
    }

    private void addClockRow(int cols, String description) {
        if (visibleRows < ERR_RESULTS) {
            Value[] r = new Value[cols];
            for (TestSignal out : outputs)
                r[out.index] = new Value(out.value);
            for (TestSignal in : inputs)
                r[in.index] = new Value(in.value);
            results.add(new TestRow(r, description)).omitInTable();
        } else
            toManyResults = true;
    }

    /**
     * @return true if all tests have passed
     */
    public boolean allPassed() {
        return allPassed;
    }


    /**
     * Indicates if there are to many entries in the table to show.
     * If there are to many entries, the test results is still correct.
     *
     * @return true if there are missing items in the results list.
     */
    public boolean toManyResults() {
        return toManyResults;
    }

    private int getIndexOf(String name) {
        if (name == null || name.length() == 0)
            return -1;

        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (n.equals(name))
                return i;
        }
        return -1;
    }

    /**
     * @return return the result
     */
    public ValueTable getResult() {
        return results;
    }

    /**
     * A test signal
     */
    public static class TestSignal {
        private final int index;
        private final ObservableValue value;

        TestSignal(int index, ObservableValue value) {
            this.index = index;
            this.value = value;
        }

        /**
         * @return the index of this value
         */
        public int getIndex() {
            return index;
        }
    }

}
