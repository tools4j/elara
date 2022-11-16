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
import org.agrona.collections.LongArrayList;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.stream.ipc.AllocationStrategy;
import org.tools4j.elara.stream.ipc.Ipc;
import org.tools4j.elara.stream.ipc.IpcConfiguration;
import org.tools4j.elara.stream.tcp.Tcp;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

import static java.lang.Long.toHexString;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tools4j.elara.stream.ipc.Cardinality.ONE;

public class MessageStreamTest {

    private static final long MESSAGE_COUNT = 1_000;
    private static final int MESSAGE_BYTES = 100;

    @ParameterizedTest(name = "sendAndReceiveMessages: {0} --> {1}")
    @MethodSource("sendersAndReceivers")
    protected void sendAndReceiveMessages(final MessageSender sender, final MessageReceiver receiver) throws Exception {
        //given
        final LongArrayList senderResults = new LongArrayList();
        final LongArrayList receiverResults = new LongArrayList();
        final Thread senderThread = new Thread(null, senderLoop(sender, senderResults), "sender");
        final Thread receiverThread = new Thread(null, receiverLoop(receiver, receiverResults), "receiver");

        //when
        receiverThread.start();
        senderThread.start();

        senderThread.join();
        receiverThread.join();

        sender.close();
        receiver.close();

        //then
        assertEquals(MESSAGE_COUNT, senderResults.getLong(0), "message count [" + sender + "]");
        assertEquals(MESSAGE_COUNT, receiverResults.getLong(0), "message count [" + receiver + "]");
        assertEquals(MESSAGE_BYTES, senderResults.getLong(1), "bytes per message [" + sender + "]");
        assertEquals(MESSAGE_BYTES, receiverResults.getLong(1), "bytes per message [" + receiver + "]");
        assertEquals(toHexString(senderResults.getLong(2)), toHexString(receiverResults.getLong(2)), "sender/receiver message hash");
    }

    private static long hash(final long hash, final long extra) {
        return 31 * hash + extra;
    }

    private static Runnable senderLoop(final MessageSender sender, final LongArrayList results) {
        requireNonNull(sender);
        return () -> {
            final MutableDirectBuffer message = new UnsafeBuffer(new byte[MESSAGE_BYTES]);
            long hash = 0;
            int retries = 0;
            long timeStart = 0;
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                long mhash = 0;
                for (int j = 0; j < MESSAGE_BYTES; j++) {
                    final byte val = (byte)(i + j);
                    message.putByte(j, val);
                    mhash = hash(mhash, val);
                }
                hash = hash(hash, mhash);
                long time = i == 0 ? System.nanoTime() : 0;
                while (sender.sendMessage(message, 0, MESSAGE_BYTES) != SendingResult.SENT) {
                    retries++;
                    time = i == 0 ? System.nanoTime() : 0;
                }
                if (i == 0) {
                    timeStart = time;
                }
            }
            final long timeEnd = System.nanoTime();
            System.out.println(sender + " sent: " + MESSAGE_COUNT + " [" + MESSAGE_BYTES + " bytes each" +
                    ", retries=" + retries + ", time=" + (timeEnd - timeStart) / 1e9f + "s, hash=" + toHexString(hash) + "]");
            results.addLong(MESSAGE_COUNT);
            results.addLong(MESSAGE_BYTES);
            results.addLong(hash);
        };
    }

    private static Runnable receiverLoop(final MessageReceiver receiver, final LongArrayList results) {
        requireNonNull(receiver);
        return () -> {
            final long[] bytesPtr = {0};
            final int[] receivedPtr = {0};
            final long[] hashPtr = {0};
            final long[] times = {0, 0};
            final MessageReceiver.Handler handler = message -> {
                long hash = 0;
                for (int i = 0; i < message.capacity(); i++) {
                    hash = hash(hash, message.getByte(i));
                    bytesPtr[0]++;
                }
                receivedPtr[0]++;
                hashPtr[0] = hash(hashPtr[0], hash);
//                System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) + " bytes each, " + bytesPtr[0] + " bytes total]");
            };
            int count = 0;
            while (bytesPtr[0] < MESSAGE_COUNT * MESSAGE_BYTES) {
                final long time = count == 0 ? System.nanoTime() : 0;
                final int polled = receiver.poll(handler);
                if (polled > 0) {
                    if (count == 0) {
                        times[0] = time;
                    }
                    count += polled;
                }
            }
            times[1] = System.nanoTime();
//            assert count == receivedPtr[0];//does not hold for TCP due to connect which also returns polled > 0
            System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) +
                    " bytes each, " + bytesPtr[0] + " bytes total, time=" + (times[1] - times[0]) / 1e9f +
                    "s, hash=" + toHexString(hashPtr[0]) + ")");
            results.addLong(receivedPtr[0]);
            results.addLong(bytesPtr[0] / receivedPtr[0]);
            results.addLong(hashPtr[0]);
        };
    }

    static Arguments[] sendersAndReceivers() {
        return new Arguments[]{
                tcpServerSenderAndClientReceiver(),
                tcpClientSenderAndServerReceiver(),
                ipcBufferedSenderToReceiverFile(),
                ipcBufferedSenderFileToReceiver(),
                ipcDirectSenderToReceiverFile(),
                ipcDirectSenderFileToReceiver(),
        };
    }

    private static Arguments ipcBufferedSenderToReceiverFile() {
        return ipcSenderToReceiverFile(AllocationStrategy.DYNAMIC);
    }

    private static Arguments ipcDirectSenderToReceiverFile() {
        return ipcSenderToReceiverFile(AllocationStrategy.FIXED);
    }

    private static Arguments ipcSenderToReceiverFile(final AllocationStrategy allocationStrategy) {
        final File file = new File("build/stream/ipc-receiver-" + allocationStrategy + ".map");
        final int length = 1 << 24;
        final IpcConfiguration config = IpcConfiguration.configure()
                .senderCardinality(ONE)
                .senderInitialBufferSize(2 * MESSAGE_BYTES)
                .senderAllocationStrategy(allocationStrategy)
                .maxMessagesReceivedPerPoll(2)
                .newFileCreateParentDirs(true)
                .newFileDeleteIfPresent(true);
        return Arguments.of(
                Ipc.retryOpenSender(file, config),
                Ipc.newReceiver(file, length, config)
        );
    }

    private static Arguments ipcBufferedSenderFileToReceiver() {
        return ipcSenderFileToReceiver(AllocationStrategy.DYNAMIC);
    }

    private static Arguments ipcDirectSenderFileToReceiver() {
        return ipcSenderFileToReceiver(AllocationStrategy.FIXED);
    }

    private static Arguments ipcSenderFileToReceiver(final AllocationStrategy allocationStrategy) {
        final File file = new File("build/stream/ipc-sender-" + allocationStrategy + ".map");
        final int length = 1 << 14;
        final IpcConfiguration config = IpcConfiguration.configure()
                .senderCardinality(ONE)
                .senderInitialBufferSize(2 * MESSAGE_BYTES)
                .senderAllocationStrategy(allocationStrategy)
                .maxMessagesReceivedPerPoll(2)
                .newFileCreateParentDirs(true)
                .newFileDeleteIfPresent(true);
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

    public static int nextFreePort() {
        try (final ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);
        }
    }
}