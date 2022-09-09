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
package org.tools4j.elara.stream.tcp;

import org.tools4j.elara.logging.Logger;

public interface TcpConfiguration {
    Logger.Factory loggerFactory();

    interface ClientConfiguration extends TcpConfiguration {
        /**
         * Returns the timeout in milliseconds after which new reconnect attempts are made;
         * returning zero or a negative value disables reconnect attempts altogether.
         *
         * @return reconnect timeout in milliseconds, zero or negative to not attempt at all
         */
        long reconnectTimeoutMillis();
        ConnectListener connectListener();
    }

    interface ServerConfiguration extends TcpConfiguration {
        int acceptConnectionsMax();
        DisconnectPolicy disconnectPolicy();
        SendingStrategy sendingStrategy();
        AcceptListener acceptListener();

        enum DisconnectPolicy {
            DISCONNECT_NEWEST,
            DISCONNECT_OLDEST
        }

        enum SendingStrategy {
            MULTICAST,
            SINGLE_NEWEST,
            SINGLE_ROUND_ROBIN
        }
    }

}
