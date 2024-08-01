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
package org.tools4j.elara.flyweight;

/**
 * Defines header type constants used in headers as TYPE field as per {@link FrameDescriptor}.
 *
 * @see Header#type()
 * @see FrameDescriptor#TYPE_OFFSET
 * @see FrameDescriptor#TYPE_LENGTH
 */
public enum FrameType {
    ;
    /** Type for a frame that contains a command */
    public static final byte COMMAND_TYPE = 0x1;
    /** Type for a frame that contains an application routed intermediary event to be succeeded by further events for same the command */
    public static final byte INTERMEDIARY_EVENT_TYPE = 0x2;
    /** Type for a frame that contains an application routed commit event, the last for the command */
    public static final byte COMMIT_EVENT_TYPE = 0x3;
    /** Type for a frame that contains a system routed auto-commit event used if the application routes no events for a command */
    public static final byte AUTO_COMMIT_EVENT_TYPE = 0x4;
    /** Type for a frame that contains a rollback event */
    public static final byte ROLLBACK_EVENT_TYPE = 0x5;
    /** Type for a frame that contains playback data */
    public static final byte PLAYBACK_TYPE = 0x6;
    /** Type for a frame that contains time metrics data */
    public static final byte TIME_METRICS_TYPE = 0x7;
    /** Type for a frame that contains frequency metrics data */
    public static final byte FREQUENCY_METRICS_TYPE = 0x8;

    public static boolean isCommandType(final byte frameType) {
        return frameType == COMMAND_TYPE;
    }

    public static boolean isAppRoutedEventType(final byte frameType) {
        return frameType >= INTERMEDIARY_EVENT_TYPE && frameType <= COMMIT_EVENT_TYPE;
    }

    public static boolean isEventType(final byte frameType) {
        return frameType >= INTERMEDIARY_EVENT_TYPE && frameType <= AUTO_COMMIT_EVENT_TYPE;
    }

    public static boolean isCommitEventType(final byte frameType) {
        return frameType == COMMIT_EVENT_TYPE || frameType == AUTO_COMMIT_EVENT_TYPE;
    }

    public static boolean isRollbackEventType(final byte frameType) {
        return frameType == ROLLBACK_EVENT_TYPE;
    }

    public static boolean isPlaybackType(final byte frameType) {
        return frameType == PLAYBACK_TYPE;
    }

    public static boolean isTimeMetricsType(final byte frameType) {
        return frameType == TIME_METRICS_TYPE;
    }

    public static boolean isFrequencyMetricsType(final byte frameType) {
        return frameType == FREQUENCY_METRICS_TYPE;
    }

    public static void validateCommandType(final byte frameType) {
        if (!isCommandType(frameType)) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a command frame");
        }
    }

    public static void validateEventType(final byte frameType) {
        if (!isEventType(frameType)) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for an event frame");
        }
    }

    public static void validateDataFrameType(final byte frameType) {
        if (!(isCommandType(frameType) || isEventType(frameType))) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a command or an event frame");
        }
    }

    public static void validatePlaybackFrameType(final byte frameType) {
        if (!isPlaybackType(frameType)) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a playback data frame");
        }
    }

    public static void validateTimeMetricsType(final byte frameType) {
        if (!isTimeMetricsType(frameType)) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a time metrics frame");
        }
    }

    public static void validateFrequencyMetricsType(final byte frameType) {
        if (!isFrequencyMetricsType(frameType)) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a frequency metrics frame");
        }
    }

    public static void validateMetricsType(final byte frameType) {
        if (!(isTimeMetricsType(frameType) || isFrequencyMetricsType(frameType))) {
            throw new IllegalArgumentException("Frame type " + frameType + " is not valid for a metrics frame");
        }
    }
}
