/*
 * Copyright (c) 2018 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.fsm;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import de.neemann.digiblock.XStreamValid;
import de.neemann.digiblock.analyse.TruthTable;
import de.neemann.digiblock.analyse.expression.ExpressionException;
import de.neemann.digiblock.draw.graphics.Graphic;
import de.neemann.digiblock.draw.graphics.Vector;
import de.neemann.digiblock.draw.graphics.VectorFloat;
import de.neemann.digiblock.lang.Lang;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple finite state machine
 */
public class FSM {

    /**
     * The moving state of the fsm.
     */
    public enum MovingState {
        /**
         * no elements are moving
         */
        STOP,
        /**
         * only transitions are moving
         */
        TRANSITIONS,
        /**
         * transitions and states are moving
         */
        BOTH
    }

    private ArrayList<State> states;
    private ArrayList<Transition> transitions;
    private transient boolean modified;
    private transient ModifiedListener modifiedListener;
    private transient int activeState = -1;
    private transient File file;
    private transient MovingState state = MovingState.STOP;

    /**
     * Creates a proper configured XStream instance
     *
     * @return the XStream instance
     */
    public static XStream getxStream() {
        XStream xStream = new XStreamValid();
        xStream.alias("fsm", FSM.class);
        xStream.alias("state", State.class);
        xStream.alias("transition", Transition.class);
        xStream.alias("vector", Vector.class);
        xStream.aliasAttribute(Vector.class, "x", "x");
        xStream.aliasAttribute(Vector.class, "y", "y");
        xStream.alias("vectorf", VectorFloat.class);
        xStream.aliasAttribute(VectorFloat.class, "x", "x");
        xStream.aliasAttribute(VectorFloat.class, "y", "y");
        return xStream;
    }

    /**
     * Creates a new circuit instance from a stored file
     *
     * @param filename filename
     * @return the fsm
     * @throws IOException IOException
     */
    public static FSM loadFSM(File filename) throws IOException {
        final FSM fsm = loadFSM(new FileInputStream(filename));
        fsm.file = filename;
        return fsm;
    }

    /**
     * Creates a new fsm instance from a stored file
     *
     * @param in the input stream
     * @return the fsm
     * @throws IOException IOException
     */
    public static FSM loadFSM(InputStream in) throws IOException {
        try {
            XStream xStream = getxStream();
            final FSM fsm = (FSM) xStream.fromXML(in);
            for (Transition t : fsm.transitions)
                t.setFSM(fsm);
            for (State s : fsm.states)
                s.setFSM(fsm);
            fsm.modified = false;
            fsm.activeState = -1;
            return fsm;
        } catch (RuntimeException e) {
            throw new IOException(Lang.get("err_invalidFileFormat"), e);
        } finally {
            in.close();
        }
    }

    /**
     * Stores the fsm in the given file
     *
     * @param filename filename
     * @throws IOException IOException
     */
    public void save(File filename) throws IOException {
        save(new FileOutputStream(filename));
        file = filename;
    }

    /**
     * Stores the circuit in the given file
     *
     * @param out the writer
     * @throws IOException IOException
     */
    public void save(OutputStream out) throws IOException {
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            XStream xStream = getxStream();
            w.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            xStream.marshal(this, new PrettyPrintWriter(w));
            modified = false;
            if (modifiedListener != null)
                modifiedListener.modifiedChanged(modified);
        }
    }

    /**
     * Creates a new FSM containing the given states
     *
     * @param state the states
     */
    public FSM(State... state) {
        states = new ArrayList<>();
        transitions = new ArrayList<>();
        for (State s : state)
            add(s);
    }

    /**
     * Sets the moving state of this FSM
     *
     * @param state the state
     */
    public void setMovingState(MovingState state) {
        if (this.state != state) {
            this.state = state;
            if (state != MovingState.BOTH)
                for (State s : states)
                    s.toRaster();
        }
    }

    /**
     * @return the moving state of this FSM
     */
    public MovingState getMovingState() {
        if (state == null)
            state = MovingState.STOP;
        return state;
    }


    /**
     * Adds a state to the FSM
     *
     * @param state the state to add
     * @return this for chained calls
     */
    public FSM add(State state) {
        if (state.getNumber() < 0)
            state.setNumber(states.size());
        if (states.isEmpty())
            state.setInitial(true);
        state.setFSM(this);
        states.add(state);
        wasModified(state, Movable.Property.ADDED);
        return this;
    }

    /**
     * Adds a transition to the FSM
     *
     * @param transition the transition to add
     * @return this for chained calls
     */
    public FSM add(Transition transition) {
        transitions.add(transition);
        transition.setFSM(this);
        wasModified(transition, Movable.Property.ADDED);
        return this;
    }

    /**
     * Adds a transition to the FSM
     *
     * @param from      the from state
     * @param to        the to state
     * @param condition the condition
     * @return this for chained calls
     * @throws FiniteStateMachineException FiniteStateMachineException
     */
    public FSM transition(String from, String to, String condition) throws FiniteStateMachineException {
        return transition(findState(from), findState(to), condition);
    }

    /**
     * Adds a transition to the FSM
     *
     * @param from      the from state
     * @param to        the to state
     * @param condition the condition
     * @return this for chained calls
     * @throws FiniteStateMachineException FiniteStateMachineException
     */
    public FSM transition(int from, int to, String condition) throws FiniteStateMachineException {
        return transition(findState(from), findState(to), condition);
    }

    /**
     * Adds a transition to the FSM
     *
     * @param from      the from state
     * @param to        the to state
     * @param condition the condition
     * @return this for chained calls
     */
    public FSM transition(State from, State to, String condition) {
        if (!states.contains(from))
            add(from);
        if (!states.contains(to))
            add(to);
        return add(new Transition(from, to, condition));
    }

    private State findState(String name) throws FiniteStateMachineException {
        for (State s : states)
            if (s.getName().equals(name))
                return s;
        throw new FiniteStateMachineException(Lang.get("err_fsmState_N_notFound", name));
    }

    private State findState(int number) throws FiniteStateMachineException {
        for (State s : states)
            if (s.getNumber() == number)
                return s;
        throw new FiniteStateMachineException(Lang.get("err_fsmState_N_notFound", number));
    }


    /**
     * @return the file, maybe null
     */
    public File getFile() {
        return file;
    }

    /**
     * Calculates all forces to move the elements
     */
    private void calculateForces() {
        for (State s : states)
            s.calcExpansionForce(states);
        for (Transition t : transitions)
            t.calcForce(states, transitions);
    }

    /**
     * @return the states
     */
    public List<State> getStates() {
        return states;
    }

    /**
     * Draws the FSM
     *
     * @param gr the Graphic instance to draw to
     */
    public void drawTo(Graphic gr) {
        for (State s : states)
            s.drawTo(gr);
        for (Transition t : transitions)
            t.drawTo(gr);
    }

    /**
     * Moved the elements
     *
     * @param dt     the time step
     * @param except element which is fixed
     */
    public void move(int dt, Movable except) {
        if (state != MovingState.STOP) {
            calculateForces();
            if (state == MovingState.BOTH)
                for (State s : states)
                    if (s != except)
                        s.move(dt);
            for (Transition t : transitions)
                if (t != except)
                    t.move(dt);
        }
    }

    /**
     * Orders all states in a big circle
     *
     * @return this for chained calls
     */
    public FSM circle() {
        double delta = 2 * Math.PI / states.size();
        double circumference = 0;
        for (State s : states)
            circumference += s.getVisualRadius() * 2;

        circumference += states.size() * State.DEFAULT_RAD * 2;
        double rad = circumference / Math.PI / 2;

        double phi = 0;
        for (State s : states) {
            s.setPosition(new VectorFloat((float) (Math.sin(phi) * rad), (float) (-Math.cos(phi) * rad)));
            phi += delta;
        }

        for (Transition t : transitions)
            t.initPos();

        return this;
    }

    /**
     * Creates the truth table which is defined by this finite state machine
     *
     * @param stateSignalName the name of the signal used to represent the state
     * @return the truth table
     * @throws ExpressionException         ExpressionException
     * @throws FiniteStateMachineException FiniteStateMachineException
     */
    public TruthTable createTruthTable(String stateSignalName) throws ExpressionException, FiniteStateMachineException {
        return new TransitionTableCreator(this, stateSignalName).create();
    }

    /**
     * @return the initial state
     * @throws FiniteStateMachineException FiniteStateMachineException
     */
    public int getInitState() throws FiniteStateMachineException {
        for (State s : states)
            if (s.isInitial())
                return s.getNumber();
        throw new FiniteStateMachineException(Lang.get("err_fsmNoInitialState"));
    }

    /**
     * Remove initial state
     */
    public void clearInitial() {
        for (State s : states)
            s.setInitial(false);
    }

    /**
     * Returns the element at the given position
     *
     * @param pos the position
     * @return the element or null
     */
    public Movable getMovable(Vector pos) {
        Movable found = null;
        float dist = Float.MAX_VALUE;
        for (Transition t : transitions)
            if (t.matches(pos)) {
                float d = pos.sub(t.getPos()).len();
                if (d < dist) {
                    dist = d;
                    found = t;
                }
            }

        if (found != null)
            return found;

        dist = Float.MAX_VALUE;
        for (State s : states)
            if (s.matches(pos)) {
                float d = pos.sub(s.getPos()).len();
                if (d < dist) {
                    dist = d;
                    found = s;
                }
            }

        return found;
    }

    /**
     * @return the transitions
     */
    public List<Transition> getTransitions() {
        return transitions;
    }

    /**
     * Removes the given transition
     *
     * @param transition the transition to remove
     */
    public void remove(Transition transition) {
        transitions.remove(transition);
        wasModified(transition, Movable.Property.REMOVED);
    }

    /**
     * Removes the given state
     *
     * @param state the state to remove
     */
    public void remove(State state) {
        states.remove(state);
        transitions.removeIf(t -> t.getStartState() == state || t.getTargetState() == state);
        wasModified(state, Movable.Property.REMOVED);
    }

    /**
     * Marks the fsm as modified
     *
     * @param movable the element changed
     * @param prop    the property which has changed
     */
    void wasModified(Movable movable, Movable.Property prop) {
        modified = true;

        if (movable instanceof State) {
            State st = (State) movable;
            if (prop == Movable.Property.POS && getMovingState() != MovingState.BOTH)
                st.toRaster();

            if ((prop == Movable.Property.POS || prop == Movable.Property.MOUSEPOS) && getMovingState() == MovingState.STOP)
                for (Transition t : transitions)
                    if (t.getTargetState() == st || t.getStartState() == st)
                        t.setPos(t.getPos());
        }

        if (modifiedListener != null)
            modifiedListener.modifiedChanged(modified);
    }

    /**
     * Sets a modified listener
     *
     * @param modifiedListener the listener called if fsm was modified
     */
    public void setModifiedListener(ModifiedListener modifiedListener) {
        this.modifiedListener = modifiedListener;
    }

    /**
     * @return true if fsm has changed
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Sets the modified flag
     *
     * @param modified the modified flag
     * @return this for chained calls
     */
    public FSM setModified(boolean modified) {
        if (modifiedListener != null)
            throw new RuntimeException("call not allowed");
        this.modified = modified;
        return this;
    }

    /**
     * Used to set the active state
     *
     * @param value the state number
     * @return true if state has changed
     */
    public boolean setActiveState(int value) {
        if (activeState != value) {
            activeState = value;
            return true;
        } else
            return false;
    }

    /**
     * @return the active state
     */
    int getActiveState() {
        return activeState;
    }

    /**
     * a modified listener
     */
    public interface ModifiedListener {
        /**
         * called if fsm was modified
         *
         * @param wasModified true is fsm is modified
         */
        void modifiedChanged(boolean wasModified);
    }
}
