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
package org.tools4j.elara.run;

import org.tools4j.elara.codec.AlphaNumericCodec;
import org.tools4j.elara.codec.ShortStringCodec;

import java.util.function.Supplier;

public enum ElaraSystemProperties {
    ;
    /**
     * System property that can be used to specify a source-ID codec, a fully qualified name of a class that implements
     * {@link org.tools4j.elara.codec.ShortStringCodec ShortStringCodec} and has a zero-arg constructor.
     */
    public static final String SOURCE_ID_CODEC_PROPERTY = "or.tools4j.elara.source-id-codec";
    /**
     * Default codec supplier used if no {@link #SOURCE_ID_CODEC_PROPERTY} is provided in system properties.
     */
    public static final Supplier<? extends ShortStringCodec> SOURCE_ID_CODEC_DEFAULT = AlphaNumericCodec::new;

    public static String getString(final String name, final String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    public static int getInt(final String name, final int defaultValue) {
        final String value = System.getProperty(name, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final Exception e) {
            System.err.printf("Parsing of system property [%s=%s] failed with [error=%s], using default: %s\n", name,
                    value, e, defaultValue);
            return defaultValue;
        }
    }

    public static <T> T getInstance(final String name, final Class<T> type, final Supplier<? extends T> defaultValue) {
        final String value = System.getProperty(name, null);
        if (value == null) {
            return defaultValue.get();
        }
        try {
            return type.cast(Class.forName(value).newInstance());
        } catch (final Exception e) {
            System.err.printf("Instantiation failed for [%s] specified in system property [%s], using default [error=%s]\n",
                    name, value, e);
            return defaultValue.get();
        }
    }
}
