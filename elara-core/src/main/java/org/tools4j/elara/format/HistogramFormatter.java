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

import org.tools4j.elara.flyweight.TimeMetricsFrame;
import org.tools4j.elara.format.IteratorMessagePrinter.Item;
import org.tools4j.elara.format.IteratorMessagePrinter.ItemFormatter;
import org.tools4j.elara.plugin.metrics.LatencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;

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
            public long intervalValue(final long line, final long entryId, final TimeMetricsFrame frame) {
                return interval;
            }
        };
    }

    ThreadLocal<HistogramValues[]> latencyHistograms = ThreadLocal.withInitial(() -> new HistogramValues[LatencyMetric.count()]);

    /** Placeholder in format string for iteration value */
    String ITERATION = "{iteration}";
    /** Placeholder in format string for interval value */
    String INTERVAL = "{interval}";

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final TimeMetricsFrame frame) {
        switch (placeholder) {
            case INTERVAL: return interval(entryId, entryId, frame);
            default: return LatencyFormatter.super.value(placeholder, line, entryId, frame);
        }
    }

    interface HistogramValues {
        TimeMetricsFrame frame();
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
    default CaptureResult capture(long line, long entryId, TimeMetricsFrame frame) {
        if (frame.metricType() != MetricType.TIME) {
            return CaptureResult.NOOP;
        }
        cacheTimeValues(line, entryId, frame);
        final EnumSet<LatencyMetric> set = metricsSet(frame);
        final long intervalValue = intervalValue(line, entryId, frame);
        boolean printAny = false;
        for (final LatencyMetric metric : set) {
            final HistogramValues histogram = histogramValue(line, entryId, frame, metric);
            final MetricValue value = metricValue(line, entryId, frame, metric);
            histogram.record(Math.max(0, value.value()));
            printAny = frame.metricTime() - histogram.resetTime() >= intervalValue;
        }
        return printAny ? CaptureResult.PRINT : CaptureResult.CAPTURE;
    }

    default Object interval(long line, long entryId, TimeMetricsFrame frame) {
        return timeFormatter().formatDuration(intervalValue(line, entryId, frame));
    }

    default long intervalValue(long line, long entryId, TimeMetricsFrame frame) {
        return 1000;//every second if in millis
    }

    @Override
    default Object metricType(long line, long entryId, TimeMetricsFrame frame) {
        final MetricType type = frame.metricType();
        switch (frame.metricType()) {
            case TIME:
                return "HIST";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }

    default HistogramValuesFormatter histogramValuesFormatter() {
        return HistogramValuesFormatter.DEFAULT.withParentFormatter(this);
    }

    default ItemFormatter<HistogramValues> histogramValuesFormatter(final String bucketValuesPattern,
                                                                    final String bucketValueSeparator) {
        final HistogramValuesFormatter formatter = histogramValuesFormatter();
        return formatter
                .withParentFormatter(this)
                .then(iterationToken(HistogramValuesFormatter.BUCKET_VALUES, bucketValuesPattern, bucketValueSeparator,
                        bucketValueFormatter(), this::bucketValues, formatter));
    }

    default BucketValueFormatter bucketValueFormatter() {
        return BucketValueFormatter.DEFAULT.withRootFormatter(this);
    }


    default Iterable<HistogramValues> histogramValues(long line, long entryId, TimeMetricsFrame frame) {
        final EnumSet<LatencyMetric> set = metricsSet(frame);
        return set.stream().map(metric -> histogramValue(line, entryId, frame, metric)).collect(Collectors.toList());
    }

    default HistogramValues histogramValue(final long line, final long entryId, final TimeMetricsFrame frame, final LatencyMetric metric) {
        HistogramValues histogram = latencyHistograms.get()[metric.ordinal()];
        if (histogram == null) {
            histogram = new DefaultHistogramValues(frame, metric, timeFormatter());
            latencyHistograms.get()[metric.ordinal()] = histogram;
        }
        return histogram;
    }

    default Iterable<BucketValue> bucketValues(final long line, final long entryId, final Item<? extends HistogramValues> histogramItem) {
        final HistogramValues histogram = histogramItem.itemValue();
        final Iterable<BucketValue> bucketValues = bucketValues(line, entryId, histogram);
        //NOTE: we reset the histograms after accessing the bucket values via item, which is not the case when we print only names
        //TODO: find a cleaner way to reset histograms
        histogram.reset(histogram.frame().metricTime());
        return bucketValues;
    }

    default Iterable<BucketValue> bucketValues(final long line, final long entryId, final HistogramValues histogram) {
        final BucketDescriptor[] bucketDescriptors = bucketDescriptors(line, entryId, histogram);
        return Arrays.stream(bucketDescriptors).map(bucketDesc ->
                new DefaultBucketValue(histogram, bucketDesc, histogram.valueAtPercentile(bucketDesc.percentile()))
        ).collect(Collectors.toList());
    }

    default BucketDescriptor[] bucketDescriptors(long line, long entryId, HistogramValues histogram) {
        return DefaultHistogramBucket.values();
    }

    interface HistogramValuesFormatter extends ItemFormatter<HistogramValues> {
        HistogramValuesFormatter DEFAULT = new HistogramValuesFormatter() {};

        /** Placeholder in metrics-values string for the name of the latency metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the histogram's value count */
        String VALUE_COUNT = "{value-count}";
        /** Placeholder in metrics-values string for the histogram bucket values */
        String BUCKET_VALUES = "{bucket-values}";

        default Object metricName(long line, long entryId, HistogramValues histogram) {return histogram.metric().displayName();}
        default Object iteration(long line, long entryId, HistogramValues histogram) {return histogram.resetCount();}

        default Object valueCount(long line, long entryId, HistogramValues histogram) {return histogram.count();}

        @Override
        default Object value(final String placeholder, final long line, final long entryId, final Item<? extends HistogramValues> item) {
            switch (placeholder) {
                case METRIC_NAME: return metricName(line, entryId, item.itemValue());
                case ITERATION: return iteration(line, entryId, item.itemValue());
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
                    return resolved != placeholder ? resolved : parentFormatter.value(placeholder, line, entryId, item.itemValue().frame());
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

        default BucketValueFormatter withRootFormatter(final HistogramFormatter rootFormatter) {
            requireNonNull(rootFormatter);
            return new BucketValueFormatter() {
                @Override
                public Object value(final String placeholder, final long line, final long entryId, final Item<? extends BucketValue> item) {
                    final Object resolved = BucketValueFormatter.this.value(placeholder, line, entryId, item);
                    return resolved != placeholder ? resolved : rootFormatter.value(placeholder, line, entryId, item.itemValue().histogram().frame());
                }
            };
        }
    }
}
