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
package org.tools4j.elara.stream;

import org.agrona.MutableDirectBuffer;
import org.agrona.collections.LongArrayList;
import org.tools4j.elara.stream.MessageSender.SendingContext;

import static java.lang.Long.toHexString;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageStreamRunner {

    private final long messageCount;
    private final int messageSizeInBytes;
    private final int maxMessagesPerSecond;

    public MessageStreamRunner(final long messageCount, final int messageSizeInBytes) {
        this(messageCount, messageSizeInBytes, 0);
    }
    public MessageStreamRunner(final long messageCount,
                               final int messageSizeInBytes,
                               final int maxMessagesPerSecond) {
        this.messageCount = messageCount;
        this.messageSizeInBytes = messageSizeInBytes;
        this.maxMessagesPerSecond = maxMessagesPerSecond;
    }

    public long messageCount() {
        return messageCount;
    }

    public int messageSizeInBytes() {
        return messageSizeInBytes;
    }

    public void sendAndReceiveMessages(final MessageSender sender,
                                       final MessageReceiver receiver,
                                       final long maxWaitMillis) throws InterruptedException {
        //given
        final StartLatch startLatch = new StartLatch(2);
        final LongArrayList senderResults = new LongArrayList();
        final LongArrayList receiverResults = new LongArrayList();
        final Thread senderThread = new Thread(null, senderLoop(sender, startLatch, senderResults), "sender");
        final Thread receiverThread = new Thread(null, receiverLoop(receiver, startLatch, receiverResults), "receiver");
        final Timer timer = maxWaitMillis <= 0 ? null : Timer.start(maxWaitMillis);

        //when
        receiverThread.start();
        senderThread.start();

        senderThread.join(timer == null ? 0 : Math.max(1, timer.remainingMillis()));
        receiverThread.join(timer == null ? 0 : Math.max(1, timer.remainingMillis()));

        sender.close();
        receiver.close();

        //then
        if (senderThread.isAlive() || receiverThread.isAlive()) {
            throw new IllegalStateException("Not terminated after " + maxWaitMillis + "ms");
        }
        assertEquals(messageCount, senderResults.getLong(0), "message count [" + sender + "]");
        assertEquals(messageCount, receiverResults.getLong(0), "message count [" + receiver + "]");
        assertEquals(messageSizeInBytes, senderResults.getLong(1), "bytes per message [" + sender + "]");
        assertEquals(messageSizeInBytes, receiverResults.getLong(1), "bytes per message [" + receiver + "]");
        assertEquals(toHexString(senderResults.getLong(2)), toHexString(receiverResults.getLong(2)), "sender/receiver message hash");
    }

    private static long hash(final long hash, final long extra) {
        return 31 * hash + extra;
    }

    private Runnable senderLoop(final MessageSender sender, final StartLatch startLatch, final LongArrayList results) {
        requireNonNull(sender);
        return () -> {
            final double minNanosPerMessage = maxMessagesPerSecond <= 0 ? 0 : 1e9/maxMessagesPerSecond;
            long hash = 0;
            int retries = 0;
            long timeStart = 0;

            startLatch.markReadyAndAwaitStartSignal();

            for (int i = 0; i < messageCount; i++) {
                long mhash, time;
                SendingResult result = null;
                do {
                    retries += (result == null ? 0 : 1);
                    try (final SendingContext context = sender.sendingMessage()) {
                        final MutableDirectBuffer buffer = context.buffer();
                        mhash = 0;
                        for (int j = 0; j < messageSizeInBytes; j++) {
                            final byte val = (byte) (i + j);
                            buffer.putByte(j, val);
                            mhash = hash(mhash, val);
                        }
                        time = i == 0 ? System.nanoTime() : 0;
                        result = context.send(messageSizeInBytes);
                    }
                } while (result != SendingResult.SENT);
                hash = hash(hash, mhash);
                if (i == 0) {
                    timeStart = time;
                }
                if (minNanosPerMessage > 0) {
                    final long nanos = (long)((i + 1.0) * minNanosPerMessage);
                    while (System.nanoTime() - timeStart < nanos);
                }
            }
            final long timeEnd = System.nanoTime();
            System.out.println(sender + " sent: " + messageCount + " [" + messageSizeInBytes + " bytes each" +
                    ", retries=" + retries + ", time=" + (timeEnd - timeStart) / 1e9f + "s, hash=" + toHexString(hash) + "]");
            results.addLong(messageCount);
            results.addLong(messageSizeInBytes);
            results.addLong(hash);
        };
    }

    private Runnable receiverLoop(final MessageReceiver receiver, final StartLatch startLatch, final LongArrayList results) {
        requireNonNull(receiver);
        return () -> {
            final long[] bytesPtr = {0};
            final int[] receivedPtr = {0};
            final long[] hashPtr = {0};
            final long[] times = {0, 0};
            final MessageReceiver.Handler handler = message -> {
                long hash = 0;
                for (int i = 0; i < message.capacity(); i++) {
                    hash = hash(hash, message.getByte(i));
                    bytesPtr[0]++;
                }
                receivedPtr[0]++;
                hashPtr[0] = hash(hashPtr[0], hash);
//                System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) + " bytes each, " + bytesPtr[0] + " bytes total]");
            };
            int count = 0;

            startLatch.markReadyAndAwaitStartSignal();

            while (bytesPtr[0] < messageCount * messageSizeInBytes) {
                final long time = count == 0 ? System.nanoTime() : 0;
                final int polled = receiver.poll(handler);
                if (polled > 0) {
                    if (count == 0) {
                        times[0] = time;
                    }
                    count += polled;
                }
            }
            times[1] = System.nanoTime();
//            assert count == receivedPtr[0];//does not hold for TCP due to connect which also returns polled > 0
            System.out.println(receiver + " received: " + receivedPtr[0] + " [" + (bytesPtr[0] / receivedPtr[0]) +
                    " bytes each, " + bytesPtr[0] + " bytes total, time=" + (times[1] - times[0]) / 1e9f +
                    "s, hash=" + toHexString(hashPtr[0]) + ")");
            results.addLong(receivedPtr[0]);
            results.addLong(bytesPtr[0] / receivedPtr[0]);
            results.addLong(hashPtr[0]);
        };
    }
}