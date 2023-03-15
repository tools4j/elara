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
package org.tools4j.elara.flyweight;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;

/**
 * Unit test for {@link FlyweightHeader}
 */
public class FlyweightHeaderTest {

    @Test
    public void unwrapped() {
        //given
        final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);

        //when + then
        assertFalse(header.valid(), "header.valid");
        assertThrowsExactly(IndexOutOfBoundsException.class, header::type, "header.type");
    }

    @Test
    public void invalidVersion() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new FlyweightHeader(HEADER_LENGTH).wrap(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ), "header.wrap");
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putShort(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);

        //when
        final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH).wrap(buffer, 0);

        //then
        assertEquals(Version.CURRENT, header.version(), "header.version");
        assertEquals(0, header.type(), "header.type");
        assertEquals(0, header.reserved(), "header.reserved");
        assertEquals(0, header.frameSize(), "header.frameSize");
    }

    @Test
    public void write() {
        //given
        final int offset = 23;
        final Values values = new Values();
        final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();

        //when
        final int written = FlyweightHeader.write(values.type, values.reserved, values.frameSize, buffer, offset);
        header.wrap(buffer, offset);

        //then
        values.assertHeader(header);
        assertEquals(HEADER_LENGTH, written, "bytes written");

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer copyBuffer = new ExpandableArrayBuffer();
        final int copyLen = header.writeTo(copyBuffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH, copyLen, "bytes copied");

        //when
        final FlyweightHeader copy = new FlyweightHeader(HEADER_LENGTH).wrap(copyBuffer, copyOffset);

        //then
        values.assertHeader(copy);

        //when
        final FlyweightHeader silentCopy = new FlyweightHeader(FlyweightEvent.HEADER_LENGTH)
                .wrapSilently(copyBuffer, copyOffset);

        //then
        values.assertHeader(silentCopy);
    }

    private static class Values {
        final byte type = FrameType.TIME_METRIC_TYPE;
        final short reserved = 12345;
        final int frameSize = 2_000_999;

        void assertHeader(final Header header) {
            assertEquals(Version.CURRENT, header.version(), "header.version");
            assertEquals(type, header.type(), "header.type");
            assertEquals(reserved, header.reserved(), "header.reserved");
            assertEquals(frameSize, header.frameSize(), "header.frameSize");
        }
    }
}