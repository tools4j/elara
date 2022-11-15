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
package org.tools4j.elara.aeron;

import io.aeron.Aeron.Context;
import io.aeron.driver.MediaDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.MessageStreamTest;

class AeronMessageStreamTest extends MessageStreamTest {

    private static MediaDriver mediaDriver;
    private static Aeron aeron;

    @BeforeAll
    static void startAeron() {
        mediaDriver = MediaDriver.launchEmbedded();
        aeron = Aeron.connect(new Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));
    }

    @AfterAll
    static void stopAeron() {
        if (aeron != null) {
            aeron.close();
            aeron = null;
        }
        if (mediaDriver != null) {
            mediaDriver.close();
            mediaDriver = null;
        }
    }

    @ParameterizedTest(name = "sendAndReceiveMessages: {0} --> {1}")
    @MethodSource("aeronSendersAndReceivers")
    @Override
    protected void sendAndReceiveMessages(final MessageSender sender, final MessageReceiver receiver) throws Exception {
        super.sendAndReceiveMessages(sender, receiver);
    }

    static Arguments[] aeronSendersAndReceivers() {
        return new Arguments[]{
                aeronIpcSenderAndReceiver(),
                aeronUdpSenderAndReceiver()
        };
    }

    private static Arguments aeronIpcSenderAndReceiver() {
        final int streamId = 123;
        return Arguments.of(
                aeron.openExclusiveSender("aeron:ipc", streamId),
                aeron.openReceiver("aeron:ipc", streamId, false)
        );
    }

    private static Arguments aeronUdpSenderAndReceiver() {
        final int port = nextFreePort();
        final int streamId = 456;
        final String url = "aeron:udp?endpoint=localhost:" + port;
        return Arguments.of(
                aeron.openSender(url, streamId),
                aeron.openReceiver(url, streamId, false)
        );
    }
}