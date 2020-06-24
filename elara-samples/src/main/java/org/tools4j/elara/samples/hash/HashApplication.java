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
package org.tools4j.elara.samples.hash;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.chronicle.ChronicleMessageLog;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver.ReceivingContext;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;

/**
 * Rules:
 * <pre>
 *     - Commands are simply long values
 *     - Events are also long values; if current state hash is
 *         - even: then event value is same as command value
 *         - odd:  then event value is the bitwise inverse of the value
 *     - State is a simple hash of all event values
 * </pre>
 */
public class HashApplication {

    public static final long NULL_VALUE = Long.MIN_VALUE;
    public static final int DEFAULT_SOURCE = 42;

    public interface State {
        long hash();
        long count();
    }

    public interface ModifiableState extends State {
        void update(long add);
    }

    public static class DefaultState implements ModifiableState {
        private long hash;
        private long count;

        @Override
        public long hash() {
            return hash;
        }

        @Override
        public void update(final long add) {
            hash = 47 * hash + add;
            count++;
        }

        @Override
        public long count() {
            return count;
        }
    }

    public static CommandProcessor commandProcessor(final State state) {
        requireNonNull(state);
        return (command, router) -> {
            if (command.isApplication()) {
                final long commandValue = command.payload().getLong(0);
                final long eventValue = isEven(state.hash()) ? commandValue : ~commandValue;
                try (final RoutingContext context = router.routingEvent()) {
                    context.buffer().putLong(0, eventValue);
                    context.route(Long.BYTES);
                }
            }
        };
    }

    public static EventApplier eventApplier(final ModifiableState state) {
        requireNonNull(state);
        return event -> {
            if (event.isApplication()) {
                final long value = event.payload().getLong(0);
                state.update(value);
            }
        };
    }

    public static Input input(final AtomicLong input) {
        return input(DEFAULT_SOURCE, input);
    }

    public static Input input(final int source, final AtomicLong input) {
        requireNonNull(input);
        final AtomicLong seqNo = new AtomicLong();
        return () -> receiver -> {
            final long value = input.getAndSet(NULL_VALUE);
            if (value != NULL_VALUE) {
                final long seq = seqNo.incrementAndGet();
                try (final ReceivingContext context = receiver.receivingMessage(source, seq)) {
                    context.buffer().putLong(0, value);
                    context.receive(Long.BYTES);
                }
                return 1;
            }
            return 0;
        };
    }

    public static ElaraRunner inMemory(final ModifiableState state, final AtomicLong input) {
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(state))
                .eventApplier(eventApplier(state))
                .input(input(input))
                .commandLog(new InMemoryLog())
                .eventLog(new InMemoryLog())
        );
    }

    public static ElaraRunner chronicleQueue(final ModifiableState state, final AtomicLong input) {
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(state))
                .eventApplier(eventApplier(state))
                .input(input(input))
                .commandLog(new ChronicleMessageLog(cq))
                .eventLog(new ChronicleMessageLog(eq))
        );
    }

    private static boolean isEven(final long value) {
        return (value & 0x1) == 0;
    }
}