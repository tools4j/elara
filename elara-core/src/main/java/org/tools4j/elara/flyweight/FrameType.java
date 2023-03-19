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
    /** Type for a frame that contains a normal user routed event */
    public static final byte EVENT_TYPE = 0x2;
    /** Type for a frame that contains an auto-commit event */
    public static final byte NIL_EVENT_TYPE = 0x3;
    /** Type for a frame that contains a rollback event */
    public static final byte ROLLBACK_EVENT_TYPE = 0x4;
    /** Type for a frame that contains time metric data */
    public static final byte TIME_METRIC_TYPE = 0x5;
    /** Type for a frame that contains frequency metric data */
    public static final byte FREQUENCY_METRIC_TYPE = 0x6;
}
