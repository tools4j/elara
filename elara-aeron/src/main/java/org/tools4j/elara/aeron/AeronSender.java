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

import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;

import static java.util.Objects.requireNonNull;

public class AeronSender extends MessageSender.Buffered {

    private final Publication publication;
    private final int maxRetriesAfterAdminAction;
    private String name;

    public AeronSender(final Publication publication, final AeronConfig config) {
        super(config.senderInitialBufferSize());
        this.publication = requireNonNull(publication);
        this.maxRetriesAfterAdminAction = config.senderMaxRetriesAfterAdminAction();
    }

    /**
     * @return the underlying aeron publication
     */
    public Publication publication() {
        return publication;
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        long result = publication.offer(buffer, offset, length);
        if (result >= 0) {
            return SendingResult.SENT;
        }
        if (result == Publication.ADMIN_ACTION) {
            result = retryAfterAdminAction(buffer, offset, length);
        }
        switch ((int)result) {
            case (int)Publication.NOT_CONNECTED:
                return SendingResult.DISCONNECTED;
            case (int)Publication.BACK_PRESSURED:
                return SendingResult.BACK_PRESSURED;
            case (int)Publication.CLOSED:
                return SendingResult.CLOSED;
            case (int)Publication.ADMIN_ACTION:
            case (int)Publication.MAX_POSITION_EXCEEDED:
            default:
                return result >= 0 ? SendingResult.SENT : SendingResult.FAILED;
        }
    }

    private long retryAfterAdminAction(final DirectBuffer buffer, final int offset, final int length) {
        for (int i = 0; i < maxRetriesAfterAdminAction; i++) {
            final long result = publication.offer(buffer, offset, length);
            if (result != Publication.ADMIN_ACTION) {
                return result;
            }
        }
        return Publication.ADMIN_ACTION;
    }

    @Override
    public boolean isClosed() {
        return publication.isClosed();
    }

    @Override
    public void close() {
        publication.close();
    }

    @Override
    public String toString() {
        return name != null ? name : (name = "AeronSender{" + publication.channel() + ":" + publication.streamId() + '}');
    }
}
