/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.gui.components.data;

import de.neemann.digiblock.core.SyncAccess;
import de.neemann.digiblock.data.DataPlotter;
import de.neemann.digiblock.data.ValueTable;
import de.neemann.digiblock.draw.graphics.GraphicSwing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The component to show the trace window.
 * It shows the data in the given dataSet.
 */
public class GraphComponent extends JComponent {
    private final DataPlotter plotter;

    /**
     * Creates a new dataSet
     *
     * @param dataSet   the dataSet to paint
     * @param modelSync lock to access the model
     */
    GraphComponent(ValueTable dataSet, SyncAccess modelSync) {
        plotter = new DataPlotter(dataSet).setModelSync(modelSync);
        addMouseWheelListener(e -> {
            double f = Math.pow(0.9, e.getWheelRotation());
            scale(f, e.getX());
        });

        addMouseMotionListener(new MouseAdapter() {
            private int lastPos;

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {
                lastPos = mouseEvent.getX();
            }

            @Override
            public void mouseDragged(MouseEvent mouseEvent) {
                int pos = mouseEvent.getX();
                plotter.move(pos - lastPos);
                lastPos = pos;
                repaint();
            }

        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                plotter.setWidth(getWidth());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        plotter.drawTo(new GraphicSwing(g2), null);
    }

    @Override
    public Dimension getPreferredSize() {
        int w = plotter.getCurrentGraphicWidth();
        if (w < 600) w = 600;
        return new Dimension(w, plotter.getGraphicHeight());
    }

    /**
     * Apply a scaling factor
     *
     * @param f    the factor
     * @param xPos fixed position
     */
    public void scale(double f, int xPos) {
        plotter.scale(f, xPos);
        repaint();
    }

    /**
     * Fits the data to the visible area
     */
    void fitData() {
        plotter.fitInside();
        repaint();
    }

    /**
     * @return the data plotter
     */
    DataPlotter getPlotter() {
        return plotter;
    }

    /**
     * Sets the scroll bar to use
     *
     * @param scrollBar the scroll bar
     */
    void setScrollBar(JScrollBar scrollBar) {
        plotter.setScrollBar(scrollBar);
        scrollBar.addAdjustmentListener(adjustmentEvent -> {
            if (plotter.setNewOffset(adjustmentEvent.getValue()))
                repaint();
        });
    }
}
