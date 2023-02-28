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
package org.tools4j.elara.codec;

enum Chars {
    ;
    static void setChar(final char ch, final StringBuilder dst, final int dstIndex) {
        if (dstIndex < dst.length()) {
            dst.setCharAt(dstIndex, ch);
        } else {
            while (dstIndex > dst.length()) {
                dst.append('0');
            }
            dst.append(ch);
        }
    }

    /**
     * Note: a shorter string is always considered before a longer string.
     * @param a sequence a
     * @param b sequence b
     * @return true if a {@code a <= b}
     */
    static boolean leq(final CharSequence a, final CharSequence b) {
        final int len = a.length();
        if (len < b.length()) {
            return true;
        }
        if (len > b.length()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            final int cmp;
            if ((cmp = Character.compare(a.charAt(i), b.charAt(i))) != 0) {
                return cmp < 0;
            }
        }
        //equal
        return true;
    }
}
