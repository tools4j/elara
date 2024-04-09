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
package org.tools4j.elara.composite;

import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Helper to create composites of different types eliminating no-OP components when doing so.
 */
enum Composites {
    ;
    static <T> T composite(final T component1,
                           final T component2,
                           final T noOp,
                           final IntFunction<? extends T[]> arrayFactory,
                           final Function<? super T[], ? extends T> aggregator) {
        if (component1 == noOp) {
            return component2;
        }
        if (component2 == noOp) {
            return component1;
        }
        final T[] components = arrayFactory.apply(2);
        components[0] = component1;
        components[1] = component2;
        return aggregator.apply(components);
    }

    static <T> T composite(final T[] components,
                           final T noOp,
                           final IntFunction<? extends T[]> arrayFactory,
                           final Function<? super T[], ? extends T> aggregator) {
        return compositeExt(components, noOp, Function.identity(), arrayFactory, aggregator);
    }

    static <T,U extends T> U compositeExt(final T[] components,
                                          final U noOp,
                                          final Function<? super T, ? extends U> cast,
                                          final IntFunction<? extends U[]> arrayFactory,
                                          final Function<? super U[], ? extends U> aggregator) {
        int count = 0;
        for (int i = 0; i < components.length; i++) {
            count += (components[i] == noOp ? 0 : 1);
        }
        if (count == 0) {
            return noOp;
        }
        if (count == 1) {
            for (int i = 0; i < components.length; i++) {
                if (components[i] != noOp) {
                    return cast.apply(components[i]);
                }
            }
            throw new InternalError("Should have exactly 1 non-NOOP component");
        }
        final U[] remaining = arrayFactory.apply(count);
        int index = 0;
        for (int i = 0; i < components.length; i++) {
            if (components[i] != noOp) {
                remaining[index] = cast.apply(components[i]);
                index++;
            }
        }
        assert index == count;
        return aggregator.apply(remaining);
    }
}
