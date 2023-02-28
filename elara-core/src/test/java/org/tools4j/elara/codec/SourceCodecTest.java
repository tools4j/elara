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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link ShortStringCodec} and implementations.
 */
class SourceCodecTest {

    private static final int MAX_LEN = 6;
    private long count;

    @FunctionalInterface
    private interface CharSupplier {
        char get(int charIndex, int run);
    }

    @BeforeEach
    void resetCount() {
        count = 0;
    }

    static Stream<Arguments> sourceCodecs() {
        return Stream.of(
                Arguments.of(new AlphaPrefixCodec(), AlphaPrefixCodec.MIN_NUMERIC, AlphaPrefixCodec.MAX_NUMERIC),
                Arguments.of(new AlphaNumericCodec(), AlphaNumericCodec.MIN_NUMERIC, AlphaNumericCodec.MAX_NUMERIC)
        );
    }


    @AfterEach
    void printCount(final TestInfo testInfo) {
        System.out.printf("%s(%s): %s tests\n",
                testInfo.getTestMethod().map(Method::getName).orElse("???"),
                testInfo.getDisplayName(),
                count);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceCodecs")
    void randomFromTo(final ShortStringCodec codec) {
        final Random rnd = new Random();
        final int iterationsPerTest = MAX_LEN == 5 ? 200 : 20;
        final int firstChars = 5;
        final int nextChars = 8;
        final IntUnaryOperator alphaNum = i -> (i < 10 ? '0' : 'A' - 10) + i;
        count = fromTo(codec, iterationsPerTest,
                (charIndex, run) -> run < firstChars ? (char)('A' + rnd.nextInt(26)) : '\0',
                (charIndex, run) -> run < nextChars ? (char)alphaNum.applyAsInt(rnd.nextInt(36)) : '\0'
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceCodecs")
    void specialFromTo(final ShortStringCodec codec) {
        final int iterationsPerTest = 1;
        final char[] firstChars = {'A', 'B', 'F', 'Y', 'Z'};
        final char[] nextChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'C', 'W', 'Z'};
        count = fromTo(codec, iterationsPerTest,
                (charIndex, run) -> run < firstChars.length ? firstChars[run] : '\0',
                (charIndex, run) -> run < nextChars.length ? nextChars[run] : '\0'
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceCodecs")
    void specialToFrom(final ShortStringCodec codec, final int minNumeric, final int maxNumeric) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= 1_000_000; i++) {
            testToFrom(codec, i, builder);
            testToFrom(codec, -i, builder);
            testToFrom(codec, maxNumeric - i, builder);
            testToFrom(codec, minNumeric + i, builder);
            testToFrom(codec, Integer.MAX_VALUE - i, builder);
            testToFrom(codec, Integer.MIN_VALUE + i, builder);
            count += 6;
        }
        count--;//one test double counted for zero
    }

    private void testToFrom(final ShortStringCodec codec, final int source, final StringBuilder builder) {
        builder.setLength(0);
        codec.toString(source, builder);
        final int from = codec.toInt(builder);
        assertEquals(source, from, source + " >> " + builder + " >> " + from);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceCodecs")
    void digitsFromTo(final ShortStringCodec codec, final int minNumeric, final int maxNumeric) {
        final StringBuilder builder = new StringBuilder();
        int inc;
        for (int intSource = minNumeric; intSource <= maxNumeric; intSource += inc) {
            final String strSource = String.valueOf(intSource);
            builder.setLength(0);

            assertEquals(strSource, codec.toString(intSource, builder).toString());
            assertEquals(intSource, codec.toInt(strSource));
            count++;

            //let's do small increments around 0 and at the boundaries
            inc = (Math.abs(intSource) <= 1_000_000 || (maxNumeric - Math.abs(intSource) <= 1_000_000)) ?
                    1 : 100;
        }
    }

    long fromTo(final ShortStringCodec codec,
                final int iterationsPerTest,
                final CharSupplier firstCharSupplier,
                final CharSupplier nextCharSuppliers) {
        long count = 0;

        final char[] chars = new char[7];
        for (int len = 1; len <= MAX_LEN; len++) {
            final int offset = 7 - len;
            chars[offset - 1] = '.';
            for (int i = 0; i < 26; i++) {
                final char ch = firstCharSupplier.get(0, i);
                if (ch == '\0') {
                    break;
                }
                chars[offset] = ch;
                count += testFromTo(codec, iterationsPerTest, nextCharSuppliers, chars, 1, len);
            }
        }
        return count;
    }

    private long testFromTo(final ShortStringCodec codec,
                            final int iterationsPerTest,
                            final CharSupplier nextCharSuppliers,
                            final char[] chars, final int index, final int len) {
        long count = 0;
        final int offset = 7 - len;
        if (index < len) {
            for (int k = 0; k < 36; k++) {
                final char ch = nextCharSuppliers.get(index, k);
                if (ch == '\0') {
                    break;
                }
                chars[offset + index] = ch;
                count += testFromTo(codec, iterationsPerTest, nextCharSuppliers, chars, index + 1, len);
            }
        } else {
            final String pos = String.valueOf(chars, offset, len);
            final String neg = String.valueOf(chars, offset - 1, len + 1);
            final boolean zeroStr = "0".equals(pos);
            final StringBuilder result = new StringBuilder();

            for (int i = 0; i < iterationsPerTest; i++) {
                final int posSrc = codec.toInt(pos);
                final int negSrc = codec.toInt(neg);
                assertTrue(posSrc >= 0, pos + " >> " + posSrc + " >= 0");
                assertTrue(posSrc > 0 || zeroStr, pos + " >> " + posSrc + " > 0 || " + pos + " == \"0\"");
                assertTrue(negSrc < 0, neg + " >> " + negSrc + " < 0");

                result.setLength(0);
                codec.toString(posSrc, result);
                assertTrue(eq(pos, result), pos + " >> " + posSrc + " >> " + result);
                count++;

                if (zeroStr) continue;

                result.setLength(0);
                codec.toString(negSrc, result);
                assertTrue(eq(neg, result), neg + " >> " + negSrc + " >> " + result);
                count++;
            }
        }
        return count;
    }

    private static boolean eq(final CharSequence a, final CharSequence b) {
        final int len;
        if ((len = a.length()) != b.length()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}