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
package org.tools4j.elara.plugin.replicate;

import java.util.Arrays;

public class DefaultServerConfig implements ServerConfig {

    private final int serverId;
    private final int[] serverIds;

    public DefaultServerConfig(final int serverId, final int... serverIds) {
        validateServerId(serverId, serverIds);
        this.serverId = serverId;
        this.serverIds = serverIds;
    }

    private static void validateServerId(final int serverId, final int... serverIds) {
        for (final int id : serverIds) {
            if (id == serverId) {
                return;
            }
        }
        throw new IllegalArgumentException("Server ID " + serverId + " is not in " + Arrays.toString(serverIds));
    }

    @Override
    public int serverId() {
        return serverId;
    }

    @Override
    public int serverCount() {
        return serverIds.length;
    }

    @Override
    public int serverId(final int index) {
        return serverIds[index];
    }

    @Override
    public String toString() {
        return "DefaultServerConfig{" +
                "serverId=" + serverId +
                ", serverIds=" + Arrays.toString(serverIds) +
                '}';
    }
}
