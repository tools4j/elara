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
package org.tools4j.elara.format;

import org.tools4j.elara.format.IteratorMessagePrinter.Item;
import org.tools4j.elara.format.IteratorMessagePrinter.ItemFormatter;
import org.tools4j.elara.plugin.metrics.LatencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry.Type;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.MessagePrinter.iterationToken;

public interface HistogramFormatter extends LatencyFormatter {

    HistogramFormatter DEFAULT = new HistogramFormatter() {};

    static HistogramFormatter create(final TimeFormatter timeFormatter, final long interval) {
        requireNonNull(timeFormatter);
        return new HistogramFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }

            @Override
            public long intervalValue(final long line, final long entryId, final MetricsStoreEntry entry) {
                return interval;
            }
        };
    }

    ThreadLocal<HistogramValues[]> latencyHistograms = ThreadLocal.withInitial(() -> new HistogramValues[LatencyMetric.count()]);

    interface HistogramValues {
        MetricsStoreEntry entry();
        Metric metric();
        TimeFormatter timeFormatter();
        long resetTime();
        long resetCount();
        long count();
        long min();
        long max();
        long valueAtPercentile(double percentile);
        void record(long value);
        void reset(long time);
    }

    interface BucketDescriptor {
        String displayName();
        double percentile();//use 0.0 for min and 1.0 for max
    }

    interface BucketValue {
        HistogramValues histogram();
        BucketDescriptor bucketDescriptor();
        long bucketValue();
    }

    enum CaptureResult {
        NOOP,
        CAPTURE,
        PRINT
    }
    default CaptureResult capture(long line, long entryId, MetricsStoreEntry entry) {
        if (entry.type() != Type.TIME) {
            return CaptureResult.NOOP;
        }
        cacheTimeValues(line, entryId, entry);
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        final long intervalValue = intervalValue(line, entryId, entry);
        boolean printAny = false;
        for (final LatencyMetric metric : set) {
            final HistogramValues histogram = histogramValue(line, entryId, entry, metric);
            final MetricValue value = metricValue(line, entryId, entry, metric);
            histogram.record(Math.max(0, value.value()));
            printAny = entry.time() - histogram.resetTime() >= intervalValue;
        }
        return printAny ? CaptureResult.PRINT : CaptureResult.CAPTURE;
    }

    @Override
    default Object interval(long line, long entryId, MetricsStoreEntry entry) {
        return timeFormatter().formatDuration(intervalValue(line, entryId, entry));
    }

    default long intervalValue(long line, long entryId, MetricsStoreEntry entry) {
        return 1000;//every second if in millis
    }

    @Override
    default Object type(long line, long entryId, MetricsStoreEntry entry) {
        final Type type = entry.type();
        switch (entry.type()) {
            case TIME:
                return "HIST";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }

    default HistogramValuesFormatter histogramValuesFormatter() {
        return HistogramValuesFormatter.DEFAULT;
    }
    default ItemFormatter<HistogramValues> histogramValuesFormatter(final String bucketValuesPattern,
                                                                    final String bucketValueSeparator) {
        final HistogramValuesFormatter formatter = histogramValuesFormatter();
        return formatter
                .withParentFormatter(this)
                .then(iterationToken(HistogramValuesFormatter.BUCKET_VALUES, bucketValuesPattern, bucketValueSeparator,
                        formatter.bucketValueFormatter(), formatter::bucketValues));
    }

    default Iterable<HistogramValues> histogramValues(long line, long entryId, MetricsStoreEntry entry) {
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        return set.stream().map(metric -> histogramValue(line, entryId, entry, metric)).collect(Collectors.toList());
    }

    default HistogramValues histogramValue(final long line, final long entryId, final MetricsStoreEntry entry, final LatencyMetric metric) {
        HistogramValues histogram = latencyHistograms.get()[metric.ordinal()];
        if (histogram == null) {
            histogram = new DefaultHistogramValues(entry, metric, timeFormatter());
            latencyHistograms.get()[metric.ordinal()] = histogram;
        }
        return histogram;
    }

    interface HistogramValuesFormatter extends ItemFormatter<HistogramValues> {
        HistogramValuesFormatter DEFAULT = new HistogramValuesFormatter() {};

        /** Placeholder in metrics-values string for the name of the latency metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the histogram's value count */
        String VALUE_COUNT = "{value-count}";
        /** Placeholder in metrics-values string for the histogram bucket values */
        String BUCKET_VALUES = "{bucket-values}";

        default BucketDescriptor[] bucketDescriptors(long line, long entryId, HistogramValues histogram) {
            return DefaultHistogramBucket.values();
        }

        default Object metricName(long line, long entryId, HistogramValues histogram) {return histogram.metric().displayName();}
        default Object repetition(long line, long entryId, HistogramValues histogram) {return histogram.resetCount();}

        default Object valueCount(long line, long entryId, HistogramValues histogram) {return histogram.count();}

        default BucketValueFormatter bucketValueFormatter() {
            return BucketValueFormatter.DEFAULT;
        }

        default Iterable<BucketValue> bucketValues(final long line, final long entryId, final Item<? extends HistogramValues> histogramItem) {
            final HistogramValues histogram = histogramItem.itemValue();
            final Iterable<BucketValue> bucketValues = bucketValues(line, entryId, histogram);
            //NOTE: we reset the histograms after accessing the bucket values via item, which is not the case when we print only names
            //TODO: find a cleaner way to reset histograms
            histogram.reset(histogram.entry().time());
            return bucketValues;
        }

        default Iterable<BucketValue> bucketValues(final long line, final long entryId, final HistogramValues histogram) {
            final BucketDescriptor[] bucketDescriptors = bucketDescriptors(line, entryId, histogram);
            return Arrays.stream(bucketDescriptors).map(bucketDesc ->
                    new DefaultBucketValue(histogram, bucketDesc, histogram.valueAtPercentile(bucketDesc.percentile()))
            ).collect(Collectors.toList());
        }

        @Override
        default Object value(final String placeholder, final long line, final long entryId, final Item<? extends HistogramValues> item) {
            switch (placeholder) {
                case METRIC_NAME: return metricName(line, entryId, item.itemValue());
                case REPETITION: return repetition(line, entryId, item.itemValue());
                case VALUE_COUNT: return valueCount(line, entryId, item.itemValue());
                default: return ItemFormatter.super.value(placeholder, line, entryId, item);
            }
        }

        default HistogramValuesFormatter withParentFormatter(final HistogramFormatter parentFormatter) {
            requireNonNull(parentFormatter);
            return new HistogramValuesFormatter() {
                @Override
                public Object value(final String placeholder, final long line, final long entryId, final Item<? extends HistogramValues> item) {
                    final Object resolved = HistogramValuesFormatter.this.value(placeholder, line, entryId, item);
                    return resolved != placeholder ? resolved : parentFormatter.value(placeholder, line, entryId, item.itemValue().entry());
                }
            };
        }
    }

    interface BucketValueFormatter extends ItemFormatter<BucketValue> {
        BucketValueFormatter DEFAULT = new BucketValueFormatter() {};

        /** Placeholder in bucket-values string for the histogram bucket name */
        String BUCKET_NAME = "{bucket-name}";
        /** Placeholder in bucket-values string for the histogram bucket value */
        String BUCKET_VALUE = "{bucket-value}";

        default Object bucketName(long line, long entryId, BucketValue value) {
            return value.bucketDescriptor().displayName();
        }
        default Object bucketValue(long line, long entryId, BucketValue value) {
            return value.histogram().timeFormatter().formatDuration(value.bucketValue());
        }

        @Override
        default Object value(final String placeholder, final long line, final long entryId, final Item<? extends BucketValue> bucketItem) {
            switch (placeholder) {
                case BUCKET_NAME: return bucketName(line, entryId, bucketItem.itemValue());
                case BUCKET_VALUE: return bucketValue(line, entryId, bucketItem.itemValue());
                default: return ItemFormatter.super.value(placeholder, line, entryId, bucketItem);
            }
        }
    }
}
