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
package org.tools4j.elara.samples.replication;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.chronicle.ChronicleMessageLog;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication;
import org.tools4j.elara.samples.hash.HashApplication.DefaultState;
import org.tools4j.elara.samples.hash.HashApplication.ModifiableState;
import org.tools4j.elara.samples.hash.HashApplication.State;
import org.tools4j.nobark.run.ThreadLike;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import static org.tools4j.elara.samples.hash.HashApplication.NULL_VALUE;
import static org.tools4j.elara.samples.hash.HashApplication.commandProcessor;
import static org.tools4j.elara.samples.hash.HashApplication.eventApplier;

/**
 * Unit test running the {@link HashApplication} on multiple nodes and threads using the
 * {@link org.tools4j.elara.plugin.replication.ReplicationPlugin replication plugin}.
 */
public class ReplicatedHashApplicationTest {

    private static final int SOURCE_OFFSET = 1000000000;
    private final int commandTransmissionDelayMillisMin = 0;
    private final int commandTransmissionDelayMillisMax = 2;
    private final int appendRequestTransmissionDelayMillisMin = 0;
    private final int appendRequestTransmissionDelayMillisMax = 10;
    private final int appendResponseTransmissionDelayMillisMin = 0;
    private final int appendResponseTransmissionDelayMillisMax = 10;
    private final float commandTransmissionLossRatio = 0.01f;
    private final float appendRequestTransmissionLossRatio = 0.01f;
    private final float appendResponseTransmissionLossRatio = 0.01f;
    private final int networkBufferCapacity = 100;

    @Test
    public void run() throws Exception {
        //given
        final int servers = 10;
        final int sources = 10;
        final int commandsPerSource = 1000;
        final BufferInput[][] inputByServerAndSource = inputs(servers, sources);
        final ModifiableState[] appStates = appStates(servers);

        //when
        final ElaraRunner[] runners = startServers(appStates, inputByServerAndSource);
        final ThreadLike[] publishers = startPublishers(commandsPerSource, inputByServerAndSource);

        //then
        for (final ThreadLike publisher : publishers) {
            publisher.join(1000);
        }
        for (final ElaraRunner runner : runners) {
            runner.stop();
            runner.join(1000);
        }
        for (int i = 0; i < servers; i++) {
            final State state = appStates[i];
            System.out.println("server[" + i + "]: count=" + state.count() + ", hash=" + state.hash());
        }
    }

    private BufferInput[][] inputs(final int servers, final int sources) {
        final BufferInput inputs[][] = new BufferInput[servers][sources];
        for (int server = 0; server < servers; server++) {
            for (int source = 0; source < sources; source++) {
                inputs[server][source] = input(SOURCE_OFFSET + source);
            }
        }
        return inputs;
    }

    private BufferInput input(final int source) {
        return new BufferInput(source, new RingBuffer(networkBufferCapacity));
    }

    private ThreadLike[] startPublishers(final int commandsPerSource, final BufferInput[][] inputByServerAndSource) {
        final int sources = inputByServerAndSource[0].length;
        final ThreadLike[] publishers = new ThreadLike[sources];
        for (int i = 0; i < sources; i++) {
            publishers[i] = startPublisher(i, commandsPerSource, inputByServerAndSource);
        }
        return publishers;
    }

    private ThreadLike startPublisher(final int sourceIndex,
                                      final int commandsPerSource,
                                      final BufferInput[][] inputByServerAndSource) {
        final int servers = inputByServerAndSource.length;
        final Channel[] channels = new Channel[servers];
        for (int i = 0; i < servers; i++) {
            channels[i] = networkChannel(inputByServerAndSource[i][sourceIndex].buffer());
        }
        return MulticastPublisher.start("publisher-" + sourceIndex, randomValueSupplier(), commandsPerSource,
                channels);
    }

    private Channel networkChannel(final Channel destination) {
        return new LatentChannel(new UnreliableChannel(destination, commandTransmissionLossRatio),
                commandTransmissionDelayMillisMin, commandTransmissionDelayMillisMax);
    }

    private ModifiableState[] appStates(final int servers) {
        final ModifiableState[] states = new ModifiableState[servers];
        for (int i = 0; i < servers; i++) {
            states[i] = new DefaultState();
        }
        return states;
    }

    private ElaraRunner[] startServers(final ModifiableState[] appStates, final BufferInput[][] inputByServerAndSource) {
        final int servers = inputByServerAndSource.length;
        final ElaraRunner[] runners = new ElaraRunner[servers];
        for (int server = 0; server < servers; server++) {
            runners[server] = startServer(server, appStates[server], inputByServerAndSource[server]);
        }
        return runners;
    }

    private ElaraRunner startServer(final int server, final ModifiableState appState, final BufferInput[] inputs) {
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + server + "-cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + server + "-evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(appState))
                .eventApplier(eventApplier(appState))
                .inputs(inputs)
                .commandLog(new ChronicleMessageLog(cq))
                .eventLog(new ChronicleMessageLog(eq))
                .duplicateHandler(DuplicateHandler.NOOP)
        );
    }

    private static LongSupplier randomValueSupplier() {
        return () -> {
            long value;
            do {
                value = ThreadLocalRandom.current().nextLong();;
            } while (value == NULL_VALUE);
            return value;
        };
    }

}