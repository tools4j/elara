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
package org.tools4j.elara.websocket;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class WebsocketClientReceiver implements MessageReceiver {

    private final WebSocketContainer webSocketContainer;
    private final ClientEndpointConfig config;
    private final URI path;
    private final Endpoint endpoint = new ClientEndpoint();
    private final Queue<byte[]> messages = new ArrayDeque<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final DirectBuffer buffer = new UnsafeBuffer(0, 0);
    private Session session;
    private boolean error;

    public WebsocketClientReceiver(final WebSocketContainer webSocketContainer,
                                   final ClientEndpointConfig config,
                                   final URI path) {
        this.webSocketContainer = requireNonNull(webSocketContainer);
        this.config = requireNonNull(config);
        this.path = requireNonNull(path);
        reconnect();
    }

    private void reconnect() {
        try {
            webSocketContainer.connectToServer(endpoint, config, path);
        } catch (final Exception e) {
            //TODO log
            session = null;
        }
    }

    @Override
    public int poll(final Handler handler) {
        if (session == null) {
            reconnect();
        }
        final byte[] message;
        synchronized (this) {
            message = messages.poll();
        }
        if (message != null) {
            handle(message, handler);
            return 1;
        }
        return 0;
    }

    private void handle(final byte[] message, final Handler handler) {
        try {
            buffer.wrap(message);
            handler.onMessage(buffer);
        } finally {
            buffer.wrap(0, 0);
        }
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        closed.set(true);
    }

    private class ClientEndpoint extends Endpoint implements MessageHandler.Whole<byte[]> {
        @Override
        public void onOpen(final Session session, final EndpointConfig config) {
            synchronized (WebsocketClientReceiver.this) {
                //TODO log
                session.addMessageHandler(this);
                WebsocketClientReceiver.this.session = requireNonNull(session);
            }
        }

        @Override
        public void onClose(final Session session, final CloseReason closeReason) {
            synchronized (WebsocketClientReceiver.this) {
                //TODO log
                WebsocketClientReceiver.this.session = null;
                session.removeMessageHandler(this);
            }
        }

        @Override
        public void onError(final Session session, final Throwable thr) {
            synchronized (WebsocketClientReceiver.this) {
                //TODO log
                WebsocketClientReceiver.this.error = true;
                WebsocketClientReceiver.this.session = null;
                try {
                    session.removeMessageHandler(this);
                    session.close();
                } catch (final IOException e) {
                    //ignore
                }
            }
        }

        @Override
        public void onMessage(final byte[] message) {
            synchronized (WebsocketClientReceiver.this) {
                messages.add(message);
            }
        }
    }
}
