/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.hash;

import org.junit.jupiter.api.Test;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication.DefaultState;
import org.tools4j.elara.samples.hash.HashApplication.ModifiableState;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tools4j.elara.samples.hash.HashApplication.NULL_VALUE;

/**
 * Unit test running the {@link HashApplication}.
 */
public class HashApplicationTest {

    @Test
    public void inMemory() throws Exception {
        //given
        final int n = 200;
        final AtomicLong input = new AtomicLong(NULL_VALUE);
        final ModifiableState state = new DefaultState();
        final Random random = new Random(123);
        final long expected = 6244545253611137478L;

        //when
        try (final ElaraRunner runner = HashApplication.inMemory(state, input)) {
            runHashApp(n, random, input, runner);
        }

        //then
        assertEquals(expected, state.hash(), "state.hash(" + n + ")");
    }

    @Test
    public void chronicleQueue() throws Exception {
        //given
        final int n = 200;
        final AtomicLong input = new AtomicLong(NULL_VALUE);
        final ModifiableState state = new DefaultState();
        final Random random = new Random(123);
        final long expected = 6244545253611137478L;

        //when
        try (final ElaraRunner runner = HashApplication.chronicleQueue(state, input)) {
            runHashApp(n, random, input, runner);
        }

        //then
        assertEquals(expected, state.hash(), "state.hash(" + n + ")");
    }

    @Test
    public void chronicleQueueWithMetrics() throws Exception {
        //given
        final int n = 5000;
        final AtomicLong input = new AtomicLong(NULL_VALUE);
        final ModifiableState state = new DefaultState();
        final Random random = new Random(123);
        final long expected = -6817377618556807803L;

        //when
        try (final ElaraRunner runner = HashApplication.chronicleQueueWithMetrics(state, input)) {
            runHashApp(n, random, input, runner);
        }

        //then
        assertEquals(expected, state.hash(), "state.hash(" + n + ")");
    }

    private static void runHashApp(final int n,
                                   final Random random,
                                   final AtomicLong input,
                                   final ElaraRunner runner) throws InterruptedException {
        long value = random.nextLong();
        for (int i = 0; i < n; ) {
            if (input.compareAndSet(NULL_VALUE, value)) {
                value = random.nextLong();
                i++;
            }
            Thread.sleep(1);
        }
        while (input.get() != NULL_VALUE) {
            Thread.sleep(50);
        }
        runner.join(200);
    }

}