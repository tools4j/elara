/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

enum Spacer {
    ;
    /**
     * Replaces values for the specified placeholders with the given space char for the length of the original value's
     * string representation.
     *
     * @param formatter             original formatter to get the value
     * @param spaceChar             the space character
     * @param placeholderToSpaceOut the place holders for which to replace value with space chars
     * @return formatter that spaces out some of the values
     */
    static <M> ValueFormatter<M> spacer(final ValueFormatter<M> formatter, final char spaceChar, final String... placeholderToSpaceOut) {
        requireNonNull(formatter);
        final Set<String> spaceOut = new HashSet<>(Arrays.asList(placeholderToSpaceOut));
        return (placeholder, line, entryId, message) -> {
            final Object value = formatter.value(placeholder, line, entryId, message);
            if (spaceOut.contains(placeholder)) {
                final char[] chars = String.valueOf(value).toCharArray();
                Arrays.fill(chars, spaceChar);
                return String.valueOf(chars);
            }
            return value;
        };
    }
}
