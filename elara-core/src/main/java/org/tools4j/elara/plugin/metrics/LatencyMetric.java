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

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.COMMAND_APPENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.COMMAND_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.EVENT_APPENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.EVENT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.ROUTING_START_TIME;

/**
 * <pre>{@code
 *                             Input --> (_) --> Command -+--> Event.0 --> (_) +-> State
 *                                                        `--> Event.1 --> (_) +-> State
 *                                                                             `---------> Output
 *
 *  (command polling latency)       <------->
 *  (command queuing latency)           <--->
 *  (command processing latency)                <--------+
 *                                                       `----------------------------->
 *  (command handling latency)               <----------------------------------------->
 *  (event routing latency)                                   <---------->
 *  (event applying latency)                                                       <--->
 *  (event queuing latency)                                               <--->
 *  (event handling latency)                                  <------------------------>
 *  (output publication latency)                                                           <---->
 *  (event publication latency)                                           <--------------------->
 *  (command to output latency)              <-------------------------------------------------->
 *  (input to output latency)       <----------------------------------------------------------->
 *
 * }</pre>
 */
public enum LatencyMetric {
    COMMAND_POLLING_LATENCY(INPUT_POLLING_TIME, COMMAND_POLLING_TIME),
    COMMAND_QUEUING_LATENCY(COMMAND_APPENDING_TIME, COMMAND_POLLING_TIME),
    COMMAND_PROCESSING_LATENCY(PROCESSING_START_TIME, PROCESSING_END_TIME),
    COMMAND_HANDLING_LATENCY(COMMAND_POLLING_TIME, PROCESSING_END_TIME),
    EVENT_ROUTING_LATENCY(ROUTING_START_TIME, EVENT_APPENDING_TIME),
    EVENT_APPLYING_LATENCY(APPLYING_START_TIME, APPLYING_END_TIME),
    EVENT_QUEUING_LATENCY(EVENT_APPENDING_TIME, EVENT_POLLING_TIME),
    EVENT_HANDLING_LATENCY(ROUTING_START_TIME, APPLYING_END_TIME),
    OUTPUT_PUBLICATION_LATENCY(OUTPUT_START_TIME, OUTPUT_END_TIME),
    EVENT_PUBLICATION_LATENCY(EVENT_APPENDING_TIME, OUTPUT_END_TIME),
    COMMAND_TO_OUTPUT_LATENCY(COMMAND_POLLING_TIME, OUTPUT_END_TIME),
    INPUT_TO_OUTPUT_LATENCY(INPUT_POLLING_TIME, OUTPUT_END_TIME);

    private final TimeMetric start;
    private final TimeMetric end;

    LatencyMetric(final TimeMetric start, final TimeMetric end) {
        this.start = requireNonNull(start);
        this.end = requireNonNull(end);
    }
}
