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

import org.tools4j.elara.plugin.metrics.LatencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry.Type;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.DefaultMessagePrinters.HISTOGRAM_BUCKET_VALUE_0;
import static org.tools4j.elara.format.DefaultMessagePrinters.HISTOGRAM_BUCKET_VALUE_N;
import static org.tools4j.elara.format.DefaultMessagePrinters.HISTOGRAM_VALUES_FORMAT;

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
        int bucketIndex();
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
        int index = 0;
        for (final LatencyMetric metric : set) {
            final HistogramValues histogram = histogram(line, entryId, entry, metric, index);
            final MetricValue value = metricValue(line, entryId, entry, metric, index);
            histogram.record(Math.max(0, value.value()));
            index++;
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

    @Override
    default Object metricsValues(long line, long entryId, MetricsStoreEntry entry) {
        if (entry.type() != Type.TIME) {
            return LatencyFormatter.super.metricsValues(line, entryId, entry);
        }
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        final StringWriter sw = new StringWriter(set.size() * 16);
        final PrintWriter pw = new PrintWriter(sw);
        int index = 0;
        for (final LatencyMetric metric : set) {
            final MessagePrinter<HistogramValues> histogramPrinter = histogramValuesPrinter(line, entryId, index);
            final HistogramValues histogram = histogram(line, entryId, entry, metric, index);
            histogramPrinter.print(line, entryId, histogram, pw);
            index++;
        }
        pw.flush();
        return sw.toString();
    }

    default HistogramValues histogram(final long line, final long entryId, final MetricsStoreEntry entry, final LatencyMetric metric, final int index) {
        HistogramValues histogram = latencyHistograms.get()[metric.ordinal()];
        if (histogram == null) {
            histogram = new DefaultHistogramValues(entry, metric, timeFormatter());
            latencyHistograms.get()[metric.ordinal()] = histogram;
        }
        return histogram;
    }

    interface HistogramValuesFormatter extends org.tools4j.elara.format.ValueFormatter<HistogramValues> {
        HistogramValuesFormatter DEFAULT = new HistogramValuesFormatter() {};

        /** Placeholder in metrics-values string for the name of the latency metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the histogram bucket values */
        String BUCKET_VALUES = "{bucket-values}";
        /** Placeholder in metrics-values string for the number of values in the histogram */
        String VALUE_COUNT = "{value-count}";

        default BucketDescriptor[] bucketDescriptors(long line, long entryId, HistogramValues histogram) {
            return DefaultHistogramBucket.values();
        }

        default Object line(long line, long entryId, HistogramValues histogram) {return line;}
        default Object entryId(long line, long entryId, HistogramValues histogram) {return entryId;}
        default Object metricName(long line, long entryId, HistogramValues histogram) {return histogram.metric().displayName();}
        default Object repetition(long line, long entryId, HistogramValues histogram) {return histogram.resetCount();}
        default Object bucketValues(long line, long entryId, HistogramValues histogram) {
            final BucketDescriptor[] bucketDescriptors = bucketDescriptors(line, entryId, histogram);
            final StringWriter sw = new StringWriter(bucketDescriptors.length * 16);
            final PrintWriter pw = new PrintWriter(sw);
            for (int i = 0; i < bucketDescriptors.length; i++) {
                final BucketDescriptor bucketDesc = bucketDescriptors[i];
                final BucketValue bucketValue = new DefaultBucketValue(histogram, bucketDesc, i,
                        histogram.valueAtPercentile(bucketDesc.percentile()));
                final MessagePrinter<BucketValue> printer = bucketValuePrinter(line, entryId, histogram, i);
                printer.print(line, entryId, bucketValue, pw);
            }
            histogram.reset(histogram.entry().time());
            pw.flush();
            return sw.toString();
        }

        default Object valueCount(long line, long entryId, HistogramValues histogram) {return histogram.count();}

        @Override
        default Object value(String placeholder, long line, long entryId, HistogramValues histogram) {
            switch (placeholder) {
                case LINE_SEPARATOR: return System.lineSeparator();
                case MESSAGE: return histogram;
                case LINE: return line(line, entryId, histogram);
                case ENTRY_ID: return entryId(line, entryId, histogram);
                case METRIC_NAME: return metricName(line, entryId, histogram);
                case REPETITION: return repetition(line, entryId, histogram);
                case BUCKET_VALUES: return bucketValues(line, entryId, histogram);
                case VALUE_COUNT: return valueCount(line, entryId, histogram);
                default: return placeholder;
            }
        }

        default MessagePrinter<BucketValue> bucketValuePrinter(long line, long entryId, HistogramValues histogram, int index) {
            final String format = index == 0 ? HISTOGRAM_BUCKET_VALUE_0 : HISTOGRAM_BUCKET_VALUE_N;
            final BucketValueFormatter formatter = bucketValueFormatter(line, entryId, histogram, index);
            return new ParameterizedMessagePrinter<>(format, formatter);
        }

        default BucketValueFormatter bucketValueFormatter(long line, long entryId, HistogramValues histogram, int index) {
            return BucketValueFormatter.DEFAULT;
        }

        default org.tools4j.elara.format.ValueFormatter<HistogramValues> withParentFormatter(final HistogramFormatter parentFormatter) {
            requireNonNull(parentFormatter);
            return (placeholder, line, entryId, histogram) -> {
                final Object resolved = HistogramValuesFormatter.this.value(placeholder, line, entryId, histogram);
                return resolved != placeholder ? resolved : parentFormatter.value(placeholder, line, entryId, histogram.entry());
            };
        }
    }

    interface BucketValueFormatter extends org.tools4j.elara.format.ValueFormatter<BucketValue> {
        BucketValueFormatter DEFAULT = new BucketValueFormatter() {};

        /** Placeholder in bucket-values string for the histogram bucket name */
        String BUCKET_NAME = "{bucket-name}";
        /** Placeholder in bucket-values string for the histogram bucket index */
        String BUCKET_INDEX = "{bucket-index}";
        /** Placeholder in bucket-values string for the histogram bucket value */
        String BUCKET_VALUE = "{bucket-value}";

        default Object line(long line, long entryId, BucketValue value) {return line;}
        default Object entryId(long line, long entryId, BucketValue value) {return entryId;}
        default Object bucketName(long line, long entryId, BucketValue value) {
            return value.bucketDescriptor().displayName();
        }
        default Object bucketIndex(long line, long entryId, BucketValue value) {
            return value.bucketIndex();
        }
        default Object bucketValue(long line, long entryId, BucketValue value) {
            return value.histogram().timeFormatter().formatDuration(value.bucketValue());
        }

        @Override
        default Object value(String placeholder, long line, long entryId, BucketValue value) {
            switch (placeholder) {
                case LINE_SEPARATOR: return System.lineSeparator();
                case MESSAGE: return value;
                case LINE: return line(line, entryId, value);
                case ENTRY_ID: return entryId(line, entryId, value);
                case BUCKET_NAME: return bucketName(line, entryId, value);
                case BUCKET_INDEX: return bucketIndex(line, entryId, value);
                case BUCKET_VALUE: return bucketValue(line, entryId, value);
                default: return placeholder;
            }
        }
    }

    default MessagePrinter<HistogramValues> histogramValuesPrinter(long line, long entryId, int index) {
        final HistogramValuesFormatter formatter = histogramValuesFormatter(line, entryId, index);
        return new ParameterizedMessagePrinter<>(HISTOGRAM_VALUES_FORMAT, formatter.withParentFormatter(this));
    }

    default HistogramValuesFormatter histogramValuesFormatter(long line, long entryId, int index) {
        return HistogramValuesFormatter.DEFAULT;
    }
}
