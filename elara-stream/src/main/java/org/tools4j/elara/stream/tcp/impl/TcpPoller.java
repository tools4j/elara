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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.nio.TransportPoller;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class TcpPoller extends TransportPoller {

    @FunctionalInterface
    interface SelectionHandler {
        int OK = 0;
        int BACK_PRESSURE = Integer.MIN_VALUE >> 1;
        int MESSAGE_TOO_LARGE = Integer.MIN_VALUE >> 2;
        SelectionHandler NO_OP = key -> OK;
        int onSelectionKey(SelectionKey key) throws IOException;
    }

    final Selector selector() {
        return selector;
    }

    final int selectNow(final SelectionHandler selectionHandler) throws IOException {
        selector.selectNow();
        final int size = selectedKeySet.size();
        if (size == 0) {
            return 0;
        }
        int result = 0;
        IOException exception = null;
        for (int i = 0; i < size; i++) {
            final SelectionKey key = selectedKeySet.keys()[i];
            result |= key.readyOps();
            try {
                result |= selectionHandler.onSelectionKey(key);
            } catch (final IOException e) {
                exception = exceptionOrSuppress(exception, e);
            }
        }
        selectedKeySet.clear();
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    boolean isClosed() {
        return !selector.isOpen();
    }

    private static <T extends Throwable> T exceptionOrSuppress(final T throwable, final T next) {
        if (throwable == null) {
            return next;
        }
        throwable.addSuppressed(next);
        return throwable;
    }
}