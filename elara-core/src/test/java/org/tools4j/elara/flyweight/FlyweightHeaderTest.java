/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;

/**
 * Unit test for {@link FlyweightHeader}
 */
public class FlyweightHeaderTest {

    @Test
    public void unwrapped() {
        //given
        final FlyweightHeader header = new FlyweightHeader();

        //when + then
        assertFalse(header.valid(), "header.valid");
        try {
            header.input();
        } catch (final IndexOutOfBoundsException e) {
            //expected
        }
    }

    @Test
    public void invalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> new FlyweightHeader().init(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ));
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putShort(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);
        final FlyweightHeader header = new FlyweightHeader().init(buffer, 0);

        //when + then
        assertEquals(0, header.input(), "header.input");
        assertEquals(0, header.type(), "header.type");
        assertEquals(0, header.sequence(), "header.sequence");
        assertEquals(0, header.time(), "header.time");
        assertEquals(Version.CURRENT, header.version(), "header.version");
        assertEquals(0, header.index(), "header.index");
        assertEquals(0, header.payloadSize(), "header.payload-size");
    }

    @Test
    public void initWithValues() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values();
        final FlyweightHeader header = new FlyweightHeader();

        //when
        header.init(values.input, values.type, values.seq, values.time, values.index, values.payloadSize,
                new ExpandableArrayBuffer(), headerOffset);

        //when + then
        assertEquals(values.input, header.input(), "header.input");
        assertEquals(values.type, header.type(), "header.type");
        assertEquals(values.seq, header.sequence(), "header.sequence");
        assertEquals(values.time, header.time(), "header.time");
        assertEquals(values.index, header.index(), "header.index");
        assertEquals(values.payloadSize, header.payloadSize(), "header.payload-size");

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = header.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH, totalLen, "total bytes length");

        //when
        final FlyweightHeader copy = new FlyweightHeader().init(buffer, copyOffset);

        //then
        assertEquals(values.input, copy.input(), "header.input");
        assertEquals(values.type, copy.type(), "header.type");
        assertEquals(values.seq, copy.sequence(), "header.sequence");
        assertEquals(values.time, copy.time(), "header.time");
        assertEquals(Version.CURRENT, copy.version(), "header.version");
        assertEquals(values.index, copy.index(), "header.index");
        assertEquals(values.payloadSize, copy.payloadSize(), "header.payload-size");

        //when
        final FlyweightHeader third = new FlyweightHeader().init(header, new ExpandableArrayBuffer(), 123);

        //then
        assertEquals(values.input, third.input(), "header.input");
        assertEquals(values.type, third.type(), "header.type");
        assertEquals(values.seq, third.sequence(), "header.sequence");
        assertEquals(values.time, third.time(), "header.time");
        assertEquals(Version.CURRENT, third.version(), "header.version");
        assertEquals(values.index, third.index(), "header.index");
        assertEquals(values.payloadSize, third.payloadSize(), "header.payload-size");
    }

    private static class Values {
        final int input = 77;
        final int type = 123;
        final long seq = 998877;
        final long time = 998877665544L;
        final short index = 7;
        final int payloadSize = 22;
    }
}