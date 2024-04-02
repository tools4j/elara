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

import org.tools4j.elara.format.HistogramFormatter.BucketDescriptor;

public enum DefaultHistogramBucket implements BucketDescriptor {
    min(0.0),
    p50(0.5),
    p90(0.9),
    p99(0.99),
    p99_9(0.999),
    p99_99(0.9999),
    p99_999(0.99999),
    max(1.0);
    private final String displayName;
    private final double percentile;

    DefaultHistogramBucket(final double percentile) {
        this.displayName = name().replace('_', '.');
        this.percentile = percentile;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public double percentile() {
        return percentile;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
