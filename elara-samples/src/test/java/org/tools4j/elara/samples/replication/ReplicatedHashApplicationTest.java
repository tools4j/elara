/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.junit.jupiter.api.condition.OS;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.type.AllInOneAppConfig;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.logging.Logger.Level;
import org.tools4j.elara.logging.OutputStreamLogger;
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
import org.tools4j.elara.samples.network.Buffer;
import org.tools4j.elara.samples.network.BufferInput;
import org.tools4j.elara.samples.network.DefaultServerTopology;
import org.tools4j.elara.samples.network.RingBuffer;
import org.tools4j.elara.samples.network.ServerTopology;
import org.tools4j.elara.samples.network.Transmitter;
import org.tools4j.elara.samples.replication.NetworkConfig.LinkConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit test running the {@link HashApplication} on multiple nodes and threads using the
 * {@link org.tools4j.elara.plugin.replication.ReplicationPlugin replication plugin}.
 */
//@Disabled
public class ReplicatedHashApplicationTest {

    private static final int SOURCE_OFFSET = 1000000000;
    private static final int ENFORCE_LEADER_SOURCE = 2 * SOURCE_OFFSET - 1;

    //private final NetworkConfig networkConfig = NetworkConfig.RELIABLE;
    private final NetworkConfig networkConfig = NetworkConfig.UNRELIABLE;

    @Test
    public void run() throws Exception {
        //given
        final int servers = 3;
        final int sources = 10;
        final int nThreads = 5;
        final int commandsPerSource = 200;
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
        final ElaraRunner[] runners = startServers(appStates, sourceIds, serverIds, serverTopology, sourceTopology, enforceLeaderInput);
        final ElaraRunner[] publishers = startSourcePublishers(commandsPerSource, sourceIds, sourceTopology);

        for (final ElaraRunner publisher : publishers) {
            publisher.join(10000);
        }
        Thread.sleep(12000);
        if (OS.WINDOWS.isCurrentOs()) {
            Thread.sleep(30000);
        }

        scheduledExecutorService.shutdown();
        if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
            scheduledExecutorService.shutdownNow();
        }
        for (final ElaraRunner runner : runners) {
            runner.close();
            runner.join(1000);
        }

        //then
        for (int i = 0; i < servers; i++) {
            final State state = appStates[i];
            System.out.println("server-" + serverIds.idByIndex(i) + ": count=" + state.count() + ", hash=" + state.hash());
        }
        final State refState = appStates[0];
        assertNotEquals(0, refState.count(), "refState.count");
        for (int i = 1; i < servers; i++) {
            final State state = appStates[i];
            if (networkConfig.isReliable()) {
                assertEquals(sources * commandsPerSource, state.count(), "appState[" + i + "].count");
            }
            assertEquals(refState.count(), state.count(), "appState[" + i + "].count");
            assertEquals(refState.hash(), state.hash(), "appState[" + i + "].hash");
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
        final long[] seqPtr = {System.currentTimeMillis()};
        final int nextLeader = serverIds.idByIndex(ThreadLocalRandom.current().nextInt(serverIds.count()));
        return receiver -> {
            if (receiver.leaderId() == nextLeader || receiver.serverId() != nextLeader) {
                return 0;
            }
            receiver.enforceLeader(ENFORCE_LEADER_SOURCE, seqPtr[0]++, nextLeader);
            return 1;
        };
    }

    private ElaraRunner[] startSourcePublishers(final int commandsPerSource,
                                               final IdMapping sourceIds,
                                               final ServerTopology sourceTopology) {
        final int sources = sourceTopology.senders();
        final ElaraRunner[] publishers = new ElaraRunner[sources];
        for (int i = 0; i < sources; i++) {
            final int source = SOURCE_OFFSET + i;
            publishers[i] = MulticastSource.startRandom(source, sourceIds, commandsPerSource, sourceTopology);
        }
        return publishers;
    }

    private ServerTopology serverTopology(final IdMapping serverIds, final Transmitter transmitter) {
        final LinkConfig apdCfg = networkConfig.appendLink();
        final int servers = serverIds.count();
        final Buffer[] sendBuffers = buffers(servers, apdCfg.senderBufferCapacity(), apdCfg.initialMessageCapacity());
        final Buffer[][] receiveBuffers = new Buffer[servers][];
        for (int i = 0; i < servers; i++) {
            receiveBuffers[i] = buffers(servers, apdCfg.receiverBufferCapacity(), apdCfg.initialMessageCapacity());
        }
        return new DefaultServerTopology(sendBuffers, receiveBuffers, transmitter);
    }

    private ServerTopology sourceTopology(final int sources,
                                          final IdMapping serverIds,
                                          final Transmitter transmitter) {
        final LinkConfig cmdCfg = networkConfig.commandLink();
        final Buffer[] sendBuffers = buffers(sources, cmdCfg.senderBufferCapacity(), cmdCfg.initialMessageCapacity());
        final Buffer[][] receiveBuffers = new Buffer[serverIds.count()][];
        for (int i = 0; i < receiveBuffers.length; i++) {
            receiveBuffers[i] = buffers(sources, cmdCfg.receiverBufferCapacity(), cmdCfg.initialMessageCapacity());
        }
        return new DefaultServerTopology(sendBuffers, receiveBuffers, transmitter);
    }

    private Buffer[] buffers(final int n, final int bufferCapacity, final int initialMessageCapacity) {
        final Buffer[] buffers = new Buffer[n];
        for (int i = 0; i < n; i++) {
            buffers[i] = new RingBuffer(bufferCapacity, initialMessageCapacity);
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
                                       final IdMapping serverIds,
                                       final ServerTopology serverTopology,
                                       final ServerTopology sourceTopology,
                                       final EnforceLeaderInput enforceLeaderInput) {
        final int servers = appStates.length;
        final ElaraRunner[] runners = new ElaraRunner[servers];
        for (int server = 0; server < servers; server++) {
            final Input[] inputs = inputs(sourceIds, sourceTopology.receiveBuffers(server));
            final ReplicationPlugin replicationPlugin = replicationPlugin(server, serverIds, serverTopology, enforceLeaderInput);
            runners[server] = startServer(server, serverIds, appStates[server], inputs, replicationPlugin);
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
        final int curServerId = serverIds.idByIndex(server);
        final Connection connection = connection(curServerId, serverIds, serverTopology);
        final org.tools4j.elara.plugin.replication.Context context = ReplicationPlugin.configure();
        for (int i = 0; i < serverIds.count(); i++) {
            final int serverId = serverIds.idByIndex(i);
            context.serverId(serverId, i == server)
                    .connection(serverId, connection);
        }
        return context.enforceLeaderInput(enforceLeaderInput);
    }

    private Connection connection(final int serverId,
                                  final IdMapping serverIds,
                                  final ServerTopology serverTopology) {
        return Connection.create(
                new ReceiveBufferPoller(serverId, serverIds, serverTopology, networkConfig.appendLink().receiverBufferCapacity()),
                new ServerTopologyPublisher(serverId, serverIds, serverTopology)
        );
    }

    private ElaraRunner startServer(final int server,
                                    final IdMapping serverIds,
                                    final ModifiableState appState,
                                    final Input[] inputs,
                                    final ReplicationPlugin replicationPlugin) {
        final int serverId = serverIds.idByIndex(server);
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + serverId + "-cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/replication/server-" + serverId + "-evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(AllInOneAppConfig.configure()
                .commandProcessor(commandProcessor(serverId, appState))
                .eventApplier(eventApplier(serverId, appState))
                .inputs(inputs)
                .commandStore(new ChronicleMessageStore(cq))
                .eventStore(new ChronicleMessageStore(eq))
                .duplicateHandler(DuplicateHandler.NOOP)
                .loggerFactory(clazz -> new OutputStreamLogger(Level.DEBUG))
                .plugin(replicationPlugin)
                .populateDefaults()
        );
    }

    private static CommandProcessor commandProcessor(final int serverId, final State appState) {
        final CommandProcessor p = new HashApplication((ModifiableState)appState);
        return p;
//        return (command, router) -> {
//            System.out.println(Instant.now() + " server-" + serverId + ": " + command);
//            p.onCommand(command, router);
//        };
    }

    private static EventApplier eventApplier(final int serverId, final ModifiableState appState) {
        final EventApplier a = new HashApplication(appState);
        return a;
//        return event -> {
//            System.out.println(Instant.now() + " server-" + serverId + ": " + event);
//            a.onEvent(event);
//        };
    }
}