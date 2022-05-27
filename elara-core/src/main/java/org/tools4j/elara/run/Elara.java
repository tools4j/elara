/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.Configuration;
import org.tools4j.elara.app.config.Context;
import org.tools4j.elara.factory.ElaraFactory;

/**
 * Starts an elara application.
 */
public enum Elara {
    ;

    @Deprecated
    public static ElaraRunner launch(final Context context) {
        return launch((Configuration)context.populateDefaults());
    }

    @Deprecated
    public static ElaraRunner launch(final ElaraFactory elaraFactory) {
        return launch(elaraFactory.configuration());
    }

    public static ElaraRunner launch(final AppConfig appConfig) {
        appConfig.validate();
        return ElaraRunner.startOnThread(new AgentRunner(
                appConfig.idleStrategy(), appConfig.exceptionHandler(), null, appConfig.createAgent()
        ));
    }
}
