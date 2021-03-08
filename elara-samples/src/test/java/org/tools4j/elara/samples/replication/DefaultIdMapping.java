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
package org.tools4j.elara.samples.replication;

import java.util.Arrays;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public class DefaultIdMapping implements IdMapping {

    private final int[] ids;

    private DefaultIdMapping(final int start, final int n) {
        this.ids = IntStream.range(start, n).toArray();
    }

    private DefaultIdMapping(final int[] ids) {
        if (Arrays.stream(ids).distinct().count() != ids.length) {
            throw new IllegalArgumentException("Duplicate IDs in " + Arrays.toString(ids));
        }
        this.ids = requireNonNull(ids);
    }

    public static DefaultIdMapping enumerate(final int n) {
        return enumerate(1, n);
    }

    public static DefaultIdMapping enumerate(final int from, int n) {
        return new DefaultIdMapping(from, from + n);
    }

    public static DefaultIdMapping of(final int... ids) {
        return new DefaultIdMapping(ids);
    }

    @Override
    public int count() {
        return ids.length;
    }

    @Override
    public int idByIndex(final int index) {
        return ids[index];
    }

    @Override
    public int indexById(final int id) {
        for (int i = 0; i < ids.length; i++) {
            if (id == ids[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return "DefaultIdMapping{" +
                "ids=" + Arrays.toString(ids) +
                '}';
    }
}
