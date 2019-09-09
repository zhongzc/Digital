/*
 * Copyright (c) 2019 Helmut Neemann.
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.core;

import de.neemann.digital.core.element.ValueSource;

import java.util.List;

/**
 * Interface which is implemented by exceptions which can provide a fix
 */
public interface FixableException {

    /**
     * @return the fixes available. Maybe null
     */
    List<Fix> getFixes();

    /**
     * A fix which can solve the problem
     */
    final class Fix {
        private final ValueSource bitSource;
        private final int bitsToFixTheProblem;

        public Fix(ValueSource bitSource, int bitsToFixTheProblem) {
            this.bitSource = bitSource;
            this.bitsToFixTheProblem = bitsToFixTheProblem;
        }

        /**
         * @return the original bits source
         */
        public ValueSource getBitSource() {
            return bitSource;
        }

        /**
         * @return the bit value which would fix the problem
         */
        public int getBitsToFixTheProblem() {
            return bitsToFixTheProblem;
        }
    }
}
