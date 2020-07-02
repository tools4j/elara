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
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.replication.Configuration;
import org.tools4j.elara.plugin.replication.Connection;
import org.tools4j.elara.plugin.replication.EnforceLeaderInput;
import org.tools4j.elara.plugin.replication.ReplicationPlugin;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication;
import org.tools4j.elara.samples.hash.HashApplication.DefaultState;
import org.tools4j.elara.samples.hash.HashApplication.ModifiableState;
import org.tools4j.elara.samples.hash.HashApplication.State;
import org.tools4j.elara.samples.network.AtomicBuffer;
import org.tools4j.elara.samples.network.Buffer;
import org.tools4j.elara.samples.network.BufferInput;
import org.tools4j.elara.samples.network.DefaultServerTopology;
import org.tools4j.elara.samples.network.RingBuffer;
import org.tools4j.elara.samples.network.ServerTopology;
import org.tools4j.elara.samples.network.Transmitter;
import org.tools4j.nobark.run.ThreadLike;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;
import static org.tools4j.elara.samples.hash.HashApplication.commandProcessor;
import static org.tools4j.elara.samples.hash.HashApplication.eventApplier;

/**
 * Unit test running the {@link HashApplication} on multiple nodes and threads using the
 * {@link org.tools4j.elara.plugin.replication.ReplicationPlugin replication plugin}.
 */
public class ReplicatedHashApplicationTest {

    private static final int SOURCE_OFFSET = 1000000000;
    private static final int ENFORCE_LEADER_SOURCE = 2 * SOURCE_OFFSET - 1;

    private final NetworkConfig networkConfig = NetworkConfig.DEFAULT;

    @Test
    public void run() throws Exception {
        //given
        final int servers = 10;
        final int sources = 10;
        final int nThreads = 1;
        final int commandsPerSource = 100;
        final IdMapping serverIds = DefaultIdMapping.enumerate(servers);
        final IdMapping sourceIds = DefaultIdMapping.enumerate(SOURCE_OFFSET, sources);
        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(nThreads);
        final Transmitter appendTransmitter = transmitter(networkConfig.appendLink(), scheduledExecutorService);
        final Transmitter commandTransmitter = transmitter(networkConfig.commandLink(), scheduledExecutorService);
        final ServerTopology serverTopology = serverTopology(serverIds, appendTransmitter);
        final ServerTopology sourceTopology = sourceTopology(sources, serverIds, commandTransmitter);
        final ModifiableState[] appStates = appStates(servers);
        final EnforceLeaderInput enforceLeaderInput = enforceLeaderInput(serverIds);

        //when
        final ElaraRunner[] runners = startServers(appStates, serverIds, serverTopology, sourceTopology, enforceLeaderInput);
        final ThreadLike[] publishers = startSourcePublishers(commandsPerSource, sourceIds, sourceTopology);

        //then
        for (final ThreadLike publisher : publishers) {
            publisher.join(10000);
        }
        scheduledExecutorService.shutdown();
        if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
            scheduledExecutorService.shutdownNow();
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

    private Input[] inputs(final IdMapping sourceIds, final Buffer[] buffers) {
        final Input[] inputs = new Input[buffers.length];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new BufferInput(sourceIds.idByIndex(i), buffers[i]);
        }
        return inputs;
    }

    private EnforceLeaderInput enforceLeaderInput(final IdMapping serverIds) {
        final AtomicInteger nextLeader = new AtomicInteger(
                serverIds.idByIndex(0)
//                serverIds.idByIndex(ThreadLocalRandom.current().nextInt(serverIds.count()))
        );
        return () -> receiver -> {
            final int leaderId = nextLeader.getAndSet(NULL_SERVER);
            if (leaderId != NULL_SERVER) {
                receiver.enforceLeader(ENFORCE_LEADER_SOURCE, System.currentTimeMillis(), leaderId);
                return 1;
            }
            return 0;
        };
    }

    private ThreadLike[] startSourcePublishers(final int commandsPerSource,
                                               final IdMapping sourceIds,
                                               final ServerTopology sourceTopology) {
        final int sources = sourceTopology.senders();
        final ThreadLike[] publishers = new ThreadLike[sources];
        for (int i = 0; i < sources; i++) {
            final int source = SOURCE_OFFSET + i;
            publishers[i] = MulticastSource.startRandom(source, sourceIds, commandsPerSource, sourceTopology);
        }
        return publishers;
    }

    private ServerTopology serverTopology(final IdMapping serverIds, final Transmitter transmitter) {
        final int servers = serverIds.count();
        final Buffer[] sendBuffers = buffers(servers, networkConfig.appendLink().senderBufferCapacity());
        final Buffer[][] receiveBuffers = new Buffer[servers][];
        for (int i = 0; i < servers; i++) {
            receiveBuffers[i] = buffers(servers, networkConfig.appendLink().receiverBufferCapacity());
        }
        return new DefaultServerTopology(sendBuffers, receiveBuffers, transmitter);
    }

    private ServerTopology sourceTopology(final int sources,
                                          final IdMapping serverIds,
                                          final Transmitter transmitter) {
        final Buffer[] sendBuffers = buffers(sources, networkConfig.commandLink().senderBufferCapacity());
        final Buffer[][] receiveBuffers = new Buffer[serverIds.count()][];
        for (int i = 0; i < receiveBuffers.length; i++) {
            receiveBuffers[i] = buffers(sources, networkConfig.commandLink().receiverBufferCapacity());
        }
        return new DefaultServerTopology(sendBuffers, receiveBuffers, transmitter);
    }

    private Buffer[] buffers(final int n, final int capacity) {
        final Buffer[] buffers = new Buffer[n];
        for (int i = 0; i < n; i++) {
            buffers[i] = capacity == 1 ? new AtomicBuffer() : new RingBuffer(capacity);
        }
        return buffers;
    }

    private Transmitter transmitter(final NetworkConfig.LinkConfig linkConfig,
                                    final ScheduledExecutorService executorService) {
        final Transmitter delayed = Transmitter.async(linkConfig.transmissionDelayNanos(), executorService);
        final float lossRatio = linkConfig.transmissionLossRatio();
        return lossRatio == 0 ? delayed : Transmitter.unreliable(lossRatio, delayed);
    }

    private ModifiableState[] appStates(final int servers) {
        final ModifiableState[] states = new ModifiableState[servers];
        for (int i = 0; i < servers; i++) {
            states[i] = new DefaultState();
        }
        return states;
    }

    private ElaraRunner[] startServers(final ModifiableState[] appStates,
                                       final IdMapping sourceIds,
                                       final ServerTopology serverTopology,
                                       final ServerTopology sourceTopology,
                                       final EnforceLeaderInput enforceLeaderInput) {
        final int servers = appStates.length;
        final ElaraRunner[] runners = new ElaraRunner[servers];
        for (int server = 0; server < servers; server++) {
            final Input[] inputs = inputs(sourceIds, sourceTopology.receiveBuffers(server));
            final ReplicationPlugin replicationPlugin = replicationPlugin(server, sourceIds, serverTopology, enforceLeaderInput);
            runners[server] = startServer(server, sourceIds, appStates[server], inputs, replicationPlugin);
        }
        return runners;
    }

    private ReplicationPlugin replicationPlugin(final int server,
                                                final IdMapping serverIds,
                                                final ServerTopology serverTopology,
                                                final EnforceLeaderInput enforceLeaderInput) {
        return Plugins.replicationPlugin(replicationConfig(server, serverIds, serverTopology, enforceLeaderInput));
    }

    private Configuration replicationConfig(final int server,
                                            final IdMapping serverIds,
                                            final ServerTopology serverTopology,
                                            final EnforceLeaderInput enforceLeaderInput) {
        final org.tools4j.elara.plugin.replication.Context context = ReplicationPlugin.configure();
        for (int i = 0; i < serverIds.count(); i++) {
            final int serverId = serverIds.idByIndex(i);
            context.serverId(serverId, i == server)
                    .connection(serverId, connection(serverId, serverIds, serverTopology));
        }
        return context.enforceLeaderInput(enforceLeaderInput);
    }

    private Connection connection(final int serverId,
                                  final IdMapping serverIds,
                                  final ServerTopology serverTopology) {
        return Connection.create(
                new LongValuePoller(serverId, serverIds, serverTopology),
                new LongValuePublisher(serverId, serverIds, serverTopology)
        );
    }

    private ElaraRunner startServer(final int server,
                                    final IdMapping sourceIds,
                                    final ModifiableState appState,
                                    final Input[] inputs,
                                    final ReplicationPlugin replicationPlugin) {
        final int serverId = sourceIds.idByIndex(server);
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + serverId + "-cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + serverId + "-evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(appState))
                .eventApplier(eventApplier(appState))
                .inputs(inputs)
                .commandLog(new ChronicleMessageLog(cq))
                .eventLog(new ChronicleMessageLog(eq))
                .duplicateHandler(DuplicateHandler.NOOP)
                .plugin(Plugins.bootPlugin())
                .plugin(replicationPlugin)
        );
    }
}