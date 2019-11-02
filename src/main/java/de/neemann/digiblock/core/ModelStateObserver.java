/*
 * Copyright (c) 2016 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.core;

/**
 * Interface to implement observers of the model.
 */
public interface ModelStateObserver {

    /**
     * called if a event was detected.
     *
     * @param event the event
     */
    void handleEvent(ModelEvent event);

}
