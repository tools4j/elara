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
package org.tools4j.elara.stream.tcp.impl;


import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RingBufferTest {

    private static final boolean DEBUG_OUT = false;
    private static final int BUFFER_CAPACITY = 2 * RingBufferImpl.ALIGNMENT;

    //under test
    private RingBuffer ringBuffer;

    @BeforeEach
    void beforeEach() {
        ringBuffer = new RingBufferImpl(2 * RingBufferImpl.ALIGNMENT);
    }

    @Test
    void readAndWrite() {
        final String msg1 = "hello world";
        final String msg2 = "say hello to the world!";
        final DirectBuffer bytes1 = new UnsafeBuffer(msg1.getBytes());
        final DirectBuffer bytes2 = new UnsafeBuffer(msg2.getBytes());
        final DirectBuffer buffer = new UnsafeBuffer(0, 0);
        boolean success;

        //WHEN: write msg1
        success = ringBuffer.writeUnsignedInt(bytes1.capacity())
                && ringBuffer.write(bytes1, 0, bytes1.capacity());

        //then
        assertTrue(success);
        assertEquals(msg1.length() + 4, ringBuffer.readLength());

        //WHEN: write msg2
        success = ringBuffer.writeUnsignedInt(bytes2.capacity()) &&
                ringBuffer.write(bytes2, 0, bytes2.capacity());

        //then
        assertTrue(success);
        assertEquals(msg1.length() + 4 + msg2.length() + 4, ringBuffer.readLength());

        //WHEN: prepare reading msg1
        final int len1 = (int)ringBuffer.readUnsignedInt(true);
        success = ringBuffer.readWrap(buffer, len1);

        //then
        assertEquals(msg1.length(), len1);
        assertTrue(success);
        assertEquals(msg1.length(), buffer.capacity());

        //WHEN: read msg1
        final String read1 = buffer.getStringWithoutLengthAscii(0, buffer.capacity());
        ringBuffer.readCommit(len1);

        //then
        assertEquals(msg1, read1);
        assertEquals(msg2.length() + 4, ringBuffer.readLength());

        //WHEN: prepare reading msg2
        final int len2 = (int)ringBuffer.readUnsignedInt(true);
        success = ringBuffer.readWrap(buffer, len2);

        //then
        assertEquals(msg2.length(), len2);
        assertTrue(success);
        assertEquals(msg2.length(), buffer.capacity());

        //WHEN: read msg2
        final String read2 = buffer.getStringWithoutLengthAscii(0, len2);
        ringBuffer.readCommit(len2);

        //then
        assertEquals(msg2, read2);
        assertEquals(0, ringBuffer.readLength());
    }

    @ParameterizedTest
    @MethodSource("readWriteWrapAround_messages")
    void readWriteWrapAround(final String msg) {
        final int times = 13 * BUFFER_CAPACITY;
        final DirectBuffer bytes = new UnsafeBuffer(msg.getBytes());
        final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);
        final int bytesPerMessage = bytes.capacity() + Integer.BYTES;

        //WHEN: fill buffer
        int written = 0;
        while (ringBuffer.writeLength() >= bytesPerMessage) {
            assertTrue(ringBuffer.writeUnsignedInt(bytes.capacity()));
            assertTrue(ringBuffer.write(bytes, 0, bytes.capacity()));
            log("[W:%s]:\t%s\n", written, ringBuffer);
            written++;
        }

        //THEN
        assertEquals(written * bytesPerMessage, ringBuffer.readLength());

        //WHEN: read one, write one
        int read = 0;
        for (int i = 0; i < times; i++) {
            //WHEN: read one
            final int len = skipReadUnsignedInt();
            assertTrue(skipReadWrap(buffer, len), "skipReadWrap[" + i + "]");

            //THEN
            assertEquals(bytes.capacity(), len, "len[" + i + "]");
            assertEquals(msg, buffer.getStringWithoutLengthAscii(0, len), "msg[" + i + "]");
            ringBuffer.readCommit(len);
            log("[R:%s]:\t%s\n", read, ringBuffer);
            read++;

            //WHEN + THEN: write one
            assertTrue(skipWriteUnsignedInt(bytes.capacity()), "skipWriteUnsignedInt[" + i + "]");
            assertTrue(skipWriteWrap(buffer, bytes.capacity()), "skipWriteWrap[" + i + "]");
            buffer.putBytes(0, bytes, 0, buffer.capacity());
            ringBuffer.writeCommit(buffer.capacity());
            log("[W:%s]:\t%s\n", written, ringBuffer);
            written++;
        }

        //WHEN: read rest
        while (read < written) {
            //WHEN: read one
            final int len = skipReadUnsignedInt();
            skipReadWrap(buffer, len);

            //THEN
            assertEquals(bytes.capacity(), len);
            assertEquals(msg, buffer.getStringWithoutLengthAscii(0, len));
            ringBuffer.readCommit(len);
            read++;
        }

        //THEN: buffer at full capacity
        assertEquals(0, ringBuffer.readLength());
        assertEquals(ringBuffer.capacity(), ringBuffer.writeLength());
        assertEquals(0, ringBuffer.readOffset());
        assertEquals(0, ringBuffer.writeOffset());
    }

    private static Stream<Arguments> readWriteWrapAround_messages() {
        final String loremIpsum = "But I must explain to you how all this mistaken idea of denouncing of a pleasure and " +
                "praising pain was born and I will give you a complete account of the system, and expound the actual " +
                "teachings of the great explorer of the truth, the master-builder of human happiness. No one rejects, " +
                "dislikes, or avoids pleasure itself, because it is pleasure, but because those who do not know how to " +
                "pursue pleasure rationally encounter consequences that are extremely painful. Nor again is there anyone " +
                "who loves or pursues or desires to obtain pain of itself, because it is pain, but occasionally " +
                "circumstances occur in which toil and pain can procure him some great pleasure. To take a trivial example, " +
                "which of us ever undertakes laborious physical exercise, except to obtain some advantage from it? But " +
                "who has any right to find fault with a man who chooses to enjoy a pleasure that has no annoying " +
                "consequences, or one who avoids a pain that produces no resultant pleasure?";
        final int maxLen = BUFFER_CAPACITY - Integer.BYTES;
        final String[] messages = new String[maxLen];
        for (int i = 0; i < maxLen; i++) {
            messages[i] = loremIpsum.substring(0, i) + "!";
        }
        return Arrays.stream(messages).map(Arguments::of);
    }

    private int skipReadUnsignedInt() {
        ringBuffer.readSkipEndGap(Integer.BYTES);
        return (int)ringBuffer.readUnsignedInt(true);
    }

    private boolean skipWriteUnsignedInt(final int value) {
        ringBuffer.writeSkipEndGap(Integer.BYTES);
        return ringBuffer.writeUnsignedInt(value);
    }

    private boolean skipReadWrap(final DirectBuffer buffer, final int length) {
        ringBuffer.readSkipEndGap(length);
        return ringBuffer.readWrap(buffer, length);
    }

    private boolean skipWriteWrap(final MutableDirectBuffer buffer, final int length) {
        ringBuffer.writeSkipEndGap(length);
        return ringBuffer.writeWrap(buffer, length);
    }

    private static void log(final String format, final Object... args) {
        if (DEBUG_OUT) {
            System.out.printf(format, args);
        }
    }
}
