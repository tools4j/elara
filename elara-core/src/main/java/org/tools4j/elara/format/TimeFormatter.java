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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public interface TimeFormatter {
    TimeFormatter DEFAULT = new Default() {};
    MilliTimeFormatter MILLIS = MilliTimeFormatter.DEFAULT;
    MicroTimeFormatter MICROS = MicroTimeFormatter.DEFAULT;
    NanoTimeFormatter NANOS = NanoTimeFormatter.DEFAULT;

    Object formatDateTime(long epochTime);
    Object formatTime(long epochTime);
    Object formatDuration(long duration);

    interface Default extends TimeFormatter {
        @Override
        default Object formatDateTime(final long epochTime) {
            return epochTime;
        }

        @Override
        default Object formatTime(final long epochTime) { return epochTime; }

        @Override
        default Object formatDuration(final long duration) {
            return duration;
        }
    }

    interface InstantFormatter extends TimeFormatter {
        DateTimeFormatter dateTimeFormatter();
        DateTimeFormatter timeFormatter();
        Instant toInstant(long epochTime);

        default LocalDateTime toLocalDateTime(long epochTime) {
            return toInstant(epochTime).atOffset(ZoneOffset.UTC).toLocalDateTime();
        }

        @Override
        default String formatDateTime(final long epochTime) {
            return dateTimeFormatter().format(toLocalDateTime(epochTime));
        }

        @Override
        default String formatTime(final long epochTime) {
            return timeFormatter().format(toLocalDateTime(epochTime));
        }

        @Override
        default Object formatDuration(final long duration) {
            return duration;
        }
    }

    interface MilliTimeFormatter extends InstantFormatter {
        MilliTimeFormatter DEFAULT = new MilliTimeFormatter() {};
        /** Similar to {@link DateTimeFormatter#ISO_LOCAL_TIME} but with fixed length millisecond part */
        DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 3, 3, true)
                .toFormatter();
        DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(TIME_FORMATTER)
                .appendLiteral('Z')
                .toFormatter();


        @Override
        default DateTimeFormatter dateTimeFormatter() {
            return DATE_TIME_FORMATTER;
        }

        @Override
        default DateTimeFormatter timeFormatter() {
            return TIME_FORMATTER;
        }

        @Override
        default Instant toInstant(final long epochMillis) {
            return Instant.ofEpochMilli(epochMillis);
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "ms";
        }
    }

    interface MicroTimeFormatter extends InstantFormatter {
        MicroTimeFormatter DEFAULT = new MicroTimeFormatter() {};
        /** Similar to {@link DateTimeFormatter#ISO_LOCAL_TIME} but with fixed length microsecond part */
        DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 6, 6, true)
                .toFormatter();
        DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(TIME_FORMATTER)
                .appendLiteral('Z')
                .toFormatter();

        @Override
        default DateTimeFormatter dateTimeFormatter() {
            return DATE_TIME_FORMATTER;
        }

        @Override
        default DateTimeFormatter timeFormatter() {
            return TIME_FORMATTER;
        }

        @Override
        default Instant toInstant(final long epochMicros) {
            final long s = MICROSECONDS.toSeconds(epochMicros);
            final long u = epochMicros - SECONDS.toMicros(s);
            return Instant.ofEpochSecond(s, MICROSECONDS.toNanos(u));
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "us";
        }
    }

    interface NanoTimeFormatter extends InstantFormatter {
        NanoTimeFormatter DEFAULT = new NanoTimeFormatter() {};
        /** Similar to {@link DateTimeFormatter#ISO_LOCAL_TIME} but with fixed length nanosecond part */
        DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 9, 9, true)
                .toFormatter();
        DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .append(TIME_FORMATTER)
                .appendLiteral('Z')
                .toFormatter();

        @Override
        default DateTimeFormatter dateTimeFormatter() {
            return DATE_TIME_FORMATTER;
        }

        @Override
        default DateTimeFormatter timeFormatter() {
            return TIME_FORMATTER;
        }

        @Override
        default Instant toInstant(final long epochNanos) {
            final long s = NANOSECONDS.toSeconds(epochNanos);
            final long n = epochNanos - SECONDS.toNanos(s);
            return Instant.ofEpochSecond(s, n);
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "ns";
        }
    }
}
