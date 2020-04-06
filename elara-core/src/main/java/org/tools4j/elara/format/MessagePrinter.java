/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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

import java.io.PrintWriter;

@FunctionalInterface
public interface MessagePrinter<M> {

    MessagePrinter<Object> DEFAULT = MessagePrinters.GENERAL;

    void print(long line, M message, PrintWriter writer);

    static <M> MessagePrinter<M> parameterized(final String pattern, final ValueFormatter<? super M> formatter) {
        return new ParameterizedMessagePrinter<>(pattern, formatter);
    }

    static <M> MessagePrinter<M> composite(final PrinterProvider<M> printerProvider) {
        return new CompositeMessagePrinter<>(printerProvider);
    }

    static <M> MessagePrinter<M> composite(final PrinterSelector<M> printerSelector,
                                           final MessagePrinter<? super M>... printers) {
        return new CompositeMessagePrinter<>(printerSelector, printers);
    }
}