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
package org.tools4j.elara.plugin.replication;

import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntArrayList;
import org.agrona.collections.IntHashSet;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class DefaultContext implements Context {
    public static final int DEFAULT_INITIAL_SEND_BUFFER_CAPACITY = 1024;
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 10000;//10s if millis
    public static final long DEFAULT_LEADER_TIMEOUT = 20000;//20s if millis
    public static final long DEFAULT_SERVER_REPLAY_TIMEOUT = 20000;//20s if millis
    private static final EnforceLeaderInput NULL_INPUT = receiver -> 0;

    private int serverId = NULL_SERVER;
    private final IntArrayList serverIds = new IntArrayList();
    private EnforceLeaderInput enforceLeaderInput = NULL_INPUT;
    private final Int2ObjectHashMap<Connection> connectionByServerId = new Int2ObjectHashMap<>();
    private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    private long leaderTimeout = DEFAULT_LEADER_TIMEOUT;
    private long serverReplayTimeout = DEFAULT_SERVER_REPLAY_TIMEOUT;
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
    public Context serverId(final int serverId) {
        return serverId(serverId, true);
    }

    @Override
    public Context serverId(final int serverId, final boolean local) {
        this.serverIds.addInt(serverId);
        if (local) {
            this.serverId = serverId;
        }
        return this;
    }

    @Override
    public Context serverIds(final int... serverIds) {
        for (final int serverId : serverIds) {
            serverId(serverId, false);
        }
        return this;
    }

    @Override
    public EnforceLeaderInput enforceLeaderInput() {
        return enforceLeaderInput;
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
    public Context connection(final int serverId, final Connection connection) {
        connectionByServerId.put(serverId, connection);
        return this;
    }

    @Override
    public long heartbeatInterval() {
        return heartbeatInterval;
    }

    @Override
    public Context heartbeatInterval(final long interval) {
        if (interval < 0) {
            throw new IllegalArgumentException("Heartbeat interval cannot be negative: " + interval);
        }
        this.heartbeatInterval = interval;
        return this;
    }

    @Override
    public long leaderTimeout() {
        return leaderTimeout;
    }

    @Override
    public Context leaderTimeout(final long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Leader timeout cannot be negative: " + timeout);
        }
        this.leaderTimeout = timeout;
        return this;
    }

    @Override
    public long serverReplyTimeout() {
        return serverReplayTimeout;
    }

    @Override
    public Context serverReplyTimeout(final long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Server replay timeout cannot be negative: " + timeout);
        }
        this.serverReplayTimeout = timeout;
        return this;
    }

    @Override
    public int initialSendBufferCapacity() {
        return initialSendBufferCapacity;
    }

    @Override
    public Context initialSendBufferCapacity(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Initial buffer capacity cannot be negative: " + capacity);
        }
        this.initialSendBufferCapacity = capacity;
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
