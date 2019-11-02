/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digiblock.draw.library;

/**
 * User has to implement a component source
 */
public interface ComponentSource {

    /**
     * User has to implement this interface in order to add components to Digiblock
     *
     * @param adder the ComponentManager
     * @throws InvalidNodeException thrown if node is invalid
     */
    void registerComponents(ComponentManager adder) throws InvalidNodeException;
}
