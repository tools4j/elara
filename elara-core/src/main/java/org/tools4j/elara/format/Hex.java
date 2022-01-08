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
package org.tools4j.elara.format;

import org.agrona.DirectBuffer;

public enum Hex {
    ;

    private static final char[] HEX_CHARS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static final char BYTE_SPACER = ' ';
    private static final String[] DECIMAL_SEPARATORS = {" : ", " :: ", " ::: "};

    public static String hex(final DirectBuffer buffer) {
        return hex(buffer, 0, buffer.capacity());
    }

    public static String hex(final DirectBuffer buffer, final int offset, final int length) {
        return hex(buffer, offset, length, true, true);
    }

    public static String hex(final DirectBuffer buffer, final int offset, final int length,
                             final boolean byteSeparators,
                             final boolean decimalSeparators) {
        final StringBuilder sb = new StringBuilder(length * 3);
        for (int i = 0; i < length; i++) {
            final byte b = buffer.getByte(offset + i);
            final int b0 = b & 0xf;
            final int b1 = (b >>> 4) & 0xf;
            if (i > 0) {
                appendSpacer(sb, i, byteSeparators, decimalSeparators);
            }
            sb.append(HEX_CHARS[b0]);
            sb.append(HEX_CHARS[b1]);
        }
        return sb.toString();
    }

    private static void appendSpacer(final StringBuilder sb, final int index,
                                     final boolean byteSeparators,
                                     final boolean decimalSeparators) {
        if (decimalSeparators && (index % 10) == 0) {
            if ((index % 100) == 0) {
                sb.append((index % 1000) == 0 ? DECIMAL_SEPARATORS[2] : DECIMAL_SEPARATORS[1]);
            } else {
                sb.append(DECIMAL_SEPARATORS[0]);
            }
        } else if (byteSeparators) {
            sb.append(BYTE_SPACER);
        }
    }
}
