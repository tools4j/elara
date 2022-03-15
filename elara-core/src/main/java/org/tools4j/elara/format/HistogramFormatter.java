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
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry.Type;
import org.tools4j.elara.plugin.metrics.TimeMetric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.MessagePrinters.HISTOGRAM_BUCKET_VALUE_0;
import static org.tools4j.elara.format.MessagePrinters.HISTOGRAM_BUCKET_VALUE_N;
import static org.tools4j.elara.format.MessagePrinters.HISTOGRAM_VALUES_FORMAT;

public interface HistogramFormatter extends LatencyFormatter {

    HistogramFormatter DEFAULT = new HistogramFormatter() {};

    ThreadLocal<ReferenceEntry> referenceEntry = new ThreadLocal<>();
    ThreadLocal<HistogramValues[]> latencyHistograms = ThreadLocal.withInitial(() -> new HistogramValues[LatencyMetric.count()]);

    interface ReferenceEntry {
        long line();
        long entryId();
        long time();
    }

    interface HistogramValues {
        MetricsLogEntry entry();
        Metric metric();
        long count();
        long min();
        long max();
        long valueAtPercentile(double percentile);
        void record(long value);
        void reset();
    }

    interface BucketValue {
        HistogramValues histogram();
        String bucketName();
        int bucketIndex();
        double percentile();
        long bucketValue();
    }

    default long printInterval(long line, long entryId, MetricsLogEntry entry) {
        return 1000;//every second if in millis
    }

    default boolean print(long line, long entryId, MetricsLogEntry entry) {
        final ReferenceEntry ref = referenceEntry.get();
        if (ref != null) {
            return entry.time() - ref.time() >= printInterval(line, entryId, entry);
        }
        referenceEntry.set(new DefaultReferenceEntry(line, entryId, entry));
        return false;
    }

    @Override
    default Object type(long line, long entryId, MetricsLogEntry entry) {
        final Type type = entry.type();
        switch (entry.type()) {
            case TIME:
                return "HIST";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }

    default void capture(long line, long entryId, MetricsLogEntry entry) {
        if (entry.type() != Type.TIME) {
            return;
        }
        Consumer<long[]> cacher = times -> {
            Arrays.fill(times, 0);
            final int count = entry.count();
            for (int i = 0; i < count; i++) {
                final TimeMetric metric = entry.target().metric(entry.flags(), i);
                final long time = entry.time(i);
                times[metric.ordinal()] = time;
            }
        };
        switch (entry.target()) {
            case COMMAND:
                cacher.accept(commandTimes.get());
                break;
            case EVENT:
                cacher.accept(eventTimes.get());
                break;
            default:
                //nothing to cache
        }
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        int index = 0;
        for (final LatencyMetric metric : set) {
            final HistogramValues histogram = histogram(line, entryId, entry, metric, index);
            final MetricValue value = metricValue(line, entryId, entry, metric, index);
            histogram.record(value.value());
            index++;
        }
    }

    @Override
    default Object metricsValues(long line, long entryId, MetricsLogEntry entry) {
        if (entry.type() != Type.TIME) {
            return LatencyFormatter.super.metricsValues(line, entryId, entry);
        }
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        final StringWriter sw = new StringWriter(set.size() * 16);
        final PrintWriter pw = new PrintWriter(sw);
        int index = 0;
        for (final LatencyMetric metric : set) {
            final MessagePrinter<HistogramValues> histogramPrinter = histogramValuesPrinter(line, entryId, entry, index);
            final HistogramValues histogram = histogram(line, entryId, entry, metric, index);
            histogramPrinter.print(line, entryId, histogram, pw);
            index++;
        }
        pw.flush();
        referenceEntry.set(new DefaultReferenceEntry(line, entryId, entry));
        return sw.toString();
    }

    default HistogramValues histogram(final long line, final long entryId, final MetricsLogEntry entry, final LatencyMetric metric, final int index) {
        HistogramValues histogram = latencyHistograms.get()[metric.ordinal()];
        if (histogram == null) {
            histogram = new DefaultHistogramValues(entry, metric);
            latencyHistograms.get()[metric.ordinal()] = histogram;
        }
        return histogram;
    }

    @FunctionalInterface
    interface HistogramValuesFormatter extends org.tools4j.elara.format.ValueFormatter<HistogramValues> {
        static HistogramValuesFormatter create(final HistogramFormatter formatter) {
            requireNonNull(formatter);
            return () -> formatter;
        }

        /** Placeholder in metrics-values string for the name of the latency metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the histogram bucket values */
        String BUCKET_VALUES = "{bucket-values}";
        /** Placeholder in metrics-values string for the number of values in the histogram */
        String VALUE_COUNT = "{value-count}";

        HistogramFormatter parentFormatter();

        default int percentileCount(long line, long entryId, HistogramValues value) {
            return 7;
        }

        default double percentileValue(long line, long entryId, HistogramValues value, int index) {
            switch (index) {
                case 0: return 0;
                case 1: return 0.5;
                case 2: return 0.9;
                case 3: return 0.99;
                case 4: return 0.999;
                case 5: return 0.9999;
                case 6: return 1.0;
                default: return Double.NaN;
            }
        }

        default String percentileName(long line, long entryId, HistogramValues value, int index) {
            switch (index) {
                case 0: return "min";
                case 1: return "p50";
                case 2: return "p90";
                case 3: return "p99";
                case 4: return "p99.9";
                case 5: return "p99.99";
                case 6: return "max";
                default: return "p(" + index + ")";
            }
        }

        default Object line(long line, long entryId, HistogramValues value) {return line;}
        default Object entryId(long line, long entryId, HistogramValues value) {return entryId;}
        default Object metricName(long line, long entryId, HistogramValues value) {return value.metric().displayName();}
        default Object bucketValues(long line, long entryId, HistogramValues value) {
            final int n = percentileCount(line, entryId, value);
            final StringWriter sw = new StringWriter(n * 16);
            final PrintWriter pw = new PrintWriter(sw);
            for (int i = 0; i < n; i++) {
                final double perc = percentileValue(line, entryId, value, i);
                final BucketValue bucketValue = new DefaultBucketValue(value,
                        percentileName(line, entryId, value, i), i, perc, value.valueAtPercentile(perc));
                final MessagePrinter<BucketValue> printer = bucketValuePrinter(line, entryId, value, i);
                printer.print(line, entryId, bucketValue, pw);
            }
            value.reset();
            pw.flush();
            return sw.toString();
        }

        default Object valueCount(long line, long entryId, HistogramValues value) {return value.count();}

        @Override
        default Object value(String placeholder, long line, long entryId, HistogramValues value) {
            switch (placeholder) {
                case LINE_SEPARATOR: return System.lineSeparator();
                case MESSAGE: return value;
                case LINE: return line(line, entryId, value);
                case ENTRY_ID: return entryId(line, entryId, value);
                case METRIC_NAME: return metricName(line, entryId, value);
                case BUCKET_VALUES: return bucketValues(line, entryId, value);
                case VALUE_COUNT: return valueCount(line, entryId, value);
                default: return parentFormatter().value(placeholder, line, entryId, value.entry());
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
            return value.bucketName();
        }
        default Object bucketIndex(long line, long entryId, BucketValue value) {
            return value.bucketIndex();
        }
        default Object bucketValue(long line, long entryId, BucketValue value) {return value.bucketValue();}

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

    default MessagePrinter<HistogramValues> histogramValuesPrinter(long line, long entryId, MetricsLogEntry entry, int index) {
        final HistogramValuesFormatter formatter = histogramValuesFormatter(line, entryId, entry, index);
        return new ParameterizedMessagePrinter<>(HISTOGRAM_VALUES_FORMAT, formatter);
    }

    default HistogramValuesFormatter histogramValuesFormatter(long line, long entryId, MetricsLogEntry entry, int index) {
        return HistogramValuesFormatter.create(this);
    }

}