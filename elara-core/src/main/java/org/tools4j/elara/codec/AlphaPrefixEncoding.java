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

enum AlphaPrefixEncoding {
    NUMERIC_UNSIGNED,
    NUMERIC_SIGNED,
    ALPHANUMERIC_UNSIGNED,
    ALPHANUMERIC_SIGNED,
    INVALID;

    public boolean isSigned() {
        return this == NUMERIC_SIGNED || this == ALPHANUMERIC_SIGNED;
    }

    public boolean isNumeric() {
        return this == NUMERIC_UNSIGNED || this == NUMERIC_SIGNED;
    }

    public boolean isAlphanumeric() {
        return this == ALPHANUMERIC_UNSIGNED || this == ALPHANUMERIC_SIGNED;
    }

    static AlphaPrefixEncoding encodingFor(final CharSequence seq) {
        final int len = seq.length();
        if (len == 0) {
            return AlphaPrefixEncoding.INVALID;
        }
        final CharType charType = CharType.forChar(seq.charAt(0));
        if (len == 1) {
            return forSingleChar(charType);
        }
        AlphaPrefixEncoding type = forMultiChar(charType, seq.charAt(1));
        for (int i = 2; i < len; i++) {
            type = type.thenChar(seq.charAt(i));
        }
        return type;
    }

    private AlphaPrefixEncoding thenChar(final char ch) {
        switch (this) {
            case NUMERIC_UNSIGNED://fallthrough
            case NUMERIC_SIGNED:
                if ('0' <= ch && ch <= '9') {
                    return this;
                }
                return INVALID;
            case ALPHANUMERIC_UNSIGNED://fallthrough
            case ALPHANUMERIC_SIGNED:
                if (('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9')) {
                    return this;
                }
                return INVALID;
            default:
                return INVALID;
        }
    }


    private static AlphaPrefixEncoding forSingleChar(final CharType charType) {
        switch (charType) {
            case ALPHA:
                return AlphaPrefixEncoding.ALPHANUMERIC_UNSIGNED;
            case ZERO_DIGIT://fallthrough
            case NON_ZERO_DIGIT:
                return AlphaPrefixEncoding.NUMERIC_UNSIGNED;
            default:
                return AlphaPrefixEncoding.INVALID;
        }
    }

    private static AlphaPrefixEncoding forMultiChar(final CharType first, final char then) {
        switch (first) {
            case ALPHA:
                if (('A' <= then && then <= 'Z') || ('0' <= then && then <= '9')) {
                    return AlphaPrefixEncoding.ALPHANUMERIC_UNSIGNED;
                }
                return AlphaPrefixEncoding.INVALID;
            case NON_ZERO_DIGIT:
                if ('0' <= then && then <= '9') {
                    return AlphaPrefixEncoding.NUMERIC_UNSIGNED;
                }
                return AlphaPrefixEncoding.INVALID;
            case ALPHANUMERIC_SIGN:
                if ('A' <= then && then <= 'Z') {
                    return AlphaPrefixEncoding.ALPHANUMERIC_SIGNED;
                }
                return AlphaPrefixEncoding.INVALID;
            case NUMERIC_SIGN:
                if ('1' <= then && then <= '9') {
                    return AlphaPrefixEncoding.NUMERIC_SIGNED;
                }
                return AlphaPrefixEncoding.INVALID;
            case ZERO_DIGIT://fallthrough
            default:
                return AlphaPrefixEncoding.INVALID;
        }
    }
}
