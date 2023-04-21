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
package org.tools4j.elara.samples.simple;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.type.AllInOneApp;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.source.InFlightState;
import org.tools4j.elara.store.InMemoryStore;

import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class SimpleStringApplication implements AllInOneApp, Output {

    private static final int SOURCE_ID = 999;
    private static final int TYPE_STRING = 1;

    public ElaraRunner launch(final Queue<String> inputQueue) {
        return launch(config -> config
                .input(SOURCE_ID, new StringInput(inputQueue))
                .commandStore(new InMemoryStore())
                .eventStore(new InMemoryStore())
        );
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        System.out.println("processing: " + command + ", payload=" + payloadFor(command.payloadType(), command.payload()));
        router.routeEvent(command.payloadType(), command.payload(), 0, command.payload().capacity());
    }

    @Override
    public void onEvent(final Event event) {
        System.out.println("applied: " + event + ", payload=" + payloadFor(event.payloadType(), event.payload()));
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry) {
        System.out.println("published: " + event + ", replay=" + replay + ", retry=" + retry + ", payload=" + payloadFor(event.payloadType(), event.payload()));
        return Ack.COMMIT;
    }

    private String payloadFor(final int type, final DirectBuffer payload) {
        if (type == TYPE_STRING) {
            return payload.getStringAscii(0);
        }
        return "(unknown)";
    }

    private static class StringInput implements SingleSourceInput {
        final Queue<String> strings;

        StringInput(final Queue<String> strings) {
            this.strings = requireNonNull(strings);
        }

        @Override
        public int poll(final CommandSender sender, final InFlightState inFlightState) {
            final String msg = strings.poll();
            if (msg != null) {
                final MutableDirectBuffer buffer = new ExpandableArrayBuffer(msg.length() + 4);
                final int length = buffer.putStringAscii(0, msg);
                sender.sendCommand(TYPE_STRING, buffer, 0, length);
                return 1;
            }
            return 0;
        }
    }
}