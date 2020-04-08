/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.nobark.run.ThreadLike;

import static java.util.Objects.requireNonNull;

/**
 * Running java app returned by launch methods of {@link Elara}.
 */
public class ElaraRunner implements ThreadLike {

    private final ThreadLike threadLike;

    public ElaraRunner(final ThreadLike threadLike) {
        this.threadLike = requireNonNull(threadLike);
    }

    @Override
    public Thread.State threadState() {
        return threadLike.threadState();
    }

    @Override
    public void join(final long millis) {
        threadLike.join(millis);
    }

    @Override
    public void stop() {
        threadLike.stop();
    }

    @Override
    public String toString() {
        return threadLike.toString();
    }
}