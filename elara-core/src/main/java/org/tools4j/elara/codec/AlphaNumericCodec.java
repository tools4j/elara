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

import static org.tools4j.elara.codec.AlphaNumericEncoding.DIGIT_PREFIXED_ALPHANUMERIC_SIGNED;
import static org.tools4j.elara.codec.Chars.leq;
import static org.tools4j.elara.codec.Chars.setChar;
import static org.tools4j.elara.codec.Ints.stringLength;

/**
 * Alphanumeric codec translating between string and integral representation of a command source.  This codec supports
 * alphanumeric strings of length 6 (or signed 7).
 * <p><br>
 * All integers are valid integer representations.  The string representation of a fully-numeric value is simply the
 * integer's to-string representation.
 * <p><br>
 * Examples of valid and invalid string representations are:
 * <pre>
 *    (V) valid representations are
 *        - a single zero character
 *        - strings with 1-6 digits with no leading zeros
 *        - all alphanumeric strings of length 1-5 with no leading zeros
 *        - all alphanumeric strings of length 1-6 with first char a letter
 *        - all alphanumeric strings of length 1-5 with first char a digit, or 6 chars and value at most '9HYLDS'
 *        - all of the above, except zero, with a sign prefix, where
 *            '-' is the sign for fully-numeric values
 *            '.' is the sign for all other alpha-numeric values
 *    (I) invalid representations are for instance
 *        - empty strings
 *        - strings longer than 7 characters
 *        - strings longer than 6 characters and no sign prefix
 *        - strings containing non-alphanumeric characters other than the sign prefix
 *        - zero-prefixed strings, except for zero itself
 *        - digit only strings with a alphanumeric '.' sign prefix
 *        - alphanumeric strings with at least one letter and a numeric '-' sign prefix
 * </pre>
 * A valid string representation follows one of the following definitions:
 * <pre>
 *     (A+) - 1-6 alphanumeric characters, all letters uppercase and no leading zeros
 *          - char  1 : 'A'-'Z', '1'-'9'
 *          - chars 2+: 'A'-'Z', '0'-'9'
 *          - if first char is '9', then length 1-5 or if length 6, then chars[1-6] &lt;= '9HYLDS'
 *     (A-) - 2-7 sign-prefixed alphanumeric characters, '.' as sign prefix, all letters uppercase and no leading zeros
 *          - char 1 : '.'
 *          - char 2 : 'A'-'Z", '1'-'9'
 *          - char 3+: 'A'-'Z', '0'-'9'
 *          - at least one char: 'A'-'Z'
 *          - if second char is '9', then length 2-6 or if length 7, then chars[2-7] &lt;= '9HYLDT'
 *     (Z)  - single zero digit character
 *          - char 1: '0'
 *     (N+) - 1-6 digit characters without leading zeros
 *          - char 1 : '1'-'9'
 *          - char 2+: '0'-'9'
 *     (N-) - 2-7 sign-prefixed digit characters: '-' sign followed by 1-6 digits but no leading zeros
 *          - char 1 : '-'
 *          - char 2 : '1'-'9'
 *          - char 3+: '0'-'9'
 * </pre>
 */
public class AlphaNumericCodec implements ShortStringCodec {
    public static final int MAX_LENGTH_UNSIGNED = 6;
    public static final int MAX_LENGTH_SIGNED = 7;
    /**
     * 0-999999
     */
    private static final int NUMERIC_BLOCK_LENGTH = 1_000_000;
    public static final int MIN_NUMERIC = -(NUMERIC_BLOCK_LENGTH - 1);
    public static final int MAX_NUMERIC = NUMERIC_BLOCK_LENGTH - 1;
    /**
     * [A-Z] + [A-Z][0-9A-Z] + [A-Z][0-9A-Z][0-9A-Z] ...
     * 26 + 26*36 + 26*36*36 + 26*36*36*36 + 26*36*36*36*36 + 26*36*36*36*36*36
     */
    private static final int ALPHA_PREFIX_BLOCK_LENGTH = 26*(1 + 36*(1 + 36*(1 + 36*(1 + 36*(1 + 36)))));

    /**
     * [1-9] + [1-9][0-9A-Z] + [1-9][0-9A-Z][0-9A-Z] ...
     * 9 + 9*36 + 9*36*36 + 9*36*36*36 + 9*36*36*36*36 + 9*36*36*36*36*36 = 559,744,029
     *
     * but we have left only 529,445,341, so we can encode ~94.587% of all possible digit-prefixed values
     */
    private static final int DIGIT_PREFIX_BLOCK_LENGTH = Integer.MAX_VALUE - NUMERIC_BLOCK_LENGTH - ALPHA_PREFIX_BLOCK_LENGTH;
    public static final String MAX_DIGIT_PREFIXED_ALPHANUMERIC = "9HYLDS";
    public static final String MIN_DIGIT_PREFIXED_ALPHANUMERIC = ".9HYLDT";

    @Override
    public int toInt(final CharSequence source) {
        return sourceToInt(source);
    }

    public static int sourceToInt(final CharSequence source) {
        final AlphaNumericEncoding encoding = AlphaNumericEncoding.encodingFor(source);
        final int len = source.length();
        final int off = encoding.isSigned() ? 1 : 0;
        int code;
        if (encoding.isNumeric()) {
            code = fromDigit(source.charAt(off), source);
            for (int i = off + 1; i < len; i++) {
                //really: code *= 10
                code = (code << 3) + (code << 1);
                code += fromDigit(source.charAt(i), source);
            }
            return off == 0 ? code : -code;
        }
        if (encoding.isAlphaPrefixAlphanumeric()) {
            code = fromAlpha0(source.charAt(off), source);
            for (int i = off + 1; i < len; i++) {
                //really: code = code * 36 + 26
                code = (code << 5) + (code << 2) + 26;
                code += fromAlphanumeric(source.charAt(i), source);
            }
            code += NUMERIC_BLOCK_LENGTH;
        } else if (encoding.isDigitPrefixAlphanumeric()) {
            code = fromDigit0(source.charAt(off), source);
            for (int i = off + 1; i < len; i++) {
                //really: code = code * 36 + 9
                code = (code << 5) + (code << 2) + 9;
                code += fromAlphanumeric(source.charAt(i), source);
            }
            code += NUMERIC_BLOCK_LENGTH + ALPHA_PREFIX_BLOCK_LENGTH;
        } else {
            throw new IllegalArgumentException(len == 0 ? "Empty source string" : "Invalid source string: " + source);
        }
        assert code >= 0 || (encoding == DIGIT_PREFIXED_ALPHANUMERIC_SIGNED && code == Integer.MIN_VALUE);
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
        if (source > -NUMERIC_BLOCK_LENGTH && source < NUMERIC_BLOCK_LENGTH) {
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
        } else if (source > -(NUMERIC_BLOCK_LENGTH + ALPHA_PREFIX_BLOCK_LENGTH) &&
                source < (NUMERIC_BLOCK_LENGTH + ALPHA_PREFIX_BLOCK_LENGTH)) {
            sign = '.';
            val -= NUMERIC_BLOCK_LENGTH;
            for (int i = 5; i >= 0; i--) {
                if (val < 26) {
                    final char ch = toAlpha0(val);
                    setChar(ch, dst, off + i);
                    start = off + i;
                    break;
                }
                val -= 26;
                final char ch = toAlphanumeric(val);
                setChar(ch, dst, off + i);
                val /= 36;
            }
        } else {
            sign = '.';
            val -= (NUMERIC_BLOCK_LENGTH + ALPHA_PREFIX_BLOCK_LENGTH);
            for (int i = 5; i >= 0; i--) {
                if (val < 9) {
                    final char ch = toDigit0(val);
                    setChar(ch, dst, off + i);
                    start = off + i;
                    break;
                }
                val -= 9;
                final char ch = toAlphanumeric(val);
                setChar(ch, dst, off + i);
                val /= 36;
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
        if (len < 1 || len > MAX_LENGTH_SIGNED) {
            return false;
        }
        final AlphaNumericEncoding encoding = AlphaNumericEncoding.encodingFor(seq);
        switch (encoding) {
            case NUMERIC_UNSIGNED:
            case ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED:
                return len <= MAX_LENGTH_UNSIGNED;
            case NUMERIC_SIGNED:
            case ALPHA_PREFIXED_ALPHANUMERIC_SIGNED:
                return true;
            case DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED:
                return len < MAX_LENGTH_UNSIGNED ||
                        (len == MAX_LENGTH_UNSIGNED && leq(seq, MIN_DIGIT_PREFIXED_ALPHANUMERIC));
            case DIGIT_PREFIXED_ALPHANUMERIC_SIGNED:
                return len < MAX_LENGTH_SIGNED ||
                        (/*len == MAX_LENGTH_SIGNED && */ leq(seq, MAX_DIGIT_PREFIXED_ALPHANUMERIC));
            default:
                return false;
        }
    }

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

    private static char toAlpha0(final int value) {
        final int code = value % 26;
        return (char)(code + 'A');
    }

    private static char toDigit0(final int value) {
        final int code = value % 9;
        return (char)(code + '1');
    }

    private static char toAlphanumeric(final int value) {
        final int code = value % 36;
        return (char)(code + (code < 10 ? '0' : 'A' - 10));
    }

    private static char toDigit(final int value) {
        final int code = value % 10;
        return (char)(code + '0');
    }

    private static int fromAlpha0(final char ch, final CharSequence seq) {
        if ('A' <= ch && ch <= 'Z') {
            return ch - 'A';
        }
        throw new IllegalArgumentException("Illegal first character '" + ch + "' in source string: " + seq);
    }

    private static int fromDigit0(final char ch, final CharSequence seq) {
        if ('1' <= ch && ch <= '9') {
            return ch - '1';
        }
        throw new IllegalArgumentException("Illegal first character '" + ch + "' in source string: " + seq);
    }

    private static int fromDigit(final char ch, final CharSequence seq) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        throw new IllegalArgumentException("Illegal character '" + ch + "' in source string: " + seq);
    }

    private static int fromAlphanumeric(final char ch, final CharSequence seq) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'Z') {
            return 10 + ch - 'A';
        }
        throw new IllegalArgumentException("Illegal character '" + ch + "' in source string: " + seq);
    }

    @Override
    public String toString() {
        return AlphaNumericCodec.class.getSimpleName();
    }
}
