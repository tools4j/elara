/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.DirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.tools4j.elara.plugin.replication.Connection.Publisher;

public class DispatchingPublisher implements Publisher {

    private final Int2ObjectHashMap<Publisher> publisherByServerId = new Int2ObjectHashMap<>();

    public DispatchingPublisher(final Configuration configuration) {
        final int currentServerId = configuration.serverId();
        for (final int serverId : configuration.serverIds()) {
            if (serverId != currentServerId) {
                final Publisher publisher = configuration.connection(serverId).publisher();
                publisherByServerId.put(serverId, publisher);
            }
        }
    }

    @Override
    public boolean publish(final int targetServerId, final DirectBuffer buffer, final int offset, final int length) {
        final Publisher publisher = publisherByServerId.get(targetServerId);
        if (publisher != null) {
            return publisher.publish(targetServerId, buffer, offset, length);
        }
        return false;
    }
}
