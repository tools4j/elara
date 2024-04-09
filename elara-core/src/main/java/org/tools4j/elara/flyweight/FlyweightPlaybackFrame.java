/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.ENGINE_TIME_OFFSET;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.MAX_AVAILABLE_EVT_SEQ_OFFSET;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.PAYLOAD_OFFSET;

/**
 * A flyweight playback frame for reading and writing playback event or heartbeat data laid out as per
 * {@link PlaybackDescriptor} definition.
 */
public class FlyweightPlaybackFrame implements Flyweight<FlyweightPlaybackFrame>, PlaybackFrame {
    public static final int HEADER_LENGTH = PlaybackDescriptor.HEADER_LENGTH;
    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final DirectBuffer payload = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightPlaybackFrame wrap(final DirectBuffer buffer, final int offset) {
        this.header.wrap(buffer, offset);
        FrameType.validatePlaybackFrameType(header.type());
        return wrapPayload(buffer, offset);
    }

    public FlyweightPlaybackFrame wrapSilently(final DirectBuffer buffer, final int offset) {
        this.header.wrapSilently(buffer, offset);
        return wrapPayload(buffer, offset);
    }

    private FlyweightPlaybackFrame wrapPayload(final DirectBuffer buffer, final int offset) {
        final int frameSize = header.frameSize();
        payload.wrap(buffer, offset + HEADER_LENGTH, frameSize - HEADER_LENGTH);
        return this;
    }

    @Override
    public boolean valid() {
        return header.valid() && FrameType.isPlaybackType(header.type());
    }

    @Override
    public FlyweightPlaybackFrame reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public long engineTime() {
        return engineTime(header.buffer());
    }

    public static long engineTime(final DirectBuffer buffer) {
        return buffer.getLong(ENGINE_TIME_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long maxAvailableEventSequence() {
        return maxAvailableEventSequence(header.buffer());
    }

    public static long maxAvailableEventSequence(final DirectBuffer buffer) {
        return buffer.getLong(MAX_AVAILABLE_EVT_SEQ_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        return writeHeaderAndPayload(header.reserved(), engineTime(), maxAvailableEventSequence(),
                payload, 0, payload.capacity(), dst, dstOffset);
    }

    public static int writeHeader(final short reserved,
                                  final long engineTime,
                                  final long maxAvailableEventSequence,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert payloadSize >= 0;
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(FrameType.PLAYBACK_TYPE, reserved, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + ENGINE_TIME_OFFSET, engineTime, LITTLE_ENDIAN);
        dst.putLong(dstOffset + MAX_AVAILABLE_EVT_SEQ_OFFSET, maxAvailableEventSequence, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int writeHeaderAndPayload(final short reserved,
                                            final long engineTime,
                                            final long maxAvailableEventSequence,
                                            final DirectBuffer payload,
                                            final int payloadOffset,
                                            final int payloadSize,
                                            final MutableDirectBuffer dst,
                                            final int dstOffset) {
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(FrameType.PLAYBACK_TYPE, reserved, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + ENGINE_TIME_OFFSET, engineTime, LITTLE_ENDIAN);
        dst.putLong(dstOffset + MAX_AVAILABLE_EVT_SEQ_OFFSET, maxAvailableEventSequence, LITTLE_ENDIAN);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, payloadOffset, payloadSize);
        return frameSize;
    }

    public static void writePayloadSize(final int payloadSize, final MutableDirectBuffer dst) {
        assert payloadSize >= 0;
        FlyweightHeader.writeFrameSize(HEADER_LENGTH + payloadSize, dst);
    }

    @Override
    public void accept(final FrameVisitor visitor) {
        visitor.playbackFrame(this);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightPlaybackFrame");
        if (valid()) {
            final Header header = header();
            dst.append(":version=").append(header.version());
            dst.append("|type=").append(type());
            dst.append("|frame-size=").append(frameSize());
            dst.append("|engine-time=").append(engineTime());
            dst.append("|max-avail-evt-seq=").append(maxAvailableEventSequence());
            dst.append("|payload-size=").append(payloadSize());
        } else {
            dst.append(":???");
        }
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(128)).toString();
    }

}
