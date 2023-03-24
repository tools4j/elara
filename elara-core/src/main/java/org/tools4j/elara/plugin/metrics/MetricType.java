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
package org.tools4j.elara.plugin.metrics;

import org.tools4j.elara.flyweight.FrameType;

public enum MetricType {
    TIME(FrameType.TIME_METRICS_TYPE),
    FREQUENCY(FrameType.FREQUENCY_METRICS_TYPE);

    private final byte frameType;

    MetricType(final byte frameType) {
        this.frameType = frameType;
    }

    public byte frameType() {
        return frameType;
    }

    /**
     * @return true if this {@link #TIME}
     */
    public boolean isTime() {
        return this == TIME;
    }

    /**
     * @return true if this {@link #FREQUENCY}
     */
    public boolean isFrequency() {
        return this == FREQUENCY;
    }

    public static MetricType valueByFrameType(final byte frameType) {
        switch (frameType) {
            case FrameType.TIME_METRICS_TYPE:
                return TIME;
            case FrameType.FREQUENCY_METRICS_TYPE:
                return FREQUENCY;
            default:
                throw new IllegalArgumentException("Not a valid metric frame type: " + frameType);
        }
    }

    public static boolean isEventFrameType(final byte frameType) {
        switch (frameType) {
            case FrameType.TIME_METRICS_TYPE:
            case FrameType.FREQUENCY_METRICS_TYPE:
                return true;
            default:
                return false;
        }
    }
}
