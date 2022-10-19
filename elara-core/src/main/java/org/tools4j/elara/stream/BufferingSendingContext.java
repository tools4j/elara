package org.tools4j.elara.stream;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.MessageSender.SendingContext;

import static java.util.Objects.requireNonNull;

/**
 * Sending context used by {@link MessageSender.Buffered}, kept out of MessageSender to keep its interface cleaner.
 */
final class BufferingSendingContext implements SendingContext {
    private final MessageSender sender;
    private final MutableDirectBuffer buffer;
    private boolean closed;

    public BufferingSendingContext(final MessageSender sender, final MutableDirectBuffer buffer) {
        this.sender = requireNonNull(sender);
        this.buffer = requireNonNull(buffer);
    }

    BufferingSendingContext init() {
        if (!closed) {
            abort();
            throw new IllegalStateException("Sending context not closed");
        }
        closed = false;
        return this;
    }

    MutableDirectBuffer unclosedBuffer() {
        if (closed) {
            throw new IllegalStateException("Sending context closed");
        }
        return buffer;
    }

    @Override
    public MutableDirectBuffer buffer() {
        return unclosedBuffer();
    }

    @Override
    public SendingResult send(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        final DirectBuffer buffer = unclosedBuffer();
        try {
            return sender.sendMessage(buffer, 0, length);
        } finally {
            closed = true;
        }
    }

    @Override
    public void abort() {
        if (!closed) {
            closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
