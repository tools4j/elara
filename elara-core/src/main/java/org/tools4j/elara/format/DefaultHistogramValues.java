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
/*
 Adapted version from http://www.hdrhistogram.org.

 Copyright (c) 2012, 2013, 2014, 2015, 2016 Gil Tene
 Copyright (c) 2014 Michael Barker
 Copyright (c) 2014 Matt Warren
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.tools4j.elara.format;

import org.tools4j.elara.flyweight.TimeMetricsFrame;
import org.tools4j.elara.format.HistogramFormatter.HistogramValues;
import org.tools4j.elara.plugin.metrics.Metric;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * Default histogram implementation, slimmed down version of HDR histogram with fixed parameters for value range and
 * precision.
 * <p>
 * See <a href="http://www.hdrhistogram.org">http://www.hdrhistogram.org</a>
 */
final class DefaultHistogramValues implements HistogramValues {

    private final static int SUB_BUCKET_COUNT = 262144;
    private final static int SUB_BUCKET_MASK = SUB_BUCKET_COUNT - 1;
    private final static int SUB_BUCKET_HALF_COUNT = SUB_BUCKET_COUNT / 2;
    private final static int SUB_BUCKET_HALF_COUNT_MAGNITUDE = 17;
    private final static int LEADING_ZERO_COUNT_BASE = 46;
    private final static int COUNTS_ARRAY_LENGTH = 6160384;
    /* init with smaller array, allows storing up to 1s in micros before resizing */
    private final static int INITIAL_ARRAY_LENGTH = COUNTS_ARRAY_LENGTH / 8;

    private final TimeMetricsFrame frame;
    private final Metric metric;
    private final TimeFormatter timeFormatter;

    private long resetTime;
    private long resetCount;
    private long min;
    private long max;
    private int count;
    private long[] counts = new long[INITIAL_ARRAY_LENGTH];

    DefaultHistogramValues(final TimeMetricsFrame frame, final Metric metric, final TimeFormatter timeFormatter) {
        this.frame = requireNonNull(frame);
        this.metric = requireNonNull(metric);
        this.timeFormatter = requireNonNull(timeFormatter);
        this.resetTime = frame.metricTime();
    }

    @Override
    public void record(final long value) {
        final int index = countsArrayIndex(value);
        incrementCount(index);
        min = count == 0 ? value : Math.min(min, value);
        max = Math.max(max, value);
        count++;
    }

    private void incrementCount(final int index) {
        final int len = counts.length;
        if (index > len) {
            int newLen = 2 * len;
            while (index > newLen) {
                newLen *= 2;
            }
            counts = Arrays.copyOf(counts, newLen);
        }
        counts[index]++;
    }

    private int countsArrayIndex(final long value) {
        if (value < 0) {
            throw new ArrayIndexOutOfBoundsException("Histogram recorded value cannot be negative.");
        }
        final int bucketIndex = getBucketIndex(value);
        final int subBucketIndex = getSubBucketIndex(value, bucketIndex);
        return countsArrayIndex(bucketIndex, subBucketIndex);
    }

    private int countsArrayIndex(final int bucketIndex, final int subBucketIndex) {
        assert(subBucketIndex < SUB_BUCKET_COUNT);
        assert(bucketIndex == 0 || (subBucketIndex >= SUB_BUCKET_HALF_COUNT));
        // Calculate the index for the first frame that will be used in the bucket (halfway through subBucketCount).
        // For bucketIndex 0, all subBucketCount entries may be used, but bucketBaseIndex is still set in the middle.
        final int bucketBaseIndex = (bucketIndex + 1) << SUB_BUCKET_HALF_COUNT_MAGNITUDE;
        // Calculate the offset in the bucket. This subtraction will result in a positive value in all buckets except
        // the 0th bucket (since a value in that bucket may be less than half the bucket's 0 to subBucketCount range).
        // However, this works out since we give bucket 0 twice as much space.
        final int offsetInBucket = subBucketIndex - SUB_BUCKET_HALF_COUNT;
        // The following is the equivalent of ((subBucketIndex  - subBucketHalfCount) + bucketBaseIndex;
        return bucketBaseIndex + offsetInBucket;
    }

    private int getBucketIndex(final long value) {
        // Calculates the number of powers of two by which the value is greater than the biggest value that fits in
        // bucket 0. This is the bucket index since each successive bucket can hold a value 2x greater.
        // The mask maps small values to bucket 0.
        return LEADING_ZERO_COUNT_BASE - Long.numberOfLeadingZeros(value | SUB_BUCKET_MASK);
    }

    private int getSubBucketIndex(final long value, final int bucketIndex) {
        // For bucketIndex 0, this is just value, so it may be anywhere in 0 to subBucketCount.
        // For other bucketIndex, this will always end up in the top half of subBucketCount: assume that for some bucket
        // k > 0, this calculation will yield a value in the bottom half of 0 to subBucketCount. Then, because of how
        // buckets overlap, it would have also been in the top half of bucket k-1, and therefore would have
        // returned k-1 in getBucketIndex(). Since we would then shift it one fewer bits here, it would be twice as big,
        // and therefore in the top half of subBucketCount.
        return  (int)(value >>> bucketIndex);
    }

    @Override
    public TimeMetricsFrame frame() {
        return frame;
    }

    @Override
    public Metric metric() {
        return metric;
    }

    @Override
    public TimeFormatter timeFormatter() {
        return timeFormatter;
    }

    @Override
    public void reset(final long time) {
        Arrays.fill(counts, 0);
        min = 0;
        max = 0;
        count = 0;
        resetTime = time;
        resetCount++;
    }

    @Override
    public long resetTime() {
        return resetTime;
    }

    @Override
    public long resetCount() {
        return resetCount;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public long min() {
        return min;
    }

    @Override
    public long max() {
        return max;
    }

    @Override
    public long valueAtPercentile(final double percentile) {
        if (percentile <= 0.0) {
            return min();
        }
        if (percentile >= 1.0) {
            return max();
        }
        // Truncate to 0..100%, and remove 1 ulp to avoid roundoff overruns into next bucket when we
        // subsequently round up to the nearest integer:
        final double requestedPercentile =
                Math.min(Math.max(Math.nextAfter(percentile, Double.NEGATIVE_INFINITY), 0.0D), 1.0);
        final long countAtPercentile = Math.max(1, (long)(Math.ceil(requestedPercentile * count)));

        long totalToCurrentIndex = 0;
        for (int i = 0; i < counts.length; i++) {
            totalToCurrentIndex += counts[i];
            if (totalToCurrentIndex >= countAtPercentile) {
                final long valueAtIndex = valueFromIndex(i);
                return highestEquivalentValue(valueAtIndex);
            }
        }
        return 0;
    }

    private long valueFromIndex(final int bucketIndex, final int subBucketIndex) {
        return ((long) subBucketIndex) << bucketIndex;
    }

    private long valueFromIndex(final int index) {
        int bucketIndex = (index >> SUB_BUCKET_HALF_COUNT_MAGNITUDE) - 1;
        int subBucketIndex = (index & (SUB_BUCKET_HALF_COUNT - 1)) + SUB_BUCKET_HALF_COUNT;
        if (bucketIndex < 0) {
            subBucketIndex -= SUB_BUCKET_HALF_COUNT;
            bucketIndex = 0;
        }
        return valueFromIndex(bucketIndex, subBucketIndex);
    }

    /**
     * Get the lowest value that is equivalent to the given value within the histogram's resolution.
     * Where "equivalent" means that value samples recorded for any two
     * equivalent values are counted in a common total count.
     *
     * @param value The given value
     * @return The lowest value that is equivalent to the given value within the histogram's resolution.
     */
    public long lowestEquivalentValue(final long value) {
        final int bucketIndex = getBucketIndex(value);
        final int subBucketIndex = getSubBucketIndex(value, bucketIndex);
        return valueFromIndex(bucketIndex, subBucketIndex);
    }

    /**
     * Get the highest value that is equivalent to the given value within the histogram's resolution.
     * Where "equivalent" means that value samples recorded for any two
     * equivalent values are counted in a common total count.
     *
     * @param value The given value
     * @return The highest value that is equivalent to the given value within the histogram's resolution.
     */
    public long highestEquivalentValue(final long value) {
        return nextNonEquivalentValue(value) - 1;
    }

    /**
     * Get the next value that is not equivalent to the given value within the histogram's resolution.
     * Where "equivalent" means that value samples recorded for any two
     * equivalent values are counted in a common total count.
     *
     * @param value The given value
     * @return The next value that is not equivalent to the given value within the histogram's resolution.
     */
    public long nextNonEquivalentValue(final long value) {
        return lowestEquivalentValue(value) + sizeOfEquivalentValueRange(value);
    }

    /**
     * Get the size (in value units) of the range of values that are equivalent to the given value within the
     * histogram's resolution. Where "equivalent" means that value samples recorded for any two
     * equivalent values are counted in a common total count.
     *
     * @param value The given value
     * @return The size of the range of values equivalent to the given value.
     */
    public long sizeOfEquivalentValueRange(final long value) {
        return 1L << getBucketIndex(value);
    }

    @Override
    public String toString() {
        return "Histogram{metric=" + metric +
                ", min=" + min() +
                ", p50=" + valueAtPercentile(0.5) +
                ", p90=" + valueAtPercentile(0.9) +
                ", p99=" + valueAtPercentile(0.99) +
                ", p99.9=" + valueAtPercentile(0.999) +
                ", p99.99=" + valueAtPercentile(0.9999) +
                ", max=" + max() +
                "}";
    }
}
