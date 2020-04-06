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

import org.tools4j.elara.application.Application;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.init.Launcher;
import org.tools4j.elara.init.PluginConfigurer;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.nobark.run.ThreadLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum  Elara {
    ;

    public static ThreadLike launch(final Application application) {
        return launch(Context.create(), application);
    }

    public static ThreadLike launch(final Context context, final Application application) {
        return launch(context, application, Collections.emptyList());
    }

    public static <A extends Application> ThreadLike launch(final Context context,
                                                            final A application,
                                                            final Plugin<?>... plugins) {
        final List<Plugin.Builder<? super A>> builders = new ArrayList<>(plugins.length);
        for (final Plugin<?> plugin : plugins) {
            builders.add(plugin.builder());
        }
        return launch(context, application, builders);
    }

    public static <A extends Application> ThreadLike launch(final Context context,
                                                            final A application,
                                                            final PluginConfigurer<A> plugins) {
        return launch(context, application, plugins.plugins());
    }

    public static <A extends Application> ThreadLike launch(final Context context,
                                                            final A application,
                                                            final List<Plugin.Builder<? super A>> pluginBuilders) {
        return Launcher.start(context, application, pluginBuilders);
    }
}