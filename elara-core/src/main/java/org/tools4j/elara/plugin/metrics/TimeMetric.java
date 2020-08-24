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
    INPUT_SENDING_TIME(Target.COMMAND, (byte)0x01),
    INPUT_POLLING_TIME(Target.COMMAND, (byte)0x02),
    COMMAND_APPENDING_TIME(Target.COMMAND, (byte)0x04),
    COMMAND_POLLING_TIME(Target.COMMAND, (byte)0x08),
    PROCESSING_START_TIME(Target.COMMAND, (byte)0x10),
    ROUTING_START_TIME(Target.EVENT, (byte)0x01),
    EVENT_APPENDING_TIME(Target.EVENT, (byte)0x02),
    EVENT_POLLING_TIME(Target.EVENT, (byte)0x04),
    OUTPUT_POLLING_TIME(Target.OUTPUT, (byte)0x01),
    APPLYING_START_TIME(Target.EVENT, (byte)0x08),
    APPLYING_END_TIME(Target.EVENT, (byte)0x10),
    PROCESSING_END_TIME(Target.COMMAND, (byte)0x20),
    OUTPUT_START_TIME(Target.OUTPUT, (byte)0x02),
    OUTPUT_END_TIME(Target.OUTPUT, (byte)0x04);

    private final Target target;
    private final byte flagBit;

    TimeMetric(final Target target, final byte flagBit) {
        this.target = requireNonNull(target);
        this.flagBit = flagBit;
    }

    public final Target target() {
        return target;
    }

    public enum Target{
        COMMAND((byte)0x40),
        EVENT((byte)0x80),
        OUTPUT((byte)0xC0);

        private static final Target[] VALUES = values();
        private static final byte MASK = (byte)0xC0;

        final byte flagMask;
        Target(final byte flagMask) {
            this.flagMask = flagMask;
        }

        public static Target byFlags(final byte flags) {
            final byte masked = (byte)(MASK & flags);
            for (final Target target : VALUES) {
                if (target.flagMask == masked) {
                    return target;
                }
            }
            throw new IllegalArgumentException("No valid target in flags: " + flags);
        }

        public byte flags(final TimeMetric... metrics) {
            byte flags = flagMask;
            for (final TimeMetric metric : metrics) {
                if (this == metric.target()) {
                    flags |= metric.flagBit;
                }
            }
            return flags;
        }

        public short flags(final Set<TimeMetric> metrics) {
            byte flags = flagMask;
            for (final TimeMetric metric : TimeMetric.VALUES) {
                if (this == metric.target() && metrics.contains(metric)) {
                    flags |= metric.flagBit;
                }
            }
            return flags;
        }
    }

    private static final TimeMetric[] VALUES = values();

}
