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
package org.tools4j.elara.logging;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class FlyweightPlaceholderReplacer implements PlaceholderReplacer {
    private final StringBuilder temp;
    private Consumer<? super CharSequence> formattedStringConsumer;
    private String message;
    private int start;

    FlyweightPlaceholderReplacer(final int initialCapacity) {
        this.temp = new StringBuilder(initialCapacity);
    }

    FlyweightPlaceholderReplacer init(final Consumer<? super CharSequence> formattedStringConsumer, final String message) {
        this.formattedStringConsumer = requireNonNull(formattedStringConsumer);
        this.temp.setLength(0);
        this.message = String.valueOf(message);//allow null
        this.start = 0;
        return this;
    }

    @Override
    public FlyweightPlaceholderReplacer replace(final long arg) {
        final int index = message.indexOf(PLACEHOLDER, start);
        if (index >= 0) {
            temp.append(message, start, index);
            temp.append(arg);
            start = index + PLACEHOLDER.length();
        }
        return this;
    }

    @Override
    public FlyweightPlaceholderReplacer replace(final Object arg) {
        return replace(arg, (obj, builder) -> {
            if (obj instanceof CharSequence) {
                builder.append((CharSequence) obj);
            } else if (obj instanceof Printable) {
                ((Printable)obj).printTo(builder);
            } else if (obj instanceof Throwable) {
                appendThrowable((Throwable)obj, builder);
            } else {
                builder.append(obj);
            }
        });
    }

    @Override
    public FlyweightPlaceholderReplacer replace(final Printable arg) {
        return replace(arg, Printable::printTo);
    }

    @Override
    public FlyweightPlaceholderReplacer replace(final Throwable arg) {
        return replace(arg, FlyweightPlaceholderReplacer::appendThrowable);
    }

    private static void appendThrowable(final Throwable throwable, final StringBuilder builder) {
        builder.append(throwable.getClass().getName());
        final String message = throwable.getMessage();
        if (message != null && message.length() > 0) {
            builder.append(':').append(message);
        }
    }

    private <T> FlyweightPlaceholderReplacer replace(final T arg, final BiConsumer<T, StringBuilder> formatter) {
        final int index = message.indexOf(PLACEHOLDER, start);
        if (index >= 0) {
            temp.append(message, start, index);
            if (arg == null) {
                temp.append((String)null);
            } else {
                formatter.accept(arg, temp);
            }
            start = index + PLACEHOLDER.length();
        }
        return this;
    }

    @Override
    public CharSequence format() {
        temp.append(message, start, message.length());
        formattedStringConsumer.accept(temp);
        return temp;
    }
}
