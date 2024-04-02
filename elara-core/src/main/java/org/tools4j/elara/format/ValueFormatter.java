/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface ValueFormatter<M> {
    /** Placeholder in format string for line separator */
    String LINE_SEPARATOR = "{nl}";
    /** Placeholder in format string for store line no */
    String LINE = "{line}";
    /** Placeholder in format string for store entry ID */
    String ENTRY_ID = "{entry-id}";
    /** Placeholder in format string for message itself */
    String MESSAGE = "{message}";

    ValueFormatter<Object> DEFAULT = defaultThen(null);

    Object value(String placeholder, long line, long entryId, M message);

    static <M> ValueFormatter<M> defaultThen(final ValueFormatter<? super M> next) {
        return (placeholder, line, entryId, message) -> {
            switch (placeholder) {
                case LINE_SEPARATOR: return System.lineSeparator();
                case LINE: return line;
                case ENTRY_ID: return entryId;
                case MESSAGE: return message;
            }
            return next == null ? placeholder : next.value(placeholder, line, entryId, message);
        };
    }

    default ValueFormatter<M> then(final ValueFormatter<? super M> next) {
        requireNonNull(next);
        return (placeholder, line, entryId, message) -> {
            final Object replaced = ValueFormatter.this.value(placeholder, line, entryId, message);
            if (replaced == placeholder) {
                return next.value(placeholder, line, entryId, message);
            }
            return replaced;
        };
    }
}
