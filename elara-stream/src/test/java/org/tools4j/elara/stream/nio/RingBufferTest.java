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
package org.tools4j.elara.stream.nio;


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
        final MutableDirectBuffer bytes1 = new UnsafeBuffer(new byte[msg1.length() + 4]);
        final MutableDirectBuffer bytes2 = new UnsafeBuffer(new byte[msg2.length() + 4]);
        final DirectBuffer buffer = new UnsafeBuffer(0, 0);
        final int len1 = bytes1.putStringAscii(0, msg1);
        final int len2 = bytes2.putStringAscii(0, msg2);
        boolean success;

        //WHEN: write msg1
        success = ringBuffer.write(bytes1, 0, bytes1.capacity());

        //then
        assertTrue(success);
        assertEquals(len1, ringBuffer.readLength());

        //WHEN: write msg2
        success = ringBuffer.write(bytes2, 0, bytes2.capacity());

        //then
        assertTrue(success);
        assertEquals(len1 + len2, ringBuffer.readLength());

        //WHEN: prepare reading msg1
        success = ringBuffer.readWrap(buffer);

        //then
        assertTrue(success);
        assertEquals(msg1.length(), buffer.getInt(0));
        assertTrue(buffer.capacity() >= len1);

        //WHEN: read msg1
        final String read1 = buffer.getStringAscii(0);
        ringBuffer.readCommit(read1.length() + 4);

        //then
        assertEquals(msg1, read1);
        assertEquals(msg2.length() + 4, ringBuffer.readLength());

        //WHEN: prepare reading msg2
        success = ringBuffer.readWrap(buffer);

        //then
        assertTrue(success);
        assertEquals(msg2.length(), buffer.getInt(0));
        assertTrue(buffer.capacity() >= len2);

        //WHEN: read msg2
        final String read2 = buffer.getStringAscii(0);
        ringBuffer.readCommit(read2.length() + 4);

        //then
        assertEquals(msg2, read2);
        assertEquals(0, ringBuffer.readLength());
    }

    @ParameterizedTest
    @MethodSource("readWriteWrapAround_messages")
    void readWriteWrapAround(final String msg) {
        final int times = 13 * BUFFER_CAPACITY;
        final MutableDirectBuffer bytes = new UnsafeBuffer(new byte[msg.length() + 4]);
        final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);
        final int bytesPerMessage = bytes.putStringAscii(0, msg);

        //WHEN: fill buffer
        int written = 0;
        while (ringBuffer.writeLength() >= bytesPerMessage) {
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
            if (!ringBuffer.readWrap(buffer, bytesPerMessage)) {
                assertTrue(ringBuffer.readSkip(ringBuffer.readLength()), "readSkip[" + i + "]");
                assertTrue(ringBuffer.readWrap(buffer, bytesPerMessage), "readWrap[" + i + "]");
            }

            //THEN
            assertTrue(buffer.capacity() >= bytesPerMessage, "len[" + i + "]");
            assertEquals(msg, buffer.getStringAscii(0), "msg[" + i + "]");
            ringBuffer.readCommit(bytesPerMessage);
            log("[R:%s]:\t%s\n", read, ringBuffer);
            read++;

            //WHEN
            if (!ringBuffer.writeWrap(buffer, bytes.capacity())) {
                assertTrue(ringBuffer.writeSkip(ringBuffer.writeLength()), "writeSkip[" + i + "]");
                assertTrue(ringBuffer.writeWrap(buffer, ringBuffer.writeLength()), "writeWrap[" + i + "]");
            }

            //WHEN + THEN: write one
            buffer.putBytes(0, bytes, 0, buffer.capacity());
            ringBuffer.writeCommit(buffer.capacity());
            log("[W:%s]:\t%s\n", written, ringBuffer);
            written++;
        }

        //WHEN: read rest
        while (read < written) {
            //WHEN: read one
            if (!ringBuffer.readWrap(buffer, bytesPerMessage)) {
                assertTrue(ringBuffer.readSkip(ringBuffer.readLength()), "readSkip");
                assertTrue(ringBuffer.readWrap(buffer, bytesPerMessage), "readWrap");
            }

            //THEN
            assertTrue(bytes.capacity() >= bytesPerMessage);
            assertEquals(msg, buffer.getStringAscii(0));
            ringBuffer.readCommit(bytesPerMessage);
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

    private static void log(final String format, final Object... args) {
        if (DEBUG_OUT) {
            System.out.printf(format, args);
        }
    }
}
