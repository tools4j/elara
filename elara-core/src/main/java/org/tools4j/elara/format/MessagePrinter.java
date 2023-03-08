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
package org.tools4j.elara.format;

import org.tools4j.elara.format.CompositeMessagePrinter.PrinterProvider;
import org.tools4j.elara.format.CompositeMessagePrinter.PrinterSelector;
import org.tools4j.elara.format.IteratorMessagePrinter.ItemFormatter;
import org.tools4j.elara.format.IteratorMessagePrinter.ValueProvider;

import java.io.PrintWriter;
import java.io.StringWriter;

import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface MessagePrinter<M> {

    MessagePrinter<Object> DEFAULT = parameterized("{line}: {message}{nl}", ValueFormatter.DEFAULT);
    MessagePrinter<Object> NOOP = (line, entryId, message, writer) -> {};

    void print(long line, long entryId, M message, PrintWriter writer);

    default String printToString(final long line, final long entryId, final M message) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        print(line, entryId, message, pw);
        pw.flush();
        return sw.toString();
    }

    default ValueFormatter<M> asValueFormatterFor(final String placeholderToken) {
        requireNonNull(placeholderToken);
        return (placeholder, line, entryId, message) -> {
            if (placeholderToken.equals(placeholder)) {
                return printToString(line, entryId, message);
            }
            return placeholder;
        };
    }

    static <M> MessagePrinter<M> parameterized(final String pattern, final ValueFormatter<? super M> formatter) {
        return new ParameterizedMessagePrinter<>(pattern, formatter);
    }

    static <M> MessagePrinter<M> composite(final PrinterProvider<M> printerProvider) {
        return new CompositeMessagePrinter<>(printerProvider);
    }

    @SafeVarargs
    static <M> MessagePrinter<M> composite(final PrinterSelector<M> printerSelector,
                                           final MessagePrinter<? super M>... printers) {
        return new CompositeMessagePrinter<>(printerSelector, printers);
    }

    static <T> IteratorMessagePrinter<T> iterate(final String iteratedPattern,
                                                 final String itemSeparator,
                                                 final ItemFormatter<T> itemFormatter) {
        return new IteratorMessagePrinter<>(iteratedPattern, itemSeparator, itemFormatter);
    }

    static <M, T> MessagePrinter<M> iterate(final String iteratedPattern,
                                            final String itemSeparator,
                                            final ItemFormatter<? super T> itemFormatter,
                                            final ValueProvider<? super M, ? extends T> valueProvider) {
        return iterate(iteratedPattern, itemSeparator, itemFormatter).map(valueProvider);
    }

    static <M, T> ValueFormatter<M> iterationToken(final String placeholderToken,
                                                   final String iteratedPattern,
                                                   final String itemSeparator,
                                                   final ItemFormatter<? super T> itemFormatter,
                                                   final ValueProvider<? super M, ? extends T> valueProvider) {
        final MessagePrinter<M> printer = iterate(iteratedPattern, itemSeparator, itemFormatter, valueProvider);
        return printer.asValueFormatterFor(placeholderToken);
    }
}
