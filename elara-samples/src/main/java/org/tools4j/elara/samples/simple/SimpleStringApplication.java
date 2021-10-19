/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.simple;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;

import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class SimpleStringApplication {

    private static final int SOURCE = 999;
    private static final int TYPE_STRING = 1;

    public ElaraRunner launch(final Queue<String> inputQueue) {
        return Elara.launch(Configuration.configure()
                .commandProcessor(this::process)
                .eventApplier(this::apply)
                .input(new StringInput(inputQueue))
                .output(this::publish)
                .commandLog(new InMemoryLog())
                .eventLog(new InMemoryLog())
        );
    }

    private void process(final Command command, final EventRouter router) {
        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
        router.routeEvent(command.type(), command.payload(), 0, command.payload().capacity());
    }

    private void apply(final Event event) {
        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
    }

    private Ack publish(final Event event, final boolean replay, final int retry, final CommandLoopback loopback) {
        System.out.println("published: " + event + ", replay=" + replay + ", retry=" + retry + ", payload=" + payloadFor(event.type(), event.payload()));
        return Ack.COMMIT;
    }

    private String payloadFor(final int type, final DirectBuffer payload) {
        if (type == TYPE_STRING) {
            return payload.getStringAscii(0);
        }
        return "(unknown)";
    }

    private static class StringInput implements Input {
        final Queue<String> strings;
        long seq = 0;

        StringInput(final Queue<String> strings) {
            this.strings = requireNonNull(strings);
        }

        @Override
        public int poll(final Receiver receiver) {
            final String msg = strings.poll();
            if (msg != null) {
                final MutableDirectBuffer buffer = new ExpandableArrayBuffer(msg.length() + 4);
                final int length = buffer.putStringAscii(0, msg);
                receiver.receiveMessage(SOURCE, ++seq, TYPE_STRING, buffer, 0, length);
                return 1;
            }
            return 0;
        }
    }
}