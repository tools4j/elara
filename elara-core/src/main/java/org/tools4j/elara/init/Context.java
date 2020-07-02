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
package org.tools4j.elara.init;

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.time.TimeSource;
import org.tools4j.nobark.loop.Step;

import java.util.Collection;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Context extends Configuration {
    Context commandProcessor(CommandProcessor commandProcessor);
    Context eventApplier(EventApplier eventApplier);
    Context input(Input input);
    Context inputs(Input... inputs);
    Context inputs(Collection<? extends Input> inputs);
    Context output(Output output);
    Context commandLog(String file);
    Context commandLog(MessageLog commandLog);
    Context commandLogMode(CommandLogMode mode);
    Context eventLog(String file);
    Context eventLog(MessageLog eventLog);
    Context timeSource(TimeSource timeSource);
    Context exceptionHandler(ExceptionHandler exceptionHandler);
    Context duplicateHandler(DuplicateHandler duplicateHandler);
    Context idleStrategy(IdleStrategy idleStrategy);
    Context dutyCycleExtraStep(Step step, ExecutionType executionType);
    Context threadFactory(String threadName);
    Context threadFactory(ThreadFactory threadFactory);

    Context plugin(Plugin<?> plugin);
    <P> Context plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    <P> Context plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    <P> Context plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    Context populateDefaults();

    static Context create() {
        return new DefaultContext();
    }
}
