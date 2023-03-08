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

import java.io.PrintWriter;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class IteratorMessagePrinter<M, T> implements MessagePrinter<M> {

    interface Item<T> {
        Function<String, Object> baseValueSupplier();
        String itemSeparator();
        int itemIndex();
        T itemValue();
    }

    interface ValueProvider<M, T> {
        Iterable<T> values(long line, long entryId, M message);
    }

    interface ItemFormatter<T> extends ValueFormatter<Item<? extends T>> {
        /**
         * Placeholder in format string for item separator in repeated groups, empty for first element
         */
        String ITEM_SEPARATOR = "{sep}";
        /**
         * Placeholder in format string for item index in repeated groups, zero for first element
         */
        String ITEM_INDEX = "{item-index}";
        /**
         * Placeholder in format string for item value in repeated groups, empty for first element
         */
        String ITEM_VALUE = "{item-value}";

        default Object itemSeparator(long line, long entryId, Item<? extends T> item) {
            return item.itemSeparator();
        }
        default Object itemIndex(long line, long entryId, Item<? extends T> item) {
            return item.itemIndex();
        }
        default Object itemValue(long line, long entryId, Item<? extends T> item) {return item.itemValue();}

        @Override
        default Object value(final String placeholder, final long line, final long entryId, final Item<? extends T> item) {
            switch (placeholder) {
                case ITEM_VALUE: return itemValue(line, entryId, item);
                case ITEM_SEPARATOR: return itemSeparator(line, entryId, item);
                case ITEM_INDEX: return itemIndex(line, entryId, item);
                default: return item.baseValueSupplier().apply(placeholder);
            }
        }

        @Override
        default ItemFormatter<T> then(final ValueFormatter<? super Item<? extends T>> next) {
            requireNonNull(next);
            return new ItemFormatter<T>() {

                @Override
                public Object itemValue(final long line, final long entryId, final Item<? extends T> item) {
                    return ItemFormatter.this.itemValue(line, entryId, item);
                }

                @Override
                public Object itemSeparator(final long line, final long entryId, final Item<? extends T> item) {
                    return ItemFormatter.this.itemSeparator(line, entryId, item);
                }

                @Override
                public Object itemIndex(final long line, final long entryId, final Item<? extends T> item) {
                    return ItemFormatter.this.itemIndex(line, entryId, item);
                }

                @Override
                public Object value(final String placeholder, final long line, final long entryId, final Item<? extends T> item) {
                    final Object replaced = ItemFormatter.this.value(placeholder, line, entryId, item);
                    if (placeholder.equals(replaced)) {
                        return next.value(placeholder, line, entryId, item);
                    }
                    return replaced;
                }
            };
        }
    }

    private final String iteratedPattern;
    private final String itemSeparator;
    private final ValueProvider<? super M, ? extends T> valueProvider;
    private final ValueFormatter<? super M> baseFormatter;
    private final MessagePrinter<Item<T>> itemPrinter;

    public IteratorMessagePrinter(final String iteratedPattern,
                                  final String itemSeparator,
                                  final ItemFormatter<? super T> itemFormatter,
                                  final ValueProvider<? super M, ? extends T> valueProvider,
                                  final ValueFormatter<? super M> baseFormatter) {
        this.iteratedPattern = requireNonNull(iteratedPattern);
        this.itemSeparator = requireNonNull(itemSeparator);
        this.valueProvider = requireNonNull(valueProvider);
        this.baseFormatter = requireNonNull(baseFormatter);
        this.itemPrinter = MessagePrinter.parameterized(iteratedPattern, itemFormatter);
    }

    @Override
    public void print(final long line, final long entryId, final M message, final PrintWriter writer) {
        final Function<String, Object> baseValueSupplier = placeholder -> baseFormatter.value(placeholder, line, entryId, message);
        final Iterable<? extends T> iterable = valueProvider.values(line, entryId, message);
        int index = 0;
        for (final T itemValue : iterable) {
            final Item<T> item = item(baseValueSupplier, index, index == 0 ? "" : itemSeparator, itemValue);
            itemPrinter.print(line, entryId, item, writer);
            index++;
        }
    }

    private static <T> Item<T> item(final Function<String, Object> baseValueSupplier, final int index, final String separator, final T value) {
        requireNonNull(baseValueSupplier);
        requireNonNull(separator);
        //nullable: value
        return new Item<T>() {
            @Override
            public Function<String, Object> baseValueSupplier() {
                return baseValueSupplier;
            }

            @Override
            public String itemSeparator() {
                return separator;
            }

            @Override
            public int itemIndex() {
                return index;
            }

            @Override
            public T itemValue() {
                return value;
            }

            @Override
            public String toString() {
                return "item[" + index + "]=" + value;
            }
        };
    }

    @Override
    public String toString() {
        return "{iterate(sep= " + itemSeparator + "):" + iteratedPattern + "}";
    }
}
