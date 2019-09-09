/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core.element;

/**
 * A value source abstraction
 */
public class ValueSource {

    private final ElementAttributes attr;
    private final Key<Integer> key;
    private final int value;

    ValueSource(ElementAttributes attr, Key<Integer> key) {
        this.attr = attr;
        this.key = key;
        this.value = attr.get(key);
    }

    /**
     * @return the attributs the value comes from
     */
    public ElementAttributes getAttr() {
        return attr;
    }

    /**
     * @return the key which belongs to the value
     */
    public Key<Integer> getKey() {
        return key;
    }

    /**
     * @return the value itself
     */
    public int get() {
        return value;
    }
}
