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
package org.tools4j.elara.factory;

import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.loop.DutyCycleStep;

/**
 * Main elara factory to create and wire elara objects, in particular the {@link #dutyCycleStep()} run by the elara
 * thread.  The elara factory contains pointers to sub-factories that are creating the other elara objects wired into
 * the duty cycle step.
 *
 * @see org.tools4j.elara.run.Elara
 */
public interface ElaraFactory {

    Configuration configuration();

    InputFactory inputFactory();
    ProcessorFactory processorFactory();
    ApplierFactory applierFactory();
    OutputFactory outputFactory();
    PluginFactory pluginFactory();

    DutyCycleStep dutyCycleStep();

    /**
     * Creates a new elara factory for the provided configuration.
     * @param configuration the configuration for the application to create
     * @return a new factory to create and wire elara objects
     */
    static ElaraFactory create(final Configuration configuration) {
        return new DefaultElaraFactory(configuration);
    }
}
