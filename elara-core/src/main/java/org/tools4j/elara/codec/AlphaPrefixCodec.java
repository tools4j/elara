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

import static org.tools4j.elara.codec.Chars.leq;
import static org.tools4j.elara.codec.Chars.setChar;
import static org.tools4j.elara.codec.Ints.stringLength;

/**
 * Alpha-prefix codec translating between string and integral representation of a command source.  This codec supports
 * alphanumeric strings of length 6 (or signed 7).  Fully numeric strings between -514,896,896 and -514,896,895 are also
 * supported.
 * <p><br>
 * All integers are valid integer representations.  The string representation of a fully-numeric value is simply the
 * integer's to-string representation.  While fully-numeric string values use the standard '-' sign character for
 * negative values, alphanumeric negative string values are prefixed with a '.' character instead.
 * <p><br>
 * A valid string representation follows one of the following definitions:
 * <pre>
 *     (A+) - 1-6 alphanumeric characters, all letters uppercase and no leading digit
 *          - char 1 : 'A'-'Z'
 *          - char 2+: 'A'-'Z', '0'-'9'
 *     (A-) - 2-7 sign-prefixed alphanumeric characters, '.' as sign prefix, all letters uppercase and no leading digit
 *          - char 1 : '.'
 *          - char 2 : 'A'-'Z'
 *          - char 3+: 'A'-'Z', '0'-'9'
 *     (Z)  - single zero digit character
 *          - char 1: '0'
 *     (N+) - 1-8 digit characters
 *          - char 1 : '1'-'9'
 *          - char 2+: '0'-'9'
 *     (N-) - 2-9 sign-prefixed digit characters: '-' sign followed by 1-8 digits
 *          - char 1 : '-'
 *          - char 2 : '1'-'9'
 *          - char 3+: '0'-'9'
 *     (L+) - 9 digit characters defining a value from '100000000' to '514896895'
 *          - char 1  : '1'-'5'
 *          - char 2-9: '0'-'9'
 *          - chars[1-9] &lt;= '514896895'
 *     (L-) - 10 sign-prefixed digit characters: '-' sign + 9 digits defining a value from '-100000000' to '-514896896'
 *          - char 1   : '-'
 *          - char 2   : '1'-'5'
 *          - char 3-10: '0'-'9'
 *          - chars[1-10] &gt;= '-514896896'
 * </pre>
 */
public class AlphaPrefixCodec implements ShortStringCodec {
    public static final int MAX_NUMERIC = 514_896_895;  //  (2^31 - 27*36^5) - 1
    public static final int MIN_NUMERIC = -514_896_896; // -(2^31 - 27*36^5)
    public static final String MAX_NUMERIC_STRING = String.valueOf(MAX_NUMERIC);
    public static final String MIN_NUMERIC_STRING = String.valueOf(MIN_NUMERIC);
    private static final int ALPHA_OFFSET = MAX_NUMERIC + 1;
    private static final int MAX_LENGTH_ALPHANUMERIC = 6;
    private static final int MAX_LENGTH_NUMERIC = 9;

    @Override
    public int toInt(final CharSequence source) {
        return sourceToInt(source);
    }

    public static int sourceToInt(final CharSequence source) {
        final AlphaPrefixEncoding encoding = AlphaPrefixEncoding.encodingFor(source);
        final int len = source.length();
        final int off = encoding.isSigned() ? 1 : 0;
        int code;
        if (encoding.isAlphanumeric()) {
            code = fromChar0(source.charAt(off), source);
            for (int i = off + 1; i < len; i++) {
                //really: code *= 36
                code = (code << 5) + (code << 2);
                code += fromChar(source.charAt(i), source);
            }
            code += ALPHA_OFFSET + off;
        } else if (encoding.isNumeric()) {
            code = fromDigit(source.charAt(off), source);
            for (int i = off + 1; i < len; i++) {
                //really: code *= 10
                code = (code << 3) + (code << 1);
                code += fromDigit(source.charAt(i), source);
            }
        } else {
            throw new IllegalArgumentException(len == 0 ? "Empty source string" : "Invalid source string: " + source);
        }
        return off == 0 ? code : -code;
    }

    @Override
    public StringBuilder toString(final int source, final StringBuilder dst) {
        return sourceToString(source, dst);
    }

    public static StringBuilder sourceToString(final int source, final StringBuilder dst) {
        final char sign;
        int val = Math.abs(source);
        int off = dst.length();
        int start = off;
        if (source < -ALPHA_OFFSET || source >= ALPHA_OFFSET) {
            sign = '.';
            val -= (source >= 0 ? ALPHA_OFFSET : ALPHA_OFFSET + 1);
            for (int i = 5; i >= 0; i--) {
                final char ch = toChar(val);
                setChar(ch, dst, off + i);
                val /= 36;
                if (val == 0) {
                    dst.setCharAt(off + i, toChar0(fromChar(ch, dst)));
                    start = off + i;
                    break;
                }
            }
        } else {
            sign = '-';
            final int len = stringLength(val);
            for (int i = len - 1; i >= 0; i--) {
                final char ch = toDigit(val);
                setChar(ch, dst, off + i);
                val /= 10;
                if (val == 0) {
                    start = off + i;
                    break;
                }
            }
        }
        if (source < 0) {
            start--;
            if (start < off) {
                dst.insert(off, sign);
                return dst;
            }
            dst.setCharAt(start, sign);
        }
        return dst.delete(off, start);
    }

    @Override
    public boolean isValid(final CharSequence seq) {
        return isSourceValid(seq);
    }

    public static boolean isSourceValid(final CharSequence seq) {
        final int len = seq.length();
        if (len < 1 || len > 1 + MAX_LENGTH_NUMERIC) {
            return false;
        }
        final AlphaPrefixEncoding encoding = AlphaPrefixEncoding.encodingFor(seq);
        switch (encoding) {
            case NUMERIC_UNSIGNED:
                return len < MAX_LENGTH_NUMERIC ||
                        (len == MAX_LENGTH_NUMERIC && leq(seq, MAX_NUMERIC_STRING));
            case NUMERIC_SIGNED:
                return len < 1 + MAX_LENGTH_NUMERIC ||
                        (/*len == 1 + MAX_LENGTH_NUMERIC && */ leq(seq, MIN_NUMERIC_STRING));
            case ALPHANUMERIC_UNSIGNED:
                return len <= MAX_LENGTH_ALPHANUMERIC;
            case ALPHANUMERIC_SIGNED:
                return len <= 1 + MAX_LENGTH_ALPHANUMERIC;
            default:
                return false;
        }
    }

    @Override
    public boolean isNegative(final CharSequence source) {
        return isSourceNegative(source);
    }

    public static boolean isSourceNegative(final CharSequence source) {
        if (source.length() > 0) {
            final char first = source.charAt(0);
            return first == '.' | first == '-';
        }
        return false;
    }

    private static char toChar0(final int value) {
        final int code = value % 27;
        return (char) (code + (code == 0 ? '0' : 'A' - 1));
    }

    private static char toChar(final int value) {
        final int code = value % 36;
        return (char) (code + (code < 10 ? '0' : 'A' - 10));
    }

    private static char toDigit(final int value) {
        final int code = value % 10;
        return (char) (code + '0');
    }

    private static int fromChar0(final char ch, final CharSequence source) {
        if ('0' == ch) {
            return 0;
        }
        if ('A' <= ch && ch <= 'Z') {
            return 1 + ch - 'A';
        }
        throw new IllegalArgumentException("Illegal first character '" + ch + "' in source string: " + source);
    }

    private static int fromDigit(final char ch, final CharSequence source) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        throw new IllegalArgumentException("Illegal character '" + ch + "' in source string: " + source);
    }

    private static int fromChar(final char ch, final CharSequence source) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'Z') {
            return 10 + ch - 'A';
        }
        throw new IllegalArgumentException("Illegal character '" + ch + "' in source string: " + source);
    }

    @Override
    public String toString() {
        return AlphaPrefixCodec.class.getSimpleName();
    }
}
