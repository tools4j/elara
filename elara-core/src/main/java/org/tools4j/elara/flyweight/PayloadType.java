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
package org.tools4j.elara.flyweight;

import org.tools4j.elara.plugin.api.ReservedPayloadType;

/**
 * Contains static helper methods for command and event payload type.
 *
 * @see DataFrame#payloadType()
 */
public enum PayloadType {
    ;
    /** Default payload type used if no explicit value is provided when encoding commands or events. */
    public static final int DEFAULT = 0;

    /** Max payload type that is reserved for elara and elara plugins*/
    public static final int RESERVED_MAX = ReservedPayloadType.MAX_RESERVED_TYPE;
    /** Min payload type that is reserved for elara and elara plugins*/
    public static final int RESERVED_MIN = ReservedPayloadType.MIN_RESERVED_TYPE;

    public static boolean isSystem(final int payloadType) {
        return payloadType < 0;
    }

    public static boolean isApplication(final int payloadType) {
        return payloadType >= 0;
    }
}
