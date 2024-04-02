/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.concurrent.AgentRunner;

import java.lang.Thread.State;
import java.util.concurrent.locks.LockSupport;

import static java.util.Objects.requireNonNull;

/**
 * Running java app returned by launch methods of {@link Elara}.
 */
public class ElaraRunner implements AutoCloseable {

    public static final int START_TIMEOUT_MILLIS = 5000;

    private final AgentRunner agentRunner;

    public ElaraRunner(final AgentRunner agentRunner) {
        this.agentRunner = requireNonNull(agentRunner);
    }

    public static ElaraRunner startOnThread(final AgentRunner agentRunner) {
        if (agentRunner.isClosed()) {
            throw new IllegalStateException("agent runner is already closed");
        }
        if (agentRunner.isClosed() || agentRunner.thread() != null) {
            throw new IllegalStateException("agent runner is running already");
        }

        final long endWait = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        final Thread thread = AgentRunner.startOnThread(agentRunner);
        while (agentRunner.thread() != thread) {
            LockSupport.parkNanos(10_000_000);
            if (System.currentTimeMillis() > endWait) {
                throw new RuntimeException(agentRunner + " has not started on thread after " + START_TIMEOUT_MILLIS + " millis");
            }
        }
        return new ElaraRunner(agentRunner);
    }

    public Thread thread() {
        return agentRunner.thread();
    }

    public Thread.State threadState() {
        final Thread thread = thread();
        return thread != null ? thread.getState() : (agentRunner.isClosed() ? State.TERMINATED : State.NEW);
    }

    public void join(final long millis) {
        final Thread thread = thread();
        if (thread != null) {
            try {
                thread.join(millis);
            } catch (final InterruptedException e) {
                throw new IllegalStateException("Join interrupted for thread " + thread);
            }
        }
    }

    @Override
    public void close() {
        agentRunner.close();
    }

    @Override
    public String toString() {
        return agentRunner.agent().roleName();
    }
}
