/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.plugin.metrics;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <pre>{@code
 *                             Input --> (_) --> Command -+--> Event.0 --> (_) +-> State
 *                                                        `--> Event.1 --> (_) +-> State
 *                                                                             `---------> Output
 *
 *                            ^     ^   ^   ^   ^             ^           ^    ^   ^   ^
 *  (input sending time)......'     |   |   |   |             |           |    |   |   |^
 *  (input polling time)............'   |   |   |             |           |    |^  |   ||  ^    ^
 *  (command appending time)............'   |   |             |           |    ||  |   ||  |    |
 *  (command polling time)..................'   |             |           |    ||  |   ||  |    |
 *  (processing start time).....................'             |           |    ||  |   ||  |    |
 *  (routing start time)......................................'           |    ||  |   ||  |    |
 *  (event appending time)................................................'    ||  |   ||  |    |
 *  (event polling time).......................................................'|  |   ||  |    |
 *  (output polling time).......................................................'  |   ||  |    |
 *  (applying start time)..........................................................'   ||  |    |
 *  (applying end time)................................................................'|  |    |
 *  (processing end time)...............................................................'  |    |
 *  (output start time)....................................................................'    |
 *  (output end time)...........................................................................'
 *
 * }</pre>
 */
public enum TimeMetric {
    INPUT_POLLING_TIME(Target.COMMAND),
    COMMAND_APPENDING_TIME(Target.COMMAND),
    COMMAND_POLLING_TIME(Target.COMMAND),
    PROCESSING_START_TIME(Target.COMMAND),
    ROUTING_START_TIME(Target.EVENT),
    EVENT_APPENDING_TIME(Target.EVENT),
    EVENT_POLLING_TIME(Target.EVENT),
    OUTPUT_POLLING_TIME(Target.OUTPUT),
    APPLYING_START_TIME(Target.EVENT),
    APPLYING_END_TIME(Target.EVENT),
    PROCESSING_END_TIME(Target.COMMAND),
    OUTPUT_START_TIME(Target.OUTPUT),
    OUTPUT_END_TIME(Target.OUTPUT);

    private final Target target;

    TimeMetric(final Target target) {
        this.target = requireNonNull(target);
    }

    public final Target target() {
        return target;
    }

    public enum Target{
        COMMAND((byte)0x40),
        EVENT((byte)0x80),
        OUTPUT((byte)0xC0);

        final byte flagMask;
        Target(final byte flagMask) {
            this.flagMask = flagMask;
        }
    }

    private static final TimeMetric[] VALUES = values();

    public static byte flags(final Target target, final TimeMetric... metrics) {
        byte flags = target.flagMask;
        for (final TimeMetric metric : metrics) {
            if (target == metric.target()) {
                flags |= metric.ordinal();
            }
        }
        return flags;
    }

    public static short flags(final Target target, final Set<TimeMetric> metrics) {
        byte flags = target.flagMask;
        for (final TimeMetric metric : VALUES) {
            if (target == metric.target() && metrics.contains(metric)) {
                flags |= metric.ordinal();
            }
        }
        return flags;
    }
}
