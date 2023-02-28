/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.codec;

import static org.tools4j.elara.codec.Chars.leq;

/**
 * Numeric only codec simply using {@link Integer#toString(int)} as string representation.
 */
public class NumericCodec implements ShortStringCodec {

    public static final String MAX_NUMERIC_STRING = String.valueOf(Integer.MAX_VALUE);
    public static final String MIN_NUMERIC_STRING = String.valueOf(Integer.MIN_VALUE);
    public static final int MAX_LENGTH_UNSIGNED = MAX_NUMERIC_STRING.length();
    public static final int MAX_LENGTH_SIGNED = MIN_NUMERIC_STRING.length();

    @Override
    public int toInt(final CharSequence source) {
        return sourceToInt(source);
    }

    /**
     * Same as {@link Integer#parseInt(String, int)} with radix = 10.
     *
     * @param source the source to parse
     * @return the integer representation of source
     * @throws IllegalArgumentException if source is not numeric or too large to fit in an integer
     */
    public static int sourceToInt(final CharSequence source) {
        //see Integer.toString(int, int) with radix=10
        int result = 0;
        boolean negative = false;
        int i = 0, len = source.length();
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = source.charAt(0);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+')
                    throw new IllegalArgumentException("Illegal first character '" + firstChar + "' in source: " + source);

                if (len == 1) // Cannot have lone "+" or "-"
                    throw new IllegalArgumentException("Invalid source string: " + source);
                i++;
            }
            multmin = limit / 10;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(source.charAt(i++), 10);
                if (digit < 0) {
                    throw new IllegalArgumentException("Illegal character '" + source.charAt(i) + "' in source: " + source);
                }
                if (result < multmin) {
                    throw new IllegalArgumentException("Invalid source string (overflow): " + source);
                }
                result *= 10;
                if (result < limit + digit) {
                    throw new IllegalArgumentException("Invalid source string (overflow): " + source);
                }
                result -= digit;
            }
        } else {
            throw new IllegalArgumentException("Empty source string");
        }
        return negative ? result : -result;
    }

    @Override
    public StringBuilder toString(final int source, final StringBuilder dst) {
        return sourceToString(source, dst);
    }

    public static StringBuilder sourceToString(final int source, final StringBuilder dst) {
        return dst.append(source);
    }

    @Override
    public boolean isValid(final CharSequence seq) {
        return isSourceValid(seq);
    }

    public static boolean isSourceValid(final CharSequence seq) {
        final int len = seq.length();
        if (len < 1 || len > MAX_LENGTH_SIGNED) {
            return false;
        }
        final AlphaPrefixEncoding encoding = AlphaPrefixEncoding.encodingFor(seq);
        switch (encoding) {
            case NUMERIC_UNSIGNED:
                return len < MAX_LENGTH_UNSIGNED ||
                        (len == MAX_LENGTH_UNSIGNED && leq(seq, MAX_NUMERIC_STRING));
            case NUMERIC_SIGNED:
                return len < MAX_LENGTH_SIGNED ||
                        (/*len == MAX_LENGTH_SIGNED && */ leq(seq, MIN_NUMERIC_STRING));
            default:
                return false;
        }
    }

    @Override
    public boolean isNegative(final CharSequence source) {
        return isSourceNegative(source);
    }

    public static boolean isSourceNegative(final CharSequence source) {
        if (source.length() > 0) {
            final char first = source.charAt(0);
            return first == '.' | first == '-';
        }
        return false;
    }

    @Override
    public String toString() {
        return NumericCodec.class.getSimpleName();
    }
}
