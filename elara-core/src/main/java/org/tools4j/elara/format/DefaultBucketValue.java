/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.format.HistogramFormatter.BucketValue;
import org.tools4j.elara.format.HistogramFormatter.HistogramValues;

import static java.util.Objects.requireNonNull;

final class DefaultBucketValue implements BucketValue {
    private final HistogramValues histogram;
    private final String name;
    private final int index;
    private final double percentile;
    private final long value;

    DefaultBucketValue(final HistogramValues histogram, final String name, final int index, final double percentile, final long value) {
        this.histogram = requireNonNull(histogram);
        this.name = requireNonNull(name);
        this.index = index;
        this.percentile = percentile;
        this.value = value;
    }

    @Override
    public HistogramValues histogram() {
        return histogram;
    }

    @Override
    public String bucketName() {
        return name;
    }

    @Override
    public int bucketIndex() {
        return index;
    }

    @Override
    public double percentile() {
        return percentile;
    }

    @Override
    public long bucketValue() {
        return value;
    }

    @Override
    public String toString() {
        return "BucketValue{metric=" + histogram.metric().displayName() + ", name=" + name + ", index=" + index + ", value=" + value + "}";
    }
}
