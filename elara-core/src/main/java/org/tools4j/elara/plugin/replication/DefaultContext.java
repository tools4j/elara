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
package org.tools4j.elara.plugin.replication;

import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntArrayList;
import org.agrona.collections.IntHashSet;
import org.tools4j.elara.input.Input;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class DefaultContext implements Context {
    public static final int DEFAULT_INITIAL_SEND_BUFFER_CAPACITY = 1024;
    private static final EnforceLeaderInput NULL_INPUT = () -> receiver -> 0;

    private int serverId = NULL_SERVER;
    private final IntArrayList serverIds = new IntArrayList();
    private Input enforceLeaderInput = NULL_INPUT;
    private Connection connection = null;
    private final Int2ObjectHashMap<Connection> connectionByServerId = new Int2ObjectHashMap<>();
    private ReplicationLogger replicationLogger = ReplicationLogger.DEFAULT;
    private int initialSendBufferCapacity = DEFAULT_INITIAL_SEND_BUFFER_CAPACITY;

    @Override
    public int serverId() {
        return serverId;
    }

    @Override
    public int[] serverIds() {
        return serverIds.toIntArray();
    }

    @Override
    public Context serverId(final short serverId, final boolean local) {
        this.serverIds.addInt(serverId);
        if (local) {
            this.serverId = serverId;
        } else {
            if (connection != null && !connectionByServerId.containsKey(serverId)) {
                connectionByServerId.put(serverId, connection);
            }
        }
        return this;
    }

    @Override
    public Input enforceLeaderInput() {
        return enforceLeaderInput;
    }

    @Override
    public Context enforceLeaderInput(final Input input) {
        this.enforceLeaderInput = requireNonNull(input);
        return this;
    }

    @Override
    public Context enforceLeaderInput(final EnforceLeaderInput input) {
        this.enforceLeaderInput = requireNonNull(input);
        return this;
    }

    @Override
    public Connection connection(final int serverId) {
        return connectionByServerId.get(serverId);
    }

    @Override
    public Context connection(final Connection connection) {
        final Connection old = this.connection;
        this.connection = requireNonNull(connection);
        for (int i = 0; i < serverIds.size(); i++) {
            final int serverId = serverIds.get(i);
            final Connection c = connectionByServerId.get(serverId);
            if (c == null || c == old) {
                connectionByServerId.put(serverId, connection);
            }
        }
        return this;
    }

    @Override
    public Context connection(final short serverId, final Connection connection) {
        connectionByServerId.put(serverId, connection);
        return this;
    }

    @Override
    public ReplicationLogger replicationLogger() {
        return replicationLogger;
    }

    @Override
    public Context replicationLogger(final ReplicationLogger logger) {
        this.replicationLogger = requireNonNull(logger);
        return this;
    }

    @Override
    public int initialSendBufferCapacity() {
        return initialSendBufferCapacity;
    }

    @Override
    public Context initialSendBufferCapacity(final int size) {
        this.initialSendBufferCapacity = size;
        return this;
    }

    static Configuration validate(final Configuration configuration) {
        final int localServerId = configuration.serverId();
        final IntHashSet serverIds = new IntHashSet(NULL_SERVER);
        for (final int serverId : configuration.serverIds()) {
            if (serverId == NULL_SERVER) {
                throw new IllegalArgumentException("Invalid server ID: " + serverId);
            }
            if (!serverIds.add(serverId)) {
                throw new IllegalArgumentException("Duplicate server ID: " + serverId);
            }
            if (serverId != localServerId && configuration.connection(serverId) == null) {
                throw new IllegalArgumentException("No connection defined for server ID: " + serverId);
            }
        }
        if (!serverIds.contains(localServerId)) {
            throw new IllegalArgumentException("Server ID " + localServerId + " must be one of the server IDs " + serverIds);
        }
        return configuration;
    }
}
