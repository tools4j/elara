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

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.factory.ElaraFactory;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.init.Context;
import org.tools4j.nobark.loop.Loop;

import static java.util.Objects.requireNonNull;

/**
 * Starts an elara application.
 */
public enum Elara {
    ;

    public static ElaraRunner launch(final Context context) {
        return launch(ElaraFactory.create(context.validateAndPopulateDefaults()));
    }

    public static ElaraRunner launch(final Configuration configuration) {
        return launch(ElaraFactory.create(configuration));
    }

    public static ElaraRunner launch(final ElaraFactory elaraFactory) {
        final Configuration context = elaraFactory.configuration();
        return new ElaraRunner(Loop.start(
                nobarkIdleStrategy(context.idleStrategy()),
                context.exceptionHandler(),
                context.threadFactory(),
                elaraFactory.dutyCycleStep()
        ));
    }

    private static org.tools4j.nobark.loop.IdleStrategy nobarkIdleStrategy(final IdleStrategy idleStrategy) {
        requireNonNull(idleStrategy);
        return new org.tools4j.nobark.loop.IdleStrategy() {
            @Override
            public void idle() {
                idleStrategy.idle();
            }

            @Override
            public void reset() {
                idleStrategy.reset();
            }

            @Override
            public void idle(final boolean workDone) {
                idleStrategy.idle(workDone ? 1 : 0);
            }

            @Override
            public String toString() {
                return idleStrategy.toString();
            }
        };
    }
}
