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
package org.tools4j.elara.command;

import org.tools4j.elara.codec.ShortStringCodec;
import org.tools4j.elara.run.ElaraSystemProperties;

import static org.tools4j.elara.run.ElaraSystemProperties.SOURCE_ID_CODEC_DEFAULT;
import static org.tools4j.elara.run.ElaraSystemProperties.SOURCE_ID_CODEC_PROPERTY;

public enum SourceIds {
    ;
    private static final ShortStringCodec codec = ElaraSystemProperties.getInstance(SOURCE_ID_CODEC_PROPERTY,
            ShortStringCodec.class, SOURCE_ID_CODEC_DEFAULT);

    public static boolean isAdmin(final int sourceId) {
        return sourceId < 0;
    }

    public static boolean isAdmin(final CharSequence sourceId) {
        return codec.isNegative(sourceId);
    }

    public static boolean isApplication(final int sourceId) {
        return sourceId >= 0;
    }

    public static boolean isApplication(final CharSequence sourceId) {
        return !isAdmin(sourceId);
    }

    public static int toInt(final CharSequence sourceId) {
        return codec.toInt(sourceId);
    }

    public static int toInt(final CharSequence sourceId, final int defaultValue) {
        return codec.isValid(sourceId) ? codec.toInt(sourceId) : defaultValue;
    }

    public static StringBuilder toString(final int sourceId, final StringBuilder dst) {
        return codec.toString(sourceId, dst);
    }

    public static String toString(final int sourceId) {
        return codec.toString(sourceId);
    }

    /**
     * Method that can be used in StringBuilder append sequences.  The method appends the sourceId string to the provided
     * string builder and returns an empty string that is subsequently passed to the builder's append method in the
     * concatenation sequence.
     * <br><br>
     * To illustrate consider the following string concatenation example:
     * <pre>
     *     StringBuilder builder = new StringBuilder():
     *     builder.append("sourceId=");
     *     Sources.toString(123, builder);
     * </pre>
     * This can now be written as
     * <pre>
     *     StringBuilder builder = new StringBuilder():
     *     builder.append("sourceId=").append(Sources.appending(123, builder));
     * </pre>
     *
     * @param sourceId the sourceId to encode
     * @param dst   the destination string builder
     * @return an empty string
     */
    public static Object appending(final int sourceId, final StringBuilder dst) {
        toString(sourceId, dst);
        return "";
    }

}
