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
 * Event type constants, each type corresponding to an event {@link FrameType}.
 */
public enum EventType {
    /**
     * Application routed intermediary event to be succeeded by further events for same the command, corresponds to
     * frame type {@link FrameType#INTERMEDIARY_EVENT_TYPE}
     */
    INTERMEDIARY(FrameType.INTERMEDIARY_EVENT_TYPE),
    /**
     * Application routed commit event, the last for the command, corresponds to frame type
     * {@link FrameType#COMMIT_EVENT_TYPE}
     */
    APP_COMMIT(FrameType.COMMIT_EVENT_TYPE),
    /**
     * System routed auto-commit event used if the application routes no events for command, corresponds to frame
     * type {@link FrameType#AUTO_COMMIT_EVENT_TYPE}
     */
    AUTO_COMMIT(FrameType.AUTO_COMMIT_EVENT_TYPE),
    /**
     * Rollback event used to void all events for the command, corresponds to frame type
     * {@link FrameType#ROLLBACK_EVENT_TYPE}
     */
    ROLLBACK(FrameType.ROLLBACK_EVENT_TYPE);

    private final byte frameType;

    EventType(final byte frameType) {
        this.frameType = frameType;
    }

    public final byte frameType() {
        return frameType;
    }

    /**
     * @return true if this is {@link #APP_COMMIT} or {@link #AUTO_COMMIT}
     */
    public final boolean isCommit() {
        return this == APP_COMMIT || this == AUTO_COMMIT;
    }

    /**
     * @return true if this is {@link #APP_COMMIT}
     */
    public final boolean isAppCommit() {
        return this == APP_COMMIT;
    }

    /**
     * @return true if this is {@link #AUTO_COMMIT}
     */
    public final boolean isAutoCommit() {
        return this == AUTO_COMMIT;
    }

    /**
     * @return true if this is {@link #ROLLBACK}
     */
    public final boolean isRollback() {
        return this == ROLLBACK;
    }

    /**
     * @return true if this is {@link #INTERMEDIARY}
     */
    public final boolean isIntermediary() {
        return this == INTERMEDIARY;
    }

    /**
     * @return true if commit or rollback is true
     */
    public final boolean isLast() {
        return isCommit() || isRollback();
    }

    /**
     * @return true if this is {@link #INTERMEDIARY} or {@link #APP_COMMIT}
     */
    public final boolean isAppRouted() {
        return this == INTERMEDIARY || this == APP_COMMIT;
    }

    public static EventType valueByFrameType(final byte frameType) {
        switch (frameType) {
            case FrameType.INTERMEDIARY_EVENT_TYPE:
                return INTERMEDIARY;
            case FrameType.COMMIT_EVENT_TYPE:
                return APP_COMMIT;
            case FrameType.AUTO_COMMIT_EVENT_TYPE:
                return AUTO_COMMIT;
            case FrameType.ROLLBACK_EVENT_TYPE:
                return ROLLBACK;
            default:
                throw new IllegalArgumentException("Not a valid even frame type: " + frameType);
        }
    }

    public static boolean isEventFrameType(final byte frameType) {
        switch (frameType) {
            case FrameType.INTERMEDIARY_EVENT_TYPE:
            case FrameType.COMMIT_EVENT_TYPE:
            case FrameType.AUTO_COMMIT_EVENT_TYPE:
            case FrameType.ROLLBACK_EVENT_TYPE:
                return true;
            default:
                return false;
        }
    }
}
