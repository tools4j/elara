/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tools4j.elara.flyweight;

public enum Flags {
    ;
    public static final byte NONE = 0;
    public static final byte COMMIT = 1;
    public static final byte ROLLBACK = 2;

    public static final String NONE_STRING = "-";
    public static final String COMMIT_STRING = "C";
    public static final String ROLLBACK_STRING = "R";

    private static final int FINAL_MASK = COMMIT | ROLLBACK;

    public static boolean isCommit(final byte flags) {
        return (flags & COMMIT) != 0;
    }

    public static boolean isRollback(final byte flags) {
        return (flags & ROLLBACK) != 0;
    }

    public static boolean isFinal(final byte flags) {
        return (flags & FINAL_MASK) != 0;
    }

    public static boolean isNonFinal(final byte flags) {
        return (flags & FINAL_MASK) == 0;
    }

    public static String toString(final byte flags) {
        switch (flags) {
            case NONE:
                return NONE_STRING;
            case COMMIT:
                return COMMIT_STRING;
            case ROLLBACK:
                return ROLLBACK_STRING;
            default:
                return Integer.toHexString(flags & 0xff);
        }
    }
}
