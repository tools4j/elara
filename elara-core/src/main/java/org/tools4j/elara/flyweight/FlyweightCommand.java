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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.command.Command;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.CommandDescriptor.COMMAND_TIME_OFFSET;
import static org.tools4j.elara.flyweight.CommandDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.CommandDescriptor.PAYLOAD_TYPE_OFFSET;
import static org.tools4j.elara.flyweight.CommandDescriptor.SOURCE_ID_OFFSET;
import static org.tools4j.elara.flyweight.CommandDescriptor.SOURCE_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;

/**
 * A flyweight command for reading and writing event data laid out as per {@link CommandDescriptor} definition.
 */
public final class FlyweightCommand implements Flyweight<FlyweightCommand>, Command, CommandFrame {
    public static final int HEADER_LENGTH = CommandDescriptor.HEADER_LENGTH;

    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final DirectBuffer payload = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightCommand wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        return wrapPayload(buffer, offset + HEADER_LENGTH);
    }

    public FlyweightCommand wrapSilently(final DirectBuffer headerBuffer, final int headerOffset,
                                         final DirectBuffer payloadBuffer, final int payloadOffset) {
        header.wrapSilently(headerBuffer, headerOffset);
        return wrapPayload(payloadBuffer, payloadOffset);
    }

    private FlyweightCommand wrapPayload(final DirectBuffer buffer, final int offset) {
        payload.wrap(buffer, offset, header.frameSize() - HEADER_LENGTH);
        return this;
    }

    @Override
    public boolean valid() {
        return header.valid();
    }

    public FlyweightCommand reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public int sourceId() {
        return sourceId(header.buffer());
    }

    public static int sourceId(final DirectBuffer buffer) {
        return buffer.getInt(SOURCE_ID_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long sourceSequence() {
        return sourceSequence(header.buffer());
    }

    public static long sourceSequence(final DirectBuffer buffer) {
        return buffer.getLong(SOURCE_SEQUENCE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int payloadType() {
        return payloadType(header.buffer());
    }

    public static int payloadType(final DirectBuffer buffer) {
        return buffer.getInt(PAYLOAD_TYPE_OFFSET);
    }

    @Override
    public long commandTime() {
        return commandTime(header.buffer());
    }

    public static long commandTime(final DirectBuffer buffer) {
        return buffer.getLong(COMMAND_TIME_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        return writeHeaderAndPayload(header.reserved(), sourceId(), sourceSequence(), commandTime(), payloadType(),
                payload, 0, payload.capacity(), dst, dstOffset);
    }

    public static int writeHeader(final int sourceId,
                                  final long sourceSequence,
                                  final long commandTime,
                                  final int payloadType,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        return writeHeader((short)0, sourceId, sourceSequence, commandTime, payloadType, payloadSize, dst, dstOffset);
    }

    public static int writeHeader(final short reserved,
                                  final int sourceId,
                                  final long sourceSequence,
                                  final long commandTime,
                                  final int payloadType,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(COMMAND_TYPE, reserved, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + SOURCE_ID_OFFSET,
                (0xffffffffL & sourceId) | ((0xffffffffL & payloadType) << 32),
                LITTLE_ENDIAN);
        dst.putLong(dstOffset + SOURCE_SEQUENCE_OFFSET, sourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + COMMAND_TIME_OFFSET, commandTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int writeHeaderAndPayload(final short reserved,
                                            final int sourceId,
                                            final long sourceSequence,
                                            final long commandTime,
                                            final int payloadType,
                                            final DirectBuffer payload,
                                            final int payloadOffset,
                                            final int payloadSize,
                                            final MutableDirectBuffer dst,
                                            final int dstOffset) {
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(COMMAND_TYPE, reserved, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + SOURCE_ID_OFFSET,
                (0xffffffffL & sourceId) | ((0xffffffffL & payloadType) << 32),
                LITTLE_ENDIAN);
        dst.putLong(dstOffset + SOURCE_SEQUENCE_OFFSET, sourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + COMMAND_TIME_OFFSET, commandTime, LITTLE_ENDIAN);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, payloadOffset, payloadSize);
        return frameSize;
    }

    public static void writePayloadSize(final int payloadSize, final MutableDirectBuffer dst) {
        FlyweightHeader.writeFrameSize(HEADER_LENGTH + payloadSize, dst);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightCommand{");
        if (valid()) {
            final Header header = header();
            dst.append("version=").append(header.version());
            dst.append("|type=").append(type());
            dst.append("|reserved=").append(header.reserved());
            dst.append("|frame-size=").append(frameSize());
            dst.append("|source-id=").append(sourceId());
            dst.append("|source-seq=").append(sourceSequence());
            dst.append("|command-time=").append(commandTime());
            dst.append("|payload-type=").append(payloadType());
            dst.append("|payload-size=").append(payloadSize());
        } else {
            dst.append("???");
        }
        dst.append('}');
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}
