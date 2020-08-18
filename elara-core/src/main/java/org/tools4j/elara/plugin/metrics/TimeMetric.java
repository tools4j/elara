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

/**
 * <pre>{@code
 *                             Input --> (_) --> Command -+--> Event.0 --> (_) +-> State
 *                                                        `--> Event.1 --> (_) +-> State
 *                                                                             `---------> Output
 *
 *                            ^     ^   ^   ^   ^             ^           ^    ^   ^   ^
 *  (input sending time)......'     |   |   |   |             |           |    |   |   |^
 *  (input polling time)............'   |   |   |             |           |    |   |   ||  ^    ^
 *  (command appending time)............'   |   |             |           |    |   |   ||  |    |
 *  (command polling time)..................'   |             |           |    |   |   ||  |    |
 *  (processing start time).....................'             |           |    |   |   ||  |    |
 *  (routing start time)......................................'           |    |   |   ||  |    |
 *  (event appending time)................................................'    |   |   ||  |    |
 *  (event polling time).......................................................'   |   ||  |    |
 *  (applying start time)..........................................................'   ||  |    |
 *  (applying end time)................................................................'|  |    |
 *  (processing end time)...............................................................'  |    |
 *  (output start time)....................................................................'    |
 *  (output end time)...........................................................................'
 *
 * }</pre>
 */
public enum TimeMetric {
    INPUT_POLLING_TIME,
    COMMAND_APPENDING_TIME,
    COMMAND_POLLING_TIME,
    PROCESSING_START_TIME,
    ROUTING_START_TIME,
    EVENT_APPENDING_TIME,
    EVENT_POLLING_TIME,
    APPLYING_START_TIME,
    APPLYING_END_TIME,
    PROCESSING_END_TIME,
    OUTPUT_START_TIME,
    OUTPUT_END_TIME
}
