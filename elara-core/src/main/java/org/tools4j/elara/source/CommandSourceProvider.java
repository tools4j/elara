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
package org.tools4j.elara.source;

/**
 * Provides access to {@link CommandSource command sources}.
 */
public interface CommandSourceProvider {
    /**
     * Returns the number of sources in use.
     * @return the number of command sources
     */
    int sources();

    /**
     * Returns the command source for the specified {@code index}, a value zero to {@link #sources()}-1
     * @param index the index, a value in {@code [0..(sources-1)]}.
     * @return the command source
     * @throws IndexOutOfBoundsException if index is not in {@code [0..(sources-1)]}
     */
    CommandSource sourceByIndex(int index);

    /**
     * Returns the command source for the specified source ID. A new source is created if no command source exists yet
     * for the specified source ID
     * @param sourceId the command source ID
     * @return the command source, newly created if necessary
     */
    CommandSource sourceById(int sourceId);
}
