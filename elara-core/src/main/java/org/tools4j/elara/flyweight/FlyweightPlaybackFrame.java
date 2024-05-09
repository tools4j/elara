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
import org.tools4j.elara.app.message.Event;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.MAX_AVAILABLE_EVT_SEQ_OFFSET;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.MAX_AVAILABLE_SOURCE_SEQ_OFFSET;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.NEWEST_EVENT_TIME_OFFSET;
import static org.tools4j.elara.flyweight.PlaybackDescriptor.PAYLOAD_OFFSET;

/**
 * A flyweight playback frame for reading and writing playback event data laid out as per {@link PlaybackDescriptor}.
 */
public class FlyweightPlaybackFrame implements Flyweight<FlyweightPlaybackFrame>, PlaybackFrame {
    public static final int HEADER_LENGTH = PlaybackDescriptor.HEADER_LENGTH;
    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final FlyweightEvent event = new FlyweightEvent();

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
        event.wrap(buffer, offset + HEADER_LENGTH);
        return this;
    }

    @Override
    public boolean valid() {
        return header.valid() && FrameType.isPlaybackType(header.type()) && event.valid();
    }

    @Override
    public FlyweightPlaybackFrame reset() {
        header.reset();
        event.reset();
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public long maxAvailableSourceSequence() {
        return maxAvailableSourceSequence(header.buffer());
    }

    public static long maxAvailableSourceSequence(final DirectBuffer buffer) {
        return buffer.getLong(MAX_AVAILABLE_SOURCE_SEQ_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long newestEventTime() {
        return newestEventTime(header.buffer());
    }

    public static long newestEventTime(final DirectBuffer buffer) {
        return buffer.getLong(NEWEST_EVENT_TIME_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long maxAvailableEventSequence() {
        return maxAvailableEventSequence(header.buffer());
    }

    public static long maxAvailableEventSequence(final DirectBuffer buffer) {
        return buffer.getLong(MAX_AVAILABLE_EVT_SEQ_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public Event event() {
        return event;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        writeHeader(header.reserved(), maxAvailableSourceSequence(), maxAvailableEventSequence(), newestEventTime(),
                event.frameSize(), dst, dstOffset);
        return HEADER_LENGTH + event.writeTo(dst, dstOffset + HEADER_LENGTH);
    }

    public static int writeHeader(final short reserved,
                                  final long maxAvailableSourceSequence,
                                  final long maxAvailableEventSequence,
                                  final long newestEventTime,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert payloadSize >= 0;
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(FrameType.PLAYBACK_TYPE, reserved, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + MAX_AVAILABLE_SOURCE_SEQ_OFFSET, maxAvailableSourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + MAX_AVAILABLE_EVT_SEQ_OFFSET, maxAvailableEventSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + NEWEST_EVENT_TIME_OFFSET, newestEventTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int writeHeaderAndPayload(final short reserved,
                                            final long maxAvailableSourceSequence,
                                            final long maxAvailableEventSequence,
                                            final long newestEventTime,
                                            final DirectBuffer payload,
                                            final int payloadOffset,
                                            final int payloadSize,
                                            final MutableDirectBuffer dst,
                                            final int dstOffset) {
        writeHeader(reserved, maxAvailableSourceSequence, maxAvailableEventSequence, newestEventTime, payloadSize,
                dst, dstOffset);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, payloadOffset, payloadSize);
        return HEADER_LENGTH + payloadSize;
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
            dst.append("|max-avail-src-seq=").append(maxAvailableSourceSequence());
            dst.append("|max-avail-evt-seq=").append(maxAvailableEventSequence());
            dst.append("|newest-evt-time=").append(newestEventTime());
            dst.append("|event=");
            event.printTo(dst);
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
