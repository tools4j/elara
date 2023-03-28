/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.stream.ipc.AllocationStrategy;
import org.tools4j.elara.stream.ipc.Ipc;
import org.tools4j.elara.stream.ipc.IpcConfiguration;
import org.tools4j.elara.stream.tcp.Tcp;
import org.tools4j.elara.stream.tcp.config.TcpConfiguration;
import org.tools4j.elara.stream.tcp.config.TcpContext;
import org.tools4j.elara.stream.udp.Udp;
import org.tools4j.elara.stream.udp.config.UdpConfiguration;
import org.tools4j.elara.stream.udp.config.UdpContext;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.tools4j.elara.stream.Network.hostAddress;
import static org.tools4j.elara.stream.Network.nextFreePort;
import static org.tools4j.elara.stream.ipc.Cardinality.ONE;

class MessageStreamTest {

    private static final long MESSAGE_COUNT = 1_000_000;
    private static final int MESSAGE_BYTES = 100;
    //private static final int MESSAGES_PER_SECOND = 200_000;
    private static final int MESSAGES_PER_SECOND = 0;
    private static final long MAX_WAIT_MILLIS = 10_000;

    @ParameterizedTest(name = "sendAndReceiveMessages: {0} --> {1}")
    @MethodSource("sendersAndReceivers")
    void sendAndReceiveMessages(final MessageSender sender, final MessageReceiver receiver) throws Exception {
        new MessageStreamRunner(MESSAGE_COUNT, MESSAGE_BYTES, MESSAGES_PER_SECOND)
                .sendAndReceiveMessages(sender, receiver, MAX_WAIT_MILLIS);
    }

    static Arguments[] sendersAndReceivers() {
        return new Arguments[]{
                tcpServerSenderAndClientReceiver(),
                tcpClientSenderAndServerReceiver(),
//                udpServerSenderAndClientReceiver(),
//                udpClientSenderAndServerReceiver(),
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
        final int length = 1 << 20;
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
        final int length = 1 << 20;
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
        //final SocketAddress address = new InetSocketAddress("localhost", nextFreePort());
        final SocketAddress address = new InetSocketAddress(hostAddress(), nextFreePort());
        final TcpContext config = TcpConfiguration.configure()
                .bufferCapacity(Math.max(1<<14, MESSAGE_BYTES << 1))
                .populateDefaults();
        return Arguments.of(
                Tcp.connect(address, config).sender(),
                Tcp.bind(address, config).receiver()
        );
    }

    private static Arguments tcpServerSenderAndClientReceiver() {
        final SocketAddress address = new InetSocketAddress("localhost", nextFreePort());
//        final SocketAddress address = new InetSocketAddress(hostAddress(), nextFreePort());
        final TcpContext config = TcpConfiguration.configure()
                .bufferCapacity(Math.max(1<<14, MESSAGE_BYTES << 1))
                .populateDefaults();
        return Arguments.of(
                Tcp.bind(address, config).sender(),
                Tcp.connect(address, config).receiver()
        );
    }

    private static Arguments udpClientSenderAndServerReceiver() {
        final SocketAddress address = new InetSocketAddress(hostAddress(), nextFreePort());
        final UdpContext config = UdpConfiguration.configure()
                .bufferCapacity(Math.max(1<<14, MESSAGE_BYTES << 1))
                .populateDefaults();
        return Arguments.of(
                Udp.connect(address, config).sender(),
                Udp.bind(address, config).receiver()
        );
    }

    private static Arguments udpServerSenderAndClientReceiver() {
        final SocketAddress address = new InetSocketAddress(hostAddress(), nextFreePort());
        final UdpContext config = UdpConfiguration.configure()
                .bufferCapacity(Math.max(1<<14, MESSAGE_BYTES << 1))
                .populateDefaults();
        return Arguments.of(
                Udp.bind(address, config).sender(),
                Udp.connect(address, config).receiver()
        );
    }
}