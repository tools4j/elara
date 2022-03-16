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

import java.time.Instant;

import static java.time.ZoneOffset.UTC;
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

    interface MilliTimeFormatter extends TimeFormatter {
        MilliTimeFormatter DEFAULT = new MilliTimeFormatter() {};

        @Override
        default Object formatDateTime(final long epochMillis) {
            return Instant.ofEpochMilli(epochMillis);
        }

        @Override
        default Object formatTime(final long epochMillis) {
            return Instant.ofEpochMilli(epochMillis).atOffset(UTC).toLocalTime();
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "ms";
        }
    }

    interface MicroTimeFormatter extends TimeFormatter {
        MicroTimeFormatter DEFAULT = new MicroTimeFormatter() {};

        default Instant instantOfEpochMicros(final long epochMicros) {
            final long s = MICROSECONDS.toSeconds(epochMicros);
            final long u = epochMicros - SECONDS.toMicros(s);
            return Instant.ofEpochSecond(s, MICROSECONDS.toNanos(u));
        }

        @Override
        default Object formatDateTime(final long epochMicros) {
            return instantOfEpochMicros(epochMicros);
        }

        @Override
        default Object formatTime(final long epochMicros) {
            return instantOfEpochMicros(epochMicros).atOffset(UTC).toLocalTime();
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "us";
        }
    }

    interface NanoTimeFormatter extends TimeFormatter {
        NanoTimeFormatter DEFAULT = new NanoTimeFormatter() {};

        default Instant instantOfEpochNanos(final long epochNanos) {
            final long s = NANOSECONDS.toSeconds(epochNanos);
            final long n = epochNanos - SECONDS.toNanos(s);
            return Instant.ofEpochSecond(s, n);
        }

        @Override
        default Object formatDateTime(final long epochTime) {
            return instantOfEpochNanos(epochTime);
        }

        @Override
        default Object formatTime(final long epochNanos) {
            return instantOfEpochNanos(epochNanos).atOffset(UTC).toLocalTime();
        }

        @Override
        default Object formatDuration(final long nanos) {
            return nanos + "ns";
        }
    }
}
