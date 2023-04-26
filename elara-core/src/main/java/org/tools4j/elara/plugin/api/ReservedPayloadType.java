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
package org.tools4j.elara.plugin.api;

import org.tools4j.elara.flyweight.BaseEvents;
import org.tools4j.elara.plugin.boot.BootPlugin;
import org.tools4j.elara.plugin.replication.ReplicationPlugin;
import org.tools4j.elara.plugin.timer.TimerPlugin;

/**
 * Reserved payload type ranges for commands and events used by elara and by {@link SystemPlugin system plugins}.
 */
public enum ReservedPayloadType {
    /** Used by system plugins that do not reserve a payload range */
    NONE(0, 0),
    /** Payload types reserved for {@link BaseEvents} */
    BASE(-1, -9),
    /** Payload types reserved for {@link TimerPlugin} */
    TIMER(-10, -19),
    /** Payload types reserved for {@link BootPlugin} */
    BOOT(-20, -29),
//    LIVELINESS(-30, -39),
//    HEARTBEAT(-40, -49),
//    CONTROL(-50, -59),
    //..
    /** Payload types reserved for {@link ReplicationPlugin} */
    REPLICATION(-90, -99);

    /** Maximum payload type value reserved by system plugins */
    public static final int MAX_RESERVED_TYPE = -1;
    /** Minimum payload type value reserved by system plugins */
    public static final int MIN_RESERVED_TYPE = -1000;

    private final int maxType;
    private final int minType;

    ReservedPayloadType(final int maxType, final int minType) {
        assert maxType >= minType;
        assert minType >= MIN_RESERVED_TYPE;
        assert maxType <= MAX_RESERVED_TYPE;
        this.maxType = maxType;
        this.minType = minType;
    }

    public boolean isInRange(final int type) {
        return maxType >= type && type >= minType;
    }
}
