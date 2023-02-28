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

enum AlphaNumericEncoding {
    NUMERIC_UNSIGNED,
    NUMERIC_SIGNED,
    ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED,
    ALPHA_PREFIXED_ALPHANUMERIC_SIGNED,
    DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED,
    DIGIT_PREFIXED_ALPHANUMERIC_SIGNED,
    INVALID;

    public boolean isSigned() {
        return this == NUMERIC_SIGNED ||
                this == ALPHA_PREFIXED_ALPHANUMERIC_SIGNED ||
                this == DIGIT_PREFIXED_ALPHANUMERIC_SIGNED;
    }

    public boolean isNumeric() {
        return this == NUMERIC_UNSIGNED || this == NUMERIC_SIGNED;
    }

    public boolean isAlphaPrefixAlphanumeric() {
        return this == ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED || this == ALPHA_PREFIXED_ALPHANUMERIC_SIGNED;
    }

    public boolean isDigitPrefixAlphanumeric() {
        return this == DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED || this == DIGIT_PREFIXED_ALPHANUMERIC_SIGNED;
    }

    static AlphaNumericEncoding encodingFor(final CharSequence seq) {
        final int len = seq.length();
        if (len == 0) {
            return AlphaNumericEncoding.INVALID;
        }
        final CharType charType = CharType.forChar(seq.charAt(0));
        if (len == 1) {
            return forSingleChar(charType);
        }
        AlphaNumericEncoding type = forMultiChar(charType, seq.charAt(1));
        for (int i = 2; i < len; i++) {
            type = type.thenChar(seq.charAt(i));
        }
        return type;
    }

    private AlphaNumericEncoding thenChar(final char ch) {
        switch (this) {
            case NUMERIC_UNSIGNED://fallthrough
                if ('0' <= ch && ch <= '9') {
                    return this;
                }
                if ('A' <= ch && ch <= 'Z') {
                    return DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED;
                }
                return INVALID;
            case NUMERIC_SIGNED:
                if ('0' <= ch && ch <= '9') {
                    return this;
                }
                if ('A' <= ch && ch <= 'Z') {
                    return DIGIT_PREFIXED_ALPHANUMERIC_SIGNED;
                }
                return INVALID;
            case ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED:  //fallthrough
            case ALPHA_PREFIXED_ALPHANUMERIC_SIGNED:    //fallthrough
            case DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED:  //fallthrough
            case DIGIT_PREFIXED_ALPHANUMERIC_SIGNED:
                if (('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9')) {
                    return this;
                }
                return INVALID;
            default:
                return INVALID;
        }
    }


    private static AlphaNumericEncoding forSingleChar(final CharType charType) {
        switch (charType) {
            case ALPHA:
                return AlphaNumericEncoding.ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED;
            case ZERO_DIGIT://fallthrough
            case NON_ZERO_DIGIT:
                return AlphaNumericEncoding.NUMERIC_UNSIGNED;
            default:
                return AlphaNumericEncoding.INVALID;
        }
    }

    private static AlphaNumericEncoding forMultiChar(final CharType first, final char then) {
        switch (first) {
            case ALPHA:
                if (('A' <= then && then <= 'Z') || ('0' <= then && then <= '9')) {
                    return AlphaNumericEncoding.ALPHA_PREFIXED_ALPHANUMERIC_UNSIGNED;
                }
                return AlphaNumericEncoding.INVALID;
            case NON_ZERO_DIGIT:
                if ('0' <= then && then <= '9') {
                    return AlphaNumericEncoding.NUMERIC_UNSIGNED;
                }
                if ('A' <= then && then <= 'Z') {
                    return AlphaNumericEncoding.DIGIT_PREFIXED_ALPHANUMERIC_UNSIGNED;
                }
                return AlphaNumericEncoding.INVALID;
            case ALPHANUMERIC_SIGN:
                if ('A' <= then && then <= 'Z') {
                    return AlphaNumericEncoding.ALPHA_PREFIXED_ALPHANUMERIC_SIGNED;
                }
                if ('1' <= then && then <= '9') {
                    return AlphaNumericEncoding.DIGIT_PREFIXED_ALPHANUMERIC_SIGNED;
                }
                return AlphaNumericEncoding.INVALID;
            case NUMERIC_SIGN:
                if ('1' <= then && then <= '9') {
                    return AlphaNumericEncoding.NUMERIC_SIGNED;
                }
                return AlphaNumericEncoding.INVALID;
            case ZERO_DIGIT://fallthrough
            default:
                return AlphaNumericEncoding.INVALID;
        }
    }
}
