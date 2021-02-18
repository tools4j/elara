/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.format;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ParameterizedMessagePrinter<M> implements MessagePrinter<M> {

    private final String pattern;
    private final ValueFormatter<? super M> formatter;
    private final MessagePrinter<? super M>[] printers;

    public ParameterizedMessagePrinter(final String pattern, final ValueFormatter<? super M> formatter) {
        this.pattern = requireNonNull(pattern);
        this.formatter = requireNonNull(formatter);
        this.printers = parse(pattern);
    }

    @Override
    public void print(final long line, final long entryId, final M message, final PrintWriter writer) {
        for (final MessagePrinter<? super M> p : printers) {
            p.print(line, entryId, message, writer);
        }
    }

    @Override
    public String toString() {
        return pattern;
    }

    private MessagePrinter<? super M>[] parse(final String pattern) {
        final List<MessagePrinter<? super M>> printers = new ArrayList<>();
        int end = -1;
        int start = pattern.indexOf('{', end + 1);
        while (start >= 0) {
            final int pre = end + 1;
            end = pattern.indexOf('}', start + 1);
            if (end < 0) {
                break;
            }
            start = lastIndexOf(pattern, '{', start, end);
            if (pre < start) {
                printers.add(stringPrinter(pattern.substring(pre, start)));
            }
            printers.add(placeHolderPrinter(pattern.substring(start, end + 1)));
            start = pattern.indexOf('{', end + 1);
        }
        if (end + 1 < pattern.length()) {
            printers.add(stringPrinter(pattern.substring(end + 1)));
        }
        @SuppressWarnings("unchecked")
        final MessagePrinter<? super M>[] arr = printers.toArray(
                (MessagePrinter<? super M>[])new MessagePrinter[0]
        );
        return arr;
    }

    private static int lastIndexOf(final String s, final char ch, final int start, final int end) {
        assert s.charAt(start) == ch;
        int index = start;
        int next = s.indexOf(ch, index + 1);
        while (next >= 0 && next < end) {
            index = next;
            next = s.indexOf(ch, index + 1);
        }
        return index;
    }

    private static MessagePrinter<Object> stringPrinter(final String s) {
        return (line, entryId, message, writer) -> writer.write(s);
    }

    private MessagePrinter<M> placeHolderPrinter(final String placeHolder) {
        return (line, entryId, message, writer) -> writer.write(
                String.valueOf(formatter.value(placeHolder, line, entryId, message))
        );
    }
}
