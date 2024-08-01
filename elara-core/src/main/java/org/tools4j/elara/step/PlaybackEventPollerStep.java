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
package org.tools4j.elara.step;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.flyweight.EventFrame;
import org.tools4j.elara.flyweight.FlyweightPlaybackFrame;
import org.tools4j.elara.flyweight.PlaybackDescriptor;
import org.tools4j.elara.handler.PlaybackHandler;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.store.PlaybackEventPoller;

import static java.util.Objects.requireNonNull;

/**
 * Polls events from the event store and invokes the {@link PlaybackHandler}.
 */
public class PlaybackEventPollerStep implements AgentStep {

    private static final short RESERVED_VALUE = 0;
    private final PlaybackEventPoller playbackEventPoller;
    private final PlaybackHandler playbackHandler;

    private final MessageStore.Handler pollerHandler = this::onEventFrame;
    private final MutableDirectBuffer playbackHeader = new UnsafeBuffer(new byte[PlaybackDescriptor.HEADER_LENGTH]);

    private final FlyweightPlaybackFrame playbackFrame = new FlyweightPlaybackFrame();
    private final EventFrame eventFrame = playbackFrame.eventFrame();

    public PlaybackEventPollerStep(final MessageStore eventStore,
                                   final PlaybackHandler playbackHandler) {
        this(new PlaybackEventPoller(eventStore), playbackHandler);
    }

    public PlaybackEventPollerStep(final PlaybackEventPoller playbackEventPoller,
                                   final PlaybackHandler playbackHandler) {
        this.playbackEventPoller = requireNonNull(playbackEventPoller);
        this.playbackHandler = requireNonNull(playbackHandler);
        playbackFrame.wrapHeaderSilently(playbackHeader, 0);
    }

    @Override
    public int doWork() {
        return playbackEventPoller.poll(pollerHandler);
    }

    private Result onEventFrame(final DirectBuffer frame) {
        try {
            playbackFrame.wrapPayload(frame, 0);
            writePlaybackHeaderFor(eventFrame.sourceId(), eventFrame.frameSize());
            playbackHandler.onPlaybackFrame(playbackFrame);
            return Result.POLL;
        } finally {
            playbackFrame.resetPayload();
        }
    }

    private void writePlaybackHeaderFor(final int sourceId, final int payloadSize) {
        FlyweightPlaybackFrame.writeHeader(
                RESERVED_VALUE,
                playbackEventPoller.maxAvailableSourceSequence(sourceId),
                playbackEventPoller.maxAvailableEventSequence(),
                playbackEventPoller.newestEventTime(),
                payloadSize,
                playbackHeader, 0
        );
    }

}
