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
package org.tools4j.elara.app.config;

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.app.state.BaseStateProvider;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.time.TimeSource;

public interface AppConfigurator extends AppConfig {
    AppConfigurator baseStateProvider(BaseStateProvider baseStateFactory);
    AppConfigurator timeSource(TimeSource timeSource);
    AppConfigurator exceptionHandler(ExceptionHandler exceptionHandler);
    AppConfigurator loggerFactory(Logger.Factory loggerFactory);
    AppConfigurator idleStrategy(IdleStrategy idleStrategy);
    AppConfigurator dutyCycleExtraStep(AgentStep step, ExecutionType executionType);
}
