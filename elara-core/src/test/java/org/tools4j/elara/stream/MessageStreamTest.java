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
package org.tools4j.elara.stream;

import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.nio.TransportPoller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.ipc.Ipc;
import org.tools4j.elara.stream.ipc.IpcConfiguration;
import org.tools4j.elara.stream.tcp.Tcp;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

class MessageStreamTest {

    private static final int MESSAGE_COUNT = 20;
    private static final int MESSAGE_BYTES = 100;

    private abstract static class Poller extends TransportPoller {
        int selectNow() {
            try {
                selector.selectNow();
                final int size = selectedKeySet.size();
                if (size == 0) {
                    return 0;
                }
                IOException exception = null;
                for (int i = 0; i < size; i++) {
                    try {
                        onSelectionKey(selectedKeySet.keys()[i]);
                    } catch (final IOException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            e.addSuppressed(e);
                        }
                    }
                }
                selectedKeySet.clear();
                if (exception != null) {
                    throw exception;
                }
                return size;
            } catch (final IOException e) {
                LangUtil.rethrowUnchecked(e);
            }
            return 0;
        }

        abstract protected void onSelectionKey(SelectionKey key) throws IOException;
    }

    @Test
    void selectorTest() throws Exception {
        final SocketAddress address = new InetSocketAddress("localhost", nextFreePort());

        final AtomicBoolean running = new AtomicBoolean(true);
        final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        final Poller serverPoller = new Poller() {
            {
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                serverSocketChannel.bind(address);
            }
            @Override
            protected void onSelectionKey(final SelectionKey key) throws IOException {
                if (key.isAcceptable()) {
                    final SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
                if (key.isReadable()) {
                    final byte[] bytes = new byte[128];
                    ((SocketChannel)key.channel()).read(ByteBuffer.wrap(bytes));
                    System.out.println("RECEIVED: " + new String(bytes).trim());
                    running.set(false);
                }
            }
        };

        final SocketChannel socketChannel = SocketChannel.open();
        final Poller clientPoller = new Poller() {
            {
                socketChannel.configureBlocking(false);
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                socketChannel.connect(address);
            }
            @Override
            protected void onSelectionKey(final SelectionKey key) throws IOException {
                if (key.isConnectable() && socketChannel.finishConnect()) {
                    socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
                if (key.isWritable()) {
                    socketChannel.write(ByteBuffer.wrap("Hello world!!".getBytes()));
                    System.out.println("SENT");
                }
            }
        };

        final Thread server = new Thread(null, () -> {
            while (running.get()) {
                serverPoller.selectNow();
            }
        }, "server");
        final Thread client = new Thread(null, () -> {
            while (running.get()) {
                clientPoller.selectNow();
            }
        }, "client");

        client.start();
        server.start();

        client.join();
        server.join();
    }

    @ParameterizedTest(name = "sendAndReceiveMessages: {0} --> {1}")
    @MethodSource("sendersAndReceivers")
    void sendAndReceiveMessages(final MessageSender sender, final MessageReceiver receiver) throws Exception {
        final Thread senderThread = new Thread(null, senderLoop(sender), "sender");
        final Thread receiverThread = new Thread(null, receiverLoop(receiver), "receiver");

        receiverThread.start();
        senderThread.start();

        senderThread.join();
        receiverThread.join();

        sender.close();
        receiver.close();
    }

    private static Runnable senderLoop(final MessageSender sender) {
        requireNonNull(sender);
        return () -> {
            final MutableDirectBuffer message = new UnsafeBuffer(new byte[MESSAGE_BYTES]);
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                for (int j = 0; j < MESSAGE_BYTES; j++) {
                    message.putByte(j, (byte) (i + j));
                }
                while (sender.sendMessage(message, 0, MESSAGE_BYTES) != SendingResult.SENT) {
                    //keep going
                }
            }
            System.out.println(sender + " sent: " + MESSAGE_COUNT + " [" + MESSAGE_BYTES + " bytes each]");
        };
    }

    private static Runnable receiverLoop(final MessageReceiver receiver) {
        requireNonNull(receiver);
        return () -> {
            final long[] bytesPtr = {0};
            final int[] receivedPtr = {0};
            final MessageReceiver.Handler handler = message -> {
                long hash = 0;
                for (int i = 0; i < message.capacity(); i++) {
                    hash = 31 * hash + message.getByte(i);
                    bytesPtr[0]++;
                }
                if (hash != Long.MAX_VALUE) {
                    receivedPtr[0]++;
                }
//                System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) + " bytes each, " + bytesPtr[0] + " bytes total]");
            };
            int count = 0;
            while (bytesPtr[0] < MESSAGE_COUNT * MESSAGE_BYTES) {
                final int polled = receiver.poll(handler);
                if (polled > 0) {
                }
                count += polled;
            }
//            assert count == receivedPtr[0];
            System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) + " bytes each, " + bytesPtr[0] + " bytes total]");
        };
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static Arguments[] sendersAndReceivers() {
        return new Arguments[]{
                tcpServerSenderAndClientReceiver(),
                tcpClientSenderAndServerReceiver(),
//                ipcSenderAndReceiver()
        };
    }

    private static Arguments ipcSenderAndReceiver() {
        final File file = new File("build/stream/ipc.map");
        final int length = 1 << 24;
        final IpcConfiguration config = IpcConfiguration.configure();

        return Arguments.of(
                Ipc.newSender(file, length, config),
                Ipc.retryOpenReceiver(file, config)
        );
    }

    private static Arguments tcpClientSenderAndServerReceiver() {
        final SocketAddress address = new InetSocketAddress("localhost", nextFreePort());
        return Arguments.of(
                Tcp.connect(address).sender(),
                Tcp.bind(address).receiver()
        );
    }

    private static Arguments tcpServerSenderAndClientReceiver() {
        final SocketAddress address = new InetSocketAddress("localhost", nextFreePort());
        return Arguments.of(
                Tcp.bind(address).sender(),
                Tcp.connect(address).receiver()
        );
    }

    private static int nextFreePort() {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}