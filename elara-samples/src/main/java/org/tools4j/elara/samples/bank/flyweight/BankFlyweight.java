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
package org.tools4j.elara.samples.bank.flyweight;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.logging.Printable;

/**
 * A flyweight object used by the banking application, usually a command or event.
 */
public interface BankFlyweight extends Printable {
    /**
     * Reset buffer, offset and length.
     * @return this flyweight.
     */
    BankFlyweight reset();

    /**
     * Wraps the given buffer to start encoding.
     * @param buffer the buffer to encode to
     * @param offset the offset in the buffer where the first byte is written to
     * @return this flyweight to do the encoding
     */
    BankFlyweight wrap(MutableDirectBuffer buffer, int offset);

    /**
     * Returns the encoding length in bytes.  For variable length types the length may change if the content changes.
     * @return the total number of bytes that this flyweight occupies in the buffer with the current values
     */
    int encodingLength();
}
